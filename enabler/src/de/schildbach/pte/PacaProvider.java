/*
 * Copyright 2014-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import de.schildbach.pte.dto.Product;

/**
 * @author Kjell Braden <afflux@pentabarf.de>
 */
public class PacaProvider extends AbstractTsiProvider
{
	public PacaProvider()
	{
		super(NetworkId.PACA, "PACA", "http://www.pacamobilite.fr/WebServices/TransinfoService/api");
	}

	@Override
	protected String translateToLocalProduct(final Product p)
	{
		switch (p)
		{
			case HIGH_SPEED_TRAIN:
				return "RAPID_TRANSIT";
			case REGIONAL_TRAIN:
				return "TRAIN|LONG_DISTANCE_TRAIN";
			case SUBURBAN_TRAIN:
				return "LOCAL_TRAIN";
			case SUBWAY:
				return "METRO";
			case TRAM:
				return "TRAMWAY";
			case BUS:
				return "BUS|COACH";
			case ON_DEMAND:
				return "TOD";
			case FERRY:
				return "FERRY";
			case CABLECAR:
			default:
				return null;
		}
	}
}
