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
		super(API_BASE);

		setNeedsSpEncId(true);
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

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Busse Bonn
		STYLES.put("B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
		STYLES.put("BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
		STYLES.put("BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
		STYLES.put("B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
		STYLES.put("B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
		STYLES.put("B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
		STYLES.put("B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
		STYLES.put("B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
		STYLES.put("B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
		STYLES.put("B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
		STYLES.put("B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
		STYLES.put("B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
		STYLES.put("B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
		STYLES.put("B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
		STYLES.put("B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
		STYLES.put("B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
		STYLES.put("B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
		STYLES.put("B614", new Style(Style.parseColor("#806839"), Style.WHITE));
		STYLES.put("B615", new Style(Style.parseColor("#532700"), Style.WHITE));
		STYLES.put("B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
		STYLES.put("B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
		STYLES.put("B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
		STYLES.put("B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
		STYLES.put("B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
		STYLES.put("B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
		STYLES.put("B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
		STYLES.put("B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
		STYLES.put("BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
		STYLES.put("BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
		STYLES.put("BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
	}
}
