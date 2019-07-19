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
    public void suggestLocationsWithUmlautDuerrenhof() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Dürrenhof");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "3000427")));
    }

    @Test
    public void suggestLocationsWithUmlautRoethenbach() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Röthenbach");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "3001970")));
    }

    @Test
    public void suggestAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Nürnberg, Wodanstraße 25");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:2519:25:9564000:1:Wodanstraße:Nürnberg:Wodanstraße::Wodanstraße:90461:ANY:DIVA_SINGLEHOUSE:4434433:681777:NAV4:VGN")));
    }

    @Test
    public void suggestStreet() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Nürnberg, Wodanstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "streetID:2519::9564000:-1:Wodanstraße:Nürnberg:Wodanstraße::Wodanstraße: 90461:ANY:DIVA_STREET:4434565:681747:NAV4:VGN")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "451", "Nürnberg", "Ostring");
        final Location to = new Location(LocationType.STATION, "510", "Nürnberg", "Hauptbahnhof");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
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
    public void tripBetweenAddresses() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:832:2:9564000:1:Saarbrückener Straße:Nürnberg:Saarbrückener Straße::Saarbrückener Straße:90469:ANY:DIVA_SINGLEHOUSE:4433846:685282:NAV4:VGN",
                null, "Saarbrückener Straße 2");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:2519:25:9564000:1:Wodanstraße:Nürnberg:Wodanstraße::Wodanstraße:90461:ANY:DIVA_SINGLEHOUSE:4434433:681777:NAV4:VGN",
                null, "Wodanstraße 25");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), false, null);
        print(result);
    }

    @Test
    public void tripBetweenStreets() throws Exception {
        final Location from = new Location(LocationType.ADDRESS,
                "streetID:832::9564000:-1:Saarbrückener Straße:Nürnberg:Saarbrückener Straße::Saarbrückener Straße: 90469:ANY:DIVA_STREET:4433819:685855:NAV4:VGN",
                null, "Saarbrückener Straße");
        final Location to = new Location(LocationType.ADDRESS,
                "streetID:2519::9564000:-1:Wodanstraße:Nürnberg:Wodanstraße::Wodanstraße: 90461:ANY:DIVA_STREET:4434565:681747:NAV4:VGN",
                null, "Wodanstraße");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), false, null);
        print(result);
    }
}
