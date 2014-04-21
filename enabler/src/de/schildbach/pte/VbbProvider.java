/*
 * Copyright 2010-2014 the original author or authors.
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
import java.util.HashSet;
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
public class VbbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VBB;
	private static final String API_BASE = "http://fahrinfo.vbb.de/bin/";
	private static final Set<Product> ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND;

	static
	{
		ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND = new HashSet<Product>(Product.ALL);
		ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND.remove(Product.HIGH_SPEED_TRAIN);
		ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND.remove(Product.ON_DEMAND);
	}

	public VbbProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 7, UTF_8, UTF_8);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'S';
		if (value == 2)
			return 'U';
		if (value == 4)
			return 'T';
		if (value == 8)
			return 'B';
		if (value == 16)
			return 'F';
		if (value == 32)
			return 'I';
		if (value == 64)
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(6, '1');
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(0, '1');
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(1, '1');
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(2, '1');
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(3, '1');
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*?) \\((.{4,}?)\\)(?: \\((U|S|S\\+U)\\))?");
	private static final Pattern P_SPLIT_NAME_COMMA = Pattern.compile("([^,]*), ([^,]*)");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
		{
			final String su = mParen.group(3);
			return new String[] { mParen.group(2), mParen.group(1) + (su != null ? " (" + su + ")" : "") };
		}

		final Matcher mComma = P_SPLIT_NAME_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { mComma.group(1), mComma.group(2) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append(jsonNearbyStationsParameters(location, maxDistance, maxStations));

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
			throw new IllegalArgumentException("cannot handle: '" + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append(xmlQueryDeparturesParameters(stationId));

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND;
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
			final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility, final Set<Option> options)
			throws IOException
	{
		return queryTripsBinary(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException
	{
		return queryMoreTripsBinary(contextObj, later);
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		if ("X#".equals(lineAndType))
			return newLine('?', "X", null);
		else
			return super.parseLineAndType(lineAndType);
	}
}
