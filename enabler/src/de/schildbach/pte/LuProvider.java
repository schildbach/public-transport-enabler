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

import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class LuProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://mobiliteitszentral.hafas.de/hafas/";

	public LuProvider()
	{
		super(NetworkId.LU, API_BASE + "stboard.exe/fn", API_BASE + "ajax-getstop.exe/fn", API_BASE + "query.exe/fn", 9, Charsets.UTF_8);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.HIGH_SPEED_TRAIN;
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

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(1, '1'); // IC/EC
			productBits.setCharAt(2, '1'); // Fernverkehrszug
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(3, '1'); // Regionalverkehrszug
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == Product.SUBWAY)
		{
		}
		else if (product == Product.TRAM)
		{
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1');
			productBits.setCharAt(6, '1');
			productBits.setCharAt(7, '1');
			productBits.setCharAt(8, '1');
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
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
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

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
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("CRE".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("CITYBUS".equals(ucType))
			return Product.BUS;
		if ("NIGHTBUS".equals(ucType))
			return Product.BUS;
		if ("DIFFBUS".equals(ucType))
			return Product.BUS;
		if ("NAVETTE".equals(ucType))
			return Product.BUS;

		return super.normalizeType(type);
	}
}
