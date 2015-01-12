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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.MvvProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
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
public class MvvProviderLiveTest extends AbstractProviderLiveTest
{
	public MvvProviderLiveTest()
	{
		super(new MvvProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = queryNearbyStations(new Location(LocationType.STATION, "350"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = queryNearbyStations(new Location(LocationType.ADDRESS, 48135232, 11560650));
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("2", false);
		assertEquals(QueryDeparturesResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocationsIdentified() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Starnberg, Agentur für Arbeit");
		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Marien");
		print(result);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("grün");
		print(result);
	}

	@Test
	public void suggestLocationsLocal() throws Exception
	{
		final SuggestLocationsResult fraunhoferStrResult = suggestLocations("fraunhofer");
		assertThat(fraunhoferStrResult.getLocations(), hasItem(new Location(LocationType.STATION, "1000150")));

		final SuggestLocationsResult hirschgartenResult = suggestLocations("Hirschgarten");
		assertEquals("München", hirschgartenResult.getLocations().get(0).place);

		final SuggestLocationsResult ostbahnhofResult = suggestLocations("Ostbahnhof");
		assertEquals("München", ostbahnhofResult.getLocations().get(0).place);

		final SuggestLocationsResult marienplatzResult = suggestLocations("Marienplatz");
		assertEquals("München", marienplatzResult.getLocations().get(0).place);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "2", "München", "Marienplatz"), null, new Location(
				LocationType.STATION, "10", "München", "Pasing"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
		final QueryTripsResult earlierResult = queryMoreTrips(laterResult.context, false);
		print(earlierResult);
	}

	@Test
	public void longTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1005530", 48002924, 11340144, "Starnberg",
				"Agentur für Arbeit"), null, new Location(LocationType.STATION, null, null, "Ackermannstraße"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
	}

	@Test
	public void tripBetweenCoordinates() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 48165238, 11577473), null, new Location(
				LocationType.ADDRESS, null, 47987199, 11326532), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripBetweenCoordinateAndStation() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 48238341, 11478230), null, new Location(LocationType.ANY,
				null, null, "Ostbahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripBetweenAddresses() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, null, "München, Maximilianstr. 1"), null, new Location(
				LocationType.ADDRESS, null, null, "Starnberg, Jahnstraße 50"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripBetweenStationAndAddress() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1220", null, "Josephsburg"), null, new Location(
				LocationType.ADDRESS, null, 48188018, 11574239, null, "München Frankfurter Ring 35"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void queryTripInvalidStation() throws Exception
	{
		final QueryTripsResult result1 = queryTrips(new Location(LocationType.STATION, "2", "München", "Marienplatz"), null, new Location(
				LocationType.STATION, "99999", 0, 0, null, null), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);

		assertEquals(QueryTripsResult.Status.UNKNOWN_TO, result1.status);

		final QueryTripsResult result2 = queryTrips(new Location(LocationType.STATION, "99999", 0, 0, null, null), null, new Location(
				LocationType.STATION, "2", "München", "Marienplatz"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);

		assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result2.status);
	}
}
