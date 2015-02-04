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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Trip implements Serializable
{
	private static final long serialVersionUID = 2508466068307110312L;

	private String id;
	public final Location from;
	public final Location to;
	public final List<Leg> legs;
	public final List<Fare> fares;
	public final int[] capacity;
	public final Integer numChanges;

	public Trip(final String id, final Location from, final Location to, final List<Leg> legs, final List<Fare> fares, final int[] capacity,
			final Integer numChanges)
	{
		this.id = id;
		this.from = from;
		this.to = to;
		this.legs = legs;
		this.fares = fares;
		this.capacity = capacity;
		this.numChanges = numChanges;
	}

	public Date getFirstDepartureTime()
	{
		if (legs != null && !legs.isEmpty())
			return legs.get(0).getDepartureTime();
		else
			return null;
	}

	public Public getFirstPublicLeg()
	{
		if (legs != null)
			for (final Leg leg : legs)
				if (leg instanceof Public)
					return (Public) leg;

		return null;
	}

	public Date getFirstPublicLegDepartureTime()
	{
		final Public firstPublicLeg = getFirstPublicLeg();
		if (firstPublicLeg != null)
			return firstPublicLeg.getDepartureTime();
		else
			return null;
	}

	public Date getLastArrivalTime()
	{
		if (legs != null && !legs.isEmpty())
			return legs.get(legs.size() - 1).getArrivalTime();
		else
			return null;
	}

	public Public getLastPublicLeg()
	{
		if (legs != null)
		{
			for (int i = legs.size() - 1; i >= 0; i--)
			{
				final Leg leg = legs.get(i);
				if (leg instanceof Public)
					return (Public) leg;
			}
		}

		return null;
	}

	public Date getLastPublicLegArrivalTime()
	{
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
	public long getDuration()
	{
		final Date first = getFirstDepartureTime();
		final Date last = getLastArrivalTime();
		return last.getTime() - first.getTime();
	}

	/**
	 * Duration of the public leg part in milliseconds. This includes individual legs between public legs, but excludes
	 * individual legs that lead or trail the trip.
	 * 
	 * @return duration in ms, or null if there are no public legs
	 */
	public Long getPublicDuration()
	{
		final Date first = getFirstPublicLegDepartureTime();
		final Date last = getLastPublicLegArrivalTime();
		if (first != null && last != null)
			return last.getTime() - first.getTime();
		else
			return null;
	}

	/** Minimum time occuring in this trip. */
	public Date getMinTime()
	{
		Date minTime = null;

		for (final Leg leg : legs)
			if (minTime == null || leg.getMinTime().before(minTime))
				minTime = leg.getMinTime();

		return minTime;
	}

	/** Maximum time occuring in this trip. */
	public Date getMaxTime()
	{
		Date maxTime = null;

		for (final Leg leg : legs)
			if (maxTime == null || leg.getMaxTime().after(maxTime))
				maxTime = leg.getMaxTime();

		return maxTime;
	}

	/** Returns true if no legs overlap, false otherwise. */
	public boolean isTravelable()
	{
		Date time = null;

		for (final Leg leg : legs)
		{
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

	public List<Product> products()
	{
		final List<Product> products = new LinkedList<Product>();

		if (legs != null)
		{
			for (final Leg leg : legs)
			{
				if (leg instanceof Public)
				{
					final Product product = Product.fromCode(((Public) leg).line.label.charAt(0));
					if (!products.contains(product))
						products.add(product);
				}
			}
		}

		return products;
	}

	public String getId()
	{
		if (id == null)
			id = buildSubstituteId();

		return id;
	}

	private String buildSubstituteId()
	{
		final StringBuilder builder = new StringBuilder();

		if (legs != null && legs.size() > 0)
		{
			for (final Leg leg : legs)
			{
				builder.append(leg.departure.hasId() ? leg.departure.id : leg.departure.lat + '/' + leg.departure.lon).append('-');
				builder.append(leg.arrival.hasId() ? leg.arrival.id : leg.arrival.lat + '/' + leg.arrival.lon).append('-');

				if (leg instanceof Individual)
				{
					builder.append(((Individual) leg).min);
				}
				else if (leg instanceof Public)
				{
					final Public publicLeg = (Public) leg;
					builder.append(publicLeg.departureStop.plannedDepartureTime.getTime()).append('-');
					builder.append(publicLeg.arrivalStop.plannedArrivalTime.getTime()).append('-');
					builder.append(publicLeg.line.label);
				}

				builder.append('|');
			}

			builder.setLength(builder.length() - 1);
		}

		return builder.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Trip))
			return false;
		final Trip other = (Trip) o;
		return Objects.equal(this.getId(), other.getId());
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(getId());
	}

	@Override
	public String toString()
	{
		final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(getId());
		final Date firstPublicLegDepartureTime = getFirstPublicLegDepartureTime();
		final Date lastPublicLegArrivalTime = getLastPublicLegArrivalTime();
		helper.addValue(firstPublicLegDepartureTime != null ? String.format(Locale.US, "%ta %<tR", firstPublicLegDepartureTime) : "null" + '-'
				+ lastPublicLegArrivalTime != null ? String.format(Locale.US, "%ta %<tR", lastPublicLegArrivalTime) : "null");
		helper.add("numChanges", numChanges);
		return helper.toString();
	}

	public abstract static class Leg implements Serializable
	{
		private static final long serialVersionUID = 8498461220084523265L;

		public final Location departure;
		public final Location arrival;
		public List<Point> path;

		public Leg(final Location departure, final Location arrival, final List<Point> path)
		{
			this.departure = departure;
			this.arrival = arrival;
			this.path = path;
		}

		/** Coarse departure time. */
		public abstract Date getDepartureTime();

		/** Coarse arrival time. */
		public abstract Date getArrivalTime();

		/** Minimum time occuring in this leg. */
		public abstract Date getMinTime();

		/** Maximum time occuring in this leg. */
		public abstract Date getMaxTime();
	}

	public final static class Public extends Leg
	{
		private static final long serialVersionUID = 1312066446239817422L;

		public final Line line;
		public final Location destination;
		public final Stop departureStop;
		public final Stop arrivalStop;
		public final List<Stop> intermediateStops;
		public final String message;

		public Public(final Line line, final Location destination, final Stop departureStop, final Stop arrivalStop,
				final List<Stop> intermediateStops, final List<Point> path, final String message)
		{
			super(departureStop.location, arrivalStop.location, path);

			this.line = line;
			this.destination = destination;
			this.departureStop = departureStop;
			this.arrivalStop = arrivalStop;
			this.intermediateStops = intermediateStops;
			this.message = message;
		}

		@Override
		public Date getDepartureTime()
		{
			final Date departureTime = departureStop.getDepartureTime();

			if (departureTime == null)
				throw new IllegalStateException();

			return departureTime;
		}

		public boolean isDepartureTimePredicted()
		{
			return departureStop.isDepartureTimePredicted();
		}

		public Long getDepartureDelay()
		{
			return departureStop.getDepartureDelay();
		}

		public Position getDeparturePosition()
		{
			return departureStop.getDeparturePosition();
		}

		public boolean isDeparturePositionPredicted()
		{
			return departureStop.isDeparturePositionPredicted();
		}

		@Override
		public Date getArrivalTime()
		{
			final Date arrivalTime = arrivalStop.getArrivalTime();

			if (arrivalTime == null)
				throw new IllegalStateException();

			return arrivalTime;
		}

		public boolean isArrivalTimePredicted()
		{
			return arrivalStop.isArrivalTimePredicted();
		}

		public Long getArrivalDelay()
		{
			return arrivalStop.getArrivalDelay();
		}

		public Position getArrivalPosition()
		{
			return arrivalStop.getArrivalPosition();
		}

		public boolean isArrivalPositionPredicted()
		{
			return arrivalStop.isArrivalPositionPredicted();
		}

		@Override
		public Date getMinTime()
		{
			return departureStop.getMinTime();
		}

		@Override
		public Date getMaxTime()
		{
			return arrivalStop.getMaxTime();
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this).add("line", line).add("destination", destination).add("departureStop", departureStop)
					.add("arrivalStop", arrivalStop).omitNullValues().toString();
		}
	}

	public final static class Individual extends Leg
	{
		public enum Type
		{
			WALK, BIKE, CAR, TRANSFER
		}

		private static final long serialVersionUID = -6651381862837233925L;

		public final Type type;
		public final Date departureTime;
		public final Date arrivalTime;
		public final int min;
		public final int distance;

		public Individual(final Type type, final Location departure, final Date departureTime, final Location arrival, final Date arrivalTime,
				final List<Point> path, final int distance)
		{
			super(departure, arrival, path);

			this.type = type;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;

			if (arrivalTime != null && departureTime != null)
				this.min = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);
			else
				this.min = 0;

			this.distance = distance;
		}

		@Override
		public Date getDepartureTime()
		{
			return departureTime;
		}

		@Override
		public Date getArrivalTime()
		{
			return arrivalTime;
		}

		@Override
		public Date getMinTime()
		{
			return departureTime;
		}

		@Override
		public Date getMaxTime()
		{
			return arrivalTime;
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this).addValue(type).add("departure", departure).add("arrival", arrival).add("min", min)
					.add("distance", distance).omitNullValues().toString();
		}
	}
}
