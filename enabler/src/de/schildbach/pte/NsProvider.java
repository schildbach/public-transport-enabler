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
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class NsProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NS;
	private static final String API_BASE = "http://hafas.bene-system.com/bin/";

	private static final Pattern HTML_NEARBY_STATIONS_PATTERN = Pattern.compile("<tr bgcolor=\"#(E7EEF9|99BAE4)\">(.*?)</tr>", Pattern.DOTALL);

	public NsProvider()
	{
		super(API_BASE + "stboard.exe/nn", API_BASE + "ajax-getstop.exe/nny", API_BASE + "query.exe/nn", 10);

		setHtmlNearbyStationsPattern(HTML_NEARBY_STATIONS_PATTERN);
		setStationBoardHasLocation(true);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
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
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // HST
			productBits.setCharAt(1, '1'); // IC/EC
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // IR/D
			productBits.setCharAt(3, '1');
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(7, '1');
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(8, '1');
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(9, '1');
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(6, '1'); // boat
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
	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(normalizeStationId(location.id));
			uri.append("&L=profi");

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location);
		}
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("SPR"))
			return 'R';
		if (ucType.equals("E")) // Budapest, Ungarn
			return 'R';
		if (ucType.equals("N")) // Avignon
			return 'R';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
