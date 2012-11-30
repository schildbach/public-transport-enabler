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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.RmvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class RmvProviderLiveTest extends AbstractProviderLiveTest
{
	public RmvProviderLiveTest()
	{
		super(new RmvProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 3000001), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 50108625, 8669604), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(3000408, 0, false);

		print(result);
	}

	@Test
	public void autocomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Flughafen");

		print(autocompletes);
	}

	@Test
	public void autocompleteUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("könig");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 3000001, null, "Hauptwache"), null, new Location(
				LocationType.STATION, 3000912, null, "Südbahnhof"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		assertEquals(QueryConnectionsResult.Status.OK, result.status);
		assertTrue(result.connections.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryConnectionsResult later2Result = queryMoreConnections(laterResult.context, true);
		System.out.println(later2Result);

		if (!later2Result.context.canQueryLater())
			return;

		final QueryConnectionsResult later3Result = queryMoreConnections(later2Result.context, true);
		System.out.println(later3Result);

		if (!later3Result.context.canQueryLater())
			return;

		final QueryConnectionsResult later4Result = queryMoreConnections(later3Result.context, true);
		System.out.println(later4Result);

		if (!later4Result.context.canQueryLater())
			return;

		final QueryConnectionsResult later5Result = queryMoreConnections(later4Result.context, true);
		System.out.println(later5Result);

		if (!later5Result.context.canQueryLater())
			return;

		final QueryConnectionsResult later6Result = queryMoreConnections(later5Result.context, true);
		System.out.println(later6Result);

		if (!result.context.canQueryEarlier())
			return;

		final QueryConnectionsResult earlierResult = queryMoreConnections(result.context, false);
		System.out.println(earlierResult);

		if (!earlierResult.context.canQueryEarlier())
			return;

		final QueryConnectionsResult earlier2Result = queryMoreConnections(earlierResult.context, false);
		System.out.println(earlier2Result);

		if (!earlier2Result.context.canQueryEarlier())
			return;

		final QueryConnectionsResult earlier3Result = queryMoreConnections(earlier2Result.context, false);
		System.out.println(earlier3Result);

		if (!earlier3Result.context.canQueryEarlier())
			return;

		final QueryConnectionsResult earlier4Result = queryMoreConnections(earlier3Result.context, false);
		System.out.println(earlier4Result);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(
				new Location(LocationType.STATION, 3029079, 50017679, 8229480, "Mainz", "An den Dünen"), null, new Location(LocationType.STATION,
						3013508, 50142890, 8895203, "Hanau", "Beethovenplatz"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.BARRIER_FREE);
		System.out.println(result);
		assertEquals(QueryConnectionsResult.Status.OK, result.status);
		assertTrue(result.connections.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void shortConnectionByName() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ANY, 0, null, "Frankfurt Bockenheimer Warte!"), null,
				new Location(LocationType.ANY, 0, null, "Frankfurt Hauptbahnhof!"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
	}
}
