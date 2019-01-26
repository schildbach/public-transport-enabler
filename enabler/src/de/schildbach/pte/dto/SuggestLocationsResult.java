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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class SuggestLocationsResult implements Serializable {
    public enum Status {
        OK, SERVICE_DOWN
    }

    public final @Nullable ResultHeader header;
    public final Status status;
    public final List<SuggestedLocation> suggestedLocations;

    public SuggestLocationsResult(final ResultHeader header, final List<SuggestedLocation> suggestedLocations) {
        this.header = header;
        this.status = Status.OK;
        this.suggestedLocations = new LinkedList<>(suggestedLocations);
        Collections.sort(this.suggestedLocations);
    }

    public SuggestLocationsResult(final ResultHeader header, final Status status) {
        this.header = header;
        this.status = checkNotNull(status);
        this.suggestedLocations = null;
    }

    public List<Location> getLocations() {
        checkState(status == Status.OK, "no locations with status: {}", status);
        final List<Location> locations = new ArrayList<>(suggestedLocations.size());
        for (final SuggestedLocation location : suggestedLocations)
            locations.add(location.location);
        return locations;
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(status);
        if (suggestedLocations != null)
            helper.add("size", suggestedLocations.size()).add("suggestedLocations", suggestedLocations);
        return helper.toString();
    }

    public String toShortString() {
        if (status == Status.OK)
            return suggestedLocations.size() + " locations";
        else
            return status.toString();
    }
}
