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
import de.schildbach.pte.NewyorkProvider;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Stephane Berube
 */
public class NewyorkProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public NewyorkProviderLiveTest() {
        super(new NewyorkProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        queryNearbyStations(Location.coord(404246, -740021));
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_area:NJR:SA:CTP37953");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("NJB:SP:xxxxx", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocations("Airport");
    }
}
