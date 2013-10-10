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
		STYLES.put("vrs|B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
		STYLES.put("vrs|BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
		STYLES.put("vrs|BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
		STYLES.put("vrs|B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
		STYLES.put("vrs|B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
		STYLES.put("vrs|B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
		STYLES.put("vrs|B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
		STYLES.put("vrs|B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
		STYLES.put("vrs|B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
		STYLES.put("vrs|B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
		STYLES.put("vrs|B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
		STYLES.put("vrs|B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
		STYLES.put("vrs|B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
		STYLES.put("vrs|B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
		STYLES.put("vrs|B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
		STYLES.put("vrs|B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
		STYLES.put("vrs|B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
		STYLES.put("vrs|B614", new Style(Style.parseColor("#806839"), Style.WHITE));
		STYLES.put("vrs|B615", new Style(Style.parseColor("#532700"), Style.WHITE));
		STYLES.put("vrs|B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
		STYLES.put("vrs|B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
		STYLES.put("vrs|B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
		STYLES.put("vrs|B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
		STYLES.put("vrs|B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
		STYLES.put("vrs|B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
		STYLES.put("vrs|B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
		STYLES.put("vrs|B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
		STYLES.put("vrs|BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
		STYLES.put("vrs|BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
		STYLES.put("vrs|BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
	}
}
