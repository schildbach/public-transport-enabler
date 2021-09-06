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

package de.schildbach.pte;

import com.google.common.base.Strings;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;
import okhttp3.HttpUrl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Andreas Schildbach
 */
public class VvmProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://mobile.defas-fgi.de/vvmapp/");

    private static final String DEPARTURE_MONITOR_ENDPOINT = "XML_DM_REQUEST";
    private static final String TRIP_ENDPOINT = "XML_TRIP_REQUEST2";

    public VvmProvider() {
        super(NetworkId.VVM, API_BASE, DEPARTURE_MONITOR_ENDPOINT, TRIP_ENDPOINT, null, null);

        setNeedsSpEncId(true);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
                                                      final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasCoord())
            return mobileCoordRequest(types, location.coord, maxDistance, maxLocations);

        if (location.type != LocationType.STATION)
            throw new IllegalArgumentException("cannot handle: " + location.type);

        throw new IllegalArgumentException("station"); // TODO
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
                                                 final int maxDepartures, final boolean equivs) throws IOException {
        checkNotNull(Strings.emptyToNull(stationId));

        return queryDeparturesMobile(stationId, time, maxDepartures, equivs);
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
                                                   final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        return mobileStopfinderRequest(constraint, types, maxLocations);
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
                                       final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        return queryTripsMobile(from, via, to, date, dep, options);
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException {
        return queryMoreTripsMobile(contextObj, later);
    }
}
