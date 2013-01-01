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

import static junit.framework.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.VbbProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class VbbProviderLiveTest extends AbstractProviderLiveTest
{
	public VbbProviderLiveTest()
	{
		super(new VbbProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 9007102), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 2449475), 0, 0);
		assertEquals(NearbyStationsResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 52548505, 1338864), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(9007102, 0, false);

		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult resultLive = provider.queryDepartures(111111, 0, false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultLive.status);

		final QueryDeparturesResult resultPlan = provider.queryDepartures(2449475, 0, false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, resultPlan.status);
	}

	@Test
	public void autocompleteUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Güntzelstr.");

		print(autocompletes);

		Assert.assertEquals("Güntzelstr. (U)", autocompletes.get(0).name);
	}

	@Test
	public void autocompleteAddress() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Sophienstr. 24");

		print(autocompletes);

		Assert.assertEquals("Sophienstr. 24", autocompletes.get(0).name);
	}

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("nol");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 9056102, "Berlin", "Nollendorfplatz"), null,
				new Location(LocationType.STATION, 9013103, "Berlin", "Prinzenstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
		final QueryConnectionsResult earlierResult = queryMoreConnections(laterResult.context, false);
		System.out.println(earlierResult);
	}

	@Test
	public void shortViaConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 9056102, "Berlin", "Nollendorfplatz"),
				new Location(LocationType.STATION, 9044202, "Berlin", "Bundesplatz"), new Location(LocationType.STATION, 9013103, "Berlin",
						"Prinzenstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52501507, 13357026, null, null), null,
				new Location(LocationType.ADDRESS, 0, 52513639, 13568648, null, null), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void viaConnectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52501507, 13357026, null, null), new Location(
				LocationType.ADDRESS, 0, 52479868, 13324247, null, null), new Location(LocationType.ADDRESS, 0, 52513639, 13568648, null, null),
				new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52479663, 13324278, "10715 Berlin-Wilmersdorf",
				"Weimarische Str. 7"), null, new Location(LocationType.ADDRESS, 0, 52541536, 13421290, "10437 Berlin-Prenzlauer Berg",
				"Göhrener Str. 5"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void viaConnectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52479663, 13324278, "10715 Berlin-Wilmersdorf",
				"Weimarische Str. 7"), new Location(LocationType.ADDRESS, 0, 52527872, 13381657, "10115 Berlin-Mitte", "Hannoversche Str. 20"),
				new Location(LocationType.ADDRESS, 0, 52526029, 13399878, "10178 Berlin-Mitte", "Sophienstr. 24"), new Date(), true, ALL_PRODUCTS,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}
}
