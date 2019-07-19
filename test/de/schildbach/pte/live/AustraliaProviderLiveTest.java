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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import de.schildbach.pte.AustraliaProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.Trip;

/**
 * Test basic from/to directions for each mode of transport, in each state of Australia supported by Navitia.
 * This is mainly to test whether or not the coverage is still in date or not. For example, at time of writing
 * Cambera, Alice Springs, and Darwin were all present on Navitia, but out of date so were unable to provide
 * journey planning.
 *
 * These tests work by taking the next Monday at 08:45 (a random peak hour time where you'd expect there to be
 * a lot of public transport available). If they are unable to find a route for a specific mode of transport,
 * then you should further investigate to see if the data is out of date or not in Navitia.
 *
 * Note that by default, only Melbourne is tested comprehensively to prevent running into API limits
 * ({@see #RUN_EXPENSIVE_TESTS}).
 */
public class AustraliaProviderLiveTest extends AbstractNavitiaProviderLiveTest {

    /**
     * If enabled, the entire set of tests will run, resulting in over 30 API calls to Navitia. Given this
     * test may or may not be run under, e.g. continuous integration, or run frequently while working on the
     * Australian API, it could end up using up your API limit unneccesarily. Thus, the default value is to
     * only perform a proper test of Melbourne, and the rest of the coverage is disabled until this flag is
     * true.
     */
    private static final boolean RUN_EXPENSIVE_TESTS = false;

    public AustraliaProviderLiveTest() {
        super(new AustraliaProvider(secretProperty("navitia.authorization")));
    }

    /**
     * Ensures that each of the suburban/rural trains, trams, and buses are represented in the journey
     * planning and location suggestion API. Based on travelling around the Camberwell area:
     * http://www.openstreetmap.org/#map=15/-37.8195/145.0586&layers=T
     */
    @Test
    public void melbourne() throws IOException {
        final Location suburbanTrainStation = assertAndGetLocation("Camberwell Railway Station (Camberwell)");
        final Location ruralTrainStation = assertAndGetLocation("Geelong Railway Station (Geelong)");
        assertJourneyExists(AustraliaProvider.NETWORK_PTV, new String[] { "Lilydale", "Belgrave", "Alamein" },
                suburbanTrainStation, ruralTrainStation);

        final Location tramStop = assertAndGetLocation("70-Cotham Rd/Burke Rd (Kew)");
        assertJourneyExists(AustraliaProvider.NETWORK_PTV, "72", suburbanTrainStation, tramStop);

        final Location busStop = assertAndGetLocation("Lawrence St/Burke Rd (Kew East)");
        assertJourneyExists(AustraliaProvider.NETWORK_PTV, "548", tramStop, busStop);
    }

