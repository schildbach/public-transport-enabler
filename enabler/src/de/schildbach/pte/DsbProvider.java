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

import java.util.Collection;

import de.schildbach.pte.dto.Product;

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
		super(API_BASE + "stboard.exe/mn", API_BASE + "ajax-getstop.exe/mn", API_BASE + "query.exe/dn", 11);

		setStationBoardHasStationTable(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 2)
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'S';
		if (value == 32)
			return 'B';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'B';
		if (value == 512)
			return 'F';
		if (value == 1024)
			return 'U';

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
		if ("IB".equals(ucType))
			return 'I';
		if ("SJ".equals(ucType))
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
		if ("X-BUS".equals(ucType) || "X BUS".equals(ucType))
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
