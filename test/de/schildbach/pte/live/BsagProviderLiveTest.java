/*
 * Copyright 2010, 2011 the original author or authors.
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

import de.schildbach.pte.BsagProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;

/**
 * @author Andreas Schildbach
 */
public class BsagProviderLiveTest
{
	private final BsagProvider provider = new BsagProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 28000257), 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 53076146, 8806858), 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Kurf");

		list(autocompletes);
	}
	
	@Test
	public void autocompleteWithUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("grünwink");

		list(autocompletes);
	}



	private void list(final List<Location> autocompletes)
	{
		System.out.print(autocompletes.size() + " ");
		for (final Location autocomplete : autocompletes)
			System.out.print(autocomplete.toDebugString() + " ");
		System.out.println();
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.STATION, 28000257, null, "Bremen, Hauptbahnhof"),
				null, new Location(LocationType.STATION, 28000512, null, "Herdentor"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}
}
