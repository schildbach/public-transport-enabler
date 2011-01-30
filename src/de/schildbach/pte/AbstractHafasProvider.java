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
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider implements NetworkProvider
{
	private static final String DEFAULT_ENCODING = "ISO-8859-1";

	private final String apiUri;
	private static final String prod = "hafas";
	private final String accessId;

	public AbstractHafasProvider(final String apiUri, final String accessId)
	{
		this.apiUri = apiUri;
		this.accessId = accessId;
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

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
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

	public QueryConnectionsResult queryConnections(Location from, Location via, Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		if (from.type == LocationType.ANY)
		{
			final List<Location> autocompletes = autocompleteStations(from.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(autocompletes, null, null);
			from = autocompletes.get(0);
		}

		if (via != null && via.type == LocationType.ANY)
		{
			final List<Location> autocompletes = autocompleteStations(via.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(null, autocompletes, null);
			via = autocompletes.get(0);
		}

		if (to.type == LocationType.ANY)
		{
			final List<Location> autocompletes = autocompleteStations(to.name);
			if (autocompletes.isEmpty())
				return new QueryConnectionsResult(QueryConnectionsResult.Status.NO_CONNECTIONS); // TODO
			if (autocompletes.size() > 1)
				return new QueryConnectionsResult(null, null, autocompletes);
			to = autocompletes.get(0);
		}

		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

		final String request = "<ConReq>" //
				+ "<Start>" + location(from) + "<Prod bike=\"0\" couchette=\"0\" direct=\"0\" sleeper=\"0\"/></Start>" //
				+ (via != null ? "<Via>" + location(via) + "</Via>" : "") //
				+ "<Dest>" + location(to) + "</Dest>" //
				+ "<ReqT a=\"" + (dep ? 0 : 1) + "\" date=\"" + DATE_FORMAT.format(date) + "\" time=\"" + TIME_FORMAT.format(date) + "\"/>" //
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

		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

		InputStream is = null;

		try
		{
			is = ParserUtils.scrapeInputStream(apiUri, wrap(request), 3);

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			assertResC(pp);
			XmlPullUtil.enter(pp);

			XmlPullUtil.enter(pp, "ConRes");
			if (pp.getName().equals("Err"))
			{
				final String code = XmlPullUtil.attr(pp, "code");
				if (code.equals("K9380"))
					return QueryConnectionsResult.TOO_CLOSE;
				if (code.equals("K9220")) // Nearby to the given address stations could not be found
					return QueryConnectionsResult.UNRESOLVABLE_ADDRESS;
				if (code.equals("K890")) // No connections found
					return QueryConnectionsResult.NO_CONNECTIONS;
				throw new IllegalStateException("error " + code + " " + XmlPullUtil.attr(pp, "text"));
			}

			XmlPullUtil.require(pp, "ConResCtxt");
			final String context = XmlPullUtil.text(pp);
			XmlPullUtil.enter(pp, "ConnectionList");

			final List<Connection> connections = new ArrayList<Connection>();

			while (XmlPullUtil.test(pp, "Connection"))
			{
				final String id = XmlPullUtil.attr(pp, "id");

				XmlPullUtil.enter(pp);
				while (pp.getName().equals("RtStateList"))
					XmlPullUtil.next(pp);
				XmlPullUtil.enter(pp, "Overview");

				XmlPullUtil.require(pp, "Date");
				final Calendar currentDate = new GregorianCalendar();
				currentDate.setTime(DATE_FORMAT.parse(XmlPullUtil.text(pp)));
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

				XmlPullUtil.enter(pp, "ConSectionList");

				final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);
				Date firstDepartureTime = null;
				Date lastArrivalTime = null;

				while (XmlPullUtil.test(pp, "ConSection"))
				{
					XmlPullUtil.enter(pp);

					// departure
					XmlPullUtil.enter(pp, "Departure");
					XmlPullUtil.enter(pp, "BasicStop");
					while (pp.getName().equals("StAttrList"))
						XmlPullUtil.next(pp);
					final Location sectionDeparture = parseLocation(pp);
					XmlPullUtil.enter(pp, "Dep");
					XmlPullUtil.require(pp, "Time");
					final Date departureTime = parseTime(currentDate, XmlPullUtil.text(pp));
					XmlPullUtil.enter(pp, "Platform");
					XmlPullUtil.require(pp, "Text");
					String departurePos = XmlPullUtil.text(pp).trim();
					if (departurePos.length() == 0)
						departurePos = null;
					XmlPullUtil.exit(pp, "Platform");

					XmlPullUtil.exit(pp);

					XmlPullUtil.exit(pp);
					XmlPullUtil.exit(pp);

					// journey
					String line = null;
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

						line = _normalizeLine(category, name, longCategory);
					}
					else if (tag.equals("Walk") || tag.equals("Transfer") || tag.equals("GisRoute"))
					{
						XmlPullUtil.enter(pp);
						XmlPullUtil.enter(pp, "Duration");
						XmlPullUtil.require(pp, "Time");
						min = parseDuration(XmlPullUtil.text(pp).substring(3, 8));
						XmlPullUtil.exit(pp);
						XmlPullUtil.exit(pp);
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
					final Date arrivalTime = parseTime(currentDate, XmlPullUtil.text(pp));
					XmlPullUtil.enter(pp, "Platform");
					XmlPullUtil.require(pp, "Text");
					String arrivalPos = XmlPullUtil.text(pp).trim();
					if (arrivalPos.length() == 0)
						arrivalPos = null;
					XmlPullUtil.exit(pp, "Platform");

					XmlPullUtil.exit(pp);

					XmlPullUtil.exit(pp);
					XmlPullUtil.exit(pp);

					XmlPullUtil.exit(pp);

					if (min == 0 || line != null)
					{
						parts.add(new Connection.Trip(line, destination, departureTime, departurePos, sectionDeparture.id, sectionDeparture.name,
								arrivalTime, arrivalPos, sectionArrival.id, sectionArrival.name, null));
					}
					else
					{
						if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
						{
							final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
							parts.add(new Connection.Footway(lastFootway.min + min, lastFootway.departureId, lastFootway.departure,
									sectionArrival.id, sectionArrival.name));
						}
						else
						{
							parts.add(new Connection.Footway(min, sectionDeparture.id, sectionDeparture.name, sectionArrival.id, sectionArrival.name));
						}
					}

					if (firstDepartureTime == null)
						firstDepartureTime = departureTime;
					lastArrivalTime = arrivalTime;
				}

				XmlPullUtil.exit(pp);

				XmlPullUtil.exit(pp);

				connections.add(new Connection(id, null, firstDepartureTime, lastArrivalTime, departure.id, departure.name, arrival.id, arrival.name,
						parts, null));
			}

			XmlPullUtil.exit(pp);

			return new QueryConnectionsResult(null, from, via, to, context, connections);
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		catch (final ParseException x)
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

	private static final Pattern P_TIME = Pattern.compile("(\\d+)d(\\d+):(\\d{2}):(\\d{2})");

	private Date parseTime(final Calendar currentDate, final String str)
	{
		final Matcher m = P_TIME.matcher(str);
		if (m.matches())
		{
			final Calendar c = new GregorianCalendar();
			c.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
			c.set(Calendar.MONTH, currentDate.get(Calendar.MONTH));
			c.set(Calendar.DAY_OF_MONTH, currentDate.get(Calendar.DAY_OF_MONTH));
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(2)));
			c.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
			c.set(Calendar.SECOND, Integer.parseInt(m.group(4)));
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(1)));
			return c.getTime();
		}
		else
		{
			throw new IllegalArgumentException("cannot parse duration: " + str);
		}
	}

	private static final Pattern P_DURATION = Pattern.compile("(\\d+):(\\d{2})");

	private final int parseDuration(final String str)
	{
		final Matcher m = P_DURATION.matcher(str);
		if (m.matches())
			return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
		else
			throw new IllegalArgumentException("cannot parse duration: " + str);
	}

	private static final String location(final Location location)
	{
		if (location.type == LocationType.STATION && location.id != 0)
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
		if (location.type == LocationType.STATION && location.id != 0)
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

	private static final Pattern P_LINE_S = Pattern.compile("S\\d+");
	private static final Pattern P_LINE_SN = Pattern.compile("SN\\d*");

	private final String _normalizeLine(final String type, final String name, final String longCategory)
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

		throw new IllegalStateException("cannot normalize type '" + normalizedType + "' (" + type + ") name '" + normalizedName + "' longCategory '"
				+ longCategory + "'");
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

	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE_COORDS = Pattern
			.compile("REQMapRoute0\\.Location0\\.X=(-?\\d+)&(?:amp;)?REQMapRoute0\\.Location0\\.Y=(-?\\d+)&");
	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("[\\?&]input=(\\d+)&[^\"]*\">([^<]*)<");

	protected abstract String nearbyStationUri(String stationId);

	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		if (stationId == null)
			throw new IllegalArgumentException("stationId must be given");

		final List<Station> stations = new ArrayList<Station>();

		final String uri = nearbyStationUri(stationId);
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
				stations.add(new Station(parsedId, nameAndPlace[0], nameAndPlace[1], parsedName, parsedLat, parsedLon, 0, null, null));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return new NearbyStationsResult(stations);
		else
			return new NearbyStationsResult(stations.subList(0, maxStations));
	}

	protected static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû/-]+)[\\s-]*(.*)");

	protected final String normalizeLine(final String type, final String line)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

		final char normalizedType = normalizeType(type);
		if (normalizedType != 0)
			return normalizedType + strippedLine;

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
		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("EN")) // EuroNight
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
		if (ucType.equals("DNZ")) // Berlin-Saratov, Berlin-Moskva, Connections only?
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

		// Regional Germany
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

		// Regional Poland
		if (ucType.equals("OS")) // Chop-Cierna nas Tisou
			return 'R';
		if (ucType.equals("SP")) // Polen
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

		// Bus
		if (ucType.equals("BUS")) // Generic Bus
			return 'B';
		if (ucType.equals("AST")) // Anruf-Sammel-Taxi
			return 'B';
		if (ucType.equals("RUF")) // Rufbus
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

		return 0;
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
