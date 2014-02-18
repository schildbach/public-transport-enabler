/*
 * Copyright 2014 Kjell Braden <afflux@pentabarf.de>
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
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.PacaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;

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
	public void autocomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("aeroport");

		print(autocompletes);
	}

	@Test
	public void autocompleteIdentified() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("EGANAUDE, Biot");

		print(autocompletes);
	}

	@Test
	public void autocompleteUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Aéroport");

		print(autocompletes);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 61088, null, "Eganaude"), null, new Location(
				LocationType.STATION, 58617, null, "Place de Gaulle"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 68629, 43441167, 5223055, "MARSEILLE", "Aeroport Hall 3 4"),
				null, new Location(LocationType.STATION, 61088, 43623140, 7057545, "BIOT", "Eganaude"), new Date(), true, Product.ALL,
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
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, 0, null, "Biot, Templiers!"), null, new Location(LocationType.ANY,
				0, null, "Eganaude!"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
	}

	@Test
	public void slowTripPoi() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.POI, 30455, 0, 0, "CANNES", "Cannes"), null, new Location(
				LocationType.POI, 30514, 0, 0, "NICE", "Nice"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}
}
