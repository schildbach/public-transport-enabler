/*
 * Copyright 2010 the original author or authors.
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

import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.GvhProvider;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Autocomplete;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;

/**
 * @author Andreas Schildbach
 */
public class GvhProviderLiveTest
{
	private final GvhProvider provider = new GvhProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Kur");

		list(autocompletes);
	}

	@Test
	public void autocompleteIdentified() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Hannover, Hannoversche Straße");

		list(autocompletes);
	}

	@Test
	public void autocompleteCity() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Hannover");

		list(autocompletes);
	}

	private void list(final List<Autocomplete> autocompletes)
	{
		System.out.print(autocompletes.size() + " ");
		for (final Autocomplete autocomplete : autocompletes)
			System.out.print(autocomplete.toDebugString() + " ");
		System.out.println();
	}

	@Test
	public void autocomplete() throws Exception
	{
		final List<Autocomplete> results = provider.autocompleteStations("Hannover");

		System.out.println(results.size() + "  " + results);
	}

	@Test
	public void nearbyStation() throws Exception
	{
		final NearbyStationsResult result = provider.nearbyStations("25000031", 0, 0, 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void incompleteConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "hann", null, null, LocationType.ANY, "laat", new Date(),
				true, ALL_PRODUCTS, WalkSpeed.FAST);
		System.out.println(result);
	}
}
