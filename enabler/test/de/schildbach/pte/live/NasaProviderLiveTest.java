/*
 * Copyright 2010-2013 the original author or authors.
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

import de.schildbach.pte.NasaProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class NasaProviderLiveTest extends AbstractProviderLiveTest
{
	public NasaProviderLiveTest()
	{
		super(new NasaProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 13000), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(13000, 0, false);

		print(result);
	}

	@Test
	public void autocomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Flughafen");

		print(autocompletes);
	}

	@Test
	public void autocompleteUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Höhle");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 11063, null, "Leipzig, Johannisplatz"), null,
				new Location(LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void anotherShortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 8010205, 51346546, 12383333, null, "Leipzig Hbf"),
				null, new Location(LocationType.STATION, 8012183, 51423340, 12223423, null, "Leipzig/Halle Flughafen"), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void outdatedConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 13002, null, "Leipzig, Augustusplatz"), null,
				new Location(LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(2011, 1, 1), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void ambiguousConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ANY, 0, null, "Platz"), null, new Location(
				LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void sameStationConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 8010205, null, "Leipzig Hbf"), null, new Location(
				LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void addressConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 51334078, 12478331, "04319 Leipzig-Engelsdorf",
				"August-Bebel-Platz"), null, new Location(LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}
}
