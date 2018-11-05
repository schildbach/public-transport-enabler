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

import de.schildbach.pte.ShProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class ShProviderLiveTest extends AbstractProviderLiveTest {
    public ShProviderLiveTest() {
        super(new ShProvider(secretProperty("sh.api_authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8002547"));
        print(result);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "999999"));
        assertEquals(NearbyLocationsResult.Status.INVALID_ID, result.status);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(54325845, 10122920));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8002547", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Lübeck");
        print(result);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Achterüm");
        print(result);
    }

    @Test
    public void suggestLocationsWithoutCoordinatesInResult() throws Exception {
        final SuggestLocationsResult result = suggestLocations("aachen");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "8002547", null, "Flughafen Hamburg"), null,
                new Location(LocationType.STATION, "8003781", null, "Lübeck Airport"), new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripKiel() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3490015"), null,
                new Location(LocationType.STATION, "706923"), new Date(), true, null);
        print(result);
    }

    @Test
    public void tripKielVia() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3490015"),
                new Location(LocationType.STATION, "3490020"), new Location(LocationType.STATION, "706923"), new Date(),
                true, null);
        print(result);
    }

    @Test
    public void tripKielPoi() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "3490015"), null,
                new Location(LocationType.POI,
                        "A=4@O=Kiel, Hiroshimapark@X=10131697@Y=54324466@U=104@L=970001375@B=1@V=14.9,@p=1397713274@"),
                new Date(), true, null);
        print(result);
    }

    @Test
    public void trip_errorTooClose() throws Exception {
        final Location station = new Location(LocationType.STATION, "003665026");
        final QueryTripsResult result = queryTrips(station, null, station, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.TOO_CLOSE, result.status);
    }
}
