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
public class SfProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SF;
	private final static String API_BASE = "http://tripplanner.transit.511.org/mtc/";

	public SfProvider()
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
		return TimeZone.getTimeZone("America/Los_Angeles");
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

	@Override
	protected String normalizeLocationName(final String name)
	{
		if (name == null || name.length() == 0)
			return null;

		return super.normalizeLocationName(name).replace("$XINT$", "&");
	}

	@Override
	protected String parseLine(final String mot, final String name, final String longName, final String noTrainName)
	{
		if ("NORTHBOUND".equals(name))
			return "?" + name;
		else if ("SOUTHBOUND".equals(name))
			return "?" + name;
		else if ("EASTBOUND".equals(name))
			return "?" + name;
		else if ("WESTBOUND".equals(name))
			return "?" + name;
		else
			return super.parseLine(mot, name, longName, noTrainName);
	}

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		// BART
		LINES.put("RDaly City / Dublin Pleasanton", new int[] { Color.parseColor("#00AEEF"), Color.WHITE });
		LINES.put("RDulin Pleasanton / Daly City", new int[] { Color.parseColor("#00AEEF"), Color.WHITE });

		LINES.put("RSFO / Pittsburg Bay Point", new int[] { Color.parseColor("#FFE800"), Color.BLACK });
		LINES.put("RPittsburg Bay Point / SFO", new int[] { Color.parseColor("#FFE800"), Color.BLACK });

		LINES.put("RDaly City / Fremont", new int[] { Color.parseColor("#4EBF49"), Color.WHITE });
		LINES.put("RFremont / Daly City", new int[] { Color.parseColor("#4EBF49"), Color.WHITE });

		LINES.put("RFremont / Richmond", new int[] { Color.parseColor("#FAA61A"), Color.WHITE });
		LINES.put("RRichmond / Fremont", new int[] { Color.parseColor("#FAA61A"), Color.WHITE });

		LINES.put("RMillbrae / Richmond", new int[] { Color.parseColor("#F81A23"), Color.WHITE });
		LINES.put("RRichmond / Millbrae", new int[] { Color.parseColor("#F81A23"), Color.WHITE });
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
