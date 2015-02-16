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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class VrnProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://fahrplanauskunft.vrn.de/vrn/";

	// http://fahrplanauskunft.vrn.de/vrn_mobile/
	// http://efa9.vrn.de/vrt/

	public VrnProvider()
	{
		super(NetworkId.VRN, API_BASE);

		setRequestUrlEncoding(Charsets.UTF_8);
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String mot, final @Nullable String symbol, final @Nullable String name,
			final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if ("InterRegio".equals(longName) && symbol == null)
				return new Line(id, Product.REGIONAL_TRAIN, "IR");
		}

		return super.parseLine(id, mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
