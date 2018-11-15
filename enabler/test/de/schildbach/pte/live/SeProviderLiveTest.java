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

import de.schildbach.pte.SeProvider;
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
public class SeProviderLiveTest extends AbstractProviderLiveTest {
    public SeProviderLiveTest() {
        super(new SeProvider(secretProperty("se.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(57709311, 11988459));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("740017515", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Airport");
        print(result);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Luleå");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "740098049")));
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Stockholm");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "740098000")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "740014867", null, "Luleå Airport");
        final Location to = new Location(LocationType.STATION, "740098000", null, "STOCKHOLM");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void shortStockholmTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "740098000", null, "STOCKHOLM");
        final Location to = new Location(LocationType.STATION, "740020101", "Stockholm", "Slussen T-bana");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void longTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "740098086", null, "KIRUNA");
        final Location to = new Location(LocationType.STATION, "740098000", null, "STOCKHOLM");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(59.3299775, 18.0576622)); // Stockholm Central
        final Location to = Location.coord(Point.fromDouble(59.3136500, 18.0620848)); // Stockholms södra
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
