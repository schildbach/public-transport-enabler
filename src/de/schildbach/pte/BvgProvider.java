/*
 * Copyright 2010, 2011 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.exception.UnexpectedRedirectException;
import de.schildbach.pte.geo.Berlin;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public final class BvgProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.BVG;
	public static final String OLD_NETWORK_ID = "mobil.bvg.de";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	private static final String BASE_URL = "http://mobil.bvg.de";
	private static final String API_BASE = BASE_URL + "/Fahrinfo/bin/";

	private final String additionalQueryParameter;

	public BvgProvider(final String additionalQueryParameter)
	{
		super(API_BASE + "query.bin/dn", 8, null);

		this.additionalQueryParameter = additionalQueryParameter;
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.NEARBY_STATIONS)
				return false;

		return true;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		throw new UnsupportedOperationException();
	}

	private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*?) \\((.{4,}?)\\)(?: \\((U|S|S\\+U)\\))?");
	private static final Pattern P_SPLIT_NAME_COMMA = Pattern.compile("([^,]*), ([^,]*)");

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
		{
			final String su = mParen.group(3);
			return new String[] { mParen.group(2), mParen.group(1) + (su != null ? " (" + su + ")" : "") };
		}

		final Matcher mComma = P_SPLIT_NAME_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { mComma.group(1), mComma.group(2) };

		return super.splitPlaceAndName(name);
	}

	private final static Pattern P_NEARBY_OWN = Pattern.compile("/Fahrinfo/bin/query\\.bin.*?"
			+ "location=(\\d+),HST,WGS84,(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)&amp;label=([^\"]*)\"");
	private final static Pattern P_NEARBY_PAGE = Pattern.compile("<table class=\"ivuTableOverview\".*?<tbody>(.*?)</tbody>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("input=(\\d+)&[^\"]*\">([^<]*)<");
	private static final Pattern P_NEARBY_ERRORS = Pattern.compile("(Haltestellen in der Umgebung anzeigen)");

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.bin/dn?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			final CharSequence page = ParserUtils.scrape(uri.toString());

			final Matcher mError = P_NEARBY_ERRORS.matcher(page);
			if (mError.find())
			{
				if (mError.group(1) != null)
					return new NearbyStationsResult(null, NearbyStationsResult.Status.INVALID_STATION);
			}

			final List<Location> stations = new ArrayList<Location>();

			final Matcher mOwn = P_NEARBY_OWN.matcher(page);
			if (mOwn.find())
			{
				final int parsedId = Integer.parseInt(mOwn.group(1));
				final int parsedLon = (int) (Float.parseFloat(mOwn.group(2)) * 1E6);
				final int parsedLat = (int) (Float.parseFloat(mOwn.group(3)) * 1E6);
				final String[] parsedPlaceAndName = splitPlaceAndName(ParserUtils.urlDecode(mOwn.group(4), "ISO-8859-1"));
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, parsedPlaceAndName[0], parsedPlaceAndName[1]));
			}

			final Matcher mPage = P_NEARBY_PAGE.matcher(page);
			if (mPage.find())
			{
				final Matcher mCoarse = P_NEARBY_COARSE.matcher(mPage.group(1));

				while (mCoarse.find())
				{
					final Matcher mFineLocation = P_NEARBY_FINE_LOCATION.matcher(mCoarse.group(1));

					if (mFineLocation.find())
					{
						final int parsedId = Integer.parseInt(mFineLocation.group(1));
						final String[] parsedPlaceAndName = splitPlaceAndName(ParserUtils.resolveEntities(mFineLocation.group(2)));
						final Location station = new Location(LocationType.STATION, parsedId, parsedPlaceAndName[0], parsedPlaceAndName[1]);
						if (!stations.contains(station))
							stations.add(station);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
					}
				}

				if (maxStations == 0 || maxStations >= stations.size())
					return new NearbyStationsResult(null, stations);
				else
					return new NearbyStationsResult(null, stations.subList(0, maxStations));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	private static final String DEPARTURE_URL_LIVE = BASE_URL + "/IstAbfahrtzeiten/index/mobil?";

	private String departuresQueryLiveUri(final int stationId)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(DEPARTURE_URL_LIVE);
		uri.append("input=").append(stationId);
		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);
		return uri.toString();
	}

	private static final String DEPARTURE_URL_PLAN = API_BASE + "stboard.bin/dox/dox?boardType=dep&disableEquivs=yes&start=yes&";

	private String departuresQueryPlanUri(final int stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(DEPARTURE_URL_PLAN);
		uri.append("input=").append(stationId);
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50);
		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_PLAN_HEAD = Pattern.compile(".*?" //
			+ "<strong>(.*?)</strong>.*?Datum:\\s*([^<\n]+)[<\n].*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_PLAN_COARSE = Pattern.compile("" //
			+ "<tr class=\"ivu_table_bg\\d\">\\s*((?:<td class=\"ivu_table_c_dep\">|<td>).+?)\\s*</tr>" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_PLAN_FINE = Pattern.compile("" //
			+ "<td><strong>(\\d{1,2}:\\d{2})</strong></td>.*?" // time
			+ "<strong>\\s*(.*?)[\\s\\*]*</strong>.*?" // line
			+ "(?:\\((Gl\\. " + ParserUtils.P_PLATFORM + ")\\).*?)?" // position
			+ "<a href=\"/Fahrinfo/bin/stboard\\.bin/dox/dox.*?evaId=(\\d+)&[^>]*>" // destinationId
			+ "\\s*(.*?)\\s*</a>.*?" // destination
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_PLAN_ERRORS = Pattern.compile("(Bhf\\./Hst\\.:)|(Wartungsarbeiten)|" //
			+ "(http-equiv=\"refresh\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern P_DEPARTURES_LIVE_HEAD = Pattern.compile(".*?" //
			+ "<strong>(.*?)</strong>.*?Datum:\\s*([^<\n]+)[<\n].*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_LIVE_COARSE = Pattern.compile("" //
			+ "<tr class=\"ivu_table_bg\\d\">\\s*((?:<td class=\"ivu_table_c_dep\">|<td>).+?)\\s*</tr>" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_LIVE_FINE = Pattern.compile("" //
			+ "<td class=\"ivu_table_c_dep\">\\s*(\\d{1,2}:\\d{2})\\s*" // time
			+ "(\\*)?\\s*</td>\\s*" // planned
			+ "<td class=\"ivu_table_c_line\">\\s*(.*?)\\s*</td>\\s*" // line
			+ "<td>.*?<a.*?[^-]>\\s*(.*?)\\s*</a>.*?</td>" // destination
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_LIVE_MSGS_COARSE = Pattern.compile("" //
			+ "<tr class=\"ivu_table_bg\\d\">\\s*(<td class=\"ivu_table_c_line\">.+?)\\s*</tr>" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_LIVE_MSGS_FINE = Pattern.compile("" //
			+ "<td class=\"ivu_table_c_line\">\\s*(.*?)\\s*</td>\\s*" // line
			+ "<td class=\"ivu_table_c_dep\">\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s*</td>\\s*" // date
			+ "<td>([^<]*)</td>" // message
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_LIVE_ERRORS = Pattern.compile("(Haltestelle:)|(Wartungsgr&uuml;nden)|(http-equiv=\"refresh\")",
			Pattern.CASE_INSENSITIVE);

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final ResultHeader header = new ResultHeader(SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		if (stationId < 1000000) // live
		{
			// scrape page
			final String uri = departuresQueryLiveUri(stationId);
			final CharSequence page = ParserUtils.scrape(uri);

			final Matcher mError = P_DEPARTURES_LIVE_ERRORS.matcher(page);
			if (mError.find())
			{
				if (mError.group(1) != null)
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
				if (mError.group(2) != null)
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
				if (mError.group(3) != null)
					throw new UnexpectedRedirectException();
			}

			// parse page
			final Matcher mHead = P_DEPARTURES_LIVE_HEAD.matcher(page);
			if (mHead.matches())
			{
				final String[] placeAndName = splitPlaceAndName(ParserUtils.resolveEntities(mHead.group(1)));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				parseDateTime(currentTime, mHead.group(2));

				final Map<String, String> messages = new HashMap<String, String>();

				final Matcher mMsgsCoarse = P_DEPARTURES_LIVE_MSGS_COARSE.matcher(page);
				while (mMsgsCoarse.find())
				{
					final Matcher mMsgsFine = P_DEPARTURES_LIVE_MSGS_FINE.matcher(mMsgsCoarse.group(1));
					if (mMsgsFine.matches())
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mMsgsFine.group(1)));
						final String message = ParserUtils.resolveEntities(mMsgsFine.group(3)).replace('\n', ' ');
						messages.put(line, message);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mMsgsCoarse.group(1) + "' on " + uri);
					}
				}

				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_LIVE_COARSE.matcher(page);
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_LIVE_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final Calendar parsedTime = new GregorianCalendar(timeZone());
						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(1));

						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsedTime.add(Calendar.DAY_OF_MONTH, 1);

						boolean isPlanned = mDepFine.group(2) != null;

						Date plannedTime = null;
						Date predictedTime = null;
						if (!isPlanned)
							predictedTime = parsedTime.getTime();
						else
							plannedTime = parsedTime.getTime();

						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(3)));

						final String position = null;

						final int destinationId = 0;

						final String destination = ParserUtils.resolveEntities(mDepFine.group(4));

						final Departure dep = new Departure(plannedTime, predictedTime, new Line(null, line, line != null ? lineColors(line) : null),
								position, destinationId, destination, null, messages.get(line));
						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, placeAndName[0], placeAndName[1]),
						departures, null));
				return result;
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
			}
		}
		else
		{
			// scrape page
			final String uri = departuresQueryPlanUri(stationId, maxDepartures);
			final CharSequence page = ParserUtils.scrape(uri);

			final Matcher mError = P_DEPARTURES_PLAN_ERRORS.matcher(page);
			if (mError.find())
			{
				if (mError.group(1) != null)
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
				if (mError.group(2) != null)
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.SERVICE_DOWN);
				if (mError.group(3) != null)
					throw new UnexpectedRedirectException();
			}

			// parse page
			final Matcher mHead = P_DEPARTURES_PLAN_HEAD.matcher(page);
			if (mHead.matches())
			{
				final String[] placeAndName = splitPlaceAndName(ParserUtils.resolveEntities(mHead.group(1)));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				ParserUtils.parseGermanDate(currentTime, mHead.group(2));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_PLAN_COARSE.matcher(page);
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_PLAN_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final Calendar parsedTime = new GregorianCalendar(timeZone());
						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(1));

						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsedTime.add(Calendar.DAY_OF_MONTH, 1);

						final Date plannedTime = parsedTime.getTime();

						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(2)));

						final String position = ParserUtils.resolveEntities(mDepFine.group(3));

						final int destinationId = Integer.parseInt(mDepFine.group(4));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(5));

						final Departure dep = new Departure(plannedTime, null, new Line(null, line, line != null ? lineColors(line) : null),
								position, destinationId, destination, null, null);
						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, placeAndName[0], placeAndName[1]),
						departures, null));
				return result;
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
			}
		}
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	private static final String URL_ENCODING = "ISO-8859-1";

	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products)
	{
		final Calendar c = new GregorianCalendar(timeZone());
		c.setTime(date);

		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("query.bin/dn");

		uri.append("?start=Suchen");

		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from), URL_ENCODING));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to), URL_ENCODING));

		if (via != null)
		{
			// workaround, for there does not seem to be a REQ0JourneyStops1.0ID parameter

			uri.append("&REQ0JourneyStops1.0A=").append(locationType(via));

			if (via.type == LocationType.STATION && via.hasId() && isValidStationId(via.id))
			{
				uri.append("&REQ0JourneyStops1.0L=").append(via.id);
			}
			else if (via.hasLocation())
			{
				uri.append("&REQ0JourneyStops1.0X=").append(via.lon);
				uri.append("&REQ0JourneyStops1.0Y=").append(via.lat);
				if (via.name == null)
					uri.append("&REQ0JourneyStops1.0O=").append(
							ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%.6f, %.6f", via.lat / 1E6, via.lon / 1E6), URL_ENCODING));
			}
			else if (via.name != null)
			{
				uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via.name, URL_ENCODING));
				if (via.type != LocationType.ANY)
					uri.append('!');
			}
		}

		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(
				String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) - 2000));
		uri.append("&REQ0JourneyTime=").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));

		for (final char p : products.toCharArray())
		{
			if (p == 'I')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_5=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_5=1");
			}
			if (p == 'R')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_6=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_6=1");
			}
			if (p == 'S')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_0=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_0=1");
			}
			if (p == 'U')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_1=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_1=1");
			}
			if (p == 'T')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_2=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_2=1");
			}
			if (p == 'B')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_3=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_3=1");
			}
			if (p == 'P')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_7=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_7=1");
			}
			if (p == 'F')
			{
				uri.append("&REQ0JourneyProduct_prod_section_0_4=1");
				if (via != null)
					uri.append("&REQ0JourneyProduct_prod_section_1_4=1");
			}
		}

		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);

		return uri.toString();
	}

	@Override
	protected boolean isValidStationId(int id)
	{
		return id >= 1000000;
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select[^>]*name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>\n(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*(?:\\[([^\\]]*)\\]\\s*)?</option>", Pattern.DOTALL);

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final String uri = connectionsQueryUri(from, via, to, date, dep, products);
		final CharSequence page = ParserUtils.scrape(uri);

		List<Location> fromAddresses = null;
		List<Location> viaAddresses = null;
		List<Location> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String optionsType = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<Location> addresses = new ArrayList<Location>();
			while (mAddresses.find())
			{
				final String name = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				final String typeStr = ParserUtils.resolveEntities(mAddresses.group(2));
				final LocationType type;
				if (typeStr == null)
					type = LocationType.ANY;
				else if ("Haltestelle".equals(typeStr))
					type = LocationType.STATION;
				else if ("Sonderziel".equals(typeStr))
					type = LocationType.POI;
				else if ("Straße Hausnummer".equals(typeStr))
					type = LocationType.ADDRESS;
				else
					throw new IllegalStateException("cannot handle: '" + typeStr + "'");

				final Location location = new Location(type, 0, null, name + "!");
				if (!addresses.contains(location))
					addresses.add(location);
			}

			if (optionsType.equals("REQ0JourneyStopsS0K"))
				fromAddresses = addresses;
			else if (optionsType.equals("REQ0JourneyStopsZ0K"))
				toAddresses = addresses;
			else if (optionsType.equals("REQ0JourneyStops1.0K"))
				viaAddresses = addresses;
			else
				throw new IllegalStateException("cannot handle: '" + optionsType + "'");
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(new ResultHeader(SERVER_PRODUCT), fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(uri, page);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);
		return queryConnections(uri, page);
	}

	private Location location(final String typeStr, final String idStr, final String latStr, final String lonStr, final String nameStr)
	{
		final int id = idStr != null ? Integer.parseInt(idStr) : 0;
		final int lat = latStr != null ? Integer.parseInt(latStr) : 0;
		final int lon = lonStr != null ? Integer.parseInt(lonStr) : 0;
		final String[] placeAndName = splitPlaceAndName(nameStr);

		final LocationType type;
		if (typeStr == null)
			type = LocationType.ANY;
		else if ("STATION".equals(typeStr))
			type = LocationType.STATION;
		else if ("POI".equals(typeStr))
			type = LocationType.POI;
		else if ("ADDRESS".equals(typeStr) || "".equals(typeStr))
			type = LocationType.ADDRESS;
		else
			throw new IllegalArgumentException("cannot handle: " + typeStr);

		return new Location(type, id, lat, lon, placeAndName[0], placeAndName[1]);
	}

	private Location location(final String[] track)
	{
		final int id = track[4].length() > 0 ? Integer.parseInt(track[4]) : 0;
		final int lat = Integer.parseInt(track[6]);
		final int lon = Integer.parseInt(track[5]);
		final String[] placeAndName = splitPlaceAndName(ParserUtils.resolveEntities(track[9]));
		final String typeStr = track[1];

		final LocationType type;
		if ("STATION".equals(typeStr))
			type = LocationType.STATION;
		else if ("ADDRESS".equals(typeStr))
			type = LocationType.ADDRESS;
		else
			throw new IllegalArgumentException("cannot handle: " + Arrays.toString(track));

		return new Location(type, id, lat, lon, placeAndName[0], placeAndName[1]);
	}

	private static final Pattern P_CONNECTIONS_ALL_DETAILS = Pattern.compile("<a href=\"([^\"]*)\"[^>]*>Details f&uuml;r alle</a>");
	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "<td headers=\"ivuAnfFrom\"[^>]*>\n" //
			+ "(?:([^\n]*)\n)?" // from name
			+ "<a href=\"/Fahrinfo/[^\"]*?MapLocation\\.X=(\\d+)&MapLocation\\.Y=(\\d+)&[^\"]*?" // from lon, lat
			+ "MapLocation\\.type=(\\w*)&(?:MapLocation.extId=(\\d+)&)?.*?" // from type, id
			+ "(?:<td headers=\"ivuAnfVia1\"[^>]*>\n" //
			+ "([^\n]*)<.*?)?" // via name
			+ "<td headers=\"ivuAnfTo\"[^>]*>\n" //
			+ "(?:([^\n]*)\n)?" // to name
			+ "<a href=\"/Fahrinfo/[^\"]*?MapLocation\\.X=(\\d+)&MapLocation\\.Y=(\\d+)&[^\"]*?" // to lon, lat
			+ "MapLocation\\.type=(\\w*)&(?:MapLocation.extId=(\\d+)&)?.*?" // to type, id
			+ "<td headers=\"ivuAnfTime\"[^>]*>.., (\\d{2}\\.\\d{2}\\.\\d{2}) \\d{1,2}:\\d{2}</td>.*?" // date
			+ "(?:<a href=\"([^\"]*)\" title=\"fr&uuml;here Verbindungen\"[^>]*?>.*?)?" // linkEarlier
			+ "(?:<a href=\"([^\"]*)\" title=\"sp&auml;tere Verbindungen\"[^>]*?>.*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern
			.compile("<form [^>]*name=\"ivuTrackListForm(\\d)\"[^>]*>(.+?)</form>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "Verbindungen - Detailansicht - Abfahrt: am (\\d{2}\\.\\d{2}\\.\\d{2}) um \\d{1,2}:\\d{2}.*?" // date
			+ "guiVCtrl_connection_detailsOut_setStatus_([^_]+)_allHalts=yes.*?" // id
			+ "<input type=\"hidden\" name=\"fitrack\" value=\"\\*([^\"]*)\" />" // track
			+ ".*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS = Pattern.compile("" //
			+ "<td[^>]*headers=\"hafasDTL\\d+_Platform\"[^>]*>\n\\s*([^\\s\n]*?)\\s*\n</td>.*?" // departure platform
			+ "(Fu&szlig;weg<br />.*?)?" // special walk between equivs
			+ "(?:\nRichtung: ([^\n]*)\n.*?)?" // destination
			+ "<td[^>]*headers=\"hafasDTL\\d+_Platform\"[^>]*>\n\\s*([^\\s\n]*?)\\s*\n</td>" // arrival platform
	, Pattern.DOTALL);

	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile("(zu dicht beieinander|mehrfach vorhanden oder identisch)|"
			+ "(keine geeigneten Haltestellen)|(keine Verbindung gefunden)|"
			+ "(derzeit nur Ausk&uuml;nfte vom)|(zwischenzeitlich nicht mehr gespeichert)|(http-equiv=\"refresh\")", Pattern.CASE_INSENSITIVE);

	private QueryConnectionsResult queryConnections(final String firstUri, CharSequence firstPage) throws IOException
	{
		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(firstPage);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return new QueryConnectionsResult(null, QueryConnectionsResult.Status.TOO_CLOSE);
			if (mError.group(2) != null)
				return new QueryConnectionsResult(null, QueryConnectionsResult.Status.UNRESOLVABLE_ADDRESS);
			if (mError.group(3) != null)
				return new QueryConnectionsResult(null, QueryConnectionsResult.Status.NO_CONNECTIONS);
			if (mError.group(4) != null)
				return new QueryConnectionsResult(null, QueryConnectionsResult.Status.INVALID_DATE);
			if (mError.group(5) != null)
				throw new SessionExpiredException();
			if (mError.group(6) != null)
				throw new UnexpectedRedirectException();
		}

		final Matcher mAllDetailsAction = P_CONNECTIONS_ALL_DETAILS.matcher(firstPage);
		if (!mAllDetailsAction.find())
			throw new IOException("cannot find all details link in '" + firstPage + "' on " + firstUri);

		final String allDetailsUri = BASE_URL + ParserUtils.resolveEntities(mAllDetailsAction.group(1));
		final CharSequence page = ParserUtils.scrape(allDetailsUri);

		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Location from = mHead.group(1) != null ? location(mHead.group(4), mHead.group(5), mHead.group(3), mHead.group(2),
					ParserUtils.resolveEntities(mHead.group(1))) : null;
			final Location via = mHead.group(6) != null ? location(null, null, null, null, ParserUtils.resolveEntities(mHead.group(6))) : null;
			final Location to = mHead.group(7) != null ? location(mHead.group(10), mHead.group(11), mHead.group(9), mHead.group(8),
					ParserUtils.resolveEntities(mHead.group(7))) : null;
			final Calendar currentDate = new GregorianCalendar(timeZone());
			currentDate.clear();
			ParserUtils.parseGermanDate(currentDate, mHead.group(12));
			// final String linkEarlier = mHead.group(13) != null ? BVG_BASE_URL +
			// ParserUtils.resolveEntities(mHead.group(13)) : null;
			final String linkLater = mHead.group(14) != null ? BASE_URL + ParserUtils.resolveEntities(mHead.group(14)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			int iCon = 0;
			while (mConCoarse.find())
			{
				if (++iCon != Integer.parseInt(mConCoarse.group(1)))
					throw new IllegalStateException("missing connection: " + iCon);
				final String connectionSection = mConCoarse.group(2);
				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(connectionSection);
				if (mConFine.matches())
				{
					final Calendar time = new GregorianCalendar(timeZone());
					time.clear();
					ParserUtils.parseGermanDate(time, mConFine.group(1));
					Date lastTime = null;

					Date firstDepartureTime = null;
					Date lastArrivalTime = null;

					final String id = mConFine.group(2);

					final String[] trackParts = mConFine.group(3).split("\\*");

					final List<List<String[]>> tracks = new ArrayList<List<String[]>>();
					for (final String trackPart : trackParts)
					{
						final String[] partElements = trackPart.split("\\|");
						if (partElements.length != 10)
							throw new IllegalStateException("cannot parse: '" + trackPart + "'");
						final int i = Integer.parseInt(partElements[0]);
						if (i >= tracks.size())
							tracks.add(new ArrayList<String[]>());
						tracks.get(i).add(partElements);
					}

					final Matcher mDetails = P_CONNECTION_DETAILS.matcher(connectionSection);

					final List<Connection.Part> parts = new ArrayList<Connection.Part>(tracks.size());
					for (int iTrack = 0; iTrack < tracks.size(); iTrack++)
					{
						if (!mDetails.find())
							throw new IllegalStateException();

						// FIXME ugly hack, just swallow footway to equiv station
						if (mDetails.group(2) != null)
							if (!mDetails.find())
								throw new IllegalStateException();

						final List<String[]> track = tracks.get(iTrack);
						final String[] tDep = track.get(0);
						final String[] tArr = track.get(track.size() - 1);

						final Location departure = location(tDep);

						final String departurePosition = !mDetails.group(1).equals("&nbsp;") ? ParserUtils.resolveEntities(mDetails.group(1)) : null;

						if (tArr[2].equals("walk"))
						{
							ParserUtils.parseEuropeanTime(time, tDep[tDep[8].length() > 0 ? 8 : 7]);
							if (lastTime != null && time.getTime().before(lastTime))
								time.add(Calendar.DAY_OF_YEAR, 1);
							lastTime = time.getTime();
							final Date departureTime = time.getTime();
							if (firstDepartureTime == null)
								firstDepartureTime = departureTime;

							final String[] tArr2 = track.size() == 1 ? tracks.get(iTrack + 1).get(0) : tArr;

							final Location arrival = location(tArr2);

							ParserUtils.parseEuropeanTime(time, tArr2[tArr2[7].length() > 0 ? 7 : 8]);
							if (lastTime != null && time.getTime().before(lastTime))
								time.add(Calendar.DAY_OF_YEAR, 1);
							lastTime = time.getTime();
							final Date arrivalTime = time.getTime();
							lastArrivalTime = arrivalTime;

							final int mins = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);

							parts.add(new Connection.Footway(mins, departure, arrival, null));
						}
						else
						{
							ParserUtils.parseEuropeanTime(time, tDep[8]);
							if (lastTime != null && time.getTime().before(lastTime))
								time.add(Calendar.DAY_OF_YEAR, 1);
							lastTime = time.getTime();
							final Date departureTime = time.getTime();
							if (firstDepartureTime == null)
								firstDepartureTime = departureTime;

							final List<Stop> intermediateStops = new LinkedList<Stop>();
							for (final String[] tStop : track.subList(1, track.size() - 1))
							{
								ParserUtils.parseEuropeanTime(time, tStop[8]);
								if (lastTime != null && time.getTime().before(lastTime))
									time.add(Calendar.DAY_OF_YEAR, 1);
								lastTime = time.getTime();
								intermediateStops.add(new Stop(location(tStop), null, time.getTime()));
							}

							final Location arrival = location(tArr);

							ParserUtils.parseEuropeanTime(time, tArr[7]);
							if (lastTime != null && time.getTime().before(lastTime))
								time.add(Calendar.DAY_OF_YEAR, 1);
							lastTime = time.getTime();
							final Date arrivalTime = time.getTime();
							lastArrivalTime = arrivalTime;

							final String arrivalPosition = !mDetails.group(4).equals("&nbsp;") ? ParserUtils.resolveEntities(mDetails.group(4))
									: null;

							final String lineStr = normalizeLine(ParserUtils.resolveEntities(tDep[3]));
							final Line line = new Line(null, lineStr, lineColors(lineStr));

							final Location destination;
							if (mDetails.group(3) != null)
							{
								final String[] destinationPlaceAndName = splitPlaceAndName(ParserUtils.resolveEntities(mDetails.group(3)));
								destination = new Location(LocationType.ANY, 0, destinationPlaceAndName[0], destinationPlaceAndName[1]);
							}
							else
							{
								// should never happen?
								destination = null;
							}

							parts.add(new Connection.Trip(line, destination, departureTime, departurePosition, departure, arrivalTime,
									arrivalPosition, arrival, intermediateStops, null));
						}
					}

					connections.add(new Connection(id, firstUri, firstDepartureTime, lastArrivalTime, from, to, parts, null, null));
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(2) + "' on " + allDetailsUri);
				}
			}

			return new QueryConnectionsResult(new ResultHeader(SERVER_PRODUCT), firstUri, from, via, to, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private static final Pattern P_DATE_TIME = Pattern.compile("([^,]*), (.*?)");

	private static final void parseDateTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_DATE_TIME.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		ParserUtils.parseGermanDate(calendar, m.group(1));
		ParserUtils.parseEuropeanTime(calendar, m.group(2));
	}

	private static final Pattern P_LINE_REGIONAL = Pattern.compile("Zug\\s+(\\d+)");
	private static final Pattern P_LINE_TRAM = Pattern.compile("Tram?\\s+([\\dA-Z/-]+)");
	private static final Pattern P_LINE_BUS = Pattern.compile("Bus\\s+([\\dA-Z/-]+)");
	private static final Pattern P_LINE_BUS_SPECIAL = Pattern.compile("Bus([A-F]/\\d+)");
	private static final Pattern P_LINE_FERRY = Pattern.compile("F\\d+|WT");
	private static final Pattern P_LINE_NUMBER = Pattern.compile("\\d{4,}");

	@Override
	protected String normalizeLine(final String line)
	{
		final Matcher mRegional = P_LINE_REGIONAL.matcher(line);
		if (mRegional.matches())
			return "R" + mRegional.group(1);

		final Matcher mTram = P_LINE_TRAM.matcher(line);
		if (mTram.matches())
			return "T" + mTram.group(1);

		final Matcher mBus = P_LINE_BUS.matcher(line);
		if (mBus.matches())
			return "B" + mBus.group(1);

		if (P_LINE_FERRY.matcher(line).matches())
			return "F" + line;

		final Matcher mBusSpecial = P_LINE_BUS_SPECIAL.matcher(line);
		if (mBusSpecial.matches())
			return "B" + mBusSpecial.group(1);

		if (P_LINE_NUMBER.matcher(line).matches())
			return "R" + line;

		return super.normalizeLine(line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AUSFL".equals(ucType)) // Umgebung Berlin
			return 'R';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		LINES.put("SS1", new int[] { Color.rgb(221, 77, 174), Color.WHITE });
		LINES.put("SS2", new int[] { Color.rgb(16, 132, 73), Color.WHITE });
		LINES.put("SS25", new int[] { Color.rgb(16, 132, 73), Color.WHITE });
		LINES.put("SS3", new int[] { Color.rgb(22, 106, 184), Color.WHITE });
		LINES.put("SS41", new int[] { Color.rgb(162, 63, 48), Color.WHITE });
		LINES.put("SS42", new int[] { Color.rgb(191, 90, 42), Color.WHITE });
		LINES.put("SS45", new int[] { Color.WHITE, Color.rgb(191, 128, 55) });
		LINES.put("SS46", new int[] { Color.rgb(191, 128, 55), Color.WHITE });
		LINES.put("SS47", new int[] { Color.rgb(191, 128, 55), Color.WHITE });
		LINES.put("SS5", new int[] { Color.rgb(243, 103, 23), Color.WHITE });
		LINES.put("SS7", new int[] { Color.rgb(119, 96, 176), Color.WHITE });
		LINES.put("SS75", new int[] { Color.rgb(119, 96, 176), Color.WHITE });
		LINES.put("SS8", new int[] { Color.rgb(85, 184, 49), Color.WHITE });
		LINES.put("SS85", new int[] { Color.WHITE, Color.rgb(85, 184, 49) });
		LINES.put("SS9", new int[] { Color.rgb(148, 36, 64), Color.WHITE });

		LINES.put("UU1", new int[] { Color.rgb(84, 131, 47), Color.WHITE });
		LINES.put("UU2", new int[] { Color.rgb(215, 25, 16), Color.WHITE });
		LINES.put("UU3", new int[] { Color.rgb(47, 152, 154), Color.WHITE });
		LINES.put("UU4", new int[] { Color.rgb(255, 233, 42), Color.BLACK });
		LINES.put("UU5", new int[] { Color.rgb(91, 31, 16), Color.WHITE });
		LINES.put("UU55", new int[] { Color.rgb(91, 31, 16), Color.WHITE });
		LINES.put("UU6", new int[] { Color.rgb(127, 57, 115), Color.WHITE });
		LINES.put("UU7", new int[] { Color.rgb(0, 153, 204), Color.WHITE });
		LINES.put("UU8", new int[] { Color.rgb(24, 25, 83), Color.WHITE });
		LINES.put("UU9", new int[] { Color.rgb(255, 90, 34), Color.WHITE });

		LINES.put("TM1", new int[] { Color.parseColor("#eb8614"), Color.WHITE });
		LINES.put("TM2", new int[] { Color.parseColor("#68c52f"), Color.WHITE });
		LINES.put("TM4", new int[] { Color.parseColor("#cf1b22"), Color.WHITE });
		LINES.put("TM5", new int[] { Color.parseColor("#bf8037"), Color.WHITE });
		LINES.put("TM6", new int[] { Color.parseColor("#1e5ca2"), Color.WHITE });
		LINES.put("TM8", new int[] { Color.parseColor("#f46717"), Color.WHITE });
		LINES.put("TM10", new int[] { Color.parseColor("#108449"), Color.WHITE });
		LINES.put("TM13", new int[] { Color.parseColor("#36ab94"), Color.WHITE });
		LINES.put("TM17", new int[] { Color.parseColor("#a23f30"), Color.WHITE });

		LINES.put("T12", new int[] { Color.parseColor("#7d64b2"), Color.WHITE });
		LINES.put("T16", new int[] { Color.parseColor("#1e5ca2"), Color.WHITE });
		LINES.put("T18", new int[] { Color.parseColor("#f46717"), Color.WHITE });
		LINES.put("T21", new int[] { Color.parseColor("#7d64b2"), Color.WHITE });
		LINES.put("T27", new int[] { Color.parseColor("#a23f30"), Color.WHITE });
		LINES.put("T37", new int[] { Color.parseColor("#a23f30"), Color.WHITE });
		LINES.put("T50", new int[] { Color.parseColor("#36ab94"), Color.WHITE });
		LINES.put("T60", new int[] { Color.parseColor("#108449"), Color.WHITE });
		LINES.put("T61", new int[] { Color.parseColor("#108449"), Color.WHITE });
		LINES.put("T62", new int[] { Color.parseColor("#125030"), Color.WHITE });
		LINES.put("T63", new int[] { Color.parseColor("#36ab94"), Color.WHITE });
		LINES.put("T67", new int[] { Color.parseColor("#108449"), Color.WHITE });
		LINES.put("T68", new int[] { Color.parseColor("#108449"), Color.WHITE });

		LINES.put("FF1", new int[] { Color.BLUE, Color.WHITE }); // Potsdam
		LINES.put("FF10", new int[] { Color.BLUE, Color.WHITE });
		LINES.put("FF11", new int[] { Color.BLUE, Color.WHITE });
		LINES.put("FF12", new int[] { Color.BLUE, Color.WHITE });
		LINES.put("FF21", new int[] { Color.BLUE, Color.WHITE });
		LINES.put("FF23", new int[] { Color.BLUE, Color.WHITE });
		LINES.put("FF24", new int[] { Color.BLUE, Color.WHITE });

		// Regional lines Brandenburg:
		LINES.put("RRE1", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
		LINES.put("RRE2", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("RRE3", new int[] { Color.parseColor("#F57921"), Color.WHITE });
		LINES.put("RRE4", new int[] { Color.parseColor("#952D4F"), Color.WHITE });
		LINES.put("RRE5", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
		LINES.put("RRE6", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
		LINES.put("RRE7", new int[] { Color.parseColor("#00854A"), Color.WHITE });
		LINES.put("RRE10", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
		LINES.put("RRE11", new int[] { Color.parseColor("#059EDB"), Color.WHITE });
		LINES.put("RRE11", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
		LINES.put("RRE15", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("RRE18", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
		LINES.put("RRB10", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
		LINES.put("RRB12", new int[] { Color.parseColor("#A3238E"), Color.WHITE });
		LINES.put("RRB13", new int[] { Color.parseColor("#F68B1F"), Color.WHITE });
		LINES.put("RRB13", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
		LINES.put("RRB14", new int[] { Color.parseColor("#A3238E"), Color.WHITE });
		LINES.put("RRB20", new int[] { Color.parseColor("#00854A"), Color.WHITE });
		LINES.put("RRB21", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
		LINES.put("RRB22", new int[] { Color.parseColor("#0087CB"), Color.WHITE });
		LINES.put("ROE25", new int[] { Color.parseColor("#0087CB"), Color.WHITE });
		LINES.put("RNE26", new int[] { Color.parseColor("#00A896"), Color.WHITE });
		LINES.put("RNE27", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
		LINES.put("RRB30", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
		LINES.put("RRB31", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
		LINES.put("RMR33", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
		LINES.put("ROE35", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
		LINES.put("ROE36", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
		LINES.put("RRB43", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
		LINES.put("RRB45", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("ROE46", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
		LINES.put("RMR51", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
		LINES.put("RRB51", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
		LINES.put("RRB54", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("RRB55", new int[] { Color.parseColor("#F57921"), Color.WHITE });
		LINES.put("ROE60", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
		LINES.put("ROE63", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("ROE65", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
		LINES.put("RRB66", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
		LINES.put("RPE70", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
		LINES.put("RPE73", new int[] { Color.parseColor("#00A896"), Color.WHITE });
		LINES.put("RPE74", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
		LINES.put("T89", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
		LINES.put("RRB91", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
		LINES.put("RRB93", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
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

	@Override
	public Point[] getArea()
	{
		return Berlin.BOUNDARY;
	}
}
