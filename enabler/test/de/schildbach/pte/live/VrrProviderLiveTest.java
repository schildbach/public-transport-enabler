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
import de.schildbach.pte.VrrProvider;
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
public class VrrProviderLiveTest extends AbstractProviderLiveTest
{
	public VrrProviderLiveTest()
	{
		super(new VrrProvider());
	}

	@Test
	public void nearbyStations() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, "20019904"), 0, 0);

		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception
	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 51218693, 6777785), 0, 0);

		print(result);

		final NearbyStationsResult result2 = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 51719648, 8754330), 0, 0);

		print(result2);
	}

	@Test
	public void queryDepartures() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("1007258", false);

		print(result);

		final QueryDeparturesResult result2 = queryDepartures("20019904", false);

		print(result2);

		// Bonn
		queryDepartures("22000687", false); // Hauptbahnhof
		queryDepartures("22001374", false); // Suedwache
	}

	@Test
	public void queryManyDeparturesWithEquivs() throws Exception
	{
		final QueryDeparturesResult result = queryDepartures("20018235", true);

		print(result);
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Kur");

		print(result);

		final SuggestLocationsResult paderbornResult = provider.suggestLocations("Paderborn Hbf");

		print(paderbornResult);
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
		final SuggestLocationsResult result = provider.suggestLocations("Düsseldorf, Am Frohnhof");

		print(result);
	}

	@Test
	public void suggestLocationsCoverage() throws Exception
	{
		final SuggestLocationsResult cologneResult = provider.suggestLocations("Köln Ebertplatz");
		print(cologneResult);
		assertThat(cologneResult.getLocations(), hasItem(new Location(LocationType.STATION, "22000035")));

		final SuggestLocationsResult dortmundResult = provider.suggestLocations("Dortmund Zugstraße");
		print(dortmundResult);
		assertThat(dortmundResult.getLocations(), hasItem(new Location(LocationType.STATION, "20000524")));

		final SuggestLocationsResult duesseldorfResult = provider.suggestLocations("Düsseldorf Sternstraße");
		print(duesseldorfResult);
		assertThat(duesseldorfResult.getLocations(), hasItem(new Location(LocationType.STATION, "20018017")));

		final SuggestLocationsResult muensterResult = provider.suggestLocations("Münster Vennheideweg");
		print(muensterResult);
		assertThat(muensterResult.getLocations(), hasItem(new Location(LocationType.STATION, "24047291")));

		final SuggestLocationsResult aachenResult = provider.suggestLocations("Aachen Elisenbrunnen");
		print(aachenResult);
		assertThat(aachenResult.getLocations(), hasItem(new Location(LocationType.STATION, "21001029")));
	}

	@Test
	public void suggestLocationsCity() throws Exception
	{
		final SuggestLocationsResult result = provider.suggestLocations("Düsseldorf");

		print(result);
	}

	@Test
	public void shortTrip() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "20009289", "Essen", "Hauptbahnhof"), null, new Location(
				LocationType.STATION, "20009161", "Essen", "Bismarckplatz"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
	public void shortTripPaderborn() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "23007000", "Paderborn", "Paderborn Hbf"), null, new Location(
				LocationType.STATION, "23007700", "Höxter", "Bahnhof / Rathaus"), new Date(), true, Product.ALL, WalkSpeed.NORMAL,
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
	public void shortTripDorsten() throws Exception
	{
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "20009643", "Bottrop", "West S"), null, new Location(
				LocationType.STATION, "20003214", "Dorsten", "ZOB Dorsten"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}
}
