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

	public NriProvider()
	{
		super(NetworkId.NRI, API_BASE + "stboard.exe/on", API_BASE + "ajax-getstop.exe/ony", API_BASE + "query.exe/on", 8);

		setJsonGetStopsEncoding(Charsets.UTF_8);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1) // Air
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.REGIONAL_TRAIN;
		if (value == 4)
			return Product.BUS;
		if (value == 8)
			return Product.TRAM;
		if (value == 16)
			return Product.SUBWAY;
		if (value == 32)
			return Product.FERRY;
		if (value == 64)
			return Product.FERRY;
		if (value == 128)
			return Product.FERRY;

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Flugzeug
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(1, '1'); // Regionalverkehrszug
			productBits.setCharAt(7, '1'); // Tourismus-Züge
			productBits.setCharAt(2, '1'); // undokumentiert
		}
		else if (product == Product.SUBURBAN_TRAIN || product == Product.TRAM)
		{
			productBits.setCharAt(3, '1'); // Stadtbahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(2, '1'); // Bus
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(5, '1'); // Express-Boot
			productBits.setCharAt(6, '1'); // Schiff
			productBits.setCharAt(7, '1'); // Fähre
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
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
