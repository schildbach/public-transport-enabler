/*
 * Copyright the original author or authors.
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

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.WordUtils;

import okhttp3.HttpUrl;

/**
 * @author full-duplex
 */
public class NegentweeProvider extends AbstractNetworkProvider {

    private static final String API_BASE = "https://api.9292.nl/0.1/";
    private static final String SERVER_PRODUCT = "negentwee";

    private static final Language DEFAULT_API_LANG = Language.NL_NL;
    private static final TimeZone API_TIMEZONE = TimeZone.getTimeZone("Europe/Amsterdam");
    private static final int DEFAULT_MAX_LOCATIONS = 50;

    private final List CAPABILITIES = Arrays.asList(
            Capability.SUGGEST_LOCATIONS,
            Capability.NEARBY_LOCATIONS,
            Capability.DEPARTURES,
            Capability.TRIPS,
            Capability.TRIPS_VIA
    );

    private static final EnumSet<Product> trainProducts = EnumSet.of(Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN,
            Product.SUBURBAN_TRAIN);

    private final Language language;
    private final ResultHeader resultHeader;

    public enum Language {
        NL_NL("nl-NL"), EN_GB("en-GB");

        private final String lang;

        private Language(String lang) {
            this.lang = lang;
        }

        @Override
        public String toString() {
            return this.lang;
        }
    }

    private enum InterchangeTime {
        STANDARD, EXTRA;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    @SuppressWarnings("serial")
    private static class QueryParameter implements Serializable {
        public String name, value;

        private QueryParameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return this.name + "=" + this.value;
        }
    }

    @SuppressWarnings("serial")
    private static class TripsContext implements QueryTripsContext {
        private String url, earlier, later;
        public Location from, to, via;

        private TripsContext(HttpUrl url, @Nullable String earlier, @Nullable String later, Location from,
                @Nullable Location via, Location to) {
            this.url = url.toString();
            this.earlier = earlier;
            this.later = later;
            this.from = from;
            this.via = via;
            this.to = to;
        }

        private HttpUrl getQueryEarlier() {
            return HttpUrl.parse(this.url).newBuilder(this.earlier).addQueryParameter("before", "4").build();
        }

        private HttpUrl getQueryLater() {
            return HttpUrl.parse(this.url).newBuilder(this.later).addQueryParameter("after", "4").build();
        }

        @Override
        public boolean canQueryEarlier() {
            return (earlier != null);
        }

        @Override
        public boolean canQueryLater() {
            return (later != null);
        }
    }

    public NegentweeProvider() {
        this(DEFAULT_API_LANG);
    }

    public NegentweeProvider(Language language) {
        super(NetworkId.NEGENTWEE);

        this.language = language;
        this.resultHeader = new ResultHeader(network, SERVER_PRODUCT);
    }

    private HttpUrl buildApiUrl(String action, List<QueryParameter> queries) {
        HttpUrl.Builder url = HttpUrl.parse(API_BASE).newBuilder().addPathSegments(action).addQueryParameter("lang",
                this.language.toString());

        for (QueryParameter q : queries) {
            url.addQueryParameter(q.name, q.value);
        }

        return url.build();
    }

