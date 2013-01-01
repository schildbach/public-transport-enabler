/*
 * Copyright 2010-2013 the original author or authors.
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Line.Attr;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryConnectionsContext;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.exception.UnexpectedRedirectException;
import de.schildbach.pte.geo.Berlin;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public final class BvgProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.BVG;
	private static final String API_BASE = "http://www.fahrinfo-berlin.de/Fahrinfo/bin/";
	private static final String DEPARTURE_URL = "http://mobil.bvg.de";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

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
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'S';
		if (value == 2)
			return 'U';
		if (value == 4)
			return 'T';
		if (value == 8)
			return 'B';
		if (value == 16)
			return 'F';
		if (value == 32)
			return 'I';
		if (value == 64)
			return 'R';
		if (value == 128)
			return 'P';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == 'R')
		{
			productBits.setCharAt(6, '1');
		}
		else if (product == 'S')
		{
			productBits.setCharAt(0, '1');
		}
		else if (product == 'U')
		{
			productBits.setCharAt(1, '1');
		}
		else if (product == 'T')
		{
			productBits.setCharAt(2, '1');
		}
		else if (product == 'B')
		{
			productBits.setCharAt(3, '1');
		}
		else if (product == 'P')
		{
			productBits.setCharAt(7, '1');
		}
		else if (product == 'F')
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
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
				final String[] parsedPlaceAndName = splitPlaceAndName(ParserUtils.urlDecode(mOwn.group(4), ISO_8859_1));
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

	private static final String DEPARTURE_URL_LIVE = DEPARTURE_URL + "/IstAbfahrtzeiten/index/mobil?";

	private String departuresQueryLiveUri(final int stationId)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(DEPARTURE_URL_LIVE);
		uri.append("input=").append(stationId);
		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);
		return uri.toString();
	}

	private static final String DEPARTURE_URL_PLAN = DEPARTURE_URL + "/Fahrinfo/bin/stboard.bin/dox?boardType=dep&disableEquivs=yes&start=yes";

	private String departuresQueryPlanUri(final int stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(DEPARTURE_URL_PLAN);
		uri.append("&input=").append(stationId);
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
	private static final Pattern P_DEPARTURES_LIVE_ERRORS = Pattern.compile(
			"(Haltestelle:)|(Wartungsgr&uuml;nden|nur eingeschränkt)|(http-equiv=\"refresh\")", Pattern.CASE_INSENSITIVE);

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
						final String lineName = ParserUtils.resolveEntities(mMsgsFine.group(1));
						final char linePproduct = normalizeType(categoryFromName(lineName));
						final Line line = newLine(linePproduct, normalizeLineName(lineName));

						final String message = ParserUtils.resolveEntities(mMsgsFine.group(3)).replace('\n', ' ');
						messages.put(line.label, message);
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

						final String lineName = ParserUtils.resolveEntities(mDepFine.group(3));
						final char lineProduct = normalizeType(categoryFromName(lineName));
						final Line line = newLine(lineProduct, normalizeLineName(lineName));

						final String position = null;

						final String[] destinationPlaceAndName = splitPlaceAndName(ParserUtils.resolveEntities(mDepFine.group(4)));
						final Location destination = new Location(LocationType.ANY, 0, destinationPlaceAndName[0], destinationPlaceAndName[1]);

						final String message = messages.get(line.label);

						final Departure dep = new Departure(plannedTime, predictedTime, line, position, destination, null, message);
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

						final String lineName = ParserUtils.resolveEntities(mDepFine.group(2));
						final char lineProduct = normalizeType(categoryFromName(lineName));
						final Line line = newLine(lineProduct, normalizeLineName(lineName));

						final String position = ParserUtils.resolveEntities(mDepFine.group(3));

						final int destinationId = Integer.parseInt(mDepFine.group(4));
						final String[] destinationPlaceAndName = splitPlaceAndName(ParserUtils.resolveEntities(mDepFine.group(5)));
						final Location destination = new Location(destinationId > 0 ? LocationType.STATION : LocationType.ANY, destinationId,
								destinationPlaceAndName[0], destinationPlaceAndName[1]);

						final Departure dep = new Departure(plannedTime, null, line, position, destination, null, null);
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

	private static final Pattern P_DATE_TIME = Pattern.compile("([^,]*), (.*?)");

	private static final void parseDateTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_DATE_TIME.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		ParserUtils.parseGermanDate(calendar, m.group(1));
		ParserUtils.parseEuropeanTime(calendar, m.group(2));
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.bin/dny?tpl=suggest2json&REQ0JourneyStopsS0A=255&S=%s?&js=true&";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ISO_8859_1));

		return jsonGetStops(uri);
	}

	@Override
	protected boolean isValidStationId(int id)
	{
		return id >= 1000000;
	}

	@Override
	protected void appendCustomConnectionsQueryBinaryUri(final StringBuilder uri)
	{
		uri.append("&h2g-direct=11");
		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);
	}

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int maxNumConnections, final String products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options) throws IOException
	{
		return queryConnectionsBinary(from, via, to, date, dep, maxNumConnections, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final QueryConnectionsContext contextObj, final boolean later, final int numConnections)
			throws IOException
	{
		return queryMoreConnectionsBinary(contextObj, later, numConnections);
	}

	private static final Pattern P_NORMALIZE_LINE_NAME_TRAM = Pattern.compile("(?:tra|tram)\\s+(.*)", Pattern.CASE_INSENSITIVE);

	@Override
	protected String normalizeLineName(final String lineName)
	{
		final Matcher mTram = P_NORMALIZE_LINE_NAME_TRAM.matcher(lineName);
		if (mTram.matches())
			return mTram.group(1);

		return super.normalizeLineName(lineName);
	}

	@Override
	protected Line newLine(final char product, final String normalizedName, final Attr... attrs)
	{
		if (product == 'S' && "S41".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.CIRCLE_CLOCKWISE));
		if (product == 'S' && "S42".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.CIRCLE_ANTICLOCKWISE));

		if (product == 'B' && "S41".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.SERVICE_REPLACEMENT, Attr.CIRCLE_CLOCKWISE));
		if (product == 'B' && "S42".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.SERVICE_REPLACEMENT, Attr.CIRCLE_ANTICLOCKWISE));

		if (product == 'B' && "TXL".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.LINE_AIRPORT));
		if (product == 'S' && "S9".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.LINE_AIRPORT));
		if (product == 'S' && "S45".equals(normalizedName))
			return super.newLine(product, normalizedName, concatAttrs(attrs, Attr.LINE_AIRPORT));

		return super.newLine(product, normalizedName, attrs);
	}

	private Attr[] concatAttrs(final Attr[] attrs1, final Attr... attrs2)
	{
		final int attrs1length = attrs1.length;
		final int attrs2length = attrs2.length;

		final Attr[] newAttrs = new Attr[attrs1length + attrs2length];
		for (int i = 0; i < attrs1length; i++)
			newAttrs[i] = attrs1[i];
		for (int i = 0; i < attrs2length; i++)
			newAttrs[attrs1length + i] = attrs2[i];

		return newAttrs;
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AUSFL".equals(ucType)) // Umgebung Berlin
			return 'R';

		if ("F".equals(ucType))
			return 'F';
		if ("WT".equals(ucType)) // Wassertaxi
			return 'F';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		LINES.put("SS1", new Style(Style.rgb(221, 77, 174), Style.WHITE));
		LINES.put("SS2", new Style(Style.rgb(16, 132, 73), Style.WHITE));
		LINES.put("SS25", new Style(Style.rgb(16, 132, 73), Style.WHITE));
		LINES.put("SS3", new Style(Style.rgb(22, 106, 184), Style.WHITE));
		LINES.put("SS41", new Style(Style.rgb(162, 63, 48), Style.WHITE));
		LINES.put("SS42", new Style(Style.rgb(191, 90, 42), Style.WHITE));
		LINES.put("SS45", new Style(Style.WHITE, Style.rgb(191, 128, 55)));
		LINES.put("SS46", new Style(Style.rgb(191, 128, 55), Style.WHITE));
		LINES.put("SS47", new Style(Style.rgb(191, 128, 55), Style.WHITE));
		LINES.put("SS5", new Style(Style.rgb(243, 103, 23), Style.WHITE));
		LINES.put("SS7", new Style(Style.rgb(119, 96, 176), Style.WHITE));
		LINES.put("SS75", new Style(Style.rgb(119, 96, 176), Style.WHITE));
		LINES.put("SS8", new Style(Style.rgb(85, 184, 49), Style.WHITE));
		LINES.put("SS85", new Style(Style.WHITE, Style.rgb(85, 184, 49)));
		LINES.put("SS9", new Style(Style.rgb(148, 36, 64), Style.WHITE));

		LINES.put("UU1", new Style(Shape.RECT, Style.rgb(84, 131, 47), Style.WHITE));
		LINES.put("UU2", new Style(Shape.RECT, Style.rgb(215, 25, 16), Style.WHITE));
		LINES.put("UU3", new Style(Shape.RECT, Style.rgb(47, 152, 154), Style.WHITE));
		LINES.put("UU4", new Style(Shape.RECT, Style.rgb(255, 233, 42), Style.BLACK));
		LINES.put("UU5", new Style(Shape.RECT, Style.rgb(91, 31, 16), Style.WHITE));
		LINES.put("UU55", new Style(Shape.RECT, Style.rgb(91, 31, 16), Style.WHITE));
		LINES.put("UU6", new Style(Shape.RECT, Style.rgb(127, 57, 115), Style.WHITE));
		LINES.put("UU7", new Style(Shape.RECT, Style.rgb(0, 153, 204), Style.WHITE));
		LINES.put("UU8", new Style(Shape.RECT, Style.rgb(24, 25, 83), Style.WHITE));
		LINES.put("UU9", new Style(Shape.RECT, Style.rgb(255, 90, 34), Style.WHITE));

		LINES.put("TM1", new Style(Shape.RECT, Style.parseColor("#eb8614"), Style.WHITE));
		LINES.put("TM2", new Style(Shape.RECT, Style.parseColor("#68c52f"), Style.WHITE));
		LINES.put("TM4", new Style(Shape.RECT, Style.parseColor("#cf1b22"), Style.WHITE));
		LINES.put("TM5", new Style(Shape.RECT, Style.parseColor("#bf8037"), Style.WHITE));
		LINES.put("TM6", new Style(Shape.RECT, Style.parseColor("#1e5ca2"), Style.WHITE));
		LINES.put("TM8", new Style(Shape.RECT, Style.parseColor("#f46717"), Style.WHITE));
		LINES.put("TM10", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		LINES.put("TM13", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		LINES.put("TM17", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));

		LINES.put("T12", new Style(Shape.RECT, Style.parseColor("#7d64b2"), Style.WHITE));
		LINES.put("T16", new Style(Shape.RECT, Style.parseColor("#1e5ca2"), Style.WHITE));
		LINES.put("T18", new Style(Shape.RECT, Style.parseColor("#f46717"), Style.WHITE));
		LINES.put("T21", new Style(Shape.RECT, Style.parseColor("#7d64b2"), Style.WHITE));
		LINES.put("T27", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));
		LINES.put("T37", new Style(Shape.RECT, Style.parseColor("#a23f30"), Style.WHITE));
		LINES.put("T50", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		LINES.put("T60", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		LINES.put("T61", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		LINES.put("T62", new Style(Shape.RECT, Style.parseColor("#125030"), Style.WHITE));
		LINES.put("T63", new Style(Shape.RECT, Style.parseColor("#36ab94"), Style.WHITE));
		LINES.put("T67", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));
		LINES.put("T68", new Style(Shape.RECT, Style.parseColor("#108449"), Style.WHITE));

		LINES.put("FF1", new Style(Style.BLUE, Style.WHITE)); // Potsdam
		LINES.put("FF10", new Style(Style.BLUE, Style.WHITE));
		LINES.put("FF11", new Style(Style.BLUE, Style.WHITE));
		LINES.put("FF12", new Style(Style.BLUE, Style.WHITE));
		LINES.put("FF21", new Style(Style.BLUE, Style.WHITE));
		LINES.put("FF23", new Style(Style.BLUE, Style.WHITE));
		LINES.put("FF24", new Style(Style.BLUE, Style.WHITE));

		// Regional lines Brandenburg:
		LINES.put("RRE1", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		LINES.put("RRE2", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("RRE3", new Style(Shape.RECT, Style.parseColor("#F57921"), Style.WHITE));
		LINES.put("RRE4", new Style(Shape.RECT, Style.parseColor("#952D4F"), Style.WHITE));
		LINES.put("RRE5", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		LINES.put("RRE6", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		LINES.put("RRE7", new Style(Shape.RECT, Style.parseColor("#00854A"), Style.WHITE));
		LINES.put("RRE10", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		LINES.put("RRE11", new Style(Shape.RECT, Style.parseColor("#059EDB"), Style.WHITE));
		LINES.put("RRE11", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		LINES.put("RRE15", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("RRE18", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		LINES.put("RRB10", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		LINES.put("RRB12", new Style(Shape.RECT, Style.parseColor("#A3238E"), Style.WHITE));
		LINES.put("RRB13", new Style(Shape.RECT, Style.parseColor("#F68B1F"), Style.WHITE));
		LINES.put("RRB13", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		LINES.put("RRB14", new Style(Shape.RECT, Style.parseColor("#A3238E"), Style.WHITE));
		LINES.put("RRB20", new Style(Shape.RECT, Style.parseColor("#00854A"), Style.WHITE));
		LINES.put("RRB21", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		LINES.put("RRB22", new Style(Shape.RECT, Style.parseColor("#0087CB"), Style.WHITE));
		LINES.put("ROE25", new Style(Shape.RECT, Style.parseColor("#0087CB"), Style.WHITE));
		LINES.put("RNE26", new Style(Shape.RECT, Style.parseColor("#00A896"), Style.WHITE));
		LINES.put("RNE27", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		LINES.put("RRB30", new Style(Shape.RECT, Style.parseColor("#00A65E"), Style.WHITE));
		LINES.put("RRB31", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		LINES.put("RMR33", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		LINES.put("ROE35", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		LINES.put("ROE36", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		LINES.put("RRB43", new Style(Shape.RECT, Style.parseColor("#5E6DB3"), Style.WHITE));
		LINES.put("RRB45", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("ROE46", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		LINES.put("RMR51", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		LINES.put("RRB51", new Style(Shape.RECT, Style.parseColor("#DB6EAB"), Style.WHITE));
		LINES.put("RRB54", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("RRB55", new Style(Shape.RECT, Style.parseColor("#F57921"), Style.WHITE));
		LINES.put("ROE60", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		LINES.put("ROE63", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("ROE65", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		LINES.put("RRB66", new Style(Shape.RECT, Style.parseColor("#60BB46"), Style.WHITE));
		LINES.put("RPE70", new Style(Shape.RECT, Style.parseColor("#FFD403"), Style.BLACK));
		LINES.put("RPE73", new Style(Shape.RECT, Style.parseColor("#00A896"), Style.WHITE));
		LINES.put("RPE74", new Style(Shape.RECT, Style.parseColor("#0072BC"), Style.WHITE));
		LINES.put("T89", new Style(Shape.RECT, Style.parseColor("#EE1C23"), Style.WHITE));
		LINES.put("RRB91", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
		LINES.put("RRB93", new Style(Shape.RECT, Style.parseColor("#A7653F"), Style.WHITE));
	}

	private static final Style STYLE_BUS_NIGHT = new Style(Shape.RECT, Style.BLACK, Style.WHITE);
	private static final Style STYLE_BUS_DAY = new Style(Shape.RECT, Style.parseColor("#993399"), Style.WHITE);

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;

		if (line.startsWith("BN"))
			return STYLE_BUS_NIGHT;
		if (line.startsWith("B"))
			return STYLE_BUS_DAY;

		return super.lineStyle(line);
	}

	@Override
	public Point[] getArea()
	{
		return Berlin.BOUNDARY;
	}
}
