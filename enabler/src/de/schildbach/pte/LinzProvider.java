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

/**
 * @author Andreas Schildbach
 */
public class LinzProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.LINZ;
	public static final String API_BASE = "http://www.linzag.at/linz2/"; // open data: http://www.linzag.at/static/

	public LinzProvider()
	{
		super(API_BASE, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		LINES.put("B11", new Style(Style.Shape.RECT, Style.parseColor("#f27b02"), Style.WHITE));
		LINES.put("B12", new Style(Style.Shape.RECT, Style.parseColor("#00863a"), Style.WHITE));
		LINES.put("B17", new Style(Style.Shape.RECT, Style.parseColor("#f47a00"), Style.WHITE));
		LINES.put("B18", new Style(Style.Shape.RECT, Style.parseColor("#0066b5"), Style.WHITE));
		LINES.put("B19", new Style(Style.Shape.RECT, Style.parseColor("#f36aa8"), Style.WHITE));
		LINES.put("B25", new Style(Style.Shape.RECT, Style.parseColor("#d29f08"), Style.WHITE));
		LINES.put("B26", new Style(Style.Shape.RECT, Style.parseColor("#0070b6"), Style.WHITE));
		LINES.put("B27", new Style(Style.Shape.RECT, Style.parseColor("#96c41c"), Style.WHITE));
		LINES.put("B33", new Style(Style.Shape.RECT, Style.parseColor("#6d1f82"), Style.WHITE));
		LINES.put("B38", new Style(Style.Shape.RECT, Style.parseColor("#ef7b02"), Style.WHITE));
		LINES.put("B43", new Style(Style.Shape.RECT, Style.parseColor("#00ace3"), Style.WHITE));
		LINES.put("B45", new Style(Style.Shape.RECT, Style.parseColor("#db0c10"), Style.WHITE));
		LINES.put("B46", new Style(Style.Shape.RECT, Style.parseColor("#00acea"), Style.WHITE));
		LINES.put("B101", new Style(Style.Shape.RECT, Style.parseColor("#fdba00"), Style.WHITE));
		LINES.put("B102", new Style(Style.Shape.RECT, Style.parseColor("#9d701f"), Style.WHITE));
		LINES.put("B103", new Style(Style.Shape.RECT, Style.parseColor("#019793"), Style.WHITE));
		LINES.put("B104", new Style(Style.Shape.RECT, Style.parseColor("#699c23"), Style.WHITE));
		LINES.put("B105", new Style(Style.Shape.RECT, Style.parseColor("#004b9e"), Style.WHITE));
		LINES.put("B191", new Style(Style.Shape.RECT, Style.parseColor("#1293a8"), Style.WHITE));
		LINES.put("B192", new Style(Style.Shape.RECT, Style.parseColor("#947ab7"), Style.WHITE));
		LINES.put("BN2", new Style(Style.Shape.RECT, Style.parseColor("#005aac"), Style.WHITE)); // night
		LINES.put("BN3", new Style(Style.Shape.RECT, Style.parseColor("#b80178"), Style.WHITE)); // night
		LINES.put("BN4", new Style(Style.Shape.RECT, Style.parseColor("#93be01"), Style.WHITE)); // night

		LINES.put("T1", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));
		LINES.put("TN1", new Style(Style.Shape.RECT, Style.parseColor("#db0e16"), Style.WHITE)); // night
		LINES.put("T2", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));
		LINES.put("T3", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));

		LINES.put("C50", new Style(Style.Shape.RECT, Style.parseColor("#4eae2c"), Style.WHITE)); // Pöstlingbergbahn
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		else
			return super.lineStyle(line);
	}
}
