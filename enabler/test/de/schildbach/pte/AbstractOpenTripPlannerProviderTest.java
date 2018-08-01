/*
 * Copyright 2010-2018 the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.schildbach.pte;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.Trip.Public;
import okhttp3.HttpUrl;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AbstractOpenTripPlannerProviderTest {

	@Spy 
    private AbstractOpenTripPlannerProvider otpProvider = new AbstractOpenTripPlannerProvider(NetworkId.MFBW, HttpUrl.parse("http://none/"));

    @Test
    public void parseLocation_coord() throws Exception {
    	String coordJson = "{\"lat\":49,\"lon\":9})";
		Location location = otpProvider.parseLocationFromTo(new JSONObject(coordJson));
    	
		assertNotNull(location);
    	assertEquals(LocationType.COORD, location.type);
    	assertEquals((int)(49*1E6), location.lat);
    	assertEquals((int)(9*1E6), location.lon);
    }

    @Test
    public void parseLegs_withoutIntermediateStops() throws Exception {
    	Trip.Public legWithoutIntermediateStops = (Public) otpProvider.parseLeg(new JSONObject("{" + 
    			"                        \"startTime\": 1532173920000," + 
    			"                        \"endTime\": 1532174340000," + 
    			"                        \"departureDelay\": 0," + 
    			"                        \"arrivalDelay\": 0," + 
    			"                        \"realTime\": false," + 
    			"                        \"distance\": 4850.009558278676," + 
    			"                        \"pathway\": false," + 
    			"                        \"mode\": \"RAIL\"," + 
    			"                        \"route\": \"Karlsruhe - Bretten - Gülshausen - Bauerbach - Flehingen - Eppingen - Schwaigern - Heilbronn - Öhringen\"," +
    			"                        \"agencyName\": \"kvv\"," + 
    			"                        \"agencyUrl\": \"http://unkwnown/\"," + 
    			"                        \"agencyTimeZoneOffset\": 7200000," + 
    			"                        \"routeColor\": \"83b23b\"," + 
    			"                        \"routeType\": 109," + 
    			"                        \"routeId\": \"nvbv:kvv:22014:E:R:j18\"," + 
    			"                        \"routeTextColor\": \"FFFFFF\"," + 
    			"                        \"interlineWithPreviousLeg\": false," + 
    			"                        \"headsign\": \"Öhringen\"," +
    			"                        \"agencyId\": \"13\"," + 
    			"                        \"tripId\": \"nvbv:kvv:22014:E:R:j18-6-20615-13:36\"," + 
    			"                        \"serviceDate\": \"20180721\"," + 
    			"                        \"from\": {" + 
    			"                            \"name\": \"Weinsberg Bahnhof\"," + 
    			"                            \"stopId\": \"nvbv:de:08125:4344:1:1\"," + 
    			"                            \"platformCode\": \"1\"," + 
    			"                            \"lon\": 9.286208029239999," + 
    			"                            \"lat\": 49.14834012859," + 
    			"                            \"arrival\": 1532173919000," + 
    			"                            \"departure\": 1532173920000," + 
    			"                            \"stopIndex\": 9," + 
    			"                            \"stopSequence\": 10," + 
    			"                            \"vertexType\": \"TRANSIT\"" + 
    			"                        }," + 
    			"                        \"to\": {" + 
    			"                            \"name\": \"Sülzbach Schule (S-Bahn)\"," +
    			"                            \"stopId\": \"nvbv:de:08125:2005:1:1\"," + 
    			"                            \"platformCode\": \"1\"," + 
    			"                            \"lon\": 9.35141767054," + 
    			"                            \"lat\": 49.145066559929994," + 
    			"                            \"arrival\": 1532174340000," + 
    			"                            \"departure\": 1532174341000," + 
    			"                            \"stopIndex\": 13," + 
    			"                            \"stopSequence\": 14," + 
    			"                            \"vertexType\": \"TRANSIT\"" + 
    			"                        }," + 
    			"                        \"legGeometry\": {" + 
    			"                            \"points\": \"ch~jHwutw@kC{dCwDau@zFkqCvUghA\"," + 
    			"                            \"length\": 5" + 
    			"                        }," + 
    			"                        \"routeLongName\": \"Karlsruhe - Bretten - Gülshausen - Bauerbach - Flehingen - Eppingen - Schwaigern - Heilbronn - Öhringen\"," +
    			"                        \"rentedBike\": false," + 
    			"                        \"transitLeg\": true," + 
    			"                        \"duration\": 420," + 
    			"                        \"intermediateStops\": []," + 
    			"                        \"steps\": []" + 
    			"                    }"));
 	
    	assertThat(legWithoutIntermediateStops, CoreMatchers.notNullValue());
    	assertThat(legWithoutIntermediateStops.intermediateStops, notNullValue());
    	assertThat(legWithoutIntermediateStops.intermediateStops.isEmpty(), equalTo(true));	
    }
    
    @Test
    public void parseLeg_withIntermediateStops() throws Exception {
    	Leg legWithIntermediateStops = otpProvider.parseLeg(new JSONObject("{" + 
    			"                        \"startTime\": 1532173920000," + 
    			"                        \"endTime\": 1532174340000," + 
    			"                        \"departureDelay\": 0," + 
    			"                        \"arrivalDelay\": 0," + 
    			"                        \"realTime\": false," + 
    			"                        \"distance\": 4850.009558278676," + 
    			"                        \"pathway\": false," + 
    			"                        \"mode\": \"RAIL\"," + 
    			"                        \"route\": \"Karlsruhe - Bretten - Gülshausen - Bauerbach - Flehingen - Eppingen - Schwaigern - Heilbronn - Öhringen\"," +
    			"                        \"agencyName\": \"kvv\"," + 
    			"                        \"agencyUrl\": \"http://unkwnown/\"," + 
    			"                        \"agencyTimeZoneOffset\": 7200000," + 
    			"                        \"routeColor\": \"83b23b\"," + 
    			"                        \"routeType\": 109," + 
    			"                        \"routeId\": \"nvbv:kvv:22014:E:R:j18\"," + 
    			"                        \"routeTextColor\": \"FFFFFF\"," + 
    			"                        \"interlineWithPreviousLeg\": false," + 
    			"                        \"headsign\": \"Öhringen\"," +
    			"                        \"agencyId\": \"13\"," + 
    			"                        \"tripId\": \"nvbv:kvv:22014:E:R:j18-6-20615-13:36\"," + 
    			"                        \"serviceDate\": \"20180721\"," + 
    			"                        \"from\": {" + 
    			"                            \"name\": \"Weinsberg Bahnhof\"," + 
    			"                            \"stopId\": \"nvbv:de:08125:4344:1:1\"," + 
    			"                            \"platformCode\": \"1\"," + 
    			"                            \"lon\": 9.286208029239999," + 
    			"                            \"lat\": 49.14834012859," + 
    			"                            \"arrival\": 1532173919000," + 
    			"                            \"departure\": 1532173920000," + 
    			"                            \"stopIndex\": 9," + 
    			"                            \"stopSequence\": 10," + 
    			"                            \"vertexType\": \"TRANSIT\"" + 
    			"                        }," + 
    			"                        \"to\": {" + 
    			"                            \"name\": \"Sülzbach Schule (S-Bahn)\"," +
    			"                            \"stopId\": \"nvbv:de:08125:2005:1:1\"," + 
    			"                            \"platformCode\": \"1\"," + 
    			"                            \"lon\": 9.35141767054," + 
    			"                            \"lat\": 49.145066559929994," + 
    			"                            \"arrival\": 1532174340000," + 
    			"                            \"departure\": 1532174341000," + 
    			"                            \"stopIndex\": 13," + 
    			"                            \"stopSequence\": 14," + 
    			"                            \"vertexType\": \"TRANSIT\"" + 
    			"                        }," + 
    			"                        \"legGeometry\": {" + 
    			"                            \"points\": \"ch~jHwutw@kC{dCwDau@zFkqCvUghA\"," + 
    			"                            \"length\": 5" + 
    			"                        }," + 
    			"                        \"routeLongName\": \"Karlsruhe - Bretten - Gülshausen - Bauerbach - Flehingen - Eppingen - Schwaigern - Heilbronn - Öhringen\"," +
    			"                        \"rentedBike\": false," + 
    			"                        \"transitLeg\": true," + 
    			"                        \"duration\": 420," + 
    			"                        \"intermediateStops\": [" + 
    			"                            {" + 
    			"                                \"name\": \"Weinsberg/Ellhofen Gewerbegebi\"," + 
    			"                                \"stopId\": \"nvbv:de:08125:1598:1:1\"," + 
    			"                                \"platformCode\": \"1\"," + 
    			"                                \"lon\": 9.30762309997," + 
    			"                                \"lat\": 49.14904026286," + 
    			"                                \"arrival\": 1532174040000," + 
    			"                                \"departure\": 1532174040000," + 
    			"                                \"stopIndex\": 10," + 
    			"                                \"stopSequence\": 11," + 
    			"                                \"vertexType\": \"TRANSIT\"" + 
    			"                            }," + 
    			"                            {" + 
    			"                                \"name\": \"Ellhofen Bahnhof\"," + 
    			"                                \"stopId\": \"nvbv:de:08125:2003:3:1\"," + 
    			"                                \"platformCode\": \"1\"," + 
    			"                                \"lon\": 9.31627837142," + 
    			"                                \"lat\": 49.149960974660004," + 
    			"                                \"arrival\": 1532174100000," + 
    			"                                \"departure\": 1532174100000," + 
    			"                                \"stopIndex\": 11," + 
    			"                                \"stopSequence\": 12," + 
    			"                                \"vertexType\": \"TRANSIT\"" + 
    			"                            }," + 
    			"                            {" + 
    			"                                \"name\": \"Sülzbach Bahnhof\"," +
    			"                                \"stopId\": \"nvbv:de:08125:2004:1:1\"," + 
    			"                                \"platformCode\": \"1\"," + 
    			"                                \"lon\": 9.33969625383," + 
    			"                                \"lat\": 49.14870741667," + 
    			"                                \"arrival\": 1532174220000," + 
    			"                                \"departure\": 1532174220000," + 
    			"                                \"stopIndex\": 12," + 
    			"                                \"stopSequence\": 13," + 
    			"                                \"vertexType\": \"TRANSIT\"" + 
    			"                            }" + 
    			"                        ]," + 
    			"                        \"steps\": []" + 
    			"                    }"));
    	
    	assertNotNull(legWithIntermediateStops);
    }
       
    @Test
    public void suggestLocations_noResults() throws Exception {
    	String suggestLocationsResponse = "[]";
		Mockito.doReturn(suggestLocationsResponse).when(otpProvider).request(Mockito.any(HttpUrl.class));
    	SuggestLocationsResult suggestLocations = otpProvider.suggestLocations("Non existing location");
    	assertTrue(suggestLocations.suggestedLocations.isEmpty());
    }

    @Test
    public void convertProductsToOTPModes(){
        String otpModes = otpProvider.convertProductsToOTPModes(Product.fromCodes(new char[]{'R','B'}));
        assertThat(otpModes, containsString("RAIL"));
        assertThat(otpModes, containsString("BUS"));
    }

    @Test
    public void convertProductsToOTPModes_cablecar(){
        String otpModes = otpProvider.convertProductsToOTPModes(Product.fromCodes(new char[]{'C'}));
        assertThat(otpModes, containsString("CABLE_CAR"));
        assertThat(otpModes, containsString("GONDOLA"));
        assertThat(otpModes, containsString("FUNICULAR"));
    }
}
