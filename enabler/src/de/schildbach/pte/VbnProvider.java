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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class VbnProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "https://fahrplaner.vbn.de/hafas/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public VbnProvider()
	{
		super(NetworkId.VBN, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dny", API_BASE + "query.exe/dn", PRODUCTS_MAP, Charsets.UTF_8);
	}

	private static final String[] PLACES = { "Bremen", "Bremerhaven", "Oldenburg(Oldb)", "Osnabrück", "Göttingen" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitStationName(name);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("SEILB".equals(ucType))
			return Product.CABLECAR;

		return super.normalizeType(type);
	}
}
