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
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fahrplan.oebb.at/bin/";

	public OebbProvider()
	{
		super(NetworkId.OEBB, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dny", API_BASE + "query.exe/dn", 13);

		setDominantPlanStopTime(true);
		setJsonGetStopsEncoding(Charsets.UTF_8);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 8)
			return Product.REGIONAL_TRAIN;
		if (value == 16)
			return Product.REGIONAL_TRAIN;
		if (value == 32)
			return Product.SUBURBAN_TRAIN;
		if (value == 64)
			return Product.BUS;
		if (value == 128)
			return Product.FERRY;
		if (value == 256)
			return Product.SUBWAY;
		if (value == 512)
			return Product.TRAM;
		if (value == 1024) // Autoreisezug
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2048)
			return Product.ON_DEMAND;
		if (value == 4096)
			return Product.HIGH_SPEED_TRAIN;

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
	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxLocations) throws IOException
	{
		if (location.hasLocation())
		{
			return nearbyLocationsByCoordinate(types, location.lat, location.lon, maxDistance, maxLocations);
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
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final String[] PLACES = { "Wien", "Graz", "Linz/Donau", "Salzburg", "Innsbruck" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " "))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitPOI(final String poi)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(poi);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(address);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("RR")) // Finnland, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("EE")) // Rumänien, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("OZ")) // Schweden, Oeresundzug, Connections only?
			return Product.HIGH_SPEED_TRAIN;
		if (ucType.equals("UUU")) // Italien, Nacht, Connections only?
			return Product.HIGH_SPEED_TRAIN;

		if (ucType.equals("S2")) // Helsinki-Turku, Connections only?
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("RE")) // RegionalExpress Deutschland
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("DPN")) // Connections only? TODO nicht evtl. doch eher ne S-Bahn?
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("IP")) // Ozd, Ungarn
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("N")) // Frankreich, Tours
			return Product.REGIONAL_TRAIN;
		if (ucType.equals("DPF")) // VX=Vogtland Express, Connections only?
			return Product.REGIONAL_TRAIN;
		if ("UAU".equals(ucType)) // Rußland
			return Product.REGIONAL_TRAIN;

		if (ucType.equals("RSB")) // Schnellbahn Wien
			return Product.SUBURBAN_TRAIN;

		if (ucType.equals("LKB")) // Connections only?
			return Product.TRAM;

		if (ucType.equals("OBU")) // Connections only?
			return Product.BUS;
		if (ucType.equals("O-BUS")) // Stadtbus
			return Product.BUS;
		if (ucType.equals("O")) // Stadtbus
			return Product.BUS;

		if (ucType.equals("SCH")) // Connections only?
			return Product.FERRY;
		if (ucType.equals("F")) // Fähre
			return Product.FERRY;

		if (ucType.equals("LIF"))
			return Product.CABLECAR;
		if (ucType.equals("LIFT")) // Graz Uhrturm
			return Product.CABLECAR;
		if (ucType.equals("SSB")) // Graz Schlossbergbahn
			return Product.CABLECAR;

		if (ucType.equals("U70")) // U.K., Connections only?
			return null;
		if (ucType.equals("X70")) // U.K., Connections only?
			return null;
		if (ucType.equals("R84")) // U.K., Connections only?
			return null;
		if (ucType.equals("S84")) // U.K., Connections only?
			return null;
		if (ucType.equals("T84")) // U.K., Connections only?
			return null;

		return super.normalizeType(type);
	}
}
