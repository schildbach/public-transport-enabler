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
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;

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

		final QueryDeparturesResult result2 = provider.queryDepartures(3000010, 0, false);
		print(result2);

		final QueryDeparturesResult result3 = provider.queryDepartures(3015989, 0, false);
		print(result3);

		final QueryDeparturesResult result4 = provider.queryDepartures(3000139, 0, false);
		print(result4);
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
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 3000001, null, "Hauptwache"), null, new Location(
				LocationType.STATION, 3000912, null, "Südbahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		System.out.println(later2Result);

		if (!later2Result.context.canQueryLater())
			return;

		final QueryTripsResult later3Result = queryMoreTrips(later2Result.context, true);
		System.out.println(later3Result);

		if (!later3Result.context.canQueryLater())
			return;

		final QueryTripsResult later4Result = queryMoreTrips(later3Result.context, true);
		System.out.println(later4Result);

		if (!later4Result.context.canQueryLater())
			return;

		final QueryTripsResult later5Result = queryMoreTrips(later4Result.context, true);
		System.out.println(later5Result);

		if (!later5Result.context.canQueryLater())
			return;

		final QueryTripsResult later6Result = queryMoreTrips(later5Result.context, true);
		System.out.println(later6Result);

		if (!result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
		System.out.println(earlierResult);

		if (!earlierResult.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier2Result = queryMoreTrips(earlierResult.context, false);
		System.out.println(earlier2Result);

		if (!earlier2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier3Result = queryMoreTrips(earlier2Result.context, false);
		System.out.println(earlier3Result);

		if (!earlier3Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier4Result = queryMoreTrips(earlier3Result.context, false);
		System.out.println(earlier4Result);
	}

	@Test
	public void slowTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 3029079, 50017679, 8229480, "Mainz", "An den Dünen"), null,
				new Location(LocationType.STATION, 3013508, 50142890, 8895203, "Hanau", "Beethovenplatz"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.BARRIER_FREE);
		System.out.println(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void shortTripByName() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, 0, null, "Frankfurt Bockenheimer Warte!"), null, new Location(
				LocationType.ANY, 0, null, "Frankfurt Hauptbahnhof!"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
	}
}
