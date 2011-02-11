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

import de.schildbach.pte.TflProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class TflProviderLiveTest
{
	private final TflProvider provider = new TflProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void nearbyStation() throws Exception
	{
		final NearbyStationsResult result = provider.nearbyStations("1000086", 0, 0, 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures("1000086", 0, false);

		System.out.println(result.stationDepartures);
	}

	@Test
	public void postcodeConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, null, "sw19 8ta"), null, new Location(
				LocationType.STATION, 1016019, 51655903, -397249, null, "Watford (Herts), Watford Town Centre"), new Date(), true, ALL_PRODUCTS,
				WalkSpeed.NORMAL);
		System.out.println(result);
	}
}
