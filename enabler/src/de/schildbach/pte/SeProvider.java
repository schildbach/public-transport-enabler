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
import java.util.List;
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

/**
 * @author Andreas Schildbach
 */
public class SeProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SE;
	private static final String API_BASE = "http://samtrafiken.hafas.de/bin/";

	// http://reseplanerare.resrobot.se/bin/
	// http://api.vasttrafik.se/bin/

	public SeProvider()
	{
		super(API_BASE + "stboard.exe/sn", API_BASE + "ajax-getstop.exe/sny", API_BASE + "query.exe/sn", 14, null, UTF_8, null);

		setClientType("ANDROID");
		setCanDoEquivs(false);
		setUseIso8601(true);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1) // Flyg
			return 'I';
		if (value == 2) // X2000
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8) // Expressbus
			return 'B';
		if (value == 16)
			return 'R';
		if (value == 32) // Tunnelbana
			return 'U';
		if (value == 64) // Spårvagn
			return 'T';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'F';
		if (value == 512) // Länstaxi
			return 'F';
		if (value == 1024) // Future
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Flyg
			productBits.setCharAt(1, '1'); // Snabbtåg
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Tåg
			productBits.setCharAt(4, '1'); // Lokaltåg
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(5, '1'); // Tunnelbana
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(6, '1'); // Spårvagn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(3, '1'); // Expressbuss
			productBits.setCharAt(7, '1'); // Buss
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(8, '1'); // Båt
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_KN = Pattern.compile("(.*?) \\((.*?) kn\\)");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_KN.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append('y');
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 150);
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
		uri.append("&h2g-direct=11");
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
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final Pattern P_NORMALIZE_LINE_BUS = Pattern.compile("Buss\\s*(.*)");
	private static final Pattern P_NORMALIZE_LINE_SUBWAY = Pattern.compile("Tunnelbana\\s*(.*)");

	@Override
	protected Line parseLineAndType(final String line)
	{
		final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(line);
		if (mBus.matches())
			return newLine('B', mBus.group(1), null);

		final Matcher mSubway = P_NORMALIZE_LINE_SUBWAY.matcher(line);
		if (mSubway.matches())
			return newLine('U', "T" + mSubway.group(1), null);

		return newLine('?', line, null);
	}
}
