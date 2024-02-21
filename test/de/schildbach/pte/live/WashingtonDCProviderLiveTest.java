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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.json.JSONException;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.WashingtonDCProvider;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Liam Norman, Jiovanny Ramirez, Miguel Lopez
 */
public class WashingtonDCProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public WashingtonDCProviderLiveTest() {
        super(new WashingtonDCProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        queryNearbyStations(Location.coord(388645, -770884));
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_area:OWD:Navitia:15281");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("stop_area:OWD:Navitia:xxxxx", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocations("Airport");
    }
}
