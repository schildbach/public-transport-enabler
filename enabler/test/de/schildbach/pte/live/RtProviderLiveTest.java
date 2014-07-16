/*
 * Copyright 2010-2014 the original author or authors.
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

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.RtProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class RtProviderLiveTest extends AbstractProviderLiveTest
{
	public RtProviderLiveTest()
	{
		super(new RtProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "8500010"), 0, 0);

		System.out.println(result.status + "  " + result.stations.size() + "  " + result.stations);
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
		final QueryDeparturesResult result = provider.queryDepartures("8588344", 0, false);

		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures("999999", 0, false);

		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("haupt");

		print(result);
	}

	@Test
	public void suggestLocationsAddress() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Dorfstrasse 10, Dällikon, Schweiz");

		print(result);
	}

	@Test
	public void suggestLocationsEncoding() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Dorfstrasse 1, Schäftland");
		assertEquals("Schöftland, Dorfstrasse", result.getLocations().get(0).name);
		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8503000", null, "Zürich HB"), null, new Location(
				LocationType.STATION, "8507785", null, "Bern, Hauptbahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void slowTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Schocherswil, Alte Post!"), null, new Location(
				LocationType.ANY, null, null, "Laconnex, Mollach"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void tripWithFootway() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, null, "Spiez, Seestraße 62"), null, new Location(
				LocationType.ADDRESS, null, null, "Einsiedeln, Erlenmoosweg 24"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void tripFromAddress() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 51521886, -51447, null,
				"26 Coopers Close, Poplar, Greater London E1 4, Vereinigtes Königreich"), null, new Location(LocationType.STATION, "8096022",
				50941312, 6967206, null, "COLOGNE"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void viaTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8400056", null, "Amsterdam RAI"), new Location(
				LocationType.STATION, "8400058", null, "Amsterdam Centraal"), new Location(LocationType.STATION, "8000085", null, "Düsseldorf Hbf"),
				new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}

	@Test
	public void crossStateTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8000207", null, "Köln Hbf"), null, new Location(
				LocationType.STATION, "6096001", null, "DUBLIN"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		System.out.println(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);
	}
}
