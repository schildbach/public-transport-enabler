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
import java.util.Collection;
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.OEBB;
	private static final String API_BASE = "http://fahrplan.oebb.at/bin/";

	public OebbProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dny", API_BASE + "query.exe/dn", 13);

		setDominantPlanStopTime(true);
		setJsonGetStopsEncoding(UTF_8);
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
			return 'I';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'R';
		if (value == 32)
			return 'S';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'F';
		if (value == 256)
			return 'U';
		if (value == 512)
			return 'T';
		if (value == 1024) // Autoreisezug
			return 'I';
		if (value == 2048)
			return 'P';
		if (value == 4096)
			return 'I';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // railjet/ICE
			productBits.setCharAt(1, '1'); // ÖBB EC/ÖBB IC
			productBits.setCharAt(2, '1'); // EC/IC
			productBits.setCharAt(10, '1'); // Autoreisezug
			productBits.setCharAt(12, '1'); // westbahn
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(3, '1'); // D/EN
			productBits.setCharAt(4, '1'); // REX/R
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(5, '1'); // S-Bahnen
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(8, '1'); // U-Bahn
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(9, '1'); // Straßenbahn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(6, '1'); // Busse
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(11, '1'); // Anrufpflichtige Verkehre
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(7, '1'); // Schiffe
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
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			appendJsonNearbyStationsParameters(uri, location, maxDistance, maxStations);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Suchen");
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
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final String[] PLACES = { "Wien", "Graz", "Linz/Donau", "Salzburg", "Innsbruck" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " "))
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
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("RR")) // Finnland, Connections only?
			return 'I';
		if (ucType.equals("EE")) // Rumänien, Connections only?
			return 'I';
		if (ucType.equals("OZ")) // Schweden, Oeresundzug, Connections only?
			return 'I';
		if (ucType.equals("UUU")) // Italien, Nacht, Connections only?
			return 'I';

		if (ucType.equals("S2")) // Helsinki-Turku, Connections only?
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress Deutschland
			return 'R';
		if (ucType.equals("DPN")) // Connections only? TODO nicht evtl. doch eher ne S-Bahn?
			return 'R';
		if (ucType.equals("E")) // Budapest, Ungarn
			return 'R';
		if (ucType.equals("IP")) // Ozd, Ungarn
			return 'R';
		if (ucType.equals("N")) // Frankreich, Tours
			return 'R';
		if (ucType.equals("DPF")) // VX=Vogtland Express, Connections only?
			return 'R';
		if ("UAU".equals(ucType)) // Rußland
			return 'R';

		if (ucType.equals("RSB")) // Schnellbahn Wien
			return 'S';

		if (ucType.equals("LKB")) // Connections only?
			return 'T';

		if (ucType.equals("OBU")) // Connections only?
			return 'B';
		if (ucType.equals("O-BUS")) // Stadtbus
			return 'B';
		if (ucType.equals("O")) // Stadtbus
			return 'B';

		if (ucType.equals("SCH")) // Connections only?
			return 'F';
		if (ucType.equals("F")) // Fähre
			return 'F';

		if (ucType.equals("LIF"))
			return 'C';
		if (ucType.equals("LIFT")) // Graz Uhrturm
			return 'C';
		if (ucType.equals("SSB")) // Graz Schlossbergbahn
			return 'C';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if (ucType.equals("U70")) // U.K., Connections only?
			return '?';
		if (ucType.equals("X70")) // U.K., Connections only?
			return '?';
		if (ucType.equals("R84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("S84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("T84")) // U.K., Connections only?
			return '?';

		return 0;
	}
}
