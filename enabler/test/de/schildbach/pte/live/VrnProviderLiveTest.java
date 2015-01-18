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
import de.schildbach.pte.VrnProvider;
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
public class VrnProviderLiveTest extends AbstractProviderLiveTest
{
	public VrnProviderLiveTest()
	{
		super(new VrnProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result1 = queryNearbyStations(new Location(LocationType.STATION, "6032236"));
		print(result1);

		final NearbyStationsResult result2 = queryNearbyStations(new Location(LocationType.STATION, "17001301"));
		print(result2);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result1 = queryNearbyStations(new Location(LocationType.ADDRESS, 49486561, 8477297));
		print(result1);

		final NearbyStationsResult result2 = queryNearbyStations(new Location(LocationType.ADDRESS, 49757571, 6639147));
		print(result2);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result1 = queryDepartures("6032236", false);
		print(result1);

		final QueryDeparturesResult result2 = queryDepartures("17001301", false);
		print(result2);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Kur");
		print(result);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("grün");
		print(result);
	}

	@Test
	public void suggestLocationsIdentified() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Bremen, KUR");
		print(result);
	}

	@Test
	public void suggestLocationsLocality() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Bremen");
		print(result);
	}

	@Test
	public void suggestLocationsCity() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Mannheim");
		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "6002417", 49479748, 8469938, "Mannheim",
				"Mannheim, Hauptbahnhof"), null, new Location(LocationType.STATION, "6005542", 49482892, 8473050, "Mannheim", "Kunsthalle"),
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

	@Test
	public void shortTrip2() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "17002402", null, "Bahnhof"), null, new Location(
				LocationType.STATION, "17009001", null, "Bahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
