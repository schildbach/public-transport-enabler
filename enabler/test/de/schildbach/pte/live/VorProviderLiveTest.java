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

import de.schildbach.pte.VorProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VorProviderLiveTest extends AbstractProviderLiveTest {
    public VorProviderLiveTest() {
        super(new VorProvider(secretProperty("vor.api_authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "490134900"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48207355, 16370602));
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("490134900", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", 0, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Wien Hauptbahnhof");
        print(result);
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
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "900018107")));
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult huetteldorfResult = suggestLocations("Wien Hütteldorf");
        print(huetteldorfResult);
        assertThat(huetteldorfResult.getLocations(), hasItem(new Location(LocationType.STATION, "490056000")));

        final SuggestLocationsResult wienerNeustadtResult = suggestLocations("Wiener Neustadt Nord");
        print(wienerNeustadtResult);
        assertThat(wienerNeustadtResult.getLocations(), hasItem(new Location(LocationType.STATION, "430522300")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "490065700", 48200852, 16368880, "Wien", "Karlsplatz"), null,
                new Location(LocationType.STATION, "490109400", 48198362, 16367667, "Wien", "Resselgasse"), new Date(),
                true, null);
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
    public void tripToPOI() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "490134900", 48185184, 16376413),
                null,
                new Location(LocationType.POI,
                        "A=4@O=Naschmarkt, Wien@X=16362903@Y=48198290@U=130@L=960068499@B=1@p=1476842541@", 48198290,
                        16362903, "Wien", "Naschmarkt"),
                new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
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
