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

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class VbbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fahrinfo.vbb.de/bin/";
	private static final Set<Product> ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND = EnumSet.complementOf(EnumSet
			.of(Product.HIGH_SPEED_TRAIN, Product.ON_DEMAND));

	public VbbProvider()
	{
		super(NetworkId.VBB, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 7, Charsets.UTF_8);

		setJsonGetStopsUseWeight(false);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.SUBURBAN_TRAIN;
		if (value == 2)
			return Product.SUBWAY;
		if (value == 4)
			return Product.TRAM;
		if (value == 8)
			return Product.BUS;
		if (value == 16)
			return Product.FERRY;
		if (value == 32)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 64)
			return Product.REGIONAL_TRAIN;

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(6, '1');
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(0, '1');
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(1, '1');
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(2, '1');
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(3, '1');
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_SU = Pattern.compile("(.*?)(?:\\s+\\((S|U|S\\+U)\\))?");
	private static final Pattern P_SPLIT_NAME_BUS = Pattern.compile("(.*?)(\\s+\\[[^\\]]+\\])?");

	@Override
	protected String[] splitStationName(String name)
	{
		final Matcher mSu = P_SPLIT_NAME_SU.matcher(name);
		if (!mSu.matches())
			throw new IllegalStateException(name);
		name = mSu.group(1);
		final String su = mSu.group(2);

		final Matcher mBus = P_SPLIT_NAME_BUS.matcher(name);
		if (!mBus.matches())
			throw new IllegalStateException(name);
		name = mBus.group(1);

		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
			return new String[] { normalizePlace(mParen.group(2)), (su != null ? su + " " : "") + mParen.group(1) };

		final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { normalizePlace(mComma.group(1)), mComma.group(2) };

		return super.splitStationName(name);
	}

	private String normalizePlace(final String place)
	{
		if ("Bln".equals(place))
			return "Berlin";
		else
			return place;
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
	public Set<Product> defaultProducts()
	{
		return ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND;
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		if ("X#".equals(lineAndType))
			return newLine(Product.HIGH_SPEED_TRAIN, "X", null); // InterConnex
		else
			return super.parseLineAndType(lineAndType);
	}
}
