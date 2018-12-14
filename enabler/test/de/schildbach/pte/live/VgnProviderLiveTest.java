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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.VgnProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VgnProviderLiveTest extends AbstractProviderLiveTest {
    public VgnProviderLiveTest() {
        super(new VgnProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "3000510"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(49455472, 11079655));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("3000510", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Kur");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result1 = suggestLocations("Dürrenhof");
        print(result1);
        assertThat(result1.getLocations(), hasItem(new Location(LocationType.STATION, "3000427")));

        final SuggestLocationsResult result2 = suggestLocations("Röthenbach");
        print(result2);
        assertThat(result2.getLocations(), hasItem(new Location(LocationType.STATION, "3001970")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "451", "Nürnberg", "Ostring"),
                null, new Location(LocationType.STATION, "510", "Nürnberg", "Hauptbahnhof"), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripToPOI() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(49527298, 10836204));
        final Location to = new Location(LocationType.POI,
                "poiID:246:9564000:1:Grundschule Grimmstr.:Nürnberg:Grundschule Grimmstr.:ANY:POI:4436708:678322:NAV4:VGN",
                Point.from1E6(49468692, 11125334), "Nürnberg", "Grundschule Grimmstr.");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripToAddress() throws Exception {
        final Location from = new Location(LocationType.STATION, "1756", "Nürnberg", "Saarbrückener Str.");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(49437392, 11094524), "Nürnberg",
                "Wodanstraße 25");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), false, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
