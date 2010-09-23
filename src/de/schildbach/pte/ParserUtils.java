/*
 * Copyright 2010 the original author or authors.
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

package de.schildbach.pte;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public final class ParserUtils
{
	private static final String SCRAPE_USER_AGENT = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.10) Gecko/20100915 Ubuntu/10.04 (lucid) Firefox/3.6.10";
	private static final int SCRAPE_INITIAL_CAPACITY = 4096;
	private static final int SCRAPE_CONNECT_TIMEOUT = 5000;
	private static final int SCRAPE_READ_TIMEOUT = 10000;
	private static final String SCRAPE_DEFAULT_ENCODING = "ISO-8859-1";

	private static String stateCookie;

	public static void resetState()
	{
		stateCookie = null;
	}

	public static CharSequence scrape(final String url) throws IOException
	{
		return scrape(url, false, null, null, false);
	}

	public static CharSequence scrape(final String url, final boolean isPost, final String request, String encoding, final boolean cookieHandling)
			throws IOException
	{
		if (encoding == null)
			encoding = SCRAPE_DEFAULT_ENCODING;

		int tries = 3;

		while (true)
		{
			try
			{
				final StringBuilder buffer = new StringBuilder(SCRAPE_INITIAL_CAPACITY);
				final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

				connection.setDoInput(true);
				connection.setDoOutput(request != null);
				connection.setConnectTimeout(SCRAPE_CONNECT_TIMEOUT);
				connection.setReadTimeout(SCRAPE_READ_TIMEOUT);
				connection.addRequestProperty("User-Agent", SCRAPE_USER_AGENT);
				// workaround to disable Vodafone compression
				connection.addRequestProperty("Cache-Control", "no-cache");

				if (cookieHandling && stateCookie != null)
				{
					connection.addRequestProperty("Cookie", stateCookie);
				}

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
				copy(pageReader, buffer);
				pageReader.close();

				if (buffer.length() > 0)
				{
					if (cookieHandling)
					{
						for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
						{
							if (entry.getKey().equalsIgnoreCase("set-cookie"))
							{
								for (final String value : entry.getValue())
								{
									if (value.startsWith("NSC_"))
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
					if (tries-- > 0)
						System.out.println("got empty page, retrying...");
					else
						throw new IOException("got empty page: " + url);
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

	private static long copy(final Reader reader, final StringBuilder builder) throws IOException
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

	private static final Pattern P_ENTITY = Pattern.compile("&(?:#(x[\\da-f]+|\\d+)|(amp|quot|apos));");

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

	public static Date parseDate(final String str)
	{
		try
		{
			return new SimpleDateFormat("dd.MM.yy").parse(str);
		}
		catch (final ParseException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static Date parseDateSlash(final String str)
	{
		try
		{
			return new SimpleDateFormat("dd/MM/yy").parse(str);
		}
		catch (final ParseException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static Date parseTime(final String str)
	{
		try
		{
			return new SimpleDateFormat("HH:mm").parse(str);
		}
		catch (final ParseException x)
		{
			throw new RuntimeException(x);
		}
	}

	public static Date joinDateTime(final Date date, final Date time)
	{
		final Calendar cDate = new GregorianCalendar();
		cDate.setTime(date);
		final Calendar cTime = new GregorianCalendar();
		cTime.setTime(time);
		cTime.set(Calendar.YEAR, cDate.get(Calendar.YEAR));
		cTime.set(Calendar.MONTH, cDate.get(Calendar.MONTH));
		cTime.set(Calendar.DAY_OF_MONTH, cDate.get(Calendar.DAY_OF_MONTH));
		return cTime.getTime();
	}

	public static long timeDiff(Date d1, Date d2)
	{
		return d1.getTime() - d2.getTime();
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

	public static String extractId(final String link)
	{
		return link.substring(link.length() - 10);
	}

	public static final String P_PLATFORM = "[\\wÄÖÜäöüßáàâéèêíìîóòôúùû\\. -/&#;]+?";
}
