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
	// stations close to those coordinates (lat, lon)
	// no decimal point, has to include 6 decimal places
	{
		nearbyStationsAddress(48573410, 7752110);
	}

	@Test
	public void nearbyStationsAddress2() throws Exception
	// different test case for stations close to coordinates (lat, lon)
	// no decimal point, has to include 6 decimal places
	{
		nearbyStationsAddress(48598480, 7761790);
	}

	@Test
	public void nearbyStationsStation() throws Exception
	// station to find other stations around
	// look in NTFS file for a stop_id (that contains "SP") and apend to "stop_point:"
	{
		nearbyStationsStation("stop_point:OST:SP:HOFER_11");
	}

	@Test
	public void nearbyStationsPoi() throws Exception
	// POI to find stations around
	// search OSM for a node, use identifier after
	// "https://www.openstreetmap.org/node/" and apend it to "poi:n"
	{
		nearbyStationsPoi("poi:n39224822");
	}

	@Test
	public void nearbyStationsAny() throws Exception
	// coordinates to find stations around
	{
		nearbyStationsAny(48573410, 7752110);
	}
	
	@Test
	public void nearbyStationsInvalidStation() throws Exception
	// station that does not exist?
	{
		nearbyStationsInvalidStation("stop_point:OBO:SP:999999");
	}

	@Test
	public void queryDeparturesEquivsFalse() throws Exception
	// what is it for??
	{
		queryDeparturesEquivsFalse("stop_point:OST:SP:HOFER_11");
	}

	@Test
	public void queryDeparturesStopArea() throws Exception
	// what is it for??
	// has to be an existing stop area (i.e. ID contains "SA")
	{
		queryDeparturesStopArea("stop_area:OST:SA:CTPHOFER_04");
	}

	@Test
	public void queryDeparturesEquivsTrue() throws Exception
	// what is it for??
	// can be the same to queryDeparturesEquivsFalse
	{
		queryDeparturesEquivsTrue("stop_point:OST:SP:HOFER_11");
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
		suggestLocationsFromName("Observat");
	}

	@Test
	public void suggestLocationsFromAddress() throws Exception
	// start of an address that should return something
	{
		suggestLocationsFromAddress("16 quai Saint");
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
		queryTrip("16 quai Saint-Nicolas Strasbourg", "6 quai Kléber Strasbourg");
	}

	@Test
	public void queryTripAddressStation() throws Exception
	// one existing address and one existing station to define a trip
	{
		queryTrip("16 quai Saint-Nicolas Strasbourg", "Illkirch Lixenbuhl");
	}

	@Test
	public void queryTripStations() throws Exception
	// two existing stops to define a trip
	{
		queryTrip("Mathieu Zell", "Illkirch Lixenbuhl");
	}

	@Test
	public void queryTripStations2() throws Exception
	// two existing stations for a trip, second test case
	{
		queryTrip("Homme de Fer", "Général Lejeune");
	}

	@Test
	public void queryTripStations3() throws Exception
	// two existing stations for a trip, third test case
	{
		queryTrip("Eurofret", "Gare aux Marchandises");
	}

	@Test
	public void queryTripStationsRapidTransit() throws Exception
	// two existing stations for "rapid transit"... ?
	{
		queryTrip("Observatoire Strasbourg", "Porte de l'Hôpital Strasbourg");
	}

	@Test
	public void queryTripNoSolution() throws Exception
	// two existing stations that are not connected
	{
		queryTripNoSolution("Homme de Fer Strasbourg", "Villers Mairie Villers-Les-Nancy");
	}

	@Test
	public void queryTripUnknownFrom() throws Exception
	// existing station for end of trip, don't know where from
	{
		queryTripUnknownFrom("Homme de Fer Strasbourg");
	}

	@Test
	public void queryTripUnknownTo() throws Exception
	// existing station to start from, don't know where to
	{
		queryTripUnknownTo("Homme de Fer Strasbourg");
	}

	@Test
	public void queryTripSlowWalk() throws Exception
	// two addresses for a "slow walk"
	{
		queryTripSlowWalk("16 quai Saint-Nicolas Strasbourg", "5 rue du Travail Strasbourg");
	}

	@Test
	public void queryTripFastWalk() throws Exception
	// two addresses for a "fast walk", can be same as above
	{
		queryTripFastWalk("16 quai Saint-Nicolas Strasbourg", "5 rue du Travail Strasbourg");
	}

	@Test
	public void queryMoreTrips() throws Exception
	// two addresses to show more trip options, can be same as above
	{
		queryMoreTrips("16 quai Saint-Nicolas Strasbourg", "5 rue du Travail Strasbourg");
	}

	@Test
	public void getArea() throws Exception
	// ??
	{
		final Point[] polygon = provider.getArea();
		assertTrue(polygon.length > 0);
	}
}
