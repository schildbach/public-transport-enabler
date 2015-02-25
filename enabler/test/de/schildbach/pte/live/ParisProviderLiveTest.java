/*
 * Copyright 2014-2015 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.ParisProvider;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Antonio El Khoury
 */
public class ParisProviderLiveTest extends AbstractProviderLiveTest
{
	public ParisProviderLiveTest()
	{
		super(new ParisProvider(Secrets.NAVITIA_AUTHORIZATION));
	}

	@Test
	public void nearbyStationsAddress() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ADDRESS, 48877523,
				2378353), 700, 10);
		assertEquals(NearbyLocationsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void nearbyStationsAddress2() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ADDRESS, 48785420,
				2212050), 2000, 30);
		assertEquals(NearbyLocationsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void nearbyStationsStation() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.STATION,
				"stop_point:RTP:SP:3926410"), 700, 10);
		assertEquals(NearbyLocationsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void nearbyStationsPoi() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.POI, "poi:n668579722"),
				700, 10);
		assertEquals(NearbyLocationsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.STATION,
				"stop_point:RTP:SP:392"), 700, 10);
		assertEquals(NearbyLocationsResult.Status.INVALID_ID, result.status);
		print(result);
	}

	@Test
	public void queryDeparturesEquivsFalse() throws Exception
	{
		final int maxDepartures = 5;
		final QueryDeparturesResult result = queryDepartures("stop_point:RTP:SP:3926410", maxDepartures, false);
		assertEquals(QueryDeparturesResult.Status.OK, result.status);
		assertEquals(1, result.stationDepartures.size());
		assertTrue(result.stationDepartures.get(0).departures.size() <= maxDepartures);
		final List<LineDestination> lines = result.stationDepartures.get(0).lines;
		assertNotNull(lines);
		assertTrue(lines.size() >= 1);
		print(result);
	}

	@Test
	public void queryDeparturesStopArea() throws Exception
	{
		final int maxDepartures = 5;
		final QueryDeparturesResult result = queryDepartures("stop_area:RTP:SA:1958", maxDepartures, true);
		assertEquals(QueryDeparturesResult.Status.OK, result.status);
		assertTrue(result.stationDepartures.size() > 1);
		int nbDepartures = 0;
		int nbLines = 0;
		for (final StationDepartures stationDepartures : result.stationDepartures)
		{
			nbDepartures += stationDepartures.departures.size();
			final List<LineDestination> lines = stationDepartures.lines;
			if (lines != null)
				nbLines += lines.size();
		}
		assertTrue(nbDepartures <= maxDepartures);
		assertTrue(nbLines >= 2);
		print(result);
	}

	@Test
	public void queryDeparturesEquivsTrue() throws Exception
	{
		final int maxDepartures = 5;
		final QueryDeparturesResult result = queryDepartures("stop_point:RTP:SP:3926410", maxDepartures, true);
		assertEquals(QueryDeparturesResult.Status.OK, result.status);
		assertTrue(result.stationDepartures.size() > 1);
		int nbDepartures = 0;
		int nbLines = 0;
		for (StationDepartures stationDepartures : result.stationDepartures)
		{
			nbDepartures += stationDepartures.departures.size();
			final List<LineDestination> lines = stationDepartures.lines;
			assertNotNull(lines);
			nbLines += lines.size();
		}
		assertTrue(nbDepartures <= maxDepartures);
		assertTrue(nbLines >= 2);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("stop_point:RTP:SP:999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("bellevi");
		assertTrue(result.getLocations().size() > 0);
		print(result);
	}

	@Test
	public void suggestLocationsFromAddress() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("13 rue man");
		assertTrue(result.getLocations().size() > 0);
		print(result);
	}

	@Test
	public void suggestLocationsNoLocation() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("bellevilleadasdjkaskd");
		assertEquals(result.getLocations().size(), 0);
		print(result);
	}

	@Test
	public void queryTripAddresses() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, 48877095, 2378431), null, new Location(LocationType.ADDRESS,
				48847168, 2261272), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripAddressStation() throws Exception
	{
		final SuggestLocationsResult fromResult = suggestLocations("155 bd hopital paris");
		assertTrue(fromResult.getLocations().size() > 0);
		final SuggestLocationsResult toResult = suggestLocations("Gare St-Lazare");
		assertTrue(toResult.getLocations().size() > 0);

		final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null, toResult.getLocations().get(0), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripStations() throws Exception
	{
		final SuggestLocationsResult fromResult = suggestLocations("Campo Formio");
		assertTrue(fromResult.getLocations().size() > 0);
		final SuggestLocationsResult toResult = suggestLocations("Gare St-Lazare");
		assertTrue(toResult.getLocations().size() > 0);

		final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null, toResult.getLocations().get(0), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripStations2() throws Exception
	{
		final SuggestLocationsResult fromResult = suggestLocations("Tour Eiffel");
		assertTrue(fromResult.getLocations().size() > 0);
		final SuggestLocationsResult toResult = suggestLocations("Orsay Ville");
		assertTrue(toResult.getLocations().size() > 0);

		final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null, toResult.getLocations().get(0), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripStations3() throws Exception
	{
		final SuggestLocationsResult fromResult = suggestLocations("Tour Eiffel");
		assertTrue(fromResult.getLocations().size() > 0);
		final SuggestLocationsResult toResult = suggestLocations("Campo Formio");
		assertTrue(toResult.getLocations().size() > 0);

		final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null, toResult.getLocations().get(0), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripStationsRapidTransit() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "stop_area:RTP:SA:1866"), null, new Location(
				LocationType.STATION, "stop_area:RTP:SA:2045"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripNoSolution() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "stop_point:RTP:SP:3926410"), null, new Location(
				LocationType.STATION, "stop_point:RTP:SP:3926410"), new Date(), true, EnumSet.noneOf(Product.class), WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.NO_TRIPS, result.status);
		print(result);
	}

	@Test
	public void queryTripUnknownFrom() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "stop_area:RTP:SA:999999"), null, new Location(
				LocationType.STATION, "stop_area:RTP:SA:1666"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result.status);
		print(result);
	}

	@Test
	public void queryTripUnknownTo() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "stop_point:RTP:SP:3926410"), null, new Location(
				LocationType.STATION, "stop_area:RTP:SA:999999"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.UNKNOWN_TO, result.status);
		print(result);
	}

	@Test
	public void queryTripSlowWalk() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, 48877095, 2378431), null, new Location(LocationType.ADDRESS,
				48847168, 2261272), new Date(), true, Product.ALL, WalkSpeed.SLOW, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryTripFastWalk() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, 48877095, 2378431), null, new Location(LocationType.ADDRESS,
				48847168, 2261272), new Date(), true, Product.ALL, WalkSpeed.FAST, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void queryMoreTrips() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, 48877095, 2378431), null, new Location(LocationType.ADDRESS,
				48847168, 2261272), Calendar.getInstance().getTime(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		final QueryTripsContext context = result.context;

		final QueryTripsResult nextResult = queryMoreTrips(context, true);
		assertEquals(QueryTripsResult.Status.OK, nextResult.status);
		print(nextResult);

		final QueryTripsResult prevResult = queryMoreTrips(context, false);
		assertEquals(QueryTripsResult.Status.OK, prevResult.status);
		print(prevResult);
	}

	@Test
	public void getArea() throws Exception
	{
		final Point[] polygon = provider.getArea();
		assertTrue(polygon.length > 0);
	}

	@Test
	public void directionsSession() throws Exception
	{
		final SuggestLocationsResult suggestedDepartures = suggestLocations("13 rue man");
		assertTrue(suggestedDepartures.getLocations().size() > 0);
		final Location departure = suggestedDepartures.getLocations().get(0);

		final SuggestLocationsResult suggestedArrivals = suggestLocations("10 marcel dassault veli");
		assertTrue(suggestedArrivals.getLocations().size() > 0);
		final Location arrival = suggestedDepartures.getLocations().get(0);

		final QueryTripsResult result = queryTrips(departure, null, arrival, new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
	}
}
