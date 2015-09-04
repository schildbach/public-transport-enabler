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
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.BUS, Product.BUS, Product.BUS, Product.BUS };

	public LuProvider()
	{
		super(NetworkId.LU, API_BASE, "fn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
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
