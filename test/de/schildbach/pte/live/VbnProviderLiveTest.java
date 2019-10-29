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

import de.schildbach.pte.AbstractHafasClientInterfaceProvider;
import de.schildbach.pte.VbnProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class VbnProviderLiveTest extends AbstractProviderLiveTest {
    public VbnProviderLiveTest() {
        super(new VbnProvider(secretProperty("vbn.api_authorization"), AbstractHafasClientInterfaceProvider
                .decryptSalt(secretProperty("vbn.encrypted_salt"), secretProperty("hci.salt_encryption_key"))));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result1 = queryNearbyStations(Location.coord(53086421, 8806388));
        print(result1);

        final NearbyLocationsResult result2 = queryNearbyStations(Location.coord(51536614, 9925673));
        print(result2);

        final NearbyLocationsResult result3 = queryNearbyStations(Location.coord(54078314, 12131715));
        print(result3);
    }

    @Test
    public void queryDeparturesFreudenstadt() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8000110", false);
        print(result);
    }

    @Test
    public void queryDeparturesGoettingen() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8000128", false);
        print(result);
    }

    @Test
    public void queryDeparturesRostockHbf() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8010304", false);
        print(result);
    }

    @Test
    public void queryDeparturesEquivs() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8010304", true);
        print(result);
        assertTrue(result.stationDepartures.size() > 1);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocationsBremen() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Bremen");
        print(result);
    }

    @Test
    public void suggestLocationsHannover() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Hannover");
        print(result);
    }

    @Test
    public void suggestLocationsRostock() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Rostock");
        print(result);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result1 = suggestLocations("Göttingen Hauptbahnhof");
        print(result1);
        assertEquals("Göttingen", result1.getLocations().get(0).name);

        final SuggestLocationsResult result2 = suggestLocations("Lütten Klein");
        print(result2);
        assertEquals("Lütten Klein", result2.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult rostockResult = suggestLocations("Rostock");
        print(rostockResult);
        assertThat(rostockResult.getLocations(), hasItem(new Location(LocationType.STATION, "8010304")));

        final SuggestLocationsResult warnemuendeResult = suggestLocations("Warnemünde");
        print(warnemuendeResult);
        assertThat(warnemuendeResult.getLocations(), hasItem(new Location(LocationType.STATION, "8013236")));
    }

    @Test
    public void suggestLocationsLocality() throws Exception {
        final SuggestLocationsResult result = suggestLocations("lange straße");
        print(result);
        assertThat(result.getLocations(),
                hasItem(new Location(LocationType.STATION, "708425", "Rostock", "Lange Straße")));
    }

    @Test
    public void suggestLocationsWithoutCoordinatesInResult() throws Exception {
        final SuggestLocationsResult result = suggestLocations("aachen");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8096109", null, "Oldenburg"),
                null, new Location(LocationType.STATION, "625398", null, "Bremerhaven"), new Date(), true, null);
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

        if (!earlierResult.context.canQueryEarlier())
            return;

        final QueryTripsResult earlier2Result = queryMoreTrips(earlierResult.context, false);
        print(earlier2Result);
    }

    @Test
    public void shortTripGoettingen() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8000128", null, "Göttingen"),
                null, new Location(LocationType.STATION, "1140061", null, "Göttingen Nikolausberger Weg"), new Date(),
                true, null);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripDateOutsideTimetablePeriod() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8096109", null, "Oldenburg"),
                null, new Location(LocationType.STATION, "625398", null, "Bremerhaven"), new Date(1155822689759L), true,
                null);
        assertEquals(QueryTripsResult.Status.INVALID_DATE, result.status);
    }
}
