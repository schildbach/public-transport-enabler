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

import de.schildbach.pte.AvvAugsburgProvider;
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
public class AvvAugsburgProviderLiveTest extends AbstractProviderLiveTest {
    public AvvAugsburgProviderLiveTest() {
        super(new AvvAugsburgProvider(secretProperty("avv_augsburg.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                Location.coord(Point.fromDouble(48.3652470, 10.8855950))); // Hbf
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8000013", false); // Hbf
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Augsburg");
        print(result);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kur");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlautBarfuesserbruecke() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Barfüßerbrücke");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "780110")));
    }

    @Test
    public void suggestLocationsWithUmlautGaertnerstrasse() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Gärtnerstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "780162")));
    }

    @Test
    public void suggestLocationsPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Fuggerei-Museum");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.POI,
                "A=4@O=Augsburg, Fuggerei-Museum (Kultur und Unterhaltung@X=10904796@Y=48369103@U=104@L=990379647@B=1@p=1410875982@")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Milchberg 6");
        print(result);
        assertEquals("Augsburg", result.getLocations().get(0).place);
        assertEquals("Milchberg 6", result.getLocations().get(0).name);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "781971"); // Königsplatz
        final Location to = new Location(LocationType.STATION, "8000013"); // Hbf
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
    }
}
