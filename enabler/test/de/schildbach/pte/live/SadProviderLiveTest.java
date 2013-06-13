/*
 * Copyright 2012-2013 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.SadProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.QueryTripsResult.Status;
import de.schildbach.pte.dto.Trip.Individual;
import de.schildbach.pte.util.Iso8601Format;

/**
 * @author Oliver Gasser
 */
public class SadProviderLiveTest extends AbstractProviderLiveTest {

	private DateFormat dateTimeFormat = Iso8601Format.newDateTimeFormat();

	public SadProviderLiveTest()
	{
		super(new SadProvider());
	}

	@Test
	public void autoComplete() throws Exception {
		final List<Location> autocompletes = provider.autocompleteStations("haupt");

		print(autocompletes);

		assertFalse(autocompletes.isEmpty());
	}

	@Test
	public void tooCloseTrip() throws Exception {
		List<Location> schuffa = provider.autocompleteStations("Welschnofen");
		final QueryTripsResult result = queryTrips(schuffa.get(0), null, schuffa.get(0),
				dateTimeFormat.parse("2012-04-01 12:30:00"), true, null, null, null);

		System.out.println(result);

		assertEquals(Status.TOO_CLOSE, result.status);
	}

	@Test
	public void invalidDate() throws Exception {
		List<Location> bz = provider.autocompleteStations("Bozen Bhf.");
		List<Location> schuffa = provider.autocompleteStations("Welschnofen");

		final QueryTripsResult result = queryTrips(bz.get(0), null, schuffa.get(0),
				dateTimeFormat.parse("2011-04-01 12:30:00"), true, null, null, null);

		System.out.println(result);

		assertEquals(Status.INVALID_DATE, result.status);
	}

	@Test
	public void tripWithFootway() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 0, null, "Bozen Bhf."), null,
				new Location(LocationType.STATION, 0, null, "Bundschen"), dateTimeFormat.parse("2012-04-01 12:30:00"), true, null, null,
				null);

		System.out.println(result);

		assertEquals(Status.OK, result.status);

		assertFalse(result.trips.isEmpty());

		assertTrue(result.trips.get(0).legs.get(0) instanceof Individual);
	}

	@Test
	public void noTrips() throws Exception {
		QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 0, null, "Bozen Bhf."), null,
				new Location(LocationType.STATION, 0, null, "Welschnofen"), dateTimeFormat.parse("2012-04-01 22:30:00"), true, null, null,
				null);

		System.out.println(result);

		// No trips between 22:30 and 23:59
		assertEquals(Status.NO_TRIPS, result.status);

		assertNull(result.trips);
	}

	@Test
	public void queryMoreTrips() throws Exception {
		// Trips between 05:30 and 10:30
		QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 0, null, "Bozen Bhf."), null,
				new Location(LocationType.STATION, 0, null, "Welschnofen"), dateTimeFormat.parse("2012-04-01 05:30:00"), true, null, null,
				null);

		System.out.println(result);

		assertEquals(Status.OK, result.status);

		assertFalse(result.trips.isEmpty());

		// No trips between 04:30 and 05:30
		QueryTripsResult moreResult = queryMoreTrips(result.context, false);

		System.out.println(moreResult);

		assertEquals(Status.NO_TRIPS, moreResult.status);

		assertNull(moreResult.trips);

		// Trips between 09:30 and 14:30
		moreResult = queryMoreTrips(result.context, true);

		System.out.println(moreResult);

		assertEquals(Status.OK, moreResult.status);

		assertFalse(moreResult.trips.isEmpty());

		System.out.println(moreResult);
	}

	@Test
	public void queryAmbiguous() throws Exception {
		// No ambiguities
		QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 0, null, "Welschn"), null,
				new Location(LocationType.STATION, 0, null, "ozen Bh"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null,
				null);

		System.out.println(result);

		assertEquals(Status.OK, result.status);

		assertFalse(result.trips.isEmpty());

		// Ambiguous departure
		result = queryTrips(new Location(LocationType.STATION, 0, null, "Welsch"), null, new Location(LocationType.STATION,
				0, null, "ozen Bh"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null, null);

		System.out.println(result);

		assertEquals(Status.AMBIGUOUS, result.status);

		assertFalse(result.ambiguousFrom.isEmpty());

		assertTrue(result.ambiguousFrom.size() > 1);

		assertFalse(result.ambiguousTo.isEmpty());

		assertFalse(result.ambiguousTo.size() > 1);

		// Ambiguous arrival
		result = queryTrips(new Location(LocationType.STATION, 0, null, "Welschn"), null, new Location(LocationType.STATION,
				0, null, "oze"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null, null);

		System.out.println(result);

		assertEquals(Status.AMBIGUOUS, result.status);

		assertFalse(result.ambiguousFrom.isEmpty());

		assertFalse(result.ambiguousFrom.size() > 1);

		assertFalse(result.ambiguousTo.isEmpty());

		assertTrue(result.ambiguousTo.size() > 1);

		// Ambiguous departure and arrival
		result = queryTrips(new Location(LocationType.STATION, 0, null, "Welsch"), null, new Location(LocationType.STATION,
				0, null, "oze"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null, null);

		System.out.println(result);

		assertEquals(Status.AMBIGUOUS, result.status);

		assertFalse(result.ambiguousFrom.isEmpty());

		assertTrue(result.ambiguousFrom.size() > 1);

		assertFalse(result.ambiguousTo.isEmpty());

		assertTrue(result.ambiguousTo.size() > 1);
	}

	@Test
	public void queryUnkown() throws Exception {
		// Unknown from
		QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 0, null, "Welschnoffen"), null,
				new Location(LocationType.STATION, 0, null, "ozen Bh"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null,
				null);

		System.out.println(result);

		assertEquals(Status.UNKNOWN_FROM, result.status);

		assertNull(result.trips);

		// Unknown to
		result = queryTrips(new Location(LocationType.STATION, 0, null, "Welsch"), null, new Location(LocationType.STATION,
				0, null, "ozenn Bh"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null, null);

		System.out.println(result);

		assertEquals(Status.UNKNOWN_TO, result.status);

		assertNull(result.trips);

		// Unknown from and to
		result = queryTrips(new Location(LocationType.STATION, 0, null, "Welschnoffen"), null, new Location(
				LocationType.STATION, 0, null, "ozenn Bh"), dateTimeFormat.parse("2012-04-01 12:30:00"), false, null, null, null);

		System.out.println(result);

		assertTrue(Status.UNKNOWN_FROM == result.status || Status.UNKNOWN_TO == result.status);

		assertNull(result.trips);
	}
}
