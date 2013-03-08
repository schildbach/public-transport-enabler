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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsContext;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class NsProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NS;
	private static final String API_BASE = "http://hafas.bene-system.com/bin/";

	public NsProvider()
	{
		super(API_BASE + "query.exe/nn", 10, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 0)
			return '?';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // HST
			productBits.setCharAt(1, '1'); // IC/EC
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // IR/D
			productBits.setCharAt(3, '1');
		}
		else if (product == 'S')
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == 'U')
		{
			// productBits.setCharAt(8, '1');
		}
		else if (product == 'T')
		{
			// productBits.setCharAt(7, '1');
		}
		else if (product == 'B' || product == 'P')
		{
			// productBits.setCharAt(5, '1');
		}
		else if (product == 'F')
		{
			productBits.setCharAt(6, '1'); // boat
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(API_BASE);
			uri.append("stboard.exe/nn");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/nny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsB=12&S=%s?&js=true&";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(Locale.ENGLISH, AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ISO_8859_1));

		return jsonGetStops(uri);
	}

	@Override
	protected void appendCustomConnectionsQueryBinaryUri(final StringBuilder uri)
	{
		uri.append("&h2g-direct=11");
	}

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int maxNumConnections, final String products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options) throws IOException
	{
		return queryConnectionsBinary(from, via, to, date, dep, maxNumConnections, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final QueryConnectionsContext contextObj, final boolean later, final int numConnections)
			throws IOException
	{
		return queryMoreConnectionsBinary(contextObj, later, numConnections);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("SPR"))
			return 'R';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
