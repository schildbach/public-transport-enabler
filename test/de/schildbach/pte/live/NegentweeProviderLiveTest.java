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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.NegentweeProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author full-duplex
 */
public class NegentweeProviderLiveTest extends AbstractProviderLiveTest {
    public NegentweeProviderLiveTest() {
        super(new NegentweeProvider(NegentweeProvider.Language.EN_GB));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                new Location(LocationType.STATION, "station-amsterdam-centraal"));
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);

        // Assert that queryNearbyStations only returns STATION locations
        assertNotNull(result.locations);
        assertTrue(result.locations.size() > 0);
        for (Location location : result.locations) {
            assertEquals(location.type, LocationType.STATION);
        }
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(52377548, 4901218));
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
    }

    @Test
    public void nearbyLocationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.ANY),
                Location.coord(52377548, 4901218), -1, 101);
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("station-amsterdam-centraal", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesWithEquivalents() throws Exception {
        final QueryDeparturesResult result = queryDepartures("station-amsterdam-centraal", true);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsComplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Amsterdam Centraal");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
    }

    @Test
    public void suggestLocationsStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Isolatorweg");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Isolatorweg 32");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Amsterdam");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Brüssel");
        print(result);
        assertEquals(SuggestLocationsResult.Status.OK, result.status);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "station-amsterdam-centraal", null, "Amsterdam Centraal"), null,
                new Location(LocationType.STATION, "station-amsterdam-zuid", null, "Amsterdam Zuid"), new Date(), true,
                null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void earlierTrip() throws Exception {
        final QueryTripsResult result1 = queryTrips(
                new Location(LocationType.STATION, "station-amsterdam-centraal", null, "Amsterdam Centraal"), null,
                new Location(LocationType.STATION, "station-rotterdam-centraal", null, "Rotterdam Centraal"),
                new Date(), true, null);
        print(result1);

        assertEquals(QueryTripsResult.Status.OK, result1.status);
        assertTrue(result1.context.canQueryLater());

        final QueryTripsResult result2 = queryMoreTrips(result1.context, false);
        print(result2);

        assertEquals(QueryTripsResult.Status.OK, result2.status);
    }

    @Test
    public void ambiguousTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Amsterdam Zuid"),
                new Location(LocationType.STATION, "station-amsterdam-centraal", null, "Amsterdam Centraal"),
                new Location(LocationType.ANY, null, null, "Rotterdam Centraal"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
    }

    @Test
    public void longTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, "amsterdam/prins-hendrikkade-80e", null, "Prins Hendrikkade"), null,
                new Location(LocationType.STATION, "breda/bushalte-cornelis-florisstraat", null,
                        "Cornelis Florisstraat"),
                new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void coordinatesTrip() throws Exception {
        final Location from = new Location(LocationType.COORD, null, Point.from1E6(51677273, 4437548));
        final Location via = new Location(LocationType.COORD, null, Point.from1E6(52162772, 4583171));
        final Location to = new Location(LocationType.COORD, null, Point.from1E6(53347140, 6720583));
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }
}
