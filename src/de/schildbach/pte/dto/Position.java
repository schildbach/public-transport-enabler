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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * @author Andreas Schildbach
 */
public final class Position implements Serializable {
    private static final long serialVersionUID = 5800904192562764917L;

    public final String name;
    public final @Nullable String section;

    public Position(final String name) {
        this(name, null);
    }

    public Position(final String name, final String section) {
        this.name = requireNonNull(name);
        // checkArgument(name.length() <= 5, "name too long: %s", name);
        this.section = section;
        checkArgument(section == null || section.length() <= 3, "section too long: %s", section);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Position))
            return false;
        final Position other = (Position) o;
        if (!Objects.equals(this.name, other.name))
            return false;
        if (!Objects.equals(this.section, other.section))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, section);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(name);
        if (section != null)
            builder.append(section);
        return builder.toString();
    }
}
