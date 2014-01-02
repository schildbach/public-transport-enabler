/*
 * Copyright 2013-2014 the original author or authors.
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
public final class Position implements Serializable
{
	public final String name;

	public Position(final String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder("Position(");
		builder.append(name != null ? name : "null");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Position))
			return false;
		final Position other = (Position) o;
		if (!nullSafeEquals(this.name, other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int hashCode = 0;
		hashCode += nullSafeHashCode(name);
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
