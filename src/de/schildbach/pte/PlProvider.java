/*
 * Copyright 2010, 2011 the original author or authors.
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.PL;
	private static final String API_BASE = "http://rozklad.sitkol.pl/bin/";

	public PlProvider()
	{
		super(API_BASE + "query.exe/pn", null, null, "UTF-8");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	private static final String[] PLACES = { "Warszawa", "Kraków" };

	@Override
	protected String[] splitNameAndPlace(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.endsWith(", " + place))
				return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitNameAndPlace(name);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	@Override
	protected String nearbyStationUri(String stationId)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("stboard.exe/pn");
		uri.append("?productsFilter=1111111");
		uri.append("&boardType=dep");
		uri.append("&input=").append(ParserUtils.urlEncode(stationId));
		uri.append("&sTI=1&start=yes&hcount=0");
		uri.append("&L=vs_java3");

		// &inputTripelId=A%3d1%40O%3dCopenhagen%20Airport%40X%3d12646941%40Y%3d55629753%40U%3d86%40L%3d900000011%40B%3d1

		return xmlNearbyStations(uri.toString());
	}

	private static final Pattern P_NORMALIZE_LINE_RUSSIA = Pattern.compile("(?:D\\s*)?(\\d{1,3}(?:[A-Z]{2}|Y))");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");

	@Override
	protected String normalizeLine(String line)
	{
		final Matcher mRussia = P_NORMALIZE_LINE_RUSSIA.matcher(line);
		if (mRussia.matches())
			return 'R' + mRussia.group(1);

		if (P_NORMALIZE_LINE_NUMBER.matcher(line).matches())
			return 'R' + line;

		return super.normalizeLine(line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("TLK".equals(ucType)) // Tanie Linie Kolejowe, Polen
			return 'I';
		if ("X".equals(ucType)) // Schweden
			return 'I';
		if ("NZ".equals(ucType)) // Schweden, Nacht
			return 'I';
		if ("LYN".equals(ucType)) // Dänemark
			return 'I';
		if ("HOT".equals(ucType)) // Spanien, Nacht
			return 'I';
		if ("AVE".equals(ucType)) // Alta Velocidad Española, Spanien
			return 'I';
		if ("TLG".equals(ucType)) // Spanien, Madrid
			return 'I';
		if ("ALS".equals(ucType)) // Spanien
			return 'I';
		if ("ARC".equals(ucType)) // Spanien
			return 'I';
		if ("EM".equals(ucType)) // EuroMed, Barcelona-Alicante, Spanien
			return 'I';
		if ("ES".equals(ucType)) // Eurostar Italia
			return 'I';
		if ("SC".equals(ucType)) // SuperCity, Tschechien
			return 'I';
		if ("EST".equals(ucType)) // Eurostar Frankreich
			return 'I';
		if ("FYR".equals(ucType)) // Fyra, Amsterdam-Schiphol-Rotterdam
			return 'I';

		if ("D".equals(ucType))
			return 'R';
		if ("KM".equals(ucType)) // Koleje Mazowieckie
			return 'R';
		if ("KD".equals(ucType)) // Koleje Dolnoslaskie
			return 'R';
		if ("AR".equals(ucType)) // Arriva Polaczen
			return 'R';
		if ("NEB".equals(ucType)) // Niederbarnimer Eisenbahn
			return 'R';
		if ("NWB".equals(ucType)) // NordWestBahn
			return 'R';
		if ("OE".equals(ucType)) // Ostdeutsche Eisenbahn
			return 'R';
		if ("MR".equals(ucType)) // Märkische Regionalbahn
			return 'R';
		if ("MRB".equals(ucType)) // Mitteldeutsche Regiobahn
			return 'R';
		if ("HZL".equals(ucType)) // Hohenzollerische Landesbahn
			return 'R';
		if ("PEG".equals(ucType)) // Prignitzer Eisenbahn
			return 'R';
		if ("HLB".equals(ucType)) // Hessische Landesbahn
			return 'R';
		if ("VBG".equals(ucType)) // Vogtlandbahn
			return 'R';
		if ("CAN".equals(ucType)) // cantus Verkehrsgesellschaft
			return 'R';
		if ("TLX".equals(ucType)) // Trilex (Vogtlandbahn)
			return 'R';
		if ("SBB".equals(ucType)) // Schweizerische Bundesbahnen
			return 'R';
		if ("HSB".equals(ucType)) // Harzer Schmalspurbahnen
			return 'R';
		if ("OLA".equals(ucType)) // Ostseeland Verkehr
			return 'R';
		if ("ÖBA".equals(ucType)) // Öchsle-Bahn Betriebsgesellschaft
			return 'R';
		if ("BOB".equals(ucType)) // Bayerische Oberlandbahn
			return 'R';
		if ("VEC".equals(ucType)) // vectus Verkehrsgesellschaft
			return 'R';
		if ("OSB".equals(ucType)) // Ortenau-S-Bahn
			return 'R';
		if ("FEG".equals(ucType)) // Freiberger Eisenbahngesellschaft
			return 'R';
		if ("BRB".equals(ucType)) // ABELLIO Rail
			return 'R';
		if ("EB".equals(ucType)) // Erfurter Bahn
			return 'R';
		if ("SBS".equals(ucType)) // Städtebahn Sachsen
			return 'R';
		if ("WEG".equals(ucType)) // Württembergische Eisenbahn-Gesellschaft
			return 'R';
		if ("EX".equals(ucType)) // Polen
			return 'R';
		if ("ERB".equals(ucType)) // eurobahn (Keolis Deutschland)
			return 'R';
		if ("UBB".equals(ucType)) // Usedomer Bäderbahn
			return 'R';
		if ("RTB".equals(ucType)) // Rurtalbahn
			return 'R';
		if ("EVB".equals(ucType)) // Eisenbahnen und Verkehrsbetriebe Elbe-Weser
			return 'R';
		if ("RNV".equals(ucType)) // Rhein-Neckar-Verkehr GmbH
			return 'R';
		if ("VIA".equals(ucType))
			return 'R';
		if ("ME".equals(ucType)) // metronom Eisenbahngesellschaft
			return 'R';
		if ("MER".equals(ucType)) // metronom regional
			return 'R';
		if ("ALX".equals(ucType)) // Arriva-Länderbahn-Express
			return 'R';
		if ("STB".equals(ucType)) // Süd-Thüringen-Bahn
			return 'R';
		if ("CB".equals(ucType)) // City Bahn Chemnitz
			return 'R';
		if ("HTB".equals(ucType)) // Hörseltalbahn
			return 'R';
		if ("NOB".equals(ucType)) // Nord-Ostsee-Bahn
			return 'R';
		if ("ARR".equals(ucType)) // Ostfriesland
			return 'R';
		if ("ABR".equals(ucType)) // Bayerische Regiobahn
			return 'R';
		if ("AG".equals(ucType)) // Ingolstadt-Landshut
			return 'R';
		if ("PRE".equals(ucType)) // Pressnitztalbahn
			return 'R';
		if ("ZR".equals(ucType)) // Bratislava, Slovakai
			return 'R';
		if ("AKN".equals(ucType)) // AKN Eisenbahn AG
			return 'R';
		if ("SHB".equals(ucType)) // Schleswig-Holstein-Bahn
			return 'R';
		if ("P".equals(ucType)) // Kasbachtalbahn
			return 'R';
		if ("NBE".equals(ucType)) // nordbahn
			return 'R';
		if ("SDG".equals(ucType)) // Sächsische Dampfeisenbahngesellschaft
			return 'R';
		if ("MBB".equals(ucType)) // Mecklenburgische Bäderbahn Molli
			return 'R';
		if ("VE".equals(ucType)) // Lutherstadt Wittenberg
			return 'R';
		if ("SOE".equals(ucType)) // Sächsisch-Oberlausitzer Eisenbahngesellschaft
			return 'R';
		if ("BLB".equals(ucType)) // Berchtesgadener Land Bahn
			return 'R';
		if ("DAB".equals(ucType)) // Daadetalbahn
			return 'R';
		if ("VEN".equals(ucType)) // Rhenus Veniro
			return 'R';
		if ("NEG".equals(ucType)) // Norddeutsche Eisenbahngesellschaft Niebüll
			return 'R';
		if ("WTB".equals(ucType)) // Wutachtalbahn e.V.
			return 'R';
		if ("KTB".equals(ucType)) // Kandertalbahn
			return 'R';
		if ("BE".equals(ucType)) // Grensland-Express
			return 'R';
		if ("CAT".equals(ucType)) // City Airport Train
			return 'R';
		if ("LEO".equals(ucType)) // Chiemgauer Lokalbahn
			return 'R';
		if ("MSB".equals(ucType)) // Mainschleifenbahn
			return 'R';
		if ("ATR".equals(ucType)) // Spanien
			return 'R';
		if ("N".equals(ucType)) // St. Pierre des Corps - Tours
			return 'R';
		// if ("INT".equals(ucType)) // Rußland
		// return 'R';

		if ("SKM".equals(ucType)) // Szybka Kolej Miejska Tricity
			return 'S';
		if ("SKW".equals(ucType)) // Szybka Kolej Miejska Warschau
			return 'S';
		if ("WKD".equals(ucType)) // Warszawska Kolej Dojazdowa
			return 'S';
		if ("RER".equals(ucType)) // Réseau Express Régional, Frankreich
			return 'S';
		if ("SWE".equals(ucType)) // Südwestdeutsche Verkehrs-AG, Ortenau-S-Bahn
			return 'S';
		if ("BSB".equals(ucType)) // Breisgau S-Bahn
			return 'S';

		if ("METRO".equals(ucType))
			return 'U';

		if ("BUSMKK".equals(ucType)) // Main-Kinz-Kreis
			return 'B';

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		if ("E".equals(ucType))
			return '?';

		return 0;
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/pn");
		uri.append("?productsFilter=1111111");
		uri.append("&boardType=dep");
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), Integer.parseInt(stationId));
	}
}
