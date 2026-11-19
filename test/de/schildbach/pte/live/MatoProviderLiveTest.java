package de.schildbach.pte.live;

import de.schildbach.pte.MatoProvider;
import de.schildbach.pte.dto.*;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class MatoProviderLiveTest extends AbstractProviderLiveTest {
    public MatoProviderLiveTest() {
        super(new MatoProvider());
    }

    @Test
    public void testDeparture() throws IOException {
        final QueryDeparturesResult result = provider.queryDepartures("gtt:476",
                Date.from(Instant.now()), 5, false
                );
        for (StationDepartures stDep: result.stationDepartures){
            System.out.println(stDep.location);

            for(Departure dep: stDep.departures){
                System.out.println(dep);
            }
        }
    }

    @Test
    public void testDate(){
        final Date date = Date.from(Instant.ofEpochMilli(1792057500000L));

        assertEquals("2026-10-15", MatoProvider.formatLocalDate(date));

        assertEquals("11:45:00", MatoProvider.formatLocalTime(date));
    }

    @Test
    public void testTripResponse() throws IOException {
        final Location from = new Location(LocationType.ADDRESS, "asdadaa",Point.fromDouble(45.08067,7.70060),
                "Torino","Corso Novara 115, Torino"
                );
        final Location to = new Location(LocationType.ADDRESS, "udbahsbsaajhs",Point.fromDouble(45.04556,7.67026),
                "Torino","Via Antonio Muratori 13, Torino"
        );
        assertTrue(to.hasCoord());
        assertTrue(from.hasCoord());
        final QueryTripsResult result = provider.queryTrips(from,null, to,
                //Date.from(Instant.ofEpochSecond(1792483200)),
                Date.from(Instant.now()),
                true, null
        );
        System.out.println("Results of the query: "+result.status);
        assertSame(QueryTripsResult.Status.OK, result.status);
        print(result);
        for (Trip tr: result.trips) {
            System.out.println(tr);
            for (Trip.Leg l : tr.legs) {
                System.out.println("Leg: "+ l.getClass());
                if (l instanceof Trip.Public) {
                    Trip.Public leg = (Trip.Public) l;

                    System.out.println("Departure position, planned: "+leg.departureStop.plannedDeparturePosition+
                            ", predicted: "+leg.departureStop.predictedDeparturePosition);
                    if(leg.getDeparturePosition()!=null){
                        assertNotEquals("null",leg.getDeparturePosition().toString());
                    }
                }
            }
        }

        QueryTripsResult resExtra = provider.queryMoreTrips(result.context, true);

        assertSame(QueryTripsResult.Status.OK, resExtra.status);

    }

    @Test
    public void testSuggestionsPlaces() throws IOException{
        final SuggestLocationsResult res = provider.suggestLocations("castello",null, 10);

        print(res);
    }

    @Test
    public void testFindStops() throws IOException{
        final NearbyLocationsResult result = provider.queryNearbyLocations(EnumSet.of(LocationType.STATION),
                Location.coord(Point.fromDouble(45.0381828,7.6671760)),
                500, 10
                );

        print(result);
        assertNotNull(result.locations);
        assertTrue(result.locations.size()>= 5);
    }
}
