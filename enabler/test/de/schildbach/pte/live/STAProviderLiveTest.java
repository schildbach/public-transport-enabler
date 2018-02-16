/*
 * Copyright 2010-2015 the original author or authors.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import org.junit.Test;

import java.util.Date;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.STAProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andreas Schildbach
 */
public class STAProviderLiveTest extends AbstractProviderLiveTest {
    public STAProviderLiveTest() {
        super(new STAProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "66000477"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(4649626, 1135875));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("60501720", false);
        print(result);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bolzano");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bürg");
        print(result);
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Sigmundskron");
        print(result);
    }

    @Test
    public void suggestLocationsCity() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bozen");
        print(result);
    }

    @Test
    public void incompleteTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "bozen"), null,
                new Location(LocationType.ANY, null, null, "meran"), new Date(), true, Product.ALL, WalkSpeed.FAST,
                Accessibility.NEUTRAL);
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Bozen Messe"),
                null, new Location(LocationType.ANY, null, null, "Bozen Bahnhof"), new Date(), true, Product.ALL,
                WalkSpeed.FAST, Accessibility.NEUTRAL);
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
    public void longTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, null, null, "Meran Bahnhof"),
                null, new Location(LocationType.STATION, null, null, "Bozen Bahnhof"), new Date(), true, Product.ALL,
                WalkSpeed.SLOW, Accessibility.NEUTRAL);
        print(result);
        // final QueryTripsResult laterResult = queryMoreTrips(provider, result.context, true);
        // print(laterResult);
    }
}
