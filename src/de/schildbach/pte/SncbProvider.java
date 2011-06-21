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
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SncbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SNCB;
	public static final String OLD_NETWORK_ID = "hari.b-rail.be";
	private static final String API_BASE = "http://hari.b-rail.be/Hafas/bin/";
	private static final String API_URI = "http://hari.b-rail.be/Hafas/bin/extxml.exe";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public SncbProvider()
	{
		super(API_URI, 16, null);
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

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(2, '1'); // IC/IR/P/ICT
		}
		else if (product == 'R' || product == 'S')
		{
			productBits.setCharAt(6, '1'); // Zug
		}
		else if (product == 'U')
		{
			productBits.setCharAt(8, '1'); // Metro
		}
		else if (product == 'T')
		{
			productBits.setCharAt(10, '1'); // Stadtbahn
		}
		else if (product == 'B' || product == 'P')
		{
			productBits.setCharAt(9, '1'); // Bus
		}
		else if (product == 'F' || product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/en?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	private String departuresQueryUri(final int stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://hari.b-rail.be/hari3/webserver1/bin/stboard.exe/dox");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50); // maximum taken from SNCB site
		uri.append("&productsFilter=").append(allProductsString());
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
			+ "<strong>([^<]*)</strong>.*?" // line
			+ "&gt;&gt;\r\n" //
			+ "(.*?)\r\n" // destination
			+ "<br />\r\n" //
			+ "<strong>(\\d{1,2}:\\d{2})</strong>\r\n" // time
			+ "(?:<span class=\"delay\">([+-]?\\d+|Ausfall)</span>\r\n)?" // delay
			+ "(?:<span class=\"delay\">Aktivierung</span>\r\n)?" + "(?:([^<]*)<br />\r\n)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
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

						final String position = mDepFine.group(5);

						final Departure dep = new Departure(parsedTime.getTime(), null, new Line(null, line, line != null ? lineColors(line) : null),
								position, 0, destination, null, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, null, location), departures, null));
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

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/dny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsB=12&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.startsWith("IC "))
			return 'I';
		if ("THALYS".equals(ucType)) // Thalys
			return 'I';

		if (ucType.startsWith("IR "))
			return 'R';

		if ("L".equals(ucType))
			return 'R';
		if ("CR".equals(ucType))
			return 'R';
		if ("ICT".equals(ucType)) // Brügge
			return 'R';
		if ("TRN".equals(ucType)) // Mons
			return 'R';

		if ("MÉT".equals(ucType))
			return 'U';
		if ("MÉTRO".equals(ucType))
			return 'U';

		if ("TRAMWAY".equals(ucType))
			return 'T';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
