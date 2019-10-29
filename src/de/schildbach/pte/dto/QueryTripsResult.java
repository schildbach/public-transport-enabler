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

package de.schildbach.pte.dto;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class QueryTripsResult implements Serializable {
    public enum Status {
        OK, AMBIGUOUS, TOO_CLOSE, UNKNOWN_FROM, UNKNOWN_VIA, UNKNOWN_TO, UNKNOWN_LOCATION, UNRESOLVABLE_ADDRESS, NO_TRIPS, INVALID_DATE, SERVICE_DOWN
    }

    public final @Nullable ResultHeader header;
    public final Status status;

    public final List<Location> ambiguousFrom;
    public final List<Location> ambiguousVia;
    public final List<Location> ambiguousTo;

    public final String queryUri;
    public final Location from;
    public final Location via;
    public final Location to;
    public final QueryTripsContext context;
    public final List<Trip> trips;

    public QueryTripsResult(final ResultHeader header, final String queryUri, final Location from, final Location via,
            final Location to, final QueryTripsContext context, final List<Trip> trips) {
        this.header = header;
        this.status = Status.OK;
        this.queryUri = queryUri;
        this.from = from;
        this.via = via;
        this.to = to;
        this.context = checkNotNull(context);
        this.trips = checkNotNull(trips);

        this.ambiguousFrom = null;
        this.ambiguousVia = null;
        this.ambiguousTo = null;
    }

    public QueryTripsResult(final ResultHeader header, final List<Location> ambiguousFrom,
            final List<Location> ambiguousVia, final List<Location> ambiguousTo) {
        this.header = header;
        this.status = Status.AMBIGUOUS;
        this.ambiguousFrom = ambiguousFrom;
        this.ambiguousVia = ambiguousVia;
        this.ambiguousTo = ambiguousTo;

        this.queryUri = null;
        this.from = null;
        this.via = null;
        this.to = null;
        this.context = null;
        this.trips = null;
    }

    public QueryTripsResult(final ResultHeader header, final Status status) {
        this.header = header;
        this.status = checkNotNull(status);

        this.ambiguousFrom = null;
        this.ambiguousVia = null;
        this.ambiguousTo = null;
        this.queryUri = null;
        this.from = null;
        this.via = null;
        this.to = null;
        this.context = null;
        this.trips = null;
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(status);
        if (status == Status.OK) {
            if (trips != null)
                helper.add("size", trips.size()).add("trips", trips);
        } else if (status == Status.AMBIGUOUS) {
            if (ambiguousFrom != null)
                helper.add("size", ambiguousFrom.size()).add("ambiguousFrom", ambiguousFrom);
            if (ambiguousVia != null)
                helper.add("size", ambiguousVia.size()).add("ambiguousVia", ambiguousVia);
            if (ambiguousTo != null)
                helper.add("size", ambiguousTo.size()).add("ambiguousTo", ambiguousTo);
        }
        return helper.toString();
    }

    public String toShortString() {
        if (status == Status.OK)
            return trips.size() + " trips" + (from != null ? " from " + from : "") + (via != null ? " via " + via : "")
                    + (to != null ? " to " + to : "");
        else
            return status.toString();
    }
}
