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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Line.Attr;
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
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.HttpClient;
import de.schildbach.pte.util.LittleEndianDataInputStream;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.StringReplaceReader;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider extends AbstractNetworkProvider
{
	protected final static String SERVER_PRODUCT = "hafas";
	private static final String REQC_PROD = "hafas";
	protected static final int DEFAULT_MAX_DEPARTURES = 100;
	protected static final int DEFAULT_MAX_LOCATIONS = 50;

	protected String stationBoardEndpoint;
	protected String getStopEndpoint;
	protected String queryEndpoint;
	protected final String mgateEndpoint;
	private @Nullable String extXmlEndpoint = null;
	private Product[] productsMap;
	private @Nullable String accessId = null;
	private @Nullable String clientType = "ANDROID";
	private @Nullable String jsonApiVersion;
	private @Nullable String jsonApiAuthorization;
	private @Nullable String jsonApiClient;
	private Charset jsonGetStopsEncoding = Charsets.ISO_8859_1;
	private boolean jsonGetStopsUseWeight = true;
	private Charset jsonNearbyLocationsEncoding = Charsets.ISO_8859_1;
	private boolean dominantPlanStopTime = false;
	private boolean useIso8601 = false;
	private boolean stationBoardHasStationTable = true;
	private boolean stationBoardHasLocation = false;
	private boolean stationBoardCanDoEquivs = true;

	private static final Logger log = LoggerFactory.getLogger(AbstractHafasProvider.class);

	@SuppressWarnings("serial")
	private static class Context implements QueryTripsContext
	{
		public final String laterContext;
		public final String earlierContext;
		public final int sequence;

		public Context(final String laterContext, final String earlierContext, final int sequence)
		{
			this.laterContext = laterContext;
			this.earlierContext = earlierContext;
			this.sequence = sequence;
		}

		public boolean canQueryLater()
		{
			return laterContext != null;
		}

		public boolean canQueryEarlier()
		{
			return earlierContext != null;
		}
	}

	@SuppressWarnings("serial")
	public static class JsonContext implements QueryTripsContext
	{
		public final Location from, to;
		public final Date date;
		public final boolean dep;
		public final Set<Product> products;
		public final String laterContext, earlierContext;

		public JsonContext(final Location from, final Location to, final Date date, final boolean dep, final Set<Product> products,
				final String laterContext, final String earlierContext)
		{
			this.from = from;
			this.to = to;
			this.date = date;
			this.dep = dep;
			this.products = products;
			this.laterContext = laterContext;
			this.earlierContext = earlierContext;
		}

		public boolean canQueryLater()
		{
			return laterContext != null;
		}

		public boolean canQueryEarlier()
		{
			return earlierContext != null;
		}
	}

	@SuppressWarnings("serial")
	public static class QueryTripsBinaryContext implements QueryTripsContext
	{
		public final String ident;
		public final int seqNr;
		public final String ld;
		public final int usedBufferSize;
		private final boolean canQueryMore;

		public QueryTripsBinaryContext(final String ident, final int seqNr, final String ld, final int usedBufferSize, final boolean canQueryMore)
		{
			this.ident = ident;
			this.seqNr = seqNr;
			this.ld = ld;
			this.usedBufferSize = usedBufferSize;
			this.canQueryMore = canQueryMore;
		}

		public boolean canQueryLater()
		{
			return canQueryMore;
		}

		public boolean canQueryEarlier()
		{
			return canQueryMore;
		}
	}

	public AbstractHafasProvider(final NetworkId network, final String apiBase, final String apiLanguage, final Product[] productsMap)
	{
		super(network);

		this.stationBoardEndpoint = apiBase + "stboard.exe/" + apiLanguage;
		this.getStopEndpoint = apiBase + "ajax-getstop.exe/" + apiLanguage;
		this.queryEndpoint = apiBase + "query.exe/" + apiLanguage;
		this.mgateEndpoint = apiBase + "mgate.exe";
		this.productsMap = productsMap;
	}

	protected void setStationBoardEndpoint(final String stationBoardEndpoint)
	{
		this.stationBoardEndpoint = stationBoardEndpoint;
	}

	protected void setGetStopEndpoint(final String getStopEndpoint)
	{
		this.getStopEndpoint = getStopEndpoint;
	}

	protected void setQueryEndpoint(final String queryEndpoint)
	{
		this.queryEndpoint = queryEndpoint;
	}

	protected void setExtXmlEndpoint(final String extXmlEndpoint)
	{
		this.extXmlEndpoint = extXmlEndpoint;
	}

	protected void setAccessId(final String accessId)
	{
		this.accessId = accessId;
	}

	protected void setClientType(final String clientType)
	{
		this.clientType = clientType;
	}

	protected void setJsonApiVersion(final String jsonApiVersion)
	{
		this.jsonApiVersion = jsonApiVersion;
	}

	protected void setJsonApiAuthorization(final String jsonApiAuthorization)
	{
		this.jsonApiAuthorization = jsonApiAuthorization;
	}

	protected void setJsonApiClient(final String jsonApiClient)
	{
		this.jsonApiClient = jsonApiClient;
	}

	protected void setDominantPlanStopTime(final boolean dominantPlanStopTime)
	{
		this.dominantPlanStopTime = dominantPlanStopTime;
	}

	protected void setJsonGetStopsEncoding(final Charset jsonGetStopsEncoding)
	{
		this.jsonGetStopsEncoding = jsonGetStopsEncoding;
	}

	protected void setJsonGetStopsUseWeight(final boolean jsonGetStopsUseWeight)
	{
		this.jsonGetStopsUseWeight = jsonGetStopsUseWeight;
	}

	protected void setJsonNearbyLocationsEncoding(final Charset jsonNearbyLocationsEncoding)
	{
		this.jsonNearbyLocationsEncoding = jsonNearbyLocationsEncoding;
	}

	protected void setUseIso8601(final boolean useIso8601)
	{
		this.useIso8601 = useIso8601;
	}

	protected void setStationBoardHasStationTable(final boolean stationBoardHasStationTable)
	{
		this.stationBoardHasStationTable = stationBoardHasStationTable;
	}

	protected void setStationBoardHasLocation(final boolean stationBoardHasLocation)
	{
		this.stationBoardHasLocation = stationBoardHasLocation;
	}

	protected void setStationBoardCanDoEquivs(final boolean canDoEquivs)
	{
		this.stationBoardCanDoEquivs = canDoEquivs;
	}

	@Override
	protected boolean hasCapability(final Capability capability)
	{
		return true;
	}

	protected final CharSequence productsString(final Set<Product> products)
	{
		final StringBuilder productsStr = new StringBuilder(productsMap.length);
		for (int i = 0; i < productsMap.length; i++)
		{
			if (productsMap[i] != null && products.contains(productsMap[i]))
				productsStr.append('1');
			else
				productsStr.append('0');
		}
		return productsStr;
	}

	protected final CharSequence allProductsString()
	{
		final StringBuilder productsStr = new StringBuilder(productsMap.length);
		for (int i = 0; i < productsMap.length; i++)
			productsStr.append('1');
		return productsStr;
	}

	protected final int allProductsInt()
	{
		return (1 << productsMap.length) - 1;
	}

	protected final Product intToProduct(final int productInt)
	{
		final int allProductsInt = allProductsInt();
		checkArgument(productInt < allProductsInt, "value " + productInt + " must be smaller than " + allProductsInt);

		int value = productInt;
		Product product = null;
		for (int i = productsMap.length - 1; i >= 0; i--)
		{
			final int v = 1 << i;
			if (value >= v)
			{
				if (product != null)
					throw new IllegalArgumentException("ambigous value: " + productInt);
				product = productsMap[i];
				value -= v;
			}
		}
		checkState(value == 0);
		return product;
	}

	protected final Set<Product> intToProducts(int value)
	{
		final int allProductsInt = allProductsInt();
		checkArgument(value <= allProductsInt, "value " + value + " cannot be greater than " + allProductsInt);

		final Set<Product> products = EnumSet.noneOf(Product.class);
		for (int i = productsMap.length - 1; i >= 0; i--)
		{
			final int v = 1 << i;
			if (value >= v)
			{
				final Product product = checkNotNull(productsMap[i], "unknown product " + i);
				products.add(product);
				value -= v;
			}
		}
		checkState(value == 0);
		return products;
	}

	protected static final Pattern P_SPLIT_NAME_FIRST_COMMA = Pattern.compile("([^,]*), (.*)");
	protected static final Pattern P_SPLIT_NAME_LAST_COMMA = Pattern.compile("(.*), ([^,]*)");
	protected static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{3,}?)\\)");

	protected String[] splitStationName(final String name)
	{
		return new String[] { null, name };
	}

	protected String[] splitPOI(final String poi)
	{
		return new String[] { null, poi };
	}

	protected String[] splitAddress(final String address)
	{
		return new String[] { null, address };
	}

	private final String wrapReqC(final CharSequence request, final Charset encoding)
	{
		return "<?xml version=\"1.0\" encoding=\"" + (encoding != null ? encoding.name() : "iso-8859-1") + "\"?>" //
				+ "<ReqC ver=\"1.1\" prod=\"" + REQC_PROD + "\" lang=\"DE\"" + (accessId != null ? " accessId=\"" + accessId + "\"" : "") + ">" //
				+ request //
				+ "</ReqC>";
	}

	private final Location parseStation(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Station".equals(type))
		{
			final String name = XmlPullUtil.attr(pp, "name");
			final String id = XmlPullUtil.attr(pp, "externalStationNr");
			final int x = XmlPullUtil.intAttr(pp, "x");
			final int y = XmlPullUtil.intAttr(pp, "y");

			final String[] placeAndName = splitStationName(name);
			return new Location(LocationType.STATION, id, y, x, placeAndName[0], placeAndName[1]);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private static final Location parsePoi(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Poi".equals(type))
		{
			String name = XmlPullUtil.attr(pp, "name");
			if (name.equals("unknown"))
				name = null;
			final int x = XmlPullUtil.intAttr(pp, "x");
			final int y = XmlPullUtil.intAttr(pp, "y");
			return new Location(LocationType.POI, null, y, x, null, name);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private final Location parseAddress(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Address".equals(type))
		{
			String name = XmlPullUtil.attr(pp, "name");
			if (name.equals("unknown"))
				name = null;
			final int x = XmlPullUtil.intAttr(pp, "x");
			final int y = XmlPullUtil.intAttr(pp, "y");

			final String[] placeAndName = splitAddress(name);
			return new Location(LocationType.ADDRESS, null, y, x, placeAndName[0], placeAndName[1]);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private final Position parsePlatform(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "Platform");
		final String platformText = XmlPullUtil.valueTag(pp, "Text");
		XmlPullUtil.skipExit(pp, "Platform");

		if (platformText == null || platformText.length() == 0)
			return null;
		else
			return parsePosition(platformText);
	}

	public SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException
	{
		final StringBuilder uri = new StringBuilder(getStopEndpoint);
		appendJsonGetStopsParameters(uri, checkNotNull(constraint), 0);

		return jsonGetStops(uri.toString());
	}

	protected void appendJsonGetStopsParameters(final StringBuilder uri, final CharSequence constraint, final int maxStops)
	{
		uri.append("?getstop=1");
		uri.append("&REQ0JourneyStopsS0A=255");
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(constraint.toString(), jsonGetStopsEncoding)).append("?");
		if (maxStops > 0)
			uri.append("&REQ0JourneyStopsB=").append(maxStops);
		uri.append("&js=true");
	}

	private static final Pattern P_AJAX_GET_STOPS_JSON = Pattern.compile("SLs\\.sls\\s*=\\s*(.*?);\\s*SLs\\.showSuggestion\\(\\);", Pattern.DOTALL);
	private static final Pattern P_AJAX_GET_STOPS_ID = Pattern.compile(".*?@L=0*(\\d+)@.*?");

	protected final SuggestLocationsResult jsonGetStops(final String uri) throws IOException
	{
		final CharSequence page = httpClient.get(uri, jsonGetStopsEncoding);

		final Matcher mJson = P_AJAX_GET_STOPS_JSON.matcher(page);
		if (mJson.matches())
		{
			final String json = mJson.group(1);
			final List<SuggestedLocation> locations = new ArrayList<SuggestedLocation>();

			try
			{
				final JSONObject head = new JSONObject(json);
				final JSONArray aSuggestions = head.getJSONArray("suggestions");

				for (int i = 0; i < aSuggestions.length(); i++)
				{
					final JSONObject suggestion = aSuggestions.optJSONObject(i);
					if (suggestion != null)
					{
						final int type = suggestion.getInt("type");
						final String value = suggestion.getString("value");
						final int lat = suggestion.optInt("ycoord");
						final int lon = suggestion.optInt("xcoord");
						final int weight = jsonGetStopsUseWeight ? suggestion.getInt("weight") : -i;
						String localId = null;
						final Matcher m = P_AJAX_GET_STOPS_ID.matcher(suggestion.getString("id"));
						if (m.matches())
							localId = m.group(1);

						final Location location;

						if (type == 1) // station
						{
							final String[] placeAndName = splitStationName(value);
							location = new Location(LocationType.STATION, localId, lat, lon, placeAndName[0], placeAndName[1]);
						}
						else if (type == 2) // address
						{
							final String[] placeAndName = splitAddress(value);
							location = new Location(LocationType.ADDRESS, null, lat, lon, placeAndName[0], placeAndName[1]);
						}
						else if (type == 4) // poi
						{
							final String[] placeAndName = splitPOI(value);
							location = new Location(LocationType.POI, localId, lat, lon, placeAndName[0], placeAndName[1]);
						}
						else if (type == 128) // crossing
						{
							final String[] placeAndName = splitAddress(value);
							location = new Location(LocationType.ADDRESS, localId, lat, lon, placeAndName[0], placeAndName[1]);
						}
						else if (type == 87)
						{
							location = null;
							// don't know what to do
						}
						else
						{
							throw new IllegalStateException("unknown type " + type + " on " + uri);
						}

						if (location != null)
						{
							final SuggestedLocation suggestedLocation = new SuggestedLocation(location, weight);
							locations.add(suggestedLocation);
						}
					}
				}

				return new SuggestLocationsResult(new ResultHeader(network, SERVER_PRODUCT), locations);
			}
			catch (final JSONException x)
			{
				throw new RuntimeException("cannot parse: '" + json + "' on " + uri, x);
			}
		}
		else
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri);
		}
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time, final int maxDepartures, final boolean equivs)
			throws IOException
	{
		checkNotNull(Strings.emptyToNull(stationId));

		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		appendXmlStationBoardParameters(uri, time, stationId, maxDepartures, equivs, "vs_java3");

		return xmlStationBoard(uri.toString(), stationId);
	}

	protected void appendXmlStationBoardParameters(final StringBuilder uri, final @Nullable Date time, final String stationId,
			final int maxDepartures, final boolean equivs, final @Nullable String styleSheet)
	{
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		if (stationBoardCanDoEquivs)
			uri.append("&disableEquivs=").append(equivs ? "0" : "1");
		uri.append("&maxJourneys=").append(maxDepartures > 0 ? maxDepartures : DEFAULT_MAX_DEPARTURES);
		uri.append("&input=").append(normalizeStationId(stationId));
		appendDateTimeParameters(uri, time, "date", "time");
		if (clientType != null)
			uri.append("&clientType=").append(ParserUtils.urlEncode(clientType));
		if (styleSheet != null)
			uri.append("&L=").append(styleSheet);
		uri.append("&hcount=0"); // prevents showing old departures
		uri.append("&start=yes");
	}

	protected void appendDateTimeParameters(final StringBuilder uri, final Date time, final String dateParamName, final String timeParamName)
	{
		final Calendar c = new GregorianCalendar(timeZone);
		c.setTime(time);
		final int year = c.get(Calendar.YEAR);
		final int month = c.get(Calendar.MONTH) + 1;
		final int day = c.get(Calendar.DAY_OF_MONTH);
		final int hour = c.get(Calendar.HOUR_OF_DAY);
		final int minute = c.get(Calendar.MINUTE);
		uri.append('&').append(dateParamName).append('=');
		uri.append(ParserUtils.urlEncode(useIso8601 ? String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month, day) : String.format(
				Locale.ENGLISH, "%02d.%02d.%02d", day, month, year - 2000)));
		uri.append('&').append(timeParamName).append('=');
		uri.append(ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%02d:%02d", hour, minute)));
	}

	private static final Pattern P_XML_STATION_BOARD_DELAY = Pattern.compile("(?:-|k\\.A\\.?|cancel|\\+?\\s*(\\d+))");

	protected final QueryDeparturesResult xmlStationBoard(final String uri, final String stationId) throws IOException
	{
		final String normalizedStationId = normalizeStationId(stationId);

		StringReplaceReader reader = null;
		String firstChars = null;

		try
		{
			// work around unparsable XML
			final InputStream is = httpClient.getInputStream(uri);
			firstChars = HttpClient.peekFirstChars(is);
			reader = new StringReplaceReader(new InputStreamReader(is, Charsets.ISO_8859_1), " & ", " &amp; ");
			reader.replace("<b>", " ");
			reader.replace("</b>", " ");
			reader.replace("<u>", " ");
			reader.replace("</u>", " ");
			reader.replace("<i>", " ");
			reader.replace("</i>", " ");
			reader.replace("<br />", " ");
			reader.replace(" ->", " &#x2192;"); // right arrow
			reader.replace(" <-", " &#x2190;"); // left arrow
			reader.replace(" <> ", " &#x2194; "); // left-right arrow
			addCustomReplaces(reader);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(reader);

			pp.nextTag();

			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				final String text = XmlPullUtil.attr(pp, "text");

				if (code.equals("H730")) // Your input is not valid
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
				if (code.equals("H890"))
				{
					result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, normalizedStationId), Collections
							.<Departure> emptyList(), null));
					return result;
				}
				throw new IllegalArgumentException("unknown error " + code + ", " + text);
			}

			String[] stationPlaceAndName = null;

			if (stationBoardHasStationTable)
				XmlPullUtil.enter(pp, "StationTable");
			else
				checkState(!XmlPullUtil.test(pp, "StationTable"));

			if (stationBoardHasLocation)
			{
				XmlPullUtil.require(pp, "St");

				final String evaId = XmlPullUtil.attr(pp, "evaId");
				if (evaId != null)
				{
					if (!evaId.equals(normalizedStationId))
						throw new IllegalStateException("stationId: " + normalizedStationId + ", evaId: " + evaId);

					final String name = XmlPullUtil.attr(pp, "name");
					if (name != null)
						stationPlaceAndName = splitStationName(name.trim());
				}
				XmlPullUtil.requireSkip(pp, "St");
			}
			else
			{
				checkState(!XmlPullUtil.test(pp, "St"));
			}

			while (XmlPullUtil.test(pp, "Journey"))
			{
				final String fpTime = XmlPullUtil.attr(pp, "fpTime");
				final String fpDate = XmlPullUtil.attr(pp, "fpDate");
				final String delay = XmlPullUtil.attr(pp, "delay");
				final String eDelay = XmlPullUtil.optAttr(pp, "e_delay", null);
				final String platform = XmlPullUtil.optAttr(pp, "platform", null);
				// TODO newpl
				final String targetLoc = XmlPullUtil.optAttr(pp, "targetLoc", null);
				// TODO hafasname
				final String dirnr = XmlPullUtil.optAttr(pp, "dirnr", null);
				final String prod = XmlPullUtil.attr(pp, "prod");
				final String classStr = XmlPullUtil.optAttr(pp, "class", null);
				final String dir = XmlPullUtil.optAttr(pp, "dir", null);
				final String capacityStr = XmlPullUtil.optAttr(pp, "capacity", null);
				final String depStation = XmlPullUtil.optAttr(pp, "depStation", null);
				final String delayReason = XmlPullUtil.optAttr(pp, "delayReason", null);
				// TODO is_reachable
				// TODO disableTrainInfo
				// TODO lineFG/lineBG (ZVV)
				final String administration = normalizeLineAdministration(XmlPullUtil.optAttr(pp, "administration", null));

				if (!"cancel".equals(delay) && !"cancel".equals(eDelay))
				{
					final Calendar plannedTime = new GregorianCalendar(timeZone);
					plannedTime.clear();
					ParserUtils.parseEuropeanTime(plannedTime, fpTime);
					if (fpDate.length() == 8)
						ParserUtils.parseGermanDate(plannedTime, fpDate);
					else if (fpDate.length() == 10)
						ParserUtils.parseIsoDate(plannedTime, fpDate);
					else
						throw new IllegalStateException("cannot parse: '" + fpDate + "'");

					final Calendar predictedTime;
					if (eDelay != null)
					{
						predictedTime = new GregorianCalendar(timeZone);
						predictedTime.setTimeInMillis(plannedTime.getTimeInMillis());
						predictedTime.add(Calendar.MINUTE, Integer.parseInt(eDelay));
					}
					else if (delay != null)
					{
						final Matcher m = P_XML_STATION_BOARD_DELAY.matcher(delay);
						if (m.matches())
						{
							if (m.group(1) != null)
							{
								predictedTime = new GregorianCalendar(timeZone);
								predictedTime.setTimeInMillis(plannedTime.getTimeInMillis());
								predictedTime.add(Calendar.MINUTE, Integer.parseInt(m.group(1)));
							}
							else
							{
								predictedTime = null;
							}
						}
						else
						{
							throw new RuntimeException("cannot parse delay: '" + delay + "'");
						}
					}
					else
					{
						predictedTime = null;
					}

					final Position position = parsePosition(ParserUtils.resolveEntities(platform));

					final String destinationName;
					if (dir != null)
						destinationName = dir.trim();
					else if (targetLoc != null)
						destinationName = targetLoc.trim();
					else
						destinationName = null;

					final Location destination;
					if (dirnr != null)
					{
						final String[] destinationPlaceAndName = splitStationName(destinationName);
						destination = new Location(LocationType.STATION, dirnr, destinationPlaceAndName[0], destinationPlaceAndName[1]);
					}
					else
					{
						destination = new Location(LocationType.ANY, null, null, destinationName);
					}

					final Line prodLine = parseLineAndType(prod);
					final Line line;
					if (classStr != null)
					{
						final Product product = intToProduct(Integer.parseInt(classStr));
						if (product == null)
							throw new IllegalArgumentException();
						// could check for type consistency here
						final Set<Attr> attrs = prodLine.attrs;
						if (attrs != null)
							line = newLine(administration, product, prodLine.label, null, attrs.toArray(new Line.Attr[0]));
						else
							line = newLine(administration, product, prodLine.label, null);
					}
					else
					{
						final Set<Attr> attrs = prodLine.attrs;
						if (attrs != null)
							line = newLine(administration, prodLine.product, prodLine.label, null, attrs.toArray(new Line.Attr[0]));
						else
							line = newLine(administration, prodLine.product, prodLine.label, null);
					}

					final int[] capacity;
					if (capacityStr != null && !"0|0".equals(capacityStr))
					{
						final String[] capacityParts = capacityStr.split("\\|");
						capacity = new int[] { Integer.parseInt(capacityParts[0]), Integer.parseInt(capacityParts[1]) };
					}
					else
					{
						capacity = null;
					}

					final String message;
					if (delayReason != null)
					{
						final String msg = delayReason.trim();
						message = msg.length() > 0 ? msg : null;
					}
					else
					{
						message = null;
					}

					final Departure departure = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
							position, destination, capacity, message);

					final Location location;
					if (!stationBoardCanDoEquivs || depStation == null)
					{
						location = new Location(LocationType.STATION, normalizedStationId, stationPlaceAndName != null ? stationPlaceAndName[0]
								: null, stationPlaceAndName != null ? stationPlaceAndName[1] : null);
					}
					else
					{
						final String[] depPlaceAndName = splitStationName(depStation);
						location = new Location(LocationType.STATION, null, depPlaceAndName[0], depPlaceAndName[1]);
					}

					StationDepartures stationDepartures = findStationDepartures(result.stationDepartures, location);
					if (stationDepartures == null)
					{
						stationDepartures = new StationDepartures(location, new ArrayList<Departure>(8), null);
						result.stationDepartures.add(stationDepartures);
					}

					stationDepartures.departures.add(departure);
				}

				XmlPullUtil.requireSkip(pp, "Journey");
			}

			if (stationBoardHasStationTable)
				XmlPullUtil.exit(pp, "StationTable");

			XmlPullUtil.requireEndDocument(pp);

			// sort departures
			for (final StationDepartures stationDepartures : result.stationDepartures)
				Collections.sort(stationDepartures.departures, Departure.TIME_COMPARATOR);

			return result;
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException("cannot parse xml: " + firstChars, x);
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}

	private StationDepartures findStationDepartures(final List<StationDepartures> stationDepartures, final Location location)
	{
		for (final StationDepartures stationDeparture : stationDepartures)
			if (stationDeparture.location.equals(location))
				return stationDeparture;

		return null;
	}

	protected void addCustomReplaces(final StringReplaceReader reader)
	{
	}

	protected final NearbyLocationsResult jsonLocGeoPos(final EnumSet<LocationType> types, final int lat, final int lon) throws IOException
	{
		final boolean getPOIs = types.contains(LocationType.POI);
		final String request = wrapJsonApiRequest("LocGeoPos", "{\"ring\":" //
				+ "{\"cCrd\":{\"x\":" + lon + ",\"y\":" + lat + "}}," //
				+ "\"getPOIs\":" + getPOIs + "}", //
				false);

		final String uri = checkNotNull(mgateEndpoint);
		final CharSequence page = httpClient.get(uri, request, "application/json", Charsets.UTF_8);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final String headErr = head.optString("err", null);
			if (headErr != null)
				throw new RuntimeException(headErr);
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), 0, null);

			final JSONArray svcResList = head.getJSONArray("svcResL");
			checkState(svcResList.length() == 1);
			final JSONObject svcRes = svcResList.optJSONObject(0);
			checkState("LocGeoPos".equals(svcRes.getString("meth")));
			final String err = svcRes.getString("err");
			if (!"OK".equals(err))
			{
				final String errTxt = svcRes.getString("errTxt");
				throw new RuntimeException(err + ": " + errTxt);
			}
			final JSONObject res = svcRes.getJSONObject("res");

			final JSONObject common = res.getJSONObject("common");
			/* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));

			final JSONArray locL = res.optJSONArray("locL");
			final List<Location> locations;
			if (locL != null)
			{
				locations = parseLocList(locL);

				// filter unwanted location types
				for (Iterator<Location> i = locations.iterator(); i.hasNext();)
				{
					final Location location = i.next();
					if (!types.contains(location.type))
						i.remove();
				}
			}
			else
			{
				locations = Collections.emptyList();
			}

			return new NearbyLocationsResult(header, locations);
		}
		catch (final JSONException x)
		{
			throw new ParserException("cannot parse json: '" + page + "' on " + uri, x);
		}
	}

	protected final QueryDeparturesResult jsonStationBoard(final String stationId, final @Nullable Date time, final int maxDepartures,
			final boolean equivs) throws IOException
	{
		final Calendar c = new GregorianCalendar(timeZone);
		c.setTime(time);
		final CharSequence jsonDate = jsonDate(c);
		final CharSequence jsonTime = jsonTime(c);
		final CharSequence normalizedStationId = normalizeStationId(stationId);
		final CharSequence stbFltrEquiv = Boolean.toString(!equivs);
		final CharSequence maxJny = Integer.toString(maxDepartures != 0 ? maxDepartures : DEFAULT_MAX_DEPARTURES);
		final CharSequence getPasslist = Boolean.toString(true); // traffic expensive
		final String request = wrapJsonApiRequest("StationBoard", "{\"type\":\"DEP\"," //
				+ "\"date\":\"" + jsonDate + "\"," //
				+ "\"time\":\"" + jsonTime + "\"," //
				+ "\"stbLoc\":{\"type\":\"S\"," + "\"state\":\"F\"," // F/M
				+ "\"extId\":" + JSONObject.quote(normalizedStationId.toString()) + "}," //
				+ "\"stbFltrEquiv\":" + stbFltrEquiv + ",\"maxJny\":" + maxJny + ",\"getPasslist\":" + getPasslist + "}", false);

		final String uri = checkNotNull(mgateEndpoint);
		final CharSequence page = httpClient.get(uri, request, "application/json", Charsets.UTF_8);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final String headErr = head.optString("err", null);
			if (headErr != null)
				throw new RuntimeException(headErr);
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), 0, null);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);

			final JSONArray svcResList = head.getJSONArray("svcResL");
			checkState(svcResList.length() == 1);
			final JSONObject svcRes = svcResList.optJSONObject(0);
			checkState("StationBoard".equals(svcRes.getString("meth")));
			final String err = svcRes.getString("err");
			if (!"OK".equals(err))
			{
				final String errTxt = svcRes.getString("errTxt");
				if ("LOCATION".equals(err) && "HCI Service: location missing or invalid".equals(errTxt))
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
				else
					throw new RuntimeException(err + ": " + errTxt);
			}
			else if ("1.10".equals(jsonApiVersion) && svcRes.toString().length() == 170)
			{
				// horrible hack, because API version 1.10 doesn't signal invalid stations via error
				return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
			}
			final JSONObject res = svcRes.getJSONObject("res");

			final JSONObject common = res.getJSONObject("common");
			/* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));
			final List<String> operators = parseOpList(common.getJSONArray("opL"));
			final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators);
			final JSONArray locList = common.getJSONArray("locL");
			final List<Location> locations = parseLocList(locList);

			final JSONArray jnyList = res.optJSONArray("jnyL");
			if (jnyList != null)
			{
				for (int iJny = 0; iJny < jnyList.length(); iJny++)
				{
					final JSONObject jny = jnyList.getJSONObject(iJny);
					final JSONObject stbStop = jny.getJSONObject("stbStop");

					final String stbStopPlatformS = stbStop.optString("dPlatfS", null);
					c.clear();
					ParserUtils.parseIsoDate(c, jny.getString("date"));
					final Date baseDate = c.getTime();

					final Date plannedTime = parseJsonTime(c, baseDate, stbStop.getString("dTimeS"));

					final Date predictedTime = parseJsonTime(c, baseDate, stbStop.optString("dTimeR", null));

					final Line line = lines.get(stbStop.getInt("dProdX"));

					final Location location = equivs ? locations.get(stbStop.getInt("locX")) : new Location(LocationType.STATION, stationId);
					final Position position = normalizePosition(stbStopPlatformS);

					final String jnyDirTxt = jny.getString("dirTxt");
					final JSONArray stopList = jny.optJSONArray("stopL");
					final Location destination;
					if (stopList != null)
					{
						final int lastStopIdx = stopList.getJSONObject(stopList.length() - 1).getInt("locX");
						final String lastStopName = locList.getJSONObject(lastStopIdx).getString("name");
						if (jnyDirTxt.equals(lastStopName))
							destination = locations.get(lastStopIdx);
						else
							destination = new Location(LocationType.ANY, null, null, jnyDirTxt);
					}
					else
					{
						destination = new Location(LocationType.ANY, null, null, jnyDirTxt);
					}

					final Departure departure = new Departure(plannedTime, predictedTime, line, position, destination, null, null);

					StationDepartures stationDepartures = findStationDepartures(result.stationDepartures, location);
					if (stationDepartures == null)
					{
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
		}
		catch (final JSONException x)
		{
			throw new ParserException("cannot parse json: '" + page + "' on " + uri, x);
		}
	}

	protected final SuggestLocationsResult jsonLocMatch(final CharSequence constraint) throws IOException
	{
		final String request = wrapJsonApiRequest("LocMatch", "{\"input\":{\"field\":\"S\",\"loc\":{\"name\":"
				+ JSONObject.quote(checkNotNull(constraint).toString()) + ",\"meta\":false},\"maxLoc\":" + DEFAULT_MAX_LOCATIONS + "}}", true);

		final String uri = checkNotNull(mgateEndpoint);
		final CharSequence page = httpClient.get(uri, request, "application/json", Charsets.UTF_8);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final String headErr = head.optString("err", null);
			if (headErr != null)
				throw new RuntimeException(headErr);
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), 0, null);

			final JSONArray svcResList = head.getJSONArray("svcResL");
			checkState(svcResList.length() == 1);
			final JSONObject svcRes = svcResList.optJSONObject(0);
			checkState("LocMatch".equals(svcRes.getString("meth")));
			final String err = svcRes.getString("err");
			if (!"OK".equals(err))
			{
				final String errTxt = svcRes.getString("errTxt");
				throw new RuntimeException(err + ": " + errTxt);
			}
			final JSONObject res = svcRes.getJSONObject("res");

			final JSONObject common = res.getJSONObject("common");
			/* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));

			final JSONObject match = res.getJSONObject("match");
			final List<Location> locations = parseLocList(match.optJSONArray("locL"));
			final List<SuggestedLocation> suggestedLocations = new ArrayList<SuggestedLocation>(locations.size());
			for (final Location location : locations)
				suggestedLocations.add(new SuggestedLocation(location));
			// TODO weight

			return new SuggestLocationsResult(header, suggestedLocations);
		}
		catch (final JSONException x)
		{
			throw new ParserException("cannot parse json: '" + page + "' on " + uri, x);
		}
	}

	private static final Joiner JOINER = Joiner.on(' ').skipNulls();

	protected final QueryTripsResult jsonTripSearch(Location from, Location to, final Date time, final boolean dep,
			final @Nullable Set<Product> products, final String moreContext) throws IOException
	{
		if (!from.hasId() && from.hasName())
		{
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);
			final List<Location> locations = suggestLocations(JOINER.join(from.place, from.name)).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
			if (locations.size() > 1)
				return new QueryTripsResult(header, locations, null, null);
			from = locations.get(0);
		}

		if (!to.hasId() && to.hasName())
		{
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);
			final List<Location> locations = suggestLocations(JOINER.join(to.place, to.name)).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
			if (locations.size() > 1)
				return new QueryTripsResult(header, null, null, locations);
			to = locations.get(0);
		}

		final Calendar c = new GregorianCalendar(timeZone);
		c.setTime(time);
		final CharSequence outDate = jsonDate(c);
		final CharSequence outTime = jsonTime(c);
		final CharSequence outFrwdKey = "1.11".equals(jsonApiVersion) ? "outFrwd" : "frwd";
		final CharSequence outFrwd = Boolean.toString(dep);
		final CharSequence jnyFltr = productsString(products);
		final CharSequence jsonContext = moreContext != null ? "\"ctxScr\":" + JSONObject.quote(moreContext) + "," : "";
		final String request = wrapJsonApiRequest("TripSearch", "{" //
				+ jsonContext //
				+ "\"depLocL\":[" + jsonLocation(from) + "]," //
				+ "\"arrLocL\":[" + jsonLocation(to) + "]," //
				+ "\"outDate\":\"" + outDate + "\"," //
				+ "\"outTime\":\"" + outTime + "\"," //
				+ "\"" + outFrwdKey + "\":" + outFrwd + "," //
				+ "\"jnyFltrL\":[{\"value\":\"" + jnyFltr + "\",\"mode\":\"BIT\",\"type\":\"PROD\"}]," //
				+ "\"gisFltrL\":[{\"mode\":\"FB\",\"profile\":{\"type\":\"F\",\"linDistRouting\":false,\"maxdist\":2000},\"type\":\"P\"}]," //
				+ "\"getPolyline\":false,\"getPasslist\":true,\"getIST\":false,\"getEco\":false,\"extChgTime\":-1}", //
				false);

		final String uri = checkNotNull(mgateEndpoint);
		final CharSequence page = httpClient.get(uri, request, "application/json", Charsets.UTF_8);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final String headErr = head.optString("err", null);
			if (headErr != null)
				throw new RuntimeException(headErr);
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, head.getString("ver"), 0, null);

			final JSONArray svcResList = head.getJSONArray("svcResL");
			checkState(svcResList.length() == 1);
			final JSONObject svcRes = svcResList.optJSONObject(0);
			checkState("TripSearch".equals(svcRes.getString("meth")));
			final String err = svcRes.getString("err");
			if (!"OK".equals(err))
			{
				log.debug("Hafas error: {}", err);
				if ("H890".equals(err)) // No connections found.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				if ("H891".equals(err)) // No route found (try entering an intermediate station).
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				if ("H895".equals(err)) // Departure/Arrival are too near.
					return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
				if ("H9220".equals(err)) // Nearby to the given address stations could not be found.
					return new QueryTripsResult(header, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
				if ("H9360".equals(err)) // Date outside of the timetable period.
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				final String errTxt = svcRes.getString("errTxt");
				throw new RuntimeException(err + ": " + errTxt);
			}
			final JSONObject res = svcRes.getJSONObject("res");

			final JSONObject common = res.getJSONObject("common");
			/* final List<String[]> remarks = */ parseRemList(common.getJSONArray("remL"));
			final List<Location> locations = parseLocList(common.getJSONArray("locL"));
			final List<String> operators = parseOpList(common.getJSONArray("opL"));
			final List<Line> lines = parseProdList(common.getJSONArray("prodL"), operators);

			final JSONArray outConList = res.optJSONArray("outConL");
			final List<Trip> trips = new ArrayList<Trip>(outConList.length());
			for (int iOutCon = 0; iOutCon < outConList.length(); iOutCon++)
			{
				final JSONObject outCon = outConList.getJSONObject(iOutCon);
				final Location tripFrom = locations.get(outCon.getJSONObject("dep").getInt("locX"));
				final Location tripTo = locations.get(outCon.getJSONObject("arr").getInt("locX"));

				c.clear();
				ParserUtils.parseIsoDate(c, outCon.getString("date"));
				final Date baseDate = c.getTime();

				final JSONArray secList = outCon.optJSONArray("secL");
				final List<Trip.Leg> legs = new ArrayList<Trip.Leg>(secList.length());
				for (int iSec = 0; iSec < secList.length(); iSec++)
				{
					final JSONObject sec = secList.getJSONObject(iSec);
					final String secType = sec.getString("type");

					final JSONObject secDep = sec.getJSONObject("dep");
					final Stop departureStop = parseJsonStop(secDep, locations, c, baseDate);

					final JSONObject secArr = sec.getJSONObject("arr");
					final Stop arrivalStop = parseJsonStop(secArr, locations, c, baseDate);

					final Trip.Leg leg;
					if ("JNY".equals(secType))
					{
						final JSONObject jny = sec.getJSONObject("jny");
						final Line line = lines.get(jny.getInt("prodX"));
						final String dirTxt = jny.optString("dirTxt", null);
						final Location destination = dirTxt != null ? new Location(LocationType.ANY, null, null, dirTxt) : null;

						final JSONArray stopList = jny.getJSONArray("stopL");
						checkState(stopList.length() >= 2);
						final List<Stop> intermediateStops = new ArrayList<Stop>(stopList.length());
						for (int iStop = 1; iStop < stopList.length() - 1; iStop++)
						{
							final JSONObject stop = stopList.getJSONObject(iStop);
							final Stop intermediateStop = parseJsonStop(stop, locations, c, baseDate);
							intermediateStops.add(intermediateStop);
						}

						leg = new Trip.Public(line, destination, departureStop, arrivalStop, intermediateStops, null, null);
					}
					else if ("WALK".equals(secType) || "TRSF".equals(secType))
					{
						final JSONObject gis = sec.getJSONObject("gis");
						final int distance = gis.getInt("dist");
						leg = new Trip.Individual(Trip.Individual.Type.WALK, departureStop.location, departureStop.getDepartureTime(),
								arrivalStop.location, arrivalStop.getArrivalTime(), null, distance);
					}
					else
					{
						throw new IllegalStateException("cannot handle type: " + secType);
					}

					legs.add(leg);
				}

				final JSONObject trfRes = outCon.optJSONObject("trfRes");
				final List<Fare> fares = new LinkedList<Fare>();
				if (trfRes != null)
				{
					final JSONArray fareSetList = trfRes.getJSONArray("fareSetL");
					for (int iFareSet = 0; iFareSet < fareSetList.length(); iFareSet++)
					{
						final JSONObject fareSet = fareSetList.getJSONObject(iFareSet);
						final String network = fareSet.optString("name", null);
						if (network != null)
						{
							final JSONArray fareList = fareSet.getJSONArray("fareL");
							for (int iFare = 0; iFare < fareList.length(); iFare++)
							{
								final JSONObject jsonFare = fareList.getJSONObject(iFare);
								final String name = jsonFare.getString("name");
								if (name.endsWith("- Jahreskarte") || name.endsWith("- Monatskarte"))
									continue;

								final Currency currency = Currency.getInstance(jsonFare.getString("cur"));
								final float price = jsonFare.getInt("prc") / 100f;

								if (name.startsWith("Vollpreis - "))
									fares.add(new Fare(network, Fare.Type.ADULT, currency, price, name.substring(12), null));
								else if (name.startsWith("Kind - "))
									fares.add(new Fare(network, Fare.Type.CHILD, currency, price, name.substring(7), null));
							}
						}
					}
				}

				final Trip trip = new Trip(null, tripFrom, tripTo, legs, fares, null, null);
				trips.add(trip);
			}

			final JsonContext context = new JsonContext(from, to, time, dep, products, res.optString("outCtxScrF"), res.optString("outCtxScrB"));
			return new QueryTripsResult(header, null, from, null, to, context, trips);
		}
		catch (final JSONException x)
		{
			throw new ParserException("cannot parse json: '" + page + "' on " + uri, x);
		}
	}

	private String wrapJsonApiRequest(final String meth, final String req, final boolean formatted)
	{
		return "{" //
				+ "\"auth\":" + checkNotNull(jsonApiAuthorization) + "," //
				+ "\"client\":" + checkNotNull(jsonApiClient) + "," //
				+ "\"ver\":\"" + checkNotNull(jsonApiVersion) + "\",\"lang\":\"eng\"," //
				+ "\"svcReqL\":[{\"cfg\":{\"polyEnc\":\"GPA\"},\"meth\":\"" + meth + "\",\"req\":" + req + "}]," //
				+ "\"formatted\":" + formatted + "}";
	}

	private String jsonLocation(final Location location)
	{
		if (location.type == LocationType.STATION && location.hasId())
			return "{\"type\":\"S\",\"extId\":" + JSONObject.quote(location.id) + "}";
		else if (location.type == LocationType.ADDRESS && location.hasId())
			return "{\"type\":\"A\",\"lid\":" + JSONObject.quote(location.id) + "}";
		else
			throw new IllegalArgumentException("cannot handle: " + location);
	}

	private CharSequence jsonDate(final Calendar time)
	{
		final int year = time.get(Calendar.YEAR);
		final int month = time.get(Calendar.MONTH) + 1;
		final int day = time.get(Calendar.DAY_OF_MONTH);
		return String.format(Locale.ENGLISH, "%04d%02d%02d", year, month, day);
	}

	private CharSequence jsonTime(final Calendar time)
	{
		final int hour = time.get(Calendar.HOUR_OF_DAY);
		final int minute = time.get(Calendar.MINUTE);
		return String.format(Locale.ENGLISH, "%02d%02d00", hour, minute);
	}

	private static final Pattern P_JSON_TIME = Pattern.compile("(\\d{2})?(\\d{2})(\\d{2})(\\d{2})");

	private final Date parseJsonTime(final Calendar calendar, final Date baseDate, final CharSequence str)
	{
		if (str == null)
			return null;

		final Matcher m = P_JSON_TIME.matcher(str);
		if (m.matches())
		{
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

	private Stop parseJsonStop(final JSONObject json, final List<Location> locations, final Calendar c, final Date baseDate) throws JSONException
	{
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

		return new Stop(location, plannedArrivalTime, predictedArrivalTime, plannedArrivalPosition, predictedArrivalPosition, arrivalCancelled,
				plannedDepartureTime, predictedDepartureTime, plannedDeparturePosition, predictedDeparturePosition, departureCancelled);
	}

	private List<String[]> parseRemList(final JSONArray remList) throws JSONException
	{
		final List<String[]> remarks = new ArrayList<String[]>(remList.length());

		for (int i = 0; i < remList.length(); i++)
		{
			final JSONObject rem = remList.getJSONObject(i);
			final String code = rem.getString("code");
			final String txt = rem.getString("txtN");
			remarks.add(new String[] { code, txt });
		}

		return remarks;
	}

	private List<Location> parseLocList(final JSONArray locList) throws JSONException
	{
		final List<Location> locations = new ArrayList<Location>(locList.length());

		for (int iLoc = 0; iLoc < locList.length(); iLoc++)
		{
			final JSONObject loc = locList.getJSONObject(iLoc);
			final String type = loc.getString("type");

			final JSONObject crd = loc.getJSONObject("crd");

			if ("S".equals(type))
			{
				final String[] placeAndName = splitStationName(loc.getString("name"));
				final int pCls = loc.optInt("pCls", -1);
				final Set<Product> products = pCls != -1 ? intToProducts(pCls) : null;
				final String id = normalizeStationId(loc.getString("extId"));
				locations.add(new Location(LocationType.STATION, id, crd.getInt("y"), crd.getInt("x"), placeAndName[0], placeAndName[1], products));
			}
			else if ("P".equals(type))
			{
				final String[] placeAndName = splitPOI(loc.getString("name"));
				final String id = normalizeStationId(loc.getString("extId"));
				locations.add(new Location(LocationType.POI, id, crd.getInt("y"), crd.getInt("x"), placeAndName[0], placeAndName[1]));
			}
			else if ("A".equals(type))
			{
				final String[] placeAndName = splitAddress(loc.getString("name"));
				final String id = loc.getString("lid");
				locations.add(new Location(LocationType.ADDRESS, id, crd.getInt("y"), crd.getInt("x"), placeAndName[0], placeAndName[1]));
			}
			else
			{
				throw new RuntimeException("Unknown type " + type + ": " + loc);
			}
		}

		return locations;
	}

	private List<String> parseOpList(final JSONArray opList) throws JSONException
	{
		final List<String> operators = new ArrayList<String>(opList.length());

		for (int i = 0; i < opList.length(); i++)
		{
			final JSONObject op = opList.getJSONObject(i);
			final String operator = op.getString("name");
			operators.add(operator);
		}

		return operators;
	}

	private List<Line> parseProdList(final JSONArray prodList, final List<String> operators) throws JSONException
	{
		final List<Line> lines = new ArrayList<Line>(prodList.length());

		for (int iProd = 0; iProd < prodList.length(); iProd++)
		{
			final JSONObject prod = prodList.getJSONObject(iProd);
			final int oprIndex = prod.optInt("oprX", -1);
			final String operator = oprIndex != -1 ? operators.get(oprIndex) : null;
			final int cls = prod.optInt("cls", -1);
			final Product product = cls != -1 ? intToProduct(cls) : null;
			final String name = prod.getString("name");
			final String normalizedName;
			if (product == Product.BUS && name.startsWith("Bus "))
				normalizedName = name.substring(4);
			else if (product == Product.TRAM && name.startsWith("Tram "))
				normalizedName = name.substring(5);
			else if (product == Product.SUBURBAN_TRAIN && name.startsWith("S "))
				normalizedName = "S" + name.substring(2);
			else
				normalizedName = name;
			final Line line = new Line(null, operator, product, normalizedName, lineStyle(operator, product, normalizedName));
			lines.add(line);
		}

		return lines;
	}

	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException
	{
		return queryTripsBinary(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return queryMoreTripsBinary(context, later);
	}

	protected final QueryTripsResult queryTripsXml(Location from, @Nullable Location via, Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed, final @Nullable Accessibility accessibility,
			final @Nullable Set<Option> options) throws IOException
	{
		final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

		if (!from.isIdentified())
		{
			final List<Location> locations = suggestLocations(from.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, locations, null, null);
			from = locations.get(0);
		}

		if (via != null && !via.isIdentified())
		{
			final List<Location> locations = suggestLocations(via.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, null, locations, null);
			via = locations.get(0);
		}

		if (!to.isIdentified())
		{
			final List<Location> locations = suggestLocations(to.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, null, null, locations);
			to = locations.get(0);
		}

		final Calendar c = new GregorianCalendar(timeZone);
		c.setTime(date);

		final CharSequence productsStr;
		if (products != null)
			productsStr = productsString(products);
		else
			productsStr = allProductsString();

		final char bikeChar = (options != null && options.contains(Option.BIKE)) ? '1' : '0';

		final StringBuilder conReq = new StringBuilder("<ConReq deliverPolyline=\"1\">");
		conReq.append("<Start>").append(locationXml(from));
		conReq.append("<Prod prod=\"").append(productsStr).append("\" bike=\"").append(bikeChar)
				.append("\" couchette=\"0\" direct=\"0\" sleeper=\"0\"/>");
		conReq.append("</Start>");
		if (via != null)
		{
			conReq.append("<Via>").append(locationXml(via));
			if (via.type != LocationType.ADDRESS)
				conReq.append("<Prod prod=\"").append(productsStr).append("\" bike=\"").append(bikeChar)
						.append("\" couchette=\"0\" direct=\"0\" sleeper=\"0\"/>");
			conReq.append("</Via>");
		}
		conReq.append("<Dest>").append(locationXml(to)).append("</Dest>");
		conReq.append("<ReqT a=\"")
				.append(dep ? 0 : 1)
				.append("\" date=\"")
				.append(String.format(Locale.ENGLISH, "%04d.%02d.%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)))
				.append("\" time=\"")
				.append(String.format(Locale.ENGLISH, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) + "\"/>");
		conReq.append("<RFlags");
		// number of trips backwards
		conReq.append(" b=\"").append(0).append("\"");
		// number of trips forwards
		conReq.append(" f=\"").append(numTripsRequested).append("\"");
		// percentual extension of change time
		conReq.append(" chExtension=\"").append(walkSpeed == WalkSpeed.SLOW ? 50 : 0).append("\"");
		// TODO nrChanges: max number of changes
		conReq.append(" sMode=\"N\"/>");
		conReq.append("</ConReq>");

		return queryTripsXml(null, true, conReq, from, via, to);
	}

	protected final QueryTripsResult queryMoreTripsXml(final QueryTripsContext contextObj, final boolean later) throws IOException
	{
		final Context context = (Context) contextObj;

		final StringBuilder conScrReq = new StringBuilder("<ConScrReq scrDir=\"").append(later ? 'F' : 'B').append("\" nrCons=\"")
				.append(numTripsRequested).append("\">");
		conScrReq.append("<ConResCtxt>").append(later ? context.laterContext : context.earlierContext).append("</ConResCtxt>");
		conScrReq.append("</ConScrReq>");

		return queryTripsXml(context, later, conScrReq, null, null, null);
	}

	private QueryTripsResult queryTripsXml(final Context previousContext, final boolean later, final CharSequence conReq, final Location from,
			final @Nullable Location via, final Location to) throws IOException
	{
		final String request = wrapReqC(conReq, null);

		Reader reader = null;
		String firstChars = null;

		try
		{
			final String endpoint = extXmlEndpoint != null ? extXmlEndpoint : queryEndpoint;
			final InputStream is = httpClient.getInputStream(endpoint, request, "application/xml", null, null);
			firstChars = HttpClient.peekFirstChars(is);
			reader = new InputStreamReader(is, Charsets.ISO_8859_1);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(reader);

			XmlPullUtil.require(pp, "ResC");
			final String product = XmlPullUtil.attr(pp, "prod").split(" ")[0];
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, product, 0, null);
			XmlPullUtil.enter(pp, "ResC");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("I3")) // Input: date outside of the timetable period
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				if (code.equals("F1")) // Spool: Error reading the spoolfile
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			XmlPullUtil.enter(pp, "ConRes");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				log.debug("Hafas error: {}", code);
				if (code.equals("K9260")) // Unknown departure station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
				if (code.equals("K9280")) // Unknown intermediate station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_VIA);
				if (code.equals("K9300")) // Unknown arrival station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
				if (code.equals("K9360")) // Date outside of the timetable period
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				if (code.equals("K9380")) // Dep./Arr./Intermed. or equivalent station defined more that once
					return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
				if (code.equals("K895")) // Departure/Arrival are too near
					return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
				if (code.equals("K9220")) // Nearby to the given address stations could not be found
					return new QueryTripsResult(header, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
				if (code.equals("K9240")) // Internal error
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				if (code.equals("K890")) // No connections found
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				if (code.equals("K891")) // No route found (try entering an intermediate station)
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				if (code.equals("K899")) // An error occurred
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				if (code.equals("K1:890")) // Unsuccessful or incomplete search (direction: forward)
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				if (code.equals("K2:890")) // Unsuccessful or incomplete search (direction: backward)
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			final String c = XmlPullUtil.optValueTag(pp, "ConResCtxt", null);
			final Context context;
			if (previousContext == null)
				context = new Context(c, c, 0);
			else if (later)
				context = new Context(c, previousContext.earlierContext, previousContext.sequence + 1);
			else
				context = new Context(previousContext.laterContext, c, previousContext.sequence + 1);

			XmlPullUtil.enter(pp, "ConnectionList");

			final List<Trip> trips = new ArrayList<Trip>();

			while (XmlPullUtil.test(pp, "Connection"))
			{
				final String id = context.sequence + "/" + XmlPullUtil.attr(pp, "id");

				XmlPullUtil.enter(pp, "Connection");
				while (pp.getName().equals("RtStateList"))
					XmlPullUtil.next(pp);
				XmlPullUtil.enter(pp, "Overview");

				final Calendar currentDate = new GregorianCalendar(timeZone);
				currentDate.clear();
				parseDate(currentDate, XmlPullUtil.valueTag(pp, "Date"));
				XmlPullUtil.enter(pp, "Departure");
				XmlPullUtil.enter(pp, "BasicStop");
				while (pp.getName().equals("StAttrList"))
					XmlPullUtil.next(pp);
				final Location departureLocation = parseLocation(pp);
				XmlPullUtil.enter(pp, "Dep");
				XmlPullUtil.skipExit(pp, "Dep");
				final int[] capacity;
				if (XmlPullUtil.test(pp, "StopPrognosis"))
				{
					XmlPullUtil.enter(pp, "StopPrognosis");
					if (XmlPullUtil.test(pp, "Arr"))
						XmlPullUtil.next(pp);
					if (XmlPullUtil.test(pp, "Dep"))
						XmlPullUtil.next(pp);
					XmlPullUtil.enter(pp, "Status");
					XmlPullUtil.skipExit(pp, "Status");
					final int capacity1st = Integer.parseInt(XmlPullUtil.optValueTag(pp, "Capacity1st", "0"));
					final int capacity2nd = Integer.parseInt(XmlPullUtil.optValueTag(pp, "Capacity2nd", "0"));
					if (capacity1st > 0 || capacity2nd > 0)
						capacity = new int[] { capacity1st, capacity2nd };
					else
						capacity = null;
					XmlPullUtil.skipExit(pp, "StopPrognosis");
				}
				else
				{
					capacity = null;
				}
				XmlPullUtil.skipExit(pp, "BasicStop");
				XmlPullUtil.skipExit(pp, "Departure");

				XmlPullUtil.enter(pp, "Arrival");
				XmlPullUtil.enter(pp, "BasicStop");
				while (pp.getName().equals("StAttrList"))
					XmlPullUtil.next(pp);
				final Location arrivalLocation = parseLocation(pp);
				XmlPullUtil.skipExit(pp, "BasicStop");
				XmlPullUtil.skipExit(pp, "Arrival");

				final int numTransfers = Integer.parseInt(XmlPullUtil.valueTag(pp, "Transfers"));

				XmlPullUtil.skipExit(pp, "Overview");

				final List<Trip.Leg> legs = new ArrayList<Trip.Leg>(4);

				XmlPullUtil.enter(pp, "ConSectionList");

				final Calendar time = new GregorianCalendar(timeZone);

				while (XmlPullUtil.test(pp, "ConSection"))
				{
					XmlPullUtil.enter(pp, "ConSection");

					// departure
					XmlPullUtil.enter(pp, "Departure");
					XmlPullUtil.enter(pp, "BasicStop");
					while (pp.getName().equals("StAttrList"))
						XmlPullUtil.next(pp);
					final Location sectionDepartureLocation = parseLocation(pp);

					if (XmlPullUtil.test(pp, "Arr"))
					{
						XmlPullUtil.enter(pp, "Arr");
						XmlPullUtil.skipExit(pp, "Arr");
					}
					XmlPullUtil.enter(pp, "Dep");
					time.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(time, XmlPullUtil.valueTag(pp, "Time"));
					final Date departureTime = time.getTime();
					final Position departurePos = parsePlatform(pp);
					XmlPullUtil.skipExit(pp, "Dep");

					XmlPullUtil.skipExit(pp, "BasicStop");
					XmlPullUtil.skipExit(pp, "Departure");

					// journey
					final Line line;
					Location destination = null;

					List<Stop> intermediateStops = null;

					final String tag = pp.getName();
					if (tag.equals("Journey"))
					{
						XmlPullUtil.enter(pp, "Journey");
						while (pp.getName().equals("JHandle"))
							XmlPullUtil.next(pp);
						XmlPullUtil.enter(pp, "JourneyAttributeList");
						boolean wheelchairAccess = false;
						String name = null;
						String category = null;
						String shortCategory = null;
						while (XmlPullUtil.test(pp, "JourneyAttribute"))
						{
							XmlPullUtil.enter(pp, "JourneyAttribute");
							XmlPullUtil.require(pp, "Attribute");
							final String attrName = XmlPullUtil.attr(pp, "type");
							final String code = XmlPullUtil.optAttr(pp, "code", null);
							XmlPullUtil.enter(pp, "Attribute");
							final Map<String, String> attributeVariants = parseAttributeVariants(pp);
							XmlPullUtil.skipExit(pp, "Attribute");
							XmlPullUtil.skipExit(pp, "JourneyAttribute");

							if ("bf".equals(code))
							{
								wheelchairAccess = true;
							}
							else if ("NAME".equals(attrName))
							{
								name = attributeVariants.get("NORMAL");
							}
							else if ("CATEGORY".equals(attrName))
							{
								shortCategory = attributeVariants.get("SHORT");
								category = attributeVariants.get("NORMAL");
								// longCategory = attributeVariants.get("LONG");
							}
							else if ("DIRECTION".equals(attrName))
							{
								final String[] destinationPlaceAndName = splitStationName(attributeVariants.get("NORMAL"));
								destination = new Location(LocationType.ANY, null, destinationPlaceAndName[0], destinationPlaceAndName[1]);
							}
						}
						XmlPullUtil.skipExit(pp, "JourneyAttributeList");

						if (XmlPullUtil.test(pp, "PassList"))
						{
							intermediateStops = new LinkedList<Stop>();

							XmlPullUtil.enter(pp, "PassList");
							while (XmlPullUtil.test(pp, "BasicStop"))
							{
								XmlPullUtil.enter(pp, "BasicStop");
								while (XmlPullUtil.test(pp, "StAttrList"))
									XmlPullUtil.next(pp);
								final Location location = parseLocation(pp);
								if (location.id != sectionDepartureLocation.id)
								{
									Date stopArrivalTime = null;
									Date stopDepartureTime = null;
									Position stopArrivalPosition = null;
									Position stopDeparturePosition = null;

									if (XmlPullUtil.test(pp, "Arr"))
									{
										XmlPullUtil.enter(pp, "Arr");
										time.setTimeInMillis(currentDate.getTimeInMillis());
										parseTime(time, XmlPullUtil.valueTag(pp, "Time"));
										stopArrivalTime = time.getTime();
										stopArrivalPosition = parsePlatform(pp);
										XmlPullUtil.skipExit(pp, "Arr");
									}

									if (XmlPullUtil.test(pp, "Dep"))
									{
										XmlPullUtil.enter(pp, "Dep");
										time.setTimeInMillis(currentDate.getTimeInMillis());
										parseTime(time, XmlPullUtil.valueTag(pp, "Time"));
										stopDepartureTime = time.getTime();
										stopDeparturePosition = parsePlatform(pp);
										XmlPullUtil.skipExit(pp, "Dep");
									}

									intermediateStops.add(new Stop(location, stopArrivalTime, stopArrivalPosition, stopDepartureTime,
											stopDeparturePosition));
								}
								XmlPullUtil.skipExit(pp, "BasicStop");
							}

							XmlPullUtil.skipExit(pp, "PassList");
						}

						XmlPullUtil.skipExit(pp, "Journey");

						if (category == null)
							category = shortCategory;

						line = parseLine(category, name, wheelchairAccess);
					}
					else if (tag.equals("Walk") || tag.equals("Transfer") || tag.equals("GisRoute"))
					{
						XmlPullUtil.enter(pp);
						XmlPullUtil.enter(pp, "Duration");
						XmlPullUtil.skipExit(pp, "Duration");
						XmlPullUtil.skipExit(pp);

						line = null;
					}
					else
					{
						throw new IllegalStateException("cannot handle: " + pp.getName());
					}

					// polyline
					final List<Point> path;
					if (XmlPullUtil.test(pp, "Polyline"))
					{
						path = new LinkedList<Point>();
						XmlPullUtil.enter(pp, "Polyline");
						while (XmlPullUtil.test(pp, "Point"))
						{
							final int x = XmlPullUtil.intAttr(pp, "x");
							final int y = XmlPullUtil.intAttr(pp, "y");
							path.add(new Point(y, x));
							XmlPullUtil.next(pp);
						}
						XmlPullUtil.skipExit(pp, "Polyline");
					}
					else
					{
						path = null;
					}

					// arrival
					XmlPullUtil.enter(pp, "Arrival");
					XmlPullUtil.enter(pp, "BasicStop");
					while (pp.getName().equals("StAttrList"))
						XmlPullUtil.next(pp);
					final Location sectionArrivalLocation = parseLocation(pp);
					XmlPullUtil.enter(pp, "Arr");
					time.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(time, XmlPullUtil.valueTag(pp, "Time"));
					final Date arrivalTime = time.getTime();
					final Position arrivalPos = parsePlatform(pp);
					XmlPullUtil.skipExit(pp, "Arr");

					XmlPullUtil.skipExit(pp, "BasicStop");
					XmlPullUtil.skipExit(pp, "Arrival");

					// remove last intermediate
					if (intermediateStops != null)
						if (!intermediateStops.isEmpty())
							if (!intermediateStops.get(intermediateStops.size() - 1).location.equals(sectionArrivalLocation))
								intermediateStops.remove(intermediateStops.size() - 1);

					XmlPullUtil.skipExit(pp, "ConSection");

					if (line != null)
					{
						final Stop departure = new Stop(sectionDepartureLocation, true, departureTime, null, departurePos, null);
						final Stop arrival = new Stop(sectionArrivalLocation, false, arrivalTime, null, arrivalPos, null);

						legs.add(new Trip.Public(line, destination, departure, arrival, intermediateStops, path, null));
					}
					else
					{
						if (legs.size() > 0 && legs.get(legs.size() - 1) instanceof Trip.Individual)
						{
							final Trip.Individual lastIndividualLeg = (Trip.Individual) legs.remove(legs.size() - 1);
							legs.add(new Trip.Individual(Trip.Individual.Type.WALK, lastIndividualLeg.departure, lastIndividualLeg.departureTime,
									sectionArrivalLocation, arrivalTime, null, 0));
						}
						else
						{
							legs.add(new Trip.Individual(Trip.Individual.Type.WALK, sectionDepartureLocation, departureTime, sectionArrivalLocation,
									arrivalTime, null, 0));
						}
					}
				}

				XmlPullUtil.skipExit(pp, "ConSectionList");

				XmlPullUtil.skipExit(pp, "Connection");

				trips.add(new Trip(id, departureLocation, arrivalLocation, legs, null, capacity, numTransfers));
			}

			XmlPullUtil.skipExit(pp, "ConnectionList");

			return new QueryTripsResult(header, null, from, via, to, context, trips);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException("cannot parse xml: " + firstChars, x);
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}

	private final Location parseLocation(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		final Location location;
		if (pp.getName().equals("Station"))
			location = parseStation(pp);
		else if (pp.getName().equals("Poi"))
			location = parsePoi(pp);
		else if (pp.getName().equals("Address"))
			location = parseAddress(pp);
		else
			throw new IllegalStateException("cannot parse: " + pp.getName());
		XmlPullUtil.next(pp);
		return location;
	}

	private final Map<String, String> parseAttributeVariants(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		final Map<String, String> attributeVariants = new HashMap<String, String>();

		while (XmlPullUtil.test(pp, "AttributeVariant"))
		{
			final String type = XmlPullUtil.attr(pp, "type");
			XmlPullUtil.enter(pp, "AttributeVariant");
			final String value = XmlPullUtil.optValueTag(pp, "Text", null);
			XmlPullUtil.skipExit(pp, "AttributeVariant");

			attributeVariants.put(type, value);
		}

		return attributeVariants;
	}

	private static final Pattern P_DATE = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");

	private static final void parseDate(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_DATE.matcher(str);
		if (!m.matches())
			throw new RuntimeException("cannot parse: '" + str + "'");

		calendar.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
		calendar.set(Calendar.MONTH, Integer.parseInt(m.group(2)) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(3)));
	}

	private static final Pattern P_TIME = Pattern.compile("(\\d+)d(\\d+):(\\d{2}):(\\d{2})");

	private static void parseTime(final Calendar calendar, final CharSequence str)
	{
		final Matcher m = P_TIME.matcher(str);
		if (!m.matches())
			throw new IllegalArgumentException("cannot parse: '" + str + "'");

		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(2)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
		calendar.set(Calendar.SECOND, Integer.parseInt(m.group(4)));
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(1)));
	}

	private static final String locationXml(final Location location)
	{
		if (location.type == LocationType.STATION && location.hasId())
			return "<Station externalId=\"" + normalizeStationId(location.id) + "\" />";
		else if (location.type == LocationType.POI && location.hasLocation())
			return "<Poi type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" />";
		else if (location.type == LocationType.ADDRESS && location.hasLocation())
			return "<Address type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" name=\""
					+ (location.place != null ? location.place + ", " : "") + location.name + "\" />";
		else if (location.type == LocationType.COORD && location.hasLocation())
			return "<Coord type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" />";
		else
			throw new IllegalArgumentException("cannot handle: " + location);
	}

	protected final String locationId(final Location location)
	{
		final StringBuilder id = new StringBuilder();

		id.append("A=").append(locationType(location));

		if (location.type == LocationType.STATION && location.hasId())
		{
			id.append("@L=").append(normalizeStationId(location.id));
		}
		else if (location.hasLocation())
		{
			id.append("@X=").append(location.lon);
			id.append("@Y=").append(location.lat);
			id.append("@O=").append(
					location.name != null ? location.name : String.format(Locale.ENGLISH, "%.6f, %.6f", location.lat / 1E6, location.lon / 1E6));
		}
		else if (location.name != null)
		{
			id.append("@G=").append(location.name);
			if (location.type != LocationType.ANY)
				id.append('!');
		}

		return id.toString();
	}

	protected static final int locationType(final Location location)
	{
		final LocationType type = location.type;
		if (type == LocationType.STATION)
			return 1;
		if (type == LocationType.POI)
			return 4;
		if (type == LocationType.COORD || (type == LocationType.ADDRESS && location.hasLocation()))
			return 16;
		if (type == LocationType.ADDRESS && location.name != null)
			return 2;
		if (type == LocationType.ANY)
			return 255;
		throw new IllegalArgumentException(location.type.toString());
	}

	protected void appendQueryTripsBinaryParameters(final StringBuilder uri, final Location from, final @Nullable Location via, final Location to,
			final Date date, final boolean dep, final @Nullable Set<Product> products, final @Nullable Accessibility accessibility,
			final @Nullable Set<Option> options)
	{
		uri.append("?start=Suchen");

		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from), Charsets.ISO_8859_1));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to), Charsets.ISO_8859_1));

		if (via != null)
		{
			// workaround, for there does not seem to be a REQ0JourneyStops1.0ID parameter

			uri.append("&REQ0JourneyStops1.0A=").append(locationType(via));

			if (via.type == LocationType.STATION && via.hasId())
			{
				uri.append("&REQ0JourneyStops1.0L=").append(via.id);
			}
			else if (via.hasLocation())
			{
				uri.append("&REQ0JourneyStops1.0X=").append(via.lon);
				uri.append("&REQ0JourneyStops1.0Y=").append(via.lat);
				if (via.name == null)
					uri.append("&REQ0JourneyStops1.0O=").append(
							ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%.6f, %.6f", via.lat / 1E6, via.lon / 1E6), Charsets.ISO_8859_1));
			}
			else if (via.name != null)
			{
				uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via.name, Charsets.ISO_8859_1));
				if (via.type != LocationType.ANY)
					uri.append('!');
			}
		}

		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");

		appendDateTimeParameters(uri, date, "REQ0JourneyDate", "REQ0JourneyTime");

		final CharSequence productsStr;
		if (products != null)
			productsStr = productsString(products);
		else
			productsStr = allProductsString();
		uri.append("&REQ0JourneyProduct_prod_list_1=").append(productsStr);

		if (accessibility != null && accessibility != Accessibility.NEUTRAL)
		{
			if (accessibility == Accessibility.LIMITED)
				uri.append("&REQ0AddParamBaimprofile=1");
			else if (accessibility == Accessibility.BARRIER_FREE)
				uri.append("&REQ0AddParamBaimprofile=0");
		}

		if (options != null && options.contains(Option.BIKE))
			uri.append("&REQ0JourneyProduct_opt3=1");

		appendCommonQueryTripsBinaryParameters(uri);
	}

	protected void appendCommonQueryTripsBinaryParameters(final StringBuilder uri)
	{
		uri.append("&h2g-direct=11");
		if (clientType != null)
			uri.append("&clientType=").append(ParserUtils.urlEncode(clientType));
	}

	private final static int QUERY_TRIPS_BINARY_BUFFER_SIZE = 384 * 1024;

	protected final QueryTripsResult queryTripsBinary(Location from, @Nullable Location via, Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed, final @Nullable Accessibility accessibility,
			final @Nullable Set<Option> options) throws IOException
	{
		final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);

		if (!from.isIdentified())
		{
			final List<Location> locations = suggestLocations(from.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, locations, null, null);
			from = locations.get(0);
		}

		if (via != null && !via.isIdentified())
		{
			final List<Location> locations = suggestLocations(via.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, null, locations, null);
			via = locations.get(0);
		}

		if (!to.isIdentified())
		{
			final List<Location> locations = suggestLocations(to.name).getLocations();
			if (locations.isEmpty())
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS); // TODO
			if (locations.size() > 1)
				return new QueryTripsResult(header, null, null, locations);
			to = locations.get(0);
		}

		final StringBuilder uri = new StringBuilder(queryEndpoint);
		appendQueryTripsBinaryParameters(uri, from, via, to, date, dep, products, accessibility, options);

		return queryTripsBinary(uri.toString(), from, via, to, QUERY_TRIPS_BINARY_BUFFER_SIZE);
	}

	protected void appendQueryMoreTripsBinaryParameters(final StringBuilder uri, final QueryTripsBinaryContext context, final boolean later)
	{
		uri.append("?seqnr=").append(context.seqNr);
		uri.append("&ident=").append(context.ident);
		if (context.ld != null)
			uri.append("&ld=").append(context.ld);
		uri.append("&REQ0HafasScrollDir=").append(later ? 1 : 2);

		appendCommonQueryTripsBinaryParameters(uri);
	}

	protected QueryTripsResult queryMoreTripsBinary(final QueryTripsContext contextObj, final boolean later) throws IOException
	{
		final QueryTripsBinaryContext context = (QueryTripsBinaryContext) contextObj;

		final StringBuilder uri = new StringBuilder(queryEndpoint);
		appendQueryMoreTripsBinaryParameters(uri, context, later);

		return queryTripsBinary(uri.toString(), null, null, null, QUERY_TRIPS_BINARY_BUFFER_SIZE + context.usedBufferSize);
	}

	private class CustomBufferedInputStream extends BufferedInputStream
	{
		public CustomBufferedInputStream(final InputStream in)
		{
			super(in);
		}

		public int getCount()
		{
			return count;
		}
	}

	private QueryTripsResult queryTripsBinary(final String uri, final Location from, final @Nullable Location via, final Location to,
			final int expectedBufferSize) throws IOException
	{
		/*
		 * Many thanks to Malte Starostik and Robert, who helped a lot with analyzing this API!
		 */

		LittleEndianDataInputStream is = null;

		try
		{
			final CustomBufferedInputStream bis = new CustomBufferedInputStream(httpClient.getInputStream(uri));
			final String firstChars = HttpClient.peekFirstChars(bis);

			// initialize input stream
			is = new LittleEndianDataInputStream(bis);
			is.mark(expectedBufferSize);

			// quick check of status
			final int version = is.readShortReverse();
			if (version != 6 && version != 5)
				throw new IllegalStateException("unknown version: " + version + ", first chars: " + firstChars);
			final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT, Integer.toString(version), 0, null);

			// quick seek for pointers
			is.reset();
			is.skipBytes(0x20);
			final int serviceDaysTablePtr = is.readIntReverse();
			final int stringTablePtr = is.readIntReverse();

			is.reset();
			is.skipBytes(0x36);
			final int stationTablePtr = is.readIntReverse();
			final int commentTablePtr = is.readIntReverse();

			is.reset();
			is.skipBytes(0x46);
			final int extensionHeaderPtr = is.readIntReverse();

			// read strings
			final StringTable strings = new StringTable(is, stringTablePtr, serviceDaysTablePtr - stringTablePtr);

			is.reset();
			is.skipBytes(extensionHeaderPtr);

			// read extension header
			final int extensionHeaderLength = is.readIntReverse();
			if (extensionHeaderLength < 0x2c)
				throw new IllegalStateException("too short: " + extensionHeaderLength);

			is.skipBytes(12);
			final int errorCode = is.readShortReverse();

			if (errorCode == 0)
			{
				// string encoding
				is.skipBytes(14);
				final Charset stringEncoding = Charset.forName(strings.read(is));
				strings.setEncoding(stringEncoding);

				// read number of trips
				is.reset();
				is.skipBytes(30);

				final int numTrips = is.readShortReverse();
				if (numTrips == 0)
					return new QueryTripsResult(header, uri, from, via, to, null, new LinkedList<Trip>());

				// read rest of header
				is.reset();
				is.skipBytes(0x02);

				final Location resDeparture = location(is, strings);
				final Location resArrival = location(is, strings);

				is.skipBytes(10);

				final long resDate = date(is);
				/* final long resDate30 = */date(is);

				is.reset();
				is.skipBytes(extensionHeaderPtr + 0x8);

				final int seqNr = is.readShortReverse();
				if (seqNr == 0)
					throw new SessionExpiredException();
				else if (seqNr < 0)
					throw new IllegalStateException("illegal sequence number: " + seqNr);

				final String requestId = strings.read(is);

				final int tripDetailsPtr = is.readIntReverse();
				if (tripDetailsPtr == 0)
					throw new IllegalStateException("no connection details");

				is.skipBytes(4);

				final int disruptionsPtr = is.readIntReverse();

				is.skipBytes(10);

				final String ld = strings.read(is);
				final int attrsOffset = is.readIntReverse();

				final int tripAttrsPtr;
				if (extensionHeaderLength >= 0x30)
				{
					if (extensionHeaderLength < 0x32)
						throw new IllegalArgumentException("too short: " + extensionHeaderLength);
					is.reset();
					is.skipBytes(extensionHeaderPtr + 0x2c);
					tripAttrsPtr = is.readIntReverse();
				}
				else
				{
					tripAttrsPtr = 0;
				}

				// determine stops offset
				is.reset();
				is.skipBytes(tripDetailsPtr);
				final int tripDetailsVersion = is.readShortReverse();
				if (tripDetailsVersion != 1)
					throw new IllegalStateException("unknown trip details version: " + tripDetailsVersion);
				is.skipBytes(0x02);

				final int tripDetailsIndexOffset = is.readShortReverse();
				final int tripDetailsLegOffset = is.readShortReverse();
				final int tripDetailsLegSize = is.readShortReverse();
				final int stopsSize = is.readShortReverse();
				final int stopsOffset = is.readShortReverse();

				// read stations
				final StationTable stations = new StationTable(is, stationTablePtr, commentTablePtr - stationTablePtr, strings);

				// read comments
				final CommentTable comments = new CommentTable(is, commentTablePtr, tripDetailsPtr - commentTablePtr, strings);

				final List<Trip> trips = new ArrayList<Trip>(numTrips);

				// read trips
				for (int iTrip = 0; iTrip < numTrips; iTrip++)
				{
					is.reset();
					is.skipBytes(0x4a + iTrip * 12);

					final int serviceDaysTableOffset = is.readShortReverse();

					final int legsOffset = is.readIntReverse();

					final int numLegs = is.readShortReverse();

					final int numChanges = is.readShortReverse();

					/* final long duration = time(is, 0, 0); */is.readShortReverse();

					is.reset();
					is.skipBytes(serviceDaysTablePtr + serviceDaysTableOffset);

					/* final String serviceDaysText = */strings.read(is);

					final int serviceBitBase = is.readShortReverse();
					final int serviceBitLength = is.readShortReverse();

					int tripDayOffset = serviceBitBase * 8;
					for (int i = 0; i < serviceBitLength; i++)
					{
						int serviceBits = is.read();
						if (serviceBits == 0)
						{
							tripDayOffset += 8;
							continue;
						}
						while ((serviceBits & 0x80) == 0)
						{
							serviceBits = serviceBits << 1;
							tripDayOffset++;
						}
						break;
					}

					is.reset();
					is.skipBytes(tripDetailsPtr + tripDetailsIndexOffset + iTrip * 2);
					final int tripDetailsOffset = is.readShortReverse();

					is.reset();
					is.skipBytes(tripDetailsPtr + tripDetailsOffset);
					final int realtimeStatus = is.readShortReverse();

					/* final short delay = */is.readShortReverse();

					/* final int legIndex = */is.readShortReverse();

					is.skipBytes(2); // 0xffff

					/* final int legStatus = */is.readShortReverse();

					is.skipBytes(2); // 0x0000

					String connectionId = null;
					if (tripAttrsPtr != 0)
					{
						is.reset();
						is.skipBytes(tripAttrsPtr + iTrip * 2);
						final int tripAttrsIndex = is.readShortReverse();

						is.reset();
						is.skipBytes(attrsOffset + tripAttrsIndex * 4);
						while (true)
						{
							final String key = strings.read(is);
							if (key == null)
								break;
							else if (key.equals("ConnectionId"))
								connectionId = strings.read(is);
							else
								is.skipBytes(2);
						}
					}

					final List<Trip.Leg> legs = new ArrayList<Trip.Leg>(numLegs);

					for (int iLegs = 0; iLegs < numLegs; iLegs++)
					{
						is.reset();
						is.skipBytes(0x4a + legsOffset + iLegs * 20);

						final long plannedDepartureTime = time(is, resDate, tripDayOffset);
						final Location departureLocation = stations.read(is);

						final long plannedArrivalTime = time(is, resDate, tripDayOffset);
						final Location arrivalLocation = stations.read(is);

						final int type = is.readShortReverse();

						final String lineName = strings.read(is);

						final Position plannedDeparturePosition = normalizePosition(strings.read(is));
						final Position plannedArrivalPosition = normalizePosition(strings.read(is));

						final int legAttrIndex = is.readShortReverse();

						final List<Line.Attr> lineAttrs = new ArrayList<Line.Attr>();
						String lineComment = null;
						boolean lineOnDemand = false;
						for (final String comment : comments.read(is))
						{
							if (comment.startsWith("bf "))
							{
								lineAttrs.add(Line.Attr.WHEEL_CHAIR_ACCESS);
							}
							else if (comment.startsWith("FA ") || comment.startsWith("FB ") || comment.startsWith("FR "))
							{
								lineAttrs.add(Line.Attr.BICYCLE_CARRIAGE);
							}
							else if (comment.startsWith("$R ") || comment.startsWith("ga ") || comment.startsWith("ja ") || comment.startsWith("Vs "))
							{
								lineOnDemand = true;
								lineComment = comment.substring(5);
							}
						}

						is.reset();
						is.skipBytes(attrsOffset + legAttrIndex * 4);
						String directionStr = null;
						int lineClass = 0;
						String lineCategory = null;
						String routingType = null;
						String lineNetwork = null;
						while (true)
						{
							final String key = strings.read(is);
							if (key == null)
								break;
							else if (key.equals("Direction"))
								directionStr = strings.read(is);
							else if (key.equals("Class"))
								lineClass = Integer.parseInt(strings.read(is));
							else if (key.equals("Category"))
								lineCategory = strings.read(is);
							// else if (key.equals("Operator"))
							// lineOperator = strings.read(is);
							else if (key.equals("GisRoutingType"))
								routingType = strings.read(is);
							else if (key.equals("AdminCode"))
								lineNetwork = normalizeLineAdministration(strings.read(is));
							else
								is.skipBytes(2);
						}

						if (lineCategory == null && lineName != null)
							lineCategory = categoryFromName(lineName);

						is.reset();
						is.skipBytes(tripDetailsPtr + tripDetailsOffset + tripDetailsLegOffset + iLegs * tripDetailsLegSize);

						if (tripDetailsLegSize != 16)
							throw new IllegalStateException("unhandled trip details leg size: " + tripDetailsLegSize);

						final long predictedDepartureTime = time(is, resDate, tripDayOffset);
						final long predictedArrivalTime = time(is, resDate, tripDayOffset);
						final Position predictedDeparturePosition = normalizePosition(strings.read(is));
						final Position predictedArrivalPosition = normalizePosition(strings.read(is));

						final int bits = is.readShortReverse();
						final boolean arrivalCancelled = (bits & 0x10) != 0;
						final boolean departureCancelled = (bits & 0x20) != 0;

						is.readShort();

						final int firstStopIndex = is.readShortReverse();

						final int numStops = is.readShortReverse();

						is.reset();
						is.skipBytes(disruptionsPtr);

						String disruptionText = null;

						if (is.readShortReverse() == 1)
						{
							is.reset();
							is.skipBytes(disruptionsPtr + 2 + iTrip * 2);

							int disruptionsOffset = is.readShortReverse();
							while (disruptionsOffset != 0)
							{
								is.reset();
								is.skipBytes(disruptionsPtr + disruptionsOffset);

								strings.read(is); // "0"

								final int disruptionLeg = is.readShortReverse();

								is.skipBytes(2); // bitmaske

								strings.read(is); // start of line
								strings.read(is); // end of line

								strings.read(is); // id
								/* final String disruptionTitle = */strings.read(is);
								final String disruptionShortText = ParserUtils.formatHtml(strings.read(is));

								disruptionsOffset = is.readShortReverse(); // next

								if (iLegs == disruptionLeg)
								{
									final int disruptionAttrsIndex = is.readShortReverse();

									is.reset();
									is.skipBytes(attrsOffset + disruptionAttrsIndex * 4);

									while (true)
									{
										final String key = strings.read(is);
										if (key == null)
											break;
										else if (key.equals("Text"))
											disruptionText = ParserUtils.resolveEntities(strings.read(is));
										else
											is.skipBytes(2);
									}

									if (disruptionShortText != null)
										disruptionText = disruptionShortText;
								}
							}
						}

						List<Stop> intermediateStops = null;

						if (numStops > 0)
						{
							is.reset();
							is.skipBytes(tripDetailsPtr + stopsOffset + firstStopIndex * stopsSize);

							if (stopsSize != 26)
								throw new IllegalStateException("unhandled stops size: " + stopsSize);

							intermediateStops = new ArrayList<Stop>(numStops);

							for (int iStop = 0; iStop < numStops; iStop++)
							{
								final long plannedStopDepartureTime = time(is, resDate, tripDayOffset);
								final Date plannedStopDepartureDate = plannedStopDepartureTime != 0 ? new Date(plannedStopDepartureTime) : null;
								final long plannedStopArrivalTime = time(is, resDate, tripDayOffset);
								final Date plannedStopArrivalDate = plannedStopArrivalTime != 0 ? new Date(plannedStopArrivalTime) : null;
								final Position plannedStopDeparturePosition = normalizePosition(strings.read(is));
								final Position plannedStopArrivalPosition = normalizePosition(strings.read(is));

								is.readInt();

								final long predictedStopDepartureTime = time(is, resDate, tripDayOffset);
								final Date predictedStopDepartureDate = predictedStopDepartureTime != 0 ? new Date(predictedStopDepartureTime) : null;
								final long predictedStopArrivalTime = time(is, resDate, tripDayOffset);
								final Date predictedStopArrivalDate = predictedStopArrivalTime != 0 ? new Date(predictedStopArrivalTime) : null;
								final Position predictedStopDeparturePosition = normalizePosition(strings.read(is));
								final Position predictedStopArrivalPosition = normalizePosition(strings.read(is));

								final int stopBits = is.readShortReverse();
								final boolean stopArrivalCancelled = (stopBits & 0x10) != 0;
								final boolean stopDepartureCancelled = (stopBits & 0x20) != 0;

								is.readShort();

								final Location stopLocation = stations.read(is);

								final boolean validPredictedDate = !dominantPlanStopTime
										|| (plannedStopArrivalDate != null && plannedStopDepartureDate != null);

								final Stop stop = new Stop(stopLocation, plannedStopArrivalDate,
										validPredictedDate ? predictedStopArrivalDate : null, plannedStopArrivalPosition,
										predictedStopArrivalPosition, stopArrivalCancelled, plannedStopDepartureDate,
										validPredictedDate ? predictedStopDepartureDate : null, plannedStopDeparturePosition,
										predictedStopDeparturePosition, stopDepartureCancelled);

								intermediateStops.add(stop);
							}
						}

						final Trip.Leg leg;
						if (type == 1 /* Fussweg */|| type == 3 /* Uebergang */|| type == 4 /* Uebergang */)
						{
							final Trip.Individual.Type individualType;
							if (routingType == null)
								individualType = type == 1 ? Trip.Individual.Type.WALK : Trip.Individual.Type.TRANSFER;
							else if ("FOOT".equals(routingType))
								individualType = Trip.Individual.Type.WALK;
							else if ("BIKE".equals(routingType))
								individualType = Trip.Individual.Type.BIKE;
							else if ("CAR".equals(routingType) || "P+R".equals(routingType))
								individualType = Trip.Individual.Type.CAR;
							else
								throw new IllegalStateException("unknown routingType: " + routingType);

							final Date departureTime = new Date(predictedDepartureTime != 0 ? predictedDepartureTime : plannedDepartureTime);
							final Date arrivalTime = new Date(predictedArrivalTime != 0 ? predictedArrivalTime : plannedArrivalTime);

							final Trip.Leg lastLeg = legs.size() > 0 ? legs.get(legs.size() - 1) : null;
							if (lastLeg != null && lastLeg instanceof Trip.Individual && ((Trip.Individual) lastLeg).type == individualType)
							{
								final Trip.Individual lastIndividualLeg = (Trip.Individual) legs.remove(legs.size() - 1);
								leg = new Trip.Individual(individualType, lastIndividualLeg.departure, lastIndividualLeg.departureTime,
										arrivalLocation, arrivalTime, null, 0);
							}
							else
							{
								leg = new Trip.Individual(individualType, departureLocation, departureTime, arrivalLocation, arrivalTime, null, 0);
							}
						}
						else if (type == 2)
						{
							final Product lineProduct;
							if (lineOnDemand)
								lineProduct = Product.ON_DEMAND;
							else if (lineClass != 0)
								lineProduct = intToProduct(lineClass);
							else
								lineProduct = normalizeType(lineCategory);

							final Line line = newLine(lineNetwork, lineProduct, normalizeLineName(lineName), lineComment,
									lineAttrs.toArray(new Line.Attr[0]));

							final Location direction;
							if (directionStr != null)
							{
								final String[] directionPlaceAndName = splitStationName(directionStr);
								direction = new Location(LocationType.ANY, null, directionPlaceAndName[0], directionPlaceAndName[1]);
							}
							else
							{
								direction = null;
							}

							final Stop departure = new Stop(departureLocation, true, plannedDepartureTime != 0 ? new Date(plannedDepartureTime)
									: null, predictedDepartureTime != 0 ? new Date(predictedDepartureTime) : null, plannedDeparturePosition,
									predictedDeparturePosition, departureCancelled);
							final Stop arrival = new Stop(arrivalLocation, false, plannedArrivalTime != 0 ? new Date(plannedArrivalTime) : null,
									predictedArrivalTime != 0 ? new Date(predictedArrivalTime) : null, plannedArrivalPosition,
									predictedArrivalPosition, arrivalCancelled);

							leg = new Trip.Public(line, direction, departure, arrival, intermediateStops, null, disruptionText);
						}
						else
						{
							throw new IllegalStateException("unhandled type: " + type);
						}
						legs.add(leg);
					}

					final Trip trip = new Trip(connectionId, resDeparture, resArrival, legs, null, null, (int) numChanges);

					if (realtimeStatus != 2) // Verbindung fällt aus
						trips.add(trip);
				}

				// if result is only one single individual leg, don't query for more
				final boolean canQueryMore = trips.size() != 1 || trips.get(0).legs.size() != 1
						|| !(trips.get(0).legs.get(0) instanceof Trip.Individual);

				final QueryTripsResult result = new QueryTripsResult(header, uri, from, via, to, new QueryTripsBinaryContext(requestId, seqNr, ld,
						bis.getCount(), canQueryMore), trips);

				return result;
			}
			else
			{
				log.debug("Hafas error: {}", errorCode);
				if (errorCode == 1)
					throw new SessionExpiredException();
				else if (errorCode == 2)
					// F2: Your search results could not be stored internally.
					throw new SessionExpiredException();
				else if (errorCode == 8)
					return new QueryTripsResult(header, QueryTripsResult.Status.AMBIGUOUS);
				else if (errorCode == 13)
					// IN13: Our booking system is currently being used by too many users at the same time.
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				else if (errorCode == 19)
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				else if (errorCode == 207)
					// H207: Unfortunately your connection request can currently not be processed.
					return new QueryTripsResult(header, QueryTripsResult.Status.SERVICE_DOWN);
				else if (errorCode == 887)
					// H887: Your inquiry was too complex. Please try entering less intermediate stations.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 890)
					// H890: No connections have been found that correspond to your request. It is possible that the
					// requested service does not operate from or to the places you stated on the requested date of
					// travel.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 891)
					// H891: Unfortunately there was no route found. Missing timetable data could be the reason.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 892)
					// H892: Your inquiry was too complex. Please try entering less intermediate stations.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 899)
					// H899: there was an unsuccessful or incomplete search due to a timetable change.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 900)
					// Unsuccessful or incomplete search (timetable change)
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 9220)
					// H9220: Nearby to the given address stations could not be found.
					return new QueryTripsResult(header, QueryTripsResult.Status.UNRESOLVABLE_ADDRESS);
				else if (errorCode == 9240)
					// H9240: Unfortunately there was no route found. Perhaps your start or destination is not served at
					// all
					// or with the selected means of transport on the required date/time.
					return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
				else if (errorCode == 9260)
					// H9260: Unknown departure station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
				else if (errorCode == 9280)
					// H9280: Unknown intermediate station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_VIA);
				else if (errorCode == 9300)
					// H9300: Unknown arrival station
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
				else if (errorCode == 9320)
					// The input is incorrect or incomplete
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				else if (errorCode == 9360)
					// H9360: Unfortunately your connection request can currently not be processed.
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				else if (errorCode == 9380)
					// H9380: Dep./Arr./Intermed. or equivalent station defined more than once
					return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
				else if (errorCode == 895)
					// H895: Departure/Arrival are too near
					return new QueryTripsResult(header, QueryTripsResult.Status.TOO_CLOSE);
				else
					throw new IllegalStateException("error " + errorCode + " on " + uri);
			}
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private Location location(final LittleEndianDataInputStream is, final StringTable strings) throws IOException
	{
		final String name = strings.read(is);
		is.readShort();
		final int type = is.readShortReverse();
		final int lon = is.readIntReverse();
		final int lat = is.readIntReverse();

		if (type == 1)
		{
			final String[] placeAndName = splitStationName(name);
			return new Location(LocationType.STATION, null, lat, lon, placeAndName[0], placeAndName[1]);
		}
		else if (type == 2)
		{
			final String[] placeAndName = splitAddress(name);
			return new Location(LocationType.ADDRESS, null, lat, lon, placeAndName[0], placeAndName[1]);
		}
		else if (type == 3)
		{
			final String[] placeAndName = splitPOI(name);
			return new Location(LocationType.POI, null, lat, lon, placeAndName[0], placeAndName[1]);
		}
		else
		{
			throw new IllegalStateException("unknown type: " + type + "  " + name);
		}
	}

	private long date(final LittleEndianDataInputStream is) throws IOException
	{
		final int days = is.readShortReverse();

		final Calendar date = new GregorianCalendar(timeZone);
		date.clear();
		date.set(Calendar.YEAR, 1980);
		date.set(Calendar.DAY_OF_YEAR, days);

		return date.getTimeInMillis();
	}

	private long time(final LittleEndianDataInputStream is, final long baseDate, final int dayOffset) throws IOException
	{
		final int value = is.readShortReverse();
		if (value == 0xffff)
			return 0;

		final int hours = value / 100;
		final int minutes = value % 100;

		if (minutes < 0 || minutes > 60)
			throw new IllegalStateException("minutes out of range: " + minutes);

		final Calendar time = new GregorianCalendar(timeZone);

		time.setTimeInMillis(baseDate);
		if (time.get(Calendar.HOUR) != 0 || time.get(Calendar.MINUTE) != 0)
			throw new IllegalStateException("baseDate not on date boundary: " + baseDate);

		time.add(Calendar.DAY_OF_YEAR, dayOffset);

		time.set(Calendar.HOUR, hours);
		time.set(Calendar.MINUTE, minutes);

		return time.getTimeInMillis();
	}

	private static class StringTable
	{
		private Charset encoding = Charsets.US_ASCII;
		private final byte[] table;

		public StringTable(final DataInputStream is, final int stringTablePtr, final int length) throws IOException
		{
			is.reset();
			is.skipBytes(stringTablePtr);
			table = new byte[length];
			is.readFully(table);
		}

		public void setEncoding(final Charset encoding)
		{
			this.encoding = encoding;
		}

		public String read(final LittleEndianDataInputStream is) throws IOException
		{
			final int pointer = is.readShortReverse();
			if (pointer == 0)
				return null;
			if (pointer >= table.length)
				throw new IllegalStateException("pointer " + pointer + " cannot exceed strings table size " + table.length);

			final InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(table, pointer, table.length - pointer), encoding);

			try
			{
				final StringBuilder builder = new StringBuilder();

				int c;
				while ((c = reader.read()) != 0)
					builder.append((char) c);

				return builder.toString().trim();
			}
			finally
			{
				reader.close();
			}
		}
	}

	private static class CommentTable
	{
		private final StringTable strings;
		private final byte[] table;

		public CommentTable(final DataInputStream is, final int commentTablePtr, final int length, final StringTable strings) throws IOException
		{
			is.reset();
			is.skipBytes(commentTablePtr);
			table = new byte[length];
			is.readFully(table);

			this.strings = strings;
		}

		public String[] read(final LittleEndianDataInputStream is) throws IOException
		{
			final int pointer = is.readShortReverse();
			if (pointer >= table.length)
				throw new IllegalStateException("pointer " + pointer + " cannot exceed comments table size " + table.length);

			final LittleEndianDataInputStream commentsInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(table, pointer,
					table.length - pointer));

			try
			{
				final int numComments = commentsInputStream.readShortReverse();
				final String[] comments = new String[numComments];

				for (int i = 0; i < numComments; i++)
					comments[i] = strings.read(commentsInputStream);

				return comments;
			}
			finally
			{
				commentsInputStream.close();
			}
		}
	}

	private class StationTable
	{
		private final StringTable strings;
		private final byte[] table;

		public StationTable(final DataInputStream is, final int stationTablePtr, final int length, final StringTable strings) throws IOException
		{
			is.reset();
			is.skipBytes(stationTablePtr);
			table = new byte[length];
			is.readFully(table);

			this.strings = strings;
		}

		private Location read(final LittleEndianDataInputStream is) throws IOException
		{
			final int index = is.readShortReverse();
			final int ptr = index * 14;
			if (ptr >= table.length)
				throw new IllegalStateException("pointer " + ptr + " cannot exceed stations table size " + table.length);

			final LittleEndianDataInputStream stationInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(table, ptr, 14));

			try
			{
				final String[] placeAndName = splitStationName(strings.read(stationInputStream));
				final int id = stationInputStream.readIntReverse();
				final int lon = stationInputStream.readIntReverse();
				final int lat = stationInputStream.readIntReverse();

				return new Location(LocationType.STATION, id != 0 ? Integer.toString(id) : null, lat, lon, placeAndName[0], placeAndName[1]);
			}
			finally
			{
				stationInputStream.close();
			}
		}
	}

	private static final Pattern P_POSITION_PLATFORM = Pattern.compile("Gleis\\s*(.*)\\s*", Pattern.CASE_INSENSITIVE);

	private Position normalizePosition(final String position)
	{
		if (position == null)
			return null;

		final Matcher m = P_POSITION_PLATFORM.matcher(position);
		if (!m.matches())
			return parsePosition(position);

		return parsePosition(m.group(1));
	}

	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxLocations) throws IOException
	{
		if (location.hasLocation())
			return nearbyLocationsByCoordinate(types, location.lat, location.lon, maxDistance, maxLocations);
		else if (location.type == LocationType.STATION && location.hasId())
			return nearbyStationsById(location.id, maxDistance);
		else
			throw new IllegalArgumentException("cannot handle: " + location);
	}

	protected final NearbyLocationsResult nearbyLocationsByCoordinate(final EnumSet<LocationType> types, final int lat, final int lon,
			final int maxDistance, final int maxLocations) throws IOException
	{
		if (types.contains(LocationType.STATION))
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			appendJsonNearbyStationsParameters(uri, lat, lon, maxDistance, maxLocations);

			return jsonNearbyLocations(uri.toString());
		}
		else if (types.contains(LocationType.POI))
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			appendJsonNearbyPOIsParameters(uri, lat, lon, maxDistance, maxLocations);

			return jsonNearbyLocations(uri.toString());
		}
		else
		{
			return new NearbyLocationsResult(null, Collections.<Location> emptyList());
		}
	}

	protected NearbyLocationsResult nearbyStationsById(final String id, final int maxDistance) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		appendXmlNearbyStationsParameters(uri, id);

		return xmlNearbyStations(uri.toString());
	}

	protected final void appendXmlNearbyStationsParameters(final StringBuilder uri, final String stationId)
	{
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&input=").append(normalizeStationId(stationId));
		uri.append("&sTI=1&start=yes&hcount=0&L=vs_java3");
		if (clientType != null)
			uri.append("&clientType=").append(ParserUtils.urlEncode(clientType));
	}

	private static final Pattern P_XML_NEARBY_STATIONS_COARSE = Pattern.compile("\\G<\\s*St\\s*(.*?)/?>(?:\n|\\z)", Pattern.DOTALL);
	private static final Pattern P_XML_NEARBY_STATIONS_FINE = Pattern.compile("" //
			+ "evaId=\"(\\d+)\"\\s*" // id
			+ "name=\"([^\"]+)\".*?" // name
			+ "(?:x=\"(\\d+)\"\\s*)?" // x
			+ "(?:y=\"(\\d+)\"\\s*)?" // y
	, Pattern.DOTALL);
	private static final Pattern P_XML_NEARBY_STATIONS_MESSAGES = Pattern.compile("<Err code=\"([^\"]*)\" text=\"([^\"]*)\"");

	protected final NearbyLocationsResult xmlNearbyStations(final String uri) throws IOException
	{
		// scrape page
		final CharSequence page = httpClient.get(uri);

		final List<Location> stations = new ArrayList<Location>();

		// parse page
		final Matcher mMessage = P_XML_NEARBY_STATIONS_MESSAGES.matcher(page);
		if (mMessage.find())
		{
			final String code = mMessage.group(1);
			final String text = mMessage.group(2);

			if (code.equals("H730")) // Your input is not valid
				return new NearbyLocationsResult(null, NearbyLocationsResult.Status.INVALID_ID);
			if (code.equals("H890")) // No trains in result
				return new NearbyLocationsResult(null, stations);
			throw new IllegalArgumentException("unknown error " + code + ", " + text);
		}

		final Matcher mCoarse = P_XML_NEARBY_STATIONS_COARSE.matcher(page);
		while (mCoarse.find())
		{
			final Matcher mFine = P_XML_NEARBY_STATIONS_FINE.matcher(mCoarse.group(1));
			if (mFine.matches())
			{
				final String parsedId = mFine.group(1);

				final String parsedName = ParserUtils.resolveEntities(mFine.group(2)).trim();

				final int parsedLon;
				final int parsedLat;
				if (mFine.group(3) != null && mFine.group(4) != null)
				{
					parsedLon = Integer.parseInt(mFine.group(3));
					parsedLat = Integer.parseInt(mFine.group(4));
				}
				else
				{
					parsedLon = 0;
					parsedLat = 0;
				}

				final String[] placeAndName = splitStationName(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, placeAndName[0], placeAndName[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
			}
		}

		return new NearbyLocationsResult(null, stations);
	}

	protected void appendJsonNearbyStationsParameters(final StringBuilder uri, final int lat, final int lon, final int maxDistance,
			final int maxStations)
	{
		uri.append('y');
		uri.append("?performLocating=2&tpl=stop2json");
		uri.append("&look_stopclass=").append(allProductsInt());
		uri.append("&look_nv=get_stopweight|yes");
		// get_shortjson|yes
		// get_lines|yes
		// combinemode|2
		// density|80
		// get_stopweight|yes
		// get_infotext|yes
		uri.append("&look_x=").append(lon);
		uri.append("&look_y=").append(lat);
		uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
		uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
	}

	protected void appendJsonNearbyPOIsParameters(final StringBuilder uri, final int lat, final int lon, final int maxDistance, final int maxStations)
	{
		uri.append('y');
		uri.append("?performLocating=4&tpl=poi2json");
		uri.append("&look_pois="); // all categories
		uri.append("&look_x=").append(lon);
		uri.append("&look_y=").append(lat);
		uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
		uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
	}

	protected final NearbyLocationsResult jsonNearbyLocations(final String uri) throws IOException
	{
		final CharSequence page = httpClient.get(uri, jsonNearbyLocationsEncoding);

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final int error = head.getInt("error");
			if (error == 0)
			{
				final List<Location> locations = new LinkedList<Location>();

				final JSONArray aStops = head.optJSONArray("stops");
				if (aStops != null)
				{
					final int nStops = aStops.length();

					for (int i = 0; i < nStops; i++)
					{
						final JSONObject stop = aStops.optJSONObject(i);
						final String id = stop.getString("extId");
						// final String name = ParserUtils.resolveEntities(stop.getString("name"));
						final String urlname = ParserUtils.urlDecode(stop.getString("urlname"), jsonNearbyLocationsEncoding);
						final int lat = stop.getInt("y");
						final int lon = stop.getInt("x");
						final int prodclass = stop.optInt("prodclass", -1);
						final int stopWeight = stop.optInt("stopweight", -1);

						if (stopWeight != 0)
						{
							final String[] placeAndName = splitStationName(urlname);
							final Set<Product> products = prodclass != -1 ? intToProducts(prodclass) : null;
							locations.add(new Location(LocationType.STATION, id, lat, lon, placeAndName[0], placeAndName[1], products));
						}
					}
				}

				final JSONArray aPOIs = head.optJSONArray("pois");
				if (aPOIs != null)
				{
					final int nPOIs = aPOIs.length();

					for (int i = 0; i < nPOIs; i++)
					{
						final JSONObject poi = aPOIs.optJSONObject(i);
						final String id = poi.getString("extId");
						// final String name = ParserUtils.resolveEntities(stop.getString("name"));
						final String urlname = ParserUtils.urlDecode(poi.getString("urlname"), jsonNearbyLocationsEncoding);
						final int lat = poi.getInt("y");
						final int lon = poi.getInt("x");

						final String[] placeAndName = splitPOI(urlname);
						locations.add(new Location(LocationType.POI, id, lat, lon, placeAndName[0], placeAndName[1]));
					}
				}

				return new NearbyLocationsResult(null, locations);
			}
			else
			{
				log.debug("Hafas error: {}", error);
				if (error == 2)
					return new NearbyLocationsResult(null, NearbyLocationsResult.Status.SERVICE_DOWN);
				else
					throw new RuntimeException("unknown error: " + error);
			}
		}
		catch (final JSONException x)
		{
			x.printStackTrace();
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
	}

	protected void setHtmlNearbyStationsPattern(final Pattern htmlNearbyStationsPattern)
	{
		this.htmlNearbyStationsPattern = htmlNearbyStationsPattern;
	}

	private Pattern htmlNearbyStationsPattern = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);

	private static final Pattern P_HTML_NEARBY_FINE_COORDS = Pattern
			.compile("REQMapRoute0\\.Location0\\.X=(-?\\d+)&(?:amp;)?REQMapRoute0\\.Location0\\.Y=(-?\\d+)&");
	private static final Pattern P_HTML_NEARBY_STATIONS_FINE_LOCATION = Pattern.compile("[\\?&;]input=(\\d+)&[^\"]*\">([^<]*)<");
	private static final Pattern P_HTML_NEARBY_STATIONS_MESSAGES = Pattern.compile("(Ihre Eingabe kann nicht interpretiert werden)");

	protected final NearbyLocationsResult htmlNearbyStations(final String uri) throws IOException
	{
		final List<Location> stations = new ArrayList<Location>();

		final CharSequence page = httpClient.get(uri);
		String oldZebra = null;

		final Matcher mCoarse = htmlNearbyStationsPattern.matcher(page);

		final Matcher mMessage = P_HTML_NEARBY_STATIONS_MESSAGES.matcher(page);
		if (mMessage.find())
		{
			if (mMessage.group(1) != null)
				return new NearbyLocationsResult(null, NearbyLocationsResult.Status.INVALID_ID);
		}

		while (mCoarse.find())
		{
			final String zebra = mCoarse.group(1);
			if (oldZebra != null && zebra.equals(oldZebra))
				throw new IllegalArgumentException("missed row? last:" + zebra);
			else
				oldZebra = zebra;

			final Matcher mFineLocation = P_HTML_NEARBY_STATIONS_FINE_LOCATION.matcher(mCoarse.group(2));

			if (mFineLocation.find())
			{
				int parsedLon = 0;
				int parsedLat = 0;
				final String parsedId = mFineLocation.group(1);
				final String parsedName = ParserUtils.resolveEntities(mFineLocation.group(2));

				final Matcher mFineCoords = P_HTML_NEARBY_FINE_COORDS.matcher(mCoarse.group(2));

				if (mFineCoords.find())
				{
					parsedLon = Integer.parseInt(mFineCoords.group(1));
					parsedLat = Integer.parseInt(mFineCoords.group(2));
				}

				final String[] placeAndName = splitStationName(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, placeAndName[0], placeAndName[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		return new NearbyLocationsResult(null, stations);
	}

	private static final Pattern P_LINE_SBAHN = Pattern.compile("SN?\\d*");
	private static final Pattern P_LINE_TRAM = Pattern.compile("STR\\w{0,5}");
	private static final Pattern P_LINE_BUS = Pattern.compile("BUS\\w{0,5}");
	private static final Pattern P_LINE_TAXI = Pattern.compile("TAX\\w{0,5}");

	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// Intercity
		if ("EC".equals(ucType)) // EuroCity
			return Product.HIGH_SPEED_TRAIN;
		if ("EN".equals(ucType)) // EuroNight
			return Product.HIGH_SPEED_TRAIN;
		if ("D".equals(ucType)) // EuroNight, Sitzwagenabteil
			return Product.HIGH_SPEED_TRAIN;
		if ("EIC".equals(ucType)) // Ekspres InterCity, Polen
			return Product.HIGH_SPEED_TRAIN;
		if ("ICE".equals(ucType)) // InterCityExpress
			return Product.HIGH_SPEED_TRAIN;
		if ("IC".equals(ucType)) // InterCity
			return Product.HIGH_SPEED_TRAIN;
		if ("ICT".equals(ucType)) // InterCity
			return Product.HIGH_SPEED_TRAIN;
		if ("ICN".equals(ucType)) // InterCityNight
			return Product.HIGH_SPEED_TRAIN;
		if ("ICD".equals(ucType)) // Intercity direkt Amsterdam-Breda
			return Product.HIGH_SPEED_TRAIN;
		if ("CNL".equals(ucType)) // CityNightLine
			return Product.HIGH_SPEED_TRAIN;
		if ("MT".equals(ucType)) // Schnee-Express
			return Product.HIGH_SPEED_TRAIN;
		if ("OEC".equals(ucType)) // ÖBB-EuroCity
			return Product.HIGH_SPEED_TRAIN;
		if ("OIC".equals(ucType)) // ÖBB-InterCity
			return Product.HIGH_SPEED_TRAIN;
		if ("RJ".equals(ucType)) // RailJet, Österreichische Bundesbahnen
			return Product.HIGH_SPEED_TRAIN;
		if ("WB".equals(ucType)) // westbahn
			return Product.HIGH_SPEED_TRAIN;
		if ("THA".equals(ucType)) // Thalys
			return Product.HIGH_SPEED_TRAIN;
		if ("TGV".equals(ucType)) // Train à Grande Vitesse
			return Product.HIGH_SPEED_TRAIN;
		if ("DNZ".equals(ucType)) // Nacht-Schnellzug
			return Product.HIGH_SPEED_TRAIN;
		if ("AIR".equals(ucType)) // Generic Flight
			return Product.HIGH_SPEED_TRAIN;
		if ("ECB".equals(ucType)) // EC, Verona-München
			return Product.HIGH_SPEED_TRAIN;
		if ("LYN".equals(ucType)) // Dänemark
			return Product.HIGH_SPEED_TRAIN;
		if ("NZ".equals(ucType)) // Schweden, Nacht
			return Product.HIGH_SPEED_TRAIN;
		if ("INZ".equals(ucType)) // Nacht
			return Product.HIGH_SPEED_TRAIN;
		if ("RHI".equals(ucType)) // ICE
			return Product.HIGH_SPEED_TRAIN;
		if ("RHT".equals(ucType)) // TGV
			return Product.HIGH_SPEED_TRAIN;
		if ("TGD".equals(ucType)) // TGV
			return Product.HIGH_SPEED_TRAIN;
		if ("IRX".equals(ucType)) // IC
			return Product.HIGH_SPEED_TRAIN;
		if ("ES".equals(ucType)) // Eurostar Italia
			return Product.HIGH_SPEED_TRAIN;
		if ("EST".equals(ucType)) // Eurostar Frankreich
			return Product.HIGH_SPEED_TRAIN;
		if ("EM".equals(ucType)) // Euromed, Barcelona-Alicante, Spanien
			return Product.HIGH_SPEED_TRAIN;
		if ("A".equals(ucType)) // Spain, Highspeed
			return Product.HIGH_SPEED_TRAIN;
		if ("AVE".equals(ucType)) // Alta Velocidad Española, Spanien
			return Product.HIGH_SPEED_TRAIN;
		if ("ARC".equals(ucType)) // Arco (Renfe), Spanien
			return Product.HIGH_SPEED_TRAIN;
		if ("ALS".equals(ucType)) // Alaris (Renfe), Spanien
			return Product.HIGH_SPEED_TRAIN;
		if ("ATR".equals(ucType)) // Altaria (Renfe), Spanien
			return Product.REGIONAL_TRAIN;
		if ("TAL".equals(ucType)) // Talgo, Spanien
			return Product.HIGH_SPEED_TRAIN;
		if ("TLG".equals(ucType)) // Spanien, Madrid
			return Product.HIGH_SPEED_TRAIN;
		if ("HOT".equals(ucType)) // Spanien, Nacht
			return Product.HIGH_SPEED_TRAIN;
		if ("X2".equals(ucType)) // X2000 Neigezug, Schweden
			return Product.HIGH_SPEED_TRAIN;
		if ("X".equals(ucType)) // InterConnex
			return Product.HIGH_SPEED_TRAIN;
		if ("FYR".equals(ucType)) // Fyra, Amsterdam-Schiphol-Rotterdam
			return Product.HIGH_SPEED_TRAIN;
		if ("FYRA".equals(ucType)) // Fyra, Amsterdam-Schiphol-Rotterdam
			return Product.HIGH_SPEED_TRAIN;
		if ("SC".equals(ucType)) // SuperCity, Tschechien
			return Product.HIGH_SPEED_TRAIN;
		if ("LE".equals(ucType)) // LEO Express, Prag
			return Product.HIGH_SPEED_TRAIN;
		if ("FLUG".equals(ucType))
			return Product.HIGH_SPEED_TRAIN;
		if ("TLK".equals(ucType)) // Tanie Linie Kolejowe, Polen
			return Product.HIGH_SPEED_TRAIN;
		if ("EIP".equals(ucType)) // Express Intercity Premium
			return Product.HIGH_SPEED_TRAIN;
		if ("INT".equals(ucType)) // Zürich-Brüssel - Budapest-Istanbul
			return Product.HIGH_SPEED_TRAIN;
		if ("HKX".equals(ucType)) // Hamburg-Koeln-Express
			return Product.HIGH_SPEED_TRAIN;

		// Regional
		if ("ZUG".equals(ucType)) // Generic Train
			return Product.REGIONAL_TRAIN;
		if ("R".equals(ucType)) // Generic Regional Train
			return Product.REGIONAL_TRAIN;
		if ("DPN".equals(ucType)) // Dritter Personen Nahverkehr
			return Product.REGIONAL_TRAIN;
		if ("RB".equals(ucType)) // RegionalBahn
			return Product.REGIONAL_TRAIN;
		if ("RE".equals(ucType)) // RegionalExpress
			return Product.REGIONAL_TRAIN;
		if ("DB".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("IR".equals(ucType)) // Interregio
			return Product.REGIONAL_TRAIN;
		if ("IRE".equals(ucType)) // Interregio Express
			return Product.REGIONAL_TRAIN;
		if ("HEX".equals(ucType)) // Harz-Berlin-Express, Veolia
			return Product.REGIONAL_TRAIN;
		if ("WFB".equals(ucType)) // Westfalenbahn
			return Product.REGIONAL_TRAIN;
		if ("RT".equals(ucType)) // RegioTram
			return Product.REGIONAL_TRAIN;
		if ("REX".equals(ucType)) // RegionalExpress, Österreich
			return Product.REGIONAL_TRAIN;
		if ("OS".equals(ucType)) // Osobný vlak, Slovakia oder Osobní vlak, Czech Republic
			return Product.REGIONAL_TRAIN;
		if ("SP".equals(ucType)) // Spěšný vlak, Czech Republic
			return Product.REGIONAL_TRAIN;
		if ("EZ".equals(ucType)) // ÖBB ErlebnisBahn
			return Product.REGIONAL_TRAIN;
		if ("ARZ".equals(ucType)) // Auto-Reisezug Brig - Iselle di Trasquera
			return Product.REGIONAL_TRAIN;
		if ("OE".equals(ucType)) // Ostdeutsche Eisenbahn
			return Product.REGIONAL_TRAIN;
		if ("MR".equals(ucType)) // Märkische Regionalbahn
			return Product.REGIONAL_TRAIN;
		if ("PE".equals(ucType)) // Prignitzer Eisenbahn GmbH
			return Product.REGIONAL_TRAIN;
		if ("NE".equals(ucType)) // NEB Betriebsgesellschaft mbH
			return Product.REGIONAL_TRAIN;
		if ("MRB".equals(ucType)) // Mitteldeutsche Regiobahn
			return Product.REGIONAL_TRAIN;
		if ("ERB".equals(ucType)) // eurobahn (Keolis Deutschland)
			return Product.REGIONAL_TRAIN;
		if ("HLB".equals(ucType)) // Hessische Landesbahn
			return Product.REGIONAL_TRAIN;
		if ("VIA".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("HSB".equals(ucType)) // Harzer Schmalspurbahnen
			return Product.REGIONAL_TRAIN;
		if ("OSB".equals(ucType)) // Ortenau-S-Bahn
			return Product.REGIONAL_TRAIN;
		if ("VBG".equals(ucType)) // Vogtlandbahn
			return Product.REGIONAL_TRAIN;
		if ("AKN".equals(ucType)) // AKN Eisenbahn AG
			return Product.REGIONAL_TRAIN;
		if ("OLA".equals(ucType)) // Ostseeland Verkehr
			return Product.REGIONAL_TRAIN;
		if ("UBB".equals(ucType)) // Usedomer Bäderbahn
			return Product.REGIONAL_TRAIN;
		if ("PEG".equals(ucType)) // Prignitzer Eisenbahn
			return Product.REGIONAL_TRAIN;
		if ("NWB".equals(ucType)) // NordWestBahn
			return Product.REGIONAL_TRAIN;
		if ("CAN".equals(ucType)) // cantus Verkehrsgesellschaft
			return Product.REGIONAL_TRAIN;
		if ("BRB".equals(ucType)) // ABELLIO Rail
			return Product.REGIONAL_TRAIN;
		if ("SBB".equals(ucType)) // Schweizerische Bundesbahnen
			return Product.REGIONAL_TRAIN;
		if ("VEC".equals(ucType)) // vectus Verkehrsgesellschaft
			return Product.REGIONAL_TRAIN;
		if ("TLX".equals(ucType)) // Trilex (Vogtlandbahn)
			return Product.REGIONAL_TRAIN;
		if ("TL".equals(ucType)) // Trilex (Vogtlandbahn)
			return Product.REGIONAL_TRAIN;
		if ("HZL".equals(ucType)) // Hohenzollerische Landesbahn
			return Product.REGIONAL_TRAIN;
		if ("ABR".equals(ucType)) // Bayerische Regiobahn
			return Product.REGIONAL_TRAIN;
		if ("CB".equals(ucType)) // City Bahn Chemnitz
			return Product.REGIONAL_TRAIN;
		if ("WEG".equals(ucType)) // Württembergische Eisenbahn-Gesellschaft
			return Product.REGIONAL_TRAIN;
		if ("NEB".equals(ucType)) // Niederbarnimer Eisenbahn
			return Product.REGIONAL_TRAIN;
		if ("ME".equals(ucType)) // metronom Eisenbahngesellschaft
			return Product.REGIONAL_TRAIN;
		if ("MER".equals(ucType)) // metronom regional
			return Product.REGIONAL_TRAIN;
		if ("ALX".equals(ucType)) // Arriva-Länderbahn-Express
			return Product.REGIONAL_TRAIN;
		if ("EB".equals(ucType)) // Erfurter Bahn
			return Product.REGIONAL_TRAIN;
		if ("EBX".equals(ucType)) // Erfurter Bahn
			return Product.REGIONAL_TRAIN;
		if ("VEN".equals(ucType)) // Rhenus Veniro
			return Product.REGIONAL_TRAIN;
		if ("BOB".equals(ucType)) // Bayerische Oberlandbahn
			return Product.REGIONAL_TRAIN;
		if ("SBS".equals(ucType)) // Städtebahn Sachsen
			return Product.REGIONAL_TRAIN;
		if ("SES".equals(ucType)) // Städtebahn Sachsen Express
			return Product.REGIONAL_TRAIN;
		if ("EVB".equals(ucType)) // Eisenbahnen und Verkehrsbetriebe Elbe-Weser
			return Product.REGIONAL_TRAIN;
		if ("STB".equals(ucType)) // Süd-Thüringen-Bahn
			return Product.REGIONAL_TRAIN;
		if ("AG".equals(ucType)) // Ingolstadt-Landshut
			return Product.REGIONAL_TRAIN;
		if ("PRE".equals(ucType)) // Pressnitztalbahn
			return Product.REGIONAL_TRAIN;
		if ("DBG".equals(ucType)) // Döllnitzbahn GmbH
			return Product.REGIONAL_TRAIN;
		if ("SHB".equals(ucType)) // Schleswig-Holstein-Bahn
			return Product.REGIONAL_TRAIN;
		if ("NOB".equals(ucType)) // Nord-Ostsee-Bahn
			return Product.REGIONAL_TRAIN;
		if ("RTB".equals(ucType)) // Rurtalbahn
			return Product.REGIONAL_TRAIN;
		if ("BLB".equals(ucType)) // Berchtesgadener Land Bahn
			return Product.REGIONAL_TRAIN;
		if ("NBE".equals(ucType)) // Nordbahn Eisenbahngesellschaft
			return Product.REGIONAL_TRAIN;
		if ("SOE".equals(ucType)) // Sächsisch-Oberlausitzer Eisenbahngesellschaft
			return Product.REGIONAL_TRAIN;
		if ("SDG".equals(ucType)) // Sächsische Dampfeisenbahngesellschaft
			return Product.REGIONAL_TRAIN;
		if ("VE".equals(ucType)) // Lutherstadt Wittenberg
			return Product.REGIONAL_TRAIN;
		if ("DAB".equals(ucType)) // Daadetalbahn
			return Product.REGIONAL_TRAIN;
		if ("WTB".equals(ucType)) // Wutachtalbahn e.V.
			return Product.REGIONAL_TRAIN;
		if ("BE".equals(ucType)) // Grensland-Express
			return Product.REGIONAL_TRAIN;
		if ("ARR".equals(ucType)) // Ostfriesland
			return Product.REGIONAL_TRAIN;
		if ("HTB".equals(ucType)) // Hörseltalbahn
			return Product.REGIONAL_TRAIN;
		if ("FEG".equals(ucType)) // Freiberger Eisenbahngesellschaft
			return Product.REGIONAL_TRAIN;
		if ("NEG".equals(ucType)) // Norddeutsche Eisenbahngesellschaft Niebüll
			return Product.REGIONAL_TRAIN;
		if ("RBG".equals(ucType)) // Regental Bahnbetriebs GmbH
			return Product.REGIONAL_TRAIN;
		if ("MBB".equals(ucType)) // Mecklenburgische Bäderbahn Molli
			return Product.REGIONAL_TRAIN;
		if ("VEB".equals(ucType)) // Vulkan-Eifel-Bahn Betriebsgesellschaft
			return Product.REGIONAL_TRAIN;
		if ("LEO".equals(ucType)) // Chiemgauer Lokalbahn
			return Product.REGIONAL_TRAIN;
		if ("VX".equals(ucType)) // Vogtland Express
			return Product.REGIONAL_TRAIN;
		if ("MSB".equals(ucType)) // Mainschleifenbahn
			return Product.REGIONAL_TRAIN;
		if ("P".equals(ucType)) // Kasbachtalbahn
			return Product.REGIONAL_TRAIN;
		if ("ÖBA".equals(ucType)) // Öchsle-Bahn Betriebsgesellschaft
			return Product.REGIONAL_TRAIN;
		if ("KTB".equals(ucType)) // Kandertalbahn
			return Product.REGIONAL_TRAIN;
		if ("ERX".equals(ucType)) // erixx
			return Product.REGIONAL_TRAIN;
		if ("ATZ".equals(ucType)) // Autotunnelzug
			return Product.REGIONAL_TRAIN;
		if ("ATB".equals(ucType)) // Autoschleuse Tauernbahn
			return Product.REGIONAL_TRAIN;
		if ("CAT".equals(ucType)) // City Airport Train
			return Product.REGIONAL_TRAIN;
		if ("EXTRA".equals(ucType) || "EXT".equals(ucType)) // Extrazug
			return Product.REGIONAL_TRAIN;
		if ("KD".equals(ucType)) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
			return Product.REGIONAL_TRAIN;
		if ("KM".equals(ucType)) // Koleje Mazowieckie
			return Product.REGIONAL_TRAIN;
		if ("EX".equals(ucType)) // Polen
			return Product.REGIONAL_TRAIN;
		if ("PCC".equals(ucType)) // PCC Rail, Polen
			return Product.REGIONAL_TRAIN;
		if ("ZR".equals(ucType)) // ZSR (Slovakian Republic Railways)
			return Product.REGIONAL_TRAIN;
		if ("RNV".equals(ucType)) // Rhein-Neckar-Verkehr GmbH
			return Product.REGIONAL_TRAIN;
		if ("DWE".equals(ucType)) // Dessau-Wörlitzer Eisenbahn
			return Product.REGIONAL_TRAIN;
		if ("BKB".equals(ucType)) // Buckower Kleinbahn
			return Product.REGIONAL_TRAIN;
		if ("GEX".equals(ucType)) // Glacier Express
			return Product.REGIONAL_TRAIN;
		if ("M".equals(ucType)) // Meridian
			return Product.REGIONAL_TRAIN;
		if ("WBA".equals(ucType)) // Waldbahn
			return Product.REGIONAL_TRAIN;
		if ("BEX".equals(ucType)) // Bernina Express
			return Product.REGIONAL_TRAIN;
		if ("VAE".equals(ucType)) // Voralpen-Express
			return Product.REGIONAL_TRAIN;
		if ("OPB".equals(ucType)) // oberpfalzbahn
			return Product.REGIONAL_TRAIN;
		if ("OPX".equals(ucType)) // oberpfalz-express
			return Product.REGIONAL_TRAIN;
		if ("TER".equals(ucType)) // Transport express régional
			return Product.REGIONAL_TRAIN;
		if ("ENO".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("THU".equals(ucType)) // Thurbo AG
			return Product.REGIONAL_TRAIN;
		if ("GW".equals(ucType)) // gwtr.cz
			return Product.REGIONAL_TRAIN;

		// Suburban Trains
		if (P_LINE_SBAHN.matcher(ucType).matches()) // Generic (Night) S-Bahn
			return Product.SUBURBAN_TRAIN;
		if ("S-BAHN".equals(ucType))
			return Product.SUBURBAN_TRAIN;
		if ("BSB".equals(ucType)) // Breisgau S-Bahn
			return Product.SUBURBAN_TRAIN;
		if ("SWE".equals(ucType)) // Südwestdeutsche Verkehrs-AG, Ortenau-S-Bahn
			return Product.SUBURBAN_TRAIN;
		if ("RER".equals(ucType)) // Réseau Express Régional, Frankreich
			return Product.SUBURBAN_TRAIN;
		if ("WKD".equals(ucType)) // Warszawska Kolej Dojazdowa (Warsaw Suburban Railway)
			return Product.SUBURBAN_TRAIN;
		if ("SKM".equals(ucType)) // Szybka Kolej Miejska Tricity
			return Product.SUBURBAN_TRAIN;
		if ("SKW".equals(ucType)) // Szybka Kolej Miejska Warschau
			return Product.SUBURBAN_TRAIN;

		// Subway
		if ("U".equals(ucType)) // Generic U-Bahn
			return Product.SUBWAY;
		if ("MET".equals(ucType))
			return Product.SUBWAY;
		if ("METRO".equals(ucType))
			return Product.SUBWAY;

		// Tram
		if (P_LINE_TRAM.matcher(ucType).matches()) // Generic Tram
			return Product.TRAM;
		if ("NFT".equals(ucType)) // Niederflur-Tram
			return Product.TRAM;
		if ("TRAM".equals(ucType))
			return Product.TRAM;
		if ("TRA".equals(ucType))
			return Product.TRAM;
		if ("WLB".equals(ucType)) // Wiener Lokalbahnen
			return Product.TRAM;
		if ("STRWLB".equals(ucType)) // Wiener Lokalbahnen
			return Product.TRAM;
		if ("SCHW-B".equals(ucType)) // Schwebebahn, gilt als "Straßenbahn besonderer Bauart"
			return Product.TRAM;

		// Bus
		if (P_LINE_BUS.matcher(ucType).matches()) // Generic Bus
			return Product.BUS;
		if ("NFB".equals(ucType)) // Niederflur-Bus
			return Product.BUS;
		if ("SEV".equals(ucType)) // Schienen-Ersatz-Verkehr
			return Product.BUS;
		if ("BUSSEV".equals(ucType)) // Schienen-Ersatz-Verkehr
			return Product.BUS;
		if ("BSV".equals(ucType)) // Bus SEV
			return Product.BUS;
		if ("FB".equals(ucType)) // Fernbus? Luxemburg-Saarbrücken
			return Product.BUS;
		if ("EXB".equals(ucType)) // Expressbus München-Prag?
			return Product.BUS;
		if ("ICB".equals(ucType)) // ÖBB ICBus
			return Product.BUS;
		if ("TRO".equals(ucType)) // Trolleybus
			return Product.BUS;
		if ("RFB".equals(ucType)) // Rufbus
			return Product.BUS;
		if ("RUF".equals(ucType)) // Rufbus
			return Product.BUS;
		if (P_LINE_TAXI.matcher(ucType).matches()) // Generic Taxi
			return Product.BUS;
		if ("RFT".equals(ucType)) // Ruftaxi
			return Product.BUS;
		if ("LT".equals(ucType)) // Linien-Taxi
			return Product.BUS;
		if ("NB".equals(ucType)) // Nachtbus Zürich
			return Product.BUS;

		// Phone
		if (ucType.startsWith("AST")) // Anruf-Sammel-Taxi
			return Product.ON_DEMAND;
		if (ucType.startsWith("ALT")) // Anruf-Linien-Taxi
			return Product.ON_DEMAND;
		if (ucType.startsWith("BUXI")) // Bus-Taxi (Schweiz)
			return Product.ON_DEMAND;
		if ("TB".equals(ucType)) // Taxi-Bus?
			return Product.ON_DEMAND;

		// Ferry
		if ("SCHIFF".equals(ucType))
			return Product.FERRY;
		if ("FÄHRE".equals(ucType))
			return Product.FERRY;
		if ("FÄH".equals(ucType))
			return Product.FERRY;
		if ("FAE".equals(ucType))
			return Product.FERRY;
		if ("SCH".equals(ucType)) // Schiff
			return Product.FERRY;
		if ("AS".equals(ucType)) // SyltShuttle, eigentlich Autoreisezug
			return Product.FERRY;
		if ("KAT".equals(ucType)) // Katamaran, e.g. Friedrichshafen - Konstanz
			return Product.FERRY;
		if ("BAT".equals(ucType)) // Boots Anlege Terminal?
			return Product.FERRY;
		if ("BAV".equals(ucType)) // Boots Anlege?
			return Product.FERRY;

		// Cable Car
		if ("SEILBAHN".equals(ucType))
			return Product.CABLECAR;
		if ("SB".equals(ucType)) // Seilbahn
			return Product.CABLECAR;
		if ("ZAHNR".equals(ucType)) // Zahnradbahn, u.a. Zugspitzbahn
			return Product.CABLECAR;
		if ("GB".equals(ucType)) // Gondelbahn
			return Product.CABLECAR;
		if ("LB".equals(ucType)) // Luftseilbahn
			return Product.CABLECAR;
		if ("FUN".equals(ucType)) // Funiculaire (Standseilbahn)
			return Product.CABLECAR;
		if ("SL".equals(ucType)) // Sessel-Lift
			return Product.CABLECAR;

		// Unknown product
		if ("E".equals(ucType))
			return null;

		throw new IllegalStateException("cannot normalize type '" + type + "'");
	}

	private static final Pattern P_NORMALIZE_LINE_NAME_BUS = Pattern.compile("bus\\s+(.*)", Pattern.CASE_INSENSITIVE);
	protected static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zßÄÅäáàâåéèêíìîÖöóòôÜüúùûØ/]+)[\\s-]*([^#]*).*");

	protected String normalizeLineName(final String lineName)
	{
		final Matcher mBus = P_NORMALIZE_LINE_NAME_BUS.matcher(lineName);
		if (mBus.matches())
			return mBus.group(1);

		final Matcher m = P_NORMALIZE_LINE.matcher(lineName);
		if (m.matches())
			return m.group(1) + m.group(2);

		return lineName;
	}

	private static final Pattern P_NORMALIZE_LINE_ADMINISTRATION = Pattern.compile("([^_]*)_*");

	private final String normalizeLineAdministration(final String administration)
	{
		if (administration == null)
			return null;

		final Matcher m = P_NORMALIZE_LINE_ADMINISTRATION.matcher(administration);
		if (m.find())
			return m.group(1);
		else
			return administration;
	}

	private static final Pattern P_CATEGORY_FROM_NAME = Pattern.compile("([A-Za-zßÄÅäáàâåéèêíìîÖöóòôÜüúùûØ]+).*");

	protected final String categoryFromName(final String lineName)
	{
		final Matcher m = P_CATEGORY_FROM_NAME.matcher(lineName);
		if (m.matches())
			return m.group(1);
		else
			return lineName;
	}

	private static final Pattern P_NORMALIZE_LINE_BUS = Pattern.compile("(?:Bus|BUS)\\s*(.*)");
	private static final Pattern P_NORMALIZE_LINE_TRAM = Pattern.compile("(?:Tram|Tra|Str|STR)\\s*(.*)");

	protected Line parseLine(final String type, final String normalizedName, final boolean wheelchairAccess)
	{
		if (normalizedName != null)
		{
			final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(normalizedName);
			if (mBus.matches())
				return newLine(Product.BUS, mBus.group(1), null);

			final Matcher mTram = P_NORMALIZE_LINE_TRAM.matcher(normalizedName);
			if (mTram.matches())
				return newLine(Product.TRAM, mTram.group(1), null);
		}

		final Product normalizedType = normalizeType(type);

		final Line.Attr[] attrs;
		if (wheelchairAccess)
			attrs = new Line.Attr[] { Line.Attr.WHEEL_CHAIR_ACCESS };
		else
			attrs = new Line.Attr[0];

		if (normalizedName != null)
		{
			final Matcher m = P_NORMALIZE_LINE.matcher(normalizedName);
			final String strippedLine = m.matches() ? m.group(1) + m.group(2) : normalizedName;

			return newLine(normalizedType, strippedLine, null, attrs);
		}
		else
		{
			return newLine(normalizedType, null, null, attrs);
		}
	}

	protected static final Pattern P_NORMALIZE_LINE_AND_TYPE = Pattern.compile("([^#]*)#(.*)");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");

	protected static final Pattern P_LINE_RUSSIA = Pattern
			.compile("\\d{3}(?:AJ|BJ|CJ|DJ|EJ|FJ|GJ|IJ|KJ|LJ|NJ|MJ|OJ|RJ|SJ|TJ|UJ|VJ|ZJ|CH|KH|ZH|EI|JA|JI|MZ|SH|SZ|PC|Y)");

	protected Line parseLineAndType(final String lineAndType)
	{
		final Matcher mLineAndType = P_NORMALIZE_LINE_AND_TYPE.matcher(lineAndType);
		if (mLineAndType.matches())
		{
			final String number = mLineAndType.group(1);
			final String type = mLineAndType.group(2);

			if (type.length() == 0)
			{
				if (number.length() == 0)
					return newLine(null, null, null);
				if (P_NORMALIZE_LINE_NUMBER.matcher(number).matches())
					return newLine(null, number, null);
				if (P_LINE_RUSSIA.matcher(number).matches())
					return newLine(Product.REGIONAL_TRAIN, number, null);
			}
			else
			{
				final Product normalizedType = normalizeType(type);
				if (normalizedType != null)
				{
					if (normalizedType == Product.BUS)
					{
						final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(number);
						if (mBus.matches())
							return newLine(Product.BUS, mBus.group(1), null);
					}

					if (normalizedType == Product.TRAM)
					{
						final Matcher mTram = P_NORMALIZE_LINE_TRAM.matcher(number);
						if (mTram.matches())
							return newLine(Product.TRAM, mTram.group(1), null);
					}
				}

				return newLine(normalizedType, number.replaceAll("\\s+", ""), null);
			}

			throw new IllegalStateException("cannot normalize type '" + type + "' number '" + number + "' line#type '" + lineAndType + "'");
		}

		throw new IllegalStateException("cannot normalize line#type '" + lineAndType + "'");
	}

	protected Line newLine(final Product product, final String normalizedName, final String comment, final Line.Attr... attrs)
	{
		return newLine(null, product, normalizedName, comment, attrs);
	}

	protected Line newLine(final String network, final Product product, final String normalizedName, final String comment, final Line.Attr... attrs)
	{
		if (attrs.length == 0)
		{
			return new Line(null, network, product, normalizedName, lineStyle(network, product, normalizedName), comment);
		}
		else
		{
			final Set<Line.Attr> attrSet = new HashSet<Line.Attr>();
			for (final Line.Attr attr : attrs)
				attrSet.add(attr);
			return new Line(null, network, product, normalizedName, lineStyle(network, product, normalizedName), attrSet, comment);
		}
	}
}
