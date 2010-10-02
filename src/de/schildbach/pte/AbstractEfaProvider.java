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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.QueryDeparturesResult.Status;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractEfaProvider implements NetworkProvider
{
	private static final Pattern P_AUTOCOMPLETE = Pattern.compile("" //
			+ "(?:" //
			+ "<itdOdvAssignedStop stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]* nameWithPlace=\"([^\"]*)\"" //
			+ "|" //
			+ "<odvNameElem [^>]* locality=\"([^\"]*)\"" //
			+ ")");

	protected abstract String autocompleteUri(final CharSequence constraint);

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(autocompleteUri(constraint));

		final List<Autocomplete> results = new ArrayList<Autocomplete>();

		final Matcher m = P_AUTOCOMPLETE.matcher(page);
		while (m.find())
		{
			if (m.group(1) != null)
			{
				final int sId = Integer.parseInt(m.group(1));
				// final double sLon = latLonToDouble(Integer.parseInt(mAutocomplete.group(2)));
				// final double sLat = latLonToDouble(Integer.parseInt(mAutocomplete.group(3)));
				final String sName = m.group(4).trim();
				results.add(new Autocomplete(LocationType.STATION, sId, sName));
			}
			else if (m.group(5) != null)
			{
				final String sName = m.group(5).trim();
				results.add(new Autocomplete(LocationType.ANY, 0, sName));
			}
		}

		return results;
	}

	private static final Pattern P_NEARBY = Pattern.compile("<itdOdvAssignedStop " // 
			+ "(?:stopID=\"(\\d+)\" x=\"(\\d+)\" y=\"(\\d+)\" mapName=\"WGS84\" [^>]*? nameWithPlace=\"([^\"]*)\" distance=\"(\\d+)\"" //
			+ "|distance=\"(\\d+)\" [^>]*? nameWithPlace=\"([^\"]*)\" [^>]*? stopID=\"(\\d+)\" [^>]*? x=\"(\\d+)\" y=\"(\\d+)\"" //
			+ ")");

	protected abstract String nearbyLatLonUri(int lat, int lon);

	protected abstract String nearbyStationUri(String stationId);

	public List<Station> nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri = null;
		if (lat != 0 || lon != 0)
			uri = nearbyLatLonUri(lat, lon);
		if (uri == null && stationId != null)
			uri = nearbyStationUri(stationId);
		if (uri == null)
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		final CharSequence page = ParserUtils.scrape(uri);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher mNearby = P_NEARBY.matcher(page);
		while (mNearby.find())
		{
			final boolean firstSyntax = mNearby.group(1) != null;
			final int sId = Integer.parseInt(mNearby.group(firstSyntax ? 1 : 8));
			final int sLon = Integer.parseInt(mNearby.group(firstSyntax ? 2 : 9));
			final int sLat = Integer.parseInt(mNearby.group(firstSyntax ? 3 : 10));
			final String sName = mNearby.group(firstSyntax ? 4 : 7).trim();
			final int sDist = Integer.parseInt(mNearby.group(firstSyntax ? 5 : 6));

			final Station station = new Station(sId, sName, sLat, sLon, sDist, null, null);
			stations.add(station);
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	private static final Pattern P_LINE_RE = Pattern.compile("RE\\d+");
	private static final Pattern P_LINE_RB = Pattern.compile("RB\\d+");
	private static final Pattern P_LINE_U = Pattern.compile("U\\d+");

	protected String parseLine(final String number, final String symbol, final String mot)
	{
		if (!number.equals(symbol))
			throw new IllegalStateException("number " + number + ", symbol " + symbol);

		final int t = Integer.parseInt(mot);

		if (t == 0)
		{
			final String[] parts = number.split(" ", 3);
			final String type = parts[0];
			final String num = parts.length >= 2 ? parts[1] : null;
			final String str = type + (num != null ? num : "");

			if (type.equals("EC")) // Eurocity
				return 'I' + str;
			if (type.equals("EN")) // Euronight
				return 'I' + str;
			if (type.equals("IC")) // Intercity
				return 'I' + str;
			if (type.equals("ICE")) // Intercity Express
				return 'I' + str;
			if (type.equals("CNL")) // City Night Line
				return 'I' + str;
			if (type.equals("THA")) // Thalys
				return 'I' + str;
			if (type.equals("TGV")) // TGV
				return 'I' + str;
			if (type.equals("RJ")) // railjet
				return 'I' + str;

			if (type.equals("IR")) // Interregio
				return 'R' + str;
			if (type.equals("IRE")) // Interregio-Express
				return 'R' + str;
			if (type.equals("RE")) // Regional-Express
				return 'R' + str;
			if (P_LINE_RE.matcher(type).matches())
				return 'R' + str;
			if (type.equals("RB")) // Regionalbahn
				return 'R' + str;
			if (P_LINE_RB.matcher(type).matches())
				return 'R' + str;
			if (type.equals("R")) // Regionalzug
				return 'R' + str;
			if (type.equals("D")) // Schnellzug
				return 'R' + str;
			if (type.equals("WFB")) // Westfalenbahn
				return 'R' + str;
			if (type.equals("NWB")) // NordWestBahn
				return 'R' + str;
			if (type.equals("ME")) // Metronom
				return 'R' + str;
			if (type.equals("ERB")) // eurobahn
				return 'R' + str;
			if (type.equals("CAN")) // cantus
				return 'R' + str;
			if (type.equals("HEX")) // Veolia Verkehr Sachsen-Anhalt
				return 'R' + str;
			if (type.equals("EB")) // Erfurter Bahn
				return 'R' + str;
			if (type.equals("MRB")) // Mittelrheinbahn
				return 'R' + str;
			if (type.equals("ABR")) // ABELLIO Rail NRW
				return 'R' + str;
			if (type.equals("NEB")) // Niederbarnimer Eisenbahn
				return 'R' + str;
			if (type.equals("OE")) // Ostdeutsche Eisenbahn
				return 'R' + str;
			if (type.equals("MR")) // Märkische Regiobahn
				return 'R' + str;
			if (type.equals("OLA")) // Ostseeland Verkehr
				return 'R' + str;
			if (type.equals("UBB")) // Usedomer Bäderbahn
				return 'R' + str;
			if (type.equals("EVB")) // Elbe-Weser
				return 'R' + str;
			if (type.equals("PEG")) // Prignitzer Eisenbahngesellschaft
				return 'R' + str;
			if (type.equals("RTB")) // Rurtalbahn
				return 'R' + str;
			if (type.equals("STB")) // Süd-Thüringen-Bahn
				return 'R' + str;
			if (type.equals("HTB")) // Hellertalbahn
				return 'R' + str;
			if (type.equals("VBG")) // Vogtlandbahn
				return 'R' + str;
			if (type.equals("VX")) // Vogtland Express
				return 'R' + str;
			if (type.equals("CB")) // City-Bahn Chemnitz
				return 'R' + str;
			if (type.equals("VEC")) // VECTUS Verkehrsgesellschaft
				return 'R' + str;
			if (type.equals("HzL")) // Hohenzollerische Landesbahn
				return 'R' + str;
			if (type.equals("OSB")) // Ortenau-S-Bahn
				return 'R' + str;
			if (type.equals("SBB")) // SBB
				return 'R' + str;
			if (type.equals("MBB")) // Mecklenburgische Bäderbahn Molli
				return 'R' + str;
			if (type.equals("OS")) // Regionalbahn
				return 'R' + str;
			if (type.equals("SP"))
				return 'R' + str;
			if (type.equals("Dab")) // Daadetalbahn
				return 'R' + str;
			if (type.equals("FEG")) // Freiberger Eisenbahngesellschaft
				return 'R' + str;
			if (type.equals("ARR")) // ARRIVA
				return 'R' + str;
			if (type.equals("HSB")) // Harzer Schmalspurbahn
				return 'R' + str;
			if (type.equals("SBE")) // Sächsisch-Böhmische Eisenbahngesellschaft
				return 'R' + str;
			if (type.equals("ALX")) // Arriva-Länderbahn-Express
				return 'R' + str;
			if (type.equals("MEr")) // metronom regional
				return 'R' + str;
			if (type.equals("AKN")) // AKN Eisenbahn
				return 'R' + str;
			if (type.equals("ZUG")) // Regionalbahn
				return 'R' + str;
			if (type.equals("SOE")) // Sächsisch-Oberlausitzer Eisenbahngesellschaft
				return 'R' + str;
			if (type.equals("VIA")) // VIAS
				return 'R' + str;
			if (type.equals("BRB")) // Bayerische Regiobahn
				return 'R' + str;
			if (type.equals("BLB")) // Berchtesgadener Land Bahn
				return 'R' + str;
			if (type.equals("HLB")) // Hessische Landesbahn
				return 'R' + str;
			if (type.equals("NOB")) // NordOstseeBahn
				return 'R' + str;
			if (type.equals("WEG")) // Wieslauftalbahn
				return 'R' + str;
			if (type.equals("NBE")) // Nordbahn Eisenbahngesellschaft
				return 'R' + str;
			if (type.equals("VEN")) // Rhenus Veniro
				return 'R' + str;
			if (type.equals("DPN")) // Nahreisezug
				return 'R' + str;
			if (type.equals("SHB")) // Schleswig-Holstein-Bahn
				return 'R' + str;
			if (type.equals("RBG")) // Regental Bahnbetriebs GmbH
				return 'R' + str;
			if (type.equals("BOB")) // Bayerische Oberlandbahn
				return 'R' + str;
			if (type.equals("SWE")) // Südwestdeutsche Verkehrs AG
				return 'R' + str;
			if (type.equals("VE")) // Vetter
				return 'R' + str;
			if (type.equals("SDG")) // Sächsische Dampfeisenbahngesellschaft
				return 'R' + str;
			if (type.equals("PRE")) // Pressnitztalbahn
				return 'R' + str;

			if (type.equals("BSB")) // Breisgau-S-Bahn
				return 'S' + str;

			if (P_LINE_U.matcher(type).matches())
				return 'U' + str;

			if (type.equals("RT")) // RegioTram
				return 'T' + str;
			if (type.equals("STR")) // Nordhausen
				return 'T' + str;

			throw new IllegalArgumentException("cannot normalize: " + number);
		}
		if (t == 1)
			return 'S' + number;
		if (t == 2)
			return 'U' + number;
		if (t == 3 || t == 4)
			return 'T' + number;
		if (t == 5 || t == 6 || t == 7 || t == 10)
			return 'B' + number;
		if (t == 8)
			return 'C' + number;
		if (t == 9)
			return 'F' + number;
		if (t == 11)
			return '?' + number;

		throw new IllegalStateException("cannot normalize mot '" + mot + "' number '" + number + "'");
	}

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		try
		{
			final CharSequence page = ParserUtils.scrape(uri);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(new StringReader(page.toString()));

			XmlPullUtil.jumpToStartTag(pp, null, "itdOdvName");
			final String nameState = pp.getAttributeValue(null, "state");
			if (nameState.equals("identified"))
			{
				XmlPullUtil.jumpToStartTag(pp, null, "odvNameElem");
				final int locationId = Integer.parseInt(pp.getAttributeValue(null, "stopID"));

				final String location = normalizeLocationName(pp.nextText());

				final Calendar departureTime = new GregorianCalendar();
				final List<Departure> departures = new ArrayList<Departure>(8);

				XmlPullUtil.jumpToStartTag(pp, null, "itdDepartureList");
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDeparture"))
				{
					if (Integer.parseInt(pp.getAttributeValue(null, "stopID")) == locationId)
					{
						String position = pp.getAttributeValue(null, "platform");
						if (position != null)
						{
							if (position.length() != 0)
								position = "Gl. " + position;
							else
								position = null;
						}

						departureTime.clear();

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDateTime"))
							throw new IllegalStateException("itdDateTime not found:" + pp.getPositionDescription());

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDate"))
							throw new IllegalStateException("itdDate not found:" + pp.getPositionDescription());
						processItdDate(pp, departureTime);
						XmlPullUtil.skipRestOfTree(pp);

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdTime"))
							throw new IllegalStateException("itdTime not found:" + pp.getPositionDescription());
						processItdTime(pp, departureTime);
						XmlPullUtil.skipRestOfTree(pp);

						XmlPullUtil.skipRestOfTree(pp);

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdServingLine"))
							throw new IllegalStateException("itdServingLine not found:" + pp.getPositionDescription());
						final String line = parseLine(pp.getAttributeValue(null, "number"), pp.getAttributeValue(null, "symbol"), pp
								.getAttributeValue(null, "motType"));
						final boolean isRealtime = pp.getAttributeValue(null, "realtime").equals("1");
						final String destination = normalizeLocationName(pp.getAttributeValue(null, "direction"));
						final int destinationId = Integer.parseInt(pp.getAttributeValue(null, "destID"));
						XmlPullUtil.skipRestOfTree(pp);

						departures.add(new Departure(!isRealtime ? departureTime.getTime() : null, isRealtime ? departureTime.getTime() : null, line,
								lineColors(line), null, position, destinationId, destination, null));
					}

					XmlPullUtil.skipRestOfTree(pp);
				}

				return new QueryDeparturesResult(uri, locationId, location, departures);
			}
			else if (nameState.equals("notidentified"))
			{
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			}
			else
			{
				throw new RuntimeException("unknown nameState '" + nameState + "' on " + uri);
			}
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
	}

	private void processItdDate(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, "itdDate");
		calendar.set(Calendar.YEAR, Integer.parseInt(pp.getAttributeValue(null, "year")));
		calendar.set(Calendar.MONTH, Integer.parseInt(pp.getAttributeValue(null, "month")) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(pp.getAttributeValue(null, "day")));
	}

	private void processItdTime(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		pp.require(XmlPullParser.START_TAG, null, "itdTime");
		calendar.set(Calendar.HOUR, Integer.parseInt(pp.getAttributeValue(null, "hour")));
		calendar.set(Calendar.MINUTE, Integer.parseInt(pp.getAttributeValue(null, "minute")));
	}

	private static final Pattern P_STATION_NAME_WHITESPACE = Pattern.compile("\\s+");

	private static String normalizeLocationName(final String name)
	{
		return P_STATION_NAME_WHITESPACE.matcher(name).replaceAll(" ");
	}

	protected static double latLonToDouble(final int value)
	{
		return (double) value / 1000000;
	}
}
