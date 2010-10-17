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
	public final int locationId;
	public final String location;
	public final List<Departure> departures;

	public QueryDeparturesResult(final int locationId, final String location, final List<Departure> departures)
	{
		this.status = Status.OK;
		this.locationId = locationId;
		this.location = location;
		this.departures = departures;
	}

	public QueryDeparturesResult(final Status status, final int locationId)
	{
		this.status = status;
		this.locationId = locationId;
		this.location = null;
		this.departures = null;
	}
}
