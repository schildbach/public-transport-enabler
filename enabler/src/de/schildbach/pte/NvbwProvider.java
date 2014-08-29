/*
 * Copyright 2010-2014 the original author or authors.
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

/**
 * @author Andreas Schildbach
 */
public class NvbwProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NVBW;
	private final static String API_BASE = "http://www.efa-bw.de/nvbw/"; // http://www.efa-bw.de/android/

	public NvbwProvider()
	{
		super(API_BASE);

		setIncludeRegionId(false);
		setUseRouteIndexAsTripId(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("ICE".equals(trainName) && trainNum == null)
				return "IICE";
			if ("Fernreisezug externer EU".equals(trainName))
				return "I";
			if ("Nahverkehrszug von Dritten".equals(trainName) || "DB-Zug".equals(trainName) || "REGIOBAHN".equals(trainName))
				return "R";
			if ("RR".equals(trainType))
				return "RRR" + trainNum;
			if ("Meridian".equals(trainName) && symbol != null)
				return "R" + symbol;
			if ("CityBahn".equals(trainName) && trainNum == null)
				return "RCB";
			if ("Trilex".equals(trainName) && trainNum == null)
				return "RTLX";
			if ("Koleje Dolnoslaskie".equals(trainName))
				return "RKD";
			if ("Bay. Seenschifffahrt".equals(trainName) && symbol != null)
				return "F" + symbol;
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
