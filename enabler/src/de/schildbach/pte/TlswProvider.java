/*
 * Copyright 2010-2014 the original author or authors.
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
public class TlswProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.TLSW;
	private final static String API_BASE = "http://www.travelinesw.com/swe/";

	public TlswProvider()
	{
		super(API_BASE);

		setTimeZone("Europe/London");
		setUseRouteIndexAsTripId(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected String normalizeLocationName(final String name)
	{
		final String normalizedName = super.normalizeLocationName(name);
		if (normalizedName != null && normalizedName.endsWith(" ()"))
			return normalizedName.substring(0, normalizedName.length() - 3);
		else
			return normalizedName;
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}
}
