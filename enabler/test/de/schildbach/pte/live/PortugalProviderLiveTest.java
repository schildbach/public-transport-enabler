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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import org.junit.Test;

import de.schildbach.pte.PortugalProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.SuggestLocationsResult;


public class PortugalProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public PortugalProviderLiveTest() {
        super(new PortugalProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        // Lisbon
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(38713576, -9122704));
        print(result);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        // Lisbon
        nearbyStationsInvalidStation("stop_area:LME:SA:CTPM46");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        // Lisbon
        nearbyStationsInvalidStation("stop_area:LME:SAP:CTPM46");
    }

    @Test
    public void queryDepartures() throws Exception {
        // Lisbon
        queryDeparturesStopArea("stop_area:LME:SA:CTPM46");
    }

    @Test
    public void suggestLocations() throws Exception
    {
        suggestLocations("Airport");
    }
}
