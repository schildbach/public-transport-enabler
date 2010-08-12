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

import java.util.Date;

/**
 * @author Andreas Schildbach
 */
public final class Departure
{
	final public Date time;
	final public String line;
	final public int[] lineColors;
	final public int destinationId;
	final public String destination;

	public Departure(final Date time, final String line, final int[] lineColors, final int destinationId, final String destination)
	{
		this.time = time;
		this.line = line;
		this.lineColors = lineColors;
		this.destinationId = destinationId;
		this.destination = destination;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Departure(");
		builder.append(time != null ? time : "null");
		builder.append(",");
		builder.append(line != null ? line : "null");
		builder.append(",");
		builder.append(destinationId);
		builder.append(",");
		builder.append(destination != null ? destination : "null");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Departure))
			return false;
		final Departure other = (Departure) o;
		if (!this.time.equals(other.time))
			return false;
		if (this.line == null && other.line != null)
			return false;
		if (other.line == null && this.line != null)
			return false;
		if (this.line != null && !this.line.equals(other.line))
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
		int hashCode = time.hashCode();
		hashCode *= 29;
		if (line != null)
			hashCode += line.hashCode();
		hashCode *= 29;
		hashCode += destinationId;
		hashCode *= 29;
		hashCode += destination.hashCode();
		return hashCode;
	}
}
