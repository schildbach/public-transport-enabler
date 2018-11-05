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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Departure;
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
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Individual.Type;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.exception.ParserException;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <code>AbstractOpenTripPlannerProvider</code> allows integration of OTP-based route planners like
 * the Ridesharing/Transit project Mitfahren-BW or TriMet's Portland regional trip planner.
 *
 * <p>Note: Currently, there are still some mapping issues which need to be resolved, some of them
 * on OTP side:</p>
 * <ul>
 *     <li>The provider matches OTP stops to PTE station locations. However, often an OTP stop
 *     represents an individual platform, which should be converted to a <code>Position</code>.
 *     A better match would be to associate a PTE station with an OTP cluster. However, OTP's REST
 *     API currently has not yet good support for querying clusters. E.g. names are in upper case,
 *     ids are generated though they probably could be deduced as common prefix from stopId (GTFS
 *     has currently the concept of parent_stops which seeems to apply mainly to large stations, but
 *     not to different platforms of a bus stop.), there are no endpoints for patterns/stoptimes/routes
 *     for a cluster,...</li>
 *     <li>The OTP geocoder API is very basic. E.g. it does not return the stop type as attribute but
 *     as prefix in the description, supports only street corner and not address search, and the
 *     implementation is currently rather unusable ((very)fuzzy matches hide exact matches). StopIds
 *     have separate AgencyStopIds with underscore, where all other endpoints expect a colon separator.
 *     Concrete implementations will need to implement their own adapter methods.</li>
 *     <li>OTP patterns in the REST API return nearly no structured information. Especially the
 *     patterns destination needs to be parsed from the pattern description, which is error prone.
 *     Additionally, it should provide a routeId which associates the pattern with it's corresponding route
 *     (we currently truncate the two rightmost colons(?))</li>
 *     <li>realtime support is limited</li>
 *     <li>fares support is not implemented yet</li>
 *     <li>OTP and PTE modes do not match perfectly.</li>
 *     <ul>
 *         <li>PTE option BIKE is interpreted as OTP'sindividual leg mode "BICYCLE"</li>
 *      </ul>
 *     <li>Still open TODOs
 *     <ul>
 *         <li>parse OTProutes bikeAllowed to bike_carriage</li>
 *         <li>for nearbyStops requested by station without coords: request stop coords first</li>
 *         <li>for equiv stops, all operations for an individual stop should work for clusters as well</li>
 *         <li>Support querying earlier or later trip</li>
 *     </ul></li>
 * </ul>
 * @author Holger Bruch
 * @author Kavitha Ravi
 */
class AbstractOpenTripPlannerProvider extends AbstractNetworkProvider {

    protected final static String SERVER_PRODUCT = "otp";
    protected final static String SERVER_VERSION = "v1.2";
    private final String router;
    private final Multimap<Product, String> productsToModeMap ;
    private final Multimap<String, Product> modeToProductMap = ArrayListMultimap.create();
    private Locale locale = Locale.ENGLISH;

    protected enum OTPMode {
        WALK, BIKE, CAR, RAIL, SUBWAY, TRAM, BUS, CABLE_CAR, FERRY, GONDOLA, FUNICULAR, AIRPLANE,
        TRANSIT
    }

    protected final HttpUrl apiBase ;
    
    @SuppressWarnings("serial")
    private static class Context implements QueryTripsContext {
        @Override
        public boolean canQueryLater() {
            return false;
        }

        @Override
        public boolean canQueryEarlier() {
            return false;
        }
    }

    protected AbstractOpenTripPlannerProvider(NetworkId network, HttpUrl apiBase) {
        this(network, apiBase, "default", null);
    }

    protected AbstractOpenTripPlannerProvider(NetworkId network, HttpUrl apiBase, String router, Multimap<Product, String> productsToMode) {
        super(network);
        this.apiBase = apiBase;
        if (productsToMode == null) {
            this.productsToModeMap = defaultProductToOTPModeMapping();
        } else {
            this.productsToModeMap = productsToMode;
        }
        Multimaps.invertFrom(this.productsToModeMap , this.modeToProductMap);
        this.router = router;
        this.httpClient.setHeader("Accept", "application/json");
    }

