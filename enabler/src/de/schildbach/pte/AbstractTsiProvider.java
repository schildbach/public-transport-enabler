/*
* Copyright 2014 Kjell Braden <afflux@pentabarf.de>
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
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.util.ParserUtils;

/**
* @author Kjell Braden <afflux@pentabarf.de>
*/
public abstract class AbstractTsiProvider extends AbstractNetworkProvider
{
	private static class Context implements QueryTripsContext
	{
		private static final long serialVersionUID = -6847355540229473013L;

		public final Accessibility accessibility;
		public final Location from;
		public final Set<Option> options;
		public final Collection<Product> products;
		public final Location to;
		public final Location via;
		public final WalkSpeed walkSpeed;

		public Date earliestArrival;
		public Date latestDeparture;

		public Context(Location from, Location via, Location to, Collection<Product> products, WalkSpeed walkSpeed, Accessibility accessibility,
				Set<Option> options)
		{
			this.from = from;
			this.via = via;
			this.to = to;
			this.products = products;
			this.walkSpeed = walkSpeed;
			this.accessibility = accessibility;
			this.options = options;
		}

		public boolean canQueryEarlier()
		{
			return true;
		}

		public boolean canQueryLater()
		{
			return true;
		}

		public QueryTripsResult queryMore(AbstractTsiProvider provider, boolean later) throws IOException
		{
			final Date date = later ? latestDeparture : earliestArrival;

			// XXX nicht sicher ob das so gewuenscht ist
			final Calendar c = Calendar.getInstance(provider.timeZone());
			c.setTime(date);
			c.add(Calendar.MINUTE, later ? 1 : -1);

			return provider.queryTripsFromContext(this, c.getTime(), later);
		}

		public void updateEarliestArrival(Date newTime) {
			if (newTime == null)
				return;
			if (earliestArrival == null || newTime.before(earliestArrival))
				earliestArrival = newTime;
		}

		public void updateLatestDeparture(Date newTime) {
			if (newTime == null)
				return;
			if (latestDeparture == null || newTime.after(latestDeparture))
				latestDeparture = newTime;
		}

	}

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final DateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
	private static final DateFormat timeFormat = new SimpleDateFormat("HH-mm", Locale.US);

	private static final String DEFAULT_STOPFINDER_ENDPOINT = "/Transport/v2/";
	private static final String DEFAULT_TRIP_ENDPOINT = "/journeyplanner/v2/";

	private static final ResultHeader header = new ResultHeader("TSI Cityway.fr");
	private static final Charset jsonEncoding = UTF_8;

	private static Map<String, Character> transportModeShorts = new HashMap<String, Character>();
	static
	{
		// HIGH_SPEED_TRAIN
		transportModeShorts.put("TGV", 'I');
		transportModeShorts.put("HST", 'I');

		// REGIONAL_TRAIN
		transportModeShorts.put("TRAIN", 'R');
		transportModeShorts.put("TER", 'R');

		// SUBURBAN_TRAIN
		transportModeShorts.put("LOCAL_TRAIN", 'S');

		// SUBWAY
		transportModeShorts.put("METRO", 'U');

		// TRAM
		transportModeShorts.put("TRAM", 'T');
		transportModeShorts.put("TRAMWAY", 'T');

		// BUS
		transportModeShorts.put("BUS", 'B');
		transportModeShorts.put("COACH", 'B');

		// CABLECAR
		transportModeShorts.put("TROLLEY", 'C');
		transportModeShorts.put("TROLLEY_BUS", 'C');
	}

	protected static double latLonToDouble(final int value)
	{
		return (double) value / 1000000;
	}

	private final String apiKey;
	private final String stopFinderEndpoint;
	private final String tripEndpoint;

	public AbstractTsiProvider(final String apiKey, final String apiBase)
	{
		this(apiKey, apiBase, null, null);
	}

	public AbstractTsiProvider(final String apiKey, final String tripEndpoint, final String stopFinderEndpoint)
	{
		this.apiKey = apiKey;
		this.tripEndpoint = tripEndpoint;
		this.stopFinderEndpoint = stopFinderEndpoint;
	}

