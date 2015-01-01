/*
 * Copyright 2014-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.PacaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Kjell Braden <afflux@pentabarf.de>
 */
public class PacaProviderLiveTest extends AbstractProviderLiveTest
{
	public PacaProviderLiveTest()
	{
		super(new PacaProvider());
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("aeroport");

		print(result);
	}

	@Test
	public void suggestLocationsIdentified() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("EGANAUDE, Biot");

		print(result);
	}

	@Test
	public void suggestLocationsUmlaut() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Aéroport");

		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "61088", null, "Eganaude"), null, new Location(
				LocationType.STATION, "58617", null, "Place de Gaulle"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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

		if (!later2Result.context.canQueryLater())
			return;

		final QueryTripsResult later3Result = queryMoreTrips(later2Result.context, true);
		print(later3Result);

		if (!later3Result.context.canQueryLater())
			return;

		final QueryTripsResult later4Result = queryMoreTrips(later3Result.context, true);
		print(later4Result);

		if (!later4Result.context.canQueryLater())
			return;

		final QueryTripsResult later5Result = queryMoreTrips(later4Result.context, true);
		print(later5Result);

		if (!later5Result.context.canQueryLater())
			return;

		final QueryTripsResult later6Result = queryMoreTrips(later5Result.context, true);
		print(later6Result);

		if (!result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
		print(earlierResult);

		if (!earlierResult.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier2Result = queryMoreTrips(earlierResult.context, false);
		print(earlier2Result);

		if (!earlier2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier3Result = queryMoreTrips(earlier2Result.context, false);
		print(earlier3Result);

		if (!earlier3Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlier4Result = queryMoreTrips(earlier3Result.context, false);
		print(earlier4Result);
	}

	@Test
	public void slowTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "68629", 43441167, 5223055, "MARSEILLE", "Aeroport Hall 3 4"),
				null, new Location(LocationType.STATION, "61088", 43623140, 7057545, "BIOT", "Eganaude"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.BARRIER_FREE);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void shortTripByName() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Biot, Templiers!"), null, new Location(
				LocationType.ANY, null, null, "Eganaude!"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
	}

	@Test
	public void slowTripPoi() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.POI, "30455", 0, 0, "CANNES", "Cannes"), null, new Location(
				LocationType.POI, "30514", 0, 0, "NICE", "Nice"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
}
