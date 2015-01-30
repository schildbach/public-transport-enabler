/*
 * Copyright 2010-2015 the original author or authors.
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
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * @author Andreas Schildbach
 */
public class NvbwProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NVBW;
	private final static String API_BASE = "http://www.efa-bw.de/nvbw/"; // no intermeditate stops
	private final static String API_BASE_MOBILE = "http://www.efa-bw.de/android/";

	// http://efa2.naldo.de/naldo/

	public NvbwProvider()
	{
		super(API_BASE + DEFAULT_DEPARTURE_MONITOR_ENDPOINT, API_BASE_MOBILE + DEFAULT_TRIP_ENDPOINT, API_BASE + DEFAULT_STOPFINDER_ENDPOINT,
				API_BASE + DEFAULT_COORD_ENDPOINT);

		setIncludeRegionId(false);
		setUseRouteIndexAsTripId(false);
		setNumTripsRequested(12);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	private static final Pattern P_LINE_S_AVG_VBK = Pattern.compile("(S\\d+) \\((?:AVG|VBK)\\)");

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if (("ICE".equals(trainName) || "InterCityExpress".equals(trainName)) && trainNum == null)
				return "IICE";
			if ("InterCity".equals(trainName) && trainNum == null)
				return "IIC";
			if ("Fernreisezug externer EU".equals(trainName) && trainNum == null)
				return "I";
			if ("SuperCity".equals(trainName) && trainNum == null)
				return "ISC";
			if ("InterRegio".equals(longName) && symbol == null)
				return "RIR";
			if ("REGIOBAHN".equals(trainName) && trainNum == null)
				return "R";
			if ("RR".equals(trainType) && trainNum == null)
				return "RRR";
			if ("Meridian".equals(trainName) && symbol != null)
				return "R" + symbol;
			if ("CityBahn".equals(trainName) && trainNum == null)
				return "RCB";
			if ("Trilex".equals(trainName) && trainNum == null)
				return "RTLX";
			if ("Bay. Seenschifffahrt".equals(trainName) && symbol != null)
				return "F" + symbol;
			if ("Nahverkehrszug von Dritten".equals(trainName) && trainNum == null)
				return "?Zug";
			if ("DB".equals(trainName) && trainNum == null)
				return "?DB";
		}
		else if ("1".equals(mot))
		{
			if (symbol != null && symbol.equals(name))
			{
				final Matcher m = P_LINE_S_AVG_VBK.matcher(symbol);
				if (m.matches())
					return "S" + m.group(1);
			}
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final Set<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility, final Set<Option> options) throws IOException
	{
		return queryTripsMobile(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException
	{
		return queryMoreTripsMobile(contextObj, later);
	}
}
