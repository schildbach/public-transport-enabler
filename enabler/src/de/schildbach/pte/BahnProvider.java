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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public final class BahnProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DB;
	private static final String API_BASE = "http://reiseauskunft.bahn.de/bin/";

	public BahnProvider()
	{
		super(API_BASE + "query.exe/dn", 14, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS || capability == Capability.DEPARTURES || capability == Capability.AUTOCOMPLETE_ONE_LINE
					|| capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 2)
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'S';
		if (value == 32)
			return 'B';
		if (value == 64)
			return 'F';
		if (value == 128)
			return 'U';
		if (value == 256)
			return 'T';
		if (value == 512)
			return 'P';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1');
			productBits.setCharAt(1, '1');
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1');
			productBits.setCharAt(3, '1');
		}
		else if (product == 'S')
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == 'U')
		{
			productBits.setCharAt(7, '1');
		}
		else if (product == 'T')
		{
			productBits.setCharAt(8, '1');
		}
		else if (product == 'B')
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == 'P')
		{
			productBits.setCharAt(9, '1');
		}
		else if (product == 'F')
		{
			productBits.setCharAt(6, '1');
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private final static Pattern P_NEARBY_STATIONS_BY_STATION = Pattern
			.compile("<a href=\"http://mobile\\.bahn\\.de/bin/mobil/bhftafel.exe/dn[^\"]*?evaId=(\\d*)&[^\"]*?\">([^<]*)</a>");

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/dny");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_nv=get_stopweight|yes");
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("bhftafel.exe/dn");
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			final CharSequence page = ParserUtils.scrape(uri.toString());

			final Matcher m = P_NEARBY_STATIONS_BY_STATION.matcher(page);

			final List<Location> stations = new ArrayList<Location>();
			while (m.find())
			{
				final int sId = Integer.parseInt(m.group(1));
				final String sName = ParserUtils.resolveEntities(m.group(2).trim());

				final Location station = new Location(LocationType.STATION, sId, null, sName);
				stations.add(station);
			}

			if (maxStations == 0 || maxStations >= stations.size())
				return new NearbyStationsResult(null, stations);
			else
				return new NearbyStationsResult(null, stations.subList(0, maxStations));
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.exe/dn?getstop=1&REQ0JourneyStopsS0A=255&S=%s?&js=true&";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ISO_8859_1));

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

	private static final Pattern P_NORMALIZE_LINE_NAME_TRAM = Pattern.compile("str\\s+(.*)", Pattern.CASE_INSENSITIVE);

	@Override
	protected String normalizeLineName(final String lineName)
	{
		final Matcher mTram = P_NORMALIZE_LINE_NAME_TRAM.matcher(lineName);
		if (mTram.matches())
			return mTram.group(1);

		return super.normalizeLineName(lineName);
	}

	@Override
	protected char normalizeType(String type)
	{
		final String ucType = type.toUpperCase();

		if ("DZ".equals(ucType)) // Dampfzug
			return 'R';

		if ("LTT".equals(ucType))
			return 'B';

		if (ucType.startsWith("RFB")) // Rufbus
			return 'P';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if ("E".equals(ucType))
			return '?';

		return 0;
	}
}
