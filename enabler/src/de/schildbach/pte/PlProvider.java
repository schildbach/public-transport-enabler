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
import java.util.List;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.PL;
	private static final String API_BASE = "http://rozklad-pkp.pl/bin/";

	// http://rozklad.sitkol.pl/bin/
	// http://h2g.sitkol.pl/bin/

	public PlProvider()
	{
		super(API_BASE + "stboard.exe/pn", null, API_BASE + "query.exe/pn", 7, null, UTF_8, UTF_8);
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
		if (value == 16) // Bus
			return 'B';
		if (value == 32) // AST, SEV
			return 'B';
		if (value == 64)
			return 'F';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Kolej dużych prędkości
			productBits.setCharAt(1, '1'); // EC/IC/EIC/Ex
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // TLK/IR/RE/D/Posp.
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(3, '1'); // Regio/Osobowe
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(6, '1'); // Metro
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(5, '1'); // Tramwaj
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(4, '1'); // Autobus
		}
		else if (product == Product.FERRY || product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Warszawa", "Kraków" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.endsWith(", " + place))
				return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
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
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0");
			uri.append("&L=vs_java3");

			return xmlNearbyStations(uri.toString());
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
		reader.replace("dir=\"Sp ", " "); // Poland
		reader.replace("dir=\"B ", " "); // Poland
		reader.replace("dir=\"K ", " "); // Poland
		reader.replace("dir=\"Eutingen i. G ", "dir=\"Eutingen\" "); // Poland
		reader.replace("StargetLoc", "Süd\" targetLoc"); // Poland
		reader.replace("platform=\"K ", " "); // Poland
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
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

		if ("AR".equals(ucType)) // Arriva Polaczen
			return 'R';
		if ("N".equals(ucType))
			return 'R';
		if ("KW".equals(ucType)) // Koleje Wielkopolskie
			return 'R';
		if ("KS".equals(ucType)) // Koleje Śląskie
			return 'R';
		if ("E".equals(ucType))
			return 'R';
		if ("DB".equals(ucType))
			return 'R';
		if ("REG".equals(ucType))
			return 'R';

		if ("FRE".equals(ucType))
			return 'F';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
