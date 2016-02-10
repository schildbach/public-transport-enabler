/*
 * Copyright 2015-2016 the original author or authors.
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

import de.schildbach.pte.FranceNorthEastProvider;
import de.schildbach.pte.dto.Point;

/**
 * @author Nicolas Derive
 * @author Stéphane Guillou
 */
public class FranceNorthEastProviderLiveTest extends AbstractNavitiaProviderLiveTest
{
	public FranceNorthEastProviderLiveTest()
	{
		super(new FranceNorthEastProvider(secretProperty("navitia.authorization")));
	}

	@Test
	public void nearbyStationsAddress() throws Exception
	// check how to convert coordinates
	{
		nearbyStationsAddress(44826434, -557312);
	}

	@Test
	public void nearbyStationsAddress2() throws Exception
	// what's the difference with the first one?
	{
		nearbyStationsAddress(44841225, -580036);
	}

	@Test
	public void nearbyStationsStation() throws Exception
	// station to find other stations around
	{
		nearbyStationsStation("stop_point:STE:SP:OCETrainTER-87581538");
	}

	@Test
	public void nearbyStationsPoi() throws Exception
	// POI to find stations around
	{
		nearbyStationsPoi("poi:n849494949");
	}

	@Test
	public void nearbyStationsAny() throws Exception
	// coordinates to find stations around
	{
		nearbyStationsAny(44826434, -557312);
	}

	@Test
	public void nearbyStationsInvalidStation() throws Exception
	// station that does not exist
	{
		nearbyStationsInvalidStation("stop_point:OBO:SP:7");
	}

	@Test
	public void queryDeparturesEquivsFalse() throws Exception
	// ??
	{
		queryDeparturesEquivsFalse("stop_point:OBO:SP:732");
	}

	@Test
	public void queryDeparturesStopArea() throws Exception
	// ??
	{
		queryDeparturesStopArea("stop_area:OBO:SA:AEROG");
	}

	@Test
	public void queryDeparturesEquivsTrue() throws Exception
	// ??
	{
		queryDeparturesEquivsTrue("stop_point:OBO:SP:732");
	}

	@Test
	public void queryDeparturesInvalidStation() throws Exception
	// station that does not exist
	{
		queryDeparturesInvalidStation("stop_point:OBO:SP:999999");
	}

	@Test
	public void suggestLocations() throws Exception
	// start of a place name that should return something
	{
		suggestLocationsFromName("quinco");
	}

	@Test
	public void suggestLocationsFromAddress() throws Exception
	// start of an address that should return something
	{
		suggestLocationsFromAddress("78 rue cam");
	}

	@Test
	public void suggestLocationsNoLocation() throws Exception
	// fake place that will not return results
	{
		suggestLocationsNoLocation("quinconcesadasdjkaskd");
	}

	@Test
	public void queryTripAddresses() throws Exception
	// two existing addresses to define a trip
	{
		queryTrip("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
	}

	@Test
	public void queryTripAddressStation() throws Exception
	// one existing address and one existing station to define a trip
	{
		queryTrip("98, rue Jean-Renaud Dandicolle Bordeaux", "Saint-Augustin");
	}

	@Test
	public void queryTripStations() throws Exception
	// two existing stops to define a trip
	{
		queryTrip("Hôpital Pellegrin", "Avenue de l'Université");
	}

	@Test
	public void queryTripStations2() throws Exception
	// two existing stations for a trip, second test case
	{
		queryTrip("Pelletan", "Barrière de Pessac");
	}

	@Test
	public void queryTripStations3() throws Exception
	// two existing stations for a trip, third test case
	{
		queryTrip("Barrière de Pessac", "Hôpital Pellegrin");
	}

	@Test
	public void queryTripStationsRapidTransit() throws Exception
	// two existing stations for "rapid transit"... ?
	{
		queryTrip("Gaviniès Bordeaux", "Saint-Augustin Bordeaux");
	}

	@Test
	public void queryTripNoSolution() throws Exception
	// two existing stations that are not connected
	{
		queryTripNoSolution("Patinoire Mériadeck Bordeaux", "Mérignac Centre");
	}

	@Test
	public void queryTripUnknownFrom() throws Exception
	// existing station for end of trip, don't know where from
	{
		queryTripUnknownFrom("Patinoire Mériadeck Bordeaux");
	}

	@Test
	public void queryTripUnknownTo() throws Exception
	// existing station to start from, don't know where to
	{
		queryTripUnknownTo("Patinoire Mériadeck Bordeaux");
	}

	@Test
	public void queryTripSlowWalk() throws Exception
	// two addresses for a "slow walk"
	{
		queryTripSlowWalk("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
	}

	@Test
	public void queryTripFastWalk() throws Exception
	// two addresses for a "fast walk", can be same as above
	{
		queryTripFastWalk("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
	}

	@Test
	public void queryMoreTrips() throws Exception
	// two addresses to show more trip options, can be same as above
	{
		queryMoreTrips("98 rue Jean-Renaud Dandicolle Bordeaux", "78 rue Camena d'Almeida Bordeaux");
	}

	@Test
	public void getArea() throws Exception
	// ??
	{
		final Point[] polygon = provider.getArea();
		assertTrue(polygon.length > 0);
	}
}
