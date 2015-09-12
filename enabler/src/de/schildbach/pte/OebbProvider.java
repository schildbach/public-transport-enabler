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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fahrplan.oebb.at/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM,
			Product.HIGH_SPEED_TRAIN, Product.ON_DEMAND, Product.HIGH_SPEED_TRAIN };

	public OebbProvider()
	{
		super(NetworkId.OEBB, API_BASE, "dn", PRODUCTS_MAP);

		setDominantPlanStopTime(true);
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
			uri.append("?near=Suchen");
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

	private static final String[] PLACES = { "Wien", "Graz", "Linz/Donau", "Salzburg", "Innsbruck" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " "))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitPOI(final String poi)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(poi);
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
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("RR")) // Finnland, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("EE")) // Rumänien, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("OZ")) // Schweden, Oeresundzug, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("UUU")) // Italien, Nacht, Connections only?
			return Product.HIGH_SPEED_TRAIN;

		if (ucType.equals("S2")) // Helsinki-Turku, Connections only?
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("RE")) // RegionalExpress Deutschland
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("DPN")) // Connections only? TODO nicht evtl. doch eher ne S-Bahn?
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("IP")) // Ozd, Ungarn
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("N")) // Frankreich, Tours
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("DPF")) // VX=Vogtland Express, Connections only?
			return Product.REGIONAL_TRAIN;
		if ("UAU".equals(ucType)) // Rußland
			return Product.REGIONAL_TRAIN;

		if (ucType.equals("RSB")) // Schnellbahn Wien
			return Product.SUBURBAN_TRAIN;

		if (ucType.equals("LKB")) // Connections only?
			return Product.TRAM;

		if (ucType.equals("OBU")) // Connections only?
			return Product.BUS;
		if (ucType.equals("O-BUS")) // Stadtbus
			return Product.BUS;
		if (ucType.equals("O")) // Stadtbus
			return Product.BUS;

		if (ucType.equals("SCH")) // Connections only?
			return Product.FERRY;
		if (ucType.equals("F")) // Fähre
			return Product.FERRY;

		if (ucType.equals("LIF"))
			return Product.CABLECAR;
		if (ucType.equals("LIFT")) // Graz Uhrturm
			return Product.CABLECAR;
		if (ucType.equals("SSB")) // Graz Schlossbergbahn
			return Product.CABLECAR;

		if (ucType.equals("U70")) // U.K., Connections only?
			return null;
		if (ucType.equals("X70")) // U.K., Connections only?
			return null;
		if (ucType.equals("R84")) // U.K., Connections only?
			return null;
		if (ucType.equals("S84")) // U.K., Connections only?
			return null;
		if (ucType.equals("T84")) // U.K., Connections only?
			return null;

		return super.normalizeType(type);
	}
}
