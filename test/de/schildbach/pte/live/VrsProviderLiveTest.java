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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ComparisonChain;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.VrsProvider;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Michael Dyrna
 */
public class VrsProviderLiveTest extends AbstractProviderLiveTest {
    public VrsProviderLiveTest() {
        super(new VrsProvider());
    }

    @Test
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(50942970, 6958570));
        print(result);

        final NearbyLocationsResult result2 = queryNearbyStations(Location.coord(51719648, 8754330));
        print(result2);
    }

    @Test
    public void nearbyLocationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(50732100, 7096820), 100, 1);
        print(result);

        final NearbyLocationsResult result2 = queryNearbyLocations(EnumSet.of(LocationType.ADDRESS),
                Location.coord(50732100, 7096820));
        print(result2);

        final NearbyLocationsResult result3 = queryNearbyLocations(EnumSet.of(LocationType.POI),
                Location.coord(50732100, 7096820));
        print(result3);

        final NearbyLocationsResult result4 = queryNearbyLocations(
                EnumSet.of(LocationType.ADDRESS, LocationType.STATION), Location.coord(50732100, 7096820));
        print(result4);
    }

    @Test
    public void nearbyLocationsByRandomCoordinates() throws Exception {
        Random rand = new Random(new Date().getTime());
        final int LAT_FROM = 50500000;
        final int LAT_TO = 51600000;
        final int LON_FROM = 6200000;
        final int LON_TO = 7600000;
        for (int i = 0; i < 10; i++) {
            int lat = LAT_FROM + rand.nextInt(LAT_TO - LAT_FROM);
            int lon = LON_FROM + rand.nextInt(LON_TO - LON_FROM);
            NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.ANY), Location.coord(lat, lon));
            System.out.println(result);
            assertNotNull(result.locations);
            assertNotNull(result.locations.get(0));
        }
    }

    @Test
    public void nearbyStationsWithLimits() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(50732100, 7096820), 0, 0);
        print(result);

        final NearbyLocationsResult result2 = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(50732100, 7096820), 0, 1);
        print(result2);

        final NearbyLocationsResult result3 = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(50732100, 7096820), 100, 0);
        print(result3);
    }

    @Test
    public void nearbyLocationsEmpty() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.allOf(LocationType.class),
                Location.coord(1, 1), 1000, 0);
        print(result);
        assertEquals(0, result.locations.size());
    }

    private static void printLineDestinations(final QueryDeparturesResult result) {
        for (StationDepartures stationDepartures : result.stationDepartures) {
            final List<LineDestination> lines = stationDepartures.lines;
            if (lines != null) {
                for (LineDestination lineDestination : lines) {
                    System.out.println(lineDestination.line + " to " + lineDestination.destination);
                }
            }
        }
    }

    @Test
    public void queryDeparturesBonnHbf() throws Exception {
        final QueryDeparturesResult result = queryDepartures("687", false);
        print(result);
        printLineDestinations(result);
    }

    @Test
    public void queryDeparturesKoelnHbf() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8", false);
        print(result);
        printLineDestinations(result);
    }

    @Test
    public void queryDeparturesGaussstr() throws Exception {
        final QueryDeparturesResult result = queryDepartures("8984", false);
        print(result);
        printLineDestinations(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void queryManyDepartures() throws Exception {
        Random rand = new Random(new Date().getTime());
        for (int i = 0; i < 10; i++) {
            Integer id = 1 + rand.nextInt(20000);
            try {
                final QueryDeparturesResult result = queryDepartures(id.toString(), false);
                if (result.status == QueryDeparturesResult.Status.OK) {
                    print(result);
                    printLineDestinations(result);
                } else {
                    System.out.println("Status is " + result.status);
                }
            } catch (SocketTimeoutException ex) {
                System.out.println("SocketTimeoutException: " + ex);
            }
        }
    }

    @Test
    public void suggestLocationsIncomplete() throws Exception {
        final SuggestLocationsResult stationResult = suggestLocations("Beuel");
        print(stationResult);

        final SuggestLocationsResult addressResult = suggestLocations("Lützow 41 Köln");
        print(addressResult);

        final SuggestLocationsResult poiResult = suggestLocations("Schokolade");
        print(poiResult);
    }

    @Test
    public void suggestLocationsWithUmlaut() throws Exception {
        final SuggestLocationsResult result = suggestLocations("grün");
        print(result);
    }

    @Test
    public void suggestLocationsIdentified() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Düsseldorf, Am Frohnhof");
        print(result);
    }

    @Test
    public void suggestLocationsCoverage() throws Exception {
        final SuggestLocationsResult cologneResult = suggestLocations("Köln Ebertplatz");
        print(cologneResult);
        assertThat(cologneResult.getLocations(), hasItem(new Location(LocationType.STATION, "35")));

        final SuggestLocationsResult dortmundResult = suggestLocations("Dortmund Zugstraße");
        print(dortmundResult);
        assertThat(dortmundResult.getLocations(), hasItem(new Location(LocationType.STATION, "54282")));

        final SuggestLocationsResult duesseldorfResult = suggestLocations("Düsseldorf Sternstraße");
        print(duesseldorfResult);
        assertThat(duesseldorfResult.getLocations(), hasItem(new Location(LocationType.STATION, "52839")));

        final SuggestLocationsResult muensterResult = suggestLocations("Münster Vennheideweg");
        print(muensterResult);
        assertThat(muensterResult.getLocations(), hasItem(new Location(LocationType.STATION, "41112")));

        final SuggestLocationsResult aachenResult = suggestLocations("Aachen Elisenbrunnen");
        print(aachenResult);
        assertThat(aachenResult.getLocations(), hasItem(new Location(LocationType.STATION, "20580")));

        final SuggestLocationsResult bonnResult = suggestLocations("Bonn Konrad-Adenauer-Platz");
        print(aachenResult);
        assertThat(bonnResult.getLocations(), hasItem(new Location(LocationType.STATION, "1500")));
    }

    @Test
    public void suggestLocationsCity() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Düsseldorf");
        print(result);
    }

    @Test
    public void suggestManyLocations() throws Exception {
        Random rand = new Random(new Date().getTime());
        for (int i = 0; i < 10; i++) {
            final StringBuilder s = new StringBuilder();
            int len = rand.nextInt(256);
            for (int j = 0; j < len; j++) {
                char c = (char) ('a' + rand.nextInt(26));
                s.append(c);
            }
            final SuggestLocationsResult result = suggestLocations(s);
            System.out.print(s + " => ");
            print(result);
        }
    }

    @Test
    public void anyTripAmbiguous() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Köln"),
                new Location(LocationType.ANY, null, null, "Leverkusen"),
                new Location(LocationType.ANY, null, null, "Bonn"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
        assertNotNull(result.ambiguousFrom);
        assertNotNull(result.ambiguousVia);
        assertNotNull(result.ambiguousTo);
    }

    @Test
    public void anyTripUnique() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "T-Mobile"), null,
                new Location(LocationType.ANY, null, null, "Schauspielhalle"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
    }

    @Test
    public void anyTripUnknown() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "\1"), null,
                new Location(LocationType.ANY, null, null, "\2"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result.status);
    }

    @Test
    public void tripEarlierLater() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8"), null,
                new Location(LocationType.STATION, "9"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        print(result);

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        assertEquals(QueryTripsResult.Status.OK, laterResult.status);
        assertTrue(laterResult.trips.size() > 0);
        print(laterResult);

        final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
        assertEquals(QueryTripsResult.Status.OK, later2Result.status);
        assertTrue(later2Result.trips.size() > 0);
        print(later2Result);

        final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
        assertEquals(QueryTripsResult.Status.OK, earlierResult.status);
        assertTrue(earlierResult.trips.size() > 0);
        print(earlierResult);
    }

    @Test
    public void tripEarlierLaterCologneBonn() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8"), null,
                new Location(LocationType.STATION, "687"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        print(result);

        System.out.println("And now earlier...");

        final QueryTripsResult earlierResult = queryMoreTrips(result.context, false);
        assertEquals(QueryTripsResult.Status.OK, earlierResult.status);
        assertTrue(earlierResult.trips.size() > 0);
        print(earlierResult);
    }

    @Test
    public void testTripWithProductFilter() throws Exception {
        final TripOptions options = new TripOptions(EnumSet.of(Product.ON_DEMAND, Product.SUBWAY, Product.FERRY,
                Product.TRAM, Product.CABLECAR, Product.BUS), null, WalkSpeed.NORMAL, Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1504"), null,
                new Location(LocationType.STATION, "1"), new Date(), true, options);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
        print(result);
    }

    @Test
    public void testTripBeuelKoelnSued() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1504"), null,
                new Location(LocationType.STATION, "25"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripBonnHbfBonnBeuel() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null,
                new Location(LocationType.STATION, "1504"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripBonnHbfDorotheenstr() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null,
                new Location(LocationType.STATION, "1150"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripKoelnHbfBresslauerPlatz() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8"), null,
                new Location(LocationType.STATION, "9"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripDuerenLammersdorf() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "6868"), null,
                new Location(LocationType.STATION, "21322"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripEhrenfeldNeumarkt() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "251"), null,
                new Location(LocationType.STATION, "2"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripCologneWickede() throws Exception {
        final TripOptions options = new TripOptions(
                EnumSet.of(Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM), null,
                WalkSpeed.NORMAL, Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8"), null,
                new Location(LocationType.STATION, "10781"), new Date(), true, options);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripByCoord() throws Exception {
        final QueryTripsResult result = queryTrips(Location.coord(50740530, 7129200), null,
                Location.coord(50933930, 6932440), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripByAddressAndEmptyPolygon() throws Exception {
        final Location from = new Location(LocationType.ADDRESS, null /* id */, Point.from1E6(50909350, 6676310),
                "Kerpen-Sindorf", "Erftstraße 43");
        final Location to = new Location(LocationType.ADDRESS, null /* id */, Point.from1E6(50923000, 6818440),
                "Frechen", "Zedernweg 1");
        final QueryTripsResult result = queryTrips(from, null, to, new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripWithSurchargeInfo() throws Exception {
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null,
                new Location(LocationType.STATION, "892"), new Date(), true, null);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    @Test
    public void testTripAachenEschweilerBus() throws Exception {
        final TripOptions options = new TripOptions(EnumSet.of(Product.BUS), null, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "10004"), null,
                new Location(LocationType.STATION, "10003"), new Date(), true, options);
        print(result);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        assertTrue(result.trips.size() > 0);
    }

    private void manyRandomTrips(int latFrom, int latTo, int lonFrom, int lonTo) throws Exception {
        Random rand = new Random(new Date().getTime());
        int errors = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            try {
                int fromLat = latFrom + rand.nextInt(latTo - latFrom);
                int fromLon = lonFrom + rand.nextInt(lonTo - lonFrom);
                int toLat = latFrom + rand.nextInt(latTo - latFrom);
                int toLon = lonFrom + rand.nextInt(lonTo - lonFrom);
                final QueryTripsResult result = queryTrips(Location.coord(fromLat, fromLon), null,
                        Location.coord(toLat, toLon), new Date(), true, null);
                System.out.println("# " + (i + 1));
                if (result.status.equals(QueryTripsResult.Status.OK)) {
                    print(result);
                } else {
                    System.out.println("Status is " + result.status);
                    errors++;
                }
            } catch (SocketTimeoutException ex) {
                System.out.println("SocketTimeoutException: " + ex);
                errors++;
            }
        }
        final long stopTime = System.currentTimeMillis();
        final long elapsedTime = stopTime - startTime;
        System.out.println("Elapsed: " + (elapsedTime / 1000) + " seconds");
        System.out.println("Errors: " + errors);
    }

    @Ignore
    @Test
    public void manyRandomTripsNRW() throws Exception {
        manyRandomTrips(50500000, 51600000, 6200000, 7600000);
    }

    @Ignore
    @Test
    public void manyRandomTripsCologne() throws Exception {
        manyRandomTrips(50828176, 51083369, 6770942, 7161643);
    }

    @Ignore
    @Test
    public void manyRandomTripsBonn() throws Exception {
        manyRandomTrips(50632639, 50774408, 7019582, 7209096);
    }

    @Ignore
    @Test
    public void manyRandomTripsDuesseldorf() throws Exception {
        manyRandomTrips(51123960, 51353094, 6689381, 6940006);
    }

    private static class LocationComparator implements Comparator<Location> {
        @Override
        public int compare(Location o1, Location o2) {
            return ComparisonChain.start().compare(o1.name, o2.name).result();
        }
    }

    private void crawlStationsAndLines(int latFrom, int latTo, int lonFrom, int lonTo) throws Exception {
        Set<Location> stations = new TreeSet<>(new LocationComparator());
        Random rand = new Random(new Date().getTime());
        for (int i = 0; i < 5; i++) {
            int lat = latFrom + rand.nextInt(latTo - latFrom);
            int lon = lonFrom + rand.nextInt(lonTo - lonFrom);
            System.out.println(i + " " + lat + " " + lon);
            NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                    Location.coord(lat, lon), 0, 3);
            if (result.status == NearbyLocationsResult.Status.OK) {
                stations.addAll(result.locations);
            }
        }
        Set<Line> lines = new TreeSet<>();
        for (Location station : stations) {
            QueryDeparturesResult qdr = provider.queryDepartures(station.id, new Date(), 100, false);
            if (qdr.status == QueryDeparturesResult.Status.OK) {
                for (StationDepartures stationDepartures : qdr.stationDepartures) {
                    final List<LineDestination> stationDeparturesLines = stationDepartures.lines;
                    if (stationDeparturesLines != null) {
                        for (LineDestination ld : stationDeparturesLines) {
                            lines.add(ld.line);
                        }
                    }
                }
            }
        }

        for (Location station : stations) {
            System.out.println(station.toString());
        }
        for (Line line : lines) {
            final Product product = line.product;
            if (product != null) {
                final Style style = line.style;
                if (style != null) {
                    System.out.printf("%s %s %6x\n", product, line.label, style.backgroundColor);
                }
            }
        }
    }

    @Ignore
    @Test
    public void crawlStationsAndLinesNRW() throws Exception {
        crawlStationsAndLines(50500000, 51600000, 6200000, 7600000);
    }

    @Ignore
    @Test
    public void crawlStationsAndLinesCologne() throws Exception {
        crawlStationsAndLines(50828176, 51083369, 6770942, 7161643);
    }

    @Ignore
    @Test
    public void crawlStationsAndLinesBonn() throws Exception {
        crawlStationsAndLines(50632639, 50774408, 7019582, 7209096);
    }

    @Ignore
    @Test
    public void crawlStationsAndLinesDuesseldorf() throws Exception {
        crawlStationsAndLines(51123960, 51353094, 6689381, 6940006);
    }

    @Ignore
    @Test
    public void crawlStationsAndLinesEssen() throws Exception {
        crawlStationsAndLines(51347508, 51533689, 6893109, 7137554);
    }
}
