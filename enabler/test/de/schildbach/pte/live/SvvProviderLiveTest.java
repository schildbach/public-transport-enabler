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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.SvvProvider;
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
public class SvvProviderLiveTest extends AbstractProviderLiveTest
{
	public SvvProviderLiveTest()
	{
		super(new SvvProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "60650002"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyLocationsResult result = queryNearbyStations(Location.coord(47809195, 13054919));
		print(result);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("60650002", false);
		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Kur");
		print(result);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception
	{
		final SuggestLocationsResult result = suggestLocations("Salzburg Süd");
		print(result);
		assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "60650458")));
	}

	@Test
	public void suggestLocationsCoverage() throws Exception
	{
		final SuggestLocationsResult salzburgResult = suggestLocations("Salzburg Süd");
		print(salzburgResult);
		assertThat(salzburgResult.getLocations(), hasItem(new Location(LocationType.STATION, "60650458")));

		final SuggestLocationsResult strasswalchenResult = suggestLocations("Straßwalchen West");
		print(strasswalchenResult);
		assertThat(strasswalchenResult.getLocations(), hasItem(new Location(LocationType.STATION, "60656483")));

		final SuggestLocationsResult schwarzachResult = suggestLocations("Schwarzach Abtsdorf");
		print(schwarzachResult);
		assertThat(schwarzachResult.getLocations(), hasItem(new Location(LocationType.STATION, "60656614")));

		final SuggestLocationsResult trimmelkamResult = suggestLocations("Trimmelkam");
		print(trimmelkamResult);
		assertThat(trimmelkamResult.getLocations(), hasItem(new Location(LocationType.STATION, "60640776")));
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "60650021", 47797036, 13053608, "Salzburg", "Justizgebäude"),
				null, new Location(LocationType.STATION, "60650022", 47793760, 13059338, "Salzburg", "Akademiestraße"), new Date(), true,
				Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
}
