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
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.schildbach.pte.AbstractHafasClientInterfaceProvider;
import de.schildbach.pte.VbbProvider;
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
public class VbbProviderLiveTest extends AbstractProviderLiveTest {
    public VbbProviderLiveTest() {
        super(new VbbProvider(secretProperty("vbb.api_authorization"), AbstractHafasClientInterfaceProvider
                .decryptSalt(secretProperty("vbb.encrypted_salt"), secretProperty("hci.salt_encryption_key"))));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(52548505, 13388640));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900007102", false);
        print(result);
    }

    @Test
    public void queryDeparturesAlexanderplatzBhf() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900100003", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesAlexanderplatzU2() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900100703", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesAlexanderplatzU5() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900100704", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesAlexanderplatzU8() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900100705", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900100003", true);
        print(result);
        assertTrue(result.stationDepartures.size() > 1);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult resultLive = queryDepartures("111111", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultLive.status);

        final QueryDeparturesResult resultPlan = queryDepartures("2449475", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultPlan.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Haubachstr.");
        print(result);
        Assert.assertEquals("Haubachstr.", result.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Güntzelstr.");
        print(result);
        Assert.assertEquals("U Güntzelstr.", result.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("schwules museum");
        print(result);
        Assert.assertThat(result.getLocations(), hasItem(new Location(LocationType.POI,
                "A=4@O=Berlin, Schwules Museum@X=13357979@Y=52504519@U=104@L=900980141@B=1@V=3.9,@p=1542286309@")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("10178 Berlin, Sophienstr. 24");
        print(result);
        Assert.assertEquals("Sophienstr. 24", result.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("nol");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz");
        final Location to = new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult earlierResult = queryMoreTrips(laterResult.context, false);
        print(earlierResult);
    }

    @Test
    public void shortFootwayTrip() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52435193, 13473409),
                "12357 Berlin-Buckow", "Kernbeisserweg 4");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(52433989, 13474353),
                "12357 Berlin-Buckow", "Distelfinkweg 35");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void shortViaTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz");
        final Location via = new Location(LocationType.STATION, "900044202", "Berlin", "Bundesplatz");
        final Location to = new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße");
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(52.5249451, 13.3696614)); // Berlin Hbf
        final Location to = Location.coord(Point.fromDouble(52.5071378, 13.3318680)); // S Zoologischer Garten
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void viaTripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(52.4999599, 13.3619411)); // U Kurfürsterstr.
        final Location via = Location.coord(Point.fromDouble(52.4778673, 13.3286942)); // S+U Bundesplatz
        final Location to = Location.coord(Point.fromDouble(52.5126122, 13.5752134)); // S+U Wuhletal
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52479663, 13324278),
                "10715 Berlin-Wilmersdorf", "Weimarische Str. 7");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(52541536, 13421290),
                "10437 Berlin-Prenzlauer Berg", "Göhrener Str. 5");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void viaTripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52479663, 13324278),
                "10715 Berlin-Wilmersdorf", "Weimarische Str. 7");
        final Location via = new Location(LocationType.ADDRESS, null, Point.from1E6(52527872, 13381657),
                "10115 Berlin-Mitte", "Hannoversche Str. 20");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(52526029, 13399878),
                "10178 Berlin-Mitte", "Sophienstr. 24");
        final QueryTripsResult result = queryTrips(from, via, to, new Date(), true, null);
        print(result);
    }
}
