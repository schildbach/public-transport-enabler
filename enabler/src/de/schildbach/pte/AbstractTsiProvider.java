/*
 * Copyright 2014-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.exception.ParserException;

import okhttp3.HttpUrl;

/**
 * @author Kjell Braden <afflux@pentabarf.de>
 */
public abstract class AbstractTsiProvider extends AbstractNetworkProvider {
    private static class Context implements QueryTripsContext {
        private static final long serialVersionUID = -6847355540229473013L;

        public final Location from;
        public final Collection<Product> products;
        public final Location to;
        public final Location via;
        public final WalkSpeed walkSpeed;

        public Date earliestArrival;
        public Date latestDeparture;

        public Context(final Location from, final Location via, final Location to, final Collection<Product> products,
                final WalkSpeed walkSpeed, final Accessibility accessibility, final Set<Option> options) {
            this.from = from;
            this.via = via;
            this.to = to;
            this.products = products;
            this.walkSpeed = walkSpeed;
        }

        @Override
        public boolean canQueryEarlier() {
            return true;
        }

        @Override
        public boolean canQueryLater() {
            return true;
        }

        private QueryTripsResult queryMore(final AbstractTsiProvider provider, final boolean later) throws IOException {
            final Date date = later ? latestDeparture : earliestArrival;

            // add/remove a little bit just to be on the safe side
            final Calendar c = Calendar.getInstance(provider.timeZone());
            c.setTime(date);
            c.add(Calendar.MINUTE, later ? 1 : -1);

            return provider.queryTripsFromContext(this, c.getTime(), later);
        }

        private void updateEarliestArrival(final Date newTime) {
            if (newTime == null)
                return;
            if (earliestArrival == null || newTime.before(earliestArrival))
                earliestArrival = newTime;
        }

        private void updateLatestDeparture(final Date newTime) {
            if (newTime == null)
                return;
            if (latestDeparture == null || newTime.after(latestDeparture))
                latestDeparture = newTime;
        }
    }

    private static final String DEFAULT_STOPFINDER_ENDPOINT = "Transport/v2";
    private static final String DEFAULT_TRIP_ENDPOINT = "journeyplanner/v2";
    private static final String SERVER_PRODUCT = "tsi";

    private static Map<String, Product> TRANSPORT_MODES = new HashMap<>();
    static {
        // HIGH_SPEED_TRAIN
        TRANSPORT_MODES.put("TGV", Product.HIGH_SPEED_TRAIN);
        TRANSPORT_MODES.put("HST", Product.HIGH_SPEED_TRAIN);

        // REGIONAL_TRAIN
        TRANSPORT_MODES.put("TRAIN", Product.REGIONAL_TRAIN);
        TRANSPORT_MODES.put("TER", Product.REGIONAL_TRAIN);

        // SUBURBAN_TRAIN
        TRANSPORT_MODES.put("LOCAL_TRAIN", Product.SUBURBAN_TRAIN);

        // SUBWAY
        TRANSPORT_MODES.put("METRO", Product.SUBWAY);

        // TRAM
        TRANSPORT_MODES.put("TRAM", Product.TRAM);
        TRANSPORT_MODES.put("TRAMWAY", Product.TRAM);

        // BUS
        TRANSPORT_MODES.put("BUS", Product.BUS);
        TRANSPORT_MODES.put("COACH", Product.BUS);

        // CABLECAR
        TRANSPORT_MODES.put("TROLLEY", Product.CABLECAR);
        TRANSPORT_MODES.put("TROLLEY_BUS", Product.CABLECAR);
    }

    protected static double latLonToDouble(final int value) {
        return (double) value / 1000000;
    }

    private final @Nullable String apiKey;
    private final HttpUrl stopFinderEndpoint;
    private final HttpUrl tripEndpoint;

    public AbstractTsiProvider(final NetworkId network, final String apiKey, final HttpUrl apiBase) {
        this(network, apiKey, apiBase, null, null);
    }

    public AbstractTsiProvider(final NetworkId network, final String apiKey, final HttpUrl tripEndpoint,
            final HttpUrl stopFinderEndpoint) {
        super(network);

        this.apiKey = apiKey;
        this.tripEndpoint = tripEndpoint;
        this.stopFinderEndpoint = stopFinderEndpoint;
    }

