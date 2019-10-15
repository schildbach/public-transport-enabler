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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.TlemProvider;
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
public class TlemProviderLiveTest extends AbstractProviderLiveTest {
    public TlemProviderLiveTest() {
        super(new TlemProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(new Location(LocationType.STATION, "1001003"));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(new Location(LocationType.STATION, "1000086"));
        print(result2);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(51507161, -127144));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result1 = queryDepartures("1001003", false);
        print(result1);

        final QueryDeparturesResult result2 = queryDepartures("1000086", false);
        print(result2);
    }

    @Test
    public void queryDeparturesEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("1001003", true);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult resultLive = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultLive.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Lower Arncott The Plough");
        print(result);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Birming");
        print(result);
    }

    @Test
    public void shortTrip1() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "1008730", null, "King & Queen Wharf"), null,
                new Location(LocationType.STATION, "1006433", null, "Edinburgh Court"), new Date(), true, null);
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
        final Location from = new Location(LocationType.STATION, "2099014", Point.from1E6(52478184, -1898364),
                "Birmingham", "Birmingham New Street Rail Station");
        final Location to = new Location(LocationType.STATION, "2099150", Point.from1E6(52585468, -2122962),
                "Wolverhampton", "Wolverhampton Rail Station");
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
    public void tripArncott() throws Exception {
        final Location from = new Location(LocationType.STATION, "60011202", Point.from1E6(51850168, -1094302),
                "Upper Arncott", "Bullingdon Prison");
        final Location to = new Location(LocationType.STATION, "60006013", Point.from1E6(51856612, -1112904),
                "Lower Arncott", "The Plough E");
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
    public void tripFromPOI() throws Exception {
        final Location from = new Location(LocationType.POI,
                "poiID:48863:31117134:-1:Statue:Ham (London):Statue:ANY:POI:517246:826916:TFLV:uk",
                Point.from1E6(51444620, -314316), "Ham (London)", "Statue");
        final Location to = new Location(LocationType.ADDRESS, "streetID:106269::31117001:-1", "London",
                "Cannon Street, London");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripFromAddress() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, "streetID:203417::31117006:-1", "London",
                "Kings Cross, London");
        final Location to = new Location(LocationType.STATION, "1002070", Point.from1E6(51508530, 46706),
                "Royal Albert", "Royal Albert");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripPostcode() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "se7 7tr"), null,
                new Location(LocationType.ANY, null, null, "n9 0nx"), new Date(), true, null);
        print(result);
    }
}
