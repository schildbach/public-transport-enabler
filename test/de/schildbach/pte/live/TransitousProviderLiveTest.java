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

import de.schildbach.pte.TransitousProvider;
import de.schildbach.pte.dto.*;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Dan Cojocaru
 */
public class TransitousProviderLiveTest extends AbstractProviderLiveTest {
    public TransitousProviderLiveTest() {
        super(new TransitousProvider());
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(50767803, 6091504));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("de-DELFI_de:05315:11211:1:11", false);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("CCCAC");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.POI, "way/[775555697]")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Theaterstraße 49");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(
                LocationType.ADDRESS, 
                null, 
                Point.fromDouble(50.770897, 6.0919898), 
                "Aachen", 
                "Theaterstraße 49")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "de-DELFI_de:05334:1008", "Aachen", "Hbf");
        final Location to = new Location(LocationType.STATION, "de-DELFI_de:05334:1016", "Aachen", "Schanz");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
        final QueryTripsResult later3Result = queryMoreTrips(earlierResult.context, true);
        print(later3Result);
    }

    @Test
    public void shortViaTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "de-DELFI_de:05334:1008", "Aachen", "Hbf");
        final Location via = new Location(LocationType.STATION, "de-DELFI_de:05334:1341:2:2", "Aachen", "Kellershaustraße");
        final Location to = new Location(LocationType.STATION, "nl-OpenOV_3945548", "Gulpen", "Busstation");
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(50767803, 6091504); // Aachen Hbf
        final Location to = Location.coord(50769870, 6073840); // Aachen, Schanz
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
