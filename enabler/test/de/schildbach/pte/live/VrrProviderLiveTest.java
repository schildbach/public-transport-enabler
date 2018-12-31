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

import de.schildbach.pte.VrrProvider;
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
public class VrrProviderLiveTest extends AbstractProviderLiveTest {
    public VrrProviderLiveTest() {
        super(new VrrProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "20019904"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateDuesseldorf() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                Location.coord(Point.fromDouble(51.2190163, 6.7757496)));
        print(result);
        assertThat(result.locations, hasItem(new Location(LocationType.STATION, "20018243"))); // Graf-Adolf-Platz
    }

    @Test
    public void nearbyStationsByCoordinatePaderborn() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                Location.coord(Point.fromDouble(51.7169873, 8.7537501)));
        print(result);
        assertThat(result.locations, hasItem(new Location(LocationType.STATION, "23207100"))); // Rathausplatz
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("1007258", false);
        print(result);

        final QueryDeparturesResult result2 = queryDepartures("20019904", false);
        print(result2);

        // Bonn
        queryDepartures("22000687", false); // Hauptbahnhof
        queryDepartures("22001374", false); // Suedwache
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void queryManyDeparturesWithEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("20018235", true);
        print(result);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kur");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Köln Mülheim");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "22000572")));
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Düsseldorf, Am Frohnhof");
        print(result);
    }

    @Test
    public void suggestLocationsCologne() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Köln Ebertplatz");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "22000035")));
    }

    @Test
    public void suggestLocationsDortmund() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Dortmund Zugstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "20000524")));
    }

    @Test
    public void suggestLocationsDuesseldorf() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Düsseldorf Sternstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "20018017")));
    }

    @Test
    public void suggestLocationsMuenster() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Münster Vennheideweg");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "24047291")));
    }

    @Test
    public void suggestLocationsAachen() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Aachen Elisenbrunnen");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "21001029")));
    }

    @Test
    public void suggestLocationsPaderborn() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Paderborn Hbf");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "23207000")));
    }

    @Test
    public void suggestLocationsCity() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Düsseldorf");
        print(result);
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Hagen, Siegstraße 30");
        print(result);
        assertThat(result.getLocations(),
                hasItem(new Location(LocationType.ADDRESS, "streetID:1500000683:30:5914000:-1")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Hagen, Siegstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500000683::5914000:-1:Siegstraße:Hagen:Siegstraße::Siegstraße: 58097:ANY:DIVA_STREET:831366:5312904:MRCV:nrw")));
    }

    @Test
    public void anyTrip() throws Exception {
        final Location from = new Location(LocationType.ANY, null, null, "Köln");
        final Location to = new Location(LocationType.ANY, null, null, "Bonn");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
    }

    @Test
    public void shortTripEssen() throws Exception {
        final Location from = new Location(LocationType.STATION, "20009289", "Essen", "Hauptbahnhof");
        final Location to = new Location(LocationType.STATION, "20009161", "Essen", "Bismarckplatz");
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
    public void shortTripPaderborn() throws Exception {
        final Location from = new Location(LocationType.STATION, "23207000"); // Paderborn Hbf
        final Location to = new Location(LocationType.STATION, "23207700"); // Höxter, Bahnhof / Rathaus
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
    public void shortTripDorsten() throws Exception {
        final Location from = new Location(LocationType.STATION, "20009643", "Bottrop", "West S");
        final Location to = new Location(LocationType.STATION, "20003214", "Dorsten", "ZOB Dorsten");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, "streetID:1500000683:30:5914000:-1", null,
                "Siegstraße 30");
        final Location to = new Location(LocationType.ADDRESS, "streetID:1500000146:1:5914000:-1", null,
                "Berliner Platz 1");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenStreets() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:1500000683::5914000:-1:Siegstraße:Hagen:Siegstraße::Siegstraße: 58097:ANY:DIVA_STREET:831366:5312904:MRCV:nrw",
                null, "Siegstraße");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:1500000146::5914000:29:Berliner Platz:Hagen:Berliner Platz::Berliner Platz: 58089:ANY:DIVA_STREET:830589:5314386:MRCV:nrw",
                null, "Berliner Platz");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
