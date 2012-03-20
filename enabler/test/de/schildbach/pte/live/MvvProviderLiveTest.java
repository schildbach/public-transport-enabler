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

import de.schildbach.pte.MvvProvider;
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
public class MvvProviderLiveTest extends AbstractProviderLiveTest
{
	private MvvProvider provider = new MvvProvider();

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 350), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 48135232, 11560650), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(2, 0, false);

		print(result);
	}

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("Marien");

		print(autocompletes);
	}

	@Test
	public void autocompleteWithUmlaut() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("grün");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.ANY, 0, null, "Marienplatz"), null, new Location(
				LocationType.ANY, 0, null, "Pasing"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
		final QueryConnectionsResult earlierResult = queryMoreConnections(provider, laterResult.context, false);
		System.out.println(earlierResult);
	}

	@Test
	public void longConnection() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.ANY, 0, null, "Starnberg, Arbeitsamt"), null,
				new Location(LocationType.STATION, 0, null, "Ackermannstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		// seems like there are no more connections all the time
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.ADDRESS, 0, 48165238, 11577473), null,
				new Location(LocationType.ADDRESS, 0, 47987199, 11326532), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenCoordinateAndStation() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.ADDRESS, 0, 48238341, 11478230), null,
				new Location(LocationType.ANY, 0, null, "Ostbahnhof"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.ADDRESS, 0, null, "München, Maximilianstr. 1"),
				null, new Location(LocationType.ADDRESS, 0, null, "Starnberg, Jahnstraße 50"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void connectionBetweenStationAndAddress() throws Exception
	{
		final QueryConnectionsResult result = queryConnections(provider, new Location(LocationType.STATION, 1220, null, "Josephsburg"), null,
				new Location(LocationType.ADDRESS, 0, 48188018, 11574239, null, "München Frankfurter Ring 35"), new Date(), true, ALL_PRODUCTS,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult laterResult = queryMoreConnections(provider, result.context, true);
		System.out.println(laterResult);
	}
}
