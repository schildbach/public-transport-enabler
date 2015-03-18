/*
 * Copyright 2015 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
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
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Michael Dyrna
 */
public class VrsProvider extends AbstractNetworkProvider
{
	@SuppressWarnings("serial")
	private static class Context implements QueryTripsContext
	{
		private Date lastDeparture = null;
		private Date firstArrival = null;
		public Location from;
		public Location via;
		public Location to;
		public Set<Product> products;

		private Context()
		{
		}

		public boolean canQueryLater()
		{
			return true;
		}

		public boolean canQueryEarlier()
		{
			return true;
		}

		public void departure(Date departure)
		{
			if (this.lastDeparture == null || this.lastDeparture.compareTo(departure) < 0)
			{
				this.lastDeparture = departure;
			}
		}

		public void arrival(Date arrival)
		{
			if (this.firstArrival == null || this.firstArrival.compareTo(arrival) > 0)
			{
				this.firstArrival = arrival;
			}
		}

		public Date getLastDeparture()
		{
			return this.lastDeparture;
		}

		public Date getFirstArrival()
		{
			return this.firstArrival;
		}
	}

	private static class LocationWithPosition
	{
		public LocationWithPosition(Location location, Position position)
		{
			this.location = location;
			this.position = position;
		}

		public Location location;
		public Position position;
	}

	// valid host names: www.vrsinfo.de, android.vrsinfo.de, ios.vrsinfo.de, ekap.vrsinfo.de (only SSL encrypted with
	// client certificate)
	// performance comparison March 2015 showed www.vrsinfo.de to be fastest for trips
	protected static final String API_BASE = "https://www.vrsinfo.de/index.php";
	protected static final String SERVER_PRODUCT = "vrs";

	@SuppressWarnings("serial")
	protected static final List<Pattern> nameWithPositionPatterns = new ArrayList<Pattern>()
	{
		{
			// Bonn Hauptbahnhof (ZOB) - Bussteig F2
			// Beuel Bf - D
			add(Pattern.compile("(.*) - (.*)"));
			// Breslauer Platz/Hbf (U) Gleis 2
			add(Pattern.compile("(.*) Gleis (.*)"));
			// Düren Bf (Bussteig D/E)
			add(Pattern.compile("(.*) \\(Bussteig (.*)\\)"));
		}
	};

	protected static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Schnellbusse VRR
		STYLES.put("BSB", new Style(Style.parseColor("#00919d"), Style.WHITE));

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

