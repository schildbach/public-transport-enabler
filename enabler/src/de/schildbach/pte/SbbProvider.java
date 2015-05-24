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
public class SbbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fahrplan.sbb.ch/bin/";

	public SbbProvider()
	{
		super(NetworkId.SBB, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 10);

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
			return Product.FERRY;
		if (value == 32)
			return Product.SUBURBAN_TRAIN;
		if (value == 64)
			return Product.BUS;
		if (value == 128)
			return Product.CABLECAR;
		if (value == 256)
			return Product.REGIONAL_TRAIN;
		if (value == 512)
			return Product.TRAM;

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // ICE/TGV/IRJ
			productBits.setCharAt(1, '1'); // EC/IC
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // IR
			productBits.setCharAt(3, '1'); // RE/D
			productBits.setCharAt(8, '1'); // ARZ/EXT
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(5, '1'); // S/SN/R
		}
		else if (product == Product.SUBWAY || product == Product.TRAM)
		{
			productBits.setCharAt(9, '1'); // Tram/Metro
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(6, '1'); // Bus
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(4, '1'); // Schiff
		}
		else if (product == Product.CABLECAR)
		{
			productBits.setCharAt(7, '1'); // Seilbahn
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
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

		if ("IN".equals(ucType)) // Italien Roma-Lecce
			return Product.HIGH_SPEED_TRAIN;
		if ("IT".equals(ucType)) // Italien Roma-Venezia
			return Product.HIGH_SPEED_TRAIN;

		if ("T".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("TE2".equals(ucType)) // Basel - Strasbourg
			return Product.REGIONAL_TRAIN;

		if ("TX".equals(ucType))
			return Product.BUS;
		if ("NFO".equals(ucType))
			return Product.BUS;
		if ("KB".equals(ucType)) // Kleinbus?
			return Product.BUS;

		return super.normalizeType(type);
	}
}
