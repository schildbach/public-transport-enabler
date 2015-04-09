/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.RsagProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 */
public class RsagProviderLiveTest extends AbstractProviderLiveTest
{
	public RsagProviderLiveTest()
	{
		super(new RsagProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8010304"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(Location.coord(54078314, 12131715));
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("8010304", false);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Rostock");
		print(result);
	}

	@Test
	public void suggestLocationsUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Lütten Klein");
		print(result);
		assertEquals("Lütten Klein", result.getLocations().get(0).name);
	}

	@Test
	public void suggestLocationsCoverage() throws Exception
	{
		final SuggestLocationsResult rostockResult = suggestLocations("Rostock");
		print(rostockResult);
		assertThat(rostockResult.getLocations(), hasItem(new Location(LocationType.STATION, "8010304")));

		final SuggestLocationsResult warnemuendeResult = suggestLocations("Warnemünde");
		print(warnemuendeResult);
		assertThat(warnemuendeResult.getLocations(), hasItem(new Location(LocationType.STATION, "8013236")));
	}

	@Test
	public void suggestLocationsLocality() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("lange");
		assertEquals("Rostock", result.getLocations().get(0).place);
		assertEquals("Lange Straße", result.getLocations().get(0).name);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8096109", null, "Oldenburg"), null, new Location(
				LocationType.STATION, "625398", null, "Bremerhaven"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripDateOutsideTimetablePeriod() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8096109", null, "Oldenburg"), null, new Location(
				LocationType.STATION, "625398", null, "Bremerhaven"), new Date(1155822689759l), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.INVALID_DATE, result.status);
	}
}
