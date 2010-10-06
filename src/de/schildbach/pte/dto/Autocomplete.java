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

import de.schildbach.pte.NetworkProvider.LocationType;

/**
 * @author Andreas Schildbach
 */
public final class Autocomplete
{
	public final LocationType locationType;
	public final int locationId;
	public final String location;

	public Autocomplete(final LocationType locationType, final int locationId, final String location)
	{
		this.locationType = locationType;
		this.locationId = locationId;
		this.location = location;
	}

	@Override
	public String toString()
	{
		return location; // invoked by AutoCompleteTextView in landscape orientation
	}

	public String toDebugString()
	{
		return "[" + locationType + " " + locationId + " '" + location + "']";
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Autocomplete))
			return false;
		final Autocomplete other = (Autocomplete) o;
		if (this.locationType != other.locationType)
			return false;
		if (this.locationId != other.locationId)
			return false;
		if (this.locationId != 0)
			return true;
		return this.location.equals(other.location);
	}

	@Override
	public int hashCode()
	{
		return locationType.hashCode(); // FIXME not very discriminative
	}
}
