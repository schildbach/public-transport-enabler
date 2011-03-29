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

import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class StationDepartures
{
	public final Location location;
	public final List<Departure> departures;
	public final List<LineDestination> lines;

	public StationDepartures(final Location location, final List<Departure> departures, final List<LineDestination> lines)
	{
		this.location = location;
		this.departures = departures;
		this.lines = lines;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append("[");
		if (location != null)
			builder.append(location.toDebugString());
		if (departures != null)
			builder.append(" ").append(departures.size()).append(" departures");
		builder.append("]");
		return builder.toString();
	}
}
