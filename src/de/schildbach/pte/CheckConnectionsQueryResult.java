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

import java.util.List;

/**
 * @author Andreas Schildbach
 */
public final class CheckConnectionsQueryResult
{
	public enum Status
	{
		OK, AMBIGUOUS, TOO_CLOSE, NO_CONNECTIONS;
	}

	public static final CheckConnectionsQueryResult TOO_CLOSE = new CheckConnectionsQueryResult(Status.TOO_CLOSE, null, null, null, null);
	public static final CheckConnectionsQueryResult NO_CONNECTIONS = new CheckConnectionsQueryResult(Status.NO_CONNECTIONS, null, null, null, null);

	public final Status status;
	public final String queryUri;
	public final List<String> ambiguousFromAddresses;
	public final List<String> ambiguousViaAddresses;
	public final List<String> ambiguousToAddresses;

	public CheckConnectionsQueryResult(final Status status, final String queryUri, final List<String> ambiguousFromAddresses,
			final List<String> ambiguousViaAddresses, final List<String> ambiguousToAddresses)
	{
		this.status = status;
		this.queryUri = queryUri;
		this.ambiguousFromAddresses = ambiguousFromAddresses;
		this.ambiguousViaAddresses = ambiguousViaAddresses;
		this.ambiguousToAddresses = ambiguousToAddresses;
	}
}
