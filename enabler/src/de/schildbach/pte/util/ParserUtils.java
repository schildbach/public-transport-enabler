/*
 * Copyright 2010-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import de.schildbach.pte.exception.BlockedException;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.exception.UnexpectedRedirectException;

/**
 * @author Andreas Schildbach
 */
public final class ParserUtils
{
	private static final String SCRAPE_USER_AGENT = "Mozilla/5.0 (Linux; Android 4.4.4; Nexus 7 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36";
	private static final String SCRAPE_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	public static final int SCRAPE_INITIAL_CAPACITY = 4096;
	private static final int SCRAPE_COPY_SIZE = 2048;
	private static final int SCRAPE_PEEK_SIZE = 4096;
	private static final int SCRAPE_CONNECT_TIMEOUT = 5000;
	private static final int SCRAPE_READ_TIMEOUT = 15000;
	private static HttpCookie sessionCookie;

	private static final Logger log = LoggerFactory.getLogger(ParserUtils.class);

	public static final CharSequence scrape(final String url) throws IOException
	{
		return scrape(url, null);
	}

	public static final CharSequence scrape(final String url, final String authorization) throws IOException
	{
		return scrape(url, null, null, null, authorization);
	}

	public static final CharSequence scrape(final String url, final String postRequest, final Charset encoding) throws IOException
	{
		return scrape(url, postRequest, encoding, null);
	}

	public static final CharSequence scrape(final String urlStr, final String postRequest, final Charset requestEncoding,
			final String sessionCookieName) throws IOException
	{
		return scrape(urlStr, postRequest, requestEncoding, sessionCookieName, null);
	}

	private static final CharSequence scrape(final String urlStr, final String postRequest, Charset requestEncoding, final String sessionCookieName,
			final String authorization) throws IOException
	{
		if (requestEncoding == null)
			requestEncoding = Charsets.ISO_8859_1;

		final StringBuilder buffer = new StringBuilder(SCRAPE_INITIAL_CAPACITY);
		final InputStream is = scrapeInputStream(urlStr, postRequest, requestEncoding, null, sessionCookieName, authorization);
		final Reader pageReader = new InputStreamReader(is, requestEncoding);
		copy(pageReader, buffer);
		pageReader.close();
		return buffer;
	}

