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
public class TlemProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.TLEM;
	private final static String API_BASE = "http://www.travelineeastmidlands.co.uk/em/";

	// http://www.travelinesoutheast.org.uk/se/
	// http://www.travelineeastanglia.org.uk/ea/

	public TlemProvider()
	{
		super(API_BASE);

		setTimeZone("Europe/London");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.SUGGEST_LOCATIONS || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Underground".equals(trainName) && trainType == null && name != null)
				return "U" + name;
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}
}
