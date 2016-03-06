/*
 * Copyright 2015 the original author or authors.
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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

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
public class VaoProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://app.verkehrsauskunft.at/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, null, Product.TRAM,
			Product.REGIONAL_TRAIN, Product.BUS, Product.BUS, Product.TRAM, Product.FERRY, Product.ON_DEMAND, Product.BUS, Product.REGIONAL_TRAIN,
			null, null, null };

	public VaoProvider(final String jsonApiAuthorization)
	{
		super(NetworkId.VAO, API_BASE, "dn", PRODUCTS_MAP);

		setJsonApiVersion("1.11");
		setJsonApiClient("{\"id\":\"VAO\",\"l\":\"vs_vvv\"}");
		setJsonApiAuthorization(jsonApiAuthorization);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final Pattern P_SPLIT_NAME_ONE_COMMA = Pattern.compile("([^,]*), ([^,]{3,64})");

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitPOI(final String poi)
	{
		final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(poi);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitPOI(poi);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitAddress(address);
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

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Salzburg S-Bahn
		STYLES.put("Salzburg AG|SS1", new Style(Style.parseColor("#b61d33"), Style.WHITE));
		STYLES.put("Salzburg AG|SS11", new Style(Style.parseColor("#b61d33"), Style.WHITE));
		STYLES.put("OEBB|SS2", new Style(Style.parseColor("#0069b4"), Style.WHITE));
		STYLES.put("OEBB|SS3", new Style(Style.parseColor("#0aa537"), Style.WHITE));
		STYLES.put("BLB|SS4", new Style(Style.parseColor("#a862a4"), Style.WHITE));

		// Salzburg Bus
		STYLES.put("Salzburg AG|B1", new Style(Style.parseColor("#e3000f"), Style.WHITE));
		STYLES.put("Salzburg AG|B2", new Style(Style.parseColor("#0069b4"), Style.WHITE));
		STYLES.put("Salzburg AG|B3", new Style(Style.parseColor("#956b27"), Style.WHITE));
		STYLES.put("Salzburg AG|B4", new Style(Style.parseColor("#ffcc00"), Style.WHITE));
		STYLES.put("Salzburg AG|B5", new Style(Style.parseColor("#04bbee"), Style.WHITE));
		STYLES.put("Salzburg AG|B6", new Style(Style.parseColor("#85bc22"), Style.WHITE));
		STYLES.put("Salzburg AG|B7", new Style(Style.parseColor("#009a9b"), Style.WHITE));
		STYLES.put("Salzburg AG|B8", new Style(Style.parseColor("#f39100"), Style.WHITE));
		STYLES.put("Salzburg AG|B10", new Style(Style.parseColor("#f8baa2"), Style.BLACK));
		STYLES.put("Salzburg AG|B12", new Style(Style.parseColor("#b9dfde"), Style.WHITE));
		STYLES.put("Salzburg AG|B14", new Style(Style.parseColor("#cfe09a"), Style.WHITE));
	}
}
