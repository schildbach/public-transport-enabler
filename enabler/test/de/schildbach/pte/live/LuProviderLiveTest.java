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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.LuProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class LuProviderLiveTest extends AbstractProviderLiveTest {
    public LuProviderLiveTest() {
        super(new LuProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "200501001"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(49610187, 6132746));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("200501001", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Aéroport");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "200416001", null, "Cité Aéroport"), null,
                new Location(LocationType.STATION, "200405035", "Luxembourg", "Gare Centrale"), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void addressTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, 49611610, 6130265, null, "Luxembourg, Rue Génistre 2"), null,
                new Location(LocationType.STATION, "200405035", "Luxembourg", "Gare Centrale"), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
