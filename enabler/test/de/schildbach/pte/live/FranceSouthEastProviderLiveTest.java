/*
 * Copyright 2016 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import de.schildbach.pte.FranceSouthEastProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Anthony Chaput
 */
public class FranceSouthEastProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public FranceSouthEastProviderLiveTest() {
        super(new FranceSouthEastProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(45185260, 5737800);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        nearbyStationsAddress(45184620, 5779780);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OGR:SP:2021");
    }

    @Test
    public void nearbyStationsPoi() throws Exception {
        nearbyStationsPoi("poi:n1245491811");
    }

    @Test
    public void nearbyStationsAny() throws Exception {
        nearbyStationsAny(45184630, 5779790);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:OGR:SP:S99999");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OGR:SP:2021");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OGR:SA:S3105");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsTrue("stop_point:OGR:SP:2021");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OBO:SP:999999");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("condil");
    }

    @Test
    public void suggestLocationsFromAddress() throws Exception {
        suggestLocationsFromAddress("360 rue des res");
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("quinconcesadasdjkaskd");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        queryTrip("360 rue des résidences", "2 rue Charles Michels");
    }

    @Test
    public void queryTripAddressStation() throws Exception {
        queryTrip("14 rue Barnave", "Louise Michel");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Victor Hugo", "Les Bauches");
    }

    @Test
    public void queryTripStations2() throws Exception {
        queryTrip("Chavant", "Louise Michel");
    }

    @Test
    public void queryTripStations3() throws Exception {
        queryTrip("Fontaine", "Vallier Libération");
    }

    @Test
    public void queryTripStationsRapidTransit() throws Exception {
        queryTrip("Alsace-Lorraine", "Vallier Libération");
    }

    @Test
    public void queryTripNoSolution() throws Exception {
        queryTripNoSolution("Robespierre", "Les Bauches");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Chavant");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Chavant");
    }

    @Test
    public void queryTripSlowWalk() throws Exception {
        queryTripSlowWalk("360 rue des résidences", "15 rue de la chimie");
    }

    @Test
    public void queryTripFastWalk() throws Exception {
        queryTripFastWalk("360 rue des résidences", "15 rue de la chimie");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("360 rue des résidences", "15 rue de la chimie");
    }

    @Test
    public void getArea() throws Exception {
        Point[] polygon = this.provider.getArea();
        Assert.assertTrue(polygon.length > 0);
    }
}
