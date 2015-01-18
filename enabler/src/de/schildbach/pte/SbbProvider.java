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
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class SbbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SBB;
	private static final String API_BASE = "http://fahrplan.sbb.ch/bin/";

	public SbbProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 10);

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
			return 'F';
		if (value == 32)
			return 'S';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'C';
		if (value == 256)
			return 'R';
		if (value == 512)
			return 'T';

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
	public Collection<Product> defaultProducts()
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
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("IN".equals(ucType)) // Italien Roma-Lecce
			return 'I';

		if ("E".equals(ucType))
			return 'R';
		if ("T".equals(ucType))
			return 'R';

		if ("TX".equals(ucType))
			return 'B';
		if ("NFO".equals(ucType))
			return 'B';
		if ("KB".equals(ucType)) // Kleinbus?
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
