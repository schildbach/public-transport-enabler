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

import de.schildbach.pte.MvvProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryConnectionsResult;

/**
 * @author Andreas Schildbach
 */
public class MvvProviderLiveTest
{
	private MvvProvider provider = new MvvProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Marienplatz"), null, new Location(
				LocationType.ANY, 0, 0, 0, "Pasing"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void longConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Starnberg, Arbeitsamt"), null,
				new Location(LocationType.STATION, 0, 0, 0, "Ackermannstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		// seems like there are no more connections all the time
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 48165238, 11577473, null), null,
				new Location(LocationType.ADDRESS, 0, 47987199, 11326532, null), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinateAndStation() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 48238341, 11478230, null), null,
				new Location(LocationType.ANY, 0, 0, 0, "Ostbahnhof"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 0, 0, "München, Maximilianstr. 1"),
				null, new Location(LocationType.ADDRESS, 0, 0, 0, "Starnberg, Jahnstraße 50"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}
}
