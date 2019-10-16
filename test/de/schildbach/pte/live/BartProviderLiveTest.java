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

import de.schildbach.pte.BartProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andreas Schildbach
 */
public class BartProviderLiveTest extends AbstractProviderLiveTest {
    public BartProviderLiveTest() {
        super(new BartProvider(secretProperty("bart.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(Point.fromDouble(37.7928550, -122.3968986)));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("100013295", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Airport");
        print(result);
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Embarcadero BART Station, San Francisco");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "100028458", null, "Airport Plaza, Concord");
        final Location to = new Location(LocationType.STATION, "100013295", null, "Embarcadero BART Station, San Francisco");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult later3Result = queryMoreTrips(later2Result.context, true);
        print(later3Result);
        final QueryTripsResult later4Result = queryMoreTrips(later3Result.context, true);
        print(later4Result);
        final QueryTripsResult later5Result = queryMoreTrips(later4Result.context, true);
        print(later5Result);
        final QueryTripsResult later6Result = queryMoreTrips(later5Result.context, true);
        print(later6Result);
        final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
        print(earlierResult);
        final QueryTripsResult earlier2Result = queryMoreTrips(earlierResult.context, false);
        print(earlier2Result);
        final QueryTripsResult earlier3Result = queryMoreTrips(earlier2Result.context, false);
        print(earlier3Result);
        final QueryTripsResult earlier4Result = queryMoreTrips(earlier3Result.context, false);
        print(earlier4Result);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(37.7927820, -122.3969430)); // Embarcadero BART Station, San Francisco
        final Location to = Location.coord(Point.fromDouble(37.9793260, -122.0541840)); // Airport Plaza, Concord
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
