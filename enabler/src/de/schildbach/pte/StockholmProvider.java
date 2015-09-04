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
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class StockholmProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://reseplanerare.trafiken.nu/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.REGIONAL_TRAIN, Product.BUS, Product.BUS,
			Product.FERRY, Product.FERRY };

	public StockholmProvider()
	{
		super(NetworkId.STOCKHOLM, API_BASE, "sn", PRODUCTS_MAP);

		setStyles(STYLES);
		setStationBoardHasStationTable(false);
		setStationBoardCanDoEquivs(false);
	}

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_PAREN.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(address);
	}

	@Override
	protected void appendCommonQueryTripsBinaryParameters(final StringBuilder uri)
	{
		super.appendCommonQueryTripsBinaryParameters(uri);

		uri.append("&REQ0HafasSearchIndividual=1&REQ0HafasSearchPublic=1"
				+ "&existIntermodalDep_enable=yes&existIntermodalDest_enable=yes&existTotal_enable=yes"
				+ "&REQ0JourneyDep_Foot_enable=1&REQ0JourneyDep_Foot_maxDist=5000&REQ0JourneyDep_Foot_minDist=0&REQ0JourneyDep_Foot_speed=100&REQ0JourneyDep_Bike_enable=0&REQ0JourneyDep_ParkRide_enable=0"
				+ "&REQ0JourneyDest_Foot_enable=1&REQ0JourneyDest_Foot_maxDist=5000&REQ0JourneyDest_Foot_minDist=0&REQ0JourneyDest_Foot_speed=100&REQ0JourneyDest_Bike_enable=0&REQ0JourneyDest_ParkRide_enable=0");
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(lineAndType);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2).replaceAll("\\s+", "");

			if (type.length() > 0)
			{
				final Product product = normalizeType(type);
				if (product != null)
					return newLine(product, number, null);
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line#type " + lineAndType);
		}

		throw new IllegalStateException("cannot normalize line#type " + lineAndType);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("TRAIN".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("NÄRTRAFIKEN".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("LOKALTÅG".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("PENDELTÅG".equals(ucType))
			return Product.SUBURBAN_TRAIN;

		if ("METRO".equals(ucType))
			return Product.SUBWAY;
		if ("TUNNELBANA".equals(ucType))
			return Product.SUBWAY;

		if ("TRAM".equals(ucType))
			return Product.TRAM;

		if ("BUS".equals(ucType))
			return Product.BUS;
		if ("BUSS".equals(ucType))
			return Product.BUS;
		if ("FLYG".equals(ucType))
			return Product.BUS;

		if ("SHIP".equals(ucType))
			return Product.FERRY;
		if ("BÅT".equals(ucType))
			return Product.FERRY;
		if ("FÄRJA".equals(ucType))
			return Product.FERRY;

		// skip parsing of "common" lines
		throw new IllegalStateException("cannot normalize type '" + type + "'");
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		STYLES.put("UMETRO10", new Style(Shape.ROUNDED, Style.parseColor("#25368b"), Style.WHITE));
		STYLES.put("UMETRO11", new Style(Shape.ROUNDED, Style.parseColor("#25368b"), Style.WHITE));

		STYLES.put("UMETRO13", new Style(Shape.ROUNDED, Style.parseColor("#f1491c"), Style.WHITE));
		STYLES.put("UMETRO14", new Style(Shape.ROUNDED, Style.parseColor("#f1491c"), Style.WHITE));

		STYLES.put("UMETRO17", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
		STYLES.put("UMETRO18", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
		STYLES.put("UMETRO19", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
	}
}
