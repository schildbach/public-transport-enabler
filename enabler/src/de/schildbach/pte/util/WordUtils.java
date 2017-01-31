/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schildbach.pte.util;

/**
 * <p>
 * Operations on Strings that contain words.
 * </p>
 * 
 * <p>
 * This class tries to handle <code>null</code> input gracefully. An exception will not be thrown for a
 * <code>null</code> input. Each method documents its behaviour in more detail.
 * </p>
 * 
 * @since 2.0
 * @version $Id$
 */
public class WordUtils {
    /**
     * <p>
     * <code>WordUtils</code> instances should NOT be constructed in standard programming. Instead, the class
     * should be used as <code>WordUtils.wrap("foo bar", 20);</code>.
     * </p>
     *
     * <p>
     * This constructor is public to permit tools that require a JavaBean instance to operate.
     * </p>
     */
    public WordUtils() {
        super();
    }

    // Empty checks, taken from StringUtils
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * </p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>
     * NOTE: This method changed in Lang version 2.0. It no longer trims the CharSequence. That functionality
     * is available in isBlank().
     * </p>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String capitalizeFirst(final String str) {
        if (str == null || str.length() <= 0)
            return str;
        if (str.length() == 1)
            return str.toUpperCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // stripped:
    // public static String wrap(final String str, final int wrapLength)
    // public static String wrap(final String str, int wrapLength, String newLineStr, final boolean
    // wrapLongWords)

    // Capitalizing
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Capitalizes all the whitespace separated words in a String. Only the first letter of each word is
     * changed. To convert the rest of each word to lowercase at the same time, use
     * {@link #capitalizeFully(String)}.
     * </p>
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>. Capitalization uses the Unicode title case, normally equivalent to upper case.
     * </p>
     *
     * <pre>
     * WordUtils.capitalize(null)        = null
     * WordUtils.capitalize("")          = ""
     * WordUtils.capitalize("i am FINE") = "I Am FINE"
     * </pre>
     * 
     * @param str
     *            the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     */
    public static String capitalize(final String str) {
        return capitalize(str, null);
    }

    /**
     * <p>
     * Capitalizes all the delimiter separated words in a String. Only the first letter of each word is
     * changed. To convert the rest of each word to lowercase at the same time, use
     * {@link #capitalizeFully(String, char[])}.
     * </p>
     *
     * <p>
     * The delimiters represent a set of characters understood to separate words. The first string character
     * and the first non-delimiter character after a delimiter will be capitalized.
     * </p>
     *
     * <p>
     * A <code>null</code> input String returns <code>null</code>. Capitalization uses the Unicode title case,
     * normally equivalent to upper case.
     * </p>
     *
     * <pre>
     * WordUtils.capitalize(null, *)            = null
     * WordUtils.capitalize("", *)              = ""
     * WordUtils.capitalize(*, new char[0])     = *
     * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
     * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
     * </pre>
     * 
     * @param str
     *            the String to capitalize, may be null
     * @param delimiters
     *            set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     * @since 2.1
     */
    public static String capitalize(final String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (isEmpty(str) || delimLen == 0) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts all the whitespace separated words in a String into capitalized words, that is each word is
     * made up of a titlecase character and then a series of lowercase characters.
     * </p>
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>. Capitalization uses the Unicode title case, normally equivalent to upper case.
     * </p>
     *
     * <pre>
     * WordUtils.capitalizeFully(null)        = null
     * WordUtils.capitalizeFully("")          = ""
     * WordUtils.capitalizeFully("i am FINE") = "I Am Fine"
     * </pre>
     * 
     * @param str
     *            the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     */
    public static String capitalizeFully(final String str) {
        return capitalizeFully(str, null);
    }

    /**
     * <p>
     * Converts all the delimiter separated words in a String into capitalized words, that is each word is
     * made up of a titlecase character and then a series of lowercase characters.
     * </p>
     *
     * <p>
     * The delimiters represent a set of characters understood to separate words. The first string character
     * and the first non-delimiter character after a delimiter will be capitalized.
     * </p>
     *
     * <p>
     * A <code>null</code> input String returns <code>null</code>. Capitalization uses the Unicode title case,
     * normally equivalent to upper case.
     * </p>
     *
     * <pre>
     * WordUtils.capitalizeFully(null, *)            = null
     * WordUtils.capitalizeFully("", *)              = ""
     * WordUtils.capitalizeFully(*, null)            = *
     * WordUtils.capitalizeFully(*, new char[0])     = *
     * WordUtils.capitalizeFully("i aM.fine", {'.'}) = "I am.Fine"
     * </pre>
     * 
     * @param str
     *            the String to capitalize, may be null
     * @param delimiters
     *            set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @since 2.1
     */
    public static String capitalizeFully(String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (isEmpty(str) || delimLen == 0) {
            return str;
        }
        str = str.toLowerCase();
        return capitalize(str, delimiters);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Uncapitalizes all the whitespace separated words in a String. Only the first letter of each word is
     * changed.
     * </p>
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>.
     * </p>
     *
     * <pre>
     * WordUtils.uncapitalize(null)        = null
     * WordUtils.uncapitalize("")          = ""
     * WordUtils.uncapitalize("I Am FINE") = "i am fINE"
     * </pre>
     * 
     * @param str
     *            the String to uncapitalize, may be null
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(final String str) {
        return uncapitalize(str, null);
    }

    /**
     * <p>
     * Uncapitalizes all the whitespace separated words in a String. Only the first letter of each word is
     * changed.
     * </p>
     *
     * <p>
     * The delimiters represent a set of characters understood to separate words. The first string character
     * and the first non-delimiter character after a delimiter will be uncapitalized.
     * </p>
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>.
     * </p>
     *
     * <pre>
     * WordUtils.uncapitalize(null, *)            = null
     * WordUtils.uncapitalize("", *)              = ""
     * WordUtils.uncapitalize(*, null)            = *
     * WordUtils.uncapitalize(*, new char[0])     = *
     * WordUtils.uncapitalize("I AM.FINE", {'.'}) = "i AM.fINE"
     * </pre>
     * 
     * @param str
     *            the String to uncapitalize, may be null
     * @param delimiters
     *            set of characters to determine uncapitalization, null means whitespace
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     * @since 2.1
     */
    public static String uncapitalize(final String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (isEmpty(str) || delimLen == 0) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean uncapitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                buffer[i] = Character.toLowerCase(ch);
                uncapitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Swaps the case of a String using a word based algorithm.
     * </p>
     * 
     * <ul>
     * <li>Upper case character converts to Lower case</li>
     * <li>Title case character converts to Lower case</li>
     * <li>Lower case character after Whitespace or at start converts to Title case</li>
     * <li>Other Lower case character converts to Upper case</li>
     * </ul>
     * 
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>.
     * </p>
     * 
     * <pre>
     * StringUtils.swapCase(null)                 = null
     * StringUtils.swapCase("")                   = ""
     * StringUtils.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     * 
     * @param str
     *            the String to swap case, may be null
     * @return the changed String, <code>null</code> if null String input
     */
    public static String swapCase(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final char[] buffer = str.toCharArray();

        boolean whitespace = true;

        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isUpperCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
                whitespace = false;
            } else if (Character.isTitleCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
                whitespace = false;
            } else if (Character.isLowerCase(ch)) {
                if (whitespace) {
                    buffer[i] = Character.toTitleCase(ch);
                    whitespace = false;
                } else {
                    buffer[i] = Character.toUpperCase(ch);
                }
            } else {
                whitespace = Character.isWhitespace(ch);
            }
        }
        return new String(buffer);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Extracts the initial letters from each word in the String.
     * </p>
     * 
     * <p>
     * The first letter of the string and all first letters after whitespace are returned as a new string.
     * Their case is not changed.
     * </p>
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}. A <code>null</code> input String returns
     * <code>null</code>.
     * </p>
     *
     * <pre>
     * WordUtils.initials(null)             = null
     * WordUtils.initials("")               = ""
     * WordUtils.initials("Ben John Lee")   = "BJL"
     * WordUtils.initials("Ben J.Lee")      = "BJ"
     * </pre>
     *
     * @param str
     *            the String to get initials from, may be null
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String,char[])
     * @since 2.2
     */
    public static String initials(final String str) {
        return initials(str, null);
    }

    /**
     * <p>
     * Extracts the initial letters from each word in the String.
     * </p>
     * 
     * <p>
     * The first letter of the string and all first letters after the defined delimiters are returned as a new
     * string. Their case is not changed.
     * </p>
     *
     * <p>
     * If the delimiters array is null, then Whitespace is used. Whitespace is defined by
     * {@link Character#isWhitespace(char)}. A <code>null</code> input String returns <code>null</code>. An
     * empty delimiter array returns an empty String.
     * </p>
     *
     * <pre>
     * WordUtils.initials(null, *)                = null
     * WordUtils.initials("", *)                  = ""
     * WordUtils.initials("Ben John Lee", null)   = "BJL"
     * WordUtils.initials("Ben J.Lee", null)      = "BJ"
     * WordUtils.initials("Ben J.Lee", [' ','.']) = "BJL"
     * WordUtils.initials(*, new char[0])         = ""
     * </pre>
     * 
     * @param str
     *            the String to get initials from, may be null
     * @param delimiters
     *            set of characters to determine words, null means whitespace
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String)
     * @since 2.2
     */
    public static String initials(final String str, final char... delimiters) {
        if (isEmpty(str)) {
            return str;
        }
        if (delimiters != null && delimiters.length == 0) {
            return "";
        }
        final int strLen = str.length();
        final char[] buf = new char[strLen / 2 + 1];
        int count = 0;
        boolean lastWasGap = true;
        for (int i = 0; i < strLen; i++) {
            final char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                lastWasGap = true;
            } else if (lastWasGap) {
                buf[count++] = ch;
                lastWasGap = false;
            } else {
                continue; // ignore ch
            }
        }
        return new String(buf, 0, count);
    }

    // -----------------------------------------------------------------------
    /**
     * Is the character a delimiter.
     *
     * @param ch
     *            the character to check
     * @param delimiters
     *            the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(final char ch, final char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (final char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }
}
