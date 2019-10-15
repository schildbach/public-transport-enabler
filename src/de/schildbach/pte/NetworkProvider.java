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

package de.schildbach.pte;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

/**
 * Interface to be implemented by providers of transportation networks.
 * 
 * @author Andreas Schildbach
 */
public interface NetworkProvider {
    public enum Capability {
        /* can suggest locations */
        SUGGEST_LOCATIONS,
        /* can determine nearby locations */
        NEARBY_LOCATIONS,
        /* can query for departures */
        DEPARTURES,
        /* can query trips */
        TRIPS
    }

    public enum Optimize {
        LEAST_DURATION, LEAST_CHANGES, LEAST_WALKING
    }

    public enum WalkSpeed {
        SLOW, NORMAL, FAST
    }

    public enum Accessibility {
        NEUTRAL, LIMITED, BARRIER_FREE
    }

    public enum TripFlag {
        BIKE
    }

    NetworkId id();

    boolean hasCapabilities(final Capability... capabilities);

    /**
     * Find locations near to given location. At least one of lat/lon pair or station id must be present in
     * that location.
     * 
     * @param types
     *            types of locations to find
     * @param location
     *            location to determine nearby stations
     * @param maxDistance
     *            maximum distance in meters, or {@code 0}
     * @param maxLocations
     *            maximum number of locations, or {@code 0}
     * @return nearby stations
     * @throws IOException
     */
    NearbyLocationsResult queryNearbyLocations(Set<LocationType> types, Location location, int maxDistance,
            int maxLocations) throws IOException;

    /**
     * Get departures at a given station, probably live
     * 
     * @param stationId
     *            id of the station
     * @param time
     *            desired time for departing, or {@code null} for the provider default
     * @param maxDepartures
     *            maximum number of departures to get or {@code 0}
     * @param equivs
     *            also query equivalent stations?
     * @return result object containing the departures
     * @throws IOException
     */
    QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs)
            throws IOException;

    /**
     * Meant for auto-completion of location names, like in an Android AutoCompleteTextView.
     * 
     * @param constraint
     *            input by user so far
     * @param types
     *            types of locations to suggest, or {@code null} for any
     * @param maxLocations
     *            maximum number of locations to suggest or {@code 0}
     * @return location suggestions
     * @throws IOException
     */
    SuggestLocationsResult suggestLocations(CharSequence constraint, @Nullable Set<LocationType> types,
            int maxLocations) throws IOException;

    @Deprecated
    SuggestLocationsResult suggestLocations(CharSequence constraint) throws IOException;

    /**
     * Typical products for a network
     * 
     * @return products
     */
    Set<Product> defaultProducts();

    /**
     * Query trips, asking for any ambiguousnesses
     * 
     * @param from
     *            location to route from, mandatory
     * @param via
     *            location to route via, may be {@code null}
     * @param to
     *            location to route to, mandatory
     * @param date
     *            desired date for departing, mandatory
     * @param dep
     *            date is departure date? {@code true} for departure, {@code false} for arrival
     * @param options
     *            additional trip options such as products, optimize, walkSpeed and accessibility, or
     *            {@code null} for the provider default
     * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible
     *         trips
     * @throws IOException
     */
    QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable TripOptions options) throws IOException;

    @Deprecated
    QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
            @Nullable Accessibility accessibility, @Nullable Set<TripFlag> flags) throws IOException;

    /**
     * Query more trips (e.g. earlier or later)
     * 
     * @param context
     *            context to query more trips from
     * @param later
     *            {@code true} to get later trips, {@code false} to get earlier trips
     * @return result object that contains possible trips
     * @throws IOException
     */
    QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException;

    /**
     * Get style of line
     * 
     * @param network
     *            network to disambiguate line, may be {@code null}
     * @param product
     *            line product to get style of, may be {@code null}
     * @param label
     *            line label to get style of, may be {@code null}
     * @return object containing background, foreground and optional border colors
     */
    Style lineStyle(@Nullable String network, @Nullable Product product, @Nullable String label);

    /**
     * Gets the primary covered area of the network
     * 
     * @return array containing points of a polygon (special case: just one coordinate defines just a center
     *         point)
     * @throws IOException
     */
    Point[] getArea() throws IOException;
}
