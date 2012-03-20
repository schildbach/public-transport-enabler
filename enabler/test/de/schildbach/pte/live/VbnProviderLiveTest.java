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

package de.schildbach.pte.live;

import static junit.framework.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.VbnProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class VbnProviderLiveTest extends AbstractProviderLiveTest
{
	private final VbnProvider provider = new VbnProvider();

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 8000110), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 51318447, 9496250), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(8000110, 0, false);

		print(result);
	}

	@Test
	public void autocomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Coppengrave");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.STATION, 8096109, null, "Oldenburg"), null,
				new Location(LocationType.STATION, 625398, null, "Bremerhaven"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		assertEquals(QueryConnectionsResult.Status.OK, result.status);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionDateOutsideTimetablePeriod() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.STATION, 8096109, null, "Oldenburg"), null,
				new Location(LocationType.STATION, 625398, null, "Bremerhaven"), new Date(1155822689759l), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		assertEquals(QueryConnectionsResult.Status.INVALID_DATE, result.status);
	}
}
