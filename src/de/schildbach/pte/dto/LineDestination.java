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

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class LineDestination implements Serializable {
    final public Line line;
    final public @Nullable Location destination;

    public LineDestination(final Line line, final Location destination) {
        this.line = requireNonNull(line);
        this.destination = destination;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LineDestination))
            return false;
        final LineDestination other = (LineDestination) o;
        if (!Objects.equals(this.line, other.line))
            return false;
        // This workaround is necessary because in rare cases destinations have IDs of other locations.
        final String thisDestinationName = this.destination != null ? this.destination.uniqueShortName() : null;
        final String otherDestinationName = other.destination != null ? other.destination.uniqueShortName() : null;
        if (!Objects.equals(thisDestinationName, otherDestinationName))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        // This workaround is necessary because in rare cases destinations have IDs of other locations.
        final String destinationName = destination != null ? destination.uniqueShortName() : null;
        return Objects.hash(line, destinationName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "line=" + line +
                (destination != null ? ",destination=" + destination : "") +
                "}";
    }
}
