/*
 * Copyright 2017 the original author or authors.
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

import de.schildbach.pte.GhanaProvider;
import de.schildbach.pte.dto.Point;

public class GhanaProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public GhanaProviderLiveTest() {
        super(new GhanaProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(5553473, -190438);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_area:SA4980974759");
    }

    @Test
    public void nearbyStationsDistance() throws Exception {
        nearbyStationsStationDistance("stop_area:SA5036738888");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_area:SC5031328601");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:5005030178");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:SA5031328607");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsTrue("stop_area:SA5031328607");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:RTP:5005030178");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("mark");
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("bellevilleadasdjkaskd");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("College", "Market");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Market");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Market");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("College", "Market");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
