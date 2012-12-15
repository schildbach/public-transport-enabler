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

import static junit.framework.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.schildbach.pte.BvgProvider;
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
public class BvgProviderLiveTest extends AbstractProviderLiveTest
{
	public BvgProviderLiveTest()
	{
		super(new BvgProvider(null));
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 9220302), 0, 0);
		assertEquals(NearbyStationsResult.Status.OK, result.status);
		print(result);
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 2449475), 0, 0);
		assertEquals(NearbyStationsResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult resultLive = provider.queryDepartures(309557, 0, false);
		assertEquals(QueryDeparturesResult.Status.OK, resultLive.status);
		System.out.println(resultLive.stationDepartures);

		final QueryDeparturesResult resultPlan = provider.queryDepartures(9100003, 0, false);
		assertEquals(QueryDeparturesResult.Status.OK, resultPlan.status);
		System.out.println(resultPlan.stationDepartures);
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
		final QueryConnectionsResult later2Result = queryMoreConnections(laterResult.context, true);
		System.out.println(later2Result);
		final QueryConnectionsResult earlierResult = queryMoreConnections(later2Result.context, false);
		System.out.println(earlierResult);
		final QueryConnectionsResult later3Result = queryMoreConnections(earlierResult.context, true);
		System.out.println(later3Result);
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
	public void connectionBetweenCoordinatesAndAddresses() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52536099, 13426309, null,
				"Christburger Straße 1, 10405 Berlin, Deutschland"), null, new Location(LocationType.ADDRESS, 0, 52486400, 13350744, null,
				"Eisenacher Straße 70, 10823 Berlin, Deutschland"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, null,
				"10715 Bln Charlb.-Wilm., Weimarische Str. 7"), null, new Location(LocationType.ADDRESS, 0, null, "10178 Bln Mitte, Sophienstr. 24"),
				new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void viaConnectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, null,
				"10715 Bln Charlb.-Wilm., Weimarische Str. 7"), new Location(LocationType.ADDRESS, 0, null, "10115 Bln Mitte, Hannoversche Str. 20"),
				new Location(LocationType.ADDRESS, 0, null, "10178 Bln Mitte, Sophienstr. 24"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}
}
