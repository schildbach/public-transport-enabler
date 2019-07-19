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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Antonio El Khoury
 */
public abstract class AbstractNavitiaProviderLiveTest extends AbstractProviderLiveTest {
    public AbstractNavitiaProviderLiveTest(final NetworkProvider provider) {
        super(provider);
    }

    protected final void nearbyStationsAddress(final int lat, final int lon) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(lat, lon), 700, 10);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        print(result);
    }

    protected final void nearbyStationsStation(final String stationId) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                new Location(LocationType.STATION, stationId), 700, 10);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        print(result);
    }

    protected final void nearbyStationsPoi(final String poiId) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                new Location(LocationType.POI, poiId), 700, 10);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        print(result);
    }

    protected final void nearbyStationsAny(final int lat, final int lon) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(lat, lon), 700, 10);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        print(result);
    }

    protected final void nearbyStationsStationDistance(final String stationId) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                new Location(LocationType.STATION, stationId), 0, 10);
        assertEquals(NearbyLocationsResult.Status.OK, result.status);
        assertTrue(result.locations.size() > 1);
        print(result);
    }

    protected final void nearbyStationsInvalidStation(final String stationId) throws IOException {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION),
                new Location(LocationType.STATION, stationId), 700, 10);
        assertEquals(NearbyLocationsResult.Status.INVALID_ID, result.status);
        print(result);
    }

    protected final void queryDeparturesEquivsFalse(final String stationId) throws IOException {
        final int maxDepartures = 5;
        final QueryDeparturesResult result = queryDepartures(stationId, maxDepartures, false);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertEquals(1, result.stationDepartures.size());
        assertTrue(result.stationDepartures.get(0).departures.size() <= maxDepartures);
        final List<LineDestination> lines = result.stationDepartures.get(0).lines;
        assertNotNull(lines);
        assertTrue(lines.size() >= 1);
        print(result);
    }

    protected final void queryDeparturesStopArea(final String stationId) throws IOException {
        final int maxDepartures = 5;
        final QueryDeparturesResult result = queryDepartures(stationId, maxDepartures, true);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertTrue(result.stationDepartures.size() > 1);
        int nbDepartures = 0;
        int nbLines = 0;
        for (final StationDepartures stationDepartures : result.stationDepartures) {
            nbDepartures += stationDepartures.departures.size();
            final List<LineDestination> lines = stationDepartures.lines;
            if (lines != null)
                nbLines += lines.size();
        }
        assertTrue(nbDepartures <= maxDepartures);
        assertTrue(nbLines >= 2);
        print(result);
    }

    protected final void queryDeparturesEquivsTrue(final String stationId) throws IOException {
        final int maxDepartures = 5;
        final QueryDeparturesResult result = queryDepartures(stationId, maxDepartures, true);
        assertEquals(QueryDeparturesResult.Status.OK, result.status);
        assertTrue(result.stationDepartures.size() > 1);
        int nbDepartures = 0;
        int nbLines = 0;
        for (StationDepartures stationDepartures : result.stationDepartures) {
            nbDepartures += stationDepartures.departures.size();
            final List<LineDestination> lines = stationDepartures.lines;
            assertNotNull(lines);
            nbLines += lines.size();
        }
        assertTrue(nbDepartures <= maxDepartures);
        assertTrue(nbLines >= 2);
        print(result);
    }

    protected final void queryDeparturesInvalidStation(final String stationId) throws IOException {
        final QueryDeparturesResult result = queryDepartures(stationId, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    protected final void suggestLocationsFromName(final CharSequence constraint) throws IOException {
        final SuggestLocationsResult result = suggestLocations(constraint);
        assertTrue(result.getLocations().size() > 0);
        print(result);
    }

    protected final void suggestLocationsFromAddress(final CharSequence constraint) throws IOException {
        final SuggestLocationsResult result = suggestLocations(constraint);
        assertTrue(result.getLocations().size() > 0);
        print(result);
    }

    protected final void suggestLocationsNoLocation(final CharSequence constraint) throws IOException {
        final SuggestLocationsResult result = suggestLocations(constraint);
        assertEquals(result.getLocations().size(), 0);
        print(result);
    }

    protected final void queryTrip(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                toResult.getLocations().get(0), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);
    }

    protected final void queryTripNoSolution(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final TripOptions options = new TripOptions(EnumSet.noneOf(Product.class), null, WalkSpeed.NORMAL,
                Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                toResult.getLocations().get(0), new Date(), true, options);
        assertEquals(QueryTripsResult.Status.NO_TRIPS, result.status);
        print(result);
    }

    protected final void queryTripUnknownFrom(final CharSequence to) throws IOException {
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "stop_area:RTP:SA:999999"), null,
                toResult.getLocations().get(0), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result.status);
        print(result);
    }

    protected final void queryTripUnknownTo(final CharSequence from) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                new Location(LocationType.STATION, "stop_area:RTP:SA:999999"), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.UNKNOWN_TO, result.status);
        print(result);
    }

    protected final void queryTripAmbiguousFrom(final Location from, final CharSequence to) throws IOException {
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(from, null, toResult.getLocations().get(0), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
        assertTrue(result.ambiguousFrom != null);
        assertTrue(result.ambiguousFrom.size() > 0);
        print(result);
    }

    protected final void queryTripAmbiguousTo(final CharSequence from, final Location to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null, to, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
        assertTrue(result.ambiguousTo != null);
        assertTrue(result.ambiguousTo.size() > 0);
        print(result);
    }

    protected final void queryTripSlowWalk(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final TripOptions options = new TripOptions(Product.ALL, null, WalkSpeed.SLOW, Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                toResult.getLocations().get(0), new Date(), true, options);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);
    }

    protected final void queryTripFastWalk(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final TripOptions options = new TripOptions(Product.ALL, null, WalkSpeed.FAST, Accessibility.NEUTRAL, null);
        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                toResult.getLocations().get(0), new Date(), true, options);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);
    }

    protected final void queryTripFromAdminToPoi(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        Location fromLocation = fromResult.getLocations().get(0);
        assertEquals(fromLocation.type, LocationType.POI);
        print(fromResult);

        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);
        Location toLocation = toResult.getLocations().get(0);
        assertEquals(toLocation.type, LocationType.POI);
        print(toResult);

        final QueryTripsResult tripsResult = queryTrips(fromLocation, null, toLocation, new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, tripsResult.status);
        print(tripsResult);
    }

    protected final void queryMoreTrips(final CharSequence from, final CharSequence to) throws IOException {
        final SuggestLocationsResult fromResult = suggestLocations(from);
        assertTrue(fromResult.getLocations().size() > 0);
        final SuggestLocationsResult toResult = suggestLocations(to);
        assertTrue(toResult.getLocations().size() > 0);

        final QueryTripsResult result = queryTrips(fromResult.getLocations().get(0), null,
                toResult.getLocations().get(0), new Date(), true, null);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        final QueryTripsContext context = result.context;

        final QueryTripsResult nextResult = queryMoreTrips(context, true);
        assertEquals(QueryTripsResult.Status.OK, nextResult.status);
        print(nextResult);

        final QueryTripsResult prevResult = queryMoreTrips(context, false);
        assertEquals(QueryTripsResult.Status.OK, prevResult.status);
        print(prevResult);
    }
}
