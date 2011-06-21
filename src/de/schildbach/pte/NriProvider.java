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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class NriProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NRI;
	private static final String API_BASE = "http://hafas.websrv05.reiseinfo.no/bin/dev/nri/";

	public NriProvider()
	{
		super(API_BASE + "query.exe/on", 8, null);
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

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // Flugzeug
		}
		else if (product == 'R')
		{
			productBits.setCharAt(1, '1'); // Regionalverkehrszug
			productBits.setCharAt(7, '1'); // Tourismus-Züge
			productBits.setCharAt(2, '1'); // undokumentiert
		}
		else if (product == 'S' || product == 'T')
		{
			productBits.setCharAt(3, '1'); // Stadtbahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == 'B' || product == 'P')
		{
			productBits.setCharAt(2, '1'); // Bus
		}
		else if (product == 'F')
		{
			productBits.setCharAt(5, '1'); // Express-Boot
			productBits.setCharAt(6, '1'); // Schiff
			productBits.setCharAt(7, '1'); // Fähre
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Oslo", "Bergen" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " "))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/ony");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 150);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/on");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0");
			uri.append("&L=vs_java3");

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/on");
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
			+ "ajax-getstop.exe/ony?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsS0B=5&REQ0JourneyStopsB=12&getstop=1&noSession=yes&REQ0JourneyStopsS0G=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AIR".equals(ucType))
			return 'I';

		if ("TRA".equals(ucType))
			return 'R';
		if ("TRAIN".equals(ucType))
			return 'R';

		if ("U".equals(ucType))
			return 'U';

		if ("TRAM".equals(ucType))
			return 'T';
		if ("MTR".equals(ucType))
			return 'T';

		if (ucType.startsWith("BUS"))
			return 'B';

		if ("EXP".equals(ucType))
			return 'F';
		if ("EXP.BOAT".equals(ucType))
			return 'F';
		if ("FERRY".equals(ucType))
			return 'F';
		if ("FER".equals(ucType))
			return 'F';
		if ("SHIP".equals(ucType))
			return 'F';
		if ("SHI".equals(ucType))
			return 'F';

		return 0;
	}
}
