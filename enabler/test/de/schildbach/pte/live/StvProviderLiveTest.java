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
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.StvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class StvProviderLiveTest extends AbstractProviderLiveTest {
    public StvProviderLiveTest() {
        super(new StvProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "63203040"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(47072612, 15431814));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("63203040", false);
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
        final SuggestLocationsResult result = suggestLocations("grün");
        print(result);
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult grazResult = suggestLocations("Graz Brauhaus");
        print(grazResult);
        assertThat(grazResult.getLocations(), hasItem(new Location(LocationType.STATION, "63203044")));

        final SuggestLocationsResult leobenResult = suggestLocations("Leoben Blockhäuser");
        print(leobenResult);
        assertThat(leobenResult.getLocations(), hasItem(new Location(LocationType.STATION, "63206224")));

        final SuggestLocationsResult bruckResult = suggestLocations("Bruck Hauptplatz");
        print(bruckResult);
        assertThat(bruckResult.getLocations(), hasItem(new Location(LocationType.STATION, "63202063")));

        final SuggestLocationsResult kindbergResult = suggestLocations("Kindberg Friedhof");
        print(kindbergResult);
        assertThat(kindbergResult.getLocations(), hasItem(new Location(LocationType.STATION, "63208877")));

        final SuggestLocationsResult mariborResult = suggestLocations("Maribor Dravograjska Sokolska");
        print(mariborResult);
        assertThat(mariborResult.getLocations(), hasItem(new Location(LocationType.STATION, "63300136")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "63203040", null, "Graz Hauptbahnhof"), null,
                new Location(LocationType.STATION, "63203149", null, "Graz Babenbergerstraße"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);

        if (!laterResult.context.canQueryLater())
            return;

        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);

        if (!later2Result.context.canQueryEarlier())
            return;

        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
    }
}
