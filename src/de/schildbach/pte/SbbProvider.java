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
public class SbbProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "fahrplan.sbb.ch";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS || capability == Capability.LOCATION_WGS84)
				return false;

		return true;
	}

	private static final String NAME_URL = "http://fahrplan.sbb.ch/bin/bhftafel.exe/dox?input=";
	private static final Pattern P_SINGLE_NAME = Pattern.compile(".*?<input type=\"hidden\" name=\"input\" value=\"(.+?)#(\\d+)\" />.*",
			Pattern.DOTALL);
	private static final Pattern P_MULTI_NAME = Pattern.compile("<a href=\"http://fahrplan\\.sbb\\.ch/bin/bhftafel\\.exe/dox\\?input=(\\d+).*?\">\n?" //
			+ "(.*?)\n?" //
			+ "</a>", Pattern.DOTALL);

	public List<String> autoCompleteStationName(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(NAME_URL + ParserUtils.urlEncode(constraint.toString()));

		final List<String> names = new ArrayList<String>();

		final Matcher mSingle = P_SINGLE_NAME.matcher(page);
		if (mSingle.matches())
		{
			names.add(ParserUtils.resolveEntities(mSingle.group(1)));
		}
		else
		{
			final Matcher mMulti = P_MULTI_NAME.matcher(page);
			while (mMulti.find())
				names.add(ParserUtils.resolveEntities(mMulti.group(2)));
		}

		return names;
	}

	public List<Station> nearbyStations(final double lat, final double lon, final int maxDistance, final int maxStations) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
		// final String uri = "http://fahrplan.sbb.ch/bin/extxml.exe/dn";
	}

	private String connectionsQueryUri(final String from, final String via, final String to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append("http://fahrplan.sbb.ch/bin/query.exe/dn");
		uri.append("?OK");
		uri.append("&REQ0HafasMaxChangeTime=120");
		uri.append("&REQ0HafasOptimize1=").append(ParserUtils.urlEncode("1:1"));
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0HafasSkipLongChanges=1");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(from));
		uri.append("&REQ0JourneyStopsS0A=7"); // any
		uri.append("&REQ0JourneyStopsS0ID=");
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via));
			uri.append("&REQ0JourneyStops1.0A=7"); // any
			uri.append("&REQ0JourneyStops1.0ID=");
		}
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(to));
		uri.append("&REQ0JourneyStopsZ0A=7"); // any
		uri.append("&REQ0JourneyStopsZ0ID=");
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&queryPageDisplayed=yes");
		uri.append("&start=Suchen");
		return uri.toString();
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\" accesskey=\"f\".*?>(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option.*?>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile("(keine Verbindung gefunden werden)");

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
		}

		List<String> fromAddresses = null;
		List<String> viaAddresses = null;
		List<String> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<String> addresses = new ArrayList<String>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(address);
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
			return new QueryConnectionsResult(QueryConnectionsResult.Status.AMBIGUOUS, fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(uri, page);
	}

	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "Von:.*?<td .*?>(.*?)</td>.*?" // from
			+ "Datum:.*?<td .*?>.., (\\d{2}\\.\\d{2}\\.\\d{2})</td>.*?" // date
			+ "Nach:.*?<td .*?>(.*?)</td>.*?" // to
			+ "(?:<a href=\"(http://fahrplan.sbb.ch/bin/query.exe/dn\\?seqnr=\\d+&ident=[\\w\\.]+&REQ0HafasScrollDir=2)\".*?>.*?)?" // linkEarlier
			+ "(?:<a href=\"(http://fahrplan.sbb.ch/bin/query.exe/dn\\?seqnr=\\d+&ident=[\\w\\.]+&REQ0HafasScrollDir=1)\".*?>.*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<tr class=\"(zebra-row-\\d)\">(.*?)</tr>\n?"//
			+ "<tr class=\"\\1\">(.+?)</tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "name=\"guiVCtrl_connection_detailsOut_select_([\\w-]+)\".*?" // id
			+ ".., (\\d{2}\\.\\d{2}\\.\\d{2}).*?" // departureDate
			+ "ab.*?(\\d{2}:\\d{2}).*?" // departureTime
			+ "duration.*?\\d{1,2}:\\d{2}.*?" //
			+ "(?:.., (\\d{2}\\.\\d{2}\\.\\d{2}).*?)?" // arrivalDate
			+ "an.*?(\\d{2}:\\d{2}).*?" // arrivalTime
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_DETAILS_COARSE = Pattern.compile("<a name=\"cis_([\\w-]+)\">.*?" // id
			+ "<table .*? class=\"hac_detail\">\n?<tr>.*?</tr>(.*?)</table>", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("<tr>(.*?class=\"stop-station-icon\".*?)</tr>\n?" //
			+ "<tr>(.*?class=\"stop-station-icon last\".*?)</tr>", Pattern.DOTALL);
	static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile(".*?" //
			+ "<td headers=\"stops-\\d+\" class=\"stop-station\">\n" //
			+ "(?:<a href=\"http://fahrplan\\.sbb\\.ch/bin/bhftafel\\.exe/dn.*?input=(\\d+)&.*?>)?" // departureId
			+ "([^\n<]*?)<.*?" // departure
			+ "<td headers=\"date-\\d+\"[^>]*>\n(?:.., (\\d{2}\\.\\d{2}\\.\\d{2})\n)?</td>.*?" // departureDate
			+ "<td headers=\"time-\\d+\"[^>]*>(?:(\\d{2}:\\d{2})|&nbsp;)</td>.*?" // departureTime
			+ "<td headers=\"platform-\\d+\"[^>]*>\\s*(.+?)?\\s*</td>.*?" // departurePosition
			+ "<img src=\"/img/2/products/(\\w+?)_pic.gif\".*?" // lineType
			+ "(?:<a href=\"http://fahrplan\\.sbb\\.ch/bin/traininfo\\.exe/dn.*?>\\s*(.*?)\\s*</a>|" // line
			+ "\n(\\d+) Min\\.).*?" // min
			+ "<td headers=\"stops-\\d+\" class=\"stop-station last\">\n" //
			+ "(?:<a href=\"http://fahrplan\\.sbb\\.ch/bin/bhftafel\\.exe/dn.*?input=(\\d+)&.*?>)?" // arrivalId
			+ "([^\n<]*?)<.*?" // arrival
			+ "<td headers=\"date-\\d+\"[^>]*>\n(?:.., (\\d{2}\\.\\d{2}\\.\\d{2})\n)?</td>.*?" // arrivalDate
			+ "<td headers=\"time-\\d+\"[^>]*>(?:(\\d{2}:\\d{2})|&nbsp;)</td>.*?" // arrivalTime
			+ "<td headers=\"platform-\\d+\"[^>]*>\\s*(.+?)?\\s*</td>.*?" // arrivalPosition
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final String from = ParserUtils.resolveEntities(mHead.group(1));
			final Date currentDate = ParserUtils.parseDate(mHead.group(2));
			final String to = ParserUtils.resolveEntities(mHead.group(3));
			final String linkEarlier = mHead.group(4) != null ? ParserUtils.resolveEntities(mHead.group(4)) : null;
			final String linkLater = mHead.group(5) != null ? ParserUtils.resolveEntities(mHead.group(5)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			while (mConCoarse.find())
			{
				final String set = mConCoarse.group(2) + mConCoarse.group(3);
				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(set);
				if (mConFine.matches())
				{
					final String id = mConFine.group(1);
					final Date departureDate = ParserUtils.parseDate(mConFine.group(2));
					final Date departureTime = ParserUtils.joinDateTime(departureDate, ParserUtils.parseTime(mConFine.group(3)));
					final Date arrivalDate = mConFine.group(4) != null ? ParserUtils.parseDate(mConFine.group(4)) : null;
					final Date arrivalTime = ParserUtils.joinDateTime(arrivalDate != null ? arrivalDate : departureDate, ParserUtils
							.parseTime(mConFine.group(5)));
					final String link = uri + "#" + id; // TODO use print link?

					final Connection connection = new Connection(id, link, departureTime, arrivalTime, null, null, 0, from, 0, to,
							new ArrayList<Connection.Part>(1));
					connections.add(connection);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + set + "' on " + uri);
				}
			}

			final Matcher mConDetCoarse = P_CONNECTIONS_DETAILS_COARSE.matcher(page);
			while (mConDetCoarse.find())
			{
				final String id = mConDetCoarse.group(1);
				final Connection connection = findConnection(connections, id);

				Date lastDate = null;

				final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(mConDetCoarse.group(2));
				while (mDetCoarse.find())
				{
					final String set = mDetCoarse.group(1) + mDetCoarse.group(2);

					final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(set);
					if (mDetFine.matches())
					{
						final String departure = ParserUtils.resolveEntities(mDetFine.group(2));

						Date departureDate = mDetFine.group(3) != null ? ParserUtils.parseDate(mDetFine.group(3)) : null;
						if (departureDate != null)
							lastDate = departureDate;
						else
							departureDate = lastDate;

						final String lineType = mDetFine.group(6);

						final String arrival = ParserUtils.resolveEntities(mDetFine.group(10));

						Date arrivalDate = mDetFine.group(11) != null ? ParserUtils.parseDate(mDetFine.group(11)) : null;
						if (arrivalDate != null)
							lastDate = arrivalDate;
						else
							arrivalDate = lastDate;

						if (!lineType.equals("fuss") && !lineType.equals("transfer"))
						{
							final int departureId = Integer.parseInt(mDetFine.group(1));

							final Date departureTime = ParserUtils.joinDateTime(departureDate, ParserUtils.parseTime(mDetFine.group(4)));

							final String departurePosition = mDetFine.group(5) != null ? ParserUtils.resolveEntities(mDetFine.group(5)) : null;

							final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDetFine.group(7)));

							final int arrivalId = Integer.parseInt(mDetFine.group(9));

							final Date arrivalTime = ParserUtils.joinDateTime(arrivalDate, ParserUtils.parseTime(mDetFine.group(12)));

							final String arrivalPosition = mDetFine.group(13) != null ? ParserUtils.resolveEntities(mDetFine.group(13)) : null;

							final Connection.Trip trip = new Connection.Trip(line, LINES.get(line.charAt(0)), null, departureTime, departurePosition,
									departureId, departure, arrivalTime, arrivalPosition, arrivalId, arrival);
							connection.parts.add(trip);
						}
						else
						{
							final int min = Integer.parseInt(mDetFine.group(8));

							final Connection.Footway footway = new Connection.Footway(min, departure, arrival);
							connection.parts.add(footway);
						}
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + set + "' on " + uri);
					}
				}
			}

			return new QueryConnectionsResult(uri, from, to, currentDate, linkEarlier, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private Connection findConnection(final List<Connection> connections, final String id)
	{
		for (final Connection connection : connections)
			if (connection.id.equals(id))
				return connection;

		return null;
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();

		uri.append("http://fahrplan.sbb.ch/bin/bhftafel.exe/dox");
		uri.append("?start=");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=1111111111000000");
		uri.append("&input=").append(stationId);

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<p class=\"qs\">\n(.+?)\n</p>.*?" //
			+ "(?:(.+)|(an dieser Haltestelle keines)).*?" //
			+ "<p class=\"links\">\n(.+?)\n</p>" //
			+ "|(Informationen zu)|(Verbindung zum Server konnte leider nicht hergestellt werden))" //
			+ ".*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" // 
			+ "<b>(.*?)</b><br />\n" // location
			+ "Abfahrt (\\d+:\\d+)\n" // time
			+ "Uhr, (\\d{2}\\.\\d{2}\\.\\d{2}).*?" // date
			+ "input=(\\d+).*?" // locationId
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"sq\">\n(.+?)\n</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<b>(.*?)</b>\n" // line
			+ "&gt;&gt;\n" //
			+ "(.*?)\n" // destination
			+ "<br />\n" //
			+ "<b>(\\d+:\\d+)</b>.*?" // time
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, Status.NO_INFO);
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult(uri, Status.SERVICE_DOWN);

			final String c = mHeadCoarse.group(1) + mHeadCoarse.group(4);
			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(c);
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(3)), ParserUtils
						.parseTime(mHeadFine.group(2)));
				final int locationId = Integer.parseInt(mHeadFine.group(4));
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

						final Departure dep = new Departure(parsed.getTime(), line, line != null ? LINES.get(line.charAt(0)) : null, 0, destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, locationId, location, currentTime, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + c + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßéèêáàâóòô]+)[\\s-]*(.*)");

	private static String normalizeLine(final String type, final String line)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

		final char normalizedType = normalizeType(type);
		if (normalizedType != 0)
			return normalizedType + strippedLine;

		throw new IllegalStateException("cannot normalize type " + type + " line " + line);
	}

	private static String normalizeLine(final String line)
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

	private static char normalizeType(final String type)
	{
		// TODO ARZ

		final String ucType = type.toUpperCase();

		if (ucType.equals("EC")) // EuroCity
			return 'I';
		if (ucType.equals("EN")) // EuroNight
			return 'I';
		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("ICN")) // Intercity-Neigezug, Schweiz
			return 'I';
		if (ucType.equals("CNL")) // CityNightLine
			return 'I';
		if (ucType.equals("THA")) // Thalys
			return 'I';
		if (ucType.equals("TGV")) // Train à Grande Vitesse
			return 'I';
		if (ucType.equals("RJ")) // RailJet, Österreichische Bundesbahnen
			return 'I';
		if (ucType.equals("OEC")) // ÖBB-EuroCity
			return 'I';
		if (ucType.equals("OIC")) // ÖBB-InterCity
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

		if (ucType.equals("R"))
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress
			return 'R';
		if (ucType.equals("IR"))
			return 'R';
		if (ucType.equals("IRE")) // Interregio Express
			return 'R';
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
		if (ucType.equals("ZUG"))
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

		if (ucType.equals("BUS"))
			return 'B';
		if (ucType.equals("TRO"))
			return 'B';
		if (ucType.equals("NFB"))
			return 'B';
		if (ucType.equals("NBU"))
			return 'B';
		if (ucType.equals("MIN"))
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
		LINES.put('?', new int[] { Color.DKGRAY, Color.WHITE });
	}

	public int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
