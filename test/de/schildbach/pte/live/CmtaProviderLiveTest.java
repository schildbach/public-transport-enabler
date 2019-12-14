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

import java.util.Date;
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.CmtaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Colin Murphy
 */
public class CmtaProviderLiveTest extends AbstractProviderLiveTest {
    public CmtaProviderLiveTest() {
        super(new CmtaProvider(secretProperty("cmta.api_authorization")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "591"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(30275000, -97740000));
        print(result);
        assertThat(result.locations, hasItem(new Location(LocationType.STATION, "591")));
    }

    @Test
    public void nearbyPOIsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.POI),
                Location.coord(30275000, -97740000));
        print(result);
        assertThat(result.locations,
                hasItem(new Location(LocationType.POI,
                        "A=4@O=Texas State Capitol@X=-97740215@Y=30275103@u=0@U=130@L=9819105@",
                        Point.from1E6(30275103, -97740215), null, "Texas State Capitol")));
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("591", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", 0, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Capitol Station");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "591")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "591", null, "Capitol Station (NB)"), null,
                new Location(LocationType.STATION, "5940", null, "Lavaca/17th (Midblock)"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void addressTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, null, "1501 Colorado St"),
                null, new Location(LocationType.ADDRESS, null, null, "4299 Duval St"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
