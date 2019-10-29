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

import de.schildbach.pte.OebbProvider;
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
public class OebbProviderLiveTest extends AbstractProviderLiveTest {
    public OebbProviderLiveTest() {
        super(new OebbProvider(secretProperty("oebb.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48200239, 16370773));
        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("902006", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertTrue(result.stationDepartures.size() > 0);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Wien");
        print(result);
        assertTrue(result.getLocations().size() > 0);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Obirhöhle");
        print(result);
    }

    @Test
    public void shortTripLinzWien() throws Exception {
        final Location from = new Location(LocationType.STATION, "1140101", null, "Linz");
        final Location to = new Location(LocationType.STATION, "1190100", null, "Wien");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void shortTripBregenzBezau() throws Exception {
        final Location from = new Location(LocationType.STATION, "1180207", null, "Bregenz");
        final Location to = new Location(LocationType.STATION, "1180204", null, "Bezau");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void slowTrip() throws Exception {
        final Location from = new Location(LocationType.ANY, null, null, "Ramsen Zoll!");
        final Location to = new Location(LocationType.ANY, null, null, "Azuga!");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripWithFootway() throws Exception {
        final Location from = new Location(LocationType.ANY, null, null, "Graz, Haselweg!");
        final Location to = new Location(LocationType.ANY, null, null, "Innsbruck, Gumppstraße 69!");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripWithFootway2() throws Exception {
        final Location from = new Location(LocationType.ANY, null, null, "Wien, Krottenbachstraße 110!");
        final Location to = new Location(LocationType.ADDRESS, null, null, "Wien, Meidlinger Hauptstraße 1!");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(48.1850101, 16.3778549)); // Wien Hauptbahnhof
        final Location to = Location.coord(Point.fromDouble(48.2902408, 14.2918619)); // Linz Hauptbahnhof
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
