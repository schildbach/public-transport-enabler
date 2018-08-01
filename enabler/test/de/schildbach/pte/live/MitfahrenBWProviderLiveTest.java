/*
 * Copyright 2010-2018 the original author or authors.
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

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Every;
import org.junit.Ignore;
import org.junit.Test;

import de.schildbach.pte.MitfahrenBWProvider;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.Trip;

/**
 * @author Holger Bruch
 * @author Kavitha Ravi
 */
public class MitfahrenBWProviderLiveTest extends AbstractProviderLiveTest {
    public MitfahrenBWProviderLiveTest() {
        super(new MitfahrenBWProvider());
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kur");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Güttingen");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "nvbv:ch:23021:19520:90")));
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult result = suggestLocations("backnang");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "nvbv:de:08119:7600:2:2", 48942720, 9425946)));
    }

    @Test
    public void suggestLocations() throws IOException {
        SuggestLocationsResult suggestLocationResult = suggestLocations("Weinsberg");
        assertEquals(SuggestLocationsResult.Status.OK, suggestLocationResult.status);
        assertFalse(suggestLocationResult.suggestedLocations.isEmpty());
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(49146838, 9278756));
        assertFalse(result.locations.isEmpty());
    }

    @Test
    public void nearByLocations() throws IOException {
        NearbyLocationsResult nearbyLocationsResult = queryNearbyLocations(null, new Location(LocationType.COORD, null, 49146838, 9278756), 500, 5);
        assertEquals(NearbyLocationsResult.Status.OK, nearbyLocationsResult.status);
        assertFalse(nearbyLocationsResult.locations.isEmpty());
    }

    @Test @Ignore("nearbyStations request with station without coords not yet supported")
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "nvbv:de:08125:2003:3:1"));
        assertFalse(result.locations.isEmpty());
    }

    @Test
    public void queryDepartures() throws IOException {
    	QueryDeparturesResult queryDeparturesResult = queryDepartures("nvbv:de:08125:4344:1:1", 1, false);
    	assertEquals(QueryDeparturesResult.Status.OK, queryDeparturesResult.status);
        assertFalse(queryDeparturesResult.stationDepartures.isEmpty());
    }

    @Test
    public void queryTrips() throws IOException {
        QueryTripsResult queryTripsResult = queryTrips( new Location(LocationType.COORD, null, 49146838,9278756), null, new Location(LocationType.COORD, null, 49149566,9353452),
                new Date(), false, null, NetworkProvider.WalkSpeed.FAST, null);
    	assertEquals(QueryTripsResult.Status.OK, queryTripsResult.status);
        assertFalse(queryTripsResult.trips.isEmpty());
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "5006118", 48782984, 9179846, "Stuttgart",
                        "Stuttgart, Hauptbahnhof"),
                null, new Location(LocationType.STATION, "5006024", 48782584, 9187098, "Stuttgart", "Staatsgalerie"),
                new Date(), true, Product.ALL, NetworkProvider.WalkSpeed.NORMAL, NetworkProvider.Accessibility.NEUTRAL);
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
    public void shortTrip_onlyBike() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "5006118", 48782984, 9179846, "Stuttgart",
                        "Stuttgart, Hauptbahnhof"),
                null, new Location(LocationType.STATION, "5006024", 48782584, 9187098, "Stuttgart", "Staatsgalerie"),
                new Date(), true, Product.ALL, NetworkProvider.WalkSpeed.NORMAL, NetworkProvider.Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void shortTrip_onlySubway() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "5006118", 48782984, 9179846, "Stuttgart",
                        "Stuttgart, Hauptbahnhof"),
                null, new Location(LocationType.STATION, "5006024", 48782584, 9187098, "Stuttgart", "Staatsgalerie"),
                new Date(), true, Collections.singleton(Product.SUBWAY), NetworkProvider.WalkSpeed.NORMAL, NetworkProvider.Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);

        List<Trip.Leg> legs = result.trips.get(0).legs;
        for (Trip.Leg leg:legs) {
            if (leg instanceof Trip.Public) {
                assertEquals(Product.SUBWAY, ((Trip.Public) leg).line.product);
            }
        }

    }

}
