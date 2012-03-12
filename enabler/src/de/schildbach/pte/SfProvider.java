/*
 * Copyright 2010-2012 the original author or authors.
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
public class SfProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SF;
	private final static String API_BASE = "http://tripplanner.transit.511.org/mtc/";

	public SfProvider()
	{
		super(API_BASE, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("America/Los_Angeles");
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected String normalizeLocationName(final String name)
	{
		if (name == null || name.length() == 0)
			return null;

		return super.normalizeLocationName(name).replace("$XINT$", "&");
	}

	@Override
	protected String parseLine(final String mot, final String name, final String longName, final String noTrainName)
	{
		if ("NORTHBOUND".equals(name))
			return "?" + name;
		else if ("SOUTHBOUND".equals(name))
			return "?" + name;
		else if ("EASTBOUND".equals(name))
			return "?" + name;
		else if ("WESTBOUND".equals(name))
			return "?" + name;
		else
			return super.parseLine(mot, name, longName, noTrainName);
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// BART
		LINES.put("RDaly City / Dublin Pleasanton", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
		LINES.put("RDulin Pleasanton / Daly City", new Style(Style.parseColor("#00AEEF"), Style.WHITE));

		LINES.put("RSFO / Pittsburg Bay Point", new Style(Style.parseColor("#FFE800"), Style.BLACK));
		LINES.put("RPittsburg Bay Point / SFO", new Style(Style.parseColor("#FFE800"), Style.BLACK));

		LINES.put("RDaly City / Fremont", new Style(Style.parseColor("#4EBF49"), Style.WHITE));
		LINES.put("RFremont / Daly City", new Style(Style.parseColor("#4EBF49"), Style.WHITE));

		LINES.put("RFremont / Richmond", new Style(Style.parseColor("#FAA61A"), Style.WHITE));
		LINES.put("RRichmond / Fremont", new Style(Style.parseColor("#FAA61A"), Style.WHITE));

		LINES.put("RMillbrae / Richmond", new Style(Style.parseColor("#F81A23"), Style.WHITE));
		LINES.put("RRichmond / Millbrae", new Style(Style.parseColor("#F81A23"), Style.WHITE));
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
