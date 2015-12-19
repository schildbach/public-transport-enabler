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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.schildbach.pte.ParisProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Antonio El Khoury
 */
public class ParisProviderLiveTest extends AbstractNavitiaProviderLiveTest
{
	public ParisProviderLiveTest()
	{
		super(new ParisProvider(secretProperty("navitia.authorization")));
	}

	@Test
	public void nearbyStationsAddress() throws Exception
	{
		nearbyStationsAddress(48877523, 2378353);
	}

	@Test
	public void nearbyStationsAddress2() throws Exception
	{
		nearbyStationsAddress(48785420, 2212050);
	}

	@Test
	public void nearbyStationsStation() throws Exception
	{
		nearbyStationsStation("stop_point:RTP:SP:3926410");
	}

	@Test
	public void nearbyStationsPoi() throws Exception
	{
		nearbyStationsPoi("poi:n668579722");
	}

	@Test
	public void nearbyStationsAny() throws Exception
	{
		nearbyStationsAny(48877523, 2378353);
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception
	{
		nearbyStationsInvalidStation("stop_point:RTP:SP:392");
	}

	@Test
	public void queryDeparturesEquivsFalse() throws Exception
	{
		queryDeparturesEquivsFalse("stop_point:RTP:SP:3926410");
	}

	@Test
	public void queryDeparturesStopArea() throws Exception
	{
		queryDeparturesStopArea("stop_area:RTP:SA:1958");
	}

	@Test
	public void queryDeparturesEquivsTrue() throws Exception
	{
		queryDeparturesEquivsTrue("stop_point:RTP:SP:3926410");
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	{
		queryDeparturesInvalidStation("stop_point:RTP:SP:999999");
	}

	@Test
	public void suggestLocations() throws Exception
	{
		suggestLocationsFromName("bellevi");
	}

	@Test
	public void suggestLocationsFromAddress() throws Exception
	{
		suggestLocationsFromAddress("13 rue man");
	}

	@Test
	public void suggestLocationsNoLocation() throws Exception
	{
		suggestLocationsNoLocation("bellevilleadasdjkaskd");
	}

	@Test
	public void queryTripAddresses() throws Exception
	{
		queryTrip("5 rue Manin Paris", "10 rue Elanger Paris");
	}

	@Test
	public void queryTripAddressStation() throws Exception
	{
		queryTrip("155 bd hopital paris", "Gare St-Lazare");
	}

	@Test
	public void queryTripStations() throws Exception
	{
		queryTrip("Campo Formio", "Gare St-Lazare");
	}

	@Test
	public void queryTripStations2() throws Exception
	{
		queryTrip("Tour Eiffel", "Orsay Ville");
	}

	@Test
	public void queryTripStations3() throws Exception
	{
		queryTrip("Tour Eiffel", "Campo Formio");
	}

	@Test
	public void queryTripStationsRapidTransit() throws Exception
	{
		queryTrip("Luxembourg Paris", "Antony Antony");
	}

	@Test
	public void queryTripNoSolution() throws Exception
	{
		queryTripNoSolution("secretan buttes chaumont paris", "Antony Antony");
	}

	@Test
	public void queryTripUnknownFrom() throws Exception
	{
		queryTripUnknownFrom("secretan buttes chaumont paris");
	}

	@Test
	public void queryTripUnknownTo() throws Exception
	{
		queryTripUnknownTo("secretan buttes chaumont paris");
	}

	@Test
	public void queryTripSlowWalk() throws Exception
	{
		queryTripSlowWalk("5 rue manin paris", "10 rue marcel dassault velizy");
	}

	@Test
	public void queryTripFastWalk() throws Exception
	{
		queryTripFastWalk("5 rue manin paris", "10 rue marcel dassault velizy");
	}

	@Test
	public void queryMoreTrips() throws Exception
	{
		queryMoreTrips("5 rue manin paris", "10 rue marcel dassault velizy");
	}

	@Test
	public void getArea() throws Exception
	{
		final Point[] polygon = provider.getArea();
		assertTrue(polygon.length > 0);
	}
}
