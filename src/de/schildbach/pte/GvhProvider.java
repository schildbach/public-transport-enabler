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

package de.schildbach.pte;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.GVH;
	public static final String OLD_NETWORK_ID = "mobil.gvh.de";
	private static final String API_BASE = "http://mobil.efa.de/mobile3/";

	public GvhProvider(final String additionalQueryParameter)
	{
		super(API_BASE, additionalQueryParameter);
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

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// Hamburg
		LINES.put("SS1", new Style(Style.parseColor("#00933B"), Style.WHITE));
		LINES.put("SS11", new Style(Style.WHITE, Style.parseColor("#00933B"), Style.parseColor("#00933B")));
		LINES.put("SS2", new Style(Style.WHITE, Style.parseColor("#9D271A"), Style.parseColor("#9D271A")));
		LINES.put("SS21", new Style(Style.parseColor("#9D271A"), Style.WHITE));
		LINES.put("SS3", new Style(Style.parseColor("#411273"), Style.WHITE));
		LINES.put("SS31", new Style(Style.parseColor("#411273"), Style.WHITE));

		LINES.put("UU1", new Style(Style.parseColor("#044895"), Style.WHITE));
		LINES.put("UU2", new Style(Style.parseColor("#DC2B19"), Style.WHITE));
		LINES.put("UU3", new Style(Style.parseColor("#EE9D16"), Style.WHITE));
		LINES.put("UU4", new Style(Style.parseColor("#13A59D"), Style.WHITE));
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
