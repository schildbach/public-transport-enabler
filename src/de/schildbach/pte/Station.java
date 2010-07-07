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
public final class Station
{
	// data
	public final int id;
	public final String name;
	public final double latitude, longitude;
	public float distance;
	public final String[] lines;
	public final int[][] lineColors;

	// transient
	public transient Date lastUpdatedDepartures;

	public Station(final int id, final String name, final double latitude, final double longitude, final float distance, final String[] lines,
			final int[][] lineColors)
	{
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.distance = distance;
		this.lines = lines;
		this.lineColors = lineColors;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Station))
			return false;
		final Station other = (Station) o;
		return this.id == other.id;
	}

	@Override
	public int hashCode()
	{
		return id;
	}
}
