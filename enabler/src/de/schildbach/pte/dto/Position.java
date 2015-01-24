/*
 * Copyright 2013-2015 the original author or authors.
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
public final class Position implements Serializable
{
	public final String name;
	public final String section;

	public Position(final String name)
	{
		this(name, null);
	}

	public Position(final String name, final String section)
	{
		if (name == null)
			throw new IllegalArgumentException("name cannot be null");
		// else if (name.length() > 5)
		// throw new IllegalArgumentException("name too long: " + name);

		if (section != null && section.length() > 3)
			throw new IllegalArgumentException("section too long: " + section);

		this.name = name;
		this.section = section;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Position))
			return false;
		final Position other = (Position) o;
		if (!Objects.equal(this.name, other.name))
			return false;
		if (!Objects.equal(this.section, other.section))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder(name);
		if (section != null)
			builder.append(section);
		return builder.toString();
	}

	@Override
	public int hashCode()
	{
		int hashCode = 0;
		hashCode += name.hashCode();
		hashCode += nullSafeHashCode(section);
		return hashCode;
	}

	private int nullSafeHashCode(final Object o)
	{
		if (o == null)
			return 0;
		return o.hashCode();
	}
}
