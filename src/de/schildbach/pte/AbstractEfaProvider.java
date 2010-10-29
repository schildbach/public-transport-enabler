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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.dto.QueryConnectionsResult.Status;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractEfaProvider implements NetworkProvider
{
	private static final String DEFAULT_ENCODING = "ISO-8859-1";

	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("Europe/Berlin");
	}

	protected abstract String autocompleteUri(final CharSequence constraint);

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = autocompleteUri(constraint);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri);

			final List<Location> results = new ArrayList<Location>();

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertItdRequest(pp);

			// parse odv name elements
			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"origin".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"origin\" />");
			if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName"))
				throw new IllegalStateException("cannot find <itdOdvName />");
			final String nameState = pp.getAttributeValue(null, "state");
			if ("identified".equals(nameState) || "list".equals(nameState))
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					results.add(processOdvNameElem(pp));

			// parse assigned stops
			if (XmlPullUtil.jumpToStartTag(pp, null, "itdOdvAssignedStops"))
			{
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvAssignedStop"))
				{
					final Location location = processItdOdvAssignedStop(pp);
					if (!results.contains(location))
						results.add(location);

					XmlPullUtil.skipRestOfTree(pp);
				}
			}

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		catch (final SocketTimeoutException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private Location processOdvNameElem(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		final String anyType = pp.getAttributeValue(null, "anyType");
		final String idStr = pp.getAttributeValue(null, "id");
		final String stopIdStr = pp.getAttributeValue(null, "stopID");
		final String poiIdStr = pp.getAttributeValue(null, "poiID");
		final String streetIdStr = pp.getAttributeValue(null, "streetID");
		int lat = 0, lon = 0;
		if ("WGS84".equals(pp.getAttributeValue(null, "mapName")))
		{
			lat = Integer.parseInt(pp.getAttributeValue(null, "y"));
			lon = Integer.parseInt(pp.getAttributeValue(null, "x"));
		}

		LocationType type;
		int id;
		if ("stop".equals(anyType))
		{
			type = LocationType.STATION;
			id = Integer.parseInt(idStr);
		}
		else if ("poi".equals(anyType))
		{
			type = LocationType.POI;
			id = Integer.parseInt(idStr);
		}
		else if ("loc".equals(anyType))
		{
			type = LocationType.ANY;
			id = 0;
		}
		else if ("street".equals(anyType) || "address".equals(anyType) || "singlehouse".equals(anyType))
		{
			type = LocationType.ADDRESS;
			id = 0;
		}
		else if (stopIdStr != null)
		{
			type = LocationType.STATION;
			id = Integer.parseInt(stopIdStr);
		}
		else if (stopIdStr == null && idStr == null && (lat != 0 || lon != 0))
		{
			type = LocationType.ADDRESS;
			id = 0;
		}
		else if (poiIdStr != null)
		{
			type = LocationType.POI;
			id = Integer.parseInt(poiIdStr);
		}
		else if (streetIdStr != null)
		{
			type = LocationType.ADDRESS;
			id = Integer.parseInt(streetIdStr);
		}
		else
		{
			throw new IllegalArgumentException("unknown type: " + anyType + " " + idStr + " " + stopIdStr);
		}

		final String name = normalizeLocationName(pp.nextText());

		return new Location(type, id, lat, lon, name);
	}

	private Location processItdOdvAssignedStop(final XmlPullParser pp)
	{
		final int id = Integer.parseInt(pp.getAttributeValue(null, "stopID"));
		int lat = 0, lon = 0;
		if ("WGS84".equals(pp.getAttributeValue(null, "mapName")))
		{
			lat = Integer.parseInt(pp.getAttributeValue(null, "y"));
			lon = Integer.parseInt(pp.getAttributeValue(null, "x"));
		}
		final String name = normalizeLocationName(pp.getAttributeValue(null, "nameWithPlace"));
		return new Location(LocationType.STATION, id, lat, lon, name);
	}

	protected abstract String nearbyLatLonUri(int lat, int lon);

	protected abstract String nearbyStationUri(String stationId);

	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri = null;
		if (uri == null && stationId != null)
			uri = nearbyStationUri(stationId);
		if (uri == null && (lat != 0 || lon != 0))
			uri = nearbyLatLonUri(lat, lon);
		if (uri == null)
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertItdRequest(pp);

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"dm".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"dm\" />");
			if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName"))
				throw new IllegalStateException("cannot find <itdOdvName />");
			final String nameState = pp.getAttributeValue(null, "state");
			if ("identified".equals(nameState))
			{
				final List<Station> stations = new ArrayList<Station>();

				Station ownStation = null;
				XmlPullUtil.jumpToStartTag(pp, null, "odvNameElem");
				String parsedOwnLocationIdStr = pp.getAttributeValue(null, "stopID");
				if (parsedOwnLocationIdStr == null)
					parsedOwnLocationIdStr = pp.getAttributeValue(null, "id");
				if (parsedOwnLocationIdStr != null)
				{
					final int parsedOwnLocationId = Integer.parseInt(parsedOwnLocationIdStr);
					int parsedOwnLat = 0, parsedOwnLon = 0;
					final String mapName = pp.getAttributeValue(null, "mapName");
					if (mapName != null)
					{
						if (!"WGS84".equals(mapName))
							throw new IllegalStateException("unknown mapName: " + mapName);
						parsedOwnLon = Integer.parseInt(pp.getAttributeValue(null, "x"));
						parsedOwnLat = Integer.parseInt(pp.getAttributeValue(null, "y"));
					}
					final String parsedOwnLocation = normalizeLocationName(pp.nextText()); // FIXME evtl. nur optional?
					ownStation = new Station(parsedOwnLocationId, parsedOwnLocation, parsedOwnLat, parsedOwnLon, 0, null, null);
				}

				if (XmlPullUtil.jumpToStartTag(pp, null, "itdOdvAssignedStops"))
				{
					while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvAssignedStop"))
					{
						final String parsedMapName = pp.getAttributeValue(null, "mapName");
						if (parsedMapName != null)
						{
							if (!"WGS84".equals(parsedMapName))
								throw new IllegalStateException("unknown mapName: " + parsedMapName);

							final int parsedLocationId = Integer.parseInt(pp.getAttributeValue(null, "stopID"));
							final String parsedName = normalizeLocationName(pp.getAttributeValue(null, "nameWithPlace"));
							final int parsedLon = Integer.parseInt(pp.getAttributeValue(null, "x"));
							final int parsedLat = Integer.parseInt(pp.getAttributeValue(null, "y"));
							final String parsedDistStr = pp.getAttributeValue(null, "distance");
							final int parsedDist = parsedDistStr != null ? Integer.parseInt(parsedDistStr) : 0;

							final Station newStation = new Station(parsedLocationId, parsedName, parsedLat, parsedLon, parsedDist, null, null);
							if (!stations.contains(newStation))
								stations.add(newStation);

							XmlPullUtil.skipRestOfTree(pp);
						}
					}
				}

				if (ownStation != null && !stations.contains(ownStation))
					stations.add(ownStation);

				if (maxStations == 0 || maxStations >= stations.size())
					return new NearbyStationsResult(uri, stations);
				else
					return new NearbyStationsResult(uri, stations.subList(0, maxStations));
			}
			else if ("notidentified".equals(nameState))
			{
				return new NearbyStationsResult(uri, NearbyStationsResult.Status.INVALID_STATION);
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
		catch (final FileNotFoundException x)
		{
			return new NearbyStationsResult(uri, NearbyStationsResult.Status.SERVICE_DOWN);
		}
		catch (final SocketTimeoutException x)
		{
			return new NearbyStationsResult(uri, NearbyStationsResult.Status.SERVICE_DOWN);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_LINE_IRE = Pattern.compile("IRE\\d+");
	private static final Pattern P_LINE_RE = Pattern.compile("RE\\d+");
	private static final Pattern P_LINE_RB = Pattern.compile("RB\\d+");
	private static final Pattern P_LINE_VB = Pattern.compile("VB\\d+");
	private static final Pattern P_LINE_OE = Pattern.compile("OE\\d+");
	private static final Pattern P_LINE_R = Pattern.compile("R\\d+(/R\\d+|\\(z\\))?");
	private static final Pattern P_LINE_U = Pattern.compile("U\\d+");
	private static final Pattern P_LINE_S = Pattern.compile("^(?:%)?(S\\d+)");
	private static final Pattern P_LINE_NUMBER = Pattern.compile("\\d+");

	protected String parseLine(final String mot, final String name, final String longName)
	{
		if (mot == null || name == null || longName == null)
			throw new IllegalStateException("cannot normalize mot '" + mot + "' name '" + name + "' long '" + longName + "'");

		final int t = Integer.parseInt(mot);

		if (t == 0)
		{
			final String[] parts = longName.split(" ", 3);
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
			if (type.equals("OEC")) // ÖBB-EuroCity
				return 'I' + str;
			if (type.equals("OIC")) // ÖBB-InterCity
				return 'I' + str;
			if (type.equals("HT")) // First Hull Trains, GB
				return 'I' + str;

			if (type.equals("IR")) // Interregio
				return 'R' + str;
			if (type.equals("IRE")) // Interregio-Express
				return 'R' + str;
			if (P_LINE_IRE.matcher(type).matches())
				return 'R' + str;
			if (type.equals("RE")) // Regional-Express
				return 'R' + str;
			if (type.equals("R-Bahn")) // Regional-Express, VRR
				return 'R' + str;
			if (type.equals("REX")) // RegionalExpress, Österreich
				return 'R' + str;
			if (P_LINE_RE.matcher(type).matches())
				return 'R' + str;
			if (type.equals("RB")) // Regionalbahn
				return 'R' + str;
			if (P_LINE_RB.matcher(type).matches())
				return 'R' + str;
			if (type.equals("R")) // Regionalzug
				return 'R' + str;
			if (P_LINE_R.matcher(type).matches())
				return 'R' + str;
			if (type.equals("Bahn"))
				return 'R' + str;
			if (type.equals("Regionalbahn"))
				return 'R' + str;
			if (type.equals("D")) // Schnellzug
				return 'R' + str;
			if (type.equals("E")) // Eilzug
				return 'R' + str;
			if (type.equals("S")) // ~Innsbruck
				return 'R' + str;
			if (type.equals("WFB")) // Westfalenbahn
				return 'R' + str;
			if (type.equals("NWB")) // NordWestBahn
				return 'R' + str;
			if (type.equals("NordWestBahn"))
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
			if (P_LINE_OE.matcher(type).matches())
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
			if (type.equals("VB")) // Vogtlandbahn
				return 'R' + str;
			if (P_LINE_VB.matcher(type).matches())
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
			if (type.equals("VEB")) // Vulkan-Eifel-Bahn
				return 'R' + str;
			if (type.equals("neg")) // Norddeutsche Eisenbahn Gesellschaft
				return 'R' + str;
			if (type.equals("AVG")) // Felsenland-Express
				return 'R' + str;
			if (type.equals("ABG")) // Anhaltische Bahngesellschaft
				return 'R' + str;
			if (type.equals("LGB")) // Lößnitzgrundbahn
				return 'R' + str;
			if (type.equals("LEO")) // Chiemgauer Lokalbahn
				return 'R' + str;
			if (type.equals("WTB")) // Weißeritztalbahn
				return 'R' + str;
			if (type.equals("P")) // Kasbachtalbahn, Wanderbahn im Regental, Rhön-Zügle
				return 'R' + str;
			if (type.equals("Abellio-Zug")) // Abellio
				return 'R' + str;
			if (type.equals("KBS")) // Kursbuchstrecke
				return 'R' + str;
			if (type.equals("Zug"))
				return 'R' + str;
			if (type.equals("ÖBB"))
				return 'R' + str;
			if (type.equals("CAT")) // City Airport Train Wien
				return 'R' + str;
			if (type.equals("CD"))
				return 'R' + str;
			if (type.equals("PR"))
				return 'R' + str;
			if (type.equals("KD")) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
				return 'R' + str;
			if (type.equals("VIAMO"))
				return 'R' + str;
			if (type.equals("SE")) // Southeastern, GB
				return 'R' + str;
			if (type.equals("SW")) // South West Trains, GB
				return 'R' + str;
			if (type.equals("SN")) // Southern, GB
				return 'R' + str;
			if (type.equals("NT")) // Northern Rail, GB
				return 'R' + str;
			if (type.equals("CH")) // Chiltern Railways, GB
				return 'R' + str;
			if (type.equals("EA")) // National Express East Anglia, GB
				return 'R' + str;
			if (type.equals("FC")) // First Capital Connect, GB
				return 'R' + str;
			if (type.equals("GW")) // First Great Western, GB
				return 'R' + str;
			if (type.equals("XC")) // Cross Country, GB, evtl. auch highspeed?
				return 'R' + str;
			if (type.equals("HC")) // Heathrow Connect, GB
				return 'R' + str;
			if (type.equals("HX")) // Heathrow Express, GB
				return 'R' + str;
			if (type.equals("GX")) // Gatwick Express, GB
				return 'R' + str;
			if (type.equals("C2C")) // c2c, GB
				return 'R' + str;
			if (type.equals("LM")) // London Midland, GB
				return 'R' + str;
			if (type.equals("EM")) // East Midlands Trains, GB
				return 'R' + str;
			if (type.equals("VT")) // Virgin Trains, GB, evtl. auch highspeed?
				return 'R' + str;
			if (type.equals("SR")) // ScotRail, GB, evtl. auch long-distance?
				return 'R' + str;
			if (type.equals("AW")) // Arriva Trains Wales, GB
				return 'R' + str;
			if (type.equals("WS")) // Wrexham & Shropshire, GB
				return 'R' + str;
			if (type.equals("TP")) // First TransPennine Express, GB, evtl. auch long-distance?
				return 'R' + str;
			if (type.equals("GC")) // Grand Central, GB
				return 'R' + str;

			if (type.equals("BSB")) // Breisgau-S-Bahn
				return 'S' + str;
			if (type.equals("RER")) // Réseau Express Régional, Frankreich
				return 'S' + str;
			if (type.equals("LO")) // London Overground, GB
				return 'S' + str;

			if (P_LINE_U.matcher(type).matches())
				return 'U' + str;

			if (type.equals("RT")) // RegioTram
				return 'T' + str;
			if (type.equals("STR")) // Nordhausen
				return 'T' + str;

			if (type.length() == 0)
				return "?";
			if (P_LINE_NUMBER.matcher(type).matches())
				return "?";

			throw new IllegalArgumentException("cannot normalize: long '" + longName + "' type '" + type + "' str '" + str + "'");
		}

		if (t == 1)
		{
			final Matcher m = P_LINE_S.matcher(name);
			if (m.find())
				return 'S' + m.group(1);
			else
				return 'S' + name;
		}

		if (t == 2)
			return 'U' + name;

		if (t == 3 || t == 4)
			return 'T' + name;

		if (t == 5 || t == 6 || t == 7 || t == 10)
			return 'B' + name;

		if (t == 8)
			return 'C' + name;

		if (t == 9)
			return 'F' + name;

		if (t == 11 || t == -1)
			return '?' + name;

		throw new IllegalStateException("cannot normalize mot '" + mot + "' name '" + name + "' long '" + longName + "'");
	}

	protected abstract String departuresQueryUri(String stationId, int maxDepartures);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures) throws IOException
	{
		final String uri = departuresQueryUri(stationId, maxDepartures);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertItdRequest(pp);

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"dm".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"dm\" />");
			if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName"))
				throw new IllegalStateException("cannot find <itdOdvName />");
			final String nameState = pp.getAttributeValue(null, "state");
			if ("identified".equals(nameState))
			{
				XmlPullUtil.jumpToStartTag(pp, null, "odvNameElem");
				String locationIdStr = pp.getAttributeValue(null, "stopID");
				if (locationIdStr == null)
					locationIdStr = pp.getAttributeValue(null, "id");
				final int locationId = Integer.parseInt(locationIdStr);

				final String location = normalizeLocationName(pp.nextText());

				final Calendar departureTime = new GregorianCalendar();
				departureTime.setTimeZone(timeZone());

				final List<Line> lines = new ArrayList<Line>(4);

				XmlPullUtil.jumpToStartTag(pp, null, "itdServingLines");
				if (!pp.isEmptyElementTag())
				{
					XmlPullUtil.enter(pp);
					while (XmlPullUtil.test(pp, "itdServingLine"))
					{
						try
						{
							final String lineStr = parseLine(pp.getAttributeValue(null, "motType"), pp.getAttributeValue(null, "number"), pp
									.getAttributeValue(null, "number"));
							final String destination = normalizeLocationName(pp.getAttributeValue(null, "direction"));
							final String destinationIdStr = pp.getAttributeValue(null, "destID");
							final int destinationId = destinationIdStr.length() > 0 ? Integer.parseInt(destinationIdStr) : 0;
							final Line line = new Line(lineStr, lineColors(lineStr), destinationId, destination);
							if (!lines.contains(line))
								lines.add(line);
						}
						catch (final IllegalArgumentException x)
						{
							// swallow for now
						}

						XmlPullUtil.enter(pp);
						XmlPullUtil.exit(pp);
					}
					XmlPullUtil.exit(pp);
				}
				else
				{
					XmlPullUtil.next(pp);
				}

				final List<Departure> departures = new ArrayList<Departure>(8);

				XmlPullUtil.require(pp, "itdDepartureList");
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDeparture"))
				{
					if (Integer.parseInt(pp.getAttributeValue(null, "stopID")) == locationId)
					{
						final String position = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null, "platformName"));

						departureTime.clear();

						processItdDateTime(pp, departureTime);

						if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdServingLine"))
							throw new IllegalStateException("itdServingLine not found:" + pp.getPositionDescription());
						final String line = parseLine(pp.getAttributeValue(null, "motType"), pp.getAttributeValue(null, "number"), pp
								.getAttributeValue(null, "number"));
						final boolean isRealtime = pp.getAttributeValue(null, "realtime").equals("1");
						final String destination = normalizeLocationName(pp.getAttributeValue(null, "direction"));
						final int destinationId = Integer.parseInt(pp.getAttributeValue(null, "destID"));
						XmlPullUtil.skipRestOfTree(pp);

						departures.add(new Departure(!isRealtime ? departureTime.getTime() : null, isRealtime ? departureTime.getTime() : null, line,
								lineColors(line), null, position, destinationId, destination, null));
					}

					XmlPullUtil.skipRestOfTree(pp);
				}

				return new QueryDeparturesResult(new Location(LocationType.STATION, locationId, 0, 0, location), departures, lines);
			}
			else if ("notidentified".equals(nameState))
			{
				return new QueryDeparturesResult(QueryDeparturesResult.Status.INVALID_STATION, Integer.parseInt(stationId));
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
		catch (final FileNotFoundException x)
		{
			return new QueryDeparturesResult(QueryDeparturesResult.Status.SERVICE_DOWN, Integer.parseInt(stationId));
		}
		catch (final SocketTimeoutException x)
		{
			return new QueryDeparturesResult(QueryDeparturesResult.Status.SERVICE_DOWN, Integer.parseInt(stationId));
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private void processItdDateTime(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDateTime"))
			throw new IllegalStateException("itdDateTime not found:" + pp.getPositionDescription());

		if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDate"))
			throw new IllegalStateException("itdDate not found:" + pp.getPositionDescription());
		processItdDate(pp, calendar);
		XmlPullUtil.skipRestOfTree(pp);

		if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdTime"))
			throw new IllegalStateException("itdTime not found:" + pp.getPositionDescription());
		processItdTime(pp, calendar);
		XmlPullUtil.skipRestOfTree(pp);

		XmlPullUtil.skipRestOfTree(pp);
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
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(pp.getAttributeValue(null, "hour")));
		calendar.set(Calendar.MINUTE, Integer.parseInt(pp.getAttributeValue(null, "minute")));
	}

	private static final Pattern P_STATION_NAME_WHITESPACE = Pattern.compile("\\s+");

	protected static String normalizeLocationName(final String name)
	{
		return P_STATION_NAME_WHITESPACE.matcher(name).replaceAll(" ");
	}

	protected static double latLonToDouble(final int value)
	{
		return (double) value / 1000000;
	}

	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep, products, walkSpeed) + "&sessionID=0";

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri);
			return queryConnections(uri, is);
		}
		catch (final SocketTimeoutException x)
		{
			return new QueryConnectionsResult(QueryConnectionsResult.Status.SERVICE_DOWN);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri);
			return queryConnections(uri, is);
		}
		catch (final SocketTimeoutException x)
		{
			return new QueryConnectionsResult(QueryConnectionsResult.Status.SERVICE_DOWN);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private QueryConnectionsResult queryConnections(final String uri, final InputStream is) throws IOException
	{
		try
		{
			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertItdRequest(pp);
			final String sessionId = pp.getAttributeValue(null, "sessionID");

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdTripRequest"))
				throw new IllegalStateException("cannot find <itdTripRequest />");

			// parse odv name elements
			List<Location> ambiguousFrom = null, ambiguousTo = null, ambiguousVia = null;
			Location from = null, via = null, to = null;

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"origin".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"origin\" />");
			XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName");
			final String originState = pp.getAttributeValue(null, "state");
			if ("list".equals(originState))
			{
				ambiguousFrom = new ArrayList<Location>();
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					ambiguousFrom.add(processOdvNameElem(pp));
			}
			else if ("identified".equals(originState))
			{
				if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					throw new IllegalStateException("cannot find <odvNameElem />");
				from = processOdvNameElem(pp);
			}

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"destination".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"destination\" />");
			XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName");
			final String destinationState = pp.getAttributeValue(null, "state");
			if ("list".equals(destinationState))
			{
				ambiguousTo = new ArrayList<Location>();
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					ambiguousTo.add(processOdvNameElem(pp));
			}
			else if ("identified".equals(destinationState))
			{
				if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					throw new IllegalStateException("cannot find <odvNameElem />");
				to = processOdvNameElem(pp);
			}

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"via".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"via\" />");
			XmlPullUtil.nextStartTagInsideTree(pp, null, "itdOdvName");
			final String viaState = pp.getAttributeValue(null, "state");
			if ("list".equals(viaState))
			{
				ambiguousVia = new ArrayList<Location>();
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					ambiguousVia.add(processOdvNameElem(pp));
			}
			else if ("identified".equals(viaState))
			{
				if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "odvNameElem"))
					throw new IllegalStateException("cannot find <odvNameElem />");
				via = processOdvNameElem(pp);
			}

			if (ambiguousFrom != null || ambiguousTo != null || ambiguousVia != null)
				return new QueryConnectionsResult(ambiguousFrom, ambiguousVia, ambiguousTo);

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdTripDateTime"))
				throw new IllegalStateException("cannot find <itdTripDateTime />");
			if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDateTime"))
				throw new IllegalStateException("cannot find <itdDateTime />");
			if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdDate"))
				throw new IllegalStateException("cannot find <itdDate />");
			if (!pp.isEmptyElementTag())
			{
				if (!XmlPullUtil.nextStartTagInsideTree(pp, null, "itdMessage"))
					throw new IllegalStateException("cannot find <itdMessage />");
				final String message = pp.nextText();
				if (message.equals("invalid date"))
					return new QueryConnectionsResult(Status.INVALID_DATE);
			}

			final Calendar departureTime = new GregorianCalendar(), arrivalTime = new GregorianCalendar();
			departureTime.setTimeZone(timeZone());
			arrivalTime.setTimeZone(timeZone());
			final List<Connection> connections = new ArrayList<Connection>();

			if (XmlPullUtil.jumpToStartTag(pp, null, "itdRouteList"))
			{
				while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdRoute"))
				{
					final String id = pp.getAttributeValue(null, "routeIndex") + "-" + pp.getAttributeValue(null, "routeTripIndex");

					XmlPullUtil.jumpToStartTag(pp, null, "itdPartialRouteList");
					final List<Connection.Part> parts = new LinkedList<Connection.Part>();
					String firstDeparture = null;
					Date firstDepartureTime = null;
					String lastArrival = null;
					Date lastArrivalTime = null;

					while (XmlPullUtil.nextStartTagInsideTree(pp, null, "itdPartialRoute"))
					{
						XmlPullUtil.jumpToStartTag(pp, null, "itdPoint");
						if (!"departure".equals(pp.getAttributeValue(null, "usage")))
							throw new IllegalStateException();
						final int departureId = Integer.parseInt(pp.getAttributeValue(null, "stopID"));
						final String departure = normalizeLocationName(pp.getAttributeValue(null, "name"));
						if (firstDeparture == null)
							firstDeparture = departure;
						final String departurePosition = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null,
								"platformName"));
						processItdDateTime(pp, departureTime);
						if (firstDepartureTime == null)
							firstDepartureTime = departureTime.getTime();
						XmlPullUtil.skipRestOfTree(pp);

						XmlPullUtil.jumpToStartTag(pp, null, "itdPoint");
						if (!"arrival".equals(pp.getAttributeValue(null, "usage")))
							throw new IllegalStateException();
						final int arrivalId = Integer.parseInt(pp.getAttributeValue(null, "stopID"));
						final String arrival = normalizeLocationName(pp.getAttributeValue(null, "name"));
						lastArrival = arrival;
						final String arrivalPosition = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null,
								"platformName"));
						processItdDateTime(pp, arrivalTime);
						lastArrivalTime = arrivalTime.getTime();
						XmlPullUtil.skipRestOfTree(pp);

						XmlPullUtil.jumpToStartTag(pp, null, "itdMeansOfTransport");
						final String productName = pp.getAttributeValue(null, "productName");
						if ("Fussweg".equals(productName))
						{
							final int min = (int) (arrivalTime.getTimeInMillis() - departureTime.getTimeInMillis()) / 1000 / 60;

							if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
							{
								final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
								parts.add(new Connection.Footway(lastFootway.min + min, lastFootway.departureId, lastFootway.departure, arrivalId,
										arrival));
							}
							else
							{
								parts.add(new Connection.Footway(min, departureId, departure, arrivalId, arrival));
							}
						}
						else if ("gesicherter Anschluss".equals(productName))
						{
							// ignore
						}
						else
						{
							final String destinationIdStr = pp.getAttributeValue(null, "destID");
							final int destinationId = destinationIdStr.length() > 0 ? Integer.parseInt(destinationIdStr) : 0;
							final String destination = normalizeLocationName(pp.getAttributeValue(null, "destination"));
							final String line = parseLine(pp.getAttributeValue(null, "motType"), pp.getAttributeValue(null, "shortname"), pp
									.getAttributeValue(null, "name"));
							parts.add(new Connection.Trip(line, lineColors(line), destinationId, destination, departureTime.getTime(),
									departurePosition, departureId, departure, arrivalTime.getTime(), arrivalPosition, arrivalId, arrival));
						}
						XmlPullUtil.skipRestOfTree(pp);

						XmlPullUtil.skipRestOfTree(pp);
					}

					connections
							.add(new Connection(id, uri, firstDepartureTime, lastArrivalTime, null, null, 0, firstDeparture, 0, lastArrival, parts));
					XmlPullUtil.skipRestOfTree(pp);
				}

				return new QueryConnectionsResult(uri, from, via, to, commandLink(sessionId, "tripNext"), connections);
			}
			else
			{
				return new QueryConnectionsResult(Status.NO_CONNECTIONS);
			}
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
	}

	private static final Pattern P_PLATFORM_GLEIS = Pattern.compile("Gleis (\\d+[a-z]?)(?: ([A-Z])\\s*-\\s*([A-Z]))?");

	private static final String normalizePlatform(final String platform, final String platformName)
	{
		if (platformName != null && platformName.length() > 0)
		{
			final Matcher mGleis = P_PLATFORM_GLEIS.matcher(platformName);
			if (mGleis.matches())
			{
				if (mGleis.group(2) == null)
					return "Gl. " + mGleis.group(1);
				else
					return "Gl. " + mGleis.group(1) + " " + mGleis.group(2) + "-" + mGleis.group(3);
			}
			else
			{
				return platformName;
			}
		}

		if (platform != null && platform.length() > 0)
			return platform;

		return null;
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	protected abstract String connectionsQueryUri(Location from, Location via, Location to, Date date, boolean dep, String products,
			WalkSpeed walkSpeed);

	protected abstract String commandLink(String sessionId, String command);

	protected static final void appendCommonConnectionParams(final StringBuilder uri)
	{
		uri.append("&outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&calcNumberOfTrips=4");
	}

	protected void appendLocation(final StringBuilder uri, final Location location, final String paramSuffix)
	{
		if ((location.type == LocationType.POI || location.type == LocationType.ADDRESS) && location.lat != 0 && location.lon != 0)
		{
			uri.append("&type_").append(paramSuffix).append("=coord");
			uri.append("&name_").append(paramSuffix).append("=").append(String.format("%2.6f:%2.6f", location.lon / 1E6, location.lat / 1E6)).append(
					":WGS84[DD.dddddd]");
		}
		else
		{
			uri.append("&type_").append(paramSuffix).append("=").append(locationTypeValue(location));
			uri.append("&name_").append(paramSuffix).append("=").append(ParserUtils.urlEncode(locationValue(location), "ISO-8859-1"));
		}
	}

	protected static final String locationTypeValue(final Location location)
	{
		final LocationType type = location.type;
		if (type == LocationType.STATION)
			return "stop";
		if (type == LocationType.ADDRESS)
			return "any"; // strange, matches with anyObjFilter
		if (type == LocationType.POI)
			return "poi";
		if (type == LocationType.ANY)
			return "any";
		throw new IllegalArgumentException(type.toString());
	}

	protected static final String locationValue(final Location location)
	{
		if ((location.type == LocationType.STATION || location.type == LocationType.POI) && location.id != 0)
			return Integer.toString(location.id);
		else
			return location.name;
	}

	protected static final String productParams(final String products)
	{
		if (products == null)
			return "";

		final StringBuilder params = new StringBuilder("&includedMeans=checkbox");

		for (final char p : products.toCharArray())
		{
			if (p == 'I' || p == 'R')
				params.append("&inclMOT_0=on");
			if (p == 'S')
				params.append("&inclMOT_1=on");
			if (p == 'U')
				params.append("&inclMOT_2=on");
			if (p == 'T')
				params.append("&inclMOT_3=on&inclMOT_4=on");
			if (p == 'B')
				params.append("&inclMOT_5=on&inclMOT_6=on&inclMOT_7=on&inclMOT_10=on");
			if (p == 'F')
				params.append("&inclMOT_9=on");
			if (p == 'C')
				params.append("&inclMOT_8=on");
			params.append("&inclMOT_11=on"); // TODO always show 'others', for now
		}

		return params.toString();
	}

	protected static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();

	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "slow");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "normal");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "fast");
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

	private void assertItdRequest(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.jumpToStartTag(pp, null, "itdRequest"))
			throw new IOException("cannot find <itdRequest />");
	}
}
