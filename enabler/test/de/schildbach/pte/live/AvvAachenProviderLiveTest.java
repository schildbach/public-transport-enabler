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

import org.junit.Test;

import de.schildbach.pte.AvvAachenProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class AvvAachenProviderLiveTest extends AbstractProviderLiveTest {
    public AvvAachenProviderLiveTest() {
        super(new AvvAachenProvider(secretProperty("avv_aachen.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(50767803, 6091504));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("1008", false);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Aachen Hbf");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "1008", "Aachen", "Hbf")));
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Gaßmühle");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "1576", "Aachen", "Gaßmühle")));
    }

    @Test
    public void suggestLocationsPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Suermondt-Ludwig-Museum");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.POI,
                "A=4@O=Aachen, Suermondt Ludwig Museum@X=6095720@Y=50773376@U=105@L=009900147@B=1@p=1515630730@")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Theaterstraße 49");
        print(result);
        assertEquals("Aachen", result.getLocations().get(0).place);
        assertEquals("Theaterstraße 49", result.getLocations().get(0).name);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "1008", "Aachen", "Hbf");
        final Location to = new Location(LocationType.STATION, "1016", "Aachen", "Schanz");
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
        final Location from = new Location(LocationType.STATION, "1008", "Aachen", "Hbf");
        final Location via = new Location(LocationType.STATION, "1341", "Aachen", "Kellerhausstraße");
        final Location to = new Location(LocationType.STATION, "3339", "Gulpen", "Busstation");
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenStations() throws Exception {
        final Location from = new Location(LocationType.STATION, "1008", "Aachen", "Hbf");
        final Location to = new Location(LocationType.STATION, "3339", "Gulpen", "Busstation");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(50767803, 6091504); // Aachen Hbf
        final Location to = Location.coord(50769870, 6073840); // Aachen, Schanz
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals("1008", result.from.id);
        assertEquals("1016", result.to.id);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, "Aachen", "Theaterstraße 49");
        final Location to = new Location(LocationType.ADDRESS, null, "Aachen", "Jakobstraße 109");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripWithoutId() throws Exception {
        final Location from = new Location(LocationType.STATION, null, "Aachen", "Hbf");
        final Location to = new Location(LocationType.STATION, null, "Aachen", "Schanz");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals("1008", result.from.id);
        assertEquals("1016", result.to.id);
    }
}
