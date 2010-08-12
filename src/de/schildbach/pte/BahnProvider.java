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

/**
 * @author Andreas Schildbach
 */
public final class BahnProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "mobile.bahn.de";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(Capability... capabilities)
	{
		return true;
	}

	private static final String NAME_URL = "http://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?input=";
	private static final Pattern P_SINGLE_NAME = Pattern
			.compile(".*<input type=\"hidden\" name=\"input\" value=\"(.+?)#\\d+?\" />.*", Pattern.DOTALL);
	private static final Pattern P_MULTI_NAME = Pattern.compile("<option value=\".+?#\\d+?\">(.+?)</option>", Pattern.DOTALL);

	public List<String> autoCompleteStationName(CharSequence constraint) throws IOException
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
				names.add(ParserUtils.resolveEntities(mMulti.group(1)));
		}

		return names;
	}

	private final static Pattern P_NEARBY_STATIONS = Pattern
			.compile("<a class=\"uLine\" href=\".+?!X=(\\d+)!Y=(\\d+)!id=(\\d+)!dist=(\\d+).*?\">(.+?)</a>");

	public List<Station> nearbyStations(final double lat, final double lon, final int maxDistance, final int maxStations) throws IOException
	{
		final String url = "http://mobile.bahn.de/bin/mobil/query.exe/dox" + "?performLocating=2&tpl=stopsnear&look_maxdist="
				+ (maxDistance > 0 ? maxDistance : 5000) + "&look_stopclass=1023" + "&look_x=" + latLonToInt(lon) + "&look_y=" + latLonToInt(lat);
		final CharSequence page = ParserUtils.scrape(url);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher m = P_NEARBY_STATIONS.matcher(page);
		while (m.find())
		{
			final int sId = Integer.parseInt(m.group(3));

			final double sLon = latLonToDouble(Integer.parseInt(m.group(1)));
			final double sLat = latLonToDouble(Integer.parseInt(m.group(2)));
			final int sDist = Integer.parseInt(m.group(4));
			final String sName = ParserUtils.resolveEntities(m.group(5).trim());

			final Station station = new Station(sId, sName, sLat, sLon, sDist, null, null);
			stations.add(station);
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	private static int latLonToInt(double value)
	{
		return (int) (value * 1000000);
	}

	private static double latLonToDouble(int value)
	{
		return (double) value / 1000000;
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private String connectionsQueryUri(final String from, final String via, final String to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append("http://mobile.bahn.de/bin/mobil/query.exe/dox");
		uri.append("?REQ0HafasOptimize1=0:1");
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(from));
		uri.append("&REQ0JourneyStopsS0A=1");
		uri.append("&REQ0JourneyStopsS0ALocation=1");
		uri.append("&REQ0JourneyStopsS0AAddress=1");
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via));
			uri.append("&REQ0JourneyStops1.0A=1");
			uri.append("&REQ0JourneyStops1.0ALocation=1");
			uri.append("&REQ0JourneyStops1.0AAddress=1");
		}
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(to));
		uri.append("&REQ0JourneyStopsZ0A=1");
		uri.append("&REQ0JourneyStopsZ0ALocation=1");
		uri.append("&REQ0JourneyStopsZ0AAddress=1");
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
			"<select name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\" class=\"nofullwidth\">(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option.*?>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern
			.compile("(?:(zu dicht beieinander|mehrfach vorhanden oder identisch)|(leider konnte zu Ihrer Anfrage keine Verbindung gefunden werden))");

	public QueryConnectionsResult queryConnections(final String from, final String via, final String to, final Date date, final boolean dep)
			throws IOException
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

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*" //
			+ "von: <span class=\"bold\">(.*?)</span>.*?" // from
			+ "nach: <span class=\"bold\">(.*?)</span>.*?" // to
			+ "Datum: <span class=\"bold\">.., (.*?)</span>.*?" // currentDate
			+ "(?:<a href=\"(http://mobile.bahn.de/bin/mobil/query.exe/dox.*?)\">.*?Fr&#252;her.*?)?" // linkEarlier
			+ "(?:<a class=\"noBG\" href=\"(http://mobile.bahn.de/bin/mobil/query.exe/dox.*?)\">.*?Sp&#228;ter.*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<tr><td class=\"overview timelink\">(.+?)</td></tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?<a href=\"(http://mobile.bahn.de/bin/mobil/query.exe/dox.*?)\">" // url
			+ "(\\d+:\\d+)<br />(\\d+:\\d+)</a></td>.+?" // departureTime, arrivalTime
			+ "<td class=\"overview iphonepfeil\">(.*?)<br />.*?" // line
	, Pattern.DOTALL);

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
					if (line != null && !line.contains(","))
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

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*<span class=\"bold\">Verbindungsdetails</span>.*", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("<div class=\"haupt rline\">\n?(.+?)\n?</div>", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // departure
			+ "(?:" //
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // line
			+ "ab\\s+(?:<span.*?>.*?</span>)?\\s*(\\d+:\\d+)\\s*(?:<span.*?>.*?</span>)?" // departureTime
			+ "\\s*(Gl\\. .+?)?\\s*\n?" // departurePosition
			+ "am\\s+(\\d+\\.\\d+\\.\\d+).*?" // departureDate
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />.*?" // arrival
			+ "an\\s+(?:<span.*?>.*?</span>)?\\s*(\\d+:\\d+)\\s*(?:<span.*?>.*?</span>)?" // arrivalTime
			+ "\\s*(Gl\\. .+?)?\\s*\n?" // arrivalPosition
			+ "am\\s+(\\d+\\.\\d+\\.\\d+).*?" // arrivalDate
			+ "|" //
			+ "(\\d+) Min\\..*?" // footway
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />" // arrival
			+ ")", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_MESSAGES = Pattern
			.compile("Dauer: \\d+:\\d+|(Anschlusszug nicht mehr rechtzeitig)|(Anschlusszug jedoch erreicht werden)|(nur teilweise dargestellt)|(L&#228;ngerer Aufenthalt)|(&#228;quivalentem Bahnhof)|(Bahnhof wird mehrfach durchfahren)");

	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			Date firstDepartureTime = null;
			String firstDeparture = null;
			Date lastArrivalTime = null;
			String lastArrival = null;
			Connection.Trip lastTrip = null;

			final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(page);
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

						final String min = mDetFine.group(10);
						if (min == null)
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
							lastTrip = new Connection.Trip(line, line != null ? LINES.get(line.charAt(0)) : null, null, departureDateTime,
									departurePosition, 0, departure, arrivalDateTime, arrivalPosition, 0, arrival);
							parts.add(lastTrip);

							if (firstDepartureTime == null)
								firstDepartureTime = departureDateTime;

							lastArrival = arrival;
							lastArrivalTime = arrivalDateTime;
						}
						else
						{
							final String arrival = ParserUtils.resolveEntities(mDetFine.group(11));

							if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
							{
								final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
								parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), lastFootway.departure, arrival));
							}
							else
							{
								parts.add(new Connection.Footway(Integer.parseInt(min), departure, arrival));
							}

							lastArrival = arrival;
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
				throw new IllegalStateException("could not parse all parts of:\n" + page + "\n" + parts);

			return new GetConnectionDetailsResult(new Date(), new Connection(ParserUtils.extractId(uri), uri, firstDepartureTime, lastArrivalTime,
					null, null, 0, firstDeparture, 0, lastArrival, parts));
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://mobile.bahn.de/bin/mobil/bhftafel.exe/dox");
		uri.append("?start=");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&boardType=Abfahrt");
		uri.append("&productsFilter=1111111111000000");
		uri.append("&input=").append(stationId);
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(
			".*?<title>Deutsche Bahn - Abfahrt</title>.*?<body >(.*?Abfahrt.*?)</body>.*?", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<div class=\"haupt rline\">\n?<span class=\"bold\">\\n?(.+?)\\s*(?:- Aktuell)?\\n</span>.*?" // location
			+ "Abfahrt (\\d+:\\d+)\\n?Uhr, (\\d+\\.\\d+\\.\\d+).*?" // currentTime
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<div class=\"sqdetailsDep trow\">(.+?)</div>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<span class=\"bold\">(.*?)</span>.*?" // line
			+ "&gt;&gt;\\n?\\s*(.+?)\\s*\\n?<br />\\n?" // destination
			+ "<span class=\"bold\">(\\d+:\\d+)</span>.*?" // time
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_URI_STATION_ID = Pattern.compile("input=(\\d+)");

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mStationId = P_DEPARTURES_URI_STATION_ID.matcher(uri);
		if (!mStationId.find())
			throw new IllegalStateException(uri);
		final int stationId = Integer.parseInt(mStationId.group(1));

		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(3)), ParserUtils
						.parseTime(mHeadFine.group(2)));
				final List<Departure> departures = new ArrayList<Departure>(8);

				// choose matcher
				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(page);
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

				return new QueryDeparturesResult(uri, stationId, location, currentTime, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + uri);
			}
		}
		else
		{
			return QueryDeparturesResult.NO_INFO;
		}
	}

	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");
	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüß]+)[\\s-]*(.*)");
	private static final Pattern P_NORMALIZE_LINE_RUSSIA = Pattern.compile("(?:D\\s*)?(\\d{1,3}[A-Z]{2})");
	private static final Pattern P_NORMALIZE_LINE_SBAHN = Pattern.compile("S\\w*\\d+");

	private static String normalizeLine(final String line)
	{
		// TODO DPN Bad Reichenhall
		// TODO EIC Polen?
		// TODO EM East Midland? http://www.eastmidlandstrains.co.uk

		if (line == null || line.length() == 0)
			return null;

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
			if (type.equals("SC")) // SuperCity, cz
				return "ISC" + number;
			if (type.equals("EST")) // Eurostar Frankreich
				return "IEST" + number;
			if (type.equals("ES")) // Eurostar Italia
				return "IES" + number;
			if (type.equals("HOT")) // Spanien, Nachtzug?
				return "IHOT" + number;
			if (type.equals("R"))
				return "R" + number;
			if (type.equals("IR")) // InterRegio
				return "RIR" + number;
			if (type.equals("RB")) // RegionalBahn
				return "RRB" + number;
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
			if (type.equals("HzL")) // Hohenzollerische Landesbahn
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
			if (type.equals("MEr")) // metronom regional
				return "RMEr" + number;
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
			if (type.equals("CB")) // http://www.railfan.de/nebahn/morac.html
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
			if (type.equals("VEN")) // Rhenus Veniro
				return "RVEN" + number;
			if (type.equals("KD")) // Koleje Dolnośląskie
				return "RKD" + number;
			if (type.equals("ALS")) // Spanien
				return "RALS" + number;
			if (type.equals("TLG")) // Spanien
				return "RTLG" + number;
			if (type.equals("ARC")) // Spanien
				return "RARC" + number;
			if (type.equals("SKW")) // Polen
				return "RSKW" + number;
			if (type.equals("KM")) // Polen
				return "RKM" + number;
			if (type.equals("PCC")) // Polen
				return "RPCC" + number;
			if (type.equals("SKM")) // Polen
				return "RSKM" + number;
			if (type.equals("WKD")) // Warszawska Kolej Dojazdowa, Polen
				return "RWKD" + number;
			if (type.equals("LYN")) // Dänemark
				return "RLYN" + number;
			if (type.equals("EX")) // Norwegen
				return "REX" + number;
			if (type.equals("NZ")) // Norwegen
				return "RNZ" + number;
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
			if (type.equals("U"))
				return "UU" + number;
			if (type.equals("STR"))
				return "T" + number;
			if (type.equals("RT")) // RegioTram
				return "TRT" + number;
			if (type.equals("Schw")) // Schwebebahn, gilt als "Straßenbahn besonderer Bauart"
				return "TSchw" + number;
			if (type.startsWith("Bus"))
				return "B" + type.substring(3) + number;
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
		LINES.put('?', new int[] { Color.DKGRAY, Color.WHITE });
	}

	public int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
