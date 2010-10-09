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
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class BahnProviderLiveTest
{
	private BahnProvider provider = new BahnProvider();
	protected static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void departures() throws Exception
	{
		final QueryDeparturesResult queryDepartures = provider.queryDepartures(provider.departuresQueryUri("692991", 0));
		System.out.println(queryDepartures.departures);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Berlin"), null, new Location(
				LocationType.ANY, 0, 0, 0, "Leipzig"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Marienburger Str., Berlin"), null,
				new Location(LocationType.ANY, 0, 0, 0, "Tutzinger-Hof-Platz, Starnberg"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 0, 0,
				"Berlin - Mitte, Unter den Linden 24"), null, new Location(LocationType.ADDRESS, 0, 0, 0, "Starnberg, Possenhofener Straße 13"),
				new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);

		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		for (final Connection connection : result.connections)
			provider.getConnectionDetails(connection.link);
		System.out.println(moreResult);
	}
}
