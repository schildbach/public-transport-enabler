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

import de.schildbach.pte.FranceSouthWestProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Nicolas Derive
 */
public class FranceSouthWestProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public FranceSouthWestProviderLiveTest() {
        super(new FranceSouthWestProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(44826434, -557312);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        nearbyStationsAddress(44841225, -580036);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:STE:SP:OCETrainTER-87581538");
    }

    @Test
    public void nearbyStationsPoi() throws Exception {
        nearbyStationsPoi("poi:n849494949");
    }

    @Test
    public void nearbyStationsAny() throws Exception {
        nearbyStationsAny(44826434, -557312);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:OBO:SP:7");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OBO:SP:732");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OBO:SA:AEROG");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsTrue("stop_point:OBO:SP:732");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OBO:SP:999999");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("quinco");
    }

    @Test
    public void suggestLocationsFromAddress() throws Exception {
        suggestLocationsFromAddress("78 rue cam");
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("quinconcesadasdjkaskd");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        queryTrip("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
    }

    @Test
    public void queryTripAddressStation() throws Exception {
        queryTrip("98, rue Jean-Renaud Dandicolle Bordeaux", "Saint-Augustin");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Hôpital Pellegrin", "Avenue de l'Université");
    }

    @Test
    public void queryTripStations2() throws Exception {
        queryTrip("Pelletan", "Barrière de Pessac");
    }

    @Test
    public void queryTripStations3() throws Exception {
        queryTrip("Barrière de Pessac", "Hôpital Pellegrin");
    }

    @Test
    public void queryTripStationsRapidTransit() throws Exception {
        queryTrip("Gaviniès Bordeaux", "Saint-Augustin Bordeaux");
    }

    @Test
    public void queryTripNoSolution() throws Exception {
        queryTripNoSolution("Patinoire Mériadeck Bordeaux", "Mérignac Centre");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Patinoire Mériadeck Bordeaux");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Patinoire Mériadeck Bordeaux");
    }

    @Test
    public void queryTripSlowWalk() throws Exception {
        queryTripSlowWalk("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
    }

    @Test
    public void queryTripFastWalk() throws Exception {
        queryTripFastWalk("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
