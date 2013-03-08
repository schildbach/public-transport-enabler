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
public class RmvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.RMV;
	private static final String API_BASE = "http://auskunft.nvv.de/auskunft/bin/jp/";

	public RmvProvider()
	{
		super(API_BASE + "query.exe/dn", 16, null, UTF_8, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // ICE
			productBits.setCharAt(1, '1'); // Zug, scheinbar IC?
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // Zug
			productBits.setCharAt(10, '1'); // Zug
		}
		else if (product == 'S')
		{
			productBits.setCharAt(3, '1'); // S-Bahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
			productBits.setCharAt(5, '1'); // Straßenbahn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(6, '1'); // Niederflurbus
			productBits.setCharAt(7, '1'); // Bus
		}
		else if (product == 'P')
		{
			productBits.setCharAt(9, '1'); // AST/Rufbus
		}
		else if (product == 'F')
		{
			productBits.setCharAt(8, '1'); // Fähre/Schiff
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
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
			return 'S';
		if (value == 16)
			return 'U';
		if (value == 32)
			return 'T';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'F';
		if (value == 512)
			return 'P';
		if (value == 1024)
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg", "Kassel", "Hanau", "Göttingen",
			"Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " - "))
				return new String[] { place, name.substring(place.length() + 3) };
			else if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitPlaceAndName(name);
	}

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
			uri.append("stboard.exe/dn?L=vs_rmv&near=Anzeigen");
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
		final String uri = String.format(Locale.ENGLISH, AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), UTF_8));

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

		if ("U-BAHN".equals(ucType))
			return 'U';

		if ("B".equals(ucType))
			return 'B';
		if ("BUFB".equals(ucType)) // BuFB
			return 'B';
		if ("BUVB".equals(ucType)) // BuVB
			return 'B';
		if ("LTAXI".equals(ucType))
			return 'B';
		if ("BN".equals(ucType)) // BN Venus
			return 'B';
		if ("ASOF".equals(ucType))
			return 'B';
		if ("AT".equals(ucType)) // Anschluß Sammel Taxi, Anmeldung nicht erforderlich
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
