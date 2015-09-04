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
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class ShProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://nah.sh.hafas.de/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public ShProvider()
	{
		super(NetworkId.SH, API_BASE, "dn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
	}

	private static final String[] PLACES = { "Hamburg", "Kiel", "Lübeck", "Flensburg", "Neumünster" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
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
	protected NearbyLocationsResult nearbyStationsById(final String id, final int maxDistance) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append("?near=Anzeigen");
		uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
		uri.append("&input=").append(normalizeStationId(id));
		return htmlNearbyStations(uri.toString());
	}
}
