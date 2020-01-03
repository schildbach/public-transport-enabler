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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Strings;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.NearbyLocationsResult.Status;
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
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Individual;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.Trip.Public;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.ParserException;

import okhttp3.HttpUrl;

/**
 * @author Antonio El Khoury
 * @author Andreas Schildbach
 * @author Torsten Grote
 */
public abstract class AbstractNavitiaProvider extends AbstractNetworkProvider {
    protected final static String SERVER_PRODUCT = "navitia";
    protected final static String SERVER_VERSION = "v1";

    protected HttpUrl apiBase = HttpUrl.parse("https://api.navitia.io/").newBuilder().addPathSegment(SERVER_VERSION)
            .build();

    private enum PlaceType {
        ADDRESS, ADMINISTRATIVE_REGION, POI, STOP_POINT, STOP_AREA
    }

    private enum SectionType {
        CROW_FLY, PUBLIC_TRANSPORT, STREET_NETWORK, TRANSFER, WAITING, STAY_IN, ON_DEMAND_TRANSPORT, BSS_RENT, BSS_PUT_BACK, BOARDING, LANDING
    }

    private enum TransferType {
        BIKE, WALKING
    }

    private enum PhysicalMode {
        AIR, BOAT, BUS, BUSRAPIDTRANSIT, COACH, FERRY, FUNICULAR, LOCALTRAIN, LONGDISTANCETRAIN, METRO, RAPIDTRANSIT, SHUTTLE, TAXI, TRAIN, TRAMWAY, TRAM, VAL, RAILSHUTTLE, OTHER
    }

    @SuppressWarnings("serial")
    private static class Context implements QueryTripsContext {
        private final Location from;
        private final Location to;
        private final String prevQueryUri;
        private final String nextQueryUri;

        private Context(final Location from, final Location to, final String prevQueryUri, final String nextQueryUri) {
            this.from = from;
            this.to = to;
            this.prevQueryUri = prevQueryUri;
            this.nextQueryUri = nextQueryUri;
        }

        @Override
        public boolean canQueryLater() {
            return (from != null && to != null && nextQueryUri != null);
        }

        @Override
        public boolean canQueryEarlier() {
            return (from != null && to != null && prevQueryUri != null);
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + from + "|" + to + "|" + prevQueryUri + "|" + nextQueryUri + "]";
        }
    }

    public AbstractNavitiaProvider(final NetworkId network, final HttpUrl apiBase, final String authorization) {
        this(network, authorization);

        this.apiBase = apiBase;
    }

    public AbstractNavitiaProvider(final NetworkId network, final String authorization) {
        super(network);

        if (authorization != null)
            httpClient.setHeader("Authorization", authorization);
    }

    protected abstract String region();

    protected int computeForegroundColor(final String lineColor) {
        int bgColor = Style.parseColor(lineColor);
        return Style.deriveForegroundColor(bgColor);
    }

    protected Style getLineStyle(final String network, final Product product, final String code, final String color) {
        return getLineStyle(network, product, code, color, null);
    }

    protected Style getLineStyle(final String network, final Product product, final String code,
            final String backgroundColor, final String foregroundColor) {
        if (backgroundColor != null) {
            if (foregroundColor == null)
                return new Style(Shape.RECT, Style.parseColor(backgroundColor),
                        computeForegroundColor(backgroundColor));
            return new Style(Shape.RECT, Style.parseColor(backgroundColor), Style.parseColor(foregroundColor));
        } else {
            final Style defaultStyle = Standard.STYLES.get(product);
            return new Style(Shape.RECT, defaultStyle.backgroundColor, defaultStyle.backgroundColor2,
                    defaultStyle.foregroundColor, defaultStyle.borderColor);
        }
    }

    private HttpUrl.Builder url() {
        return apiBase.newBuilder().addPathSegment("coverage").addPathSegment(region());
    }

