/*
 * Copyright 2010 the original author or authors.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class TlseProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "www.travelinesoutheast.org.uk";
	private final static String API_BASE = "http://www.travelinesoutheast.org.uk/se/";

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "XSLT_TRIP_REQUEST2?outputFormat=XML&coordOutputFormat=WGS84&locationServerActive=1&type_origin=any&name_origin=%s";

	@Override
	protected String autocompleteUri(final CharSequence constraint)
	{
		return String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), "ISO-8859-1"));
	}

	@Override
	protected String nearbyLatLonUri(final int lat, final int lon)
	{
		return null;
	}

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&coordOutputFormat=WGS84&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, ParserUtils.urlEncode(stationId));
	}

	@Override
	protected String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		uri.append("&useRealtime=1");
		return uri.toString();
	}

	@Override
	protected String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmm");

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");

		uri.append("?language=de");
		appendCommonConnectionParams(uri);

		appendLocation(uri, from, "origin");
		appendLocation(uri, to, "destination");
		if (via != null)
			appendLocation(uri, via, "via");

		uri.append("&itdDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&itdTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&itdTripDateTimeDepArr=").append(dep ? "dep" : "arr");

		uri.append("&ptOptionsActive=1");
		uri.append("&changeSpeed=").append(WALKSPEED_MAP.get(walkSpeed));
		uri.append(productParams(products));

		uri.append("&locationServerActive=1");
		uri.append("&useRealtime=1");

		return uri.toString();
	}

	@Override
	protected String commandLink(final String sessionId, final String command)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");
		uri.append("?sessionID=").append(sessionId);
		appendCommonConnectionParams(uri);
		uri.append("&command=").append(command);
		return uri.toString();
	}
}
