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

import java.util.Date;

/**
 * @author Andreas Schildbach
 */
public final class Departure
{
	final public Date plannedTime;
	final public Date predictedTime;
	final public Line line;
	final public String position;
	final public int destinationId;
	final public String destination;
	final public int[] capacity;
	final public String message;

	public Departure(final Date plannedTime, final Date predictedTime, final Line line, final String position, final int destinationId,
			final String destination, final int[] capacity, final String message)
	{
		this.plannedTime = plannedTime;
		this.predictedTime = predictedTime;
		this.line = line;
		this.position = position;
		this.destinationId = destinationId;
		this.destination = destination;
		this.capacity = capacity;
		this.message = message;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Departure(");
		builder.append(plannedTime != null ? plannedTime : "null");
		builder.append(",");
		builder.append(predictedTime != null ? predictedTime : "null");
		builder.append(",");
		builder.append(line != null ? line : "null");
		builder.append(",");
		builder.append(position != null ? position : "null");
		builder.append(",");
		builder.append(destinationId);
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
		if (!nullSafeEquals(this.plannedTime, other.plannedTime))
			return false;
		if (!nullSafeEquals(this.predictedTime, other.predictedTime))
			return false;
		if (!nullSafeEquals(this.line, other.line))
			return false;
		if (this.destinationId != other.destinationId)
			return false;
		if (!this.destination.equals(other.destination))
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
		hashCode += destinationId;
		hashCode *= 29;
		hashCode += destination.hashCode();
		return hashCode;
	}

	private boolean nullSafeEquals(final Object o1, final Object o2)
	{
		if (o1 == null && o2 == null)
			return true;
		if (o1 != null && o1.equals(o2))
			return true;
		return false;
	}

	private int nullSafeHashCode(final Object o)
	{
		if (o == null)
			return 0;
		return o.hashCode();
	}
}
