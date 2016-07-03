package de.schildbach.pte.live;

import de.schildbach.pte.FrenchSouthEastProvider;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Point;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Anthony Chaput
 */
public class FrenchSouthEastProviderLiveTest
		extends AbstractNavitiaProviderLiveTest
{
	public FrenchSouthEastProviderLiveTest()
	{
		super(new FrenchSouthEastProvider(secretProperty("navitia.authorization")));
	}

	@Test
	public void nearbyStationsAddress()
			throws Exception
	{
		nearbyStationsAddress(45185260, 5737800);
	}

	@Test
	public void nearbyStationsAddress2()
			throws Exception
	{
		nearbyStationsAddress(45184620, 5779780);
	}

	@Test
	public void nearbyStationsStation()
			throws Exception
	{
		nearbyStationsStation("stop_point:OGR:SP:2021");
	}

	@Test
	public void nearbyStationsPoi()
			throws Exception
	{
		nearbyStationsPoi("poi:n1245491811");
	}

	@Test
	public void nearbyStationsAny()
			throws Exception
	{
		nearbyStationsAny(45184630, 5779790);
	}

	@Test
	public void nearbyStationsInvalidStation()
			throws Exception
	{
		nearbyStationsInvalidStation("stop_point:OGR:SP:S99999");
	}

	@Test
	public void queryDeparturesEquivsFalse()
			throws Exception
	{
		queryDeparturesEquivsFalse("stop_point:OGR:SP:2021");
	}

	@Test
	public void queryDeparturesStopArea()
			throws Exception
	{
		queryDeparturesStopArea("stop_area:OGR:SA:S3105");
	}

	@Test
	public void queryDeparturesEquivsTrue()
			throws Exception
	{
		queryDeparturesEquivsTrue("stop_point:OGR:SP:2021");
	}

	@Test
	public void queryDeparturesInvalidStation()
			throws Exception
	{
		queryDeparturesInvalidStation("stop_point:OBO:SP:999999");
	}

	@Test
	public void suggestLocations()
			throws Exception
	{
		suggestLocationsFromName("condil");
	}

	@Test
	public void suggestLocationsFromAddress()
			throws Exception
	{
		suggestLocationsFromAddress("360 rue des res");
	}

	@Test
	public void suggestLocationsNoLocation()
			throws Exception
	{
		suggestLocationsNoLocation("quinconcesadasdjkaskd");
	}

	@Test
	public void queryTripAddresses()
			throws Exception
	{
		queryTrip("360 rue des résidences", "2 rue Charles Michels");
	}

	@Test
	public void queryTripAddressStation()
			throws Exception
	{
		queryTrip("14 rue Barnave", "Louise Michel");
	}

	@Test
	public void queryTripStations()
			throws Exception
	{
		queryTrip("Victor Hugo", "Les Bauches");
	}

	@Test
	public void queryTripStations2()
			throws Exception
	{
		queryTrip("Chavant", "Louise Michel");
	}

	@Test
	public void queryTripStations3()
			throws Exception
	{
		queryTrip("Fontaine", "Vallier Libération");
	}

	@Test
	public void queryTripStationsRapidTransit()
			throws Exception
	{
		queryTrip("Alsace-Lorraine", "Vallier Libération");
	}

	@Test
	public void queryTripNoSolution()
			throws Exception
	{
		queryTripNoSolution("Robespierre", "Les Bauches");
	}

	@Test
	public void queryTripUnknownFrom()
			throws Exception
	{
		queryTripUnknownFrom("Chavant");
	}

	@Test
	public void queryTripUnknownTo()
			throws Exception
	{
		queryTripUnknownTo("Chavant");
	}

	@Test
	public void queryTripSlowWalk()
			throws Exception
	{
		queryTripSlowWalk("360 rue des résidences", "15 rue de la chimie");
	}

	@Test
	public void queryTripFastWalk()
			throws Exception
	{
		queryTripFastWalk("360 rue des résidences", "15 rue de la chimie");
	}

	@Test
	public void queryMoreTrips()
			throws Exception
	{
		queryMoreTrips("360 rue des résidences", "15 rue de la chimie");
	}

	@Test
	public void getArea()
			throws Exception
	{
		Point[] polygon = this.provider.getArea();
		Assert.assertTrue(polygon.length > 0);
	}
}
