/*
 * Copyright 2010-2015 the original author or authors.
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
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
import javax.annotation.RegEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;

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
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Michael Dyrna
 */
public class VrsProvider extends AbstractNetworkProvider {
	private static class Context implements QueryTripsContext {
		private static final long serialVersionUID = 8758709354176420641L;
		private Date lastDeparture = null;
		private Date firstArrival = null;
		public Location from;
		public Location via;
		public Location to;
		public Set<Product> products;

		private Context() {
		}

		public boolean canQueryLater() {
			return true;
		}

		public boolean canQueryEarlier() {
			return true;
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
			return lastDeparture;
		}

		public Date getFirstArrival() {
			return firstArrival;
		}
	}

	protected static final String API_BASE = "http://android.vrsinfo.de/index.php";
	protected static final boolean httpPost = false;
	protected static final String SERVER_PRODUCT = "vrs";
	protected static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZ");
	protected static final Pattern nameWithPosition = Pattern.compile("(.*) - (.*)");

	protected static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Schnellbusse VRR
		STYLES.put("vrs|BSB", new Style(Style.parseColor("#00919d"), Style.WHITE));

		// Stadtbahn Köln-Bonn
		STYLES.put("vrs|T1", new Style(Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("vrs|T3", new Style(Style.parseColor("#f680c5"), Style.WHITE));
		STYLES.put("vrs|T4", new Style(Style.parseColor("#f24dae"), Style.WHITE));
		STYLES.put("vrs|T5", new Style(Style.parseColor("#9c8dce"), Style.WHITE));
		STYLES.put("vrs|T7", new Style(Style.parseColor("#f57947"), Style.WHITE));
		STYLES.put("vrs|T9", new Style(Style.parseColor("#f5777b"), Style.WHITE));
		STYLES.put("vrs|T12", new Style(Style.parseColor("#80cc28"), Style.WHITE));
		STYLES.put("vrs|T13", new Style(Style.parseColor("#9e7b65"), Style.WHITE));
		STYLES.put("vrs|T15", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
		STYLES.put("vrs|T16", new Style(Style.parseColor("#33baab"), Style.WHITE));
		STYLES.put("vrs|T18", new Style(Style.parseColor("#05a1e6"), Style.WHITE));
		STYLES.put("vrs|T61", new Style(Style.parseColor("#80cc28"), Style.WHITE));
		STYLES.put("vrs|T62", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
		STYLES.put("vrs|T63", new Style(Style.parseColor("#73d2f6"), Style.WHITE));
		STYLES.put("vrs|T65", new Style(Style.parseColor("#b3db18"), Style.WHITE));
		STYLES.put("vrs|T66", new Style(Style.parseColor("#ec008c"), Style.WHITE));
		STYLES.put("vrs|T67", new Style(Style.parseColor("#f680c5"), Style.WHITE));
		STYLES.put("vrs|T68", new Style(Style.parseColor("#ca93d0"), Style.WHITE));

		// Busse Bonn
		STYLES.put("vrs|B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
		STYLES.put("vrs|BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
		STYLES.put("vrs|BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
		STYLES.put("vrs|B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
		STYLES.put("vrs|B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
		STYLES.put("vrs|B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
		STYLES.put("vrs|B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
		STYLES.put("vrs|B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
		STYLES.put("vrs|B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
		STYLES.put("vrs|B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
		STYLES.put("vrs|B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
		STYLES.put("vrs|B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
		STYLES.put("vrs|B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
		STYLES.put("vrs|B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
		STYLES.put("vrs|B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
		STYLES.put("vrs|B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
		STYLES.put("vrs|B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
		STYLES.put("vrs|B614", new Style(Style.parseColor("#806839"), Style.WHITE));
		STYLES.put("vrs|B615", new Style(Style.parseColor("#532700"), Style.WHITE));
		STYLES.put("vrs|B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
		STYLES.put("vrs|B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
		STYLES.put("vrs|B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
		STYLES.put("vrs|B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
		STYLES.put("vrs|B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
		STYLES.put("vrs|B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
		STYLES.put("vrs|B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
		STYLES.put("vrs|B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
		STYLES.put("vrs|BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
		STYLES.put("vrs|BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
		STYLES.put("vrs|BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		
		STYLES.put("vrs|S", new Style(Style.parseColor("#f18e00"), Style.WHITE));
		STYLES.put("vrs|R", new Style(Style.parseColor("#009d81"), Style.WHITE));
	}

	public VrsProvider() {
		super(NetworkId.VRS);
		setStyles(STYLES);
	}
	
	@Override
	protected boolean hasCapability(Capability capability) {
		switch (capability) {
		case DEPARTURES:
			return true;
		case NEARBY_LOCATIONS:
			return true;
		case SUGGEST_LOCATIONS:
			return true;
		case TRIPS:
			return true;
		default:
			return false;
		}
	}

	// only stations supported
	public NearbyLocationsResult queryNearbyLocations(EnumSet<LocationType> types /* only STATION supported */, Location location, int maxDistance /* filtered */, int maxLocations) throws IOException {
		// g=p means group by product
		final StringBuilder parameters = new StringBuilder();
		parameters.append("?eID=tx_vrsinfo_ass2_timetable");
		if (location.hasId()) {
			parameters.append("&i=").append(location.id);
		} else {
			parameters.append("&r=").append(location.lat / 1E6).append(",").append(location.lon / 1E6);
		}
		parameters.append("&c=1");
		// c=1 limits the departures at each stop to 1 - actually we don't need any at this point
		if (maxLocations > 0) {
			parameters.append("&s=").append(maxLocations);
		}
		// s=number of stops
		final StringBuilder uri = new StringBuilder(API_BASE);
		if (!httpPost) {
			uri.append(parameters);
		}
		// System.out.println(uri);
		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, Charsets.UTF_8);
		try {
			final List<Location> locations = new ArrayList<Location>();
			final JSONObject head = new JSONObject(page.toString());
			if (contains(head, "error")) {
				return new NearbyLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), NearbyLocationsResult.Status.SERVICE_DOWN);
			}
			final JSONArray timetable = head.getJSONArray("timetable");
			long serverTime = 0;
			for (int i = 0; i < timetable.length(); i++) {
				final JSONObject entry = timetable.getJSONObject(i);
				final JSONObject stop = entry.getJSONObject("stop");
				final Location loc = parseLocation(stop);
				int distance = stop.getInt("distance");
				if (maxDistance > 0 && distance > maxDistance) {
					break; // we rely on the server side sorting
				}
				if (types.contains(loc.type) || types.contains(LocationType.ANY)) {
					locations.add(loc);
				}
				serverTime = parseDateTime(timetable.getJSONObject(i).getString("generated")).getTime();
			}
			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, serverTime != 0 ? serverTime : null, null);
			return new NearbyLocationsResult(header, locations);
		} catch (final JSONException x) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		} catch (ParseException e) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	// VRS does not show LongDistanceTrains departures. Parameter p for product
	// filter is supported, but LongDistanceTrains filter seems to be ignored.
	public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs /* not supported */) throws IOException {
		// g=p means group by product
		// d=minutes overwrites c=count and returns departures for the next d minutes
		final StringBuilder parameters = new StringBuilder();
		parameters.append("?eID=tx_vrsinfo_ass2_timetable&i=").append(stationId);
		parameters.append("&c=").append(maxDepartures);
		parameters.append("&t=");
		appendDate(parameters, time);
		final StringBuilder uri = new StringBuilder(API_BASE);
		if (!httpPost) {
			uri.append(parameters);
		}
		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, Charsets.UTF_8);
		try {
			final JSONObject head = new JSONObject(page.toString());
			if (contains(head, "error")) {
				return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryDeparturesResult.Status.SERVICE_DOWN);
			}
			final JSONArray timetable = head.getJSONArray("timetable");
			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);
			// for all stations
			for (int i = 0; i < timetable.length(); i++) {
				final List<Departure> departures = new ArrayList<Departure>();
				JSONObject station = timetable.getJSONObject(i);
				final Location location = parseLocation(station.getJSONObject("stop"));
				final JSONArray events = station.getJSONArray("events");
				// for all departures
				for (int j = 0; j < events.length(); j++) {
					JSONObject event = events.getJSONObject(j);
					Date plannedTime = null;
					Date predictedTime = null;
					if (contains(event, "departureScheduled")) {
						plannedTime = parseDateTime(event.getString("departureScheduled"));
						predictedTime = parseDateTime(event.getString("departure"));
					} else {
						plannedTime = parseDateTime(event.getString("departure"));
					}
					final JSONObject lineObj = event.getJSONObject("line");
					final Line line = parseLine(lineObj);
					Position position = null;
					if (contains(event, "post")) {
						JSONObject post = event.getJSONObject("post");
						final String positionStr = post.getString("name");
						position = new Position(positionStr.substring(positionStr.lastIndexOf(' ') + 1));
						// System.out.println("Position is " + position);
					}
					final Location destination = new Location(LocationType.STATION, null, null, lineObj.getString("direction"));
					Departure d = new Departure(plannedTime, predictedTime, line, position, destination, null, null);
					departures.add(d);
				}

				final List<LineDestination> lines = queryLinesForStation(location.id);

				result.stationDepartures.add(new StationDepartures(location, departures, lines));
			}

			return result;
		} catch (final JSONException x) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		} catch (ParseException e) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	private List<LineDestination> queryLinesForStation(String stationId) throws IOException {
		final List<LineDestination> lineDestinations = new ArrayList<LineDestination>();
		final StringBuilder parameters = new StringBuilder();
		parameters.append("?eID=tx_vrsinfo_his_info&i=").append(stationId);
		final StringBuilder uri = new StringBuilder(API_BASE);
		if (!httpPost) {
			uri.append(parameters);
		}
		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, Charsets.UTF_8);
		try {
			final JSONObject head = new JSONObject(page.toString());
			if (!contains(head, "his")) {
				return lineDestinations;
			}
			final JSONObject his = head.getJSONObject("his");
			final JSONArray lines = his.getJSONArray("lines");
			for (int i = 0; i < lines.length(); i++) {
				final JSONObject line = lines.getJSONObject(i);
				final String number = line.getString("number");
				final Product product = productFromLineNumber(number);
				final LineDestination lineDestination = new LineDestination(new Line(null /* id */, product, number, lineStyle("vrs", product, number)), null /* destination */);
				lineDestinations.add(lineDestination);
				// System.out.println("LineDestination " + lineDestination);
			}
		} catch (final JSONException x) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
		return lineDestinations;
	}

	public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException {
		// sc = station count
		// ac = address count
		// pc = points of interest count
		// t = sap (stops and/or addresses and/or pois)
		final String parameters = "?eID=tx_vrsinfo_ass2_objects&sc=10&ac=10&pc=10&t=sap&q=" + ParserUtils.urlEncode(new Location(LocationType.ANY, null, null, constraint.toString()).name);

		final StringBuilder uri = new StringBuilder(API_BASE);
		if (!httpPost) {
			uri.append(parameters);
		}

		// System.out.println(uri);

		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, Charsets.UTF_8);

		try {
			final List<SuggestedLocation> locations = new ArrayList<SuggestedLocation>();

			final JSONObject head = new JSONObject(page.toString());
			if (contains(head, "error")) {
				return new SuggestLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), SuggestLocationsResult.Status.SERVICE_DOWN);
			}
			final JSONArray stops = head.optJSONArray("stops");
			final JSONArray addresses = head.optJSONArray("addresses");
			final JSONArray pois = head.optJSONArray("pois");

			final int nStops = stops.length();
			for (int i = 0; i < nStops; i++) {
				final JSONObject stop = stops.optJSONObject(i);
				final Location location = parseLocation(stop);
				locations.add(new SuggestedLocation(location, i));
			}

			final int nPois = pois.length();
			for (int i = 0; i < nPois; i++) {
				final JSONObject poi = pois.optJSONObject(i);
				final Location location = parseLocation(poi);
				locations.add(new SuggestedLocation(location, i + stops.length()));
			}

			final int nAddresses = addresses.length();
			for (int i = 0; i < nAddresses; i++) {
				final JSONObject address = addresses.optJSONObject(i);
				final Location location = parseLocation(address);
				locations.add(new SuggestedLocation(location, i + stops.length() + pois.length()));
			}

			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
			return new SuggestLocationsResult(header, locations);
		} catch (final JSONException x) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
	}

	// http://android.vrsinfo.de/index.php?eID=tx_vrsinfo_ass2_router&c=1&f=2071&t=1504&d=2015-02-11T11%3A47%3A20%2B01%3A00
	// c: count (default: 5)
	// f: from (id or lat,lon as float)
	// v: via (id or lat,lon as float)
	// t: to (id or lat,lon as float)
	// a/d: date (default now)
	// vt: via time in minutes - not supported by Öffi
	// s: t => allow surcharge
	// p: products as comma separated list
	// TODO intermediate stops - how to query?
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, Date date, boolean dep, final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed /* not supported */, final @Nullable Accessibility accessibility /* not * supported */, @Nullable Set<Option> options /* not supported */) throws IOException {
		final List<Location> ambiguousFrom = new ArrayList<Location>();
		String fromString = generateLocation(from, ambiguousFrom);

		final List<Location> ambiguousVia = new ArrayList<Location>();
		String viaString = generateLocation(via, ambiguousVia);
		
		final List<Location> ambiguousTo = new ArrayList<Location>();
		String toString = generateLocation(to, ambiguousTo);
		
		if (!ambiguousFrom.isEmpty() || !ambiguousVia.isEmpty() || !ambiguousTo.isEmpty()) {
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT),
					ambiguousFrom.isEmpty() ? null : ambiguousFrom,
					ambiguousVia.isEmpty() ? null : ambiguousVia,
					ambiguousTo.isEmpty() ? null : ambiguousTo);
		}
		
		if (fromString == null) {
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_FROM);
		}
		if (via != null && viaString == null) {
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_VIA);
		}
		if (toString == null) {
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_TO);
		}

		final StringBuilder parameters = new StringBuilder();
		parameters.append("?eID=tx_vrsinfo_ass2_router&f=").append(fromString).append("&t=").append(toString);
		if (via != null) {
			parameters.append("&v=").append(via.id);
		}
		if (dep) {
			parameters.append("&d=");
		} else {
			parameters.append("&a=");
		}
		appendDate(parameters, date);
		parameters.append("&s=t");
		parameters.append("&p=");
		parameters.append(generateProducts(products));

		final StringBuilder uri = new StringBuilder(API_BASE);
		if (!httpPost) {
			uri.append(parameters);
		}

		// System.out.println(uri);

		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, Charsets.UTF_8);

		try {
			final List<Trip> trips = new ArrayList<Trip>();
			final JSONObject head = new JSONObject(page.toString());
			if (contains(head, "error")) {
				return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.SERVICE_DOWN);
			}
			final JSONArray routes = head.getJSONArray("routes");
			final Context context = new Context();
			// for all routes
			for (int i = 0; i < routes.length(); i++) {
				// System.out.println("Route " + i);
				final JSONObject route = routes.getJSONObject(i);
				final JSONArray segments = route.getJSONArray("segments");
				List<Leg> legs = new ArrayList<Leg>();
				Location tripOrigin = null;
				Location tripDestination = null;
				// for all segments
				for (int j = 0; j < segments.length(); j++) {
					// System.out.println("Segment " + j);
					final JSONObject segment = segments.getJSONObject(j);
					final String type = segment.getString("type");
					final JSONObject origin = segment.getJSONObject("origin");
					final Location segmentOrigin = parseLocation(origin);
					final Position segmentOriginPosition = parsePositionFromJSONObject(origin);
					if (j == 0) {
						tripOrigin = segmentOrigin;
					}
					final JSONObject destination = segment.getJSONObject("destination");
					final Location segmentDestination = parseLocation(destination);
					final Position segmentDestinationPosition = parsePositionFromJSONObject(destination);
					if (j == segments.length() - 1) {
						tripDestination = segmentDestination;
					}
					Date departurePlanned = null;
					Date departurePredicted = null;
					if (contains(segment, "departureScheduled")) {
						departurePlanned = parseDateTime(segment.getString("departureScheduled"));
						departurePredicted = (contains(segment, "departure")) ? parseDateTime(segment.getString("departure")) : null;
						context.departure(departurePredicted);
					} else if (contains(segment, "departure")) {
						departurePlanned = parseDateTime(segment.getString("departure"));
						context.departure(departurePlanned);
					}
					Date arrivalPlanned = null;
					Date arrivalPredicted = null;
					if (contains(segment, "arrivalScheduled")) {
						arrivalPlanned = parseDateTime(segment.getString("arrivalScheduled"));
						arrivalPredicted = (contains(segment, "arrival")) ? parseDateTime(segment.getString("arrival")) : null;
						context.arrival(arrivalPredicted);
					} else if (contains(segment, "arrival")) {
						arrivalPlanned = parseDateTime(segment.getString("arrival"));
						context.arrival(arrivalPlanned);
					}
					long traveltime = segment.getLong("traveltime");
					long distance = contains(segment, "distance") ? segment.getLong("distance") : 0;
					Line line = null;
					if (contains(segment, "line")) {
						JSONObject lineObject = segment.getJSONObject("line");
						line = parseLine(lineObject);
					}
					StringBuilder message = new StringBuilder();
					if (contains(segment, "infos")) {
						JSONArray infos = segment.getJSONArray("infos");
						for (int k = 0; k < infos.length(); k++) {
							if (k > 0) {
								message.append(", ");
							}
							message.append(infos.getJSONObject(k).getString("text"));
						}
						// System.out.println("Message is " + message.toString());
					}

					List<Point> points = new ArrayList<Point>();
					points.add(new Point(segmentOrigin.lat, segmentOrigin.lon));
					points.add(new Point(segmentDestination.lat, segmentDestination.lon));
					if (type.equals("walk")) {
						if (departurePlanned == null) {
							departurePlanned = legs.get(j - 1).getArrivalTime();
						}
						if (arrivalPlanned == null) {
							arrivalPlanned = new Date(legs.get(j - 1).getArrivalTime().getTime() + traveltime * 1000);
						}
						legs.add(new Trip.Individual(Trip.Individual.Type.WALK, segmentOrigin, departurePlanned, segmentDestination, arrivalPlanned, points, (int) distance));
						// System.out.println("walk from " + segmentOrigin + "//" + segmentOriginPosition + " at "+ departurePlanned + " to " + segmentDestination + "//" + segmentDestinationPosition + " at " + arrivalPlanned);
					} else if (type.equals("publicTransport")) {
						legs.add(new Trip.Public(line, segmentDestination,
								new Stop(segmentOrigin, true /* departure */, departurePlanned, departurePredicted, segmentOriginPosition, segmentOriginPosition),
								new Stop(segmentOrigin, false /* departure */, arrivalPlanned, arrivalPredicted, segmentDestinationPosition, segmentDestinationPosition),
								null /* intermediateStops */, points, message.toString()));
						// System.out.println("ride from " + segmentOrigin + "//" + segmentOriginPosition + " at "+ departurePlanned + " to " + segmentDestination + "//" + segmentDestinationPosition + " at " + arrivalPlanned);
					}
				}
				int changes = route.getInt("changes");
				List<Fare> fares = new ArrayList<Fare>();
				if (contains(route, "costs")) {
					final JSONObject costs = route.getJSONObject("costs");

					// final String name = costs.getString("name"); // seems constant "VRS-Tarif"
					final String text = costs.getString("text"); // e.g. "Preisstufe 4 [RegioTicket] 7,70 €", "VRR-Tarif! (Details: www.vrr.de)"
					float price = contains(costs, "price") ? (float) costs.getDouble("price") : 0; // e.g. 7.7 or not existent outside VRS
					// long zone = costs.getLong("zone"); // e.g. 2600
					// final String level = costs.getString("level"); // e.g. "4"

					fares.add(new Fare(text, Fare.Type.ADULT, Currency.getInstance("EUR"), price, null /* unitName */, null /* units */));
				}

				trips.add(new Trip(null /* id */, tripOrigin, tripDestination, legs, fares, null /* capacity */, changes));
			}
			long serverTime = parseDateTime(head.getString("generated")).getTime();
			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, serverTime, null);
			context.from = from;
			context.to = to;
			context.via = via;
			context.products = products;
			return new QueryTripsResult(header, uri.toString(), from, via, to, context, trips);
		} catch (final JSONException x) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		} catch (final ParseException e) {
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
		Context ctx = (Context) context;
		if (later) {
			return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getLastDeparture(), true, ctx.products, null, null, null);
		} else {
			return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getFirstArrival(), false, ctx.products, null, null, null);
		}
	}

	@Override
	public Style lineStyle(final @Nullable String network, final @Nullable Product product, final @Nullable String label)
	{
		if (product == Product.BUS && label != null && label.startsWith("SB")) {
			return super.lineStyle(network, product, "SB");
		}

		return super.lineStyle(network, product, label);
	}

	@Override
	public Point[] getArea() throws IOException {
		return new Point[] { new Point(50929981, 6967821) };
	}

	private Product productFromLineNumber(String number) {
		if (number.startsWith("I") || number.startsWith("E")) {
			return Product.HIGH_SPEED_TRAIN;
		} else if (number.startsWith("R") || number.startsWith("MRB") || number.startsWith("DPN")) {
			return Product.REGIONAL_TRAIN;
		} else if (number.startsWith("S")) {
			return Product.SUBURBAN_TRAIN;
		} else if (number.startsWith("U")) {
			return Product.SUBWAY;
		} else if (number.length() <= 2 || number.length() == 3 && (number.startsWith("70")|| number.startsWith("71"))) {
			return Product.TRAM;
		} else {
			return Product.BUS;
		}
	}

	private boolean contains(JSONObject jsonObject, String attribute) throws JSONException {
		final JSONArray attributes = jsonObject.names();
		for (int i = 0; i < attributes.length(); i++) {
			if (attributes.get(i).equals(attribute)) {
				return true;
			}
		}
		return false;
	}

	private Line parseLine(JSONObject line) throws JSONException {
		final String number = line.getString("number");
		final String product = line.getString("product");
		final Product productObj = parseProduct(product, number);
		final Style style = lineStyle("vrs", productObj, number);
		// System.out.format("Line %s has style %x %x %x\n", number, style.backgroundColor & 0xFFFFFF, style.foregroundColor & 0xFFFFFF, style.borderColor & 0xFFFFFF);
		return new Line(null /* id=? */, productObj, number, style);
	}

	private Product parseProduct(String product, String number) {
		if (product.equals("LongDistanceTrains")) {
			return Product.HIGH_SPEED_TRAIN;
		} else if (product.equals("RegionalTrains")) {
			return Product.REGIONAL_TRAIN;
		} else if (product.equals("SuburbanTrains")) {
			return Product.SUBURBAN_TRAIN;
		} else if (product.equals("Underground") || product.equals("LightRail") && number.startsWith("U")) {
			return Product.SUBWAY;
		} else if (product.equals("LightRail")) {
			return Product.TRAM;
		} else if (product.equals("Bus") || product.equals("CommunityBus") || product.equals("RailReplacementServices")) {
			return Product.BUS;
		} else if (product.equals("Boat")) {
			return Product.FERRY;
		} else if (product.equals("OnDemandServices")) {
			return Product.ON_DEMAND;
		} else {
			throw new IllegalArgumentException("unknown product: '" + product + "'");
		}
	}

	private String generateProducts(Set<Product> products) {
		StringBuilder ret = new StringBuilder();
		Iterator<Product> it = products.iterator();
		while (it.hasNext()) {
			final Product product = it.next();
			if (ret.length() > 0 && !ret.substring(ret.length() - 1).equals(",")) {
				ret.append(",");
			}
			ret.append(generateProduct(product));
		}
		return ret.toString();
	}

	private String generateProduct(Product product) {
		switch (product) {
		case BUS:
			return "Bus,CommunityBus";
		case CABLECAR:
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
		}
		throw new IllegalArgumentException("unknown product: '" + product + "'");
	}

	private Location parseLocation(JSONObject location) throws JSONException {
		final LocationType locationType;
		String id = null;
		String name = null;
		if (contains(location, "id")) {
			locationType = LocationType.STATION;
			id = location.getString("id");
			name = location.getString("name");
			// strip position (e.g.  "Bonn Hauptbahnhof (ZOB) - Bussteig F2")
			Matcher matcher = nameWithPosition.matcher(name);
			if (matcher.matches()) {
				name = matcher.group(1);
			}
		} else if (contains(location, "street")) {
			locationType = LocationType.ADDRESS;
			id = location.getString("tempId");
			name = (location.getString("street") + " " + location.getString("number")).trim();
		} else if (contains(location, "name")) {
			locationType = LocationType.POI;
			id = location.getString("tempId");
			name = location.getString("name");
		} else {
			locationType = LocationType.ANY;
		}
		String place = null;
		if (contains(location, "city")) {
			place = location.getString("city");
			if (contains(location, "district") && !location.getString("district").isEmpty()) {
				place += "-" + location.getString("district");
			}
		}
		final int lat = contains(location, "x") ? (int) Math.round(location.getDouble("x") * 1E6) : 0;
		final int lon = contains(location, "y") ? (int) Math.round(location.getDouble("y") * 1E6) : 0;
		return new Location(locationType, id, lat, lon, place, name);
	}

	private String generateLocation(Location loc, List<Location> ambiguous) throws IOException {
		if (loc == null) {
			return null;
		} else if (loc.id != null) {
			return loc.id;
		} else if (loc.lat != 0 && loc.lon != 0) {
			return String.format(Locale.ENGLISH, "%f,%f", (double) loc.lat / 1E6, (double) loc.lon / 1E6);
		} else {
			SuggestLocationsResult suggestLocationsResult = suggestLocations(loc.name);
			final List<Location> suggestedLocations = suggestLocationsResult.getLocations();
			if (suggestedLocations.size() == 1) {
				return suggestedLocations.get(0).id;
			} else {
				ambiguous.addAll(suggestedLocations);
				return null;
			}
		}
	}

	private final void appendDate(final StringBuilder uri, final Date time) {
		final Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		c.setTime(time);
		final int year = c.get(Calendar.YEAR);
		final int month = c.get(Calendar.MONTH) + 1;
		final int day = c.get(Calendar.DAY_OF_MONTH);
		final int hour = c.get(Calendar.HOUR_OF_DAY);
		final int minute = c.get(Calendar.MINUTE);
		final int second = c.get(Calendar.SECOND);
		uri.append(ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second)));
	}

	private final Date parseDateTime(final String dateTimeStr) throws ParseException {
		return dateTimeFormat.parse(dateTimeStr.substring(0, dateTimeStr.lastIndexOf(':')) + "00");
	}

	private Position parsePositionFromJSONObject(JSONObject location) throws JSONException {
		if (contains(location, "id")) {
			final String name = location.getString("name");
			if (name != null) {
				Matcher matcher = nameWithPosition.matcher(name);
				if (matcher.matches()) {
					final String position = matcher.group(2);
					return new Position(position.substring(position.lastIndexOf(" ") + 1));
				}
			}
		}
		return null;
	}
}
