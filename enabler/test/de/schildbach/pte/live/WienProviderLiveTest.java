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
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.WienProvider;
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
public class WienProviderLiveTest extends AbstractProviderLiveTest {
    public WienProviderLiveTest() {
        super(new WienProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "60203090"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48207355, 16370602));
        print(result);
    }

    @Test
    public void nearbyLocationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION, LocationType.POI),
                Location.coord(48207355, 16370602));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("60203090", false);
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
        final SuggestLocationsResult result = suggestLocations("Längenfeld");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "60200820")));
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult huetteldorfResult = suggestLocations("Wien Hütteldorf");
        print(huetteldorfResult);
        assertThat(huetteldorfResult.getLocations(), hasItem(new Location(LocationType.STATION, "60200560")));

        final SuggestLocationsResult wienerNeustadtResult = suggestLocations("Wiener Neustadt Nord");
        print(wienerNeustadtResult);
        assertThat(wienerNeustadtResult.getLocations(), hasItem(new Location(LocationType.STATION, "60205223")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "60200657", Point.from1E6(48200756, 16369001), "Wien",
                "Karlsplatz");
        final Location to = new Location(LocationType.STATION, "60201094", Point.from1E6(48198612, 16367719), "Wien",
                "Resselgasse");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
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

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(48180281, 16333551), null,
                Location.coord(48240452, 16444788), new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
