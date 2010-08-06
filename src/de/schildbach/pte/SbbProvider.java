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
public class SbbProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "fahrplan.sbb.ch";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability != Capability.DEPARTURES)
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
	}

	private String connectionsQueryUri(final String from, final String via, final String to, final Date date, final boolean dep)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append("http://fahrplan.sbb.ch/bin/query.exe/dn");
		uri.append("?REQ0HafasInitialSelection=0");
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(from));
		uri.append("&REQ0JourneyStopsS0A=1");
		uri.append("&REQ0JourneyStopsS0ID=");
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via));
			uri.append("&REQ0JourneyStops1.0A=1");
		}
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(to));
		uri.append("&REQ0JourneyStopsZ0A=1");
		uri.append("&REQ0JourneyStopsZ0ID=");
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&start=Suchen");

		return uri.toString();
	}

	public CheckConnectionsQueryResult checkConnectionsQuery(final String from, final String via, final String to, final Date date, final boolean dep)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(final String queryUri) throws IOException
	{
		throw new UnsupportedOperationException();
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
		uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=1111111111000000");
		uri.append("&input=").append(stationId);

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD = Pattern.compile(".*?<p class=\"qs\">\n?" //
			+ "<b>(.*?)</b><br />\n?"//
			+ "Abfahrt (\\d+:\\d+)\n?"//
			+ "Uhr, (\\d+\\.\\d+\\.\\d+)\n?"//
			+ "</p>.*", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"sq\">(.+?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?<b>(.*?)</b>\n?" //
			+ "&gt;&gt;\n?" //
			+ "(.*?)\n?" //
			+ "<br />\n?" //
			+ "<b>(\\d+:\\d+)</b>.*", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_URI_STATION_ID = Pattern.compile("input=(\\d+)");

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mStationId = P_DEPARTURES_URI_STATION_ID.matcher(uri);
		if (!mStationId.find())
			throw new IllegalStateException(uri);
		final int stationId = Integer.parseInt(mStationId.group(1));

		// parse page
		final Matcher mHead = P_DEPARTURES_HEAD.matcher(page);
		if (mHead.matches())
		{
			final String location = ParserUtils.resolveEntities(mHead.group(1));
			final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHead.group(3)), ParserUtils.parseTime(mHead.group(2)));
			final List<Departure> departures = new ArrayList<Departure>(8);

			// choose matcher
			final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(page);
			while (mDepCoarse.find())
			{
				final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
				if (mDepFine.matches())
				{
					final Departure dep = parseDeparture(mDepFine, currentTime);
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
			return QueryDeparturesResult.NO_INFO;
		}
	}

	private static Departure parseDeparture(final Matcher mDep, final Date currentTime)
	{
		// line
		final String line = normalizeLine(ParserUtils.resolveEntities(mDep.group(1)));

		// destination
		final String destination = ParserUtils.resolveEntities(mDep.group(2));

		// time
		final Calendar current = new GregorianCalendar();
		current.setTime(currentTime);
		final Calendar parsed = new GregorianCalendar();
		parsed.setTime(ParserUtils.parseTime(mDep.group(3)));
		parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
		parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
		parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
		if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
			parsed.add(Calendar.DAY_OF_MONTH, 1);

		return new Departure(parsed.getTime(), line, line != null ? LINES.get(line.charAt(0)) : null, destination);
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüß]+)[\\s-]*(.*)");

	private static String normalizeLine(final String line)
	{
		// TODO IN Torino-Napoli
		// TODO TAL

		if (line == null || line.length() == 0)
			return null;

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2);

			if (type.equals("ICE")) // InterCityExpress
				return "IICE" + number;
			if (type.equals("IC")) // InterCity
				return "IIC" + number;
			if (type.equals("ICN")) // Intercity-Neigezug, Schweiz
				return "IICN" + number;
			if (type.equals("EC")) // EuroCity
				return "IEC" + number;
			if (type.equals("EN")) // EuroNight
				return "IEN" + number;
			if (type.equals("CNL")) // CityNightLine
				return "ICNL" + number;
			if (type.equals("TGV")) // Train à Grande Vitesse
				return "ITGV" + number;
			if (type.equals("THA")) // Thalys
				return "ITHA" + number;
			if (type.equals("X")) // InterConnex
				return "IX" + number;
			if (type.equals("RJ")) // RailJet, Österreichische Bundesbahnen
				return "IRJ" + number;
			if (type.equals("OEC")) // ÖBB-EuroCity
				return "IOEC" + number;
			if (type.equals("OIC")) // ÖBB-InterCity
				return "IOIC" + number;
			if (type.equals("ES")) // Eurostar Italia
				return "IES" + number;
			if (type.equals("EST")) // Eurostar Frankreich
				return "IEST" + number;
			if (type.equals("NZ")) // Nachtzug?
				return "INZ" + number;
			if (type.equals("R"))
				return "R" + number;
			if (type.equals("IR")) // InterRegio
				return "RIR" + number;
			if (type.equals("D")) // D-Zug?
				return "RD" + number;
			if (type.equals("E"))
				return "RE" + number;
			if (type.equals("RE")) // RegionalExpress
				return "RRE" + number;
			if (type.equals("IRE")) // Interregio Express
				return "RIRE" + number;
			if (type.equals("EXT"))
				return "REXT" + number;
			if (type.equals("ATZ"))
				return "RATZ" + number;
			if (type.equals("RSB"))
				return "RRSB" + number;
			if (type.equals("SN"))
				return "RSN" + number;
			if (type.equals("CAT")) // City Airport Train Wien
				return "RCAT" + number;
			if (type.equals("ALS")) // Spanien
				return "RALS" + number;
			if (type.equals("ARC")) // Spanien
				return "RARC" + number;
			if (type.equals("S"))
				return "SS" + number;
			if (type.equals("T"))
				return "T" + number;
			if (type.equals("Tram"))
				return "T" + number;
			if (type.equals("M")) // Lausanne
				return "TM" + number;
			if (type.startsWith("Bus"))
				return "B" + type.substring(3) + number;
			if (type.equals("BUS"))
				return "B" + number;
			if (type.equals("Tro"))
				return "BTro" + number;
			if (type.equals("NFB"))
				return "BNFB" + number;
			if (type.equals("TX"))
				return "BTX" + number;
			if (type.equals("BAT"))
				return "FBAT" + number;
			if (type.equals("BAV"))
				return "FBAV" + number;
			if (type.equals("FAE"))
				return "FFAE" + number;
			if (type.equals("KAT")) // z.B. Friedrichshafen <-> Konstanz
				return "FKAT" + number;
			if (type.equals("GB")) // Gondelbahn
				return "CGB" + number;
			if (type.equals("SL")) // Sessel-Lift
				return "CSL" + number;
			if (type.equals("LB"))
				return "CLB" + number;
			if (type.equals("FUN") || type.equals("Fun")) // Standseilbahn
				return "CFun" + number;

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
