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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class RmvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.RMV;
	private static final String API_BASE = "http://auskunft.nvv.de/auskunft/bin/jp/";

	public RmvProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 16, null, UTF_8, null);
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
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // ICE
			productBits.setCharAt(1, '1'); // Zug, scheinbar IC?
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Zug
			productBits.setCharAt(10, '1'); // Zug
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(3, '1'); // S-Bahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(5, '1'); // Straßenbahn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(6, '1'); // Niederflurbus
			productBits.setCharAt(7, '1'); // Bus
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(9, '1'); // AST/Rufbus
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(8, '1'); // Fähre/Schiff
		}
		else if (product == Product.CABLECAR)
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
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append('y');
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
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?L=vs_rmv&near=Anzeigen");
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
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append(xmlQueryDeparturesParameters(stationId));

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	@Override
	protected void addCustomReplaces(final StringReplaceReader reader)
	{
		reader.replace("<br />", " ");
		reader.replace("<ul>", " ");
		reader.replace("</ul>", " ");
		reader.replace("<li>", " ");
		reader.replace("</li>", " ");
		reader.replace("<b>", " ");
		reader.replace("</b>", " ");
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

		if ("MOFA".equals(ucType)) // Mobilfalt-Fahrt
			return 'P';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
