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

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.NvvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Andreas Schildbach
 */
public class NvvProviderLiveTest extends AbstractProviderLiveTest {
    public NvvProviderLiveTest() {
        super(new NvvProvider(secretProperty("nvv.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(50108625, 8669604));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateKassel() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(51318447, 9496250));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("3000408", false);
        print(result);

        final QueryDeparturesResult result2 = queryDepartures("3000010", false);
        print(result2);

        final QueryDeparturesResult result3 = queryDepartures("3015989", false);
        print(result3);

        final QueryDeparturesResult result4 = queryDepartures("3000139", false);
        print(result4);
    }

    @Test
    public void queryDeparturesEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("3000010", true);
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
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kassel Wilhelmshöhe");
        print(result);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("könig");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "3000001", null, "Hauptwache");
        final Location to = new Location(LocationType.STATION, "3000912", null, "Südbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult later3Result = queryMoreTrips(later2Result.context, true);
        print(later3Result);
        final QueryTripsResult later4Result = queryMoreTrips(later3Result.context, true);
        print(later4Result);
        final QueryTripsResult later5Result = queryMoreTrips(later4Result.context, true);
        print(later5Result);
        final QueryTripsResult later6Result = queryMoreTrips(later5Result.context, true);
        print(later6Result);
        final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
        print(earlierResult);
        final QueryTripsResult earlier2Result = queryMoreTrips(earlierResult.context, false);
        print(earlier2Result);
        final QueryTripsResult earlier3Result = queryMoreTrips(earlier2Result.context, false);
        print(earlier3Result);
        final QueryTripsResult earlier4Result = queryMoreTrips(earlier3Result.context, false);
        print(earlier4Result);
    }

    @Test
    public void shortTripKassel() throws Exception {
        final Location from = new Location(LocationType.STATION, "2200007", null, "Kassel Wilhelmshöhe");
        final Location to = new Location(LocationType.STATION, "2200278", null, "Kassel Wilhelmshöher Weg");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void slowTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "3029079", Point.from1E6(50017679, 8229480), "Mainz",
                "An den Dünen");
        final Location to = new Location(LocationType.STATION, "3013508", Point.from1E6(50142890, 8895203), "Hanau",
                "Beethovenplatz");
        final TripOptions options = new TripOptions(Product.ALL, null, WalkSpeed.NORMAL, Accessibility.BARRIER_FREE,
                null);
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, options);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void shortTripByName() throws Exception {
        final Location from = new Location(LocationType.ANY, null, null, "Frankfurt Bockenheimer Warte!");
        final Location to = new Location(LocationType.ANY, null, null, "Frankfurt Hauptbahnhof!");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripUsingMuchBuffer() throws IOException {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(50119563, 8697044), null,
                "Hegelstrasse, 60316 Frankfurt am Main");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(50100364, 8615193), null,
                "Mainzer Landstrasse, Frankfurt");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(1378368840000L), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripUsingEvenMoreBuffer() throws IOException {
        final Location from = new Location(LocationType.STATION, "3000909", Point.from1E6(50094052, 8690923), null,
                "F Brauerei");
        final Location to = new Location(LocationType.STATION, "3001201", Point.from1E6(50119950, 8653924), null,
                "F Bockenheimer Warte");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(1378368840000L), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(51.3183386, 9.4896007)); // Kassel Hauptbahnhof
        final Location to = Location.coord(Point.fromDouble(51.3245728, 9.4521398)); // Kassel-Kirchditmold
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