	public static final long copy(final Reader reader, final StringBuilder builder) throws IOException
	{
		final char[] buffer = new char[SCRAPE_COPY_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = reader.read(buffer)))
		{
			builder.append(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static final InputStream scrapeInputStream(final String url) throws IOException
	{
		return scrapeInputStream(url, null);
	}

	public static final InputStream scrapeInputStream(final String url, final String sessionCookieName) throws IOException
	{
		return scrapeInputStream(url, null, null, null, sessionCookieName);
	}

	public static final InputStream scrapeInputStream(final String urlStr, final String postRequest, final Charset requestEncoding,
			final String referer, final String sessionCookieName) throws IOException
	{
		return scrapeInputStream(urlStr, postRequest, requestEncoding, referer, sessionCookieName, null);
	}

	public static final InputStream scrapeInputStream(final String urlStr, final String postRequest, Charset requestEncoding, final String referer,
			final String sessionCookieName, final String authorization) throws IOException
	{
		log.debug("{}: {}", postRequest != null ? "POST" : "GET", urlStr);

		if (requestEncoding == null)
			requestEncoding = Charsets.ISO_8859_1;

		int tries = 3;

		while (true)
		{
			final URL url = new URL(urlStr);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setDoInput(true);
			connection.setDoOutput(postRequest != null);
			connection.setConnectTimeout(SCRAPE_CONNECT_TIMEOUT);
			connection.setReadTimeout(SCRAPE_READ_TIMEOUT);
			connection.addRequestProperty("User-Agent", SCRAPE_USER_AGENT);
			connection.addRequestProperty("Accept", SCRAPE_ACCEPT);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			// workaround to disable Vodafone compression
			connection.addRequestProperty("Cache-Control", "no-cache");

			if (referer != null)
				connection.addRequestProperty("Referer", referer);

			if (sessionCookie != null && sessionCookie.getName().equals(sessionCookieName))
				connection.addRequestProperty("Cookie", sessionCookie.toString());

			// Set authorization.
			if (authorization != null)
				connection.addRequestProperty("Authorization", authorization);

			if (postRequest != null)
			{
				final byte[] postRequestBytes = postRequest.getBytes(requestEncoding.name());

				connection.setRequestMethod("POST");
				connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.addRequestProperty("Content-Length", Integer.toString(postRequestBytes.length));

				final OutputStream os = connection.getOutputStream();
				os.write(postRequestBytes);
				os.close();
			}

			final int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK)
			{
				final String contentType = connection.getContentType();
				final String contentEncoding = connection.getContentEncoding();

				InputStream is = new BufferedInputStream(connection.getInputStream());

				if ("gzip".equalsIgnoreCase(contentEncoding) || "application/octet-stream".equalsIgnoreCase(contentType))
					is = wrapGzip(is);

				if (!url.getHost().equals(connection.getURL().getHost()))
					throw new UnexpectedRedirectException(url, connection.getURL());

				final String firstChars = peekFirstChars(is);

				final URL redirectUrl = testRedirect(url, firstChars);
				if (redirectUrl != null)
					throw new UnexpectedRedirectException(url, redirectUrl);

				if (testExpired(firstChars))
					throw new SessionExpiredException();

				if (testInternalError(firstChars))
					throw new InternalErrorException(url, new InputStreamReader(is, requestEncoding));

				// save cookie
				if (sessionCookieName != null)
				{
					c: for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
					{
						if ("set-cookie".equalsIgnoreCase(entry.getKey()) || "set-cookie2".equalsIgnoreCase(entry.getKey()))
						{
							for (final String value : entry.getValue())
							{
								for (final HttpCookie cookie : HttpCookie.parse(value))
								{
									if (cookie.getName().equals(sessionCookieName))
									{
										sessionCookie = cookie;
										break c;
									}
								}
							}
						}
					}
				}

				return is;
			}
			else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
					|| responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE
					|| responseCode == HttpURLConnection.HTTP_UNAVAILABLE)
			{
				throw new BlockedException(url, new InputStreamReader(connection.getErrorStream(), requestEncoding));
			}
			else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND)
			{
				throw new NotFoundException(url, new InputStreamReader(connection.getErrorStream(), requestEncoding));
			}
			else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				throw new UnexpectedRedirectException(url, connection.getURL());
			}
			else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
			{
				throw new InternalErrorException(url, new InputStreamReader(connection.getErrorStream(), requestEncoding));
			}
			else
			{
				final String message = "got response: " + responseCode + " " + connection.getResponseMessage();
				if (tries-- > 0)
					log.info("{}, retrying...", message);
				else
					throw new IOException(message + ": " + url);
			}
		}
	}

	private static InputStream wrapGzip(final InputStream is) throws IOException
	{
		is.mark(2);
		final int byte0 = is.read();
		final int byte1 = is.read();
		is.reset();

		// check for gzip header
		if (byte0 == 0x1f && byte1 == 0x8b)
		{
			final BufferedInputStream is2 = new BufferedInputStream(new GZIPInputStream(is));
			is2.mark(2);
			final int byte0_2 = is2.read();
			final int byte1_2 = is2.read();
			is2.reset();

			// check for gzip header again
			if (byte0_2 == 0x1f && byte1_2 == 0x8b)
			{
				// double gzipped
				return new BufferedInputStream(new GZIPInputStream(is2));
			}
			else
			{
				// gzipped
				return is2;
			}
		}
		else
		{
			// uncompressed
			return is;
		}
	}

	public static String peekFirstChars(final InputStream is) throws IOException
	{
		is.mark(SCRAPE_PEEK_SIZE);
		final byte[] firstBytes = new byte[SCRAPE_PEEK_SIZE];
		final int read = is.read(firstBytes);
		if (read == -1)
			return "";
		is.reset();
		return new String(firstBytes, 0, read).replaceAll("\\p{C}", "");
	}

	private static final Pattern P_REDIRECT_HTTP_EQUIV = Pattern.compile("<META\\s+http-equiv=\"?refresh\"?\\s+content=\"\\d+;\\s*URL=([^\"]+)\"",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern P_REDIRECT_SCRIPT = Pattern.compile(
			"<script\\s+(?:type=\"text/javascript\"|language=\"javascript\")>\\s*(?:window.location|location.href)\\s*=\\s*\"([^\"]+)\"",
			Pattern.CASE_INSENSITIVE);

	public static URL testRedirect(final URL context, final String content) throws MalformedURLException
	{
		// check for redirect by http-equiv meta tag header
		final Matcher mHttpEquiv = P_REDIRECT_HTTP_EQUIV.matcher(content);
		if (mHttpEquiv.find())
			return new URL(context, mHttpEquiv.group(1));

		// check for redirect by window.location javascript
		final Matcher mScript = P_REDIRECT_SCRIPT.matcher(content);
		if (mScript.find())
			return new URL(context, mScript.group(1));

		return null;
	}

	private static final Pattern P_EXPIRED = Pattern
			.compile(">\\s*(Your session has expired\\.|Session Expired|Ihre Verbindungskennung ist nicht mehr g.ltig\\.)\\s*<");

	public static boolean testExpired(final String content)
	{
		// check for expired session
		final Matcher mSessionExpired = P_EXPIRED.matcher(content);
		if (mSessionExpired.find())
			return true;

		return false;
	}

	private static final Pattern P_INTERNAL_ERROR = Pattern
			.compile(">\\s*(Internal Error|Server ein Fehler aufgetreten|Internal error in gateway|VRN - Keine Verbindung zum Server m.glich)\\s*<");

	public static boolean testInternalError(final String content)
	{
		// check for internal error
		final Matcher m = P_INTERNAL_ERROR.matcher(content);
		if (m.find())
			return true;

		return false;
	}

	private static final Pattern P_HTML_UNORDERED_LIST = Pattern.compile("<ul>(.*?)</ul>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern P_HTML_LIST_ITEM = Pattern.compile("<li>(.*?)</li>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern P_HTML_BREAKS = Pattern.compile("(<br\\s*/>)+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	public static String formatHtml(final CharSequence html)
	{
		if (html == null)
			return null;

		// list item
		final StringBuilder builder = new StringBuilder(html.length());
		final Matcher mListItem = P_HTML_LIST_ITEM.matcher(html);
		int pListItem = 0;
		while (mListItem.find())
		{
			builder.append(html.subSequence(pListItem, mListItem.start()));
			builder.append("• ");
			builder.append(mListItem.group(1));
			builder.append('\n');
			pListItem = mListItem.end();
		}
		builder.append(html.subSequence(pListItem, html.length()));
		final String html1 = builder.toString();

		// unordered list
		builder.setLength(0);
		final Matcher mUnorderedList = P_HTML_UNORDERED_LIST.matcher(html1);
		int pUnorderedList = 0;
		while (mUnorderedList.find())
		{
			builder.append(html1.subSequence(pUnorderedList, mUnorderedList.start()));
			builder.append('\n');
			builder.append(mUnorderedList.group(1));
			pUnorderedList = mUnorderedList.end();
		}
		builder.append(html1.subSequence(pUnorderedList, html1.length()));
		final String html2 = builder.toString();

		// breaks
		builder.setLength(0);
		final Matcher mBreaks = P_HTML_BREAKS.matcher(html2);
		int pBreaks = 0;
		while (mBreaks.find())
		{
			builder.append(html2.subSequence(pBreaks, mBreaks.start()));
			builder.append(' ');
			pBreaks = mBreaks.end();
		}
		builder.append(html2.subSequence(pBreaks, html2.length()));
		final String html3 = builder.toString();

		return resolveEntities(html3);
	}

	private static final Pattern P_ENTITY = Pattern.compile("&(?:#(x[\\da-f]+|\\d+)|(amp|quot|apos|szlig|nbsp));");

	public static String resolveEntities(final CharSequence str)
	{
		if (str == null)
			return null;

		final Matcher matcher = P_ENTITY.matcher(str);
		final StringBuilder builder = new StringBuilder(str.length());
		int pos = 0;
		while (matcher.find())
		{
			final char c;
			final String code = matcher.group(1);
			if (code != null)
			{
				if (code.charAt(0) == 'x')
					c = (char) Integer.valueOf(code.substring(1), 16).intValue();
				else
					c = (char) Integer.parseInt(code);
			}
			else
			{
				final String namedEntity = matcher.group(2);
				if (namedEntity.equals("amp"))
					c = '&';
				else if (namedEntity.equals("quot"))
					c = '"';
				else if (namedEntity.equals("apos"))
					c = '\'';
				else if (namedEntity.equals("szlig"))
					c = '\u00df';
				else if (namedEntity.equals("nbsp"))
					c = ' ';
				else
					throw new IllegalStateException("unknown entity: " + namedEntity);
			}
			builder.append(str.subSequence(pos, matcher.start()));
			builder.append(c);
			pos = matcher.end();
		}
		builder.append(str.subSequence(pos, str.length()));
		return builder.toString();
	}

	private static final Pattern P_ISO_DATE = Pattern.compile("(\\d{4})-?(\\d{2})-?(\\d{2})");
	private static final Pattern P_ISO_DATE_REVERSE = Pattern.compile("(\\d{2})[-\\.](\\d{2})[-\\.](\\d{4})");

	public static final void parseIsoDate(final Calendar calendar, final CharSequence str)
	{
		final Matcher mIso = P_ISO_DATE.matcher(str);
		if (mIso.matches())
		{
			calendar.set(Calendar.YEAR, Integer.parseInt(mIso.group(1)));
			calendar.set(Calendar.MONTH, Integer.parseInt(mIso.group(2)) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mIso.group(3)));
			return;
		}

		final Matcher mIsoReverse = P_ISO_DATE_REVERSE.matcher(str);
		if (mIsoReverse.matches())
		{
			calendar.set(Calendar.YEAR, Integer.parseInt(mIsoReverse.group(3)));
			calendar.set(Calendar.MONTH, Integer.parseInt(mIsoReverse.group(2)) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mIsoReverse.group(1)));
			return;
		}

		throw new RuntimeException("cannot parse: '" + str + "'");
	}

	private static final Pattern P_ISO_TIME = Pattern.compile("(\\d{2})-?(\\d{2})");

	public static final void parseIsoTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher mIso = P_ISO_TIME.matcher(str);
		if (mIso.matches())
		{
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mIso.group(1)));
			calendar.set(Calendar.MINUTE, Integer.parseInt(mIso.group(2)));
			return;
		}

		throw new RuntimeException("cannot parse: '" + str + "'");
	}

	private static final Pattern P_GERMAN_DATE = Pattern.compile("(\\d{2})[\\./-](\\d{2})[\\./-](\\d{2,4})");

	public static final void parseGermanDate(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_GERMAN_DATE.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(1)));
		calendar.set(Calendar.MONTH, Integer.parseInt(m.group(2)) - 1);
		final int year = Integer.parseInt(m.group(3));
		calendar.set(Calendar.YEAR, year >= 100 ? year : year + 2000);
	}

	private static final Pattern P_AMERICAN_DATE = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2,4})");

	public static final void parseAmericanDate(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_AMERICAN_DATE.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.MONTH, Integer.parseInt(m.group(1)) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(2)));
		final int year = Integer.parseInt(m.group(3));
		calendar.set(Calendar.YEAR, year >= 100 ? year : year + 2000);
	}

	private static final Pattern P_EUROPEAN_TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");

	public static final void parseEuropeanTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_EUROPEAN_TIME.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(1)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
		calendar.set(Calendar.SECOND, m.group(3) != null ? Integer.parseInt(m.group(3)) : 0);
	}

	private static final Pattern P_AMERICAN_TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))? (AM|PM)");

	public static final void parseAmericanTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_AMERICAN_TIME.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.HOUR, Integer.parseInt(m.group(1)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
		calendar.set(Calendar.SECOND, m.group(3) != null ? Integer.parseInt(m.group(3)) : 0);
		calendar.set(Calendar.AM_PM, m.group(4).equals("AM") ? Calendar.AM : Calendar.PM);
	}

	public static long timeDiff(final Date d1, final Date d2)
	{
		final long t1 = d1.getTime();
		final long t2 = d2.getTime();
		return t1 - t2;
	}

	public static Date addDays(final Date time, final int days)
	{
		final Calendar c = new GregorianCalendar();
		c.setTime(time);
		c.add(Calendar.DAY_OF_YEAR, days);
		return c.getTime();
	}

	public static void printGroups(final Matcher m)
	{
		final int groupCount = m.groupCount();
		for (int i = 1; i <= groupCount; i++)
			System.out.println("group " + i + ":" + (m.group(i) != null ? "'" + m.group(i) + "'" : "null"));
	}

	public static void printXml(final CharSequence xml)
	{
		final Matcher m = Pattern.compile("(<.{80}.*?>)\\s*").matcher(xml);
		while (m.find())
			System.out.println(m.group(1));
	}

	public static void printPlain(final CharSequence plain)
	{
		final Matcher m = Pattern.compile("(.{1,80})").matcher(plain);
		while (m.find())
			System.out.println(m.group(1));
	}

	public static void printFromReader(final Reader reader) throws IOException
	{
		while (true)
		{
			final int c = reader.read();
			if (c == -1)
				return;

			System.out.print((char) c);
		}
	}

	public static String urlEncode(final String str)
	{
		try
		{
			return URLEncoder.encode(str, "utf-8");
		}
		catch (final UnsupportedEncodingException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static String urlEncode(final String str, final Charset encoding)
	{
		try
		{
			return URLEncoder.encode(str, encoding.name());
		}
		catch (final UnsupportedEncodingException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static String urlDecode(final String str, final Charset encoding)
	{
		try
		{
			return URLDecoder.decode(str, encoding.name());
		}
		catch (final UnsupportedEncodingException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static String firstNotEmpty(final String... strings)
	{
		for (final String str : strings)
			if (str != null && str.length() > 0)
				return str;

		return null;
	}

	public static final String P_PLATFORM = "[\\wÄÖÜäöüßáàâéèêíìîóòôúùû\\. -/&#;]+?";
}
