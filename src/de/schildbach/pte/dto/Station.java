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

package de.schildbach.pte.dto;

/**
 * @author Andreas Schildbach
 */
public final class Station
{
	// data
	public final int id;
	public final String place;
	public final String name;
	public final String longName;
	public final int latitude, longitude;

	public Station(final int id, final String place, final String name, final String longName, final int latitude, final int longitude)
	{
		this.id = id;
		this.place = place;
		this.name = name;
		this.longName = longName;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public final boolean hasLocation()
	{
		return latitude != 0 || longitude != 0;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Station(");
		builder.append(id);
		builder.append("|");
		builder.append(place);
		builder.append("|");
		builder.append(name);
		builder.append("|");
		builder.append(longName);
		builder.append("|");
		builder.append(latitude);
		builder.append(",");
		builder.append(longitude);
		builder.append(")");
		return builder.toString();
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
