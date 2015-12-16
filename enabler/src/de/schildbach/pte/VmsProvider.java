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

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
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

		setUseLineRestriction(false);
	}

	@Override
	protected String xsltTripRequestParameters(final Location from, final @Nullable Location via, final Location to, final Date time,
			final boolean dep, final @Nullable Collection<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options)
	{
		final StringBuilder uri = new StringBuilder(
				super.xsltTripRequestParameters(from, via, to, time, dep, products, optimize, walkSpeed, accessibility, options));

		uri.append("&inclMOT_11=on");
		uri.append("&inclMOT_13=on");
		uri.append("&inclMOT_14=on");
		uri.append("&inclMOT_15=on");
		uri.append("&inclMOT_16=on");
		uri.append("&inclMOT_17=on");

		return uri.toString();
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
			if ("CityBahn".equals(longName) && "C11".equals(symbol))
				return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
			if (trainType == null && ("C11".equals(trainNum) || "C13".equals(trainNum) || "C14".equals(trainNum) || "C15".equals(trainNum)))
				return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
			if ("Zug".equals(longName) && ("C11".equals(symbol) || "C13".equals(symbol) || "C14".equals(symbol) || "C15".equals(symbol)))
				return new Line(id, network, Product.REGIONAL_TRAIN, symbol);

			if ("RE 3".equals(symbol) && "Zug".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "RE3");
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
