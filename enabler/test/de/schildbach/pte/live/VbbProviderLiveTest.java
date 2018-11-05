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

import de.schildbach.pte.VbbProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VbbProviderLiveTest extends AbstractProviderLiveTest {
    public VbbProviderLiveTest() {
        super(new VbbProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "9007102"));
        print(result);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "2449475"));
        assertEquals(NearbyLocationsResult.Status.INVALID_ID, result.status);
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
        Assert.assertThat(result.getLocations(), hasItem(new Location(LocationType.POI, "900980141")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Sophienstr. 24");
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
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz"), null,
                new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße"), new Date(), true, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);

        final QueryTripsResult earlierResult = queryMoreTrips(laterResult.context, false);
        print(earlierResult);
    }

    @Test
    public void shortFootwayTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, 52435193, 13473409, "12357 Berlin-Buckow", "Kernbeisserweg 4"),
                null,
                new Location(LocationType.ADDRESS, null, 52433989, 13474353, "12357 Berlin-Buckow", "Distelfinkweg 35"),
                new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void shortViaTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "900056102", "Berlin", "Nollendorfplatz"),
                new Location(LocationType.STATION, "900044202", "Berlin", "Bundesplatz"),
                new Location(LocationType.STATION, "900013103", "Berlin", "Prinzenstraße"), new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(52501507, 13357026), null,
                Location.coord(52513639, 13568648), new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void viaTripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(52501507, 13357026),
                Location.coord(52479868, 13324247), Location.coord(52513639, 13568648), new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, 52479663, 13324278, "10715 Berlin-Wilmersdorf",
                        "Weimarische Str. 7"),
                null, new Location(LocationType.ADDRESS, null, 52541536, 13421290, "10437 Berlin-Prenzlauer Berg",
                        "Göhrener Str. 5"),
                new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void viaTripBetweenAddresses() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, 52479663, 13324278, "10715 Berlin-Wilmersdorf",
                        "Weimarische Str. 7"),
                new Location(LocationType.ADDRESS, null, 52527872, 13381657, "10115 Berlin-Mitte",
                        "Hannoversche Str. 20"),
                new Location(LocationType.ADDRESS, null, 52526029, 13399878, "10178 Berlin-Mitte", "Sophienstr. 24"),
                new Date(), true, null);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
