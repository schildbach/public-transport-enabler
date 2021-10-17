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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Line;
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
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.PolylineFormat;

import okhttp3.HttpUrl;

/**
 * This is an implementation of the HCI (HAFAS Client Interface).
 * 
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasClientInterfaceProvider extends AbstractHafasProvider {
    private final HttpUrl apiBase;
    private String apiEndpoint = "mgate.exe";
    @Nullable
    private String apiVersion;
    @Nullable
    private String apiExt;
    @Nullable
    private String apiAuthorization;
    @Nullable
    private String apiClient;
    @Nullable
    private byte[] requestChecksumSalt;
    @Nullable
    private byte[] requestMicMacSalt;

    private static final String SERVER_PRODUCT = "hci";
    private static final String SECTION_TYPE_JOURNEY = "JNY";
    private static final String SECTION_TYPE_WALK = "WALK";
    private static final String SECTION_TYPE_TRANSFER = "TRSF";
    private static final String SECTION_TYPE_TELE_TAXI = "TETA";
    private static final String SECTION_TYPE_DEVI = "DEVI";
    @SuppressWarnings("deprecation")
    private static final HashFunction MD5 = Hashing.md5();
    private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    public AbstractHafasClientInterfaceProvider(final NetworkId network, final HttpUrl apiBase,
            final Product[] productsMap) {
        super(network, productsMap);
        this.apiBase = checkNotNull(apiBase);
    }

    public HttpUrl getApiBase() {
        return apiBase;
    }

    protected AbstractHafasClientInterfaceProvider setApiEndpoint(final String apiEndpoint) {
        this.apiEndpoint = checkNotNull(apiEndpoint);
        return this;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    protected AbstractHafasClientInterfaceProvider setApiVersion(final String apiVersion) {
        checkArgument(apiVersion.compareToIgnoreCase("1.14") >= 0, "apiVersion must be 1.14 or higher");
        this.apiVersion = apiVersion;
        return this;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    protected AbstractHafasClientInterfaceProvider setApiExt(final String apiExt) {
        this.apiExt = checkNotNull(apiExt);
        return this;
    }

    public String getApiExt() {
        return apiExt;
    }

    protected AbstractHafasClientInterfaceProvider setApiAuthorization(final String apiAuthorization) {
        this.apiAuthorization = apiAuthorization;
        return this;
    }

    public String getApiAuthorization() {
        return apiAuthorization;
    }

    protected AbstractHafasClientInterfaceProvider setApiClient(final String apiClient) {
        this.apiClient = apiClient;
        return this;
    }

    public String getApiClient() {
        return apiClient;
    }

    protected AbstractHafasClientInterfaceProvider setRequestChecksumSalt(final byte[] requestChecksumSalt) {
        this.requestChecksumSalt = requestChecksumSalt;
        return this;
    }

    public byte[] getRequestChecksumSalt() {
        return requestChecksumSalt;
    }

    protected AbstractHafasClientInterfaceProvider setRequestMicMacSalt(final byte[] requestMicMacSalt) {
        this.requestMicMacSalt = requestMicMacSalt;
        return this;
    }

    public byte[] getRequestMicMacSalt() {
        return requestMicMacSalt;
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasCoord())
            return jsonLocGeoPos(types, location.coord, maxDistance, maxLocations);
        else
            throw new IllegalArgumentException("cannot handle: " + location);
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        return jsonStationBoard(stationId, time, maxDepartures, equivs);
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        return jsonLocMatch(constraint, types, maxLocations);
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        return jsonTripSearch(from, via, to, date, dep, options != null ? options.products : null,
                options != null ? options.walkSpeed : null, null);
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException {
        final JsonContext jsonContext = (JsonContext) context;
        return jsonTripSearch(jsonContext.from, jsonContext.via, jsonContext.to, jsonContext.date, jsonContext.dep,
                jsonContext.products, jsonContext.walkSpeed, later ? jsonContext.laterContext : jsonContext.earlierContext);
    }

    protected final NearbyLocationsResult jsonLocGeoPos(final Set<LocationType> types, final Point coord,
            int maxDistance, int maxLocations) throws IOException {
        if (maxDistance == 0)
            maxDistance = DEFAULT_MAX_DISTANCE;
        if (maxLocations == 0)
            maxLocations = DEFAULT_MAX_LOCATIONS;
        final boolean getStations = types.contains(LocationType.STATION);
        final boolean getPOIs = types.contains(LocationType.POI);
        final String request = wrapJsonApiRequest("LocGeoPos", "{\"ring\":" //
                + "{\"cCrd\":{\"x\":" + coord.getLonAs1E6() + ",\"y\":" + coord.getLatAs1E6() + "}," //
                + "\"maxDist\":" + maxDistance + "}," //
                + "\"getStops\":" + getStations + "," //
                + "\"getPOIs\":" + getPOIs + "," //
                + "\"maxLoc\":" + maxLocations + "}", //
                false);

        final HttpUrl url = requestUrl(request);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String headErr = head.optString("err", null);
            if (headErr != null && !"OK".equals(headErr)) {
                final String headErrTxt = head.optString("errTxt");
                throw new RuntimeException(headErr + " " + headErrTxt);
            }

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 2);
            final ResultHeader header = parseServerInfo(svcResList.getJSONObject(0), head.getString("ver"));

            final JSONObject svcRes = svcResList.getJSONObject(1);
            checkState("LocGeoPos".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.optString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new NearbyLocationsResult(header, NearbyLocationsResult.Status.SERVICE_DOWN);
                if ("CGI_READ_FAILED".equals(err))
                    return new NearbyLocationsResult(header, NearbyLocationsResult.Status.SERVICE_DOWN);
                if ("CGI_NO_SERVER".equals(err))
                    return new NearbyLocationsResult(header, NearbyLocationsResult.Status.SERVICE_DOWN);
                if ("H_UNKNOWN".equals(err))
                    return new NearbyLocationsResult(header, NearbyLocationsResult.Status.SERVICE_DOWN);
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            /* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));
            final JSONArray crdSysList = common.optJSONArray("crdSysL");

            final JSONArray locL = res.optJSONArray("locL");
            final List<Location> locations;
            if (locL != null) {
                locations = parseLocList(locL, crdSysList);

                // filter unwanted location types
                for (Iterator<Location> i = locations.iterator(); i.hasNext();) {
                    final Location location = i.next();
                    if (!types.contains(location.type))
                        i.remove();
                }
            } else {
                locations = Collections.emptyList();
            }

            return new NearbyLocationsResult(header, locations);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    protected final QueryDeparturesResult jsonStationBoard(final String stationId, final @Nullable Date time,
            int maxDepartures, final boolean equivs) throws IOException {
        final boolean canStbFltrEquiv = apiVersion.compareToIgnoreCase("1.18") <= 0;
        if (maxDepartures == 0)
            maxDepartures = DEFAULT_MAX_DEPARTURES;
        if (!equivs && !canStbFltrEquiv) {
            final int raisedMaxDepartures = maxDepartures * 4;
            log.info("stbFltrEquiv workaround in effect: querying for {} departures rather than {}",
                    raisedMaxDepartures, maxDepartures);
            maxDepartures = raisedMaxDepartures;
        }

        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);
        final CharSequence jsonDate = jsonDate(c);
        final CharSequence jsonTime = jsonTime(c);
        final CharSequence normalizedStationId = normalizeStationId(stationId);
        final CharSequence maxJny = Integer.toString(maxDepartures);
        final String request = wrapJsonApiRequest("StationBoard", "{\"type\":\"DEP\"," //
                + "\"date\":\"" + jsonDate + "\"," //
                + "\"time\":\"" + jsonTime + "\"," //
                + "\"stbLoc\":{\"type\":\"S\"," + "\"state\":\"F\"," // F/M
                + "\"extId\":" + JSONObject.quote(normalizedStationId.toString()) + "}," //
                + (canStbFltrEquiv ? "\"stbFltrEquiv\":" + Boolean.toString(!equivs) + "," : "") //
                + "\"maxJny\":" + maxJny + "}", false);

        final HttpUrl url = requestUrl(request);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String headErr = head.optString("err", null);
            if (headErr != null && !"OK".equals(headErr)) {
                final String headErrTxt = head.optString("errTxt");
                throw new RuntimeException(headErr + " " + headErrTxt);
            }

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 2);
            final ResultHeader header = parseServerInfo(svcResList.getJSONObject(0), head.getString("ver"));
            final QueryDeparturesResult result = new QueryDeparturesResult(header);

            final JSONObject svcRes = svcResList.optJSONObject(1);
            checkState("StationBoard".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.optString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("LOCATION".equals(err) && "HCI Service: location missing or invalid".equals(errTxt))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                if ("PROBLEMS".equals(err) && "HCI Service: problems during service execution".equals(errTxt))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                if ("CGI_READ_FAILED".equals(err))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                if ("CGI_NO_SERVER".equals(err))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                if ("H_UNKNOWN".equals(err))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            final List<String[]> remarks = parseRemList(common.getJSONArray("remL"));
            final List<Style> styles = parseIcoList(common.getJSONArray("icoL"));
            final List<String> operators = parseOpList(common.getJSONArray("opL"));
            final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators, styles);
            final JSONArray crdSysList = common.optJSONArray("crdSysL");
            final JSONArray locList = common.getJSONArray("locL");

            final JSONArray jnyList = res.optJSONArray("jnyL");
            if (jnyList != null) {
                for (int iJny = 0; iJny < jnyList.length(); iJny++) {
                    final JSONObject jny = jnyList.getJSONObject(iJny);
                    final JSONObject stbStop = jny.getJSONObject("stbStop");

                    final boolean cancelled = stbStop.optBoolean("dCncl", false);
                    if (cancelled)
                        continue;

                    final Position position = parseJsonPosition(stbStop, "dPlatfS", "dPltfS");

                    c.clear();
                    ParserUtils.parseIsoDate(c, jny.getString("date"));
                    final Date baseDate = c.getTime();

                    final Date plannedTime = parseJsonTime(c, baseDate, stbStop.getString("dTimeS"));

                    final Date predictedTime = parseJsonTime(c, baseDate, stbStop.optString("dTimeR", null));

                    final int dProdX = stbStop.optInt("dProdX", -1);
                    final Line line = dProdX != -1 ? lines.get(dProdX) : null;

                    final Location location = parseLoc(locList, stbStop.getInt("locX"), null, crdSysList);
                    checkState(location.type == LocationType.STATION);
                    if (!equivs && !location.id.equals(stationId))
                        continue;

                    final String jnyDirTxt = jny.getString("dirTxt");
                    Location destination = null;
                    // if last entry in stopL happens to be our destination, use it
                    final JSONArray stopList = jny.optJSONArray("stopL");
                    if (stopList != null) {
                        final int lastStopIdx = stopList.getJSONObject(stopList.length() - 1).getInt("locX");
                        final String lastStopName = locList.getJSONObject(lastStopIdx).getString("name");
                        if (jnyDirTxt.equals(lastStopName))
                            destination = parseLoc(locList, lastStopIdx, null, crdSysList);
                    }
                    // otherwise split unidentified destination as if it was a station and use it
                    if (destination == null) {
                        final String[] splitJnyDirTxt = splitStationName(jnyDirTxt);
                        destination = new Location(LocationType.ANY, null, splitJnyDirTxt[0], splitJnyDirTxt[1]);
                    }

                    final JSONArray remList = jny.optJSONArray("remL");
                    String message = null;
                    if (remList != null) {
                        for (int iRem = 0; iRem < remList.length(); iRem++) {
                            final JSONObject rem = remList.getJSONObject(iRem);
                            final String[] remark = remarks.get(rem.getInt("remX"));
                            if ("l?".equals(remark[0]))
                                message = remark[1];
                        }
                    }

                    if (line != null) {
                        final Departure departure = new Departure(plannedTime, predictedTime, line, position,
                                destination, null, message);

                        StationDepartures stationDepartures = findStationDepartures(result.stationDepartures, location);
                        if (stationDepartures == null) {
                            stationDepartures = new StationDepartures(location, new ArrayList<Departure>(8), null);
                            result.stationDepartures.add(stationDepartures);
                        }

                        stationDepartures.departures.add(departure);
                    }
                }
            }

            // sort departures
            for (final StationDepartures stationDepartures : result.stationDepartures)
                Collections.sort(stationDepartures.departures, Departure.TIME_COMPARATOR);

            return result;
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    protected final SuggestLocationsResult jsonLocMatch(final CharSequence constraint,
            final @Nullable Set<LocationType> types, int maxLocations) throws IOException {
        checkNotNull(constraint);
        if (maxLocations == 0)
            maxLocations = DEFAULT_MAX_LOCATIONS;
        final String type;
        if (types == null || types.contains(LocationType.ANY)
                || types.containsAll(EnumSet.of(LocationType.STATION, LocationType.ADDRESS, LocationType.POI)))
            type = "ALL";
        else
            type = Joiner.on("").skipNulls().join(types.contains(LocationType.STATION) ? "S" : null,
                    types.contains(LocationType.ADDRESS) ? "A" : null, types.contains(LocationType.POI) ? "P" : null);
        final String loc = "{\"name\":" + JSONObject.quote(constraint + "?") + ",\"type\":\"" + type + "\"}";
        final String request = wrapJsonApiRequest("LocMatch",
                "{\"input\":{\"field\":\"S\",\"loc\":" + loc + ",\"maxLoc\":" + maxLocations + "}}", false);

        final HttpUrl url = requestUrl(request);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String headErr = head.optString("err", null);
            if (headErr != null && !"OK".equals(headErr)) {
                final String headErrTxt = head.optString("errTxt");
                throw new RuntimeException(headErr + " " + headErrTxt);
            }

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 2);
            final ResultHeader header = parseServerInfo(svcResList.getJSONObject(0), head.getString("ver"));

            final JSONObject svcRes = svcResList.optJSONObject(1);
            checkState("LocMatch".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.optString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new SuggestLocationsResult(header, SuggestLocationsResult.Status.SERVICE_DOWN);
                if ("CGI_READ_FAILED".equals(err))
                    return new SuggestLocationsResult(header, SuggestLocationsResult.Status.SERVICE_DOWN);
                if ("CGI_NO_SERVER".equals(err))
                    return new SuggestLocationsResult(header, SuggestLocationsResult.Status.SERVICE_DOWN);
                if ("H_UNKNOWN".equals(err))
                    return new SuggestLocationsResult(header, SuggestLocationsResult.Status.SERVICE_DOWN);
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            /* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));

            final JSONObject match = res.getJSONObject("match");
            final JSONArray crdSysList = common.optJSONArray("crdSysL");
            final List<Location> locations = parseLocList(match.optJSONArray("locL"), crdSysList);
            final List<SuggestedLocation> suggestedLocations = new ArrayList<>(locations.size());
            for (final Location location : locations)
                suggestedLocations.add(new SuggestedLocation(location));
            // TODO weight

            return new SuggestLocationsResult(header, suggestedLocations);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    private static final Joiner JOINER = Joiner.on(' ').skipNulls();

    private Location jsonTripSearchIdentify(final Location location) throws IOException {
        if (location.hasId())
            return location;
        if (location.hasName()) {
            final SuggestLocationsResult result = jsonLocMatch(JOINER.join(location.place, location.name), null, 1);
            if (result.status == SuggestLocationsResult.Status.OK) {
                final List<Location> locations = result.getLocations();
                if (!locations.isEmpty())
                    return locations.get(0);
            }
        }
        if (location.hasCoord()) {
            final NearbyLocationsResult result = jsonLocGeoPos(EnumSet.allOf(LocationType.class), location.coord, 0, 1);
            if (result.status == NearbyLocationsResult.Status.OK) {
                final List<Location> locations = result.locations;
                if (!locations.isEmpty())
                    return locations.get(0);
            }
        }
        return null;
    }

    protected final QueryTripsResult jsonTripSearch(Location from, @Nullable Location via, Location to, final Date time,
            final boolean dep, final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed,
            final @Nullable String moreContext) throws IOException {
        from = jsonTripSearchIdentify(from);
        if (from == null)
            return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT),
                    QueryTripsResult.Status.UNKNOWN_FROM);
        if (via != null) {
            via = jsonTripSearchIdentify(via);
            if (via == null)
                return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT),
                        QueryTripsResult.Status.UNKNOWN_VIA);
        }
        to = jsonTripSearchIdentify(to);
        if (to == null)
            return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_TO);

        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);
        final CharSequence outDate = jsonDate(c);
        final CharSequence outTime = jsonTime(c);
        final CharSequence outFrwd = Boolean.toString(dep);
        final CharSequence jnyFltr = products != null ? productsString(products) : null;
        final String meta = "foot_speed_" + (walkSpeed != null ? walkSpeed : WalkSpeed.NORMAL).name().toLowerCase();
        final CharSequence jsonContext = moreContext != null ? "\"ctxScr\":" + JSONObject.quote(moreContext) + "," : "";
        final String request = wrapJsonApiRequest("TripSearch", "{" //
                + jsonContext //
                + "\"depLocL\":[" + jsonLocation(from) + "]," //
                + "\"arrLocL\":[" + jsonLocation(to) + "]," //
                + (via != null ? "\"viaLocL\":[{\"loc\":" + jsonLocation(via) + "}]," : "") //
                + "\"outDate\":\"" + outDate + "\"," //
                + "\"outTime\":\"" + outTime + "\"," //
                + "\"outFrwd\":" + outFrwd + "," //
                + (jnyFltr != null
                        ? "\"jnyFltrL\":[{\"value\":\"" + jnyFltr + "\",\"mode\":\"BIT\",\"type\":\"PROD\"}]," : "") //
                + "\"gisFltrL\":[{\"mode\":\"FB\",\"profile\":{\"type\":\"F\",\"linDistRouting\":false,\"maxdist\":2000},\"type\":\"M\",\"meta\":\""
                + meta + "\"}]," //
                + "\"getPolyline\":true,\"getPasslist\":true," //
                + "\"getConGroups\":false,\"getIST\":false,\"getEco\":false,\"extChgTime\":-1}", //
                false);

        final HttpUrl url = requestUrl(request);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String headErr = head.optString("err", null);
            if (headErr != null && !"OK".equals(headErr)) {
                final String headErrTxt = head.optString("errTxt");
                throw new RuntimeException(headErr + " " + headErrTxt);
            }

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 2);
            final ResultHeader header = parseServerInfo(svcResList.getJSONObject(0), head.getString("ver"));

            final JSONObject svcRes = svcResList.optJSONObject(1);
            checkState("TripSearch".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.optString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("H890".equals(err)) // No connections found.
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H891".equals(err)) // No route found (try entering an intermediate station).
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H892".equals(err)) // HAFAS Kernel: Request too complex (try entering less intermediate
                                        // stations).
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H895".equals(err)) // Departure/Arrival are too near.
                    return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
                if ("H9220".equals(err)) // Nearby to the given address stations could not be found.
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
                if ("H886".equals(err)) // HAFAS Kernel: No connections found within the requested time
                                        // interval.
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H887".equals(err)) // HAFAS Kernel: Kernel computation time limit reached.
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("H9240".equals(err)) // HAFAS Kernel: Internal error.
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("H9360".equals(err)) // Date outside of the timetable period.
                    return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
                if ("H9380".equals(err)) // Departure/Arrival/Intermediate or equivalent stations def'd more
                                         // than once.
                    return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("PROBLEMS".equals(err) && "HCI Service: problems during service execution".equals(errTxt))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("LOCATION".equals(err) && "HCI Service: location missing or invalid".equals(errTxt))
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_LOCATION);
                if ("CGI_READ_FAILED".equals(err))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("CGI_NO_SERVER".equals(err))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("H_UNKNOWN".equals(err))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            final List<String[]> remarks = parseRemList(common.getJSONArray("remL"));
            final List<Style> styles = parseIcoList(common.getJSONArray("icoL"));
            final JSONArray crdSysList = common.optJSONArray("crdSysL");
            final JSONArray locList = common.getJSONArray("locL");
            final List<String> operators = parseOpList(common.getJSONArray("opL"));
            final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators, styles);
            final List<String> encodedPolylines = parsePolyList(common.getJSONArray("polyL"));

            final JSONArray outConList = res.optJSONArray("outConL");
            final List<Trip> trips = new ArrayList<>(outConList.length());
            for (int iOutCon = 0; iOutCon < outConList.length(); iOutCon++) {
                final JSONObject outCon = outConList.getJSONObject(iOutCon);
                final Location tripFrom = parseLoc(locList, outCon.getJSONObject("dep").getInt("locX"),
                        new HashSet<Integer>(), crdSysList);
                final Location tripTo = parseLoc(locList, outCon.getJSONObject("arr").getInt("locX"),
                        new HashSet<Integer>(), crdSysList);

                c.clear();
                ParserUtils.parseIsoDate(c, outCon.getString("date"));
                final Date baseDate = c.getTime();

                final JSONArray secList = outCon.optJSONArray("secL");
                final List<Trip.Leg> legs = new ArrayList<>(secList.length());
                for (int iSec = 0; iSec < secList.length(); iSec++) {
                    final JSONObject sec = secList.getJSONObject(iSec);
                    final String secType = sec.getString("type");

                    final JSONObject secDep = sec.getJSONObject("dep");
                    final Stop departureStop = parseJsonStop(secDep, locList, crdSysList, c, baseDate);

                    final JSONObject secArr = sec.getJSONObject("arr");
                    final Stop arrivalStop = parseJsonStop(secArr, locList, crdSysList, c, baseDate);

                    final Trip.Leg leg;
                    if (SECTION_TYPE_JOURNEY.equals(secType) || SECTION_TYPE_TELE_TAXI.equals(secType)) {
                        final JSONObject jny = sec.getJSONObject("jny");
                        final Line line = lines.get(jny.getInt("prodX"));
                        final String dirTxt = jny.optString("dirTxt", null);

                        final Location destination;
                        if (dirTxt != null) {
                            final String[] splitDirTxt = splitStationName(dirTxt);
                            destination = new Location(LocationType.ANY, null, splitDirTxt[0], splitDirTxt[1]);
                        } else {
                            destination = null;
                        }

                        final JSONArray stopList = jny.optJSONArray("stopL");
                        final List<Stop> intermediateStops;
                        if (stopList != null) {
                            checkState(stopList.length() >= 2);
                            intermediateStops = new ArrayList<>(stopList.length());
                            for (int iStop = 1; iStop < stopList.length() - 1; iStop++) {
                                final JSONObject stop = stopList.getJSONObject(iStop);
                                final Stop intermediateStop = parseJsonStop(stop, locList, crdSysList, c, baseDate);
                                intermediateStops.add(intermediateStop);
                            }
                        } else {
                            intermediateStops = null;
                        }

                        final List<Point> path;
                        final JSONObject polyG = jny.optJSONObject("polyG");
                        if (polyG != null) {
                            final int crdSysX = polyG.optInt("crdSysX", -1);
                            if (crdSysX != -1) {
                                final String crdSysType = crdSysList.getJSONObject(crdSysX).getString("type");
                                if (!"WGS84".equals(crdSysType))
                                    throw new RuntimeException("unknown type: " + crdSysType);
                            }
                            final JSONArray polyXList = polyG.getJSONArray("polyXL");
                            path = new LinkedList<>();
                            final int polyXListLen = polyXList.length();
                            checkState(polyXListLen <= 1);
                            for (int i = 0; i < polyXListLen; i++) {
                                final String encodedPolyline = encodedPolylines.get(polyXList.getInt(i));
                                path.addAll(PolylineFormat.decode(encodedPolyline));
                            }
                        } else {
                            path = null;
                        }

                        final JSONArray remList = jny.optJSONArray("remL");
                        String message = null;
                        if (remList != null) {
                            for (int iRem = 0; iRem < remList.length(); iRem++) {
                                final JSONObject rem = remList.getJSONObject(iRem);
                                final String[] remark = remarks.get(rem.getInt("remX"));
                                if ("l?".equals(remark[0]))
                                    message = remark[1];
                            }
                        }

                        leg = new Trip.Public(line, destination, departureStop, arrivalStop, intermediateStops, path,
                                message);
                    } else if (SECTION_TYPE_WALK.equals(secType)) {
                        final JSONObject gis = sec.getJSONObject("gis");
                        final int distance = gis.optInt("dist", 0);
                        leg = new Trip.Individual(Trip.Individual.Type.WALK, departureStop.location,
                                departureStop.getDepartureTime(), arrivalStop.location, arrivalStop.getArrivalTime(),
                                null, distance);
                    } else if (SECTION_TYPE_TRANSFER.equals(secType) || SECTION_TYPE_DEVI.equals(secType)) {
                        final JSONObject gis = sec.optJSONObject("gis");
                        final int distance = gis != null ? gis.optInt("dist", 0) : 0;
                        leg = new Trip.Individual(Trip.Individual.Type.TRANSFER, departureStop.location,
                                departureStop.getDepartureTime(), arrivalStop.location, arrivalStop.getArrivalTime(),
                                null, distance);
                    } else {
                        throw new IllegalStateException("cannot handle type: " + secType);
                    }

                    legs.add(leg);
                }

                final List<Fare> fares;
                final JSONObject trfRes = outCon.optJSONObject("trfRes");
                final JSONArray ovwTrfRefList = outCon.optJSONArray("ovwTrfRefL");
                if (trfRes != null && ovwTrfRefList != null) {
                    fares = new LinkedList<>();
                    final JSONArray fareSetList = trfRes.getJSONArray("fareSetL");
                    for (int i = 0; i < ovwTrfRefList.length(); i++) {
                        final JSONObject ovwTrfRef = ovwTrfRefList.getJSONObject(i);
                        final String type = ovwTrfRef.getString("type");
                        final int fareSetX = ovwTrfRef.getInt("fareSetX");
                        final int fareX = ovwTrfRef.getInt("fareX");
                        final JSONObject jsonFareSet = fareSetList.getJSONObject(fareSetX);
                        final JSONObject jsonFare = jsonFareSet.getJSONArray("fareL").getJSONObject(fareX);
                        final String fareName = jsonFare.getString("name");
                        final Fare fare;
                        if (type.equals("T")) { // ticket
                            final int ticketX = ovwTrfRef.getInt("ticketX");
                            final JSONObject jsonTicket = jsonFare.getJSONArray("ticketL").getJSONObject(ticketX);
                            final String ticketName = jsonTicket.getString("name");
                            final String currencyStr = jsonTicket.getString("cur");
                            if (!Strings.isNullOrEmpty(currencyStr)) {
                                final Currency currency = Currency.getInstance(currencyStr);
                                final float price = jsonTicket.getInt("prc") / 100f;
                                fare = new Fare(normalizeFareName(fareName) + '\n' + ticketName,
                                        normalizeFareType(ticketName), currency, price, null, null);
                            } else {
                                fare = null;
                            }
                        } else if (type.equals("F")) { // fare
                            final String currencyStr = jsonFare.optString("cur");
                            if (!Strings.isNullOrEmpty(currencyStr)) {
                                final Currency currency = ParserUtils.getCurrency(currencyStr);
                                final float price = jsonFare.getInt("prc") / 100f;
                                fare = new Fare(normalizeFareName(fareName), normalizeFareType(fareName), currency,
                                        price, null, null);
                            } else {
                                fare = null;
                            }
                        } else {
                            throw new IllegalArgumentException("cannot handle type: " + type);
                        }
                        if (fare != null && !hideFare(fare))
                            fares.add(fare);
                    }
                } else {
                    fares = null;
                }

                final Trip trip = new Trip(null, tripFrom, tripTo, legs, fares, null, null);
                trips.add(trip);
            }

            final JsonContext context = new JsonContext(from, via, to, time, dep, products, walkSpeed,
                    res.optString("outCtxScrF"), res.optString("outCtxScrB"));
            return new QueryTripsResult(header, null, from, null, to, context, trips);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    protected Fare.Type normalizeFareType(final String fareName) {
        final String fareNameLc = fareName.toLowerCase(Locale.US);
        if (fareNameLc.contains("erwachsene") || fareNameLc.contains("adult"))
            return Fare.Type.ADULT;
        if (fareNameLc.contains("kind") || fareNameLc.contains("child") || fareNameLc.contains("kids"))
            return Fare.Type.CHILD;
        if (fareNameLc.contains("ermäßigung"))
            return Fare.Type.CHILD;
        if (fareNameLc.contains("schüler") || fareNameLc.contains("azubi"))
            return Fare.Type.STUDENT;
        if (fareNameLc.contains("fahrrad"))
            return Fare.Type.BIKE;
        if (fareNameLc.contains("senior"))
            return Fare.Type.SENIOR;
        return Fare.Type.ADULT;
    }

    protected String normalizeFareName(final String fareName) {
        return fareName;
    }

    protected boolean hideFare(final Fare fare) {
        return false;
    }

    private String wrapJsonApiRequest(final String meth, final String req, final boolean formatted) {
        return "{" //
                + (apiAuthorization != null ? "\"auth\":" + apiAuthorization + "," : "") //
                + "\"client\":" + checkNotNull(apiClient) + "," //
                + (apiExt != null ? "\"ext\":\"" + apiExt + "\"," : "") //
                + "\"ver\":\"" + checkNotNull(apiVersion) + "\",\"lang\":\"eng\"," //
                + "\"svcReqL\":[" //
                + "{\"meth\":\"ServerInfo\",\"req\":{\"getServerDateTime\":true,\"getTimeTablePeriod\":false}}," //
                + "{\"meth\":\"" + meth + "\",\"cfg\":{\"polyEnc\":\"GPA\"},\"req\":" + req + "}" //
                + "]," //
                + "\"formatted\":" + formatted + "}";
    }

    private HttpUrl requestUrl(final String body) {
        final HttpUrl.Builder url = apiBase.newBuilder().addPathSegment(apiEndpoint);
        if (requestChecksumSalt != null) {
            final HashCode checksum = MD5.newHasher().putString(body, Charsets.UTF_8).putBytes(requestChecksumSalt)
                    .hash();
            url.addQueryParameter("checksum", checksum.toString());
        }
        if (requestMicMacSalt != null) {
            final HashCode mic = MD5.newHasher().putString(body, Charsets.UTF_8).hash();
            url.addQueryParameter("mic", HEX.encode(mic.asBytes()));
            final HashCode mac = MD5.newHasher().putString(HEX.encode(mic.asBytes()), Charsets.UTF_8)
                    .putBytes(requestMicMacSalt).hash();
            url.addQueryParameter("mac", HEX.encode(mac.asBytes()));
        }
        return url.build();
    }

    private String jsonLocation(final Location location) {
        if (location.type == LocationType.STATION && location.hasId())
            return "{\"type\":\"S\",\"extId\":" + JSONObject.quote(location.id) + "}";
        else if (location.type == LocationType.ADDRESS && location.hasId())
            return "{\"type\":\"A\",\"lid\":" + JSONObject.quote(location.id) + "}";
        else if (location.type == LocationType.POI && location.hasId())
            return "{\"type\":\"P\",\"lid\":" + JSONObject.quote(location.id) + "}";
        else
            throw new IllegalArgumentException("cannot handle: " + location);
    }

    private CharSequence jsonDate(final Calendar time) {
        final int year = time.get(Calendar.YEAR);
        final int month = time.get(Calendar.MONTH) + 1;
        final int day = time.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.ENGLISH, "%04d%02d%02d", year, month, day);
    }

    private CharSequence jsonTime(final Calendar time) {
        final int hour = time.get(Calendar.HOUR_OF_DAY);
        final int minute = time.get(Calendar.MINUTE);
        return String.format(Locale.ENGLISH, "%02d%02d00", hour, minute);
    }

    private ResultHeader parseServerInfo(final JSONObject serverInfo, final String serverVersion) throws JSONException {
        checkState("ServerInfo".equals(serverInfo.getString("meth")));
        final String err = serverInfo.optString("err", null);
        if (err != null && !"OK".equals(err)) {
            final String errTxt = serverInfo.optString("errTxt");
            log.info("ServerInfo error: {} {}, ignoring", err, errTxt);
            return new ResultHeader(network, SERVER_PRODUCT, serverVersion, null, 0, null);
        }
        final JSONObject res = serverInfo.getJSONObject("res");
        final Calendar c = new GregorianCalendar(timeZone);
        ParserUtils.parseIsoDate(c, res.getString("sD"));
        c.setTime(parseJsonTime(c, c.getTime(), res.getString("sT")));
        return new ResultHeader(network, SERVER_PRODUCT, serverVersion, null, c.getTimeInMillis(), null);
    }

    private static final Pattern P_JSON_TIME = Pattern.compile("(\\d{2})?(\\d{2})(\\d{2})(\\d{2})");

    private final Date parseJsonTime(final Calendar calendar, final Date baseDate, final CharSequence str) {
        if (str == null)
            return null;

        final Matcher m = P_JSON_TIME.matcher(str);
        if (m.matches()) {
            calendar.setTime(baseDate);

            if (m.group(1) != null)
                calendar.add(Calendar.DAY_OF_YEAR, Integer.parseInt(m.group(1)));
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(2)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
            calendar.set(Calendar.SECOND, Integer.parseInt(m.group(4)));

            return calendar.getTime();
        }

        throw new RuntimeException("cannot parse: '" + str + "'");
    }

    private Position parseJsonPosition(final JSONObject json, final String platfName, final String pltfName)
            throws JSONException {
        final JSONObject pltf = json.optJSONObject(pltfName);
        if (pltf != null)
            return new Position(pltf.getString("txt")); // TODO type
        final String platf = json.optString(platfName, null);
        if (platf != null)
            return normalizePosition(platf);
        return null;
    }

    private Stop parseJsonStop(final JSONObject json, final JSONArray locList, final JSONArray crdSysList,
            final Calendar c, final Date baseDate) throws JSONException {
        final Location location = parseLoc(locList, json.getInt("locX"), new HashSet<Integer>(), crdSysList);

        final boolean arrivalCancelled = json.optBoolean("aCncl", false);
        final Date plannedArrivalTime = parseJsonTime(c, baseDate, json.optString("aTimeS", null));
        final Date predictedArrivalTime = parseJsonTime(c, baseDate, json.optString("aTimeR", null));
        final Position plannedArrivalPosition = parseJsonPosition(json, "aPlatfS", "aPltfS");
        final Position predictedArrivalPosition = parseJsonPosition(json, "aPlatfR", "aPltfR");

        final boolean departureCancelled = json.optBoolean("dCncl", false);
        final Date plannedDepartureTime = parseJsonTime(c, baseDate, json.optString("dTimeS", null));
        final Date predictedDepartureTime = parseJsonTime(c, baseDate, json.optString("dTimeR", null));
        final Position plannedDeparturePosition = parseJsonPosition(json, "dPlatfS", "dPltfS");
        final Position predictedDeparturePosition = parseJsonPosition(json, "dPlatfR", "dPltfR");

        return new Stop(location, plannedArrivalTime, predictedArrivalTime, plannedArrivalPosition,
                predictedArrivalPosition, arrivalCancelled, plannedDepartureTime, predictedDepartureTime,
                plannedDeparturePosition, predictedDeparturePosition, departureCancelled);
    }

    private List<String[]> parseRemList(final JSONArray remList) throws JSONException {
        final List<String[]> remarks = new ArrayList<>(remList.length());

        for (int i = 0; i < remList.length(); i++) {
            final JSONObject rem = remList.getJSONObject(i);
            final String code = rem.optString("code", null);
            final String txtS = rem.optString("txtS", null);
            final String txtN = rem.optString("txtN", null);
            remarks.add(new String[] { code, txtS != null ? txtS : txtN });
        }

        return remarks;
    }

    private List<Style> parseIcoList(final JSONArray icoList) throws JSONException {
        final List<Style> styles = new ArrayList<>(icoList.length());
        for (int i = 0; i < icoList.length(); i++) {
            final JSONObject ico = icoList.getJSONObject(i);
            if (ico.has("bg")) {
                final int background = parseIcoColor(ico.getJSONObject("bg"));
                final JSONObject fg = ico.optJSONObject("fg");
                final int foreground = fg != null ? parseIcoColor(fg) : Style.deriveForegroundColor(background);
                final String shp = ico.optString("shp", null);
                if (shp == null) {
                    styles.add(new Style(background, foreground));
                } else {
                    final Style.Shape shape;
                    if ("C".equals(shp))
                        shape = Style.Shape.CIRCLE;
                    else if ("R".equals(shp))
                        shape = Style.Shape.RECT;
                    else
                        throw new IllegalStateException("cannot handle shp: " + shp);
                    styles.add(new Style(shape, background, foreground));
                }
            } else {
                styles.add(null);
            }
        }
        return styles;
    }

    private int parseIcoColor(final JSONObject color) throws JSONException {
        final int a = color.optInt("a", 255);
        final int r = color.getInt("r");
        final int g = color.getInt("g");
        final int b = color.getInt("b");
        if (r != -1 || g != -1 || b != -1)
            return Style.argb(a, r, g, b);
        else
            return 0;
    }

    private List<Location> parseLocList(final JSONArray locList, final JSONArray crdSysList) throws JSONException {
        final List<Location> locations = new ArrayList<>(locList.length());
        for (int iLoc = 0; iLoc < locList.length(); iLoc++) {
            final Location location = parseLoc(locList, iLoc, new HashSet<Integer>(), crdSysList);
            if (location != null)
                locations.add(location);
        }
        return locations;
    }

    private Location parseLoc(final JSONArray locList, final int locListIndex,
            @Nullable Set<Integer> previousLocListIndexes, final JSONArray crdSysList) throws JSONException {
        final JSONObject loc = locList.getJSONObject(locListIndex);
        final String type = loc.optString("type", null);
        if (type == null)
            return null;

        final LocationType locationType;
        final String id;
        final String[] placeAndName;
        final Set<Product> products;
        if ("S".equals(type)) {
            final int mMastLocX = loc.optInt("mMastLocX", -1);
            if (previousLocListIndexes != null && mMastLocX != -1 && !previousLocListIndexes.contains(mMastLocX)) {
                previousLocListIndexes.add(locListIndex);
                return parseLoc(locList, mMastLocX, previousLocListIndexes, crdSysList);
            }
            locationType = LocationType.STATION;
            id = normalizeStationId(loc.getString("extId"));
            placeAndName = splitStationName(loc.getString("name"));
            final int pCls = loc.optInt("pCls", -1);
            products = pCls != -1 ? intToProducts(pCls) : null;
        } else if ("P".equals(type)) {
            locationType = LocationType.POI;
            id = loc.getString("lid");
            placeAndName = splitPOI(loc.getString("name"));
            products = null;
        } else if ("A".equals(type)) {
            locationType = LocationType.ADDRESS;
            id = loc.getString("lid");
            placeAndName = splitAddress(loc.getString("name"));
            products = null;
        } else {
            throw new RuntimeException("Unknown type " + type + ": " + loc);
        }

        final Point coord;
        final JSONObject crd = loc.optJSONObject("crd");
        if (crd != null) {
            final int crdSysX = loc.optInt("crdSysX", -1);
            if (crdSysX != -1) {
                final String crdSysType = crdSysList.getJSONObject(crdSysX).getString("type");
                if (!"WGS84".equals(crdSysType))
                    throw new RuntimeException("unknown type: " + crdSysType);
            }
            coord = Point.from1E6(crd.getInt("y"), crd.getInt("x"));
        } else {
            coord = null;
        }

        return new Location(locationType, id, coord, placeAndName[0], placeAndName[1], products);
    }

    private List<String> parseOpList(final JSONArray opList) throws JSONException {
        final List<String> operators = new ArrayList<>(opList.length());

        for (int i = 0; i < opList.length(); i++) {
            final JSONObject op = opList.getJSONObject(i);
            final String operator = op.getString("name");
            operators.add(operator);
        }

        return operators;
    }

    private List<Line> parseProdList(final JSONArray prodList, final List<String> operators, final List<Style> styles)
            throws JSONException {
        final int prodListLen = prodList.length();
        final List<Line> lines = new ArrayList<>(prodListLen);

        for (int iProd = 0; iProd < prodListLen; iProd++) {
            final JSONObject prod = prodList.getJSONObject(iProd);
            final String name = Strings.emptyToNull(prod.getString("name"));
            final String nameS = prod.optString("nameS", null);
            final String number = prod.optString("number", null);
            final int icoIndex = prod.getInt("icoX");
            final Style style = styles.get(icoIndex);
            final int oprIndex = prod.optInt("oprX", -1);
            final String operator = oprIndex != -1 ? operators.get(oprIndex) : null;
            final int cls = prod.optInt("cls", -1);
            final JSONObject prodCtx = prod.optJSONObject("prodCtx");
            final String id = prodCtx != null ? prodCtx.optString("lineId", null) : null;
            final Product product = cls != -1 ? intToProduct(cls) : null;
            lines.add(newLine(id, operator, product, name, nameS, number, style));
        }

        return lines;
    }

    private List<String> parsePolyList(final JSONArray polyList) throws JSONException {
        final int len = polyList.length();
        final List<String> polylines = new ArrayList<>(len);

        for (int i = 0; i < len; i++) {
            final JSONObject poly = polyList.getJSONObject(i);
            checkState(poly.getBoolean("delta"));
            polylines.add(poly.getString("crdEncYX"));
        }
        return polylines;
    }

    protected Line newLine(final String id, final String operator, final Product product, final @Nullable String name,
            final @Nullable String shortName, final @Nullable String number, final Style style) {
        final String longName;
        if (name != null)
            longName = name + (number != null && !name.endsWith(number) ? " (" + number + ")" : "");
        else if (shortName != null)
            longName = shortName + (number != null && !shortName.endsWith(number) ? " (" + number + ")" : "");
        else
            longName = number;

        if (product == Product.BUS || product == Product.TRAM) {
            // For bus and tram, prefer a slightly shorter label without the product prefix
            final String label;
            if (shortName != null)
                label = shortName;
            else if (number != null && name != null && name.endsWith(number))
                label = number;
            else
                label = name;
            return new Line(id, operator, product, label, longName, lineStyle(operator, product, label));
        } else {
            // Otherwise the longer label is fine
            return new Line(id, operator, product, name, longName, lineStyle(operator, product, name));
        }
    }

    @SuppressWarnings("serial")
    public static class JsonContext implements QueryTripsContext {
        public final Location from, via, to;
        public final Date date;
        public final boolean dep;
        public final Set<Product> products;
        public final WalkSpeed walkSpeed;
        public final String laterContext, earlierContext;

        public JsonContext(final Location from, final @Nullable Location via, final Location to, final Date date,
                final boolean dep, final Set<Product> products, final WalkSpeed walkSpeed, final String laterContext,
                final String earlierContext) {
            this.from = from;
            this.via = via;
            this.to = to;
            this.date = date;
            this.dep = dep;
            this.products = products;
            this.walkSpeed = walkSpeed;
            this.laterContext = laterContext;
            this.earlierContext = earlierContext;
        }

        @Override
        public boolean canQueryLater() {
            return laterContext != null;
        }

        @Override
        public boolean canQueryEarlier() {
            return earlierContext != null;
        }
    }

    public static final byte[] decryptSalt(final String encryptedSalt, final String saltEncryptionKey) {
        try {
            final byte[] key = BaseEncoding.base16().lowerCase().decode(saltEncryptionKey);
            checkState(key.length * 8 == 128, "encryption key must be 128 bits");
            final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[16]);
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            return cipher.doFinal(BaseEncoding.base64().decode(encryptedSalt));
        } catch (final GeneralSecurityException x) {
            // should not happen
            throw new RuntimeException(x);
        }
    }
}
