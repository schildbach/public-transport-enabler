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

/**
 * @author Andreas Schildbach
 */
public final class Line implements Serializable
{
	final public String label;
	final public int[] colors;

	public Line(final String label, final int[] colors)
	{
		this.label = label;
		this.colors = colors;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder("Line(");
		builder.append(label);
		builder.append(")");
		return builder.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Line))
			return false;
		final Line other = (Line) o;
		return (this.label.equals(other.label));
	}

	@Override
	public int hashCode()
	{
		return label.hashCode();
	}
}
