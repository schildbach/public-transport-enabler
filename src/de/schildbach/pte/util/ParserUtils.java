/*
 * Copyright 2010, 2011 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import de.schildbach.pte.exception.UnexpectedRedirectException;

/**
 * @author Andreas Schildbach
 */
public final class ParserUtils
{
	private static final String SCRAPE_USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:2.0) Gecko/20100101 Firefox/4.0";
	private static final String SCRAPE_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	private static final int SCRAPE_INITIAL_CAPACITY = 4096;
	private static final int SCRAPE_CONNECT_TIMEOUT = 5000;
	private static final int SCRAPE_READ_TIMEOUT = 15000;
	private static final String SCRAPE_DEFAULT_ENCODING = "ISO-8859-1";
	private static final int SCRAPE_PAGE_EMPTY_THRESHOLD = 2;

	private static String stateCookie;

	public static void resetState()
	{
		stateCookie = null;
	}

	public static final CharSequence scrape(final String url) throws IOException
	{
		return scrape(url, false, null, null, null);
	}

	public static final CharSequence scrape(final String url, final boolean isPost, final String request, String encoding,
			final String sessionCookieName) throws IOException
	{
		return scrape(url, isPost, request, encoding, sessionCookieName, 3);
	}

	public static final CharSequence scrape(final String urlStr, final boolean isPost, final String request, String encoding,
			final String sessionCookieName, int tries) throws IOException
	{
		if (encoding == null)
			encoding = SCRAPE_DEFAULT_ENCODING;

		while (true)
		{
			try
			{
				final StringBuilder buffer = new StringBuilder(SCRAPE_INITIAL_CAPACITY);
				final URL url = new URL(urlStr);
				final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

				connection.setDoInput(true);
				connection.setDoOutput(request != null);
				connection.setConnectTimeout(SCRAPE_CONNECT_TIMEOUT);
				connection.setReadTimeout(SCRAPE_READ_TIMEOUT);
				connection.addRequestProperty("User-Agent", SCRAPE_USER_AGENT);
				connection.addRequestProperty("Accept", SCRAPE_ACCEPT);
				// workaround to disable Vodafone compression
				connection.addRequestProperty("Cache-Control", "no-cache");

				if (sessionCookieName != null && stateCookie != null)
					connection.addRequestProperty("Cookie", stateCookie);

				if (request != null)
				{
					if (isPost)
					{
						connection.setRequestMethod("POST");
						connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						connection.addRequestProperty("Content-Length", Integer.toString(request.length()));
					}

					final Writer writer = new OutputStreamWriter(connection.getOutputStream(), encoding);
					writer.write(request);
					writer.close();
				}

				final Reader pageReader = new InputStreamReader(connection.getInputStream(), encoding);
				if (!url.equals(connection.getURL()))
					throw new UnexpectedRedirectException(url, connection.getURL());
				copy(pageReader, buffer);
				pageReader.close();

				if (buffer.length() > SCRAPE_PAGE_EMPTY_THRESHOLD)
				{
					if (sessionCookieName != null)
					{
						for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
						{
							if ("set-cookie".equalsIgnoreCase(entry.getKey()))
							{
								for (final String value : entry.getValue())
								{
									if (value.startsWith(sessionCookieName))
									{
										stateCookie = value.split(";", 2)[0];
									}
								}
							}
						}
					}

					return buffer;
				}
				else
				{
					final String message = "got empty page (length: " + buffer.length() + ")";
					if (tries-- > 0)
						System.out.println(message + ", retrying...");
					else
						throw new IOException(message + ": " + url);
				}
			}
			catch (final SocketTimeoutException x)
			{
				if (tries-- > 0)
					System.out.println("socket timed out, retrying...");
				else
					throw x;
			}
		}
	}

	private static final long copy(final Reader reader, final StringBuilder builder) throws IOException
	{
		final char[] buffer = new char[SCRAPE_INITIAL_CAPACITY];
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
		return scrapeInputStream(url, null, null, 3);
	}

	public static final InputStream scrapeInputStream(final String urlStr, final String postRequest, final String sessionCookieName, int tries)
			throws IOException
	{
		while (true)
		{
			final URL url = new URL(urlStr);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setDoInput(true);
			connection.setDoOutput(postRequest != null);
			connection.setConnectTimeout(SCRAPE_CONNECT_TIMEOUT);
			connection.setReadTimeout(SCRAPE_READ_TIMEOUT);
			connection.addRequestProperty("User-Agent", SCRAPE_USER_AGENT);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			// workaround to disable Vodafone compression
			connection.addRequestProperty("Cache-Control", "no-cache");

			if (sessionCookieName != null && stateCookie != null)
				connection.addRequestProperty("Cookie", stateCookie);

			if (postRequest != null)
			{
				connection.setRequestMethod("POST");
				connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.addRequestProperty("Content-Length", Integer.toString(postRequest.length()));

				final Writer writer = new OutputStreamWriter(connection.getOutputStream(), SCRAPE_DEFAULT_ENCODING);
				writer.write(postRequest);
				writer.close();
			}

			final int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK)
			{
				final String contentEncoding = connection.getContentEncoding();
				final InputStream is = connection.getInputStream();
				if (!url.equals(connection.getURL()))
					throw new UnexpectedRedirectException(url, connection.getURL());

				if (sessionCookieName != null)
				{
					for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
					{
						if ("set-cookie".equalsIgnoreCase(entry.getKey()))
						{
							for (final String value : entry.getValue())
							{
								if (value.startsWith(sessionCookieName))
								{
									stateCookie = value.split(";", 2)[0];
								}
							}
						}
					}
				}

				if ("gzip".equalsIgnoreCase(contentEncoding))
					return new GZIPInputStream(is);

				return is;
			}
			else
			{
				final String message = "got response: " + responseCode + " " + connection.getResponseMessage();
				if (tries-- > 0)
					System.out.println(message + ", retrying...");
				else
					throw new IOException(message + ": " + url);
			}
		}
	}

	private static final Pattern P_ENTITY = Pattern.compile("&(?:#(x[\\da-f]+|\\d+)|(amp|quot|apos|szlig));");

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
					c = 'ß';
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

	private static final Pattern P_ISO_DATE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");

	public static final void parseIsoDate(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_ISO_DATE.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
		calendar.set(Calendar.MONTH, Integer.parseInt(m.group(2)) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(3)));
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

	public static String urlEncode(final String str, final String enc)
	{
		try
		{
			return URLEncoder.encode(str, enc);
		}
		catch (final UnsupportedEncodingException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static String urlDecode(final String str, final String enc)
	{
		try
		{
			return URLDecoder.decode(str, enc);
		}
		catch (final UnsupportedEncodingException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static <T> T selectNotNull(final T... groups)
	{
		T selected = null;

		for (final T group : groups)
		{
			if (group != null)
			{
				if (selected == null)
					selected = group;
				else
					throw new IllegalStateException("ambiguous");
			}
		}

		return selected;
	}

	public static final String P_PLATFORM = "[\\wÄÖÜäöüßáàâéèêíìîóòôúùû\\. -/&#;]+?";
}
