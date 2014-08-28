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
import de.schildbach.pte.SncbProvider;
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
public class SncbProviderLiveTest extends AbstractProviderLiveTest
{
	public SncbProviderLiveTest()
	{
		super(new SncbProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "8813003"), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 50748017, 3407118), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("8813003", false);

		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("999999", false);

		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Brussel S");

		print(result);
	}

	@Test
	public void suggestLocationsUmlaut() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Brüssel");

		print(result);
	}

	@Test
	public void suggestLocationsAddress() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Rue Paul Janson 9, 1030 Bruxelles");

		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8821006", "Antwerpen", "Centraal"), null, new Location(
				LocationType.STATION, "8813003", "Brussel", "Centraal"), new Date(), true, null, WalkSpeed.FAST, Accessibility.NEUTRAL);
		print(result);

		if (result.context != null)
		{
			final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
			print(laterResult);
		}
	}

	@Test
	public void longTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "207280", "Brussel", "Wannecouter"), null, new Location(
				LocationType.STATION, "207272", "Brussel", "Stadion"), new Date(), true, null, WalkSpeed.FAST, Accessibility.NEUTRAL);
		print(result);

		if (result.context != null)
		{
			final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
			print(laterResult);
		}
	}

	@Test
	public void tripFromAddress() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, null, "Bruxelles - Haren, Rue Paul Janson 9"), null,
				new Location(LocationType.STATION, "8500010", null, "Basel"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);

		if (result.context != null)
		{
			final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
			print(laterResult);
		}
	}
}
