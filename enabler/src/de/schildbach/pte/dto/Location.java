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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * @author Andreas Schildbach
 */
public final class Location implements Serializable
{
	public final LocationType type;
	public final String id;
	public final int lat, lon;
	public final String place;
	public final String name;

	public Location(final LocationType type, final String id, final int lat, final int lon, final String place, final String name)
	{
		this.type = checkNotNull(type);
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.place = place;
		this.name = name;

		checkArgument(place == null || name != null, "place '%s' without name cannot exist", place);
	}

	public Location(final LocationType type, final String id, final Point coord, final String place, final String name)
	{
		this(type, id, coord != null ? coord.lat : 0, coord != null ? coord.lon : 0, place, name);
	}

	public Location(final LocationType type, final String id, final String place, final String name)
	{
		this.type = checkNotNull(type);
		this.id = id;
		this.lat = 0;
		this.lon = 0;
		this.place = place;
		this.name = name;

		checkArgument(place == null || name != null, "place '%s' without name cannot exist", place);
	}

	public Location(final LocationType type, final String id, final int lat, final int lon)
	{
		this.type = checkNotNull(type);
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.place = null;
		this.name = null;
	}

	public Location(final LocationType type, final String id, final Point coord)
	{
		this(type, id, coord != null ? coord.lat : 0, coord != null ? coord.lon : 0);
	}

	public Location(final LocationType type, final String id)
	{
		this.type = checkNotNull(type);
		this.id = id;
		this.lat = 0;
		this.lon = 0;
		this.place = null;
		this.name = null;
	}

	public Location(final LocationType type, final int lat, final int lon)
	{
		this.type = checkNotNull(type);
		this.id = null;
		this.lat = lat;
		this.lon = lon;
		this.place = null;
		this.name = null;
	}

	public Location(final LocationType type, final Point coord)
	{
		this(type, coord != null ? coord.lat : 0, coord != null ? coord.lon : 0);
	}

	public final boolean hasId()
	{
		return id != null;
	}

	public final boolean hasLocation()
	{
		return lat != 0 || lon != 0;
	}

	public final boolean hasName()
	{
		return name != null;
	}

	public final boolean isIdentified()
	{
		if (type == LocationType.STATION)
			return hasId();

		if (type == LocationType.POI)
			return true;

		if (type == LocationType.ADDRESS)
			return hasLocation();

		return false;
	}

	private static final String[] NON_UNIQUE_NAMES = { "Hauptbahnhof", "Hbf", "Bahnhof", "Bf", "Busbahnhof", "ZOB", "Zentrum", "Dorf", "Kirche",
			"Nord", "Ost", "Süd", "West" };
	static
	{
		Arrays.sort(NON_UNIQUE_NAMES);
	}

	public final String uniqueShortName()
	{
		if (place != null && name != null && Arrays.binarySearch(NON_UNIQUE_NAMES, name) >= 0)
			return place + ", " + name;
		else if (name != null)
			return name;
		else if (hasId())
			return id;
		else
			return null;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Location))
			return false;
		final Location other = (Location) o;
		if (!Objects.equal(this.type, other.type))
			return false;
		if (this.id != null)
			return Objects.equal(this.id, other.id);
		if (this.lat != 0 && this.lon != 0)
			return this.lat == other.lat && this.lon == other.lon;

		// only discriminate by name/place if no ids are given
		if (!Objects.equal(this.name, other.name))
			return false;
		if (!Objects.equal(this.place, other.place))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		if (id != null)
			return Objects.hashCode(type, id);
		else
			return Objects.hashCode(type, lat, lon);
	}

	@Override
	public String toString()
	{
		final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(type).addValue(id);
		if (lat != 0 || lon != 0)
			helper.addValue(lat + "/" + lon);
		return helper.add("place", place).add("name", name).omitNullValues().toString();
	}
}
