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
public final class Connection implements Serializable
{
	private static final long serialVersionUID = 2508466068307110312L;

	private String id;
	public final Location from;
	public final Location to;
	public final List<Part> parts;
	public final List<Fare> fares;
	public final int[] capacity;
	public final Integer numChanges;

	public Connection(final String id, final Location from, final Location to, final List<Part> parts, final List<Fare> fares, final int[] capacity,
			final Integer numChanges)
	{
		this.id = id;
		this.from = from;
		this.to = to;
		this.parts = parts;
		this.fares = fares;
		this.capacity = capacity;
		this.numChanges = numChanges;
	}

	public Date getFirstDepartureTime()
	{
		if (parts != null)
		{
			int mins = 0;
			for (final Part part : parts)
			{
				if (part instanceof Footway)
					mins += ((Footway) part).min;
				else if (part instanceof Trip)
					return new Date(((Trip) part).getDepartureTime().getTime() - 1000 * 60 * mins);
			}
		}

		return null;
	}

	public Trip getFirstTrip()
	{
		if (parts != null)
			for (final Part part : parts)
				if (part instanceof Trip)
					return (Trip) part;

		return null;
	}

	public Date getFirstTripDepartureTime()
	{
		final Trip firstTrip = getFirstTrip();
		if (firstTrip != null)
			return firstTrip.getDepartureTime();
		else
			return null;
	}

	public Date getLastArrivalTime()
	{
		if (parts != null)
		{
			int mins = 0;
			for (int i = parts.size() - 1; i >= 0; i--)
			{
				final Part part = parts.get(i);
				if (part instanceof Footway)
					mins += ((Footway) part).min;
				else if (part instanceof Trip)
					return new Date(((Trip) part).getArrivalTime().getTime() + 1000 * 60 * mins);
			}
		}

		return null;
	}

	public Trip getLastTrip()
	{
		if (parts != null)
		{
			for (int i = parts.size() - 1; i >= 0; i--)
			{
				final Part part = parts.get(i);
				if (part instanceof Trip)
					return (Trip) part;
			}
		}

		return null;
	}

	public Date getLastTripArrivalTime()
	{
		final Trip lastTrip = getLastTrip();
		if (lastTrip != null)
			return lastTrip.getArrivalTime();
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

		if (parts != null && parts.size() > 0)
		{
			for (final Part part : parts)
			{
				builder.append(part.departure.hasId() ? part.departure.id : part.departure.lat + '/' + part.departure.lon).append('-');
				builder.append(part.arrival.hasId() ? part.arrival.id : part.arrival.lat + '/' + part.arrival.lon).append('-');

				if (part instanceof Footway)
				{
					builder.append(((Footway) part).min);
				}
				else if (part instanceof Trip)
				{
					final Trip trip = (Trip) part;
					builder.append(trip.departureStop.plannedDepartureTime.getTime()).append('-');
					builder.append(trip.arrivalStop.plannedArrivalTime.getTime()).append('-');
					builder.append(trip.line.label);
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
		final Date firstTripDepartureTime = getFirstTripDepartureTime();
		str.append(firstTripDepartureTime != null ? FORMAT.format(firstTripDepartureTime) : "null");
		str.append('-');
		final Date lastTripArrivalTime = getLastTripArrivalTime();
		str.append(lastTripArrivalTime != null ? FORMAT.format(lastTripArrivalTime) : "null");
		str.append(' ').append(numChanges).append("ch");

		return str.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Connection))
			return false;
		final Connection other = (Connection) o;
		return getId().equals(other.getId());
	}

	@Override
	public int hashCode()
	{
		return getId().hashCode();
	}

	public static class Part implements Serializable
	{
		private static final long serialVersionUID = 8498461220084523265L;

		public final Location departure;
		public final Location arrival;
		public List<Point> path;

		public Part(final Location departure, final Location arrival, final List<Point> path)
		{
			this.departure = departure;
			this.arrival = arrival;
			this.path = path;
		}
	}

	public final static class Trip extends Part
	{
		private static final long serialVersionUID = 1312066446239817422L;

		public final Line line;
		public final Location destination;
		public final Stop departureStop;
		public final Stop arrivalStop;
		public final List<Stop> intermediateStops;
		public final String message;

		public Trip(final Line line, final Location destination, final Stop departureStop, final Stop arrivalStop,
				final List<Stop> intermediateStops, final List<Point> path, final String message)
		{
			super(departureStop != null ? departureStop.location : null, arrivalStop != null ? arrivalStop.location : null, path);

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

	public final static class Footway extends Part
	{
		private static final long serialVersionUID = -6651381862837233925L;

		public final int min;
		public final int distance;
		public final boolean transfer;

		public Footway(final int min, final int distance, final boolean transfer, final Location departure, final Location arrival,
				final List<Point> path)
		{
			super(departure, arrival, path);

			this.min = min;
			this.distance = distance;
			this.transfer = transfer;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("min=").append(min);
			builder.append(",");
			builder.append("distance=").append(distance);
			builder.append(",");
			builder.append("transfer=").append(transfer);
			builder.append(",");
			builder.append("departure=").append(departure.toDebugString());
			builder.append(",");
			builder.append("arrival=").append(arrival.toDebugString());
			builder.append("]");
			return builder.toString();
		}
	}
}
