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

import java.util.Date;
import java.util.TimeZone;

import de.schildbach.pte.dto.Location;

/**
 * @author Andreas Schildbach
 */
public class TflProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "journeyplanner.tfl.gov.uk";
	private static final String API_BASE = "http://journeyplanner.tfl.gov.uk/user/";

	@Override
	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("Europe/London");
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	@Override
	protected String autocompleteUri(CharSequence constraint)
	{
		throw new UnsupportedOperationException();
	}

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	@Override
	protected String nearbyLatLonUri(int lat, int lon)
	{
		return null;
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
		uri.append("&useRealtime=1");
		return uri.toString();
	}

	@Override
	protected String connectionsQueryUri(Location from, Location via, Location to, Date date, boolean dep, String products, WalkSpeed walkSpeed)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected String commandLink(String sessionId, String command)
	{
		throw new UnsupportedOperationException();
	}
}
