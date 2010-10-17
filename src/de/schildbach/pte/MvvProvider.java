/*
 * Copyright 2010 the original author or authors.
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "efa.mvv-muenchen.de";
	private static final String API_BASE = "http://efa.mvv-muenchen.de/mobile/";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		// TODO remove
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS)
				return false;

		return true;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "XSLT_TRIP_REQUEST2?outputFormat=XML&coordOutputFormat=WGS84&locationServerActive=1&type_origin=any&name_origin=%s";

	@Override
	protected String autocompleteUri(final CharSequence constraint)
	{
		return String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), "ISO-8859-1"));
	}

	private static final String NEARBY_LATLON_URI = "http://efa.mvv-muenchen.de/ultralite/XML_DM_REQUEST"
			+ "?coordOutputFormat=WGS84&type_dm=coord&name_dm=%2.6f:%2.6f:WGS84&mode=direct";

	@Override
	protected String nearbyLatLonUri(final int lat, final int lon)
	{
		return String.format(NEARBY_LATLON_URI, latLonToDouble(lon), latLonToDouble(lat));
	}

	private static final String NEARBY_STATION_URI = "http://efa.mvv-muenchen.de/ultralite/XML_DM_REQUEST"
			+ "?coordOutputFormat=WGS84&type_dm=stop&name_dm=%s&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	private static final Pattern P_NEARBY_COARSE = Pattern.compile("<dp>(.*?)</dp>");
	private static final Pattern P_NEARBY_FINE = Pattern.compile(".*?<n>(.*?)</n>.*?<r>.*?<id>(.*?)</id>.*?</r>.*?(?:<c>(\\d+),(\\d+)</c>.*?)?");

	@Override
	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri;
		if (lat != 0 || lon != 0)
			uri = nearbyLatLonUri(lat, lon);
		else if (stationId != null)
			uri = nearbyStationUri(stationId);
		else
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		final CharSequence page = ParserUtils.scrape(uri);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher mNearbyCoarse = P_NEARBY_COARSE.matcher(page);
		while (mNearbyCoarse.find())
		{
			final Matcher mNearbyFine = P_NEARBY_FINE.matcher(mNearbyCoarse.group(1));
			if (mNearbyFine.matches())
			{
				final String sName = mNearbyFine.group(1).trim();
				final int sId = Integer.parseInt(mNearbyFine.group(2));
				final int sLon = mNearbyFine.group(3) != null ? Integer.parseInt(mNearbyFine.group(3)) : 0;
				final int sLat = mNearbyFine.group(4) != null ? Integer.parseInt(mNearbyFine.group(4)) : 0;

				final Station station = new Station(sId, sName, sLat, sLon, 0, null, null);
				stations.add(station);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mNearbyCoarse.group(1) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return new NearbyStationsResult(uri, stations);
		else
			return new NearbyStationsResult(uri, stations.subList(0, maxStations));
	}

	@Override
	protected String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		uri.append("&useRealtime=1");
		return uri.toString();
	}

	@Override
	protected String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmm");

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");

		uri.append("?language=de");
		appendCommonConnectionParams(uri);

		appendLocation(uri, from, "origin");
		appendLocation(uri, to, "destination");
		if (via != null)
			appendLocation(uri, via, "via");

		uri.append("&itdDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&itdTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&itdTripDateTimeDepArr=").append(dep ? "dep" : "arr");

		uri.append("&ptOptionsActive=1");
		uri.append("&changeSpeed=").append(WALKSPEED_MAP.get(walkSpeed));
		uri.append(productParams(products));

		uri.append("&locationServerActive=1");
		uri.append("&useRealtime=1");

		return uri.toString();
	}

	@Override
	protected String commandLink(final String sessionId, final String command)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");
		uri.append("?sessionID=").append(sessionId);
		appendCommonConnectionParams(uri);
		uri.append("&command=").append(command);
		return uri.toString();
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
}
