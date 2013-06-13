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
public class AvvProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.AVV;
	private final static String API_BASE = "http://efa.avv-augsburg.de/avv/";

	public AvvProvider()
	{
		super(API_BASE);

		setUseRouteIndexAsTripId(false);
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

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		LINES.put("BB1", new Style(Style.Shape.CIRCLE, Style.parseColor("#93117e"), Style.WHITE));
		LINES.put("BB3", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
		LINES.put("B21", new Style(Style.Shape.CIRCLE, Style.parseColor("#00896b"), Style.WHITE));
		LINES.put("B22", new Style(Style.Shape.CIRCLE, Style.parseColor("#eb6b59"), Style.WHITE));
		LINES.put("B23", new Style(Style.Shape.CIRCLE, Style.parseColor("#97bf0d"), Style.parseColor("#d10019")));
		LINES.put("B27", new Style(Style.Shape.CIRCLE, Style.parseColor("#74b57e"), Style.WHITE));
		LINES.put("B29", new Style(Style.Shape.CIRCLE, Style.parseColor("#5f689f"), Style.WHITE));
		LINES.put("B30", new Style(Style.Shape.CIRCLE, Style.parseColor("#829ac3"), Style.WHITE));
		LINES.put("B31", new Style(Style.Shape.CIRCLE, Style.parseColor("#a3cdb0"), Style.parseColor("#006835")));
		LINES.put("B32", new Style(Style.Shape.CIRCLE, Style.parseColor("#45a477"), Style.WHITE));
		LINES.put("B33", new Style(Style.Shape.CIRCLE, Style.parseColor("#a0ca82"), Style.WHITE));
		LINES.put("B35", new Style(Style.Shape.CIRCLE, Style.parseColor("#0085c5"), Style.WHITE));
		LINES.put("B36", new Style(Style.Shape.CIRCLE, Style.parseColor("#b1c2e1"), Style.parseColor("#006ab3")));
		LINES.put("B37", new Style(Style.Shape.CIRCLE, Style.parseColor("#eac26b"), Style.BLACK));
		LINES.put("B38", new Style(Style.Shape.CIRCLE, Style.parseColor("#c3655a"), Style.WHITE));
		LINES.put("B41", new Style(Style.Shape.CIRCLE, Style.parseColor("#d26110"), Style.WHITE));
		LINES.put("B42", new Style(Style.Shape.CIRCLE, Style.parseColor("#d57642"), Style.WHITE));
		LINES.put("B43", new Style(Style.Shape.CIRCLE, Style.parseColor("#e29241"), Style.WHITE));
		LINES.put("B44", new Style(Style.Shape.CIRCLE, Style.parseColor("#d0aacc"), Style.parseColor("#6d1f80")));
		LINES.put("B45", new Style(Style.Shape.CIRCLE, Style.parseColor("#a76da7"), Style.WHITE));
		LINES.put("B46", new Style(Style.Shape.CIRCLE, Style.parseColor("#52bcc2"), Style.WHITE));
		LINES.put("B48", new Style(Style.Shape.CIRCLE, Style.parseColor("#a6d7d2"), Style.parseColor("#079098")));
		LINES.put("B51", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
		LINES.put("B52", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
		LINES.put("B54", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
		LINES.put("B56", new Style(Style.Shape.CIRCLE, Style.parseColor("#a86853"), Style.WHITE));
		LINES.put("B57", new Style(Style.Shape.CIRCLE, Style.parseColor("#a76da7"), Style.WHITE));
		LINES.put("B58", new Style(Style.Shape.CIRCLE, Style.parseColor("#d0aacc"), Style.parseColor("#6d1f80")));
		LINES.put("B59", new Style(Style.Shape.CIRCLE, Style.parseColor("#b1c2e1"), Style.parseColor("#00519e")));
		LINES.put("B70", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
		LINES.put("B71", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
		LINES.put("B72", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
		LINES.put("B76", new Style(Style.Shape.CIRCLE, Style.parseColor("#c3655a"), Style.WHITE));

		LINES.put("T2", new Style(Style.Shape.RECT, Style.parseColor("#006ab3"), Style.WHITE));
		LINES.put("T13", new Style(Style.Shape.RECT, Style.parseColor("#e2001a"), Style.WHITE));
		LINES.put("T64", new Style(Style.Shape.RECT, Style.parseColor("#97bf0d"), Style.WHITE));

		LINES.put("RR1", new Style(Style.Shape.ROUNDED, Style.parseColor("#1bbbea"), Style.WHITE));
		LINES.put("RR2", new Style(Style.Shape.ROUNDED, Style.parseColor("#003a80"), Style.WHITE));
		LINES.put("RR4", new Style(Style.Shape.ROUNDED, Style.parseColor("#bd5619"), Style.WHITE));
		LINES.put("RR6", new Style(Style.Shape.ROUNDED, Style.parseColor("#0098a1"), Style.WHITE));
		LINES.put("RR7", new Style(Style.Shape.ROUNDED, Style.parseColor("#80191c"), Style.WHITE));
		LINES.put("RR8", new Style(Style.Shape.ROUNDED, Style.parseColor("#007d40"), Style.WHITE));
		LINES.put("RR11", new Style(Style.Shape.ROUNDED, Style.parseColor("#e6a300"), Style.WHITE));
	}

	private static final Style BUS_STYLE = new Style(Style.Shape.CIRCLE, Style.parseColor("#abb1b1"), Style.BLACK);

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		else if (line.charAt(0) == 'B')
			return BUS_STYLE;
		else
			return super.lineStyle(line);
	}
}
