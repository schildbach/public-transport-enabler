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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.NvbwProvider;
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
public class NvbwProviderLiveTest extends AbstractProviderLiveTest {
    public NvbwProviderLiveTest() {
        super(new NvbwProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(new Location(LocationType.STATION, "6900001"));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(new Location(LocationType.STATION, "53019174"));
        print(result2);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(Location.coord(48778953, 9178963));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(Location.coord(48493550, 9205656));
        print(result2);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result1 = queryDepartures("6900001", false);
        print(result1);

        final QueryDeparturesResult result2 = queryDepartures("53019174", false);
        print(result2);
    }

    @Test
    public void queryDeparturesMesseKarlsruhe() throws Exception {
        final QueryDeparturesResult result = queryDepartures("7000211", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kur");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("grünwink");
        print(result);
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult freiburgResult = suggestLocations("Freiburg Hauptbahnhof");
        print(freiburgResult);
        assertThat(freiburgResult.getLocations(), hasItem(new Location(LocationType.STATION, "6906508")));

        final SuggestLocationsResult baselResult = suggestLocations("Basel");
        print(baselResult);
        assertThat(baselResult.getLocations(), hasItem(new Location(LocationType.STATION, "51000007")));

        final SuggestLocationsResult constanceResult = suggestLocations("Konstanz");
        print(constanceResult);
        assertThat(constanceResult.getLocations(), hasItem(new Location(LocationType.STATION, "8706554")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "17002402", null, "Bahnhof"),
                null, new Location(LocationType.STATION, "17009001", null, "Bahnhof"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
    public void shortTripReutlingen() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "8029333", 48492484, 9207456, "Reutlingen", "ZOB"), null,
                new Location(LocationType.STATION, "8029109", 48496968, 9213320, "Reutlingen", "Bismarckstr."),
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
    public void trip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "6900037", 48063184, 7779532, "Buchheim (Breisgau)", "Fortuna"),
                null,
                new Location(LocationType.STATION, "6906508", 47996616, 7840450, "Freiburg im Breisgau",
                        "Freiburg im Breisgau, Hauptbahnhof"),
                new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void tripPforzheimToKarlsruhe() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "7900050"), null,
                new Location(LocationType.STATION, "7000090"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }
}
