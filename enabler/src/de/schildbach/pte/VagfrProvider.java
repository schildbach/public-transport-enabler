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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class VagfrProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "https://efaserver.vag-freiburg.de/vagfr/";

	public VagfrProvider()
	{
		super(NetworkId.VAGFR, API_BASE);

		setUseRouteIndexAsTripId(false);
		setStyles(STYLES);
		setSessionCookieName("EFABWLB");
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if (("N".equals(trainType) || "Nahverkehrszug".equals(trainName)) && trainNum != null)
				return new Line(id, network, Product.REGIONAL_TRAIN, "N" + trainNum);
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Tram
		STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#33b540"), Style.WHITE));
		STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#f79210"), Style.WHITE));
		STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#ef58a1"), Style.WHITE));
		STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#0994ce"), Style.WHITE));

		// Nachtbus
		STYLES.put("N46", new Style(Style.parseColor("#28bda5"), Style.WHITE));
		STYLES.put("N47", new Style(Style.parseColor("#d6de20"), Style.WHITE));
	}
}
