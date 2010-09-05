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

package de.schildbach.pte;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class Connection implements Serializable
{
	final public String id;
	final public String link;
	final public Date departureTime;
	final public Date arrivalTime;
	final public String line;
	final public int[] lineColors;
	final public int fromId;
	final public String from;
	final public int toId;
	final public String to;
	final public List<Part> parts;

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
		final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
		builder.append("id=").append(id);
		builder.append(",departureTime=").append(departureTime);
		builder.append(",arrivalTime=").append(arrivalTime);
		builder.append(",parts=").append(parts);
		builder.append("]");
		return builder.toString();
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
		final public String line;
		final public int[] lineColors;
		final public String destination;
		final public Date departureTime;
		final public String departurePosition;
		final public int departureId;
		final public String departure;
		final public Date arrivalTime;
		final public String arrivalPosition;
		final public int arrivalId;
		final public String arrival;

		public Trip(final String line, final int[] lineColors, final String destination, final Date departureTime, final String departurePosition,
				final int departureId, final String departure, final Date arrivalTime, final String arrivalPosition, final int arrivalId,
				final String arrival)
		{
			this.line = line;
			this.lineColors = lineColors;
			this.destination = destination;
			this.departureTime = departureTime;
			this.departurePosition = departurePosition;
			this.departureId = departureId;
			this.departure = departure;
			this.arrivalTime = arrivalTime;
			this.arrivalPosition = arrivalPosition;
			this.arrivalId = arrivalId;
			this.arrival = arrival;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder(getClass().getName() + "[");
			builder.append("line=").append(line);
			builder.append(",");
			builder.append("destination=").append(destination);
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
		final public int min;
		final public int departureId;
		final public String departure;
		final public int arrivalId;
		final public String arrival;
		final public double arrivalLat, arrivalLon;

		public Footway(final int min, final int departureId, final String departure, final int arrivalId, final String arrival,
				final double arrivalLat, final double arrivalLon)
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
