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
import de.schildbach.pte.util.Color;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.GVH;
	public static final String OLD_NETWORK_ID = "mobil.gvh.de";
	private static final String API_BASE = "http://mobil.gvh.de/mobile3/";

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

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&type_dm=stop&name_dm=%s&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

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

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		// Hamburg
		LINES.put("SS1", new int[] { Color.parseColor("#00933B"), Color.WHITE });
		LINES.put("SS11", new int[] { Color.WHITE, Color.parseColor("#00933B"), Color.parseColor("#00933B") });
		LINES.put("SS2", new int[] { Color.WHITE, Color.parseColor("#9D271A"), Color.parseColor("#9D271A") });
		LINES.put("SS21", new int[] { Color.parseColor("#9D271A"), Color.WHITE });
		LINES.put("SS3", new int[] { Color.parseColor("#411273"), Color.WHITE });
		LINES.put("SS31", new int[] { Color.parseColor("#411273"), Color.WHITE });

		LINES.put("UU1", new int[] { Color.parseColor("#044895"), Color.WHITE });
		LINES.put("UU2", new int[] { Color.parseColor("#DC2B19"), Color.WHITE });
		LINES.put("UU3", new int[] { Color.parseColor("#EE9D16"), Color.WHITE });
		LINES.put("UU4", new int[] { Color.parseColor("#13A59D"), Color.WHITE });
	}

	@Override
	public int[] lineColors(final String line)
	{
		final int[] lineColors = LINES.get(line);
		if (lineColors != null)
			return lineColors;
		else
			return super.lineColors(line);
	}
}
