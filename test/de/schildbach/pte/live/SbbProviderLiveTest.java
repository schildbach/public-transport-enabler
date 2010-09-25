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
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.Autocomplete;
import de.schildbach.pte.QueryConnectionsResult;
import de.schildbach.pte.SbbProvider;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;

/**
 * @author Andreas Schildbach
 */
public class SbbProviderLiveTest
{
	private SbbProvider provider = new SbbProvider();

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Zürich!", null, null, LocationType.ANY, "Bern",
				new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Schocherswil, Alte Post!", null, null, LocationType.ANY,
				"Laconnex, Mollach", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ADDRESS, "Spiez, Seestraße 62", null, null,
				LocationType.ADDRESS, "Einsiedeln, Erlenmoosweg 24", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void autoComplete() throws Exception
	{
		final List<Autocomplete> result = provider.autocompleteStations("haupt");
		System.out.println(result);
	}
}
