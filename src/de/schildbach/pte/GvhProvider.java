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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "mobil.gvh.de";
	private static final String API_BASE = "http://mobil.gvh.de/mobile2/";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "XML_STOPFINDER_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&name_sf=%s&type_sf=any";
	private static final String ENCODING = "ISO-8859-1";

	@Override
	protected String autocompleteUri(final CharSequence constraint)
	{
		return String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));
	}

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	@Override
	protected String nearbyLatLonUri(final double lat, final double lon)
	{
		return null;
	}

	public StationLocationResult stationLocation(String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(String stationId, int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		return uri.toString();
	}

	private static final Pattern P_LINE_RE = Pattern.compile("RE\\d+");
	private static final Pattern P_LINE_RB = Pattern.compile("RB\\d+");
	private static final Pattern P_LINE_U = Pattern.compile("U\\d+");

	@Override
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
		if (t == 3 || t == 4)
			return 'T' + number;
		if (t == 5 || t == 6 || t == 7 || t == 10)
			return 'B' + number;
		if (t == 9)
			return 'F' + number;
		if (t == 11)
			return '?' + number;

		throw new IllegalStateException("cannot normalize mot '" + mot + "' number '" + number + "'");
	}

	public QueryConnectionsResult queryConnections(LocationType fromType, String from, LocationType viaType, String via, LocationType toType,
			String to, Date date, boolean dep, WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
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
