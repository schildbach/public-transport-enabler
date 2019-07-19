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

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.ZvvProvider;
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
public class ZvvProviderLiveTest extends AbstractProviderLiveTest {
    public ZvvProviderLiveTest() {
        super(new ZvvProvider(secretProperty("zvv.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(47378968, 8540534));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8503000", false); // Hauptbahnhof
        print(result);
    }

    @Test
    public void queryDeparturesSuburbanTrain() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8500169", false); // Muriaux
        print(result);
    }

    @Test
    public void queryDeparturesTram() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8591276", false); // Milchbuck
        print(result);
    }

    @Test
    public void queryDeparturesTrolley() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8591177", false); // Hardplatz
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Flughafen");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "8503000", "Zürich", "Hauptbahnhof");
        final Location to = new Location(LocationType.STATION, "8507785", "Bern", "Hauptbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void trip() throws Exception {
        final Location from = new Location(LocationType.STATION, "8503000", Point.from1E6(47378491, 8537945), "Zürich",
                "Zürich, Hauptbahnhof");
        final Location to = new Location(LocationType.STATION, "8530812", Point.from1E6(47361762, 8560715), "Zürich",
                "Hegibachplatz");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(47.3782535, 8.5392280)); // Zürich Hauptbahnhof
        final Location to = Location.coord(Point.fromDouble(47.3852910, 8.5172170)); // Bahnhof Hardbrücke
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
