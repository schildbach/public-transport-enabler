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
public class VorProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VOR;
	private final static String API_BASE = "http://efa.vor.at/wvb/";

	public VorProvider()
	{
		super(API_BASE, null);

		setReferer("http://www.wienerlinien.at/eportal/");
		setIncludeRegionId(false);
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
		// Wien
		LINES.put("UU1", new Style(Style.Shape.RECT, Style.parseColor("#c6292a"), Style.WHITE));
		LINES.put("UU2", new Style(Style.Shape.RECT, Style.parseColor("#a82783"), Style.WHITE));
		LINES.put("UU3", new Style(Style.Shape.RECT, Style.parseColor("#f39315"), Style.WHITE));
		LINES.put("UU4", new Style(Style.Shape.RECT, Style.parseColor("#23a740"), Style.WHITE));
		LINES.put("UU6", new Style(Style.Shape.RECT, Style.parseColor("#be762c"), Style.WHITE));
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
