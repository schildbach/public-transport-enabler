/*
 * Copyright 2012-2015 the original author or authors.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public class Iso8601Format extends SimpleDateFormat {
    private Iso8601Format(final String formatString) {
        super(formatString, Locale.US);
    }

    public static DateFormat newTimeFormat() {
        return new Iso8601Format("HH:mm:ss");
    }

    public static DateFormat newDateFormat() {
        return new Iso8601Format("yyyy-MM-dd");
    }

    public static DateFormat newDateTimeFormat() {
        return new Iso8601Format("yyyy-MM-dd HH:mm:ss");
    }

    public static String formatDateTime(final Date date) {
        return newDateTimeFormat().format(date);
    }

    public static Date parseDateTime(final String date) throws ParseException {
        return newDateTimeFormat().parse(date);
    }
}
