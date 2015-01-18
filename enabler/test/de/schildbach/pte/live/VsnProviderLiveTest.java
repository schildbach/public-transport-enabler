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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.VsnProvider;
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
public class VsnProviderLiveTest extends AbstractProviderLiveTest
{
	public VsnProviderLiveTest()
	{
		super(new VsnProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8000128"));

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = queryNearbyStations(new Location(LocationType.ADDRESS, 51536614, 9925673));

		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("8000128", false);

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
		final SuggestLocationsResult result = suggestLocations("Hannover");

		print(result);
	}

	@Test
	public void suggestLocationsUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Göttingen");

		print(result);
	}

	@Test
	public void suggestLocationsWithoutCoordinatesInResult() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("aachen");

		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8000128", null, "Göttingen"), null, new Location(
				LocationType.STATION, "1140061", null, "Göttingen Nikolausberger Weg"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
				Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
}
