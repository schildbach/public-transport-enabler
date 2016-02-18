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
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Fare.Type;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class ShProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://nah.sh.hafas.de/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public ShProvider(final String jsonApiAuthorization)
	{
		super(NetworkId.SH, API_BASE, "dn", PRODUCTS_MAP);

		setJsonApiVersion("1.10");
		setJsonApiClient("{\"id\":\"NAHSH\"}");
		setJsonApiAuthorization(jsonApiAuthorization);
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
	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxLocations) throws IOException
	{
		if (location.hasLocation())
			return jsonLocGeoPos(types, location.lat, location.lon);
		else
			throw new IllegalArgumentException("cannot handle: " + location);
	}

	@Override
	public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time, final int maxDepartures, final boolean equivs)
			throws IOException
	{
		return jsonStationBoard(stationId, time, maxDepartures, equivs);
	}

	@Override
	public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException
	{
		return jsonLocMatch(constraint);
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException
	{
		return jsonTripSearch(from, to, date, dep, products, null);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		final JsonContext jsonContext = (JsonContext) context;
		return jsonTripSearch(jsonContext.from, jsonContext.to, jsonContext.date, jsonContext.dep, jsonContext.products,
				later ? jsonContext.laterContext : jsonContext.earlierContext);
	}

	@Override
	protected Fare parseJsonTripFare(final @Nullable String fareSetName, final @Nullable String fareSetDescription, String name,
			final Currency currency, final float price)
	{
		if (!"Normalpreis".equals(fareSetDescription) || !name.startsWith("Einzelfahrkarte "))
			return null;
		name = name.substring(16);
		if (name.startsWith("Übergang"))
			return null;
		if (name.startsWith("Kind "))
			return new Fare("SH-Tarif", Type.CHILD, currency, price, name.substring(5), null);
		else
			return new Fare("SH-Tarif", Type.ADULT, currency, price, name, null);
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
		STYLES.put("Autokraft Kiel GmbH|B" + name, style);
		STYLES.put("Kieler Verkehrsgesellschaft mbH|B" + name, style);
	}
}
