/*
 * Copyright 2010-2015 the original author or authors.
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

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.TlemProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class TlemProviderLiveTest extends AbstractProviderLiveTest
{
	public TlemProviderLiveTest()
	{
		super(new TlemProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result1 = provider.queryNearbyStations(new Location(LocationType.STATION, "1001003"), 0, 0);
		print(result1);

		final NearbyStationsResult result2 = provider.queryNearbyStations(new Location(LocationType.STATION, "1000086"), 0, 0);
		print(result2);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 51507161, -0127144), 0, 0);
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result1 = queryDepartures("1001003", false);
		print(result1);

		final QueryDeparturesResult result2 = queryDepartures("1000086", false);
		print(result2);
	}

	@Test
	public void queryDeparturesEquivs() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("1001003", true);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult resultLive = queryDepartures("999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultLive.status);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Lower Arncott The Plough");

		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Birming");

		print(result);
	}

	@Test
	public void shortTrip1() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1008730", null, "King & Queen Wharf"), null, new Location(
				LocationType.STATION, "1006433", null, "Edinburgh Court"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		print(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		print(earlierResult);
	}

	@Test
	public void shortTrip2() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "2099014", 52478184, -1898364, "Birmingham",
				"Birmingham New Street Rail Station"), null, new Location(LocationType.STATION, "2099150", 52585468, -2122962, "Wolverhampton",
				"Wolverhampton Rail Station"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		print(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		print(earlierResult);
	}

	@Test
	public void tripArncott() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "60011202", 51850168, -1094302, "Upper Arncott",
				"Bullingdon Prison"), null, new Location(LocationType.STATION, "60006013", 51856612, -1112904, "Lower Arncott", "The Plough E"),
				new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		print(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		print(earlierResult);
	}
}
