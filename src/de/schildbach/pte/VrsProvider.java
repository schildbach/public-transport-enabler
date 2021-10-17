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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.util.ParserUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.base.Strings;

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
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Michael Dyrna
 */
public class VrsProvider extends AbstractNetworkProvider {

    private final List CAPABILITIES = Arrays.asList(
            Capability.SUGGEST_LOCATIONS,
            Capability.NEARBY_LOCATIONS,
            Capability.DEPARTURES,
            Capability.TRIPS,
            Capability.TRIPS_VIA
    );

    @SuppressWarnings("serial")
    private static class Context implements QueryTripsContext {
        private boolean canQueryLater = true;
        private boolean canQueryEarlier = true;
        private Date lastDeparture = null;
        private Date firstArrival = null;
        public Location from;
        public Location via;
        public Location to;
        public Set<Product> products;

        private Context() {
        }

        @Override
        public boolean canQueryLater() {
            return this.canQueryLater;
        }

        @Override
        public boolean canQueryEarlier() {
            return this.canQueryEarlier;
        }

        public void departure(Date departure) {
            if (this.lastDeparture == null || this.lastDeparture.compareTo(departure) < 0) {
                this.lastDeparture = departure;
            }
        }

        public void arrival(Date arrival) {
            if (this.firstArrival == null || this.firstArrival.compareTo(arrival) > 0) {
                this.firstArrival = arrival;
            }
        }

        public Date getLastDeparture() {
            return this.lastDeparture;
        }

        public Date getFirstArrival() {
            return this.firstArrival;
        }

        public void disableEarlier() {
            this.canQueryEarlier = false;
        }

        public void disableLater() {
            this.canQueryLater = false;
        }
    }

    private static class LocationWithPosition {
        public LocationWithPosition(Location location, Position position) {
            this.location = location;
            this.position = position;
        }

        public Location location;
        public Position position;
    }

    // valid host names: www.vrsinfo.de, android.vrsinfo.de, ios.vrsinfo.de, ekap.vrsinfo.de (only SSL
    // encrypted with client certificate)
    // performance comparison March 2015 showed www.vrsinfo.de to be fastest for trips
    protected static final HttpUrl API_BASE = HttpUrl.parse("http://android.vrsinfo.de/index.php");
    protected static final String SERVER_PRODUCT = "vrs";

    @SuppressWarnings("serial")
    protected static final List<Pattern> NAME_WITH_POSITION_PATTERNS = new ArrayList<Pattern>() {
        {
            // Bonn Hauptbahnhof (ZOB) - Bussteig F2
            // Beuel Bf - D
            add(Pattern.compile("(.*) - (.*)"));
            // Breslauer Platz/Hbf (U) Gleis 2
            add(Pattern.compile("(.*) Gleis (.*)"));
            // Bonn Hauptbahnhof (Stadtbahn) (Bahnsteig H)
            add(Pattern.compile("(.*) \\(Bahnsteig ([^)]*)\\)"));
            // Düren Bf (Bussteig D/E)
            add(Pattern.compile("(.*) \\(Bussteig ([^)]*)\\)"));
            // Venloer Str./Gürtel (Gleis 1)
            add(Pattern.compile("(?:(.*) )?\\(Gleis ([^)]*)\\)"));
            // Aachen alle Buslinien
            add(Pattern.compile("(.*) \\(H\\.(\\d+).*\\)"));
            // Neumarkt Bussteig B
            add(Pattern.compile("(.*) Bussteig (.*)"));
        }
    };
    protected static final Pattern nrwTarifPattern = Pattern.compile("([\\d]+,\\d\\d)");

