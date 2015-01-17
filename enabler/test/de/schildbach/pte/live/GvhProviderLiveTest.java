/*
 * Copyright 2010-2015 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.GvhProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
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
public class GvhProviderLiveTest extends AbstractProviderLiveTest
{
	public GvhProviderLiveTest()
	{
		super(new GvhProvider(null));
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "25000031"), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 52379497, 9735832), 0, 0);

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("25000031", false);

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
	public void suggestLocationsIdentified() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Hannover, Hannoversche Straße");

		print(result);
	}

	@Test
	public void suggestLocationsCity() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Hannover");

		print(result);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Hannover");

		print(result);
	}

	@Test
	public void incompleteTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "hann"), null, new Location(LocationType.ANY, null,
				null, "laat"), new Date(), true, Product.ALL, WalkSpeed.FAST, Accessibility.NEUTRAL);
		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "25000031", null, "Hannover Hauptbahnhof"), null, new Location(
				LocationType.STATION, "25001141", null, "Hannover Bismarckstraße"), new Date(), true, Product.ALL, WalkSpeed.FAST,
				Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);

		if (!result.context.canQueryLater())
			return;

		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);

		if (!laterResult.context.canQueryLater())
			return;

		final QueryTripsResult later2Result = queryMoreTrips(laterResult.context, true);
		print(later2Result);

		if (!later2Result.context.canQueryEarlier())
			return;

		final QueryTripsResult earlierResult = queryMoreTrips(later2Result.context, false);
		print(earlierResult);
	}

	@Test
	public void tripBetweenAnyAndAddress() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, 53069619, 8799202, null, "bremen, neustadtswall 12"), null,
				new Location(LocationType.ADDRESS, null, 53104124, 8788575, null, "Bremen Glücksburger Straße 37"), new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}

	@Test
	public void tripBetweenAddresses() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, null, 53622859, 10133545, null,
				"Zamenhofweg 14, 22159 Hamburg, Deutschland"), null, new Location(LocationType.ADDRESS, null, 53734260, 9674990, null,
				"Lehmkuhlen 5, 25337 Elmshorn, Deutschland"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
}
