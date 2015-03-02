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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.VrsProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Michael Dyrna
 */
public class VrsProviderLiveTest extends AbstractProviderLiveTest {
	public VrsProviderLiveTest() {
		super(new VrsProvider());
	}

	@Test
	public void nearbyStations() throws Exception {
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "8"));
		print(result);
	}

	@Test
	public void nearbyStationsByCoordinate() throws Exception {
		final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.ANY, 51218693, 6777785));
		print(result);

		final NearbyLocationsResult result2 = queryNearbyStations(new Location(LocationType.ANY, 51719648, 8754330));
		print(result2);
	}

	@Test
	public void nearbyLocationsByCoordinate() throws Exception {
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ANY, 50732100, 7096820), 100, 1);
		print(result);

		final NearbyLocationsResult result2 = queryNearbyLocations(EnumSet.of(LocationType.ADDRESS), new Location(LocationType.ANY, 50732100, 7096820));
		print(result2);

		final NearbyLocationsResult result3 = queryNearbyLocations(EnumSet.of(LocationType.POI), new Location(LocationType.ANY, 50732100, 7096820));
		print(result3);

		final NearbyLocationsResult result4 = queryNearbyLocations(EnumSet.of(LocationType.ADDRESS, LocationType.STATION), new Location(LocationType.ANY, 50732100, 7096820));
		print(result4);
	}

	@Test
	public void nearbyLocationsByRandomCoordinates() throws Exception {
		Random rand = new Random(new Date().getTime());
		int LAT_FROM = 50500000;
		int LAT_TO = 51600000;
		int LON_FROM = 6200000;
		int LON_TO = 7600000;
		for (int i = 0; i < 10; i++) {
			int lat = LAT_FROM + rand.nextInt(LAT_TO - LAT_FROM);
			int lon = LON_FROM + rand.nextInt(LON_TO - LON_FROM);
			NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.ANY), new Location(LocationType.ANY, lat, lon));
			System.out.println(result);
			assertNotNull(result.locations);
			assertNotNull(result.locations.get(0));
		}
	}
	
	@Test
	public void nearbyStationsWithLimits() throws Exception {
		final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ANY, 50732100, 7096820), 0, 0);
		print(result);

		final NearbyLocationsResult result2 = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ANY, 50732100, 7096820), 0, 50);
		print(result2);

		final NearbyLocationsResult result3 = queryNearbyLocations(EnumSet.of(LocationType.STATION), new Location(LocationType.ANY, 50732100, 7096820), 1000, 50);
		print(result3);
	}

	@Test
	public void queryDepartures() throws Exception {
		final QueryDeparturesResult result = queryDepartures("687", false);
		print(result);
	}

	@Ignore
	@Test
	public void queryManyDepartures() throws Exception {
		Set<Integer> skip = new HashSet<Integer>(Arrays.asList(new Integer[] { 5, 10, 14, 16, 33, 49, 50, 55 }));
		for (Integer i = 1; i < 55; i++) {
			if (!skip.contains(i)) {
				final QueryDeparturesResult result = queryDepartures(i.toString(), false);
				print(result);
			}
		}
	}

	@Test
	public void suggestLocationsIncomplete() throws Exception {
		final SuggestLocationsResult stationResult = suggestLocations("Beuel");
		print(stationResult);

		final SuggestLocationsResult addressResult = suggestLocations("Lützow 41 Köln");
		print(addressResult);

		final SuggestLocationsResult poiResult = suggestLocations("Schokolade");
		print(poiResult);
	}

	@Test
	public void suggestLocationsWithUmlaut() throws Exception {
		final SuggestLocationsResult result = suggestLocations("grün");
		print(result);
	}

	@Test
	public void suggestLocationsIdentified() throws Exception {
		final SuggestLocationsResult result = suggestLocations("Düsseldorf, Am Frohnhof");
		print(result);
	}

	@Test
	public void suggestLocationsCoverage() throws Exception {
		final SuggestLocationsResult cologneResult = suggestLocations("Köln Ebertplatz");
		print(cologneResult);
		assertThat(cologneResult.getLocations(), hasItem(new Location(LocationType.STATION, "35")));

		final SuggestLocationsResult dortmundResult = suggestLocations("Dortmund Zugstraße");
		print(dortmundResult);
		assertThat(dortmundResult.getLocations(), hasItem(new Location(LocationType.STATION, "54282")));

		final SuggestLocationsResult duesseldorfResult = suggestLocations("Düsseldorf Sternstraße");
		print(duesseldorfResult);
		assertThat(duesseldorfResult.getLocations(), hasItem(new Location(LocationType.STATION, "52839")));

		final SuggestLocationsResult muensterResult = suggestLocations("Münster Vennheideweg");
		print(muensterResult);
		assertThat(muensterResult.getLocations(), hasItem(new Location(LocationType.STATION, "41112")));

		final SuggestLocationsResult aachenResult = suggestLocations("Aachen Elisenbrunnen");
		print(aachenResult);
		assertThat(aachenResult.getLocations(), hasItem(new Location(LocationType.STATION, "20580")));

		final SuggestLocationsResult bonnResult = suggestLocations("Bonn Konrad-Adenauer-Platz");
		print(aachenResult);
		assertThat(bonnResult.getLocations(), hasItem(new Location(LocationType.STATION, "1500")));
	}

	@Test
	public void suggestLocationsCity() throws Exception {
		final SuggestLocationsResult result = suggestLocations("Düsseldorf");
		print(result);
	}

	@Test
	public void anyTripAmbiguous() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "Köln"), new Location(LocationType.ANY, null, null, "Leverkusen"), new Location(LocationType.ANY, null, null, "Bonn"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.AMBIGUOUS, result.status);
		assertNotNull(result.ambiguousFrom);
		assertNotNull(result.ambiguousVia);
		assertNotNull(result.ambiguousTo);
	}

	@Test
	public void anyTripUnique() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "T-Mobile"), null, new Location(LocationType.ANY, null, null, "Schauspielhalle"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
	}

	@Test
	public void anyTripUnknown() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, null, null, "\1"), null, new Location(LocationType.ANY, null, null, "\2"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		assertEquals(QueryTripsResult.Status.UNKNOWN_FROM, result.status);
	}

	@Test
	public void shortTrip() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "8"), null, new Location(LocationType.STATION, "587"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
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
	public void testTripBeuelKoelnSued() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "1504"), null, new Location(LocationType.STATION, "25"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}

	@Test
	public void testTripBonnHbfBonnBeuel() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null, new Location(LocationType.STATION, "1504"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}

	@Test
	public void testTripBonnHbfDorotheenstr() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null, new Location(LocationType.STATION, "1150"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}

	@Test
	public void testTripByCoord() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.ANY, 50740530, 7129200), null, new Location(LocationType.ANY, 50933930, 6932440), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}

	@Test
	public void testTripWithSurchargeInfo() throws Exception {
		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, "687"), null, new Location(LocationType.STATION, "892"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
		print(result);
		assertEquals(QueryTripsResult.Status.OK, result.status);
		assertTrue(result.trips.size() > 0);
	}

	@Ignore
	@Test
	public void getStringsFromDEXFile() throws Exception {
		File inFile = new File("/mnt/hgfs/Transfer/de.vrsinfo-1/classes.dex");
		File outFile = new File("/mnt/hgfs/Transfer/de.vrsinfo-1/strings.txt");
		FileInputStream in = new FileInputStream(inFile);
		FileOutputStream out = new FileOutputStream(outFile);
		byte contents[] = new byte[(int) inFile.length()];
		in.read(contents);
		in.close();
		int i = 0;
		for (int pointer = 0x70; pointer < inFile.length(); pointer += 4) {
			int offsetString = byteArrayToInt(contents, pointer);
			// System.out.println("offset " + offsetString);
			int length = byteArrayToShort(contents, offsetString);
			// System.out.println("length " + length);
			String string = new String(contents, offsetString + 1, length);
			System.out.println(string);
			out.write(string.getBytes());
			out.write(0x0a);
			if (i++ > 17758)
				break;
		}
		out.close();
	}

	private int byteArrayToInt(byte[] b, int offset) {
		return b[offset + 0] & 0xFF | (b[offset + 1] & 0xFF) << 8 | (b[offset + 2] & 0xFF) << 16 | (b[offset + 3] & 0xFF) << 24;
	}

	private int byteArrayToShort(byte[] b, int offset) {
		return b[offset] & 0xFF;
	}
}
