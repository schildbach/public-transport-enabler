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
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SeProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SE;
	private static final String API_BASE = "http://reseplanerare.resrobot.se/bin/";

	public SeProvider()
	{
		super(API_BASE + "query.exe/sn", 14, null, "UTF-8", null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1) // Flyg
			return 'I';
		if (value == 2) // X2000
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8) // Expressbus
			return 'B';
		if (value == 16)
			return 'R';
		if (value == 32) // Tunnelbana
			return 'U';
		if (value == 64) // Spårvagn
			return 'T';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'F';
		if (value == 512) // Länstaxi
			return 'F';
		if (value == 1024) // Future
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // Flyg
			productBits.setCharAt(1, '1'); // Snabbtåg
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // Tåg
			productBits.setCharAt(4, '1'); // Lokaltåg
		}
		else if (product == 'S')
		{
		}
		else if (product == 'U')
		{
			productBits.setCharAt(5, '1'); // Tunnelbana
		}
		else if (product == 'T')
		{
			productBits.setCharAt(6, '1'); // Spårvagn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(3, '1'); // Expressbuss
			productBits.setCharAt(7, '1'); // Buss
		}
		else if (product == 'P')
		{
		}
		else if (product == 'F')
		{
			productBits.setCharAt(8, '1'); // Båt
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_KN = Pattern.compile("(.*?) \\((.*?) kn\\)");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_KN.matcher(name);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/sny");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 150);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/sn");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0&L=vs_java3");

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
		uri.append(API_BASE).append("stboard.exe/sn");
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
			+ "ajax-getstop.exe/sny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=7&getstop=1&noSession=yes&REQ0JourneyStopsB=12&REQ0JourneyStopsS0G=&S=%s";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
