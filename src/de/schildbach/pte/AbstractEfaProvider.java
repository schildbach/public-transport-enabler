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
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Fare.Type;
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
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.exception.InvalidDataException;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.util.HttpClient;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractEfaProvider extends AbstractNetworkProvider {
    protected static final String DEFAULT_DEPARTURE_MONITOR_ENDPOINT = "XSLT_DM_REQUEST";
    protected static final String DEFAULT_TRIP_ENDPOINT = "XSLT_TRIP_REQUEST2";
    protected static final String DEFAULT_STOPFINDER_ENDPOINT = "XML_STOPFINDER_REQUEST";
    protected static final String DEFAULT_COORD_ENDPOINT = "XML_COORD_REQUEST";

    protected static final String SERVER_PRODUCT = "efa";
    protected static final String COORD_FORMAT = "WGS84[DD.ddddd]";
    protected static final int COORD_FORMAT_TAIL = 7;

    private final List CAPABILITIES = Arrays.asList(
            Capability.SUGGEST_LOCATIONS,
            Capability.NEARBY_LOCATIONS,
            Capability.DEPARTURES,
            Capability.TRIPS,
            Capability.TRIPS_VIA
    );

    private final HttpUrl departureMonitorEndpoint;
    private final HttpUrl tripEndpoint;
    private final HttpUrl stopFinderEndpoint;
    private final HttpUrl coordEndpoint;

    private String language = "de";
    private boolean needsSpEncId = false;
    private boolean includeRegionId = true;
    private boolean useProxFootSearch = true;
    private @Nullable String httpReferer = null;
    private @Nullable String httpRefererTrip = null;
    private boolean useRouteIndexAsTripId = true;
    private boolean useLineRestriction = true;
    private boolean useStringCoordListOutputFormat = true;
    private float fareCorrectionFactor = 1f;

    private final XmlPullParserFactory parserFactory;

    private static final Logger log = LoggerFactory.getLogger(AbstractEfaProvider.class);

    @SuppressWarnings("serial")
    private static class Context implements QueryTripsContext {
        private final String context;

        private Context(final String context) {
            this.context = context;
        }

        @Override
        public boolean canQueryLater() {
            return context != null;
        }

        @Override
        public boolean canQueryEarlier() {
            return false; // TODO enable earlier querying
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + context + "]";
        }
    }

    public AbstractEfaProvider(final NetworkId network, final HttpUrl apiBase) {
        this(network, apiBase, null, null, null, null);
    }

    public AbstractEfaProvider(final NetworkId network, final HttpUrl apiBase, final String departureMonitorEndpoint,
            final String tripEndpoint, final String stopFinderEndpoint, final String coordEndpoint) {
        this(network,
                apiBase.newBuilder()
                        .addPathSegment(departureMonitorEndpoint != null ? departureMonitorEndpoint
                                : DEFAULT_DEPARTURE_MONITOR_ENDPOINT)
                        .build(),
                apiBase.newBuilder().addPathSegment(tripEndpoint != null ? tripEndpoint : DEFAULT_TRIP_ENDPOINT)
                        .build(),
                apiBase.newBuilder()
                        .addPathSegment(stopFinderEndpoint != null ? stopFinderEndpoint : DEFAULT_STOPFINDER_ENDPOINT)
                        .build(),
                apiBase.newBuilder().addPathSegment(coordEndpoint != null ? coordEndpoint : DEFAULT_COORD_ENDPOINT)
                        .build());
    }

    public AbstractEfaProvider(final NetworkId network, final HttpUrl departureMonitorEndpoint,
            final HttpUrl tripEndpoint, final HttpUrl stopFinderEndpoint, final HttpUrl coordEndpoint) {
        super(network);

        try {
            parserFactory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME),
                    null);
        } catch (final XmlPullParserException x) {
            throw new RuntimeException(x);
        }

        this.departureMonitorEndpoint = departureMonitorEndpoint;
        this.tripEndpoint = tripEndpoint;
        this.stopFinderEndpoint = stopFinderEndpoint;
        this.coordEndpoint = coordEndpoint;
    }

    protected AbstractEfaProvider setLanguage(final String language) {
        this.language = language;
        return this;
    }

    protected AbstractEfaProvider setHttpReferer(final String httpReferer) {
        this.httpReferer = httpReferer;
        this.httpRefererTrip = httpReferer;
        return this;
    }

    public AbstractEfaProvider setHttpRefererTrip(final String httpRefererTrip) {
        this.httpRefererTrip = httpRefererTrip;
        return this;
    }

    protected AbstractEfaProvider setIncludeRegionId(final boolean includeRegionId) {
        this.includeRegionId = includeRegionId;
        return this;
    }

    protected AbstractEfaProvider setUseProxFootSearch(final boolean useProxFootSearch) {
        this.useProxFootSearch = useProxFootSearch;
        return this;
    }

    protected AbstractEfaProvider setUseRouteIndexAsTripId(final boolean useRouteIndexAsTripId) {
        this.useRouteIndexAsTripId = useRouteIndexAsTripId;
        return this;
    }

    protected AbstractEfaProvider setUseLineRestriction(final boolean useLineRestriction) {
        this.useLineRestriction = useLineRestriction;
        return this;
    }

    protected AbstractEfaProvider setUseStringCoordListOutputFormat(final boolean useStringCoordListOutputFormat) {
        this.useStringCoordListOutputFormat = useStringCoordListOutputFormat;
        return this;
    }

    protected AbstractEfaProvider setNeedsSpEncId(final boolean needsSpEncId) {
        this.needsSpEncId = needsSpEncId;
        return this;
    }

    protected AbstractEfaProvider setFareCorrectionFactor(final float fareCorrectionFactor) {
        this.fareCorrectionFactor = fareCorrectionFactor;
        return this;
    }

    // this should be overridden by networks not providing one of the default capabilities
    @Override
    protected boolean hasCapability(final Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    private final void appendCommonRequestParams(final HttpUrl.Builder url, final String outputFormat) {
        url.addEncodedQueryParameter("outputFormat", outputFormat);
        url.addEncodedQueryParameter("language", language);
        url.addEncodedQueryParameter("stateless", "1");
        url.addEncodedQueryParameter("coordOutputFormat", COORD_FORMAT);
        url.addEncodedQueryParameter("coordOutputFormatTail", Integer.toString(COORD_FORMAT_TAIL));
    }

    protected SuggestLocationsResult jsonStopfinderRequest(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        final HttpUrl.Builder url = stopFinderEndpoint.newBuilder();
        appendStopfinderRequestParameters(url, constraint, "JSON", types, maxLocations);
        final CharSequence page = httpClient.get(url.build());
        final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

        try {
            final List<SuggestedLocation> locations = new ArrayList<>();
            final JSONObject head = new JSONObject(page.toString());
            final JSONObject stopFinder = head.optJSONObject("stopFinder");
            if (stopFinder != null) {
                final JSONArray messages = stopFinder.optJSONArray("message");
                if (messages != null) {
                    final SuggestLocationsResult.Status status = parseJsonMessages(messages);
                    if (status != null)
                        return new SuggestLocationsResult(header, status);
                }

                final JSONObject points = stopFinder.optJSONObject("points");
                if (points != null) {
                    final JSONObject point = points.getJSONObject("point");
                    final SuggestedLocation location = parseJsonPoint(point);
                    locations.add(location);
                }

                final JSONArray pointsArray = stopFinder.optJSONArray("points");
                if (pointsArray != null) {
                    final int nPoints = pointsArray.length();
                    for (int i = 0; i < nPoints; i++) {
                        final JSONObject point = pointsArray.optJSONObject(i);
                        final SuggestedLocation location = parseJsonPoint(point);
                        locations.add(location);
                    }
                }
            } else {
                final JSONArray messages = head.optJSONArray("message");
                if (messages != null) {
                    final SuggestLocationsResult.Status status = parseJsonMessages(messages);
                    if (status != null)
                        return new SuggestLocationsResult(header, status);
                }

                final JSONArray pointsArray = head.optJSONArray("stopFinder");
                if (pointsArray != null) {
                    final int nPoints = pointsArray.length();
                    for (int i = 0; i < nPoints; i++) {
                        final JSONObject point = pointsArray.optJSONObject(i);
                        final SuggestedLocation location = parseJsonPoint(point);
                        locations.add(location);
                    }
                }
            }

            return new SuggestLocationsResult(header, locations);
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    private SuggestLocationsResult.Status parseJsonMessages(final JSONArray messages) throws JSONException {
        final int messagesSize = messages.length();
        for (int i = 0; i < messagesSize; i++) {
            final JSONObject message = messages.optJSONObject(i);
            final String messageName = message.getString("name");
            final String messageValue = Strings.emptyToNull(message.getString("value"));
            if ("code".equals(messageName) && !"-8010".equals(messageValue) && !"-8011".equals(messageValue))
                return SuggestLocationsResult.Status.SERVICE_DOWN;
        }
        return null;
    }

    private SuggestedLocation parseJsonPoint(final JSONObject point) throws JSONException {
        String type = point.getString("type");
        if ("any".equals(type))
            type = point.getString("anyType");
        final String stateless = point.getString("stateless");
        final String name = normalizeLocationName(point.optString("name"));
        final String object = normalizeLocationName(point.optString("object"));
        final String postcode = point.optString("postcode");
        final int quality = point.getInt("quality");
        final JSONObject ref = point.getJSONObject("ref");
        final String id = ref.getString("id");
        String place = ref.getString("place");
        if (place != null && place.length() == 0)
            place = null;
        final Point coord = parseCoord(ref.optString("coords", null));

        final Location location;
        if ("stop".equals(type)) {
            if (!stateless.startsWith(id))
                throw new RuntimeException("id mismatch: '" + id + "' vs '" + stateless + "'");
            location = new Location(LocationType.STATION, id, coord, place, object);
        } else if ("poi".equals(type)) {
            location = new Location(LocationType.POI, stateless, coord, place, object);
        } else if ("crossing".equals(type)) {
            location = new Location(LocationType.ADDRESS, stateless, coord, place, object);
        } else if ("street".equals(type) || "address".equals(type) || "singlehouse".equals(type)
                || "buildingname".equals(type) || "loc".equals(type)) {
            location = new Location(LocationType.ADDRESS, stateless, coord, place, name);
        } else if ("postcode".equals(type)) {
            location = new Location(LocationType.ADDRESS, stateless, coord, place, postcode);
        } else {
            throw new JSONException("unknown type: " + type);
        }

        return new SuggestedLocation(location, quality);
    }

    private void appendStopfinderRequestParameters(final HttpUrl.Builder url, final CharSequence constraint,
            final String outputFormat, final @Nullable Set<LocationType> types, final int maxLocations) {
        appendCommonRequestParams(url, outputFormat);
        url.addEncodedQueryParameter("locationServerActive", "1");
        if (includeRegionId)
            url.addEncodedQueryParameter("regionID_sf", "1"); // prefer own region
        url.addEncodedQueryParameter("type_sf", "any");
        url.addEncodedQueryParameter("name_sf", ParserUtils.urlEncode(constraint.toString(), requestUrlEncoding));
        if (needsSpEncId)
            url.addEncodedQueryParameter("SpEncId", "0");
        int filter = 0;
        if (types == null || types.contains(LocationType.STATION))
            filter += 2; // stop
        if (types == null || types.contains(LocationType.POI))
            filter += 32; // poi
        if (types == null || types.contains(LocationType.ADDRESS))
            filter += 4 + 8 + 16 + 64; // street + address + crossing + postcode
        url.addEncodedQueryParameter("anyObjFilter_sf", Integer.toString(filter));
        url.addEncodedQueryParameter("reducedAnyPostcodeObjFilter_sf", "64");
        url.addEncodedQueryParameter("reducedAnyTooManyObjFilter_sf", "2");
        url.addEncodedQueryParameter("useHouseNumberList", "true");
        if (maxLocations > 0)
            url.addEncodedQueryParameter("anyMaxSizeHitList", Integer.toString(maxLocations));
    }

    protected SuggestLocationsResult mobileStopfinderRequest(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        final HttpUrl.Builder url = stopFinderEndpoint.newBuilder();
        appendStopfinderRequestParameters(url, constraint, "XML", types, maxLocations);
        final AtomicReference<SuggestLocationsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterEfa(pp);
                XmlPullUtil.optSkip(pp, "ers");

                final List<SuggestedLocation> locations = new ArrayList<>();
                XmlPullUtil.require(pp, "sf");
                if (XmlPullUtil.optEnter(pp, "sf")) {
                    while (XmlPullUtil.optEnter(pp, "p")) {
                        final String name = normalizeLocationName(XmlPullUtil.valueTag(pp, "n"));
                        final String u = XmlPullUtil.valueTag(pp, "u");
                        if (!"sf".equals(u))
                            throw new RuntimeException("unknown usage: " + u);
                        final String ty = XmlPullUtil.valueTag(pp, "ty");
                        final LocationType type;
                        if ("stop".equals(ty))
                            type = LocationType.STATION;
                        else if ("poi".equals(ty))
                            type = LocationType.POI;
                        else if ("loc".equals(ty))
                            type = LocationType.COORD;
                        else if ("street".equals(ty))
                            type = LocationType.ADDRESS;
                        else if ("singlehouse".equals(ty))
                            type = LocationType.ADDRESS;
                        else
                            throw new RuntimeException("unknown type: " + ty);

                        XmlPullUtil.enter(pp, "r");

                        final String id = XmlPullUtil.valueTag(pp, "id");
                        XmlPullUtil.optValueTag(pp, "gid", null);
                        final String stateless = XmlPullUtil.valueTag(pp, "stateless");
                        XmlPullUtil.valueTag(pp, "omc");
                        final String place = normalizeLocationName(XmlPullUtil.optValueTag(pp, "pc", null));
                        XmlPullUtil.valueTag(pp, "pid");
                        final Point coord = parseCoord(XmlPullUtil.optValueTag(pp, "c", null));

                        XmlPullUtil.skipExit(pp, "r");

                        final String qal = XmlPullUtil.optValueTag(pp, "qal", null);
                        final int quality = qal != null ? Integer.parseInt(qal) : 0;

                        XmlPullUtil.skipExit(pp, "p");

                        final Location location = new Location(type, type == LocationType.STATION ? id : stateless,
                                coord, place, name);
                        final SuggestedLocation locationAndQuality = new SuggestedLocation(location, quality);
                        locations.add(locationAndQuality);
                    }

                    XmlPullUtil.skipExit(pp, "sf");
                }

                result.set(new SuggestLocationsResult(header, locations));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    private void appendCoordRequestParameters(final HttpUrl.Builder url, final Set<LocationType> types,
            final Point coord, final int maxDistance, final int maxLocations) {
        appendCommonRequestParams(url, "XML");
        url.addEncodedQueryParameter("coord", ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%.7f:%.7f:%s",
                coord.getLonAsDouble(), coord.getLatAsDouble(), COORD_FORMAT), requestUrlEncoding));
        url.addEncodedQueryParameter("coordListOutputFormat", useStringCoordListOutputFormat ? "string" : "list");
        url.addEncodedQueryParameter("max", Integer.toString(maxLocations != 0 ? maxLocations : 50));
        url.addEncodedQueryParameter("inclFilter", "1");
        int i = 1;
        for (final LocationType type : types) {
            url.addEncodedQueryParameter("radius_" + i, Integer.toString(maxDistance != 0 ? maxDistance : 1320));
            if (type == LocationType.STATION)
                url.addEncodedQueryParameter("type_" + i, "STOP");
            else if (type == LocationType.POI)
                url.addEncodedQueryParameter("type_" + i, "POI_POINT");
            else
                throw new IllegalArgumentException("cannot handle location type: " + type);
            i++;
        }
    }

    protected NearbyLocationsResult xmlCoordRequest(final Set<LocationType> types, final Point coord,
            final int maxDistance, final int maxStations) throws IOException {
        final HttpUrl.Builder url = coordEndpoint.newBuilder();
        appendCoordRequestParameters(url, types, coord, maxDistance, maxStations);
        final AtomicReference<NearbyLocationsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterItdRequest(pp);

                XmlPullUtil.enter(pp, "itdCoordInfoRequest");

                XmlPullUtil.enter(pp, "itdCoordInfo");

                XmlPullUtil.optSkip(pp, "coordInfoRequest");

                final List<Location> locations = new ArrayList<>();

                if (XmlPullUtil.optEnter(pp, "coordInfoItemList")) {
                    while (XmlPullUtil.test(pp, "coordInfoItem")) {
                        final String type = XmlPullUtil.attr(pp, "type");
                        final LocationType locationType;
                        if ("STOP".equals(type))
                            locationType = LocationType.STATION;
                        else if ("POI_POINT".equals(type))
                            locationType = LocationType.POI;
                        else
                            throw new IllegalStateException("unknown type: " + type);

                        String id = XmlPullUtil.optAttr(pp, "stateless", null);
                        if (id == null)
                            id = XmlPullUtil.attr(pp, "id");

                        final String name = normalizeLocationName(XmlPullUtil.optAttr(pp, "name", null));
                        final String place = normalizeLocationName(XmlPullUtil.optAttr(pp, "locality", null));

                        XmlPullUtil.enter(pp, "coordInfoItem");

                        // FIXME this is always only one coordinate
                        final List<Point> path = processItdPathCoordinates(pp);
                        final Point coord1 = path != null ? path.get(0) : null;

                        EnumSet<Product> products = null;
                        if (XmlPullUtil.optEnter(pp, "genAttrList")) {
                            while (XmlPullUtil.optEnter(pp, "genAttrElem")) {
                                final String attrName = XmlPullUtil.valueTag(pp, "name");
                                final String attrValue = XmlPullUtil.valueTag(pp, "value");
                                XmlPullUtil.skipExit(pp, "genAttrElem");

                                if ("STOP_MAJOR_MEANS".equals(attrName)) {
                                    products = EnumSet.noneOf(Product.class);
                                    final Product product = majorMeansToProduct(Integer.parseInt(attrValue));
                                    if (product != null)
                                        products.add(product);
                                }
                            }
                            XmlPullUtil.skipExit(pp, "genAttrList");
                        }

                        XmlPullUtil.skipExit(pp, "coordInfoItem");

                        if (name != null)
                            locations.add(new Location(locationType, id, coord1, place, name, products));
                    }

                    XmlPullUtil.skipExit(pp, "coordInfoItemList");
                }

                result.set(new NearbyLocationsResult(header, locations));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    protected NearbyLocationsResult mobileCoordRequest(final Set<LocationType> types, final Point coord,
            final int maxDistance, final int maxStations) throws IOException {
        final HttpUrl.Builder url = coordEndpoint.newBuilder();
        appendCoordRequestParameters(url, types, coord, maxDistance, maxStations);
        final AtomicReference<NearbyLocationsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterEfa(pp);

                XmlPullUtil.enter(pp, "ci");

                XmlPullUtil.enter(pp, "request");
                XmlPullUtil.skipExit(pp, "request");

                final List<Location> stations = new ArrayList<>();

                if (XmlPullUtil.optEnter(pp, "pis")) {
                    while (XmlPullUtil.optEnter(pp, "pi")) {
                        final String name = normalizeLocationName(XmlPullUtil.optValueTag(pp, "de", null));
                        final String type = XmlPullUtil.valueTag(pp, "ty");
                        final LocationType locationType;
                        if ("STOP".equals(type))
                            locationType = LocationType.STATION;
                        else if ("POI_POINT".equals(type))
                            locationType = LocationType.POI;
                        else
                            throw new IllegalStateException("unknown type: " + type);

                        final String id = XmlPullUtil.valueTag(pp, "id");
                        XmlPullUtil.valueTag(pp, "omc");
                        XmlPullUtil.optValueTag(pp, "pid", null);
                        final String place = normalizeLocationName(XmlPullUtil.optValueTag(pp, "locality", null));
                        XmlPullUtil.valueTag(pp, "layer");
                        XmlPullUtil.valueTag(pp, "gisID");
                        XmlPullUtil.valueTag(pp, "ds");
                        final String stateless = XmlPullUtil.valueTag(pp, "stateless");
                        final String locationId = locationType == LocationType.STATION ? id : stateless;
                        final Point coord1 = parseCoord(XmlPullUtil.valueTag(pp, "c"));

                        EnumSet<Product> products = null;
                        if (XmlPullUtil.optEnter(pp, "attrs")) {
                            while (XmlPullUtil.optEnter(pp, "attr")) {
                                final String attrName = XmlPullUtil.valueTag(pp, "n");
                                final String attrValue = XmlPullUtil.valueTag(pp, "v");
                                XmlPullUtil.skipExit(pp, "attr");

                                if ("STOP_MAJOR_MEANS".equals(attrName)) {
                                    products = EnumSet.noneOf(Product.class);
                                    final Product product = majorMeansToProduct(Integer.parseInt(attrValue));
                                    if (product != null)
                                        products.add(product);
                                }
                            }
                            XmlPullUtil.skipExit(pp, "attrs");
                        }

                        final Location location;
                        if (name != null)
                            location = new Location(locationType, locationId, coord1, place, name, products);
                        else
                            location = new Location(locationType, locationId, coord1, null, place, products);
                        stations.add(location);

                        XmlPullUtil.skipExit(pp, "pi");
                    }

                    XmlPullUtil.skipExit(pp, "pis");
                }

                XmlPullUtil.skipExit(pp, "ci");

                result.set(new NearbyLocationsResult(header, stations));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        return jsonStopfinderRequest(constraint, types, maxLocations);
    }

    private interface ProcessItdOdvCallback {
        void location(String nameState, Location location, int matchQuality);
    }

    private String processItdOdv(final XmlPullParser pp, final String expectedUsage,
            final ProcessItdOdvCallback callback) throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "itdOdv");

        final String usage = XmlPullUtil.attr(pp, "usage");
        if (expectedUsage != null && !usage.equals(expectedUsage))
            throw new IllegalStateException("expecting <itdOdv usage=\"" + expectedUsage + "\" />");

        final String type = XmlPullUtil.attr(pp, "type");

        XmlPullUtil.enter(pp, "itdOdv");

        final String place = processItdOdvPlace(pp);

        XmlPullUtil.require(pp, "itdOdvName");
        final String nameState = XmlPullUtil.attr(pp, "state");
        XmlPullUtil.enter(pp, "itdOdvName");

        XmlPullUtil.optSkip(pp, "itdMessage");

        if ("identified".equals(nameState)) {
            final Location location = processOdvNameElem(pp, type, place);
            if (location != null)
                callback.location(nameState, location, Integer.MAX_VALUE);
        } else if ("list".equals(nameState)) {
            while (XmlPullUtil.test(pp, "odvNameElem")) {
                final int matchQuality = XmlPullUtil.intAttr(pp, "matchQuality");
                final Location location = processOdvNameElem(pp, type, place);
                if (location != null)
                    callback.location(nameState, location, matchQuality);
            }
        } else if ("notidentified".equals(nameState) || "empty".equals(nameState)) {
            XmlPullUtil.optSkip(pp, "odvNameElem");
        } else {
            throw new RuntimeException("cannot handle nameState '" + nameState + "'");
        }

        XmlPullUtil.optSkipMultiple(pp, "infoLink");
        XmlPullUtil.optSkip(pp, "itdMapItemList");
        XmlPullUtil.optSkip(pp, "odvNameInput");

        XmlPullUtil.exit(pp, "itdOdvName");

        XmlPullUtil.optSkip(pp, "odvInfoList");

        XmlPullUtil.optSkip(pp, "itdPoiHierarchyRoot");

        if (XmlPullUtil.optEnter(pp, "itdOdvAssignedStops")) {
            while (XmlPullUtil.test(pp, "itdOdvAssignedStop")) {
                final Location stop = processItdOdvAssignedStop(pp);

                if (stop != null)
                    callback.location("assigned", stop, 0);
            }

            XmlPullUtil.exit(pp, "itdOdvAssignedStops");
        }

        XmlPullUtil.optSkip(pp, "itdServingModes");

        XmlPullUtil.optSkip(pp, "genAttrList");

        XmlPullUtil.exit(pp, "itdOdv");

        return nameState;
    }

    private String processItdOdvPlace(final XmlPullParser pp) throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "itdOdvPlace");

        final String placeState = XmlPullUtil.attr(pp, "state");

        XmlPullUtil.enter(pp, "itdOdvPlace");
        String place = null;
        if ("identified".equals(placeState)) {
            if (XmlPullUtil.test(pp, "odvPlaceElem"))
                place = normalizeLocationName(XmlPullUtil.valueTag(pp, "odvPlaceElem"));
        }
        XmlPullUtil.skipExit(pp, "itdOdvPlace");

        return place;
    }

    private Location processOdvNameElem(final XmlPullParser pp, String type, final String defaultPlace)
            throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "odvNameElem");

        if ("any".equals(type))
            type = XmlPullUtil.attr(pp, "anyType");
        final String id = XmlPullUtil.optAttr(pp, "id", null);
        final String stateless = XmlPullUtil.attr(pp, "stateless");
        final String locality = normalizeLocationName(XmlPullUtil.optAttr(pp, "locality", null));
        final String objectName = normalizeLocationName(XmlPullUtil.optAttr(pp, "objectName", null));
        final String buildingName = XmlPullUtil.optAttr(pp, "buildingName", null);
        final String buildingNumber = XmlPullUtil.optAttr(pp, "buildingNumber", null);
        final String postCode = XmlPullUtil.optAttr(pp, "postCode", null);
        final String streetName = XmlPullUtil.optAttr(pp, "streetName", null);
        final Point coord = processCoordAttr(pp);

        XmlPullUtil.enter(pp, "odvNameElem");
        XmlPullUtil.optSkip(pp, "itdMapItemList");
        final String nameElem;
        if (pp.getEventType() == XmlPullParser.TEXT) {
            nameElem = normalizeLocationName(pp.getText());
            pp.next();
        } else {
            nameElem = null;
        }
        XmlPullUtil.exit(pp, "odvNameElem");

        if ("stop".equals(type)) {
            if (id != null && !stateless.startsWith(id))
                throw new RuntimeException("id mismatch: '" + id + "' vs '" + stateless + "'");
            return new Location(LocationType.STATION, id != null ? id : stateless, coord,
                    locality != null ? locality : defaultPlace, objectName != null ? objectName : nameElem);
        } else if ("poi".equals(type)) {
            return new Location(LocationType.POI, stateless, coord, locality != null ? locality : defaultPlace,
                    objectName != null ? objectName : nameElem);
        } else if ("loc".equals(type)) {
            if (locality != null) {
                return new Location(LocationType.ADDRESS, stateless, coord, null, locality);
            } else if (nameElem != null) {
                return new Location(LocationType.ADDRESS, stateless, coord, null, nameElem);
            } else if (coord != null) {
                return new Location(LocationType.COORD, stateless, coord, null, null);
            } else {
                throw new IllegalArgumentException("not enough data for type/anyType: " + type);
            }
        } else if ("address".equals(type) || "singlehouse".equals(type)) {
            return new Location(LocationType.ADDRESS, stateless, coord, locality != null ? locality : defaultPlace,
                    objectName + (buildingNumber != null ? " " + buildingNumber : ""));
        } else if ("street".equals(type) || "crossing".equals(type)) {
            return new Location(LocationType.ADDRESS, stateless, coord, locality != null ? locality : defaultPlace,
                    objectName != null ? objectName : nameElem);
        } else if ("postcode".equals(type)) {
            return new Location(LocationType.ADDRESS, stateless, coord, locality != null ? locality : defaultPlace,
                    postCode);
        } else if ("buildingname".equals(type)) {
            return new Location(LocationType.ADDRESS, stateless, coord, locality != null ? locality : defaultPlace,
                    buildingName != null ? buildingName : streetName);
        } else if ("coord".equals(type)) {
            return new Location(LocationType.ADDRESS, stateless, coord, defaultPlace, nameElem);
        } else {
            throw new IllegalArgumentException("unknown type/anyType: " + type);
        }
    }

    private Location processItdOdvAssignedStop(final XmlPullParser pp) throws XmlPullParserException, IOException {
        final String id = XmlPullUtil.attr(pp, "stopID");
        final Point coord = processCoordAttr(pp);
        final String place = normalizeLocationName(XmlPullUtil.optAttr(pp, "place", null));
        final String name = normalizeLocationName(XmlPullUtil.optValueTag(pp, "itdOdvAssignedStop", null));

        if (name != null)
            return new Location(LocationType.STATION, id, coord, place, name);
        else
            return null;
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasCoord())
            return xmlCoordRequest(types, location.coord, maxDistance, maxLocations);

        if (location.type != LocationType.STATION)
            throw new IllegalArgumentException("cannot handle: " + location.type);

        if (!location.hasId())
            throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

        return nearbyStationsRequest(location.id, maxLocations);
    }

    private NearbyLocationsResult nearbyStationsRequest(final String stationId, final int maxLocations)
            throws IOException {
        final HttpUrl.Builder url = departureMonitorEndpoint.newBuilder();
        appendCommonRequestParams(url, "XML");
        url.addEncodedQueryParameter("type_dm", "stop");
        url.addEncodedQueryParameter("name_dm",
                ParserUtils.urlEncode(normalizeStationId(stationId), requestUrlEncoding));
        url.addEncodedQueryParameter("itOptionsActive", "1");
        url.addEncodedQueryParameter("ptOptionsActive", "1");
        if (useProxFootSearch)
            url.addEncodedQueryParameter("useProxFootSearch", "1");
        url.addEncodedQueryParameter("mergeDep", "1");
        url.addEncodedQueryParameter("useAllStops", "1");
        url.addEncodedQueryParameter("mode", "direct");
        final AtomicReference<NearbyLocationsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterItdRequest(pp);

                XmlPullUtil.enter(pp, "itdDepartureMonitorRequest");

                final AtomicReference<Location> ownStation = new AtomicReference<>();
                final List<Location> stations = new ArrayList<>();

                final String nameState = processItdOdv(pp, "dm", (nameState1, location, matchQuality) -> {
                    if (location.type == LocationType.STATION) {
                        if ("identified".equals(nameState1)) ownStation.set(location);
                        else if ("assigned".equals(nameState1)) stations.add(location);
                    } else {
                        throw new IllegalStateException("cannot handle: " + location.type);
                    }
                });

                if ("notidentified".equals(nameState)) {
                    result.set(new NearbyLocationsResult(header, NearbyLocationsResult.Status.INVALID_ID));
                    return;
                }

                if (ownStation.get() != null && !stations.contains(ownStation.get()))
                    stations.add(ownStation.get());

                if (maxLocations == 0 || maxLocations >= stations.size())
                    result.set(new NearbyLocationsResult(header, stations));
                else
                    result.set(new NearbyLocationsResult(header, stations.subList(0, maxLocations)));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    private static final Pattern P_LINE_RE = Pattern.compile("RE ?\\d+[ab]?");
    private static final Pattern P_LINE_RB = Pattern.compile("RB ?\\d+[abc]?");
    private static final Pattern P_LINE_R = Pattern.compile("R ?\\d+");
    private static final Pattern P_LINE_IRE = Pattern.compile("IRE\\d+[ab]?");
    private static final Pattern P_LINE_MEX = Pattern.compile("MEX\\d+[abc]?");
    private static final Pattern P_LINE_S = Pattern.compile("S ?\\d+");
    private static final Pattern P_LINE_S_DB = Pattern.compile("(S\\d+) \\((?:DB Regio AG)\\)");
    private static final Pattern P_LINE_NUMBER = Pattern.compile("\\d+");

    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if (mot == null) {
            if (trainName != null) {
                final String str = Strings.nullToEmpty(name);
                if (trainName.equals("S-Bahn"))
                    return new Line(id, network, Product.SUBURBAN_TRAIN, str);
                if (trainName.equals("U-Bahn"))
                    return new Line(id, network, Product.SUBWAY, str);
                if (trainName.equals("Straßenbahn"))
                    return new Line(id, network, Product.TRAM, str);
                if (trainName.equals("Badner Bahn"))
                    return new Line(id, network, Product.TRAM, str);
                if (trainName.equals("Stadtbus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Citybus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Regionalbus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("ÖBB-Postbus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Autobus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Discobus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Nachtbus"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Anrufsammeltaxi"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Ersatzverkehr"))
                    return new Line(id, network, Product.BUS, str);
                if (trainName.equals("Vienna Airport Lines"))
                    return new Line(id, network, Product.BUS, str);
            }
        } else if ("0".equals(mot)) {
            final String trainNumStr = Strings.nullToEmpty(trainNum);

            if (("EC".equals(trainType) || "EuroCity".equals(trainName) || "Eurocity".equals(trainName))
                    && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "EC" + trainNum);
            if (("EN".equals(trainType) || "EuroNight".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "EN" + trainNum);
            if (("IC".equals(trainType) || "IC".equals(trainName) || "InterCity".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "IC" + trainNum);
            if ("IC21".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, trainNum);
            if ("IC40".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, trainNum);
            if (("ICE".equals(trainType) || "ICE".equals(trainName) || "Intercity-Express".equals(trainName))
                    && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ICE" + trainNum);
            if (("ICN".equals(trainType) || "InterCityNight".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ICN" + trainNum);
            if (("X".equals(trainType) || "InterConnex".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "X" + trainNum);
            if (("CNL".equals(trainType) || "CityNightLine".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "CNL" + trainNum);
            if (("THA".equals(trainType) || "Thalys".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "THA" + trainNum);
            if ("RHI".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "RHI" + trainNum);
            if (("TGV".equals(trainType) || "TGV".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "TGV" + trainNum);
            if ("TGD".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "TGD" + trainNum);
            if ("INZ".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "INZ" + trainNum);
            if (("RJ".equals(trainType) || "railjet".equals(trainName)))
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "RJ" + Strings.nullToEmpty(trainNum));
            if (("RJX".equals(trainType) || "railjet xpress".equals(trainName)))
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "RJX" + Strings.nullToEmpty(trainNum));
            if (("WB".equals(trainType) || "WESTbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "WB" + trainNum);
            if (("HKX".equals(trainType) || "Hamburg-Köln-Express".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "HKX" + trainNum);
            if ("INT".equals(trainType) && trainNum != null) // SVV, VAGFR
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "INT" + trainNum);
            if (("SC".equals(trainType) || "SC Pendolino".equals(trainName)) && trainNum != null) // SuperCity
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "SC" + trainNum);
            if ("ECB".equals(trainType) && trainNum != null) // EC, Verona-München
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ECB" + trainNum);
            if ("ES".equals(trainType) && trainNum != null) // Eurostar Italia
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ES" + trainNum);
            if (("EST".equals(trainType) || "EUROSTAR".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "EST" + trainNum);
            if ("EIC".equals(trainType) && trainNum != null) // Ekspres InterCity, Polen
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "EIC" + trainNum);
            if ("MT".equals(trainType) && "Schnee-Express".equals(trainName) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "MT" + trainNum);
            if (("TLK".equals(trainType) || "Tanie Linie Kolejowe".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "TLK" + trainNum);
            if ("DNZ".equals(trainType) && trainNum != null) // Nacht-Schnellzug
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "DNZ" + trainNum);
            if ("AVE".equals(trainType) && trainNum != null) // klimatisierter Hochgeschwindigkeitszug
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "DNZ" + trainNum);
            if ("ARC".equals(trainType) && trainNum != null) // Arco/Alvia/Avant (Renfe), Spanien
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ARC" + trainNum);
            if ("HOT".equals(trainType) && trainNum != null) // Spanien, Nacht
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "HOT" + trainNum);
            if ("LCM".equals(trainType) && "Locomore".equals(trainName) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "LCM" + trainNum);
            if ("Locomore".equals(longName))
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "LOC" + Strings.nullToEmpty(trainNum));
            if ("NJ".equals(trainType) && trainNum != null) // NightJet
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "NJ" + trainNum);
            if ("FLX".equals(trainType) && "FlixTrain".equals(trainName) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "FLX" + trainNum);

            if ("IR".equals(trainType) || "Interregio".equals(trainName) || "InterRegio".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "IR" + trainNum);
            if ("IR13".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("IR36".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("IR37".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("IR75".equals(trainNum) && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("IRE".equals(trainType) || "Interregio-Express".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "IRE" + trainNum);
            if (trainType == null && trainNum != null && P_LINE_IRE.matcher(trainNum).matches())
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("RE".equals(trainType) || "Regional-Express".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE" + trainNum);
            if (trainType == null && trainNum != null && P_LINE_RE.matcher(trainNum).matches())
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("RE3 / RB30".equals(trainNum) && trainType == null && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE3/RB30");
            if ("Regionalexpress".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("RE1 (RRX)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE1");
            if ("RE5 (RRX)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE5");
            if ("RE6 (RRX)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE6");
            if ("RE11 (RRX)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE11");
            if ("RB-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if (trainType == null && "RB67/71".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if (trainType == null && "RB65/68".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("RE-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("REX".equals(trainType)) // RegionalExpress, Österreich
                return new Line(id, network, Product.REGIONAL_TRAIN, "REX" + trainNum);
            if (("RB".equals(trainType) || "Regionalbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "RB" + trainNum);
            if (trainType == null && trainNum != null && P_LINE_RB.matcher(trainNum).matches())
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("Abellio-Zug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("Westfalenbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("Chiemseebahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R".equals(trainType) || "Regionalzug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "R" + trainNum);
            if (trainType == null && trainNum != null && P_LINE_R.matcher(trainNum).matches())
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("D".equals(trainType) || "Schnellzug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "D" + trainNum);
            if ("E".equals(trainType) || "Eilzug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "E" + trainNum);
            if ("WFB".equals(trainType) || "WestfalenBahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "WFB" + trainNum);
            if (("NWB".equals(trainType) || "NordWestBahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "NWB" + trainNum);
            if ("WES".equals(trainType) || "Westbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "WES" + trainNum);
            if ("ERB".equals(trainType) || "eurobahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ERB" + trainNum);
            if ("CAN".equals(trainType) || "cantus Verkehrsgesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "CAN" + trainNum);
            if ("HEX".equals(trainType) || "Veolia Verkehr Sachsen-Anhalt".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "HEX" + trainNum);
            if ("EB".equals(trainType) || "Erfurter Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EB" + trainNum);
            if ("Erfurter Bahn".equals(longName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EB");
            if ("EBx".equals(trainType) || "Erfurter Bahn Express".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EBx" + trainNum);
            if ("Erfurter Bahn Express".equals(longName) && symbol == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "EBx");
            if ("MR".equals(trainType) && "Märkische Regiobahn".equals(trainName) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "MR" + trainNum);
            if ("MRB".equals(trainType) || "Mitteldeutsche Regiobahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "MRB" + trainNum);
            if ("MRB26".equals(trainNum) && trainType == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("ABR".equals(trainType) || "ABELLIO Rail NRW GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ABR" + trainNum);
            if ("NEB".equals(trainType) || "NEB Niederbarnimer Eisenbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "NEB" + trainNum);
            if ("OE".equals(trainType) || "Ostdeutsche Eisenbahn GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "OE" + trainNum);
            if ("Ostdeutsche Eisenbahn GmbH".equals(longName) && symbol == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "OE");
            if ("ODE".equals(trainType) && symbol != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("OLA".equals(trainType) || "Ostseeland Verkehr GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "OLA" + trainNum);
            if ("UBB".equals(trainType) || "Usedomer Bäderbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "UBB" + trainNum);
            if ("EVB".equals(trainType) || "ELBE-WESER GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EVB" + trainNum);
            if ("RTB".equals(trainType) || "Rurtalbahn GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RTB" + trainNum);
            if ("STB".equals(trainType) || "Süd-Thüringen-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "STB" + trainNum);
            if ("HTB".equals(trainType) || "Hellertalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "HTB" + trainNum);
            if ("VBG".equals(trainType) || "Vogtlandbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VBG" + trainNum);
            if ("CB".equals(trainType) || "City-Bahn Chemnitz".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "CB" + trainNum);
            if (trainType == null && ("C11".equals(trainNum) || "C13".equals(trainNum) || "C14".equals(trainNum)
                    || "C15".equals(trainNum)))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("CB523".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("VEC".equals(trainType) || "vectus Verkehrsgesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VEC" + trainNum);
            if ("HzL".equals(trainType) || "Hohenzollerische Landesbahn AG".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "HzL" + trainNum);
            if ("SBB".equals(trainType) || "SBB GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SBB" + trainNum);
            if ("MBB".equals(trainType) || "Mecklenburgische Bäderbahn Molli".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "MBB" + trainNum);
            if ("OS".equals(trainType)) // Osobní vlak
                return new Line(id, network, Product.REGIONAL_TRAIN, "OS" + trainNum);
            if ("SP".equals(trainType) || "Sp".equals(trainType)) // Spěšný vlak
                return new Line(id, network, Product.REGIONAL_TRAIN, "SP" + trainNum);
            if ("Dab".equals(trainType) || "Daadetalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "Dab" + trainNum);
            if ("FEG".equals(trainType) || "Freiberger Eisenbahngesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "FEG" + trainNum);
            if ("ARR".equals(trainType) || "ARRIVA".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ARR" + trainNum);
            if ("HSB".equals(trainType) || "Harzer Schmalspurbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "HSB" + trainNum);
            if ("ALX".equals(trainType) || "alex - Länderbahn und Vogtlandbahn GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ALX" + trainNum);
            if ("EX".equals(trainType) || "Fatra".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EX" + trainNum);
            if ("ME".equals(trainType) || "metronom".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ME" + trainNum);
            if ("metronom".equals(longName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ME");
            if ("MEr".equals(trainType))
                return new Line(id, network, Product.REGIONAL_TRAIN, "MEr" + trainNum);
            if ("AKN".equals(trainType) || "AKN Eisenbahn AG".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "AKN" + trainNum);
            if ("SOE".equals(trainType) || "Sächsisch-Oberlausitzer Eisenbahngesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SOE" + trainNum);
            if ("VIA".equals(trainType) || "VIAS GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VIA" + trainNum);
            if ("BRB".equals(trainType) || "Bayerische Regiobahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "BRB" + trainNum);
            if ("BLB".equals(trainType) || "Berchtesgadener Land Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "BLB" + trainNum);
            if ("HLB".equals(trainType) || "Hessische Landesbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "HLB" + trainNum);
            if ("NOB".equals(trainType) || "NordOstseeBahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "NOB" + trainNum);
            if ("NBE".equals(trainType) || "Nordbahn Eisenbahngesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "NBE" + trainNum);
            if ("VEN".equals(trainType) || "Rhenus Veniro".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VEN" + trainType);
            if ("DPN".equals(trainType) || "Nahreisezug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DPN" + trainNum);
            if ("RBG".equals(trainType) || "Regental Bahnbetriebs GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RBG" + trainNum);
            if ("BOB".equals(trainType) || "Bodensee-Oberschwaben-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "BOB" + trainNum);
            if ("VE".equals(trainType) || "Vetter".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VE" + trainNum);
            if ("SDG".equals(trainType) || "SDG Sächsische Dampfeisenbahngesellschaft mbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SDG" + trainNum);
            if ("PRE".equals(trainType) || "Pressnitztalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "PRE" + trainNum);
            if ("VEB".equals(trainType) || "Vulkan-Eifel-Bahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "VEB" + trainNum);
            if ("neg".equals(trainType) || "Norddeutsche Eisenbahn Gesellschaft".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "neg" + trainNum);
            if ("AVG".equals(trainType) || "Felsenland-Express".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "AVG" + trainNum);
            if ("P".equals(trainType) || "BayernBahn Betriebs-GmbH".equals(trainName)
                    || "Brohltalbahn".equals(trainName) || "Kasbachtalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "P" + trainNum);
            if ("SBS".equals(trainType) || "Städtebahn Sachsen".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SBS" + trainNum);
            if ("SES".equals(trainType) || "Städteexpress Sachsen".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SES" + trainNum);
            if ("SB-".equals(trainType)) // Städtebahn Sachsen
                return new Line(id, network, Product.REGIONAL_TRAIN, "SB" + trainNum);
            if ("ag".equals(trainType)) // agilis
                return new Line(id, network, Product.REGIONAL_TRAIN, "ag" + trainNum);
            if ("agi".equals(trainType) || "agilis".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "agi" + trainNum);
            if ("as".equals(trainType) || "agilis-Schnellzug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "as" + trainNum);
            if ("TLX".equals(trainType) || "TRILEX".equals(trainName)) // Trilex (Vogtlandbahn)
                return new Line(id, network, Product.REGIONAL_TRAIN, "TLX" + trainNum);
            if ("MSB".equals(trainType) || "Mainschleifenbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "MSB" + trainNum);
            if ("BE".equals(trainType) || "Bentheimer Eisenbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "BE" + trainNum);
            if ("erx".equals(trainType) || "erixx - Der Heidesprinter".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "erx" + trainNum);
            if (("ERX".equals(trainType) || "Erixx".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "ERX" + trainNum);
            if ("SWE".equals(trainType) || "Südwestdeutsche Verkehrs-AG".equals(trainName) || "Südwestdeutsche Landesverkehrs-AG".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SWE" + Strings.nullToEmpty(trainNum));
            if ("SWEG-Zug".equals(trainName)) // Südwestdeutschen Verkehrs-Aktiengesellschaft
                return new Line(id, network, Product.REGIONAL_TRAIN, "SWEG" + trainNum);
            if (longName != null && longName.startsWith("SWEG-Zug"))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SWEG" + Strings.nullToEmpty(trainNum));
            if ("EGP Eisenbahngesellschaft Potsdam".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EGP" + trainNumStr);
            if ("ÖBB".equals(trainType) || "ÖBB".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "ÖBB" + trainNum);
            if ("CAT".equals(trainType)) // City Airport Train Wien
                return new Line(id, network, Product.REGIONAL_TRAIN, "CAT" + trainNum);
            if ("DZ".equals(trainType) || "Dampfzug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DZ" + trainNum);
            if ("CD".equals(trainType)) // Tschechien
                return new Line(id, network, Product.REGIONAL_TRAIN, "CD" + trainNum);
            if ("VR".equals(trainType)) // Polen
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("PR".equals(trainType)) // Polen
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("KD".equals(trainType)) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("Koleje Dolnoslaskie".equals(trainName) && symbol != null) // Koleje Dolnośląskie
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("OO".equals(trainType) || "Ordinary passenger (o.pas.)".equals(trainName)) // GB
                return new Line(id, network, Product.REGIONAL_TRAIN, "OO" + trainNum);
            if ("XX".equals(trainType) || "Express passenger    (ex.pas.)".equals(trainName)) // GB
                return new Line(id, network, Product.REGIONAL_TRAIN, "XX" + trainNum);
            if ("XZ".equals(trainType) || "Express passenger sleeper".equals(trainName)) // GB
                return new Line(id, network, Product.REGIONAL_TRAIN, "XZ" + trainNum);
            if ("ATB".equals(trainType)) // Autoschleuse Tauernbahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "ATB" + trainNum);
            if ("ATZ".equals(trainType)) // Autozug
                return new Line(id, network, Product.REGIONAL_TRAIN, "ATZ" + trainNum);
            if ("AZ".equals(trainType) || "Auto-Zug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "AZ" + trainNum);
            if ("AZS".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "AZS" + trainNum);
            if ("DWE".equals(trainType) || "Dessau-Wörlitzer Eisenbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DWE" + trainNum);
            if ("KTB".equals(trainType) || "Kandertalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "KTB" + trainNum);
            if ("CBC".equals(trainType) || "CBC".equals(trainName)) // City-Bahn Chemnitz
                return new Line(id, network, Product.REGIONAL_TRAIN, "CBC" + trainNum);
            if ("Bernina Express".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
            if ("STR".equals(trainType)) // Harzquerbahn, Nordhausen
                return new Line(id, network, Product.REGIONAL_TRAIN, "STR" + trainNum);
            if ("EXT".equals(trainType) || "Extrazug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "EXT" + trainNum);
            if ("Heritage Railway".equals(trainName)) // GB
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("WTB".equals(trainType) || "Wutachtalbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "WTB" + trainNum);
            if ("DB".equals(trainType) || "DB Regio".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DB" + trainNum);
            if ("M".equals(trainType) && "Meridian".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "M" + trainNum);
            if ("M".equals(trainType) && "Messezug".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "M" + trainNum);
            if ("EZ".equals(trainType)) // ÖBB Erlebniszug
                return new Line(id, network, Product.REGIONAL_TRAIN, "EZ" + trainNum);
            if ("DPF".equals(trainType))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DPF" + trainNum);
            if ("WBA".equals(trainType) || "Waldbahn".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "WBA" + trainNum);
            if ("ÖB".equals(trainType) && "Öchsle-Bahn-Betriebsgesellschaft mbH".equals(trainName) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "ÖB" + trainNum);
            if ("ÖBA".equals(trainType) && trainNum != null) // Eisenbahn-Betriebsgesellschaft Ochsenhausen
                return new Line(id, network, Product.REGIONAL_TRAIN, "ÖBA" + trainNum);
            if (("UEF".equals(trainType) || "Ulmer Eisenbahnfreunde".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "UEF" + trainNum);
            if (("DBG".equals(trainType) || "Döllnitzbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "DBG" + trainNum);
            if (("TL".equals(trainType) || "Trilex".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "TL" + trainNum);
            if (("OPB".equals(trainType) || "oberpfalzbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "OPB" + trainNum);
            if (("OPX".equals(trainType) || "oberpfalz-express".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "OPX" + trainNum);
            if (("LEO".equals(trainType) || "Chiemgauer Lokalbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "LEO" + trainNum);
            if (("VAE".equals(trainType) || "Voralpen-Express".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "VAE" + trainNum);
            if (("V6".equals(trainType) || "vlexx".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "vlexx" + trainNum);
            if (("ARZ".equals(trainType) || "Autoreisezug".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "ARZ" + trainNum);
            if ("RR".equals(trainType))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RR" + Strings.nullToEmpty(trainNum));
            if (("TER".equals(trainType) || "Train Express Regional".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "TER" + trainNum);
            if (("ENO".equals(trainType) || "enno".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "ENO" + trainNum);
            if ("enno".equals(longName) && symbol == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "enno");
            if (("PLB".equals(trainType) || "Pinzgauer Lokalbahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "PLB" + trainNum);
            if (("NX".equals(trainType) || "National Express".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "NX" + trainNum);
            if (("SE".equals(trainType) || "ABELLIO Rail Mitteldeutschland GmbH".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "SE" + trainNum);
            if (("DNA".equals(trainType) && trainNum != null)) // Dieselnetz Augsburg
                return new Line(id, network, Product.REGIONAL_TRAIN, "DNA" + trainNum);
            if ("Dieselnetz".equals(trainType) && "Augsburg".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "DNA");
            if (("SAB".equals(trainType) || "Schwäbische Alb-Bahn".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "SAB" + trainNum);
            if (trainType == null && trainNum != null && P_LINE_MEX.matcher(trainNum).matches()) // Metropolexpress
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);

            if (("BSB".equals(trainType) || "Breisgau-S-Bahn Gmbh".equals(trainName)) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "BSB" + trainNum);
            if ("BSB-Zug".equals(trainName) && trainNum != null) // Breisgau-S-Bahn
                return new Line(id, network, Product.SUBURBAN_TRAIN, trainNum);
            if ("BSB-Zug".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.SUBURBAN_TRAIN, "BSB");
            if (longName != null && longName.startsWith("BSB-Zug"))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "BSB" + Strings.nullToEmpty(trainNum));
            if ("RSB".equals(trainType)) // Regionalschnellbahn, Wien
                return new Line(id, network, Product.SUBURBAN_TRAIN, "RSB" + trainNum);
            if ("RER".equals(trainName) && symbol != null && symbol.length() == 1) // Réseau Express Régional
                return new Line(id, network, Product.SUBURBAN_TRAIN, symbol);
            if ("S".equals(trainType))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "S" + trainNum);
            if ("S-Bahn".equals(trainName))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "S" + trainNumStr);

            if ("RT".equals(trainType) || "RegioTram".equals(trainName))
                return new Line(id, network, Product.TRAM, "RT" + trainNum);

            if ("Bus".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.BUS, trainNum);
            if ("Bus".equals(longName) && symbol == null)
                return new Line(id, network, Product.BUS, longName);
            if ("SEV".equals(trainType) || "SEV".equals(trainNum) || "SEV".equals(trainName) || "SEV".equals(symbol)
                    || "BSV".equals(trainType) || "Ersatzverkehr".equals(trainName)
                    || "Schienenersatzverkehr".equals(trainName))
                return new Line(id, network, Product.BUS, "SEV" + trainNumStr);
            if ("Bus replacement".equals(trainName)) // GB
                return new Line(id, network, Product.BUS, "BR");
            if ("BR".equals(trainType) && trainName != null && trainName.startsWith("Bus")) // GB
                return new Line(id, network, Product.BUS, "BR" + trainNum);
            if ("EXB".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.BUS, "EXB" + trainNum);

            if ("GB".equals(trainType)) // Gondelbahn
                return new Line(id, network, Product.CABLECAR, "GB" + trainNum);
            if ("SB".equals(trainType)) // Seilbahn
                return new Line(id, network, Product.SUBURBAN_TRAIN, "SB" + trainNum);

            if ("Zug".equals(trainName) && symbol != null)
                return new Line(id, network, null, symbol);
            if ("Zug".equals(longName) && symbol == null)
                return new Line(id, network, null, "Zug");
            if ("Zuglinie".equals(trainName) && symbol != null)
                return new Line(id, network, null, symbol);
            if ("ZUG".equals(trainType) && trainNum != null)
                return new Line(id, network, null, trainNum);
            if (symbol != null && P_LINE_NUMBER.matcher(symbol).matches() && trainType == null && trainName == null)
                return new Line(id, network, null, symbol);
            if ("N".equals(trainType) && trainName == null && symbol == null)
                return new Line(id, network, null, "N" + trainNum);
            if ("Train".equals(trainName))
                return new Line(id, network, null, null);
            if ("PPN".equals(trainType) && "Osobowy".equals(trainName) && trainNum != null)
                return new Line(id, network, null, "PPN" + trainNum);

            // generic
            if (trainName != null && trainType == null && trainNum == null)
                return new Line(id, network, null, trainName);
        } else if ("1".equals(mot)) {
            if (symbol != null && P_LINE_S.matcher(symbol).matches())
                return new Line(id, network, Product.SUBURBAN_TRAIN, symbol);
            if (name != null && P_LINE_S.matcher(name).matches())
                return new Line(id, network, Product.SUBURBAN_TRAIN, name);
            if ("S-Bahn".equals(trainName))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "S" + Strings.nullToEmpty(trainNum));
            if (symbol != null && symbol.equals(name)) {
                final Matcher m = P_LINE_S_DB.matcher(symbol);
                if (m.matches())
                    return new Line(id, network, Product.SUBURBAN_TRAIN, m.group(1));
            }
            if ("REX".equals(trainType))
                return new Line(id, network, Product.REGIONAL_TRAIN, "REX" + Strings.nullToEmpty(trainNum));
            return new Line(id, network, Product.SUBURBAN_TRAIN, ParserUtils.firstNotEmpty(symbol, name));
        } else if ("2".equals(mot)) {
            return new Line(id, network, Product.SUBWAY, name);
        } else if ("3".equals(mot) || "4".equals(mot)) {
            return new Line(id, network, Product.TRAM, name);
        } else if ("5".equals(mot) || "6".equals(mot) || "7".equals(mot)) {
            if ("Schienenersatzverkehr".equals(name))
                return new Line(id, network, Product.BUS, "SEV");
            return new Line(id, network, Product.BUS, name);
        } else if ("8".equals(mot)) {
            return new Line(id, network, Product.CABLECAR, name);
        } else if ("9".equals(mot)) {
            return new Line(id, network, Product.FERRY, name);
        } else if ("10".equals(mot)) {
            return new Line(id, network, Product.ON_DEMAND, name);
        } else if ("11".equals(mot)) {
            return new Line(id, network, null, ParserUtils.firstNotEmpty(symbol, name));
        } else if ("12".equals(mot)) {
            if ("Schulbus".equals(trainName) && symbol != null)
                return new Line(id, network, Product.BUS, symbol);
        } else if ("13".equals(mot)) {
            if (("SEV".equals(trainName) || "Ersatzverkehr".equals(trainName)) && trainType == null)
                return new Line(id, network, Product.BUS, "SEV");
            if (trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, Strings.nullToEmpty(trainType) + trainNum);
            return new Line(id, network, Product.REGIONAL_TRAIN, name);
        } else if ("14".equals(mot) || "15".equals(mot) || "16".equals(mot)) {
            if (trainType != null || trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, Strings.nullToEmpty(trainType) + Strings.nullToEmpty(trainNum));
            return new Line(id, network, Product.HIGH_SPEED_TRAIN, name);
        } else if ("17".equals(mot)) { // Schienenersatzverkehr
            if (trainNum == null && trainName != null && trainName.startsWith("Schienenersatz"))
                return new Line(id, network, Product.BUS, "SEV");
            return new Line(id, network, Product.BUS, name);
        } else if ("18".equals(mot)) { // Zug-Shuttle
            return new Line(id, network, Product.REGIONAL_TRAIN, name);
        } else if ("19".equals(mot)) { // Bürgerbus
            if (("Bürgerbus".equals(trainName) || "BürgerBus".equals(trainName) || "Kleinbus".equals(trainName)) && symbol != null)
                return new Line(id, network, Product.BUS, symbol);
            return new Line(id, network, Product.BUS, name);
        }

        throw new IllegalStateException(
                "cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name + "' long='" + longName
                        + "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='" + trainName + "'");
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        checkNotNull(Strings.emptyToNull(stationId));

        return xsltDepartureMonitorRequest(stationId, time, maxDepartures, equivs);
    }

    protected void appendDepartureMonitorRequestParameters(final HttpUrl.Builder url, final String stationId,
            final @Nullable Date time, final int maxDepartures, final boolean equivs) {
        appendCommonRequestParams(url, "XML");
        url.addEncodedQueryParameter("type_dm", "stop");
        url.addEncodedQueryParameter("name_dm",
                ParserUtils.urlEncode(normalizeStationId(stationId), requestUrlEncoding));
        if (time != null)
            appendItdDateTimeParameters(url, time);
        url.addEncodedQueryParameter("useRealtime", "1");
        url.addEncodedQueryParameter("mode", "direct");
        url.addEncodedQueryParameter("ptOptionsActive", "1");
        url.addEncodedQueryParameter("deleteAssignedStops_dm", equivs ? "0" : "1");
        if (useProxFootSearch)
            url.addEncodedQueryParameter("useProxFootSearch", equivs ? "1" : "0");
        url.addEncodedQueryParameter("mergeDep", "1"); // merge departures
        if (maxDepartures > 0)
            url.addEncodedQueryParameter("limit", Integer.toString(maxDepartures));
    }

    private final void appendItdDateTimeParameters(final HttpUrl.Builder url, final Date time) {
        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);
        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);
        url.addEncodedQueryParameter("itdDate", String.format(Locale.ENGLISH, "%04d%02d%02d", year, month, day));
        url.addEncodedQueryParameter("itdTime", String.format(Locale.ENGLISH, "%02d%02d", hour, minute));
    }

    private QueryDeparturesResult xsltDepartureMonitorRequest(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        final HttpUrl.Builder url = departureMonitorEndpoint.newBuilder();
        appendDepartureMonitorRequestParameters(url, stationId, time, maxDepartures, equivs);
        final AtomicReference<QueryDeparturesResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterItdRequest(pp);

                final QueryDeparturesResult r = new QueryDeparturesResult(header);

                XmlPullUtil.enter(pp, "itdDepartureMonitorRequest");
                XmlPullUtil.optSkipMultiple(pp, "itdMessage");

                final String nameState = processItdOdv(pp, "dm", (nameState1, location, matchQuality) -> {
                    if (location.type == LocationType.STATION)
                        if (findStationDepartures(r.stationDepartures, location.id) == null)
                            r.stationDepartures.add(new StationDepartures(location, new LinkedList<Departure>(), new LinkedList<LineDestination>()));
                });

                if (!"identified".equals(nameState)) {
                    result.set(new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION));
                    return;
                }

                XmlPullUtil.optSkip(pp, "itdDateTime");

                XmlPullUtil.optSkip(pp, "itdDMDateTime");

                XmlPullUtil.optSkip(pp, "itdDateRange");

                XmlPullUtil.optSkip(pp, "itdTripOptions");

                XmlPullUtil.optSkip(pp, "itdMessage");

                if (XmlPullUtil.test(pp, "itdServingLines")) {
                    if (!pp.isEmptyElementTag()) {
                        XmlPullUtil.enter(pp, "itdServingLines");
                        while (XmlPullUtil.test(pp, "itdServingLine")) {
                            final String assignedStopId = XmlPullUtil.optAttr(pp, "assignedStopID", null);
                            final LineDestinationAndCancelled lineDestinationAndCancelled = processItdServingLine(
                                    pp);
                            final LineDestination lineDestination = new LineDestination(
                                    lineDestinationAndCancelled.line, lineDestinationAndCancelled.destination);

                            StationDepartures assignedStationDepartures;
                            if (assignedStopId == null)
                                assignedStationDepartures = r.stationDepartures.get(0);
                            else
                                assignedStationDepartures = findStationDepartures(r.stationDepartures,
                                        assignedStopId);

                            if (assignedStationDepartures == null)
                                assignedStationDepartures = new StationDepartures(
                                        new Location(LocationType.STATION, assignedStopId),
                                        new LinkedList<Departure>(), new LinkedList<LineDestination>());

                            final List<LineDestination> assignedStationDeparturesLines = checkNotNull(
                                    assignedStationDepartures.lines);
                            if (!assignedStationDeparturesLines.contains(lineDestination))
                                assignedStationDeparturesLines.add(lineDestination);
                        }
                        XmlPullUtil.skipExit(pp, "itdServingLines");
                    } else {
                        XmlPullUtil.next(pp);
                    }
                } else {
                    result.set(new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION));
                    return;
                }

                XmlPullUtil.require(pp, "itdDepartureList");
                if (XmlPullUtil.optEnter(pp, "itdDepartureList")) {
                    final Calendar plannedDepartureTime = new GregorianCalendar(timeZone);
                    final Calendar predictedDepartureTime = new GregorianCalendar(timeZone);

                    while (XmlPullUtil.test(pp, "itdDeparture")) {
                        final String assignedStopId = XmlPullUtil.attr(pp, "stopID");

                        StationDepartures assignedStationDepartures = findStationDepartures(r.stationDepartures,
                                assignedStopId);
                        if (assignedStationDepartures == null) {
                            final Point coord = processCoordAttr(pp);

                            // final String name = normalizeLocationName(XmlPullUtil.attr(pp, "nameWO"));

                            assignedStationDepartures = new StationDepartures(
                                    new Location(LocationType.STATION, assignedStopId, coord),
                                    new LinkedList<Departure>(), new LinkedList<LineDestination>());
                        }

                        final Position position = parsePosition(XmlPullUtil.optAttr(pp, "platformName", null));

                        XmlPullUtil.enter(pp, "itdDeparture");

                        XmlPullUtil.require(pp, "itdDateTime");
                        plannedDepartureTime.clear();
                        processItdDateTime(pp, plannedDepartureTime);

                        predictedDepartureTime.clear();
                        if (XmlPullUtil.test(pp, "itdRTDateTime"))
                            processItdDateTime(pp, predictedDepartureTime);

                        XmlPullUtil.optSkip(pp, "itdFrequencyInfo");

                        XmlPullUtil.require(pp, "itdServingLine");
                        final boolean isRealtime = XmlPullUtil.attr(pp, "realtime").equals("1");
                        final LineDestinationAndCancelled lineDestinationAndCancelled = processItdServingLine(pp);

                        if (isRealtime && !predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY))
                            predictedDepartureTime.setTimeInMillis(plannedDepartureTime.getTimeInMillis());

                        XmlPullUtil.skipExit(pp, "itdDeparture");

                        if (!lineDestinationAndCancelled.cancelled) {
                            final Departure departure = new Departure(plannedDepartureTime.getTime(),
                                    predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY)
                                            ? predictedDepartureTime.getTime() : null,
                                    lineDestinationAndCancelled.line, position,
                                    lineDestinationAndCancelled.destination, null, null);
                            assignedStationDepartures.departures.add(departure);
                        }
                    }

                    XmlPullUtil.skipExit(pp, "itdDepartureList");
                }

                result.set(r);
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    protected QueryDeparturesResult queryDeparturesMobile(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        final HttpUrl.Builder url = departureMonitorEndpoint.newBuilder();
        appendDepartureMonitorRequestParameters(url, stationId, time, maxDepartures, equivs);
        final AtomicReference<QueryDeparturesResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                final XmlPullParser pp = parserFactory.newPullParser();
                pp.setInput(body.charStream());
                final ResultHeader header = enterEfa(pp);
                final QueryDeparturesResult r = new QueryDeparturesResult(header);

                if (XmlPullUtil.optEnter(pp, "ers")) {
                    XmlPullUtil.enter(pp, "err");
                    final String mod = XmlPullUtil.valueTag(pp, "mod");
                    final String co = XmlPullUtil.valueTag(pp, "co");
                    XmlPullUtil.optValueTag(pp, "u", null);
                    XmlPullUtil.optValueTag(pp, "tx", null);
                    if ("-2000".equals(co)) { // STOP_INVALID
                        result.set(new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION));
                    } else if ("-4050".equals(co)) { // NO_SERVINGLINES
                        result.set(r);
                    } else {
                        log.debug("EFA error: {} {}", co, mod);
                        result.set(new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN));
                    }
                    XmlPullUtil.exit(pp, "err");
                    XmlPullUtil.exit(pp, "ers");
                } else if (XmlPullUtil.optEnter(pp, "dps")) {
                    final Calendar plannedDepartureTime = new GregorianCalendar(timeZone);
                    final Calendar predictedDepartureTime = new GregorianCalendar(timeZone);

                    while (XmlPullUtil.optEnter(pp, "dp")) {
                        // misc
                        /* final String stationName = */normalizeLocationName(XmlPullUtil.valueTag(pp, "n"));
                        /* final String gid = */XmlPullUtil.optValueTag(pp, "gid", null);
                        /* final String pgid = */XmlPullUtil.optValueTag(pp, "pgid", null);
                        /* final boolean isRealtime = */XmlPullUtil.valueTag(pp, "realtime").equals("1");
                        /* final String rts = */XmlPullUtil.optValueTag(pp, "rts", null);

                        XmlPullUtil.optSkip(pp, "dt");

                        // time
                        parseMobileSt(pp, plannedDepartureTime, predictedDepartureTime);

                        final LineDestination lineDestination = parseMobileM(pp, true);

                        XmlPullUtil.enter(pp, "r");
                        final String assignedId = XmlPullUtil.valueTag(pp, "id");
                        XmlPullUtil.valueTag(pp, "a");
                        final Position position = parsePosition(XmlPullUtil.optValueTag(pp, "pl", null));
                        XmlPullUtil.skipExit(pp, "r");

                        /* final Point positionCoordinate = */parseCoord(XmlPullUtil.optValueTag(pp, "c", null));

                        // TODO messages

                        StationDepartures stationDepartures = findStationDepartures(r.stationDepartures,
                                assignedId);
                        if (stationDepartures == null) {
                            stationDepartures = new StationDepartures(
                                    new Location(LocationType.STATION, assignedId),
                                    new ArrayList<Departure>(maxDepartures), null);
                            r.stationDepartures.add(stationDepartures);
                        }

                        stationDepartures.departures.add(new Departure(plannedDepartureTime.getTime(),
                                predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY)
                                        ? predictedDepartureTime.getTime() : null,
                                lineDestination.line, position, lineDestination.destination, null, null));

                        XmlPullUtil.skipExit(pp, "dp");
                    }

                    XmlPullUtil.skipExit(pp, "dps");

                    result.set(r);
                } else {
                    result.set(new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION));
                }
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpReferer);

        return result.get();
    }

    private static final Pattern P_MOBILE_M_SYMBOL = Pattern.compile("([^\\s]*)\\s+([^\\s]*)");

    private LineDestination parseMobileM(final XmlPullParser pp, final boolean tyOrCo)
            throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "m");

        final String n = XmlPullUtil.optValueTag(pp, "n", null);
        final String productNu = XmlPullUtil.valueTag(pp, "nu");
        final String ty = XmlPullUtil.valueTag(pp, "ty");

        final Line line;
        final Location destination;
        if ("100".equals(ty) || "99".equals(ty)) {
            destination = null;
            line = Line.FOOTWAY;
        } else if ("105".equals(ty)) {
            destination = null;
            line = Line.TRANSFER;
        } else if ("98".equals(ty)) {
            destination = null;
            line = Line.SECURE_CONNECTION;
        } else if ("97".equals(ty)) {
            destination = null;
            line = Line.DO_NOT_CHANGE;
        } else {
            final String co = XmlPullUtil.valueTag(pp, "co");
            final String productType = tyOrCo ? ty : co;
            XmlPullUtil.optValueTag(pp, "prid", null);
            XmlPullUtil.optValueTag(pp, "trainType", null);
            final String destinationName = normalizeLocationName(XmlPullUtil.optValueTag(pp, "des", null));
            destination = destinationName != null ? new Location(LocationType.ANY, null, null, destinationName) : null;
            XmlPullUtil.optValueTag(pp, "dy", null);
            final String de = XmlPullUtil.optValueTag(pp, "de", null);
            final String productName = n != null ? n : de;
            XmlPullUtil.optValueTag(pp, "routeDesc", null);
            XmlPullUtil.optValueTag(pp, "tco", null);
            final String lineId = parseMobileDv(pp);

            final String symbol;
            if (productName != null && productNu == null)
                symbol = productName;
            else if (productName != null && productNu.endsWith(" " + productName))
                symbol = productNu.substring(0, productNu.length() - productName.length() - 1);
            else
                symbol = productNu;

            final String trainType;
            final String trainNum;
            final Matcher mSymbol = P_MOBILE_M_SYMBOL.matcher(symbol);
            if (mSymbol.matches()) {
                trainType = mSymbol.group(1);
                trainNum = mSymbol.group(2);
            } else {
                trainType = null;
                trainNum = null;
            }

            final String network = lineId.substring(0, lineId.indexOf(':'));
            final Line parsedLine = parseLine(lineId, network, productType, symbol, symbol, null, trainType, trainNum,
                    productName);
            line = new Line(parsedLine.id, parsedLine.network, parsedLine.product, parsedLine.label,
                    lineStyle(parsedLine.network, parsedLine.product, parsedLine.label));
        }

        XmlPullUtil.skipExit(pp, "m");

        return new LineDestination(line, destination);
    }

    private String parseMobileDv(final XmlPullParser pp) throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "dv");
        XmlPullUtil.optValueTag(pp, "branch", null);
        final String lineIdLi = XmlPullUtil.valueTag(pp, "li");
        final String lineIdSu = XmlPullUtil.valueTag(pp, "su");
        final String lineIdPr = XmlPullUtil.valueTag(pp, "pr");
        final String lineIdDct = XmlPullUtil.valueTag(pp, "dct");
        final String lineIdNe = XmlPullUtil.valueTag(pp, "ne");
        XmlPullUtil.skipExit(pp, "dv");

        return lineIdNe + ":" + lineIdLi + ":" + lineIdSu + ":" + lineIdDct + ":" + lineIdPr;
    }

    private void parseMobileSt(final XmlPullParser pp, final Calendar plannedDepartureTime,
            final Calendar predictedDepartureTime) throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "st");

        plannedDepartureTime.clear();
        ParserUtils.parseIsoDate(plannedDepartureTime, XmlPullUtil.valueTag(pp, "da"));
        ParserUtils.parseIsoTime(plannedDepartureTime, XmlPullUtil.valueTag(pp, "t"));

        predictedDepartureTime.clear();
        if (XmlPullUtil.test(pp, "rda")) {
            ParserUtils.parseIsoDate(predictedDepartureTime, XmlPullUtil.valueTag(pp, "rda"));
            ParserUtils.parseIsoTime(predictedDepartureTime, XmlPullUtil.valueTag(pp, "rt"));
        }

        XmlPullUtil.skipExit(pp, "st");
    }

    private StationDepartures findStationDepartures(final List<StationDepartures> stationDepartures, final String id) {
        for (final StationDepartures stationDeparture : stationDepartures)
            if (id.equals(stationDeparture.location.id))
                return stationDeparture;

        return null;
    }

    private Location processItdPointAttributes(final XmlPullParser pp) {
        final String id = XmlPullUtil.attr(pp, "stopID");

        String place = normalizeLocationName(XmlPullUtil.optAttr(pp, "locality", null));
        if (place == null)
            place = normalizeLocationName(XmlPullUtil.optAttr(pp, "place", null));

        String name = normalizeLocationName(XmlPullUtil.optAttr(pp, "nameWO", null));
        if (name == null)
            name = normalizeLocationName(XmlPullUtil.optAttr(pp, "name", null));

        final Point coord = processCoordAttr(pp);

        return new Location(LocationType.STATION, id, coord, place, name);
    }

    private boolean processItdDateTime(final XmlPullParser pp, final Calendar calendar)
            throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp);
        calendar.clear();
        final boolean success = processItdDate(pp, calendar);
        if (success)
            processItdTime(pp, calendar);
        XmlPullUtil.skipExit(pp);

        return success;
    }

    private boolean processItdDate(final XmlPullParser pp, final Calendar calendar)
            throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "itdDate");
        final int year = XmlPullUtil.intAttr(pp, "year");
        final int month = XmlPullUtil.intAttr(pp, "month") - 1;
        final int day = XmlPullUtil.intAttr(pp, "day");
        final int weekday = XmlPullUtil.intAttr(pp, "weekday");
        XmlPullUtil.next(pp);

        if (weekday < 0)
            return false;
        if (year == 0)
            return false;
        if (year < 1900 || year > 2100)
            throw new InvalidDataException("invalid year: " + year);
        if (month < 0 || month > 11)
            throw new InvalidDataException("invalid month: " + month);
        if (day < 1 || day > 31)
            throw new InvalidDataException("invalid day: " + day);

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return true;
    }

    private void processItdTime(final XmlPullParser pp, final Calendar calendar)
            throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "itdTime");
        calendar.set(Calendar.HOUR_OF_DAY, XmlPullUtil.intAttr(pp, "hour"));
        calendar.set(Calendar.MINUTE, XmlPullUtil.intAttr(pp, "minute"));
        XmlPullUtil.next(pp);
    }

    private static class LineDestinationAndCancelled {
        public final Line line;
        public final Location destination;
        public final boolean cancelled;

        public LineDestinationAndCancelled(final Line line, final Location destination, final boolean cancelled) {
            this.line = line;
            this.destination = destination;
            this.cancelled = cancelled;
        }
    }

    private LineDestinationAndCancelled processItdServingLine(final XmlPullParser pp)
            throws XmlPullParserException, IOException {
        XmlPullUtil.require(pp, "itdServingLine");

        final String destinationName = normalizeLocationName(XmlPullUtil.optAttr(pp, "direction", null));
        final String destinationIdStr = XmlPullUtil.optAttr(pp, "destID", null);
        final String destinationId = !"-1".equals(destinationIdStr) ? destinationIdStr : null;
        final Location destination;
        if (destinationId != null)
            destination = new Location(LocationType.STATION, destinationId, null, destinationName);
        else if (destinationId == null && destinationName != null)
            destination = new Location(LocationType.ANY, null, null, destinationName);
        else
            destination = null;

        final String slMotType = XmlPullUtil.attr(pp, "motType");
        final String slSymbol = XmlPullUtil.optAttr(pp, "symbol", null);
        final String slNumber = XmlPullUtil.optAttr(pp, "number", null);
        final String slStateless = XmlPullUtil.optAttr(pp, "stateless", null);
        final String slTrainType = XmlPullUtil.optAttr(pp, "trainType", null);
        final String slTrainName = XmlPullUtil.optAttr(pp, "trainName", null);
        final String slTrainNum = XmlPullUtil.optAttr(pp, "trainNum", null);

        XmlPullUtil.enter(pp, "itdServingLine");
        String itdTrainName = null;
        String itdTrainType = null;
        String itdMessage = null;
        String itdDelay = null;
        if (XmlPullUtil.test(pp, "itdTrain")) {
            itdTrainName = XmlPullUtil.optAttr(pp, "name", null);
            itdTrainType = XmlPullUtil.attr(pp, "type");
            itdDelay = XmlPullUtil.optAttr(pp, "delay", null);
            XmlPullUtil.requireSkip(pp, "itdTrain");
        }
        if (XmlPullUtil.test(pp, "itdNoTrain")) {
            itdTrainName = XmlPullUtil.optAttr(pp, "name", null);
            itdTrainType = XmlPullUtil.optAttr(pp, "type", null);
            itdDelay = XmlPullUtil.optAttr(pp, "delay", null);
            if (!pp.isEmptyElementTag()) {
                final String text = XmlPullUtil.valueTag(pp, "itdNoTrain");
                if (itdTrainName != null && itdTrainName.toLowerCase().contains("ruf"))
                    itdMessage = text;
                else if (text != null && text.toLowerCase().contains("ruf"))
                    itdMessage = text;
            } else {
                XmlPullUtil.next(pp);
            }
        }

        XmlPullUtil.require(pp, "motDivaParams");
        final String divaNetwork = XmlPullUtil.optAttr(pp, "network", null);

        XmlPullUtil.skipExit(pp, "itdServingLine");

        final String trainType = ParserUtils.firstNotEmpty(slTrainType, itdTrainType);
        final String trainName = ParserUtils.firstNotEmpty(slTrainName, itdTrainName);
        final Line slLine = parseLine(slStateless, divaNetwork, slMotType, slSymbol, slNumber, slNumber, trainType,
                slTrainNum, trainName);

        final Line line = new Line(slLine.id, slLine.network, slLine.product, slLine.label,
                lineStyle(slLine.network, slLine.product, slLine.label), itdMessage);
        final boolean cancelled = "-9999".equals(itdDelay);
        return new LineDestinationAndCancelled(line, destination, cancelled);
    }

    private static final Pattern P_STATION_NAME_WHITESPACE = Pattern.compile("\\s+");

    protected String normalizeLocationName(final String name) {
        if (Strings.isNullOrEmpty(name))
            return null;

        return P_STATION_NAME_WHITESPACE.matcher(name).replaceAll(" ");
    }

    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            @Nullable TripOptions options) {
        appendCommonRequestParams(url, "XML");

        url.addEncodedQueryParameter("sessionID", "0");
        url.addEncodedQueryParameter("requestID", "0");

        appendCommonTripRequestParams(url);

        appendLocationParams(url, from, "origin");
        appendLocationParams(url, to, "destination");
        if (via != null)
            appendLocationParams(url, via, "via");

        appendItdDateTimeParameters(url, time);

        url.addEncodedQueryParameter("itdTripDateTimeDepArr", dep ? "dep" : "arr");

        url.addEncodedQueryParameter("calcNumberOfTrips", Integer.toString(numTripsRequested));

        url.addEncodedQueryParameter("ptOptionsActive", "1"); // enable public transport options
        url.addEncodedQueryParameter("itOptionsActive", "1"); // enable individual transport options

        if (options == null)
            options = new TripOptions();

        if (options.optimize == Optimize.LEAST_DURATION)
            url.addEncodedQueryParameter("routeType", "LEASTTIME");
        else if (options.optimize == Optimize.LEAST_CHANGES)
            url.addEncodedQueryParameter("routeType", "LEASTINTERCHANGE");
        else if (options.optimize == Optimize.LEAST_WALKING)
            url.addEncodedQueryParameter("routeType", "LEASTWALKING");
        else if (options.optimize != null)
            log.info("Cannot handle " + options.optimize + ", ignoring.");

        url.addEncodedQueryParameter("changeSpeed", WALKSPEED_MAP.get(options.walkSpeed));

        if (options.accessibility == Accessibility.BARRIER_FREE)
            url.addEncodedQueryParameter("imparedOptionsActive", "1").addEncodedQueryParameter("wheelchair", "on")
                    .addEncodedQueryParameter("noSolidStairs", "on");
        else if (options.accessibility == Accessibility.LIMITED)
            url.addEncodedQueryParameter("imparedOptionsActive", "1").addEncodedQueryParameter("wheelchair", "on")
                    .addEncodedQueryParameter("lowPlatformVhcl", "on").addEncodedQueryParameter("noSolidStairs", "on");

        if (options.products != null) {
            url.addEncodedQueryParameter("includedMeans", "checkbox");

            boolean hasI = false;
            for (final Product p : options.products) {
                if (p == Product.HIGH_SPEED_TRAIN || p == Product.REGIONAL_TRAIN) {
                    url.addEncodedQueryParameter("inclMOT_0", "on");
                    if (p == Product.HIGH_SPEED_TRAIN)
                        hasI = true;
                }

                if (p == Product.HIGH_SPEED_TRAIN)
                    url.addEncodedQueryParameter("inclMOT_14", "on").addEncodedQueryParameter("inclMOT_15", "on")
                            .addEncodedQueryParameter("inclMOT_16", "on");

                if (p == Product.REGIONAL_TRAIN)
                    url.addEncodedQueryParameter("inclMOT_13", "on").addEncodedQueryParameter("inclMOT_18", "on");

                if (p == Product.SUBURBAN_TRAIN)
                    url.addEncodedQueryParameter("inclMOT_1", "on");

                if (p == Product.SUBWAY)
                    url.addEncodedQueryParameter("inclMOT_2", "on");

                if (p == Product.TRAM)
                    url.addEncodedQueryParameter("inclMOT_3", "on").addEncodedQueryParameter("inclMOT_4", "on");

                if (p == Product.BUS)
                    url.addEncodedQueryParameter("inclMOT_5", "on").addEncodedQueryParameter("inclMOT_6", "on")
                            .addEncodedQueryParameter("inclMOT_7", "on").addEncodedQueryParameter("inclMOT_17", "on")
                            .addEncodedQueryParameter("inclMOT_19", "on");

                if (p == Product.ON_DEMAND)
                    url.addEncodedQueryParameter("inclMOT_10", "on");

                if (p == Product.FERRY)
                    url.addEncodedQueryParameter("inclMOT_9", "on");

                if (p == Product.CABLECAR)
                    url.addEncodedQueryParameter("inclMOT_8", "on");
            }

            // workaround for highspeed trains: fails when you want highspeed, but not regional
            if (useLineRestriction && !hasI)
                url.addEncodedQueryParameter("lineRestriction", "403"); // means: all but ice
        }

        if (useProxFootSearch)
            url.addEncodedQueryParameter("useProxFootSearch", "1"); // walk if it makes journeys quicker
        url.addEncodedQueryParameter("trITMOTvalue100", "10"); // maximum time to walk to first or from last
                                                               // stop

        if (options.flags != null && options.flags.contains(TripFlag.BIKE))
            url.addEncodedQueryParameter("bikeTakeAlong", "1");

        url.addEncodedQueryParameter("locationServerActive", "1");
        url.addEncodedQueryParameter("useRealtime", "1");
        url.addEncodedQueryParameter("nextDepsPerLeg", "1"); // next departure in case previous was missed
    }

    private HttpUrl commandLink(final String sessionId, final String requestId) {
        final HttpUrl.Builder url = tripEndpoint.newBuilder();
        url.addEncodedQueryParameter("sessionID", sessionId);
        url.addEncodedQueryParameter("requestID", requestId);
        url.addEncodedQueryParameter("calcNumberOfTrips", Integer.toString(numTripsRequested));
        appendCommonTripRequestParams(url);
        return url.build();
    }

    private final void appendCommonTripRequestParams(final HttpUrl.Builder url) {
        url.addEncodedQueryParameter("coordListOutputFormat", useStringCoordListOutputFormat ? "string" : "list");
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        final HttpUrl.Builder url = tripEndpoint.newBuilder();
        appendTripRequestParameters(url, from, via, to, date, dep, options);
        final AtomicReference<QueryTripsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                result.set(queryTrips(url.build(), body.charStream()));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            } catch (final RuntimeException x) {
                throw new RuntimeException("uncategorized problem while processing " + url, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpRefererTrip);

        return result.get();
    }

    protected QueryTripsResult queryTripsMobile(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        final HttpUrl.Builder url = tripEndpoint.newBuilder();
        appendTripRequestParameters(url, from, via, to, date, dep, options);
        final AtomicReference<QueryTripsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                result.set(queryTripsMobile(url.build(), from, via, to, body.charStream()));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            } catch (final RuntimeException x) {
                throw new RuntimeException("uncategorized problem while processing " + url, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpRefererTrip);

        return result.get();
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException {
        final Context context = (Context) contextObj;
        final HttpUrl commandUrl = HttpUrl.parse(context.context);
        final HttpUrl.Builder url = commandUrl.newBuilder();
        appendCommonRequestParams(url, "XML");
        url.addEncodedQueryParameter("command", later ? "tripNext" : "tripPrev");
        final AtomicReference<QueryTripsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                result.set(queryTrips(url.build(), body.charStream()));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            } catch (final RuntimeException x) {
                throw new RuntimeException("uncategorized problem while processing " + url, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpRefererTrip);

        return result.get();
    }

    protected QueryTripsResult queryMoreTripsMobile(final QueryTripsContext contextObj, final boolean later)
            throws IOException {
        final Context context = (Context) contextObj;
        final HttpUrl commandUrl = HttpUrl.parse(context.context);
        final HttpUrl.Builder url = commandUrl.newBuilder();
        appendCommonRequestParams(url, "XML");
        url.addEncodedQueryParameter("command", later ? "tripNext" : "tripPrev");
        final AtomicReference<QueryTripsResult> result = new AtomicReference<>();

        final HttpClient.Callback callback = (bodyPeek, body) -> {
            try {
                result.set(queryTripsMobile(url.build(), null, null, null, body.charStream()));
            } catch (final XmlPullParserException | ParserException x) {
                throw new ParserException("cannot parse xml: " + bodyPeek, x);
            } catch (final RuntimeException x) {
                throw new RuntimeException("uncategorized problem while processing " + url, x);
            }
        };

        httpClient.getInputStream(callback, url.build(), httpRefererTrip);

        return result.get();
    }

    private QueryTripsResult queryTrips(final HttpUrl url, final Reader reader)
            throws XmlPullParserException, IOException {
        final XmlPullParser pp = parserFactory.newPullParser();
        pp.setInput(reader);
        final ResultHeader header = enterItdRequest(pp);
        final Object context = header.context;

        XmlPullUtil.require(pp, "itdTripRequest");
        final String requestId = XmlPullUtil.attr(pp, "requestID");
        XmlPullUtil.enter(pp, "itdTripRequest");

        while (XmlPullUtil.test(pp, "itdMessage")) {
            final int code = XmlPullUtil.intAttr(pp, "code");
            if (code == -4000) // no trips
                return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
            XmlPullUtil.next(pp);
        }
        XmlPullUtil.optSkip(pp, "itdPrintConfiguration");
        XmlPullUtil.optSkip(pp, "itdAddress");

        List<Location> ambiguousFrom = null, ambiguousTo = null, ambiguousVia = null;
        Location from = null, via = null, to = null;

        while (XmlPullUtil.test(pp, "itdOdv")) {
            final String usage = XmlPullUtil.attr(pp, "usage");

            final List<Location> locations = new ArrayList<>();
            final String nameState = processItdOdv(pp, usage, (nameState1, location, matchQuality) -> locations.add(location));

            if ("list".equals(nameState)) {
                if ("origin".equals(usage))
                    ambiguousFrom = locations;
                else if ("via".equals(usage))
                    ambiguousVia = locations;
                else if ("destination".equals(usage))
                    ambiguousTo = locations;
                else
                    throw new IllegalStateException("unknown usage: " + usage);
            } else if ("identified".equals(nameState)) {
                if ("origin".equals(usage))
                    from = locations.get(0);
                else if ("via".equals(usage))
                    via = locations.get(0);
                else if ("destination".equals(usage))
                    to = locations.get(0);
                else
                    throw new IllegalStateException("unknown usage: " + usage);
            } else if ("notidentified".equals(nameState)) {
                if ("origin".equals(usage))
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
                else if ("via".equals(usage))
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_VIA);
                else if ("destination".equals(usage))
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
                else
                    throw new IllegalStateException("unknown usage: " + usage);
            }
        }

        if (ambiguousFrom != null || ambiguousTo != null || ambiguousVia != null)
            return new QueryTripsResult(header, ambiguousFrom, ambiguousVia, ambiguousTo);

        XmlPullUtil.optSkip(pp, "itdAddOdvSeq");
        XmlPullUtil.enter(pp, "itdTripDateTime");
        XmlPullUtil.enter(pp, "itdDateTime");
        XmlPullUtil.require(pp, "itdDate");
        if (XmlPullUtil.optEnter(pp, "itdDate")) {
            if (XmlPullUtil.test(pp, "itdMessage")) {
                final String message = XmlPullUtil.nextText(pp, null, "itdMessage");

                if ("invalid date".equals(message))
                    return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
                else
                    throw new IllegalStateException("unknown message: " + message);
            }
            XmlPullUtil.skipExit(pp, "itdDate");
        }
        XmlPullUtil.skipExit(pp, "itdDateTime");
        XmlPullUtil.skipExit(pp, "itdTripDateTime");

        XmlPullUtil.requireSkip(pp, "itdTripOptions");
        XmlPullUtil.optSkipMultiple(pp, "omcTaxi");

        final List<Trip> trips = new ArrayList<>();
        final Joiner idJoiner = Joiner.on('-').skipNulls();

        XmlPullUtil.require(pp, "itdItinerary");
        if (XmlPullUtil.optEnter(pp, "itdItinerary")) {
            XmlPullUtil.optSkip(pp, "itdLegTTs");

            if (XmlPullUtil.optEnter(pp, "itdRouteList")) {
                final Calendar calendar = new GregorianCalendar(timeZone);

                while (XmlPullUtil.test(pp, "itdRoute")) {
                    final String tripId;
                    if (useRouteIndexAsTripId) {
                        final String routeIndex = XmlPullUtil.optAttr(pp, "routeIndex", null);
                        final String routeTripIndex = XmlPullUtil.optAttr(pp, "routeTripIndex", null);
                        tripId = Strings.emptyToNull(idJoiner.join(routeIndex, routeTripIndex));
                    } else {
                        tripId = null;
                    }
                    final int numChanges = XmlPullUtil.intAttr(pp, "changes");
                    XmlPullUtil.enter(pp, "itdRoute");

                    XmlPullUtil.optSkipMultiple(pp, "itdDateTime");
                    XmlPullUtil.optSkip(pp, "itdMapItemList");

                    XmlPullUtil.enter(pp, "itdPartialRouteList");
                    final List<Trip.Leg> legs = new LinkedList<>();
                    Location firstDepartureLocation = null;
                    Location lastArrivalLocation = null;

                    boolean cancelled = false;

                    while (XmlPullUtil.test(pp, "itdPartialRoute")) {
                        final String itdPartialRouteType = XmlPullUtil.attr(pp, "type");
                        final int distance = XmlPullUtil.optIntAttr(pp, "distance", 0);
                        XmlPullUtil.enter(pp, "itdPartialRoute");

                        XmlPullUtil.test(pp, "itdPoint");
                        if (!"departure".equals(XmlPullUtil.attr(pp, "usage")))
                            throw new IllegalStateException();
                        final Location departureLocation = processItdPointAttributes(pp);
                        if (firstDepartureLocation == null)
                            firstDepartureLocation = departureLocation;
                        final Position departurePosition = parsePosition(XmlPullUtil.optAttr(pp, "platformName", null));
                        XmlPullUtil.enter(pp, "itdPoint");
                        XmlPullUtil.optSkip(pp, "itdMapItemList");
                        XmlPullUtil.require(pp, "itdDateTime");
                        processItdDateTime(pp, calendar);
                        final Date departureTime = calendar.getTime();
                        final Date departureTargetTime;
                        if (XmlPullUtil.test(pp, "itdDateTimeTarget")) {
                            processItdDateTime(pp, calendar);
                            departureTargetTime = calendar.getTime();
                        } else {
                            departureTargetTime = null;
                        }
                        XmlPullUtil.skipExit(pp, "itdPoint");

                        XmlPullUtil.test(pp, "itdPoint");
                        if (!"arrival".equals(XmlPullUtil.attr(pp, "usage")))
                            throw new IllegalStateException();
                        final Location arrivalLocation = processItdPointAttributes(pp);
                        lastArrivalLocation = arrivalLocation;
                        final Position arrivalPosition = parsePosition(XmlPullUtil.optAttr(pp, "platformName", null));
                        XmlPullUtil.enter(pp, "itdPoint");
                        XmlPullUtil.optSkip(pp, "itdMapItemList");
                        XmlPullUtil.require(pp, "itdDateTime");
                        processItdDateTime(pp, calendar);
                        final Date arrivalTime = calendar.getTime();
                        final Date arrivalTargetTime;
                        if (XmlPullUtil.test(pp, "itdDateTimeTarget")) {
                            processItdDateTime(pp, calendar);
                            arrivalTargetTime = calendar.getTime();
                        } else {
                            arrivalTargetTime = null;
                        }
                        XmlPullUtil.skipExit(pp, "itdPoint");

                        XmlPullUtil.test(pp, "itdMeansOfTransport");

                        final String itdMeansOfTransportProductName = XmlPullUtil.optAttr(pp, "productName", null);
                        final int itdMeansOfTransportType = XmlPullUtil.intAttr(pp, "type");

                        if (itdMeansOfTransportType <= 16) {
                            cancelled |= processPublicLeg(pp, legs, calendar, departureTime, departureTargetTime,
                                    departureLocation, departurePosition, arrivalTime, arrivalTargetTime,
                                    arrivalLocation, arrivalPosition);
                        } else if (itdMeansOfTransportType == 97
                                && "nicht umsteigen".equals(itdMeansOfTransportProductName)) {
                            // ignore
                            XmlPullUtil.enter(pp, "itdMeansOfTransport");
                            XmlPullUtil.skipExit(pp, "itdMeansOfTransport");
                        } else if (itdMeansOfTransportType == 98
                                && "gesicherter Anschluss".equals(itdMeansOfTransportProductName)) {
                            // ignore
                            XmlPullUtil.enter(pp, "itdMeansOfTransport");
                            XmlPullUtil.skipExit(pp, "itdMeansOfTransport");
                        } else if (itdMeansOfTransportType == 99 && "Fussweg".equals(itdMeansOfTransportProductName)) {
                            processIndividualLeg(pp, legs, Trip.Individual.Type.WALK, distance, departureTime,
                                    departureLocation, arrivalTime, arrivalLocation);
                        } else if (itdMeansOfTransportType == 100 && (itdMeansOfTransportProductName == null
                                || "Fussweg".equals(itdMeansOfTransportProductName))) {
                            processIndividualLeg(pp, legs, Trip.Individual.Type.WALK, distance, departureTime,
                                    departureLocation, arrivalTime, arrivalLocation);
                        } else if (itdMeansOfTransportType == 105 && "Taxi".equals(itdMeansOfTransportProductName)) {
                            processIndividualLeg(pp, legs, Trip.Individual.Type.CAR, distance, departureTime,
                                    departureLocation, arrivalTime, arrivalLocation);
                        } else {
                            throw new IllegalStateException(MoreObjects.toStringHelper("")
                                    .add("itdPartialRoute.type", itdPartialRouteType)
                                    .add("itdMeansOfTransport.type", itdMeansOfTransportType)
                                    .add("itdMeansOfTransport.productName", itdMeansOfTransportProductName).toString());
                        }

                        XmlPullUtil.skipExit(pp, "itdPartialRoute");
                    }

                    XmlPullUtil.skipExit(pp, "itdPartialRouteList");

                    final List<Fare> fares = new ArrayList<>(2);
                    if (XmlPullUtil.optEnter(pp, "itdFare")) {
                        if (XmlPullUtil.test(pp, "itdSingleTicket")) {
                            final String net = XmlPullUtil.optAttr(pp, "net", null);
                            if (net != null) {
                                final Currency currency = parseCurrency(XmlPullUtil.attr(pp, "currency"));
                                final String fareAdult = XmlPullUtil.optAttr(pp, "fareAdult", null);
                                final String fareChild = XmlPullUtil.optAttr(pp, "fareChild", null);
                                final String unitName = XmlPullUtil.optAttr(pp, "unitName", null);
                                final String unitsAdult = XmlPullUtil.optAttr(pp, "unitsAdult", null);
                                final String unitsChild = XmlPullUtil.optAttr(pp, "unitsChild", null);
                                final String levelAdult = XmlPullUtil.optAttr(pp, "levelAdult", null);
                                final String levelChild = XmlPullUtil.optAttr(pp, "levelChild", null);
                                if (fareAdult != null)
                                    fares.add(new Fare(net.toUpperCase(), Type.ADULT, currency,
                                            Float.parseFloat(fareAdult) * fareCorrectionFactor,
                                            levelAdult != null ? null : unitName,
                                            levelAdult != null ? levelAdult : unitsAdult));
                                if (fareChild != null)
                                    fares.add(new Fare(net.toUpperCase(), Type.CHILD, currency,
                                            Float.parseFloat(fareChild) * fareCorrectionFactor,
                                            levelChild != null ? null : unitName,
                                            levelChild != null ? levelChild : unitsChild));

                                if (XmlPullUtil.optEnter(pp, "itdSingleTicket")) {
                                    if (XmlPullUtil.optEnter(pp, "itdGenericTicketList")) {
                                        while (XmlPullUtil.test(pp, "itdGenericTicketGroup")) {
                                            final Fare fare = processItdGenericTicketGroup(pp, net.toUpperCase(),
                                                    currency);
                                            if (fare != null)
                                                fares.add(fare);
                                        }
                                        XmlPullUtil.skipExit(pp, "itdGenericTicketList");
                                    }
                                    XmlPullUtil.skipExit(pp, "itdSingleTicket");
                                }
                            }
                        }
                        XmlPullUtil.skipExit(pp, "itdFare");
                    }

                    XmlPullUtil.skipExit(pp, "itdRoute");

                    final Trip trip = new Trip(tripId, firstDepartureLocation, lastArrivalLocation, legs,
                            fares.isEmpty() ? null : fares, null, numChanges);

                    if (!cancelled)
                        trips.add(trip);
                }

                XmlPullUtil.skipExit(pp, "itdRouteList");
            }
            XmlPullUtil.skipExit(pp, "itdItinerary");
        }

        return new QueryTripsResult(header, url.toString(), from, via, to,
                new Context(commandLink((String) context, requestId).toString()), trips);
    }

    private void processIndividualLeg(final XmlPullParser pp, final List<Leg> legs,
            final Trip.Individual.Type individualType, final int distance, final Date departureTime,
            final Location departureLocation, final Date arrivalTime, final Location arrivalLocation)
            throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "itdMeansOfTransport");
        XmlPullUtil.skipExit(pp, "itdMeansOfTransport");

        XmlPullUtil.optSkip(pp, "itdStopSeq");
        XmlPullUtil.optSkip(pp, "itdFootPathInfo");

        List<Point> path = null;
        if (XmlPullUtil.test(pp, "itdPathCoordinates"))
            path = processItdPathCoordinates(pp);

        final Trip.Leg lastLeg = legs.size() > 0 ? legs.get(legs.size() - 1) : null;
        if (lastLeg != null && lastLeg instanceof Trip.Individual
                && ((Trip.Individual) lastLeg).type == individualType) {
            final Trip.Individual lastIndividual = (Trip.Individual) legs.remove(legs.size() - 1);
            if (path != null && lastIndividual.path != null)
                path.addAll(0, lastIndividual.path);
            legs.add(new Trip.Individual(individualType, lastIndividual.departure, lastIndividual.departureTime,
                    arrivalLocation, arrivalTime, path, distance));
        } else {
            legs.add(new Trip.Individual(individualType, departureLocation, departureTime, arrivalLocation, arrivalTime,
                    path, distance));
        }
    }

    private boolean processPublicLeg(final XmlPullParser pp, final List<Leg> legs, final Calendar calendar,
            final Date departureTime, final Date departureTargetTime, final Location departureLocation,
            final Position departurePosition, final Date arrivalTime, final Date arrivalTargetTime,
            final Location arrivalLocation, final Position arrivalPosition) throws XmlPullParserException, IOException {
        final String destinationName = normalizeLocationName(XmlPullUtil.optAttr(pp, "destination", null));
        final String destinationId = XmlPullUtil.optAttr(pp, "destID", null);
        final Location destination;
        if (destinationId != null)
            destination = new Location(LocationType.STATION, destinationId, null, destinationName);
        else if (destinationId == null && destinationName != null)
            destination = new Location(LocationType.ANY, null, null, destinationName);
        else
            destination = null;

        final String motSymbol = XmlPullUtil.optAttr(pp, "symbol", null);
        final String motType = XmlPullUtil.optAttr(pp, "motType", null);
        final String motShortName = XmlPullUtil.optAttr(pp, "shortname", null);
        final String motName = XmlPullUtil.attr(pp, "name");
        final String motTrainName = XmlPullUtil.optAttr(pp, "trainName", null);
        final String motTrainType = XmlPullUtil.optAttr(pp, "trainType", null);

        XmlPullUtil.enter(pp, "itdMeansOfTransport");
        XmlPullUtil.require(pp, "motDivaParams");
        final String divaNetwork = XmlPullUtil.attr(pp, "network");
        final String divaLine = XmlPullUtil.attr(pp, "line");
        final String divaSupplement = XmlPullUtil.optAttr(pp, "supplement", "");
        final String divaDirection = XmlPullUtil.attr(pp, "direction");
        final String divaProject = XmlPullUtil.optAttr(pp, "project", "");
        final String lineId = divaNetwork + ':' + divaLine + ':' + divaSupplement + ':' + divaDirection + ':'
                + divaProject;
        XmlPullUtil.skipExit(pp, "itdMeansOfTransport");

        final Line line;
        if ("AST".equals(motSymbol))
            line = new Line(null, divaNetwork, Product.BUS, "AST");
        else
            line = parseLine(lineId, divaNetwork, motType, motSymbol, motShortName, motName, motTrainType, motShortName,
                    motTrainName);

        final Integer departureDelay;
        final Integer arrivalDelay;
        final boolean cancelled;
        if (XmlPullUtil.test(pp, "itdRBLControlled")) {
            departureDelay = XmlPullUtil.optIntAttr(pp, "delayMinutes", 0);
            arrivalDelay = XmlPullUtil.optIntAttr(pp, "delayMinutesArr", 0);
            cancelled = departureDelay == -9999 || arrivalDelay == -9999;

            XmlPullUtil.next(pp);
        } else {
            departureDelay = null;
            arrivalDelay = null;
            cancelled = false;
        }

        boolean lowFloorVehicle = false;
        String message = null;
        if (XmlPullUtil.optEnter(pp, "itdInfoTextList")) {
            while (XmlPullUtil.test(pp, "infoTextListElem")) {
                final String text = XmlPullUtil.valueTag(pp, "infoTextListElem");
                if (text != null) {
                    final String lcText = text.toLowerCase();
                    if (lcText.startsWith("niederflurwagen")) // KVV
                        lowFloorVehicle = true;
                    else if (lcText.contains("ruf") || lcText.contains("anmeld")) // Bedarfsverkehr
                        message = text;
                }
            }
            XmlPullUtil.skipExit(pp, "itdInfoTextList");
        }

        XmlPullUtil.optSkip(pp, "itdFootPathInfo");

        while (XmlPullUtil.optEnter(pp, "infoLink")) {
            XmlPullUtil.optSkip(pp, "paramList");
            final String infoLinkText = XmlPullUtil.valueTag(pp, "infoLinkText");
            if (message == null)
                message = infoLinkText;
            XmlPullUtil.skipExit(pp, "infoLink");
        }

        List<Stop> intermediateStops = null;
        if (XmlPullUtil.optEnter(pp, "itdStopSeq")) {
            intermediateStops = new LinkedList<>();
            while (XmlPullUtil.test(pp, "itdPoint")) {
                final Location stopLocation = processItdPointAttributes(pp);

                final Position stopPosition = parsePosition(XmlPullUtil.optAttr(pp, "platformName", null));

                XmlPullUtil.enter(pp, "itdPoint");
                XmlPullUtil.optSkip(pp, "genAttrList");
                XmlPullUtil.optSkip(pp, "sPAs");
                XmlPullUtil.require(pp, "itdDateTime");

                final Date plannedStopArrivalTime;
                final Date predictedStopArrivalTime;
                if (processItdDateTime(pp, calendar)) {
                    plannedStopArrivalTime = calendar.getTime();
                    if (arrivalDelay != null) {
                        calendar.add(Calendar.MINUTE, arrivalDelay);
                        predictedStopArrivalTime = calendar.getTime();
                    } else {
                        predictedStopArrivalTime = null;
                    }
                } else {
                    plannedStopArrivalTime = null;
                    predictedStopArrivalTime = null;
                }

                final Date plannedStopDepartureTime;
                final Date predictedStopDepartureTime;
                if (XmlPullUtil.test(pp, "itdDateTime") && processItdDateTime(pp, calendar)) {
                    plannedStopDepartureTime = calendar.getTime();
                    if (departureDelay != null) {
                        calendar.add(Calendar.MINUTE, departureDelay);
                        predictedStopDepartureTime = calendar.getTime();
                    } else {
                        predictedStopDepartureTime = null;
                    }
                } else {
                    plannedStopDepartureTime = null;
                    predictedStopDepartureTime = null;
                }

                final Stop stop = new Stop(stopLocation, plannedStopArrivalTime, predictedStopArrivalTime, stopPosition,
                        null, plannedStopDepartureTime, predictedStopDepartureTime, stopPosition, null);

                intermediateStops.add(stop);

                XmlPullUtil.skipExit(pp, "itdPoint");
            }
            XmlPullUtil.skipExit(pp, "itdStopSeq");

            // remove first and last, because they are not intermediate
            final int size = intermediateStops.size();
            if (size >= 2) {
                final Location lastLocation = intermediateStops.get(size - 1).location;
                if (!lastLocation.equals(arrivalLocation))
                    throw new IllegalStateException(lastLocation + " vs " + arrivalLocation);
                intermediateStops.remove(size - 1);

                final Location firstLocation = intermediateStops.get(0).location;
                if (!firstLocation.equals(departureLocation))
                    throw new IllegalStateException(firstLocation + " vs " + departureLocation);
                intermediateStops.remove(0);
            }
        }

        List<Point> path = null;
        if (XmlPullUtil.test(pp, "itdPathCoordinates"))
            path = processItdPathCoordinates(pp);

        XmlPullUtil.optSkip(pp, "itdITPathDescription");
        XmlPullUtil.optSkip(pp, "itdInterchangePathCoordinates");

        boolean wheelChairAccess = false;
        if (XmlPullUtil.optEnter(pp, "genAttrList")) {
            while (XmlPullUtil.optEnter(pp, "genAttrElem")) {
                final String name = XmlPullUtil.valueTag(pp, "name");
                final String value = XmlPullUtil.valueTag(pp, "value");
                XmlPullUtil.skipExit(pp, "genAttrElem");

                // System.out.println("genAttrElem: name='" + name + "' value='" + value + "'");

                if ("PlanWheelChairAccess".equals(name) && "1".equals(value))
                    wheelChairAccess = true;
            }
            XmlPullUtil.skipExit(pp, "genAttrList");
        }

        if (XmlPullUtil.optEnter(pp, "nextDeps")) {
            while (XmlPullUtil.test(pp, "itdDateTime")) {
                processItdDateTime(pp, calendar);
                /* final Date nextDepartureTime = */calendar.getTime();
            }
            XmlPullUtil.skipExit(pp, "nextDeps");
        }

        final Set<Line.Attr> lineAttrs = new HashSet<>();
        if (wheelChairAccess || lowFloorVehicle)
            lineAttrs.add(Line.Attr.WHEEL_CHAIR_ACCESS);
        final Line styledLine = new Line(line.id, line.network, line.product, line.label,
                lineStyle(line.network, line.product, line.label), lineAttrs);

        final Stop departure = new Stop(departureLocation, true,
                departureTargetTime != null ? departureTargetTime : departureTime,
                departureTime != null ? departureTime : null, departurePosition, null);
        final Stop arrival = new Stop(arrivalLocation, false,
                arrivalTargetTime != null ? arrivalTargetTime : arrivalTime, arrivalTime != null ? arrivalTime : null,
                arrivalPosition, null);

        legs.add(new Trip.Public(styledLine, destination, departure, arrival, intermediateStops, path, message));

        return cancelled;
    }

    private QueryTripsResult queryTripsMobile(final HttpUrl url, final Location from, final @Nullable Location via,
            final Location to, final Reader reader) throws XmlPullParserException, IOException {
        final XmlPullParser pp = parserFactory.newPullParser();
        pp.setInput(reader);
        final ResultHeader header = enterEfa(pp);
        XmlPullUtil.optSkip(pp, "msgs");

        final Calendar plannedTimeCal = new GregorianCalendar(timeZone);
        final Calendar predictedTimeCal = new GregorianCalendar(timeZone);

        final List<Trip> trips = new ArrayList<>();

        if (XmlPullUtil.optEnter(pp, "ts")) {
            while (XmlPullUtil.optEnter(pp, "tp")) {
                XmlPullUtil.optSkip(pp, "attrs");

                XmlPullUtil.valueTag(pp, "d"); // duration
                final int numChanges = Integer.parseInt(XmlPullUtil.valueTag(pp, "ic"));
                XmlPullUtil.valueTag(pp, "de");
                XmlPullUtil.optValueTag(pp, "optval", null);
                XmlPullUtil.optValueTag(pp, "alt", null);
                final String tripId = XmlPullUtil.valueTag(pp, "gix");

                XmlPullUtil.enter(pp, "ls");

                final List<Trip.Leg> legs = new LinkedList<>();
                Location firstDepartureLocation = null;
                Location lastArrivalLocation = null;

                while (XmlPullUtil.test(pp, "l")) {
                    XmlPullUtil.enter(pp, "l");
                    XmlPullUtil.optSkip(pp, "rtStatus");

                    XmlPullUtil.enter(pp, "ps");

                    Stop departure = null;
                    Stop arrival = null;

                    while (XmlPullUtil.optEnter(pp, "p")) {
                        final String name = XmlPullUtil.valueTag(pp, "n");
                        final String usage = XmlPullUtil.valueTag(pp, "u");
                        XmlPullUtil.optValueTag(pp, "de", null);
                        XmlPullUtil.optValueTag(pp, "gid", null);
                        XmlPullUtil.optValueTag(pp, "pgid", null);
                        XmlPullUtil.optValueTag(pp, "rtStatus", null);
                        XmlPullUtil.requireSkip(pp, "dt");

                        parseMobileSt(pp, plannedTimeCal, predictedTimeCal);

                        XmlPullUtil.optSkip(pp, "lis"); // links

                        XmlPullUtil.enter(pp, "r");
                        final String id = XmlPullUtil.valueTag(pp, "id");
                        XmlPullUtil.optValueTag(pp, "a", null);
                        final Position position = parsePosition(XmlPullUtil.optValueTag(pp, "pl", null));
                        final String place = normalizeLocationName(XmlPullUtil.optValueTag(pp, "pc", null));
                        final Point coord = parseCoord(XmlPullUtil.optValueTag(pp, "c", null));
                        XmlPullUtil.skipExit(pp, "r");

                        final Location location;
                        if (id.equals("99999997") || id.equals("99999998"))
                            location = new Location(LocationType.ADDRESS, null, coord, place, name);
                        else
                            location = new Location(LocationType.STATION, id, coord, place, name);

                        XmlPullUtil.skipExit(pp, "p");

                        final Date plannedTime = plannedTimeCal.isSet(Calendar.HOUR_OF_DAY) ? plannedTimeCal.getTime()
                                : null;
                        final Date predictedTime = predictedTimeCal.isSet(Calendar.HOUR_OF_DAY)
                                ? predictedTimeCal.getTime() : null;

                        if ("departure".equals(usage)) {
                            departure = new Stop(location, true, plannedTime, predictedTime, position, null);
                            if (firstDepartureLocation == null)
                                firstDepartureLocation = location;
                        } else if ("arrival".equals(usage)) {
                            arrival = new Stop(location, false, plannedTime, predictedTime, position, null);
                            lastArrivalLocation = location;
                        } else {
                            throw new IllegalStateException("unknown usage: " + usage);
                        }
                    }

                    checkState(departure != null);
                    checkState(arrival != null);

                    XmlPullUtil.skipExit(pp, "ps");

                    final boolean isRealtime = XmlPullUtil.valueTag(pp, "realtime").equals("1");

                    final LineDestination lineDestination = parseMobileM(pp, false);

                    final List<Point> path;
                    if (XmlPullUtil.test(pp, "pt"))
                        path = processCoordinateStrings(pp, "pt");
                    else
                        path = null;

                    final List<Stop> intermediateStops;
                    XmlPullUtil.require(pp, "pss");
                    if (XmlPullUtil.optEnter(pp, "pss")) {
                        intermediateStops = new LinkedList<>();

                        while (XmlPullUtil.test(pp, "s")) {
                            plannedTimeCal.clear();
                            predictedTimeCal.clear();

                            final String s = XmlPullUtil.valueTag(pp, "s");
                            final String[] intermediateParts = s.split(";");
                            final String id = intermediateParts[0];
                            if (!id.equals(departure.location.id) && !id.equals(arrival.location.id)) {
                                final String name = normalizeLocationName(intermediateParts[1]);

                                if (!(intermediateParts[2].startsWith("000") && intermediateParts[3].startsWith("000"))) {
                                    ParserUtils.parseIsoDate(plannedTimeCal, intermediateParts[2]);
                                    ParserUtils.parseIsoTime(plannedTimeCal, intermediateParts[3]);

                                    if (isRealtime) {
                                        ParserUtils.parseIsoDate(predictedTimeCal, intermediateParts[2]);
                                        ParserUtils.parseIsoTime(predictedTimeCal, intermediateParts[3]);

                                        if (intermediateParts.length > 5 && intermediateParts[5].length() > 0) {
                                            final int delay = Integer.parseInt(intermediateParts[5]);
                                            predictedTimeCal.add(Calendar.MINUTE, delay);
                                        }
                                    }
                                }
                                final String coordPart = intermediateParts[4];

                                final Point coords;
                                if (!"::".equals(coordPart)) {
                                    final String[] coordParts = coordPart.split(":");
                                    final String mapName = coordParts[2];
                                    if (COORD_FORMAT.equals(mapName)) {
                                        final double lat = Double.parseDouble(coordParts[1]);
                                        final double lon = Double.parseDouble(coordParts[0]);
                                        coords = Point.fromDouble(lat, lon);
                                    } else {
                                        coords = null;
                                    }
                                } else {
                                    coords = null;
                                }
                                final Location location = new Location(LocationType.STATION, id, coords, null, name);

                                final Date plannedTime = plannedTimeCal.isSet(Calendar.HOUR_OF_DAY)
                                        ? plannedTimeCal.getTime() : null;
                                final Date predictedTime = predictedTimeCal.isSet(Calendar.HOUR_OF_DAY)
                                        ? predictedTimeCal.getTime() : null;
                                final Stop stop = new Stop(location, false, plannedTime, predictedTime, null, null);

                                intermediateStops.add(stop);
                            }
                        }

                        XmlPullUtil.skipExit(pp, "pss");
                    } else {
                        intermediateStops = null;
                    }

                    XmlPullUtil.optSkip(pp, "interchange");

                    XmlPullUtil.requireSkip(pp, "ns");
                    // TODO messages

                    XmlPullUtil.skipExit(pp, "l");

                    if (lineDestination.line == Line.FOOTWAY) {
                        legs.add(new Trip.Individual(Trip.Individual.Type.WALK, departure.location,
                                departure.getDepartureTime(), arrival.location, arrival.getArrivalTime(), path, 0));
                    } else if (lineDestination.line == Line.TRANSFER) {
                        legs.add(new Trip.Individual(Trip.Individual.Type.TRANSFER, departure.location,
                                departure.getDepartureTime(), arrival.location, arrival.getArrivalTime(), path, 0));
                    } else if (lineDestination.line == Line.SECURE_CONNECTION
                            || lineDestination.line == Line.DO_NOT_CHANGE) {
                        // ignore
                    } else {
                        legs.add(new Trip.Public(lineDestination.line, lineDestination.destination, departure, arrival,
                                intermediateStops, path, null));
                    }
                }

                XmlPullUtil.skipExit(pp, "ls");

                XmlPullUtil.optSkip(pp, "seqroutes");

                final List<Fare> fares;
                if (XmlPullUtil.optEnter(pp, "tcs")) {
                    fares = new ArrayList<>(2);
                    XmlPullUtil.optSkipMultiple(pp, "tc"); // TODO fares
                    XmlPullUtil.skipExit(pp, "tcs");
                } else {
                    fares = null;
                }

                final Trip trip = new Trip(tripId, firstDepartureLocation, lastArrivalLocation, legs, fares, null,
                        numChanges);
                trips.add(trip);

                XmlPullUtil.skipExit(pp, "tp");
            }

            XmlPullUtil.skipExit(pp, "ts");
        }

        if (trips.size() > 0) {
            final String[] context = (String[]) header.context;
            return new QueryTripsResult(header, url.toString(), from, via, to,
                    new Context(commandLink(context[0], context[1]).toString()), trips);
        } else {
            return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
        }
    }

    private List<Point> processItdPathCoordinates(final XmlPullParser pp) throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "itdPathCoordinates");
        final List<Point> path;

        final String ellipsoid = XmlPullUtil.valueTag(pp, "coordEllipsoid");
        if ("WGS84".equals(ellipsoid)) {
            final String type = XmlPullUtil.valueTag(pp, "coordType");
            if (!"GEO_DECIMAL".equals(type))
                throw new IllegalStateException("unknown type: " + type);
            if (XmlPullUtil.test(pp, "itdCoordinateString")) {
                path = processCoordinateStrings(pp, "itdCoordinateString");
            } else if (XmlPullUtil.test(pp, "itdCoordinateBaseElemList")) {
                path = processCoordinateBaseElems(pp);
            } else {
                throw new IllegalStateException(pp.getPositionDescription());
            }
        } else {
            path = null;
        }

        XmlPullUtil.skipExit(pp, "itdPathCoordinates");
        return path;
    }

    private @Nullable List<Point> processCoordinateStrings(final XmlPullParser pp, final String tag)
            throws XmlPullParserException, IOException {
        final List<Point> path = new LinkedList<>();

        final String value = XmlPullUtil.optValueTag(pp, tag, null);
        if (value != null) {
            for (final String coordStr : value.split(" +"))
                path.add(parseCoord(coordStr));
            return path;
        } else {
            return null;
        }
    }

    private List<Point> processCoordinateBaseElems(final XmlPullParser pp) throws XmlPullParserException, IOException {
        final List<Point> path = new LinkedList<>();

        XmlPullUtil.enter(pp, "itdCoordinateBaseElemList");

        while (XmlPullUtil.optEnter(pp, "itdCoordinateBaseElem")) {
            final double x = Double.parseDouble(XmlPullUtil.valueTag(pp, "x"));
            final double y = Double.parseDouble(XmlPullUtil.valueTag(pp, "y"));
            path.add(Point.fromDouble(y, x));

            XmlPullUtil.skipExit(pp, "itdCoordinateBaseElem");
        }

        XmlPullUtil.skipExit(pp, "itdCoordinateBaseElemList");

        return path;
    }

    private Point parseCoord(final String coordStr) {
        if (coordStr == null)
            return null;

        final String[] parts = coordStr.split(",");
        final double lat = Double.parseDouble(parts[1]);
        final double lon = Double.parseDouble(parts[0]);
        return Point.fromDouble(lat, lon);
    }

    private Point processCoordAttr(final XmlPullParser pp) {
        final String mapName = XmlPullUtil.optAttr(pp, "mapName", null);
        final double x = XmlPullUtil.optFloatAttr(pp, "x", 0);
        final double y = XmlPullUtil.optFloatAttr(pp, "y", 0);

        if (mapName == null || (x == 0 && y == 0))
            return null;

        if (!COORD_FORMAT.equals(mapName))
            return null;

        return Point.fromDouble(y, x);
    }

    private Product majorMeansToProduct(final int majorMeans) {
        switch (majorMeans) {
            case 1:
                return Product.SUBWAY;
            case 2:
                return Product.SUBURBAN_TRAIN;
            case 3:
                return Product.BUS;
            case 4:
                return Product.TRAM;
            default:
                log.info("unknown STOP_MAJOR_MEANS value: {}", majorMeans);
                return null;
        }
    }

    private Fare processItdGenericTicketGroup(final XmlPullParser pp, final String net, final Currency currency)
            throws XmlPullParserException, IOException {
        XmlPullUtil.enter(pp, "itdGenericTicketGroup");

        Type type = null;
        float fare = 0;

        while (XmlPullUtil.optEnter(pp, "itdGenericTicket")) {
            final String key = XmlPullUtil.valueTag(pp, "ticket");
            final String value = XmlPullUtil.valueTag(pp, "value");

            if (key.equals("FOR_RIDER")) {
                final String typeStr = value.split(" ")[0].toUpperCase();
                if (typeStr.equals("REGULAR"))
                    type = Type.ADULT;
                else
                    type = Type.valueOf(typeStr);
            } else if (key.equals("PRICE")) {
                fare = Float.parseFloat(value) * fareCorrectionFactor;
            }

            XmlPullUtil.skipExit(pp, "itdGenericTicket");
        }

        XmlPullUtil.skipExit(pp, "itdGenericTicketGroup");

        if (type != null)
            return new Fare(net, type, currency, fare, null, null);
        else
            return null;
    }

    private Currency parseCurrency(final String currencyStr) {
        if (currencyStr.equals("US$"))
            return Currency.getInstance("USD");
        if (currencyStr.equals("Dirham"))
            return Currency.getInstance("AED");
        return ParserUtils.getCurrency(currencyStr);
    }

    private static final Pattern P_POSITION = Pattern.compile(
            "(?:Gleis|Gl\\.|Bahnsteig|Bstg\\.|Bussteig|Busstg\\.|Steig|Hp\\.|Stop|Pos\\.|Zone|Platform|Stand|Bay|Stance)?\\s*(.+)",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected Position parsePosition(final String position) {
        if (position == null)
            return null;

        if (position.startsWith("Ri.") || position.startsWith("Richtung "))
            return null;

        final Matcher m = P_POSITION.matcher(position);
        if (m.matches())
            return super.parsePosition(m.group(1));

        return super.parsePosition(position);
    }

    private void appendLocationParams(final HttpUrl.Builder url, final Location location, final String paramSuffix) {
        if (location.type == LocationType.STATION && location.hasId()) {
            url.addEncodedQueryParameter("type_" + paramSuffix, "stop");
            url.addEncodedQueryParameter("name_" + paramSuffix,
                    ParserUtils.urlEncode(normalizeStationId(location.id), requestUrlEncoding));
        } else if (location.type == LocationType.POI && location.hasId()) {
            url.addEncodedQueryParameter("type_" + paramSuffix, "poi");
            url.addEncodedQueryParameter("name_" + paramSuffix, ParserUtils.urlEncode(location.id, requestUrlEncoding));
        } else if (location.type == LocationType.ADDRESS && location.hasId()) {
            url.addEncodedQueryParameter("type_" + paramSuffix, "address");
            url.addEncodedQueryParameter("name_" + paramSuffix, ParserUtils.urlEncode(location.id, requestUrlEncoding));
        } else if ((location.type == LocationType.ADDRESS || location.type == LocationType.COORD)
                && location.hasCoord()) {
            url.addEncodedQueryParameter("type_" + paramSuffix, "coord");
            url.addEncodedQueryParameter("name_" + paramSuffix,
                    ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%.7f:%.7f:%s", location.getLonAsDouble(),
                            location.getLatAsDouble(), COORD_FORMAT), requestUrlEncoding));
        } else if (location.name != null) {
            url.addEncodedQueryParameter("type_" + paramSuffix, "any");
            url.addEncodedQueryParameter("name_" + paramSuffix,
                    ParserUtils.urlEncode(location.name, requestUrlEncoding));
        } else {
            throw new IllegalArgumentException("cannot append location: " + location);
        }
    }

    private static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<>();

    static {
        WALKSPEED_MAP.put(WalkSpeed.SLOW, "slow");
        WALKSPEED_MAP.put(WalkSpeed.NORMAL, "normal");
        WALKSPEED_MAP.put(WalkSpeed.FAST, "fast");
    }

    private ResultHeader enterItdRequest(final XmlPullParser pp) throws XmlPullParserException, IOException {
        if (pp.getEventType() != XmlPullParser.START_DOCUMENT)
            throw new ParserException("start of document expected");

        pp.next();

        if (pp.getEventType() == XmlPullParser.DOCDECL)
            pp.next();

        if (pp.getEventType() == XmlPullParser.END_DOCUMENT)
            throw new ParserException("empty document");

        XmlPullUtil.require(pp, "itdRequest");

        final String serverVersion = XmlPullUtil.attr(pp, "version");
        final String now = XmlPullUtil.optAttr(pp, "now", null);
        final String sessionId = XmlPullUtil.attr(pp, "sessionID");
        final String serverId = XmlPullUtil.attr(pp, "serverID");

        final long serverTime;
        if (now != null) {
            final Calendar calendar = new GregorianCalendar(timeZone);
            ParserUtils.parseIsoDate(calendar, now.substring(0, 10));
            ParserUtils.parseEuropeanTime(calendar, now.substring(11));
            serverTime = calendar.getTimeInMillis();
        } else {
            serverTime = 0;
        }

        final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, serverVersion, serverId, serverTime,
                sessionId);

        XmlPullUtil.enter(pp, "itdRequest");

        XmlPullUtil.optSkip(pp, "itdMessageList");
        XmlPullUtil.optSkip(pp, "clientHeaderLines");
        XmlPullUtil.optSkip(pp, "itdVersionInfo");
        XmlPullUtil.optSkip(pp, "itdLayoutParams");
        XmlPullUtil.optSkip(pp, "itdInfoLinkList");
        XmlPullUtil.optSkip(pp, "serverMetaInfo");

        return header;
    }

    private ResultHeader enterEfa(final XmlPullParser pp) throws XmlPullParserException, IOException {
        if (pp.getEventType() != XmlPullParser.START_DOCUMENT)
            throw new ParserException("start of document expected");

        pp.next();

        if (pp.getEventType() == XmlPullParser.END_DOCUMENT)
            throw new ParserException("empty document");

        XmlPullUtil.enter(pp, "efa");

        if (XmlPullUtil.test(pp, "error")) {
            final String message = XmlPullUtil.valueTag(pp, "error");
            throw new RuntimeException(message);
        } else {
            final String now = XmlPullUtil.valueTag(pp, "now");
            final Calendar serverTime = new GregorianCalendar(timeZone);
            ParserUtils.parseIsoDate(serverTime, now.substring(0, 10));
            ParserUtils.parseEuropeanTime(serverTime, now.substring(11));

            final Map<String, String> params = processPas(pp);
            final String requestId = params.get("requestID");
            final String sessionId = params.get("sessionID");
            final String serverId = params.get("serverID");

            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, null, serverId,
                    serverTime.getTimeInMillis(), new String[] { sessionId, requestId });

            return header;
        }
    }

    private Map<String, String> processPas(final XmlPullParser pp) throws XmlPullParserException, IOException {
        final Map<String, String> params = new HashMap<>();

        XmlPullUtil.enter(pp, "pas");

        while (XmlPullUtil.optEnter(pp, "pa")) {
            final String name = XmlPullUtil.valueTag(pp, "n");
            final String value = XmlPullUtil.valueTag(pp, "v");
            params.put(name, value);
            XmlPullUtil.skipExit(pp, "pa");
        }

        XmlPullUtil.skipExit(pp, "pas");

        return params;
    }
}
