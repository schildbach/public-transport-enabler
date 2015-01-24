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

import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class LineDestination implements Serializable
{
	final public Line line;
	final public Location destination;

	public LineDestination(final Line line, final Location destination)
	{
		this.line = line;
		this.destination = destination;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("LineDestination(");
		builder.append(line != null ? line : "null");
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
		if (!Objects.equal(this.line, other.line))
			return false;
		if (!Objects.equal(this.destination, other.destination))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(line, destination);
	}
}
