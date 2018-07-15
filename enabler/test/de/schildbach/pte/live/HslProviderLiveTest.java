/*
 * Copyright 2015 the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.HslProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.Trip;

/**
 * @author Mats Sjöberg <mats@sjoberg.fi>
 */
public class HslProviderLiveTest extends AbstractProviderLiveTest {
    public HslProviderLiveTest() {
        super(new HslProvider(secretProperty("hsl.usertoken"), secretProperty("hsl.passphrase")));
    }

    @Test
    public void nearbyLocations() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(null, Location.coord(60174022, 24939222));
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(60174022, 24939222));
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("1030424", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertTrue(result.stationDepartures.size() > 0);
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("3029");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
        assertTrue(result.getLocations().size() == 1);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kum");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
        assertTrue(result.getLocations().size() > 0);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Jät");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
        assertTrue(result.getLocations().size() > 0);
    }

    private void assertTimesInSequence(List<Trip> trips) {
        for (int i = 1; i < trips.size(); i++) {
            Date start1 = trips.get(i - 1).getFirstDepartureTime();
            Date start2 = trips.get(i).getFirstDepartureTime();
            assertTrue(i + ": " + start1 + " vs " + start2, start1.compareTo(start2) <= 0);
        }
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, null, "", "Gustaf Hällströmin katu 1"), null,
                new Location(LocationType.STATION, null, "", "Tyynenmerenkatu 11"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        assertTimesInSequence(result.trips);

        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);

        assertTrue(result.context.canQueryLater());

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
        assertTrue(laterResult.trips.size() > result.trips.size());
        assertTimesInSequence(laterResult.trips);

        assertTrue(laterResult.context.canQueryLater());

        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        for (Trip trip : later2Result.trips) {
            System.out.println("LATER2 " + trip.getFirstDepartureTime() + " " + trip.getId());
        }
        assertEquals(QueryTripsResult.Status.OK, later2Result.status);
        assertTrue(later2Result.trips.size() > laterResult.trips.size());
        assertTimesInSequence(later2Result.trips);

        assertTrue(later2Result.context.canQueryEarlier());

        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
        for (Trip trip : earlierResult.trips) {
            System.out.println("EARLIER " + trip.getFirstDepartureTime() + " " + trip.getId());
        }
        assertEquals(QueryTripsResult.Status.OK, earlierResult.status);
        assertTrue(earlierResult.trips.size() > later2Result.trips.size());
        assertTimesInSequence(earlierResult.trips);
    }
}