    public AbstractTsiProvider(final NetworkId network, final String apiKey, final HttpUrl apiBase,
            final String tripEndpoint, final String stopFinderEndpoint) {
        this(network, apiKey,
                apiBase.newBuilder().addPathSegments(tripEndpoint != null ? tripEndpoint : DEFAULT_TRIP_ENDPOINT)
                        .build(),
                apiBase.newBuilder()
                        .addPathSegments(stopFinderEndpoint != null ? stopFinderEndpoint : DEFAULT_STOPFINDER_ENDPOINT)
                        .build());
    }

    @Override
    protected boolean hasCapability(final Capability capability) {
        if (capability == Capability.DEPARTURES)
            return false;
        else
            return true;
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException {
        final HttpUrl.Builder url = stopFinderEndpoint.newBuilder().addPathSegments("SearchTripPoint/json");
        appendCommonRequestParams(url);
        url.addQueryParameter("MaxItems", "50"); // XXX good value?
        url.addQueryParameter("Keywords", constraint.toString());

        final CharSequence page = httpClient.get(url.build());
        try {
            final List<SuggestedLocation> locations = new ArrayList<>();
            final JSONObject head = new JSONObject(page.toString());

            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

            final int status = head.getInt("StatusCode");

            if (status != 200)
                return new SuggestLocationsResult(header, SuggestLocationsResult.Status.SERVICE_DOWN);

            JSONArray dataArray = head.getJSONArray("Data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject data = dataArray.getJSONObject(i);
                final Location location = parseJsonTransportLocation(data);

                if (location.isIdentified()) // make sure the location is really identified
                    // some addresses may not contain coordinates, we ignore them
                    locations.add(new SuggestedLocation(location));
            }

            return new SuggestLocationsResult(header, locations);
        } catch (final JSONException x) {
            throw new ParserException(x);
        }
    }

    private final void appendCommonRequestParams(final HttpUrl.Builder url) {
        url.addQueryParameter("Key", apiKey);
    }

    private Line createLine(final String id, final String mode, final String number, final String name,
            final String operatorCode, final String codeActivity) {
        final Product product = TRANSPORT_MODES.get(mode);

        if (product == null)
            throw new IllegalStateException("cannot normalize mode '" + mode + "' number '" + number + "'");

        final StringBuilder label = new StringBuilder();

        if (number != null) {
            label.append(number);
        } else if (operatorCode != null) {
            label.append(operatorCode);
            if (codeActivity != null)
                label.append(codeActivity);
        } else {
            label.append(name);
        }

        return new Line(id, operatorCode, product, label.toString());
    }

    private List<Location> identifyLocation(final Location location) throws IOException {
        if (location.isIdentified())
            return Collections.singletonList(location);

        final List<Location> locations = suggestLocations(location.uniqueShortName()).getLocations();
        if (locations == null)
            return new ArrayList<>(0);

        return locations;
    }

    private NearbyLocationsResult jsonCoordRequest(final int lat, final int lon, final int maxDistance,
            final int maxStations) throws IOException {
        final HttpUrl.Builder url = stopFinderEndpoint.newBuilder().addPathSegments("SearchTripPoint/json");
        appendCommonRequestParams(url);
        url.addQueryParameter("Latitude", String.format(Locale.FRENCH, "%2.6f", latLonToDouble(lat)));
        url.addQueryParameter("Longitude", String.format(Locale.FRENCH, "%2.6f", latLonToDouble(lon)));
        url.addQueryParameter("MaxItems", Integer.toString(maxStations != 0 ? maxStations : 50));
        url.addQueryParameter("Perimeter", Integer.toString(maxDistance != 0 ? maxDistance : 1320));
        url.addQueryParameter("PointTypes", "Stop_Place");

        final CharSequence page = httpClient.get(url.build());
        try {
            final List<Location> stations = new ArrayList<>();
            final JSONObject head = new JSONObject(page.toString());

            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

            final int status = head.getInt("StatusCode");

            if (status != 200) {
                return new NearbyLocationsResult(header, status == 300 ? NearbyLocationsResult.Status.INVALID_ID
                        : NearbyLocationsResult.Status.SERVICE_DOWN);
            }

            JSONArray dataArray = head.getJSONArray("Data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject data = dataArray.getJSONObject(i);
                stations.add(parseJsonTransportLocation(data));
            }

            return new NearbyLocationsResult(header, stations);
        } catch (final JSONException x) {
            throw new ParserException(x);
        }
    }

    private String jsonOptString(final JSONObject json, final String key) throws JSONException {
        return json.isNull(key) ? null : json.getString(key);
    }

    private Location jsonStationRequestCoord(final String id) throws IOException {
        final HttpUrl.Builder url = stopFinderEndpoint.newBuilder().addPathSegments("GetTripPoint/json");
        appendCommonRequestParams(url);
        url.addQueryParameter("TripPointId", id);
        url.addQueryParameter("PointType", "Stop_Place");

        final CharSequence page = httpClient.get(url.build());
        try {
            final JSONObject head = new JSONObject(page.toString());

            int status = head.getInt("StatusCode");

            if (status != 200)
                return null;

            JSONObject data = head.getJSONObject("Data");
            return parseJsonTransportLocation(data);
        } catch (final JSONException x) {
            throw new ParserException(x);
        }
    }

    private Trip.Individual parseJsonJourneyplannerIndividualLeg(final JSONObject legInfo) throws JSONException {
        final String transportMode = legInfo.getString("TransportMode");
        final Trip.Individual.Type type;
        if ("WALK".equals(transportMode)) {
            type = Trip.Individual.Type.WALK;
        } else {
            throw new JSONException("unknown transportMode=" + transportMode);
        }

        final JSONObject arrInfo = legInfo.getJSONObject("Arrival");
        final JSONObject depInfo = legInfo.getJSONObject("Departure");

        final Location departure = parseJsonJourneyplannerLocation(depInfo.getJSONObject("Site"));
        final Location arrival = parseJsonJourneyplannerLocation(arrInfo.getJSONObject("Site"));

        final String arrTimeStr = arrInfo.getString("Time");
        final String depTimeStr = depInfo.getString("Time");
        final Date depTime, arrTime;
        try {
            final DateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
            depTime = fullDateFormat.parse(depTimeStr);
            arrTime = fullDateFormat.parse(arrTimeStr);
        } catch (final ParseException e) {
            throw new JSONException("failed to parse trip times: " + depTimeStr + " or " + arrTimeStr);
        }

        final JSONArray pathLinks = legInfo.getJSONObject("pathLinks").getJSONArray("PathLink");
        final List<Point> path = new ArrayList<>(pathLinks.length() + 1);

        int distance = 0;
        path.add(new Point(departure.lat, departure.lon));
        for (int i = 0; i < pathLinks.length(); i++) {
            final JSONObject pathLink = pathLinks.getJSONObject(i);
            distance += pathLink.getInt("Distance");

            final Location toLoc = parseJsonJourneyplannerLocation(
                    pathLink.getJSONObject("Arrival").getJSONObject("Site"));
            path.add(new Point(toLoc.lat, toLoc.lon));
        }
        return new Trip.Individual(type, departure, depTime, arrival, arrTime, path, distance);
    }

    private Trip.Leg parseJsonJourneyplannerLeg(final JSONObject jsonObject) throws JSONException {
        final JSONObject legInfo = jsonObject.getJSONObject("Leg");
        final JSONObject ptrInfo = jsonObject.getJSONObject("PTRide");

        if (legInfo.isNull("TransportMode") && !ptrInfo.isNull("TransportMode")) {
            return parseJsonJourneyplannerPublicLeg(ptrInfo);
        } else if (!legInfo.isNull("TransportMode") && ptrInfo.isNull("TransportMode")) {
            return parseJsonJourneyplannerIndividualLeg(legInfo);
        } else {
            throw new JSONException("unknown leg type");
        }
    }

    private Location parseJsonJourneyplannerLocation(final JSONObject data) throws JSONException {
        final String locTypeStr = data.getString("Type");
        final LocationType locType;
        final String id;

        if ("POI".equals(locTypeStr)) {
            locType = LocationType.POI;
            id = data.getString("id");
        } else if ("BOARDING_POSITION".equals(locTypeStr)) {
            locType = LocationType.STATION;
            if (!data.isNull("LogicalId"))
                id = data.getString("LogicalId");
            else
                id = data.getString("id");
        } else {
            id = data.optString("id");
            locType = LocationType.ADDRESS;
        }

        final Point coord;
        final JSONObject posObj = data.optJSONObject("Position");
        if (posObj != null) {
            final double lat = posObj.getDouble("Lat");
            final double lon = posObj.getDouble("Long");
            coord = Point.fromDouble(lat, lon);
        } else {
            coord = null;
        }

        final String name = data.getString("Name");
        final String place = jsonOptString(data, "CityName");
        return new Location(locType, id, coord, place, name);
    }

    private Trip.Public parseJsonJourneyplannerPublicLeg(final JSONObject ptrInfo) throws JSONException {
        final DateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
        final Line line = parseJsonLine(ptrInfo);

        final JSONObject destObj = ptrInfo.optJSONObject("Direction");
        String destinationName = jsonOptString(ptrInfo, "Destination");
        if (destinationName == null && destObj != null)
            destinationName = destObj.optString("Name");
        final Location lineDestination = new Location(LocationType.ANY, null, null, destinationName);

        final Stop departureStop, arrivalStop;

        final JSONObject departureInfo = ptrInfo.getJSONObject("Departure");
        final JSONObject arrivalInfo = ptrInfo.getJSONObject("Arrival");
        final Location departureLocation = parseJsonJourneyplannerLocation(departureInfo.getJSONObject("StopPlace"));
        final Location arrivalLocation = parseJsonJourneyplannerLocation(arrivalInfo.getJSONObject("StopPlace"));
        try {
            final Date departureTime = fullDateFormat.parse(departureInfo.getString("Time"));
            final Date arrivalTime = fullDateFormat.parse(arrivalInfo.getString("Time"));
            departureStop = new Stop(departureLocation, true, departureTime, null, null, null);
            arrivalStop = new Stop(arrivalLocation, false, arrivalTime, null, null, null);
        } catch (final ParseException e) {
            throw new JSONException(e);
        }

        final JSONArray stepArray = ptrInfo.getJSONObject("steps").getJSONArray("Step");
        List<Stop> intermediateStops = new ArrayList<>(stepArray.length() - 1);
        for (int i = 0; i < stepArray.length() - 1; i++) {
            final JSONObject enterStop = stepArray.getJSONObject(i).getJSONObject("Arrival");
            final JSONObject leaveStop = stepArray.getJSONObject(i + 1).getJSONObject("Departure");

            final Location location = parseJsonJourneyplannerLocation(leaveStop.getJSONObject("StopPlace"));
            Date enterTime;
            Date leaveTime;
            try {
                enterTime = fullDateFormat.parse(enterStop.getString("Time"));
                leaveTime = fullDateFormat.parse(leaveStop.getString("Time"));
                intermediateStops.add(new Stop(location, enterTime, null, leaveTime, null));
            } catch (final ParseException e) {
                throw new JSONException(e);
            }
        }

        final String message = jsonOptString(ptrInfo, "Notes");

        return new Trip.Public(line, lineDestination, departureStop, arrivalStop, intermediateStops, null, message);
    }

    private Trip parseJsonJourneyplannerTrip(final JSONObject tripObject) throws JSONException {
        final JSONObject departureInfo = tripObject.getJSONObject("Departure");
        final JSONObject arrivalInfo = tripObject.getJSONObject("Arrival");
        final Location from = parseJsonJourneyplannerLocation(departureInfo.getJSONObject("Site"));
        final Location to = parseJsonJourneyplannerLocation(arrivalInfo.getJSONObject("Site"));

        final JSONArray legArray = tripObject.getJSONObject("sections").getJSONArray("Section");
        final List<Trip.Leg> legs = new ArrayList<>(legArray.length());

        for (int i = 0; i < legArray.length(); i++) {
            legs.add(parseJsonJourneyplannerLeg(legArray.getJSONObject(i)));
        }

        return new Trip(null, from, to, legs, null, null, null);
    }

    private Line parseJsonLine(final JSONObject ptrInfo) throws JSONException {
        final JSONObject networkInfo = ptrInfo.optJSONObject("PTNetwork");
        final String network;
        if (networkInfo != null)
            network = jsonOptString(networkInfo, "Name");
        else
            network = null;

        final JSONObject lineInfo = ptrInfo.getJSONObject("Line");

        final String id = lineInfo.getString("id");

        final String transportMode = ptrInfo.getString("TransportMode");

        final String lineNumber = lineInfo.optString("Number");
        final String lineName = lineInfo.getString("Name");

        final JSONObject operatorInfo = ptrInfo.optJSONObject("Operator");
        final String operatorCode;
        if (operatorInfo != null)
            operatorCode = jsonOptString(operatorInfo, "Code");
        else
            operatorCode = null;

        final String codeActivity = jsonOptString(ptrInfo, "CodeActivity");

        final Line line = createLine(id, transportMode, lineNumber, lineName, operatorCode, codeActivity);
        final Line styledLine = new Line(line.id, line.network, line.product, line.label,
                lineStyle(network, line.product, line.label));
        return styledLine;
    }

    private Location parseJsonTransportLocation(final JSONObject data) throws JSONException {
        final String id = data.getString("Id");
        final LocationType locType;

        switch (data.getInt("PointType")) {
        case 1:
            locType = LocationType.POI;
            break;
        case 4:
            locType = LocationType.STATION;
            break;
        case 3:
        default:
            locType = LocationType.ADDRESS;
        }

        final double lat = data.optDouble("Latitude", 0);
        final double lon = data.optDouble("Longitude", 0);

        final String name = data.getString("Name");
        String place = null;
        final JSONObject localityObj = data.optJSONObject("Locality");
        if (localityObj != null) {
            place = localityObj.getString("Name");
        }
        return new Location(locType, id, Point.fromDouble(lat, lon), place, name);
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException {
        return ((Context) context).queryMore(this, later);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        Location queryLocation = location;
        if (!queryLocation.hasLocation()) {
            if (location.type != LocationType.STATION)
                throw new IllegalArgumentException("cannot handle: " + location.type);
            queryLocation = jsonStationRequestCoord(location.id);
        }

        if (queryLocation == null)
            throw new IllegalArgumentException("null location or station not found");

        return jsonCoordRequest(location.lat, location.lon, maxDistance, maxLocations);
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable Set<Product> products,
            final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
            final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException {
        final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);
        final List<Location> possibleFroms, possibleTos, possibleVias;

        possibleFroms = identifyLocation(from);
        possibleTos = identifyLocation(to);

        if (via != null)
            possibleVias = identifyLocation(via);
        else
            possibleVias = Collections.singletonList(null);

        if (possibleFroms.isEmpty())
            return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
        if (possibleTos.isEmpty())
            return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
        if (possibleVias.isEmpty())
            return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_VIA);

        if (possibleFroms.size() > 1 || possibleVias.size() > 1 || possibleTos.size() > 1)
            return new QueryTripsResult(header, possibleFroms.size() > 1 ? possibleFroms : null,
                    possibleVias.size() > 1 ? possibleVias : null, possibleTos.size() > 1 ? possibleTos : null);

        final Context context = new Context(possibleFroms.get(0), possibleVias.get(0), possibleTos.get(0), products,
                walkSpeed, accessibility, options);

        return queryTripsFromContext(context, date, dep);
    }

    protected QueryTripsResult queryTripsFromContext(final Context context, final Date date, final boolean dep)
            throws IOException {
        final String mode;
        if (context.products != null) {
            final StringBuilder modeBuilder = new StringBuilder();
            for (Product p : context.products) {
                final String productName = translateToLocalProduct(p);
                if (productName != null)
                    modeBuilder.append(productName).append("|");
            }
            mode = modeBuilder.substring(0, modeBuilder.length() - 1);
        } else {
            mode = null;
        }

        final HttpUrl.Builder url = tripEndpoint.newBuilder().addPathSegments("PlanTrip/json");
        appendCommonRequestParams(url);
        url.addQueryParameter("Disruptions", "0"); // XXX what does this even mean?
        url.addQueryParameter("Algorithm", "FASTEST");
        url.addQueryParameter("MaxWalkDist", "1000"); // XXX good value? (in meters)

        if (context.from.type == LocationType.STATION) {
            url.addQueryParameter("DepType", "STOP_PLACE");
            url.addQueryParameter("DepId", context.from.id + "$0");
        } else if (context.from.type == LocationType.POI) {
            url.addQueryParameter("DepType", "POI");
            url.addQueryParameter("DepId", context.from.id + "$0");
        } else {
            url.addQueryParameter("DepLat", Double.toString(latLonToDouble(context.from.lat)));
            url.addQueryParameter("DepLon", Double.toString(latLonToDouble(context.from.lon)));
        }

        if (context.to.type == LocationType.STATION) {
            url.addQueryParameter("ArrType", "STOP_PLACE");
            url.addQueryParameter("ArrId", context.to.id + "$0");
        } else if (context.to.type == LocationType.POI) {
            url.addQueryParameter("ArrType", "POI");
            url.addQueryParameter("ArrId", context.to.id + "$0");
        } else {
            url.addQueryParameter("ArrLat", Double.toString(latLonToDouble(context.to.lat)));
            url.addQueryParameter("ArrLon", Double.toString(latLonToDouble(context.to.lon)));
        }

        if (context.via != null) {
            if (context.via.type == LocationType.STATION) {
                url.addQueryParameter("ViaType", "STOP_PLACE");
                url.addQueryParameter("ViaId", context.via.id + "$0");
            } else if (context.via.type == LocationType.POI) {
                url.addQueryParameter("ViaType", "POI");
                url.addQueryParameter("ViaId", context.via.id + "$0");
            } else {
                url.addQueryParameter("ViaLat", Double.toString(latLonToDouble(context.via.lat)));
                url.addQueryParameter("ViaLon", Double.toString(latLonToDouble(context.via.lon)));
            }
        }

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        url.addQueryParameter("date", dateFormat.format(date));

        final DateFormat timeFormat = new SimpleDateFormat("HH-mm", Locale.US);
        url.addQueryParameter(dep ? "DepartureTime" : "ArrivalTime", timeFormat.format(date));

        url.addQueryParameter("WalkSpeed", translateWalkSpeed(context.walkSpeed));

        if (mode != null)
            url.addQueryParameter("Modes", mode);

        final CharSequence page = httpClient.get(url.build());
        try {
            final JSONObject head = new JSONObject(page.toString());

            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

            final JSONObject statusObj = head.optJSONObject("Status");

            if (statusObj == null) {
                return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
            }

            final String statusStr = statusObj.optString("Code");

            if ("NO_SOLUTION_FOR_REQUEST".equals(statusStr)) {
                return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
            }

            if (!"OK".equals(statusStr)) {
                return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
            }

            final JSONArray tripArray = head.getJSONObject("trips").getJSONArray("Trip");
            final List<Trip> trips = new ArrayList<>(tripArray.length());

            for (int i = 0; i < tripArray.length(); i++) {
                final JSONObject tripObject = tripArray.getJSONObject(i);
                trips.add(parseJsonJourneyplannerTrip(tripObject));
            }

            if (trips.size() > 0) {
                context.updateEarliestArrival(trips.get(0).getLastArrivalTime());
                context.updateLatestDeparture(trips.get(trips.size() - 1).getFirstDepartureTime());
            }

            return new QueryTripsResult(header, url.build().toString(), context.from, context.via, context.to, context,
                    trips);
        } catch (final JSONException x) {
            throw new ParserException(x);
        }
    }

    protected TimeZone timeZone() {
        return TimeZone.getTimeZone("Europe/Paris");
    }

    protected abstract String translateToLocalProduct(final Product p);

    /**
     * @param walkSpeed
     * @return walk speed in km/h
     */
    protected String translateWalkSpeed(final WalkSpeed walkSpeed) {
        switch (walkSpeed) {
        case FAST:
            return "6";
        case SLOW:
            return "4";
        case NORMAL:
        default:
            return "5";
        }
    }
}
