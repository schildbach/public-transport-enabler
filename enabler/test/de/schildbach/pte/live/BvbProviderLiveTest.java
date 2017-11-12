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

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.BvbProvider;
import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.ZvvProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Matthias Cullmann
 */
public class BvbProviderLiveTest extends AbstractProviderLiveTest {
	private final Location BAHNHOF_SBB = new Location(LocationType.STATION, "51000007", null, "Bahnhof SBB");
	private final Location COORDINATES_BAHNHOF_SBB = Location.coord(47547925, 7589887);
	private final Location BRUDERHOLZ = new Location(LocationType.STATION, "51000019", null, "Bruderholz");

	public BvbProviderLiveTest() {
		super(new BvbProvider());
	}

	@Test
	public void nearbyStationsByStation() throws Exception {
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, BAHNHOF_SBB.id));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception {
		final NearbyLocationsResult result = queryNearbyStations(COORDINATES_BAHNHOF_SBB);
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception {
		final QueryDeparturesResult result = queryDepartures(BAHNHOF_SBB.id, false);
		print(result);
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception {
		final QueryDeparturesResult result = queryDepartures("999999", false);
		assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
	}

	@Test
	public void suggestLocations() throws Exception {
		final SuggestLocationsResult result = suggestLocations("Bahnhof");
		print(result);
	}

	@Test
	public void shortTrip() throws Exception {
		final QueryTripsResult result = queryTrips(BAHNHOF_SBB, null, BRUDERHOLZ, new Date(), true, Product.ALL,
				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
		print(laterResult);
	}
	
}
