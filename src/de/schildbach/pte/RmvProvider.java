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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class RmvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.RMV;
	public static final String OLD_NETWORK_ID = "mobil.rmv.de";
	private static final String API_BASE = "http://www.rmv.de/auskunft/bin/jp/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public RmvProvider()
	{
		super(null, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg", "Kassel", "Hanau", "Göttingen",
			"Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

	@Override
	protected String[] splitNameAndPlace(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitNameAndPlace(name);
	}

	private static final String NAME_URL = API_BASE + "stboard.exe/dox?input=";
	private static final Pattern P_SINGLE_NAME = Pattern.compile(".*<input type=\"hidden\" name=\"input\" value=\"(.+?)#(\\d+)\" />.*",
			Pattern.DOTALL);
	private static final Pattern P_MULTI_NAME = Pattern.compile("<a href=\"/auskunft/bin/jp/stboard.exe/dox.*?input=(\\d+)&.*?\">\\s*(.*?)\\s*</a>",
			Pattern.DOTALL);

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(NAME_URL + ParserUtils.urlEncode(constraint.toString()));

		final List<Location> results = new ArrayList<Location>();

		final Matcher mSingle = P_SINGLE_NAME.matcher(page);
		if (mSingle.matches())
		{
			results.add(new Location(LocationType.STATION, Integer.parseInt(mSingle.group(2)), null, ParserUtils.resolveEntities(mSingle.group(1))));
		}
		else
		{
			final Matcher mMulti = P_MULTI_NAME.matcher(page);
			while (mMulti.find())
				results.add(new Location(LocationType.STATION, Integer.parseInt(mMulti.group(1)), null, ParserUtils.resolveEntities(mMulti.group(2))));
		}

		return results;
	}

	private final String NEARBY_URI = API_BASE + "stboard.exe/dn?L=vs_rmv&distance=50&near&input=%s";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, ParserUtils.urlEncode(stationId));
	}

	private static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();
	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "115");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "100");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "85");
	}

	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("query.exe/dox");
		uri.append("?REQ0HafasInitialSelection=0");
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from)));
		if (via != null)
			uri.append("&REQ0JourneyStops1.0ID=").append(ParserUtils.urlEncode(locationId(via)));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to)));
		uri.append("&REQ0JourneyDep_Foot_speed=").append(WALKSPEED_MAP.get(walkSpeed));

		for (final char p : products.toCharArray())
		{
			if (p == 'I')
				uri.append("&REQ0JourneyProduct_prod_list_1=1000000000000000");
			if (p == 'R')
				uri.append("&REQ0JourneyProduct_prod_list_2=0110000000100000");
			if (p == 'S')
				uri.append("&REQ0JourneyProduct_prod_list_3=0001000000000000");
			if (p == 'U')
				uri.append("&REQ0JourneyProduct_prod_list_4=0000100000000000");
			if (p == 'T')
				uri.append("&REQ0JourneyProduct_prod_list_5=0000010000000000");
			if (p == 'B')
				uri.append("&REQ0JourneyProduct_prod_list_6=0000001101000000");
			if (p == 'F')
				uri.append("&REQ0JourneyProduct_prod_list_7=0000000010000000");
			// FIXME if (p == 'C')
		}

		uri.append("&start=Suchen");

		return uri.toString();
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile("(?:Geben Sie einen (Startort|Zielort) an.*?)?Bitte w&#228;hlen Sie aus der Liste",
			Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile(
			"<span class=\"tplight\">.*?<a href=\"http://www.rmv.de/auskunft/bin/jp/query.exe/dox.*?\">\\s*(.*?)\\s*</a>.*?</span>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile(
			"(mehrfach vorhanden oder identisch)|(keine geeigneten Haltestellen)|(keine Verbindung gefunden)|(derzeit nur Ausk&#252;nfte vom)",
			Pattern.CASE_INSENSITIVE);

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep, products, walkSpeed);
		final CharSequence page = ParserUtils.scrape(uri);

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
		}

		List<Location> fromAddresses = null;
		List<Location> viaAddresses = null;
		List<Location> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);

			final Matcher mAddresses = P_ADDRESSES.matcher(page);
			final List<Location> addresses = new ArrayList<Location>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(new Location(LocationType.ANY, 0, null, address + "!"));
			}

			if (type == null)
				viaAddresses = addresses;
			else if (type.equals("Startort"))
				fromAddresses = addresses;
			else if (type.equals("Zielort"))
				toAddresses = addresses;
			else
				throw new IllegalStateException(type);
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "Von: <b>(.*?)</b>.*?" // from
			+ "Nach: <b>(.*?)</b>.*?" // to
			+ "Datum: .., (\\d+\\..\\d+\\.\\d+).*?" // currentDate
			+ "(?:<a href=\"(http://www.rmv.de/auskunft/bin/jp/query.exe/dox[^\"]*?REQ0HafasScrollDir=2)\".*?)?" // linkEarlier
			+ "(?:<a href=\"(http://www.rmv.de/auskunft/bin/jp/query.exe/dox[^\"]*?REQ0HafasScrollDir=1)\".*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<p class=\"con(?:L|D)\">(.+?)</p>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "<a href=\"(http://www.rmv.de/auskunft/bin/jp/query.exe/dox[^\"]*?)\">" // link
			+ "(\\d+:\\d+)-(\\d+:\\d+)</a>" // departureTime, arrivalTime
			+ "(?:&nbsp;(.+?))?" // line
	, Pattern.DOTALL);

	@Override
	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		return queryConnections(uri, page);
	}

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
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
					final Connection connection = new Connection(extractConnectionId(link), link, departureTime, arrivalTime, 0, from.name, 0,
							to.name, null, null);
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

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?<p class=\"details\">\n" //
			+ "- <b>(.*?)</b> -.*?" // firstDeparture
			+ "Abfahrt: (\\d{2}\\.\\d{2}\\.\\d{2})<br />\n"// date
			+ "(?:Ankunft: \\d{2}\\.\\d{2}\\.\\d{2}<br />\n)?" //
			+ "Dauer: (\\d{1,2}:\\d{2})<br />.*?" // duration
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("/b> -\n(.*?- <b>[^<]*)<", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("<br />\n" //
			+ "(?:(.*?) nach (.*?)\n" // line, destination
			+ "<br />\n" //
			+ "ab (\\d{1,2}:\\d{2})\n" // departureTime
			+ "(?:(.*?)\\s*\n)?" // departurePosition
			+ "<br />\n" //
			+ "an (\\d{1,2}:\\d{2})\n" // arrivalTime
			+ "(?:(.*?)\\s*\n)?" // arrivalPosition
			+ "<br />\n|" //
			+ "<a href=[^>]*>\n" //
			+ "Fussweg\\s*\n" //
			+ "</a>\n" //
			+ "(\\d+) Min.<br />\n)" // footway
			+ "- <b>(.*?)" // arrival
	, Pattern.DOTALL);

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final String firstDeparture = ParserUtils.resolveEntities(mHead.group(1));
			final Date currentDate = ParserUtils.parseDate(mHead.group(2));
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			Date lastTime = currentDate;

			Date firstDepartureTime = null;
			Date lastArrivalTime = null;
			String lastArrival = null;
			Connection.Trip lastTrip = null;

			final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(page);
			while (mDetCoarse.find())
			{
				final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(mDetCoarse.group(1));
				if (mDetFine.matches())
				{
					final String departure = lastArrival != null ? lastArrival : firstDeparture;

					final String arrival = ParserUtils.resolveEntities(mDetFine.group(8));
					lastArrival = arrival;

					final String min = mDetFine.group(7);
					if (min == null)
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mDetFine.group(1)));

						final Location destination = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mDetFine.group(2)));

						final Date departureTime = upTime(lastTime, ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mDetFine.group(3))));

						final String departurePosition = ParserUtils.resolveEntities(mDetFine.group(4));

						final Date arrivalTime = upTime(lastTime, ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mDetFine.group(5))));

						final String arrivalPosition = ParserUtils.resolveEntities(mDetFine.group(6));

						lastTrip = new Connection.Trip(line, destination, departureTime, departurePosition, 0, departure, arrivalTime,
								arrivalPosition, 0, arrival, null);
						parts.add(lastTrip);

						if (firstDepartureTime == null)
							firstDepartureTime = departureTime;

						lastArrivalTime = arrivalTime;
					}
					else
					{
						if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
						{
							final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
							parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), 0, lastFootway.departure, 0, arrival));
						}
						else
						{
							parts.add(new Connection.Footway(Integer.parseInt(min), 0, departure, 0, arrival));
						}
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mDetCoarse.group(1) + "' on " + uri);
				}
			}

			return new GetConnectionDetailsResult(currentDate, new Connection(extractConnectionId(uri), uri, firstDepartureTime, lastArrivalTime, 0,
					firstDeparture, 0, lastArrival, parts, null));
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static Date upTime(final Date lastTime, Date time)
	{
		while (time.before(lastTime))
			time = ParserUtils.addDays(time, 1);

		lastTime.setTime(time.getTime());

		return time;
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final Date now = new Date();

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dox");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep"); // show departures
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50); // maximum taken from RMV site
		uri.append("&time=").append(TIME_FORMAT.format(now));
		uri.append("&date=").append(DATE_FORMAT.format(now));
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<p class=\"qs\">\n(.*?)</p>\n" // head
			+ "(.*?)<p class=\"links\">.*?" // departures
			+ "input=(\\d+).*?" // locationId
			+ "|(Eingabe kann nicht interpretiert|Eingabe ist nicht eindeutig)" // messages
			+ "|(Internal Error)" // messages
			+ ").*?", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" //
			+ "<b>(.*?)</b><br />.*?" //
			+ "Abfahrt (\\d+:\\d+).*?" //
			+ "Uhr, (\\d+\\.\\d+\\.\\d+).*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"sq\">\n(.+?)</p>", Pattern.DOTALL);
	static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<b>\\s*(.*?)\\s*</b>.*?" // line
			+ "&gt;&gt;\n" //
			+ "(.*?)\n" // destination
			+ "<br />\n" //
			+ "<b>(\\d{1,2}:\\d{2})</b>\n" // plannedTime
			+ "(?:keine Prognose verf&#252;gbar\n)?" //
			+ "(?:<span class=\"red\">ca\\. (\\d{1,2}:\\d{2})</span>\n)?" // predictedTime
			+ "(?:<span class=\"red\">heute (Gl\\. " + ParserUtils.P_PLATFORM + ")</span><br />\n)?" // predictedPosition
			+ "(?:(Gl\\. " + ParserUtils.P_PLATFORM + ")<br />\n)?" // position
			+ "(?:<span class=\"red\">([^>]*)</span>\n)?" // message
			+ "(?:<img src=\".+?\" alt=\"\" />\n<b>[^<]*</b>\n<br />\n)*" // (messages)
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(departuresQueryUri(stationId, maxDepartures));

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(Status.INVALID_STATION, Integer.parseInt(stationId));
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(Status.SERVICE_DOWN, Integer.parseInt(stationId));

			final int locationId = Integer.parseInt(mHeadCoarse.group(3));

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(3)),
						ParserUtils.parseTime(mHeadFine.group(2)));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
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
						final Date plannedTime = parsed.getTime();

						Date predictedTime = null;
						if (mDepFine.group(4) != null)
						{
							parsed.setTime(ParserUtils.parseTime(mDepFine.group(4)));
							parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
							parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
							parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
							if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								parsed.add(Calendar.DAY_OF_MONTH, 1);
							predictedTime = parsed.getTime();
						}

						final String position = ParserUtils.resolveEntities(ParserUtils.selectNotNull(mDepFine.group(5), mDepFine.group(6)));

						final Departure dep = new Departure(plannedTime, predictedTime, line, line != null ? lineColors(line) : null, null, position,
								0, destination, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				final String[] nameAndPlace = splitNameAndPlace(location);
				return new QueryDeparturesResult(new Location(LocationType.STATION, locationId, nameAndPlace[0], nameAndPlace[1]), departures, null);
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

	private static String normalizeLine(final String line)
	{
		if (line == null || line.length() == 0)
			return null;

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
			if (type.equals("DNZ")) // Basel-Minsk, Nacht
				return "IDNZ" + number;
			if (type.equals("D")) // Prag-Fulda
				return "ID" + number;
			if (type.equals("RB")) // RegionalBahn
				return "RRB" + number;
			if (type.equals("RE")) // RegionalExpress
				return "RRE" + number;
			if (type.equals("SE")) // StadtExpress
				return "RSE" + number;
			if (type.equals("R"))
				return "R" + number;
			if (type.equals("S"))
				return "SS" + number;
			if (type.equals("U"))
				return "UU" + number;
			if (type.equals("Tram"))
				return "T" + number;
			if (type.equals("RT")) // RegioTram
				return "TRT" + number;
			if (type.startsWith("Bus"))
				return "B" + type.substring(3) + number;
			if (type.startsWith("AST")) // Anruf-Sammel-Taxi
				return "BAST" + type.substring(3) + number;
			if (type.startsWith("ALT")) // Anruf-Linien-Taxi
				return "BALT" + type.substring(3) + number;
			if (type.equals("LTaxi"))
				return "BLTaxi" + number;
			if (type.equals("AT")) // AnschlußSammelTaxi
				return "BAT" + number;
			if (type.equals("SCH"))
				return "FSCH" + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		throw new UnsupportedOperationException();
	}
}
