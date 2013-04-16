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

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.BahnProvider;
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
public class BahnProviderLiveTest extends AbstractProviderLiveTest
{
	public BahnProviderLiveTest()
	{
		super(new BahnProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 692991), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 52525589, 13369548), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(692991, 0, false);

		print(result);
	}

	@Test
	public void autocompleteUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Güntzelstr. (U)");

		print(autocompletes);

		assertEquals("Güntzelstr. (U), Berlin", autocompletes.get(0).name);
	}

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Landungsbr");

		print(autocompletes);

		assertEquals("Hamburg Landungsbrücken", autocompletes.get(0).name);
	}

	@Test
	public void autocompleteIdentified() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Berlin");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 8011160, null, "Berlin Hbf"), null, new Location(
				LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 732655, 52535576, 13422171, null,
				"Marienburger Str., Berlin"), null, new Location(LocationType.STATION, 623234, 48000221, 11342490, null,
				"Tutzinger-Hof-Platz, Starnberg"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;

		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void noConnections() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 513729, null, "Schillerplatz, Kaiserslautern"),
				null, new Location(LocationType.STATION, 403631, null, "Trippstadt Grundschule"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
	}

	@Test
	public void connectionWithFootway() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.ADDRESS, 0, 52517139, 13388749, null,
				"Berlin - Mitte, Unter den Linden 24"), null, new Location(LocationType.ADDRESS, 0, 47994243, 11338543, null,
				"Starnberg, Possenhofener Straße 13"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		if (!result.context.canQueryLater())
			return;

		final QueryConnectionsResult laterResult = queryMoreConnections(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionsTooClose() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 8010205, null, "Leipzig Hbf"), null, new Location(
				LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		assertEquals(QueryConnectionsResult.Status.TOO_CLOSE, result.status);
	}

	@Test
	public void connectionsInvalidDate() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(new Location(LocationType.STATION, 8011160, null, "Berlin Hbf"), null, new Location(
				LocationType.STATION, 8010205, null, "Leipzig Hbf"), new Date(0), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);

		assertEquals(QueryConnectionsResult.Status.INVALID_DATE, result.status);
	}
}
