/*
 * Copyright 2010, 2011 the original author or authors.
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

/**
 * @author Andreas Schildbach
 */
public final class Connection implements Serializable
{
	private static final long serialVersionUID = 2508466068307110312L;

	public final String id;
	public final String link;
	public final Date departureTime;
	public final Date arrivalTime;
	public final Location from;
	public final Location to;
	public final List<Part> parts;
	public final List<Fare> fares;
	public final int[] capacity;

	public Connection(final String id, final String link, final Date departureTime, final Date arrivalTime, final Location from, final Location to,
			final List<Part> parts, final List<Fare> fares, final int[] capacity)
	{
		this.id = id;
		this.link = link;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.from = from;
		this.to = to;
		this.parts = parts;
		this.fares = fares;
		this.capacity = capacity;
	}

	@Override
	public String toString()
	{
		final SimpleDateFormat FORMAT = new SimpleDateFormat("E HH:mm");
		return id + " " + FORMAT.format(departureTime) + "-" + FORMAT.format(arrivalTime);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Connection))
			return false;
		final Connection other = (Connection) o;
		return id.equals(other.id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
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
		public final Line line;
		public final Location destination;
		public final Date departureTime;
		public final String departurePosition;
		public final Date arrivalTime;
		public final String arrivalPosition;
		public final List<Stop> intermediateStops;

		public Trip(final Line line, final Location destination, final Date departureTime, final String departurePosition, final Location departure,
				final Date arrivalTime, final String arrivalPosition, final Location arrival, final List<Stop> intermediateStops,
				final List<Point> path)
		{
			super(departure, arrival, path);

			this.line = line;
			this.destination = destination;
			this.departureTime = departureTime;
			this.departurePosition = departurePosition;
			this.arrivalTime = arrivalTime;
			this.arrivalPosition = arrivalPosition;
			this.intermediateStops = intermediateStops;
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
		public final int min;

		public Footway(final int min, final Location departure, final Location arrival, final List<Point> path)
		{
			super(departure, arrival, path);

			this.min = min;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("min=").append(min);
			builder.append(",");
			builder.append("departure=").append(departure.toDebugString());
			builder.append(",");
			builder.append("arrival=").append(arrival.toDebugString());
			builder.append("]");
			return builder.toString();
		}
	}
}