    private Location queryLocationById(String stationId) throws IOException {
        HttpUrl url = buildApiUrl("locations/" + stationId, new ArrayList<QueryParameter>());
        final CharSequence page = httpClient.get(url);

        try {
            JSONObject head = new JSONObject(page.toString());
            JSONObject location = head.getJSONObject("location");

            return locationFromJSONObject(location);
        } catch (final JSONException x) {
            throw new IOException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    private Location queryLocationByName(String locationName, Set<LocationType> types) throws IOException {
        for (Location location : queryLocationsByName(locationName, types)) {
            if (location.name != null && location.name.equals(locationName)) {
                return location;
            }
        }

        throw new RuntimeException("Cannot find station with name " + locationName);
    }

    private List<Location> queryLocationsByName(String locationName, Set<LocationType> types) throws IOException {
        List<QueryParameter> queryParameters = new ArrayList<>();
        queryParameters.add(new QueryParameter("q", locationName));

        // Add types if specified
        String locationTypes = locationTypesToQueryParameterString(types);
        if (locationTypes.length() > 0)
            queryParameters.add(new QueryParameter("type", locationTypes));

        HttpUrl url = buildApiUrl("locations", queryParameters);
        final CharSequence page = httpClient.get(url);

        try {
            JSONObject head = new JSONObject(page.toString());
            JSONArray locations = head.getJSONArray("locations");

            Location[] foundLocations = new Location[locations.length()];
            for (int i = 0; i < locations.length(); i++) {
                foundLocations[i] = locationFromJSONObject(locations.getJSONObject(i));
            }

            return Arrays.asList(foundLocations);
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    private LocationType locationTypeFromTypeString(String type) throws JSONException {
        switch (type) {
        case "station":
        case "stop":
            return LocationType.STATION;
        case "address":
        case "street":
        case "streetrange":
        case "place":
        case "postcode":
            return LocationType.ADDRESS;
        case "poi":
            return LocationType.POI;
        case "latlong":
            return LocationType.COORD;
        default:
            throw new JSONException("Unsupported location type: " + type);
        }
    }

    private List<String> locationStringsFromLocationType(LocationType type) {
        switch (type) {
        case STATION:
            return Arrays.asList("station", "stop");
        case POI:
            return Arrays.asList("poi");
        case ADDRESS:
            return Arrays.asList("address", "street", "streetrange", "place", "postcode");
        case COORD:
            return Arrays.asList("latlong");
        default:
            return Arrays.asList();
        }
    }

    private Set<Product> productSetFromTypeString(String type) {
        switch (type.toLowerCase()) {
        case "train":
            return EnumSet.of(Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN);
        case "subway":
            return EnumSet.of(Product.SUBWAY);
        case "tram":
            return EnumSet.of(Product.TRAM);
        case "bus":
            return EnumSet.of(Product.BUS);
        case "ferry":
            return EnumSet.of(Product.FERRY);
        case "walk":
            return EnumSet.of(Product.ON_DEMAND);
        default:
            return EnumSet.noneOf(Product.class);
        }
    }

    private Product productFromMode(String type, String name) {
        switch (type.toLowerCase()) {
        case "train":
            switch (name.toLowerCase()) {
            // TODO: Likely not all possible train names, add here if trains are classified incorrectly.
            case "thalys":
            case "ice":
            case "intercity direct":
            case "intercity":
                return Product.HIGH_SPEED_TRAIN;
            case "sprinter":
            default:
                return Product.REGIONAL_TRAIN;
            }
        case "tram":
            return Product.TRAM;
        case "subway":
            return Product.SUBWAY;
        case "bus":
            return Product.BUS;
        case "ferry":
            return Product.FERRY;
        case "walk":
            return Product.ON_DEMAND;
        }

        return null;
    }

    private String locationToQueryParameterString(Location loc) {
        if (loc.hasId()) {
            return loc.id;
        } else if (loc.hasCoord()) {
            return loc.getLatAsDouble() + "," + loc.getLonAsDouble();
        } else {
            return null;
        }
    }

    // Including these type names will cause the locations API to fail, skip them
    private static final ImmutableSet<String> DISALLOWED_TYPE_NAMES = ImmutableSet.of("latlong", "streetrange");

    private String locationTypesToQueryParameterString(Set<LocationType> types) {
        StringBuilder typeValue = new StringBuilder();
        if (!types.contains(LocationType.ANY) && types.size() > 0) {
            for (LocationType type : types) {
                for (String addition : locationStringsFromLocationType(type)) {
                    if (DISALLOWED_TYPE_NAMES.contains(addition))
                        continue;

                    if (typeValue.length() > 0)
                        typeValue.append(",");
                    typeValue.append(addition);
                }
            }
        }

        return typeValue.toString();
    }

    private String formatApiDateTime(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmm");
        formatter.setTimeZone(API_TIMEZONE);
        return formatter.format(date.getTime());
    }

    private Date dateFromJSONObject(JSONObject obj, String key) throws JSONException {
        try {
            Calendar cal = Calendar.getInstance(API_TIMEZONE);
            ParserUtils.parseIsoDateTime(cal, obj.getString(key));
            return cal.getTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Date timeFromJSONObject(JSONObject obj, String key) throws JSONException {
        try {
            Calendar calParsed = Calendar.getInstance(API_TIMEZONE);
            ParserUtils.parseIsoTime(calParsed, obj.getString(key));

            // Assume this time is always between NOW-00:05 and NOW+23:55, allowing for a 5 minute delay.
            Calendar calNow = Calendar.getInstance();
            calNow.add(Calendar.MINUTE, -5);
            if (calParsed.before(calNow)) {
                calNow.add(Calendar.HOUR, 24);
            }

            return calParsed.getTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Date realtimeDateFromJSONObject(JSONObject obj, String key, String realtimeKey) throws JSONException {
        return dateFromJSONObject(obj, (!obj.isNull(realtimeKey)) ? realtimeKey : key);
    }

    private Trip tripFromJSONObject(JSONObject trip, @Nullable Location from, @Nullable Location to,
            @Nullable Map<String, JSONObject> disturbances) throws JSONException {
        JSONArray legs = trip.getJSONArray("legs");

        Date tripDeparture = realtimeDateFromJSONObject(trip, "departure", "realtimeDeparture");
        /* Date tripArrival = */ realtimeDateFromJSONObject(trip, "arrival", "realtimeArrival");

        // Get journey legs
        LinkedList<Trip.Leg> foundLegs = new LinkedList<>();
        for (int i = 0; i < legs.length(); i++) {
            JSONObject leg = legs.getJSONObject(i);

            JSONArray stops = leg.getJSONArray("stops");
            JSONObject mode = leg.getJSONObject("mode");
            JSONObject operator = leg.optJSONObject("operator");

            LinkedList<Point> foundPoints = new LinkedList<>();

            // First stop
            Stop firstStop = stopFromJSONObject(stops.getJSONObject(0));
            foundPoints.add(firstStop.location.coord);

            // Intermediate stops
            LinkedList<Stop> foundStops = new LinkedList<>();
            for (int j = 1; j < stops.length() - 1; j++) {
                foundStops.add(stopFromJSONObject(stops.getJSONObject(j)));
                foundPoints.add(foundStops.getLast().location.coord);
            }

            // Last stop
            Stop lastStop = stopFromJSONObject(stops.getJSONObject(stops.length() - 1));
            foundPoints.add(lastStop.location.coord);

            switch (leg.getString("type").toLowerCase()) {
            case "scheduled":
                Product lineProduct = productFromMode(mode.getString("type"), mode.getString("name"));

                StringBuilder legMessage = new StringBuilder();

                // Add attributes to leg message
                JSONArray legAttributes = leg.getJSONArray("attributes");
                for (int k = 0; k < legAttributes.length(); k++) {
                    JSONObject legAttribute = legAttributes.getJSONObject(k);

                    if (legMessage.length() > 0)
                        legMessage.append(", ");
                    legMessage.append(WordUtils.capitalizeFirst(legAttribute.getString("title")));
                }

                // Add disturbances to leg message
                if (disturbances != null) {
                    JSONArray legDisturbances = leg.getJSONArray("disturbancePlannerIds");
                    for (int k = 0; k < legDisturbances.length(); k++) {
                        String legDisturbanceId = legDisturbances.optString(k);

                        if (legDisturbanceId != null && disturbances.containsKey(legDisturbanceId)) {
                            JSONObject legDisturbance = disturbances.get(legDisturbanceId);

                            if (legMessage.length() > 0)
                                legMessage.append("<br>\n<br>\n");
                            legMessage.append(legDisturbance.getString("title"));
                            legMessage.append(":<br>\n");
                            legMessage.append(legDisturbance.getString("effect"));
                            legMessage.append(" ");
                            legMessage.append(legDisturbance.getString("measure"));
                        }
                    }
                }

                StringBuilder lineName = new StringBuilder();
                lineName.append(mode.getString("name"));

                // Service codes have no relevant meaning for trains
                if (!leg.isNull("service") && !trainProducts.contains(lineProduct)) {
                    lineName.append(" ");
                    lineName.append(leg.getString("service"));
                }

                foundLegs.add(new Trip.Public(
                        new Line(leg.getString("service"), (operator != null) ? operator.getString("name") : null,
                                lineProduct, lineName.toString(), leg.optString("service"),
                                Standard.STYLES.get(lineProduct), null, null),
                        new Location(LocationType.STATION, null, null, leg.getString("destination")), firstStop,
                        lastStop, foundStops, foundPoints, legMessage.length() > 0 ? legMessage.toString() : null));
                break;
            case "continuous":
                // Get leg time from trip or previous leg
                Date legDeparture = (i == 0) ? tripDeparture : foundLegs.getLast().getArrivalTime();
                Date legArrival = ParserUtils.addMinutes(legDeparture,
                        ParserUtils.parseMinutesFromTimeString(leg.getString("duration")));

                foundLegs.add(new Trip.Individual(Trip.Individual.Type.WALK, firstStop.location, legDeparture,
                        lastStop.location, legArrival, foundPoints, -1));
                break;
            default:
                throw new JSONException("Unknown leg type: " + leg.getString("type"));
            }
        }

        // Get journey fares
        JSONObject fareInfo = trip.getJSONObject("fareInfo");

        List<Fare> tripFares = null;
        if (fareInfo.getBoolean("complete")) {
            tripFares = Arrays.asList(
                new Fare("Full-price", Fare.Type.ADULT, ParserUtils.CURRENCY_EUR,
                    fareInfo.getInt("fullPriceCents") / 100, null, null),
                new Fare("Reduced-price", Fare.Type.ADULT, ParserUtils.CURRENCY_EUR,
                    fareInfo.getInt("reducedPriceCents") / 100, null, null));
        }

        return new Trip(trip.getString("id"), from, to, foundLegs, tripFares, null, trip.getInt("numberOfChanges"));
    }

    private Stop stopFromJSONObject(JSONObject stop) throws JSONException {
        Position plannedPlatform = positionFromJSONObject(stop, "platform");
        Position changedPlatform = positionFromJSONObject(stop, "platformChange");

        return new Stop(locationFromJSONObject(stop.getJSONObject("location")), dateFromJSONObject(stop, "arrival"),
                dateFromJSONObject(stop, "realtimeArrival"), plannedPlatform, changedPlatform, false,
                dateFromJSONObject(stop, "departure"), dateFromJSONObject(stop, "realtimeDeparture"), plannedPlatform,
                changedPlatform, false);
    }

    private Fare fareFromJSONObject(JSONObject fareLeg) throws JSONException {
        JSONArray fares = fareLeg.getJSONArray("fares");

        float farePrice = 0;
        for (int j = 0; j < fares.length(); j++) {
            JSONObject fare = fares.getJSONObject(j);

            // Always get the full non-reduced 2nd class fare price
            String fareClass = fare.getString("class");
            if (!fare.getBoolean("reduced") && (fareClass.equals("none") || fareClass.equals("second"))) {
                farePrice = (fare.getInt("eurocents") / 100);
                break;
            }
        }

        return new Fare(fareLeg.getString("operatorString"), Fare.Type.ADULT, ParserUtils.CURRENCY_EUR, farePrice,
                null, null);
    }

    private Departure departureFromJSONObject(JSONObject departure) throws JSONException {
        JSONObject mode = departure.getJSONObject("mode");

        /* String lineName = */ departure.optString("service");
        Product lineProduct = productFromMode(mode.getString("type"), mode.getString("name"));
        return new Departure(timeFromJSONObject(departure, "time"), timeFromJSONObject(departure, "time"),
                new Line(null, departure.getString("operatorName"), lineProduct,
                        !departure.isNull("service") ? departure.getString("service") : mode.getString("name"), null,
                        Standard.STYLES.get(lineProduct), null, null),
                !departure.isNull("platform") ? new Position(departure.getString("platform")) : null,
                new Location(LocationType.STATION, null, null, departure.getString("destinationName")), null,
                !departure.isNull("realtimeText") ? departure.optString("realtimeText") : null);
    }

    private Position positionFromJSONObject(JSONObject obj, String key) throws JSONException {
        String position = obj.getString(key);
        if (position != null && !position.equals("null")) {
            return new Position(position);
        } else {
            return null;
        }
    }

    private Location locationFromJSONObject(JSONObject location) throws JSONException {
        return locationFromJSONObject(location, true);
    }

    private Location locationFromJSONObject(JSONObject location, boolean addTypePrefix) throws JSONException {
        JSONObject latlon = location.getJSONObject("latLong");
        JSONObject place = location.optJSONObject("place");

        String locationType = location.getString("type");
        String locationName = location.optString("name", null);

        if (locationName != null) {
            if (addTypePrefix && !location.isNull(locationType + "Type") && !locationType.equals("poi")) {
                locationName = location.getString(locationType + "Type") + " " + locationName;
            }

            if (locationType.equals("address")) {
                String houseNumber = location.optString("houseNr");
                if (!houseNumber.isEmpty()) {
                    locationName = locationName + " " + houseNumber;
                }
            }
        }

        Point locationPoint = Point.fromDouble(latlon.getDouble("lat"), latlon.getDouble("long"));

        return new Location(locationTypeFromTypeString(locationType), location.getString("id"), locationPoint,
                !(place == null) ? place.optString("name", null) : null, locationName, null);
    }

    private List<Location> solveAmbiguousLocation(Location location) throws IOException {
        if (location.hasId()) {
            return Arrays.asList(location);
        } else if (location.hasCoord()) {
            return queryNearbyLocations(EnumSet.of(location.type), location, -1, -1).locations;
        } else if (location.hasName()) {
            return queryLocationsByName(location.name, EnumSet.of(location.type));
        } else {
            return null;
        }
    }

    private QueryTripsResult ambiguousQueryTrips(Location from, @Nullable Location via, Location to)
            throws IOException {
        List<Location> ambiguousFrom = solveAmbiguousLocation(from);
        if (ambiguousFrom == null || ambiguousFrom.size() <= 0)
            return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_FROM);

        List<Location> ambiguousTo = solveAmbiguousLocation(to);
        if (ambiguousTo == null || ambiguousTo.size() <= 0)
            return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_TO);

        List<Location> ambiguousVia = null;
        if (via != null) {
            ambiguousVia = solveAmbiguousLocation(via);
            if (ambiguousVia == null || ambiguousVia.size() <= 0)
                return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_VIA);
        }

        return new QueryTripsResult(this.resultHeader, ambiguousFrom, ambiguousVia, ambiguousTo);
    }

    private QueryTripsResult queryTrips(HttpUrl url, Location from, @Nullable Location via, Location to)
            throws IOException {
        final CharSequence page;
        try {
            page = httpClient.get(url);
        } catch (InternalErrorException e) {
            return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.SERVICE_DOWN);
        }

        List<Trip> foundTrips = new ArrayList<>();
        String tripsEarlier, tripsLater;
        try {
            final JSONObject head = new JSONObject(page.toString());

            if (head.has("error")) {
                switch (head.getString("error")) {
                case "WithinWalkingDistance":
                    return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.TOO_CLOSE);
                case "DateOutOfRange":
                    return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.INVALID_DATE);
                case "UnknownLocations":
                    String errorDetails = head.getString("details");
                    if (errorDetails.startsWith("From:")) {
                        return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_FROM);
                    } else if (errorDetails.startsWith("Via:")) {
                        return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_VIA);
                    } else if (errorDetails.startsWith("To:")) {
                        return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNKNOWN_TO);
                    } else {
                        return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
                    }
                default:
                    return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.NO_TRIPS);
                }
            }

