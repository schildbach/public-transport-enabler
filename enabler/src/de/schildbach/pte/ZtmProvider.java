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

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class ZtmProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://wyszukiwarka.ztm.waw.pl/bin/";
	private static final Product[] PRODUCTS_MAP = { null, null, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS,
			Product.TRAM, Product.SUBWAY, null, Product.FERRY };

	public ZtmProvider()
	{
		super(NetworkId.ZTM, API_BASE, "pn", PRODUCTS_MAP);
	}

	@Override
	public boolean hasCapability(final Capability capability)
	{
		if (capability == Capability.DEPARTURES)
			return true;
		else
			return super.hasCapability(capability);
	}
}
