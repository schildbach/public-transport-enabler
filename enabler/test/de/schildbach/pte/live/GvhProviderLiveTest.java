/*
 * Copyright 2010-2012 the original author or authors.
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

import de.schildbach.pte.GvhProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class GvhProviderLiveTest extends AbstractProviderLiveTest
{
	private final GvhProvider provider = new GvhProvider(null);

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 25000031), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 52379497, 9735832), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(25000031, 0, false);

		print(result);
	}

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Kur");

		print(autocompletes);
	}

	@Test
	public void autocompleteWithUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("grün");

		print(autocompletes);
	}

	@Test
	public void autocompleteIdentified() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Hannover, Hannoversche Straße");

		print(autocompletes);
	}

	@Test
	public void autocompleteCity() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Hannover");

		print(autocompletes);
	}

	@Test
	public void autocomplete() throws Exception
	{
		final List<Location> results = provider.autocompleteStations("Hannover");

		System.out.println(results.size() + "  " + results);
	}

	@Test
	public void incompleteConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, null, "hann"), null, new Location(
				LocationType.ANY, 0, null, "laat"), new Date(), true, ALL_PRODUCTS, WalkSpeed.FAST, Accessibility.NEUTRAL);
		System.out.println(result);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.STATION, 25000031, null, "Hannover Hauptbahnhof"),
				null, new Location(LocationType.STATION, 25001141, null, "Hannover Bismarckstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.FAST,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = provider.queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenAnyAndAddress() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ANY, 0, 53069619, 8799202, null,
				"bremen, neustadtswall 12"), null, new Location(LocationType.ADDRESS, 0, 53104124, 8788575, null, "Bremen Glücksburger Straße 37"),
				new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = provider.queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 53622859, 10133545, null,
				"Zamenhofweg 14, 22159 Hamburg, Deutschland"), null, new Location(LocationType.ADDRESS, 0, 53734260, 9674990, null,
				"Lehmkuhlen 5, 25337 Elmshorn, Deutschland"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = provider.queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}
}
