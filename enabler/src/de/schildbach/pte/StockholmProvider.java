/*
 * Copyright 2010-2013 the original author or authors.
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
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
		super(API_BASE + "stboard.exe/sn", API_BASE + "ajax-getstop.exe/sny", API_BASE + "query.exe/sn", 7, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
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

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append('y');
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append(xmlNearbyStationsParameters(location.id));

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append(xmlQueryDeparturesParameters(stationId));

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final StringBuilder uri = new StringBuilder(getStopEndpoint);
		uri.append(jsonGetStopsParameters(constraint));

		return jsonGetStops(uri.toString());
	}

	@Override
	protected void appendCustomTripsQueryBinaryUri(final StringBuilder uri)
	{
		uri.append("&h2g-direct=11"
				+ "&REQ0HafasSearchIndividual=1&REQ0HafasSearchPublic=1"
				+ "&existIntermodalDep_enable=yes&existIntermodalDest_enable=yes&existTotal_enable=yes"
				+ "&REQ0JourneyDep_Foot_enable=1&REQ0JourneyDep_Foot_maxDist=5000&REQ0JourneyDep_Foot_minDist=0&REQ0JourneyDep_Foot_speed=100&REQ0JourneyDep_Bike_enable=0&REQ0JourneyDep_ParkRide_enable=0"
				+ "&REQ0JourneyDest_Foot_enable=1&REQ0JourneyDest_Foot_maxDist=5000&REQ0JourneyDest_Foot_minDist=0&REQ0JourneyDest_Foot_speed=100&REQ0JourneyDest_Bike_enable=0&REQ0JourneyDest_ParkRide_enable=0");
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int numTrips, final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options) throws IOException
	{
		return queryTripsBinary(from, via, to, date, dep, numTrips, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later, final int numTrips) throws IOException
	{
		return queryMoreTripsBinary(contextObj, later, numTrips);
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

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		LINES.put("UMETRO10", new Style(Shape.ROUNDED, Style.parseColor("#25368b"), Style.WHITE));
		LINES.put("UMETRO11", new Style(Shape.ROUNDED, Style.parseColor("#25368b"), Style.WHITE));

		LINES.put("UMETRO13", new Style(Shape.ROUNDED, Style.parseColor("#f1491c"), Style.WHITE));
		LINES.put("UMETRO14", new Style(Shape.ROUNDED, Style.parseColor("#f1491c"), Style.WHITE));

		LINES.put("UMETRO17", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
		LINES.put("UMETRO18", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
		LINES.put("UMETRO19", new Style(Shape.ROUNDED, Style.parseColor("#6ec72d"), Style.WHITE));
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;

		return super.lineStyle(line);
	}
}
