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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SbbProvider extends AbstractHafasProvider
{
	public static final String NETWORK_ID = "fahrplan.sbb.ch";
	private static final String API_BASE = "http://fahrplan.sbb.ch/bin/";
	private static final String API_URI = "http://fahrplan.sbb.ch/bin/extxml.exe";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public SbbProvider(final String accessId)
	{
		super(API_URI, accessId);
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	private final static String NEARBY_URI = API_BASE + "bhftafel.exe/dn?input=%s&distance=50&near=Anzeigen";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, stationId);
	}

	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("query.exe/dox");
		uri.append("?OK");
		uri.append("&REQ0HafasMaxChangeTime=120");
		uri.append("&REQ0HafasOptimize1=").append(ParserUtils.urlEncode("1:1"));
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0HafasSkipLongChanges=1");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(locationValue(from)));
		uri.append("&REQ0JourneyStopsS0A=").append(locationTypeValue(from));
		uri.append("&REQ0JourneyStopsS0ID=");
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(locationValue(via)));
			uri.append("&REQ0JourneyStops1.0A=").append(locationTypeValue(via));
			uri.append("&REQ0JourneyStops1.0ID=");
		}
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(locationValue(to)));
		uri.append("&REQ0JourneyStopsZ0A=").append(locationTypeValue(to));
		uri.append("&REQ0JourneyStopsZ0ID=");
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&queryPageDisplayed=yes");

		// TODO products

		uri.append("&start=Suchen");

		return uri.toString();
	}

	private static int locationTypeValue(final Location location)
	{
		final LocationType type = location.type;
		if (type == LocationType.STATION)
			return 1;
		if (type == LocationType.ADDRESS)
			return 2;
		if (type == LocationType.ANY)
			return 7;
		throw new IllegalArgumentException(type.toString());
	}

	private static String locationValue(final Location location)
	{
		if (location.type == LocationType.STATION && location.id != 0)
			return Integer.toString(location.id);
		else
			return location.name;
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern
			.compile("(mehrfach vorhanden oder identisch)|(keine Verbindung gefunden werden)|(liegt nach dem Ende der Fahrplanperiode|liegt vor Beginn der Fahrplanperiode)");

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.TOO_CLOSE;
			if (mError.group(2) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(3) != null)
				return QueryConnectionsResult.INVALID_DATE;
		}

		List<Location> fromAddresses = null;
		List<Location> viaAddresses = null;
		List<Location> toAddresses = null;

		// FIXME cannot parse ambiguous

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<Location> addresses = new ArrayList<Location>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(new Location(LocationType.ANY, 0, 0, 0, address + "!"));
			}

			if (type.equals("REQ0JourneyStopsS0K"))
				fromAddresses = addresses;
			else if (type.equals("REQ0JourneyStopsZ0K"))
				toAddresses = addresses;
			else if (type.equals("REQ0JourneyStops1.0K"))
				viaAddresses = addresses;
			else
				throw new IOException(type);
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(uri, page);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "Von: <strong>([^<]*)<.*?" // from
			+ "Nach: <strong>([^<]*)<.*?" // to
			+ "Datum: .., (\\d{2}\\.\\d{2}\\.\\d{2}).*?" // currentDate
			+ "(<p class=\"con_.*?)<p class=\"link1\">.*?" // body
			+ "(?:<a href=\"(http://[^\"]*REQ0HafasScrollDir=2)\".*?)?" // linkEarlier
			+ "(?:<a href=\"(http://[^\"]*REQ0HafasScrollDir=1)\".*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("\\G" //
			+ "<p class=\"(con_\\d+)\">\n" //
			+ "(.*?)</p>\n", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile("" //
			+ "(?:<img [^>]*>)?" //
			+ "<a href=\"(http://[^\"]*)\">" // link
			+ "(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})</a>.*?" // departureTime, arrivalTime
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Location from = new Location(LocationType.ANY, 0, 0, 0, ParserUtils.resolveEntities(mHead.group(1)));
			final Location to = new Location(LocationType.ANY, 0, 0, 0, ParserUtils.resolveEntities(mHead.group(2)));
			final Date currentDate = ParserUtils.parseDate(mHead.group(3));
			final String body = mHead.group(4);
			final String linkEarlier = mHead.group(5) != null ? ParserUtils.resolveEntities(mHead.group(5)) : null;
			final String linkLater = mHead.group(6) != null ? ParserUtils.resolveEntities(mHead.group(6)) : null;
			final List<Connection> connections = new ArrayList<Connection>();
			String oldZebra = null;

			final Matcher mCoarse = P_CONNECTIONS_COARSE.matcher(body);
			while (mCoarse.find())
			{
				final String zebra = mCoarse.group(1);
				if (oldZebra != null && zebra.equals(oldZebra))
					throw new IllegalArgumentException("missed row? last:" + zebra);
				else
					oldZebra = zebra;

				final String set = mCoarse.group(2);
				final Matcher mFine = P_CONNECTIONS_FINE.matcher(set);
				if (mFine.matches())
				{
					final String link = ParserUtils.resolveEntities(mFine.group(1));
					Date departureTime = ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mFine.group(2)));
					if (!connections.isEmpty())
					{
						final long diff = ParserUtils.timeDiff(departureTime, connections.get(connections.size() - 1).departureTime);
						if (diff > PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime = ParserUtils.addDays(departureTime, -1);
						else if (diff < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime = ParserUtils.addDays(departureTime, 1);
					}
					Date arrivalTime = ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mFine.group(3)));
					if (departureTime.after(arrivalTime))
						arrivalTime = ParserUtils.addDays(arrivalTime, 1);

					connections.add(new Connection(extractConnectionId(link), link, departureTime, arrivalTime, null, null, 0, from.name, 0, to.name,
							null));
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + set + "' on " + uri);
				}
			}

			return new QueryConnectionsResult(uri, from, null, to, linkEarlier, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?" //
			+ "<p class=\"conSecStart\">\n- ([^<]*) -\n</p>\n" //
			+ "(.*?)" //
			+ "<p class=\"remark\">\n" //
			+ "Abfahrt: (\\d+\\.\\d+\\.\\d+).*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("\\G" //
			+ "<p class=\"conSecJourney\">\n(.*?)</p>\n" //
			+ "<p class=\"conSecDestination\">\n(.*?)</p>\n" //
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_JOURNEY = Pattern.compile("" //
			+ "(?:" //
			+ "(.+?)\n.*?" // line
			+ "ab (\\d{1,2}:\\d{2})\n" // departureTime
			+ "(?: (Gl\\. .+?)\\s*\n)?" // departurePosition
			+ ".*?" //
			+ "an (\\d{1,2}:\\d{2})\n" // arrivalTime
			+ "(?: (Gl\\. .+?)\\s*\n)?" // arrivalPosition
			+ "|" //
			+ "(?:Fussweg|&#220;bergang)\n" //
			+ "(\\d+) Min\\.\n" // minutes
			+ ")" //
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_DESTINATION = Pattern.compile("" //
			+ "- ([^<]*) -\n" // destination
	, Pattern.DOTALL);

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			Date firstDepartureTime = null;
			final String firstDeparture = ParserUtils.resolveEntities(mHead.group(1));
			Date lastArrivalTime = null;
			String departure = firstDeparture;
			final String body = mHead.group(2);
			final Date date = ParserUtils.parseDate(mHead.group(3));

			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			final Matcher mCoarse = P_CONNECTION_DETAILS_COARSE.matcher(body);
			while (mCoarse.find())
			{
				final Matcher mJourney = P_CONNECTION_DETAILS_JOURNEY.matcher(mCoarse.group(1));
				final Matcher mDestination = P_CONNECTION_DETAILS_DESTINATION.matcher(mCoarse.group(2));
				if (mJourney.matches() && mDestination.matches())
				{
					final String arrival = mDestination.group(1);

					if (mJourney.group(6) == null)
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mJourney.group(1)));
						Date departureTime = ParserUtils.joinDateTime(date, ParserUtils.parseTime(mJourney.group(2)));
						if (lastArrivalTime != null && departureTime.before(lastArrivalTime))
							departureTime = ParserUtils.addDays(departureTime, 1);
						final String departurePosition = mJourney.group(3) != null ? ParserUtils.resolveEntities(mJourney.group(3)) : null;
						Date arrivalTime = ParserUtils.joinDateTime(date, ParserUtils.parseTime(mJourney.group(4)));
						if (departureTime.after(arrivalTime))
							arrivalTime = ParserUtils.addDays(arrivalTime, 1);
						final String arrivalPosition = mJourney.group(5) != null ? ParserUtils.resolveEntities(mJourney.group(5)) : null;

						parts.add(new Connection.Trip(line, lineColors(line), 0, null, departureTime, departurePosition, 0, departure, arrivalTime,
								arrivalPosition, 0, arrival));

						if (firstDepartureTime == null)
							firstDepartureTime = departureTime;
						lastArrivalTime = arrivalTime;
						departure = arrival;
					}
					else
					{
						final int min = Integer.parseInt(mJourney.group(6));

						parts.add(new Connection.Footway(min, 0, departure, 0, arrival, 0, 0));

						departure = arrival;
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "', '" + mCoarse.group(2) + "' on " + uri);
				}
			}

			return new GetConnectionDetailsResult(new Date(), new Connection(AbstractHafasProvider.extractConnectionId(uri), uri, firstDepartureTime,
					lastArrivalTime, null, null, 0, firstDeparture, 0, departure, parts));
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}

	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("bhftafel.exe/dox");
		uri.append("?start=");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=1111111111000000");
		uri.append("&input=").append(stationId);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<p class=\"querysummary\">\n(.*?)</p>\n" // head
			+ "(?:(.*?)|(an dieser Haltestelle keines))\n" // departures
			+ "<p class=\"link1\">\n(.*?)</p>\n" // footer
			+ "|(Informationen zu)" // messages
			+ "|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden)" // messages
			+ ").*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" // 
			+ "<strong>([^<]*)</strong>(?:<br />)?\n" // location
			+ "Abfahrt (\\d{1,2}:\\d{2})\n" // time
			+ "Uhr, (\\d{2}\\.\\d{2}\\.\\d{2})\n" // date
			+ ".*?input=(\\d+)&.*?" // locationId
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"(journey con_\\d+)\">\n(.*?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<strong>(.*?)</strong>\n" // line
			+ "&gt;&gt;\n" //
			+ "(.*?)\n" // destination
			+ "<br />\n" //
			+ "<strong>(\\d+:\\d+)</strong>\n" // time
			+ "(?:Gl\\. (" + ParserUtils.P_PLATFORM + ")\n)?" // position
			+ ".*?" //
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(departuresQueryUri(stationId, maxDepartures));

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult( Status.NO_INFO, Integer.parseInt(stationId));
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult( Status.INVALID_STATION, Integer.parseInt(stationId));
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult( Status.SERVICE_DOWN, Integer.parseInt(stationId));

			final String head = mHeadCoarse.group(1) + mHeadCoarse.group(4);
			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(head);
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(3)), ParserUtils
						.parseTime(mHeadFine.group(2)));
				final int locationId = Integer.parseInt(mHeadFine.group(4));
				final List<Departure> departures = new ArrayList<Departure>(8);
				String oldZebra = null;

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
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
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						final Calendar current = new GregorianCalendar();
						current.setTime(currentTime);
						final Calendar parsed = new GregorianCalendar();
						parsed.setTime(ParserUtils.parseTime(mDepFine.group(3)));
						parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
						parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
						parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
						if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsed.add(Calendar.DAY_OF_MONTH, 1);

						final String position = ParserUtils.resolveEntities(mDepFine.group(4));

						final Departure dep = new Departure(parsed.getTime(), line, line != null ? lineColors(line) : null, position, 0, destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				return new QueryDeparturesResult(locationId, location, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + head + "' on " + stationId);
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

	private static final Pattern P_NORMALIZE_TYPE_SBAHN = Pattern.compile("SN?\\d*");
	private static final Pattern P_NORMALIZE_TYPE_BUS = Pattern.compile("BUS\\w*");

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		if (ucType.equals("ICN")) // Intercity-Neigezug, Schweiz
			return 'I';
		if (ucType.equals("X")) // InterConnex
			return 'I';
		if (ucType.equals("ES")) // Eurostar Italia
			return 'I';
		if (ucType.equals("EST")) // Eurostar Frankreich
			return 'I';
		if (ucType.equals("NZ")) // Nachtzug?
			return 'I';
		if (ucType.equals("IN")) // Oslo
			return 'I';
		if (ucType.equals("AVE")) // Alta Velocidad Española, Spanien
			return 'I';
		if (ucType.equals("EM")) // Barcelona-Alicante, Spanien
			return 'I';
		if (ucType.equals("FYR")) // Fyra, Amsterdam-Schiphol-Rotterdam
			return 'I';
		if (ucType.equals("ARZ")) // Frankreich, Nacht
			return 'I';

		if (ucType.equals("D"))
			return 'R';
		if (ucType.equals("E"))
			return 'R';
		if (ucType.equals("EXT"))
			return 'R';
		if (ucType.equals("ATZ"))
			return 'R';
		if (ucType.equals("RSB"))
			return 'R';
		if (ucType.equals("SN"))
			return 'R';
		if (ucType.equals("CAT")) // City Airport Train Wien
			return 'R';
		if (ucType.equals("ALS")) // Spanien
			return 'R';
		if (ucType.equals("ARC")) // Spanien
			return 'R';
		if (ucType.equals("TAL")) // Spanien
			return 'R';
		if (ucType.equals("ATR")) // Spanien
			return 'R';

		if (P_NORMALIZE_TYPE_SBAHN.matcher(ucType).matches())
			return 'S';

		if (ucType.equals("MET")) // Lausanne
			return 'U';

		if (ucType.equals("TRAM"))
			return 'T';
		if (ucType.equals("TRA"))
			return 'T';
		if (ucType.equals("M")) // Lausanne
			return 'T';
		if (ucType.equals("T"))
			return 'T';
		if (ucType.equals("NTR"))
			return 'T';

		if (ucType.equals("TRO"))
			return 'B';
		if (ucType.equals("NTO")) // Niederflurtrolleybus zwischen Bern, Bahnhofsplatz und Bern, Wankdorf Bahnhof
			return 'B';
		if (ucType.equals("NFB"))
			return 'B';
		if (ucType.equals("NBU"))
			return 'B';
		if (ucType.equals("MIN"))
			return 'B';
		if (ucType.equals("MID"))
			return 'B';
		if (ucType.equals("N"))
			return 'B';
		if (ucType.equals("TX"))
			return 'B';
		if (ucType.equals("TAXI"))
			return 'B';
		if (ucType.equals("BUXI"))
			return 'B';
		if (P_NORMALIZE_TYPE_BUS.matcher(ucType).matches())
			return 'B';

		if (ucType.equals("BAT"))
			return 'F';
		if (ucType.equals("BAV"))
			return 'F';
		if (ucType.equals("FAE"))
			return 'F';
		if (ucType.equals("KAT")) // z.B. Friedrichshafen <-> Konstanz
			return 'F';

		if (ucType.equals("GB")) // Gondelbahn
			return 'C';
		if (ucType.equals("SL")) // Sessel-Lift
			return 'C';
		if (ucType.equals("LB"))
			return 'C';
		if (ucType.equals("FUN")) // Standseilbahn
			return 'C';

		if (ucType.equals("P"))
			return '?';

		return 0;
	}
}
