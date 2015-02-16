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

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class VmsProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://www.vms.de/vms2/";

	public VmsProvider()
	{
		super(NetworkId.VMS, API_BASE);
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Ilztalbahn".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, "ITB");
			if ("Meridian".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, "M");
			if ("CityBahn".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, "CB");

			if ("RE 3".equals(symbol) && "Zug".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "RE3");
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
