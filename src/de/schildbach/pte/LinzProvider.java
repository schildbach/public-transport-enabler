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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public class LinzProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "www.linzag.at";
	public static final String API_BASE = "http://www.linzag.at/linz/";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "XML_STOPFINDER_REQUEST"
			+ "?outputFormat=XML&coordOutputFormat=WGS84&name_sf=%s&type_sf=%s";
	private static final String AUTOCOMPLETE_TYPE = "any"; // any, stop, street, poi
	private static final Pattern P_AUTOCOMPLETE = Pattern.compile("" //
			+ "(?:" //
			+ "<itdOdvAssignedStop stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]* nameWithPlace=\"([^\"]*)\"" //
			+ "|" //
			+ "<odvNameElem [^>]* locality=\"([^\"]*)\"" //
			+ ")");
	private static final String ENCODING = "ISO-8859-1";

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING), AUTOCOMPLETE_TYPE);
		final CharSequence page = ParserUtils.scrape(uri);

		final List<Autocomplete> results = new ArrayList<Autocomplete>();

		final Matcher m = P_AUTOCOMPLETE.matcher(page);
		while (m.find())
		{
			if (m.group(1) != null)
			{
				final int sId = Integer.parseInt(m.group(1));
				// final double sLon = latLonToDouble(Integer.parseInt(mAutocomplete.group(2)));
				// final double sLat = latLonToDouble(Integer.parseInt(mAutocomplete.group(3)));
				final String sName = m.group(4).trim();
				results.add(new Autocomplete(sId, sName));
			}
			else if (m.group(5) != null)
			{
				final String sName = m.group(5).trim();
				results.add(new Autocomplete(0, sName));
			}
		}

		return results;
	}

	private static final String NEARBY_LATLON_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&mode=direct&coordOutputFormat=WGS84&mergeDep=1&useAllStops=1&name_dm=%2.6f:%2.6f:WGS84&type_dm=coord&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&excludedMeans=checkbox";
	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&mode=direct&coordOutputFormat=WGS84&mergeDep=1&useAllStops=1&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&excludedMeans=checkbox";
	private static final Pattern P_NEARBY = Pattern
			.compile("<itdOdvAssignedStop stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]* nameWithPlace=\"([^\"]*)\" distance=\"(\\d+)\"");

	public List<Station> nearbyStations(final String stationId, final double lat, final double lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri;
		if (lat != 0 || lon != 0)
			uri = String.format(NEARBY_LATLON_URI, lon, lat);
		else if (stationId != null)
			uri = String.format(NEARBY_STATION_URI, stationId);
		else
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		final CharSequence page = ParserUtils.scrape(uri);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher mNearby = P_NEARBY.matcher(page);
		while (mNearby.find())
		{
			final int sId = Integer.parseInt(mNearby.group(1));
			final double sLon = latLonToDouble(Integer.parseInt(mNearby.group(2)));
			final double sLat = latLonToDouble(Integer.parseInt(mNearby.group(3)));
			final String sName = mNearby.group(4).trim();
			final int sDist = Integer.parseInt(mNearby.group(5));

			final Station station = new Station(sId, sName, sLat, sLon, sDist, null, null);
			stations.add(station);
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	private static double latLonToDouble(int value)
	{
		return (double) value / 1000000;
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<itdOdv type=\"stop\" usage=\"dm\">(.*?)</itdOdv>.*?" // head
			+ "(?:<itdDepartureList>(.*?)</itdDepartureList>.*?)?" // departures
			+ "|" //
			+ "(Server-Wartung).*?" // messages
			+ ")" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<odvNameElem .*? stopID=\"(\\d+)\" [^>]*>" // locationId
			+ "([^<]*).*?" // location
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<itdDeparture (.*?)</itdDeparture>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "stopID=\"(\\d+)\" [^>]* area=\"\\d+\" platform=\"(\\d+)?\" platformName=\"\".*?" // locationId
			+ "<itdDate year=\"(\\d+)\" month=\"(\\d+)\" day=\"(\\d+)\" weekday=\"\\d+\"/>" // date
			+ "<itdTime hour=\"(\\d+)\" minute=\"(\\d+)\" ap=\"\"/>" // time
			+ ".*?" //
			+ "<itdServingLine [^>]* number=\"([^<]*)\" symbol=\"([^<]*)\" motType=\"(\\d+)\" " // line, symbol, type
			+ "realtime=\"(\\d+)\" " // realtime
			+ "direction=\"([^\"]*)\" destID=\"(\\d+)\"" // destination, destinationId
			+ ".*?" //			
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, QueryDeparturesResult.Status.SERVICE_DOWN);

			final String headerText = mHeadCoarse.group(1);
			final String departuresText = mHeadCoarse.group(2);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(headerText);
			if (mHeadFine.matches())
			{
				final int locationId = Integer.parseInt(mHeadFine.group(1));
				final String location = ParserUtils.resolveEntities(mHeadFine.group(2));
				final List<Departure> departures = new ArrayList<Departure>(8);

				if (departuresText != null)
				{
					final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(departuresText);
					while (mDepCoarse.find())
					{
						final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
						if (mDepFine.matches())
						{
							if (Integer.parseInt(mDepFine.group(1)) == locationId)
							{
								final String position = mDepFine.group(2) != null ? "Gl. " + mDepFine.group(2) : null;

								final Date departureDate = parseDate(mDepFine.group(3), mDepFine.group(4), mDepFine.group(5), mDepFine.group(6),
										mDepFine.group(7));

								final String line = parseLine(mDepFine.group(8), mDepFine.group(9), mDepFine.group(10));

								final boolean isRealtime = mDepFine.group(11).equals("1");

								final String destination = mDepFine.group(12);

								final int destinationId = Integer.parseInt(mDepFine.group(13));

								departures.add(new Departure(!isRealtime ? departureDate : null, isRealtime ? departureDate : null, line, LINES
										.get(line.charAt(0)), null, position, destinationId, destination, null));
							}
						}
						else
						{
							throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
						}
					}
				}

				return new QueryDeparturesResult(uri, locationId, location, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + headerText + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static Date parseDate(final String year, final String month, final String day, final String hour, final String minute)
	{
		final Calendar calendar = new GregorianCalendar();
		calendar.clear();
		calendar.set(Calendar.YEAR, Integer.parseInt(year));
		calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
		calendar.set(Calendar.HOUR, Integer.parseInt(hour));
		calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
		return calendar.getTime();
	}

	private String parseLine(final String number, final String symbol, final String mot)
	{
		if (!number.equals(symbol))
			throw new IllegalStateException("number " + number + ", symbol " + symbol);

		int t = Integer.parseInt(mot);

		if (t == 4)
			return 'T' + number;
		if (t == 5 || t == 6 || t == 7 || t == 10)
			return 'B' + number;
		if (t == 8)
			return 'C' + number;

		throw new IllegalStateException("cannot normalize type '" + mot + "' line '" + number + "'");
	}

	private static final Map<Character, int[]> LINES = new HashMap<Character, int[]>();

	static
	{
		LINES.put('I', new int[] { Color.WHITE, Color.RED, Color.RED });
		LINES.put('R', new int[] { Color.GRAY, Color.WHITE });
		LINES.put('S', new int[] { Color.parseColor("#006e34"), Color.WHITE });
		LINES.put('U', new int[] { Color.parseColor("#003090"), Color.WHITE });
		LINES.put('T', new int[] { Color.parseColor("#cc0000"), Color.WHITE });
		LINES.put('B', new int[] { Color.parseColor("#993399"), Color.WHITE });
		LINES.put('F', new int[] { Color.BLUE, Color.WHITE });
		LINES.put('?', new int[] { Color.DKGRAY, Color.WHITE });
	}

	public int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