    private Point parseCoord(final JSONObject coord) throws IOException {
        try {
            final double lat = coord.getDouble("lat");
            final double lon = coord.getDouble("lon");
            return Point.fromDouble(lat, lon);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private LocationType getLocationType(PlaceType placeType) {
        switch (placeType) {
        case STOP_POINT: {
            return LocationType.STATION;
        }
        case STOP_AREA: {
            return LocationType.STATION;
        }
        case ADDRESS: {
            return LocationType.ADDRESS;
        }
        case POI: {
            return LocationType.POI;
        }
        default:
            throw new IllegalArgumentException("Unhandled place type: " + placeType);
        }
    }

    /**
     * Some Navitia providers return location names with wrong case. This method can be used to fix the name
     * when locations are parsed.
     * 
     * @param name
     *            The name of the location
     * @return the fixed name of the location
     */
    protected String getLocationName(String name) {
        return name;
    }

    /**
     * Navitia always formats the address label in the French way.
     * This method can be used to display addresses in a localized way.
     * It defaults to putting the house number behind the street name.
     *
     * @param name The name of the address. Usually a street name.
     * @param houseNumber The house number of the address.
     * @return the localized name of the address location
     */
    protected String getAddressName(final String name, final String houseNumber) {
        return name + " " + houseNumber;
    }

    private Location parsePlace(JSONObject location, PlaceType placeType) throws IOException {
        try {
            final LocationType type = getLocationType(placeType);
            String id = null;
            if (placeType != PlaceType.ADDRESS && placeType != PlaceType.POI)
                id = location.getString("id");
            final JSONObject coord = location.getJSONObject("coord");
            final Point point = parseCoord(coord);
            final String placeName = location.getString("name");
            String name;
            if (placeType == PlaceType.ADDRESS) {
                final String houseNumber = location.optString("house_number", "0");
                name = houseNumber.equals("0") ? placeName : getAddressName(placeName, houseNumber);
            } else {
                name = getLocationName(placeName);
            }
            String place = null;
            if (location.has("administrative_regions")) {
                JSONArray admin = location.getJSONArray("administrative_regions");
                if (admin.length() > 0)
                    place = Strings.emptyToNull(admin.getJSONObject(0).optString("name"));
            }
            Set<Product> products = null;
            if (location.has("stop_area") && location.getJSONObject("stop_area").has("physical_modes")) {
                products = EnumSet.noneOf(Product.class);
                JSONArray physicalModes = location.getJSONObject("stop_area").getJSONArray("physical_modes");
                for (int i = 0; i < physicalModes.length(); i++) {
                    JSONObject mode = physicalModes.getJSONObject(i);
                    Product product = parseLineProductFromMode(mode.getString("id"));
                    if (product != null)
                        products.add(product);
                }
            }
            return new Location(type, id, point, place, name, products);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Location parseAdministrativeRegion(final JSONObject j) throws IOException {
        try {
            final JSONObject coord = j.getJSONObject("coord");
            final Point point = parseCoord(coord);
            final String name = j.getString("name");
            return new Location(LocationType.POI, null, point, null, name);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Location parseLocation(final JSONObject j) throws IOException {
        try {
            final String type = j.getString("embedded_type");
            final PlaceType placeType = PlaceType.valueOf(type.toUpperCase());
            JSONObject location;

            switch (placeType) {
            case STOP_POINT: {
                location = j.getJSONObject("stop_point");
                break;
            }
            case STOP_AREA: {
                location = j.getJSONObject("stop_area");
                break;
            }
            case ADDRESS: {
                location = j.getJSONObject("address");
                break;
            }
            case POI: {
                location = j.getJSONObject("poi");
                break;
            }
            case ADMINISTRATIVE_REGION: {
                return parseAdministrativeRegion(j.getJSONObject("administrative_region"));
            }
            default:
                throw new IllegalArgumentException("Unhandled place type: " + type);
            }
            return parsePlace(location, placeType);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private String printLocation(final Location location) {
        if (location.hasId())
            return location.id;
        else if (location.hasCoord())
            return location.getLonAsDouble() + ";" + location.getLatAsDouble();
        else
            return "";
    }

    private Date parseDate(final String dateString) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        format.setTimeZone(timeZone);
        return format.parse(dateString);
    }

    private String printDate(final Date date) {
        DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        format.setTimeZone(timeZone);
        return format.format(date);
    }

    private LinkedList<Point> parsePath(final JSONArray coordinates) throws IOException {
        LinkedList<Point> path = new LinkedList<>();

        for (int i = 0; i < coordinates.length(); ++i) {
            try {
                final JSONArray jsonPoint = coordinates.getJSONArray(i);
                final double lon = jsonPoint.getDouble(0);
                final double lat = jsonPoint.getDouble(1);
                final Point point = Point.fromDouble(lat, lon);
                path.add(point);
            } catch (final JSONException jsonExc) {
                throw new ParserException(jsonExc);
            }
        }

        return path;
    }

    private static class LegInfo {
        public final Location departure;
        public final Date departureTime;
        public final Location arrival;
        public final Date arrivalTime;
        public final List<Point> path;
        public final int distance;
        public final int min;

        public LegInfo(final Location departure, final Date departureTime, final Location arrival,
                final Date arrivalTime, final List<Point> path, final int distance, final int min) {
            this.departure = departure;
            this.departureTime = departureTime;
            this.arrival = arrival;
            this.arrivalTime = arrivalTime;
            this.path = path;
            this.distance = distance;
            this.min = min;
        }
    }

    private LegInfo parseLegInfo(final JSONObject section) throws IOException {
        try {
            final String type = section.getString("type");

            if (!type.equals("waiting")) {
                // Build departure location.
                final JSONObject sectionFrom = section.getJSONObject("from");
                final Location departure = parseLocation(sectionFrom);

                // Build departure time.
                final String departureDateTime = section.getString("departure_date_time");
                final Date departureTime = parseDate(departureDateTime);

                // Build arrival location.
                final JSONObject sectionTo = section.getJSONObject("to");
                final Location arrival = parseLocation(sectionTo);

                // Build arrival time.
                final String arrivalDateTime = section.getString("arrival_date_time");
                final Date arrivalTime = parseDate(arrivalDateTime);

                // Build path and distance. Check first that geojson
                // object exists.
                LinkedList<Point> path = null;
                int distance = 0;
                if (section.has("geojson")) {
                    final JSONObject jsonPath = section.getJSONObject("geojson");
                    final JSONArray coordinates = jsonPath.getJSONArray("coordinates");
                    path = parsePath(coordinates);

                    final JSONArray properties = jsonPath.getJSONArray("properties");
                    for (int i = 0; i < properties.length(); ++i) {
                        final JSONObject property = properties.getJSONObject(i);
                        if (property.has("length")) {
                            distance = property.getInt("length");
                            break;
                        }
                    }
                }

                // Build duration in min.
                final int min = section.getInt("duration") / 60;

                return new LegInfo(departure, departureTime, arrival, arrivalTime, path, distance, min);
            } else {
                return null;
            }
        } catch (final JSONException | ParseException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Line parseLineFromSection(final JSONObject section, final SectionType type) throws IOException {
        try {
            final JSONArray links = section.getJSONArray("links");
            String lineId = null;
            String modeId = null;
            for (int i = 0; i < links.length(); ++i) {
                final JSONObject link = links.getJSONObject(i);
                final String linkType = link.getString("type");
                if (linkType.equals("line"))
                    lineId = link.getString("id");
                else if (linkType.equals("physical_mode"))
                    modeId = link.getString("id");
            }

            final Product product = type == SectionType.ON_DEMAND_TRANSPORT ? Product.ON_DEMAND
                    : parseLineProductFromMode(modeId);
            final JSONObject displayInfo = section.getJSONObject("display_informations");
            final String network = Strings.emptyToNull(displayInfo.optString("network"));
            final String code = displayInfo.getString("code");
            final String color = Strings.emptyToNull(displayInfo.getString("color"));
            final String name = Strings.emptyToNull(displayInfo.optString("headsign"));
            final Style lineStyle = getLineStyle(network, product, code, color != null ? "#" + color : null);

            return new Line(lineId, network, product, code, name, lineStyle);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Stop parseStop(final JSONObject stopDateTime) throws IOException {
        try {
            // Build location.
            final JSONObject stopPoint = stopDateTime.getJSONObject("stop_point");
            final Location location = parsePlace(stopPoint, PlaceType.STOP_POINT);

            // Build planned arrival time.
            final Date plannedArrivalTime = parseDate(stopDateTime.getString("arrival_date_time"));

            // Build planned arrival position.
            final Position plannedArrivalPosition = null;

            // Build planned departure time.
            final Date plannedDepartureTime = parseDate(stopDateTime.getString("departure_date_time"));

            // Build planned departure position.
            final Position plannedDeparturePosition = null;

            return new Stop(location, plannedArrivalTime, plannedArrivalPosition, plannedDepartureTime,
                    plannedDeparturePosition);
        } catch (final JSONException | ParseException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Leg parseLeg(final JSONObject section) throws IOException {
        try {
            // Build common leg info.
            final LegInfo legInfo = parseLegInfo(section);
            if (legInfo == null)
                return null;

            final String type = section.getString("type");
            final SectionType sectionType = SectionType.valueOf(type.toUpperCase());

            switch (sectionType) {
            case CROW_FLY: {
                // Return null leg if duration is 0.
                if (legInfo.min == 0)
                    return null;

                // Build type.
                final Individual.Type individualType = Individual.Type.WALK;

                return new Individual(individualType, legInfo.departure, legInfo.departureTime, legInfo.arrival,
                        legInfo.arrivalTime, legInfo.path, legInfo.distance);
            }
            case ON_DEMAND_TRANSPORT:
            case PUBLIC_TRANSPORT: {
                // Build line.
                final Line line = parseLineFromSection(section, sectionType);

                // Build destination.
                final JSONObject displayInfo = section.getJSONObject("display_informations");
                final String direction = displayInfo.getString("direction");
                final Location destination = new Location(LocationType.ANY, null, null, getLocationName(direction));

                final JSONArray stopDateTimes = section.getJSONArray("stop_date_times");
                final int nbStopDateTime = stopDateTimes.length();

                // Build departure stop.
                final Stop departureStop = parseStop(stopDateTimes.getJSONObject(0));

                // Build arrival stop.
                final Stop arrivalStop = parseStop(stopDateTimes.getJSONObject(nbStopDateTime - 1));

                // Build intermediate stops.
                final LinkedList<Stop> intermediateStops = new LinkedList<>();
                for (int i = 1; i < nbStopDateTime - 1; ++i) {
                    final Stop intermediateStop = parseStop(stopDateTimes.getJSONObject(i));
                    intermediateStops.add(intermediateStop);
                }

                // Build message.
                final String message = null;

                return new Public(line, destination, departureStop, arrivalStop, intermediateStops, legInfo.path,
                        message);
            }
            case STREET_NETWORK: {
                final String modeType = section.getString("mode");
                final TransferType transferType = TransferType.valueOf(modeType.toUpperCase());

                // Build type.
                final Individual.Type individualType;
                switch (transferType) {
                case BIKE:
                    individualType = Individual.Type.BIKE;
                    break;
                case WALKING:
                    individualType = Individual.Type.WALK;
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled transfer type: " + modeType);
                }

                return new Individual(individualType, legInfo.departure, legInfo.departureTime, legInfo.arrival,
                        legInfo.arrivalTime, legInfo.path, legInfo.distance);
            }
            case TRANSFER: {
                // Build type.
                final Individual.Type individualType = Individual.Type.WALK;

                return new Individual(individualType, legInfo.departure, legInfo.departureTime, legInfo.arrival,
                        legInfo.arrivalTime, legInfo.path, legInfo.distance);
            }
            case WAITING: {
                return null;
                // Do not add leg in case of waiting on the peer.
            }
            default:
                throw new IllegalArgumentException("Unhandled leg type: " + type);
            }
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private void parseQueryTripsResult(final JSONObject head, final Location from, final Location to,
            final QueryTripsResult result) throws IOException {
        try {
            // Fill trips.
            final JSONArray journeys = head.getJSONArray("journeys");
            for (int i = 0; i < journeys.length(); ++i) {
                final JSONObject journey = journeys.getJSONObject(i);
                final int changeCount = journey.getInt("nb_transfers");

                // Build leg list.
                final List<Leg> legs = new LinkedList<>();
                final JSONArray sections = journey.getJSONArray("sections");

                for (int j = 0; j < sections.length(); ++j) {
                    final JSONObject section = sections.getJSONObject(j);
                    final Leg leg = parseLeg(section);
                    if (leg != null)
                        legs.add(leg);
                }

                result.trips.add(new Trip(null, from, to, legs, null, null, changeCount));
            }
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private Line parseLine(final JSONObject jsonRoute) throws IOException {
        try {
            final JSONObject jsonLine = jsonRoute.getJSONObject("line");
            final String lineId = jsonLine.getString("id");
            String network = null;
            if (jsonLine.has("network"))
                network = Strings.emptyToNull(jsonLine.getJSONObject("network").optString("name"));
            final JSONObject mode = jsonRoute.getJSONArray("physical_modes").getJSONObject(0);
            final String modeId = mode.getString("id");
            final Product product = parseLineProductFromMode(modeId);
            final String code = jsonLine.getString("code");
            final String name = Strings.emptyToNull(jsonLine.optString("name"));
            final String color = Strings.emptyToNull(jsonLine.getString("color"));
            final String textColor = Strings.emptyToNull(jsonLine.optString("text_color"));
            final Style lineStyle = getLineStyle(network, product, code, color != null ? "#" + color : null,
                    textColor != null ? "#" + textColor : null);

            return new Line(lineId, network, product, code, name, lineStyle);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private @Nullable Product parseLineProductFromMode(final String modeId) {
        final String modeType = modeId.replace("physical_mode:", "");
        final PhysicalMode physicalMode = PhysicalMode.valueOf(modeType.toUpperCase());

        switch (physicalMode) {
        case BUS:
        case BUSRAPIDTRANSIT:
        case COACH:
        case SHUTTLE:
            return Product.BUS;
        case RAPIDTRANSIT:
        case TRAIN:
        case LOCALTRAIN:
        case LONGDISTANCETRAIN:
        case VAL:
        case RAILSHUTTLE:
            return Product.SUBURBAN_TRAIN;
        case TRAMWAY:
        case TRAM:
            return Product.TRAM;
        case METRO:
            return Product.SUBWAY;
        case FERRY:
            return Product.FERRY;
        case FUNICULAR:
            return Product.CABLECAR;
        case TAXI:
            return Product.ON_DEMAND;
        case OTHER:
            return null;
        default:
            throw new IllegalArgumentException("Unhandled physical mode: " + modeId);
        }
    }

    private LineDestination getStationLine(final Line line, final JSONObject jsonDeparture) throws IOException {
        try {
            final JSONObject route = jsonDeparture.getJSONObject("route");
            final JSONObject direction = route.getJSONObject("direction");
            final Location destination = parseLocation(direction);

            return new LineDestination(line, destination);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    private String getStopAreaId(final String stopPointId) throws IOException {
        final HttpUrl.Builder url = url().addPathSegment("stop_points").addPathSegment(stopPointId);
        url.addQueryParameter("depth", "1");
        final CharSequence page = httpClient.get(url.build());

        try {
            final JSONObject head = new JSONObject(page.toString());
            final JSONArray stopPoints = head.getJSONArray("stop_points");
            final JSONObject stopPoint = stopPoints.getJSONObject(0);
            final JSONObject stopArea = stopPoint.getJSONObject("stop_area");
            return stopArea.getString("id");
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    @Override
    protected boolean hasCapability(final Capability capability) {
        if (capability == Capability.SUGGEST_LOCATIONS || capability == Capability.NEARBY_LOCATIONS
                || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
            return true;
        else
            return false;
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
            int maxDistance, final int maxLocations) throws IOException {
        final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);

        // Build url depending of location type.
        final HttpUrl.Builder url = url();
        if (location.hasCoord()) {
            final double lon = location.getLonAsDouble();
            final double lat = location.getLatAsDouble();
            url.addPathSegment("coords").addPathSegment(lon + ";" + lat);
        } else if (location.type == LocationType.STATION) {
            if (!location.isIdentified())
                throw new IllegalArgumentException();
            url.addPathSegment("stop_points").addPathSegment(location.id);
        } else if (location.type == LocationType.POI) {
            if (!location.isIdentified())
                throw new IllegalArgumentException();
            url.addPathSegment("pois").addPathSegment(location.id);
        } else {
            throw new IllegalArgumentException("Unhandled location type: " + location.type);
        }
        url.addPathSegment("places_nearby");
        url.addQueryParameter("type[]", "stop_point");
        url.addQueryParameter("distance", Integer.toString(maxDistance == 0 ? 50000 : maxDistance));
        if (maxLocations > 0)
            url.addQueryParameter("count", Integer.toString(maxLocations));
        url.addQueryParameter("depth", "3");
        final CharSequence page = httpClient.get(url.build());

        try {
            final JSONObject head = new JSONObject(page.toString());

            final JSONObject pagination = head.getJSONObject("pagination");
            final int nbResults = pagination.getInt("total_result");
            // If no result is available, location id must be
            // faulty.
            if (nbResults == 0) {
                return new NearbyLocationsResult(resultHeader, Status.INVALID_ID);
            } else {
                final List<Location> stations = new ArrayList<>();

                final JSONArray places = head.getJSONArray("places_nearby");

                // Cycle through nearby stations.
                for (int i = 0; i < places.length(); ++i) {
                    final JSONObject place = places.getJSONObject(i);

                    // Add location to station list only if
                    // station is active, i.e. at least one
                    // departure exists within one hour.
                    final Location nearbyLocation = parseLocation(place);
                    stations.add(nearbyLocation);
                }

                return new NearbyLocationsResult(resultHeader, stations);
            }
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        checkNotNull(Strings.emptyToNull(stationId));

        final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);

        try {
            final QueryDeparturesResult result = new QueryDeparturesResult(resultHeader,
                    QueryDeparturesResult.Status.OK);

            // If equivs is equal to true, get stop_area corresponding
            // to stop_point and query departures.
            final HttpUrl.Builder url = url();
            final String header = stationId.substring(0, stationId.indexOf(":"));
            if (equivs && header.equals("stop_point")) {
                final String stopAreaId = getStopAreaId(stationId);
                url.addPathSegment("stop_areas");
                url.addPathSegment(stopAreaId);
            } else if (header.equals("stop_area")) {
                url.addPathSegment("stop_areas");
                url.addPathSegment(stationId);
            } else {
                url.addPathSegment("stop_points");
                url.addPathSegment(stationId);
            }
            url.addPathSegment("departures");
            url.addQueryParameter("from_datetime", printDate(time));
            url.addQueryParameter("count", Integer.toString(maxDepartures));
            url.addQueryParameter("duration", "86400");
            url.addQueryParameter("depth", "0");

            final CharSequence page = httpClient.get(url.build());

            final JSONObject head = new JSONObject(page.toString());

            final JSONArray departures = head.getJSONArray("departures");

            // Fill departures in StationDepartures.
            for (int i = 0; i < departures.length(); ++i) {
                final JSONObject jsonDeparture = departures.getJSONObject(i);

                // Build departure date.
                final JSONObject stopDateTime = jsonDeparture.getJSONObject("stop_date_time");
                final String departureDateTime = stopDateTime.getString("departure_date_time");
                final Date plannedTime = parseDate(departureDateTime);

                // Build line.
                final JSONObject route = jsonDeparture.getJSONObject("route");
                final Line line = parseLine(route);

                final JSONObject stopPoint = jsonDeparture.getJSONObject("stop_point");
                final Location location = parsePlace(stopPoint, PlaceType.STOP_POINT);

                // If stop point has already been added, retrieve it from result,
                // otherwise add it and add station lines.
                StationDepartures stationDepartures = result.findStationDepartures(location.id);
                if (stationDepartures == null) {
                    stationDepartures = new StationDepartures(location, new LinkedList<Departure>(),
                            new LinkedList<LineDestination>());
                    result.stationDepartures.add(stationDepartures);
                }
                final LineDestination lineDestination = getStationLine(line, jsonDeparture);
                final List<LineDestination> lines = stationDepartures.lines;
                if (lines != null && !lines.contains(lineDestination))
                    lines.add(lineDestination);
                final Location destination = lineDestination.destination;

                // Add departure to list.
                final Departure departure = new Departure(plannedTime, null, line, null, destination, null, null);
                stationDepartures.departures.add(departure);
            }

            return result;
        } catch (final JSONException | ParseException jsonExc) {
            throw new ParserException(jsonExc);
        } catch (final NotFoundException fnfExc) {
            try {
                final JSONObject head = new JSONObject(fnfExc.getBodyPeek().toString());
                final JSONObject error = head.getJSONObject("error");
                final String id = error.getString("id");

                if (id.equals("unknown_object"))
                    return new QueryDeparturesResult(resultHeader, QueryDeparturesResult.Status.INVALID_STATION);
                else
                    throw new IllegalArgumentException("Unhandled error id: " + id);
            } catch (final JSONException jsonExc) {
                throw new ParserException("Cannot parse error content, original exception linked", fnfExc);
            }
        }
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        final String nameCstr = constraint.toString();

        final HttpUrl.Builder url = url().addPathSegment("places");
        url.addQueryParameter("q", nameCstr);
        if (types == null || types.contains(LocationType.ANY) || types.contains(LocationType.STATION))
            url.addQueryParameter("type[]", "stop_area");
        if (types == null || types.contains(LocationType.ANY) || types.contains(LocationType.ADDRESS))
            url.addQueryParameter("type[]", "address");
        if (types == null || types.contains(LocationType.ANY) || types.contains(LocationType.POI))
            url.addQueryParameter("type[]", "poi");
        url.addQueryParameter("type[]", "administrative_region");
        url.addQueryParameter("depth", "1");
        final CharSequence page = httpClient.get(url.build());

        try {
            final List<SuggestedLocation> locations = new ArrayList<>();

            final JSONObject head = new JSONObject(page.toString());

            if (head.has("places")) {
                final JSONArray places = head.getJSONArray("places");
                final int numPlaces = places.length();

                for (int i = 0; i < numPlaces; ++i) {
                    final JSONObject place = places.getJSONObject(i);
                    final int priority = numPlaces - i; // "quality" found in JSON is deprecated, only sort order matters

                    // Add location to station list.
                    final Location location = parseLocation(place);
                    locations.add(new SuggestedLocation(location, priority));
                }
            }

            final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);
            return new SuggestLocationsResult(resultHeader, locations);
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, @Nullable TripOptions options) throws IOException {
        final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);

        try {
            if (from != null && from.isIdentified() && to != null && to.isIdentified()) {
                final HttpUrl.Builder url = url().addPathSegment("journeys");
                url.addQueryParameter("from", printLocation(from));
                url.addQueryParameter("to", printLocation(to));
                url.addQueryParameter("datetime", printDate(date));
                url.addQueryParameter("datetime_represents", dep ? "departure" : "arrival");
                url.addQueryParameter("min_nb_journeys", Integer.toString(this.numTripsRequested));
                url.addQueryParameter("depth", "0");

                if (options == null)
                    options = new TripOptions();

                // Set walking speed.
                if (options.walkSpeed != null) {
                    final double walkingSpeed;
                    switch (options.walkSpeed) {
                    case SLOW:
                        walkingSpeed = 1.12 * 0.8;
                        break;
                    case FAST:
                        walkingSpeed = 1.12 * 1.2;
                        break;
                    case NORMAL:
                    default:
                        walkingSpeed = 1.12;
                        break;
                    }

                    url.addQueryParameter("walking_speed", Double.toString(walkingSpeed));
                }

                if (options.flags != null && options.flags.contains(TripFlag.BIKE)) {
                    url.addQueryParameter("first_section_mode", "bike");
                    url.addQueryParameter("last_section_mode", "bike");
                }

                // Set forbidden physical modes.
                if (options.products != null && !options.products.equals(Product.ALL)) {
                    url.addQueryParameter("forbidden_uris[]", "physical_mode:Air");
                    url.addQueryParameter("forbidden_uris[]", "physical_mode:Boat");
                    if (!options.products.contains(Product.REGIONAL_TRAIN)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Localdistancetrain");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Train");
                    }
                    if (!options.products.contains(Product.SUBURBAN_TRAIN)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Localtrain");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Train");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Rapidtransit");
                    }
                    if (!options.products.contains(Product.SUBWAY)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Metro");
                    }
                    if (!options.products.contains(Product.TRAM)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Tramway");
                    }
                    if (!options.products.contains(Product.BUS)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Bus");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Busrapidtransit");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Coach");
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Shuttle");
                    }
                    if (!options.products.contains(Product.FERRY)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Ferry");
                    }
                    if (!options.products.contains(Product.CABLECAR)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Funicular");
                    }
                    if (!options.products.contains(Product.ON_DEMAND)) {
                        url.addQueryParameter("forbidden_uris[]", "physical_mode:Taxi");
                    }
                }

                final CharSequence page = httpClient.get(url.build());

                try {
                    final JSONObject head = new JSONObject(page.toString());

                    if (head.has("error")) {
                        final JSONObject error = head.getJSONObject("error");
                        final String id = error.getString("id");

                        if (id.equals("no_solution"))
                            return new QueryTripsResult(resultHeader, QueryTripsResult.Status.NO_TRIPS);
                        else
                            throw new IllegalArgumentException("Unhandled error id: " + id);
                    } else {
                        // Fill context.
                        HttpUrl prevQueryUrl = null;
                        HttpUrl nextQueryUrl = null;
                        final JSONArray links = head.getJSONArray("links");
                        for (int i = 0; i < links.length(); ++i) {
                            final JSONObject link = links.getJSONObject(i);
                            final String type = link.getString("type");
                            if (type.equals("prev")) {
                                prevQueryUrl = HttpUrl.parse(link.getString("href"));
                            } else if (type.equals("next")) {
                                nextQueryUrl = HttpUrl.parse(link.getString("href"));
                            }
                        }

                        String prevQueryUrlString = prevQueryUrl != null ? prevQueryUrl.toString() : null;
                        String nextQueryUrlString = nextQueryUrl != null ? nextQueryUrl.toString() : null;

                        final QueryTripsResult result = new QueryTripsResult(resultHeader, url.build().toString(), from,
                                null, to, new Context(from, to, prevQueryUrlString, nextQueryUrlString),
                                new LinkedList<Trip>());

                        parseQueryTripsResult(head, from, to, result);

                        return result;
                    }
                } catch (final JSONException jsonExc) {
                    throw new ParserException(jsonExc);
                }
            } else if (from != null && to != null) {
                List<Location> ambiguousFrom = null, ambiguousTo = null;
                Location newFrom = null, newTo = null;

                if (!from.isIdentified() && from.hasName()) {
                    ambiguousFrom = suggestLocations(from.name, null, 0).getLocations();
                    if (ambiguousFrom.isEmpty())
                        return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_FROM);
                    if (ambiguousFrom.size() == 1 && ambiguousFrom.get(0).isIdentified())
                        newFrom = ambiguousFrom.get(0);
                }

                if (!to.isIdentified() && to.hasName()) {
                    ambiguousTo = suggestLocations(to.name, null, 0).getLocations();
                    if (ambiguousTo.isEmpty())
                        return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_TO);
                    if (ambiguousTo.size() == 1 && ambiguousTo.get(0).isIdentified())
                        newTo = ambiguousTo.get(0);
                }

                if (newTo != null && newFrom != null)
                    return queryTrips(newFrom, via, newTo, date, dep, options);

                if (ambiguousFrom != null || ambiguousTo != null)
                    return new QueryTripsResult(resultHeader, ambiguousFrom, null, ambiguousTo);
            }
            return new QueryTripsResult(resultHeader, QueryTripsResult.Status.NO_TRIPS);
        } catch (final NotFoundException fnfExc) {
            try {
                final JSONObject head = new JSONObject(fnfExc.getBodyPeek().toString());
                final JSONObject error = head.getJSONObject("error");
                final String id = error.getString("id");

                if (id.equals("unknown_object")) {
                    // Identify unknown object.
                    final String fromString = printLocation(from);
                    final String toString = printLocation(to);

                    final String message = error.getString("message");
                    if (message.contains(fromString))
                        return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_FROM);
                    else if (message.contains(toString))
                        return new QueryTripsResult(resultHeader, QueryTripsResult.Status.UNKNOWN_TO);
                    else
                        throw new IllegalArgumentException("Unhandled error message: " + message);
                } else if (id.equals("date_out_of_bounds")) {
                    return new QueryTripsResult(resultHeader, QueryTripsResult.Status.INVALID_DATE);
                } else {
                    throw new IllegalArgumentException("Unhandled error id: " + id);
                }
            } catch (final JSONException jsonExc) {
                throw new ParserException("Cannot parse error content, original exception linked", fnfExc);
            }
        }
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException {
        final ResultHeader resultHeader = new ResultHeader(network, SERVER_PRODUCT, SERVER_VERSION, null, 0, null);

        final Context context = (Context) contextObj;
        final Location from = context.from;
        final Location to = context.to;
        final HttpUrl queryUrl = HttpUrl.parse(later ? context.nextQueryUri : context.prevQueryUri);
        final CharSequence page = httpClient.get(queryUrl);

        try {
            if (from.isIdentified() && to.isIdentified()) {
                final JSONObject head = new JSONObject(page.toString());

                // Fill context.
                final JSONArray links = head.getJSONArray("links");
                final JSONObject prev = links.getJSONObject(0);
                final HttpUrl prevQueryUrl = HttpUrl.parse(prev.getString("href"));
                final JSONObject next = links.getJSONObject(1);
                final HttpUrl nextQueryUrl = HttpUrl.parse(next.getString("href"));

                final QueryTripsResult result = new QueryTripsResult(resultHeader, queryUrl.toString(), from, null, to,
                        new Context(from, to, prevQueryUrl.toString(), nextQueryUrl.toString()),
                        new LinkedList<Trip>());

                parseQueryTripsResult(head, from, to, result);

                return result;
            } else {
                return new QueryTripsResult(null, QueryTripsResult.Status.NO_TRIPS);
            }
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }

    @Override
    public Point[] getArea() throws IOException {
        final HttpUrl.Builder url = url();
        final CharSequence page = httpClient.get(url.build());

        try {
            // Get shape string.
            final JSONObject head = new JSONObject(page.toString());
            final JSONArray regions = head.getJSONArray("regions");
            final JSONObject regionInfo = regions.getJSONObject(0);
            final String shape = regionInfo.getString("shape");

            // Parse string using JSON tokenizer for coordinates.
            List<Point> pointList = new ArrayList<>();
            final JSONTokener shapeTokener = new JSONTokener(shape);
            shapeTokener.skipTo('(');
            shapeTokener.next();
            shapeTokener.next();
            char c = shapeTokener.next();
            while (c != ')') {
                // Navitia coordinates are in (longitude, latitude) order.
                final String lonString = shapeTokener.nextTo(' ');
                shapeTokener.next();
                final String latString = shapeTokener.nextTo(",)");
                c = shapeTokener.next();

                // Append new point with (latitude, longitude) order.
                final double lat = Double.parseDouble(latString);
                final double lon = Double.parseDouble(lonString);
                pointList.add(Point.fromDouble(lat, lon));
            }

            // Fill point array.
            final Point[] pointArray = new Point[pointList.size()];
            for (int i = 0; i < pointList.size(); ++i)
                pointArray[i] = pointList.get(i);

            return pointArray;
        } catch (final JSONException jsonExc) {
            throw new ParserException(jsonExc);
        }
    }
}
