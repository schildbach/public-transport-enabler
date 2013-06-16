/*
 * Copyright 2010-2013 the original author or authors.
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
import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class QueryTripsResult implements Serializable
{
	public enum Status
	{
		OK, AMBIGUOUS, TOO_CLOSE, UNKNOWN_FROM, UNKNOWN_TO, UNRESOLVABLE_ADDRESS, NO_TRIPS, INVALID_DATE, SERVICE_DOWN;
	}

	public final ResultHeader header;
	public final Status status;

	public final List<Location> ambiguousFrom;
	public final List<Location> ambiguousVia;
	public final List<Location> ambiguousTo;

	public final String queryUri;
	public final Location from;
	public final Location via;
	public final Location to;
	public final QueryTripsContext context;
	public final List<Trip> trips;

	public QueryTripsResult(final ResultHeader header, final String queryUri, final Location from, final Location via, final Location to,
			final QueryTripsContext context, final List<Trip> trips)
	{
		this.header = header;
		this.status = Status.OK;
		this.queryUri = queryUri;
		this.from = from;
		this.via = via;
		this.to = to;
		this.context = context;
		this.trips = trips;

		this.ambiguousFrom = null;
		this.ambiguousVia = null;
		this.ambiguousTo = null;
	}

	public QueryTripsResult(final ResultHeader header, final List<Location> ambiguousFrom, final List<Location> ambiguousVia,
			final List<Location> ambiguousTo)
	{
		this.header = header;
		this.status = Status.AMBIGUOUS;
		this.ambiguousFrom = ambiguousFrom;
		this.ambiguousVia = ambiguousVia;
		this.ambiguousTo = ambiguousTo;

		this.queryUri = null;
		this.from = null;
		this.via = null;
		this.to = null;
		this.context = null;
		this.trips = null;
	}

	public QueryTripsResult(final ResultHeader header, final Status status)
	{
		this.header = header;
		this.status = status;

		this.ambiguousFrom = null;
		this.ambiguousVia = null;
		this.ambiguousTo = null;
		this.queryUri = null;
		this.from = null;
		this.via = null;
		this.to = null;
		this.context = null;
		this.trips = null;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append("[");

		if (status == Status.OK)
		{
			if (trips != null)
				builder.append(trips.size()).append(" trips " + trips + ", ");
		}
		else if (status == Status.AMBIGUOUS)
		{
			if (ambiguousFrom != null)
				builder.append(ambiguousFrom.size()).append(" ambiguous from, ");
			if (ambiguousVia != null)
				builder.append(ambiguousVia.size()).append(" ambiguous via, ");
			if (ambiguousTo != null)
				builder.append(ambiguousTo.size()).append(" ambiguous to, ");
		}
		else
		{
			builder.append(this.status);
		}

		if (builder.substring(builder.length() - 2).equals(", "))
			builder.setLength(builder.length() - 2);
		builder.append("]");
		return builder.toString();
	}
}
