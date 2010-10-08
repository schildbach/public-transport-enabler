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

import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class VrnProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "fahrplanauskunft.vrn.de";
	private static final String API_BASE = "http://fahrplanauskunft.vrn.de/vrn_mobile/";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS || capability == Capability.LOCATION_STATION_ID
					|| capability == Capability.LOCATION_WGS84)
				return true;

		return false;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "XSLT_TRIP_REQUEST2?outputFormat=XML&locationServerActive=1&type_origin=any&name_origin=%s";

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
			+ "?outputFormat=XML&coordOutputFormat=WGS84&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct&deleteAssignedStop=0";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		return uri.toString();
	}

	@Override
	protected String connectionsQueryUri(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final String products, final WalkSpeed walkSpeed)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmm");

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");

		uri.append("?language=de");
		uri.append("&outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");

		if (fromType == LocationType.WGS84)
		{
			final String[] parts = from.split(",\\s*", 2);
			final int lat = Integer.parseInt(parts[0]);
			final int lon = Integer.parseInt(parts[1]);
			uri.append("&nameInfo_origin=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
			uri.append("&typeInfo_origin=coord");
		}
		else
		{
			uri.append("&type_origin=").append(locationTypeValue(fromType));
			uri.append("&name_origin=").append(ParserUtils.urlEncode(from, "ISO-8859-1")); // fine-grained location
		}

		if (toType == LocationType.WGS84)
		{
			final String[] parts = to.split(",\\s*", 2);
			final int lat = Integer.parseInt(parts[0]);
			final int lon = Integer.parseInt(parts[1]);
			uri.append("&nameInfo_destination=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
			uri.append("&typeInfo_destination=coord");
		}
		else
		{
			uri.append("&type_destination=").append(locationTypeValue(toType));
			uri.append("&name_destination=").append(ParserUtils.urlEncode(to, "ISO-8859-1")); // fine-grained location
		}

		if (via != null)
		{
			if (viaType == LocationType.WGS84)
			{
				final String[] parts = via.split(",\\s*", 2);
				final int lat = Integer.parseInt(parts[0]);
				final int lon = Integer.parseInt(parts[1]);
				uri.append("&nameInfo_via=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
				uri.append("&typeInfo_via=coord");
			}
			else
			{
				uri.append("&type_via=").append(locationTypeValue(viaType));
				uri.append("&name_via=").append(ParserUtils.urlEncode(via, "ISO-8859-1"));
			}
		}

		uri.append("&itdDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&itdTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&itdTripDateTimeDepArr=").append(dep ? "dep" : "arr");

		// TODO products

		uri.append("&changeSpeed=").append(WALKSPEED_MAP.get(walkSpeed));

		uri.append("&locationServerActive=1");

		return uri.toString();
	}

	@Override
	protected String commandLink(final String sessionId, final String command)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");
		uri.append("?sessionID=").append(sessionId);
		uri.append("&command=").append(command);
		return uri.toString();
	}
}
