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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.StringReplaceReader;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider extends AbstractNetworkProvider
{
	protected final static String SERVER_PRODUCT = "hafas";

	private static final String DEFAULT_ENCODING = "ISO-8859-1";
	private static final String PROD = "hafas";
	private static final int NUM_CONNECTIONS = 6;

	private final String apiUri;
	private final int numProductBits;
	private final String accessId;
	private final String jsonEncoding;
	private final String xmlMlcResEncoding;

	public AbstractHafasProvider(final String apiUri, final int numProductBits, final String accessId, final String jsonEncoding,
			final String xmlMlcResEncoding)
	{
		this.apiUri = apiUri;
		this.numProductBits = numProductBits;
		this.accessId = accessId;
		this.jsonEncoding = jsonEncoding;
		this.xmlMlcResEncoding = xmlMlcResEncoding;
	}

	public AbstractHafasProvider(final String apiUri, final int numProductBits, final String accessId)
	{
		this.apiUri = apiUri;
		this.numProductBits = numProductBits;
		this.accessId = accessId;
		this.jsonEncoding = DEFAULT_ENCODING;
		this.xmlMlcResEncoding = DEFAULT_ENCODING;
	}

	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("CET");
	}

	protected final String allProductsString()
	{
		final StringBuilder allProducts = new StringBuilder(numProductBits);
		for (int i = 0; i < numProductBits; i++)
			allProducts.append('1');
		return allProducts.toString();
	}

	protected final int allProductsInt()
	{
		return (1 << numProductBits) - 1;
	}

	protected char intToProduct(final int value)
	{
		return 0;
	}

	protected abstract void setProductBits(StringBuilder productBits, char product);

	private static final Pattern P_SPLIT_ADDRESS = Pattern.compile("(\\d{4,5}\\s+[^,]+),\\s+(.*)");

	protected String[] splitPlaceAndName(final String name)
	{
		final Matcher matcher = P_SPLIT_ADDRESS.matcher(name);
		if (matcher.matches())
			return new String[] { matcher.group(1), matcher.group(2) };
		else
			return new String[] { null, name };
	}

	private final String wrap(final String request)
	{
		return "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" //
				+ "<ReqC ver=\"1.1\" prod=\"" + PROD + "\" lang=\"DE\"" + (accessId != null ? " accessId=\"" + accessId + "\"" : "") + ">" //
				+ request //
				+ "</ReqC>";
	}

	private final Location parseStation(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Station".equals(type))
		{
			final String name = pp.getAttributeValue(null, "name").trim();
			final int id = Integer.parseInt(pp.getAttributeValue(null, "externalStationNr"));
			final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
			final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));

			final String[] placeAndName = splitPlaceAndName(name);
			return new Location(LocationType.STATION, id, y, x, placeAndName[0], placeAndName[1]);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private static final Location parsePoi(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Poi".equals(type))
		{
			String name = pp.getAttributeValue(null, "name").trim();
			if (name.equals("unknown"))
				name = null;
			final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
			final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));
			return new Location(LocationType.POI, 0, y, x, null, name);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private final Location parseAddress(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Address".equals(type))
		{
			String name = pp.getAttributeValue(null, "name").trim();
			if (name.equals("unknown"))
				name = null;
			final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
			final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));

			final String[] placeAndName = splitPlaceAndName(name);
			return new Location(LocationType.ADDRESS, 0, y, x, placeAndName[0], placeAndName[1]);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private static final Location parseReqLoc(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("ReqLoc".equals(type))
		{
			XmlPullUtil.requireAttr(pp, "type", "ADR");
			final String name = pp.getAttributeValue(null, "output").trim();
			return new Location(LocationType.ADDRESS, 0, null, name);
		}
		throw new IllegalStateException("cannot handle: " + type);
	}

	private static final String parsePlatform(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "Platform");
		XmlPullUtil.require(pp, "Text");
		final String position = XmlPullUtil.text(pp).trim();
		XmlPullUtil.exit(pp, "Platform");

		if (position.length() == 0)
			return null;
		else
			return position;
	}

	public List<Location> xmlLocValReq(final CharSequence constraint) throws IOException
	{
		final String request = "<LocValReq id=\"req\" maxNr=\"20\"><ReqLoc match=\"" + constraint + "\" type=\"ALLTYPE\"/></LocValReq>";

		// System.out.println(ParserUtils.scrape(apiUri, true, wrap(request), null, false));

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), null, 3);

			final List<Location> results = new ArrayList<Location>();

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertResC(pp);
			XmlPullUtil.enter(pp);

			XmlPullUtil.require(pp, "LocValRes");
			XmlPullUtil.requireAttr(pp, "id", "req");
			XmlPullUtil.enter(pp);

			while (pp.getEventType() == XmlPullParser.START_TAG)
			{
				final String tag = pp.getName();
				if ("Station".equals(tag))
					results.add(parseStation(pp));
				else if ("Poi".equals(tag))
					results.add(parsePoi(pp));
				else if ("Address".equals(tag))
					results.add(parseAddress(pp));
				else if ("ReqLoc".equals(tag))
					/* results.add(parseReqLoc(pp)) */;
				else
					System.out.println("cannot handle tag: " + tag);

				XmlPullUtil.next(pp);
			}

			XmlPullUtil.exit(pp);

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_AJAX_GET_STOPS_JSON = Pattern.compile("SLs\\.sls\\s*=\\s*(.*?);\\s*SLs\\.showSuggestion\\(\\);", Pattern.DOTALL);
	private static final Pattern P_AJAX_GET_STOPS_ID = Pattern.compile(".*?@L=(\\d+)@.*?");

	protected final List<Location> jsonGetStops(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri, false, null, jsonEncoding, null);

		final Matcher mJson = P_AJAX_GET_STOPS_JSON.matcher(page);
		if (mJson.matches())
		{
			final String json = mJson.group(1);
			final List<Location> results = new ArrayList<Location>();

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
						int localId = 0;
						final Matcher m = P_AJAX_GET_STOPS_ID.matcher(suggestion.getString("id"));
						if (m.matches())
							localId = Integer.parseInt(m.group(1));

						if (type == 1) // station
						{
							final String[] placeAndName = splitPlaceAndName(value);
							results.add(new Location(LocationType.STATION, localId, lat, lon, placeAndName[0], placeAndName[1]));
						}
						else if (type == 2) // address
						{
							final String[] placeAndName = splitPlaceAndName(value);
							results.add(new Location(LocationType.ADDRESS, 0, lat, lon, placeAndName[0], placeAndName[1]));
						}
						else if (type == 4) // poi
						{
							results.add(new Location(LocationType.POI, localId, lat, lon, null, value));
						}
						else if (type == 71) // strange (VBN)
						{
							// TODO don't know what to do
						}
						else if (type == 87) // strange (ZTM)
						{
							// TODO don't know what to do
						}
						else if (type == 128) // strange (SEPTA)
						{
							// TODO don't know what to do
						}
						else
						{
							throw new IllegalStateException("unknown type " + type + " on " + uri);
						}
					}
				}

				return results;
			}
			catch (final JSONException x)
			{
				x.printStackTrace();
				throw new RuntimeException("cannot parse: '" + json + "' on " + uri, x);
			}
		}
		else
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri);
		}
	}

	private static final Pattern P_XML_MLC_REQ_ID = Pattern.compile(".*?@L=(\\d+)@.*?");
	private static final Pattern P_XML_MLC_REQ_LONLAT = Pattern.compile(".*?@X=(-?\\d+)@Y=(-?\\d+)@.*?");

	protected final List<Location> xmlMLcReq(final CharSequence constraint) throws IOException
	{
		final String request = "<MLcReq><MLc n=\"" + constraint + "?\" t=\"ALLTYPE\" /></MLcReq>";

		// ParserUtils.printXml(ParserUtils.scrape(apiUri, true, wrap(request), mlcResEncoding, false));

		InputStream is = null;

		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), null, 3);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, xmlMlcResEncoding);

			final List<Location> results = new ArrayList<Location>();

			assertResC(pp);
			XmlPullUtil.enter(pp, "ResC");
			XmlPullUtil.enter(pp, "MLcRes");

			while (XmlPullUtil.test(pp, "MLc"))
			{
				final String t = XmlPullUtil.attr(pp, "t");
				final LocationType type;
				if ("ST".equals(t))
					type = LocationType.STATION;
				else if ("POI".equals(t))
					type = LocationType.POI;
				else if ("ADR".equals(t))
					type = LocationType.ADDRESS;
				else
					throw new IllegalStateException("cannot handle: '" + t + "'");

				final int id;
				final String i = pp.getAttributeValue(null, "i");
				if (i != null)
				{
					final Matcher iMatcherId = P_XML_MLC_REQ_ID.matcher(i);
					if (!iMatcherId.matches())
						throw new IllegalStateException("cannot parse id: '" + i + "'");
					id = Integer.parseInt(iMatcherId.group(1));
				}
				else
				{
					id = 0;
				}

				final String name = XmlPullUtil.attr(pp, "n");

				final String r = pp.getAttributeValue(null, "r");
				final Matcher iMatcherLonLat = P_XML_MLC_REQ_LONLAT.matcher(i != null ? i : r);
				if (!iMatcherLonLat.matches())
					throw new IllegalStateException("cannot parse lon/lat: '" + i + "' or '" + r + "'");
				final int lon = Integer.parseInt(iMatcherLonLat.group(1));
				final int lat = Integer.parseInt(iMatcherLonLat.group(2));

				final String[] placeAndName = splitPlaceAndName(name);
				results.add(new Location(type, id, lat, lon, placeAndName[0], placeAndName[1]));

				XmlPullUtil.next(pp);
			}

			XmlPullUtil.exit(pp, "MLcRes");
			XmlPullUtil.exit(pp, "ResC");

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_XML_QUERY_DEPARTURES_DELAY = Pattern.compile("(?:-|k\\.A\\.?|cancel|\\+?\\s*(\\d+))");

	protected QueryDeparturesResult xmlQueryDepartures(final String uri, final int stationId) throws IOException
	{
		StringReplaceReader reader = null;

		try
		{
			reader = new StringReplaceReader(new InputStreamReader(ParserUtils.scrapeInputStream(uri), DEFAULT_ENCODING), "Ringbahn ->",
					"Ringbahn -&gt;");
			reader.replace("Ringbahn <-", "Ringbahn &lt;-");

			// System.out.println(uri);
			// ParserUtils.printFromReader(reader);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(reader);

			pp.nextTag();

			final ResultHeader header = new ResultHeader(SERVER_PRODUCT);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);
			final List<Departure> departures = new ArrayList<Departure>(8);

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				final String text = XmlPullUtil.attr(pp, "text");

				if (code.equals("H730")) // Your input is not valid
					return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
				if (code.equals("H890"))
				{
					result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId), Collections
							.<Departure> emptyList(), null));
					return result;
				}
				throw new IllegalArgumentException("unknown error " + code + ", " + text);
			}

			if (XmlPullUtil.test(pp, "StationTable"))
			{
				XmlPullUtil.enter(pp, "StationTable");
				if (pp.getEventType() == XmlPullParser.TEXT)
					pp.nextTag();
			}

			while (XmlPullUtil.test(pp, "Journey"))
			{
				final String fpTime = XmlPullUtil.attr(pp, "fpTime");
				final String fpDate = XmlPullUtil.attr(pp, "fpDate");
				final String delay = XmlPullUtil.attr(pp, "delay");
				// TODO e_delay
				final String platform = pp.getAttributeValue(null, "platform");
				// TODO newpl
				final String targetLoc = pp.getAttributeValue(null, "targetLoc");
				// TODO hafasname
				final String dirnr = pp.getAttributeValue(null, "dirnr");
				final String prod = XmlPullUtil.attr(pp, "prod");
				final String classStr = pp.getAttributeValue(null, "class");
				final String dir = pp.getAttributeValue(null, "dir");
				final String capacityStr = pp.getAttributeValue(null, "capacity");
				final String depStation = pp.getAttributeValue(null, "depStation");
				final String delayReason = pp.getAttributeValue(null, "delayReason");
				// TODO is_reachable
				// TODO disableTrainInfo

				if (depStation == null)
				{
					final Calendar plannedTime = new GregorianCalendar(timeZone());
					plannedTime.clear();
					ParserUtils.parseEuropeanTime(plannedTime, fpTime);
					if (fpDate.length() == 8)
						ParserUtils.parseGermanDate(plannedTime, fpDate);
					else if (fpDate.length() == 10)
						ParserUtils.parseIsoDate(plannedTime, fpDate);
					else
						throw new IllegalStateException("cannot parse: '" + fpDate + "'");

					final Calendar predictedTime;
					if (delay != null)
					{
						final Matcher m = P_XML_QUERY_DEPARTURES_DELAY.matcher(delay);
						if (m.matches())
						{
							if (m.group(1) != null)
							{
								predictedTime = new GregorianCalendar(timeZone());
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

					final String position = platform != null ? "Gl. " + ParserUtils.resolveEntities(platform) : null;

					final String destination;
					if (dir != null)
						destination = dir.trim();
					else if (targetLoc != null)
						destination = targetLoc.trim();
					else
						destination = null;

					final int destinationId;
					if (dirnr != null)
						destinationId = Integer.parseInt(dirnr);
					else
						destinationId = 0;

					final String lineStr;
					if (classStr != null)
					{
						final char classChar = intToProduct(Integer.parseInt(classStr));
						final Matcher m = P_NORMALIZE_LINE.matcher(prod);
						if (m.matches())
							lineStr = classChar + m.group(1) + m.group(2);
						else
							lineStr = classChar + prod;
					}
					else
					{
						lineStr = normalizeLine(prod);
					}
					final Line line = new Line(null, lineStr, lineStr != null ? lineColors(lineStr) : null);

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
							position, destinationId, destination, capacity, message);
					departures.add(departure);
				}

				if (pp.isEmptyElementTag())
				{
					XmlPullUtil.next(pp);
				}
				else
				{
					XmlPullUtil.enter(pp, "Journey");
					XmlPullUtil.exit(pp, "Journey");
				}

				if (pp.getEventType() == XmlPullParser.TEXT)
					pp.nextTag();
			}

			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId), departures, null));
			return result;
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}

	public QueryConnectionsResult queryConnections(Location from, Location via, Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final ResultHeader header = new ResultHeader(SERVER_PRODUCT);

		if (from.type == LocationType.ANY || (from.type == LocationType.ADDRESS && !from.hasLocation()))
		{
			final List<Location> autocompletes = autocompleteStations(from.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(header, autocompletes, null, null);
			from = autocompletes.get(0);
		}

		if (via != null && (via.type == LocationType.ANY || (via.type == LocationType.ADDRESS && !via.hasLocation())))
		{
			final List<Location> autocompletes = autocompleteStations(via.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(header, null, autocompletes, null);
			via = autocompletes.get(0);
		}

		if (to.type == LocationType.ANY || (to.type == LocationType.ADDRESS && !to.hasLocation()))
		{
			final List<Location> autocompletes = autocompleteStations(to.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(header, null, null, autocompletes);
			to = autocompletes.get(0);
		}

		final Calendar c = new GregorianCalendar(timeZone());
		c.setTime(date);

		final StringBuilder productsStr = new StringBuilder(numProductBits);
		if (products != null)
		{
			for (int i = 0; i < numProductBits; i++)
				productsStr.append('0');
			for (final char p : products.toCharArray())
				setProductBits(productsStr, p);
		}
		else
		{
			productsStr.append(allProductsString());
		}

		final StringBuilder request = new StringBuilder("<ConReq>");

		request.append("<Start>").append(locationXml(from));
		request.append("<Prod prod=\"").append(productsStr).append("\" bike=\"0\" couchette=\"0\" direct=\"0\" sleeper=\"0\"/>");
		request.append("</Start>");
		if (via != null)
			request.append("<Via>").append(locationXml(via)).append("</Via>");
		request.append("<Dest>").append(locationXml(to)).append("</Dest>");
		request.append("<ReqT a=\"").append(dep ? 0 : 1).append("\" date=\"")
				.append(String.format("%04d.%02d.%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)))
				.append("\" time=\"").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) + "\"/>");
		request.append("<RFlags b=\"0\" chExtension=\"").append(walkSpeed == WalkSpeed.SLOW ? 50 : 0).append("\" f=\"").append(NUM_CONNECTIONS)
				.append("\" sMode=\"N\"/>");
		request.append("</ConReq>");

		return queryConnections(request.toString(), from, via, to);
	}

	public QueryConnectionsResult queryMoreConnections(final String context) throws IOException
	{
		final String request = "<ConScrReq scr=\"F\" nrCons=\"" + NUM_CONNECTIONS + "\">" //
				+ "<ConResCtxt>" + context + "</ConResCtxt>" //
				+ "</ConScrReq>";

		return queryConnections(request, null, null, null);
	}

	private QueryConnectionsResult queryConnections(final String request, final Location from, final Location via, final Location to)
			throws IOException
	{
		// System.out.println(request);
		// ParserUtils.printXml(ParserUtils.scrape(apiUri, true, wrap(request), null, null));

		InputStream is = null;

		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), null, 3);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertResC(pp);
			final String product = XmlPullUtil.attr(pp, "prod").split(" ")[0];
			final ResultHeader header = new ResultHeader(SERVER_PRODUCT, product, 0, null);
			XmlPullUtil.enter(pp, "ResC");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("I3")) // Input: date outside of the timetable period
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.INVALID_DATE);
				if (code.equals("F1")) // Spool: Error reading the spoolfile
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.SERVICE_DOWN);
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			XmlPullUtil.enter(pp, "ConRes");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("K9380") || code.equals("K895")) // Departure/Arrival are too near
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.TOO_CLOSE);
				if (code.equals("K9220")) // Nearby to the given address stations could not be found
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.UNRESOLVABLE_ADDRESS);
				if (code.equals("K9240")) // Internal error
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.SERVICE_DOWN);
				if (code.equals("K9260")) // Departure station does not exist
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS);
				if (code.equals("K890")) // No connections found
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS);
				if (code.equals("K891")) // No route found (try entering an intermediate station)
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.NO_CONNECTIONS);
				if (code.equals("K899")) // An error occurred
					return new QueryConnectionsResult(header, QueryConnectionsResult.Status.SERVICE_DOWN);
				// if (code.equals("K1:890")) // Unsuccessful or incomplete search (direction: forward)
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			// workaround for broken firstConDiffersFromReqDate="true" as text node
			if (pp.getEventType() == XmlPullParser.TEXT)
				pp.nextTag();

			final String context;
			if (XmlPullUtil.test(pp, "ConResCtxt"))
				context = XmlPullUtil.text(pp);
			else
				context = null;

			XmlPullUtil.enter(pp, "ConnectionList");

			final List<Connection> connections = new ArrayList<Connection>();

			while (XmlPullUtil.test(pp, "Connection"))
			{
				final String id = XmlPullUtil.attr(pp, "id");

				XmlPullUtil.enter(pp, "Connection");
				while (pp.getName().equals("RtStateList"))
					XmlPullUtil.next(pp);
				XmlPullUtil.enter(pp, "Overview");

				XmlPullUtil.require(pp, "Date");
				final Calendar currentDate = new GregorianCalendar(timeZone());
				currentDate.clear();
				parseDate(currentDate, XmlPullUtil.text(pp));
				XmlPullUtil.enter(pp, "Departure");
				XmlPullUtil.enter(pp, "BasicStop");
				while (pp.getName().equals("StAttrList"))
					XmlPullUtil.next(pp);
				final Location departure = parseLocation(pp);
				XmlPullUtil.enter(pp, "Dep");
				XmlPullUtil.exit(pp, "Dep");
				final int[] capacity;
				if (XmlPullUtil.test(pp, "StopPrognosis"))
				{
					XmlPullUtil.enter(pp, "StopPrognosis");
					if (XmlPullUtil.test(pp, "Arr"))
						XmlPullUtil.next(pp);
					if (XmlPullUtil.test(pp, "Dep"))
						XmlPullUtil.next(pp);
					XmlPullUtil.enter(pp, "Status");
					XmlPullUtil.exit(pp, "Status");
					if (XmlPullUtil.test(pp, "Capacity1st"))
					{
						final int capacity1st = Integer.parseInt(XmlPullUtil.text(pp));
						XmlPullUtil.require(pp, "Capacity2nd");
						final int capacity2nd = Integer.parseInt(XmlPullUtil.text(pp));
						capacity = new int[] { capacity1st, capacity2nd };
					}
					else
					{
						capacity = null;
					}
					XmlPullUtil.exit(pp, "StopPrognosis");
				}
				else
				{
					capacity = null;
				}
				XmlPullUtil.exit(pp, "BasicStop");
				XmlPullUtil.exit(pp, "Departure");

				XmlPullUtil.enter(pp, "Arrival");
				XmlPullUtil.enter(pp, "BasicStop");
				while (pp.getName().equals("StAttrList"))
					XmlPullUtil.next(pp);
				final Location arrival = parseLocation(pp);
				XmlPullUtil.exit(pp, "BasicStop");
				XmlPullUtil.exit(pp, "Arrival");

				XmlPullUtil.exit(pp, "Overview");

				final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);
				Date firstDepartureTime = null;
				Date lastArrivalTime = null;

				XmlPullUtil.enter(pp, "ConSectionList");

				final Calendar time = new GregorianCalendar(timeZone());

				while (XmlPullUtil.test(pp, "ConSection"))
				{
					XmlPullUtil.enter(pp, "ConSection");

					// departure
					XmlPullUtil.enter(pp, "Departure");
					XmlPullUtil.enter(pp, "BasicStop");
					while (pp.getName().equals("StAttrList"))
						XmlPullUtil.next(pp);
					final Location sectionDeparture = parseLocation(pp);
					XmlPullUtil.enter(pp, "Dep");
					XmlPullUtil.require(pp, "Time");
					time.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(time, XmlPullUtil.text(pp));
					final Date departureTime = time.getTime();
					final String departurePos = parsePlatform(pp);
					XmlPullUtil.exit(pp, "Dep");

					XmlPullUtil.exit(pp, "BasicStop");
					XmlPullUtil.exit(pp, "Departure");

					// journey
					final Line line;
					Location destination = null;
					int min = 0;

					List<Stop> intermediateStops = null;

					final String tag = pp.getName();
					if (tag.equals("Journey"))
					{
						XmlPullUtil.enter(pp, "Journey");
						while (pp.getName().equals("JHandle"))
							XmlPullUtil.next(pp);
						XmlPullUtil.enter(pp, "JourneyAttributeList");
						String name = null;
						String category = null;
						String shortCategory = null;
						String longCategory = null;
						while (XmlPullUtil.test(pp, "JourneyAttribute"))
						{
							XmlPullUtil.enter(pp, "JourneyAttribute");
							XmlPullUtil.require(pp, "Attribute");
							final String attrName = pp.getAttributeValue(null, "type");
							XmlPullUtil.enter(pp);
							final Map<String, String> attributeVariants = parseAttributeVariants(pp);
							XmlPullUtil.exit(pp);
							XmlPullUtil.exit(pp, "JourneyAttribute");

							if ("NAME".equals(attrName))
							{
								name = attributeVariants.get("NORMAL");
							}
							else if ("CATEGORY".equals(attrName))
							{
								shortCategory = attributeVariants.get("SHORT");
								category = attributeVariants.get("NORMAL");
								longCategory = attributeVariants.get("LONG");
							}
							else if ("DIRECTION".equals(attrName))
							{
								destination = new Location(LocationType.ANY, 0, null, attributeVariants.get("NORMAL"));
							}
						}
						XmlPullUtil.exit(pp, "JourneyAttributeList");

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
								if (location.id != sectionDeparture.id)
								{
									if (XmlPullUtil.test(pp, "Arr"))
										XmlPullUtil.next(pp);
									if (XmlPullUtil.test(pp, "Dep"))
									{
										XmlPullUtil.enter(pp, "Dep");
										XmlPullUtil.require(pp, "Time");
										time.setTimeInMillis(currentDate.getTimeInMillis());
										parseTime(time, XmlPullUtil.text(pp));
										final String position = parsePlatform(pp);
										XmlPullUtil.exit(pp, "Dep");

										intermediateStops.add(new Stop(location, position, time.getTime()));
									}
								}
								XmlPullUtil.exit(pp, "BasicStop");
							}

							XmlPullUtil.exit(pp, "PassList");
						}

						XmlPullUtil.exit(pp, "Journey");

						if (category == null)
							category = shortCategory;

						line = parseLine(category, name);
					}
					else if (tag.equals("Walk") || tag.equals("Transfer") || tag.equals("GisRoute"))
					{
						XmlPullUtil.enter(pp);
						XmlPullUtil.enter(pp, "Duration");
						XmlPullUtil.require(pp, "Time");
						min = parseDuration(XmlPullUtil.text(pp).substring(3, 8));
						XmlPullUtil.exit(pp);
						XmlPullUtil.exit(pp);

						line = null;
					}
					else
					{
						throw new IllegalStateException("cannot handle: " + pp.getName());
					}

					// arrival
					XmlPullUtil.enter(pp, "Arrival");
					XmlPullUtil.enter(pp, "BasicStop");
					while (pp.getName().equals("StAttrList"))
						XmlPullUtil.next(pp);
					final Location sectionArrival = parseLocation(pp);
					XmlPullUtil.enter(pp, "Arr");
					XmlPullUtil.require(pp, "Time");
					time.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(time, XmlPullUtil.text(pp));
					final Date arrivalTime = time.getTime();
					final String arrivalPos = parsePlatform(pp);
					XmlPullUtil.exit(pp, "Arr");

					XmlPullUtil.exit(pp, "BasicStop");
					XmlPullUtil.exit(pp, "Arrival");

					// remove last intermediate
					final int size = intermediateStops != null ? intermediateStops.size() : 0;
					if (size >= 1)
						if (intermediateStops.get(size - 1).location.id == sectionArrival.id)
							intermediateStops.remove(size - 1);

					XmlPullUtil.exit(pp, "ConSection");

					if (min == 0 || line != null)
					{
						parts.add(new Connection.Trip(line, destination, departureTime, departurePos, sectionDeparture, arrivalTime, arrivalPos,
								sectionArrival, intermediateStops, null));
					}
					else
					{
						if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
						{
							final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
							parts.add(new Connection.Footway(lastFootway.min + min, lastFootway.departure, sectionArrival, null));
						}
						else
						{
							parts.add(new Connection.Footway(min, sectionDeparture, sectionArrival, null));
						}
					}

					if (firstDepartureTime == null)
						firstDepartureTime = departureTime;
					lastArrivalTime = arrivalTime;
				}

				XmlPullUtil.exit(pp, "ConSectionList");

				XmlPullUtil.exit(pp, "Connection");

				connections.add(new Connection(id, null, firstDepartureTime, lastArrivalTime, departure, arrival, parts, null, capacity));
			}

			XmlPullUtil.exit(pp);

			return new QueryConnectionsResult(header, null, from, via, to, context, connections);
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private final Location parseLocation(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		Location location;
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
			XmlPullUtil.enter(pp);
			final String value;
			if (XmlPullUtil.test(pp, "Text"))
				value = XmlPullUtil.text(pp).trim();
			else
				value = null;
			XmlPullUtil.exit(pp);

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

	private static final Pattern P_DURATION = Pattern.compile("(\\d+):(\\d{2})");

	private static final int parseDuration(final CharSequence str)
	{
		final Matcher m = P_DURATION.matcher(str);
		if (m.matches())
			return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
		else
			throw new IllegalArgumentException("cannot parse duration: '" + str + "'");
	}

	private static final String locationXml(final Location location)
	{
		if (location.type == LocationType.STATION && location.hasId())
			return "<Station externalId=\"" + location.id + "\" />";
		else if (location.type == LocationType.POI && location.hasLocation())
			return "<Poi type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" />";
		else if (location.type == LocationType.ADDRESS && location.hasLocation())
			return "<Address type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" name=\""
					+ (location.place != null ? location.place + ", " : "") + location.name + "\" />";
		else
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
	}

	protected final String locationId(final Location location)
	{
		final StringBuilder id = new StringBuilder();

		id.append("A=").append(locationType(location));

		if (location.type == LocationType.STATION && location.hasId() && isValidStationId(location.id))
		{
			id.append("@L=").append(location.id);
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
		if (type == LocationType.ADDRESS && location.hasLocation())
			return 16;
		if (type == LocationType.ADDRESS && location.name != null)
			return 2;
		if (type == LocationType.ANY)
			return 255;
		throw new IllegalArgumentException(location.type.toString());
	}

	protected boolean isValidStationId(int id)
	{
		return true;
	}

	public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private static final Pattern P_XML_NEARBY_STATIONS_COARSE = Pattern.compile("\\G<\\s*St\\s*(.*?)/?>(?:\n|\\z)", Pattern.DOTALL);
	private static final Pattern P_XML_NEARBY_STATIONS_FINE = Pattern.compile("" //
			+ "evaId=\"(\\d+)\"\\s*" // id
			+ "name=\"([^\"]+)\".*?" // name
			+ "(?:x=\"(\\d+)\"\\s*)?" // x
			+ "(?:y=\"(\\d+)\"\\s*)?" // y
	, Pattern.DOTALL);
	private static final Pattern P_XML_NEARBY_STATIONS_MESSAGES = Pattern.compile("<Err code=\"([^\"]*)\" text=\"([^\"]*)\"");

	protected final NearbyStationsResult xmlNearbyStations(final String uri) throws IOException
	{
		// scrape page
		final CharSequence page = ParserUtils.scrape(uri);

		final List<Location> stations = new ArrayList<Location>();

		// parse page
		final Matcher mMessage = P_XML_NEARBY_STATIONS_MESSAGES.matcher(page);
		if (mMessage.find())
		{
			final String code = mMessage.group(1);
			final String text = mMessage.group(2);

			if (code.equals("H730")) // Your input is not valid
				return new NearbyStationsResult(null, NearbyStationsResult.Status.INVALID_STATION);
			if (code.equals("H890")) // No trains in result
				return new NearbyStationsResult(null, stations);
			throw new IllegalArgumentException("unknown error " + code + ", " + text);
		}

		final Matcher mCoarse = P_XML_NEARBY_STATIONS_COARSE.matcher(page);
		while (mCoarse.find())
		{
			final Matcher mFine = P_XML_NEARBY_STATIONS_FINE.matcher(mCoarse.group(1));
			if (mFine.matches())
			{
				final int parsedId = Integer.parseInt(mFine.group(1));

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

				final String[] placeAndName = splitPlaceAndName(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, placeAndName[0], placeAndName[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
			}
		}

		return new NearbyStationsResult(null, stations);
	}

	protected final NearbyStationsResult jsonNearbyStations(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri, false, null, jsonEncoding, null);

		final List<Location> stations = new ArrayList<Location>();

		try
		{
			final JSONObject head = new JSONObject(page.toString());
			final JSONArray aStops = head.getJSONArray("stops");

			for (int i = 0; i < aStops.length(); i++)
			{
				final JSONObject stop = aStops.optJSONObject(i);
				final int id = stop.getInt("extId");
				final String name = ParserUtils.resolveEntities(stop.getString("name"));
				final int lat = stop.getInt("y");
				final int lon = stop.getInt("x");
				final int stopWeight = stop.optInt("stopweight", -1);

				if (stopWeight != 0)
				{
					final String[] placeAndName = splitPlaceAndName(name);
					stations.add(new Location(LocationType.STATION, id, lat, lon, placeAndName[0], placeAndName[1]));
				}
			}
		}
		catch (final JSONException x)
		{
			x.printStackTrace();
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}

		return new NearbyStationsResult(null, stations);
	}

	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE_COORDS = Pattern
			.compile("REQMapRoute0\\.Location0\\.X=(-?\\d+)&(?:amp;)?REQMapRoute0\\.Location0\\.Y=(-?\\d+)&");
	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("[\\?&]input=(\\d+)&[^\"]*\">([^<]*)<");

	protected final NearbyStationsResult htmlNearbyStations(final String uri) throws IOException
	{
		final List<Location> stations = new ArrayList<Location>();

		final CharSequence page = ParserUtils.scrape(uri);
		String oldZebra = null;

		final Matcher mCoarse = P_NEARBY_COARSE.matcher(page);

		while (mCoarse.find())
		{
			final String zebra = mCoarse.group(1);
			if (oldZebra != null && zebra.equals(oldZebra))
				throw new IllegalArgumentException("missed row? last:" + zebra);
			else
				oldZebra = zebra;

			final Matcher mFineLocation = P_NEARBY_FINE_LOCATION.matcher(mCoarse.group(2));

			if (mFineLocation.find())
			{
				int parsedLon = 0;
				int parsedLat = 0;
				final int parsedId = Integer.parseInt(mFineLocation.group(1));
				final String parsedName = ParserUtils.resolveEntities(mFineLocation.group(2));

				final Matcher mFineCoords = P_NEARBY_FINE_COORDS.matcher(mCoarse.group(2));

				if (mFineCoords.find())
				{
					parsedLon = Integer.parseInt(mFineCoords.group(1));
					parsedLat = Integer.parseInt(mFineCoords.group(2));
				}

				final String[] placeAndName = splitPlaceAndName(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, placeAndName[0], placeAndName[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		return new NearbyStationsResult(null, stations);
	}

	private static final Pattern P_LINE_SBAHN = Pattern.compile("SN?\\d*");
	private static final Pattern P_LINE_TRAM = Pattern.compile("STR\\w{0,5}");
	private static final Pattern P_LINE_BUS = Pattern.compile("BUS\\w{0,5}");
	private static final Pattern P_LINE_TAXI = Pattern.compile("TAX\\w{0,5}");

	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// Intercity
		if ("EC".equals(ucType)) // EuroCity
			return 'I';
		if ("EN".equals(ucType)) // EuroNight
			return 'I';
		if ("D".equals(ucType)) // EuroNight, Sitzwagenabteil
			return 'I';
		if ("EIC".equals(ucType)) // Ekspres InterCity, Polen
			return 'I';
		if ("ICE".equals(ucType)) // InterCityExpress
			return 'I';
		if ("IC".equals(ucType)) // InterCity
			return 'I';
		if ("ICT".equals(ucType)) // InterCity
			return 'I';
		if ("ICN".equals(ucType)) // Intercity-Neigezug, Schweiz
			return 'I';
		if ("CNL".equals(ucType)) // CityNightLine
			return 'I';
		if ("OEC".equals(ucType)) // ÖBB-EuroCity
			return 'I';
		if ("OIC".equals(ucType)) // ÖBB-InterCity
			return 'I';
		if ("RJ".equals(ucType)) // RailJet, Österreichische Bundesbahnen
			return 'I';
		if ("THA".equals(ucType)) // Thalys
			return 'I';
		if ("TGV".equals(ucType)) // Train à Grande Vitesse
			return 'I';
		if ("DNZ".equals(ucType)) // Nachtzug Basel-Moskau
			return 'I';
		if ("AIR".equals(ucType)) // Generic Flight
			return 'I';
		if ("ECB".equals(ucType)) // EC, Verona-München
			return 'I';
		if ("LYN".equals(ucType)) // Dänemark
			return 'I';
		if ("NZ".equals(ucType)) // Schweden, Nacht
			return 'I';
		if ("INZ".equals(ucType)) // Nacht
			return 'I';
		if ("RHI".equals(ucType)) // ICE
			return 'I';
		if ("RHT".equals(ucType)) // TGV
			return 'I';
		if ("TGD".equals(ucType)) // TGV
			return 'I';
		if ("IRX".equals(ucType)) // IC
			return 'I';
		if ("ES".equals(ucType)) // Eurostar Italia
			return 'I';
		if ("EST".equals(ucType)) // Eurostar Frankreich
			return 'I';
		if ("EM".equals(ucType)) // Euromed, Barcelona-Alicante, Spanien
			return 'I';
		if ("AVE".equals(ucType)) // Alta Velocidad Española, Spanien
			return 'I';
		if ("ARC".equals(ucType)) // Arco (Renfe), Spanien
			return 'I';
		if ("ALS".equals(ucType)) // Alaris (Renfe), Spanien
			return 'I';
		if ("ATR".equals(ucType)) // Altaria (Renfe), Spanien
			return 'R';
		if ("TAL".equals(ucType)) // Talgo, Spanien
			return 'I';
		if ("TLG".equals(ucType)) // Spanien, Madrid
			return 'I';
		if ("HOT".equals(ucType)) // Spanien, Nacht
			return 'I';
		if ("X2".equals(ucType)) // X2000 Neigezug, Schweden
			return 'I';
		if ("X".equals(ucType)) // InterConnex
			return 'I';
		if ("FYR".equals(ucType)) // Fyra, Amsterdam-Schiphol-Rotterdam
			return 'I';
		if ("SC".equals(ucType)) // SuperCity, Tschechien
			return 'I';
		if ("FLUG".equals(ucType))
			return 'I';
		if ("TLK".equals(ucType)) // Tanie Linie Kolejowe, Polen
			return 'I';
		if ("INT".equals(ucType)) // Zürich-Brüssel - Budapest-Istanbul
			return 'I';
		if ("HKX".equals(ucType)) // Hamburg-Koeln-Express
			return 'I';

		// Regional
		if ("ZUG".equals(ucType)) // Generic Train
			return 'R';
		if ("R".equals(ucType)) // Generic Regional Train
			return 'R';
		if ("DPN".equals(ucType)) // Dritter Personen Nahverkehr
			return 'R';
		if ("RB".equals(ucType)) // RegionalBahn
			return 'R';
		if ("RE".equals(ucType)) // RegionalExpress
			return 'R';
		if ("IR".equals(ucType)) // Interregio
			return 'R';
		if ("IRE".equals(ucType)) // Interregio Express
			return 'R';
		if ("HEX".equals(ucType)) // Harz-Berlin-Express, Veolia
			return 'R';
		if ("WFB".equals(ucType)) // Westfalenbahn
			return 'R';
		if ("RT".equals(ucType)) // RegioTram
			return 'R';
		if ("REX".equals(ucType)) // RegionalExpress, Österreich
			return 'R';
		if ("OS".equals(ucType)) // Osobný vlak, Slovakia oder Osobní vlak, Czech Republic
			return 'R';
		if ("SP".equals(ucType)) // Spěšný vlak, Czech Republic
			return 'R';
		if ("EZ".equals(ucType)) // ÖBB ErlebnisBahn
			return 'R';
		if ("ARZ".equals(ucType)) // Auto-Reisezug Brig - Iselle di Trasquera
			return 'R';
		if ("OE".equals(ucType)) // Ostdeutsche Eisenbahn
			return 'R';
		if ("MR".equals(ucType)) // Märkische Regionalbahn
			return 'R';
		if ("PE".equals(ucType)) // Prignitzer Eisenbahn GmbH
			return 'R';
		if ("NE".equals(ucType)) // NEB Betriebsgesellschaft mbH
			return 'R';
		if ("MRB".equals(ucType)) // Mitteldeutsche Regiobahn
			return 'R';
		if ("ERB".equals(ucType)) // eurobahn (Keolis Deutschland)
			return 'R';
		if ("HLB".equals(ucType)) // Hessische Landesbahn
			return 'R';
		if ("VIA".equals(ucType))
			return 'R';
		if ("HSB".equals(ucType)) // Harzer Schmalspurbahnen
			return 'R';
		if ("OSB".equals(ucType)) // Ortenau-S-Bahn
			return 'R';
		if ("VBG".equals(ucType)) // Vogtlandbahn
			return 'R';
		if ("AKN".equals(ucType)) // AKN Eisenbahn AG
			return 'R';
		if ("OLA".equals(ucType)) // Ostseeland Verkehr
			return 'R';
		if ("UBB".equals(ucType)) // Usedomer Bäderbahn
			return 'R';
		if ("PEG".equals(ucType)) // Prignitzer Eisenbahn
			return 'R';
		if ("NWB".equals(ucType)) // NordWestBahn
			return 'R';
		if ("CAN".equals(ucType)) // cantus Verkehrsgesellschaft
			return 'R';
		if ("BRB".equals(ucType)) // ABELLIO Rail
			return 'R';
		if ("SBB".equals(ucType)) // Schweizerische Bundesbahnen
			return 'R';
		if ("VEC".equals(ucType)) // vectus Verkehrsgesellschaft
			return 'R';
		if ("TLX".equals(ucType)) // Trilex (Vogtlandbahn)
			return 'R';
		if ("HZL".equals(ucType)) // Hohenzollerische Landesbahn
			return 'R';
		if ("ABR".equals(ucType)) // Bayerische Regiobahn
			return 'R';
		if ("CB".equals(ucType)) // City Bahn Chemnitz
			return 'R';
		if ("WEG".equals(ucType)) // Württembergische Eisenbahn-Gesellschaft
			return 'R';
		if ("NEB".equals(ucType)) // Niederbarnimer Eisenbahn
			return 'R';
		if ("ME".equals(ucType)) // metronom Eisenbahngesellschaft
			return 'R';
		if ("MER".equals(ucType)) // metronom regional
			return 'R';
		if ("ALX".equals(ucType)) // Arriva-Länderbahn-Express
			return 'R';
		if ("EB".equals(ucType)) // Erfurter Bahn
			return 'R';
		if ("VEN".equals(ucType)) // Rhenus Veniro
			return 'R';
		if ("BOB".equals(ucType)) // Bayerische Oberlandbahn
			return 'R';
		if ("SBS".equals(ucType)) // Städtebahn Sachsen
			return 'R';
		if ("EVB".equals(ucType)) // Eisenbahnen und Verkehrsbetriebe Elbe-Weser
			return 'R';
		if ("STB".equals(ucType)) // Süd-Thüringen-Bahn
			return 'R';
		if ("AG".equals(ucType)) // Ingolstadt-Landshut
			return 'R';
		if ("PRE".equals(ucType)) // Pressnitztalbahn
			return 'R';
		if ("SHB".equals(ucType)) // Schleswig-Holstein-Bahn
			return 'R';
		if ("NOB".equals(ucType)) // Nord-Ostsee-Bahn
			return 'R';
		if ("RTB".equals(ucType)) // Rurtalbahn
			return 'R';
		if ("BLB".equals(ucType)) // Berchtesgadener Land Bahn
			return 'R';
		if ("NBE".equals(ucType)) // Nordbahn Eisenbahngesellschaft
			return 'R';
		if ("SOE".equals(ucType)) // Sächsisch-Oberlausitzer Eisenbahngesellschaft
			return 'R';
		if ("SDG".equals(ucType)) // Sächsische Dampfeisenbahngesellschaft
			return 'R';
		if ("VE".equals(ucType)) // Lutherstadt Wittenberg
			return 'R';
		if ("DAB".equals(ucType)) // Daadetalbahn
			return 'R';
		if ("WTB".equals(ucType)) // Wutachtalbahn e.V.
			return 'R';
		if ("BE".equals(ucType)) // Grensland-Express
			return 'R';
		if ("ARR".equals(ucType)) // Ostfriesland
			return 'R';
		if ("HTB".equals(ucType)) // Hörseltalbahn
			return 'R';
		if ("FEG".equals(ucType)) // Freiberger Eisenbahngesellschaft
			return 'R';
		if ("NEG".equals(ucType)) // Norddeutsche Eisenbahngesellschaft Niebüll
			return 'R';
		if ("RBG".equals(ucType)) // Regental Bahnbetriebs GmbH
			return 'R';
		if ("MBB".equals(ucType)) // Mecklenburgische Bäderbahn Molli
			return 'R';
		if ("VEB".equals(ucType)) // Vulkan-Eifel-Bahn Betriebsgesellschaft
			return 'R';
		if ("LEO".equals(ucType)) // Chiemgauer Lokalbahn
			return 'R';
		if ("VX".equals(ucType)) // Vogtland Express
			return 'R';
		if ("MSB".equals(ucType)) // Mainschleifenbahn
			return 'R';
		if ("P".equals(ucType)) // Kasbachtalbahn
			return 'R';
		if ("ÖBA".equals(ucType)) // Öchsle-Bahn Betriebsgesellschaft
			return 'R';
		if ("KTB".equals(ucType)) // Kandertalbahn
			return 'R';
		if ("ATZ".equals(ucType)) // Autotunnelzug
			return 'R';
		if ("ATB".equals(ucType)) // Autoschleuse Tauernbahn
			return 'R';
		if ("CAT".equals(ucType)) // City Airport Train
			return 'R';
		if ("EXTRA".equals(ucType) || "EXT".equals(ucType)) // Extrazug
			return 'R';
		if ("KD".equals(ucType)) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
			return 'R';
		if ("KM".equals(ucType)) // Koleje Mazowieckie
			return 'R';
		if ("EX".equals(ucType)) // Polen
			return 'R';
		if ("PCC".equals(ucType)) // PCC Rail, Polen
			return 'R';
		if ("ZR".equals(ucType)) // ZSR (Slovakian Republic Railways)
			return 'R';

		// if ("E".equals(normalizedType)) // Eilzug, stimmt wahrscheinlich nicht
		// return "R" + normalizedName;

		// Suburban Trains
		if (P_LINE_SBAHN.matcher(ucType).matches()) // Generic (Night) S-Bahn
			return 'S';
		if ("BSB".equals(ucType)) // Breisgau S-Bahn
			return 'S';
		if ("SWE".equals(ucType)) // Südwestdeutsche Verkehrs-AG, Ortenau-S-Bahn
			return 'S';
		if ("RER".equals(ucType)) // Réseau Express Régional, Frankreich
			return 'S';
		if ("WKD".equals(ucType)) // Warszawska Kolej Dojazdowa (Warsaw Suburban Railway)
			return 'S';
		if ("SKM".equals(ucType)) // Szybka Kolej Miejska Tricity
			return 'S';
		if ("SKW".equals(ucType)) // Szybka Kolej Miejska Warschau
			return 'S';
		// if ("SPR".equals(normalizedType)) // Sprinter, Niederlande
		// return "S" + normalizedName;

		// Subway
		if ("U".equals(ucType)) // Generic U-Bahn
			return 'U';
		if ("MET".equals(ucType))
			return 'U';

		// Tram
		if (P_LINE_TRAM.matcher(ucType).matches()) // Generic Tram
			return 'T';
		if ("TRAM".equals(ucType))
			return 'T';
		if ("TRA".equals(ucType))
			return 'T';
		if ("STRWLB".equals(ucType)) // Wiener Lokalbahnen
			return 'T';

		// Bus
		if (P_LINE_BUS.matcher(ucType).matches()) // Generic Bus
			return 'B';
		if ("NFB".equals(ucType)) // Niederflur-Bus
			return 'B';
		if ("SEV".equals(ucType)) // Schienen-Ersatz-Verkehr
			return 'B';
		if ("BUSSEV".equals(ucType)) // Schienen-Ersatz-Verkehr
			return 'B';
		if ("BSV".equals(ucType)) // Bus SEV
			return 'B';
		if ("FB".equals(ucType)) // Fernbus? Luxemburg-Saarbrücken
			return 'B';
		if ("TRO".equals(ucType)) // Trolleybus
			return 'B';
		if ("AST".equals(ucType)) // Anruf-Sammel-Taxi
			return 'B';
		if ("RFB".equals(ucType)) // Rufbus
			return 'B';
		if ("RUF".equals(ucType)) // Rufbus
			return 'B';
		if (P_LINE_TAXI.matcher(ucType).matches()) // Generic Taxi
			return 'B';
		if ("RFT".equals(ucType)) // Ruftaxi
			return 'B';
		if ("LT".equals(ucType)) // Linien-Taxi
			return 'B';
		if ("ALT".equals(ucType)) // Anruf-Linien-Taxi
			return 'B';
		// if ("N".equals(normalizedType)) // Nachtbus
		// return "B" + normalizedName;

		// Ferry
		if ("SCHIFF".equals(ucType))
			return 'F';
		if ("FÄHRE".equals(ucType))
			return 'F';
		if ("FÄH".equals(ucType))
			return 'F';
		if ("FAE".equals(ucType))
			return 'F';
		if ("SCH".equals(ucType)) // Schiff
			return 'F';
		if ("AS".equals(ucType)) // SyltShuttle, eigentlich Autoreisezug
			return 'F';
		if ("KAT".equals(ucType)) // Katamaran, e.g. Friedrichshafen - Konstanz
			return 'F';
		if ("BAT".equals(ucType)) // Boots Anlege Terminal?
			return 'F';
		if ("BAV".equals(ucType)) // Boots Anlege?
			return 'F';

		// Cable Car
		if ("SEILBAHN".equals(ucType))
			return 'C';
		if ("SB".equals(ucType)) // Seilbahn
			return 'C';
		if ("ZAHNR".equals(ucType)) // Zahnradbahn, u.a. Zugspitzbahn
			return 'C';
		if ("GB".equals(ucType)) // Gondelbahn
			return 'C';
		if ("LB".equals(ucType)) // Luftseilbahn
			return 'C';
		if ("FUN".equals(ucType)) // Funiculaire (Standseilbahn)
			return 'C';
		if ("SL".equals(ucType)) // Sessel-Lift
			return 'C';

		// if ("L".equals(normalizedType))
		// return "?" + normalizedName;
		// if ("CR".equals(normalizedType))
		// return "?" + normalizedName;
		// if ("TRN".equals(normalizedType))
		// return "?" + normalizedName;

		return 0;
	}

	protected static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zßÄÅäáàâåéèêíìîÖöóòôÜüúùûØ/]+)[\\s-]*(.*)");

	protected Line parseLine(final String type, final String line)
	{
		final char normalizedType = normalizeType(type);

		if (normalizedType != 0)
		{
			final String lineStr;

			if (line != null)
			{
				final Matcher m = P_NORMALIZE_LINE.matcher(line);
				final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

				lineStr = normalizedType + strippedLine;
			}
			else
			{
				lineStr = Character.toString(normalizedType);
			}

			return new Line(null, lineStr, lineColors(lineStr));
		}
		else
		{
			throw new IllegalStateException("cannot normalize type '" + type + "' line '" + line + "'");
		}
	}

	protected String normalizeLine(final String line)
	{
		if (line == null || line.length() == 0)
			return null;

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2);

			final char normalizedType = normalizeType(type);
			if (normalizedType != 0)
				return normalizedType + type + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	private static final Pattern P_NORMALIZE_LINE_AND_TYPE = Pattern.compile("([^#]*)#(.*)");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");
	// saved from RtProvider
	private static final Pattern P_NORMALIZE_LINE_RUSSIA = Pattern.compile("(\\d{3}(BJ|FJ|IJ|MJ|NJ|OJ|TJ|SZ))");

	protected final String parseLineAndType(final String lineAndType)
	{
		final Matcher m = P_NORMALIZE_LINE_AND_TYPE.matcher(lineAndType);
		if (m.matches())
		{
			final String number = m.group(1).replaceAll("\\s+", " ");
			final String type = m.group(2);

			if (type.length() == 0)
			{
				if (number.length() == 0)
					return "?";
				if (P_NORMALIZE_LINE_NUMBER.matcher(number).matches())
					return "?" + number;
				// saved from RtProvider
				// if (P_NORMALIZE_LINE_RUSSIA.matcher(number).matches())
				// return 'R' + number;
			}
			else
			{
				final char normalizedType = normalizeType(type);
				if (normalizedType != 0)
					return normalizedType + number;
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line#type " + lineAndType);
		}

		throw new IllegalStateException("cannot normalize line#type " + lineAndType);
	}

	private static final Pattern P_CONNECTION_ID = Pattern.compile("co=(C\\d+-\\d+)&");

	protected static String extractConnectionId(final String link)
	{
		final Matcher m = P_CONNECTION_ID.matcher(link);
		if (m.find())
			return m.group(1);
		else
			throw new IllegalArgumentException("cannot extract id from " + link);
	}

	private void assertResC(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.jumpToStartTag(pp, null, "ResC"))
			throw new IOException("cannot find <ResC />");
	}
}
