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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class QueryConnectionsResult implements Serializable
{
	public enum Status
	{
		OK, AMBIGUOUS, TOO_CLOSE, NO_CONNECTIONS, INVALID_DATE;
	}

	public static final QueryConnectionsResult TOO_CLOSE = new QueryConnectionsResult(Status.TOO_CLOSE);
	public static final QueryConnectionsResult NO_CONNECTIONS = new QueryConnectionsResult(Status.NO_CONNECTIONS);
	public static final QueryConnectionsResult INVALID_DATE = new QueryConnectionsResult(Status.INVALID_DATE);

	public final Status status;

	public final List<Autocomplete> ambiguousFrom;
	public final List<Autocomplete> ambiguousVia;
	public final List<Autocomplete> ambiguousTo;

	public final String queryUri;
	public final String from;
	public final String to;
	public final Date currentDate;
	public final String linkEarlier;
	public final String linkLater;
	public final List<Connection> connections;

	public QueryConnectionsResult(final String queryUri, final String from, final String to, final Date currentDate, final String linkEarlier,
			final String linkLater, final List<Connection> connections)
	{
		this.status = Status.OK;
		this.queryUri = queryUri;
		this.from = from;
		this.to = to;
		this.currentDate = currentDate;
		this.linkEarlier = linkEarlier;
		this.linkLater = linkLater;
		this.connections = connections;

		this.ambiguousFrom = null;
		this.ambiguousVia = null;
		this.ambiguousTo = null;
	}

	public QueryConnectionsResult(final List<Autocomplete> ambiguousFrom, final List<Autocomplete> ambiguousVia, final List<Autocomplete> ambiguousTo)
	{
		this.status = Status.AMBIGUOUS;
		this.ambiguousFrom = ambiguousFrom;
		this.ambiguousVia = ambiguousVia;
		this.ambiguousTo = ambiguousTo;

		this.queryUri = null;
		this.from = null;
		this.to = null;
		this.currentDate = null;
		this.linkEarlier = null;
		this.linkLater = null;
		this.connections = null;
	}

	public QueryConnectionsResult(final Status status)
	{
		this.status = status;

		this.ambiguousFrom = null;
		this.ambiguousVia = null;
		this.ambiguousTo = null;
		this.queryUri = null;
		this.from = null;
		this.to = null;
		this.currentDate = null;
		this.linkEarlier = null;
		this.linkLater = null;
		this.connections = null;
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append("[").append(this.status).append(": ");
		if (connections != null)
			builder.append(connections.size()).append(" connections, ");
		if (ambiguousFrom != null)
			builder.append(ambiguousFrom.size()).append(" ambiguous from, ");
		if (ambiguousVia != null)
			builder.append(ambiguousVia.size()).append(" ambiguous via, ");
		if (ambiguousTo != null)
			builder.append(ambiguousTo.size()).append(" ambiguous to, ");
		if (builder.substring(builder.length() - 2).equals(", "))
			builder.setLength(builder.length() - 2);
		builder.append("]");
		return builder.toString();
	}
}
