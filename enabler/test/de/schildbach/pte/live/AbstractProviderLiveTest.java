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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractProviderLiveTest {
    protected final NetworkProvider provider;

    public AbstractProviderLiveTest(final NetworkProvider provider) {
        this.provider = provider;
    }

    protected final void print(final NearbyLocationsResult result) {
        System.out.println(result);

        // for (final Location location : result.locations)
        // System.out.println(location);
    }

    protected final void print(final QueryDeparturesResult result) {
        System.out.println(result);

        // for (final StationDepartures stationDepartures : result.stationDepartures)
        // for (final Departure departure : stationDepartures.departures)
        // System.out.println(departure);
    }

    protected final void print(final SuggestLocationsResult result) {
        System.out.println(result);

        // for (final Location location : result.getLocations())
        // System.out.println(location);
    }

    protected final void print(final QueryTripsResult result) {
        System.out.println(result);

        // for (final Trip trip : result.trips)
        // {
        // System.out.println(trip);
        // for (final Leg leg : trip.legs)
        // System.out.println("- " + leg);
        // }
    }

    protected final NearbyLocationsResult queryNearbyStations(final Location location) throws IOException {
        return queryNearbyLocations(EnumSet.of(LocationType.STATION), location, 0, 5);
    }

    protected final NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location)
            throws IOException {
        return queryNearbyLocations(types, location, 0, 5);
    }

    protected final NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
            final int maxDistance, final int maxStations) throws IOException {
        return provider.queryNearbyLocations(types, location, maxDistance, maxStations);
    }

    protected final QueryDeparturesResult queryDepartures(final String stationId, final boolean equivs)
            throws IOException {
        return queryDepartures(stationId, 5, equivs);
    }

    protected final QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures,
            final boolean equivs) throws IOException {
        final QueryDeparturesResult result = provider.queryDepartures(stationId, new Date(), maxDepartures, equivs);

        if (result.status == QueryDeparturesResult.Status.OK) {
            if (equivs)
                assertTrue(result.stationDepartures.size() > 1);
            else
                assertTrue(result.stationDepartures.size() == 1);
        }

        return result;
    }

    protected final SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException {
        return provider.suggestLocations(constraint, null, 0);
    }

    protected final QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        return provider.queryTrips(from, via, to, date, dep, options);
    }

    protected final QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later)
            throws IOException {
        return provider.queryMoreTrips(context, later);
    }

    protected final static String secretProperty(final String key) {
        try {
            final Properties properties = new Properties();
            final String secretPropertiesFilename = "secrets.properties";
            final InputStream is = AbstractProviderLiveTest.class.getResourceAsStream(secretPropertiesFilename);
            if (is == null)
                throw new IllegalStateException(
                        "Could not find secret property file " + secretPropertiesFilename + " in classpath.");
            properties.load(is);
            final String secret = properties.getProperty(key);
            if (secret == null)
                throw new IllegalStateException(
                        "Could not find secret value for '" + key + "' in " + secretPropertiesFilename + ".");
            return secret;
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }
}
