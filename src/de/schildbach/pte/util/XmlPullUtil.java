/*
 * For license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/).
 * According to www.xmlpull.org, this code is in the public domain.
 */

package de.schildbach.pte.util;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Handy functions that combines XmlPull API into higher level functionality.
 * 
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 * @author Naresh Bhatia
 */
public final class XmlPullUtil
{
	public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

	/**
	 * directly jumps forward to start tag, ignoring any structure
	 */
	public static void jump(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		if (!jumpToStartTag(pp, null, tagName))
			throw new IllegalStateException("cannot find <" + tagName + " />");
	}

	public static void require(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, tagName);
	}

	/**
	 * enters current tag
	 * 
	 * @throws IOException
	 */
	public static void enter(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() != XmlPullParser.START_TAG)
			throw new IllegalStateException("expecting start tag to enter");
		if (pp.isEmptyElementTag())
			throw new IllegalStateException("cannot enter empty tag");

		pp.next();
	}

	public static void enter(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, tagName);
		enter(pp);
	}

	public static void exit(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		exitSkipToEnd(pp);

		if (pp.getEventType() != XmlPullParser.END_TAG)
			throw new IllegalStateException("expecting end tag to exit");

		pp.next();
	}

	public static void exit(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		exitSkipToEnd(pp);
		pp.require(XmlPullParser.END_TAG, null, tagName);
		pp.next();
	}

	private static void exitSkipToEnd(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		while (pp.getEventType() != XmlPullParser.END_TAG)
		{
			if (pp.getEventType() == XmlPullParser.START_TAG)
				next(pp);
			else if (pp.getEventType() == XmlPullParser.TEXT)
				pp.next();
			else
				throw new IllegalStateException();
		}
	}

	public static boolean test(final XmlPullParser pp, final String tagName) throws XmlPullParserException
	{
		return pp.getEventType() == XmlPullParser.START_TAG && pp.getName().equals(tagName);
	}

	public static void next(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		skipSubTree(pp);
		pp.next();
	}

	public static String attr(final XmlPullParser pp, final String attrName)
	{
		return pp.getAttributeValue(null, attrName).trim();
	}

	public static int intAttr(final XmlPullParser pp, final String attrName)
	{
		return Integer.parseInt(pp.getAttributeValue(null, attrName).trim());
	}

	public static float floatAttr(final XmlPullParser pp, final String attrName)
	{
		return Float.parseFloat(pp.getAttributeValue(null, attrName).trim());
	}

	public static void requireAttr(final XmlPullParser pp, final String attrName, final String requiredValue)
	{
		if (!requiredValue.equals(attr(pp, attrName)))
			throw new IllegalStateException("cannot find " + attrName + "=\"" + requiredValue + "\" />");
	}

	public static String text(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() != XmlPullParser.START_TAG || pp.isEmptyElementTag())
			throw new IllegalStateException("expecting start tag to get text from");

		enter(pp);

		String text = "";
		if (pp.getEventType() == XmlPullParser.TEXT)
			text = pp.getText();

		exit(pp);

		return text;
	}

	/**
	 * Return value of attribute with given name and no namespace.
	 */
	public static String getAttributeValue(final XmlPullParser pp, final String name)
	{
		return pp.getAttributeValue(XmlPullParser.NO_NAMESPACE, name);
	}

	/**
	 * Return PITarget from Processing Instruction (PI) as defined in XML 1.0 Section 2.6 Processing Instructions <code>[16] PI ::= '&lt;?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'</code>
	 */
	public static String getPITarget(final XmlPullParser pp) throws IllegalStateException
	{
		int eventType;

		try
		{
			eventType = pp.getEventType();
		}
		catch (final XmlPullParserException x)
		{
			// should never happen ...
			throw new IllegalStateException("could not determine parser state: " + x + pp.getPositionDescription());
		}

		if (eventType != XmlPullParser.PROCESSING_INSTRUCTION)
			throw new IllegalStateException("parser must be on processing instruction and not " + XmlPullParser.TYPES[eventType]
					+ pp.getPositionDescription());

		final String PI = pp.getText();
		for (int i = 0; i < PI.length(); i++)
		{
			if (isS(PI.charAt(i)))
			{
				// assert i > 0
				return PI.substring(0, i);
			}
		}

		return PI;
	}

	/**
	 * Return everything past PITarget and S from Processing Instruction (PI) as defined in XML 1.0 Section 2.6
	 * Processing Instructions <code>[16] PI ::= '&lt;?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'</code>
	 * 
	 * <p>
	 * <b>NOTE:</b> if there is no PI data it returns empty string.
	 */
	public static String getPIData(final XmlPullParser pp) throws IllegalStateException
	{
		int eventType;

		try
		{
			eventType = pp.getEventType();
		}
		catch (final XmlPullParserException x)
		{
			// should never happen ...
			throw new IllegalStateException("could not determine parser state: " + x + pp.getPositionDescription());
		}

		if (eventType != XmlPullParser.PROCESSING_INSTRUCTION)
			throw new IllegalStateException("parser must be on processing instruction and not " + XmlPullParser.TYPES[eventType]
					+ pp.getPositionDescription());

		final String PI = pp.getText();
		int pos = -1;
		for (int i = 0; i < PI.length(); i++)
		{
			if (isS(PI.charAt(i)))
			{
				pos = i;
			}
			else if (pos > 0)
			{
				return PI.substring(i);
			}
		}

		return "";
	}

	/**
	 * Return true if chacters is S as defined in XML 1.0 <code>S ::=  (#x20 | #x9 | #xD | #xA)+</code>
	 */
	private static boolean isS(final char ch)
	{
		return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t');
	}

	/**
	 * Skip sub tree that is currently porser positioned on. <br>
	 * NOTE: parser must be on START_TAG and when funtion returns parser will be positioned on corresponding END_TAG
	 */
	public static void skipSubTree(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, null);

		int level = 1;
		while (level > 0)
		{
			final int eventType = pp.next();
			if (eventType == XmlPullParser.END_TAG)
				--level;
			else if (eventType == XmlPullParser.START_TAG)
				++level;
		}
	}

	/**
	 * call parser nextTag() and check that it is START_TAG, throw exception if not.
	 */
	public static void nextStartTag(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.nextTag() != XmlPullParser.START_TAG)
			throw new XmlPullParserException("expected START_TAG and not " + pp.getPositionDescription());
	}

	/**
	 * combine nextTag(); pp.require(XmlPullParser.START_TAG, null, name);
	 */
	public static void nextStartTag(final XmlPullParser pp, final String name) throws XmlPullParserException, IOException
	{
		pp.nextTag();
		pp.require(XmlPullParser.START_TAG, null, name);
	}

	/**
	 * combine nextTag(); pp.require(XmlPullParser.START_TAG, namespace, name);
	 */
	public static void nextStartTag(final XmlPullParser pp, final String namespace, final String name) throws XmlPullParserException, IOException
	{
		pp.nextTag();
		pp.require(XmlPullParser.START_TAG, namespace, name);
	}

	/**
	 * combine nextTag(); pp.require(XmlPullParser.END_TAG, namespace, name);
	 */
	public static void nextEndTag(final XmlPullParser pp, final String namespace, final String name) throws XmlPullParserException, IOException
	{
		pp.nextTag();
		pp.require(XmlPullParser.END_TAG, namespace, name);
	}

	/**
	 * Read text content of element ith given namespace and name (use null namespace do indicate that nemspace should
	 * not be checked)
	 */

	public static String nextText(final XmlPullParser pp, final String namespace, final String name) throws IOException, XmlPullParserException
	{
		if (name == null)
			throw new XmlPullParserException("name for element can not be null");

		pp.require(XmlPullParser.START_TAG, namespace, name);
		return pp.nextText();
	}

	/**
	 * Read attribute value and return it or throw exception if current element does not have such attribute.
	 */

	public static String getRequiredAttributeValue(final XmlPullParser pp, final String namespace, final String name) throws IOException,
			XmlPullParserException
	{
		final String value = pp.getAttributeValue(namespace, name);
		if (value == null)
			throw new XmlPullParserException("required attribute " + name + " is not present");
		else
			return value;
	}

	/**
	 * Call parser nextTag() and check that it is END_TAG, throw exception if not.
	 */
	public static void nextEndTag(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.nextTag() != XmlPullParser.END_TAG)
			throw new XmlPullParserException("expected END_TAG and not" + pp.getPositionDescription());
	}

	/**
	 * Tests if the current event is of the given type and if the namespace and name match. null will match any
	 * namespace and any name. If the test passes a true is returned otherwise a false is returned.
	 */
	public static boolean matches(final XmlPullParser pp, final int type, final String namespace, final String name) throws XmlPullParserException
	{
		boolean matches = type == pp.getEventType() && (namespace == null || namespace.equals(pp.getNamespace()))
				&& (name == null || name.equals(pp.getName()));

		return matches;
	}

	/**
	 * Writes a simple element such as <username>johndoe</username>. The namespace and elementText are allowed to be
	 * null. If elementText is null, an xsi:nil="true" will be added as an attribute.
	 */
	public static void writeSimpleElement(final XmlSerializer serializer, final String namespace, final String elementName, final String elementText)
			throws IOException, XmlPullParserException
	{
		if (elementName == null)
			throw new XmlPullParserException("name for element can not be null");

		serializer.startTag(namespace, elementName);
		if (elementText == null)
			serializer.attribute(XSI_NS, "nil", "true");
		else
			serializer.text(elementText);
		serializer.endTag(namespace, elementName);
	}

	/**
	 * This method bypasses all child subtrees until it reached END_TAG for current tree. Parser must be on START_TAG of
	 * one of child subtrees.
	 */
	public static void jumpToEndOfTree(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, null);

		while (true)
		{
			final int eventType = pp.next();
			if (eventType == XmlPullParser.START_TAG)
			{
				skipSubTree(pp);
				pp.require(XmlPullParser.END_TAG, null, null);
				pp.next(); // skip end tag
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				break;
			}
		}
	}

	/**
	 * This method bypasses all child subtrees until it finds a child subtree with start tag that matches the tag name
	 * (if not null) and namespsce (if not null) passed in. Parser must be positioned on START_TAG.
	 * <p>
	 * If succesfulpositions parser on such START_TAG and return true otherwise this method returns false and parser is
	 * positioned on END_TAG signaling last element in curren subtree.
	 */
	public static boolean jumpToSubTree(final XmlPullParser pp, final String tagNamespace, final String tagName) throws XmlPullParserException,
			IOException
	{
		if (tagNamespace == null && tagName == null)
			throw new IllegalArgumentException("namespace and name argument can not be both null:" + pp.getPositionDescription());

		pp.require(XmlPullParser.START_TAG, null, null);

		while (true)
		{
			final int eventType = pp.next();

			if (eventType == XmlPullParser.START_TAG)
			{
				final String name = pp.getName();
				final String namespace = pp.getNamespace();
				boolean matches = (tagNamespace != null && tagNamespace.equals(namespace)) || (tagName != null && tagName.equals(name));
				if (matches)
					return true;

				skipSubTree(pp);
				pp.require(XmlPullParser.END_TAG, name, namespace);
				pp.next(); // skip end tag
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				return false;
			}
		}
	}

	public static boolean nextStartTagInsideTree(final XmlPullParser pp, final String tagNamespace, final String tagName)
			throws XmlPullParserException, IOException
	{
		if (tagNamespace == null && tagName == null)
			throw new IllegalArgumentException("namespace and name argument can not be both null:" + pp.getPositionDescription());

		if (pp.getEventType() != XmlPullParser.START_TAG && pp.getEventType() != XmlPullParser.END_TAG)
			throw new IllegalStateException("expected START_TAG of parent or END_TAG of child:" + pp.getPositionDescription());

		while (true)
		{
			final int eventType = pp.next();

			if (eventType == XmlPullParser.START_TAG)
			{
				final String name = pp.getName();
				final String namespace = pp.getNamespace();
				boolean matches = (tagNamespace != null && tagNamespace.equals(namespace)) || (tagName != null && tagName.equals(name));
				if (matches)
					return true;

				skipSubTree(pp);
				pp.require(XmlPullParser.END_TAG, namespace, name);
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				return false;
			}
		}
	}

	public static void skipRestOfTree(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() != XmlPullParser.START_TAG && pp.getEventType() != XmlPullParser.END_TAG)
			throw new IllegalStateException("expected START_TAG of parent or END_TAG of child:" + pp.getPositionDescription());

		while (true)
		{
			final int eventType = pp.next();

			if (eventType == XmlPullParser.START_TAG)
			{
				skipSubTree(pp);
				pp.require(XmlPullParser.END_TAG, null, null);
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				return;
			}
		}
	}

	/**
	 * This method bypasses all events until it finds a start tag that has passed in namespace (if not null) and
	 * namespace (if not null).
	 * 
	 * @return true if such START_TAG was found or false otherwise (and parser is on END_DOCUMENT).
	 */
	public static boolean jumpToStartTag(final XmlPullParser pp, final String tagNamespace, final String tagName) throws XmlPullParserException,
			IOException
	{
		if (tagNamespace == null && tagName == null)
			throw new IllegalArgumentException("namespace and name argument can not be both null:" + pp.getPositionDescription());

		while (true)
		{
			final int eventType = pp.next();

			if (eventType == XmlPullParser.START_TAG)
			{
				final String name = pp.getName();
				final String namespace = pp.getNamespace();

				boolean matches = (tagNamespace != null && tagNamespace.equals(namespace)) || (tagName != null && tagName.equals(name));
				if (matches)
					return true;
			}
			else if (eventType == XmlPullParser.END_DOCUMENT)
			{
				return false;
			}
		}
	}

	/**
	 * This method bypasses all events until it finds an end tag that has passed in namespace (if not null) and
	 * namespace (if not null).
	 * 
	 * @return true if such END_TAG was found or false otherwise (and parser is on END_DOCUMENT).
	 */
	public static boolean jumpToEndTag(final XmlPullParser pp, final String tagNamespace, final String tagName) throws XmlPullParserException,
			IOException
	{
		if (tagNamespace == null && tagName == null)
			throw new IllegalArgumentException("namespace and name argument can not be both null:" + pp.getPositionDescription());

		while (true)
		{
			final int eventType = pp.next();
			if (eventType == XmlPullParser.END_TAG)
			{
				final String name = pp.getName();
				final String namespace = pp.getNamespace();

				boolean matches = (tagNamespace != null && tagNamespace.equals(namespace)) || (tagName != null && tagName.equals(name));
				if (matches)
					return true;
			}
			else if (eventType == XmlPullParser.END_DOCUMENT)
			{
				return false;
			}
		}
	}
}
