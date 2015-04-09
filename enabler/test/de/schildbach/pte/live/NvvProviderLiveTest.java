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

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.NvvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class NvvProviderLiveTest extends AbstractProviderLiveTest
{
	public NvvProviderLiveTest()
	{
		super(new NvvProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "3000001"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(Location.coord(50108625, 8669604));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinateKassel() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(Location.coord(51318447, 9496250));
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("3000408", false);
		print(result);

		final QueryDeparturesResult result2 = queryDepartures("3000010", false);
		print(result2);

		final QueryDeparturesResult result3 = queryDepartures("3015989", false);
		print(result3);

		final QueryDeparturesResult result4 = queryDepartures("3000139", false);
		print(result4);
	}

	@Test
	public void queryDeparturesEquivs() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("3000010", true);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Flughafen");
		print(result);
	}

	@Test
	public void suggestLocationsIdentified() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Kassel Wilhelmshöhe");
		print(result);
	}

	@Test
	public void suggestLocationsUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("könig");
		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3000001", null, "Hauptwache"), null, new Location(
				LocationType.STATION, "3000912", null, "Südbahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
	public void shortTripKassel() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "2200007", null, "Kassel Wilhelmshöhe"), null, new Location(
				LocationType.STATION, "2200278", null, "Kassel Wilhelmshöher Weg"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void slowTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3029079", 50017679, 8229480, "Mainz", "An den Dünen"), null,
				new Location(LocationType.STATION, "3013508", 50142890, 8895203, "Hanau", "Beethovenplatz"), new Date(), true, Product.ALL,
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
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Frankfurt Bockenheimer Warte!"), null, new Location(
				LocationType.ANY, null, null, "Frankfurt Hauptbahnhof!"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
	}

	@Test
	public void tripUsingMuchBuffer() throws IOException
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 50119563, 8697044, null,
				"Hegelstrasse, 60316 Frankfurt am Main"), null, new Location(LocationType.ADDRESS, null, 50100364, 8615193, null,
				"Mainzer Landstrasse, Frankfurt"), new Date(1378368840000l), true, Product.ALL, null, null);
		print(result);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripUsingEvenMoreBuffer() throws IOException
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3000909", 50094052, 8690923, null, "F Brauerei"), null,
				new Location(LocationType.STATION, "3001201", 50119950, 8653924, null, "F Bockenheimer Warte"), new Date(1378368840000l), true,
				Product.ALL, null, null);
		print(result);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
}
