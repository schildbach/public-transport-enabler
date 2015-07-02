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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class ShProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://nah.sh.hafas.de/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public ShProvider()
	{
		super(NetworkId.SH, API_BASE, "dn", PRODUCTS_MAP);

		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
	}

	private static final String[] PLACES = { "Hamburg", "Kiel", "Lübeck", "Flensburg", "Neumünster" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
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

	@Override
	protected NearbyLocationsResult nearbyStationsById(final String id, final int maxDistance) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append("?near=Anzeigen");
		uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
		uri.append("&input=").append(normalizeStationId(id));
		return htmlNearbyStations(uri.toString());
	}

	protected static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Busse Kiel
		putKielBusStyle("1", new Style(Style.parseColor("#7288af"), Style.WHITE));
		putKielBusStyle("2", new Style(Style.parseColor("#50bbb4"), Style.WHITE));
		putKielBusStyle("5", new Style(Style.parseColor("#f39222"), Style.WHITE));
		putKielBusStyle("6", new Style(Style.parseColor("#aec436"), Style.WHITE));
		putKielBusStyle("8", new Style(Style.parseColor("#bcb261"), Style.WHITE));
		putKielBusStyle("9", new Style(Style.parseColor("#c99c7d"), Style.WHITE));
		putKielBusStyle("11", new Style(Style.parseColor("#f9b000"), Style.WHITE));
		putKielBusStyle("22", new Style(Style.parseColor("#8ea48a"), Style.WHITE));
		putKielBusStyle("31", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		putKielBusStyle("32", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		putKielBusStyle("33", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		putKielBusStyle("34", new Style(Style.parseColor("#009ee3"), Style.WHITE));
		putKielBusStyle("41", new Style(Style.parseColor("#8ba5d6"), Style.WHITE));
		putKielBusStyle("42", new Style(Style.parseColor("#8ba5d6"), Style.WHITE));
		putKielBusStyle("50", new Style(Style.parseColor("#00a138"), Style.WHITE));
		putKielBusStyle("51", new Style(Style.parseColor("#00a138"), Style.WHITE));
		putKielBusStyle("52", new Style(Style.parseColor("#00a138"), Style.WHITE));
		putKielBusStyle("60S", new Style(Style.parseColor("#92b4af"), Style.WHITE));
		putKielBusStyle("60", new Style(Style.parseColor("#92b4af"), Style.WHITE));
		putKielBusStyle("61", new Style(Style.parseColor("#9d1380"), Style.WHITE));
		putKielBusStyle("62", new Style(Style.parseColor("#9d1380"), Style.WHITE));
		putKielBusStyle("71", new Style(Style.parseColor("#777e6f"), Style.WHITE));
		putKielBusStyle("72", new Style(Style.parseColor("#777e6f"), Style.WHITE));
		putKielBusStyle("81", new Style(Style.parseColor("#00836e"), Style.WHITE));
		putKielBusStyle("91", new Style(Style.parseColor("#947e62"), Style.WHITE));
		putKielBusStyle("92", new Style(Style.parseColor("#947e62"), Style.WHITE));
		putKielBusStyle("100", new Style(Style.parseColor("#d40a11"), Style.WHITE));
		putKielBusStyle("101", new Style(Style.parseColor("#d40a11"), Style.WHITE));
		putKielBusStyle("300", new Style(Style.parseColor("#cf94c2"), Style.WHITE));
		putKielBusStyle("501", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
		putKielBusStyle("502", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
		putKielBusStyle("503", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
		putKielBusStyle("503S", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
		putKielBusStyle("512", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
		putKielBusStyle("512S", new Style(Style.parseColor("#0f3f93"), Style.WHITE));
	}

	private static void putKielBusStyle(final String name, final Style style)
	{
		STYLES.put("AK|B" + name, style);
		STYLES.put("KIEL|B" + name, style);
	}
}
