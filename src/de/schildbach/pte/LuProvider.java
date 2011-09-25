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

/**
 * @author Andreas Schildbach
 */
public class LuProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.LU;
	private static final String API_BASE = "http://mobiliteitszentral.hafas.de/hafas/";

	public LuProvider()
	{
		super(API_BASE + "query.exe/dn", 10, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(1, '1'); // IC/EC
			productBits.setCharAt(2, '1'); // Fernverkehrszug
		}
		else if (product == 'R')
		{
			productBits.setCharAt(3, '1'); // Regionalverkehrszug
		}
		else if (product == 'S')
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(7, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
			productBits.setCharAt(8, '1'); // Straßenbahn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(5, '1'); // Bus
		}
		else if (product == 'P')
		{
			productBits.setCharAt(9, '1'); // AST/Rufbus
		}
		else if (product == 'F')
		{
			productBits.setCharAt(6, '1'); // Schiff
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
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(API_BASE);
			uri.append("stboard.exe/dn");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0&L=vs_java3");

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

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
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

		if ("CRE".equals(ucType))
			return 'R';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
