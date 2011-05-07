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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
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
import de.schildbach.pte.dto.QueryConnectionsResult.Status;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider implements NetworkProvider
{
	private static final String DEFAULT_ENCODING = "ISO-8859-1";
	private static final String prod = "hafas";

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

	protected String[] splitNameAndPlace(final String name)
	{
		return new String[] { null, name };
	}

	private final String wrap(final String request)
	{
		return "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" //
				+ "<ReqC ver=\"1.1\" prod=\"" + prod + "\" lang=\"DE\"" + (accessId != null ? " accessId=\"" + accessId + "\"" : "") + ">" //
				+ request //
				+ "</ReqC>";
	}

	private static final Location parseStation(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Station".equals(type))
		{
			final String name = pp.getAttributeValue(null, "name").trim();
			final int id = Integer.parseInt(pp.getAttributeValue(null, "externalStationNr"));
			final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
			final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));
			return new Location(LocationType.STATION, id, y, x, null, name);
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

	private static final Location parseAddress(final XmlPullParser pp)
	{
		final String type = pp.getName();
		if ("Address".equals(type))
		{
			String name = pp.getAttributeValue(null, "name").trim();
			if (name.equals("unknown"))
				name = null;
			final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
			final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));
			return new Location(LocationType.ADDRESS, 0, y, x, null, name);
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

	public List<Location> xmlLocValReq(final CharSequence constraint) throws IOException
	{
		final String request = "<LocValReq id=\"req\" maxNr=\"20\"><ReqLoc match=\"" + constraint + "\" type=\"ALLTYPE\"/></LocValReq>";

		// System.out.println(ParserUtils.scrape(apiUri, true, wrap(request), null, false));

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), 3);

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
		final CharSequence page = ParserUtils.scrape(uri, false, null, jsonEncoding, false);

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
							final String[] nameAndPlace = splitNameAndPlace(value);
							results.add(new Location(LocationType.STATION, localId, lat, lon, nameAndPlace[0], nameAndPlace[1]));
						}
						else if (type == 2) // address
						{
							results.add(new Location(LocationType.ADDRESS, 0, lat, lon, null, value));
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
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), 3);

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

				final String[] nameAndPlace = splitNameAndPlace(name);
				results.add(new Location(type, id, lat, lon, nameAndPlace[0], nameAndPlace[1]));

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

	private static final Pattern P_XML_QUERY_DEPARTURES_COARSE = Pattern.compile(
			"\\G<Journey ([^>]*?)(?:/>|><HIMMessage ([^>]*?)/></Journey>)(?:\n|\\z)", Pattern.DOTALL);
	private static final Pattern P_XML_QUERY_DEPARTURES_FINE = Pattern.compile("" //
			+ "fpTime\\s*=\"(\\d{1,2}:\\d{2})\"\\s*" // time
			+ "fpDate\\s*=\"(\\d{2}\\.\\d{2}\\.\\d{2}|\\d{4}-\\d{2}-\\d{2})\"\\s*" // date
			+ "delay\\s*=\"(?:-|k\\.A\\.?|cancel|\\+?\\s*(\\d+))\"\\s*" // delay
			+ "(?:e_delay\\s*=\"\\d+\"\\s*)?" // (???)
			+ "(?:newpl\\s*=\"([^\"]*)\"\\s*)?" //
			+ "(?:platform\\s*=\"([^\"]*)\"\\s*)?" // position
			+ "targetLoc\\s*=\"([^\"]*)\"\\s*" // destination
			+ "(?:hafasname\\s*=\"[^\"]*\"\\s*)?" // (???)
			+ "prod\\s*=\"([^\"]*)\"\\s*" // line
			+ "(?:class\\s*=\"[^\"]*\"\\s*)?" // (???)
			+ "(?:dir\\s*=\"[^\"]*\"\\s*)?" // (destination)
			+ "(?:capacity\\s*=\"[^\"]*\"\\s*)?" // (???)
			+ "(?:depStation\\s*=\"(.*?)\"\\s*)?" //
			+ "(?:delayReason\\s*=\"([^\"]*)\"\\s*)?" // message
			+ "(?:is_reachable\\s*=\"([^\"]*)\"\\s*)?" // (???)
	);
	private static final Pattern P_XML_QUERY_DEPARTURES_MESSAGES = Pattern.compile("<Err code=\"([^\"]*)\" text=\"([^\"]*)\"");

	protected QueryDeparturesResult xmlQueryDepartures(final String uri, final int stationId) throws IOException
	{
		// scrape page
		final CharSequence page = ParserUtils.scrape(uri);

		final QueryDeparturesResult result = new QueryDeparturesResult();

		// parse page
		final Matcher mMessage = P_XML_QUERY_DEPARTURES_MESSAGES.matcher(page);
		if (mMessage.find())
		{
			final String code = mMessage.group(1);
			final String text = mMessage.group(2);

			if (code.equals("H730")) // Your input is not valid
				return new QueryDeparturesResult(QueryDeparturesResult.Status.INVALID_STATION);
			if (code.equals("H890"))
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			throw new IllegalArgumentException("unknown error " + code + ", " + text);
		}

		final List<Departure> departures = new ArrayList<Departure>(8);

		final Matcher mCoarse = P_XML_QUERY_DEPARTURES_COARSE.matcher(page);
		while (mCoarse.find())
		{
			// TODO parse HIMMessage

			final Matcher mFine = P_XML_QUERY_DEPARTURES_FINE.matcher(mCoarse.group(1));
			if (mFine.matches())
			{
				if (mFine.group(8) == null)
				{
					final Calendar plannedTime = new GregorianCalendar(timeZone());
					plannedTime.clear();
					ParserUtils.parseEuropeanTime(plannedTime, mFine.group(1));
					final String dateStr = mFine.group(2);
					if (dateStr.length() == 8)
						ParserUtils.parseGermanDate(plannedTime, dateStr);
					else
						ParserUtils.parseIsoDate(plannedTime, dateStr);

					final Calendar predictedTime;
					if (mFine.group(3) != null)
					{
						predictedTime = new GregorianCalendar(timeZone());
						predictedTime.setTimeInMillis(plannedTime.getTimeInMillis());
						predictedTime.add(Calendar.MINUTE, Integer.parseInt(mFine.group(3)));
					}
					else
					{
						predictedTime = null;
					}

					// TODO parse newpl if present

					final String position = mFine.group(5) != null ? "Gl. " + ParserUtils.resolveEntities(mFine.group(5)) : null;

					final String destination = ParserUtils.resolveEntities(mFine.group(6)).trim();

					final String line = normalizeLine(ParserUtils.resolveEntities(mFine.group(7)));

					final String message;
					if (mFine.group(9) != null)
					{
						final String m = ParserUtils.resolveEntities(mFine.group(9)).trim();
						message = m.length() > 0 ? m : null;
					}
					else
					{
						message = null;
					}

					departures.add(new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
							line != null ? lineColors(line) : null, null, position, 0, destination, message));
				}
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
			}
		}

		result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId), departures, null));
		return result;
	}

	public QueryConnectionsResult queryConnections(Location from, Location via, Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		if (from.type == LocationType.ANY || (from.type == LocationType.ADDRESS && !from.hasLocation()))
		{
			final List<Location> autocompletes = autocompleteStations(from.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(autocompletes, null, null);
			from = autocompletes.get(0);
		}

		if (via != null && (via.type == LocationType.ANY || (via.type == LocationType.ADDRESS && !via.hasLocation())))
		{
			final List<Location> autocompletes = autocompleteStations(via.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(null, autocompletes, null);
			via = autocompletes.get(0);
		}

		if (to.type == LocationType.ANY || (to.type == LocationType.ADDRESS && !to.hasLocation()))
		{
			final List<Location> autocompletes = autocompleteStations(to.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(null, null, autocompletes);
			to = autocompletes.get(0);
		}

		final Calendar c = new GregorianCalendar(timeZone());
		c.setTime(date);

		final String request = "<ConReq>" //
				+ "<Start>"
				+ location(from)
				+ "<Prod bike=\"0\" couchette=\"0\" direct=\"0\" sleeper=\"0\"/></Start>" //
				+ (via != null ? "<Via>" + location(via) + "</Via>" : "") //
				+ "<Dest>"
				+ location(to)
				+ "</Dest>" //
				+ "<ReqT a=\"" + (dep ? 0 : 1)
				+ "\" date=\""
				+ String.format("%04d.%02d.%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				+ "\" time=\""
				+ String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) + "\"/>" //
				+ "<RFlags b=\"0\" chExtension=\"0\" f=\"4\" sMode=\"N\"/>" //
				+ "</ConReq>";

		return queryConnections(request, from, via, to);
	}

	public QueryConnectionsResult queryMoreConnections(final String context) throws IOException
	{
		final String request = "<ConScrReq scr=\"F\" nrCons=\"4\">" //
				+ "<ConResCtxt>" + context + "</ConResCtxt>" //
				+ "</ConScrReq>";

		return queryConnections(request, null, null, null);
	}

	private QueryConnectionsResult queryConnections(final String request, final Location from, final Location via, final Location to)
			throws IOException
	{
		// System.out.println(request);
		// ParserUtils.printXml(ParserUtils.scrape(apiUri, true, wrap(request), null, false));

		InputStream is = null;

		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), 3);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertResC(pp);
			XmlPullUtil.enter(pp, "ResC");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("F1")) // Spool: Error reading the spoolfile
					return new QueryConnectionsResult(Status.SERVICE_DOWN);
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			XmlPullUtil.enter(pp, "ConRes");

			if (XmlPullUtil.test(pp, "Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("K9380") || code.equals("K895")) // Departure/Arrival are too near
					return QueryConnectionsResult.TOO_CLOSE;
				if (code.equals("K9220")) // Nearby to the given address stations could not be found
					return QueryConnectionsResult.UNRESOLVABLE_ADDRESS;
				if (code.equals("K9240")) // Internal error
					return new QueryConnectionsResult(Status.SERVICE_DOWN);
				if (code.equals("K9260")) // Departure station does not exist
					return QueryConnectionsResult.NO_CONNECTIONS;
				if (code.equals("K890")) // No connections found
					return QueryConnectionsResult.NO_CONNECTIONS;
				if (code.equals("K891")) // No route found (try entering an intermediate station)
					return QueryConnectionsResult.NO_CONNECTIONS;
				if (code.equals("K899")) // An error occurred
					return new QueryConnectionsResult(Status.SERVICE_DOWN);
				// if (code.equals("K1:890")) // Unsuccessful or incomplete search (direction: forward)
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			XmlPullUtil.require(pp, "ConResCtxt");
			final String context = XmlPullUtil.text(pp);
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
					final Calendar departureTime = new GregorianCalendar(timeZone());
					departureTime.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(departureTime, XmlPullUtil.text(pp));
					XmlPullUtil.enter(pp, "Platform");
					XmlPullUtil.require(pp, "Text");
					String departurePos = XmlPullUtil.text(pp).trim();
					if (departurePos.length() == 0)
						departurePos = null;
					XmlPullUtil.exit(pp, "Platform");

					XmlPullUtil.exit(pp, "Dep");

					XmlPullUtil.exit(pp, "BasicStop");
					XmlPullUtil.exit(pp, "Departure");

					// journey
					final Line line;
					Location destination = null;
					int min = 0;

					final String tag = pp.getName();
					if (tag.equals("Journey"))
					{
						XmlPullUtil.enter(pp);
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
							XmlPullUtil.exit(pp);

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
						XmlPullUtil.exit(pp);
						XmlPullUtil.exit(pp);

						if (category == null)
							category = shortCategory;

						final char type = normalizeType(category);
						final String lineStr;
						if (type != 0)
							lineStr = type + name;
						else
							lineStr = _normalizeLine(category, name); // for compatibility
						line = new Line(lineStr, lineColors(lineStr));
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
					final Calendar arrivalTime = new GregorianCalendar(timeZone());
					arrivalTime.setTimeInMillis(currentDate.getTimeInMillis());
					parseTime(arrivalTime, XmlPullUtil.text(pp));
					XmlPullUtil.enter(pp, "Platform");
					XmlPullUtil.require(pp, "Text");
					String arrivalPos = XmlPullUtil.text(pp).trim();
					if (arrivalPos.length() == 0)
						arrivalPos = null;
					XmlPullUtil.exit(pp, "Platform");

					XmlPullUtil.exit(pp, "Arr");

					XmlPullUtil.exit(pp, "BasicStop");
					XmlPullUtil.exit(pp, "Arrival");

					XmlPullUtil.exit(pp, "ConSection");

					if (min == 0 || line != null)
					{
						parts.add(new Connection.Trip(line, destination, departureTime.getTime(), departurePos, sectionDeparture, arrivalTime
								.getTime(), arrivalPos, sectionArrival, null, null));
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
						firstDepartureTime = departureTime.getTime();
					lastArrivalTime = arrivalTime.getTime();
				}

				XmlPullUtil.exit(pp, "ConSectionList");

				XmlPullUtil.exit(pp, "Connection");

				connections.add(new Connection(id, null, firstDepartureTime, lastArrivalTime, departure, arrival, parts, null));
			}

			XmlPullUtil.exit(pp);

			return new QueryConnectionsResult(null, from, via, to, context, connections);
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
			XmlPullUtil.require(pp, "Text");
			final String value = XmlPullUtil.text(pp).trim();
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

	private static final String location(final Location location)
	{
		if (location.type == LocationType.STATION && location.hasId())
			return "<Station externalId=\"" + location.id + "\" />";
		if (location.type == LocationType.POI && location.hasLocation())
			return "<Poi type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" />";
		if (location.type == LocationType.ADDRESS && location.hasLocation())
			return "<Address type=\"WGS84\" x=\"" + location.lon + "\" y=\"" + location.lat + "\" />";

		throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
	}

	protected static final String locationId(final Location location)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("A=").append(locationType(location));
		if (location.hasLocation())
			builder.append("@X=" + location.lon + "@Y=" + location.lat);
		if (location.name != null)
			builder.append("@G=" + location.name);
		if (location.type == LocationType.STATION && location.hasId())
			builder.append("@L=").append(location.id);
		return builder.toString();
	}

	protected static final int locationType(final Location location)
	{
		if (location.type == LocationType.STATION)
			return 1;
		if (location.type == LocationType.POI)
			return 4;
		if (location.type == LocationType.ADDRESS && location.hasLocation())
			return 16;
		if (location.type == LocationType.ADDRESS && location.name != null)
			return 2;
		if (location.type == LocationType.ANY)
			return 255;
		throw new IllegalArgumentException(location.type.toString());
	}

	private final static Pattern P_WHITESPACE = Pattern.compile("\\s+");

	private final String normalizeWhitespace(final String str)
	{
		return P_WHITESPACE.matcher(str).replaceAll("");
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
				return new NearbyStationsResult(NearbyStationsResult.Status.INVALID_STATION);
			if (code.equals("H890")) // No trains in result
				return new NearbyStationsResult(stations);
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

				final String[] nameAndPlace = splitNameAndPlace(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, nameAndPlace[0], nameAndPlace[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
			}
		}

		return new NearbyStationsResult(stations);
	}

	protected final NearbyStationsResult jsonNearbyStations(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri, false, null, jsonEncoding, false);

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

				final String[] nameAndPlace = splitNameAndPlace(name);
				stations.add(new Location(LocationType.STATION, id, lat, lon, nameAndPlace[0], nameAndPlace[1]));
			}
		}
		catch (final JSONException x)
		{
			x.printStackTrace();
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}

		return new NearbyStationsResult(stations);
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

				final String[] nameAndPlace = splitNameAndPlace(parsedName);
				stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, nameAndPlace[0], nameAndPlace[1]));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		return new NearbyStationsResult(stations);
	}

	protected static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zßÄÅäáàâåéèêíìîÖöóòôÜüúùûØ/-]+)[\\s-]*(.*)");

	protected String normalizeLine(final String type, final String line)
	{
		final char normalizedType = normalizeType(type);

		if (normalizedType != 0)
		{
			if (line != null)
			{
				final Matcher m = P_NORMALIZE_LINE.matcher(line);
				final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

				return normalizedType + strippedLine;
			}
			else
			{
				return Character.toString(normalizedType);
			}
		}

		throw new IllegalStateException("cannot normalize type '" + type + "' line '" + line + "'");
	}

	protected abstract char normalizeType(String type);

	protected final char normalizeCommonTypes(final String ucType)
	{
		// Intercity
		if (ucType.equals("EC")) // EuroCity
			return 'I';
		if (ucType.equals("EN")) // EuroNight
			return 'I';
		if (ucType.equals("EIC")) // Ekspres InterCity, Polen
			return 'I';
		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("ICT")) // InterCity
			return 'I';
		if ("ICN".equals(ucType)) // Intercity-Neigezug, Schweiz
			return 'I';
		if (ucType.equals("CNL")) // CityNightLine
			return 'I';
		if (ucType.equals("OEC")) // ÖBB-EuroCity
			return 'I';
		if (ucType.equals("OIC")) // ÖBB-InterCity
			return 'I';
		if (ucType.equals("RJ")) // RailJet, Österreichische Bundesbahnen
			return 'I';
		if (ucType.equals("THA")) // Thalys
			return 'I';
		if (ucType.equals("TGV")) // Train à Grande Vitesse
			return 'I';
		if (ucType.equals("DNZ")) // Nachtzug Basel-Moskau
			return 'I';
		if (ucType.equals("AIR")) // Generic Flight
			return 'I';
		if (ucType.equals("ECB")) // EC, Verona-München
			return 'I';
		if (ucType.equals("INZ")) // Nacht
			return 'I';
		if (ucType.equals("RHI")) // ICE
			return 'I';
		if (ucType.equals("RHT")) // TGV
			return 'I';
		if (ucType.equals("TGD")) // TGV
			return 'I';
		if (ucType.equals("IRX")) // IC
			return 'I';
		if ("FLUG".equals(ucType))
			return 'I';

		// Regional
		if (ucType.equals("ZUG")) // Generic Train
			return 'R';
		if (ucType.equals("R")) // Generic Regional Train
			return 'R';
		if (ucType.equals("DPN")) // Dritter Personen Nahverkehr
			return 'R';
		if (ucType.equals("RB")) // RegionalBahn
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress
			return 'R';
		if (ucType.equals("IR")) // Interregio
			return 'R';
		if (ucType.equals("IRE")) // Interregio Express
			return 'R';
		if (ucType.equals("HEX")) // Harz-Berlin-Express, Veolia
			return 'R';
		if (ucType.equals("WFB")) // Westfalenbahn
			return 'R';
		if (ucType.equals("RT")) // RegioTram
			return 'R';
		if (ucType.equals("REX")) // RegionalExpress, Österreich
			return 'R';
		if (ucType.equals("OS")) // Osobný vlak, Slovakia oder Osobní vlak, Czech Republic
			return 'R';
		if (ucType.equals("SP")) // Spěšný vlak, Czech Republic
			return 'R';
		if ("EZ".equals(ucType)) // ÖBB ErlebnisBahn
			return 'R';
		if ("ARZ".equals(ucType)) // Auto-Reisezug Brig - Iselle di Trasquera
			return 'R';

		// Suburban Trains
		if (ucType.equals("S")) // Generic S-Bahn
			return 'S';

		// Subway
		if (ucType.equals("U")) // Generic U-Bahn
			return 'U';

		// Tram
		if (ucType.equals("STR")) // Generic Tram
			return 'T';
		if ("TRAM".equals(ucType))
			return 'T';
		if ("TRA".equals(ucType))
			return 'T';

		// Bus
		if (ucType.equals("BUS")) // Generic Bus
			return 'B';
		if (ucType.equals("AST")) // Anruf-Sammel-Taxi
			return 'B';
		if (ucType.equals("RUF")) // Rufbus
			return 'B';
		if ("RFT".equals(ucType)) // Ruftaxi
			return 'B';
		if (ucType.equals("SEV")) // Schienen-Ersatz-Verkehr
			return 'B';
		if (ucType.equals("BUSSEV")) // Schienen-Ersatz-Verkehr
			return 'B';
		if (ucType.equals("BSV")) // Bus SEV
			return 'B';
		if (ucType.equals("FB")) // Luxemburg-Saarbrücken
			return 'B';

		// Ferry
		if (ucType.equals("SCH")) // Schiff
			return 'F';
		if (ucType.equals("AS")) // SyltShuttle, eigentlich Autoreisezug
			return 'F';
		if ("SCHIFF".equals(ucType))
			return 'F';
		if ("KAT".equals(ucType)) // Katamaran
			return 'F';

		// Cable Car
		if ("SB".equals(ucType)) // Seilbahn
			return 'C';
		if ("ZAHNR".equals(ucType)) // Zahnradbahn, u.a. Zugspitzbahn
			return 'C';

		return 0;
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

	private static final Pattern P_LINE_S = Pattern.compile("S\\d+");
	private static final Pattern P_LINE_SN = Pattern.compile("SN\\d*");

	private final String _normalizeLine(final String type, final String name)
	{
		final String normalizedType = type.split(" ", 2)[0];
		final String normalizedName = normalizeWhitespace(name);

		if ("EN".equals(normalizedType)) // EuroNight
			return "I" + normalizedName;
		if ("EC".equals(normalizedType)) // EuroCity
			return "I" + normalizedName;
		if ("ICE".equals(normalizedType)) // InterCityExpress
			return "I" + normalizedName;
		if ("IC".equals(normalizedType)) // InterCity
			return "I" + normalizedName;
		if ("ICN".equals(normalizedType)) // IC-Neigezug
			return "I" + normalizedName;
		if ("CNL".equals(normalizedType)) // CityNightLine
			return "I" + normalizedName;
		if ("OEC".equals(normalizedType)) // ÖBB EuroCity
			return "I" + normalizedName;
		if ("OIC".equals(normalizedType)) // ÖBB InterCity
			return "I" + normalizedName;
		if ("TGV".equals(normalizedType)) // Train à grande vit.
			return "I" + normalizedName;
		if ("THA".equals(normalizedType)) // Thalys
			return "I" + normalizedName;
		if ("THALYS".equals(normalizedType)) // THALYS
			return "I" + normalizedName;
		if ("ES".equals(normalizedType)) // Eurostar Italia
			return "I" + normalizedName;
		if ("EST".equals(normalizedType)) // Eurostar
			return "I" + normalizedName;
		if ("X2".equals(normalizedType)) // X2000 Neigezug, Schweden
			return "I" + normalizedName;
		if ("RJ".equals(normalizedType)) // Railjet
			return "I" + normalizedName;
		if ("AVE".equals(normalizedType)) // Alta Velocidad ES
			return "I" + normalizedName;
		if ("ARC".equals(normalizedType)) // Arco, Spanien
			return "I" + normalizedName;
		if ("ALS".equals(normalizedType)) // Alaris, Spanien
			return "I" + normalizedName;
		if ("TAL".equals(normalizedType)) // Talgo, Spanien
			return "I" + normalizedName;
		if ("NZ".equals(normalizedType)) // Nacht-Zug
			return "I" + normalizedName;
		if ("FYR".equals(normalizedType)) // Fyra, Amsterdam-Schiphol-Rotterdam
			return "I" + normalizedName;

		if ("R".equals(normalizedType)) // Regio
			return "R" + normalizedName;
		if ("D".equals(normalizedType)) // Schnellzug
			return "R" + normalizedName;
		if ("E".equals(normalizedType)) // Eilzug
			return "R" + normalizedName;
		if ("RE".equals(normalizedType)) // RegioExpress
			return "R" + normalizedName;
		if ("IR".equals(normalizedType)) // InterRegio
			return "R" + normalizedName;
		if ("IRE".equals(normalizedType)) // InterRegioExpress
			return "R" + normalizedName;
		if ("ATZ".equals(normalizedType)) // Autotunnelzug
			return "R" + normalizedName;
		if ("EXT".equals(normalizedType)) // Extrazug
			return "R" + normalizedName;
		if ("CAT".equals(normalizedType)) // City Airport Train
			return "R" + normalizedName;

		if ("S".equals(normalizedType)) // S-Bahn
			return "S" + normalizedName;
		if (P_LINE_S.matcher(normalizedType).matches()) // diverse S-Bahnen
			return "S" + normalizedType;
		if (P_LINE_SN.matcher(normalizedType).matches()) // Nacht-S-Bahn
			return "S" + normalizedType;
		if ("SPR".equals(normalizedType)) // Sprinter, Niederlande
			return "S" + normalizedName;

		if ("Met".equals(normalizedType)) // Metro
			return "U" + normalizedName;
		if ("M".equals(normalizedType)) // Metro
			return "U" + normalizedName;
		if ("Métro".equals(normalizedType))
			return "U" + normalizedName;

		if ("Tram".equals(normalizedType)) // Tram
			return "T" + normalizedName;
		if ("TRAM".equals(normalizedType)) // Tram
			return "T" + normalizedName;
		if ("T".equals(normalizedType)) // Tram
			return "T" + normalizedName;
		if ("Tramway".equals(normalizedType))
			return "T" + normalizedName;

		if ("BUS".equals(normalizedType)) // Bus
			return "B" + normalizedName;
		if ("Bus".equals(normalizedType)) // Niederflurbus
			return "B" + normalizedName;
		if ("NFB".equals(normalizedType)) // Niederflur-Bus
			return "B" + normalizedName;
		if ("N".equals(normalizedType)) // Nachtbus
			return "B" + normalizedName;
		if ("Tro".equals(normalizedType)) // Trolleybus
			return "B" + normalizedName;
		if ("Taxi".equals(normalizedType)) // Taxi
			return "B" + normalizedName;
		if ("TX".equals(normalizedType)) // Taxi
			return "B" + normalizedName;

		if ("BAT".equals(normalizedType)) // Schiff
			return "F" + normalizedName;

		if ("GB".equals(normalizedType)) // Gondelbahn
			return "C" + normalizedName;
		if ("LB".equals(normalizedType)) // Luftseilbahn
			return "C" + normalizedName;
		if ("FUN".equals(normalizedType)) // Standseilbahn
			return "C" + normalizedName;
		if ("Fun".equals(normalizedType)) // Funiculaire
			return "C" + normalizedName;

		if ("L".equals(normalizedType))
			return "?" + normalizedName;
		if ("P".equals(normalizedType))
			return "?" + normalizedName;
		if ("CR".equals(normalizedType))
			return "?" + normalizedName;
		if ("TRN".equals(normalizedType))
			return "?" + normalizedName;

		throw new IllegalStateException("cannot normalize type '" + normalizedType + "' (" + type + ") name '" + normalizedName + "'");
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

	private static final Map<Character, int[]> LINES = new HashMap<Character, int[]>();

	static
	{
		LINES.put('I', new int[] { Color.WHITE, Color.RED, Color.RED });
		LINES.put('R', new int[] { Color.GRAY, Color.WHITE });
		LINES.put('S', new int[] { Color.parseColor("#006e34"), Color.WHITE });
		LINES.put('U', new int[] { Color.parseColor("#003090"), Color.WHITE });
		LINES.put('T', new int[] { Color.parseColor("#cc0000"), Color.WHITE });
		LINES.put('B', new int[] { Color.parseColor("#993399"), Color.WHITE });
		LINES.put('F', new int[] { Color.BLUE, Color.WHITE });
		LINES.put('?', new int[] { Color.DKGRAY, Color.WHITE });
	}

	public int[] lineColors(final String line)
	{
		if (line.length() == 0)
			return null;
		return LINES.get(line.charAt(0));
	}

	private void assertResC(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.jumpToStartTag(pp, null, "ResC"))
			throw new IOException("cannot find <ResC />");
	}
}
