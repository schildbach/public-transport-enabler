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

import de.schildbach.pte.BvgProvider;
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
public class BvgProviderLiveTest extends AbstractProviderLiveTest {
    public BvgProviderLiveTest() {
        super(new BvgProvider(secretProperty("bvg.api_authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "900220302"));
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(52486400, 13350744));
        print(result);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "2449475"));
        assertEquals(NearbyLocationsResult.Status.INVALID_ID, result.status);
    }

    @Test
    public void queryDeparturesWilmsstrasse() throws Exception {
        final QueryDeparturesResult result = queryDepartures("900016254", false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
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
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Güntzelstr.");
        print(result);
        assertThat(result.getLocations(),
                hasItem(new Location(LocationType.STATION, "900043201", "Berlin", "U Güntzelstr.")));
    }

    @Test
    public void suggestLocationsLocality() throws Exception {
        final SuggestLocationsResult result = suggestLocations("seeling");
        print(result);
        assertEquals(new Location(LocationType.STATION, null, "Berlin", "Seelingstr."), result.getLocations().get(0));
    }

    @Test
    public void suggestLocationsPOI() throws Exception {
        final SuggestLocationsResult result = suggestLocations("schwules museum");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.POI,
                "A=4@O=Berlin, Schwules Museum@X=13357979@Y=52504519@U=104@L=900980141@B=1@p=1540465509@")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Berlin, Sophienstr. 24");
        print(result);
        assertEquals("Sophienstr. 24", result.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("nol");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz"), null,
                new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße"), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
        final QueryTripsResult later3Result = queryMoreTrips(earlierResult.context, true);
        print(later3Result);
    }

    @Test
    public void tripBetweenStations() throws Exception {
        final Location from = new Location(LocationType.STATION, "900055101", Point.from1E6(52496176, 13343273), null,
                "U Viktoria-Luise-Platz");
        final Location to = new Location(LocationType.STATION, "900089303", Point.from1E6(52588810, 13288699), null,
                "S Tegel");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void shortViaTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz"),
                new Location(LocationType.STATION, "900044202", "Berlin", "Bundesplatz"),
                new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße"), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(52501507, 13357026), null,
                Location.coord(52513639, 13568648), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinatesAndAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52536099, 13426309), null,
                "Christburger Straße 1, 10405 Berlin, Deutschland");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(52486400, 13350744), null,
                "Eisenacher Straße 70, 10823 Berlin, Deutschland");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void viaTripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(52501507, 13357026),
                Location.coord(52479868, 13324247), Location.coord(52513639, 13568648), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52479663, 13324278),
                "10715 Berlin-Wilmersdorf", "Weimarische Str. 7");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(52541536, 13421290),
                "10437 Berlin-Prenzlauer Berg", "Göhrener Str. 5");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
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
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripAddressWithoutId() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52481922, 13388383), null,
                "Bayernring, 12101 Berlin, Deutschland");
        final Location to = new Location(LocationType.STATION, "900064301", Point.from1E6(52429099, 13328081), null,
                "S Lichterfelde Ost Bhf");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
