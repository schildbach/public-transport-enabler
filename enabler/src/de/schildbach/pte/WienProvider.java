/*
 * Copyright 2013 the original author or authors.
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
public class WienProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.WIEN;
	private final static String API_BASE = "http://www.wienerlinien.at/ogd_routing/";

	public WienProvider()
	{
		super(API_BASE);

		setIncludeRegionId(false);
		setStyles(STYLES);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Wien
		STYLES.put("SS1", new Style(Style.Shape.ROUNDED, Style.parseColor("#1e5cb3"), Style.WHITE));
		STYLES.put("SS2", new Style(Style.Shape.ROUNDED, Style.parseColor("#59c594"), Style.WHITE));
		STYLES.put("SS3", new Style(Style.Shape.ROUNDED, Style.parseColor("#c8154c"), Style.WHITE));
		STYLES.put("SS7", new Style(Style.Shape.ROUNDED, Style.parseColor("#dc35a3"), Style.WHITE));
		STYLES.put("SS40", new Style(Style.Shape.ROUNDED, Style.parseColor("#f24d3e"), Style.WHITE));
		STYLES.put("SS45", new Style(Style.Shape.ROUNDED, Style.parseColor("#0f8572"), Style.WHITE));
		STYLES.put("SS50", new Style(Style.Shape.ROUNDED, Style.parseColor("#34b6e5"), Style.WHITE));
		STYLES.put("SS60", new Style(Style.Shape.ROUNDED, Style.parseColor("#82b429"), Style.WHITE));
		STYLES.put("SS80", new Style(Style.Shape.ROUNDED, Style.parseColor("#e96619"), Style.WHITE));

		STYLES.put("UU1", new Style(Style.Shape.RECT, Style.parseColor("#c6292a"), Style.WHITE));
		STYLES.put("UU2", new Style(Style.Shape.RECT, Style.parseColor("#a82783"), Style.WHITE));
		STYLES.put("UU3", new Style(Style.Shape.RECT, Style.parseColor("#f39315"), Style.WHITE));
		STYLES.put("UU4", new Style(Style.Shape.RECT, Style.parseColor("#23a740"), Style.WHITE));
		STYLES.put("UU6", new Style(Style.Shape.RECT, Style.parseColor("#be762c"), Style.WHITE));
	}
}