    private void buildInverseMap() {
        for (Product product: productsToModeMap.keys()){
            Collection<String> modes = productsToModeMap.get(product);
            modeToProductMap.put(modes.iterator().next(), product);
        }
    }

    private HttpUrl.Builder url() {
        return apiBase.newBuilder().addPathSegment("routers").addPathSegment(router());
    }

    protected String router() {
        return router;
    }

    @Override
    protected boolean hasCapability(final Capability capability) {
        if (capability == Capability.SUGGEST_LOCATIONS || capability == Capability.NEARBY_LOCATIONS
                || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
            return true;
        else
            return false;
    }

    /**
     * Set {@code Locale} used to query te OTP server.
     * @param locale
     */
    public void setLocale(Locale locale){
        this.locale = locale;
    }

    protected Multimap<Product, String> defaultProductToOTPModeMapping() {
        Multimap<Product, String> productsToModeMap = ArrayListMultimap.create();
        // OTP does not distinguish between high speed/long distance
        // regional or suburban trains. For requests, we map all to rail.
        // When parsing responses, we rely on OTP's routeType, where available
        productsToModeMap.put(Product.HIGH_SPEED_TRAIN, OTPMode.RAIL.toString());
        productsToModeMap.put(Product.REGIONAL_TRAIN, OTPMode.RAIL.toString());
        productsToModeMap.put(Product.SUBURBAN_TRAIN, OTPMode.RAIL.toString());
        productsToModeMap.put(Product.SUBWAY, OTPMode.SUBWAY.toString());
        productsToModeMap.put(Product.TRAM, OTPMode.TRAM.toString());
        productsToModeMap.put(Product.BUS, OTPMode.BUS.toString());
        productsToModeMap.put(Product.FERRY, OTPMode.FERRY.toString());
        productsToModeMap.put(Product.CABLECAR, OTPMode.CABLE_CAR.toString());
        // GONDOLA and FUNICULAR are no PTE products, but OTP modes.
        // Associate them with CABLE_CAR, when performing requests
        productsToModeMap.put(Product.CABLECAR, OTPMode.GONDOLA.toString());
        productsToModeMap.put(Product.CABLECAR, OTPMode.FUNICULAR.toString());
        // Product.ON_DEMAND is not supported by OTP yet
        // OTPMode.AIRPLANE is not supported by PTE yet
        return productsToModeMap;
    }

    protected CharSequence request(HttpUrl build) throws IOException {
        return httpClient.get(build);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(EnumSet<LocationType> types, Location location, int maxDistance,
                                                      int maxLocations) throws IOException {
        final Builder url = buildQueryNearybyLocations(location, maxDistance);
        final String page = httpClient.get(url.build()).toString();
        return parseNearbyLocations(page);
    }

    private Builder buildQueryNearybyLocations(Location location, int maxDistance) {
        final Builder url = url();
        if (!location.hasLocation())
            throw new IllegalArgumentException();
        url.addPathSegments("/index/stops");
        url.addQueryParameter("lat", Double.toString(location.getLatAsDouble()));
        url.addQueryParameter("lon", Double.toString(location.getLonAsDouble()));
        url.addQueryParameter("radius", Integer.toString(maxDistance == 0 ? 5000 : maxDistance));
        return url;
    }

    protected NearbyLocationsResult parseNearbyLocations(String page) throws ParserException {
        try {
            final JSONArray body = new JSONArray(page);
            final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);
            final int nbResults = body.length();
            // If no result is available, location id must be
            // faulty.
            if (nbResults == 0) {
                return new NearbyLocationsResult(resultHeader, NearbyLocationsResult.Status.INVALID_ID);
            } else {
                final List<Location> stations = new ArrayList<>();

                // Cycle through nearby stations.
                for (int i = 0; i < body.length(); ++i) {
                    final JSONObject place = body.getJSONObject(i);
                    final Location nearbyLocation = parseNearbyLocation(place);
                    stations.add(nearbyLocation);
                }

                return new NearbyLocationsResult(resultHeader, stations);
            }
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    protected Location parseNearbyLocation(JSONObject location) throws ParserException {
        try {
            String id = location.getString("id");
            Point point = Point.fromDouble(location.getDouble("lat"),location.getDouble("lon"));
            String name = location.getString("name");
            String[] placeAndName = splitStationName(name);
            Location loc = new Location(LocationType.STATION, id, point, placeAndName[0], placeAndName[1]);
            return loc;
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    @Override
    public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs) throws IOException {
        // <stopId>/stoptimes returns stoptimes grouped by patterns. A route (line in pte parlance)
        // may have multiple patterns. A pattern in otp is a unique order of stops and may
        // correspond to e.g. different destinations.
        // PTE expects QueryDeparturesResults, which for each station consist of a list of
        // LineDepartures (Routes with different destinations), and a list of departures, each of
        // which is associated with a line and destination.
        // We collect all lines = routes for the stop, and afterwards, the stoptimes.
        // From the OTP pattern id we derive the route id (by truncating the two right colons).
        // And from the pattern desc, we do string parsing to figure put the name and id of the
        // destination stop. => Would be nice if OTP would provide this info explicitly.
        // Examples:
        // http://api.mfdz.de/otp/routers/default/index/stops/nvbv:de:08125:4344:1:1/routes
        // http://api.mfdz.de/otp/routers/default/index/stops/nvbv:de:08125:4344:1:1/stoptimes?numberOfDepartures=2
        // Note: numberOfDepartures is the number of departures per pattern... All patterns are listed :-(
        // As a workaround, we need to cut results on the client side

        checkNotNull(Strings.emptyToNull(stationId));
        List<Line> lines = queryLines(stationId, equivs);

        final Builder url = buildQueryDepartures(stationId, maxDepartures, equivs);
        final CharSequence page = httpClient.get(url.build());
        return parseQueryDeparturesResponse(page, lines);
    }

    private List<Line> queryLines(String stationId, boolean equivs) throws IOException {
        final HttpUrl.Builder url = buildQueryLines(stationId, equivs);
        final CharSequence page = httpClient.get(url.build());
        return parseLinesFromStopRoutes(page.toString());
    }

    protected Builder buildQueryLines(String stationId, boolean equivs) {
        final HttpUrl.Builder url = url();
        // TODO equivs not yet implemented, currently we silently ignore
        url.addPathSegments("index/stops");
        url.addPathSegment(stationId);
        url.addPathSegment("routes");
        return url;
    }

    protected List<Line> parseLinesFromStopRoutes(String page) throws ParserException {
        try {
            JSONArray routes = new JSONArray(page.toString());
            List<Line> lines = new ArrayList<>();
            for (int i = 0; i < routes.length(); ++i) {
                JSONObject route = routes.getJSONObject(i);
                Line line = parseLineFromStopRoutes(route);
                lines.add(line);
            }
            return lines;
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    protected Line parseLineFromStopRoutes(JSONObject route) throws JSONException {
        String id = route.getString("id");
        String network = route.getString("agencyName");
        String mode = route.getString("mode");
        // TODO fix OTP to return routeType, as OTP mode RAIL does not discriminate
        // highspeed, regional, suburban (or perhaps better: switch to graphql query)
        int routeType = route.optInt("routeType");
        Product product = parseProduct(mode, routeType);
        String label = route.optString("shortName");
        String name = route.optString("longName");
        Style style = new Style(Style.parseColor("#"+route.optString("color", "ffffff")));

        return new Line(id, network, product, label, name, style);
    }

    protected Builder buildQueryDepartures(String stationId, int maxDepartures, boolean equivs) {
        final Builder url = url();
        // TODO what are equivs stops?
        url.addPathSegments("index/stops");
        url.addPathSegment(stationId);
        url.addPathSegment("stoptimes");
        url.addQueryParameter("numberOfDepartures", Integer.toString(maxDepartures));
        return url;
    }

    protected QueryDeparturesResult parseQueryDeparturesResponse(CharSequence page, List<Line> lines) throws ParserException {
        try {
            final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);
            final QueryDeparturesResult result = new QueryDeparturesResult(resultHeader,
                    QueryDeparturesResult.Status.OK);
            final JSONArray patterns = new JSONArray(page.toString());

            // Fill departures in StationDepartures.
            for (int i = 0; i < patterns.length(); ++i) {
                final JSONObject jsonPattern = patterns.getJSONObject(i);
                String patternId = jsonPattern.getJSONObject("pattern").getString("id");
                final Line line = findLineByPatternId(lines, patternId);
                final Location destination = parseDestinationFromPatternDesc(jsonPattern.getJSONObject("pattern").getString("desc"));

                final JSONArray jsonTimes = jsonPattern.getJSONArray("times");

                for (int j = 0; j < jsonTimes.length(); ++j) {
                    JSONObject jsonTime = jsonTimes.getJSONObject(j);
                    final int scheduledDeparture = jsonTime.getInt("scheduledDeparture");
                    final long serviceDay = jsonTime.getLong("serviceDay");
                    final Date plannedTime = parseDate(serviceDay, scheduledDeparture);
                    final String stopId = jsonTime.getString("stopId");
                    StationDepartures stationDepartures = findOrCreateStationDepartures(result, stopId);
                    addLineDestination(stationDepartures, line, destination);
                    Date predictedTime = null;
                    if ( jsonTime.getBoolean("realtime") == true) {
                        final int realtimeArrival = jsonTime.getInt("realtimeArrival");
                        predictedTime = parseDate(serviceDay, realtimeArrival);
                    }
                    final Departure departure = new Departure(plannedTime, predictedTime, line, null, destination, null, null);
                    stationDepartures.departures.add(departure);
                }
            }
            return result;
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }


    /**
     * Converts OTP's mode and optional route type into the corresponding PTE product.
     * If the routeType is given, the conversion is uses this value,
     * otherwise the productsToMode mapping is used if unique.
     * As OTP's mode RAIL is to generic to deduce the correct product
     * (highspeed, regional or suburban train), this implementation returns regional train.
     * If not appropriate, subclasses would need to implement their own method.
     *
     * @param mode
     * @param routeType
     * @return
     */
    protected Product parseProduct(String mode, int routeType) {
        if (routeType == 707 || routeType == 715 || routeType == 1700 ) {
            // For now, we map Bürgerbus, AST and Mitfahren-BW's ridesharing to ON_DEMAND
            return Product.ON_DEMAND;
        } else if (routeType >= 100 && routeType <= 106) {
            return Product.HIGH_SPEED_TRAIN;
        } else if (routeType >= 106 && routeType <= 109) {
            return Product.REGIONAL_TRAIN;
        } else if (routeType >= 109 && routeType <= 112 ||
                routeType >= 300 && routeType <= 400 ) {
            return Product.SUBURBAN_TRAIN;
        } else if (routeType >= 112 && routeType <= 200) {
            // default to REGIONAL TRAIN(?)
            return Product.REGIONAL_TRAIN;
        } else if (routeType >= 200 && routeType <= 300) {
            return Product.BUS;
        } else if (routeType >= 400 && routeType <= 700) {
                return Product.SUBWAY;
        } else if (routeType >= 700 && routeType <= 900) {
            // Treat Bus Service and Trolley Bus Service as BUS
            return Product.BUS;
        } else if (routeType >= 900 && routeType <= 1000) {
            return Product.TRAM;
        } else if (routeType >= 1000 && routeType <= 1100 ||
                routeType >= 1200 && routeType <= 1300 ) {
            return Product.FERRY;
        } else if (routeType >= 1300 && routeType <= 1500) {
            // Mapping Funicular and Gondola (telecabin) to cablecar, as other providers do likewise
            // e.g. ZvvProvider
            return Product.CABLECAR;
        }

        Collection<Product> products = modeToProductMap.get(mode);
        if (products.size()==1) {
            return products.iterator().next();
        } else if (OTPMode.RAIL.name().equalsIgnoreCase(mode)) {
            // subclasses will need to infer proper type on their own...
            return Product.REGIONAL_TRAIN;
        } else {
            return null;
        }
    }

    protected static final Pattern P_EXTRACT_DEST_FROM_PATTERN = Pattern.compile(" to (.*) \\((.*)\\) from .*");

    private Location parseDestinationFromPatternDesc(String patternDesc) {
        Matcher m = P_EXTRACT_DEST_FROM_PATTERN.matcher(patternDesc);
        if (m.find()) {
            String id = m.group(2);
            String name = m.group(1);
            String[] placeAndName = splitStationName(name);
            return new Location(LocationType.STATION, id, placeAndName[0], placeAndName[1]);
        } else {
            return null;
        }
    }

    private void addLineDestination(StationDepartures stationDepartures, Line line, Location destination) {
        final LineDestination lineDestination = new LineDestination(line, destination);
        final List<LineDestination> lineDestinations = stationDepartures.lines;
        if (lineDestinations != null && !lineDestinations.contains(lineDestination))
            lineDestinations.add(lineDestination);
    }

    private Line findLineByPatternId(List<Line> lines, String patternId) {
        for (final Line line : lines) {
            if (patternId.startsWith(line.id))
                return line;
        }
        return null;
    }

    private StationDepartures findOrCreateStationDepartures(QueryDeparturesResult result, String stopId) {
        StationDepartures stationDepartures = result.findStationDepartures(stopId);
        if (stationDepartures == null) {
            Location location = new Location(LocationType.STATION, stopId);
            stationDepartures = new StationDepartures(location, new LinkedList<Departure>(),
                    new LinkedList<LineDestination>());
            result.stationDepartures.add(stationDepartures);
        }
        return stationDepartures;
    }

    private Date parseDate(long serviceDay, int secondsSinceMidnight) {
    	return new Date((serviceDay + secondsSinceMidnight)* 1000);
	}

    @Override
    public SuggestLocationsResult suggestLocations(CharSequence constraint) throws IOException {
        // Example: http://api.mfdz.de/otp/routers/default/geocode?corners=false&query=Weinsberg
        // Note: Returned matches are no exact matches, though they exist. Guess otp's geocoder endpoint needs some rework
        Builder suggestLocationsURL = buildSuggestLocationURL(constraint);
		final CharSequence page = request(suggestLocationsURL.build());
        return parseSuggestLocationResponse(page);
    }

    protected Builder buildSuggestLocationURL(CharSequence constraint) {
        final HttpUrl.Builder url = url().addPathSegment("geocode");
        url.addQueryParameter("query", constraint.toString());
        url.addQueryParameter("corners", "false");
        return  url;
	}

    protected SuggestLocationsResult parseSuggestLocationResponse(final CharSequence page) throws ParserException {
		try {
            final List<SuggestedLocation> locations = new ArrayList<>();

            final JSONArray body = new JSONArray(page.toString());

            final int nbResults = body.length();
            for (int i = 0; i < nbResults; ++i) {
                final JSONObject place = body.getJSONObject(i);
                final int priority = nbResults - i; // no "quality" so use sort order

                // Add location to station list.
                final Location location = parseSuggestedLocation(place);
                locations.add(new SuggestedLocation(location, priority));
            }
            
            final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);
            return new SuggestLocationsResult(resultHeader, locations);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
	}

    protected Location parseSuggestedLocation(JSONObject location) throws ParserException {
        try {
            Point point = Point.fromDouble(location.getDouble("lat"),location.getDouble("lng"));
            String description = location.getString("description");
			int firstSpaceIdx = description.indexOf(" ");
			String type = description.substring(0, firstSpaceIdx);
            String name = description.substring(firstSpaceIdx+1);

            Location loc = null;
            if (type.equals("stop")) {
            	String id = location.getString("id");
            	// first underscore needs to be replaced by colon to be comparable with index/stops
            	// e.g. nvbv_de:08212:32:1:1 becomes nvbv:de:08212:32:1:1
            	String normalizedId = id.replaceFirst("_", ":");
                String[] placeAndName = splitStationName(name);
            	loc = new Location(LocationType.STATION, normalizedId, point, placeAndName[0], placeAndName[1]);
            } else if (type.equals("corner")) {
                String[] placeAndName = splitAddress(name);
                loc = new Location(LocationType.ADDRESS, null, point,placeAndName[0], placeAndName[1]);
            } else {
                loc = new Location(LocationType.ANY, null, point, null, name);
            }
            
            return loc;	
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    protected String[] splitStationName(String name) {
        return new String[]{null, name};
    }

    protected String[] splitPOI(String poi) {
        return new String[]{null, poi};
    }

    protected String[] splitAddress(String address) {
        return new String[]{null, address};
    }

    @Override
    public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep, @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed, @Nullable Accessibility accessibility, @Nullable Set<Option> options) throws IOException {
        // Example: http://api.mfdz.de/otp/routers/default/plan?fromPlace=49.1468387814,9.2787565521&toPlace=49.14956614462,9.35345214869&date=20180721&time=14:37&showIntermediateStops=true&arriveBy=true
        // Note: we start with from, to, date, dep parameters. Others will be added stepby step
        final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);
		if(from != null && from.isIdentified() && to != null && to.isIdentified()) {
    		Builder queryTripsURL = buildQueryTrips(from, via, to, date, dep, products, walkSpeed, accessibility, options);
    		final CharSequence page = httpClient.get(queryTripsURL.build());
    		try {
    			final JSONObject head = new JSONObject(page.toString());
    			// TODO ?
    			String requestUri = "";
    			// TODO rethink if from/via/to of request or of response should be used
				List<Trip> trips = parseTrips(head, from, to);
				return new QueryTripsResult(resultHeader, requestUri, from, via, to, new Context(), trips );
    		} catch (final JSONException jsonExc) {
                throw new ParserException(jsonExc);
            }	 		
 		} 			
		return new QueryTripsResult(resultHeader, QueryTripsResult.Status.NO_TRIPS );
    }

    private List<Trip> parseTrips(JSONObject head, Location from, Location to) throws IOException {
    	ArrayList<Trip> trips = new ArrayList<>();	
    	try {
	    	JSONArray jsonTrips = head.getJSONObject("plan").getJSONArray("itineraries");
	    	if (jsonTrips != null) {
				for (int i = 0; i<jsonTrips.length();i++) {
					JSONObject jsonTrip = jsonTrips.getJSONObject(i);
					List<Leg> legs = parseLegs(jsonTrip);
					Integer numChanges = jsonTrip.getInt("transfers");
					// no support for fares yet
					Trip trip = new Trip(null, from, to, legs, null, null, numChanges);
					trips.add(trip);
				}	
	    	} else {
	    		throw new RuntimeException("No plan or itineraries available" );
	    	}
    	} catch (final JSONException jsonExc) {
    		throw new ParserException(jsonExc);
    	}    	
    	return trips;
	}

	protected ArrayList<Leg> parseLegs(JSONObject jsonTrip) throws IOException {
		try {
			JSONArray jsonLegs = jsonTrip.getJSONArray("legs");
            ArrayList<Leg> legs = new ArrayList<>();
            for (int i = 0; i<jsonLegs.length();i++) {
            	legs.add(parseLeg(jsonLegs.getJSONObject(i)));
			}
            return legs;
		} 	catch (final JSONException jsonExc) {
    		throw new ParserException(jsonExc);
		}
	}

	protected Leg parseLeg(JSONObject jsonLeg) throws JSONException, ParserException {
		JSONObject jsonFrom = jsonLeg.getJSONObject("from");
		JSONObject jsonTo = jsonLeg.getJSONObject("to");
		List<Point> path = PolylineDecoder.decode(jsonLeg.getJSONObject("legGeometry").getString("points"));
		boolean isPublicLeg = jsonLeg.getBoolean("transitLeg");
		Leg leg = null;

        if (isPublicLeg) {
		    Line line = parseLineFromLeg(jsonLeg);
		    Stop departureStop = parseStop(jsonFrom);
		    Stop arrivalStop = parseStop(jsonTo);
		    List<Stop> intermediateStops = parseIntermediateStops(jsonLeg);
		    String message = null;
		    leg = new Trip.Public(line, departureStop.location, departureStop, arrivalStop, intermediateStops, path, message);
		} else {
		    String mode = jsonLeg.getString("mode");
		    Type type = parseIndividualLegType(mode);
		    Location departure = parseLocationFromTo(jsonFrom);
		    Date departureTime = new Date(jsonFrom.getLong("departure"));
		    Location arrival = parseLocationFromTo(jsonTo);
		    Date arrivalTime = new Date(jsonTo.getLong("arrival"));
		    int distance = jsonLeg.getInt("distance");
		    leg = new Trip.Individual(type, departure, departureTime, arrival, arrivalTime, path, distance);
		}
		return leg;
	}

    protected Line parseLineFromLeg(JSONObject leg) throws JSONException {
        String id = leg.getString("routeId");
        String network = leg.getString("agencyName");
        String mode = leg.getString("mode");
        int routeType = leg.getInt ("routeType");
        Product product = parseProduct(mode, routeType);
        String label = leg.optString("routeShortName");
        String name = leg.optString("routeLongName");
        Style style = new Style(Style.parseColor("#"+leg.optString("routeColor", "ffffff")));

        return new Line(id, network, product, label, name, style);
    }

    protected List<Stop> parseIntermediateStops(JSONObject jsonLeg) throws JSONException, ParserException {
        ArrayList<Stop> stops = new ArrayList<>();
        JSONArray jsonIntermediateStops = jsonLeg.getJSONArray("intermediateStops");
        for (int i=0; i<jsonIntermediateStops.length();i++){
            stops.add(parseStop(jsonIntermediateStops.getJSONObject(i)));
        }
        return stops;
    }

    protected Stop parseStop(JSONObject jsonFromTo) throws ParserException, JSONException {
        Location location = parseLocationFromTo(jsonFromTo);
        Position position = parsePosition(jsonFromTo);
        Date arrivalTime = new Date(jsonFromTo.getLong("arrival"));
        Date departureTime = null;
        Position departurePosition = null;
        if (jsonFromTo.has("departure")) {
            departureTime = new Date(jsonFromTo.getLong("departure"));
            departurePosition = position;
        }
        return new Stop(location, arrivalTime, position, departureTime, position);
    }

    protected Position parsePosition(JSONObject jsonFromTo) {
        if (jsonFromTo.has("platformCode")) {
            return new Position(jsonFromTo.optString("platformCode"));
        } else {
            return null;
        }
    }

    protected Location parseLocationFromTo(JSONObject location) throws ParserException {
        try {
            Point point = Point.fromDouble(location.getDouble("lat"), location.getDouble("lon"));
            if (location.has("name")) {
				String name = location.getString("name");
				String vertexType = location.optString("vertexType");
				if ("TRANSIT".equals(vertexType)) {
					String id = location.getString("stopId");
                    String[] placeAndName = splitStationName(name);
                    return new Location(LocationType.STATION, id, point, placeAndName[0], placeAndName[1]);
				} else {
                    String[] placeAndName = splitAddress(name);
                    return new Location(LocationType.ANY, null, point, placeAndName[0], placeAndName[1]);
				}
			} else {
				return Location.coord(point);
			}
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    public String convertProductsToOTPModes(Set<Product> products, @Nullable Set<Option> options){
        defaultProductToOTPModeMapping();

        // PTE currently has no support for requesting different individual modes, so start by WALK
        StringBuilder modes = new StringBuilder();
        if (options != null && options.contains(Option.BIKE)) {
            // Note: OTP currently has no explicit param for bikeAllowed
            // However, it allows BICYCLE as individual mode.
            // (which should imply bike allowed(?))
            modes.append(OTPMode.BIKE.toString());
        } else {
            modes.append(OTPMode.WALK.toString());
        }
        if (Product.ALL.equals(products)) {
            modes.append(',');
            modes.append(OTPMode.TRANSIT);
        } else {
            for (Product product : products) {
                Collection<String> otpModes = productsToModeMap.get(product);
                for (String otpMode : otpModes) {
                    modes.append(',');
                    modes.append(otpMode);
                }
            }
        }
        return modes.toString();
    }

    protected Type parseIndividualLegType(String mode) {
		Type type = null;
		if ("WALK".equalsIgnoreCase(mode)) {
			type = Type.WALK;
		} else if ("BICYCLE".equalsIgnoreCase(mode)) {
			type = Type.BIKE;
		} else if ("CAR".equalsIgnoreCase(mode)) {
			type = Type.CAR;
		} else {
			throw new RuntimeException("No support yet for individual leg mode " + mode);
		}
		return type;
	}

	protected Builder buildQueryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep, Set<Product> products, @Nullable WalkSpeed walkSpeed, @Nullable Accessibility accessibility, @Nullable Set<Option> options) {
		final HttpUrl.Builder url = url().addPathSegment("plan");
		url.addQueryParameter("fromPlace", printLocation(from));
		if (via!=null) {
		    url.addQueryParameter("intermediatePlaces", printLocation(via));
        }
        url.addQueryParameter("toPlace", printLocation(to));
        url.addQueryParameter("date", new SimpleDateFormat("yyyyMMdd").format(date));
		url.addQueryParameter("time", new SimpleDateFormat("HHmmss").format(date));
		url.addQueryParameter("arriveBy", Boolean.toString(dep));
        url.addQueryParameter("showIntermediateStops", "true");
        url.addQueryParameter("locale", locale.toString());
        url.addQueryParameter("numItineraries", Integer.toString(numTripsRequested));
        if (walkSpeed != null) {
            double walkSpeedMeterPerSecond = walkSpeedInMeterPerSecond(walkSpeed);
            url.addQueryParameter("walkSpeed", Double.toString(walkSpeedMeterPerSecond));
        }
        if (Accessibility.BARRIER_FREE.equals(accessibility)) {
            url.addQueryParameter("wheelchair", "true");
        }
        if (products != null) {
            url.addQueryParameter("mode", convertProductsToOTPModes(products, options));
        }

        return url;
	}

    private double walkSpeedInMeterPerSecond(NetworkProvider.WalkSpeed walkSpeed) {
        double walkSpeedMeterPerSecond = 0;
        switch(walkSpeed) {
            case NORMAL: walkSpeedMeterPerSecond = 1.33; break;
            case FAST: walkSpeedMeterPerSecond = 1.77; break;
            case SLOW: walkSpeedMeterPerSecond = 1; break;
        }
        return walkSpeedMeterPerSecond;
    }

    private String printLocation(final Location location) {
        if (location.hasLocation())
            return location.getLatAsDouble() + "," + location.getLonAsDouble();
        else
            throw new IllegalArgumentException("Location has no coords");
    }

    @Override
    public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
        throw new RuntimeException("Not yet implemented");
    }

    /**
     * <code>PolylineDecoder</code> functionality is taken from the OpenTripPlanner project (licencesed under LGPL).
     * https://github.com/opentripplanner/OpenTripPlanner/blob/master/src/main/java/org/opentripplanner/util/PolylineEncoder.java
     */
    private static class PolylineDecoder {
        public static List<Point> decode(String encodedPolyline) {
            ArrayList<Point> points = new ArrayList<>();
            double lat = 0;
            double lon = 0;
            int strIndex = 0;
            while (strIndex < encodedPolyline.length()) {
                int[] rLat = decodeSignedNumberWithIndex(encodedPolyline, strIndex);
                lat = lat + rLat[0] * 1e-5;
                strIndex = rLat[1];
                int[] rLon = decodeSignedNumberWithIndex(encodedPolyline, strIndex);
                lon = lon + rLon[0] * 1e-5;
                strIndex = rLon[1];
                points.add(Point.fromDouble(lat, lon));
            }

            return points;
        }
    }

    private static int[] decodeSignedNumberWithIndex(String value, int index) {
        int[] r = decodeNumberWithIndex(value, index);
        int sgn_num = r[0];
        if ((sgn_num & 0x01) > 0) {
            sgn_num = ~(sgn_num);
        }
        r[0] = sgn_num >> 1;
        return r;
    }

    private static int[] decodeNumberWithIndex(String value, int index) {
        if (value.length() == 0)
            throw new IllegalArgumentException("string is empty");
        int num = 0;
        int v = 0;
        int shift = 0;
        do {
            v = value.charAt(index++) - 63;
            num |= (v & 0x1f) << shift;
            shift += 5;
        } while (v >= 0x20);
        return new int[] { num, index };
    }
}
