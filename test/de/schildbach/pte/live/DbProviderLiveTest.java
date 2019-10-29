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
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.AbstractHafasClientInterfaceProvider;
import de.schildbach.pte.DbProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Andreas Schildbach
 */
public class DbProviderLiveTest extends AbstractProviderLiveTest {
    public DbProviderLiveTest() {
        super(new DbProvider(secretProperty("db.api_authorization"), AbstractHafasClientInterfaceProvider
                .decryptSalt(secretProperty("db.encrypted_salt"), secretProperty("hci.salt_encryption_key"))));
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(52525589, 13369548));
        print(result);
    }

    @Test
    public void nearbyPOIsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.POI),
                Location.coord(Point.fromDouble(52.5304903, 13.3791152)));
        print(result);
        assertThat(result.locations, hasItem(new Location(LocationType.POI,
                "A=4@O=Berlin, Museum für Naturkunde (Kultur und Unterhal@X=13380003@Y=52529724@u=0@U=104@L=991597061@",
                "Berlin", "Museum für Naturkunde")));
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("692991", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult resultLive = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultLive.status);
    }

    @Test
    public void suggestLocationsUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Güntzelstr. (U)");
        print(result);
        assertThat(result.getLocations(),
                hasItem(new Location(LocationType.STATION, "731371", "Berlin", "Güntzelstr. (U)")));
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Dammt");
        print(result);
        assertThat(result.getLocations(),
                hasItem(new Location(LocationType.STATION, "8002548", null, "Hamburg Dammtor")));
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Berlin");
        print(result);
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Friedenstraße 2");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.ADDRESS,
                "A=2@O=München - Berg am Laim, Friedenstraße 2@X=11602251@Y=48123949@U=103@L=980857648@B=1@p=1378873973@",
                "München - Berg am Laim", "Friedenstraße 2")));
    }

    @Test
    public void shortTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "8011160", null, "Berlin Hbf");
        final Location to = new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        print(later2Result);
        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        print(earlierResult);
        final QueryTripsResult later3Result = queryMoreTrips(earlierResult.context, true);
        print(later3Result);
    }

    @Test
    public void slowTrip() throws Exception {
        final Location from = new Location(LocationType.STATION, "732655", Point.from1E6(52535576, 13422171), null,
                "Marienburger Str., Berlin");
        final Location to = new Location(LocationType.STATION, "623234", Point.from1E6(48000221, 11342490), null,
                "Tutzinger-Hof-Platz, Starnberg");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void noTrips() throws Exception {
        final Location from = new Location(LocationType.STATION, "513729", null, "Schillerplatz, Kaiserslautern");
        final Location to = new Location(LocationType.STATION, "403631", null, "Trippstadt Grundschule");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripWithFootway() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null, Point.from1E6(52517139, 13388749), null,
                "Berlin - Mitte, Unter den Linden 24");
        final Location to = new Location(LocationType.ADDRESS, null, Point.from1E6(47994243, 11338543), null,
                "Starnberg, Possenhofener Straße 13");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripsAcrossBorder() throws Exception {
        final TripOptions options = new TripOptions(EnumSet.of(Product.BUS), null, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL, null);
        final Location from = new Location(LocationType.STATION, "8506131", null, "Kreuzlingen");
        final Location to = new Location(LocationType.STATION, "8003400", null, "Konstanz");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, options);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final Location from = Location.coord(52535576, 13422171); // Berlin Marienburger Str.
        final Location to = Location.coord(52525589, 13369548); // Berlin Hbf
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }

    @Test
    public void tripsTooClose() throws Exception {
        final Location location = new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf");
        final QueryTripsResult result = queryTrips(location, null, location, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.TOO_CLOSE, result.status);
    }

    @Test
    public void tripsInvalidDate() throws Exception {
        final Location from = new Location(LocationType.STATION, "8011160", null, "Berlin Hbf");
        final Location to = new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf");
        final Date date = new Date(System.currentTimeMillis() - 2 * 365 * 24 * 3600 * 1000L); // 2 years ago
        final QueryTripsResult result = queryTrips(from, null, to, date, true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.INVALID_DATE, result.status);
    }

    @Test
    public void tripBetweenAreas() throws Exception {
        final Location from = new Location(LocationType.STATION, "8096021"); // FRANKFURT(MAIN)
        final Location to = new Location(LocationType.STATION, "8096022"); // KÖLN
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
    }
}
