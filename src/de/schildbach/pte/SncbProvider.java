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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SncbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SNCB;
	public static final String OLD_NETWORK_ID = "hari.b-rail.be";
	private static final String API_BASE = "http://hari.b-rail.be/Hafas/bin/";
	private static final String API_URI = "http://hari.b-rail.be/Hafas/bin/extxml.exe";

	public SncbProvider()
	{
		super(API_URI, 16, null);
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
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(2, '1'); // IC/IR/P/ICT
		}
		else if (product == 'R' || product == 'S')
		{
			productBits.setCharAt(6, '1'); // Zug
		}
		else if (product == 'U')
		{
			productBits.setCharAt(8, '1'); // Metro
		}
		else if (product == 'T')
		{
			productBits.setCharAt(10, '1'); // Stadtbahn
		}
		else if (product == 'B' || product == 'P')
		{
			productBits.setCharAt(9, '1'); // Bus
		}
		else if (product == 'F' || product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Antwerpen", "Gent", "Charleroi", "Liege", "Liège", "Brussel" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/nn?near=Zoek");
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
		uri.append(API_BASE).append("stboard.exe/nn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/nny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsB=12&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected String normalizeLine(final String line)
	{
		return parseLineAndType(line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.startsWith("IC "))
			return 'I';
		if ("THALYS".equals(ucType)) // Thalys
			return 'I';

		if (ucType.startsWith("IR "))
			return 'R';

		if ("L".equals(ucType))
			return 'R';
		if ("CR".equals(ucType))
			return 'R';
		if ("ICT".equals(ucType)) // Brügge
			return 'R';
		if ("TRN".equals(ucType)) // Mons
			return 'R';

		if ("MÉT".equals(ucType))
			return 'U';
		if ("MÉTRO".equals(ucType))
			return 'U';

		if ("TRAMWAY".equals(ucType))
			return 'T';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
