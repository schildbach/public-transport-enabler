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

package de.schildbach.pte.live;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractProviderLiveTest
{
	protected final NetworkProvider provider;

	public AbstractProviderLiveTest(final NetworkProvider provider)
	{
		this.provider = provider;
	}

	protected final void print(final NearbyStationsResult result)
	{
		System.out.println(result.status
				+ (result.status == NearbyStationsResult.Status.OK ? " " + result.stations.size() + "  " + result.stations : ""));
	}

	protected final void print(final QueryDeparturesResult result)
	{
		System.out.println(result.status + (result.status == QueryDeparturesResult.Status.OK ? " " + result.stationDepartures : ""));

		// for (final StationDepartures stationDepartures : result.stationDepartures)
		// for (final Departure departure : stationDepartures.departures)
		// System.out.println(departure);
	}

	protected final void print(final List<Location> autocompletes)
	{
		System.out.print(autocompletes.size() + " ");
		for (final Location autocomplete : autocompletes)
			System.out.print(autocomplete.toDebugString() + " ");
		System.out.println();
	}

	protected final QueryTripsResult queryTrips(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility) throws IOException
	{
		return provider.queryTrips(from, via, to, date, dep, 4, products, walkSpeed, accessibility, null);
	}

	protected final QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return provider.queryMoreTrips(context, later, 4);
	}
}
