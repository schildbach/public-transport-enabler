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

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class VmsProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VMS;
	private static final String API_BASE = "http://www.vms.de/vms2/";

	public VmsProvider()
	{
		super(API_BASE);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected Line parseLine(final String id, final String mot, final String symbol, final String name, final String longName,
			final String trainType, final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Ilztalbahn".equals(trainName) && trainNum == null)
				return new Line(id, Product.REGIONAL_TRAIN, "ITB");
			if ("Meridian".equals(trainName) && trainNum == null)
				return new Line(id, Product.REGIONAL_TRAIN, "M");
			if ("CityBahn".equals(trainName) && trainNum == null)
				return new Line(id, Product.REGIONAL_TRAIN, "CB");

			if ("RE 3".equals(symbol) && "Zug".equals(longName))
				return new Line(id, Product.REGIONAL_TRAIN, "RE3");
		}

		return super.parseLine(id, mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
