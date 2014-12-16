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
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class NvvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NVV;
	private static final String API_BASE = "http://auskunft.nvv.de/auskunft/bin/jp/";

	public NvvProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 12, UTF_8);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // ICE
			productBits.setCharAt(1, '1'); // IC/EC
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Regionalzug
			productBits.setCharAt(10, '1'); // Zug
			productBits.setCharAt(11, '1'); // RegioTram
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
		if (value == 2048)
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg", "Kassel", "Hanau", "Göttingen",
			"Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

	@Override
	protected String[] splitStationName(final String name)
	{
		if (name.startsWith("F "))
			return new String[] { "Frankfurt", name.substring(2) };
		if (name.startsWith("OF "))
			return new String[] { "Offenbach", name.substring(3) };
		if (name.startsWith("MZ "))
			return new String[] { "Mainz", name.substring(3) };

		for (final String place : PLACES)
		{
			if (name.startsWith(place + " - "))
				return new String[] { place, name.substring(place.length() + 3) };
			else if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (mComma.matches())
			return new String[] { mComma.group(1), mComma.group(2) };

		return super.splitStationName(address);
	}

	@Override
	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			appendJsonNearbyStationsParameters(uri, location, maxDistance, maxStations);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?L=vs_rmv&near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(normalizeStationId(location.id));

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location);
		}
	}

	@Override
	protected void addCustomReplaces(final StringReplaceReader reader)
	{
		reader.replace("<ul>", " ");
		reader.replace("</ul>", " ");
		reader.replace("<li>", " ");
		reader.replace("</li>", " ");
		reader.replace("Park&Ride", "Park&amp;Ride");
		reader.replace("C&A", "C&amp;A");
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
