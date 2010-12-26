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

import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class QueryDeparturesResult
{
	public enum Status
	{
		OK, NO_INFO, INVALID_STATION, SERVICE_DOWN
	}

	public final Status status;
	public final Location location;
	public final List<Departure> departures;
	public final List<Line> lines;

	public QueryDeparturesResult(final Location location, final List<Departure> departures, final List<Line> lines)
	{
		this.status = Status.OK;
		this.location = location;
		this.departures = departures;
		this.lines = lines;
	}

	public QueryDeparturesResult(final Status status, final int locationId)
	{
		this.status = status;
		this.location = new Location(LocationType.STATION, locationId, null);
		this.departures = null;
		this.lines = null;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append("[").append(this.status).append(": ");
		if (departures != null)
			builder.append(departures.size()).append(" departures");
		builder.append("]");
		return builder.toString();
	}
}
