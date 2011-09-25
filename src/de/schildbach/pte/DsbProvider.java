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
public class DsbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DSB;
	private static final String API_BASE = "http://dk.hafas.de/bin/mobile/";

	// http://dk.hafas.de/bin/fat/
	// http://mobil.rejseplanen.dk/mobil-bin/

	public DsbProvider()
	{
		super(API_BASE + "query.exe/dn", 11, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
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
			productBits.setCharAt(0, '1'); // Intercity
			productBits.setCharAt(1, '1'); // InterCityExpress
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // Regionalzug
			productBits.setCharAt(3, '1'); // sonstige Züge
		}
		else if (product == 'S')
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(10, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
		}
		else if (product == 'B')
		{
			productBits.setCharAt(5, '1'); // Bus
			productBits.setCharAt(6, '1'); // ExpressBus
			productBits.setCharAt(7, '1'); // Nachtbus
		}
		else if (product == 'P')
		{
			productBits.setCharAt(8, '1'); // Telebus/andere
		}
		else if (product == 'F')
		{
			productBits.setCharAt(9, '1'); // Schiff
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
			uri.append("query.exe/mny");
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
			uri.append("stboard.exe/mn");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0");
			uri.append("&L=vs_java3");

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: '" + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/mn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.exe/dn?getstop=1&REQ0JourneyStopsS0A=255&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected String normalizeLine(final String line)
	{
		return parseLineAndType(line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("ICL".equals(ucType))
			return 'I';

		if ("ØR".equals(ucType))
			return 'R';
		if ("RA".equals(ucType))
			return 'R';
		if ("RX".equals(ucType))
			return 'R';
		if ("PP".equals(ucType))
			return 'R';

		if ("S-TOG".equals(ucType))
			return 'S';

		if ("BYBUS".equals(ucType))
			return 'B';
		if ("X-BUS".equals(ucType))
			return 'B';
		if ("HV-BUS".equals(ucType)) // Havnebus
			return 'B';
		if ("T-BUS".equals(ucType)) // Togbus
			return 'B';

		if ("TELEBUS".equals(ucType))
			return 'P';
		if ("TELETAXI".equals(ucType))
			return 'P';

		if ("FÆRGE".equals(ucType))
			return 'F';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
