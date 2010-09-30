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

package de.schildbach.pte.live;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.BahnProvider;
import de.schildbach.pte.Connection;
import de.schildbach.pte.QueryConnectionsResult;
import de.schildbach.pte.QueryDeparturesResult;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;

/**
 * @author Andreas Schildbach
 */
public class BahnProviderLiveTest
{
	private BahnProvider provider = new BahnProvider();
	
	@Test
	public void departures() throws Exception
	{
		final QueryDeparturesResult queryDepartures = provider.queryDepartures(provider.departuresQueryUri("692991", 0));
		System.out.println(queryDepartures.departures);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Berlin", null, null, LocationType.ANY, "Leipzig",
				new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Marienburger Str., Berlin ", null, null, LocationType.ANY,
				"Tutzinger-Hof-Platz, Starnberg", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ADDRESS, "Berlin - Mitte, Unter den Linden 24", null, null,
				LocationType.ADDRESS, "Starnberg, Possenhofener Straße 13", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);

		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}
}
