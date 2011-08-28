/*
 * Copyright 2010, 2011 the original author or authors.
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.PL;
	private static final String API_BASE = "http://rozklad.sitkol.pl/bin/";

	public PlProvider()
	{
		super(API_BASE + "query.exe/pn", 7, null, null, "UTF-8");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
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
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(1, '1'); // EC/IC/EIC/Ex
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // TLK/IR/D
			productBits.setCharAt(3, '1'); // Regionalverkehrszug
		}
		else if (product == 'S' || product == 'T')
		{
			productBits.setCharAt(5, '1'); // Stadtbahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(6, '1'); // U-Bahn
		}
		else if (product == 'B' || product == 'P')
		{
			productBits.setCharAt(4, '1'); // Bus
		}
		else if (product == 'F' || product == 'C')
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
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(API_BASE);
			uri.append("stboard.exe/pn");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0");
			uri.append("&L=vs_java3");

			// &inputTripelId=A%3d1%40O%3dCopenhagen%20Airport%40X%3d12646941%40Y%3d55629753%40U%3d86%40L%3d900000011%40B%3d1

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/pn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	private static final Pattern P_NORMALIZE_LINE_RUSSIA = Pattern.compile("(?:D\\s*)?(\\d{1,3}(?:[A-Z]{2}|Y))");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");

	@Override
	protected String normalizeLine(String line)
	{
		// replace badly encoded character (stations 8530643 and 8530644)
		if (line.equals("F\u0084hre"))
			line = "Fähre";

		final Matcher mRussia = P_NORMALIZE_LINE_RUSSIA.matcher(line);
		if (mRussia.matches())
			return 'R' + mRussia.group(1);

		if (P_NORMALIZE_LINE_NUMBER.matcher(line).matches())
			return 'R' + line;

		return super.normalizeLine(line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("REG".equals(ucType))
			return 'R';
		if ("AR".equals(ucType)) // Arriva Polaczen
			return 'R';
		if ("RNV".equals(ucType)) // Rhein-Neckar-Verkehr GmbH
			return 'R';
		if ("N".equals(ucType)) // St. Pierre des Corps - Tours
			return 'R';

		if ("METRO".equals(ucType))
			return 'U';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if ("E".equals(ucType))
			return '?';

		return 0;
	}
}
