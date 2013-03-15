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

import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.GVH;
	private static final String API_BASE = "http://mobil.efa.de/mobile3/";

	public GvhProvider(final String additionalQueryParameter)
	{
		super(API_BASE);

		setAdditionalQueryParameter(additionalQueryParameter);
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
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("S1".equals(symbol))
				return "SS1";
			if ("S11".equals(symbol))
				return "SS11";
			if ("S2".equals(symbol))
				return "SS2";
			if ("S21".equals(symbol))
				return "SS21";
			if ("S3".equals(symbol))
				return "SS3";
			if ("S31".equals(symbol))
				return "SS31";
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// Hamburg
		LINES.put("SS1", new Style(Style.parseColor("#00933B"), Style.WHITE));
		LINES.put("SS11", new Style(Style.WHITE, Style.parseColor("#00933B"), Style.parseColor("#00933B")));
		LINES.put("SS2", new Style(Style.WHITE, Style.parseColor("#9D271A"), Style.parseColor("#9D271A")));
		LINES.put("SS21", new Style(Style.parseColor("#9D271A"), Style.WHITE));
		LINES.put("SS3", new Style(Style.parseColor("#411273"), Style.WHITE));
		LINES.put("SS31", new Style(Style.parseColor("#411273"), Style.WHITE));

		LINES.put("UU1", new Style(Style.parseColor("#044895"), Style.WHITE));
		LINES.put("UU2", new Style(Style.parseColor("#DC2B19"), Style.WHITE));
		LINES.put("UU3", new Style(Style.parseColor("#EE9D16"), Style.WHITE));
		LINES.put("UU4", new Style(Style.parseColor("#13A59D"), Style.WHITE));
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
