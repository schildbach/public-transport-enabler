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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SeptaProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SEPTA;
	private static final String API_BASE = "http://airs1.septa.org/bin/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public SeptaProvider()
	{
		super(API_BASE + "query.exe/en", 4, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("EST");
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
		}
		else if (product == 'R' || product == 'S')
		{
			productBits.setCharAt(3, '1'); // Regional Rail
		}
		else if (product == 'U')
		{
			productBits.setCharAt(0, '1'); // Subway
		}
		else if (product == 'T')
		{
			productBits.setCharAt(1, '1'); // Trolley
		}
		else if (product == 'B')
		{
			productBits.setCharAt(2, '1'); // Bus
		}
		else if (product == 'P' || product == 'F' || product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_ADDRESS = Pattern.compile("(.*),\\s+([^,]+\\s+\\d{4,5})");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher matcher = P_SPLIT_ADDRESS.matcher(name);
		if (matcher.matches())
			return new String[] { matcher.group(2), matcher.group(1) };
		else
			return super.splitPlaceAndName(name);
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
		final Calendar now = new GregorianCalendar(timeZone());

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/en");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&time=").append(
				ParserUtils.urlEncode(String.format("%02d:%02d %s", now.get(Calendar.HOUR), now.get(Calendar.MINUTE),
						now.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm")));
		uri.append("&date=").append(
				String.format("%02d%02d%04d", now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.YEAR)));
		uri.append("&productsFilter=").append(allProductsString());
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_PAGE_COARSE = Pattern
			.compile(
					".*?" //
							+ "(?:" //
							+ "<div class=\"hfsTitleText\">([^<]*)<.*?" // location
							+ "\n(\\d{2}/\\d{2}/\\d{4})[^\n]*\n" // date
							+ "Departure (\\d{1,2}:\\d{2} [AP]M)\n.*?" // time
							+ "(?:<table class=\"resultTable\"[^>]*>(.+?)</table>|(No trains in this space of time))" //
							+ "|(input cannot be interpreted)|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
							+ ".*?" //
					, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<td class=\"time\">(\\d{1,2}:\\d{2} [AP]M)</td>\n" // plannedTime
			+ "(?:<td class=\"[\\w ]*prognosis[\\w ]*\">\n" //
			+ "(?:&nbsp;|<span class=\"rtLimit\\d\">(p&#252;nktlich|\\d{1,2}:\\d{2})</span>)\n</td>\n" // predictedTime
			+ ")?.*?" //
			+ "<img class=\"product\" src=\"/hafas-res/img/products/(\\w+)_pic\\.gif\" width=\"\\d+\" height=\"\\d+\" alt=\"([^\"]*)\".*?" // type,
			// line
			+ "<strong>\n" //
			+ "<a href=\"http://airs1\\.septa\\.org/bin/stboard\\.exe/en\\?input=(\\d+)&[^>]*>" // destinationId
			+ "\\s*(.*?)\\s*</a>\n" // destination
			+ "</strong>.*?" //
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final ResultHeader header = new ResultHeader(SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		// scrape page
		final String uri = departuresQueryUri(stationId, maxDepartures);
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mPageCoarse = P_DEPARTURES_PAGE_COARSE.matcher(page);
		if (mPageCoarse.matches())
		{
			// messages
			if (mPageCoarse.group(5) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			else if (mPageCoarse.group(6) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mPageCoarse.group(7) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final String location = ParserUtils.resolveEntities(mPageCoarse.group(1));
			final Calendar currentTime = new GregorianCalendar(timeZone());
			currentTime.clear();
			ParserUtils.parseAmericanDate(currentTime, mPageCoarse.group(2));
			ParserUtils.parseAmericanTime(currentTime, mPageCoarse.group(3));

			final List<Departure> departures = new ArrayList<Departure>(8);
			String oldZebra = null;

			final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mPageCoarse.group(4));
			while (mDepCoarse.find())
			{
				final String zebra = mDepCoarse.group(1);
				if (oldZebra != null && zebra.equals(oldZebra))
					throw new IllegalArgumentException("missed row? last:" + zebra);
				else
					oldZebra = zebra;

				final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(2));
				if (mDepFine.matches())
				{
					final Calendar plannedTime = new GregorianCalendar(timeZone());
					plannedTime.setTimeInMillis(currentTime.getTimeInMillis());
					ParserUtils.parseAmericanTime(plannedTime, mDepFine.group(1));

					if (plannedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
						plannedTime.add(Calendar.DAY_OF_MONTH, 1);

					final Calendar predictedTime;
					final String prognosis = ParserUtils.resolveEntities(mDepFine.group(2));
					if (prognosis != null)
					{
						predictedTime = new GregorianCalendar(timeZone());
						if (prognosis.equals("pünktlich"))
						{
							predictedTime.setTimeInMillis(plannedTime.getTimeInMillis());
						}
						else
						{
							predictedTime.setTimeInMillis(currentTime.getTimeInMillis());
							ParserUtils.parseAmericanTime(predictedTime, prognosis);
						}
					}
					else
					{
						predictedTime = null;
					}

					final String lineType = mDepFine.group(3);

					final Line line = parseLine(lineType, ParserUtils.resolveEntities(mDepFine.group(4)));

					final int destinationId = mDepFine.group(5) != null ? Integer.parseInt(mDepFine.group(5)) : 0;

					final String destination = ParserUtils.resolveEntities(mDepFine.group(6));

					final String position = mDepFine.group(7) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(7)) : null;

					final Departure dep = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
							position, destinationId, destination, null, null);

					if (!departures.contains(dep))
						departures.add(dep);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
				}
			}

			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, null, location), departures, null));
			return result;
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

		// skip parsing of "common" lines, because this is America

		// Regional
		if (ucType.equals("RAI"))
			return 'R';

		// Subway
		if (ucType.equals("BSS"))
			return 'U';
		if (ucType.equals("BSL"))
			return 'U';
		if (ucType.equals("MFL"))
			return 'U';

		// Tram
		if (ucType.equals("TRM"))
			return 'T';
		if (ucType.equals("NHS")) // Tro NHSL
			return 'T';

		// Bus
		if (ucType.equals("BUS"))
			return 'B';
		if (ucType.equals("TRO"))
			return 'B';

		// from Connections:

		if (ucType.equals("RAIL"))
			return 'R';

		if (ucType.equals("SUBWAY"))
			return 'U';

		if (ucType.equals("TROLLEY"))
			return 'B';

		return 0;
	}
}
