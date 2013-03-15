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

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class TflProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.TFL;
	private static final String API_BASE = "http://journeyplanner.tfl.gov.uk/user/";

	public TflProvider()
	{
		super(API_BASE);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("Europe/London");
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainName)
	{
		if ("0".equals(mot) && "First Hull Trains".equals(trainName))
			return "I" + name;

		else if ("0".equals(mot) && ("Southern".equals(trainName) || "=SN".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("Southeastern".equals(trainName) || "=SE".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("South West Trains".equals(trainName) || "=SW".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("Greater Anglia".equals(trainName) || "=LE".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("First Great Western".equals(trainName) || "=GW".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("First Capital Connect".equals(trainName) || "=FC".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("Northern Rail".equals(trainName) || "=NT".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && "Chiltern Railways".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && "Heathrow Connect".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && ("Heathrow Express".equals(trainName) || "=HX".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && "Gatwick Express".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && ("Merseyrail".equals(trainName) || "=ME".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("East Coast".equals(trainName) || "=GR".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && "Cross Country".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && ("East Midlands Trains".equals(trainName) || "=EM".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("Arriva Trains Wales".equals(trainName) || "=AW".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("First TransPennine Express".equals(trainName) || "=TP".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("ScotRail".equals(trainName) || "=SR".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("London Midland".equals(trainName) || "=LM".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && ("c2c".equals(trainName) || "=CC".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && "Grand Central".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && ("Virgin Trains".equals(trainName) || "=VT".equals(trainName)))
			return "R" + name;
		else if ("0".equals(mot) && "Island Line".equals(trainName))
			return "R" + name;
		else if ("0".equals(mot) && ("Chiltern Railways".equals(trainName) || "=CH".equals(trainName)))
			return "R" + name;

		else if ("0".equals(mot) && "London Overground".equals(trainName))
			return "S" + name;

		else
			return super.parseLine(mot, symbol, name, longName, trainName);
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// London
		LINES.put("UBakerloo", new Style(Style.parseColor("#9D5324"), Style.WHITE));
		LINES.put("UCentral", new Style(Style.parseColor("#D52B1E"), Style.WHITE));
		LINES.put("UCircle", new Style(Style.parseColor("#FECB00"), Style.BLACK));
		LINES.put("UDistrict", new Style(Style.parseColor("#007934"), Style.WHITE));
		LINES.put("UEast London", new Style(Style.parseColor("#FFA100"), Style.WHITE));
		LINES.put("UHammersmith & City", new Style(Style.parseColor("#C5858F"), Style.BLACK));
		LINES.put("UJubilee", new Style(Style.parseColor("#818A8F"), Style.WHITE));
		LINES.put("UMetropolitan", new Style(Style.parseColor("#850057"), Style.WHITE));
		LINES.put("UNorthern", new Style(Style.BLACK, Style.WHITE));
		LINES.put("UPicadilly", new Style(Style.parseColor("#0018A8"), Style.WHITE));
		LINES.put("UVictoria", new Style(Style.parseColor("#00A1DE"), Style.WHITE));
		LINES.put("UWaterloo & City", new Style(Style.parseColor("#76D2B6"), Style.BLACK));

		LINES.put("SDLR", new Style(Style.parseColor("#00B2A9"), Style.WHITE));
		LINES.put("SLO", new Style(Style.parseColor("#f46f1a"), Style.WHITE));

		LINES.put("TTramlink 1", new Style(Style.rgb(193, 215, 46), Style.WHITE));
		LINES.put("TTramlink 2", new Style(Style.rgb(193, 215, 46), Style.WHITE));
		LINES.put("TTramlink 3", new Style(Style.rgb(124, 194, 66), Style.BLACK));
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		if (line.startsWith("SLO"))
			return LINES.get("SLO");
		return super.lineStyle(line);
	}
}
