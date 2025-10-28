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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * @author Andreas Schildbach
 */
public final class Departure implements Serializable {
    private static final long serialVersionUID = -9104517779537062795L;

    final public @Nullable Date plannedTime;
    final public @Nullable Date predictedTime;
    final public Line line;
    final public @Nullable Position position;
    final public @Nullable Location destination;
    final public @Nullable int[] capacity;
    final public @Nullable String message;

    public Departure(final Date plannedTime, final Date predictedTime, final Line line, final Position position,
            final Location destination, final int[] capacity, final String message) {
        this.plannedTime = plannedTime;
        this.predictedTime = predictedTime;
        checkArgument(plannedTime != null || predictedTime != null);
        this.line = requireNonNull(line);
        this.position = position;
        this.destination = destination;
        this.capacity = capacity;
        this.message = message;
    }

    public Date getTime() {
        if (predictedTime != null)
            return predictedTime;
        else
            return plannedTime;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                (plannedTime != null ?
                        "planned=" + String.format(Locale.US, "%ta %<tR", plannedTime) + "," : "") +
                (predictedTime != null ?
                        "predicted=" + String.format(Locale.US, "%ta %<tR", predictedTime) + "," : "") +
                (position != null ? position + "," : "") +
                (destination != null ? "destination=" + destination + "," : "") +
                line + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Departure))
            return false;
        final Departure other = (Departure) o;
        if (!Objects.equals(this.plannedTime, other.plannedTime))
            return false;
        if (!Objects.equals(this.predictedTime, other.predictedTime))
            return false;
        if (!Objects.equals(this.line, other.line))
            return false;
        if (!Objects.equals(this.destination, other.destination))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(plannedTime, predictedTime, line, destination);
    }

    public static final Comparator<Departure> TIME_COMPARATOR = (departure0, departure1) -> departure0.getTime().compareTo(departure1.getTime());
}
