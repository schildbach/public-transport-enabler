package de.schildbach.pte.live;


import de.schildbach.pte.CapMetroProvider;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.*;
import org.junit.Test;

import java.util.Date;
import java.util.EnumSet;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CapMetroLiveTest extends AbstractProviderLiveTest {
    public CapMetroLiveTest() {
        super(new CapMetroProvider());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nearbyStations() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(new Location(LocationType.STATION, "591"));
        print(result);
    }

    @Test
    public void nearbyStationsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyStations(Location.coord(30275000, -97740000));
        print(result);
        assertThat(result.locations, hasItem(new Location(LocationType.STATION, "591")));
    }

    @Test
    public void nearbyPOIsByCoordinate() throws Exception {
        final NearbyLocationsResult result = queryNearbyLocations(EnumSet.of(LocationType.POI), Location.coord(30275000, -97740000));
        print(result);
        assertThat(result.locations,
                hasItem(new Location(LocationType.POI, "A=4@O=Texas State Capitol@X=-97740215@Y=30275103@u=0@U=130@L=9819105@", 30275103, -97740215, null, "Texas State Capitol")));
    }

    @Test
    public void queryDepartures() throws Exception {
        final QueryDeparturesResult result = queryDepartures("591", false);
        print(result);
    }

    @Test
    public void queryDeparturesInvalidStation() throws Exception {
        final QueryDeparturesResult result = queryDepartures("999999", 0, false);
        assertEquals(QueryDeparturesResult.Status.INVALID_STATION, result.status);
    }

    @Test
    public void suggestLocations() throws Exception {
        final SuggestLocationsResult result = suggestLocations("Capitol Station");
        print(result);
        assertThat(result.getLocations(), hasItem(new Location(LocationType.STATION, "591")));
    }

    @Test
    public void shortTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.STATION, "591", null, "Capitol Station (NB)"),
                null,
                new Location(LocationType.STATION, "5940", null, "Lavaca/17th (Midblock)"),
                new Date(), true, Product.ALL, NetworkProvider.WalkSpeed.NORMAL, NetworkProvider.Accessibility.NEUTRAL);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }

    @Test
    public void addressTrip() throws Exception {
        final QueryTripsResult result = queryTrips(
                new Location(LocationType.ADDRESS, null, null, "1501 Colorado St"),
                null,
                new Location(LocationType.ADDRESS, null, null, "4299 Duval St"),
                new Date(), true, Product.ALL, NetworkProvider.WalkSpeed.NORMAL, NetworkProvider.Accessibility.NEUTRAL);
        assertEquals(QueryTripsResult.Status.OK, result.status);
        print(result);

        if (!result.context.canQueryLater())
            return;

        final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
        print(laterResult);
    }
}
