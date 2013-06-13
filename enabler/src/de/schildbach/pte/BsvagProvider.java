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

/**
 * @author Andreas Schildbach
 */
public class BsvagProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.BSVAG;
	private final static String API_BASE = "http://212.68.73.240/bsvag/"; // http://212.68.73.240/vrbstd/

	public BsvagProvider()
	{
		super(API_BASE);
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

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlStopfinderRequest(new Location(LocationType.STATION, 0, null, constraint.toString()));
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// Braunschweig
		LINES.put("TM1", new Style(Style.parseColor("#62c2a2"), Style.WHITE));
		LINES.put("TM2", new Style(Style.parseColor("#b35e89"), Style.WHITE));
		LINES.put("TM3", new Style(Style.parseColor("#f9b5b9"), Style.WHITE));
		LINES.put("TM4", new Style(Style.parseColor("#811114"), Style.WHITE));
		LINES.put("TM5", new Style(Style.parseColor("#ffd00b"), Style.WHITE));

		LINES.put("BM11", new Style(Style.parseColor("#88891e"), Style.WHITE));
		LINES.put("BM13", new Style(Style.parseColor("#24a06d"), Style.WHITE));
		LINES.put("BM16", new Style(Style.parseColor("#f8991b"), Style.WHITE));
		LINES.put("BM19", new Style(Style.parseColor("#2c2768"), Style.WHITE));
		LINES.put("BM29", new Style(Style.parseColor("#2c2768"), Style.WHITE));

		LINES.put("B412", new Style(Style.parseColor("#094f34"), Style.WHITE));
		LINES.put("B414", new Style(Style.parseColor("#00bce4"), Style.WHITE));
		LINES.put("B415", new Style(Style.parseColor("#b82837"), Style.WHITE));
		LINES.put("B417", new Style(Style.parseColor("#2a2768"), Style.WHITE));
		LINES.put("B418", new Style(Style.parseColor("#c12056"), Style.WHITE));
		LINES.put("B420", new Style(Style.parseColor("#b7d55b"), Style.WHITE));
		LINES.put("B422", new Style(Style.parseColor("#16bce4"), Style.WHITE));
		LINES.put("B424", new Style(Style.parseColor("#ffdf65"), Style.WHITE));
		LINES.put("B427", new Style(Style.parseColor("#b5d55b"), Style.WHITE));
		LINES.put("B431", new Style(Style.parseColor("#fddb62"), Style.WHITE));
		LINES.put("B433", new Style(Style.parseColor("#ed0e65"), Style.WHITE));
		LINES.put("B434", new Style(Style.parseColor("#bf2555"), Style.WHITE));
		LINES.put("B436", new Style(Style.parseColor("#0080a2"), Style.WHITE));
		LINES.put("B437", new Style(Style.parseColor("#fdd11a"), Style.WHITE));
		LINES.put("B442", new Style(Style.parseColor("#cc3f68"), Style.WHITE));
		LINES.put("B443", new Style(Style.parseColor("#405a80"), Style.WHITE));
		LINES.put("B445", new Style(Style.parseColor("#3ca14a"), Style.WHITE));
		LINES.put("B450", new Style(Style.parseColor("#f2635a"), Style.WHITE));
		LINES.put("B451", new Style(Style.parseColor("#f5791e"), Style.WHITE));
		LINES.put("B452", new Style(Style.parseColor("#f0a3ca"), Style.WHITE));
		LINES.put("B455", new Style(Style.parseColor("#395f95"), Style.WHITE));
		LINES.put("B461", new Style(Style.parseColor("#00b8a0"), Style.WHITE));
		LINES.put("B464", new Style(Style.parseColor("#00a14b"), Style.WHITE));
		LINES.put("B465", new Style(Style.parseColor("#77234b"), Style.WHITE));
		LINES.put("B471", new Style(Style.parseColor("#380559"), Style.WHITE));
		LINES.put("B480", new Style(Style.parseColor("#2c2768"), Style.WHITE));
		LINES.put("B481", new Style(Style.parseColor("#007ec1"), Style.WHITE));
		LINES.put("B484", new Style(Style.parseColor("#dc8998"), Style.WHITE));
		LINES.put("B485", new Style(Style.parseColor("#ea8d52"), Style.WHITE));
		LINES.put("B493", new Style(Style.parseColor("#f24825"), Style.WHITE));
		LINES.put("B560", new Style(Style.parseColor("#9f6fb0"), Style.WHITE));

		// Wolfsburg
		LINES.put("B201", new Style(Style.parseColor("#f1471c"), Style.WHITE));
		LINES.put("B202", new Style(Style.parseColor("#127bca"), Style.WHITE));
		LINES.put("B203", new Style(Style.parseColor("#f35c95"), Style.WHITE));
		LINES.put("B204", new Style(Style.parseColor("#00a650"), Style.WHITE));
		LINES.put("B205", new Style(Style.parseColor("#f67c13"), Style.WHITE));
		LINES.put("B206", new Style(Style.WHITE, Style.parseColor("#00adef"), Style.parseColor("#00adef")));
		LINES.put("B207", new Style(Style.parseColor("#94d221"), Style.WHITE));
		LINES.put("B208", new Style(Style.parseColor("#00adef"), Style.WHITE));
		LINES.put("B209", new Style(Style.parseColor("#bf7f50"), Style.WHITE));
		LINES.put("B211", new Style(Style.parseColor("#be65ba"), Style.WHITE));
		LINES.put("B212", new Style(Style.parseColor("#be65ba"), Style.WHITE));
		LINES.put("B213", new Style(Style.parseColor("#918f90"), Style.WHITE));
		LINES.put("B218", new Style(Style.parseColor("#a950ae"), Style.WHITE));
		LINES.put("B219", new Style(Style.parseColor("#bf7f50"), Style.WHITE));
		LINES.put("B230", new Style(Style.parseColor("#ca93d0"), Style.WHITE));
		LINES.put("B231", new Style(Style.WHITE, Style.parseColor("#fab20a"), Style.parseColor("#fab20a")));
		LINES.put("B244", new Style(Style.parseColor("#66cef6"), Style.WHITE));
		LINES.put("B267", new Style(Style.parseColor("#918f90"), Style.WHITE));
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
