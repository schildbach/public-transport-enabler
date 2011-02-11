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

import org.junit.Test;

import de.schildbach.pte.SeptaProvider;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class SeptaProviderLiveTest
{
	private final SeptaProvider provider = new SeptaProvider();

	@Test
	public void nearbyStation() throws Exception
	{
		final NearbyStationsResult result = provider.nearbyStations("1000001", 0, 0, 0, 0);

		System.out.println(result.stations.size() + "  " + result.stations);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures("1000002", 0, false);

		System.out.println(result.stationDepartures);
	}
}
