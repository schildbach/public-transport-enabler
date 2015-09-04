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
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public RsagProvider()
	{
		super(NetworkId.RSAG, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
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
		STYLES.put("SS1", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));
		STYLES.put("SS2", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));
		STYLES.put("SS3", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));

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

		STYLES.put("B101", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B102", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		STYLES.put("B103", new Style(Style.parseColor("#d18e00"), Style.WHITE));
		STYLES.put("B104", new Style(Style.parseColor("#006f9d"), Style.WHITE));
		STYLES.put("B105", new Style(Style.parseColor("#c2a712"), Style.WHITE));
		STYLES.put("B106", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B107", new Style(Style.parseColor("#a52240"), Style.WHITE));
		STYLES.put("B108", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		STYLES.put("B109", new Style(Style.parseColor("#a97ea6"), Style.WHITE));
		STYLES.put("B110", new Style(Style.parseColor("#95c11e"), Style.WHITE));
		STYLES.put("B111", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B112", new Style(Style.parseColor("#e50068"), Style.WHITE));
		STYLES.put("B113", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B114", new Style(Style.parseColor("#925b00"), Style.WHITE));
		STYLES.put("B115", new Style(Style.parseColor("#74b958"), Style.WHITE));
		STYLES.put("B116", new Style(Style.parseColor("#0084ab"), Style.WHITE));
		STYLES.put("B118", new Style(Style.parseColor("#4a96d1"), Style.WHITE));
		STYLES.put("B119", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("B120", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("B121", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B123", new Style(Style.parseColor("#f39200"), Style.WHITE));
		STYLES.put("B124", new Style(Style.parseColor("#004f9e"), Style.WHITE));
		STYLES.put("B125", new Style(Style.parseColor("#e7ac00"), Style.WHITE));
		STYLES.put("B128", new Style(Style.parseColor("#e50068"), Style.WHITE));
		STYLES.put("B129", new Style(Style.parseColor("#e5007d"), Style.WHITE));
		STYLES.put("B131", new Style(Style.parseColor("#12a537"), Style.WHITE));
		STYLES.put("B132", new Style(Style.parseColor("#ef7c00"), Style.WHITE));
		STYLES.put("B134", new Style(Style.parseColor("#008e5c"), Style.WHITE));
		STYLES.put("B135", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B136", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B137", new Style(Style.parseColor("#ef7c00"), Style.WHITE));
		STYLES.put("B138", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B139", new Style(Style.parseColor("#f8ac00"), Style.WHITE));
		STYLES.put("B140", new Style(Style.parseColor("#c2a712"), Style.WHITE));
		STYLES.put("B200", new Style(Style.parseColor("#e5007d"), Style.WHITE));
		STYLES.put("B201", new Style(Style.parseColor("#009440"), Style.WHITE));
		STYLES.put("B203", new Style(Style.parseColor("#f49a00"), Style.WHITE));
		STYLES.put("B204", new Style(Style.parseColor("#9fc41c"), Style.WHITE));
		STYLES.put("B205", new Style(Style.parseColor("#dc6ba5"), Style.WHITE));
		STYLES.put("B208", new Style(Style.parseColor("#004f94"), Style.WHITE));
		STYLES.put("B210", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B211", new Style(Style.parseColor("#95c11e"), Style.WHITE));
		STYLES.put("B213", new Style(Style.parseColor("#a777b2"), Style.WHITE));
		STYLES.put("B215", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		STYLES.put("B216", new Style(Style.parseColor("#fabd5d"), Style.WHITE));
		STYLES.put("B220", new Style(Style.parseColor("#0090d6"), Style.WHITE));
		STYLES.put("B221", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B222", new Style(Style.parseColor("#ef87b5"), Style.WHITE));
		STYLES.put("B223", new Style(Style.parseColor("#f7a600"), Style.WHITE));
		STYLES.put("B224", new Style(Style.parseColor("#004f9e"), Style.WHITE));
		STYLES.put("B228", new Style(Style.parseColor("#003d7c"), Style.WHITE));
		STYLES.put("B229", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B230", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("B231", new Style(Style.parseColor("#00843d"), Style.WHITE));
		STYLES.put("B232", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B233", new Style(Style.parseColor("#113274"), Style.WHITE));
		STYLES.put("B234", new Style(Style.parseColor("#ea5197"), Style.WHITE));
		STYLES.put("B235", new Style(Style.parseColor("#ba0066"), Style.WHITE));
		STYLES.put("B240", new Style(Style.parseColor("#942642"), Style.WHITE));
		STYLES.put("B241", new Style(Style.parseColor("#ea5197"), Style.WHITE));
		STYLES.put("B242", new Style(Style.parseColor("#f39200"), Style.WHITE));
		STYLES.put("B243", new Style(Style.parseColor("#fbb900"), Style.WHITE));
		STYLES.put("B244", new Style(Style.parseColor("#f7aa59"), Style.WHITE));
		STYLES.put("B245", new Style(Style.parseColor("#76b72a"), Style.WHITE));
		STYLES.put("B246", new Style(Style.parseColor("#f39a8b"), Style.WHITE));
		STYLES.put("B247", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		STYLES.put("B250", new Style(Style.parseColor("#0080c8"), Style.WHITE));
		STYLES.put("B251", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("B252", new Style(Style.parseColor("#e41a18"), Style.WHITE));
		STYLES.put("B260", new Style(Style.parseColor("#e5007d"), Style.WHITE));
		STYLES.put("B270", new Style(Style.parseColor("#fabd5d"), Style.WHITE));
		STYLES.put("B271", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("B272", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		STYLES.put("B273", new Style(Style.parseColor("#004899"), Style.WHITE));
		STYLES.put("B280", new Style(Style.parseColor("#e41a18"), Style.WHITE));
		STYLES.put("B281", new Style(Style.parseColor("#f8ac00"), Style.WHITE));
		STYLES.put("B282", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("B283", new Style(Style.parseColor("#eb609f"), Style.WHITE));
		STYLES.put("B284", new Style(Style.parseColor("#951b81"), Style.WHITE));
		STYLES.put("B285", new Style(Style.parseColor("#a42422"), Style.WHITE));
		STYLES.put("B286", new Style(Style.parseColor("#e5007d"), Style.WHITE));
		STYLES.put("B290", new Style(Style.parseColor("#302683"), Style.WHITE));
		STYLES.put("B291", new Style(Style.parseColor("#a61680"), Style.WHITE));
		STYLES.put("B292", new Style(Style.parseColor("#c9be46"), Style.WHITE));

		STYLES.put("F", new Style(Shape.CIRCLE, Style.parseColor("#17a4da"), Style.WHITE));
	}
}
