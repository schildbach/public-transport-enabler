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
	// http://www.dsb.dk/Rejseplan/bin/
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.BUS, Product.BUS, Product.BUS, Product.FERRY, Product.SUBWAY };

	public DsbProvider()
	{
		super(NetworkId.DSB, API_BASE, "mn", PRODUCTS_MAP);

		setStationBoardHasStationTable(false);
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
