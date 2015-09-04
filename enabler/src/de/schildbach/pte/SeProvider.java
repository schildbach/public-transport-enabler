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

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class SeProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://samtrafiken.hafas.de/bin/";
	// http://reseplanerare.resrobot.se/bin/
	// http://api.vasttrafik.se/bin/
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.BUS,
			Product.REGIONAL_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.FERRY, Product.FERRY, Product.REGIONAL_TRAIN, null, null,
			null };

	public SeProvider()
	{
		super(NetworkId.SE, API_BASE, "sn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setUseIso8601(true);
		setStationBoardHasStationTable(false);
		setStationBoardCanDoEquivs(false);
	}

	private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{3,}?) kn\\)");

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
			return new String[] { mParen.group(2), mParen.group(1) };

		return super.splitStationName(name);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(address);
	}

	private static final Pattern P_NORMALIZE_LINE_BUS = Pattern.compile("Buss\\s*(.*)");
	private static final Pattern P_NORMALIZE_LINE_SUBWAY = Pattern.compile("Tunnelbana\\s*(.*)");

	@Override
	protected Line parseLineAndType(final String line)
	{
		final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(line);
		if (mBus.matches())
			return newLine(Product.BUS, mBus.group(1), null);

		final Matcher mSubway = P_NORMALIZE_LINE_SUBWAY.matcher(line);
		if (mSubway.matches())
			return newLine(Product.SUBWAY, "T" + mSubway.group(1), null);

		return newLine(null, line, null);
	}
}
