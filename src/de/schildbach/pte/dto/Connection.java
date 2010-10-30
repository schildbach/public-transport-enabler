/*
 * Copyright 2010 the original author or authors.
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
	public final String id;
	public final String link;
	public final Date departureTime;
	public final Date arrivalTime;
	public final String line;
	public final int[] lineColors;
	public final int fromId;
	public final String from;
	public final int toId;
	public final String to;
	public final List<Part> parts;

	public Connection(final String id, final String link, final Date departureTime, final Date arrivalTime, final String line,
			final int[] lineColors, final int fromId, final String from, final int toId, final String to, final List<Part> parts)
	{
		this.id = id;
		this.link = link;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.line = line;
		this.lineColors = lineColors;
		this.fromId = fromId;
		this.from = from;
		this.toId = toId;
		this.to = to;
		this.parts = parts;
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

	public static interface Part extends Serializable
	{
	}

	public final static class Trip implements Part
	{
		public final String line;
		public final int[] lineColors;
		public final int destinationId;
		public final String destination;
		public final Date departureTime;
		public final String departurePosition;
		public final int departureId;
		public final String departure;
		public final Date arrivalTime;
		public final String arrivalPosition;
		public final int arrivalId;
		public final String arrival;
		public final List<Stop> intermediateStops;

		public Trip(final String line, final int[] lineColors, final int destinationId, final String destination, final Date departureTime,
				final String departurePosition, final int departureId, final String departure, final Date arrivalTime, final String arrivalPosition,
				final int arrivalId, final String arrival, final List<Stop> intermediateStops)
		{
			this.line = line;
			this.lineColors = lineColors;
			this.destinationId = destinationId;
			this.destination = destination;
			this.departureTime = departureTime;
			this.departurePosition = departurePosition;
			this.departureId = departureId;
			this.departure = departure;
			this.arrivalTime = arrivalTime;
			this.arrivalPosition = arrivalPosition;
			this.arrivalId = arrivalId;
			this.arrival = arrival;
			this.intermediateStops = intermediateStops;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("line=").append(line);
			builder.append(",");
			builder.append("destination=").append(destination).append("/").append(destinationId);
			builder.append(",");
			builder.append("departure=").append(departureTime).append("/").append(departurePosition).append("/").append(departureId).append("/")
					.append(departure);
			builder.append(",");
			builder.append("arrival=").append(arrivalTime).append("/").append(arrivalPosition).append("/").append(arrivalId).append("/").append(
					arrival);
			builder.append("]");
			return builder.toString();
		}
	}

	public final static class Footway implements Part
	{
		public final int min;
		public final int departureId;
		public final String departure;
		public final int arrivalId;
		public final String arrival;
		public final int arrivalLat, arrivalLon;

		public Footway(final int min, final int departureId, final String departure, final int arrivalId, final String arrival, final int arrivalLat,
				final int arrivalLon)
		{
			this.min = min;
			this.departureId = departureId;
			this.departure = departure;
			this.arrivalId = arrivalId;
			this.arrival = arrival;
			this.arrivalLat = arrivalLat;
			this.arrivalLon = arrivalLon;
		}

		public Footway(final int min, final int departureId, final String departure, final int arrivalId, final String arrival)
		{
			this.min = min;
			this.departureId = departureId;
			this.departure = departure;
			this.arrivalId = arrivalId;
			this.arrival = arrival;
			this.arrivalLat = 0;
			this.arrivalLon = 0;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("min=").append(min);
			builder.append(",");
			builder.append("departure=").append(departureId).append("/").append(departure);
			builder.append(",");
			builder.append("arrival=").append(arrivalId).append("/").append(arrival).append("/").append(arrivalLat).append(",").append(arrivalLon);
			builder.append("]");
			return builder.toString();
		}
	}
}
