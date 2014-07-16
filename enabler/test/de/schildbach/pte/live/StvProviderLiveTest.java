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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.StvProvider;
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
public class StvProviderLiveTest extends AbstractProviderLiveTest
{
	public StvProviderLiveTest()
	{
		super(new StvProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "63203040"), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 47072612, 15431814), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = provider.queryDepartures("63203040", 0, false);

		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Kur");

		print(result);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("grün");

		print(result);
	}

	@Test
	public void suggestLocationsCoverage() throws Exception
	{
		final SuggestLocationsResult grazResult = provider.suggestLocations("Graz Brauhaus");
		print(grazResult);
		assertThat(grazResult.getLocations(), hasItem(new Location(LocationType.STATION, "63203044")));

		final SuggestLocationsResult leobenResult = provider.suggestLocations("Leoben Blockhäuser");
		print(leobenResult);
		assertThat(leobenResult.getLocations(), hasItem(new Location(LocationType.STATION, "63206224")));

		final SuggestLocationsResult bruckResult = provider.suggestLocations("Bruck Hauptplatz");
		print(bruckResult);
		assertThat(bruckResult.getLocations(), hasItem(new Location(LocationType.STATION, "63202063")));

		final SuggestLocationsResult kindbergResult = provider.suggestLocations("Kindberg Friedhof");
		print(kindbergResult);
		assertThat(kindbergResult.getLocations(), hasItem(new Location(LocationType.STATION, "63208877")));

		final SuggestLocationsResult mariborResult = provider.suggestLocations("Maribor Dravograjska Sokolska");
		print(mariborResult);
		assertThat(mariborResult.getLocations(), hasItem(new Location(LocationType.STATION, "63300136")));
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "63203040", null, "Graz Hauptbahnhof"), null, new Location(
				LocationType.STATION, "63203149", null, "Graz Babenbergerstraße"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		System.out.println(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		System.out.println(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		System.out.println(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		System.out.println(earlierResult);
	}
}
