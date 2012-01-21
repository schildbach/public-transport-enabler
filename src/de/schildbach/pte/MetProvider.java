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
import java.util.TimeZone;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class MetProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.MET;
	private final static String API_BASE = "http://jp.metlinkmelbourne.com.au/metlink/";

	public MetProvider()
	{
		super(API_BASE, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("Australia/Melbourne");
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return jsonStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private static final Map<Character, Style> LINES = new HashMap<Character, Style>();

	static
	{
		LINES.put('R', new Style(Style.parseColor("#a24ba3"), Style.WHITE));
		LINES.put('S', new Style(Style.parseColor("#3a75c4"), Style.WHITE));
		LINES.put('T', new Style(Style.parseColor("#5bbf21"), Style.WHITE));
		LINES.put('B', new Style(Style.parseColor("#f77f00"), Style.WHITE));
	}

	@Override
	public Style lineStyle(final String line)
	{
		// TODO NightRider buses (buses with numbers > 940): #f26522

		final Style style = LINES.get(line.charAt(0));
		if (style != null)
			return style;
		else
			return super.lineStyle(line);
	}
}
