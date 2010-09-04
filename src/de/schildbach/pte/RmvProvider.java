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

import de.schildbach.pte.QueryDeparturesResult.Status;

/**
 * @author Andreas Schildbach
 */
public class RmvProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "mobil.rmv.de";
	public static final String NETWORK_ID_ALT = "www.rmv.de";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.LOCATION_WGS84)
				return false;

		return true;
	}

	private static final String NAME_URL = "http://www.rmv.de/auskunft/bin/jp/stboard.exe/dox?input=";
	private static final Pattern P_SINGLE_NAME = Pattern.compile(".*<input type=\"hidden\" name=\"input\" value=\"(.+?)#(\\d+)\" />.*",
			Pattern.DOTALL);
	private static final Pattern P_MULTI_NAME = Pattern.compile("<a href=\"/auskunft/bin/jp/stboard.exe/dox.*?input=(\\d+)&.*?\">\\s*(.*?)\\s*</a>",
			Pattern.DOTALL);

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(NAME_URL + ParserUtils.urlEncode(constraint.toString()));

		final List<Autocomplete> results = new ArrayList<Autocomplete>();

		final Matcher mSingle = P_SINGLE_NAME.matcher(page);
		if (mSingle.matches())
		{
			results.add(new Autocomplete(Integer.parseInt(mSingle.group(2)), ParserUtils.resolveEntities(mSingle.group(1))));
		}
		else
		{
			final Matcher mMulti = P_MULTI_NAME.matcher(page);
			while (mMulti.find())
				results.add(new Autocomplete(Integer.parseInt(mMulti.group(1)), ParserUtils.resolveEntities(mMulti.group(2))));
		}

		return results;
	}

	private final static Pattern P_NEARBY_STATIONS = Pattern.compile("<a href=\"/auskunft/bin/jp/stboard.exe/dox.+?input=(\\d+).*?\">\\n"
			+ "(.+?)\\s*\\((\\d+) m/[A-Z]+\\)\\n</a>", Pattern.DOTALL);

	public List<Station> nearbyStations(final double lat, final double lon, final int maxDistance, final int maxStations) throws IOException
	{
		final String url = "http://www.rmv.de/auskunft/bin/jp/stboard.exe/dox?input=" + lat + "%20" + lon;
		final CharSequence page = ParserUtils.scrape(url);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher m = P_NEARBY_STATIONS.matcher(page);
		while (m.find())
		{
			final int sId = Integer.parseInt(m.group(1));
			final String sName = ParserUtils.resolveEntities(m.group(2));
			final int sDist = Integer.parseInt(m.group(3));

			final Station station = new Station(sId, sName, 0, 0, sDist, null, null);
			stations.add(station);
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	private static Pattern P_STATION_LOCATION = Pattern
			.compile("REQMapRoute0\\.Location0\\.X=(\\d+)&REQMapRoute0\\.Location0\\.Y=(\\d+)&REQMapRoute0\\.Location0\\.Name=(.+?)\"");

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		final String uri = "http://www.rmv.de/auskunft/bin/jp/stboard.exe/dn?L=vs_rmv&selectDate=today&time=now&showStBoard=yes&boardType=dep&maxJourneys=10&start&input="
				+ stationId;

		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher m = P_STATION_LOCATION.matcher(page);
		if (m.find())
		{
			final double lon = latLonToDouble(Integer.parseInt(m.group(1)));
			final double lat = latLonToDouble(Integer.parseInt(m.group(2)));
			final String name = ParserUtils.resolveEntities(m.group(3));

			return new StationLocationResult(lat, lon, name);
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static double latLonToDouble(int value)
	{
		return (double) value / 1000000;
	}

	private String connectionsQueryUri(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append("http://www.rmv.de/auskunft/bin/jp/query.exe/dox");
		uri.append("?REQ0HafasInitialSelection=0");
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(from));
		uri.append("&REQ0JourneyStopsS0A=").append(locationType(fromType));
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(to));
		uri.append("&REQ0JourneyStopsZ0A=").append(locationType(toType));
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via));
			uri.append("&REQ0JourneyStops1.0A=").append(locationType(viaType));
		}
		uri.append("&start=Suchen");

		return uri.toString();
	}

	private static int locationType(final LocationType locationType)
	{
		if (locationType == LocationType.STATION)
			return 1;
		if (locationType == LocationType.ADDRESS)
			return 2;
		if (locationType == LocationType.ANY)
			return 255;
		throw new IllegalArgumentException(locationType.toString());
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile("(?:Geben Sie einen (Startort|Zielort) an.*?)?Bitte w&#228;hlen Sie aus der Liste",
			Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile(
			"<span class=\"tplight\">.*?<a href=\"http://www.rmv.de/auskunft/bin/jp/query.exe/dox.*?\">\\s*(.*?)\\s*</a>.*?</span>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile(
			"(?:(mehrfach vorhanden oder identisch)|(keine Verbindung gefunden werden))", Pattern.CASE_INSENSITIVE);

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep) throws IOException
	{
		final String uri = connectionsQueryUri(fromType, from, viaType, via, toType, to, date, dep);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.TOO_CLOSE;
			if (mError.group(2) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
		}

		List<String> fromAddresses = null;
		List<String> viaAddresses = null;
		List<String> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);

			final Matcher mAddresses = P_ADDRESSES.matcher(page);
			final List<String> addresses = new ArrayList<String>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(address);
			}

			if (type == null)
				viaAddresses = addresses;
			else if (type.equals("Startort"))
				fromAddresses = addresses;
			else if (type.equals("Zielort"))
				toAddresses = addresses;
			else
				throw new IOException(type);
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(QueryConnectionsResult.Status.AMBIGUOUS, fromAddresses, viaAddresses, toAddresses);
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
			+ "(\\d+:\\d+)-(\\d+:\\d+)</a>" //
			+ "(?:&nbsp;(.+?))?" //
	, Pattern.DOTALL);

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
			final String from = ParserUtils.resolveEntities(mHead.group(1));
			final String to = ParserUtils.resolveEntities(mHead.group(2));
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
					String line = mConFine.group(4);
					if (line != null && !line.endsWith("Um."))
						line = normalizeLine(line);
					else
						line = null;
					final Connection connection = new Connection(ParserUtils.extractId(link), link, departureTime, arrivalTime, line,
							line != null ? LINES.get(line.charAt(0)) : null, 0, from, 0, to, null);
					connections.add(connection);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(1) + "' on " + uri);
				}
			}

			return new QueryConnectionsResult(uri, from, to, currentDate, linkEarlier, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?<p class=\"details\">\n?" //
			+ "- <b>(.*?)</b> -.*?" // firstDeparture
			+ "Abfahrt: (\\d+\\.\\d+\\.\\d+)<br />\n?"// date
			+ "Dauer: (\\d+:\\d+)<br />.*?" // duration
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("/b> -\n?(.*?- <b>.*?)<", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("<br />\n?" //
			+ "(?:(.*?) nach (.*?)\n?" // line, destination
			+ "<br />\n?" //
			+ "ab (\\d+:\\d+)\n?" // departureTime
			+ "(.*?)\\s*\n?" // departurePosition
			+ "<br />\n?" //
			+ "an (\\d+:\\d+)\n?" // arrivalTime
			+ "(.*?)\\s*\n?" // arrivalPosition
			+ "<br />\n?|" //
			+ "<a href=\".*?\">\n?" //
			+ "Fussweg\\s*\n?" //
			+ "</a>\n?" //
			+ "(\\d+) Min.<br />\n?)" // footway
			+ "- <b>(.*?)" // arrival
	, Pattern.DOTALL);

	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final String firstDeparture = ParserUtils.resolveEntities(mHead.group(1));
			final Date currentDate = ParserUtils.parseDate(mHead.group(2));
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

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

						final String destination = ParserUtils.resolveEntities(mDetFine.group(2));

						final Date departureTime = ParserUtils.parseTime(mDetFine.group(3));

						final String departurePosition = ParserUtils.resolveEntities(mDetFine.group(4));

						// final Date departureDate = ParserUtils.parseDate(mDet.group(5));

						final Date arrivalTime = ParserUtils.parseTime(mDetFine.group(5));

						final String arrivalPosition = ParserUtils.resolveEntities(mDetFine.group(6));

						// final Date arrivalDate = ParserUtils.parseDate(mDet.group(9));

						final Date departureDateTime = ParserUtils.joinDateTime(currentDate, departureTime);
						final Date arrivalDateTime = ParserUtils.joinDateTime(currentDate, arrivalTime);
						lastTrip = new Connection.Trip(line, line != null ? LINES.get(line.charAt(0)) : null, destination, departureDateTime,
								departurePosition, 0, departure, arrivalDateTime, arrivalPosition, 0, arrival);
						parts.add(lastTrip);

						if (firstDepartureTime == null)
							firstDepartureTime = departureDateTime;

						lastArrivalTime = arrivalDateTime;
					}
					else
					{
						if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
						{
							final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
							parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), lastFootway.departure, arrival));
						}
						else
						{
							parts.add(new Connection.Footway(Integer.parseInt(min), departure, arrival));
						}
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mDetCoarse.group(1) + "' on " + uri);
				}
			}

			return new GetConnectionDetailsResult(currentDate, new Connection(ParserUtils.extractId(uri), uri, firstDepartureTime, lastArrivalTime,
					null, null, 0, firstDeparture, 0, lastArrival, parts));
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final Date now = new Date();

		final StringBuilder uri = new StringBuilder();
		uri.append("http://www.rmv.de/auskunft/bin/jp/stboard.exe/dox");
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
			+ "|(Eingabe kann nicht interpretiert)" // messages
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
			+ "(?:<span class=\"red\">heute Gl\\. " + ParserUtils.P_PLATFORM + "</span><br />\n)?" // predictedPosition
			+ "(?:(Gl\\. " + ParserUtils.P_PLATFORM + ")<br />\n)?" // position
			+ "(?:<img src=\".+?\" alt=\"\" />\n<b>.+?</b>\n<br />\n)*" // (messages)
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(uri, Status.SERVICE_DOWN);

			final int stationId = Integer.parseInt(mHeadCoarse.group(3));

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(3)), ParserUtils
						.parseTime(mHeadFine.group(2)));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						// line
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						// destination
						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						// time
						final Calendar current = new GregorianCalendar();
						current.setTime(currentTime);
						final Calendar parsed = new GregorianCalendar();

						// plannedTime
						parsed.setTime(ParserUtils.parseTime(mDepFine.group(3)));
						parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
						parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
						parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
						if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsed.add(Calendar.DAY_OF_MONTH, 1);
						final Date plannedTime = parsed.getTime();

						// predictedTime
						if (mDepFine.group(4) != null)
						{
							parsed.setTime(ParserUtils.parseTime(mDepFine.group(4)));
							parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
							parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
							parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
							if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								parsed.add(Calendar.DAY_OF_MONTH, 1);
							final Date predictedTime = parsed.getTime();
						}

						// position
						final String position = ParserUtils.resolveEntities(mDepFine.group(5));

						final Departure dep = new Departure(plannedTime, line, line != null ? LINES.get(line.charAt(0)) : null, position, 0,
								destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, stationId, location, currentTime, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû]+)[\\s-]*(.*)");

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
	}

	public int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
