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
import de.schildbach.pte.util.Color;

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

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&coordOutputFormat=WGS84&type_dm=stop&name_dm=%s&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final int stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private static final Map<Character, int[]> LINES = new HashMap<Character, int[]>();

	static
	{
		LINES.put('R', new int[] { Color.parseColor("#a24ba3"), Color.WHITE });
		LINES.put('S', new int[] { Color.parseColor("#3a75c4"), Color.WHITE });
		LINES.put('T', new int[] { Color.parseColor("#5bbf21"), Color.WHITE });
		LINES.put('B', new int[] { Color.parseColor("#f77f00"), Color.WHITE });
	}

	@Override
	public int[] lineColors(final String line)
	{
		// TODO NightRider buses (buses with numbers > 940): #f26522

		final int[] lineColors = LINES.get(line.charAt(0));
		if (lineColors != null)
			return lineColors;
		else
			return super.lineColors(line);
	}
}
