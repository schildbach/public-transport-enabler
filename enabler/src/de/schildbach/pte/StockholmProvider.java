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
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class StockholmProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.STOCKHOLM;
	private static final String API_BASE = "http://reseplanerare.trafiken.nu/bin/";

	public StockholmProvider()
	{
		super(API_BASE + "stboard.exe/sn", API_BASE + "ajax-getstop.exe/sny", API_BASE + "query.exe/sn", 7);

		setStyles(STYLES);
		setStationBoardHasStationTable(false);
		setStationBoardCanDoEquivs(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1) // Pendeltåg
			return 'S';
		if (value == 2) // Tunnelbana
			return 'U';
		if (value == 4) // Lokalbanor
			return 'R';
		if (value == 8) // Bussar
			return 'B';
		if (value == 16) // Flygbussar
			return 'B';
		if (value == 32)
			return 'F';
		if (value == 64) // Waxholmsbåtar
			return 'F';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Lokalbanor
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Pendeltåg
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(1, '1'); // Tunnelbana
		}
		else if (product == Product.TRAM)
		{
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(3, '1'); // Bussar
			productBits.setCharAt(4, '1'); // Flygbussar
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(6, '1'); // Waxholmsbåtar
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{4,}?)\\)");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
			return new String[] { mParen.group(2), mParen.group(1) };

		return super.splitPlaceAndName(name);
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
			final String number = m.group(2).replaceAll("\\s+", " ");

			if (type.length() > 0)
			{
				final char normalizedType = normalizeType(type);
				if (normalizedType != 0)
					return newLine(normalizedType, number, null);
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line#type " + lineAndType);
		}

		throw new IllegalStateException("cannot normalize line#type " + lineAndType);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("TRAIN".equals(ucType))
			return 'R';
		if ("NÄRTRAFIKEN".equals(ucType))
			return 'R';
		if ("LOKALTÅG".equals(ucType))
			return 'R';

		if ("PENDELTÅG".equals(ucType))
			return 'S';

		if ("METRO".equals(ucType))
			return 'U';
		if ("TUNNELBANA".equals(ucType))
			return 'U';

		if ("TRAM".equals(ucType))
			return 'T';

		if ("BUS".equals(ucType))
			return 'B';
		if ("BUSS".equals(ucType))
			return 'B';
		if ("FLYG".equals(ucType))
			return 'B';

		if ("SHIP".equals(ucType))
			return 'F';
		if ("BÅT".equals(ucType))
			return 'F';
		if ("FÄRJA".equals(ucType))
			return 'F';

		return 0;
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
