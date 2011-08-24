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

/**
 * @author Andreas Schildbach
 */
public final class LineDestination
{
	final public Line line;
	final public int destinationId;
	final public String destination;

	public LineDestination(final Line line, final int destinationId, final String destination)
	{
		this.line = line;
		this.destinationId = destinationId;
		this.destination = destination;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("LineDestination(");
		builder.append(line != null ? line : "null");
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
		if (!(o instanceof LineDestination))
			return false;
		final LineDestination other = (LineDestination) o;
		if (!nullSafeEquals(this.line, other.line))
			return false;
		if (this.destinationId != other.destinationId)
			return false;
		if (!nullSafeEquals(this.destination, other.destination))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int hashCode = 0;
		hashCode += nullSafeHashCode(line);
		hashCode *= 29;
		hashCode += destinationId;
		hashCode *= 29;
		hashCode += nullSafeHashCode(destination);
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
