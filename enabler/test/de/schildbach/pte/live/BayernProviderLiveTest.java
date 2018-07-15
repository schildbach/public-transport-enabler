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
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.EnumSet;

import org.junit.Test;

import de.schildbach.pte.BayernProvider;
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
public class BayernProviderLiveTest extends AbstractProviderLiveTest {
    public BayernProviderLiveTest() {
        super(new BayernProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "3001459"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(48135232, 11560650));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void nearbyLocationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION, LocationType.POI),
                Location.coord(48135232, 11560650));
        print(result);
        assertTrue(result.locations.size() > 0);
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult munichOstbahnhof = queryDepartures("80000793", false);
        print(munichOstbahnhof);

        final QueryDeparturesResult munichHauptbahnhof = queryDepartures("80000689", false);
        print(munichHauptbahnhof);

        final QueryDeparturesResult nurembergHauptbahnhof = queryDepartures("80001020", false);
        print(nurembergHauptbahnhof);
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Marien");
        print(result);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München Mühldorfstraße");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "1000921")));
    }

    @Test
    public void suggestLocationsAddress() throws Exception {
        final SuggestLocationsResult result = suggestLocations("München, Friedenstraße 2");
        print(result);
    }

    @Test
    public void suggestLocationsLocal() throws Exception {
        final SuggestLocationsResult regensburgResult = suggestLocations("Regensburg");
        assertEquals("80001083", regensburgResult.getLocations().iterator().next().id);

        final SuggestLocationsResult munichResult = suggestLocations("München");
        assertEquals("80000689", munichResult.getLocations().iterator().next().id);

        final SuggestLocationsResult nurembergResult = suggestLocations("Nürnberg");
        assertEquals("80001020", nurembergResult.getLocations().iterator().next().id);
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "80000793", "München", "Ostbahnhof"), null,
                new Location(LocationType.STATION, "80000799", "München", "Pasing"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void longTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "1005530", "Starnberg", "Arbeitsamt"), null,
                new Location(LocationType.STATION, "3001459", "Nürnberg", "Fallrohrstraße"), new Date(), true,
                Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        // seems like there are no more trips all the time
    }

    @Test
    public void tripBetweenCoordinates() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(48165238, 11577473), null,
                Location.coord(47987199, 11326532), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenCoordinateAndStation() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 48238341, 11478230), null,
                new Location(LocationType.STATION, "80000793", "München", "Ostbahnhof"), new Date(), true, Product.ALL,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenAddresses() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, null, "München, Maximilianstr. 1"), null,
                new Location(LocationType.ADDRESS, null, null, "Starnberg, Jahnstraße 50"), new Date(), true,
                Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenStationAndAddress() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1001220", null, "Josephsburg"),
                null, new Location(LocationType.ADDRESS, null, 48188018, 11574239, null, "München Frankfurter Ring 35"),
                new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripBetweenPOIs() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.POI, null, 47710568, 12621970, null, "Ruhpolding, Seehaus"), null,
                new Location(LocationType.POI, null, 47738372, 12630996, null, "Ruhpolding, Unternberg-Bahn"),
                new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void tripRegensburg() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "4014051", "Regensburg", "Klenzestraße"), null,
                new Location(LocationType.STATION, "4014080", "Regensburg", "Universität"), new Date(), true,
                Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
        print(result);
        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
