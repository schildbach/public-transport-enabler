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
public class TflProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.TFL;
	public static final String OLD_NETWORK_ID = "journeyplanner.tfl.gov.uk";
	private static final String API_BASE = "http://journeyplanner.tfl.gov.uk/user/";

	public TflProvider()
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
		return TimeZone.getTimeZone("Europe/London");
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
		// London
		LINES.put("UBakerloo", new int[] { Color.parseColor("#9D5324"), Color.WHITE });
		LINES.put("UCentral", new int[] { Color.parseColor("#D52B1E"), Color.WHITE });
		LINES.put("UCircle", new int[] { Color.parseColor("#FECB00"), Color.BLACK });
		LINES.put("UDistrict", new int[] { Color.parseColor("#007934"), Color.WHITE });
		LINES.put("UEast London", new int[] { Color.parseColor("#FFA100"), Color.WHITE });
		LINES.put("UHammersmith & City", new int[] { Color.parseColor("#C5858F"), Color.BLACK });
		LINES.put("UJubilee", new int[] { Color.parseColor("#818A8F"), Color.WHITE });
		LINES.put("UMetropolitan", new int[] { Color.parseColor("#850057"), Color.WHITE });
		LINES.put("UNorthern", new int[] { Color.BLACK, Color.WHITE });
		LINES.put("UPicadilly", new int[] { Color.parseColor("#0018A8"), Color.WHITE });
		LINES.put("UVictoria", new int[] { Color.parseColor("#00A1DE"), Color.WHITE });
		LINES.put("UWaterloo & City", new int[] { Color.parseColor("#76D2B6"), Color.BLACK });

		LINES.put("SDLR", new int[] { Color.parseColor("#00B2A9"), Color.WHITE });
		LINES.put("SLO", new int[] { Color.parseColor("#f46f1a"), Color.WHITE });

		LINES.put("TTramlink 1", new int[] { Color.rgb(193, 215, 46), Color.WHITE });
		LINES.put("TTramlink 2", new int[] { Color.rgb(193, 215, 46), Color.WHITE });
		LINES.put("TTramlink 3", new int[] { Color.rgb(124, 194, 66), Color.BLACK });
	}

	@Override
	public int[] lineColors(final String line)
	{
		final int[] lineColors = LINES.get(line);
		if (lineColors != null)
			return lineColors;
		if (line.startsWith("SLO"))
			return LINES.get("SLO");
		return super.lineColors(line);
	}
}
