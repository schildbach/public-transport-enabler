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
public class SfProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SF;
	private final static String API_BASE = "http://tripplanner.transit.511.org/mtc/";

	public SfProvider()
	{
		super(API_BASE);

		setUseRouteIndexAsTripId(false);
		setFareCorrectionFactor(0.01f);
		setStyles(STYLES);
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
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
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
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Train".equals(trainName))
				return "R" + symbol;

			if ("Millbrae / Richmond".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Richmond / Millbrae".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Fremont / Richmond".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Richmond / Fremont".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Pittsburg Bay Point / SFO".equals(symbol)) // BART
				return 'U' + symbol;
			if ("SFO / Pittsburg Bay Point".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Dublin Pleasanton / Daly City".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Daly City / Dublin Pleasanton".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Fremont / Daly City".equals(symbol)) // BART
				return 'U' + symbol;
			if ("Fremont / Daly City".equals(trainNum)) // BART
				return 'U' + trainNum;
			if ("Daly City / Fremont".equals(symbol)) // BART
				return 'U' + symbol;

			if ("Muni Rail".equals(trainName)) // Muni
				return 'T' + symbol;
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// BART
		STYLES.put("RDaly City / Dublin Pleasanton", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
		STYLES.put("RDulin Pleasanton / Daly City", new Style(Style.parseColor("#00AEEF"), Style.WHITE));

		STYLES.put("RSFO / Pittsburg Bay Point", new Style(Style.parseColor("#FFE800"), Style.BLACK));
		STYLES.put("RPittsburg Bay Point / SFO", new Style(Style.parseColor("#FFE800"), Style.BLACK));

		STYLES.put("RDaly City / Fremont", new Style(Style.parseColor("#4EBF49"), Style.WHITE));
		STYLES.put("RFremont / Daly City", new Style(Style.parseColor("#4EBF49"), Style.WHITE));

		STYLES.put("RFremont / Richmond", new Style(Style.parseColor("#FAA61A"), Style.WHITE));
		STYLES.put("RRichmond / Fremont", new Style(Style.parseColor("#FAA61A"), Style.WHITE));

		STYLES.put("RMillbrae / Richmond", new Style(Style.parseColor("#F81A23"), Style.WHITE));
		STYLES.put("RRichmond / Millbrae", new Style(Style.parseColor("#F81A23"), Style.WHITE));
	}
}
