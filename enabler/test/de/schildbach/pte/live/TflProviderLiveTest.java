/*
 * Copyright 2010-2014 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.TflProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.Trip;

/**
 * @author Andreas Schildbach
 */
public class TflProviderLiveTest extends AbstractProviderLiveTest
{
	public TflProviderLiveTest()
	{
		super(new TflProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "1000086"), 0, 0);

		print(result);
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
		final QueryDeparturesResult result = provider.queryDepartures("1000086", 0, false);

		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Kur");

		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1008730", null, "King & Queen Wharf"), null, new Location(
				LocationType.STATION, "1006433", null, "Edinburgh Court"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		System.out.println(earlierResult);
	}

	@Test
	public void postcodeTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "sw19 8ta"), null, new Location(LocationType.STATION,
				"1016019", 51655903, -397249, null, "Watford (Herts), Watford Town Centre"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
	}

	@Test
	public void tripItdMessageList() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 51446072, -239417, "Wandsworth",
				"Timsbury Walk, Wandsworth"), null, new Location(LocationType.STATION, "90046985", 53225140, -1472433, "Chesterfield (Derbys)",
				"Walton (Chesterfield), Netherfield Road (on Somersall Lane)"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		System.out.println(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		System.out.println(earlierResult);
	}
	
	
	@Test
	public void utcTime() throws Exception
	{
		Date date = new Date();
		
		QueryTripsResult tflResult = queryTrips(
				new Location(LocationType.ADDRESS, "0", (int) (51.5148683 * 1E6),
				(int) (-0.13981000000001131 * 1E6), "Hills Place, London, Greater London W1F, Vereinigtes K�nigreich",
				"Hills Place, London, Greater London W1F, Vereinigtes K�nigreich"), 
				null, 
				new Location(
				LocationType.ADDRESS, "0", (int) (51.49992710000001 * 1E6), (int) (-0.09083910000003925 * 1E6),
				"Sterry Street, London, Greater London SE1, Vereinigtes K�nigreich",
				"Sterry Street, London, Greater London SE1, Vereinigtes K�nigreich"), 
				date, true, null, null, null);
		
		Trip tflTrip = tflResult.trips.get(0);
		
//		System.out.println(date);
//		System.out.println(tflTrip.getFirstDepartureTime());
//		System.out.println(date.getTime() - tflTrip.getFirstDepartureTime().getTime());

		// start time difference should be a maximum of 1/2 hour
		long difference = date.getTime() - tflTrip.getFirstDepartureTime().getTime();
		Assert.assertTrue(difference <= 1500000 && difference >= -1500000);
	}
}
