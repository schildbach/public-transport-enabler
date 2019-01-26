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

package de.schildbach.pte.dto;

import java.io.Serializable;
import java.util.Locale;

import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Point implements Serializable {
    private static final long serialVersionUID = -256077054671402897L;

    private final double lat, lon;

    private Point(final double lat, final double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public static Point fromDouble(final double lat, final double lon) {
        return new Point(lat, lon);
    }

    public static Point from1E6(final int lat, final int lon) {
        return new Point(lat / 1E6, lon / 1E6);
    }

    public static Point from1E5(final int lat, final int lon) {
        return new Point(lat / 1E5, lon / 1E5);
    }

    public double getLatAsDouble() {
        return lat;
    }

    public double getLonAsDouble() {
        return lon;
    }

    public int getLatAs1E6() {
        return (int) Math.round(lat * 1E6);
    }

    public int getLonAs1E6() {
        return (int) Math.round(lon * 1E6);
    }

    public int getLatAs1E5() {
        return (int) Math.round(lat * 1E5);
    }

    public int getLonAs1E5() {
        return (int) Math.round(lon * 1E5);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Point))
            return false;
        final Point other = (Point) o;
        if (this.lat != other.lat)
            return false;
        if (this.lon != other.lon)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lat, lon);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%.7f/%.7f", lat, lon);
    }
}
