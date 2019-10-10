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

import de.schildbach.pte.BayernProvider;
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
public class BayernProviderLiveTest extends AbstractProviderLiveTest {
    public BayernProviderLiveTest() {
        super(new BayernProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "3001459"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                Location.coord(Point.fromDouble(48.1331686, 11.5580299))); // München, Beethovenplatz
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
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult munichMarienplatz = queryDepartures("91000002", false);
        print(munichMarienplatz);

        final QueryDeparturesResult munichHauptbahnhof = queryDepartures("91000006", false);
        print(munichHauptbahnhof);

        final QueryDeparturesResult nurembergHauptbahnhof = queryDepartures("80001020", false);
        print(nurembergHauptbahnhof);

        final QueryDeparturesResult augsburgAfrabruecke = queryDepartures("2000770", false);
        print(augsburgAfrabruecke);

        final QueryDeparturesResult ingolstadtHbf = queryDepartures("80000706", false);
        print(ingolstadtHbf);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Marien");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München Mühldorfstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "91000921")));
    }

    @Test
    public void suggestLocationsRegensburg() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Regensburg");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "80001083")));
    }

    @Test
    public void suggestLocationsMunich() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "91000100")));
    }

    @Test
    public void suggestLocationsNuernberg() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Nürnberg");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "80001020")));
    }

    @Test
    public void suggestPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Ruhpolding, Seehaus");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.POI,
                "poiID:40519640:9189140:-1:Seehaus:Ruhpolding:Seehaus:ANY:POI:1405062:5941100:MRCV:BAY")));
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Friedenstraße 2");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500001985:2:9162000:-1:Friedenstraße:München:Friedenstraße::Friedenstraße:81671:ANY:DIVA_SINGLEHOUSE:1291659:5872432:MRCV:BAY")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Friedenstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:1500001985::9162000:-1:Friedenstraße:München:Friedenstraße::Friedenstraße: 81671:ANY:DIVA_STREET:1292214:5871888:MRCV:BAY")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "91000005", "München", "Ostbahnhof");
        final Location to = new Location(LocationType.STATION, "91001796", "München", "Pasing");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void longTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "1005530", "Starnberg", "Arbeitsamt");
        final Location to = new Location(LocationType.STATION, "3001459", "Nürnberg", "Fallrohrstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(48165238, 11577473);
        final Location to = Location.coord(47987199, 11326532);
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenCoordinateAndStation() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(48238341, 11478230));
        final Location to = new Location(LocationType.STATION, "80000793", "München", "Ostbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, null, "München, Maximilianstr. 1");
        final Location to = new Location(LocationType.ADDRESS, null, null, "Starnberg, Jahnstraße 50");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenStationAndAddress() throws Exception {
        final Location from = new Location(LocationType.STATION, "1001220", null, "Josephsburg");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(48188018, 11574239), null,
                "München Frankfurter Ring 35");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenPOIs() throws Exception {
        final Location from = new Location(LocationType.POI,
                "poiID:40499046:9189140:-1:Seehaus:Ruhpolding:Seehaus:ANY:POI:1405062:5941100:MRCV:BAY", "Ruhpolding",
                "Seehaus");
        final Location to = new Location(LocationType.POI,
                "poiID:40215904:9189140:-1:Alpengasthof Laubau:Ruhpolding:Alpengasthof Laubau:ANY:POI:1409082:5938642:MRCV:BAY",
                "Ruhpolding", "Alpengasthof Laubau");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripRegensburg() throws Exception {
        final Location from = new Location(LocationType.STATION, "4014051", "Regensburg", "Klenzestraße");
        final Location to = new Location(LocationType.STATION, "4014080", "Regensburg", "Universität");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
