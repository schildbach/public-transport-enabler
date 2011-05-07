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
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class ZvvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.ZVV;
	private static final String API_BASE = "http://onlinev2.fahrplan.zvv.ch/bin/"; // http://online.fahrplan.zvv.ch/bin/

	public ZvvProvider()
	{
		super(API_BASE + "query.exe/dn", null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	@Override
	protected String nearbyStationUri(String stationId)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("stboard.exe/dn");
		uri.append("?productsFilter=1111111111");
		uri.append("&boardType=dep");
		uri.append("&input=").append(ParserUtils.urlEncode(stationId));
		uri.append("&sTI=1&start=yes&hcount=0");
		uri.append("&L=vs_java3");

		// &inputTripelId=A%3d1%40O%3dCopenhagen%20Airport%40X%3d12646941%40Y%3d55629753%40U%3d86%40L%3d900000011%40B%3d1

		return xmlNearbyStations(uri.toString());
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

	private static final Pattern P_NORMALIZE_LINE_S = Pattern.compile("SN?\\d+");

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// E-Bus: Bus, Tram oder Zug?
		// TX: Ruftaxi?

		if ("D".equals(ucType))
			return 'R';
		if ("EXT".equals(ucType))
			return 'R';
		if ("ATZ".equals(ucType)) // Autotunnelzug
			return 'R';

		if ("S-BAHN".equals(ucType))
			return 'S';
		if (P_NORMALIZE_LINE_S.matcher(type).matches())
			return 'S';

		if ("T".equals(ucType))
			return 'T';
		if ("TRM".equals(ucType))
			return 'T';
		if ("TRM-NF".equals(ucType)) // Niederflur
			return 'T';

		if ("BUS-NF".equals(ucType)) // Niederflur
			return 'B';
		if ("TRO".equals(ucType))
			return 'B';

		if ("D-SCHIFF".equals(ucType))
			return 'F';
		if ("FAE".equals(ucType))
			return 'F';

		if ("BERGBAHN".equals(ucType))
			return 'C';
		if ("SEILBAHN".equals(ucType))
			return 'C';
		if ("FUN".equals(ucType)) // Standseilbahn
			return 'C';
		if ("GB".equals(ucType)) // Gondelbahn
			return 'C';
		if ("LB".equals(ucType)) // Luftseilbahn
			return 'C';
		if ("SL".equals(ucType)) // Sessel-Lift
			return 'C';

		if ("UNB".equals(ucType))
			return '?';

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		return 0;
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=1111111111");
		uri.append("&boardType=dep");
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), Integer.parseInt(stationId));
	}
}
