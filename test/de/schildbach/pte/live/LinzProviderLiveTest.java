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

import de.schildbach.pte.LinzProvider;
import de.schildbach.pte.QueryConnectionsResult;
import de.schildbach.pte.QueryDeparturesResult;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;

/**
 * @author Andreas Schildbach
 */
public class LinzProviderLiveTest
{
	private LinzProvider provider = new LinzProvider();
	
	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(provider.departuresQueryUri("60501720", 0));
		System.out.println(result);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.STATION, "Linz Hauptbahnhof", null, null, LocationType.STATION,
				"Linz Auwiesen", new Date(), true, WalkSpeed.FAST);
		System.out.println(result);
//		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
//		System.out.println(moreResult);
	}

	@Test
	public void longConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.STATION, "Linz Auwiesen", null, null, LocationType.STATION,
				"Linz Hafen", new Date(), true, WalkSpeed.SLOW);
		System.out.println(result);
//		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
//		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.WGS84, "48.165238,11.577473", null, null, LocationType.WGS84,
				"47.987199,11.326532", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
//		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
//		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinateAndStation() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.WGS84, "48.238341,11.478230", null, null, LocationType.ANY,
				"Ostbahnhof", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
//		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
//		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ADDRESS, "München, Maximilianstr. 1", null, null,
				LocationType.ADDRESS, "Starnberg, Jahnstraße 50", new Date(), true, WalkSpeed.NORMAL);
		System.out.println(result);
//		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
//		System.out.println(moreResult);
	}
}
