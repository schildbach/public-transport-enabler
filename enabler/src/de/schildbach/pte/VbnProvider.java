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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

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
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VbnProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "https://fahrplaner.vbn.de/hafas/";
	// http://fahrplaner.vsninfo.de/hafas/
	// http://fahrplan.rsag-online.de/hafas/
	// http://fahrplanauskunft.verkehrsverbund-warnow.de/bin/

	private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
			Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

	public VbnProvider(final String jsonApiAuthorization)
	{
		super(NetworkId.VBN, API_BASE, "dn", PRODUCTS_MAP);

		setJsonApiVersion("1.10");
		setJsonApiClient("{\"id\":\"VBN\"}");
		setJsonApiAuthorization(jsonApiAuthorization);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
	}

	private static final String[] PLACES = { "Bremen", "Bremerhaven", "Oldenburg(Oldb)", "Osnabrück", "Göttingen", "Rostock", "Warnemünde" };

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

		return super.splitAddress(address);
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
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
		// Rostock
		STYLES.put("DB Regio AG|SS1", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));
		STYLES.put("DB Regio AG|SS2", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));
		STYLES.put("DB Regio AG|SS3", new Style(Shape.CIRCLE, Style.parseColor("#009037"), Style.WHITE));

		STYLES.put("Rostocker Straßenbahn AG|T1", new Style(Shape.RECT, Style.parseColor("#712090"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|T2", new Style(Shape.RECT, Style.parseColor("#f47216"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|T3", new Style(Shape.RECT, Style.parseColor("#870e12"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|T4", new Style(Shape.RECT, Style.parseColor("#d136a3"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|T5", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|T6", new Style(Shape.RECT, Style.parseColor("#fab20b"), Style.WHITE));

		STYLES.put("Rostocker Straßenbahn AG|B15", new Style(Style.parseColor("#008dc6"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B16", new Style(Style.parseColor("#1d3c85"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B17", new Style(Style.parseColor("#5784cc"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B18", new Style(Style.parseColor("#0887c9"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B19", new Style(Style.parseColor("#166ab8"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|PRFT 19A", new Style(Style.WHITE, Style.parseColor("#166ab8")));
		STYLES.put("Rostocker Straßenbahn AG|PRFT 20A", new Style(Style.WHITE, Style.parseColor("#1959a6")));
		STYLES.put("Rostocker Straßenbahn AG|B22", new Style(Style.parseColor("#3871c1"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B23", new Style(Style.parseColor("#173e7d"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B25", new Style(Style.parseColor("#0994dc"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B26", new Style(Style.parseColor("#0994dc"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B27", new Style(Style.parseColor("#6e87cd"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B28", new Style(Style.parseColor("#4fc6f4"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|PRFT 30A", new Style(Style.WHITE, Style.parseColor("#1082ce")));
		STYLES.put("Rostocker Straßenbahn AG|B31", new Style(Style.parseColor("#3a9fdf"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B35", new Style(Style.parseColor("#1969bc"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|PRFT 35A", new Style(Style.WHITE, Style.parseColor("#1969bc")));
		STYLES.put("Rostocker Straßenbahn AG|B36", new Style(Style.parseColor("#1c63b7"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B37", new Style(Style.parseColor("#36aee8"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B38", new Style(Style.parseColor("#6e87cd"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B39", new Style(Style.parseColor("#173e7d"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|B45", new Style(Style.parseColor("#66cef5"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|PRFT 45A", new Style(Style.WHITE, Style.parseColor("#66cef5")));
		STYLES.put("Rostocker Straßenbahn AG|B49", new Style(Style.parseColor("#202267"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|BF1", new Style(Style.parseColor("#231f20"), Style.WHITE));
		STYLES.put("Rostocker Straßenbahn AG|PRFT F1A", new Style(Style.WHITE, Style.parseColor("#231f20")));
		STYLES.put("Rostocker Straßenbahn AG|BF2", new Style(Style.parseColor("#656263"), Style.WHITE));

		STYLES.put("rebus Regionalbus Rostock GmbH|B101", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B102", new Style(Style.parseColor("#2699d6"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B103", new Style(Style.parseColor("#d18f00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B104", new Style(Style.parseColor("#006f9e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B105", new Style(Style.parseColor("#c2a712"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B106", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B107", new Style(Style.parseColor("#a62341"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B108", new Style(Style.parseColor("#009fe3"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B109", new Style(Style.parseColor("#aa7fa6"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B110", new Style(Style.parseColor("#95c11f"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B111", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B112", new Style(Style.parseColor("#e50069"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B113", new Style(Style.parseColor("#935b00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B114", new Style(Style.parseColor("#935b00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B115", new Style(Style.parseColor("#74b959"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B116", new Style(Style.parseColor("#0085ac"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B118", new Style(Style.parseColor("#f9b000"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B119", new Style(Style.parseColor("#055da9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B120", new Style(Style.parseColor("#74b959"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B121", new Style(Style.parseColor("#e63323"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B122", new Style(Style.parseColor("#009870"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B123", new Style(Style.parseColor("#f39200"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B124", new Style(Style.parseColor("#9dc41a"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B125", new Style(Style.parseColor("#935b00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B127", new Style(Style.parseColor("#079897"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B128", new Style(Style.parseColor("#7263a9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B129", new Style(Style.parseColor("#e6007e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B131", new Style(Style.parseColor("#0075bf"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B132", new Style(Style.parseColor("#ef7d00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B134", new Style(Style.parseColor("#008e5c"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B135", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B136", new Style(Style.parseColor("#aa7fa6"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B137", new Style(Style.parseColor("#ef7c00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B138", new Style(Style.parseColor("#e30513"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B139", new Style(Style.parseColor("#f8ac00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B140", new Style(Style.parseColor("#c2a712"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B200", new Style(Style.parseColor("#e6007e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B201", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B203", new Style(Style.parseColor("#f59c00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B204", new Style(Style.parseColor("#b3cf3b"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B205", new Style(Style.parseColor("#dd6ca7"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B208", new Style(Style.parseColor("#9dc41a"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B210", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B211", new Style(Style.parseColor("#95c11f"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B213", new Style(Style.parseColor("#a877b2"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B215", new Style(Style.parseColor("#009fe3"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B216", new Style(Style.parseColor("#935b00"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B220", new Style(Style.parseColor("#0090d7"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B221", new Style(Style.parseColor("#009640"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B222", new Style(Style.parseColor("#f088b6"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B223", new Style(Style.parseColor("#f9b000"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B224", new Style(Style.parseColor("#004f9f"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B225", new Style(Style.parseColor("#7263a9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B230", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B231", new Style(Style.parseColor("#00853e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B232", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B233", new Style(Style.parseColor("#123274"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B235", new Style(Style.parseColor("#ba0066"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B240", new Style(Style.parseColor("#7263a9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B241", new Style(Style.parseColor("#ea5297"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B244", new Style(Style.parseColor("#f7ab59"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B245", new Style(Style.parseColor("#76b82a"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B246", new Style(Style.parseColor("#f39a8b"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B247", new Style(Style.parseColor("#009fe3"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B250", new Style(Style.parseColor("#009741"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B251", new Style(Style.parseColor("#033572"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B252", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B260", new Style(Style.parseColor("#e6007e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B270", new Style(Style.parseColor("#fbbe5e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B271", new Style(Style.parseColor("#e30613"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B272", new Style(Style.parseColor("#009fe3"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B273", new Style(Style.parseColor("#004899"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B280", new Style(Style.parseColor("#e41b18"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B281", new Style(Style.parseColor("#f9b000"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B282", new Style(Style.parseColor("#005ca9"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B283", new Style(Style.parseColor("#ec619f"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B284", new Style(Style.parseColor("#951b81"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B285", new Style(Style.parseColor("#a42522"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B286", new Style(Style.parseColor("#e6007e"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B290", new Style(Style.parseColor("#312783"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B291", new Style(Style.parseColor("#a71680"), Style.WHITE));
		STYLES.put("rebus Regionalbus Rostock GmbH|B292", new Style(Style.parseColor("#cabe46"), Style.WHITE));

		STYLES.put("Rostocker Fähren|F", new Style(Shape.CIRCLE, Style.parseColor("#17a4da"), Style.WHITE));
	}
}
