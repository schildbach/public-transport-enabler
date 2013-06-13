/*
 * Copyright 2010-2013 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class DsbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DSB;
	private static final String API_BASE = "http://mobil.rejseplanen.dk/mobil-bin/";

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
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Intercity
			productBits.setCharAt(1, '1'); // InterCityExpress
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Regionalzug
			productBits.setCharAt(3, '1'); // sonstige Züge
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(10, '1'); // U-Bahn
		}
		else if (product == Product.TRAM)
		{
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1'); // Bus
			productBits.setCharAt(6, '1'); // ExpressBus
			productBits.setCharAt(7, '1'); // Nachtbus
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(8, '1'); // Telebus/andere
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(9, '1'); // Schiff
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String NEARBY_STATIONS_BY_COORDINATE_URI = "http://xmlopen.rejseplanen.dk/bin/rest.exe/stopsNearby?coordX=%d&coordY=%d";

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(String.format(Locale.ENGLISH, NEARBY_STATIONS_BY_COORDINATE_URI, location.lon, location.lat));
			if (maxStations != 0)
				uri.append("&maxNumber=").append(maxStations);
			if (maxDistance != 0)
				uri.append("&maxRadius=").append(maxDistance);

			final List<Location> locations = xmlLocationList(uri.toString());

			return new NearbyStationsResult(null, locations);
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(API_BASE);

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

	private static final String AUTOCOMPLETE_URI = "http://xmlopen.rejseplanen.dk/bin/rest.exe/location.name?input=%s";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(Locale.ENGLISH, AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ISO_8859_1));

		return xmlLocationList(uri);
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
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
		if ("TOGBUS".equals(ucType))
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
