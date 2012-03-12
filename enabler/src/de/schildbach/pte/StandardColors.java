/*
 * Copyright 2010-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class StandardColors
{
	public static final Map<Character, Style> LINES = new HashMap<Character, Style>();

	static
	{
		LINES.put('I', new Style(Shape.RECT, Style.WHITE, Style.RED, Style.RED));
		LINES.put('R', new Style(Shape.RECT, Style.GRAY, Style.WHITE));
		LINES.put('S', new Style(Shape.CIRCLE, Style.parseColor("#006e34"), Style.WHITE));
		LINES.put('U', new Style(Shape.RECT, Style.parseColor("#003090"), Style.WHITE));
		LINES.put('T', new Style(Shape.RECT, Style.parseColor("#cc0000"), Style.WHITE));
		LINES.put('B', new Style(Style.parseColor("#993399"), Style.WHITE));
		LINES.put('F', new Style(Shape.CIRCLE, Style.BLUE, Style.WHITE));
		LINES.put('?', new Style(Style.DKGRAY, Style.WHITE));
	}
}
