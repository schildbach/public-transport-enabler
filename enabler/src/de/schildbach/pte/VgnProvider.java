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

import java.util.Date;

import de.schildbach.pte.dto.Location;

/**
 * @author Andreas Schildbach
 */
public class VgnProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VGN;
	private static final String DEPARTURE_MONITOR_ENDPOINT = "XML_DM_REQUEST";
	private static final String TRIP_ENDPOINT = "XML_TRIP_REQUEST2";

	public VgnProvider(final String apiBase)
	{
		super(apiBase, DEPARTURE_MONITOR_ENDPOINT, TRIP_ENDPOINT, null, false, false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected String xsltTripRequest2Uri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int numConnections, final String products, final WalkSpeed walkSpeed, final Accessibility accessibility)
	{
		return super.xsltTripRequest2Uri(from, via, to, date, dep, numConnections, products, walkSpeed, accessibility) + "&itdLPxx_showTariffLevel=1";
	}
}
