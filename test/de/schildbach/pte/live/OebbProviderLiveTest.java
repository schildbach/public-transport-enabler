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

import de.schildbach.pte.OebbProvider;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.Station;

/**
 * @author Andreas Schildbach
 */
public class OebbProviderLiveTest
{
	private OebbProvider provider = new OebbProvider();

	@Test
	public void nearbyStation() throws Exception
	{
		final List<Station> results = provider.nearbyStations("902006", 0, 0, 0, 0);

		System.out.println(results.size() + "  " + results);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Linz", null, null, LocationType.ANY, "Berlin", new Date(),
				true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Ramsen, Zoll", null, null, LocationType.ANY, "Azuga",
				new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Graz, Haselweg", null, null, LocationType.ADDRESS,
				"Innsbruck, Gumppstraße 69", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway2() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ANY, "Wien, Krottenbachstraße 110!", null, null,
				LocationType.ADDRESS, "Wien, Meidlinger Hauptstraße 1", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}
}
