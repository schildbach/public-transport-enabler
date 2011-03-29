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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class NsProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NS;
	public static final String OLD_NETWORK_ID = "hafas.bene-system.com";
	private static final String API_URI = "http://hafas.bene-system.com/bin/extxml.exe";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public NsProvider()
	{
		super(API_URI, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	private final String NEARBY_URI = "http://hari.b-rail.be/HAFAS/bin/stboard.exe/en?input=%s&distance=50&near=Anzeigen";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, ParserUtils.urlEncode(stationId));
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://hari.b-rail.be/hari3/webserver1/bin/stboard.exe/dox");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50); // maximum taken from SNCB site
		uri.append("&productsFilter=1:1111111111111111");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<div id=\"hfs_content\">\r\n" //
			+ "<p class=\"qs\">\r\n(.*?)\r\n</p>\r\n" // head
			+ "(.*?)\r\n</div>" // departures
			+ "|(Eingabe kann nicht interpretiert)|(Verbindung zum Server konnte leider nicht hergestellt werden))" //
			+ ".*?", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" //
			+ "<strong>(.*?)</strong><br />\r\n" // location
			+ "Abfahrt (\\d{1,2}:\\d{2}),\r\n" // time
			+ "(\\d{2}/\\d{2}/\\d{2})" // date
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"journey\">\r\n(.*?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<strong>(.*?)</strong>.*?" // line
			+ "&gt;&gt;\r\n" //
			+ "(.*?)\r\n" // destination
			+ "<br />\r\n" //
			+ "<strong>(\\d{1,2}:\\d{2})</strong>\r\n" // time
			+ "(?:<span class=\"delay\">([+-]?\\d+|Ausfall)</span>\r\n)?" // delay
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final QueryDeparturesResult result = new QueryDeparturesResult();

		// scrape page
		final String uri = departuresQueryUri(stationId, maxDepartures);
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(Status.INVALID_STATION);
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(Status.SERVICE_DOWN);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				ParserUtils.parseEuropeanTime(currentTime, mHeadFine.group(2));
				ParserUtils.parseGermanDate(currentTime, mHeadFine.group(3));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						final Calendar parsedTime = new GregorianCalendar(timeZone());
						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(3));

						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsedTime.add(Calendar.DAY_OF_MONTH, 1);

						mDepFine.group(4); // TODO delay

						final Departure dep = new Departure(parsedTime.getTime(), line, line != null ? lineColors(line) : null, null, 0, destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, Integer.parseInt(stationId), null, location),
						departures, null));
				return result;
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + stationId);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + stationId);
		}
	}

	private String normalizeLine(final String line)
	{
		if (line == null || line.length() == 0)
			return null;

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2);

			final char normalizedType = normalizeType(type);
			if (normalizedType != 0)
				return normalizedType + type + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		if (ucType.equals("EST")) // Eurostar Frankreich
			return 'I';
		if (ucType.equals("INT")) // Zürich-Brüssel
			return 'I';

		if (ucType.equals("L"))
			return 'R';
		if (ucType.equals("P"))
			return 'R';
		if (ucType.equals("CR"))
			return 'R';
		if (ucType.equals("ICT")) // Brügge
			return 'R';
		if (ucType.equals("TRN")) // Mons
			return 'R';

		if (ucType.equals("MÉT"))
			return 'U';

		if (ucType.equals("TRA"))
			return 'T';

		return 0;
	}
}
