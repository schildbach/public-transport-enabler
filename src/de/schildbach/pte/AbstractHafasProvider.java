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
public abstract class AbstractHafasProvider implements NetworkProvider
{
	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE = Pattern.compile(".*?&REQMapRoute0\\.Location0\\.X=(-?\\d+)&REQMapRoute0\\.Location0\\.Y=(-?\\d+)"
			+ "&.*?[\\?&]input=(\\d+)&[^\"]*\">([^<]*)<.*?", Pattern.DOTALL);

	protected abstract String nearbyStationUri(String stationId);

	public List<Station> nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		if (stationId == null)
			throw new IllegalArgumentException("stationId must be given");

		final List<Station> stations = new ArrayList<Station>();

		final String uri = nearbyStationUri(stationId);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mCoarse = P_NEARBY_COARSE.matcher(page);
		while (mCoarse.find())
		{
			final Matcher mFine = P_NEARBY_FINE.matcher(mCoarse.group(2));
			if (mFine.matches())
			{
				final int parsedLon = Integer.parseInt(mFine.group(1));
				final int parsedLat = Integer.parseInt(mFine.group(2));
				final int parsedId = Integer.parseInt(mFine.group(3));
				final String parsedName = ParserUtils.resolveEntities(mFine.group(4));

				stations.add(new Station(parsedId, parsedName, parsedLat, parsedLon, 0, null, null));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}
}