    protected static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Stadtbahn Köln-Bonn
        STYLES.put("T1", new Style(Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("T3", new Style(Style.parseColor("#f680c5"), Style.WHITE));
        STYLES.put("T4", new Style(Style.parseColor("#f24dae"), Style.WHITE));
        STYLES.put("T5", new Style(Style.parseColor("#9c8dce"), Style.WHITE));
        STYLES.put("T7", new Style(Style.parseColor("#f57947"), Style.WHITE));
        STYLES.put("T9", new Style(Style.parseColor("#f5777b"), Style.WHITE));
        STYLES.put("T12", new Style(Style.parseColor("#80cc28"), Style.WHITE));
        STYLES.put("T13", new Style(Style.parseColor("#9e7b65"), Style.WHITE));
        STYLES.put("T15", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
        STYLES.put("T16", new Style(Style.parseColor("#33baab"), Style.WHITE));
        STYLES.put("T18", new Style(Style.parseColor("#05a1e6"), Style.WHITE));
        STYLES.put("T61", new Style(Style.parseColor("#80cc28"), Style.WHITE));
        STYLES.put("T62", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
        STYLES.put("T63", new Style(Style.parseColor("#73d2f6"), Style.WHITE));
        STYLES.put("T65", new Style(Style.parseColor("#b3db18"), Style.WHITE));
        STYLES.put("T66", new Style(Style.parseColor("#ec008c"), Style.WHITE));
        STYLES.put("T67", new Style(Style.parseColor("#f680c5"), Style.WHITE));
        STYLES.put("T68", new Style(Style.parseColor("#ca93d0"), Style.WHITE));

        // Busse Köln
        STYLES.put("BSB40", new Style(Style.parseColor("#FF0000"), Style.WHITE));
        STYLES.put("B106", new Style(Style.parseColor("#0994dd"), Style.WHITE));
        STYLES.put("B120", new Style(Style.parseColor("#24C6E8"), Style.WHITE));
        STYLES.put("B121", new Style(Style.parseColor("#89E82D"), Style.WHITE));
        STYLES.put("B122", new Style(Style.parseColor("#4D44FF"), Style.WHITE));
        STYLES.put("B125", new Style(Style.parseColor("#FF9A2E"), Style.WHITE));
        STYLES.put("B126", new Style(Style.parseColor("#FF8EE5"), Style.WHITE));
        STYLES.put("B127", new Style(Style.parseColor("#D164A4"), Style.WHITE));
        STYLES.put("B130", new Style(Style.parseColor("#5AC0E8"), Style.WHITE));
        STYLES.put("B131", new Style(Style.parseColor("#8cd024"), Style.WHITE));
        STYLES.put("B132", new Style(Style.parseColor("#E8840C"), Style.WHITE));
        STYLES.put("B133", new Style(Style.parseColor("#FF9EEE"), Style.WHITE));
        STYLES.put("B135", new Style(Style.parseColor("#f24caf"), Style.WHITE));
        STYLES.put("B136", new Style(Style.parseColor("#C96C44"), Style.WHITE));
        STYLES.put("B138", new Style(Style.parseColor("#ef269d"), Style.WHITE));
        STYLES.put("B139", new Style(Style.parseColor("#D13D1E"), Style.WHITE));
        STYLES.put("B140", new Style(Style.parseColor("#FFD239"), Style.WHITE));
        STYLES.put("B141", new Style(Style.parseColor("#2CE8D0"), Style.WHITE));
        STYLES.put("B142", new Style(Style.parseColor("#9E54FF"), Style.WHITE));
        STYLES.put("B143", new Style(Style.parseColor("#82E827"), Style.WHITE));
        STYLES.put("B144", new Style(Style.parseColor("#FF8930"), Style.WHITE));
        STYLES.put("B145", new Style(Style.parseColor("#24C6E8"), Style.WHITE));
        STYLES.put("B146", new Style(Style.parseColor("#F25006"), Style.WHITE));
        STYLES.put("B147", new Style(Style.parseColor("#FF8EE5"), Style.WHITE));
        STYLES.put("B149", new Style(Style.parseColor("#176fc1"), Style.WHITE));
        STYLES.put("B150", new Style(Style.parseColor("#f68712"), Style.WHITE));
        STYLES.put("B151", new Style(Style.parseColor("#ECB43A"), Style.WHITE));
        STYLES.put("B152", new Style(Style.parseColor("#FFDE44"), Style.WHITE));
        STYLES.put("B153", new Style(Style.parseColor("#C069FF"), Style.WHITE));
        STYLES.put("B154", new Style(Style.parseColor("#E85D25"), Style.WHITE));
        STYLES.put("B155", new Style(Style.parseColor("#0994dd"), Style.WHITE));
        STYLES.put("B156", new Style(Style.parseColor("#4B69EC"), Style.WHITE));
        STYLES.put("B157", new Style(Style.parseColor("#5CC3F9"), Style.WHITE));
        STYLES.put("B158", new Style(Style.parseColor("#66c530"), Style.WHITE));
        STYLES.put("B159", new Style(Style.parseColor("#FF00CC"), Style.WHITE));
        STYLES.put("B160", new Style(Style.parseColor("#66c530"), Style.WHITE));
        STYLES.put("B161", new Style(Style.parseColor("#33bef3"), Style.WHITE));
        STYLES.put("B162", new Style(Style.parseColor("#f033a3"), Style.WHITE));
        STYLES.put("B163", new Style(Style.parseColor("#00adef"), Style.WHITE));
        STYLES.put("B163/550", new Style(Style.parseColor("#00adef"), Style.WHITE));
        STYLES.put("B164", new Style(Style.parseColor("#885bb4"), Style.WHITE));
        STYLES.put("B164/501", new Style(Style.parseColor("#885bb4"), Style.WHITE));
        STYLES.put("B165", new Style(Style.parseColor("#7b7979"), Style.WHITE));
        STYLES.put("B166", new Style(Style.parseColor("#7b7979"), Style.WHITE));
        STYLES.put("B167", new Style(Style.parseColor("#7b7979"), Style.WHITE));
        STYLES.put("B180", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B181", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B182", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B183", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B184", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B185", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
        STYLES.put("B186", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
        STYLES.put("B187", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
        STYLES.put("B188", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B190", new Style(Style.parseColor("#4D44FF"), Style.WHITE));
        STYLES.put("B191", new Style(Style.parseColor("#00a998"), Style.WHITE));

        // Busse Bonn
        STYLES.put("B16", new Style(Style.parseColor("#33baab"), Style.WHITE));
        STYLES.put("B18", new Style(Style.parseColor("#05a1e6"), Style.WHITE));
        STYLES.put("B61", new Style(Style.parseColor("#80cc28"), Style.WHITE));
        STYLES.put("B62", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
        STYLES.put("B63", new Style(Style.parseColor("#73d2f6"), Style.WHITE));
        STYLES.put("B65", new Style(Style.parseColor("#b3db18"), Style.WHITE));
        STYLES.put("B66", new Style(Style.parseColor("#ec008c"), Style.WHITE));
        STYLES.put("B67", new Style(Style.parseColor("#f680c5"), Style.WHITE));
        STYLES.put("B68", new Style(Style.parseColor("#ca93d0"), Style.WHITE));
        STYLES.put("BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
        STYLES.put("BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
        STYLES.put("BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
        STYLES.put("B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
        STYLES.put("B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
        STYLES.put("B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
        STYLES.put("B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
        STYLES.put("B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
        STYLES.put("B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
        STYLES.put("B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
        STYLES.put("B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
        STYLES.put("B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
        STYLES.put("B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
        STYLES.put("B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
        STYLES.put("B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
        STYLES.put("B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
        STYLES.put("B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
        STYLES.put("B614", new Style(Style.parseColor("#806839"), Style.WHITE));
        STYLES.put("B615", new Style(Style.parseColor("#532700"), Style.WHITE));
        STYLES.put("B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
        STYLES.put("B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
        STYLES.put("B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
        STYLES.put("B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
        STYLES.put("B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
        STYLES.put("B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
        STYLES.put("B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
        STYLES.put("B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
        STYLES.put("BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
        STYLES.put("BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
        STYLES.put("BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));

        // andere Busse
        STYLES.put("B250", new Style(Style.parseColor("#8FE84B"), Style.WHITE));
        STYLES.put("B260", new Style(Style.parseColor("#FF8365"), Style.WHITE));
        STYLES.put("B423", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
        STYLES.put("B434", new Style(Style.parseColor("#14E80B"), Style.WHITE));
        STYLES.put("B436", new Style(Style.parseColor("#BEEC49"), Style.WHITE));
        STYLES.put("B481", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
        STYLES.put("B504", new Style(Style.parseColor("#8cd024"), Style.WHITE));
        STYLES.put("B505", new Style(Style.parseColor("#0994dd"), Style.WHITE));
        STYLES.put("B885", new Style(Style.parseColor("#40bb6a"), Style.WHITE));
        STYLES.put("B935", new Style(Style.parseColor("#bf7e71"), Style.WHITE));
        STYLES.put("B961", new Style(Style.parseColor("#f140a9"), Style.WHITE));
        STYLES.put("B962", new Style(Style.parseColor("#9c83c9"), Style.WHITE));
        STYLES.put("B963", new Style(Style.parseColor("#f46c68"), Style.WHITE));
        STYLES.put("B965", new Style(Style.parseColor("#FF0000"), Style.WHITE));
        STYLES.put("B970", new Style(Style.parseColor("#f68712"), Style.WHITE));
        STYLES.put("B980", new Style(Style.parseColor("#c38bcc"), Style.WHITE));

        STYLES.put("BN", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("BNE1", new Style(Style.parseColor("#993399"), Style.WHITE)); // default

        STYLES.put("S", new Style(Style.parseColor("#f18e00"), Style.WHITE));
        STYLES.put("R", new Style(Style.parseColor("#009d81"), Style.WHITE));
    }

    public VrsProvider() {
        super(NetworkId.VRS);

        setStyles(STYLES);
    }

    @Override
    protected boolean hasCapability(Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(Set<LocationType> types, Location location, 
            int maxDistance, int maxLocations) throws IOException {
        final Point queryCoord;
        if (location.hasCoord()) {
            queryCoord = location.coord;
        } else if (location.type == LocationType.STATION && location.hasId()) {
            queryCoord = stationToCoord(location.id);
        } else {
            throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");
        }

        final HttpUrl.Builder url = API_BASE.newBuilder();
        url.addQueryParameter("eID", "tx_ekap_here");
        url.addQueryParameter("ta", "vrs");
        url.addQueryParameter("lat", String.format(Locale.ENGLISH, "%.6f", queryCoord.getLatAsDouble()));
        url.addQueryParameter("lon", String.format(Locale.ENGLISH, "%.6f", queryCoord.getLonAsDouble()));
        final CharSequence page = httpClient.get(url.build());

        try {
            int num = 0;
            final List<Location> locations = new ArrayList<>();
            final JSONObject head = new JSONObject(page.toString());
            final JSONArray objects = head.getJSONArray("objects");
            for (int i = 0; i < objects.length(); i++) {
                final JSONObject entry = objects.getJSONObject(i);
                final LocationType type = parseLocationType(entry.getString("type"));
                if (!(types.contains(type) || types.contains(LocationType.ANY))) {
                    continue;
                }
                final Point coord = Point.fromDouble(entry.getDouble("lat"), entry.getDouble("lon"));
                if (maxDistance > 0 && entry.optInt("distance") > maxDistance) {
                    continue;
                }
                // TODO "distance" is only given for stops. For other location types, calculate distance from coordinates
                String id = entry.optString("id");
                if (id == null || id.isEmpty()) {
                    id = entry.getString("ifopt");
                }
                String place = entry.getString("municipality");
                final String locality = entry.optString("locality");
                if (locality != null && !locality.isEmpty()) {
                    place += "-" + locality;
                }
                String name = entry.getString("name");
                if (entry.getString("type").equals("parkandride")) {
                    name = "P+R " + name;
                }
                final JSONArray lines = entry.optJSONArray("lines");
                final EnumSet<Product> products = EnumSet.noneOf(Product.class);
                for (int j = 0; lines != null && j < lines.length(); j++) {
                    final JSONObject line = lines.getJSONObject(j);
                    products.add(parseProduct(line.getString("productCode"), line.getString("name")));
                }
                locations.add(new Location(type, id, coord, place, name, products));
                if (maxLocations > 0 && ++num >= maxLocations) {
                    break;
                }
            }
            final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, null, new Date().getTime(), null);
            return new NearbyLocationsResult(header, locations);
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    // TODO equivs not supported; JSON result would support multiple timetables
    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, @Nullable Date time, int maxDepartures,
            boolean equivs) throws IOException {
        checkNotNull(Strings.emptyToNull(stationId));

        // g=p means group by product; not used here
        // d=minutes overwrites c=count and returns departures for the next d minutes
        final HttpUrl.Builder url = API_BASE.newBuilder();
        url.addQueryParameter("eID", "tx_vrsinfo_ass2_timetable");
        url.addQueryParameter("i", stationId);
        url.addQueryParameter("c", Integer.toString(maxDepartures));
        if (time != null) {
            url.addQueryParameter("t", formatDate(time));
        }
        url.addQueryParameter("p", "LongDistanceTrains,RegionalTrains,SuburbanTrains,Underground,LightRail,Bus,CommunityBus,RailReplacementServices,Boat,OnDemandServices");
        final CharSequence page = httpClient.get(url.build());

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String error = Strings.emptyToNull(head.optString("error", "").trim());
            if (error != null) {
                if (error.equals("ASS2-Server lieferte leere Antwort."))
                    return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryDeparturesResult.Status.SERVICE_DOWN);
                else if (error.equals("Leere ASS-ID und leere Koordinate"))
                    return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryDeparturesResult.Status.INVALID_STATION);
                else if (error.equals("Keine Abfahrten gefunden."))
                    return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryDeparturesResult.Status.INVALID_STATION);
                else
                    throw new IllegalStateException("unknown error: " + error);
            }
            final JSONArray timetable = head.getJSONArray("timetable");
            final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
            final QueryDeparturesResult result = new QueryDeparturesResult(header);
            if (timetable.length() == 0) {
                return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
            }
            // for all stations
            for (int iStation = 0; iStation < timetable.length(); iStation++) {
                final List<Departure> departures = new ArrayList<>();
                final JSONObject station = timetable.getJSONObject(iStation);
                final Location location = parseLocationAndPosition(station.getJSONObject("stop"), null).location;
                final JSONArray events = station.getJSONArray("events");
                final List<LineDestination> lines = new ArrayList<>();
                // for all departures
                for (int iEvent = 0; iEvent < events.length(); iEvent++) {
                    final JSONObject event = events.getJSONObject(iEvent);
                    Date plannedTime = null;
                    Date predictedTime = null;
                    if (event.has("departureScheduled")) {
                        plannedTime = parseDateTime(event.getString("departureScheduled"));
                        predictedTime = parseDateTime(event.getString("departure"));
                    } else {
                        plannedTime = parseDateTime(event.getString("departure"));
                    }
                    final JSONObject lineObj = event.getJSONObject("line");
                    final Line line = parseLine(lineObj);
                    Position position = null;
                    final JSONObject post = event.optJSONObject("post");
                    if (post != null) {
                        String postName = post.getString("name");
                        for (Pattern pattern : NAME_WITH_POSITION_PATTERNS) {
                            Matcher matcher = pattern.matcher(postName);
                            if (matcher.matches()) {
                                position = new Position(matcher.group(2));
                                break;
                            }
                        }
                        if (position == null) {
                            if (postName.startsWith("(") && postName.endsWith(")"))
                                postName = postName.substring(1, postName.length() - 1);
                            position = new Position(postName);
                        }
                    }
                    final Location destination = new Location(LocationType.STATION, null /* id */, null /* place */,
                            lineObj.getString("direction"));

                    final LineDestination lineDestination = new LineDestination(line, destination);
                    if (!lines.contains(lineDestination)) {
                        lines.add(lineDestination);
                    }
                    final Departure d = new Departure(plannedTime, predictedTime, line, position, destination, null,
                            null);
                    departures.add(d);
                }

                result.stationDepartures.add(new StationDepartures(location, departures, lines));
            }

            return result;
        } catch (final JSONException | ParseException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        // sc = station count
        final int sc = EnumSet.of(LocationType.STATION).equals(types) ? maxLocations : maxLocations / 2;
        // ac = address count
        final int ac = EnumSet.of(LocationType.ADDRESS).equals(types) ? maxLocations : maxLocations / 4;
        // pc = points of interest count
        final int pc = EnumSet.of(LocationType.POI).equals(types) ? maxLocations : maxLocations / 4;
        // t = sap (stops and/or addresses and/or pois)
        final HttpUrl.Builder url = API_BASE.newBuilder();
        url.addQueryParameter("eID", "tx_vrsinfo_ass2_objects");
        url.addQueryParameter("sc", Integer.toString(sc));
        url.addQueryParameter("ac", Integer.toString(ac));
        url.addQueryParameter("pc", Integer.toString(pc));
        url.addQueryParameter("t", "sap");
        url.addQueryParameter("q", constraint.toString());

        final CharSequence page = httpClient.get(url.build());

        try {
            final List<SuggestedLocation> locations = new ArrayList<>();

            final JSONObject head = new JSONObject(page.toString());
            final String error = Strings.emptyToNull(head.optString("error", "").trim());
            if (error != null) {
                if (error.equals("ASS2-Server lieferte leere Antwort."))
                    return new SuggestLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            SuggestLocationsResult.Status.SERVICE_DOWN);
                else if (error.equals("Leere Suche"))
                    return new SuggestLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), locations);
                else
                    throw new IllegalStateException("unknown error: " + error);
            }
            final JSONArray stops = head.optJSONArray("stops");
            final JSONArray addresses = head.optJSONArray("addresses");
            final JSONArray pois = head.optJSONArray("pois");

            final int nStops = stops.length();
            for (int iStop = 0; iStop < nStops; iStop++) {
                final JSONObject stop = stops.optJSONObject(iStop);
                final Location location = parseLocationAndPosition(stop, null).location;
                locations.add(new SuggestedLocation(location, sc + ac + pc - iStop));
            }

            final int nAddresses = addresses.length();
            for (int iAddress = 0; iAddress < nAddresses; iAddress++) {
                final JSONObject address = addresses.optJSONObject(iAddress);
                final Location location = parseLocationAndPosition(address, null).location;
                locations.add(new SuggestedLocation(location, ac + pc - iAddress));
            }

            final int nPois = pois.length();
            for (int iPoi = 0; iPoi < nPois; iPoi++) {
                final JSONObject poi = pois.optJSONObject(iPoi);
                final Location location = parseLocationAndPosition(poi, null).location;
                locations.add(new SuggestedLocation(location, pc - iPoi));
            }

            final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
            return new SuggestLocationsResult(header, locations);
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    // http://www.vrsinfo.de/index.php?eID=tx_vrsinfo_ass2_router&c=1&f=2071&t=1504&d=2015-02-11T11%3A47%3A20%2B01%3A00
    // c: count (default: 5)
    // f: from (id or lat,lon as float)
    // v: via (id or lat,lon as float)
    // t: to (id or lat,lon as float)
    // a/d: date (default now)
    // vt: via time in minutes - not supported by Öffi
    // s: t => allow surcharge
    // p: products as comma separated list
    // o: options:
    // 'v' for showing via stations
    // 'd' for showing walking directions
    // 'p' for showing exact geographical coordinates along the route
    // walkSpeed not supported.
    // accessibility not supported.
    // options not supported.
    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, Date date,
            boolean dep, @Nullable TripOptions options) throws IOException {
        // The EXACT_POINTS feature generates an about 50% bigger API response, probably well compressible.
        final boolean EXACT_POINTS = true;
        final List<Location> ambiguousFrom = new ArrayList<>();
        String fromString = generateLocation(from, ambiguousFrom);

        final List<Location> ambiguousVia = new ArrayList<>();
        String viaString = generateLocation(via, ambiguousVia);

        final List<Location> ambiguousTo = new ArrayList<>();
        String toString = generateLocation(to, ambiguousTo);

        if (!ambiguousFrom.isEmpty() || !ambiguousVia.isEmpty() || !ambiguousTo.isEmpty()) {
            return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                    ambiguousFrom.isEmpty() ? null : ambiguousFrom, ambiguousVia.isEmpty() ? null : ambiguousVia,
                    ambiguousTo.isEmpty() ? null : ambiguousTo);
        }

        if (fromString == null) {
            return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                    QueryTripsResult.Status.UNKNOWN_FROM);
        }
        if (via != null && viaString == null) {
            return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                    QueryTripsResult.Status.UNKNOWN_VIA);
        }
        if (toString == null) {
            return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                    QueryTripsResult.Status.UNKNOWN_TO);
        }

        if (options == null)
            options = new TripOptions();

        final HttpUrl.Builder url = API_BASE.newBuilder();
        url.addQueryParameter("eID", "tx_vrsinfo_ass2_router");
        url.addQueryParameter("f", fromString);
        url.addQueryParameter("t", toString);
        if (via != null) {
            url.addQueryParameter("v", via.id);
        }
        url.addQueryParameter(dep ? "d" : "a", formatDate(date));
        url.addQueryParameter("s", "t");
        if (options.products != null && !options.products.equals(Product.ALL))
            url.addQueryParameter("p", generateProducts(options.products));
        url.addQueryParameter("o", "v" + (EXACT_POINTS ? "p" : ""));

        final CharSequence page = httpClient.get(url.build());

        try {
            final List<Trip> trips = new ArrayList<>();
            final JSONObject head = new JSONObject(page.toString());
            final String error = Strings.emptyToNull(head.optString("error", "").trim());
            if (error != null) {
                if (error.equals("ASS2-Server lieferte leere Antwort."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.SERVICE_DOWN);
                else if (error.equals("Zeitüberschreitung bei der Verbindung zum ASS2-Server"))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.SERVICE_DOWN);
                else if (error.equals("Server Error"))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.SERVICE_DOWN);
                else if (error.equals("Keine Verbindungen gefunden."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.NO_TRIPS);
                else if (error.equals("Es wurden keine gültigen Verbindungen für diese Anfrage gefunden."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.NO_TRIPS);
                else if (error.startsWith("Keine Verbindung gefunden."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.NO_TRIPS);
                else if (error.equals("Origin invalid."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.UNKNOWN_FROM);
                else if (error.equals("Via invalid."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.UNKNOWN_VIA);
                else if (error.equals("Destination invalid."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.UNKNOWN_TO);
                else if (error.equals("Fehlerhafter Start"))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.UNKNOWN_FROM);
                else if (error.equals("Fehlerhaftes Ziel"))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.UNKNOWN_TO);
                else if (error.equals("Produkt ungültig."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.NO_TRIPS);
                else if (error.equals("Keine Route."))
                    return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
                            QueryTripsResult.Status.NO_TRIPS);
                else
                    throw new IllegalStateException("unknown error: " + error);
            }
            final JSONArray routes = head.getJSONArray("routes");
            final Context context = new Context();
            // for all routes
            for (int iRoute = 0; iRoute < routes.length(); iRoute++) {
                final JSONObject route = routes.getJSONObject(iRoute);
                final JSONArray segments = route.getJSONArray("segments");
                List<Leg> legs = new ArrayList<>();
                Location tripOrigin = null;
                Location tripDestination = null;
                // for all segments
                for (int iSegment = 0; iSegment < segments.length(); iSegment++) {
                    final JSONObject segment = segments.getJSONObject(iSegment);
                    final String type = segment.getString("type");
                    final JSONObject origin = segment.getJSONObject("origin");
                    final LocationWithPosition segmentOriginLocationWithPosition = parseLocationAndPosition(origin, null);
                    Location segmentOrigin = segmentOriginLocationWithPosition.location;
                    final Position segmentOriginPosition = segmentOriginLocationWithPosition.position;
                    if (iSegment == 0) {
                        // special case: first origin is an address
                        if (from.type == LocationType.ADDRESS) {
                            segmentOrigin = from;
                        }
                        tripOrigin = segmentOrigin;
                    }
                    final JSONObject destination = segment.getJSONObject("destination");
                    final LocationWithPosition segmentDestinationLocationWithPosition = parseLocationAndPosition(
                            destination, null);
                    Location segmentDestination = segmentDestinationLocationWithPosition.location;
                    final Position segmentDestinationPosition = segmentDestinationLocationWithPosition.position;
                    if (iSegment == segments.length() - 1) {
                        // special case: last destination is an address
                        if (to.type == LocationType.ADDRESS) {
                            segmentDestination = to;
                        }
                        tripDestination = segmentDestination;
                    }
                    List<Stop> intermediateStops = new ArrayList<>();
                    final JSONArray vias = segment.optJSONArray("vias");
                    if (vias != null) {
                        for (int iVia = 0; iVia < vias.length(); iVia++) {
                            final JSONObject viaJsonObject = vias.getJSONObject(iVia);
                            final LocationWithPosition viaLocationWithPosition = parseLocationAndPosition(
                                    viaJsonObject, null);
                            final Location viaLocation = viaLocationWithPosition.location;
                            final Position viaPosition = viaLocationWithPosition.position;
                            Date arrivalPlanned = null;
                            Date arrivalPredicted = null;
                            if (viaJsonObject.has("arrivalScheduled")) {
                                arrivalPlanned = parseDateTime(viaJsonObject.getString("arrivalScheduled"));
                                arrivalPredicted = (viaJsonObject.has("arrival"))
                                        ? parseDateTime(viaJsonObject.getString("arrival")) : null;
                            } else if (segment.has("arrival")) {
                                arrivalPlanned = parseDateTime(viaJsonObject.getString("arrival"));
                            }
                            final Stop intermediateStop = new Stop(viaLocation, false /* arrival */, arrivalPlanned,
                                    arrivalPredicted, viaPosition, viaPosition);
                            intermediateStops.add(intermediateStop);
                        }
                    }
                    Date departurePlanned = null;
                    Date departurePredicted = null;
                    if (segment.has("departureScheduled")) {
                        departurePlanned = parseDateTime(segment.getString("departureScheduled"));
                        departurePredicted = (segment.has("departure")) ? parseDateTime(segment.getString("departure"))
                                : null;
                        if (iSegment == 0) {
                            context.departure(departurePredicted);
                        }
                    } else if (segment.has("departure")) {
                        departurePlanned = parseDateTime(segment.getString("departure"));
                        if (iSegment == 0) {
                            context.departure(departurePlanned);
                        }
                    }
                    Date arrivalPlanned = null;
                    Date arrivalPredicted = null;
                    if (segment.has("arrivalScheduled")) {
                        arrivalPlanned = parseDateTime(segment.getString("arrivalScheduled"));
                        arrivalPredicted = (segment.has("arrival")) ? parseDateTime(segment.getString("arrival"))
                                : null;
                        if (iSegment == segments.length() - 1) {
                            context.arrival(arrivalPredicted);
                        }
                    } else if (segment.has("arrival")) {
                        arrivalPlanned = parseDateTime(segment.getString("arrival"));
                        if (iSegment == segments.length() - 1) {
                            context.arrival(arrivalPlanned);
                        }
                    }
                    long traveltime = segment.getLong("traveltime");
                    long distance = segment.optLong("distance", 0);
                    Line line = null;
                    String direction = null;
                    JSONObject lineObject = segment.optJSONObject("line");
                    if (lineObject != null) {
                        line = parseLine(lineObject);
                        direction = lineObject.optString("direction", null);
                    }
                    StringBuilder message = new StringBuilder();
                    JSONArray infos = segment.optJSONArray("infos");
                    if (infos != null) {
                        for (int k = 0; k < infos.length(); k++) {
                            // TODO there can also be a "header" string
                            if (k > 0) {
                                message.append(", ");
                            }
                            message.append(infos.getJSONObject(k).getString("text"));
                        }
                    }

                    List<Point> points = new ArrayList<>();
                    points.add(segmentOrigin.coord);
                    if (EXACT_POINTS && segment.has("polygon")) {
                        parsePolygon(segment.getString("polygon"), points);
                    } else {
                        for (Stop intermediateStop : intermediateStops) {
                            points.add(intermediateStop.location.coord);
                        }
                    }
                    points.add(segmentDestination.coord);
                    if (type.equals("walk")) {
                        if (departurePlanned == null)
                            departurePlanned = legs.get(legs.size() - 1).getArrivalTime();
                        if (arrivalPlanned == null)
                            arrivalPlanned = new Date(departurePlanned.getTime() + traveltime * 1000);

                        legs.add(new Trip.Individual(Trip.Individual.Type.WALK, segmentOrigin, departurePlanned,
                                segmentDestination, arrivalPlanned, points, (int) distance));
                    } else if (type.equals("publicTransport")) {
                        legs.add(new Trip.Public(line, direction != null
                                ? new Location(LocationType.STATION, null /* id */, null /* place */, direction) : null,
                                new Stop(segmentOrigin, true /* departure */, departurePlanned, departurePredicted,
                                        segmentOriginPosition, segmentOriginPosition),
                                new Stop(segmentDestination, false /* departure */, arrivalPlanned, arrivalPredicted,
                                        segmentDestinationPosition, segmentDestinationPosition),
                                intermediateStops, points, Strings.emptyToNull(message.toString())));
                    } else {
                        throw new IllegalStateException("unhandled type: " + type);
                    }
                }
                int changes = route.getInt("changes");
                List<Fare> fares = parseFare(route.optJSONObject("costs"));

                trips.add(new Trip(null /* id */, tripOrigin, tripDestination, legs, fares, null /* capacity */,
                        changes));
            }
            long serverTime = parseDateTime(head.getString("generated")).getTime();
            final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, null, serverTime, null);
            context.from = from;
            context.to = to;
            context.via = via;
            context.products = options.products;
            if (trips.size() == 1) {
                if (dep)
                    context.disableLater();
                else
                    context.disableEarlier();
            }
            return new QueryTripsResult(header, url.build().toString(), from, via, to, context, trips);
        } catch (final JSONException | ParseException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    private static List<Fare> parseFare(final JSONObject costs) throws JSONException {
        List<Fare> fares = new ArrayList<>();
        if (costs != null) {
            final String name = costs.optString("name", null); // e.g. "VRS-Tarif", "NRW-Tarif"
            final String text = costs.optString("text", null); // e.g. "Preisstufe 4 [RegioTicket] 7,70 €",
            // "VRR-Tarif! (Details: www.vrr.de)", "17,30 € (2.Kl) / PauschalpreisTickets gültig"
            float price = (float) costs.optDouble("price", 0.0); // e.g. 7.7 or not existent outside VRS
            // long zone = costs.getLong("zone"); // e.g. 2600
            final String level = costs.has("level") ? "Preisstufe " + costs.getString("level") : null; // e.g.
                                                                                                       // "4"

            if (name != null && price != 0.0 && level != null) {
                fares.add(new Fare(name, Fare.Type.ADULT, ParserUtils.CURRENCY_EUR, price, level, null /* units */));
            } else if (name != null && name.equals("NRW-Tarif") && text != null) {
                Matcher matcher = nrwTarifPattern.matcher(text);
                if (matcher.find()) {
                    fares.add(new Fare(name, Fare.Type.ADULT, ParserUtils.CURRENCY_EUR,
                            Float.parseFloat(matcher.group(0).replace(",", ".")), null /* level */, null /* units */));
                }
            }
        }
        return fares;
    }

    protected static void parsePolygon(final String polygonStr, final List<Point> polygonArr) {
        if (polygonStr != null && !polygonStr.isEmpty()) {
            String pointsArr[] = polygonStr.split("\\s");
            for (String point : pointsArr) {
                String latlon[] = point.split(",");
                polygonArr.add(Point.fromDouble(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1])));
            }
        }
    }

    @Override
    public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
        Context ctx = (Context) context;
        TripOptions options = new TripOptions(ctx.products, null, null, null, null);
        if (later) {
            return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getLastDeparture(), true, options);
        } else {
            return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getFirstArrival(), false, options);
        }
    }

    @Override
    public Style lineStyle(final @Nullable String network, final @Nullable Product product,
            final @Nullable String label) {
        if (product == Product.BUS && label != null && label.startsWith("SB")) {
            return super.lineStyle(network, product, "SB");
        }

        return super.lineStyle(network, product, label);
    }

    @Override
    public Point[] getArea() throws IOException {
        return new Point[] { Point.from1E6(50937531, 6960279) };
    }

    private Line parseLine(JSONObject line) throws JSONException {
        final String number = processLineNumber(line.getString("number"));
        final Product productObj = parseProduct(line.getString("product"), number);
        final Style style = lineStyle("vrs", productObj, number);
        return new Line(null /* id */, NetworkId.VRS.toString(), productObj, number, style);
    }

    private static String processLineNumber(final String number) {
        if (number.startsWith("AST ") || number.startsWith("VRM ") || number.startsWith("VRR ")) {
            return number.substring(4);
        } else if (number.startsWith("AST") || number.startsWith("VRM") || number.startsWith("VRR")) {
            return number.substring(3);
        } else if (number.startsWith("TaxiBus ")) {
            return number.substring(8);
        } else if (number.startsWith("TaxiBus")) {
            return number.substring(7);
        } else if (number.equals("Schienen-Ersatz-Verkehr (SEV)")) {
            return "SEV";
        } else {
            return number;
        }
    }

    private static Product parseProduct(String product, String number) {
        if (product.equals("LongDistanceTrains")) {
            return Product.HIGH_SPEED_TRAIN;
        } else if (product.equals("RegionalTrains")) {
            return Product.REGIONAL_TRAIN;
        } else if (product.equals("SuburbanTrains")) {
            return Product.SUBURBAN_TRAIN;
        } else if (product.equals("Underground") || product.equals("LightRail") && number.startsWith("U")) {
            return Product.SUBWAY;
        } else if (product.equals("LightRail")) {
            // note that also the Skytrain (Flughafen Düsseldorf Bahnhof - Flughafen Düsseldorf Terminan
            // and Schwebebahn Wuppertal (line 60) are both returned as product "LightRail".
            return Product.TRAM;
        } else if (product.equals("Bus") || product.equals("CommunityBus")
                || product.equals("RailReplacementServices")) {
            return Product.BUS;
        } else if (product.equals("Boat")) {
            return Product.FERRY;
        } else if (product.equals("OnDemandServices")) {
            return Product.ON_DEMAND;
        } else {
            throw new IllegalArgumentException("unknown product: '" + product + "'");
        }
    }

    private static String generateProducts(Set<Product> products) {
        StringBuilder ret = new StringBuilder();
        Iterator<Product> it = products.iterator();
        while (it.hasNext()) {
            final Product product = it.next();
            final String productStr = generateProduct(product);
            if (ret.length() > 0 && !ret.substring(ret.length() - 1).equals(",") && !productStr.isEmpty()) {
                ret.append(",");
            }
            ret.append(productStr);
        }
        return ret.toString();
    }

    private static String generateProduct(Product product) {
        switch (product) {
        case BUS:
            // can't filter for RailReplacementServices although this value is valid in API responses
            return "Bus,CommunityBus";
        case CABLECAR:
            // no mapping in VRS
            return "";
        case FERRY:
            return "Boat";
        case HIGH_SPEED_TRAIN:
            return "LongDistanceTrains";
        case ON_DEMAND:
            return "OnDemandServices";
        case REGIONAL_TRAIN:
            return "RegionalTrains";
        case SUBURBAN_TRAIN:
            return "SuburbanTrains";
        case SUBWAY:
            return "LightRail,Underground";
        case TRAM:
            return "LightRail";
        default:
            throw new IllegalArgumentException("unknown product: '" + product + "'");
        }
    }

    public LocationWithPosition parseLocationAndPosition(JSONObject location, JSONArray events) throws JSONException {
        final LocationType locationType;
        String id = null;
        String name = null;
        String position = null;
        if (location.has("id")) {
            locationType = LocationType.STATION;
            id = location.getString("id");
            name = location.getString("name");
            for (Pattern pattern : NAME_WITH_POSITION_PATTERNS) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.matches()) {
                    name = matcher.group(1);
                    position = matcher.group(2);
                    break;
                }
            }
        } else if (location.has("street")) {
            locationType = LocationType.ADDRESS;
            name = (location.getString("street") + " " + location.getString("number")).trim();
        } else if (location.has("name")) {
            locationType = LocationType.POI;
            id = location.getString("tempId");
            name = location.getString("name");
        } else if (location.has("x") && location.has("y")) {
            locationType = LocationType.ANY;
        } else {
            throw new IllegalArgumentException("unknown location JSONObject: " + location);
        }
        String place = location.optString("city", null);
        if (place != null) {
            if (location.has("district") && !location.getString("district").isEmpty()) {
                place += "-" + location.getString("district");
            }
        }
        final double lat = location.optDouble("x", 0);
        final double lon = location.optDouble("y", 0);
        final Point coord = Point.fromDouble(lat, lon);

        final EnumSet<Product> products = EnumSet.noneOf(Product.class);
        if (events != null) {
            for (int iEvent = 0; iEvent < events.length(); iEvent++) {
                final JSONObject event = events.getJSONObject(iEvent);
                final Line line = parseLine(event.getJSONObject("line"));
                products.add(line.product);
            }
        }
        return new LocationWithPosition(new Location(locationType, id, coord, place, name, products),
                position != null ? new Position(position.substring(position.lastIndexOf(" ") + 1)) : null);
    }

    private String generateLocation(Location loc, List<Location> ambiguous) throws IOException {
        if (loc == null) {
            return null;
        } else if (loc.id != null) {
            return loc.id;
        } else if (loc.coord != null) {
            return String.format(Locale.ENGLISH, "%f,%f", loc.getLatAsDouble(), loc.getLonAsDouble());
        } else {
            SuggestLocationsResult suggestLocationsResult = suggestLocations(loc.name, null, 0);
            final List<Location> suggestedLocations = suggestLocationsResult.getLocations();
            if (suggestedLocations.size() == 1) {
                return suggestedLocations.get(0).id;
            } else {
                ambiguous.addAll(suggestedLocations);
                return null;
            }
        }
    }

    private final static String formatDate(final Date time) {
        final Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.setTime(time);
        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);
        final int second = c.get(Calendar.SECOND);
        return String.format(Locale.ENGLISH, "%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second);
    }

    private final static Date parseDateTime(final String dateTimeStr) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZ")
                .parse(dateTimeStr.substring(0, dateTimeStr.lastIndexOf(':')) + "00");
    }

    private final Point stationToCoord(String id) throws IOException {
        final HttpUrl.Builder url = API_BASE.newBuilder();
        url.addQueryParameter("eID", "tx_vrsinfo_ass2_timetable");
        url.addQueryParameter("i", id);

        final CharSequence page = httpClient.get(url.build());

        try {
            final JSONObject head = new JSONObject(page.toString());
            final String error = Strings.emptyToNull(head.optString("error", "").trim());
            if (error != null) {
                throw new IllegalStateException(error);
            }
            final JSONArray timetable = head.getJSONArray("timetable");
            final JSONObject entry = timetable.getJSONObject(0);
            final JSONObject stop = entry.getJSONObject("stop");
            return Point.fromDouble(stop.getDouble("x"), stop.getDouble("y"));
        } catch (final JSONException x) {
            throw new RuntimeException("cannot parse: '" + page + "' on " + url, x);
        }
    }

    private final static LocationType parseLocationType(String type) {
        if (type.equals("stop")) {
            return LocationType.STATION;
        } else if (type.equals("poi") || type.equals("parkandride")) {
            return LocationType.POI;
        } else {
            return LocationType.ANY;
        }
    }
}
