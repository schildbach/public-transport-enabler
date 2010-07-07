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
import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class QueryConnectionsResult
{
	public final String from;
	public final String to;
	public final Date currentDate;
	public final String linkEarlier;
	public final String linkLater;
	public final List<Connection> connections;

	public QueryConnectionsResult(final String from, final String to, final Date currentDate, final String linkEarlier, final String linkLater,
			final List<Connection> connections)
	{
		this.from = from;
		this.to = to;
		this.currentDate = currentDate;
		this.linkEarlier = linkEarlier;
		this.linkLater = linkLater;
		this.connections = connections;
	}
}
