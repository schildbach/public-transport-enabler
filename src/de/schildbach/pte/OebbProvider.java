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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.OEBB;
	public static final String OLD_NETWORK_ID = "fahrplan.oebb.at";
	private static final String API_BASE = "http://fahrplan.oebb.at/bin/";
	private static final String URL_ENCODING = "ISO-8859-1";

	public OebbProvider()
	{
		super(API_BASE + "query.exe/dn", 12, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS || capability == Capability.DEPARTURES || capability == Capability.AUTOCOMPLETE_ONE_LINE
					|| capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // railjet/ICE
			productBits.setCharAt(1, '1'); // ÖBB EC/ÖBB IC
			productBits.setCharAt(2, '1'); // EC/IC
		}
		else if (product == 'R')
		{
			productBits.setCharAt(3, '1'); // D/EN
			productBits.setCharAt(4, '1'); // REX/R
		}
		else if (product == 'S')
		{
			productBits.setCharAt(5, '1'); // S-Bahnen
		}
		else if (product == 'U')
		{
			productBits.setCharAt(8, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
			productBits.setCharAt(9, '1'); // Straßenbahn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(6, '1'); // Busse
		}
		else if (product == 'P')
		{
			productBits.setCharAt(11, '1'); // Anrufpflichtige Verkehre
		}
		else if (product == 'F')
		{
			productBits.setCharAt(7, '1'); // Schiffe
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/dny");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/dn?near=Suchen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/dny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsB=12&S=%s?&js=true&";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), URL_ENCODING));

		return jsonGetStops(uri);
	}

	private static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();
	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "115");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "100");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "85");
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("RR")) // Finnland, Connections only?
			return 'I';
		if (ucType.equals("EE")) // Rumänien, Connections only?
			return 'I';
		if (ucType.equals("OZ")) // Schweden, Oeresundzug, Connections only?
			return 'I';
		if (ucType.equals("UUU")) // Italien, Nacht, Connections only?
			return 'I';

		if (ucType.equals("S2")) // Helsinki-Turku, Connections only?
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress Deutschland
			return 'R';
		if (ucType.equals("DPN")) // Connections only? TODO nicht evtl. doch eher ne S-Bahn?
			return 'R';
		if (ucType.equals("E")) // Budapest, Ungarn
			return 'R';
		if (ucType.equals("IP")) // Ozd, Ungarn
			return 'R';
		if (ucType.equals("N")) // Frankreich, Tours
			return 'R';
		if (ucType.equals("DPF")) // VX=Vogtland Express, Connections only?
			return 'R';
		// if (ucType.equals("SBE")) // Zittau-Seifhennersdorf, via JSON API
		// return 'R';
		// if (ucType.equals("RNV")) // Rhein-Neckar-Verkehr GmbH, via JSON API
		// return 'R';
		if ("UAU".equals(ucType)) // Rußland
			return 'R';

		if (ucType.equals("RSB")) // Schnellbahn Wien
			return 'S';
		// if (ucType.equals("DPN")) // S3 Bad Reichenhall-Freilassing, via JSON API
		// return 'S';

		if (ucType.equals("LKB")) // Connections only?
			return 'T';
		// if (ucType.equals("WLB")) // via JSON API
		// return 'T';

		if (ucType.equals("OBU")) // Connections only?
			return 'B';
		// if (ucType.equals("ASTSV")) // via JSON API
		// return 'B';
		if (ucType.equals("ICB")) // ÖBB ICBus
			return 'B';
		if (ucType.equals("BSV")) // Deutschland, Connections only?
			return 'B';
		if (ucType.equals("O-BUS")) // Stadtbus
			return 'B';

		if (ucType.equals("SCH")) // Connections only?
			return 'F';
		if (ucType.equals("F")) // Fähre
			return 'F';

		if (ucType.equals("LIF"))
			return 'C';
		if (ucType.equals("SSB")) // Graz Schlossbergbahn
			return 'C';
		// if (ucType.equals("HBB")) // Innsbruck Hungerburgbahn, via JSON API
		// return 'C';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if (ucType.equals("U70")) // U.K., Connections only?
			return '?';
		if (ucType.equals("X70")) // U.K., Connections only?
			return '?';
		if (ucType.equals("R84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("S84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("T84")) // U.K., Connections only?
			return '?';

		return 0;
	}
}
