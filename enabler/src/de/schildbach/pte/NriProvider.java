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
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * @author Andreas Schildbach
 */
public class NriProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://hafas.websrv05.reiseinfo.no/bin/dev/nri/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.BUS, Product.TRAM, Product.SUBWAY,
			Product.FERRY, Product.FERRY, Product.FERRY };

	public NriProvider()
	{
		super(NetworkId.NRI, API_BASE, "on", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
	}

	private static final String[] PLACES = { "Oslo", "Bergen" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " "))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException
	{
		return queryTripsXml(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return queryMoreTripsXml(context, later);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AIR".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;

		if ("TRA".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("TRAIN".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("HEL".equals(ucType)) // Heli
			return Product.REGIONAL_TRAIN;

		if ("U".equals(ucType))
			return Product.SUBWAY;

		if ("TRAM".equals(ucType))
			return Product.TRAM;
		if ("MTR".equals(ucType))
			return Product.TRAM;

		if (ucType.startsWith("BUS"))
			return Product.BUS;

		if ("EXP".equals(ucType))
			return Product.FERRY;
		if ("EXP.BOAT".equals(ucType))
			return Product.FERRY;
		if ("FERRY".equals(ucType))
			return Product.FERRY;
		if ("FER".equals(ucType))
			return Product.FERRY;
		if ("SHIP".equals(ucType))
			return Product.FERRY;
		if ("SHI".equals(ucType))
			return Product.FERRY;

		// skip parsing of "common" lines
		throw new IllegalStateException("cannot normalize type '" + type + "'");
	}
}
