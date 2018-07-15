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
import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class StationDepartures implements Serializable {
    public final Location location;
    public final List<Departure> departures;
    public final @Nullable List<LineDestination> lines;

    public StationDepartures(final Location location, final List<Departure> departures,
            final List<LineDestination> lines) {
        this.location = checkNotNull(location);
        this.departures = checkNotNull(departures);
        this.lines = lines;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StationDepartures))
            return false;
        final StationDepartures other = (StationDepartures) o;
        if (!Objects.equal(this.location, other.location))
            return false;
        if (!Objects.equal(this.departures, other.departures))
            return false;
        if (!Objects.equal(this.lines, other.lines))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location, departures, lines);
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(location);
        if (departures != null)
            helper.add("size", departures.size()).add("departures", departures);
        return helper.toString();
    }
}
