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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Line.Attr;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public final class BvgProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://bvg-apps.hafas.de/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.FERRY,
			Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.ON_DEMAND };

	public BvgProvider()
	{
		super(NetworkId.BVG, API_BASE, "dn", PRODUCTS_MAP);

		setJsonGetStopsUseWeight(false);
		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
	}

	private static final Pattern P_SPLIT_NAME_SU = Pattern.compile("(.*?)(?:\\s+\\((S|U|S\\+U)\\))?");
	private static final Pattern P_SPLIT_NAME_BUS = Pattern.compile("(.*?)(\\s+\\[[^\\]]+\\])?");

	@Override
	protected String[] splitStationName(String name)
	{
		final Matcher mSu = P_SPLIT_NAME_SU.matcher(name);
		if (!mSu.matches())
			throw new IllegalStateException(name);
		name = mSu.group(1);
		final String su = mSu.group(2);

		final Matcher mBus = P_SPLIT_NAME_BUS.matcher(name);
		if (!mBus.matches())
			throw new IllegalStateException(name);
		name = mBus.group(1);

		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
			return new String[] { normalizePlace(mParen.group(2)), (su != null ? su + " " : "") + mParen.group(1) };

		final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { normalizePlace(mComma.group(1)), mComma.group(2) };

		return super.splitStationName(name);
	}

	private String normalizePlace(final String place)
	{
		if ("Bln".equals(place))
			return "Berlin";
		else
			return place;
	}

	@Override
	protected String[] splitPOI(final String poi)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(poi);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(address);
	}

	private static final Pattern P_NORMALIZE_LINE_NAME_TRAM = Pattern.compile("(?:tra|tram)\\s+(.*)", Pattern.CASE_INSENSITIVE);

	@Override
	protected String normalizeLineName(final String lineName)
	{
		final Matcher mTram = P_NORMALIZE_LINE_NAME_TRAM.matcher(lineName);
		if (mTram.matches())
			return mTram.group(1);

		return super.normalizeLineName(lineName);
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		if ("X#".equals(lineAndType))
			return newLine(Product.HIGH_SPEED_TRAIN, "X", null); // InterConnex
		else
			return super.parseLineAndType(lineAndType);
	}

	@Override
	protected Line newLine(final Product product, final String normalizedName, final String comment, final Attr... attrs)
	{
		if (product == Product.SUBURBAN_TRAIN && "S41".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.CIRCLE_CLOCKWISE));
		if (product == Product.SUBURBAN_TRAIN && "S42".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.CIRCLE_ANTICLOCKWISE));

		if (product == Product.BUS && "S41".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.SERVICE_REPLACEMENT, Attr.CIRCLE_CLOCKWISE));
		if (product == Product.BUS && "S42".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.SERVICE_REPLACEMENT, Attr.CIRCLE_ANTICLOCKWISE));

		if (product == Product.BUS && "TXL".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.LINE_AIRPORT));
		if (product == Product.SUBURBAN_TRAIN && "S9".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.LINE_AIRPORT));
		if (product == Product.SUBURBAN_TRAIN && "S45".equals(normalizedName))
			return super.newLine(product, normalizedName, comment, concatAttrs(attrs, Attr.LINE_AIRPORT));

		return super.newLine(product, normalizedName, comment, attrs);
	}

	private Attr[] concatAttrs(final Attr[] attrs1, final Attr... attrs2)
	{
		final int attrs1length = attrs1.length;
		final int attrs2length = attrs2.length;

		final Attr[] newAttrs = new Attr[attrs1length + attrs2length];
		for (int i = 0; i < attrs1length; i++)
			newAttrs[i] = attrs1[i];
		for (int i = 0; i < attrs2length; i++)
			newAttrs[attrs1length + i] = attrs2[i];

		return newAttrs;
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		STYLES.put("SS1", new Style(Style.rgb(221, 77, 174), Style.WHITE));
		STYLES.put("SS2", new Style(Style.rgb(16, 132, 73), Style.WHITE));
		STYLES.put("SS25", new Style(Style.rgb(16, 132, 73), Style.WHITE));
		STYLES.put("SS3", new Style(Style.rgb(22, 106, 184), Style.WHITE));
		STYLES.put("SS41", new Style(Style.rgb(162, 63, 48), Style.WHITE));
		STYLES.put("SS42", new Style(Style.rgb(191, 90, 42), Style.WHITE));
		STYLES.put("SS45", new Style(Style.WHITE, Style.rgb(191, 128, 55), Style.rgb(191, 128, 55)));
		STYLES.put("SS46", new Style(Style.rgb(191, 128, 55), Style.WHITE));
		STYLES.put("SS47", new Style(Style.rgb(191, 128, 55), Style.WHITE));
		STYLES.put("SS5", new Style(Style.rgb(243, 103, 23), Style.WHITE));
		STYLES.put("SS7", new Style(Style.rgb(119, 96, 176), Style.WHITE));
		STYLES.put("SS75", new Style(Style.rgb(119, 96, 176), Style.WHITE));
		STYLES.put("SS8", new Style(Style.rgb(85, 184, 49), Style.WHITE));
		STYLES.put("SS85", new Style(Style.WHITE, Style.rgb(85, 184, 49), Style.rgb(85, 184, 49)));
		STYLES.put("SS9", new Style(Style.rgb(148, 36, 64), Style.WHITE));

		STYLES.put("UU1", new Style(Shape.RECT, Style.rgb(84, 131, 47), Style.WHITE));
		STYLES.put("UU2", new Style(Shape.RECT, Style.rgb(215, 25, 16), Style.WHITE));
		STYLES.put("UU12", new Style(Shape.RECT, Style.rgb(84, 131, 47), Style.rgb(215, 25, 16), Style.WHITE, 0));
		STYLES.put("UU3", new Style(Shape.RECT, Style.rgb(47, 152, 154), Style.WHITE));
		STYLES.put("UU4", new Style(Shape.RECT, Style.rgb(255, 233, 42), Style.BLACK));
		STYLES.put("UU5", new Style(Shape.RECT, Style.rgb(91, 31, 16), Style.WHITE));
		STYLES.put("UU55", new Style(Shape.RECT, Style.rgb(91, 31, 16), Style.WHITE));
		STYLES.put("UU6", new Style(Shape.RECT, Style.rgb(127, 57, 115), Style.WHITE));
		STYLES.put("UU7", new Style(Shape.RECT, Style.rgb(0, 153, 204), Style.WHITE));
		STYLES.put("UU8", new Style(Shape.RECT, Style.rgb(24, 25, 83), Style.WHITE));
		STYLES.put("UU9", new Style(Shape.RECT, Style.rgb(255, 90, 34), Style.WHITE));

		STYLES.put("TM1", new Style(Shape.RECT, Style.parseColor("#eb8614"), Style.WHITE));
		STYLES.put("TM2", new Style(Shape.RECT, Style.parseColor("#68c52f"), Style.WHITE));
		STYLES.put("TM4", new Style(Shape.RECT, Style.parseColor("#cf1b22"), Style.WHITE));
		STYLES.put("TM5", new Style(Shape.RECT, Style.parseColor("#bf8037"), Style.WHITE));
		STYLES.put("TM6", new Style(Shape.RECT, Style.parseColor("#1e5ca2"), Style.WHITE));
		STYLES.put("TM8", new Style(Shape.RECT, Style.parseColor("#f46717"), Style.WHITE));
		STYLES.put("TM10", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("TM13", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		STYLES.put("TM17", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));

		STYLES.put("T12", new Style(Shape.RECT, Style.parseColor("#7d64b2"), Style.WHITE));
		STYLES.put("T16", new Style(Shape.RECT, Style.parseColor("#1e5ca2"), Style.WHITE));
		STYLES.put("T18", new Style(Shape.RECT, Style.parseColor("#f46717"), Style.WHITE));
		STYLES.put("T21", new Style(Shape.RECT, Style.parseColor("#7d64b2"), Style.WHITE));
		STYLES.put("T27", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));
		STYLES.put("T37", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));
		STYLES.put("T50", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		STYLES.put("T60", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("T61", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("T62", new Style(Shape.RECT, Style.parseColor("#125030"), Style.WHITE));
		STYLES.put("T63", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		STYLES.put("T67", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("T68", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));

		STYLES.put("B", new Style(Shape.RECT, Style.parseColor("#993399"), Style.WHITE));
		STYLES.put("BN", new Style(Shape.RECT, Style.BLACK, Style.WHITE));

		STYLES.put("FF1", new Style(Style.BLUE, Style.WHITE)); // Potsdam
		STYLES.put("FF10", new Style(Style.BLUE, Style.WHITE));
		STYLES.put("FF11", new Style(Style.BLUE, Style.WHITE));
		STYLES.put("FF12", new Style(Style.BLUE, Style.WHITE));
		STYLES.put("FF21", new Style(Style.BLUE, Style.WHITE));
		STYLES.put("FF23", new Style(Style.BLUE, Style.WHITE));
		STYLES.put("FF24", new Style(Style.BLUE, Style.WHITE));

		// Regional lines Brandenburg:
		STYLES.put("RRE1", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		STYLES.put("RRE2", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("RRE3", new Style(Shape.RECT, Style.parseColor("#F57921"), Style.WHITE));
		STYLES.put("RRE4", new Style(Shape.RECT, Style.parseColor("#952D4F"), Style.WHITE));
		STYLES.put("RRE5", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		STYLES.put("RRE6", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		STYLES.put("RRE7", new Style(Shape.RECT, Style.parseColor("#00854A"), Style.WHITE));
		STYLES.put("RRE10", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		STYLES.put("RRE11", new Style(Shape.RECT, Style.parseColor("#059EDB"), Style.WHITE));
		STYLES.put("RRE11", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		STYLES.put("RRE15", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("RRE18", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		STYLES.put("RRB10", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		STYLES.put("RRB12", new Style(Shape.RECT, Style.parseColor("#A3238E"), Style.WHITE));
		STYLES.put("RRB13", new Style(Shape.RECT, Style.parseColor("#F68B1F"), Style.WHITE));
		STYLES.put("RRB13", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		STYLES.put("RRB14", new Style(Shape.RECT, Style.parseColor("#A3238E"), Style.WHITE));
		STYLES.put("RRB20", new Style(Shape.RECT, Style.parseColor("#00854A"), Style.WHITE));
		STYLES.put("RRB21", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		STYLES.put("RRB22", new Style(Shape.RECT, Style.parseColor("#0087CB"), Style.WHITE));
		STYLES.put("ROE25", new Style(Shape.RECT, Style.parseColor("#0087CB"), Style.WHITE));
		STYLES.put("RNE26", new Style(Shape.RECT, Style.parseColor("#00A896"), Style.WHITE));
		STYLES.put("RNE27", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		STYLES.put("RRB30", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		STYLES.put("RRB31", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		STYLES.put("RMR33", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		STYLES.put("ROE35", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		STYLES.put("ROE36", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		STYLES.put("RRB43", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		STYLES.put("RRB45", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("ROE46", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		STYLES.put("RMR51", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		STYLES.put("RRB51", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		STYLES.put("RRB54", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("RRB55", new Style(Shape.RECT, Style.parseColor("#F57921"), Style.WHITE));
		STYLES.put("ROE60", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		STYLES.put("ROE63", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("ROE65", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		STYLES.put("RRB66", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		STYLES.put("RPE70", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		STYLES.put("RPE73", new Style(Shape.RECT, Style.parseColor("#00A896"), Style.WHITE));
		STYLES.put("RPE74", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		STYLES.put("T89", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		STYLES.put("RRB91", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		STYLES.put("RRB93", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
	}

	@Override
	public Point[] getArea()
	{
		return new Point[] { Point.fromDouble(52.674189, 13.074604), Point.fromDouble(52.341100, 13.757130) };
	}
}
