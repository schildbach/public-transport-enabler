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

import de.schildbach.pte.OebbProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;

/**
 * @author Andreas Schildbach
 */
public class OebbProviderLiveTest
{
	private OebbProvider provider = new OebbProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void nearbyStation() throws Exception
	{
		final NearbyStationsResult result = provider.nearbyStations("902006", 0, 0, 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Linz"), null, new Location(
				LocationType.ANY, 0, 0, 0, "Berlin"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Ramsen, Zoll"), null, new Location(
				LocationType.ANY, 0, 0, 0, "Azuga"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Graz, Haselweg"), null,
				new Location(LocationType.ADDRESS, 0, 0, 0, "Innsbruck, Gumppstraße 69"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway2() throws Exception
	{
		final QueryConnectionsResult result = provider
				.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Wien, Krottenbachstraße 110!"), null, new Location(LocationType.ADDRESS,
						0, 0, 0, "Wien, Meidlinger Hauptstraße 1"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}
}
