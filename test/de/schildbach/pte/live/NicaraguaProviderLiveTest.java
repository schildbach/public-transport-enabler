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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.schildbach.pte.NicaraguaProvider;
import de.schildbach.pte.dto.Point;

public class NicaraguaProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public NicaraguaProviderLiveTest() {
        super(new NicaraguaProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        // Managua
        nearbyStationsAddress(13090080, -86356250);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        // Esteli
        nearbyStationsAddress(12146120, -86274660);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:MNI:SP:node3230617621");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:MNIX:SP:node3230617621");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:MNI:SP:node3230617621");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:MNIX:SP:node3230617621");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Hospital");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("27 de Mayo", "San Miguel Arcángel");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Hospital", "Super Las Segovias");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
