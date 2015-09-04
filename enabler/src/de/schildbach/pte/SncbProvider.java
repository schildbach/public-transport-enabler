/*
 * Copyright 2010-2015 the original author or authors.
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
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class SncbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/";
	// http://hari.b-rail.be/hafas/bin/
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, null, Product.HIGH_SPEED_TRAIN, null, null, Product.BUS,
			Product.REGIONAL_TRAIN, null, Product.SUBWAY, Product.BUS, Product.TRAM, null, null, null, null, null };

	public SncbProvider()
	{
		super(NetworkId.SNCB, API_BASE, "nn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStationBoardHasLocation(true);
	}

	private static final String[] PLACES = { "Antwerpen", "Gent", "Charleroi", "Liege", "Liège", "Brussel" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(address);
	}

	@Override
	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxLocations) throws IOException
	{
		if (location.hasLocation())
		{
			return nearbyLocationsByCoordinate(types, location.lat, location.lon, maxDistance, maxLocations);
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Zoek");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(normalizeStationId(location.id));

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location);
		}
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("THALYS".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;

		if ("L".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("MÉTRO".equals(ucType))
			return Product.SUBWAY;

		if ("TRAMWAY".equals(ucType))
			return Product.TRAM;

		return super.normalizeType(type);
	}
}
