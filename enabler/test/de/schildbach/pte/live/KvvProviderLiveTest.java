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

import de.schildbach.pte.KvvProvider;
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
public class KvvProviderLiveTest extends AbstractProviderLiveTest {
    public KvvProviderLiveTest() {
        super(new KvvProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "7000090"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(49008184, 8400736));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("7000090", false);
        print(result);
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
        final SuggestLocationsResult result = suggestLocations("Händelstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "7000044")));
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Karlsruhe, Bahnhofsplatz 2");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500000173:2:8212000:15:Bahnhofplatz:Karlsruhe:Bahnhofplatz::Bahnhofplatz:76137:ANY:DIVA_SINGLEHOUSE:935315:5725973:MRCV:b_w")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Karlsruhe, Bahnhofsplatz");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500000173::8212000:-1:Bahnhofplatz:Karlsruhe:Bahnhofplatz::Bahnhofplatz: 76137:ANY:DIVA_STREET:935120:5726009:MRCV:b_w")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "7000001", Point.from1E6(49009526, 8404914),
                "Karlsruhe", "Marktplatz");
        final Location to = new Location(LocationType.STATION, "7000002", Point.from1E6(49009393, 8408866), "Karlsruhe",
                "Kronenplatz (Kaiserstr.)");
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

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:1500000173:2:8212000:15:Bahnhofplatz:Karlsruhe:Bahnhofplatz::Bahnhofplatz:76137:ANY:DIVA_SINGLEHOUSE:935315:5725973:MRCV:b_w",
                null, "Bahnhofsplatz 2");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:1500001060:15:8212000:-1:Lorenzstraße:Karlsruhe:Lorenzstraße::Lorenzstraße:76135:ANY:DIVA_SINGLEHOUSE:933225:5724788:MRCV:b_w",
                null, "Lorenzstraße 15");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenStreets() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:1500000173::8212000:-1:Bahnhofplatz:Karlsruhe:Bahnhofplatz::Bahnhofplatz: 76137:ANY:DIVA_STREET:935120:5726009:MRCV:b_w",
                null, "Bahnhofsplatz");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:1500001060::8212000:-1:Lorenzstraße:Karlsruhe:Lorenzstraße::Lorenzstraße: 76135:ANY:DIVA_STREET:933225:5724788:MRCV:b_w",
                null, "Lorenzstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
