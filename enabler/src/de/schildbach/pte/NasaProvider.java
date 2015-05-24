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
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class NasaProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://reiseauskunft.insa.de/bin/";

	public NasaProvider()
	{
		super(NetworkId.NASA, API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 8, Charsets.UTF_8);

		setStationBoardHasLocation(true);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.REGIONAL_TRAIN;
		if (value == 8)
			return Product.REGIONAL_TRAIN;
		if (value == 16)
			return Product.SUBURBAN_TRAIN;
		if (value == 32)
			return Product.TRAM;
		if (value == 64)
			return Product.BUS;
		if (value == 128) // Rufbus
			return Product.ON_DEMAND;

		throw new IllegalArgumentException("cannot handle: " + value);
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
			productBits.setCharAt(3, '1'); // RE/RB
			productBits.setCharAt(7, '1'); // Tourismus-Züge
			productBits.setCharAt(2, '1'); // undokumentiert
		}
		else if (product == Product.SUBURBAN_TRAIN || product == Product.SUBWAY)
		{
			productBits.setCharAt(4, '1'); // S/U
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(5, '1'); // Straßenbahn
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(6, '1'); // Bus
		}
		else if (product == Product.FERRY || product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

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
			uri.append("?near=Anzeigen");
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
		reader.replace("\"Florian Geyer\"", "Florian Geyer");
	}

	@Override
	protected Product normalizeType(String type)
	{
		final String ucType = type.toUpperCase();

		if ("ECW".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;
		if ("IXB".equals(ucType)) // ICE International
			return Product.HIGH_SPEED_TRAIN;
		if ("RRT".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;

		if ("DPF".equals(ucType)) // mit Dampflok bespannter Zug
			return Product.REGIONAL_TRAIN;
		if ("DAM".equals(ucType)) // Harzer Schmalspurbahnen: mit Dampflok bespannter Zug
			return Product.REGIONAL_TRAIN;
		if ("TW".equals(ucType)) // Harzer Schmalspurbahnen: Triebwagen
			return Product.REGIONAL_TRAIN;
		if ("RR".equals(ucType)) // Polen
			return Product.REGIONAL_TRAIN;
		if ("BAHN".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("ZUGBAHN".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("DAMPFZUG".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("RUFBUS".equals(ucType)) // Rufbus
			return Product.BUS;
		if ("RBS".equals(ucType)) // Rufbus
			return Product.BUS;

		return super.normalizeType(type);
	}
}
