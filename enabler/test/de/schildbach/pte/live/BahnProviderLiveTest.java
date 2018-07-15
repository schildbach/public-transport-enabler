/*
 * Copyright 2010-2015 the original author or authors.
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

import de.schildbach.pte.BahnProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class BahnProviderLiveTest extends AbstractProviderLiveTest {
    public BahnProviderLiveTest() {
        super(new BahnProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "692991"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(52525589, 13369548));
        print(result);
    }

    @Test
    public void nearbyPOIsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.POI),
                Location.coord(52525589, 13369548));
        print(result);
        assertThat(result.locations,
                hasItem(new Location(LocationType.POI, "990416076", "Berlin", "Museum für Naturkunde")));
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
        assertEquals("Güntzelstr. (U)", result.getLocations().get(0).name);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Dammt");
        print(result);
        assertEquals("Hamburg Dammtor", result.getLocations().get(0).name);
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
        assertEquals(LocationType.ADDRESS, result.getLocations().get(0).type);
        assertEquals("Friedenstraße 2", result.getLocations().get(0).name);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8011160", null, "Berlin Hbf"),
                null, new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "732655", 52535576, 13422171, null, "Marienburger Str., Berlin"),
                null,
                new Location(LocationType.STATION, "623234", 48000221, 11342490, null,
                        "Tutzinger-Hof-Platz, Starnberg"),
                new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void noTrips() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "513729", null, "Schillerplatz, Kaiserslautern"), null,
                new Location(LocationType.STATION, "403631", null, "Trippstadt Grundschule"), new Date(), true,
                Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
    }

    @Test
    public void tripWithFootway() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, 52517139, 13388749, null,
                        "Berlin - Mitte, Unter den Linden 24"),
                null,
                new Location(LocationType.ADDRESS, null, 47994243, 11338543, null,
                        "Starnberg, Possenhofener Straße 13"),
                new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripsAcrossBorder() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8506131", null, "Kreuzlingen"),
                null, new Location(LocationType.STATION, "8003400", null, "Konstanz"), new Date(), true,
                EnumSet.of(Product.BUS), WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void tripsByCoordinate() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(52535576, 13422171), null,
                Location.coord(52525589, 13369548), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL);
        print(result);
    }

    @Test
    public void tripsTooClose() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"),
                null, new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.TOO_CLOSE, result.status);
    }

    @Test
    public void tripsInvalidDate() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8011160", null, "Berlin Hbf"),
                null, new Location(LocationType.STATION, "8010205", null, "Leipzig Hbf"), new Date(0), true,
                Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        assertEquals(QueryTripsResult.Status.INVALID_DATE, result.status);
    }
}
