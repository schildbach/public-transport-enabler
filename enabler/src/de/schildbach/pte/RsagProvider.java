/*
 * Copyright 2014-2015 the original author or authors.
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
import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class RsagProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fahrplan.rsag-online.de/hafas/";

	// http://fahrplanauskunft.verkehrsverbund-warnow.de/bin/

	public RsagProvider()
	{
		super(NetworkId.RSAG, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 10, Charsets.UTF_8);

		setStyles(STYLES);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 8)
			return Product.REGIONAL_TRAIN;
		if (value == 16)
			return Product.SUBURBAN_TRAIN;
		if (value == 32)
			return Product.BUS;
		if (value == 64)
			return Product.FERRY;
		if (value == 128)
			return Product.SUBWAY;
		if (value == 256)
			return Product.TRAM;
		if (value == 512)
			return Product.ON_DEMAND;

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // ICE, TGV, Thalys, etc.
			productBits.setCharAt(1, '1'); // IC/EC
			productBits.setCharAt(2, '1'); // sonstiger Schnellzug
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(3, '1'); // Regionalzug
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(7, '1'); // U-Bahn
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(8, '1'); // Straßenbahn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1'); // Bus
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(9, '1'); // Anrufverkehre
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(6, '1'); // Fähre
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Rostock", "Warnemünde" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(address);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected void appendJsonGetStopsParameters(final StringBuilder uri, final CharSequence constraint, final int maxStops)
	{
		super.appendJsonGetStopsParameters(uri, constraint, maxStops);

		uri.append("&REQ0JourneyStopsS0B=5");
		uri.append("&REQ0JourneyStopsS0F=distinguishPerimeterFilter;12140548;54126457;50");
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Rostock
		STYLES.put("SS1", new Style(Shape.CIRCLE, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("SS2", new Style(Shape.CIRCLE, Style.parseColor("#66a933"), Style.WHITE));
		STYLES.put("SS3", new Style(Shape.CIRCLE, Style.parseColor("#00a063"), Style.WHITE));

		STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#712090"), Style.WHITE));
		STYLES.put("TTram1", new Style(Shape.RECT, Style.parseColor("#712090"), Style.WHITE));
		STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#d136a3"), Style.WHITE));
		STYLES.put("TTram2", new Style(Shape.RECT, Style.parseColor("#d136a3"), Style.WHITE));
		STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#870e12"), Style.WHITE));
		STYLES.put("TTram3", new Style(Shape.RECT, Style.parseColor("#870e12"), Style.WHITE));
		STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#f47216"), Style.WHITE));
		STYLES.put("TTram4", new Style(Shape.RECT, Style.parseColor("#f47216"), Style.WHITE));
		STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("TTram5", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T6", new Style(Shape.RECT, Style.parseColor("#fab20b"), Style.WHITE));
		STYLES.put("TTram6", new Style(Shape.RECT, Style.parseColor("#fab20b"), Style.WHITE));

		STYLES.put("B15", new Style(Style.parseColor("#008dc6"), Style.WHITE));
		STYLES.put("B16", new Style(Style.parseColor("#1d3c85"), Style.WHITE));
		STYLES.put("B17", new Style(Style.parseColor("#5784cc"), Style.WHITE));
		STYLES.put("B18", new Style(Style.parseColor("#0887c9"), Style.WHITE));
		STYLES.put("B19", new Style(Style.parseColor("#202267"), Style.WHITE));
		STYLES.put("PALT19A", new Style(Style.WHITE, Style.parseColor("#202267")));
		STYLES.put("PALT20A", new Style(Style.WHITE, Style.parseColor("#1959a6")));
		STYLES.put("B22", new Style(Style.parseColor("#3871c1"), Style.WHITE));
		STYLES.put("B23", new Style(Style.parseColor("#009ddb"), Style.WHITE));
		STYLES.put("B25", new Style(Style.parseColor("#0994dc"), Style.WHITE));
		STYLES.put("B26", new Style(Style.parseColor("#0994dc"), Style.WHITE));
		STYLES.put("B27", new Style(Style.parseColor("#6e87cd"), Style.WHITE));
		STYLES.put("B28", new Style(Style.parseColor("#4fc6f4"), Style.WHITE));
		STYLES.put("PALT30A", new Style(Style.WHITE, Style.parseColor("#1082ce")));
		STYLES.put("B31", new Style(Style.parseColor("#3a9fdf"), Style.WHITE));
		STYLES.put("B33", new Style(Style.parseColor("#21518d"), Style.WHITE));
		STYLES.put("B35", new Style(Style.parseColor("#1969bc"), Style.WHITE));
		STYLES.put("PALT35A", new Style(Style.WHITE, Style.parseColor("#1969bc")));
		STYLES.put("B36", new Style(Style.parseColor("#2c6d8b"), Style.WHITE));
		STYLES.put("B37", new Style(Style.parseColor("#36aee8"), Style.WHITE));
		STYLES.put("B38", new Style(Style.parseColor("#173e7d"), Style.WHITE));
		STYLES.put("B45", new Style(Style.parseColor("#66cef5"), Style.WHITE));
		STYLES.put("PALT45A", new Style(Style.WHITE, Style.parseColor("#66cef5")));
		STYLES.put("B49", new Style(Style.parseColor("#166ab8"), Style.WHITE));
		STYLES.put("BF1", new Style(Style.parseColor("#656263"), Style.WHITE));
		STYLES.put("PALTF1A", new Style(Style.WHITE, Style.parseColor("#656263")));
		STYLES.put("BF2", new Style(Style.parseColor("#9c9a9b"), Style.WHITE));

		STYLES.put("F", new Style(Shape.CIRCLE, Style.parseColor("#00adef"), Style.WHITE));
	}
}
