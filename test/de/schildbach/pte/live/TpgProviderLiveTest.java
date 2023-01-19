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

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.TpgProvider;
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
public class TpgProviderLiveTest extends AbstractProviderLiveTest {
    public TpgProviderLiveTest() {
        super(new TpgProvider(secretProperty("tpg.api_authorization")));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(46209460, 6167140)); // Parc et plage des Eaux-Vives
        print(result);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8501238", false); // Genève-Paquis
        print(result);
    }

    @Test
    public void queryDeparturesSuburbanTrain() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8516155", false); // Lancy-Pont-Rouge
        print(result);
    }

    @Test
    public void queryDeparturesTram() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8587907", true); // Plainpalais
        print(result);
    }

    // @Test
    // public void queryDeparturesTrolley() throws Exception {
    //     final QueryDeparturesResult result = queryDepartures("8591177", false); // Hardplatz
    //     print(result);
    // }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Champel");
        print(result);
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "8592913", "Genève", "Trembley");
        final Location to = new Location(LocationType.STATION, "8595027", "Cointrin", "De-Joinville");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    // @Test
    // public void trip() throws Exception {
    //     // final Location from = new Location(LocationType.STATION, "8593059", Point.from1E6(46163584, 6106258), "Plan-les-Ouates",
    //     //         "Plan-les-Ouates, Galaise");
    //     final Location from = new Location(LocationType.STATION, "8592913", "Genève", "Trembley");
    //     final Location to = new Location(LocationType.STATION, "8593217", Point.from1E6(46325307, 6064940), "Cessy",
    //             "Cessy-Les Hauts");
    //     final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
    //     print(result);
    //     final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
    //     print(laterResult);
    // }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(Point.fromDouble(46.163584, 6.106258)); // Plan-les-Ouates, Galaise
        final Location to = Location.coord(Point.fromDouble(46.325307, 6.064940)); // Cessy, Cessy-Les Hauts
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
