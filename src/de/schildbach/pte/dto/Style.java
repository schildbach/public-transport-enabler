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

package de.schildbach.pte.dto;

import java.io.Serializable;

/**
 * @author Andreas Schildbach
 */
public class Style implements Serializable
{
	public final int backgroundColor;
	public final int foregroundColor;
	public final int borderColor;

	public Style(final int backgroundColor, final int foregroundColor)
	{
		this.backgroundColor = backgroundColor;
		this.foregroundColor = foregroundColor;
		this.borderColor = 0;
	}

	public Style(final int backgroundColor, final int foregroundColor, final int borderColor)
	{
		this.backgroundColor = backgroundColor;
		this.foregroundColor = foregroundColor;
		this.borderColor = borderColor;
	}

	public final boolean hasBorder()
	{
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

	public static int parseColor(final String colorString)
	{
		if (colorString.charAt(0) == '#')
		{
			// Use a long to avoid rollovers on #ffXXXXXX
			long color = Long.parseLong(colorString.substring(1), 16);
			if (colorString.length() == 7)
			{
				// Set the alpha value
				color |= 0x00000000ff000000;
			}
			else if (colorString.length() != 9)
			{
				throw new IllegalArgumentException("Unknown color");
			}
			return (int) color;
		}
		throw new IllegalArgumentException("Unknown color");
	}

	public static int rgb(final int red, final int green, final int blue)
	{
		return (0xFF << 24) | (red << 16) | (green << 8) | blue;
	}
}
