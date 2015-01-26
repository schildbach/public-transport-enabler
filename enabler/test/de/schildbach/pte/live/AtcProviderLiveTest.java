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

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.AtcProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
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
public class AtcProviderLiveTest extends AbstractProviderLiveTest
{
	public AtcProviderLiveTest()
	{
		super(new AtcProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "740"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		// TODO bad coordinate!
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.ADDRESS, 8168907, 10609969));
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("740", false);
		print(result);
	}

	@Test
	public void suggestLocations() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("ponte");
		print(result);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("grünwink");
		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, null, null, "Hauptwache"), null, new Location(
				LocationType.STATION, null, null, "Südbahnhof"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
}
