/*
 * Copyright 2013-2015 the original author or authors.
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

import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class SuggestedLocation implements Serializable, Comparable<SuggestedLocation> {
    public final Location location;
    public final int priority;

    public SuggestedLocation(final Location location, final int priority) {
        this.location = checkNotNull(location);
        this.priority = priority;
    }

    public SuggestedLocation(final Location location) {
        this(location, 0);
    }

    @Override
    public int compareTo(final SuggestedLocation other) {
        // prefer quality
        if (this.priority > other.priority)
            return -1;
        else if (this.priority < other.priority)
            return 1;

        // prefer stations
        final int compareLocationType = this.location.type.compareTo(other.location.type);
        if (compareLocationType != 0)
            return compareLocationType;

        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SuggestedLocation))
            return false;
        final SuggestedLocation other = (SuggestedLocation) o;
        return Objects.equal(this.location, other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public String toString() {
        return priority + ":" + location;
    }
}
