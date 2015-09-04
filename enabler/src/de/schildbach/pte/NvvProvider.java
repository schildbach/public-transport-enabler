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
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class NvvProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://auskunft.nvv.de/auskunft/bin/jp/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
			Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.BUS, Product.FERRY, Product.ON_DEMAND, Product.REGIONAL_TRAIN,
			Product.REGIONAL_TRAIN };

	public NvvProvider()
	{
		super(NetworkId.NVV, API_BASE, "dn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
	}

	private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg", "Kassel", "Hanau", "Göttingen",
			"Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

	@Override
	protected String[] splitStationName(final String name)
	{
		if (name.startsWith("F "))
			return new String[] { "Frankfurt", name.substring(2) };
		if (name.startsWith("OF "))
			return new String[] { "Offenbach", name.substring(3) };
		if (name.startsWith("MZ "))
			return new String[] { "Mainz", name.substring(3) };

		for (final String place : PLACES)
		{
			if (name.startsWith(place + " - "))
				return new String[] { place, name.substring(place.length() + 3) };
			else if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

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
			uri.append("?L=vs_rmv&near=Anzeigen");
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
	protected void addCustomReplaces(final StringReplaceReader reader)
	{
		reader.replace("<ul>", " ");
		reader.replace("</ul>", " ");
		reader.replace("<li>", " ");
		reader.replace("</li>", " ");
		reader.replace("Park&Ride", "Park&amp;Ride");
		reader.replace("C&A", "C&amp;A");
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("U-BAHN".equals(ucType))
			return Product.SUBWAY;

		if ("AT".equals(ucType)) // Anschluß Sammel Taxi, Anmeldung nicht erforderlich
			return Product.BUS;
		if ("LTAXI".equals(ucType))
			return Product.BUS;

		if ("MOFA".equals(ucType)) // Mobilfalt-Fahrt
			return Product.ON_DEMAND;

		return super.normalizeType(type);
	}
}
