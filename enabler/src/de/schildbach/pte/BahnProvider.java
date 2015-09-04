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
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public final class BahnProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://reiseauskunft.bahn.de/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND, null, null,
			null, null };

	public BahnProvider()
	{
		super(NetworkId.DB, API_BASE, "dn", PRODUCTS_MAP);

		setStationBoardEndpoint("http://mobile.bahn.de/bin/mobil/bhftafel.exe/dn");
		setStationBoardHasStationTable(false);
		setJsonGetStopsUseWeight(false);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final Pattern P_SPLIT_NAME_ONE_COMMA = Pattern.compile("([^,]*), ([^,]*)");

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

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

	private static final Pattern P_NORMALIZE_LINE_NAME_TRAM = Pattern.compile("str\\s+(.*)", Pattern.CASE_INSENSITIVE);

	@Override
	protected String normalizeLineName(final String lineName)
	{
		final Matcher mTram = P_NORMALIZE_LINE_NAME_TRAM.matcher(lineName);
		if (mTram.matches())
			return mTram.group(1);

		return super.normalizeLineName(lineName);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("N".equals(ucType))
			return null;

		return super.normalizeType(type);
	}
}
