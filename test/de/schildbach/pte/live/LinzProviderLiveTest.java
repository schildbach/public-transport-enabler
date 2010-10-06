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

import de.schildbach.pte.LinzProvider;
import de.schildbach.pte.NetworkProvider.LocationType;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Autocomplete;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class LinzProviderLiveTest
{
	private LinzProvider provider = new LinzProvider();
	private static final String ALL_PRODUCTS = "IRSUTBFC";

	@Test
	public void autocompleteIncomplete() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Linz, H");

		list(autocompletes);
	}

	@Test
	public void autocompleteIdentified() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Leonding, Haag");

		list(autocompletes);
	}

	@Test
	public void autocompleteCity() throws Exception
	{
		final List<Autocomplete> autocompletes = provider.autocompleteStations("Linz");

		list(autocompletes);
	}

	private void list(final List<Autocomplete> autocompletes)
	{
		System.out.print(autocompletes.size() + " ");
		for (final Autocomplete autocomplete : autocompletes)
			System.out.print(autocomplete.toDebugString() + " ");
		System.out.println();
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures(provider.departuresQueryUri("60501720", 0));
		System.out.println(result);
	}

	@Test
	public void shortConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.STATION, "Linz Hauptbahnhof", null, null, LocationType.STATION,
				"Linz Auwiesen", new Date(), true, ALL_PRODUCTS, WalkSpeed.FAST);
		System.out.println(result);
		// final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		// System.out.println(moreResult);
	}

	@Test
	public void longConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.STATION, "Linz Auwiesen", null, null, LocationType.STATION,
				"Linz Hafen", new Date(), true, ALL_PRODUCTS, WalkSpeed.SLOW);
		System.out.println(result);
		// final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		// System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinates() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.WGS84, "48165238,11577473", null, null, LocationType.WGS84,
				"47987199,11326532", new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		// final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		// System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenCoordinateAndStation() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.WGS84, "48238341,11478230", null, null, LocationType.ANY,
				"Ostbahnhof", new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		// final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		// System.out.println(moreResult);
	}

	@Test
	public void connectionBetweenAddresses() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections(LocationType.ADDRESS, "München, Maximilianstr. 1", null, null,
				LocationType.ADDRESS, "Starnberg, Jahnstraße 50", new Date(), true, ALL_PRODUCTS, WalkSpeed.NORMAL);
		System.out.println(result);
		// final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		// System.out.println(moreResult);
	}
}
