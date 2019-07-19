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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.live;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.schildbach.pte.FinlandProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Adrian Perez de Castro <aperez@igalia.com>
 */
public class FinlandProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public FinlandProviderLiveTest() {
        super(new FinlandProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(60160920, 24941870);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OFI:SP:1050412");
    }

    @Test
    public void nearbyStationsPoi() throws Exception {
        nearbyStationsPoi("poi:osm:way:29071686");
    }

    @Test
    public void nearbyStationsAny() throws Exception {
        nearbyStationsAny(60160920, 24941870);
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:9999999999");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OFI:SA:1000201");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OFI:SP:1050412");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsTrue("stop_area:OFI:SA:1000201");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OFI:SP:999999");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("postitalo");
    }

    @Test
    public void suggestLocationsFromAddress() throws Exception {
        suggestLocationsFromAddress("10 yrjönkatu");
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("fontana di trevi blah blah");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        queryTrip("Yrjönkatu, 10, Helsinki", "Kolmas Linja, 5, Helsinki");
    }

    @Test
    public void queryTripAddressStation() throws Exception {
        queryTrip("Viides Linja, 3, Helsinki", "Kapylän asema");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Kapylän asema", "Päärautatieasema");
    }

    @Test
    public void queryTripNoSolution() throws Exception {
        queryTripNoSolution("Steissi, Helsinki", "Keskuskatu 1, Kuopio");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Rautatieasema");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Rautatieasema");
    }

    @Test
    public void queryTripSlowWalk() throws Exception {
        queryTripSlowWalk("Rautatieasema", "Postitalo");
    }

    @Test
    public void queryTripFastWalk() throws Exception {
        queryTripFastWalk("Rautatieasema", "Postitalo");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Steissi", "Töölöntori");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
