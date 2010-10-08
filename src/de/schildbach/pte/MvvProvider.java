/*
 * Copyright 2010 the original author or authors.
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Autocomplete;
import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider
{
	public static final String NETWORK_ID = "efa.mvv-muenchen.de";
	private static final String API_BASE = "http://efa.mvv-muenchen.de/mobile/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;
	private static final String ENCODING = "ISO-8859-1";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		// TODO remove
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS)
				return false;

		return true;
	}

	private static final String AUTOCOMPLETE_URI = "http://efa.mvv-muenchen.de/ultralite/XML_STOPFINDER_REQUEST"
			+ "?coordOutputFormat=WGS84&anyObjFilter_sf=126&locationServerActive=1&name_sf=%s&type_sf=%s";
	private static final String AUTOCOMPLETE_TYPE = "any"; // any, stop, street, poi
	private static final Pattern P_AUTOCOMPLETE_COARSE = Pattern.compile("<p>(.*?)</p>");
	private static final Pattern P_AUTOCOMPLETE_FINE = Pattern.compile(".*?<n>(.*?)</n>.*?<ty>(.*?)</ty>.*?<id>(.*?)</id>.*?<pc>(.*?)</pc>.*?");

	@Override
	protected String autocompleteUri(final CharSequence constraint)
	{
		return String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING), AUTOCOMPLETE_TYPE);
	}

	@Override
	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = autocompleteUri(constraint);
		final CharSequence page = ParserUtils.scrape(uri);

		final List<Autocomplete> results = new ArrayList<Autocomplete>();

		final Matcher mAutocompleteCoarse = P_AUTOCOMPLETE_COARSE.matcher(page);
		while (mAutocompleteCoarse.find())
		{
			final Matcher mAutocompleteFine = P_AUTOCOMPLETE_FINE.matcher(mAutocompleteCoarse.group(1));
			if (mAutocompleteFine.matches())
			{
				final String location = mAutocompleteFine.group(1) + ", " + mAutocompleteFine.group(4);
				final String type = mAutocompleteFine.group(2);
				final int locationId = Integer.parseInt(mAutocompleteFine.group(3));

				if (type.equals("stop"))
					results.add(new Autocomplete(LocationType.STATION, locationId, location));
				else if (type.equals("street"))
					results.add(new Autocomplete(LocationType.ADDRESS, 0, location));
				else if (type.equals("singlehouse"))
					results.add(new Autocomplete(LocationType.ADDRESS, 0, location));
				else if (type.equals("poi"))
					results.add(new Autocomplete(LocationType.POI, 0, location));
				else
					throw new IllegalStateException("unknown type " + type + " on " + uri);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mAutocompleteCoarse.group(1) + "' on " + uri);
			}
		}

		return results;
	}

	private static final String NEARBY_LATLON_URI = "http://efa.mvv-muenchen.de/ultralite/XML_DM_REQUEST"
			+ "?mode=direct&coordOutputFormat=WGS84&mergeDep=1&useAllStops=1&name_dm=%2.6f:%2.6f:WGS84&type_dm=coord&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&excludedMeans=checkbox";

	@Override
	protected String nearbyLatLonUri(final int lat, final int lon)
	{
		return String.format(NEARBY_LATLON_URI, latLonToDouble(lon), latLonToDouble(lat));
	}

	private static final String NEARBY_STATION_URI = "http://efa.mvv-muenchen.de/ultralite/XML_DM_REQUEST"
			+ "?mode=direct&coordOutputFormat=WGS84&mergeDep=1&useAllStops=1&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&excludedMeans=checkbox";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	private static final Pattern P_NEARBY_COARSE = Pattern.compile("<dp>(.*?)</dp>");
	private static final Pattern P_NEARBY_FINE = Pattern.compile(".*?<n>(.*?)</n>.*?<r>.*?<id>(.*?)</id>.*?</r>.*?(?:<c>(\\d+),(\\d+)</c>.*?)?");

	@Override
	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		String uri;
		if (lat != 0 || lon != 0)
			uri = nearbyLatLonUri(lat, lon);
		else if (stationId != null)
			uri = nearbyStationUri(stationId);
		else
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		final CharSequence page = ParserUtils.scrape(uri);

		final List<Station> stations = new ArrayList<Station>();

		final Matcher mNearbyCoarse = P_NEARBY_COARSE.matcher(page);
		while (mNearbyCoarse.find())
		{
			final Matcher mNearbyFine = P_NEARBY_FINE.matcher(mNearbyCoarse.group(1));
			if (mNearbyFine.matches())
			{
				final String sName = mNearbyFine.group(1).trim();
				final int sId = Integer.parseInt(mNearbyFine.group(2));
				final int sLon = mNearbyFine.group(3) != null ? Integer.parseInt(mNearbyFine.group(3)) : 0;
				final int sLat = mNearbyFine.group(4) != null ? Integer.parseInt(mNearbyFine.group(4)) : 0;

				final Station station = new Station(sId, sName, sLat, sLon, 0, null, null);
				stations.add(station);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mNearbyCoarse.group(1) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return new NearbyStationsResult(uri, stations);
		else
			return new NearbyStationsResult(uri, stations.subList(0, maxStations));
	}

	@Override
	protected String connectionsQueryUri(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final String products, final WalkSpeed walkSpeed)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
		final DateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
		final DateFormat MONTH_FORMAT = new SimpleDateFormat("M");
		final DateFormat DAY_FORMAT = new SimpleDateFormat("d");
		final DateFormat HOUR_FORMAT = new SimpleDateFormat("H");
		final DateFormat MINUTE_FORMAT = new SimpleDateFormat("m");

		final StringBuilder uri = new StringBuilder(API_BASE + "XSLT_TRIP_REQUEST2");
		uri.append("?language=de");
		uri.append("&sessionID=0");
		uri.append("&requestID=0");
		uri.append("&command=");
		uri.append("&execInst=");
		uri.append("&ptOptionsActive=1");
		uri.append("&itOptionsActive=1");
		uri.append("&imageFormat=PNG");
		uri.append("&imageWidth=400");
		uri.append("&imageHeight=300");
		uri.append("&imageOnly=1");
		uri.append("&imageNoTiles=1");
		uri.append("&itdLPxx_advancedOptions=0"); // LP=LayoutParams
		uri.append("&itdLPxx_odvPPType=");
		uri.append("&itdLPxx_execInst=");
		uri.append("&itdDateDay=").append(ParserUtils.urlEncode(DAY_FORMAT.format(date)));
		uri.append("&itdDateMonth=").append(ParserUtils.urlEncode(MONTH_FORMAT.format(date)));
		uri.append("&itdDateYear=").append(ParserUtils.urlEncode(YEAR_FORMAT.format(date)));
		uri.append("&locationServerActive=1");
		uri.append("&useProxFootSearch=1"); // Take nearby stops into account and possibly use them instead
		uri.append("&anySigWhenPerfectNoOtherMatches=1");
		// uri.append("&lineRestriction=403");

		if (fromType == LocationType.WGS84)
		{
			final String[] parts = from.split(",\\s*", 2);
			final int lat = Integer.parseInt(parts[0]);
			final int lon = Integer.parseInt(parts[1]);
			uri.append("&nameInfo_origin=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
			uri.append("&typeInfo_origin=coord");
		}
		else
		{
			uri.append("&useHouseNumberList_origin=1");
			uri.append("&place_origin="); // coarse-grained location, e.g. city
			uri.append("&placeState_origin=empty"); // empty|identified
			uri.append("&nameState_origin=empty"); // empty|identified|list|notidentified
			uri.append("&placeInfo_origin=invalid"); // invalid
			uri.append("&nameInfo_origin=invalid"); // invalid
			uri.append("&typeInfo_origin=invalid"); // invalid
			uri.append("&reducedAnyWithoutAddressObjFilter_origin=102");
			uri.append("&reducedAnyPostcodeObjFilter_origin=64");
			uri.append("&reducedAnyTooManyObjFilter_origin=2");
			uri.append("&type_origin=").append(locationType(fromType));
			uri.append("&name_origin=").append(ParserUtils.urlEncode(from, ENCODING)); // fine-grained location
			uri.append("&anyObjFilter_origin=").append(locationAnyObjFilter(fromType));
		}

		if (toType == LocationType.WGS84)
		{
			final String[] parts = to.split(",\\s*", 2);
			final int lat = Integer.parseInt(parts[0]);
			final int lon = Integer.parseInt(parts[1]);
			uri.append("&nameInfo_destination=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
			uri.append("&typeInfo_destination=coord");
		}
		else
		{
			uri.append("&useHouseNumberList_destination=1");
			uri.append("&place_destination="); // coarse-grained location, e.g. city
			uri.append("&placeState_destination=empty"); // empty|identified
			uri.append("&nameState_destination=empty"); // empty|identified|list|notidentified
			uri.append("&placeInfo_destination=invalid"); // invalid
			uri.append("&nameInfo_destination=invalid"); // invalid
			uri.append("&typeInfo_destination=invalid"); // invalid
			uri.append("&reducedAnyWithoutAddressObjFilter_destination=102");
			uri.append("&reducedAnyPostcodeObjFilter_destination=64");
			uri.append("&reducedAnyTooManyObjFilter_destination=2");
			uri.append("&type_destination=").append(locationType(toType));
			uri.append("&name_destination=").append(ParserUtils.urlEncode(to, ENCODING)); // fine-grained location
			uri.append("&anyObjFilter_destination=").append(locationAnyObjFilter(toType));
		}

		if (via != null)
		{
			if (viaType == LocationType.WGS84)
			{
				final String[] parts = via.split(",\\s*", 2);
				final int lat = Integer.parseInt(parts[0]);
				final int lon = Integer.parseInt(parts[1]);
				uri.append("&nameInfo_via=").append(String.format("%2.6f:%2.6f", lon / 1E6, lat / 1E6)).append(":WGS84[DD.dddddd]");
				uri.append("&typeInfo_via=coord");
			}
			else
			{
				uri.append("&useHouseNumberList_via=1");
				uri.append("&place_via=");
				uri.append("&placeState_via=empty");
				uri.append("&nameState_via=empty");
				uri.append("&placeInfo_via=invalid");
				uri.append("&nameInfo_via=invalid");
				uri.append("&typeInfo_via=invalid");
				uri.append("&reducedAnyWithoutAddressObjFilter_via=102");
				uri.append("&reducedAnyPostcodeObjFilter_via=64");
				uri.append("&reducedAnyTooManyObjFilter_via=2");
				uri.append("&type_via=").append(locationType(viaType));
				uri.append("&name_via=").append(ParserUtils.urlEncode(via, ENCODING));
				uri.append("&anyObjFilter_via=").append(locationAnyObjFilter(viaType));
			}
		}

		uri.append("&itdTripDateTimeDepArr=").append(dep ? "dep" : "arr");
		uri.append("&itdTimeHour=").append(ParserUtils.urlEncode(HOUR_FORMAT.format(date)));
		uri.append("&itdTimeMinute=").append(ParserUtils.urlEncode(MINUTE_FORMAT.format(date)));
		uri.append("&itdDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&changeSpeed=").append(WALKSPEED_MAP.get(walkSpeed));
		uri.append(productParams(products));

		return uri.toString();
	}

	private static String locationType(final LocationType locationType)
	{
		if (locationType == LocationType.STATION)
			return "stop";
		if (locationType == LocationType.ADDRESS)
			return "any"; // strange, matches with anyObjFilter
		if (locationType == LocationType.POI)
			return "any";
		if (locationType == LocationType.ANY)
			return "any";
		throw new IllegalArgumentException(locationType.toString());
	}

	private static int locationAnyObjFilter(final LocationType locationType)
	{
		if (locationType == LocationType.STATION)
			return 2;
		if (locationType == LocationType.ADDRESS)
			return 12;
		if (locationType == LocationType.POI)
			return 32;
		if (locationType == LocationType.ANY)
			return 0;
		throw new IllegalArgumentException(locationType.toString());
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile("<select name=\"(name_origin|name_destination|name_via)\"[^>]*>(.*?)</select>",
			Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile(
			"(Start und Ziel sind identisch)|(konnte keine Verbindung gefunden werden)", Pattern.CASE_INSENSITIVE);

	@Override
	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final String products, final WalkSpeed walkSpeed)
			throws IOException
	{
		final String uri = connectionsQueryUri(fromType, from, viaType, via, toType, to, date, dep, products, walkSpeed);
		System.out.println(uri);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.TOO_CLOSE;
			if (mError.group(2) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
		}

		List<Autocomplete> fromAddresses = null;
		List<Autocomplete> viaAddresses = null;
		List<Autocomplete> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<Autocomplete> addresses = new ArrayList<Autocomplete>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(new Autocomplete(LocationType.ANY, 0, address));
			}

			if (type.equals("name_origin"))
				fromAddresses = addresses;
			else if (type.equals("name_destination"))
				toAddresses = addresses;
			else if (type.equals("name_via"))
				viaAddresses = addresses;
			else
				throw new IOException(type);
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(uri, page);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*<b>Von:[\\xa0\\s]+</b>(.+?)<br />[\\xa0\\s]+"
			+ "<b>Nach:[\\xa0\\s]+</b>(.+?)<br />[\\xa0\\s]+" //
			+ "(?:<b>itdTripRequestDetails/via:[\\xa0\\s]+</b>(.+?)<br />[\\xa0\\s]+)?" //
			+ "<b>Datum:[\\xa0\\s]+</b>\\w{2}\\.,\\s(\\d+)\\.\\s(\\w{3,4})\\.[\\xa0\\s]+(\\d{4}).*?"
			+ "(?:<a href=\"(XSLT_TRIP_REQUEST2\\?language=de&amp;sessionID=[^&]+&amp;requestID=[^&]+&amp;command=tripPrev)\">.*?)?" //
			+ "(?:<a href=\"(XSLT_TRIP_REQUEST2\\?language=de&amp;sessionID=[^&]+&amp;requestID=[^&]+&amp;command=tripNext)\">.*?)?", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<div style=\"background-color:#\\w{6};\">(.+?)</div>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "(?:<font color=\"red\">(\\d+)\\.(\\d+)\\. </font>.*?)?" // date
			+ "<a href=\"(XSLT_TRIP_REQUEST2.*?itdLPxx_view=detail_\\d+)\">" // url
			+ "(?:" //
			+ "(\\d{1,2}:\\d{2})[\\xa0\\s]+-[\\xa0\\s]+(\\d{1,2}:\\d{2})" // departureTime, arrivalTime
			+ "|" //
			+ "Fußweg.*?Dauer:[\\xa0\\s]+(\\d{1,2}):(\\d{2})" // min
			+ ").*?", Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final String from = ParserUtils.resolveEntities(mHead.group(1));
			final String to = ParserUtils.resolveEntities(mHead.group(2));
			// final String via = ParserUtils.resolveEntities(mHead.group(3));
			final Date currentDate = parseDate(mHead.group(4), mHead.group(5), mHead.group(6));
			final String linkEarlier = mHead.group(7) != null ? API_BASE + ParserUtils.resolveEntities(mHead.group(7)) : null;
			final String linkLater = mHead.group(8) != null ? API_BASE + ParserUtils.resolveEntities(mHead.group(8)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			while (mConCoarse.find())
			{
				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(mConCoarse.group(1));
				if (mConFine.matches())
				{
					final String link = API_BASE + ParserUtils.resolveEntities(mConFine.group(3));

					if (mConFine.group(6) == null)
					{
						Date date;
						if (mConFine.group(1) != null)
							date = parseDate(mConFine.group(1), mConFine.group(2), new SimpleDateFormat("yyyy").format(currentDate));
						else
							date = currentDate;
						Date departureTime = ParserUtils.joinDateTime(date, ParserUtils.parseTime(mConFine.group(4)));
						if (!connections.isEmpty())
						{
							final long diff = ParserUtils.timeDiff(departureTime, connections.get(connections.size() - 1).departureTime);
							if (diff > PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								departureTime = ParserUtils.addDays(departureTime, -1);
							else if (diff < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								departureTime = ParserUtils.addDays(departureTime, 1);
						}
						Date arrivalTime = ParserUtils.joinDateTime(date, ParserUtils.parseTime(mConFine.group(5)));
						if (departureTime.after(arrivalTime))
							arrivalTime = ParserUtils.addDays(arrivalTime, 1);
						final Connection connection = new Connection(ParserUtils.extractId(link), link, departureTime, arrivalTime, null, null, 0,
								from, 0, to, null);
						connections.add(connection);
					}
					else
					{
						final int min = Integer.parseInt(mConFine.group(6)) * 60 + Integer.parseInt(mConFine.group(7));
						final Calendar calendar = new GregorianCalendar();
						final Date departureTime = calendar.getTime();
						calendar.add(Calendar.MINUTE, min);
						final Date arrivalTime = calendar.getTime();
						final Connection connection = new Connection(ParserUtils.extractId(link), link, departureTime, arrivalTime, null, null, 0,
								from, 0, to, null);
						connections.add(connection);
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(1) + "' on " + uri);
				}
			}

			return new QueryConnectionsResult(uri, from, to, currentDate, linkEarlier, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?<b>Detailansicht</b>.*?" //
			+ "<b>Datum:[\\xa0\\s]+</b>\\w{2}\\.,\\s(\\d+)\\.\\s(\\w{3,4})\\.[\\xa0\\s]+(\\d{4}).*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("" //
			+ "<tr bgcolor=\"#(\\w{6})\">\r\\x0a(.+?)</tr>.*?" //
			+ "<tr bgcolor=\"#\\1\">\r\\x0a(.+?)</tr>.*?" //
			+ "<tr bgcolor=\"#\\1\">\r\\x0a(.+?)</tr>", Pattern.DOTALL);
	static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("(?:" //
			+ "<td colspan=\"\\d+\">ab (\\d{1,2}:\\d{2})\\s(.*?)\\s*<.*?" // departureTime, departure
			+ "(?:<img src=\"images/means.*?\" alt=\"(.*?)\" />.*?)?" // product
			+ "<td>\\s*(.*?)\\s*<br />Richtung\\s*(.*?)\\s*</td>.*?" // line, destination
			+ "<td colspan=\"\\d+\">an (\\d{1,2}:\\d{2})\\s(.*?)\\s*<" // arrivalTime, arrival
			+ "|" //
			+ "<td colspan=\"\\d+\">ab (.*?)\\s*<.*?" // departure
			+ "Fußweg[\\xa0\\s]+\\(ca\\.[\\xa0\\s]+(\\d+)[\\xa0\\s]+Minute.*?" // min
			+ "<td colspan=\"\\d+\">an (.*?)\\s*<" // arrival
			+ "|" //
			+ ".*?<img src=\"images/means/seat.gif\" alt=\"Sitzenbleiber\" />" //
			+ ").*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_ERRORS = Pattern.compile("(session has expired)", Pattern.CASE_INSENSITIVE);

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Date currentDate = parseDate(mHead.group(1), mHead.group(2), mHead.group(3));
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			Date lastTime = currentDate;

			Date firstDepartureTime = null;
			String firstDeparture = null;
			Date lastArrivalTime = null;
			String lastArrival = null;
			String oldZebra = null;

			final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(page);
			while (mDetCoarse.find())
			{
				final String zebra = mDetCoarse.group(1);
				if (oldZebra != null && zebra.equals(oldZebra))
					throw new IllegalArgumentException("missed row? last:" + zebra);
				else
					oldZebra = zebra;

				final String set = mDetCoarse.group(2) + mDetCoarse.group(3) + mDetCoarse.group(4);
				final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(set);
				if (mDetFine.matches())
				{
					if (mDetFine.group(1) != null)
					{
						final Date departureTime = upTime(lastTime, ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mDetFine.group(1))));

						final String departure = ParserUtils.resolveEntities(mDetFine.group(2));
						if (departure != null && firstDeparture == null)
							firstDeparture = departure;

						final String product = ParserUtils.resolveEntities(mDetFine.group(3));

						final String line = ParserUtils.resolveEntities(mDetFine.group(4));

						final String destination = ParserUtils.resolveEntities(mDetFine.group(5));

						final Date arrivalTime = upTime(lastTime, ParserUtils.joinDateTime(currentDate, ParserUtils.parseTime(mDetFine.group(6))));

						final String arrival = ParserUtils.resolveEntities(mDetFine.group(7));

						final String normalizedLine = normalizeLine(product, line);

						parts.add(new Connection.Trip(normalizedLine, LINES.get(normalizedLine), 0, destination, departureTime, null, 0, departure,
								arrivalTime, null, 0, arrival));

						if (firstDepartureTime == null)
							firstDepartureTime = departureTime;

						lastArrival = arrival;
						lastArrivalTime = arrivalTime;
					}
					else if (mDetFine.group(8) != null)
					{
						final String departure = ParserUtils.resolveEntities(mDetFine.group(8));
						if (departure != null && firstDeparture == null)
							firstDeparture = departure;

						final String min = mDetFine.group(9);

						final String arrival = ParserUtils.resolveEntities(mDetFine.group(10));

						if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
						{
							final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
							parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), 0, lastFootway.departure, 0, arrival));
						}
						else
						{
							parts.add(new Connection.Footway(Integer.parseInt(min), 0, departure, 0, arrival));
						}

						lastArrival = arrival;
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + set + "' on " + uri);
				}
			}

			if (firstDepartureTime == null && lastArrivalTime == null && parts.size() == 1 && parts.get(0) instanceof Connection.Footway)
			{
				final Calendar calendar = new GregorianCalendar();
				firstDepartureTime = calendar.getTime();
				calendar.add(Calendar.MINUTE, ((Connection.Footway) parts.get(0)).min);
				lastArrivalTime = calendar.getTime();
			}

			return new GetConnectionDetailsResult(new Date(), new Connection(ParserUtils.extractId(uri), uri, firstDepartureTime, lastArrivalTime,
					null, null, 0, firstDeparture, 0, lastArrival, parts));
		}
		else
		{
			if (P_CONNECTION_DETAILS_ERRORS.matcher(page).find())
				throw new SessionExpiredException();
			else
				throw new IOException(page.toString());
		}
	}

	private static Date upTime(final Date lastTime, Date time)
	{
		while (time.before(lastTime))
			time = ParserUtils.addDays(time, 1);

		lastTime.setTime(time.getTime());

		return time;
	}

	@Override
	protected String commandLink(final String sessionId, final String command)
	{
		return null;
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("XSLT_DM_REQUEST");
		uri.append("?outputFormat=XML");
		uri.append("&coordOutputFormat=WGS84");
		uri.append("&type_dm=stop");
		uri.append("&name_dm=").append(stationId);
		uri.append("&mode=direct");
		return uri.toString();
	}

	private static final Pattern P_NORMALIZE_LINE_TRAM = Pattern.compile("[12]\\d");
	private static final Pattern P_NORMALIZE_LINE_NACHTTRAM = Pattern.compile("N[12]\\d");
	private static final Pattern P_NORMALIZE_LINE_METROBUS = Pattern.compile("[56]\\d");
	private static final Pattern P_NORMALIZE_LINE_STADTBUS = Pattern.compile("1\\d{2}");
	private static final Pattern P_NORMALIZE_LINE_NACHTBUS = Pattern.compile("N[48]\\d");
	private static final Pattern P_NORMALIZE_LINE_REGIONALBUS = Pattern.compile("\\d{3}[A-Z]?");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{4}");

	private String normalizeLine(final String product, final String line)
	{
		if (product == null)
		{
			if (P_NORMALIZE_LINE_METROBUS.matcher(line).matches())
				return "B" + line;
			if (P_NORMALIZE_LINE_STADTBUS.matcher(line).matches())
				return "B" + line;
			if (P_NORMALIZE_LINE_NACHTBUS.matcher(line).matches())
				return "B" + line;
			if (line.equals("N117")) // Ersatzbus für N17
				return "BN117";
			if (P_NORMALIZE_LINE_REGIONALBUS.matcher(line).matches())
				return "B" + line;
			if (line.equals("Schienenersatzverkehr"))
				return "BSEV";
			if (line.startsWith("MVV-Ruftaxi "))
				return "B" + line;
			if (P_NORMALIZE_LINE_TRAM.matcher(line).matches())
				return "T" + line;
			if (P_NORMALIZE_LINE_NACHTTRAM.matcher(line).matches())
				return "T" + line;
			if (LINES.containsKey("S" + line))
				return "S" + line;
			if (line.equals("S20/27"))
				return "S" + line;
			if (LINES.containsKey("U" + line))
				return "U" + line;
			if (line.startsWith("D "))
				return "R" + line;
			if (line.startsWith("RE "))
				return "R" + line;
			if (line.startsWith("RB "))
				return "R" + line;
			if (line.startsWith("ALX ")) // Alex
				return "R" + line;
			if (line.startsWith("BOB ")) // Bayerische Oberlandbahn
				return "R" + line;
			if (line.startsWith("VBG ")) // Vogtlandbahn
				return "R" + line;
			if (line.startsWith("ICE "))
				return "I" + line;
			if (line.startsWith("IC "))
				return "I" + line;
			if (line.startsWith("EC "))
				return "I" + line;
			if (line.startsWith("CNL "))
				return "I" + line;
			if (P_NORMALIZE_LINE_NUMBER.matcher(line).matches())
				return "?" + line;

			throw new IllegalStateException("cannot normalize null product, line " + line);
		}
		else if (product.equals("Bus"))
		{
			if (line.startsWith("Bus"))
				return "B" + line.substring(4);
			else if (line.startsWith("StadtBus"))
				return "B" + line.substring(9);
			else if (line.startsWith("MetroBus"))
				return "B" + line.substring(9);
			else if (line.startsWith("Regionalbus"))
				return "B" + line.substring(12);
			else
				return "B" + line;
		}
		else if (product.equals("Tram"))
		{
			if (line.startsWith("Tram"))
				return "T" + line.substring(5);
			else
				return "T" + line;
		}
		else if (product.equals("U-Bahn"))
		{
			if (line.startsWith("U-Bahn"))
				return "U" + line.substring(7);
			else
				return "U" + line;
		}
		else if (product.equals("S-Bahn"))
		{
			if (line.startsWith("S-Bahn"))
				return "S" + line.substring(7);
			else
				return "S" + line;
		}
		else if (product.equals("Zug"))
		{
			final String[] lineParts = line.split("\\s+");
			final String type = lineParts[0];
			final String number = lineParts[1];
			if (type.equals("IC"))
				return "I" + type + number;
			if (type.equals("ICE"))
				return "I" + type + number;
			if (type.equals("EC"))
				return "I" + type + number;
			if (type.equals("EN")) // EuroNight
				return "I" + type + number;
			if (type.equals("CNL"))
				return "I" + type + number;
			if (type.equals("RJ")) // Railjet, Österreich
				return "I" + type + number;
			if (type.equals("IRE")) // Franken-Sachsen-Express
				return "I" + type + number;
			if (type.equals("RB"))
				return "R" + type + number;
			if (type.equals("RE"))
				return "R" + type + number;
			if (type.equals("D"))
				return "R" + type + number;
			if (type.equals("BOB"))
				return "R" + type + number;
			if (type.equals("BRB")) // Bayerische Regiobahn
				return "R" + type + number;
			if (type.equals("ALX")) // Länderbahn und Vogtlandbahn
				return "R" + type + number;
			if (type.equals("ERB")) // Eurobahn
				return "R" + type + number;
			if (type.equals("VBG")) // Vogtlandbahn
				return "R" + line;

			throw new IllegalStateException("cannot normalize product " + product + " line " + line);
		}
		else if (product.equals("Schiff"))
		{
			return "F" + line;
		}
		else if (product.equals("Seilbahn")) // strangely marked as 'Seilbahn', but means 'Schienenersatzverkehr'
		{
			return "BSEV" + line;
		}

		throw new IllegalStateException("cannot normalize product " + product + " line " + line);
	}

	private static Date parseDate(final String day, final String month, final String year)
	{
		final Calendar calendar = new GregorianCalendar();
		calendar.clear();
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
		calendar.set(Calendar.MONTH, parseMonth(month));
		calendar.set(Calendar.YEAR, Integer.parseInt(year));
		return calendar.getTime();
	}

	private final static String[] MONTHS = new String[] { "Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sept", "Okt", "Nov", "Dez" };

	private static int parseMonth(final String month)
	{
		for (int m = 0; m < MONTHS.length; m++)
			if (MONTHS[m].equals(month))
				return m;

		throw new IllegalArgumentException("cannot parse month: " + month);
	}

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		LINES.put("SS1", new int[] { Color.parseColor("#00ccff"), Color.WHITE });
		LINES.put("SS2", new int[] { Color.parseColor("#66cc00"), Color.WHITE });
		LINES.put("SS3", new int[] { Color.parseColor("#880099"), Color.WHITE });
		LINES.put("SS4", new int[] { Color.parseColor("#ff0033"), Color.WHITE });
		LINES.put("SS6", new int[] { Color.parseColor("#00aa66"), Color.WHITE });
		LINES.put("SS7", new int[] { Color.parseColor("#993333"), Color.WHITE });
		LINES.put("SS8", new int[] { Color.BLACK, Color.parseColor("#ffcc00") });
		LINES.put("SS20", new int[] { Color.BLACK, Color.parseColor("#ffaaaa") });
		LINES.put("SS27", new int[] { Color.parseColor("#ffaaaa"), Color.WHITE });
		LINES.put("SA", new int[] { Color.parseColor("#231f20"), Color.WHITE });

		LINES.put("T12", new int[] { Color.parseColor("#883388"), Color.WHITE });
		LINES.put("T15", new int[] { Color.parseColor("#3366CC"), Color.WHITE });
		LINES.put("T16", new int[] { Color.parseColor("#CC8833"), Color.WHITE });
		LINES.put("T17", new int[] { Color.parseColor("#993333"), Color.WHITE });
		LINES.put("T18", new int[] { Color.parseColor("#66bb33"), Color.WHITE });
		LINES.put("T19", new int[] { Color.parseColor("#cc0000"), Color.WHITE });
		LINES.put("T20", new int[] { Color.parseColor("#00bbee"), Color.WHITE });
		LINES.put("T21", new int[] { Color.parseColor("#33aa99"), Color.WHITE });
		LINES.put("T23", new int[] { Color.parseColor("#fff000"), Color.WHITE });
		LINES.put("T25", new int[] { Color.parseColor("#ff9999"), Color.WHITE });
		LINES.put("T27", new int[] { Color.parseColor("#ff6600"), Color.WHITE });
		LINES.put("TN17", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN19", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN20", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });
		LINES.put("TN27", new int[] { Color.parseColor("#999999"), Color.parseColor("#ffff00") });

		LINES.put("UU1", new int[] { Color.parseColor("#227700"), Color.WHITE });
		LINES.put("UU2", new int[] { Color.parseColor("#bb0000"), Color.WHITE });
		LINES.put("UU2E", new int[] { Color.parseColor("#bb0000"), Color.WHITE });
		LINES.put("UU3", new int[] { Color.parseColor("#ee8800"), Color.WHITE });
		LINES.put("UU4", new int[] { Color.parseColor("#00ccaa"), Color.WHITE });
		LINES.put("UU5", new int[] { Color.parseColor("#bb7700"), Color.WHITE });
		LINES.put("UU6", new int[] { Color.parseColor("#0000cc"), Color.WHITE });
	}

	@Override
	public int[] lineColors(final String line)
	{
		final int[] lineColors = LINES.get(line);
		if (lineColors != null)
			return lineColors;
		else
			return super.lineColors(line);
	}
}
