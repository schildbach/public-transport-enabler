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

import de.schildbach.pte.BrazilProvider;
import de.schildbach.pte.dto.Point;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Torsten Grote
 */
public class BrazilProviderLiveTest extends AbstractNavitiaProviderLiveTest {
    public BrazilProviderLiveTest() {
        super(new BrazilProvider(secretProperty("navitia.authorization")));
    }

    @Test
    public void nearbyStationsAddress() throws Exception {
        // Sao Paulo
        nearbyStationsAddress(-23547900, -46635200);
    }

    @Test
    public void nearbyStationsAddress2() throws Exception {
        // Rio de Janeiro
        nearbyStationsAddress(-22905300, -43179500);
    }

    @Test
    public void nearbyStationsStation() throws Exception {
        nearbyStationsStation("stop_point:OIO:SP:18255914");
    }

    @Test
    public void nearbyStationsInvalidStation() throws Exception {
        nearbyStationsInvalidStation("stop_point:OIO:SPX:18255914");
    }

    @Test
    public void queryDeparturesEquivsFalse() throws Exception {
        queryDeparturesEquivsFalse("stop_point:OSA:SP:800016608");
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        queryDeparturesInvalidStation("stop_point:OWX:SP:6911");
    }

    @Test
    public void suggestLocations() throws Exception {
        suggestLocationsFromName("Republica");
    }

    @Test
    public void queryTripStations() throws Exception {
        queryTrip("Benjamim Constant", "Avenida Paulista");
    }

    @Test
    public void queryMoreTrips() throws Exception {
        queryMoreTrips("Republica", "Avenida Paulista");
    }

    @Test
    public void getArea() throws Exception {
        final Point[] polygon = provider.getArea();
        assertTrue(polygon.length > 0);
    }
}
