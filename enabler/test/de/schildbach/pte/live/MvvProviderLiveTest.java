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
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.MvvProvider;
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
public class MvvProviderLiveTest extends AbstractProviderLiveTest {
    public MvvProviderLiveTest() {
        super(new MvvProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "350"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateMarienplatz() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                Location.coord(Point.fromDouble(48.1364360, 11.5776610)));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void nearbyLocationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION, LocationType.POI),
                Location.coord(48135232, 11560650));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDeparturesMarienplatz() throws Exception {
        final QueryDeparturesResult result1 = queryDepartures("2", false);
        assertEquals(QueryDeparturesResult.Status.OK, result1.status);
        print(result1);

        final QueryDeparturesResult result2 = queryDepartures("1000002", false);
        assertEquals(QueryDeparturesResult.Status.OK, result2.status);
        print(result2);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Starnberg, Agentur für Arbeit");
        print(result);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Marien");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Grüntal");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "1000619")));
    }

    @Test
    public void suggestLocationsFraunhofer() throws Exception {
        final SuggestLocationsResult result = suggestLocations("fraunhofer");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "1000150")));
    }

    @Test
    public void suggestLocationsHirschgarten() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Hirschgarten");
        print(result);
        assertEquals("München", result.getLocations().get(0).place);
    }

    @Test
    public void suggestLocationsOstbahnhof() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Ostbahnhof");
        print(result);
        assertEquals("München", result.getLocations().get(0).place);
    }

    @Test
    public void suggestLocationsMarienplatz() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Marienplatz");
        print(result);
        assertEquals("München", result.getLocations().get(0).place);
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Maximilianstr. 1");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:3239:1:9162000:9162000:Maximilianstraße:München:Maximilianstraße::Maximilianstraße:80539:ANY:DIVA_ADDRESS:4468763:826437:MVTT:MVV")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Maximilianstr.");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:3239::9162000:-1:Maximilianstraße:München:Maximilianstraße::Maximilianstraße: 80539 80538:ANY:DIVA_STREET:4469138:826553:MVTT:MVV")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "2", "München", "Marienplatz");
        final Location to = new Location(LocationType.STATION, "10", "München", "Pasing");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult earlierResult = queryMoreTrips(laterResult.context, false);
        print(earlierResult);
    }

    @Test
    public void longTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "1005530", Point.from1E6(48002924, 11340144),
                "Starnberg", "Agentur für Arbeit");
        final Location to = new Location(LocationType.STATION, null, null, "Ackermannstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(48165238, 11577473);
        final Location to = Location.coord(47987199, 11326532);
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinateAndStation() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(48238341, 11478230));
        final Location to = new Location(LocationType.ANY, null, null, "Ostbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:3239:1:9162000:9162000:Maximilianstraße:München:Maximilianstraße::Maximilianstraße:80539:ANY:DIVA_ADDRESS:4468763:826437:MVTT:MVV",
                null, "Maximilianstraße 1");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:753:4:9162000:1:Burggrafenstraße:München:Burggrafenstraße::Burggrafenstraße:81671:ANY:DIVA_SINGLEHOUSE:4471134:827570:MVTT:MVV",
                null, "Burggrafenstraße 4");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenStreets() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:3239::9162000:-1:Maximilianstraße:München:Maximilianstraße::Maximilianstraße: 80539 80538:ANY:DIVA_STREET:4469138:826553:MVTT:MVV",
                null, "Maximilianstraße");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:753::9162000:1:Burggrafenstraße:München:Burggrafenstraße::Burggrafenstraße: 81671:ANY:DIVA_STREET:4471150:827576:MVTT:MVV",
                null, "Burggrafenstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenStationAndAddress() throws Exception {
        final Location from = new Location(LocationType.STATION, "1220", null, "Josephsburg");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(48188018, 11574239), null,
                "München Frankfurter Ring 35");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripInvalidStation() throws Exception {
        final Location valid = new Location(LocationType.STATION, "2", "München", "Marienplatz");
        final Location invalid = new Location(LocationType.STATION, "99999", null, null);
        final QueryTripsResult result1 = queryTrips(valid, null, invalid, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.UNKNOWN_TO, result1.status);
        final QueryTripsResult result2 = queryTrips(invalid, null, valid, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result2.status);
    }
}
