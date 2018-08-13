/*
 * Copyright 2018 Erik Uhlmann.
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

import org.junit.Test;

import de.schildbach.pte.MassachusettsProvider;

/**
 * @author Erik Uhlmann
 */
public class MassachusettsProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public MassachusettsProviderLiveTest() {
        super(new MassachusettsProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        nearbyStationsStation("stop_point:OUB:SP:70243");
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        nearbyStationsAny(42353187, -71067045);
    }

    @Test
    public void queryDepartures() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OUB:SP:70198");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OUB:SP:xxxx");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Airport");
    }
}
