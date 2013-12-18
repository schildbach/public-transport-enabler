/*
 * For license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/).
 * According to www.xmlpull.org, this code is in the public domain.
 */

package de.schildbach.pte.util;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Handy functions that combines XmlPull API into higher level functionality.
 */
public final class XmlPullUtil
{
	public static boolean test(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		skipWhitespace(pp);

		return pp.getEventType() == XmlPullParser.START_TAG && pp.getName().equals(tagName);
	}

	public static void require(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		skipWhitespace(pp);

		pp.require(XmlPullParser.START_TAG, null, tagName);
	}

	public static void enter(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		skipWhitespace(pp);

		if (pp.getEventType() != XmlPullParser.START_TAG)
			throw new IllegalStateException("expecting start tag to enter");
		if (pp.isEmptyElementTag())
			throw new IllegalStateException("cannot enter empty tag");

		pp.next();
	}

	public static void enter(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		skipWhitespace(pp);

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

	private static void skipWhitespace(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() == XmlPullParser.START_DOCUMENT)
			pp.next();
		if (pp.getEventType() == XmlPullParser.TEXT && pp.isWhitespace())
			pp.next();
	}

	public static void requireSkip(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		require(pp, tagName);

		if (!pp.isEmptyElementTag())
		{
			enter(pp, tagName);
			exit(pp, tagName);
		}
		else
		{
			next(pp);
		}
	}

	public static void optSkip(final XmlPullParser pp, final String tagName) throws XmlPullParserException, IOException
	{
		if (test(pp, tagName))
			requireSkip(pp, tagName);
	}

	public static void next(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		skipSubTree(pp);
		pp.next();
	}

	public static String attr(final XmlPullParser pp, final String attrName)
	{
		final String attr = optAttr(pp, attrName, null);

		if (attr != null)
			return attr;
		else
			throw new IllegalStateException("expecting attribute: " + attrName);
	}

	public static String optAttr(final XmlPullParser pp, final String attrName, final String defaultValue)
	{
		final String attr = pp.getAttributeValue(null, attrName);

		if (attr != null)
		{
			final String trimmedAttr = attr.trim();

			if (trimmedAttr.length() > 0)
				return trimmedAttr;
		}

		return defaultValue;
	}

	public static int intAttr(final XmlPullParser pp, final String attrName)
	{
		return Integer.parseInt(attr(pp, attrName));
	}

	public static int optIntAttr(final XmlPullParser pp, final String attrName, final int defaultValue)
	{
		final String attr = optAttr(pp, attrName, null);
		if (attr != null)
			return Integer.parseInt(attr);
		else
			return defaultValue;
	}

	public static float floatAttr(final XmlPullParser pp, final String attrName)
	{
		return Float.parseFloat(attr(pp, attrName));
	}

	public static float optFloatAttr(final XmlPullParser pp, final String attrName, final float defaultValue)
	{
		final String attr = optAttr(pp, attrName, null);
		if (attr != null)
			return Float.parseFloat(attr);
		else
			return defaultValue;
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
	 * Read text content of element ith given namespace and name (use null namespace do indicate that nemspace should
	 * not be checked)
	 */

	public static String nextText(final XmlPullParser pp, final String namespace, final String name) throws IOException, XmlPullParserException
	{
		if (name == null)
			throw new XmlPullParserException("name for element can not be null");

		pp.require(XmlPullParser.START_TAG, namespace, name);
		final String text = pp.nextText();

		// work around http://code.google.com/p/android/issues/detail?id=21425
		if (pp.getEventType() != XmlPullParser.END_TAG)
			pp.nextTag();

		pp.require(XmlPullParser.END_TAG, namespace, name);

		return text;
	}
}
