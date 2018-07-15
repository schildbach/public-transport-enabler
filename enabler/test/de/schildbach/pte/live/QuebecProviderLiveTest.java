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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import org.junit.Test;

import de.schildbach.pte.QuebecProvider;

/**
 * @author Stephane Berube
 */
public class QuebecProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public QuebecProviderLiveTest() {
        super(new QuebecProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_area:OML:SA:CTP3102842");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_area:OML:SA:CTPxxxxxxx");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OML:SA:CTP3102842");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocations("Airport");
    }
}
