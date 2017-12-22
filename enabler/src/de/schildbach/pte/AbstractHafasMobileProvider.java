/*
 * Copyright 2010-2017 the original author or authors.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
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
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.util.ParserUtils;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasMobileProvider extends AbstractHafasProvider {
    public HttpUrl mgateEndpoint;
    @Nullable
    public String apiVersion;
    @Nullable
    public String apiAuthorization;
    @Nullable
    public String apiClient;

    public AbstractHafasMobileProvider(final NetworkId network, final HttpUrl apiBase, final Product[] productsMap) {
        super(network, productsMap);
        this.mgateEndpoint = apiBase.newBuilder().addPathSegment("mgate.exe").build();
    }

    protected AbstractHafasMobileProvider setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    protected AbstractHafasMobileProvider setApiAuthorization(final String apiAuthorization) {
        this.apiAuthorization = apiAuthorization;
        return this;
    }

    protected AbstractHafasMobileProvider setApiClient(final String apiClient) {
        this.apiClient = apiClient;
        return this;
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasLocation())
            return jsonLocGeoPos(types, location.lat, location.lon, maxDistance, maxLocations);
        else
            throw new IllegalArgumentException("cannot handle: " + location);
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        return jsonStationBoard(stationId, time, maxDepartures, equivs);
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException {
        return jsonLocMatch(constraint);
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable Set<Product> products,
            final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
            final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException {
        return jsonTripSearch(from, via, to, date, dep, products, null);
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException {
        final JsonContext jsonContext = (JsonContext) context;
        return jsonTripSearch(jsonContext.from, jsonContext.via, jsonContext.to, jsonContext.date, jsonContext.dep,
                jsonContext.products, later ? jsonContext.laterContext : jsonContext.earlierContext);
    }

    protected final NearbyLocationsResult jsonLocGeoPos(final EnumSet<LocationType> types, final int lat, final int lon, int maxDistance, int maxLocations)
            throws IOException {
        final boolean getPOIs = types.contains(LocationType.POI);
        final String request = wrapJsonApiRequest("LocGeoPos",
                "{\"ring\":" //
                        + "{\"cCrd\":{\"x\":" + lon + ",\"y\":" + lat + "}" + (maxDistance > 0 ? ",\"maxDist\":" + maxDistance : "") + "}," //
                        + (maxLocations > 0 ? "\"maxLoc\":" + maxLocations + "," : "") //
                        + "\"getPOIs\":" + getPOIs + "}", //
                false);

        final HttpUrl url = checkNotNull(mgateEndpoint);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = parseJsonPage(page);
            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), null, 0, null);

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 1);
            final JSONObject svcRes = svcResList.optJSONObject(0);
            checkState("LocGeoPos".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.getString("errTxt");
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            /* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));

            final JSONArray locL = res.optJSONArray("locL");
            final List<Location> locations;
            if (locL != null) {
                locations = parseLocList(locL);

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
            final int maxDepartures, final boolean equivs) throws IOException {
        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);
        final CharSequence jsonDate = jsonDate(c);
        final CharSequence jsonTime = jsonTime(c);
        final CharSequence normalizedStationId = normalizeStationId(stationId);
        final CharSequence stbFltrEquiv = Boolean.toString(!equivs);
        final CharSequence maxJny = Integer.toString(maxDepartures != 0 ? maxDepartures : DEFAULT_MAX_DEPARTURES);
        final CharSequence getPasslist = Boolean.toString(true); // traffic expensive
        final String request = wrapJsonApiRequest("StationBoard",
                "{\"type\":\"DEP\"," //
                        + "\"date\":\"" + jsonDate + "\"," //
                        + "\"time\":\"" + jsonTime + "\"," //
                        + "\"stbLoc\":{\"type\":\"S\"," + "\"state\":\"F\"," // F/M
                        + "\"extId\":" + JSONObject.quote(normalizedStationId.toString()) + "}," //
                        + ("1.20".equals(apiVersion) ? "" : "\"stbFltrEquiv\":" + stbFltrEquiv + ",\"getPasslist\":" + getPasslist + ",") //
                        + "\"maxJny\":" + maxJny + "}",
                false);

        final HttpUrl url = checkNotNull(mgateEndpoint);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = parseJsonPage(page);
            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), null, 0, null);
            final QueryDeparturesResult result = new QueryDeparturesResult(header);

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 1);
            final JSONObject svcRes = svcResList.optJSONObject(0);
            checkState("StationBoard".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.getString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("LOCATION".equals(err) && "HCI Service: location missing or invalid".equals(errTxt))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
                throw new RuntimeException(err + " " + errTxt);
            } else if ("1.10".equals(apiVersion) && svcRes.toString().length() == 170) {
                // horrible hack, because API version 1.10 doesn't signal invalid stations via error
                return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            final List<String[]> remarks = parseRemList(common.getJSONArray("remL"));
            final List<String> operators = parseOpList(common.getJSONArray("opL"));
            final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators);
            final JSONArray locList = common.getJSONArray("locL");
            final List<Location> locations = parseLocList(locList);

            final JSONArray jnyList = res.optJSONArray("jnyL");
            if (jnyList != null) {
                for (int iJny = 0; iJny < jnyList.length(); iJny++) {
                    final JSONObject jny = jnyList.getJSONObject(iJny);
                    final JSONObject stbStop = jny.getJSONObject("stbStop");

                    final String stbStopPlatformS = stbStop.optString("dPlatfS", null);
                    c.clear();
                    ParserUtils.parseIsoDate(c, jny.getString("date"));
                    final Date baseDate = c.getTime();

                    final Date plannedTime = parseJsonTime(c, baseDate, stbStop.getString("dTimeS"));

                    final Date predictedTime = parseJsonTime(c, baseDate, stbStop.optString("dTimeR", null));

                    final Line line = lines.get(stbStop.getInt("dProdX"));

                    final Location location = equivs ? locations.get(stbStop.getInt("locX"))
                            : new Location(LocationType.STATION, stationId);
                    final Position position = normalizePosition(stbStopPlatformS);

                    final String jnyDirTxt = jny.getString("dirTxt");
                    final JSONArray stopList = jny.optJSONArray("stopL");
                    final Location destination;
                    if (stopList != null) {
                        final int lastStopIdx = stopList.getJSONObject(stopList.length() - 1).getInt("locX");
                        final String lastStopName = locList.getJSONObject(lastStopIdx).getString("name");
                        if (jnyDirTxt.equals(lastStopName))
                            destination = locations.get(lastStopIdx);
                        else
                            destination = new Location(LocationType.ANY, null, null, jnyDirTxt);
                    } else {
                        destination = new Location(LocationType.ANY, null, null, jnyDirTxt);
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

                    final Departure departure = new Departure(plannedTime, predictedTime, line, position, destination,
                            null, message);

                    StationDepartures stationDepartures = findStationDepartures(result.stationDepartures, location);
                    if (stationDepartures == null) {
                        stationDepartures = new StationDepartures(location, new ArrayList<Departure>(8), null);
                        result.stationDepartures.add(stationDepartures);
                    }

                    stationDepartures.departures.add(departure);
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

    protected final SuggestLocationsResult jsonLocMatch(final CharSequence constraint) throws IOException {
        final String request = wrapJsonApiRequest("LocMatch",
                "{\"input\":{\"field\":\"S\",\"loc\":{\"name\":" + JSONObject.quote(checkNotNull(constraint).toString())
                        + ",\"meta\":false},\"maxLoc\":" + DEFAULT_MAX_LOCATIONS + "}}",
                true);

        final HttpUrl url = checkNotNull(mgateEndpoint);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = parseJsonPage(page);
            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), null, 0, null);

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 1);
            final JSONObject svcRes = svcResList.optJSONObject(0);
            checkState("LocMatch".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.getString("errTxt");
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            /* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));

            final JSONObject match = res.getJSONObject("match");
            final List<Location> locations = parseLocList(match.optJSONArray("locL"));
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

    private static JSONObject parseJsonPage(CharSequence page) throws JSONException {
        final JSONObject head = new JSONObject(page.toString());
        final String headErr = head.optString("err", null);
        final String headErrText = head.optString("errTxt", null);
        if ("OK".equals(headErr))
            return head;
        if (headErr != null && headErrText != null)
            throw new RuntimeException(headErr + ": " + headErrText);
        else if (headErr != null)
            throw new RuntimeException(headErr);
        else
            return head;
    }

    private Location jsonTripSearchIdentify(final Location location) throws IOException {
        if (location.hasName()) {
            final List<Location> locations = jsonLocMatch(JOINER.join(location.place, location.name)).getLocations();
            if (!locations.isEmpty())
                return locations.get(0);
        }
        if (location.hasLocation()) {
            final List<Location> locations = jsonLocGeoPos(EnumSet.allOf(LocationType.class), location.lat,
                    location.lon, 0, 0).locations;
            if (!locations.isEmpty())
                return locations.get(0);
        }
        return null;
    }

    protected final QueryTripsResult jsonTripSearch(Location from, @Nullable Location via, Location to, final Date time,
            final boolean dep, final @Nullable Set<Product> products, final String moreContext) throws IOException {
        if (!from.hasId()) {
            from = jsonTripSearchIdentify(from);
            if (from == null)
                return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT),
                        QueryTripsResult.Status.UNKNOWN_FROM);
        }

        if (via != null && !via.hasId()) {
            via = jsonTripSearchIdentify(via);
            if (via == null)
                return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT),
                        QueryTripsResult.Status.UNKNOWN_VIA);
        }

        if (!to.hasId()) {
            to = jsonTripSearchIdentify(to);
            if (to == null)
                return new QueryTripsResult(new ResultHeader(network, SERVER_PRODUCT),
                        QueryTripsResult.Status.UNKNOWN_TO);
        }

        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);
        final CharSequence outDate = jsonDate(c);
        final CharSequence outTime = jsonTime(c);
        final CharSequence outFrwdKey = "1.10".equals(apiVersion) ? "frwd" : "outFrwd";
        final CharSequence outFrwd = Boolean.toString(dep);
        final CharSequence jnyFltr = productsString(products);
        final CharSequence jsonContext = moreContext != null ? "\"ctxScr\":" + JSONObject.quote(moreContext) + "," : "";
        final String request = wrapJsonApiRequest("TripSearch", "{" //
                + jsonContext //
                + "\"depLocL\":[" + jsonLocation(from) + "]," //
                + "\"arrLocL\":[" + jsonLocation(to) + "]," //
                + (via != null ? "\"viaLocL\":[{\"loc\":" + jsonLocation(via) + "}]," : "") //
                + "\"outDate\":\"" + outDate + "\"," //
                + "\"outTime\":\"" + outTime + "\"," //
                + "\"" + outFrwdKey + "\":" + outFrwd + "," //
                + "\"jnyFltrL\":[{\"value\":\"" + jnyFltr + "\",\"mode\":\"BIT\",\"type\":\"PROD\"}]," //
                + "\"gisFltrL\":[{\"mode\":\"FB\",\"profile\":{\"type\":\"F\",\"linDistRouting\":false,\"maxdist\":2000},\"type\":\"P\"}]," //
                + "\"getPolyline\":false,\"getPasslist\":true,\"getIST\":false,\"getEco\":false,\"extChgTime\":-1}", //
                false);

        final HttpUrl url = checkNotNull(mgateEndpoint);
        final CharSequence page = httpClient.get(url, request, "application/json");

        try {
            final JSONObject head = parseJsonPage(page);
            final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), null, 0, null);

            final JSONArray svcResList = head.getJSONArray("svcResL");
            checkState(svcResList.length() == 1);
            final JSONObject svcRes = svcResList.optJSONObject(0);
            checkState("TripSearch".equals(svcRes.getString("meth")));
            final String err = svcRes.getString("err");
            if (!"OK".equals(err)) {
                final String errTxt = svcRes.getString("errTxt");
                log.debug("Hafas error: {} {}", err, errTxt);
                if ("H890".equals(err)) // No connections found.
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H891".equals(err)) // No route found (try entering an intermediate station).
                    return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
                if ("H895".equals(err)) // Departure/Arrival are too near.
                    return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
                if ("H9220".equals(err)) // Nearby to the given address stations could not be found.
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
                if ("H9240".equals(err)) // HAFAS Kernel: Internal error.
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("H9360".equals(err)) // Date outside of the timetable period.
                    return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
                if ("H9380".equals(err)) // Departure/Arrival/Intermediate or equivalent stations def'd more
                                         // than once.
                    return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
                if ("FAIL".equals(err) && "HCI Service: request failed".equals(errTxt))
                    return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
                if ("LOCATION".equals(err) && "HCI Service: location missing or invalid".equals(errTxt))
                    return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_LOCATION);
                throw new RuntimeException(err + " " + errTxt);
            }
            final JSONObject res = svcRes.getJSONObject("res");

            final JSONObject common = res.getJSONObject("common");
            final List<String[]> remarks = parseRemList(common.getJSONArray("remL"));
            final List<Location> locations = parseLocList(common.getJSONArray("locL"));
            final List<String> operators = parseOpList(common.getJSONArray("opL"));
            final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators);

            final JSONArray outConList = res.optJSONArray("outConL");
            final List<Trip> trips = new ArrayList<>(outConList.length());
            for (int iOutCon = 0; iOutCon < outConList.length(); iOutCon++) {
                final JSONObject outCon = outConList.getJSONObject(iOutCon);
                final Location tripFrom = locations.get(outCon.getJSONObject("dep").getInt("locX"));
                final Location tripTo = locations.get(outCon.getJSONObject("arr").getInt("locX"));

                c.clear();
                ParserUtils.parseIsoDate(c, outCon.getString("date"));
                final Date baseDate = c.getTime();

                final JSONArray secList = outCon.optJSONArray("secL");
                final List<Trip.Leg> legs = new ArrayList<>(secList.length());
                for (int iSec = 0; iSec < secList.length(); iSec++) {
                    final JSONObject sec = secList.getJSONObject(iSec);
                    final String secType = sec.getString("type");

                    final JSONObject secDep = sec.getJSONObject("dep");
                    final Stop departureStop = parseJsonStop(secDep, locations, c, baseDate);

                    final JSONObject secArr = sec.getJSONObject("arr");
                    final Stop arrivalStop = parseJsonStop(secArr, locations, c, baseDate);

                    final Trip.Leg leg;
                    if ("JNY".equals(secType)) {
                        final JSONObject jny = sec.getJSONObject("jny");
                        final Line line = lines.get(jny.getInt("prodX"));
                        final String dirTxt = jny.optString("dirTxt", null);
                        final Location destination = dirTxt != null ? new Location(LocationType.ANY, null, null, dirTxt)
                                : null;

                        final JSONArray stopList = jny.getJSONArray("stopL");
                        checkState(stopList.length() >= 2);
                        final List<Stop> intermediateStops = new ArrayList<>(stopList.length());
                        for (int iStop = 1; iStop < stopList.length() - 1; iStop++) {
                            final JSONObject stop = stopList.getJSONObject(iStop);
                            final Stop intermediateStop = parseJsonStop(stop, locations, c, baseDate);
                            intermediateStops.add(intermediateStop);
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

                        leg = new Trip.Public(line, destination, departureStop, arrivalStop, intermediateStops, null,
                                message);
                    } else if ("WALK".equals(secType) || "TRSF".equals(secType)) {
                        final JSONObject gis = sec.getJSONObject("gis");
                        final int distance = gis.optInt("dist", 0);
                        leg = new Trip.Individual(Trip.Individual.Type.WALK, departureStop.location,
                                departureStop.getDepartureTime(), arrivalStop.location, arrivalStop.getArrivalTime(),
                                null, distance);
                    } else {
                        throw new IllegalStateException("cannot handle type: " + secType);
                    }

                    legs.add(leg);
                }

                final JSONObject trfRes = outCon.optJSONObject("trfRes");
                final List<Fare> fares = new LinkedList<>();
                if (trfRes != null) {
                    final JSONArray fareSetList = trfRes.getJSONArray("fareSetL");
                    for (int iFareSet = 0; iFareSet < fareSetList.length(); iFareSet++) {
                        final JSONObject fareSet = fareSetList.getJSONObject(iFareSet);
                        final String fareSetName = fareSet.optString("name", null);
                        final String fareSetDescription = fareSet.optString("desc", null);
                        if (fareSetName != null || fareSetDescription != null) {
                            final JSONArray fareList = fareSet.getJSONArray("fareL");
                            for (int iFare = 0; iFare < fareList.length(); iFare++) {
                                final JSONObject jsonFare = fareList.getJSONObject(iFare);
                                final String name = jsonFare.getString("name");
                                final JSONArray ticketList = jsonFare.optJSONArray("ticketL");
                                if (ticketList != null) {
                                    for (int iTicket = 0; iTicket < ticketList.length(); iTicket++) {
                                        final JSONObject jsonTicket = ticketList.getJSONObject(iTicket);
                                        final String ticketName = jsonTicket.getString("name");
                                        final Currency currency = Currency.getInstance(jsonTicket.getString("cur"));
                                        final float price = jsonTicket.getInt("prc") / 100f;
                                        final Fare fare = parseJsonTripFare(name, fareSetDescription, ticketName,
                                                currency, price);
                                        if (fare != null)
                                            fares.add(fare);
                                    }
                                } else {
                                    final Currency currency = Currency.getInstance(jsonFare.getString("cur"));
                                    final float price = jsonFare.getInt("prc") / 100f;
                                    final Fare fare = parseJsonTripFare(fareSetName, fareSetDescription, name, currency,
                                            price);
                                    if (fare != null)
                                        fares.add(fare);
                                }
                            }
                        }
                    }
                }

                final Trip trip = new Trip(null, tripFrom, tripTo, legs, fares, null, null);
                trips.add(trip);
            }

            final JsonContext context = new JsonContext(from, via, to, time, dep, products, res.optString("outCtxScrF"),
                    res.optString("outCtxScrB"));
            return new QueryTripsResult(header, null, from, null, to, context, trips);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    protected Fare parseJsonTripFare(final @Nullable String fareSetName, final @Nullable String fareSetDescription,
            final String name, final Currency currency, final float price) {
        if (name.endsWith("- Jahreskarte") || name.endsWith("- Monatskarte"))
            return null;
        if (name.startsWith("Vollpreis - "))
            return new Fare(fareSetName, Fare.Type.ADULT, currency, price, name.substring(12), null);
        if (name.startsWith("Kind - "))
            return new Fare(fareSetName, Fare.Type.CHILD, currency, price, name.substring(7), null);
        return null;
    }

    private String wrapJsonApiRequest(final String meth, final String req, final boolean formatted) {
        return "{" //
                + "\"auth\":" + checkNotNull(apiAuthorization) + "," //
                + "\"client\":" + checkNotNull(apiClient) + "," //
                + "\"ver\":\"" + checkNotNull(apiVersion) + "\",\"lang\":\"eng\"," //
                + "\"svcReqL\":[{\"cfg\":{\"polyEnc\":\"GPA\"},\"meth\":\"" + meth + "\",\"req\":" + req + "}]," //
                + "\"formatted\":" + formatted + "}";
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

    private Stop parseJsonStop(final JSONObject json, final List<Location> locations, final Calendar c,
            final Date baseDate) throws JSONException {
        final Location location = locations.get(json.getInt("locX"));

        final boolean arrivalCancelled = json.optBoolean("aCncl", false);
        final Date plannedArrivalTime = parseJsonTime(c, baseDate, json.optString("aTimeS", null));
        final Date predictedArrivalTime = parseJsonTime(c, baseDate, json.optString("aTimeR", null));
        final Position plannedArrivalPosition = normalizePosition(json.optString("aPlatfS", null));
        final Position predictedArrivalPosition = normalizePosition(json.optString("aPlatfR", null));

        final boolean departureCancelled = json.optBoolean("dCncl", false);
        final Date plannedDepartureTime = parseJsonTime(c, baseDate, json.optString("dTimeS", null));
        final Date predictedDepartureTime = parseJsonTime(c, baseDate, json.optString("dTimeR", null));
        final Position plannedDeparturePosition = normalizePosition(json.optString("dPlatfS", null));
        final Position predictedDeparturePosition = normalizePosition(json.optString("dPlatfR", null));

        return new Stop(location, plannedArrivalTime, predictedArrivalTime, plannedArrivalPosition,
                predictedArrivalPosition, arrivalCancelled, plannedDepartureTime, predictedDepartureTime,
                plannedDeparturePosition, predictedDeparturePosition, departureCancelled);
    }

    private List<String[]> parseRemList(final JSONArray remList) throws JSONException {
        final List<String[]> remarks = new ArrayList<>(remList.length());

        for (int i = 0; i < remList.length(); i++) {
            final JSONObject rem = remList.getJSONObject(i);
            final String code = rem.getString("code");
            final String txt = rem.getString("txtN");
            remarks.add(new String[] { code, txt });
        }

        return remarks;
    }

    private List<Location> parseLocList(final JSONArray locList) throws JSONException {
        final List<Location> locations = new ArrayList<>(locList.length());

        for (int iLoc = 0; iLoc < locList.length(); iLoc++) {
            final JSONObject loc = locList.getJSONObject(iLoc);
            final String type = loc.getString("type");

            final LocationType locationType;
            final String id;
            final String[] placeAndName;
            final Set<Product> products;
            if ("S".equals(type)) {
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

            final JSONObject crd = loc.optJSONObject("crd");
            if (crd != null)
                locations.add(new Location(locationType, id, crd.getInt("y"), crd.getInt("x"), placeAndName[0],
                        placeAndName[1], products));
            else
                locations.add(new Location(LocationType.STATION, id, null, placeAndName[0], placeAndName[1], products));
        }

        return locations;
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

    private List<Line> parseProdList(final JSONArray prodList, final List<String> operators) throws JSONException {
        final List<Line> lines = new ArrayList<>(prodList.length());

        for (int iProd = 0; iProd < prodList.length(); iProd++) {
            final JSONObject prod = prodList.getJSONObject(iProd);
            final int oprIndex = prod.optInt("oprX", -1);
            final String operator = oprIndex != -1 ? operators.get(oprIndex) : null;
            final int cls = prod.optInt("cls", -1);
            final Product product = cls != -1 ? intToProduct(cls) : null;
            final String name = prod.getString("name");
            lines.add(newLine(operator, product, name));
        }

        return lines;
    }

    protected Line newLine(final String operator, final Product product, final String name) {
        final String normalizedName;
        if (product == Product.BUS && name.startsWith("Bus "))
            normalizedName = name.substring(4);
        else if (product == Product.TRAM && name.startsWith("Tram "))
            normalizedName = name.substring(5);
        else if (product == Product.SUBURBAN_TRAIN && name.startsWith("S "))
            normalizedName = "S" + name.substring(2);
        else
            normalizedName = name;
        return new Line(null, operator, product, normalizedName, lineStyle(operator, product, normalizedName));
    }

    @SuppressWarnings("serial")
    public static class JsonContext implements QueryTripsContext {
        public final Location from, via, to;
        public final Date date;
        public final boolean dep;
        public final Set<Product> products;
        public final String laterContext, earlierContext;

        public JsonContext(final Location from, final @Nullable Location via, final Location to, final Date date,
                final boolean dep, final Set<Product> products, final String laterContext,
                final String earlierContext) {
            this.from = from;
            this.via = via;
            this.to = to;
            this.date = date;
            this.dep = dep;
            this.products = products;
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
}