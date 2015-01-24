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

import com.google.common.base.Charsets;

/**
 * @author Andreas Schildbach
 */
public class VvoProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VVO;
	private final static String API_BASE = "http://efa.vvo-online.de:8080/dvb/";

	public VvoProvider()
	{
		this(API_BASE);
	}

	public VvoProvider(final String apiBase)
	{
		super(apiBase);

		setUseRealtime(false);
		setUseStringCoordListOutputFormat(false);
		setRequestUrlEncoding(Charsets.UTF_8);
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
			if ("Twoje Linie Kolejowe".equals(trainName) && symbol != null)
				return "ITLK" + symbol;

			if ("Regionalbahn".equals(trainName) && trainNum == null)
				return "R";
			if ("Ostdeutsche Eisenbahn GmbH".equals(longName))
				return "ROE";
			if ("Meridian".equals(longName))
				return "RM";
			if ("trilex".equals(longName))
				return "RTLX";
			if ("Trilex".equals(trainName) && trainNum == null)
				return "RTLX";
			if ("U28".equals(symbol)) // Nationalparkbahn
				return "RU28";

			if ("Fernbus".equals(trainName) && trainNum == null)
				return "B" + trainName;
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
