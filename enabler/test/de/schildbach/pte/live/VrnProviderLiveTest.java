/*
 * Copyright the original author or authors.
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

import de.schildbach.pte.VrnProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VrnProviderLiveTest extends AbstractProviderLiveTest {
    public VrnProviderLiveTest() {
        super(new VrnProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(new Location(LocationType.STATION, "6032236"));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(new Location(LocationType.STATION, "17001301"));
        print(result2);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(Location.coord(49486561, 8477297));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(Location.coord(49757571, 6639147));
        print(result2);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result1 = queryDepartures("6032236", false);
        print(result1);

        final QueryDeparturesResult result2 = queryDepartures("17001301", false);
        print(result2);
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
        final SuggestLocationsResult result = suggestLocations("Käfertal");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "6005547")));
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bremen, KUR");
        print(result);
    }

    @Test
    public void suggestLocationsLocality() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bremen");
        print(result);
    }

    @Test
    public void suggestLocationsCity() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Mannheim");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "6002417", Point.from1E6(49479748, 8469938),
                "Mannheim", "Mannheim, Hauptbahnhof");
        final Location to = new Location(LocationType.STATION, "6005542", Point.from1E6(49482892, 8473050), "Mannheim",
                "Kunsthalle");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
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
    public void shortTrip2() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "17002402", null, "Bahnhof"),
                null, new Location(LocationType.STATION, "17009001", null, "Bahnhof"), new Date(), true, null);
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
    public void tripWithUmlaut() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Käfertal"), null,
                new Location(LocationType.STATION, "6002417", "Mannheim", "Hauptbahnhof"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        assertEquals(result.trips.get(0).from, new Location(LocationType.STATION, "6005547"));
    }
}
