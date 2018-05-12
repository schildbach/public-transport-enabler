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

import org.junit.Test;

import de.schildbach.pte.TexasProvider;
import de.schildbach.pte.dto.Location;

/**
 * @author Ojas Ahuja
 */

public class TexasProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public TexasProviderLiveTest() {
        super(new TexasProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OCM:SP:5567");
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        queryNearbyStations(Location.coord(302745403, -977361964));
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDepartures("stop_point:OCM:SP:xxxx", false);
    }

    @Test
    public void queryDepartures() throws Exception {
        queryDepartures("OCM:SP:5567", false);
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocations("Airport");
    }
}
