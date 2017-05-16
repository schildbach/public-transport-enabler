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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import de.schildbach.pte.AustraliaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.SuggestLocationsResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AustraliaProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public AustraliaProviderLiveTest() {
        super(new AustraliaProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void suggestLocationsInMelbourne() throws Exception {
        SuggestLocationsResult locations = suggestLocations("Camberwell Railway Station");
        assertEquals(SuggestLocationsResult.Status.OK, locations.status);
        assertTrue(locations.getLocations().size() > 0);

        boolean found = false;
        for (Location location : locations.getLocations()) {
            if ("stop_area:OMB:SA:CTP19853".equals(location.id)) {
                assertEquals("Camberwell Railway Station (Camberwell)", location.name);
                assertEquals(-37826567, location.lat);
                assertEquals(145058697, location.lon);
                found = true;
            }
        }

        assertTrue("Results should contain Camberwell Railway Station", found);
    }

    @Test
    public void suggestLocationsInSydney() throws Exception {
        SuggestLocationsResult locations = suggestLocations("Sydney Central Station");
        assertEquals(SuggestLocationsResult.Status.OK, locations.status);
        assertTrue(locations.getLocations().size() > 0);

        boolean found = false;
        for (Location location : locations.getLocations()) {
            if ("stop_area:OEY:SA:PST1100".equals(location.id)) {
                assertEquals("Sydney Central Station", location.name);
                assertEquals(-33884084, location.lat);
                assertEquals(151206292, location.lon);
                found = true;
            }
        }

        assertTrue("Results should contain Central Railway Station", found);
    }
}
