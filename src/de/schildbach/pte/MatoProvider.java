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

import de.schildbach.pte.dto.*;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.util.GraphQLClient;
import de.schildbach.pte.util.PolylineFormat;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static de.schildbach.pte.LocationUtils.computeDistance;
import static de.schildbach.pte.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class MatoProvider extends AbstractNetworkProvider {
    private static final String URL_REQ = "https://mapi.5t.torino.it/routing/v1/routers/mat/index/graphql";
    private static final String URL_PLAN = "https://plan.muoversiatorino.it/otp/routers/mato/index/graphql";

    private static final String URL_GEOCODING ="https://geocode.muoversinpiemonte.it/v1/autocomplete";
    protected static final String SERVER_PRODUCT = "mato";


    private final GraphQLClient graphQLClient;
    private final int MAX_DEPARTURES_DEFAULT = 10;

    private ResultHeader getResultHeader() {return new ResultHeader(network, SERVER_PRODUCT);}

    public MatoProvider() {
        super(NetworkId.MATO);
        setStyles(STYLES);
        setTimeZone("Europe/Rome");
        setRequestUrlEncoding(StandardCharsets.UTF_8);
        setNumTripsRequested(6);
        //httpClient.setHeader("Host", "mapi.5t.torino.it");
        graphQLClient = new GraphQLClient(this.httpClient);
    }

    private final List CAPABILITIES = Arrays.asList(
            Capability.SUGGEST_LOCATIONS,
            Capability.NEARBY_LOCATIONS,
            Capability.DEPARTURES,
            Capability.TRIPS,
            Capability.TRIPS_VIA
    );

    @Override
    public final boolean hasCapability(final Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public Point[] getArea() throws IOException {
        //city center
        return new Point[]{Point.fromDouble(45.0677551, 7.6824892)};
    }

    @Override
    public Set<Product> defaultProducts() {
        return EnumSet.of(Product.BUS, Product.TRAM, Product.SUBURBAN_TRAIN, Product.CABLECAR, Product.REGIONAL_TRAIN, Product.SUBWAY);
    }

    @Nullable
    Product getProductFromString(String name){
        switch (name){
            case "BUS":
                return Product.BUS;
            case "TRAM":
                return Product.TRAM;
            case "SUBWAY":
                return Product.SUBWAY;
            case "RAIL":
                return Product.REGIONAL_TRAIN;
            case "FUNICULAR":
            case "CABLE_CAR":
                return Product.CABLECAR;
            case "FERRY":
                return Product.FERRY;
            case "AIRPLANE":
        }
        return null;
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(Set<LocationType> types, Location location, int maxDistance, int maxLocations) throws IOException {
        final ResultHeader resHeader = new ResultHeader(network, SERVER_PRODUCT);
        if(!location.hasCoord()){
            return new NearbyLocationsResult(resHeader, NearbyLocationsResult.Status.INVALID_ID);
        }
        final ArrayList<Location> foundLocations = new ArrayList<>();
        if(!types.contains(LocationType.STATION)){
            //return empty -- we can only get stops so far
            return new NearbyLocationsResult(resHeader,foundLocations);
            //throw new IllegalArgumentException("Need to have STATION in the types to query");
        }
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put("lat", location.getLatAsDouble());
        vars.put("lon", location.getLonAsDouble());
        vars.put("radius", maxDistance);

        JSONObject res;
        //try {
            res = graphQLClient.makeRequest(URL_REQ, "nearbyStops", vars,Queries.QUERY_NEARBY);
        //} catch (IOException e) {
        //    throw new RuntimeException(e);
        //}
        try{
            //["data"]["viewer"]["stopsByRadius"]["edges"]
            JSONArray stopsDists = res.getJSONObject("data")
                    .getJSONObject("viewer")
                    .getJSONObject("stopsByRadius").getJSONArray("edges");

            for(int i=0; i<stopsDists.length(); ++i){
                JSONObject elm = stopsDists.getJSONObject(i);
                if(elm == null)
                    continue;
                elm = elm.getJSONObject("node");
                if(elm == null) continue;
                //int distance = elm.getInt("distance");
                JSONObject stop = elm.getJSONObject("stop");
                foundLocations.add(parseMatoStopIntoLocation(stop));
            }
        } catch (JSONException e){
            throw new ParserException("Failed to parse nearby stops information", e);
        }
        //This cannot throw null (both `location` and all the stops found have coords)
        foundLocations.sort((l1, l2) ->
                (int) ((computeDistance(location.coord, l1.coord) - computeDistance(location.coord, l2.coord)) * 10));

        return new NearbyLocationsResult(resHeader,
                maxLocations>0 ? foundLocations.subList(0, Math.min(foundLocations.size(), maxLocations)) : foundLocations
                );
    }


    @Override
    public SuggestLocationsResult suggestLocations(CharSequence constraint, @Nullable Set<LocationType> types, int maxLocations) throws IOException {
        final ResultHeader resHeader = new ResultHeader(network, "autocomplete");

        HttpUrl url = requireNonNull(HttpUrl.parse(URL_GEOCODING)).newBuilder()
                .addQueryParameter("text", constraint.toString())
                .addQueryParameter("lang", "it").build();

        String resu = httpClient.get(url).toString();
        try{
            JSONObject queryRes = new JSONObject(resu);
            JSONArray feat = queryRes.getJSONArray("features");
            List<SuggestedLocation> suggestedLocations = new ArrayList<>();
            for(int i=0; i<feat.length(); ++i) {
                JSONObject feature = feat.getJSONObject(i);
                //only get Feature geojson
                if(!feature.getString("type").equals("Feature"))
                    continue;
                JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
                Point gpsLoc = Point.fromDouble(coords.getDouble(1),coords.getDouble(0) );

                JSONObject props = feature.getJSONObject("properties");
                LocationType type = LocationType.ADDRESS;
                if (Objects.equals(props.getString("layer"), "venue")){
                    type = LocationType.POI;
                }
                /*if(props.has("addendum")){
                    JSONObject x = props.getJSONObject("addendum").optJSONObject("osm");
                    if (x!=null){
                        if(x.optString("operator","").toLowerCase(Locale.ROOT).equals("gruppo torinese trasporti"))
                            type = LocationType.STATION;
                    }
                }*/
                suggestedLocations.add(new SuggestedLocation(
                        new Location(type, props.optString("id",""),
                                gpsLoc,
                                props.optString("locality",""),
                                props.getString("label")

                        ))
                );
            }
            if(types!=null){
                suggestedLocations = suggestedLocations.stream().filter(
                        sL -> types.contains(sL.location.type)
                ).collect(Collectors.toList());
            }
            if(maxLocations > 0)
                suggestedLocations = suggestedLocations.subList(0, Math.min(suggestedLocations.size(), maxLocations));

            return new SuggestLocationsResult(resHeader, suggestedLocations);

        } catch (JSONException e) {
            throw new ParserException("Cannot parse query response",e);
        }
    }

    /**
     * Build a Line from a JSON route. Uses attributes: "mode", "gtfsId","shortName", "color"
     * It would be cool to use the "longName" attribute too
     *
     * @param route the JSON route
     * @return the equivalent Line
     * @throws JSONException
     */
    private Line parseJsonRoute(JSONObject route) throws JSONException {
        final String mode = route.getString("mode");
        final String routeId = route.getString("gtfsId");
        final Product product;
        switch (mode) {
            case "RAIL":
                product = Product.SUBURBAN_TRAIN;
                break;
            case "SUBWAY":
                product = Product.SUBWAY;
                break;
            case "TRAM":
                product = Product.TRAM;
                break;
            case "CABLE_CAR":
            case "FUNICULAR":
                product = Product.CABLECAR;
                break;
            case "BUS":
            default:
                product = Product.BUS;
        }

        return new Line(routeId,
                routeId.split(":")[0],
                product,
                route.getString("shortName"), new Style(Style.parseColor("#" + route.getString("color")), Style.WHITE)
        );
    }

    private Location parseMatoStopIntoLocation(JSONObject stopJson) throws JSONException {
        Set<Product> products = null;
        if(stopJson.has("routes")){
            JSONArray rs = stopJson.getJSONArray("routes");
            products = new HashSet<>();
            for(int j=0; j<rs.length(); j++){
                JSONObject route = rs.getJSONObject(j);
                String mode = route.optString("mode");
                Product prod = mode != null ? getProductFromString(mode) : null;
                if(prod!=null)
                    products.add(prod);
            }
        }
        return new Location(LocationType.STATION,
                stopJson.getString("gtfsId"),
                Point.fromDouble(stopJson.getDouble("lat"), stopJson.getDouble("lon")),
                stopJson.getString("desc"),
                stopJson.getString("name"), products
        );
    }
    private Location parseMatoPlaceIntoLocation(JSONObject placeJson) throws  JSONException {
        return new Location(LocationType.ADDRESS, null,
                Point.fromDouble(placeJson.getDouble("lat"), placeJson.getDouble("lon")), null,
                placeJson.getString("name"), null
        );
    }

    private Stop parseMatoPlaceIntoStop(JSONObject placeJson, boolean isUpdated, boolean isCanceled) throws JSONException {
        //stop has departure time and arrival time
        final JSONObject stopJson = placeJson.optJSONObject("stop");
        final Location location;
        Position pos = null;
        if (stopJson == null){
            location = new Location(LocationType.ADDRESS, null,
                    Point.fromDouble(placeJson.getDouble("lat"), placeJson.getDouble("lon")), null,
                    placeJson.getString("name"), null
                    );
                    //Location.coord(Point.fromDouble(placeJson.getDouble("lat"), placeJson.getDouble("lon")));
        } else {
             location = parseMatoStopIntoLocation(stopJson);
             if (!Objects.equals(stopJson.optString("platformCode", "null"), "null")){
                 pos = new Position(stopJson.getString("platformCode"));
             }
        }
        final Date departureDate, arrivalDate;
        if(placeJson.has("departureTime"))
            departureDate = Date.from(Instant.ofEpochMilli(placeJson.getLong("departureTime")));
        else departureDate = null;
        if(placeJson.has("arrivalTime"))
            arrivalDate = Date.from(Instant.ofEpochMilli(placeJson.getLong("arrivalTime")));
        else arrivalDate = null;

        return new Stop(location,
                isUpdated ? null : arrivalDate, isUpdated? arrivalDate : null,
                isUpdated ? null : pos, isUpdated? pos : null, isCanceled,
                isUpdated ? null : departureDate, isUpdated? departureDate : null,
                isUpdated ? null : pos, isUpdated? pos : null, isCanceled
                );
    }

    @Override
    public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs) throws IOException {
        //need 3 things: timestamp (seconds), string of the station, and maxDepartures
        requireNonNull(stationId);
        final ResultHeader resultHeader = new ResultHeader(this.network, SERVER_PRODUCT);

        QueryDeparturesResult departuresResult = null;

        JSONObject currentObject = null;
        try {
            HashMap<String, Object> args = new HashMap<>();
            args.put("name", stationId);
            Instant instant;
            if (time != null) instant = time.toInstant();
            else instant = Instant.now();
            args.put("startTime", (long) (instant.toEpochMilli() / 1000));
            int numDep;
            if (maxDepartures == 0) numDep = 3;
            else numDep = maxDepartures;
            args.put("numberOfDepartures", numDep);

            JSONObject requestResult = graphQLClient.makeRequest(URL_REQ, "StopArrivals", args, Queries.QUERY_ARRIVALS);
            currentObject = requestResult;

            if (GraphQLClient.countErrors(requestResult) > 0) {
                //TODO: Do something better with the errors
                return new QueryDeparturesResult(resultHeader, QueryDeparturesResult.Status.INVALID_STATION);
            }
            JSONObject dataJson = requestResult.getJSONObject("data");
            if (dataJson.isNull("stop")) {
                return new QueryDeparturesResult(resultHeader, QueryDeparturesResult.Status.INVALID_STATION);
            }
            JSONObject stopJson = dataJson.getJSONObject("stop");
            currentObject = stopJson;

            departuresResult = new QueryDeparturesResult(resultHeader);

            final Location stopLocation = parseMatoStopIntoLocation(stopJson);
            final JSONArray patterns = stopJson.getJSONArray("stoptimesForPatterns");
            //final HashMap<String,ArrayList<Departure>> departuresByPattern = new HashMap<>();
            final ArrayList<Departure> departures = new ArrayList<>();
            final ArrayList<Departure> extraDeps = new ArrayList<>();
            for (int i = 0; i < patterns.length(); i++) {
                final JSONObject patternStops = patterns.getJSONObject(i);
                currentObject = patternStops;
                final JSONObject pat = patternStops.getJSONObject("pattern");
                final JSONObject route = pat.getJSONObject("route");

                final Line line = parseJsonRoute(route);

                final Location destination = new Location(LocationType.ADDRESS, null, null, pat.getString("headsign"));

                final JSONArray times = patternStops.getJSONArray("stoptimes");
                //final String patternCode = pat.getString("code");
                final ArrayList<Departure> patternDeps = new ArrayList<>();


                for (int j = 0; j < times.length(); j++) {
                    final JSONObject st = times.getJSONObject(j);
                    if ("CANCELED".equals(st.optString("realtimeState"))) continue;

                    final long day = st.getLong("serviceDay");            // epoch mezzanotte
                    final Date planned = new Date((day + st.getLong("scheduledDeparture")) * 1000);
                    final Date predicted = st.optBoolean("realtime")
                            ? new Date((day + st.getLong("realtimeDeparture")) * 1000) : null;
                    //TODO check if position=null is OK
                    final Departure dep = (new Departure(planned, predicted, line, null, destination, null, null));
                    //departures.add(dep);
                    patternDeps.add(dep);
                }
                //TODO: Decide if to show at least one departure for each line
                final int minNumPatt = 0; // set to 1
                Collections.sort(patternDeps, Departure.TIME_COMPARATOR);
                //departuresByPattern.put(patternCode, patternDeps);
                if (patternDeps.isEmpty())
                    continue;
                departures.addAll(patternDeps.subList(0, minNumPatt));
                extraDeps.addAll(patternDeps.subList(minNumPatt, patternDeps.size()));
            }

            if (departures.size() < maxDepartures) {
                int nadd = maxDepartures - departures.size();
                Collections.sort(extraDeps, Departure.TIME_COMPARATOR);
                departures.addAll(extraDeps.subList(0, Math.min(nadd, extraDeps.size())));
            }

            //minor TODO: Decide how to cut short the list, at some stops
            //
            departuresResult.stationDepartures.add(new StationDepartures(stopLocation, departures, null));

        } catch (JSONException exc) {
            throw new ParserException("Error when parsing JSON in request: " + currentObject, exc);
        } catch (RuntimeException e) {
            departuresResult = new QueryDeparturesResult(resultHeader, QueryDeparturesResult.Status.SERVICE_DOWN);
        }

        return departuresResult;
    }

    // ------- TRIP UTILS -----
    private HashMap<String, Object> getDefaultTripArguments() {
        HashMap<String, Object> args = new HashMap<>();
        /*
        An extra penalty added on transfers (i.e. all boardings except the first
        one). Not to be confused with bikeBoardCost and walkBoardCost, which are the
        cost of boarding a vehicle with and without a bicycle. The boardCosts are
        used to model the 'usual' perceived cost of using a transit vehicle, and the
        transferPenalty is used when a user requests even less transfers.
        */
        args.put("transferPenalty", 0);
        args.put("maxWalkDistance", 3000);
        args.put("bikeSpeed", 5);
        args.put("preferred", new HashMap<String, Object>());
        args.put("locale", "it"); //TODO: check if other locales work well
        return args;
    }

    private String getStringForLocation(Location location) {

        StringBuilder sb = new StringBuilder();
        if (location.name != null)
            sb.append(location.name);
        else if (location.place != null)
            sb.append(location.place);
        //do not append anything else
        sb.append("::");
        if (location.coord != null) {
            sb.append(location.coord.getLatAsDouble());
            sb.append(",");
            sb.append(location.coord.getLonAsDouble());
        }
        return sb.toString();
    }

    private Map<String, Object> makeTripArguments(TripsQueryContext context) {
        //first, get time and day
        final String dayStr = formatLocalDate(context.date);
        final String timeStr = formatLocalTime(context.date);

        final Map<String, Object> args = getDefaultTripArguments();
        args.put("date", dayStr);
        args.put("time", timeStr);
        args.put("fromPlace", getStringForLocation(context.from));
        args.put("toPlace", getStringForLocation(context.to));
        args.put("arriveBy", !context.isDateDep);

        //intermediate location
        final ArrayList<HashMap<String, Double>> intermediateLocs = new ArrayList<>();
        if (context.via != null) {
            final HashMap<String, Double> map = new HashMap<>();
            map.put("lat", context.via.getLatAsDouble());
            map.put("lon", context.via.getLonAsDouble());
            intermediateLocs.add(map);
        }
        args.put("intermediatePlaces", intermediateLocs);

        args.put("numItineraries", context.numItineraries);
        final int DEFAULT_TRANSFER_TIME = 120;
        //TODO: move this with options -> Product
        args.put("modes", "BUS,TRAM,SUBWAY,RAIL,WALK");

        if (context.options == null) {
            //Set default
            args.put("walkBoardCost", 600); // cost in seconds
            args.put("minTransferTime", DEFAULT_TRANSFER_TIME); //in seconds
            args.put("walkReluctance", 2); // Normale in Mato
            args.put("walkSpeed", 1.2);
            args.put("accessible", false);
        } else {
            double walkSpeed = 1.2;
            if (context.options.walkSpeed != null) switch (context.options.walkSpeed) {
                case FAST:
                    walkSpeed = 1.5; //circa 5.5 km/h
                    break;
                case SLOW:
                    walkSpeed = 0.83;
                    break;
                case NORMAL: //nothing to do
            }
            args.put("walkSpeed", walkSpeed);
            int walkBoardCost = 600; // in seconds
            int minTransferTime = DEFAULT_TRANSFER_TIME; // in seconds
            int walkReluctance = 2;
            if (context.options.optimize != null) switch (context.options.optimize) {
                case LEAST_CHANGES:
                    walkBoardCost = 3600;
                    break;
                case LEAST_WALKING:
                    walkReluctance = 5;  //5 is maximum, 1 is minimum
                    walkBoardCost = 360;
                    break;
                case LEAST_DURATION:
                    minTransferTime = 60;
            }
            args.put("walkBoardCost", walkBoardCost);
            args.put("minTransferTime", minTransferTime);
            args.put("walkReluctance", walkReluctance);
            args.put("accessible",
                    (context.options.accessibility != null && context.options.accessibility.equals(Accessibility.BARRIER_FREE)));

        }

        // products
        return args;
    }

    private QueryTripsResult makeTripRequest(TripsQueryContext tripsContext) throws IOException {
        final Location from = tripsContext.from;
        final Location to = tripsContext.to;
        final ResultHeader resultHeader = getResultHeader();

        if (from == null || !from.hasCoord()) {
            return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_FROM);
        } else if (to == null || !to.hasCoord()) {
            return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_TO);
        }
        if (tripsContext.via != null && !tripsContext.via.hasCoord())
            return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_VIA);

        final Map<String, Object> variables = makeTripArguments(tripsContext);

        httpClient.setHeader("Origin", "https://mato.muoversiatorino.it");
        httpClient.setHeader("Referer", "https://mato.muoversiatorino.it/");
        httpClient.setHeader("Content-Type", "application/json");

        String idquery = "from_"+from.uniqueShortName()+"_to_"+to.uniqueShortName()+"_date_"+
                formatLocalDate(tripsContext.date)+"T"+formatLocalTime(tripsContext.date);
        final JSONObject response = graphQLClient.makeRequest(URL_PLAN, Queries.NAME_QUERY_PLAN, variables, Queries.QUERY_PLAN);
        final ArrayList<Trip> trips = new ArrayList<>();
        try {
            final JSONArray iters = response.getJSONObject("data").getJSONObject("viewer").getJSONObject("_planMyRoute").getJSONArray("itineraries");
            for (int i = 0; i < iters.length(); i++) {
                final JSONObject itin = iters.getJSONObject(i);

                final ArrayList<Trip.Leg> legs = new ArrayList<>();
                final JSONArray legsJson = itin.getJSONArray("legs");
                int nMezzi = 0;

                for (int j = 0; j < legsJson.length(); j++) {
                    //each leg
                    final JSONObject legJ = legsJson.getJSONObject(j);
                    final boolean isCancelled = "CANCELED".equals(legJ.getString("realtimeState"));
                    final boolean isRealtime = "UPDATED".equals(legJ.getString("realtimeState"));
                    final Trip.Leg leg;
                    final JSONObject posFromJ = legJ.getJSONObject("from");
                    final JSONObject posToJ = legJ.getJSONObject("to");
                    final List<Point> points = PolylineFormat.decode(legJ.getJSONObject("legGeometry").getString("points"));

                    if (legJ.getBoolean("transitLeg")) {
                        final JSONArray iPlaces = legJ.getJSONArray("intermediatePlaces");
                        final JSONArray allStops = legJ.getJSONObject("trip").getJSONArray("stops");
                        //assuming that there is at least one stop in the stops array
                        final Location dest = parseMatoStopIntoLocation(allStops.getJSONObject(allStops.length() - 1));

                        final ArrayList<Stop> intermediateStops = new ArrayList<>();
                        for(int k=0; k<iPlaces.length(); k++){
                            intermediateStops.add(parseMatoPlaceIntoStop(iPlaces.getJSONObject(k), isRealtime, isCancelled));
                        }
                        leg = new Trip.Public(parseJsonRoute(legJ.getJSONObject("route")),
                                dest, parseMatoPlaceIntoStop(posFromJ, isRealtime, isCancelled),
                                parseMatoPlaceIntoStop(posToJ, isRealtime, isCancelled),
                                intermediateStops, points, null
                        );
                        // `message` should not be empty, but the MaTO endpoint does not provide alerts
                        nMezzi++;
                    } else{
                        //TODO
                        Trip.Individual.Type type = Trip.Individual.Type.TRANSFER;
                        switch( legJ.getString("mode")){
                            case "CAR":
                                type = Trip.Individual.Type.CAR;
                                break;
                            case "BICYCLE":
                                type = Trip.Individual.Type.BIKE;
                                break;
                            case "WALK":
                                type = Trip.Individual.Type.WALK;
                        }

                        leg = new Trip.Individual(type, parseMatoPlaceIntoLocation(posFromJ),
                                Date.from(Instant.ofEpochMilli(legJ.getLong("startTime"))),
                                parseMatoPlaceIntoLocation(posToJ),
                                Date.from(Instant.ofEpochMilli(legJ.getLong("endTime"))),
                                points, (int) legJ.getDouble("distance")
                        );
                    }
                    legs.add(leg);
                }
                trips.add(new Trip(idquery+"-"+i,
                                legs.get(0).departure, legs.get(legs.size()-1).arrival, legs, null, null, nMezzi-1
                        )
                );
            }
            return new QueryTripsResult(resultHeader, null, from, tripsContext.via, to, tripsContext, trips);
        } catch (JSONException e) {
            throw new IOException("Could not parse JSON of response", e);
        }
    }

    @Override
    public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep, @Nullable TripOptions options) throws IOException {
        final TripsQueryContext tripsContext = new TripsQueryContext(from, via, to, date, dep, options, 5);

        return makeTripRequest(tripsContext);
    }

    @Override
    public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
        final ResultHeader header = getResultHeader();
        if(context instanceof TripsQueryContext && (context.canQueryEarlier() || context.canQueryLater())  )
        {

            final TripsQueryContext tripsContext = ((TripsQueryContext) context);
            return makeTripRequest(tripsContext.newWithItineraries(tripsContext.numItineraries+2));
        } else
            return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
    }

    // -- dates util
    private static final ZoneId ITALY = ZoneId.of("Europe/Rome");

    public static String formatLocalDate(Date time) {
        return time.toInstant().atZone(ITALY).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String formatLocalTime(Date time) {
        return time.toInstant().atZone(ITALY).toLocalTime().withNano(0).format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    // ----- STYLES STUFF -----
    private static final Map<String, Style> STYLES = new HashMap<>();

    private static void addStyle(String key, String hex) {
        STYLES.put(key, new Style(Style.parseColor(hex), Style.WHITE));
    }

    static {
        addStyle("gtt:3105E", "#0080ff");
        addStyle("gtt:METROU", "#ff0000");
        addStyle("gtt:75U", "#ff9900");
        addStyle("gtt:63U", "#ff9900");
        addStyle("gtt:51U", "#ff9900");
        addStyle("gtt:5135E", "#0080ff");
        addStyle("gtt:3445E", "#0080ff");
        addStyle("gtt:3106E", "#0080ff");
        addStyle("gtt:16CDU", "#cc9900");
        addStyle("gtt:W60U", "#ff9900");
        addStyle("gtt:4546E", "#0080ff");
        addStyle("gtt:2016E", "#0080ff");
        addStyle("gtt:3990U", "#33cc00");
        addStyle("gtt:50U", "#ff9900");
        addStyle("gtt:VE1U", "#ff9900");
        addStyle("gtt:74U", "#ff9900");
        addStyle("gtt:62U", "#ff9900");
        addStyle("gtt:3006E", "#0080ff");
        addStyle("gtt:3321E", "#0080ff");
        addStyle("gtt:65U", "#ff9900");
        addStyle("gtt:53U", "#ff9900");
        addStyle("gtt:89U", "#ff9900");
        addStyle("gtt:2027E", "#0080ff");
        addStyle("gtt:77U", "#ff9900");
        addStyle("gtt:2354E", "#0080ff");
        addStyle("gtt:VE2U", "#ff9900");
        addStyle("gtt:41U", "#ff9900");
        addStyle("gtt:5379E", "#0080ff");
        addStyle("gtt:5137E", "#0080ff");
        addStyle("gtt:3310E", "#0080ff");
        addStyle("gtt:76U", "#ff9900");
        addStyle("gtt:64U", "#ff9900");
        addStyle("gtt:88U", "#ff9900");
        addStyle("gtt:4108E", "#0080ff");
        addStyle("gtt:2014E", "#0080ff");
        addStyle("gtt:52U", "#ff9900");
        addStyle("gtt:40U", "#ff9900");
        addStyle("gtt:5322E", "#0080ff");
        addStyle("gtt:3331E", "#0080ff");
        addStyle("gtt:43U", "#ff9900");
        addStyle("gtt:67U", "#ff9900");
        addStyle("gtt:55U", "#ff9900");
        addStyle("gtt:4541E", "#0080ff");
        addStyle("gtt:79U", "#33cc00");
        addStyle("gtt:S04U", "#ff9900");
        addStyle("gtt:1NU", "#ff9900");
        addStyle("gtt:1068E", "#0080ff");
        addStyle("gtt:54U", "#ff9900");
        addStyle("gtt:42U", "#ff9900");
        addStyle("gtt:78U", "#ff9900");
        addStyle("gtt:66U", "#ff9900");
        addStyle("gtt:S05U", "#ff9900");
        addStyle("gtt:30U", "#ff9900");
        addStyle("gtt:13BU", "#ff9900");
        addStyle("gtt:59BU", "#ff9900");
        addStyle("gtt:45U", "#ff9900");
        addStyle("gtt:33U", "#ff9900");
        addStyle("gtt:69U", "#ff9900");
        addStyle("gtt:S18U", "#ff9900");
        addStyle("gtt:19NU", "#ff9900");
        addStyle("gtt:VEXU", "#33cc00");
        addStyle("gtt:32U", "#ff9900");
        addStyle("gtt:20U", "#ff9900");
        addStyle("gtt:56U", "#ff9900");
        addStyle("gtt:44U", "#ff9900");
        addStyle("gtt:68U", "#ff9900");
        addStyle("gtt:2CU", "#ff9900");
        addStyle("gtt:NPVU", "#ff9900");
        addStyle("gtt:3133E", "#0080ff");
        addStyle("gtt:3121E", "#0080ff");
        addStyle("gtt:SE1U", "#ff9900");
        addStyle("gtt:10NU", "#ff9900");
        addStyle("gtt:132U", "#ff9900");
        addStyle("gtt:SE2U", "#ff9900");
        addStyle("gtt:N08U", "#ff9900");
        addStyle("gtt:1CU", "#ff9900");
        addStyle("gtt:1087E", "#0080ff");
        addStyle("gtt:1099E", "#0080ff");
        addStyle("gtt:3131E", "#0080ff");
        addStyle("gtt:4147E", "#0080ff");
        addStyle("gtt:72BU", "#ff9900");
        addStyle("gtt:159U", "#ff9900");
        addStyle("gtt:81U", "#ff9900");
        addStyle("gtt:3023E", "#0080ff");
        addStyle("gtt:ST1U", "#ff9900");
        addStyle("gtt:1086E", "#0080ff");
        addStyle("gtt:3132E", "#0080ff");
        addStyle("gtt:80U", "#ff9900");
        addStyle("gtt:5318E", "#0080ff");
        addStyle("gtt:ST2U", "#ff9900");
        addStyle("gtt:1085E", "#0080ff");
        addStyle("gtt:3165E", "#0080ff");
        addStyle("gtt:4157E", "#0080ff");
        addStyle("gtt:5319E", "#0080ff");
        addStyle("gtt:83U", "#ff9900");
        addStyle("gtt:71U", "#ff9900");
        addStyle("gtt:3166E", "#0080ff");
        addStyle("gtt:70U", "#ff9900");
        addStyle("gtt:82U", "#ff9900");
        addStyle("gtt:17BU", "#ff9900");
        addStyle("gtt:93BU", "#ff9900");
        addStyle("gtt:2172E", "#0080ff");
        addStyle("gtt:44SU", "#ff9900");
        addStyle("gtt:61U", "#ff9900");
        addStyle("gtt:4155E", "#0080ff");
        addStyle("gtt:73U", "#ff9900");
        addStyle("gtt:3382E", "#0080ff");
        addStyle("gtt:4156E", "#0080ff");
        addStyle("gtt:4168E", "#0080ff");
        addStyle("gtt:102U", "#ff9900");
        addStyle("gtt:2183E", "#0080ff");
        addStyle("gtt:72U", "#ff9900");
        addStyle("gtt:60U", "#ff9900");
        addStyle("gtt:84U", "#ff9900");
        addStyle("gtt:5095E", "#0080ff");
        addStyle("gtt:N10U", "#ff9900");
        addStyle("gtt:19U", "#ff9900");
        addStyle("gtt:2073E", "#0080ff");
        addStyle("gtt:9U", "#cc9900");
        addStyle("gtt:4153E", "#0080ff");
        addStyle("gtt:89BU", "#ff9900");
        addStyle("gtt:18U", "#ff9900");
        addStyle("gtt:8U", "#ff9900");
        addStyle("gtt:4154E", "#0080ff");
        addStyle("gtt:1091E", "#0080ff");
        addStyle("gtt:CP1U", "#ff9900");
        addStyle("gtt:16CSU", "#cc9900");
        addStyle("gtt:36NU", "#ff9900");
        addStyle("gtt:OB1U", "#ff9900");
        addStyle("gtt:4151E", "#0080ff");
        addStyle("gtt:E68U", "#ff9900");
        addStyle("gtt:1511E", "#0080ff");
        addStyle("gtt:5BU", "#ff9900");
        addStyle("gtt:4152E", "#0080ff");
        addStyle("gtt:3096E", "#0080ff");
        addStyle("gtt:1510E", "#0080ff");
        addStyle("gtt:35NU", "#ff9900");
        addStyle("gtt:RV2U", "#ff9900");
        addStyle("gtt:38SU", "#ff9900");
        addStyle("gtt:N04U", "#ff9900");
        addStyle("gtt:5150E", "#0080ff");
        addStyle("gtt:1435E", "#0080ff");
        addStyle("gtt:86BU", "#ff9900");
        addStyle("gtt:63BU", "#ff9900");
        addStyle("gtt:58BU", "#ff9900");
        addStyle("gtt:5140E", "#0080ff");
        addStyle("gtt:3092E", "#0080ff");
        addStyle("gtt:LS1U", "#ff9900");
        addStyle("gtt:11U", "#ff9900");
        addStyle("gtt:47U", "#ff9900");
        addStyle("gtt:35U", "#ff9900");
        addStyle("gtt:59U", "#ff9900");
        addStyle("gtt:SPXU", "#ff9900");
        addStyle("gtt:3429E", "#0080ff");
        addStyle("gtt:3502E", "#0080ff");
        addStyle("gtt:3950E", "#0080ff");
        addStyle("gtt:1432E", "#0080ff");
        addStyle("gtt:10U", "#cc9900");
        addStyle("gtt:34U", "#ff9900");
        addStyle("gtt:22U", "#ff9900");
        addStyle("gtt:58U", "#ff9900");
        addStyle("gtt:46U", "#ff9900");
        addStyle("gtt:MEORU", "#ff9900");
        addStyle("gtt:2U", "#ff9900");
        addStyle("gtt:3971E", "#0080ff");
        addStyle("gtt:25U", "#ff9900");
        addStyle("gtt:13U", "#cc9900");
        addStyle("gtt:49U", "#ff9900");
        addStyle("gtt:1442E", "#0080ff");
        addStyle("gtt:5141E", "#0080ff");
        addStyle("gtt:1224E", "#0080ff");
        addStyle("gtt:1128E", "#0080ff");
        addStyle("gtt:12U", "#ff9900");
        addStyle("gtt:36U", "#ff9900");
        addStyle("gtt:1SU", "#ff9900");
        addStyle("gtt:24U", "#ff9900");
        addStyle("gtt:48U", "#ff9900");
        addStyle("gtt:3428E", "#0080ff");
        addStyle("gtt:3545E", "#0080ff");
        addStyle("gtt:4U", "#cc9900");
        addStyle("gtt:39U", "#ff9900");
        addStyle("gtt:79BU", "#ff9900");
        addStyle("gtt:27U", "#ff9900");
        addStyle("gtt:15U", "#cc9900");
        addStyle("gtt:3316E", "#0080ff");
        addStyle("gtt:3U", "#cc9900");
        addStyle("gtt:45BU", "#ff9900");
        addStyle("gtt:68BU", "#ff9900");
        addStyle("gtt:5143E", "#0080ff");
        addStyle("gtt:2009E", "#0080ff");
        addStyle("gtt:W01U", "#ff9900");
        addStyle("gtt:14U", "#ff9900");
        addStyle("gtt:38U", "#ff9900");
        addStyle("gtt:26U", "#ff9900");
        addStyle("gtt:3329E", "#0080ff");
        addStyle("gtt:3107E", "#0080ff");
        addStyle("gtt:29U", "#ff9900");
        addStyle("gtt:17U", "#ff9900");
        addStyle("gtt:5134E", "#0080ff");
        addStyle("gtt:7U", "#33cc00");
        addStyle("gtt:M1SU", "#ff9900");
        addStyle("gtt:5U", "#ff9900");
        addStyle("gtt:5145E", "#0080ff");
        addStyle("gtt:4512E", "#0080ff");
        addStyle("gtt:W15U", "#ff9900");
        addStyle("gtt:6U", "#ff9900");
    }
    final static class TripsQueryContext implements QueryTripsContext {

        final Location from;
        final Location to;
        final @Nullable Location via;
        final Date date;
        final boolean isDateDep;
        final @Nullable TripOptions options;

        final int numItineraries;

        public TripsQueryContext(Location from, @Nullable Location via, Location to, Date date, boolean isDateDep, @Nullable TripOptions options, int numItineraries) {
            this.from = from;
            this.to = to;
            this.via = via;
            this.date = date;
            this.isDateDep = isDateDep;
            this.options = options;
            this.numItineraries = numItineraries;
        }

        public TripsQueryContext newWithItineraries(int numItineraries){
            return new TripsQueryContext(from, via, to, date, isDateDep, options, numItineraries);
        }
        @Override
        public boolean canQueryLater() {
            return isDateDep;
        }

        @Override
        public boolean canQueryEarlier() {
            return !isDateDep;
        }
    }

    final static class Queries {
        final static String QUERY_ARRIVALS = "query StopArrivals(\n  $name: String!\n  $startTime: Long\n  $numberOfDepartures: Int\n) {\n  stop(id: $name) {\n    lat lon gtfsId code name desc zoneId\n    wheelchairBoarding\n    " +
                "stoptimesForPatterns(\n            startTime: $startTime\n            numberOfDepartures: $numberOfDepartures\n          ) {\n            " +
                "pattern {\n              id\n              code\n              headsign\n              directionId\n              " +
                "route {\n                gtfsId\n                shortName\n                mode\n                color\n              }\n            }\n     " +
                "       stoptimes {\n              serviceDay\n              scheduledDeparture\n              realtimeDeparture\n              realtime\n              realtimeState\n            }\n    }\n  }\n}";

        final static String NAME_QUERY_PLAN = "PlanRoute";
        //final static String QUERY_PLAN = "query Routes( $fromPlace: String $toPlace: String $date: String $time: String $locale: String $modes: String $arriveBy: Boolean $accessible: Boolean $intermediatePlaces: [InputCoordinates]! $numItineraries: Int! $walkBoardCost: Int! $minTransferTime: Int! $transferPenalty: Int! $walkReluctance: Float! $walkSpeed: Float! $maxWalkDistance: Float! $bikeSpeed: Float! $preferred: InputPreferred! ) { viewer { ...Fc } } fragment Fa on Place{ arrivalTime departureTime name lat lon vertexType stop { lat lon gtfsId code name desc platformCode } } fragment Fc on QueryType{ _planMyRoute: plan( fromPlace: $fromPlace toPlace: $toPlace intermediatePlaces: $intermediatePlaces numItineraries: $numItineraries modes: $modes date: $date time: $time walkReluctance: $walkReluctance walkBoardCost: $walkBoardCost minTransferTime: $minTransferTime walkSpeed: $walkSpeed maxWalkDistance: $maxWalkDistance wheelchair: $accessible arriveBy: $arriveBy transferPenalty: $transferPenalty bikeSpeed: $bikeSpeed optimize: QUICK itineraryFiltering: 1.5 preferred: $preferred locale: $locale ) { itineraries { startTime endTime legs { mode transitLeg startTime endTime distance realTime realtimeState legGeometry { points length } from { ...Fa } to { ...Fa } route { gtfsId shortName longName mode color agency { name } } trip { gtfsId pattern { code } tripHeadsign } intermediatePlaces { ...Fa } } } } } ";
        final  static String QUERY_PLAN ="query PlanRoute( $fromPlace: String $toPlace: String $date: String $time: String $locale: String $modes: String $arriveBy: Boolean $accessible: Boolean $intermediatePlaces: [InputCoordinates]! $numItineraries: Int! $walkBoardCost: Int! $minTransferTime: Int! $transferPenalty: Int! $walkReluctance: Float! $walkSpeed: Float! $maxWalkDistance: Float! $bikeSpeed: Float! $preferred: InputPreferred! ) { viewer { ...Fc } } fragment Fa on Place{ name lat lon vertexType stop { lat lon gtfsId code name desc platformCode } } fragment Fc on QueryType{ _planMyRoute: plan( fromPlace: $fromPlace toPlace: $toPlace intermediatePlaces: $intermediatePlaces numItineraries: $numItineraries modes: $modes date: $date time: $time walkReluctance: $walkReluctance walkBoardCost: $walkBoardCost minTransferTime: $minTransferTime walkSpeed: $walkSpeed maxWalkDistance: $maxWalkDistance wheelchair: $accessible arriveBy: $arriveBy transferPenalty: $transferPenalty bikeSpeed: $bikeSpeed optimize: QUICK itineraryFiltering: 1.5 preferred: $preferred locale: $locale ) { itineraries { startTime endTime legs { mode transitLeg startTime endTime distance realTime realtimeState legGeometry { points length } from { ...Fa departureTime } to { ...Fa arrivalTime } route { gtfsId shortName longName mode color agency { name } } trip { gtfsId pattern { code } tripHeadsign stops { name gtfsId lat lon desc } } intermediatePlaces { arrivalTime departureTime ...Fa } } } } } "
                ;

        final static String QUERY_NEARBY="query nearbyStops( $lat: Float! $lon: Float! $radius: Int! ) { viewer{ stopsByRadius(lat: $lat, lon: $lon, radius: $radius, ){ edges { node{ stop{ id name lat lon direction code desc gtfsId routes { gtfsId mode } } distance } } } } }";
    }
}