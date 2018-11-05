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

import de.schildbach.pte.OoevvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class OoevvProviderLiveTest extends AbstractProviderLiveTest {
    public OoevvProviderLiveTest() {
        super(new OoevvProvider(secretProperty("ooevv.api_authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "490132007"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48207355, 16370602));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinateSalzburg() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(47809195, 13054919));
        print(result);
    }

    @Test
    public void queryDeparturesLinz() throws Exception {
        final QueryDeparturesResult result = queryDepartures("444116400", 0, false);
        print(result);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
    }

    @Test
    public void queryDeparturesSalzburg() throws Exception {
        final QueryDeparturesResult result = queryDepartures("455000200", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", 0, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Linz Hauptbahnhof");
        print(result);
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Mutterstraße 4, 6800 Feldkirch");
        print(result);
    }

    @Test
    public void suggestLocationsEncoding() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Wien Schönbrunn");
        assertEquals("Wien Schönbrunn", result.getLocations().get(0).name);
        print(result);
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult linzResult = suggestLocations("Linz Hauptbahnhof");
        print(linzResult);
        assertThat(linzResult.getLocations(), hasItem(new Location(LocationType.STATION, "444116400")));
    }

    @Test
    public void shortTripFeldkirch() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "480082200", null, "Feldkirch Katzenturm"), null,
                new Location(LocationType.STATION, "480081700", null, "Feldkirch Bahnhof"), new Date(), true, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);

        final QueryTripsResult laterResult2 = queryMoreTrips(laterResult.context, true);
        print(laterResult2);

        final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
        print(earlierResult);
    }

    @Test
    public void shortTripWien() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "490132000", null, "Wien Stephansplatz"), null,
                new Location(LocationType.STATION, "490024500", null, "Wien Stubentor"), new Date(), true, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);

        final QueryTripsResult laterResult2 = queryMoreTrips(laterResult.context, true);
        print(laterResult2);

        final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
        print(earlierResult);
    }

    @Test
    public void shortTripSalzburg() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "455000900", 47808976, 13056409, "Salzburg", "Vogelweiderstraße"),
                null, new Location(LocationType.STATION, "455084400", 47811556, 13050278, "Salzburg", "Merianstraße"),
                new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);

        if (!laterResult.context.canQueryLater())
            return;

        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);

        if (!later2Result.context.canQueryEarlier())
            return;

        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
    }

    @Test
    public void tripAddressToStation() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS,
                        "A=2@O=6800 Feldkirch, Kapfweg 6@X=9585539@Y=47239257@U=103@L=980092305@B=1@p=1437727591@",
                        "6800 Feldkirch", "Kapfweg 6"),
                null, new Location(LocationType.STATION, "480081700", null, "Feldkirch Bahnhof"), new Date(), true,
                null);
        print(result);
    }

    @Test
    public void tripCoordinateToStation() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(47238096, 9585581), null,
                new Location(LocationType.STATION, "480081700", null, "Feldkirch Bahnhof"), new Date(), true, null);
        print(result);
    }
}
