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
	private VbbProvider provider = new VbbProvider();

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 9007102), 0, 0);

		print(result);
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
	public void autocompleteIncomplete() throws Exception
	{
		final List<Location> autocompletes = provider.autocompleteStations("nol");

		print(autocompletes);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.STATION, 9056102, "Berlin", "Nollendorfplatz"),
				null, new Location(LocationType.STATION, 9013103, "Berlin", "Prinzenstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult nextResult = provider.queryMoreConnections(result.context, true);
		System.out.println(nextResult);
		final QueryConnectionsResult previousResult = provider.queryMoreConnections(result.context, false);
		System.out.println(previousResult);
	}

	@Test
	public void shortViaConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.STATION, 9056102, "Berlin", "Nollendorfplatz"),
				new Location(LocationType.STATION, 9044202, "Berlin", "Bundesplatz"), new Location(LocationType.STATION, 9013103, "Berlin",
						"Prinzenstraße"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context, true);
		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 52501507, 13357026, null, null), null,
				new Location(LocationType.ADDRESS, 0, 52513639, 13568648, null, null), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context, true);
		System.out.println(moreResult);
	}

	@Test
	public void viaConnectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, 52501507, 13357026, null, null),
				new Location(LocationType.ADDRESS, 0, 52479868, 13324247, null, null), new Location(LocationType.ADDRESS, 0, 52513639, 13568648,
						null, null), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context, true);
		System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, null,
				"10715 Bln Charlb.-Wilm., Weimarische Str. 7"), null, new Location(LocationType.ADDRESS, 0, null, "10178 Bln Mitte, Sophienstr. 24"),
				new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context, true);
		System.out.println(moreResult);
	}

	@Test
	public void viaConnectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(new Location(LocationType.ADDRESS, 0, null,
				"10715 Bln Charlb.-Wilm., Weimarische Str. 7"), new Location(LocationType.ADDRESS, 0, null, "10115 Bln Mitte, Hannoversche Str. 20"),
				new Location(LocationType.ADDRESS, 0, null, "10178 Bln Mitte, Sophienstr. 24"), new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.context, true);
		System.out.println(moreResult);
	}
}
