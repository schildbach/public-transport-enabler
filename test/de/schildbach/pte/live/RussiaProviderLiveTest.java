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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import de.schildbach.pte.RussiaProvider;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Anton Nesterov
 */
public class RussiaProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public RussiaProviderLiveTest() {
        super(new RussiaProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(
                new Location(LocationType.STATION, "stop_point:ORUSPB:SP:1290"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(60027851, 3022264));
        print(result);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("дорога на");
        print(result);
    }

   @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("ORUSPB:SP:1291", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("ORUSPB:SP:xxx", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
