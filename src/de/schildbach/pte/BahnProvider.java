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
import java.util.Collections;
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
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public final class BahnProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DB;
	public static final String OLD_NETWORK_ID = "mobile.bahn.de";
	private static final String API_BASE = "http://mobile.bahn.de/bin/mobil/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public BahnProvider()
	{
		super("http://reiseauskunft.bahn.de/bin/extxml.exe", null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		return true;
	}

	private final static Pattern P_NEARBY_STATIONS_BY_COORDINATE = Pattern
			.compile("<a class=\"uLine\" href=\".+?!X=(\\d+)!Y=(\\d+)!id=(\\d+)!dist=(\\d+).*?\">(.+?)</a>");
	private final static Pattern P_NEARBY_STATIONS_BY_STATION = Pattern
			.compile("<a href=\"http://mobile\\.bahn\\.de/bin/mobil/bhftafel.exe/dn[^\"]*?evaId=(\\d*)&[^\"]*?\">([^<]*)</a>");

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		final List<Station> stations = new ArrayList<Station>();

		if (lat == 0 && lon == 0)
		{
			final String uri = API_BASE + "bhftafel.exe/dn?near=Anzeigen&distance=50&input=" + ParserUtils.urlEncode(stationId);
			final CharSequence page = ParserUtils.scrape(uri);

			final Matcher m = P_NEARBY_STATIONS_BY_STATION.matcher(page);
			while (m.find())
			{
				final int sId = Integer.parseInt(m.group(1));
				final String sName = ParserUtils.resolveEntities(m.group(2).trim());

				final Station station = new Station(sId, null, sName, null, 0, 0, 0, null, null);
				stations.add(station);
			}
		}
		else
		{
			final String uri = API_BASE + "query.exe/dox" + "?performLocating=2&tpl=stopsnear&look_maxdist=" + (maxDistance > 0 ? maxDistance : 5000)
					+ "&look_stopclass=1023" + "&look_x=" + lon + "&look_y=" + lat;
			final CharSequence page = ParserUtils.scrape(uri);

			final Matcher m = P_NEARBY_STATIONS_BY_COORDINATE.matcher(page);
			while (m.find())
			{
				final int sId = Integer.parseInt(m.group(3));

				final int sLon = Integer.parseInt(m.group(1));
				final int sLat = Integer.parseInt(m.group(2));
				final int sDist = Integer.parseInt(m.group(4));
				final String sName = ParserUtils.resolveEntities(m.group(5).trim());

				final Station station = new Station(sId, null, sName, null, sLat, sLon, sDist, null, null);
				stations.add(station);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return new NearbyStationsResult(stations);
		else
			return new NearbyStationsResult(stations.subList(0, maxStations));
	}

	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("query.exe/dox");
		uri.append("?REQ0HafasOptimize1=0:1");
		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from)));
		if (via != null)
			uri.append("&REQ0JourneyStops1.0ID=").append(ParserUtils.urlEncode(locationId(via)));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to)));
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&REQ0Tariff_Class=2");
		uri.append("&REQ0Tariff_TravellerAge.1=35");
		uri.append("&REQ0Tariff_TravellerReductionClass.1=0");
		uri.append("&existOptimizePrice=1");
		uri.append("&existProductNahverkehr=yes");
		uri.append("&start=Suchen");

		return uri.toString();
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern
			.compile("(zu dicht beieinander|mehrfach vorhanden oder identisch)|(keine geeigneten Haltestellen)|(keine Verbindung gefunden)|(derzeit nur Ausk&#252;nfte vom)|(zwischenzeitlich nicht mehr gespeichert)");

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep);
		final CharSequence page = ParserUtils.scrape(uri);

		List<Location> fromAddresses = null;
		List<Location> viaAddresses = null;
		List<Location> toAddresses = null;

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
					addresses.add(new Location(LocationType.ANY, 0, null, address + "!"));
			}

			if (type.equals("REQ0JourneyStopsS0K"))
				fromAddresses = addresses;
			else if (type.equals("REQ0JourneyStopsZ0K"))
				toAddresses = addresses;
			else if (type.equals("REQ0JourneyStops1.0K"))
				viaAddresses = addresses;
			else
				throw new IllegalStateException(type);
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
			+ "von: <span class=\"bold\">([^<]*)</span>.*?" // from
			+ "nach: <span class=\"bold\">([^<]*)</span>.*?" // to
			+ "Datum: <span class=\"bold\">.., (\\d{2}\\.\\d{2}\\.\\d{2})</span>.*?" // currentDate
			+ "(?:<a href=\"([^\"]*)\"><img [^>]*>\\s*Fr&#252;her.*?)?" // linkEarlier
			+ "(?:<a class=\"noBG\" href=\"([^\"]*)\"><img [^>]*>\\s*Sp&#228;ter.*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<tr><td class=\"overview timelink\">(.+?)</td></tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "<a href=\"(http://mobile.bahn.de/bin/mobil/query.exe/dox[^\"]*?)\">" // link
			+ "(\\d{1,2}:\\d{2})<br />(\\d{1,2}:\\d{2})</a></td>.+?" // departureTime, arrivalTime
			+ "<td class=\"overview iphonepfeil\">(.*?)<br />.*?" // line
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.TOO_CLOSE;
			if (mError.group(2) != null)
				return QueryConnectionsResult.UNRESOLVABLE_ADDRESS;
			if (mError.group(3) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(4) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(5) != null)
				throw new SessionExpiredException();
		}

		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Location from = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(1)));
			final Location to = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(2)));
			final Date currentDate = ParserUtils.parseDate(mHead.group(3));
			final String linkEarlier = mHead.group(4) != null ? ParserUtils.resolveEntities(mHead.group(4)) : null;
			final String linkLater = mHead.group(5) != null ? ParserUtils.resolveEntities(mHead.group(5)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			while (mConCoarse.find())
			{
				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(mConCoarse.group(1));
				if (mConFine.matches())
				{
					final String link = ParserUtils.resolveEntities(mConFine.group(1));
					Date departureTime = ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mConFine.group(2)));
					if (!connections.isEmpty())
					{
						final long diff = ParserUtils.timeDiff(departureTime, connections.get(connections.size() - 1).departureTime);
						if (diff > PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime = ParserUtils.addDays(departureTime, -1);
						else if (diff < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime = ParserUtils.addDays(departureTime, 1);
					}
					Date arrivalTime = ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mConFine.group(3)));
					if (departureTime.after(arrivalTime))
						arrivalTime = ParserUtils.addDays(arrivalTime, 1);
					final Connection connection = new Connection(AbstractHafasProvider.extractConnectionId(link), link, departureTime, arrivalTime,
							0, from.name, 0, to.name, null, null);
					connections.add(connection);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(1) + "' on " + uri);
				}
			}

			return new QueryConnectionsResult(uri, from, null, to, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?" //
			+ "<span class=\"bold\">Verbindungsdetails</span>(.*?)<div class=\"rline\"></div>.*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("<div class=\"haupt rline\">\n(.+?>\n)</div>", Pattern.DOTALL);
	static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // departure
			+ "(?:" //
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // line
			+ "ab\\s+(?:<span[^>]*>.*?</span>)?\\s*(\\d{1,2}:\\d{2})\\s*(?:<span[^>]*>.*?</span>)?" // departureTime
			+ "\\s*(Gl\\. .+?)?\\s*\n" // departurePosition
			+ "am\\s+(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // departureDate
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />.*?" // arrival
			+ "an\\s+(?:<span[^>]*>.*?</span>)?\\s*(\\d{1,2}:\\d{2})\\s*(?:<span[^>]*>.*?</span>)?" // arrivalTime
			+ "\\s*(Gl\\. .+?)?\\s*\n" // arrivalPosition
			+ "am\\s+(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // arrivalDate
			+ "|" //
			+ "(\\d+) Min\\..*?" // footway
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />\n" // arrival
			+ "|" //
			+ "&#220;bergang.*?" //
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />\n" // arrival
			+ ")", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_ERROR = Pattern.compile("(zwischenzeitlich nicht mehr gespeichert)");
	private static final Pattern P_CONNECTION_DETAILS_MESSAGES = Pattern
			.compile("<div class=\"him\">|Dauer: \\d+:\\d+|Anschlusszug nicht mehr rechtzeitig|Anschlusszug jedoch erreicht werden|nur teilweise dargestellt|L&#228;ngerer Aufenthalt|&#228;quivalentem Bahnhof|Bahnhof wird mehrfach durchfahren");

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CONNECTION_DETAILS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				throw new SessionExpiredException();
		}

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			Date firstDepartureTime = null;
			String firstDeparture = null;
			Date lastArrivalTime = null;
			String lastArrival = null;
			Connection.Trip lastTrip = null;

			final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(mHead.group(1));
			while (mDetCoarse.find())
			{
				final String section = mDetCoarse.group(1);
				if (P_CONNECTION_DETAILS_MESSAGES.matcher(section).find())
				{
					// ignore message for now
				}
				else
				{
					final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(section);
					if (mDetFine.matches())
					{
						final String departure = ParserUtils.resolveEntities(mDetFine.group(1));
						if (departure != null && firstDeparture == null)
							firstDeparture = departure;

						if (mDetFine.group(2) != null)
						{
							final String line = normalizeLine(ParserUtils.resolveEntities(mDetFine.group(2)));

							final Date departureTime = ParserUtils.parseTime(mDetFine.group(3));

							final String departurePosition = mDetFine.group(4) != null ? ParserUtils.resolveEntities(mDetFine.group(4)) : null;

							final Date departureDate = ParserUtils.parseDate(mDetFine.group(5));

							final String arrival = ParserUtils.resolveEntities(mDetFine.group(6));

							final Date arrivalTime = ParserUtils.parseTime(mDetFine.group(7));

							final String arrivalPosition = mDetFine.group(8) != null ? ParserUtils.resolveEntities(mDetFine.group(8)) : null;

							final Date arrivalDate = ParserUtils.parseDate(mDetFine.group(9));

							final Date departureDateTime = ParserUtils.joinDateTime(departureDate, departureTime);
							final Date arrivalDateTime = ParserUtils.joinDateTime(arrivalDate, arrivalTime);
							lastTrip = new Connection.Trip(line, null, departureDateTime, departurePosition, 0, departure, arrivalDateTime,
									arrivalPosition, 0, arrival, null);
							parts.add(lastTrip);

							if (firstDepartureTime == null)
								firstDepartureTime = departureDateTime;

							lastArrival = arrival;
							lastArrivalTime = arrivalDateTime;
						}
						else if (mDetFine.group(10) != null)
						{
							final String min = mDetFine.group(10);

							final String arrival = ParserUtils.resolveEntities(mDetFine.group(11));

							if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
							{
								final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
								parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), 0, lastFootway.departure, 0, arrival));
							}
							else
							{
								parts.add(new Connection.Footway(Integer.parseInt(min), 0, departure, 0, arrival));
							}

							lastArrival = arrival;
						}
						else
						{
							final String arrival = ParserUtils.resolveEntities(mDetFine.group(12));

							parts.add(new Connection.Footway(0, 0, departure, 0, arrival));
						}
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + section + "' on " + uri);
					}
				}
			}

			// verify
			if (firstDepartureTime == null || lastArrivalTime == null)
				throw new IllegalStateException("could not parse all parts of:\n" + mHead.group(1) + "\n" + parts);

			return new GetConnectionDetailsResult(new Date(), new Connection(AbstractHafasProvider.extractConnectionId(uri), uri, firstDepartureTime,
					lastArrivalTime, 0, firstDeparture, 0, lastArrival, parts, null));
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("bhftafel.exe/dn");
		uri.append("?productsFilter=11111111111111");
		uri.append("&boardType=dep");
		// taken from railnavigator; ignore maxDepartures because result contains other stations
		uri.append("&maxJourneys=50");
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);
		return uri.toString();
	}

	static final Pattern P_DEPARTURES_COARSE = Pattern.compile("\\G<Journey (.*?)/?>(?:\n|\\z)", Pattern.DOTALL);
	static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "fpTime=\"(\\d{1,2}:\\d{2})\" fpDate=\"(\\d{2}\\.\\d{2}\\.\\d{2})\" \n" // time, date
			+ "delay=\"(?:-|k\\.A\\.?|cancel|\\+?\\s*(\\d+))\" \n" // delay
			+ "(?:platform =\"([^\"]*)\" \n)?" // position
			+ "(?:newpl =\"([^\"]*)\" \n)?" //
			+ "targetLoc=\"(.*?)\" \n" // destination
			+ "prod=\"([^\"]*)\" \n" // line
			+ "(?:dir=[^\n]*\n)?" // (destination)
			+ "(?:depStation=\"(.*?)\"\n)?" //
			+ "delayReason=\"([^\"]*)\"\n" // message
	);
	private static final Pattern P_DEPARTURES_MESSAGES = Pattern.compile("<Err code=\"([^\"]*)\" text=\"([^\"]*)\"");

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures) throws IOException
	{
		final String uri = departuresQueryUri(stationId, maxDepartures);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mMessage = P_DEPARTURES_MESSAGES.matcher(page);
		if (mMessage.find())
		{
			final String code = mMessage.group(1);
			final String text = mMessage.group(2);

			if (code.equals("H730")) // Your input is not valid
				return new QueryDeparturesResult(QueryDeparturesResult.Status.INVALID_STATION, Integer.parseInt(stationId));
			if (code.equals("H890")) // No trains in result
				return new QueryDeparturesResult(new Location(LocationType.STATION, Integer.parseInt(stationId)),
						Collections.<Departure> emptyList(), null);
			throw new IllegalArgumentException("unknown error " + code + ", " + text);
		}

		final List<Departure> departures = new ArrayList<Departure>(8);
		final Calendar calendar = new GregorianCalendar();

		final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(page);
		while (mDepCoarse.find())
		{
			final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
			if (mDepFine.matches())
			{
				if (mDepFine.group(8) == null)
				{
					final Date plannedTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mDepFine.group(2)),
							ParserUtils.parseTime(mDepFine.group(1)));

					Date predictedTime = null;
					if (mDepFine.group(3) != null)
					{
						calendar.setTime(plannedTime);
						calendar.add(Calendar.MINUTE, Integer.parseInt(mDepFine.group(3)));
						predictedTime = calendar.getTime();
					}

					final String position = mDepFine.group(4) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(4)) : null;

					final String destination = ParserUtils.resolveEntities(mDepFine.group(6)).trim();

					final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(7)));

					final String message = ParserUtils.resolveEntities(mDepFine.group(9));

					departures.add(new Departure(plannedTime, predictedTime, line, line != null ? lineColors(line) : null, null, position, 0,
							destination, message));
				}
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
			}
		}

		return new QueryDeparturesResult(new Location(LocationType.STATION, Integer.parseInt(stationId)), departures, null);
	}

	@Override
	protected char normalizeType(String type)
	{
		throw new UnsupportedOperationException();
	}

	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");
	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû]+)[\\s-]*(.*)");
	private static final Pattern P_NORMALIZE_LINE_RUSSIA = Pattern.compile("(?:D\\s*)?(\\d{1,3}(?:[A-Z]{2}|Y))");
	private static final Pattern P_NORMALIZE_LINE_SBAHN = Pattern.compile("S\\w*\\d+");

	private static String normalizeLine(final String line)
	{
		// TODO ARZ Simplon Tunnel: Brig - Iselle di Trasquera
		// ARZ29171
		// ARZ29172
		// ARZ29173
		// ARZ29177
		// ARZ29178

		if (line == null)
			return null;

		if (line.length() == 0)
			return "?";

		if (line.equals("---"))
			return "?---";

		if (P_NORMALIZE_LINE_NUMBER.matcher(line).matches()) // just numbers
			return "?" + line;

		final Matcher mRussia = P_NORMALIZE_LINE_RUSSIA.matcher(line);
		if (mRussia.matches())
			return "R" + mRussia.group(1);

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2).replace(" ", "");

			if (type.equals("ICE")) // InterCityExpress
				return "IICE" + number;
			if (type.equals("IC")) // InterCity
				return "IIC" + number;
			if (type.equals("EC")) // EuroCity
				return "IEC" + number;
			if (type.equals("EN")) // EuroNight
				return "IEN" + number;
			if (type.equals("CNL")) // CityNightLine
				return "ICNL" + number;
			if (type.equals("X")) // InterConnex
				return "IX" + number;
			if (type.equals("TLK")) // Tanie Linie Kolejowe (Polen)
				return "ITLK" + number;
			if (type.equals("TGV")) // Train à Grande Vitesse
				return "ITGV" + number;
			if (type.equals("THA")) // Thalys
				return "ITHA" + number;
			if (type.equals("RJ")) // RailJet, Österreichische Bundesbahnen
				return "IRJ" + number;
			if (type.equals("OEC")) // ÖBB-EuroCity
				return "IOEC" + number;
			if (type.equals("OIC")) // ÖBB-InterCity
				return "IOIC" + number;
			if (type.equals("ICN")) // Intercity-Neigezug, Schweiz
				return "IICN" + number;
			if (type.equals("AVE")) // Alta Velocidad Española, Spanien
				return "IAVE" + number;
			if (type.equals("SC")) // SuperCity, Tschechien
				return "ISC" + number;
			if (type.equals("EST")) // Eurostar Frankreich
				return "IEST" + number;
			if (type.equals("ES")) // Eurostar Italia
				return "IES" + number;
			if (type.equals("ALS")) // Spanien
				return "IALS" + number;
			if (type.equals("ARC")) // Spanien
				return "IARC" + number;
			if (type.equals("TLG")) // Spanien
				return "ITLG" + number;
			if (type.equals("HOT")) // Spanien, Nacht
				return "IHOT" + number;
			if (type.equals("EM")) // EuroMed, Spanien
				return "IEM" + number;
			if (type.equals("EIC")) // Polen
				return "IEIC" + number;
			if (type.equals("FYR")) // Fyra, Amsterdam-Schiphol-Rotterdam
				return "IFYR" + number;

			if (type.equals("R"))
				return "R" + number;
			if (type.equals("IR")) // InterRegio
				return "RIR" + number;
			if (type.equals("RB")) // RegionalBahn, evtl. auch Regental Bahnbetriebs GmbH
				return "RRB" + number;
			if (type.equals("RBG")) // Regental Bahnbetriebs GmbH
				return "RRBG" + number;
			if (type.equals("RE")) // RegionalExpress
				return "RRE" + number;
			if (type.equals("IRE")) // Interregio Express
				return "RIRE" + number;
			if (type.equals("RFB")) // Reichenbachfall-Bahn
				return "RRFB" + number;
			if (type.equals("VEC")) // vectus Verkehrsgesellschaft
				return "RVEC" + number;
			if (type.equals("HTB")) // Hörseltalbahn
				return "RHTB" + number;
			if (type.equals("HLB")) // Hessenbahn
				return "RHLB" + number;
			if (type.equals("MRB")) // Mitteldeutsche Regiobahn
				return "RMRB" + number;
			if (type.equals("VBG")) // Vogtlandbahn
				return "RVBG" + number;
			if (type.equals("VX")) // Vogtland Express
				return "RVX" + number;
			if (type.equals("HzL") || type.equals("HZL")) // Hohenzollerische Landesbahn
				return "RHzL" + number;
			if (type.equals("BOB")) // Bayerische Oberlandbahn
				return "RBOB" + number;
			if (type.equals("BRB")) // Bayerische Regiobahn
				return "RBRB" + number;
			if (type.equals("ALX")) // Arriva-Länderbahn-Express
				return "RALX" + number;
			if (type.equals("NWB")) // NordWestBahn
				return "RNWB" + number;
			if (type.equals("HEX")) // Harz-Berlin-Express, Veolia
				return "RHEX" + number;
			if (type.equals("PEG")) // Prignitzer Eisenbahn
				return "RPEG" + number;
			if (type.equals("STB")) // Süd-Thüringen-Bahn
				return "RSTB" + number;
			if (type.equals("HSB")) // Harzer Schmalspurbahnen
				return "RHSB" + number;
			if (type.equals("EVB")) // Eisenbahnen und Verkehrsbetriebe Elbe-Weser
				return "REVB" + number;
			if (type.equals("NOB")) // Nord-Ostsee-Bahn
				return "RNOB" + number;
			if (type.equals("WFB")) // Westfalenbahn
				return "RWFB" + number;
			if (type.equals("FEG")) // Freiberger Eisenbahngesellschaft
				return "RFEG" + number;
			if (type.equals("SHB")) // Schleswig-Holstein-Bahn
				return "RSHB" + number;
			if (type.equals("OSB")) // Ortenau-S-Bahn
				return "ROSB" + number;
			if (type.equals("WEG")) // Württembergische Eisenbahn-Gesellschaft
				return "RWEG" + number;
			if (type.equals("MR")) // Märkische Regionalbahn
				return "RMR" + number;
			if (type.equals("OE")) // Ostdeutsche Eisenbahn
				return "ROE" + number;
			if (type.equals("UBB")) // Usedomer Bäderbahn
				return "RUBB" + number;
			if (type.equals("NEB")) // Niederbarnimer Eisenbahn
				return "RNEB" + number;
			if (type.equals("AKN")) // AKN Eisenbahn AG
				return "RAKN" + number;
			if (type.equals("SBB")) // Schweizerische Bundesbahnen
				return "RSBB" + number;
			if (type.equals("OLA")) // Ostseeland Verkehr
				return "ROLA" + number;
			if (type.equals("ME")) // metronom
				return "RME" + number;
			if (type.equals("MEr") || type.equals("MER")) // metronom regional
				return "RMER" + number;
			if (type.equals("ERB")) // eurobahn (Keolis Deutschland)
				return "RERB" + number;
			if (type.equals("EB")) // Erfurter Bahn
				return "REB" + number;
			if (type.equals("VIA")) // VIAS
				return "RVIA" + number;
			if (type.equals("CAN")) // cantus Verkehrsgesellschaft
				return "RCAN" + number;
			if (type.equals("PEG")) // Prignitzer Eisenbahn
				return "RPEG" + number;
			if (type.equals("BLB")) // Berchtesgadener Land Bahn
				return "RBLB" + number;
			if (type.equals("PRE")) // Pressnitztalbahn
				return "RPRE" + number;
			if (type.equals("neg")) // Norddeutsche Eisenbahngesellschaft Niebüll
				return "Rneg" + number;
			if (type.equals("NBE")) // nordbahn
				return "RNBE" + number;
			if (type.equals("MBB")) // Mecklenburgische Bäderbahn Molli
				return "RMBB" + number;
			if (type.equals("ABR")) // Abellio Rail NRW
				return "RABR" + number;
			if (type.equals("ABG")) // Anhaltische Bahngesellschaft
				return "RABG" + number;
			if (type.equals("Sp")) // EgroNet?
				return "RSp" + number;
			if (type.equals("Os")) // EgroNet?
				return "ROs" + number;
			if (type.equals("REX")) // Österreich?
				return "RREX" + number;
			if (type.equals("SB")) // Säntis-Bahn, Schweiz - evtl. auch SaarBahn+Bus?
				return "RSB" + number;
			if (type.equals("LT"))
				return "RLT" + number;
			if (type.equals("CB")) // City Bahn Chemnitz
				return "RCB" + number;
			if (type.equals("SWE")) // SWEG
				return "RSWE" + number;
			if (type.equals("ÖBA")) // Öchsle-Bahn Betriebsgesellschaft
				return "RÖBA" + number;
			if (type.equals("RTB")) // Rurtalbahn
				return "RRTB" + number;
			if (type.equals("SOE")) // Sächsisch-Oberlausitzer Eisenbahngesellschaft
				return "RSOE" + number;
			if (type.equals("SBE")) // Sächsisch-Böhmische Eisenbahngesellschaft
				return "RSBE" + number;
			if (type.equals("Dab")) // Daadetalbahn
				return "RDab" + number;
			if (type.equals("SDG")) // Sächsische Dampfeisenbahngesellschaft
				return "RSDG" + number;
			if (type.equals("ARR")) // Ostfriesland
				return "RARR" + number;
			if (type.equals("MEL")) // Museumsbahn Merzig-Losheim
				return "RMEL" + number;
			if (type.equals("VEB")) // Vulkan-Eifel-Bahn Betriebsgesellschaft
				return "RVEB" + number;
			if (type.equals("P")) // Kasbachtalbahn
				return "RP" + number;
			if (type.equals("MSB")) // Mainschleifenbahn
				return "RMSB" + number;
			if (type.equals("KTB")) // Kandertalbahn
				return "RKTB" + number;
			if (type.equals("WTB")) // Wutachtalbahn
				return "RWTB" + number;
			if (type.equals("DPNCbahn")) // Chiemsee-Bahn
				return "RDPNCbahn" + number;
			if (type.equals("LEO")) // Chiemgauer Lokalbahn
				return "RLEO" + number;
			if (type.equals("SBS")) // Städtebahn Sachsen
				return "RSBS" + number;
			if (type.equals("VEN")) // Rhenus Veniro
				return "RVEN" + number;
			if (type.equals("KD")) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
				return "RKD" + number;
			if (type.equals("SKW")) // Polen
				return "RSKW" + number;
			if (type.equals("KM")) // Polen
				return "RKM" + number;
			if (type.equals("PCC")) // Polen
				return "RPCC" + number;
			if (type.equals("SKM")) // Polen
				return "RSKM" + number;
			if (type.equals("LYN")) // Dänemark
				return "RLYN" + number;
			if (type.equals("EX")) // Norwegen
				return "REX" + number;
			if (type.equals("NZ")) // Norwegen
				return "RNZ" + number;
			if (type.equals("IP")) // InterPici, Ungarn
				return "RIP" + number;
			if (type.equals("Zr")) // ZSR, Slovakai
				return "RZr" + number;
			if (type.equals("N")) // Frankreich, Tours, Orléans
				return "RN" + number;
			if (type.equals("VE")) // Lutherstadt Wittenberg
				return "RVE" + number;
			if (type.equals("DZ")) // Dampfzug Freiburg-Innsbruck
				return "RDZ " + number;
			if (type.equals("ag")) // Regensburg-Landshut
				return "Rag " + number;
			if (type.equals("TLX")) // Trilex (Vogtlandbahn)
				return "RTLX" + number;
			if (type.equals("BE")) // Grensland-Express
				return "RBE" + number;
			if (type.equals("ATB")) // Autoschleuse Tauernbahn
				return "RATB" + number;
			if (type.equals("ARZ")) // Brig-Iselle di Trasquera
				return "RARZ" + number;

			if (type.equals("S"))
				return "SS" + number;
			if (type.equals("BSB")) // Breisgau S-Bahn
				return "SBSB" + number;
			if (type.equals("RER")) // Réseau Express Régional, Frankreich
				return "SRER" + number;
			if (type.equals("RSB")) // Schnellbahn Wien
				return "SRSB" + number;
			if (type.equals("CAT")) // City Airport Train, Schweden
				return "SCAT" + number;
			if (type.equals("DPN")) // S3 Bad Reichenhall-Freilassing
				return "SDPN" + number;
			if (type.equals("WKD")) // Warszawska Kolej Dojazdowa (Warsaw Suburban Railway)
				return "SWKD" + number;

			if (type.equals("U"))
				return "UU" + number;

			if (type.equals("STR"))
				return "T" + number;
			if (type.equals("STRNE"))
				return "T" + number;
			if (type.equals("STRKbahn"))
				return "TKbahn" + number;
			if (type.equals("RT")) // RegioTram
				return "TRT" + number;
			if (type.equals("Schw")) // Schwebebahn, gilt als "Straßenbahn besonderer Bauart"
				return "TSchw" + number;

			if (type.equals("BUS"))
				return "BBUS" + number;
			if (type.startsWith("Bus"))
				return "B" + type.substring(3) + number;
			if (type.equals("O")) // Salzburg
				return "BO" + number;
			if (type.startsWith("AST")) // Anruf-Sammel-Taxi
				return "BAST" + type.substring(3) + number;
			if (type.startsWith("ALT")) // Anruf-Linien-Taxi
				return "BALT" + type.substring(3) + number;
			if (type.startsWith("RFB")) // Rufbus
				return "BRFB" + type.substring(3) + number;
			if (type.equals("RNV")) // Rhein-Neckar-Verkehr GmbH - TODO aufteilen in Tram/Bus/Fähre
				return "BRNV" + number;

			if (type.equals("Fähre"))
				return "F" + number;
			if (type.equals("Fäh"))
				return "F" + number;
			if (type.equals("Schiff"))
				return "FSchiff" + number;
			if (type.equals("KAT")) // z.B. Friedrichshafen <-> Konstanz
				return "FKAT" + number;
			if (type.equals("AS")) // SyltShuttle
				return "FAS" + number;

			if (type.equals("ZahnR")) // Zahnradbahn, u.a. Zugspitzbahn
				return "RZahnR" + number;
			if (type.equals("Flug"))
				return "IFlug" + number;
			if (type.equals("E"))
			{
				if (P_NORMALIZE_LINE_SBAHN.matcher(number).matches())
					return "S" + number;
				else
					return "RE" + number;
			}
			if (type.equals("D"))
				return "?D" + number;

			throw new IllegalStateException("cannot normalize type '" + type + "' number '" + number + "' line '" + line + "'");
		}

		throw new IllegalStateException("cannot normalize line '" + line + "'");
	}
}
