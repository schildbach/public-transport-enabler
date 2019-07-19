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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Trip implements Serializable {
    private static final long serialVersionUID = 2508466068307110312L;

    private String id;
    public final Location from;
    public final Location to;
    public final List<Leg> legs;
    public final List<Fare> fares;
    public final int[] capacity;
    public final Integer numChanges;

    public Trip(final String id, final Location from, final Location to, final List<Leg> legs, final List<Fare> fares,
            final int[] capacity, final Integer numChanges) {
        this.id = id;
        this.from = checkNotNull(from);
        this.to = checkNotNull(to);
        this.legs = checkNotNull(legs);
        this.fares = fares;
        this.capacity = capacity;
        this.numChanges = numChanges;

        checkArgument(!legs.isEmpty());
    }

    public Date getFirstDepartureTime() {
        return legs.get(0).getDepartureTime();
    }

    public @Nullable Public getFirstPublicLeg() {
        for (final Leg leg : legs)
            if (leg instanceof Public)
                return (Public) leg;

        return null;
    }

    public @Nullable Date getFirstPublicLegDepartureTime() {
        final Public firstPublicLeg = getFirstPublicLeg();
        if (firstPublicLeg != null)
            return firstPublicLeg.getDepartureTime();
        else
            return null;
    }

    public Date getLastArrivalTime() {
        return legs.get(legs.size() - 1).getArrivalTime();
    }

    public @Nullable Public getLastPublicLeg() {
        for (int i = legs.size() - 1; i >= 0; i--) {
            final Leg leg = legs.get(i);
            if (leg instanceof Public)
                return (Public) leg;
        }

        return null;
    }

    public @Nullable Date getLastPublicLegArrivalTime() {
        final Public lastPublicLeg = getLastPublicLeg();
        if (lastPublicLeg != null)
            return lastPublicLeg.getArrivalTime();
        else
            return null;
    }

    /**
     * Duration of whole trip in milliseconds, including leading and trailing individual legs.
     * 
     * @return duration in ms
     */
    public long getDuration() {
        final Date first = getFirstDepartureTime();
        final Date last = getLastArrivalTime();
        return last.getTime() - first.getTime();
    }

    /**
     * Duration of the public leg part in milliseconds. This includes individual legs between public legs, but
     * excludes individual legs that lead or trail the trip.
     * 
     * @return duration in ms, or null if there are no public legs
     */
    public @Nullable Long getPublicDuration() {
        final Date first = getFirstPublicLegDepartureTime();
        final Date last = getLastPublicLegArrivalTime();
        if (first != null && last != null)
            return last.getTime() - first.getTime();
        else
            return null;
    }

    /** Minimum time occurring in this trip. */
    public Date getMinTime() {
        Date minTime = null;

        for (final Leg leg : legs)
            if (minTime == null || leg.getMinTime().before(minTime))
                minTime = leg.getMinTime();

        return minTime;
    }

    /** Maximum time occurring in this trip. */
    public Date getMaxTime() {
        Date maxTime = null;

        for (final Leg leg : legs)
            if (maxTime == null || leg.getMaxTime().after(maxTime))
                maxTime = leg.getMaxTime();

        return maxTime;
    }

    /**
     * <p>
     * Number of changes on the trip.
     * </p>
     * 
     * <p>
     * Returns {@link #numChanges} if it isn't null. Otherwise, it tries to compute the number by counting
     * public legs of the trip. The number of changes for a Trip consisting of only individual Legs is null.
     * </p>
     *
     * @return number of changes on the trip, or null if no public legs are involved
     */
    @Nullable
    public Integer getNumChanges() {
        if (numChanges == null) {
            Integer numCount = null;

            for (final Leg leg : legs) {
                if (leg instanceof Public) {
                    if (numCount == null) {
                        numCount = 0;
                    } else {
                        numCount++;
                    }
                }
            }
            return numCount;
        } else {
            return numChanges;
        }
    }

    /**
     * Returns true if it looks like the trip can be traveled. Returns false if legs overlap, important
     * departures or arrivals are cancelled and that sort of thing.
     */
    public boolean isTravelable() {
        Date time = null;

        for (final Leg leg : legs) {
            if (leg instanceof Public) {
                final Public publicLeg = (Public) leg;
                if (publicLeg.departureStop.departureCancelled || publicLeg.arrivalStop.arrivalCancelled)
                    return false;
            }

            final Date departureTime = leg.getDepartureTime();
            if (time != null && departureTime.before(time))
                return false;
            time = departureTime;

            final Date arrivalTime = leg.getArrivalTime();
            if (time != null && arrivalTime.before(time))
                return false;
            time = arrivalTime;
        }

        return true;
    }

    /** If an individual leg overlaps, try to adjust so that it doesn't. */
    public void adjustUntravelableIndividualLegs() {
        final int numLegs = legs.size();
        if (numLegs < 1)
            return;

        for (int i = 1; i < numLegs; i++) {
            final Trip.Leg leg = legs.get(i);

            if (leg instanceof Trip.Individual) {
                final Trip.Leg previous = legs.get(i - 1);

                if (leg.getDepartureTime().before(previous.getArrivalTime()))
                    legs.set(i, ((Trip.Individual) leg).movedClone(previous.getArrivalTime()));
            }
        }
    }

    public Set<Product> products() {
        final Set<Product> products = EnumSet.noneOf(Product.class);

        for (final Leg leg : legs)
            if (leg instanceof Public)
                products.add(((Public) leg).line.product);

        return products;
    }

    public String getId() {
        if (id == null)
            id = buildSubstituteId();

        return id;
    }

    private String buildSubstituteId() {
        final StringBuilder builder = new StringBuilder();

        for (final Leg leg : legs) {
            builder.append(leg.departure.hasId() ? leg.departure.id : leg.departure.coord).append('-');
            builder.append(leg.arrival.hasId() ? leg.arrival.id : leg.arrival.coord).append('-');

            if (leg instanceof Individual) {
                builder.append("individual");
            } else if (leg instanceof Public) {
                final Public publicLeg = (Public) leg;
                final Date plannedDepartureTime = publicLeg.departureStop.plannedDepartureTime;
                if (plannedDepartureTime != null)
                    builder.append(plannedDepartureTime.getTime()).append('-');
                final Date plannedArrivalTime = publicLeg.arrivalStop.plannedArrivalTime;
                if (plannedArrivalTime != null)
                    builder.append(plannedArrivalTime.getTime()).append('-');
                final Line line = publicLeg.line;
                builder.append(line.productCode());
                builder.append(line.label);
            }

            builder.append('|');
        }

        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Trip))
            return false;
        final Trip other = (Trip) o;
        return Objects.equal(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(getId());
        final Date firstPublicLegDepartureTime = getFirstPublicLegDepartureTime();
        final Date lastPublicLegArrivalTime = getLastPublicLegArrivalTime();
        helper.addValue(
                firstPublicLegDepartureTime != null ? String.format(Locale.US, "%ta %<tR", firstPublicLegDepartureTime)
                        : "null" + '-' + lastPublicLegArrivalTime != null
                                ? String.format(Locale.US, "%ta %<tR", lastPublicLegArrivalTime) : "null");
        helper.add("numChanges", numChanges);
        return helper.toString();
    }

    public abstract static class Leg implements Serializable {
        private static final long serialVersionUID = 8498461220084523265L;

        public final Location departure;
        public final Location arrival;
        public transient List<Point> path; // custom serialization, to save space

        public Leg(final Location departure, final Location arrival, final List<Point> path) {
            this.departure = checkNotNull(departure);
            this.arrival = checkNotNull(arrival);
            this.path = path;
        }

        /** Coarse departure time. */
        public abstract Date getDepartureTime();

        /** Coarse arrival time. */
        public abstract Date getArrivalTime();

        /** Minimum time occurring in this leg. */
        public abstract Date getMinTime();

        /** Maximum time occurring in this leg. */
        public abstract Date getMaxTime();

        private void writeObject(final ObjectOutputStream os) throws IOException {
            os.defaultWriteObject();
            if (path != null) {
                os.writeInt(path.size());
                for (final Point p : path) {
                    os.writeInt(p.getLatAs1E6());
                    os.writeInt(p.getLonAs1E6());
                }
            } else {
                os.writeInt(-1);
            }
        }

        private void readObject(final ObjectInputStream is) throws ClassNotFoundException, IOException {
            is.defaultReadObject();
            try {
                final int pathSize = is.readInt();
                if (pathSize >= 0) {
                    path = new ArrayList<>(pathSize);
                    for (int i = 0; i < pathSize; i++)
                        path.add(Point.from1E6(is.readInt(), is.readInt()));
                } else {
                    path = null;
                }
            } catch (final EOFException x) {
                path = null;
            }
        }
    }

    public final static class Public extends Leg {
        private static final long serialVersionUID = 1312066446239817422L;

        public final Line line;
        public final @Nullable Location destination;
        public final Stop departureStop;
        public final Stop arrivalStop;
        public final @Nullable List<Stop> intermediateStops;
        public final @Nullable String message;

        public Public(final Line line, final Location destination, final Stop departureStop, final Stop arrivalStop,
                final List<Stop> intermediateStops, final List<Point> path, final String message) {
            super(departureStop.location, arrivalStop.location, path);

            this.line = checkNotNull(line);
            this.destination = destination;
            this.departureStop = checkNotNull(departureStop);
            this.arrivalStop = checkNotNull(arrivalStop);
            this.intermediateStops = intermediateStops;
            this.message = message;

            checkNotNull(departureStop.getDepartureTime());
            checkNotNull(arrivalStop.getArrivalTime());
        }

        @Override
        public Date getDepartureTime() {
            return departureStop.getDepartureTime(false);
        }

        public Date getDepartureTime(final boolean preferPlanTime) {
            return departureStop.getDepartureTime(preferPlanTime);
        }

        public boolean isDepartureTimePredicted() {
            return departureStop.isDepartureTimePredicted(false);
        }

        public Long getDepartureDelay() {
            return departureStop.getDepartureDelay();
        }

        public Position getDeparturePosition() {
            return departureStop.getDeparturePosition();
        }

        public boolean isDeparturePositionPredicted() {
            return departureStop.isDeparturePositionPredicted();
        }

        @Override
        public Date getArrivalTime() {
            return arrivalStop.getArrivalTime(false);
        }

        public Date getArrivalTime(final boolean preferPlanTime) {
            return arrivalStop.getArrivalTime(preferPlanTime);
        }

        public boolean isArrivalTimePredicted() {
            return arrivalStop.isArrivalTimePredicted(false);
        }

        public Long getArrivalDelay() {
            return arrivalStop.getArrivalDelay();
        }

        public Position getArrivalPosition() {
            return arrivalStop.getArrivalPosition();
        }

        public boolean isArrivalPositionPredicted() {
            return arrivalStop.isArrivalPositionPredicted();
        }

        @Override
        public Date getMinTime() {
            return departureStop.getMinTime();
        }

        @Override
        public Date getMaxTime() {
            return arrivalStop.getMaxTime();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("line", line).add("destination", destination)
                    .add("departureStop", departureStop).add("arrivalStop", arrivalStop).omitNullValues().toString();
        }
    }

    public final static class Individual extends Leg {
        public enum Type {
            WALK, BIKE, CAR, TRANSFER
        }

        private static final long serialVersionUID = -6651381862837233925L;

        public final Type type;
        public final Date departureTime;
        public final Date arrivalTime;
        public final int min;
        public final int distance;

        public Individual(final Type type, final Location departure, final Date departureTime, final Location arrival,
                final Date arrivalTime, final List<Point> path, final int distance) {
            super(departure, arrival, path);

            this.type = checkNotNull(type);
            this.departureTime = checkNotNull(departureTime);
            this.arrivalTime = checkNotNull(arrivalTime);
            this.min = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);
            this.distance = distance;
        }

        public Individual movedClone(final Date departureTime) {
            final Date arrivalTime = new Date(
                    departureTime.getTime() + this.arrivalTime.getTime() - this.departureTime.getTime());
            return new Trip.Individual(this.type, this.departure, departureTime, this.arrival, arrivalTime, this.path,
                    this.distance);
        }

        @Override
        public Date getDepartureTime() {
            return departureTime;
        }

        @Override
        public Date getArrivalTime() {
            return arrivalTime;
        }

        @Override
        public Date getMinTime() {
            return departureTime;
        }

        @Override
        public Date getMaxTime() {
            return arrivalTime;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).addValue(type).add("departure", departure).add("arrival", arrival)
                    .add("min", min).add("distance", distance).omitNullValues().toString();
        }
    }
}
