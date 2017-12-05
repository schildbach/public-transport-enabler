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

import org.junit.Test;

import de.schildbach.pte.BrFloripaProvider;
import de.schildbach.pte.BrProvider;
import de.schildbach.pte.dto.Point;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertTrue;

/**
 * @author Torsten Grote
 */
public class BrFloripaProviderLiveTest extends AbstractNavitiaProviderLiveTest {

	public BrFloripaProviderLiveTest() {
		super(new BrFloripaProvider(HttpUrl.parse("https://transportr.grobox.de/api/v1/"), null));
	}

	@Test
	public void nearbyStationsAddress() throws Exception {
		nearbyStationsAddress(-27597000, -48553000);
	}

	@Test
	public void nearbyStationsStation() throws Exception {
		nearbyStationsStation("stop_point:43719878");
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception {
		nearbyStationsInvalidStation("stop_point:3719878");
	}

	@Test
	public void queryDeparturesEquivsFalse() throws Exception {
		queryDeparturesEquivsFalse("stop_point:43719878");
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception {
		queryDeparturesInvalidStation("stop_point:OWX:SP:6911");
	}

	@Test
	public void suggestLocations() throws Exception {
		suggestLocationsFromName("Lagoa");
	}

	@Test
	public void queryTripStations() throws Exception {
		queryTrip("Rua da Capela", "Bola de Neve Church");
	}

	@Test
	public void queryMoreTrips() throws Exception {
		queryMoreTrips("CELESC", "SC-406");
	}

	@Test
	public void getArea() throws Exception {
		final Point[] polygon = provider.getArea();
		assertTrue(polygon.length > 0);
	}

}
