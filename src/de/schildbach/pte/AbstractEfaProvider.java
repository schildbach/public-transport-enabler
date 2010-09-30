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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.QueryDeparturesResult.Status;
import de.schildbach.pte.util.XmlPullUtil;

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

	protected abstract String parseLine(String number, String symbol, String mot);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		try
		{
			final CharSequence page = ParserUtils.scrape(uri);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(new StringReader(page.toString()));

			XmlPullUtil.jumpToStartTag(pp, null, "itdOdvName");
			final String nameState = pp.getAttributeValue(null, "state");
			if (nameState.equals("identified"))
			{
				XmlPullUtil.jumpToStartTag(pp, null, "odvNameElem");
				final int locationId = Integer.parseInt(pp.getAttributeValue(null, "stopID"));

				final String location = pp.nextText();

				final Calendar departureTime = new GregorianCalendar();
				final List<Departure> departures = new ArrayList<Departure>(8);

				XmlPullUtil.jumpToStartTag(pp, null, "itdDepartureList");
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDeparture"))
				{
					if (Integer.parseInt(pp.getAttributeValue(null, "stopID")) == locationId)
					{
						String position = pp.getAttributeValue(null, "platform");
						if (position != null)
							position = "Gl. " + position;

						departureTime.clear();

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDateTime"))
							throw new IllegalStateException("itdDateTime not found:" + pp.getPositionDescription());

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDate"))
							throw new IllegalStateException("itdDate not found:" + pp.getPositionDescription());
						processItdDate(pp, departureTime);
						XmlPullUtil.skipRestOfTree(pp);

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdTime"))
							throw new IllegalStateException("itdTime not found:" + pp.getPositionDescription());
						processItdTime(pp, departureTime);
						XmlPullUtil.skipRestOfTree(pp);

						XmlPullUtil.skipRestOfTree(pp);

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdServingLine"))
							throw new IllegalStateException("itdServingLine not found:" + pp.getPositionDescription());
						final String line = parseLine(pp.getAttributeValue(null, "number"), pp.getAttributeValue(null, "symbol"), pp
								.getAttributeValue(null, "motType"));
						final boolean isRealtime = pp.getAttributeValue(null, "realtime").equals("1");
						final String destination = pp.getAttributeValue(null, "direction");
						final int destinationId = Integer.parseInt(pp.getAttributeValue(null, "destID"));
						XmlPullUtil.skipRestOfTree(pp);

						departures.add(new Departure(!isRealtime ? departureTime.getTime() : null, isRealtime ? departureTime.getTime() : null, line,
								lineColors(line), null, position, destinationId, destination, null));
					}

					XmlPullUtil.skipRestOfTree(pp);
				}

				return new QueryDeparturesResult(uri, locationId, location, departures);
			}
			else if (nameState.equals("notidentified"))
			{
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			}
			else
			{
				throw new RuntimeException("unknown nameState '" + nameState + "' on " + uri);
			}
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
	}

	private void processItdDate(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, "itdDate");
		calendar.set(Calendar.YEAR, Integer.parseInt(pp.getAttributeValue(null, "year")));
		calendar.set(Calendar.MONTH, Integer.parseInt(pp.getAttributeValue(null, "month")) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(pp.getAttributeValue(null, "day")));
	}

	private void processItdTime(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, "itdTime");
		calendar.set(Calendar.HOUR, Integer.parseInt(pp.getAttributeValue(null, "hour")));
		calendar.set(Calendar.MINUTE, Integer.parseInt(pp.getAttributeValue(null, "minute")));
	}
}
