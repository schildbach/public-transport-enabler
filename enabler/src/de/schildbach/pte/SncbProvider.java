/*
 * Copyright 2010-2014 the original author or authors.
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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class SncbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SNCB;
	private static final String API_BASE = "http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/"; // http://hari.b-rail.be/hafas/bin/

	public SncbProvider()
	{
		super(API_BASE + "stboard.exe/nn", API_BASE + "ajax-getstop.exe/nny", API_BASE + "query.exe/nn", 16);

		setJsonGetStopsEncoding(UTF_8);
		setJsonNearbyStationsEncoding(UTF_8);
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
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 4)
			return 'I';
		if (value == 32)
			return 'B';
		if (value == 64)
			return 'R';
		if (value == 256)
			return 'U';
		if (value == 512)
			return 'B';
		if (value == 1024)
			return 'T';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(2, '1'); // IC/IR/P/ICT
		}
		else if (product == Product.REGIONAL_TRAIN || product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(6, '1'); // Zug
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(8, '1'); // Metro
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(10, '1'); // Stadtbahn
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(5, '1'); // Bus
			productBits.setCharAt(9, '1'); // Bus
		}
		else if (product == Product.FERRY || product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Antwerpen", "Gent", "Charleroi", "Liege", "Liège", "Brussel" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append(jsonNearbyStationsParameters(location, maxDistance, maxStations));

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Zoek");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append(xmlQueryDeparturesParameters(stationId));

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final StringBuilder uri = new StringBuilder(getStopEndpoint);
		uri.append(jsonGetStopsParameters(constraint));

		return jsonGetStops(uri.toString());
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

		if (ucType.startsWith("IC "))
			return 'I';
		if ("THALYS".equals(ucType))
			return 'I';

		if (ucType.startsWith("IR "))
			return 'R';
		if ("L".equals(ucType))
			return 'R';
		if ("CR".equals(ucType))
			return 'R';

		if ("MÉTRO".equals(ucType))
			return 'U';

		if ("TRAMWAY".equals(ucType))
			return 'T';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
