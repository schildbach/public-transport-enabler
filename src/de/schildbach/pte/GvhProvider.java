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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.GVH;
	public static final String OLD_NETWORK_ID = "mobil.gvh.de";
	private static final String API_BASE = "http://mobil.efa.de/mobile3/";

	public GvhProvider(final String additionalQueryParameter)
	{
		super(additionalQueryParameter);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
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

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&type_dm=stop&name_dm=%s&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, ParserUtils.urlEncode(stationId, "ISO-8859-1"));
	}

	@Override
	protected String nearbyLatLonUri(final int lat, final int lon)
	{
		return null;
	}

	@Override
	protected String departuresQueryUri(String stationId, int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?type_dm=stop");
		uri.append("&name_dm=").append(ParserUtils.urlEncode(stationId));
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
	protected String commandLink(final String sessionId, final String requestId, final String command)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE);
		uri.append("XSLT_TRIP_REQUEST2");
		uri.append("?sessionID=").append(sessionId);
		uri.append("&requestID=").append(requestId);
		appendCommonConnectionParams(uri);
		uri.append("&command=").append(command);
		return uri.toString();
	}

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		// Hamburg
		LINES.put("SS1", new int[] { Color.parseColor("#00933B"), Color.WHITE });
		LINES.put("SS11", new int[] { Color.WHITE, Color.parseColor("#00933B"), Color.parseColor("#00933B") });
		LINES.put("SS2", new int[] { Color.WHITE, Color.parseColor("#9D271A"), Color.parseColor("#9D271A") });
		LINES.put("SS21", new int[] { Color.parseColor("#9D271A"), Color.WHITE });
		LINES.put("SS3", new int[] { Color.parseColor("#411273"), Color.WHITE });
		LINES.put("SS31", new int[] { Color.parseColor("#411273"), Color.WHITE });

		LINES.put("UU1", new int[] { Color.parseColor("#044895"), Color.WHITE });
		LINES.put("UU2", new int[] { Color.parseColor("#DC2B19"), Color.WHITE });
		LINES.put("UU3", new int[] { Color.parseColor("#EE9D16"), Color.WHITE });
		LINES.put("UU4", new int[] { Color.parseColor("#13A59D"), Color.WHITE });
	}

	@Override
	public int[] lineColors(final String line)
	{
		final int[] lineColors = LINES.get(line);
		if (lineColors != null)
			return lineColors;
		else
			return super.lineColors(line);
	}
}
