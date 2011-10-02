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
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.util.Color;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.MVV;
	public static final String OLD_NETWORK_ID = "efa.mvv-muenchen.de";
	private static final String API_BASE = "http://efa.mvv-muenchen.de/mobile/";

	public MvvProvider()
	{
		super(API_BASE, null, false);
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

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		LINES.put("SS1", new int[] { Color.parseColor("#00ccff"), Color.WHITE });
		LINES.put("SS2", new int[] { Color.parseColor("#66cc00"), Color.WHITE });
		LINES.put("SS3", new int[] { Color.parseColor("#880099"), Color.WHITE });
		LINES.put("SS4", new int[] { Color.parseColor("#ff0033"), Color.WHITE });
		LINES.put("SS6", new int[] { Color.parseColor("#00aa66"), Color.WHITE });
		LINES.put("SS7", new int[] { Color.parseColor("#993333"), Color.WHITE });
		LINES.put("SS8", new int[] { Color.BLACK, Color.parseColor("#ffcc00") });
		LINES.put("SS20", new int[] { Color.BLACK, Color.parseColor("#ffaaaa") });
		LINES.put("SS27", new int[] { Color.parseColor("#ffaaaa"), Color.WHITE });
		LINES.put("SA", new int[] { Color.parseColor("#231f20"), Color.WHITE });

		LINES.put("T12", new int[] { Color.parseColor("#883388"), Color.WHITE });
		LINES.put("T15", new int[] { Color.parseColor("#3366CC"), Color.WHITE });
		LINES.put("T16", new int[] { Color.parseColor("#CC8833"), Color.WHITE });
		LINES.put("T17", new int[] { Color.parseColor("#993333"), Color.WHITE });
		LINES.put("T18", new int[] { Color.parseColor("#66bb33"), Color.WHITE });
		LINES.put("T19", new int[] { Color.parseColor("#cc0000"), Color.WHITE });
		LINES.put("T20", new int[] { Color.parseColor("#00bbee"), Color.WHITE });
		LINES.put("T21", new int[] { Color.parseColor("#33aa99"), Color.WHITE });
		LINES.put("T23", new int[] { Color.parseColor("#fff000"), Color.WHITE });
		LINES.put("T25", new int[] { Color.parseColor("#ff9999"), Color.WHITE });
		LINES.put("T27", new int[] { Color.parseColor("#ff6600"), Color.WHITE });
		LINES.put("TN17", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN19", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN20", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN27", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });

		LINES.put("UU1", new int[] { Color.parseColor("#227700"), Color.WHITE });
		LINES.put("UU2", new int[] { Color.parseColor("#bb0000"), Color.WHITE });
		LINES.put("UU2E", new int[] { Color.parseColor("#bb0000"), Color.WHITE });
		LINES.put("UU3", new int[] { Color.parseColor("#ee8800"), Color.WHITE });
		LINES.put("UU4", new int[] { Color.parseColor("#00ccaa"), Color.WHITE });
		LINES.put("UU5", new int[] { Color.parseColor("#bb7700"), Color.WHITE });
		LINES.put("UU6", new int[] { Color.parseColor("#0000cc"), Color.WHITE });
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

	@Override
	public Point[] getArea()
	{
		return new Point[] { new Point(48.140377f, 11.560643f) };
	}
}
