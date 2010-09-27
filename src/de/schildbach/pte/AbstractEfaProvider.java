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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractEfaProvider implements NetworkProvider
{
	private static final Pattern P_AUTOCOMPLETE = Pattern.compile("" //
			+ "(?:" //
			+ "<itdOdvAssignedStop stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]* nameWithPlace=\"([^\"]*)\"" //
			+ "|" //
			+ "<odvNameElem [^>]* locality=\"([^\"]*)\"" //
			+ ")");

	protected abstract String autocompleteUri(final CharSequence constraint);

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(autocompleteUri(constraint));

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
				results.add(new Autocomplete(LocationType.STATION, sId, sName));
			}
			else if (m.group(5) != null)
			{
				final String sName = m.group(5).trim();
				results.add(new Autocomplete(LocationType.ANY, 0, sName));
			}
		}

		return results;
	}

	private static final Pattern P_NEARBY = Pattern.compile("<itdOdvAssignedStop " // 
			+ "(?:stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]*? nameWithPlace=\"([^\"]*)\" distance=\"(\\d+)\"" //
			+ "|distance=\"(\\d+)\" [^>]*? nameWithPlace=\"([^\"]*)\" [^>]*? stopID=\"(\\d+)\" [^>]*? x=\"(\\d+)\" y=\"(\\d+)\"" //
			+ ")");

	protected abstract String nearbyLatLonUri(double lat, double lon);

	protected abstract String nearbyStationUri(String stationId);

	public List<Station> nearbyStations(final String stationId, final double lat, final double lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri = null;
		if (lat != 0 || lon != 0)
			uri = nearbyLatLonUri(lat, lon);
		if (uri == null && stationId != null)
			uri = nearbyStationUri(stationId);
		if (uri == null)
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		final CharSequence page = ParserUtils.scrape(uri);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher mNearby = P_NEARBY.matcher(page);
		while (mNearby.find())
		{
			final boolean firstSyntax = mNearby.group(1) != null;
			final int sId = Integer.parseInt(mNearby.group(firstSyntax ? 1 : 8));
			final double sLon = latLonToDouble(Integer.parseInt(mNearby.group(firstSyntax ? 2 : 9)));
			final double sLat = latLonToDouble(Integer.parseInt(mNearby.group(firstSyntax ? 3 : 10)));
			final String sName = mNearby.group(firstSyntax ? 4 : 7).trim();
			final int sDist = Integer.parseInt(mNearby.group(firstSyntax ? 5 : 6));

			final Station station = new Station(sId, sName, sLat, sLon, sDist, null, null);
			stations.add(station);
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	private static double latLonToDouble(final int value)
	{
		return (double) value / 1000000;
	}
}
