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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.schildbach.pte.dto.Departure;
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
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.ParserException;
import okhttp3.HttpUrl;

/**
 * Provider implementation for Deutsche Bahn (Germany).
 * 
 * @author Andreas Schildbach
 */
public final class DbMovasProvider extends AbstractNetworkProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://app.vendo.noncd.db.de/mob/");
    private static final Map<String, Product> PRODUCTS_MAP = new HashMap<String, Product>() {
        {
            put("HOCHGESCHWINDIGKEITSZUEGE", Product.HIGH_SPEED_TRAIN);
            put("INTERCITYUNDEUROCITYZUEGE", Product.HIGH_SPEED_TRAIN);
            put("ICE", Product.HIGH_SPEED_TRAIN);
            put("IC_EC", Product.HIGH_SPEED_TRAIN);
            put("IC", Product.HIGH_SPEED_TRAIN);
            put("EC", Product.HIGH_SPEED_TRAIN);
            put("INTERREGIOUNDSCHNELLZUEGE", Product.HIGH_SPEED_TRAIN);
            put("IR", Product.HIGH_SPEED_TRAIN);
            put("NAHVERKEHRSONSTIGEZUEGE", Product.REGIONAL_TRAIN);
            put("RB", Product.REGIONAL_TRAIN);
            put("RE", Product.REGIONAL_TRAIN);
            put("SBAHNEN", Product.SUBURBAN_TRAIN);
            put("SBAHN", Product.SUBURBAN_TRAIN);
            put("BUSSE", Product.BUS);
            put("BUS", Product.BUS);
            put("SCHIFFE", Product.FERRY);
            put("SCHIFF", Product.FERRY);
            put("UBAHN", Product.SUBWAY);
            put("STRASSENBAHN", Product.TRAM);
            put("STR", Product.TRAM);
            put("ANRUFPFLICHTIGEVERKEHRE", Product.ON_DEMAND);
        }
    };

    private static final Map<String, LocationType> LOCATION_TYPE_MAP = new HashMap<String, LocationType>() {
        {
            put("1", LocationType.STATION);
            put("4", LocationType.POI);
            put("2", LocationType.ADDRESS);
        }
    };

    private static final int DEFAULT_MAX_LOCATIONS = 50;
    private static final int DEFAULT_MAX_DISTANCE = 10000;

    private final HttpUrl departureEndpoint;
    private final HttpUrl tripEndpoint;
    private final HttpUrl locationsEndpoint;
    private final HttpUrl nearbyEndpoint;

    private TimeZone timeZone = TimeZone.getTimeZone("CET");

    public DbMovasProvider() {
        super(NetworkId.DBMOVAS);
        this.departureEndpoint = API_BASE.newBuilder().addPathSegments("bahnhofstafel/abfahrt").build();
        this.tripEndpoint = API_BASE.newBuilder().addPathSegments("angebote/fahrplan").build();
        this.locationsEndpoint = API_BASE.newBuilder().addPathSegments("location/search").build();
        this.nearbyEndpoint = API_BASE.newBuilder().addPathSegments("location/nearby").build();
    }

    private String doRequest(final HttpUrl url, final String body, final String contentType) throws IOException {
        httpClient.setHeader("Accept", contentType);
        httpClient.setHeader("Content-Type", contentType);
        httpClient.setHeader("X-Correlation-ID", "null");
        final CharSequence page = httpClient.get(url, body, null); // Content-Type must be exactly as passed above (no
                                                                   // charset)
        return page.toString();
    }

    private String createLidEntry(final String key, final Object value) {
        return key + "=" + value + "@";
    }

    private String formatLid(final Location loc) {
        final String typeId = LOCATION_TYPE_MAP
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == loc.type)
                .findFirst()
                .map(e -> e.getKey())
                .orElse("2");

        final StringBuilder out = new StringBuilder();
        out.append(createLidEntry("A", typeId));
        if (loc.name != null) {
            out.append(createLidEntry("O", loc.name));
        }
        if (loc.coord != null) {
            out.append(createLidEntry("X", loc.coord.getLonAs1E6()));
            out.append(createLidEntry("Y", loc.coord.getLatAs1E6()));
        }
        if (loc.id != null) {
            out.append(createLidEntry("L", normalizeStationId(loc.id)));
        }
        return out.toString();
    }

    private String formatLid(final String stationId) {
        return formatLid(new Location(LocationType.STATION, stationId));
    }

    private Location parseLid(final String loc) {
        if (loc == null)
            return null;
        final Map<String, String> props = Arrays.stream(loc.split("@"))
                .map(chunk -> chunk.split("="))
                .filter(e -> e.length == 2)
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));
        Point coord = null;
        try {
            coord = Point.from1E6(Integer.parseInt(props.get("Y")), Integer.parseInt(props.get("X")));
        } catch (Exception e) {
        }
        return new Location(
                Optional.ofNullable(LOCATION_TYPE_MAP.get(props.get("A"))).orElse(LocationType.ADDRESS),
                props.get("L"),
                coord,
                null,
                props.get("O"));
    }

    private Set<Product> parseProducts(final JSONArray products) {
        if (products == null)
            return null;
        final Set<Product> out = new HashSet<>();
        for (int i = 0; i < products.length(); i++) {
            final Product p = PRODUCTS_MAP.get(products.optString(i, null));
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private Location parseLocation(JSONObject loc) {
        if (loc == null)
            return null;
        final Location lid = parseLid(loc.optString("locationId", null));
        Point coord = null;
        JSONObject pos = loc.optJSONObject("coordinates");
        if (pos == null) {
            pos = loc.optJSONObject("position");
        }
        if (pos != null) {
            coord = Point.fromDouble(pos.optDouble("latitude"), pos.optDouble("longitude"));
        } else {
            coord = lid.coord;
        }

        return parseLocation(
                lid.type,
                Optional.ofNullable(loc.optString("evaNr", null)).orElse(lid.id),
                coord,
                loc.optString("name", null),
                parseProducts(loc.optJSONArray("products")));
    }

    private Location parseLocation(final LocationType type, final String id, final Point coord, String name,
            final Set<Product> products) {
        String place = null;
        if (name != null) {
            String[] split = name.split(", ");
            if (split.length > 1) {
                place = split[split.length - 1];
                name = Arrays.stream(split).limit(split.length - 1).collect(Collectors.joining(", "));
            }
        }
        return new Location(type, id, coord, place, name, products);
    }

    private List<String> parseMessages(final JSONObject e) throws JSONException {
        final JSONArray msgs = e.optJSONArray("echtzeitNotizen");
        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < msgs.length(); i++) {
            final JSONObject msgObj = msgs.getJSONObject(i);
            final String msg = msgObj.optString("text", null);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    private Line parseLine(final JSONObject e) {
        final Product p = PRODUCTS_MAP.get(e.optString("produktGattung", null));
        final String name = e.optString("mitteltext", null);
        String shortName = name;
        if (name != null) {
            shortName = name.replaceAll("^[A-Za-z]+ ", "");
        }
        return new Line(
                e.optString("zuglaufId", null),
                null,
                p,
                shortName,
                name,
                lineStyle(null, p, name));
    }

    private CharSequence jsonDate(final Calendar time) {
        final int year = time.get(Calendar.YEAR);
        final int month = time.get(Calendar.MONTH) + 1;
        final int day = time.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month, day);
    }

    private CharSequence jsonTime(final Calendar time) {
        final int hour = time.get(Calendar.HOUR_OF_DAY);
        final int minute = time.get(Calendar.MINUTE);
        return String.format(Locale.ENGLISH, "%02d:%02d", hour, minute);
    }

    private Date parseIso8601WOffset(final String time) {
        if (time == null)
            return null;
        return Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(time)));
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(Set<LocationType> types, Location location, int maxDistance,
            int maxLocations) throws IOException {
        if (maxDistance == 0)
            maxDistance = DEFAULT_MAX_DISTANCE;
        if (maxLocations == 0)
            maxLocations = DEFAULT_MAX_LOCATIONS;
        // TODO POIs not supported (?)
        final String request = "{\"area\":" //
                + "{\"coordinates\":{\"longitude\":" + location.coord.getLonAsDouble() + ",\"latitude\":"
                + location.coord.getLatAsDouble() + "}," //
                + "\"radius\":" + maxDistance + "}," //
                + "\"maxResults\":" + maxLocations + "," //
                + "\"products\":[\"ALL\"]}";

        final HttpUrl url = this.nearbyEndpoint;
        final String contentType = "application/x.db.vendo.mob.location.v3+json";
        final ResultHeader h = new ResultHeader(NetworkId.DBMOVAS, contentType);
        String page = null;
        try {
            page = doRequest(url, request, contentType);

            final JSONArray locs = new JSONArray(page);
            final List<Location> locations = new ArrayList<>();
            for (int i = 0; i < locs.length(); i++) {
                final Location l = parseLocation(locs.getJSONObject(i));
                if (l != null) {
                    locations.add(l);
                }
            }
            return new NearbyLocationsResult(h, locations);
        } catch (InternalErrorException e) {
            return new NearbyLocationsResult(h, NearbyLocationsResult.Status.INVALID_ID);
        } catch (IOException | RuntimeException e) {
            return new NearbyLocationsResult(h, NearbyLocationsResult.Status.SERVICE_DOWN);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    @Override
    public QueryDeparturesResult queryDepartures(String stationId, Date time, int maxDepartures, boolean equivs)
            throws IOException {
        final Calendar c = new GregorianCalendar(timeZone);
        c.setTime(time);

        final String request = "{\"anfragezeit\": \"" + jsonTime(c) + "\"," //
                + "\"datum\": \"" + jsonDate(c) + "\"," //
                + "\"ursprungsBahnhofId\": \"" + formatLid(stationId) + "\"," //
                + "\"verkehrsmittel\":[\"ALL\"]}";

        final HttpUrl url = this.departureEndpoint;
        final String contentType = "application/x.db.vendo.mob.bahnhofstafeln.v2+json";

        final ResultHeader h = new ResultHeader(NetworkId.DBMOVAS, contentType);
        String page = null;
        try {
            page = doRequest(url, request, contentType);
            final QueryDeparturesResult result = new QueryDeparturesResult(h);
            final JSONObject head = new JSONObject(page);
            final JSONArray deps = head.getJSONArray("bahnhofstafelAbfahrtPositionen");
            int added = 0;
            for (int i = 0; i < deps.length(); i++) {
                final JSONObject dep = deps.getJSONObject(i);

                final Location l = parseLocation(dep.optJSONObject("abfrageOrt"));
                StationDepartures stationDepartures = result.findStationDepartures(l.id);
                if (stationDepartures == null) {
                    stationDepartures = new StationDepartures(l, new ArrayList<Departure>(8), null);
                    result.stationDepartures.add(stationDepartures);
                }
                if (!equivs && l.id != stationId) {
                    continue;
                }

                final Departure departure = new Departure(
                        parseIso8601WOffset(dep.optString("abgangsDatum", null)),
                        parseIso8601WOffset(dep.optString("ezAbgangsDatum", null)),
                        parseLine(dep),
                        parsePosition(Optional.ofNullable(dep.optString("ezGleis", null))
                                .orElse(dep.optString("gleis", null))),
                        parseLocation(LocationType.STATION, null, null, dep.optString("richtung", null), null),
                        null,
                        String.join(", ", parseMessages(dep)));

                stationDepartures.departures.add(departure);
                added += 1;
                if (added >= maxDepartures) {
                    break;
                }
            }

            for (final StationDepartures stationDepartures : result.stationDepartures)
                Collections.sort(stationDepartures.departures, Departure.TIME_COMPARATOR);
            return result;
        } catch (InternalErrorException e) {
            return new QueryDeparturesResult(h, QueryDeparturesResult.Status.INVALID_STATION);
        } catch (IOException e) {
            return new QueryDeparturesResult(h, QueryDeparturesResult.Status.SERVICE_DOWN);
        } catch (final JSONException x) {
            throw new ParserException("cannot parse json: '" + page + "' on " + url, x);
        }
    }

    @Override
    public SuggestLocationsResult suggestLocations(CharSequence constraint, Set<LocationType> types, int maxLocations)
            throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'suggestLocations'");
    }

    @Override
    public QueryTripsResult queryTrips(Location from, Location via, Location to, Date date, boolean dep,
            TripOptions options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryTrips'");
    }

    @Override
    public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryMoreTrips'");
    }

    @Override
    protected boolean hasCapability(Capability capability) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasCapability'");
    }

}
