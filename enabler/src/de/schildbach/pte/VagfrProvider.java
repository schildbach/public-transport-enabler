/*
 * Copyright 2010-2013 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class VagfrProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VAGFR;
	private final static String API_BASE = "http://efa.vag-freiburg.de/vagfr/";

	public VagfrProvider()
	{
		super(API_BASE);

		setUseRouteIndexAsConnectionId(false);
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
		return xmlStopfinderRequest(new Location(LocationType.STATION, 0, null, constraint.toString()));
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("BSB-Zug".equals(longName))
				return "SBSB";
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// Tram
		LINES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		LINES.put("T2", new Style(Shape.RECT, Style.parseColor("#33b540"), Style.WHITE));
		LINES.put("T3", new Style(Shape.RECT, Style.parseColor("#f79210"), Style.WHITE));
		LINES.put("T5", new Style(Shape.RECT, Style.parseColor("#0994ce"), Style.WHITE));

		// Nachtbus
		LINES.put("BN42 Jupiter", new Style(Style.parseColor("#33b540"), Style.WHITE));
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		else
			return super.lineStyle(line);
	}
}