            if (head.has("exception")) {
                return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.NO_TRIPS);
            }

            final JSONArray trips = head.optJSONArray("journeys");

            final JSONArray disturbances = head.optJSONArray("disturbances");

            // Prepare disturbances mapping for leg messages
            Map<String, JSONObject> disturbancesMap;
            if (disturbances != null && disturbances.length() > 0) {
                disturbancesMap = new HashMap<>();
                for (int i = 0; i < disturbances.length(); i++) {
                    JSONObject disturbance = disturbances.getJSONObject(i);
                    disturbancesMap.put(disturbance.getString("plannerDisturbanceId"), disturbance);
                }
            } else {
                disturbancesMap = null;
            }

            tripsEarlier = head.optString("earlier");
            tripsLater = head.optString("later");

            for (int i = 0; i < trips.length(); i++) {
                JSONObject trip = trips.getJSONObject(i);

                // Skip impossible or cancelled trips
                JSONObject realtimeInfo = trip.optJSONObject("realtimeInfo");
                if (realtimeInfo != null && ("fatal".equals(realtimeInfo.optString("delays"))
                        || "cancellations".equals(realtimeInfo.optString("cancellations"))))
                    continue;

                foundTrips.add(tripFromJSONObject(trip, from, to, disturbancesMap));
            }
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }

        return new QueryTripsResult(null, url.toString(), from, via, to,
                new TripsContext(url, tripsEarlier, tripsLater, from, via, to), foundTrips);
    }

    @Override
    public Set<Product> defaultProducts() {
        return EnumSet.of(Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY,
                Product.TRAM, Product.BUS, Product.FERRY);
    }

    @Override
    protected boolean hasCapability(Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(Set<LocationType> types, Location location, int maxDistance,
            int maxLocations) throws IOException {
        // Coordinates are required
        if (!location.hasCoord()) {
            try {
                if (location.hasId()) {
                    location = queryLocationById(location.id);
                } else if (location.hasName()) {
                    location = queryLocationByName(location.name, EnumSet.of(location.type));
                }
            } catch (InternalErrorException | NotFoundException | RuntimeException e) {
                return new NearbyLocationsResult(this.resultHeader, NearbyLocationsResult.Status.INVALID_ID);
            } catch (IOException e) {
                return new NearbyLocationsResult(this.resultHeader, NearbyLocationsResult.Status.SERVICE_DOWN);
            }

            if (location == null || !location.hasCoord()) {
                return new NearbyLocationsResult(this.resultHeader, NearbyLocationsResult.Status.INVALID_ID);
            }
        }

        // Default query options
        List<QueryParameter> queryParameters = new ArrayList<>();
        queryParameters.add(new QueryParameter("latlong", location.getLatAsDouble() + "," + location.getLonAsDouble()));
        queryParameters.add(new QueryParameter("rows",
                String.valueOf(Math.min((maxLocations <= 0) ? DEFAULT_MAX_LOCATIONS : maxLocations, 100))));

        // Add types if specified
        String locationTypes = locationTypesToQueryParameterString(types);
        if (locationTypes.length() > 0)
            queryParameters.add(new QueryParameter("type", locationTypes));

        HttpUrl url = buildApiUrl("locations", queryParameters);

        CharSequence page;
        try {
            page = httpClient.get(url);
        } catch (InternalErrorException e) {
            return new NearbyLocationsResult(this.resultHeader, NearbyLocationsResult.Status.SERVICE_DOWN);
        }

        // Parse result into location list
        final List<Location> foundLocations = new ArrayList<>();
        try {
            final JSONObject head = new JSONObject(page.toString());
            final JSONArray locations = head.optJSONArray("locations");

            for (int i = 0; i < locations.length(); i++) {
                foundLocations.add(locationFromJSONObject(locations.getJSONObject(i)));
            }
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }

        return new NearbyLocationsResult(new ResultHeader(network, SERVER_PRODUCT), foundLocations);
    }

    @Override
    public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures,
            boolean equivs) throws IOException {
        // The stationId does not need the / character escaped
        HttpUrl url = buildApiUrl("locations/" + stationId + "/departure-times", new ArrayList<QueryParameter>());
        final CharSequence page;
        try {
            page = httpClient.get(url);
        } catch (InternalErrorException | NotFoundException e) {
            return new QueryDeparturesResult(this.resultHeader, QueryDeparturesResult.Status.INVALID_STATION);
        } catch (Exception e) {
            return new QueryDeparturesResult(this.resultHeader, QueryDeparturesResult.Status.SERVICE_DOWN);
        }

        QueryDeparturesResult queryDeparturesResult = new QueryDeparturesResult(this.resultHeader);
        try {
            JSONObject head = new JSONObject(page.toString());
            JSONArray tabs = head.getJSONArray("tabs");
            for (int t = 0; t < tabs.length(); t++) {
                JSONObject tab = tabs.getJSONObject(t);

                JSONArray locations = tab.getJSONArray("locations");
                for (int l = 0; l < locations.length(); l++) {
                    JSONObject location = locations.getJSONObject(l);

                    // Ignore if equivs is false and stationId is not a strict match
                    if (!equivs && !location.getString("id").equals(stationId)) {
                        continue;
                    }

                    // Get list of departures
                    List<Departure> departuresResult = new ArrayList<>();
                    List<LineDestination> lineDestinationResult = new ArrayList<>();

                    JSONArray departures = tab.getJSONArray("departures");
                    for (int i = 0; i < departures.length(); i++) {
                        JSONObject departure = departures.getJSONObject(i);
                        JSONObject mode = departure.getJSONObject("mode");

                        departuresResult.add(departureFromJSONObject(departure));

                        Product lineProduct = productFromMode(mode.getString("type"), mode.getString("name"));
                        lineDestinationResult.add(new LineDestination(
                                new Line(null, departure.getString("operatorName"), lineProduct, mode.getString("name"),
                                        null, Standard.STYLES.get(lineProduct), null, null),
                                new Location(LocationType.STATION, null, null, null,
                                        departure.getString("destinationName"), EnumSet.of(lineProduct))));
                    }

                    // Add to result object
                    queryDeparturesResult.stationDepartures.add(new StationDepartures(locationFromJSONObject(location),
                            departuresResult, lineDestinationResult));
                }
            }

            return queryDeparturesResult;
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    @Override
    public SuggestLocationsResult suggestLocations(CharSequence constraint, @Nullable Set<LocationType> types,
            int maxLocations) throws IOException {
        HttpUrl url = buildApiUrl("locations", Arrays.asList(new QueryParameter("q", constraint.toString())));
        final CharSequence page;
        try {
            page = httpClient.get(url);
        } catch (InternalErrorException e) {
            return new SuggestLocationsResult(this.resultHeader, SuggestLocationsResult.Status.SERVICE_DOWN);
        }

        final List<SuggestedLocation> foundLocations = new ArrayList<>();
        try {
            final JSONObject head = new JSONObject(page.toString());
            final JSONArray locations = head.optJSONArray("locations");

            if (head.has("error")) {
                return new SuggestLocationsResult(this.resultHeader, SuggestLocationsResult.Status.SERVICE_DOWN);
            }

            for (int i = 0; i < locations.length(); i++) {
                JSONObject location = locations.getJSONObject(i);

                foundLocations.add(new SuggestedLocation(locationFromJSONObject(location)));
            }
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }

        return new SuggestLocationsResult(this.resultHeader, foundLocations);
    }

    @Override
    public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable TripOptions options) throws IOException {
        if (!(from.hasId() || from.hasCoord()))
            return ambiguousQueryTrips(from, via, to);

        if (!(to.hasId() || to.hasCoord()))
            return ambiguousQueryTrips(from, via, to);

        // Default query options
        List<QueryParameter> queryParameters = new ArrayList<>(
                Arrays.asList(new QueryParameter("from", locationToQueryParameterString(from)),
                        new QueryParameter("to", locationToQueryParameterString(to)),
                        new QueryParameter("searchType", dep ? "departure" : "arrival"),
                        new QueryParameter("dateTime", formatApiDateTime(date)),
                        new QueryParameter("sequence", "1"), new QueryParameter("realtime", "true"),
                        new QueryParameter("before", "1"), new QueryParameter("after", "5")));

        if (via != null) {
            if (!(via.hasId() || via.hasCoord()))
                return ambiguousQueryTrips(from, via, to);

            queryParameters.add(new QueryParameter("via", locationToQueryParameterString(via)));
        }

        if (options == null)
            options = new TripOptions();

        if (options.walkSpeed != null && options.walkSpeed == WalkSpeed.SLOW) {
            queryParameters.add(new QueryParameter("interchangeTime", InterchangeTime.EXTRA.toString()));
        } else {
            queryParameters.add(new QueryParameter("interchangeTime", InterchangeTime.STANDARD.toString()));
        }

        // Add trip product options to query
        Set<Product> products = options.products;
        if (products == null || products.size() == 0) {
            products = defaultProducts();
        }

        queryParameters.add(new QueryParameter("byBus", String.valueOf(products.contains(Product.BUS))));
        queryParameters.add(new QueryParameter("byTrain", String.valueOf(products.contains(Product.HIGH_SPEED_TRAIN)
                || products.contains(Product.REGIONAL_TRAIN) || products.contains(Product.SUBURBAN_TRAIN))));
        queryParameters.add(new QueryParameter("bySubway", String.valueOf(products.contains(Product.SUBWAY))));
        queryParameters.add(new QueryParameter("byTram", String.valueOf(products.contains(Product.TRAM))));
        queryParameters.add(new QueryParameter("byFerry", String.valueOf(products.contains(Product.FERRY))));

        return queryTrips(buildApiUrl("journeys", queryParameters), from, via, to);
    }

    @Override
    public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
        TripsContext tripContext = (TripsContext) context;

        HttpUrl url;
        if (later && context.canQueryLater()) {
            url = tripContext.getQueryLater();
        } else if (!later && context.canQueryEarlier()) {
            url = tripContext.getQueryEarlier();
        } else {
            return new QueryTripsResult(this.resultHeader, QueryTripsResult.Status.NO_TRIPS);
        }

        return queryTrips(url, tripContext.from, tripContext.via, tripContext.to);
    }
}
