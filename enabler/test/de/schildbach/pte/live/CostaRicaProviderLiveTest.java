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

import de.schildbach.pte.CostaRicaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Jaime Gutiérrez Alfaro
 */
public class CostaRicaProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public CostaRicaProviderLiveTest() {
        super(new CostaRicaProvider(null));
    }


    @Test
    public void nearbyStationsByCoordinate() throws Exception {
      // stations close to those coordinates (lat, lon)
      // no decimal point, has to include 6 decimal places
      // Rio Segundo's lat and lon
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(10003436, -84186128));
        print(result);
    }

    @Test
    public void nearbyStations() throws Exception {
        // Estación Atlántico
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "stop_point:4310018677"));
        print(result);
    }

    @Test
    public void queryTripStations() throws Exception {
        // two existing stops to define a trip
        queryTrip("Río Segundo", "Estación Heredia");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        // two existing addresses to define a trip
        queryTrip("Miraflores", "UCR");
    }


    @Test
    public void queryDeparturesStopPoint() throws Exception {
        // stop area of Estación Atlántico
        queryDeparturesStopArea("stop_point:4310018677");
    }


    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("stop_point:404477xxxx", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        // location: Alajuela
        final SuggestLocationsResult result = suggestLocations("Alaju");
        print(result);
    }

}