		// Busse Köln - source: http://www.koelnwiki.de/
		STYLES.put("BSB40", new Style(Style.parseColor("#FF0000"), Style.WHITE));
		STYLES.put("B120", new Style(Style.parseColor("#24C6E8"), Style.WHITE));
		STYLES.put("B121", new Style(Style.parseColor("#89E82D"), Style.WHITE));
		STYLES.put("B122", new Style(Style.parseColor("#4D44FF"), Style.WHITE));
		STYLES.put("B125", new Style(Style.parseColor("#FF9A2E"), Style.WHITE));
		STYLES.put("B126", new Style(Style.parseColor("#FF8EE5"), Style.WHITE));
		STYLES.put("B127", new Style(Style.parseColor("#D164A4"), Style.WHITE));
		STYLES.put("B130", new Style(Style.parseColor("#5AC0E8"), Style.WHITE));
		STYLES.put("B132", new Style(Style.parseColor("#E8840C"), Style.WHITE));
		STYLES.put("B133", new Style(Style.parseColor("#FF9EEE"), Style.WHITE));
		STYLES.put("B136", new Style(Style.parseColor("#C96C44"), Style.WHITE));
		STYLES.put("B139", new Style(Style.parseColor("#D13D1E"), Style.WHITE));
		STYLES.put("B140", new Style(Style.parseColor("#FFD239"), Style.WHITE));
		STYLES.put("B141", new Style(Style.parseColor("#2CE8D0"), Style.WHITE));
		STYLES.put("B142", new Style(Style.parseColor("#9E54FF"), Style.WHITE));
		STYLES.put("B143", new Style(Style.parseColor("#82E827"), Style.WHITE));
		STYLES.put("B144", new Style(Style.parseColor("#FF8930"), Style.WHITE));
		STYLES.put("B145", new Style(Style.parseColor("#24C6E8"), Style.WHITE));
		STYLES.put("B146", new Style(Style.parseColor("#F25006"), Style.WHITE));
		STYLES.put("B147", new Style(Style.parseColor("#FF8EE5"), Style.WHITE));
		STYLES.put("B148", new Style(Style.parseColor("#65B0FF"), Style.WHITE));
		STYLES.put("B151", new Style(Style.parseColor("#ECB43A"), Style.WHITE));
		STYLES.put("B152", new Style(Style.parseColor("#FFDE44"), Style.WHITE));
		STYLES.put("B153", new Style(Style.parseColor("#C069FF"), Style.WHITE));
		STYLES.put("B154", new Style(Style.parseColor("#E85D25"), Style.WHITE));
		STYLES.put("B156", new Style(Style.parseColor("#4B69EC"), Style.WHITE));
		STYLES.put("B157", new Style(Style.parseColor("#5CC3F9"), Style.WHITE));
		STYLES.put("B159", new Style(Style.parseColor("#FF00CC"), Style.WHITE));
		STYLES.put("B181", new Style(Style.parseColor("#333333"), Style.WHITE));
		STYLES.put("B185", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
		STYLES.put("B187", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
		STYLES.put("B190", new Style(Style.parseColor("#4D44FF"), Style.WHITE));
		STYLES.put("B250", new Style(Style.parseColor("#8FE84B"), Style.WHITE));
		STYLES.put("B260", new Style(Style.parseColor("#FF8365"), Style.WHITE));
		STYLES.put("B423", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
		STYLES.put("B434", new Style(Style.parseColor("#14E80B"), Style.WHITE));
		STYLES.put("B436", new Style(Style.parseColor("#BEEC49"), Style.WHITE));
		STYLES.put("B481", new Style(Style.parseColor("#D3D2D2"), Style.WHITE));
		STYLES.put("B965", new Style(Style.parseColor("#FF0000"), Style.WHITE));

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
		STYLES.put("B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
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

		STYLES.put("BN", new Style(Style.parseColor("#000000"), Style.WHITE));
		STYLES.put("BNE1", new Style(Style.parseColor("#993399"), Style.WHITE)); // default

		STYLES.put("S", new Style(Style.parseColor("#f18e00"), Style.WHITE));
		STYLES.put("R", new Style(Style.parseColor("#009d81"), Style.WHITE));
	}

	public VrsProvider()
	{
		super(NetworkId.VRS);

		setStyles(STYLES);
	}

	@Override
	protected boolean hasCapability(Capability capability)
	{
		switch (capability)
		{
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
	public NearbyLocationsResult queryNearbyLocations(EnumSet<LocationType> types /* only STATION supported */, Location location, int maxDistance,
			int maxLocations) throws IOException
	{
		// g=p means group by product; not used here
		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("?eID=tx_vrsinfo_ass2_timetable");
		if (location.hasLocation())
		{
			uri.append("&r=").append(String.format(Locale.ENGLISH, "%.6f,%.6f", location.lat / 1E6, location.lon / 1E6));
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("&i=").append(ParserUtils.urlEncode(location.id));
		}
		else
		{
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");
		}
		// c=1 limits the departures at each stop to 1 - actually we don't need any at this point
		uri.append("&c=1");
		if (maxLocations > 0)
		{
			// s=number of stops
			uri.append("&s=").append(Math.min(16, maxLocations)); // artificial server limit
		}

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, Charsets.UTF_8);

		// System.out.println(uri);
		// System.out.println(page);

		try
		{
			final List<Location> locations = new ArrayList<Location>();
			final JSONObject head = new JSONObject(page.toString());
			final String error = head.optString("error", null);
			if (error != null)
			{
				if (error.equals("Leere Koordinate.") || error.equals("Leere ASS-ID und leere Koordinate"))
					return new NearbyLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, 0, null), locations);
				else if (error.equals("ASS2-Server lieferte leere Antwort."))
					return new NearbyLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), NearbyLocationsResult.Status.SERVICE_DOWN);
				else
					throw new IllegalStateException("unknown error: " + error);
			}
			final JSONArray timetable = head.getJSONArray("timetable");
			long serverTime = 0;
			for (int i = 0; i < timetable.length(); i++)
			{
				final JSONObject entry = timetable.getJSONObject(i);
				final JSONObject stop = entry.getJSONObject("stop");
				final Location loc = parseLocationAndPosition(stop).location;
				int distance = stop.getInt("distance");
				if (maxDistance > 0 && distance > maxDistance)
				{
					break; // we rely on the server side sorting by distance
				}
				if (types.contains(loc.type) || types.contains(LocationType.ANY))
				{
					locations.add(loc);
				}
				serverTime = parseDateTime(timetable.getJSONObject(i).getString("generated")).getTime();
			}
			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT, null, serverTime, null);
			return new NearbyLocationsResult(header, locations);
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
		catch (final ParseException e)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	// VRS does not show LongDistanceTrains departures. Parameter p for product
	// filter is supported, but LongDistanceTrains filter seems to be ignored.
	// equivs not supported.
	public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs) throws IOException
	{
		// g=p means group by product; not used here
		// d=minutes overwrites c=count and returns departures for the next d minutes
		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("?eID=tx_vrsinfo_ass2_timetable&i=").append(ParserUtils.urlEncode(stationId));
		uri.append("&c=").append(maxDepartures);
		if (time != null)
		{
			uri.append("&t=");
			appendDate(uri, time);
		}
		final CharSequence page = ParserUtils.scrape(uri.toString(), null, Charsets.UTF_8);

		// System.out.println(uri);
		// System.out.println(page);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final String error = head.optString("error", null);
			if (error != null)
			{
				if (error.equals("ASS2-Server lieferte leere Antwort."))
					return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryDeparturesResult.Status.SERVICE_DOWN);
				else if (error.equals("Leere ASS-ID und leere Koordinate"))
					return new QueryDeparturesResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryDeparturesResult.Status.INVALID_STATION);
				else
					throw new IllegalStateException("unknown error: " + error);
			}
			final JSONArray timetable = head.getJSONArray("timetable");
			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);
			// for all stations
			if (timetable.length() == 0)
			{
				return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
			}
			for (int i = 0; i < timetable.length(); i++)
			{
				final List<Departure> departures = new ArrayList<Departure>();
				JSONObject station = timetable.getJSONObject(i);
				final Location location = parseLocationAndPosition(station.getJSONObject("stop")).location;
				final JSONArray events = station.getJSONArray("events");
				final List<LineDestination> lines = new ArrayList<LineDestination>();
				// for all departures
				for (int j = 0; j < events.length(); j++)
				{
					JSONObject event = events.getJSONObject(j);
					Date plannedTime = null;
					Date predictedTime = null;
					if (event.has("departureScheduled"))
					{
						plannedTime = parseDateTime(event.getString("departureScheduled"));
						predictedTime = parseDateTime(event.getString("departure"));
					}
					else
					{
						plannedTime = parseDateTime(event.getString("departure"));
					}
					final JSONObject lineObj = event.getJSONObject("line");
					final Line line = parseLine(lineObj);
					Position position = null;
					final JSONObject post = event.optJSONObject("post");
					if (post != null)
					{
						final String positionStr = post.getString("name");
						// examples for post:
						// (U) Gleis 2
						// Bonn Hauptbahnhof (ZOB) - Bussteig C4
						// A
						position = new Position(positionStr.substring(positionStr.lastIndexOf(' ') + 1));
					}
					final Location destination = new Location(LocationType.STATION, null /* id */, null /* place */, lineObj.getString("direction"));

					final LineDestination lineDestination = new LineDestination(line, destination);
					if (!lines.contains(lineDestination))
					{
						lines.add(lineDestination);
					}
					final Departure d = new Departure(plannedTime, predictedTime, line, position, destination, null, null);
					departures.add(d);
				}

				queryLinesForStation(location.id, lines);

				result.stationDepartures.add(new StationDepartures(location, departures, lines));
			}

			return result;
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
		catch (final ParseException e)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	private void queryLinesForStation(String stationId, List<LineDestination> lineDestinations) throws IOException
	{
		Set<String> lineNumbersAlreadyKnown = new HashSet<String>();
		for (LineDestination lineDestionation : lineDestinations)
		{
			lineNumbersAlreadyKnown.add(lineDestionation.line.label);
		}
		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("?eID=tx_vrsinfo_his_info&i=").append(ParserUtils.urlEncode(stationId));

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, Charsets.UTF_8);

		// System.out.println(uri);
		// System.out.println(page);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final JSONObject his = head.optJSONObject("his");
			if (his != null)
			{
				final JSONArray lines = his.optJSONArray("lines");
				if (lines != null)
				{
					for (int i = 0; i < lines.length(); i++)
					{
						final JSONObject line = lines.getJSONObject(i);
						final String number = processLineNumber(line.getString("number"));
						if (lineNumbersAlreadyKnown.contains(number))
						{
							continue;
						}
						final Product product = productFromLineNumber(number);
						String direction = null;
						final JSONArray postings = line.optJSONArray("postings");
						if (postings != null)
						{
							for (int j = 0; j < postings.length(); j++)
							{
								JSONObject posting = (JSONObject) postings.get(j);
								direction = posting.getString("direction");
								lineDestinations.add(new LineDestination(new Line(null /* id */, NetworkId.VRS.toString(), product, number,
										lineStyle("vrs", product, number)), new Location(LocationType.STATION, null /* id */, null /* place */,
										direction)));
							}
						}
						else
						{
							lineDestinations.add(new LineDestination(new Line(null /* id */, NetworkId.VRS.toString(), product, number, lineStyle(
									"vrs", product, number)), null /* direction */));
						}
					}
				}
			}
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
		Collections.sort(lineDestinations, new LineDestinationComparator());
	}

	private static class LineDestinationComparator implements Comparator<LineDestination>
	{
		public int compare(LineDestination o1, LineDestination o2)
		{
			return o1.line.compareTo(o2.line);
		}
	}

	public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException
	{
		// sc = station count
		final int sc = 10;
		// ac = address count
		final int ac = 5;
		// pc = points of interest count
		final int pc = 5;
		// t = sap (stops and/or addresses and/or pois)
		final String uri = API_BASE + "?eID=tx_vrsinfo_ass2_objects&sc=" + sc + "&ac=" + ac + "&pc=" + ac + "&t=sap&q="
				+ ParserUtils.urlEncode(new Location(LocationType.ANY, null, null, constraint.toString()).name);

		final CharSequence page = ParserUtils.scrape(uri, null, Charsets.UTF_8);

		// System.out.println(uri);
		// System.out.println(page);

		try
		{
			final List<SuggestedLocation> locations = new ArrayList<SuggestedLocation>();

			final JSONObject head = new JSONObject(page.toString());
			final String error = head.optString("error", null);
			if (error != null)
			{
				if (error.equals("ASS2-Server lieferte leere Antwort."))
					return new SuggestLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), SuggestLocationsResult.Status.SERVICE_DOWN);
				else if (error.equals("Leere Suche"))
					return new SuggestLocationsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), locations);
				else
					throw new IllegalStateException("unknown error: " + error);
			}
			final JSONArray stops = head.optJSONArray("stops");
			final JSONArray addresses = head.optJSONArray("addresses");
			final JSONArray pois = head.optJSONArray("pois");

			final int nStops = stops.length();
			for (int i = 0; i < nStops; i++)
			{
				final JSONObject stop = stops.optJSONObject(i);
				final Location location = parseLocationAndPosition(stop).location;
				locations.add(new SuggestedLocation(location, sc + ac + pc - i));
			}

			final int nAddresses = addresses.length();
			for (int i = 0; i < nAddresses; i++)
			{
				final JSONObject address = addresses.optJSONObject(i);
				final Location location = parseLocationAndPosition(address).location;
				locations.add(new SuggestedLocation(location, ac + pc - i));
			}

			final int nPois = pois.length();
			for (int i = 0; i < nPois; i++)
			{
				final JSONObject poi = pois.optJSONObject(i);
				final Location location = parseLocationAndPosition(poi).location;
				locations.add(new SuggestedLocation(location, pc - i));
			}

			final ResultHeader header = new ResultHeader(NetworkId.VRS, SERVER_PRODUCT);
			return new SuggestLocationsResult(header, locations);
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
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
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, Date date, boolean dep,
			final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed, final @Nullable Accessibility accessibility,
			@Nullable Set<Option> options) throws IOException
	{
		// The EXACT_POINTS feature generates an about 50% bigger API response, probably well compressible.
		final boolean EXACT_POINTS = true;
		final List<Location> ambiguousFrom = new ArrayList<Location>();
		String fromString = generateLocation(from, ambiguousFrom);

		final List<Location> ambiguousVia = new ArrayList<Location>();
		String viaString = generateLocation(via, ambiguousVia);

		final List<Location> ambiguousTo = new ArrayList<Location>();
		String toString = generateLocation(to, ambiguousTo);

		if (!ambiguousFrom.isEmpty() || !ambiguousVia.isEmpty() || !ambiguousTo.isEmpty())
		{
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), ambiguousFrom.isEmpty() ? null : ambiguousFrom,
					ambiguousVia.isEmpty() ? null : ambiguousVia, ambiguousTo.isEmpty() ? null : ambiguousTo);
		}

		if (fromString == null)
		{
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_FROM);
		}
		if (via != null && viaString == null)
		{
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_VIA);
		}
		if (toString == null)
		{
			return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_TO);
		}

		final StringBuilder uri = new StringBuilder(API_BASE);
		uri.append("?eID=tx_vrsinfo_ass2_router&f=").append(fromString).append("&t=").append(toString);
		if (via != null)
		{
			uri.append("&v=").append(via.id);
		}
		if (dep)
		{
			uri.append("&d=");
		}
		else
		{
			uri.append("&a=");
		}
		appendDate(uri, date);
		uri.append("&s=t");
		uri.append("&p=");
		uri.append(generateProducts(products));
		uri.append("&o=v");
		if (EXACT_POINTS)
		{
			uri.append("p");
		}

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, Charsets.UTF_8);

		// System.out.println(uri);
		// System.out.println(page);

		try
		{
			final List<Trip> trips = new ArrayList<Trip>();
			final JSONObject head = new JSONObject(page.toString());
			final String error = head.optString("error", null);
			if (error != null)
			{
				if (error.equals("ASS2-Server lieferte leere Antwort."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.SERVICE_DOWN);
				else if (error.equals("Keine Verbindungen gefunden."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.NO_TRIPS);
				else if (error.startsWith("Keine Verbindung gefunden."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.NO_TRIPS);
				else if (error.equals("Origin invalid."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_FROM);
				else if (error.equals("Via invalid."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_VIA);
				else if (error.equals("Destination invalid."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.UNKNOWN_TO);
				else if (error.equals("Produkt ungültig."))
					return new QueryTripsResult(new ResultHeader(NetworkId.VRS, SERVER_PRODUCT), QueryTripsResult.Status.NO_TRIPS);
				else
					throw new IllegalStateException("unknown error: " + error);
			}
			final JSONArray routes = head.getJSONArray("routes");
			final Context context = new Context();
			// for all routes
			for (int i = 0; i < routes.length(); i++)
			{
				final JSONObject route = routes.getJSONObject(i);
				final JSONArray segments = route.getJSONArray("segments");
				List<Leg> legs = new ArrayList<Leg>();
				Location tripOrigin = null;
				Location tripDestination = null;
				// for all segments
				for (int j = 0; j < segments.length(); j++)
				{
					final JSONObject segment = segments.getJSONObject(j);
					final String type = segment.getString("type");
					final JSONObject origin = segment.getJSONObject("origin");
					final LocationWithPosition segmentOriginLocationWithPosition = parseLocationAndPosition(origin);
					Location segmentOrigin = segmentOriginLocationWithPosition.location;
					final Position segmentOriginPosition = segmentOriginLocationWithPosition.position;
					if (j == 0)
					{
						// special case: first origin is an address
						if (from.type == LocationType.ADDRESS)
						{
							segmentOrigin = from;
						}
						tripOrigin = segmentOrigin;
					}
					final JSONObject destination = segment.getJSONObject("destination");
					final LocationWithPosition segmentDestinationLocationWithPosition = parseLocationAndPosition(destination);
					Location segmentDestination = segmentDestinationLocationWithPosition.location;
					final Position segmentDestinationPosition = segmentDestinationLocationWithPosition.position;
					if (j == segments.length() - 1)
					{
						// special case: last destination is an address
						if (to.type == LocationType.ADDRESS)
						{
							segmentDestination = to;
						}
						tripDestination = segmentDestination;
					}
					List<Stop> intermediateStops = new ArrayList<Stop>();
					final JSONArray vias = segment.optJSONArray("vias");
					if (vias != null)
					{
						for (int k = 0; k < vias.length(); k++)
						{
							final JSONObject viaJsonObject = vias.getJSONObject(k);
							Location viaLocation = parseLocationAndPosition(viaJsonObject).location;
							Date arrivalPlanned = null;
							Date arrivalPredicted = null;
							if (viaJsonObject.has("arrivalScheduled"))
							{
								arrivalPlanned = parseDateTime(viaJsonObject.getString("arrivalScheduled"));
								arrivalPredicted = (viaJsonObject.has("arrival")) ? parseDateTime(viaJsonObject.getString("arrival")) : null;
							}
							else if (segment.has("arrival"))
							{
								arrivalPlanned = parseDateTime(viaJsonObject.getString("arrival"));
							}
							final Stop intermediateStop = new Stop(viaLocation, false /* arrival */, arrivalPlanned, arrivalPredicted,
									null /* plannedPosition */, null /* predictedPosition */);
							intermediateStops.add(intermediateStop);
						}
					}
					Date departurePlanned = null;
					Date departurePredicted = null;
					if (segment.has("departureScheduled"))
					{
						departurePlanned = parseDateTime(segment.getString("departureScheduled"));
						departurePredicted = (segment.has("departure")) ? parseDateTime(segment.getString("departure")) : null;
						if (j == 0)
						{
							context.departure(departurePredicted);
						}
					}
					else if (segment.has("departure"))
					{
						departurePlanned = parseDateTime(segment.getString("departure"));
						if (j == 0)
						{
							context.departure(departurePlanned);
						}
					}
					Date arrivalPlanned = null;
					Date arrivalPredicted = null;
					if (segment.has("arrivalScheduled"))
					{
						arrivalPlanned = parseDateTime(segment.getString("arrivalScheduled"));
						arrivalPredicted = (segment.has("arrival")) ? parseDateTime(segment.getString("arrival")) : null;
						if (j == segments.length() - 1)
						{
							context.arrival(arrivalPredicted);
						}
					}
					else if (segment.has("arrival"))
					{
						arrivalPlanned = parseDateTime(segment.getString("arrival"));
						if (j == segments.length() - 1)
						{
							context.arrival(arrivalPlanned);
						}
					}
					long traveltime = segment.getLong("traveltime");
					long distance = segment.optLong("distance", 0);
					Line line = null;
					String direction = null;
					JSONObject lineObject = segment.optJSONObject("line");
					if (lineObject != null)
					{
						line = parseLine(lineObject);
						direction = lineObject.optString("direction", null);
					}
					StringBuilder message = new StringBuilder();
					JSONArray infos = segment.optJSONArray("infos");
					if (infos != null)
					{
						for (int k = 0; k < infos.length(); k++)
						{
							if (k > 0)
							{
								message.append(", ");
							}
							message.append(infos.getJSONObject(k).getString("text"));
						}
					}

					List<Point> points = new ArrayList<Point>();
					points.add(new Point(segmentOrigin.lat, segmentOrigin.lon));
					if (EXACT_POINTS && segment.has("polygon"))
					{
						parsePolygon(segment.getString("polygon"), points);
					}
					else
					{
						for (Stop intermediateStop : intermediateStops)
						{
							points.add(new Point(intermediateStop.location.lat, intermediateStop.location.lon));
						}
					}
					points.add(new Point(segmentDestination.lat, segmentDestination.lon));
					if (type.equals("walk"))
					{
						if (departurePlanned == null)
						{
							departurePlanned = legs.get(j - 1).getArrivalTime();
						}
						if (arrivalPlanned == null)
						{
							arrivalPlanned = new Date(legs.get(j - 1).getArrivalTime().getTime() + traveltime * 1000);
						}
						legs.add(new Trip.Individual(Trip.Individual.Type.WALK, segmentOrigin, departurePlanned, segmentDestination, arrivalPlanned,
								points, (int) distance));
					}
					else if (type.equals("publicTransport"))
					{
						legs.add(new Trip.Public(line, direction != null ? new Location(LocationType.STATION, null /* id */, null /* place */,
								direction) : null, new Stop(segmentOrigin, true /* departure */, departurePlanned, departurePredicted,
								segmentOriginPosition, segmentOriginPosition), new Stop(segmentDestination, false /* departure */, arrivalPlanned,
								arrivalPredicted, segmentDestinationPosition, segmentDestinationPosition), intermediateStops, points, Strings
								.emptyToNull(message.toString())));
					}
				}
				int changes = route.getInt("changes");
				List<Fare> fares = new ArrayList<Fare>();
				final JSONObject costs = route.optJSONObject("costs");
				if (costs != null)
				{
					final String name = costs.optString("name", null); // seems constant "VRS-Tarif"
					// final String text = costs.getString("text"); // e.g. "Preisstufe 4 [RegioTicket] 7,70 €",
					// "VRR-Tarif! (Details: www.vrr.de)", "NRW-Tarif"
					float price = (float) costs.optDouble("price", 0.0); // e.g. 7.7 or not existent outside VRS
					// long zone = costs.getLong("zone"); // e.g. 2600
					final String level = costs.has("level") ? "Preisstufe " + costs.getString("level") : null; // e.g.
																												// "4"

					if (name != null && price != 0.0 && level != null)
					{
						fares.add(new Fare(name, Fare.Type.ADULT, Currency.getInstance("EUR"), price, level, null /* units */));
					}
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
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
		catch (final ParseException e)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, e);
		}
	}

	protected static void parsePolygon(final String polygonStr, final List<Point> polygonArr)
	{
		if (polygonStr != null && !polygonStr.isEmpty())
		{
			String pointsArr[] = polygonStr.split("\\s");
			for (String point : pointsArr)
			{
				String latlon[] = point.split(",");
				polygonArr
						.add(new Point((int) Math.round(Double.parseDouble(latlon[0]) * 1E6), (int) Math.round(Double.parseDouble(latlon[1]) * 1E6)));
			}
		}
	}

	public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException
	{
		Context ctx = (Context) context;
		if (later)
		{
			return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getLastDeparture(), true, ctx.products, null, null, null);
		}
		else
		{
			return queryTrips(ctx.from, ctx.via, ctx.to, ctx.getFirstArrival(), false, ctx.products, null, null, null);
		}
	}

	@Override
	public Style lineStyle(final @Nullable String network, final @Nullable Product product, final @Nullable String label)
	{
		if (product == Product.BUS && label != null && label.startsWith("SB"))
		{
			return super.lineStyle(network, product, "SB");
		}

		return super.lineStyle(network, product, label);
	}

	@Override
	public Point[] getArea() throws IOException
	{
		return new Point[] { new Point(50937531, 6960279) };
	}

	private static Product productFromLineNumber(String number)
	{
		if (number.startsWith("I") || number.startsWith("E"))
		{
			return Product.HIGH_SPEED_TRAIN;
		}
		else if (number.startsWith("R") || number.startsWith("MRB") || number.startsWith("DPN"))
		{
			return Product.REGIONAL_TRAIN;
		}
		else if (number.startsWith("S") && !number.startsWith("SB") && !number.startsWith("SEV"))
		{
			return Product.SUBURBAN_TRAIN;
		}
		else if (number.startsWith("U"))
		{
			return Product.SUBWAY;
		}
		else if (number.length() <= 2 && !number.startsWith("N"))
		{
			return Product.TRAM;
		}
		else
		{
			return Product.BUS;
		}
	}

	private Line parseLine(JSONObject line) throws JSONException
	{
		final String number = processLineNumber(line.getString("number"));
		final Product productObj = parseProduct(line.getString("product"), number);
		final Style style = lineStyle("vrs", productObj, number);
		return new Line(null /* id */, NetworkId.VRS.toString(), productObj, number, style);
	}

	private static String processLineNumber(final String number)
	{
		if (number.startsWith("AST ") || number.startsWith("VRM ") || number.startsWith("VRR "))
		{
			return number.substring(4);
		}
		else if (number.startsWith("AST") || number.startsWith("VRM") || number.startsWith("VRR"))
		{
			return number.substring(3);
		}
		else if (number.equals("Schienen-Ersatz-Verkehr (SEV)"))
		{
			return "SEV";
		}
		else
		{
			return number;
		}
	}

	private static Product parseProduct(String product, String number)
	{
		if (product.equals("LongDistanceTrains"))
		{
			return Product.HIGH_SPEED_TRAIN;
		}
		else if (product.equals("RegionalTrains"))
		{
			return Product.REGIONAL_TRAIN;
		}
		else if (product.equals("SuburbanTrains"))
		{
			return Product.SUBURBAN_TRAIN;
		}
		else if (product.equals("Underground") || product.equals("LightRail") && number.startsWith("U"))
		{
			return Product.SUBWAY;
		}
		else if (product.equals("LightRail"))
		{
			// note that also the Skytrain (Flughafen Düsseldorf Bahnhof - Flughafen Düsseldorf Terminan
			// and Schwebebahn Wuppertal (line 60) are both returned as product "LightRail".
			return Product.TRAM;
		}
		else if (product.equals("Bus") || product.equals("CommunityBus") || product.equals("RailReplacementServices"))
		{
			return Product.BUS;
		}
		else if (product.equals("Boat"))
		{
			return Product.FERRY;
		}
		else if (product.equals("OnDemandServices"))
		{
			return Product.ON_DEMAND;
		}
		else
		{
			throw new IllegalArgumentException("unknown product: '" + product + "'");
		}
	}

	private static String generateProducts(Set<Product> products)
	{
		StringBuilder ret = new StringBuilder();
		Iterator<Product> it = products.iterator();
		while (it.hasNext())
		{
			final Product product = it.next();
			final String productStr = generateProduct(product);
			if (ret.length() > 0 && !ret.substring(ret.length() - 1).equals(",") && !productStr.isEmpty())
			{
				ret.append(",");
			}
			ret.append(productStr);
		}
		return ret.toString();
	}

	private static String generateProduct(Product product)
	{
		switch (product)
		{
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

	public static LocationWithPosition parseLocationAndPosition(JSONObject location) throws JSONException
	{
		final LocationType locationType;
		String id = null;
		String name = null;
		String position = null;
		if (location.has("id"))
		{
			locationType = LocationType.STATION;
			id = location.getString("id");
			name = location.getString("name");
			for (Pattern pattern : nameWithPositionPatterns)
			{
				Matcher matcher = pattern.matcher(name);
				if (matcher.matches())
				{
					name = matcher.group(1);
					position = matcher.group(2);
					break;
				}
			}
		}
		else if (location.has("street"))
		{
			locationType = LocationType.ADDRESS;
			name = (location.getString("street") + " " + location.getString("number")).trim();
		}
		else if (location.has("name"))
		{
			locationType = LocationType.POI;
			id = location.getString("tempId");
			name = location.getString("name");
		}
		else if (location.has("x") && location.has("y"))
		{
			locationType = LocationType.ANY;
		}
		else
		{
			throw new IllegalArgumentException("unknown location JSONObject: " + location);
		}
		String place = location.optString("city", null);
		if (place != null)
		{
			if (location.has("district") && !location.getString("district").isEmpty())
			{
				place += "-" + location.getString("district");
			}
		}
		final int lat = (int) Math.round(location.optDouble("x", 0) * 1E6);
		final int lon = (int) Math.round(location.optDouble("y", 0) * 1E6);
		return new LocationWithPosition(new Location(locationType, id, lat, lon, place, name), position != null ? new Position(
				position.substring(position.lastIndexOf(" ") + 1)) : null);
	}

	private String generateLocation(Location loc, List<Location> ambiguous) throws IOException
	{
		if (loc == null)
		{
			return null;
		}
		else if (loc.id != null)
		{
			return loc.id;
		}
		else if (loc.lat != 0 && loc.lon != 0)
		{
			return String.format(Locale.ENGLISH, "%f,%f", loc.lat / 1E6, loc.lon / 1E6);
		}
		else
		{
			SuggestLocationsResult suggestLocationsResult = suggestLocations(loc.name);
			final List<Location> suggestedLocations = suggestLocationsResult.getLocations();
			if (suggestedLocations.size() == 1)
			{
				return suggestedLocations.get(0).id;
			}
			else
			{
				ambiguous.addAll(suggestedLocations);
				return null;
			}
		}
	}

	private final static void appendDate(final StringBuilder uri, final Date time)
	{
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

	private final static Date parseDateTime(final String dateTimeStr) throws ParseException
	{
		return new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZ").parse(dateTimeStr.substring(0, dateTimeStr.lastIndexOf(':')) + "00");
	}
}
