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

package de.schildbach.pte.dto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

/**
 * @author Andreas Schildbach
 */
public class Style implements Serializable {
    private static final long serialVersionUID = 7145603493425043304L;

    public final Shape shape;
    public final int backgroundColor, backgroundColor2;
    public final int foregroundColor;
    public final int borderColor;

    public enum Shape {
        RECT, ROUNDED, CIRCLE
    }

    public Style(final int backgroundColor, final int foregroundColor) {
        this.shape = Shape.ROUNDED;
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = 0;
        this.foregroundColor = foregroundColor;
        this.borderColor = 0;
    }

    public Style(final Shape shape, final int backgroundColor, final int foregroundColor) {
        this.shape = checkNotNull(shape);
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = 0;
        this.foregroundColor = foregroundColor;
        this.borderColor = 0;
    }

    public Style(final Shape shape, final int backgroundColor, final int foregroundColor, final int borderColor) {
        this.shape = checkNotNull(shape);
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = 0;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
    }

    public Style(final Shape shape, final int backgroundColor, final int backgroundColor2, final int foregroundColor,
            final int borderColor) {
        this.shape = checkNotNull(shape);
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = backgroundColor2;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
    }

    public Style(final int backgroundColor, final int foregroundColor, final int borderColor) {
        this.shape = Shape.ROUNDED;
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = 0;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
    }

    public Style(final int backgroundColor, final int backgroundColor2, final int foregroundColor,
            final int borderColor) {
        this.shape = Shape.ROUNDED;
        this.backgroundColor = backgroundColor;
        this.backgroundColor2 = backgroundColor2;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
    }

    public final boolean hasBorder() {
        return borderColor != 0;
    }

    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int parseColor(final String colorStr) {
        checkNotNull(colorStr);
        checkArgument((colorStr.length() == 7 || colorStr.length() == 9) && colorStr.charAt(0) == '#',
                "Unknown color: %s", colorStr);
        try {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorStr.substring(1), 16);
            if (colorStr.length() == 7)
                color |= 0xff000000; // Amend the alpha value
            return (int) color;
        } catch (final NumberFormatException x) {
            throw new IllegalArgumentException("Not a number: " + colorStr);
        }
    }

    public static int argb(final int alpha, final int red, final int green, final int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int rgb(final int red, final int green, final int blue) {
        return argb(0xFF, red, green, blue);
    }

    public static int red(final int color) {
        return (color >> 16) & 0xff;
    }

    public static int green(final int color) {
        return (color >> 8) & 0xff;
    }

    public static int blue(final int color) {
        return color & 0xff;
    }

    public static float perceivedBrightness(final int color) {
        // formula for perceived brightness computation: http://www.w3.org/TR/AERT#color-contrast
        return (0.299f * Style.red(color) + 0.587f * Style.green(color) + 0.114f * Style.blue(color)) / 256;
    }

    public static int deriveForegroundColor(final int backgroundColor) {
        // dark colors, white font. Or light colors, black font
        if (perceivedBrightness(backgroundColor) < 0.5)
            return WHITE;
        else
            return BLACK;
    }
}
