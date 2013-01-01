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
					builder.append(trip.departureTime.getTime()).append('-');
					builder.append(trip.arrivalTime.getTime()).append('-');
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
		public final Date departureTime; // TODO rename to plannedDepartureTime
		public final Date predictedDepartureTime;
		public final String departurePosition; // TODO rename to plannedDeparturePosition
		public final String predictedDeparturePosition;
		public final Date arrivalTime; // TODO rename to plannedArrivalTime
		public final Date predictedArrivalTime;
		public final String arrivalPosition; // TODO rename to plannedArrivalPosition
		public final String predictedArrivalPosition;
		public final List<Stop> intermediateStops;
		public final String message;

		public Trip(final Line line, final Location destination, final Date plannedDepartureTime, final Date predictedDepartureTime,
				final String departurePosition, final String predictedDeparturePosition, final Location departure, final Date plannedArrivalTime,
				final Date predictedArrivalTime, final String arrivalPosition, final String predictedArrivalPosition, final Location arrival,
				final List<Stop> intermediateStops, final List<Point> path, final String message)
		{
			super(departure, arrival, path);

			this.line = line;
			this.destination = destination;
			this.departureTime = plannedDepartureTime;
			this.predictedDepartureTime = predictedDepartureTime;
			this.departurePosition = departurePosition;
			this.predictedDeparturePosition = predictedDeparturePosition;
			this.arrivalTime = plannedArrivalTime;
			this.predictedArrivalTime = predictedArrivalTime;
			this.arrivalPosition = arrivalPosition;
			this.predictedArrivalPosition = predictedArrivalPosition;
			this.intermediateStops = intermediateStops;
			this.message = message;
		}

		public Date getDepartureTime()
		{
			if (predictedDepartureTime != null)
				return predictedDepartureTime;
			else if (departureTime != null)
				return departureTime;
			else
				throw new IllegalStateException();
		}

		public boolean isDepartureTimePredicted()
		{
			return predictedDepartureTime != null;
		}

		public Long getDepartureDelay()
		{
			if (departureTime != null && predictedDepartureTime != null)
				return predictedDepartureTime.getTime() - departureTime.getTime();
			else
				return null;
		}

		public String getDeparturePosition()
		{
			if (predictedDeparturePosition != null)
				return predictedDeparturePosition;
			else if (departurePosition != null)
				return departurePosition;
			else
				return null;
		}

		public boolean isDeparturePositionPredicted()
		{
			return predictedDeparturePosition != null;
		}

		public Date getArrivalTime()
		{
			if (predictedArrivalTime != null)
				return predictedArrivalTime;
			else if (arrivalTime != null)
				return arrivalTime;
			else
				throw new IllegalStateException();
		}

		public boolean isArrivalTimePredicted()
		{
			return predictedArrivalTime != null;
		}

		public Long getArrivalDelay()
		{
			if (arrivalTime != null && predictedArrivalTime != null)
				return predictedArrivalTime.getTime() - arrivalTime.getTime();
			else
				return null;
		}

		public String getArrivalPosition()
		{
			if (predictedArrivalPosition != null)
				return predictedArrivalPosition;
			else if (arrivalPosition != null)
				return arrivalPosition;
			else
				return null;
		}

		public boolean isArrivalPositionPredicted()
		{
			return predictedArrivalPosition != null;
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
			builder.append("departure=").append(departureTime).append("/").append(departurePosition).append("/").append(departure.toDebugString());
			builder.append(",");
			builder.append("arrival=").append(arrivalTime).append("/").append(arrivalPosition).append("/").append(arrival.toDebugString());
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
