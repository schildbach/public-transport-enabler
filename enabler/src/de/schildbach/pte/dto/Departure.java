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
import java.util.Comparator;
import java.util.Date;

import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Departure implements Serializable
{
	final public Date plannedTime;
	final public Date predictedTime;
	final public Line line;
	final public Position position;
	final public Location destination;
	final public int[] capacity;
	final public String message;

	public Departure(final Date plannedTime, final Date predictedTime, final Line line, final Position position, final Location destination,
			final int[] capacity, final String message)
	{
		this.plannedTime = plannedTime;
		this.predictedTime = predictedTime;
		this.line = line;
		this.position = position;
		this.destination = destination;
		this.capacity = capacity;
		this.message = message;
	}

	public Date getTime()
	{
		if (predictedTime != null)
			return predictedTime;
		else if (plannedTime != null)
			return plannedTime;
		else
			return null;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder("Departure(");
		builder.append(plannedTime != null ? plannedTime : "null");
		builder.append(",");
		builder.append(predictedTime != null ? predictedTime : "null");
		builder.append(",");
		builder.append(line != null ? line : "null");
		builder.append(",");
		builder.append(position != null ? position : "null");
		builder.append(",");
		builder.append(destination != null ? destination : "null");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Departure))
			return false;
		final Departure other = (Departure) o;
		if (!Objects.equal(this.plannedTime, other.plannedTime))
			return false;
		if (!Objects.equal(this.predictedTime, other.predictedTime))
			return false;
		if (!Objects.equal(this.line, other.line))
			return false;
		if (!Objects.equal(this.destination, other.destination))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int hashCode = 0;
		hashCode += nullSafeHashCode(plannedTime);
		hashCode *= 29;
		hashCode += nullSafeHashCode(predictedTime);
		hashCode *= 29;
		hashCode += nullSafeHashCode(line);
		hashCode *= 29;
		hashCode += nullSafeHashCode(destination);
		return hashCode;
	}

	private int nullSafeHashCode(final Object o)
	{
		if (o == null)
			return 0;
		return o.hashCode();
	}

	public static final Comparator<Departure> TIME_COMPARATOR = new Comparator<Departure>()
	{
		public int compare(final Departure departure0, final Departure departure1)
		{
			return departure0.getTime().compareTo(departure1.getTime());
		}
	};
}
