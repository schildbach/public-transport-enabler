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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public final class ParserUtils {
    private static final Pattern P_HTML_UNORDERED_LIST = Pattern.compile("<ul>(.*?)</ul>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern P_HTML_LIST_ITEM = Pattern.compile("<li>(.*?)</li>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern P_HTML_BREAKS = Pattern.compile("(<br\\s*/>)+",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static final Currency CURRENCY_EUR = Currency.getInstance("EUR");

    public static String formatHtml(final CharSequence html) {
        if (html == null)
            return null;

        // list item
        final StringBuilder builder = new StringBuilder(html.length());
        final Matcher mListItem = P_HTML_LIST_ITEM.matcher(html);
        int pListItem = 0;
        while (mListItem.find()) {
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
        while (mUnorderedList.find()) {
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
        while (mBreaks.find()) {
            builder.append(html2.subSequence(pBreaks, mBreaks.start()));
            builder.append(' ');
            pBreaks = mBreaks.end();
        }
        builder.append(html2.subSequence(pBreaks, html2.length()));
        final String html3 = builder.toString();

        return resolveEntities(html3);
    }

    private static final Pattern P_ENTITY = Pattern.compile("&(?:#(x[\\da-f]+|\\d+)|(amp|quot|apos|szlig|nbsp));");

    public static String resolveEntities(final CharSequence str) {
        if (str == null)
            return null;

        final Matcher matcher = P_ENTITY.matcher(str);
        final StringBuilder builder = new StringBuilder(str.length());
        int pos = 0;
        while (matcher.find()) {
            final char c;
            final String code = matcher.group(1);
            if (code != null) {
                if (code.charAt(0) == 'x')
                    c = (char) Integer.valueOf(code.substring(1), 16).intValue();
                else
                    c = (char) Integer.parseInt(code);
            } else {
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

    public static final void parseIsoDate(final Calendar calendar, final CharSequence str) {
        final Matcher mIso = P_ISO_DATE.matcher(str);
        if (mIso.matches()) {
            calendar.set(Calendar.YEAR, Integer.parseInt(mIso.group(1)));
            calendar.set(Calendar.MONTH, Integer.parseInt(mIso.group(2)) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mIso.group(3)));
            return;
        }

        final Matcher mIsoReverse = P_ISO_DATE_REVERSE.matcher(str);
        if (mIsoReverse.matches()) {
            calendar.set(Calendar.YEAR, Integer.parseInt(mIsoReverse.group(3)));
            calendar.set(Calendar.MONTH, Integer.parseInt(mIsoReverse.group(2)) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mIsoReverse.group(1)));
            return;
        }

        throw new RuntimeException("cannot parse: '" + str + "'");
    }

    private static final Pattern P_ISO_TIME = Pattern.compile("(\\d{2})[-:]?(\\d{2})([-:]?(\\d{2}))?");

    public static final void parseIsoTime(final Calendar calendar, final CharSequence str) {
        final Matcher mIso = P_ISO_TIME.matcher(str);
        if (mIso.matches()) {
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mIso.group(1)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(mIso.group(2)));
            calendar.set(Calendar.SECOND, mIso.group(4) != null ? Integer.parseInt(mIso.group(4)) : 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return;
        }

        throw new RuntimeException("cannot parse: '" + str + "'");
    }

    public static final void parseIsoDateTime(final Calendar calendar, final CharSequence str) {
        final String[] timeParts = str.toString().split("T");
        if (timeParts.length != 2)
            throw new RuntimeException("cannot parse :'" + str + "'");

        parseIsoDate(calendar, timeParts[0]);
        parseIsoTime(calendar, timeParts[1]);
    }

    private static final Pattern P_GERMAN_DATE = Pattern.compile("(\\d{2})[\\./-](\\d{2})[\\./-](\\d{2,4})");

    public static final void parseGermanDate(final Calendar calendar, final CharSequence str) {
        final Matcher m = P_GERMAN_DATE.matcher(str);
        if (!m.matches())
            throw new RuntimeException("cannot parse: '" + str + "'");

        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(1)));
        calendar.set(Calendar.MONTH, Integer.parseInt(m.group(2)) - 1);
        final int year = Integer.parseInt(m.group(3));
        calendar.set(Calendar.YEAR, year >= 100 ? year : year + 2000);
    }

    private static final Pattern P_AMERICAN_DATE = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2,4})");

    public static final void parseAmericanDate(final Calendar calendar, final CharSequence str) {
        final Matcher m = P_AMERICAN_DATE.matcher(str);
        if (!m.matches())
            throw new RuntimeException("cannot parse: '" + str + "'");

        calendar.set(Calendar.MONTH, Integer.parseInt(m.group(1)) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(2)));
        final int year = Integer.parseInt(m.group(3));
        calendar.set(Calendar.YEAR, year >= 100 ? year : year + 2000);
    }

    private static final Pattern P_EUROPEAN_TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");

    public static final void parseEuropeanTime(final Calendar calendar, final CharSequence str) {
        final Matcher m = P_EUROPEAN_TIME.matcher(str);
        if (!m.matches())
            throw new RuntimeException("cannot parse: '" + str + "'");

        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(1)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
        calendar.set(Calendar.SECOND, m.group(3) != null ? Integer.parseInt(m.group(3)) : 0);
    }

    private static final Pattern P_AMERICAN_TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))? (AM|PM)");

    public static final void parseAmericanTime(final Calendar calendar, final CharSequence str) {
        final Matcher m = P_AMERICAN_TIME.matcher(str);
        if (!m.matches())
            throw new RuntimeException("cannot parse: '" + str + "'");

        calendar.set(Calendar.HOUR, Integer.parseInt(m.group(1)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
        calendar.set(Calendar.SECOND, m.group(3) != null ? Integer.parseInt(m.group(3)) : 0);
        calendar.set(Calendar.AM_PM, m.group(4).equals("AM") ? Calendar.AM : Calendar.PM);
    }

    public static int parseMinutesFromTimeString(final String duration) {
        final String[] durationElem = duration.split(":");
        return (Integer.parseInt(durationElem[0]) * 60) + Integer.parseInt(durationElem[1]);
    }

    public static long timeDiff(final Date d1, final Date d2) {
        final long t1 = d1.getTime();
        final long t2 = d2.getTime();
        return t1 - t2;
    }

    public static Date addDays(final Date time, final int days) {
        final Calendar c = new GregorianCalendar();
        c.setTime(time);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    public static Date addMinutes(final Date time, final int minutes) {
        final Calendar c = new GregorianCalendar();
        c.setTime(time);
        c.add(Calendar.MINUTE, minutes);
        return c.getTime();
    }

    public static void printGroups(final Matcher m) {
        final int groupCount = m.groupCount();
        for (int i = 1; i <= groupCount; i++)
            System.out.println("group " + i + ":" + (m.group(i) != null ? "'" + m.group(i) + "'" : "null"));
    }

    public static void printXml(final CharSequence xml) {
        final Matcher m = Pattern.compile("(<.{80}.*?>)\\s*").matcher(xml);
        while (m.find())
            System.out.println(m.group(1));
    }

    public static void printPlain(final CharSequence plain) {
        final Matcher m = Pattern.compile("(.{1,80})").matcher(plain);
        while (m.find())
            System.out.println(m.group(1));
    }

    public static void printFromReader(final Reader reader) throws IOException {
        while (true) {
            final int c = reader.read();
            if (c == -1)
                return;

            System.out.print((char) c);
        }
    }

    public static String urlEncode(final String str, final Charset encoding) {
        try {
            return URLEncoder.encode(str, encoding.name());
        } catch (final UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    public static String urlDecode(final String str, final Charset encoding) {
        try {
            return URLDecoder.decode(str, encoding.name());
        } catch (final UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    public static String firstNotEmpty(final String... strings) {
        for (final String str : strings)
            if (str != null && str.length() > 0)
                return str;

        return null;
    }

    public static Currency getCurrency(final String code) {
        try {
            return Currency.getInstance(code);
        } catch (final IllegalArgumentException x) {
            throw new RuntimeException("unknown ISO 4217 code: " + code);
        }
    }
}
