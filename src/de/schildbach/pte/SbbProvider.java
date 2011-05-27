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
public class SbbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SBB;
	public static final String OLD_NETWORK_ID = "fahrplan.sbb.ch";
	private static final String API_BASE = "http://fahrplan.sbb.ch/bin/";
	private static final String API_URI = "http://fahrplan.sbb.ch/bin/extxml.exe"; // xmlfahrplan.sbb.ch

	public SbbProvider(final String accessId)
	{
		super(API_URI, 10, accessId);
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

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/dny");
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
			uri.append("stboard.exe/dn");
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
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=").append(equivs ? "no" : "yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.exe/dn?getstop=1&REQ0JourneyStopsS0A=255&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	private static final Pattern P_NORMALIZE_LINE_AND_TYPE = Pattern.compile("([^#]*)#(.*)");

	@Override
	protected String normalizeLine(final String line)
	{
		final Matcher m = P_NORMALIZE_LINE_AND_TYPE.matcher(line);
		if (m.matches())
		{
			final String number = m.group(1).replaceAll("\\s+", " ");
			final String type = m.group(2);

			final char normalizedType = normalizeType(type);
			if (normalizedType != 0)
				return normalizedType + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("IN".equals(ucType)) // Italien Roma-Lecce
			return 'I';

		if ("E".equals(ucType))
			return 'R';
		if ("T".equals(ucType))
			return 'R';

		if ("M".equals(ucType)) // Metro Wien
			return 'U';

		if ("TX".equals(ucType))
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
