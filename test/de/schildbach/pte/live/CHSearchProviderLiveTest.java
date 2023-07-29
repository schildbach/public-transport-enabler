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

import static org.junit.Assert.*;

import de.schildbach.pte.CHSearchProvider;
import de.schildbach.pte.dto.*;

import org.junit.Test;

import java.io.IOException;
import java.util.Date;


/**
 * @author Tobias Bossert
 */
public class CHSearchProviderLiveTest extends AbstractProviderLiveTest {

    private static final Date today = new Date();
    private static final Date daytimeDate = new Date(today.getYear(),
            today.getMonth(),
            today.getDay(),
            12,
            5,
            0
    );

    private static final Date nightTime = new Date(today.getYear(),
            today.getMonth(),
            today.getDay(),
            3,
            5,
            0
    );

    public CHSearchProviderLiveTest() {
        super(new CHSearchProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        //valid station
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8572547"));
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assert result.locations != null;
        assertTrue(result.locations.size() > 0);

        //invalid station
        final NearbyLocationsResult invalidResult = queryNearbyStations(new Location(LocationType.STATION, "99999999"));
        assertEquals(NearbyLocationsResult.Status.INVALID_ID, invalidResult.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        // normal filter with umlaut
        final SuggestLocationsResult result = suggestLocations("Höhle");
        assert result.suggestedLocations != null;
        assertTrue(result.suggestedLocations.size() > 0);

        //filter which (hopefully) never returns a result
        final SuggestLocationsResult noResult = suggestLocations("nco11onnoxapq1qspf11pfẍdv");
        assertEquals(0, noResult.suggestedLocations.size());

    }

    @Test
    public void stationBoard() throws IOException {
        // Valid query
        final QueryDeparturesResult result = queryDepartures("8503000", 0, false);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertTrue(result.stationDepartures.size() > 0);
        assertNotNull(result.stationDepartures.get(0));
        assertTrue(result.stationDepartures.get(0).departures.size() > 0);

        //Test if maxDepartures is honored (as of 29.7.23, this seems to be unstable sometimes..)
        final QueryDeparturesResult result2 = queryDepartures("8503000", 5, false);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertNotNull(result2.stationDepartures.get(0));
        assertEquals(5, result2.stationDepartures.get(0).departures.size());

        // Invalid query
        final QueryDeparturesResult invalidResult = queryDepartures("9999999", 0, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, invalidResult.status);
        assertEquals(0, invalidResult.stationDepartures.size());

    }

    //    @Test
//    // Address suggestions are not supported...
//    public void suggestLocationsAddress() throws Exception {
//        final SuggestLocationsResult result = suggestLocations("Dorfstrasse 10, Dällikon, Schweiz");
//        print(result);
//    }
//
    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8503000", null, "Zürich HB"),
                null, new Location(LocationType.STATION, "8507785", null, "Bern, Hauptbahnhof"), daytimeDate, true,
                null);
        assert result.trips != null;
        assertTrue(result.trips.size() > 0);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        assert laterResult.trips != null;
        assertTrue(laterResult.trips.size() > 0);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
    }

    @Test
    public void nightTrip() throws Exception {
        final QueryTripsResult resultNight = queryTrips(new Location(LocationType.STATION, "8503000", null, "Zürich HB"),
                null, new Location(LocationType.STATION, "8507785", null, "Bern, Hauptbahnhof"), nightTime, true,
                null);
        assert resultNight.trips != null;
        assertTrue(resultNight.trips.size() > 0);
        assertEquals(QueryTripsResult.Status.OK, resultNight.status);
        final QueryTripsResult laterNightResult = queryMoreTrips(resultNight.context, true);
        assert laterNightResult.trips != null;
        assertTrue(laterNightResult.trips.size() > 0);
        assertEquals(QueryTripsResult.Status.OK, laterNightResult.status);
    }

    @Test
    public void invalidTrip() throws Exception {
        //Invalid station (from)
        final QueryTripsResult invalidResult = queryTrips(new Location(LocationType.STATION, "999999999", null, "Zürich HB"),
                null, new Location(LocationType.STATION, "8507785", null, "Bern, Hauptbahnhof"), daytimeDate, true,
                null);
        assertEquals(QueryTripsResult.Status.NO_TRIPS, invalidResult.status);
        final QueryTripsResult invalidLater = queryMoreTrips(invalidResult.context, true);
        assertEquals(QueryTripsResult.Status.NO_TRIPS, invalidLater.status);
        final QueryTripsResult invalidEarlier = queryMoreTrips(invalidResult.context, false);
        assertEquals(QueryTripsResult.Status.NO_TRIPS, invalidEarlier.status);
    }

    @Test
    public void tripWithVia() throws Exception {

        final QueryTripsResult viaResult = queryTrips(
                new Location(LocationType.STATION, "8503000", null, "Zürich HB"),
                new Location(LocationType.STATION, "8500202", null, "Solothurn"),
                new Location(LocationType.STATION, "8507785", null, "Bern, Hauptbahnhof"),
                daytimeDate,
                true,
                null);
        assertEquals(QueryTripsResult.Status.OK, viaResult.status);
    }

    //
    @Test
    public void slowTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "8587210", null, "Schocherswil, Alte Post"), null,
                new Location(LocationType.STATION, "8592972", null, "Laconnex, Mollach"), daytimeDate, true, null);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        final QueryTripsResult beforeResult = queryMoreTrips(result.context, false);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
        assertEquals(QueryTripsResult.Status.OK, beforeResult.status);
    }

    //
    @Test
    public void tripWithFootway() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(46689354, 7683444), null,
                "Spiez, Seestraße 62");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(47133169, 8767425), null,
                "Einsiedeln, Erlenmoosweg 24");
        final QueryTripsResult result = queryTrips(from, null, to, daytimeDate, true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
    }

    @Test
    public void tripFromAddress() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(47438595, 8437369), null,
                "Dorfstrasse 10, Dällikon, Schweiz");
        final Location to = new Location(LocationType.STATION, "8500010", null, "Basel");
        final QueryTripsResult result = queryTrips(from, null, to, daytimeDate, true, null);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
    }
}
