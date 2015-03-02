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

import java.util.Set;
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class DsbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://mobil.rejseplanen.dk/mobil-bin/";

	// http://dk.hafas.de/bin/fat/
	// http://mobil.rejseplanen.dk/mobil-bin/

	public DsbProvider()
	{
		super(NetworkId.DSB, API_BASE + "stboard.exe/mn", API_BASE + "ajax-getstop.exe/mn", API_BASE + "query.exe/dn", 11);

		setStationBoardHasStationTable(false);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.REGIONAL_TRAIN;
		if (value == 8)
			return Product.REGIONAL_TRAIN;
		if (value == 16)
			return Product.SUBURBAN_TRAIN;
		if (value == 32)
			return Product.BUS;
		if (value == 64)
			return Product.BUS;
		if (value == 128)
			return Product.BUS;
		if (value == 256)
			return Product.BUS;
		if (value == 512)
			return Product.FERRY;
		if (value == 1024)
			return Product.SUBWAY;

		throw new IllegalArgumentException("cannot handle: " + value);
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
			productBits.setCharAt(10, '1'); // Metro
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
			productBits.setCharAt(9, '1'); // Fähre
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_PAREN.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(name);
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

		if ("ICL".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;
		if ("IB".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;
		if ("SJ".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;

		if ("ØR".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("RA".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("RX".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("PP".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("S-TOG".equals(ucType))
			return Product.SUBURBAN_TRAIN;

		if ("BYBUS".equals(ucType))
			return Product.BUS;
		if ("X-BUS".equals(ucType) || "X BUS".equals(ucType))
			return Product.BUS;
		if ("HV-BUS".equals(ucType)) // Havnebus
			return Product.BUS;
		if ("T-BUS".equals(ucType)) // Togbus
			return Product.BUS;
		if ("TOGBUS".equals(ucType))
			return Product.BUS;

		if ("TELEBUS".equals(ucType))
			return Product.ON_DEMAND;
		if ("TELETAXI".equals(ucType))
			return Product.ON_DEMAND;

		if ("FÆRGE".equals(ucType))
			return Product.FERRY;

		return super.normalizeType(type);
	}
}
