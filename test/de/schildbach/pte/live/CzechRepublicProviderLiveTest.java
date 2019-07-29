/*
 * Copyright 2019 the original author or authors.
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

import de.schildbach.pte.CzechRepublicProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Point;

/**
 * @author Filip Hejsek
 */
public class CzechRepublicProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public CzechRepublicProviderLiveTest() {
        super(new CzechRepublicProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        nearbyStationsAddress(50062956, 14430641);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        nearbyStationsAddress(50083373, 14423001);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OCZPRA:U527Z102P");
    }

    @Test
    public void nearbyStationsPoi() throws Exception {
        nearbyStationsPoi("poi:osm:way:141756627");
    }

    @Test
    public void nearbyStationsAny() throws Exception {
        nearbyStationsAny(50062956, 14430641);
    }

    @Test
    public void nearbyStationsDistance() throws Exception {
        nearbyStationsStationDistance("stop_point:OCZPRA:U527Z102P");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:RTP:SP:392");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OCZPRA:U1072Z121P");
    }

    @Test
    public void queryDeparturesStopArea() throws Exception {
        queryDeparturesStopArea("stop_area:OCZPRA:U527S1");
    }

    @Test
    public void queryDeparturesEquivsTrue() throws Exception {
        queryDeparturesEquivsTrue("stop_point:OCZPRA:U527Z101P");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:RTP:SP:999999");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Vyšehr");
    }

    @Test
    public void suggestLocationsFromAddress() throws Exception {
        suggestLocationsFromAddress("This doesn't work");
        // Address search doesn't work for Czech Republic in Navitia
    }

    @Test
    public void suggestLocationsNoLocation() throws Exception {
        suggestLocationsNoLocation("bellevilleadasdjkaskd");
    }

    @Test
    public void queryTripAddresses() throws Exception {
        queryTrip("This doesn't work", "This doesn't work");
        // Address search doesn't work for Czech Republic in Navitia
    }

    @Test
    public void queryTripAddressStation() throws Exception {
        queryTrip("This doesn't work", "Muzeum");
        // Address search doesn't work for Czech Republic in Navitia
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Vyšehrad", "Muzeum");
    }

    @Test
    public void queryTripStations2() throws Exception {
        queryTrip("Sídliště Písnice", "Modřanská rokle");
    }

    @Test
    public void queryTripStations3() throws Exception {
        queryTrip("Modřanská rokle", "Českomoravská");
    }

    @Test
    public void queryTripUnknownFrom() throws Exception {
        queryTripUnknownFrom("Muzeum");
    }

    @Test
    public void queryTripUnknownTo() throws Exception {
        queryTripUnknownTo("Vyšehrad");
    }

    @Test
    public void queryTripAmbiguousFrom() throws Exception {
        queryTripAmbiguousFrom(new Location(LocationType.ANY, null, null, "Sídliště"), "Muzeum");
    }

    @Test
    public void queryTripAmbiguousTo() throws Exception {
        queryTripAmbiguousTo("Vyšehrad", new Location(LocationType.ANY, null, null, "Sídliště"));
    }

    @Test
    public void queryTripSlowWalk() throws Exception {
        queryTripSlowWalk("Nemocnice Krč", "Budějovická");
    }

    @Test
    public void queryTripFastWalk() throws Exception {
        queryTripFastWalk("Nemocnice Krč", "Budějovická");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Nemocnice Krč", "Budějovická");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
