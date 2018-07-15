/*
 * Copyright 2014-2015 the original author or authors.
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

import de.schildbach.pte.NzProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Torsten Grote
 */
public class NzProviderLiveTest extends AbstractNavitiaProviderLiveTest {

    public NzProviderLiveTest() {
        super(new NzProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        // Auckland
        nearbyStationsAddress(-36852200, 174763000);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        // Wellington
        nearbyStationsAddress(-41292500, 174777000);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        // Ghuznee Street at Cuba Street (Wellington)
        nearbyStationsStation("stop_point:OWT:SP:6909");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:OWX:SP:6909");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OWT:SP:6911");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OWX:SP:6911");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Cuba St");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Cuba Street at Weltec", "Petone");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Manners Street", "Lower Hutt");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
