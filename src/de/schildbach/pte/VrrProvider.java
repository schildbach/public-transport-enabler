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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class VrrProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VRR;
	public static final String OLD_NETWORK_ID = "efa3.vrr.de";
	private static final String API_BASE = "http://app.vrr.de/standard/";

	public VrrProvider()
	{
		super(API_BASE, null);
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
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlStopfinderRequest(constraint);
	}

	private static final String NEARBY_LATLON_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&mode=direct&coordOutputFormat=WGS84&mergeDep=1&useAllStops=1&name_dm=%2.6f:%2.6f:WGS84&type_dm=coord&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1";

	@Override
	protected String nearbyLatLonUri(final int lat, final int lon)
	{
		return String.format(Locale.ENGLISH, NEARBY_LATLON_URI, latLonToDouble(lon), latLonToDouble(lat));
	}

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST"
			+ "?outputFormat=XML&coordOutputFormat=WGS84&type_dm=stop&name_dm=%s&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct&deleteAssignedStop=0";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, ParserUtils.urlEncode(stationId, "ISO-8859-1"));
	}

	@Override
	protected String departuresQueryUri(final String stationId, final int maxDepartures)
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
		// Busse Bonn
		LINES.put("B63", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B16", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B66", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B67", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B68", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B18", new int[] { Color.parseColor("#0065ae"), Color.WHITE });
		LINES.put("B61", new int[] { Color.parseColor("#e4000b"), Color.WHITE });
		LINES.put("B62", new int[] { Color.parseColor("#e4000b"), Color.WHITE });
		LINES.put("B65", new int[] { Color.parseColor("#e4000b"), Color.WHITE });
		LINES.put("BSB55", new int[] { Color.parseColor("#00919e"), Color.WHITE });
		LINES.put("BSB60", new int[] { Color.parseColor("#8f9867"), Color.WHITE });
		LINES.put("BSB69", new int[] { Color.parseColor("#db5f1f"), Color.WHITE });
		LINES.put("B529", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B537", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B541", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B550", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B163", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B551", new int[] { Color.parseColor("#2e2383"), Color.WHITE });
		LINES.put("B600", new int[] { Color.parseColor("#817db7"), Color.WHITE });
		LINES.put("B601", new int[] { Color.parseColor("#831b82"), Color.WHITE });
		LINES.put("B602", new int[] { Color.parseColor("#dd6ba6"), Color.WHITE });
		LINES.put("B603", new int[] { Color.parseColor("#e6007d"), Color.WHITE });
		LINES.put("B604", new int[] { Color.parseColor("#009f5d"), Color.WHITE });
		LINES.put("B605", new int[] { Color.parseColor("#007b3b"), Color.WHITE });
		LINES.put("B606", new int[] { Color.parseColor("#9cbf11"), Color.WHITE });
		LINES.put("B607", new int[] { Color.parseColor("#60ad2a"), Color.WHITE });
		LINES.put("B608", new int[] { Color.parseColor("#f8a600"), Color.WHITE });
		LINES.put("B609", new int[] { Color.parseColor("#ef7100"), Color.WHITE });
		LINES.put("B610", new int[] { Color.parseColor("#3ec1f1"), Color.WHITE });
		LINES.put("B611", new int[] { Color.parseColor("#0099db"), Color.WHITE });
		LINES.put("B612", new int[] { Color.parseColor("#ce9d53"), Color.WHITE });
		LINES.put("B613", new int[] { Color.parseColor("#7b3600"), Color.WHITE });
		LINES.put("B614", new int[] { Color.parseColor("#806839"), Color.WHITE });
		LINES.put("B615", new int[] { Color.parseColor("#532700"), Color.WHITE });
		LINES.put("B630", new int[] { Color.parseColor("#c41950"), Color.WHITE });
		LINES.put("B631", new int[] { Color.parseColor("#9b1c44"), Color.WHITE });
		LINES.put("B633", new int[] { Color.parseColor("#88cdc7"), Color.WHITE });
		LINES.put("B635", new int[] { Color.parseColor("#cec800"), Color.WHITE });
		LINES.put("B636", new int[] { Color.parseColor("#af0223"), Color.WHITE });
		LINES.put("B637", new int[] { Color.parseColor("#e3572a"), Color.WHITE });
		LINES.put("B638", new int[] { Color.parseColor("#af5836"), Color.WHITE });
		LINES.put("B640", new int[] { Color.parseColor("#004f81"), Color.WHITE });
		LINES.put("BT650", new int[] { Color.parseColor("#54baa2"), Color.WHITE });
		LINES.put("BT651", new int[] { Color.parseColor("#005738"), Color.WHITE });
		LINES.put("BT680", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B800", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B812", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B843", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B845", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B852", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B855", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B856", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
		LINES.put("B857", new int[] { Color.parseColor("#4e6578"), Color.WHITE });
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
