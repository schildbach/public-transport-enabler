/*
 * Copyright 2015-2016 the original author or authors.
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

import de.schildbach.pte.SpainProvider;
import de.schildbach.pte.dto.Point;

public class SpainProviderLiveTest extends AbstractNavitiaProviderLiveTest {

    public SpainProviderLiveTest() {
        super(new SpainProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        // Valencia
        nearbyStationsAddress(39473600, -371100);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        // Madrid
        nearbyStationsAddress(40410400, -3702400);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:E38:SP:2223");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:EX38:SP:2223");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:E38:SP:2223");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OWX:SP:6911");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Turia");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Benimaclet", "Nou d'Octubre");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Valencia Sud", "Faitanar");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
