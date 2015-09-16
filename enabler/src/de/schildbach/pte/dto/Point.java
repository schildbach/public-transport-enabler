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
public final class Point implements Serializable
{
	private static final long serialVersionUID = -256077054671402897L;

	public final int lat, lon;

	public Point(final int lat, final int lon)
	{
		this.lat = lat;
		this.lon = lon;
	}

	public static Point fromDouble(final double lat, final double lon)
	{
		return new Point((int) Math.round(lat * 1E6), (int) Math.round(lon * 1E6));
	}

	public double getLatAsDouble()
	{
		return lat / 1E6;
	}

	public double getLonAsDouble()
	{
		return lon / 1E6;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Point))
			return false;
		final Point other = (Point) o;
		if (this.lat != other.lat)
			return false;
		if (this.lon != other.lon)
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(lat, lon);
	}

	@Override
	public String toString()
	{
		return lat + "/" + lon;
	}
}
