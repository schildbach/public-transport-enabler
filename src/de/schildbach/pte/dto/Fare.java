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
import java.util.Currency;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Fare implements Serializable {
    public enum Type {
        ADULT, CHILD, YOUTH, STUDENT, MILITARY, SENIOR, DISABLED, BIKE
    }

    private static final long serialVersionUID = -6136489996930976421L;

    public final String name;
    public final Type type;
    public final Currency currency;
    public final float fare;
    public final @Nullable String unitName;
    public final @Nullable String units;

    public Fare(final String name, final Type type, final Currency currency, final float fare, final String unitName,
            final String units) {
        this.name = checkNotNull(name);
        this.type = checkNotNull(type);
        this.currency = checkNotNull(currency);
        this.fare = fare;
        this.unitName = unitName;
        this.units = units;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Fare))
            return false;
        final Fare other = (Fare) o;
        if (!Objects.equal(this.name, other.name))
            return false;
        if (!Objects.equal(this.type, other.type))
            return false;
        if (!Objects.equal(this.currency, other.currency))
            return false;
        if (this.fare != other.fare)
            return false;
        if (!Objects.equal(this.unitName, other.unitName))
            return false;
        if (!Objects.equal(this.units, other.units))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type, currency, fare, unitName, units);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(name).addValue(type).addValue(currency).addValue(fare)
                .addValue(unitName).addValue(units).toString();
    }
}
