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

import de.schildbach.pte.SbbProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class SbbProviderLiveTest
{
	private SbbProvider provider = new SbbProvider(Secrets.SBB_ACCESS_ID);
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void autoComplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("haupt");

		list(autocompletes);
	}

	private void list(final List<Location> autocompletes)
	{
		System.out.print(autocompletes.size() + " ");
		for (final Location autocomplete : autocompletes)
			System.out.print(autocomplete.toDebugString() + " ");
		System.out.println();
	}

	@Test
	public void nearbyStation() throws Exception
	{
		final NearbyStationsResult result = provider.nearbyStations("8500010", 0, 0, 0, 0);

		System.out.println(result.status + "  " + result.stations.size() + "  " + result.stations);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures("8500010", 0);

		System.out.println(result.status + "  " + result.departures.size() + "  " + result.departures);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.STATION, 8503000, 0, 0, "Zürich HB"), null,
				new Location(LocationType.STATION, 8507785, 0, 0, "Bern, Hauptbahnhof"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 0, 0, "Schocherswil, Alte Post!"), null,
				new Location(LocationType.ANY, 0, 0, 0, "Laconnex, Mollach"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 0, 0, "Spiez, Seestraße 62"), null,
				new Location(LocationType.ADDRESS, 0, 0, 0, "Einsiedeln, Erlenmoosweg 24"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context);
		System.out.println(moreResult);
	}
}