    @Test
    public void adelaideRail() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location railwayStation = assertAndGetLocation("Woodville Park Railway Station");
        final Location railwayStation2 = assertAndGetLocation("Unley Park Railway Station");
        assertJourneyExists(AustraliaProvider.NETWORK_SA, new String[] { "GRNG", "BEL", "OUTHA" }, railwayStation,
                railwayStation2);
    }

    @Test
    public void adelaideBus() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location busStation = assertAndGetLocation("Stop 137 Portrush Rd - East side");
        final Location busStation2 = assertAndGetLocation("Stop 144 Portrush Rd - East side");
        assertJourneyExists(AustraliaProvider.NETWORK_SA, "300", busStation, busStation2);
    }

    @Test
    public void adelaideTram() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location tramStation = assertAndGetLocation("Stop 15 Dunbar Tce - Brighton Rd");
        final Location tramStation2 = assertAndGetLocation("Stop 5 Black Forest");
        assertJourneyExists(AustraliaProvider.NETWORK_SA, "Tram", tramStation, tramStation2);
    }

    @Test
    public void perthTrain() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location railwayStation = assertAndGetLocation("Kenwick Stn");
        final Location railwayStation2 = assertAndGetLocation("Warwick Stn");
        assertJourneyExists(AustraliaProvider.NETWORK_WA, "", railwayStation, railwayStation2);
    }

    @Test
    public void perthBus() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location bus = assertAndGetLocation("Curtin University");
        final Location bus2 = assertAndGetLocation("Murdoch Stn");
        assertJourneyExists(AustraliaProvider.NETWORK_WA, "", bus, bus2);
    }

    @Test
    public void perthFerry() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location ferry = assertAndGetLocation("Elizabeth Quay Stn");
        final Location ferry2 = assertAndGetLocation("Ferry Route Mends St Jetty");
        assertJourneyExists(AustraliaProvider.NETWORK_WA, "", ferry, ferry2);
    }

    @Test
    public void brisbaneRail() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location railwayStation = assertAndGetLocation("Beenleigh station");
        final Location railwayStation2 = assertAndGetLocation("Ipswich station");
        assertJourneyExists(AustraliaProvider.NETWORK_QLD, "BNFG", railwayStation, railwayStation2);
    }

    @Test
    public void brisbaneFerry() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location ferry = assertAndGetLocation("Broadbeach South");
        final Location ferry2 = assertAndGetLocation("Southport");
        assertJourneyExists(AustraliaProvider.NETWORK_QLD, "GLKN", ferry, ferry2);
    }

    @Test
    public void brisbaneTram() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location tram = assertAndGetLocation("South Bank 2 ferry terminal");
        final Location tram2 = assertAndGetLocation("Guyatt Park ferry terminal");
        assertJourneyExists(AustraliaProvider.NETWORK_QLD, "UQSL", tram, tram2);
    }

    @Test
    public void hobartBus() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location bus = assertAndGetLocation("Stop 15, No.237 New Town Rd");
        final Location bus2 = assertAndGetLocation("Stop 2, No.131 Elizabeth St");
        assertJourneyExists(AustraliaProvider.NETWORK_TAS, "504", bus, bus2);
    }

    @Test
    public void launcestonBus() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location bus = assertAndGetLocation("Riverside Dr / Rannoch Ave");
        final Location bus2 = assertAndGetLocation("Trevallyn Shops");
        assertJourneyExists(AustraliaProvider.NETWORK_TAS, "90", bus, bus2);
    }

    @Test
    public void bernieBus() throws IOException {
        assumeTrue(RUN_EXPENSIVE_TESTS);

        final Location bus = assertAndGetLocation("Stop 31, 197 Mount St");
        final Location bus2 = assertAndGetLocation("Burnie Park opposite 55 West Park Gr");
        assertJourneyExists(AustraliaProvider.NETWORK_TAS, "12", bus, bus2);
    }

    // Although Navitia has a GTFS feed for ACT, Darwin, and Alice Springs, they were out of date at time of
    // writing.
    @Test
    @Ignore
    public void act() {
    }

    @Test
    @Ignore
    public void darwin() {
    }

    @Test
    @Ignore
    public void aliceSprings() {
    }

    /**
     * Suggests locations similar to {@param locationName}, but then ensures that one matches exactly and then
     * returns it. Try not to use an ambiguous name such as "Central Station", because it may exist in several
     * datasets on Navitia.
     */
    private Location assertAndGetLocation(String locationName) throws IOException {
        SuggestLocationsResult locations = suggestLocations(locationName);
        assertEquals(SuggestLocationsResult.Status.OK, locations.status);
        assertTrue(locations.getLocations().size() > 0);

        StringBuilder nonMatching = new StringBuilder();
        for (Location location : locations.getLocations()) {
            if (locationName.equals(location.name)) {
                return location;
            }

            nonMatching.append('[').append(location.name).append("] ");
        }

        throw new AssertionError(
                "suggestLocations() did not find \"" + locationName + "\". Options were: " + nonMatching);
    }

    /**
     * @see #assertJourneyExists(String, String[], Location, Location)
     */
    private void assertJourneyExists(String network, String eligibleLine, Location from, Location to)
            throws IOException {
        assertJourneyExists(network, new String[] { eligibleLine }, from, to);
    }

    private Date getNextMondayMorning() {
        Calendar date = Calendar.getInstance();
        date.setTime(new Date());
        date.set(Calendar.HOUR_OF_DAY, 8);
        date.set(Calendar.MINUTE, 45);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            date.add(Calendar.DATE, 1);
        }

        return date.getTime();
    }

    private void assertJourneyExists(String network, String[] eligibleLines, Location from, Location to)
            throws IOException {
        QueryTripsResult trips = queryTrips(from, null, to, getNextMondayMorning(), true, null);
        assertNull(trips.ambiguousFrom);
        assertNull(trips.ambiguousTo);
        assertEquals(QueryTripsResult.Status.OK, trips.status);
        assertNotNull(trips.trips);
        assertTrue(trips.trips.size() > 0);

        Set<String> eligibleLineSet = new HashSet<>();
        Collections.addAll(eligibleLineSet, eligibleLines);

        for (Trip trip : trips.trips) {
            boolean hasPublicTransport = false;
            boolean matchesCode = false;
            for (Trip.Leg leg : trip.legs) {
                if (leg instanceof Trip.Public) {
                    hasPublicTransport = true;

                    Trip.Public publicLeg = (Trip.Public) leg;
                    assertEquals(network, publicLeg.line.network);

                    if (eligibleLineSet.contains(publicLeg.line.label)) {
                        matchesCode = true;
                    }
                }
            }

            if (hasPublicTransport && matchesCode) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Trip trip : trips.trips) {
            sb.append("\n  ");
            for (Trip.Leg leg : trip.legs) {
                String via = leg instanceof Trip.Public ? " (via " + ((Trip.Public) leg).line.label + ") " : " -> ";
                sb.append('[').append(leg.arrival.name).append(']').append(via).append('[').append(leg.departure.name)
                        .append(']').append("  ...  ");
            }
        }

        fail("No public trip found between [" + from.name + "] and [" + to.name
                + "] using appropriate line. Found trips:" + sb);
    }
}
