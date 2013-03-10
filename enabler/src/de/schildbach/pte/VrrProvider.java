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
public class VrrProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VRR;
	private static final String API_BASE = "http://app.vrr.de/standard/";

	public VrrProvider()
	{
		super(API_BASE, null, null, null, null, null, true);
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
		return xmlStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// Busse Bonn
		LINES.put("B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		LINES.put("B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		LINES.put("B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		LINES.put("B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		LINES.put("BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
		LINES.put("BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
		LINES.put("BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
		LINES.put("B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		LINES.put("B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
		LINES.put("B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
		LINES.put("B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
		LINES.put("B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
		LINES.put("B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
		LINES.put("B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
		LINES.put("B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
		LINES.put("B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
		LINES.put("B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
		LINES.put("B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
		LINES.put("B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
		LINES.put("B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
		LINES.put("B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
		LINES.put("B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
		LINES.put("B614", new Style(Style.parseColor("#806839"), Style.WHITE));
		LINES.put("B615", new Style(Style.parseColor("#532700"), Style.WHITE));
		LINES.put("B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
		LINES.put("B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
		LINES.put("B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
		LINES.put("B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
		LINES.put("B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
		LINES.put("B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
		LINES.put("B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
		LINES.put("B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
		LINES.put("BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
		LINES.put("BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
		LINES.put("BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		LINES.put("B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
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
