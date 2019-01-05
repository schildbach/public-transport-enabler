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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NasaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.util.Iso8601Format;

/**
 * @author Andreas Schildbach
 */
public class NasaProviderLiveTest extends AbstractProviderLiveTest {
    public NasaProviderLiveTest() {
        super(new NasaProvider(secretProperty("nasa.api_authorization")));
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "13000"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(51346546, 12383333));

        print(result);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("13000", false);
        print(result);
    }

    @Test
    public void queryDeparturesEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("13000", true);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Flughafen");
        print(result);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Höhle");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "11063", null, "Leipzig, Johannisplatz"), null,
                new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void anotherShortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "8010205", Point.from1E6(51346546, 12383333), null,
                "Leipzig Hbf");
        final Location to = new Location(LocationType.STATION, "8012183", Point.from1E6(51423340, 12223423), null,
                "Leipzig/Halle Flughafen");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void outdatedTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "13002", null, "Leipzig, Augustusplatz"), null,
                new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"),
                Iso8601Format.newDateFormat().parse("2011-01-01"), true, null);
        assertEquals(QueryTripsResult.Status.INVALID_DATE, result.status);
    }

    @Test
    public void ambiguousTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Platz"), null,
                new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
        print(result);
    }

    @Test
    public void sameStationTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"),
                null, new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.TOO_CLOSE, result.status);
    }

    @Test
    public void addressTrip() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(51334078, 12478331),
                "04319 Leipzig-Engelsdorf", "August-Bebel-Platz");
        final Location to = new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
