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

import de.schildbach.pte.NvbwProvider;
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
public class NvbwProviderLiveTest extends AbstractProviderLiveTest {
    public NvbwProviderLiveTest() {
        super(new NvbwProvider());
    }

    @Test
    public void nearbyStationsStuttgart() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "5006022")); // Schlossplatz
        print(result);
    }

    @Test
    public void nearbyStationsReutlingen() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "53019174")); // Reutlingen
        print(result);
    }

    @Test
    public void nearbyStationsFreiburg() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "6930112")); // Faulerstraße
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateStuttgart() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48778953, 9178963));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateReutlingen() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48493550, 9205656));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateFreiburg() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48000295, 7854338));
        print(result);
    }

    @Test
    public void queryDeparturesStuttgart() throws Exception {
        final QueryDeparturesResult result = queryDepartures("5006022", false); // Schlossplatz
        print(result);
    }

    @Test
    public void queryDeparturesReutlingen() throws Exception {
        final QueryDeparturesResult result = queryDepartures("53019174", false); // Reutlingen
        print(result);
    }

    @Test
    public void queryDeparturesKarlsruhe() throws Exception {
        final QueryDeparturesResult result = queryDepartures("7000211", false); // Messe
        print(result);
    }

    @Test
    public void queryDeparturesFreiburg() throws Exception {
        final QueryDeparturesResult result = queryDepartures("6930112", false); // Faulerstraße
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
        final SuggestLocationsResult result = suggestLocations("grünwink");
        print(result);
    }

    @Test
    public void suggestLocationsCoverageFreiburg() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Freiburg Hauptbahnhof");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "6906508")));
    }

    @Test
    public void suggestLocationsCoverageBasel() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Basel");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "51000007")));
    }

    @Test
    public void suggestLocationsCoverageKonstanz() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Konstanz");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "8706554")));
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Stuttgart, Kronenstraße 3");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500000599:3:8111000:51:Kronenstraße:Stuttgart:Kronenstraße::Kronenstraße:70173:ANY:DIVA_SINGLEHOUSE:1021956:5762095:MRCV:B_W")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Stuttgart, Kronenstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500000599::8111000:51:Kronenstraße:Stuttgart:Kronenstraße::Kronenstraße: 70174 70173:ANY:DIVA_STREET:1021539:5761790:MRCV:B_W")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "17002402", null, "Bahnhof");
        final Location to = new Location(LocationType.STATION, "17009001", null, "Bahnhof");
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
    public void shortTripReutlingen() throws Exception {
        final Location from = new Location(LocationType.STATION, "8029333", Point.from1E6(48492484, 9207456),
                "Reutlingen", "ZOB");
        final Location to = new Location(LocationType.STATION, "8029109", Point.from1E6(48496968, 9213320),
                "Reutlingen", "Bismarckstr.");
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
    public void shortTripFreiburg() throws Exception {
        final Location from = new Location(LocationType.STATION, "6930100", null, "Freiburg Bertoldsbrunnen");
        final Location to = new Location(LocationType.STATION, "6930101", null, "Freiburg Siegesdenkmal");
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
    public void trip() throws Exception {
        final Location from = new Location(LocationType.STATION, "6900037", Point.from1E6(48063184, 7779532),
                "Buchheim (Breisgau)", "Fortuna");
        final Location to = new Location(LocationType.STATION, "6906508", Point.from1E6(47996616, 7840450),
                "Freiburg im Breisgau", "Freiburg im Breisgau, Hauptbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void tripPforzheimToKarlsruhe() throws Exception {
        final Location from = new Location(LocationType.STATION, "7900050");
        final Location to = new Location(LocationType.STATION, "7000090");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:1500000484:11:8111000:-1:Wilhelmsplatz (Stgt):Stuttgart:Wilhelmsplatz (Stgt)::Wilhelmsplatz (Stgt):70182:ANY:DIVA_SINGLEHOUSE:1021706:5763896:MRCV:B_W",
                null, "Wilhelmsplatz 11");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:1500000599:3:8111000:51:Kronenstraße:Stuttgart:Kronenstraße::Kronenstraße:70173:ANY:DIVA_SINGLEHOUSE:1021956:5762095:MRCV:B_W",
                null, "Kronenstraße 3");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), false, null);
        print(result);
    }

    @Test
    public void tripBetweenStreets() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:1500000484::8111000:51:Wilhelmsplatz (Stgt):Stuttgart:Wilhelmsplatz (Stgt)::Wilhelmsplatz (Stgt): 70182:ANY:DIVA_STREET:1021828:5763870:MRCV:B_W",
                null, "Wilhelmsplatz");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:1500000599::8111000:51:Kronenstraße:Stuttgart:Kronenstraße::Kronenstraße: 70174 70173:ANY:DIVA_STREET:1021539:5761790:MRCV:B_W",
                null, "Kronenstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), false, null);
        print(result);
    }
}
