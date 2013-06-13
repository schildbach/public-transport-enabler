/*
 * Copyright 2010-2013 the original author or authors.
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
			return legs.get(0).departureTime;
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
			return legs.get(legs.size() - 1).arrivalTime;
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
	public String toString()
	{
		final SimpleDateFormat FORMAT = new SimpleDateFormat("E HH:mm", Locale.US);

		final StringBuilder str = new StringBuilder(getId());
		str.append(' ');
		final Date firstPublicLegDepartureTime = getFirstPublicLegDepartureTime();
		str.append(firstPublicLegDepartureTime != null ? FORMAT.format(firstPublicLegDepartureTime) : "null");
		str.append('-');
		final Date lastPublicLegArrivalTime = getLastPublicLegArrivalTime();
		str.append(lastPublicLegArrivalTime != null ? FORMAT.format(lastPublicLegArrivalTime) : "null");
		str.append(' ').append(numChanges).append("ch");

		return str.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Trip))
			return false;
		final Trip other = (Trip) o;
		return getId().equals(other.getId());
	}

	@Override
	public int hashCode()
	{
		return getId().hashCode();
	}

	public static class Leg implements Serializable
	{
		private static final long serialVersionUID = 8498461220084523265L;

		public final Location departure;
		public final Date departureTime;
		public final Location arrival;
		public final Date arrivalTime;
		public List<Point> path;
		public final int min;

		public Leg(final Location departure, final Date departureTime, final Location arrival, final Date arrivalTime, final List<Point> path)
		{
			this.departure = departure;
			this.departureTime = departureTime;
			this.arrival = arrival;
			this.arrivalTime = arrivalTime;
			this.path = path;

			if (arrivalTime != null && departureTime != null)
				this.min = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);
			else
				this.min = 0;
		}
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
			super(departureStop.location, departureStop.getDepartureTime(), arrivalStop.location, arrivalStop.getArrivalTime(), path);

			this.line = line;
			this.destination = destination;
			this.departureStop = departureStop;
			this.arrivalStop = arrivalStop;
			this.intermediateStops = intermediateStops;
			this.message = message;
		}

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

		public String getDeparturePosition()
		{
			return departureStop.getDeparturePosition();
		}

		public boolean isDeparturePositionPredicted()
		{
			return departureStop.isDeparturePositionPredicted();
		}

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

		public String getArrivalPosition()
		{
			return arrivalStop.getArrivalPosition();
		}

		public boolean isArrivalPositionPredicted()
		{
			return arrivalStop.isArrivalPositionPredicted();
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("line=").append(line);
			if (destination != null)
			{
				builder.append(",");
				builder.append("destination=").append(destination.toDebugString());
			}
			builder.append(",");
			builder.append("departure=").append(departureStop);
			builder.append(",");
			builder.append("arrival=").append(arrivalStop);
			builder.append("]");
			return builder.toString();
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
		public final int distance;

		public Individual(final Type type, final Location departure, final Date departureTime, final Location arrival, final Date arrivalTime,
				final List<Point> path, final int distance)
		{
			super(departure, departureTime, arrival, arrivalTime, path);

			this.type = type;
			this.distance = distance;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("type=").append(type);
			builder.append(",");
			builder.append("departure=").append(departure.toDebugString());
			builder.append(",");
			builder.append("arrival=").append(arrival.toDebugString());
			builder.append(",");
			builder.append("distance=").append(distance);
			builder.append(",");
			builder.append("min=").append(min);
			builder.append("]");
			return builder.toString();
		}
	}
}
