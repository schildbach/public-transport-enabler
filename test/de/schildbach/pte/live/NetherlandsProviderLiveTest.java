/*
 * Copyright 2015 the original author or authors.
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

import de.schildbach.pte.NetherlandsProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Guus Hoekman
 */
public class NetherlandsProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public NetherlandsProviderLiveTest() {
        super(new NetherlandsProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(52080225, 4323195);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        nearbyStationsAddress(52069885, 4320715);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OAM:2323791");
    }

    @Test
    public void nearbyStationsPoi() throws Exception {
        nearbyStationsPoi("poi:n457396814");
    }

    @Test
    public void nearbyStationsAny() throws Exception {
        nearbyStationsAny(52080225, 4323195);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:OAM:23237945");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OAM:2323791");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OAM:stoparea:390116");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OAM:2323791");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OAM:99999999");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("rotterdam");
    }

    @Test
    public void suggestLocationsFromAddress() throws Exception {
        suggestLocationsFromAddress("Drakenburgstraat 4");
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("rotasodfasdfnasd");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        queryTrip("Drakenburgstraat 4 Utrecht", "Helling 7 Utrecht");
    }

    @Test
    public void queryTripAddressStation() throws Exception {
        queryTrip("Drakenburgstraat 4 Utrecht", "Utrecht Centraal");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Amsterdam Centraal", "Utrecht Centraal");
    }

    @Test
    public void queryTripStations2() throws Exception {
        queryTrip("Den Haag HS", "Leiden Centraal");
    }

    @Test
    public void queryTripStations3() throws Exception {
        queryTrip("Woerden", "Hoorn");
    }

    @Test
    public void queryTripStationsRapidTransit() throws Exception {
        queryTrip("Amsterdam, De Pijp", "Amsterdam, Noorderpark");
    }

    @Test
    public void queryTripNoSolution() throws Exception {
        queryTripNoSolution("Den Haag, Oostinje", "Den Haag, Juliana van Stolberglaan");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Arnhem Centraal");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Arnhem Centraal");
    }

    @Test
    public void queryTripSlowWalk() throws Exception {
        queryTripSlowWalk("Lange Poten 10 Den Haag", "Rijnstraat 8 Den Haag");
    }

    @Test
    public void queryTripFastWalk() throws Exception {
        queryTripSlowWalk("Lange Poten 10 Den Haag", "Rijnstraat 8 Den Haag");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Coolsingel 40 Rotterdam", "Grotekerkplein 15 Rotterdam");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