	public AbstractTsiProvider(final String apiKey, final String apiBase, final String tripEndpoint, final String stopFinderEndpoint)
	{
		this(apiKey, apiBase + (tripEndpoint != null ? tripEndpoint : DEFAULT_TRIP_ENDPOINT), //
				apiBase + (stopFinderEndpoint != null ? stopFinderEndpoint : DEFAULT_STOPFINDER_ENDPOINT));
	}

	public List<Location> autocompleteStations(CharSequence constraint) throws IOException
	{
		final StringBuilder parameters = buildCommonRequestParams("SearchTripPoint", "json");
		parameters.append("&MaxItems=").append(50); // XXX good value?
		parameters.append("&Keywords=").append(ParserUtils.urlEncode(constraint.toString(), jsonEncoding));

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		uri.append(parameters);

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, jsonEncoding, null, 3);
		try
		{

			final List<Location> stations = new ArrayList<Location>();
			final JSONObject head = new JSONObject(page.toString());

			int status = head.getInt("StatusCode");

			if (status != 200)
				return stations;

			JSONArray dataArray = head.getJSONArray("Data");
			for (int i = 0; i < dataArray.length(); i++)
			{
				JSONObject data = dataArray.getJSONObject(i);
				final Location location = parseJsonTransportLocation(data);

				if (location.isIdentified()) // make sure the location is really identified
					// some addresses may not contain coordinates, we ignore them
					stations.add(location);
			}

			return stations;
		}
		catch (final JSONException x)
		{
			throw new ParserException(x);
		}
	}

	private final StringBuilder buildCommonRequestParams(final String method, final String outputFormat)
	{
		StringBuilder uri = new StringBuilder(method);

		uri.append('/').append(outputFormat);
		uri.append("?Key=").append(apiKey);
		return uri;
	}

	protected String createLineLabel(String mode, String number, String name, String operatorCode, String codeActivity)
	{
		final char modePrefix;
		if (transportModeShorts.containsKey(mode))
			modePrefix = transportModeShorts.get(mode);
		else
			modePrefix = '?';

		final StringBuilder result = new StringBuilder(Character.toString(modePrefix));

		if (number != null && !number.isEmpty())
			result.append(number);
		else if (operatorCode != null) {
			result.append(operatorCode);
			if (codeActivity != null)
				result.append(codeActivity);
		}
		else
			result.append(name);

		return result.toString();
	}

	private List<Location> identifyLocation(Location location) throws IOException
	{
		if (location.isIdentified())
			return Collections.singletonList(location);

		List<Location> result = autocompleteStations(location.uniqueShortName());
		if (result == null)
			return new ArrayList<Location>(0);

		return result;
	}

	protected NearbyStationsResult jsonCoordRequest(int lat, int lon, int maxDistance, int maxStations) throws IOException
	{
		final StringBuilder parameters = buildCommonRequestParams("SearchTripPoint", "json");
		parameters.append(String.format(Locale.FRENCH, "&Latitude=%2.6f&Longitude=%2.6f", latLonToDouble(lat), latLonToDouble(lon)));
		parameters.append("&MaxItems=").append(maxStations != 0 ? maxStations : 50);
		parameters.append("&Perimeter=").append(maxDistance != 0 ? maxDistance : 1320);
		parameters.append("&PointTypes=Stop_Place");

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		uri.append(parameters);

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, jsonEncoding, null, 3);
		try
		{

			final List<Location> stations = new ArrayList<Location>();
			final JSONObject head = new JSONObject(page.toString());

			int status = head.getInt("StatusCode");

			if (status != 200)
			{
				return new NearbyStationsResult(header, status == 300 ? NearbyStationsResult.Status.INVALID_STATION
						: NearbyStationsResult.Status.SERVICE_DOWN);
			}

			JSONArray dataArray = head.getJSONArray("Data");
			for (int i = 0; i < dataArray.length(); i++)
			{
				JSONObject data = dataArray.getJSONObject(i);
				stations.add(parseJsonTransportLocation(data));
			}

			return new NearbyStationsResult(header, stations);
		}
		catch (final JSONException x)
		{
			throw new ParserException(x);
		}
	}

	private String jsonOptString(JSONObject json, String key) throws JSONException
	{
		return json.isNull(key) ? null : json.getString(key);
	}

	protected Location jsonStationRequestCoord(int id) throws IOException
	{
		final StringBuilder parameters = buildCommonRequestParams("GetTripPoint", "json");
		parameters.append("&TripPointId=").append(id);
		parameters.append("&PointType=Stop_Place");

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		uri.append(parameters);

		final CharSequence page = ParserUtils.scrape(uri.toString(), null, jsonEncoding, null, 3);
		try
		{

			final JSONObject head = new JSONObject(page.toString());

			int status = head.getInt("StatusCode");

			if (status != 200)
				return null;

			JSONObject data = head.getJSONObject("Data");
			return parseJsonTransportLocation(data);
		}
		catch (final JSONException x)
		{
			throw new ParserException(x);
		}
	}

	protected Trip.Individual parseJsonJourneyplannerIndividualLeg(JSONObject legInfo) throws JSONException
	{
		final String transportMode = legInfo.getString("TransportMode");
		final Trip.Individual.Type type;
		if ("WALK".equals(transportMode))
		{
			type = Trip.Individual.Type.WALK;
		}
		else
		{
			throw new JSONException("unknown transportMode=" + transportMode);
		}

		int distance = 0;

		final JSONObject arrInfo = legInfo.getJSONObject("Arrival");
		final JSONObject depInfo = legInfo.getJSONObject("Departure");

		final Location departure = parseJsonJourneyplannerLocation(depInfo.getJSONObject("Site"));
		final Location arrival = parseJsonJourneyplannerLocation(arrInfo.getJSONObject("Site"));

		final String arrTimeStr = arrInfo.getString("Time");
		final String depTimeStr = depInfo.getString("Time");
		final Date depTime, arrTime;
		try
		{
			depTime = fullDateFormat.parse(depTimeStr);
			arrTime = fullDateFormat.parse(arrTimeStr);
		}
		catch (ParseException e)
		{
			throw new JSONException("failed to parse trip times: " + depTimeStr + " or " + arrTimeStr);
		}

		final JSONArray pathLinks = legInfo.getJSONObject("pathLinks").getJSONArray("PathLink");
		final List<Point> path = new ArrayList<Point>(pathLinks.length() + 1);

		path.add(new Point(departure.lat, departure.lon));
		for (int i = 0; i < pathLinks.length(); i++)
		{
			JSONObject pathLink = pathLinks.getJSONObject(i);
			distance += pathLink.getInt("Distance");

			final Location toLoc = parseJsonJourneyplannerLocation(pathLink.getJSONObject("Arrival").getJSONObject("Site"));
			path.add(new Point(toLoc.lat, toLoc.lon));
		}
		return new Trip.Individual(type, departure, depTime, arrival, arrTime, path, distance);
	}

	protected Trip.Leg parseJsonJourneyplannerLeg(JSONObject jsonObject) throws JSONException
	{
		final JSONObject legInfo = jsonObject.getJSONObject("Leg");
		final JSONObject ptrInfo = jsonObject.getJSONObject("PTRide");

		if (legInfo.isNull("TransportMode") && !ptrInfo.isNull("TransportMode"))
		{
			return parseJsonJourneyplannerPublicLeg(ptrInfo);
		}
		else if (!legInfo.isNull("TransportMode") && ptrInfo.isNull("TransportMode"))
		{
			return parseJsonJourneyplannerIndividualLeg(legInfo);
		}
		else
		{
			throw new JSONException("unknown leg type");
		}
	}

	protected Location parseJsonJourneyplannerLocation(JSONObject data) throws JSONException
	{
		final String locTypeStr = data.getString("Type");
		final LocationType locType;
		final int id;

		if ("POI".equals(locTypeStr))
		{
			locType = LocationType.POI;
			id = data.getInt("id");
		}
		else if ("BOARDING_POSITION".equals(locTypeStr))
		{
			locType = LocationType.STATION;
			if (!data.isNull("LogicalId"))
				id = data.getInt("LogicalId");
			else
				id = data.getInt("id");
		}
		else
		{
			id = data.optInt("id", 0);
			locType = LocationType.ADDRESS;
		}

		final Point coord;
		final JSONObject posObj = data.optJSONObject("Position");
		if (posObj != null)
		{
			final double lat = posObj.getDouble("Lat");
			final double lon = posObj.getDouble("Long");
			coord = new Point((int) Math.round(lat * 1E6), (int) Math.round(lon * 1E6));
		}
		else
		{
			coord = null;
		}

		final String name = data.getString("Name");
		final String place = jsonOptString(data, "CityName");
		return new Location(locType, id, coord.lat, coord.lon, place, name);
	}

	protected Trip.Public parseJsonJourneyplannerPublicLeg(JSONObject ptrInfo) throws JSONException
	{
		final Line line = parseJsonLine(ptrInfo);

		final JSONObject destObj = ptrInfo.optJSONObject("Direction");
		String destinationName = jsonOptString(ptrInfo, "Destination");
		if (destinationName == null && destObj != null)
			destinationName = destObj.optString("Name");
		final Location lineDestination = new Location(LocationType.ANY, 0, null, destinationName);

		final Stop departureStop, arrivalStop;

		final JSONObject departureInfo = ptrInfo.getJSONObject("Departure");
		final JSONObject arrivalInfo = ptrInfo.getJSONObject("Arrival");
		final Location departureLocation = parseJsonJourneyplannerLocation(departureInfo.getJSONObject("StopPlace"));
		final Location arrivalLocation = parseJsonJourneyplannerLocation(arrivalInfo.getJSONObject("StopPlace"));
		try
		{
			Date departureTime = fullDateFormat.parse(departureInfo.getString("Time"));
			Date arrivalTime = fullDateFormat.parse(arrivalInfo.getString("Time"));
			departureStop = new Stop(departureLocation, true, departureTime, null, null, null);
			arrivalStop = new Stop(arrivalLocation, false, arrivalTime, null, null, null);
		}
		catch (ParseException e)
		{
			throw new JSONException(e);
		}

		final JSONArray stepArray = ptrInfo.getJSONObject("steps").getJSONArray("Step");
		List<Stop> intermediateStops = new ArrayList<Stop>(stepArray.length() - 1);
		for (int i = 0; i < stepArray.length() - 1; i++)
		{
			final JSONObject enterStop = stepArray.getJSONObject(i).getJSONObject("Arrival");
			final JSONObject leaveStop = stepArray.getJSONObject(i + 1).getJSONObject("Departure");

			final Location location = parseJsonJourneyplannerLocation(leaveStop.getJSONObject("StopPlace"));
			Date enterTime;
			Date leaveTime;
			try
			{
				enterTime = fullDateFormat.parse(enterStop.getString("Time"));
				leaveTime = fullDateFormat.parse(leaveStop.getString("Time"));
				intermediateStops.add(new Stop(location, enterTime, null, leaveTime, null));
			}
			catch (ParseException e)
			{
				throw new JSONException(e);
			}
		}

		final String message = jsonOptString(ptrInfo, "Notes");

		return new Trip.Public(line, lineDestination, departureStop, arrivalStop, intermediateStops, null, message);
	}

	protected Trip parseJsonJourneyplannerTrip(JSONObject tripObject) throws JSONException
	{
		final JSONObject departureInfo = tripObject.getJSONObject("Departure");
		final JSONObject arrivalInfo = tripObject.getJSONObject("Arrival");
		final Location from = parseJsonJourneyplannerLocation(departureInfo.getJSONObject("Site"));
		final Location to = parseJsonJourneyplannerLocation(arrivalInfo.getJSONObject("Site"));

		final JSONArray legArray = tripObject.getJSONObject("sections").getJSONArray("Section");
		final List<Trip.Leg> legs = new ArrayList<Trip.Leg>(legArray.length());

		for (int i = 0; i < legArray.length(); i++)
		{
			legs.add(parseJsonJourneyplannerLeg(legArray.getJSONObject(i)));
		}

		return new Trip(null, from, to, legs, null, null, null);
	}

	protected Line parseJsonLine(JSONObject ptrInfo) throws JSONException
	{
		final JSONObject networkInfo = ptrInfo.optJSONObject("PTNetwork");
		final String network;
		if (networkInfo != null)
			network = jsonOptString(networkInfo, "Name");
		else
			network = null;

		final JSONObject lineInfo = ptrInfo.getJSONObject("Line");
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

		final String lineLabel = createLineLabel(transportMode, lineNumber, lineName, operatorCode, codeActivity);

		return new Line(lineInfo.getString("id"), lineLabel, lineStyle(network, lineLabel), null, null);
	}

	protected Location parseJsonTransportLocation(JSONObject data) throws JSONException
	{
		final int id = data.getInt("Id");
		final LocationType locType;

		switch (data.getInt("PointType"))
		{
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
		final int latInt = (int) Math.round(lat * 1E6);
		final int lonInt = (int) Math.round(lon * 1E6);

		final String name = data.getString("Name");
		String place = null;
		final JSONObject localityObj = data.optJSONObject("Locality");
		if (localityObj != null)
		{
			place = localityObj.getString("Name");
		}
		return new Location(locType, id, latInt, lonInt, place, name);
	}

	public QueryDeparturesResult queryDepartures(int stationId, int maxDepartures, boolean equivs) throws IOException
	{
		// unsupported
		return null;
	}

	public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException
	{
		return ((Context) context).queryMore(this, later);
	}

	public NearbyStationsResult queryNearbyStations(Location location, int maxDistance, int maxStations) throws IOException
	{
		Location queryLocation = location;
		if (!queryLocation.hasLocation())
		{
			if (location.type != LocationType.STATION)
				throw new IllegalArgumentException("cannot handle: " + location.type);
			queryLocation = jsonStationRequestCoord(location.id);
		}

		if (queryLocation == null)
			throw new IllegalArgumentException("null location or station not found");

		return jsonCoordRequest(location.lat, location.lon, maxDistance, maxStations);
	}

	public QueryTripsResult queryTrips(Location from, Location via, Location to, Date date, boolean dep, Collection<Product> products,
			WalkSpeed walkSpeed, Accessibility accessibility, Set<Option> options) throws IOException
	{

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
			return new QueryTripsResult(header, possibleFroms.size() > 1 ? possibleFroms : null, possibleVias.size() > 1 ? possibleVias : null,
					possibleTos.size() > 1 ? possibleTos : null);

		final Context ctx = new Context(possibleFroms.get(0), possibleVias.get(0), possibleTos.get(0), products, walkSpeed, accessibility, options);

		return queryTripsFromContext(ctx, date, dep);
	}

	protected QueryTripsResult queryTripsFromContext(Context ctx, Date date, boolean dep) throws IOException
	{
		final String mode;
		if (ctx.products != null)
		{
			final StringBuilder modeBuilder = new StringBuilder();
			for (Product p : ctx.products)
			{
				final String productName = translateToLocalProduct(p);
				if (productName != null)
					modeBuilder.append(productName).append("|");
			}
			mode = modeBuilder.substring(0, modeBuilder.length() - 1);
		}
		else
			mode = null;

		final String walkSpeedStr = translateWalkSpeed(ctx.walkSpeed);

		final StringBuilder parameters = buildCommonRequestParams("PlanTrip", "json");
		parameters.append("&Disruptions=").append(0); // XXX what does this even mean?
		parameters.append("&Algorithm=FASTEST");
		parameters.append("&MaxWalkDist=1000"); // XXX good value? (in meters)

		if (ctx.from.type == LocationType.STATION)
		{
			parameters.append("&DepType=STOP_PLACE&DepId=").append(ctx.from.id);
			parameters.append("%240"); // "$0"
		}
		else if (ctx.from.type == LocationType.POI)
		{
			parameters.append("&DepType=POI&DepId=").append(ctx.from.id);
			parameters.append("%240"); // "$0"
		}
		else
		{
			parameters.append("&DepLat=").append(latLonToDouble(ctx.from.lat));
			parameters.append("&DepLon=").append(latLonToDouble(ctx.from.lon));
		}

		if (ctx.to.type == LocationType.STATION)
		{
			parameters.append("&ArrType=STOP_PLACE&ArrId=").append(ctx.to.id);
			parameters.append("%240"); // "$0"
		}
		else if (ctx.to.type == LocationType.POI)
		{
			parameters.append("&ArrType=POI&ArrId=").append(ctx.to.id);
			parameters.append("%240"); // "$0"
		}
		else
		{
			parameters.append("&ArrLat=").append(latLonToDouble(ctx.to.lat));
			parameters.append("&ArrLon=").append(latLonToDouble(ctx.to.lon));
		}

		if (ctx.via != null)
		{
			if (ctx.via.type == LocationType.STATION)
			{
				parameters.append("&ViaType=STOP_PLACE&ViaId=").append(ctx.via.id);
				parameters.append("%240"); // "$0"
			}
			else if (ctx.via.type == LocationType.POI)
			{
				parameters.append("&ViaType=POI&ViaId=").append(ctx.via.id);
				parameters.append("%240"); // "$0"
			}
			else
			{
				parameters.append("&ViaLat=").append(latLonToDouble(ctx.via.lat));
				parameters.append("&ViaLon=").append(latLonToDouble(ctx.via.lon));
			}
		}

		parameters.append("&date=").append(dateFormat.format(date));

		final String timeModeParam = (dep) ? "&DepartureTime=" : "&ArrivalTime=";
		parameters.append(timeModeParam).append(timeFormat.format(date));

		parameters.append("&WalkSpeed=").append(walkSpeedStr);

		if (mode != null)
			parameters.append("&Modes=").append(ParserUtils.urlEncode(mode.toString(), jsonEncoding));

		final StringBuilder uri = new StringBuilder(tripEndpoint);
		uri.append(parameters);
		final CharSequence page = ParserUtils.scrape(uri.toString(), null, jsonEncoding, null, 3);
		try
		{
			final JSONObject head = new JSONObject(page.toString());

			final JSONObject statusObj = head.optJSONObject("Status");

			if (statusObj == null)
			{
				return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
			}

			final String statusStr = statusObj.optString("Code");

			if ("NO_SOLUTION_FOR_REQUEST".equals(statusStr))
			{
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
			}

			if (!"OK".equals(statusStr))
			{
				return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
			}

			final JSONArray tripArray = head.getJSONObject("trips").getJSONArray("Trip");
			final List<Trip> trips = new ArrayList<Trip>(tripArray.length());

			for (int i = 0; i < tripArray.length(); i++)
			{
				JSONObject tripObject = tripArray.getJSONObject(i);
				trips.add(parseJsonJourneyplannerTrip(tripObject));
			}

			if (trips.size() > 0)
			{
				ctx.updateEarliestArrival(trips.get(0).getLastArrivalTime());
				ctx.updateLatestDeparture(trips.get(trips.size() - 1).getFirstDepartureTime());
			}

			return new QueryTripsResult(header, uri.toString(), ctx.from, ctx.via, ctx.to, ctx, trips);
		}
		catch (final JSONException x)
		{
			throw new ParserException(x);
		}
	}

	protected TimeZone timeZone()
	{
		// Set to French time zone
		return TimeZone.getTimeZone("Europe/Paris");
	}

	protected abstract String translateToLocalProduct(Product p);

	/**
	 * @param walkSpeed
	 * @return walk speed in km/h
	 */
	protected String translateWalkSpeed(WalkSpeed walkSpeed)
	{
		switch (walkSpeed)
		{
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
