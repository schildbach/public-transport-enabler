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
public class VvoProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "http://efa.vvo-online.de:8080/dvb/";

	public VvoProvider()
	{
		this(API_BASE);
	}

	public VvoProvider(final String apiBase)
	{
		super(NetworkId.VVO, apiBase);

		setUseRealtime(false);
		setRequestUrlEncoding(Charsets.UTF_8);
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Twoje Linie Kolejowe".equals(trainName) && symbol != null)
				return new Line(id, network, Product.HIGH_SPEED_TRAIN, "TLK" + symbol);

			if ("Regionalbahn".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, null);
			if ("Ostdeutsche Eisenbahn GmbH".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "OE");
			if ("Meridian".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "M");
			if ("trilex".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "TLX");
			if ("Trilex".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, "TLX");
			if ("U28".equals(symbol)) // Nationalparkbahn
				return new Line(id, network, Product.REGIONAL_TRAIN, "U28");

			if ("Fernbus".equals(trainName) && trainNum == null)
				return new Line(id, network, Product.BUS, trainName);
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
