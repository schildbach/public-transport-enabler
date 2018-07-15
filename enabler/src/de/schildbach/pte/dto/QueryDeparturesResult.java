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
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class QueryDeparturesResult implements Serializable {
    public enum Status {
        OK, INVALID_STATION, SERVICE_DOWN
    }

    public final @Nullable ResultHeader header;
    public final Status status;
    public final List<StationDepartures> stationDepartures = new LinkedList<>();

    public QueryDeparturesResult(final ResultHeader header) {
        this.header = header;
        this.status = Status.OK;
    }

    public QueryDeparturesResult(final ResultHeader header, final Status status) {
        this.header = header;
        this.status = checkNotNull(status);
    }

    public StationDepartures findStationDepartures(final String stationId) {
        for (final StationDepartures departures : stationDepartures) {
            final Location location = departures.location;
            if (location != null && stationId.equals(location.id))
                return departures;
        }

        return null;
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(status);
        if (stationDepartures != null)
            helper.add("size", stationDepartures.size()).add("stationDepartures", stationDepartures);
        return helper.toString();
    }

    public String toShortString() {
        if (status == Status.OK)
            return stationDepartures.size() + " stationDepartures";
        else
            return status.toString();
    }
}
