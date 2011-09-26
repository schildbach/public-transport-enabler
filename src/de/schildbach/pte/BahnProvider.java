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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public final class BahnProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DB;
	public static final String OLD_NETWORK_ID = "mobile.bahn.de";
	private static final String API_BASE = "http://mobile.bahn.de/bin/mobil/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public BahnProvider()
	{
		super("http://reiseauskunft.bahn.de/bin/extxml.exe", 14, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.NEARBY_STATIONS || capability == Capability.DEPARTURES || capability == Capability.AUTOCOMPLETE_ONE_LINE
					|| capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		throw new UnsupportedOperationException();
	}

	private final static Pattern P_NEARBY_STATIONS_BY_STATION = Pattern
			.compile("<a href=\"http://mobile\\.bahn\\.de/bin/mobil/bhftafel.exe/dn[^\"]*?evaId=(\\d*)&[^\"]*?\">([^<]*)</a>");

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/dny");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 200);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_nv=get_stopweight|yes");
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("bhftafel.exe/dn");
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			final CharSequence page = ParserUtils.scrape(uri.toString());

			final Matcher m = P_NEARBY_STATIONS_BY_STATION.matcher(page);

			final List<Location> stations = new ArrayList<Location>();
			while (m.find())
			{
				final int sId = Integer.parseInt(m.group(1));
				final String sName = ParserUtils.resolveEntities(m.group(2).trim());

				final Location station = new Location(LocationType.STATION, sId, null, sName);
				stations.add(station);
			}

			if (maxStations == 0 || maxStations >= stations.size())
				return new NearbyStationsResult(null, stations);
			else
				return new NearbyStationsResult(null, stations.subList(0, maxStations));
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.exe/dn?getstop=1&REQ0JourneyStopsS0A=255&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products)
	{
		final Calendar c = new GregorianCalendar(timeZone());
		c.setTime(date);

		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("query.exe/dox");
		uri.append("?REQ0HafasOptimize1=0:1");

		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from)));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to)));

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
							ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%.6f, %.6f", via.lat / 1E6, via.lon / 1E6)));
			}
			else if (via.name != null)
			{
				uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via.name));
				if (via.type != LocationType.ANY)
					uri.append('!');
			}
		}

		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&REQ0JourneyDate=").append(
				String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) - 2000));
		uri.append("&REQ0JourneyTime=").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
		uri.append("&REQ0Tariff_Class=2");
		uri.append("&REQ0Tariff_TravellerAge.1=35");
		uri.append("&REQ0Tariff_TravellerReductionClass.1=0");
		uri.append("&existOptimizePrice=1");
		uri.append("&existProductNahverkehr=yes");
		uri.append("&start=Suchen");

		if (products != null)
		{
			for (final char p : products.toCharArray())
			{
				if (p == 'I')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_0=1&REQ0JourneyProduct_prod_section_0_1=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_0=1&REQ0JourneyProduct_prod_section_1_1=1");
				}
				if (p == 'R')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_2=1&REQ0JourneyProduct_prod_section_0_3=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_2=1&REQ0JourneyProduct_prod_section_1_3=1");
				}
				if (p == 'S')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_4=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_4=1");
				}
				if (p == 'U')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_7=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_7=1");
				}
				if (p == 'T')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_8=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_8=1");
				}
				if (p == 'B')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_5=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_5=1");
				}
				if (p == 'P')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_9=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_9=1");
				}
				if (p == 'F')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_6=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_6=1");
				}
				// FIXME if (p == 'C')
			}
		}

		return uri.toString();
	}

	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern
			.compile("(zu dicht beieinander|mehrfach vorhanden oder identisch)|(keine geeigneten Haltestellen)|(keine Verbindung gefunden)|(derzeit nur Ausk&#252;nfte vom)|(zwischenzeitlich nicht mehr gespeichert)");

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
			final String type = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<Location> addresses = new ArrayList<Location>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(new Location(LocationType.ANY, 0, null, address + "!"));
			}

			if (type.equals("REQ0JourneyStopsS0K"))
				fromAddresses = addresses;
			else if (type.equals("REQ0JourneyStopsZ0K"))
				toAddresses = addresses;
			else if (type.equals("REQ0JourneyStops1.0K"))
				viaAddresses = addresses;
			else
				throw new IllegalStateException(type);
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

	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "von: <span class=\"bold\">([^<]*)</span>.*?" // from
			+ "nach: <span class=\"bold\">([^<]*)</span>.*?" // to
			+ "Datum: <span class=\"bold\">.., (\\d{2}\\.\\d{2}\\.\\d{2})</span>.*?" // currentDate
			+ "(?:<a href=\"([^\"]*)\"><img [^>]*>\\s*Fr&#252;her.*?)?" // linkEarlier
			+ "(?:<a class=\"noBG\" href=\"([^\"]*)\"><img [^>]*>\\s*Sp&#228;ter.*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<tr><td class=\"overview timelink\">(.+?)</td></tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "<a href=\"(http://mobile.bahn.de/bin/mobil/query2?.exe/dox[^\"]*?)\">" // link
			+ "(\\d{1,2}:\\d{2})<br />(\\d{1,2}:\\d{2})</a></td>.+?" // departureTime, arrivalTime
			+ "<td class=\"overview iphonepfeil\">(.*?)<br />.*?" // line
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String uri, final CharSequence page) throws IOException
	{
		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(page);
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
		}

		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Location from = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(1)));
			final Location to = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(2)));
			final Calendar currentDate = new GregorianCalendar(timeZone());
			currentDate.clear();
			ParserUtils.parseGermanDate(currentDate, mHead.group(3));
			// final String linkEarlier = mHead.group(4) != null ? ParserUtils.resolveEntities(mHead.group(4)) : null;
			final String linkLater = mHead.group(5) != null ? ParserUtils.resolveEntities(mHead.group(5)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			while (mConCoarse.find())
			{
				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(mConCoarse.group(1));
				if (mConFine.matches())
				{
					final String link = ParserUtils.resolveEntities(mConFine.group(1));
					final Calendar departureTime = new GregorianCalendar(timeZone());
					departureTime.setTimeInMillis(currentDate.getTimeInMillis());
					ParserUtils.parseEuropeanTime(departureTime, mConFine.group(2));
					if (!connections.isEmpty())
					{
						final long diff = departureTime.getTimeInMillis() - connections.get(connections.size() - 1).departureTime.getTime();
						if (diff > PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime.add(Calendar.DAY_OF_YEAR, -1);
						else if (diff < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							departureTime.add(Calendar.DAY_OF_YEAR, 1);
					}
					final Calendar arrivalTime = new GregorianCalendar(timeZone());
					arrivalTime.setTimeInMillis(currentDate.getTimeInMillis());
					ParserUtils.parseEuropeanTime(arrivalTime, mConFine.group(3));
					if (departureTime.after(arrivalTime))
						arrivalTime.add(Calendar.DAY_OF_YEAR, 1);
					final Connection connection = new Connection(AbstractHafasProvider.extractConnectionId(link), link, departureTime.getTime(),
							arrivalTime.getTime(), from, to, null, null, null);
					connections.add(connection);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(1) + "' on " + uri);
				}
			}

			return new QueryConnectionsResult(new ResultHeader(SERVER_PRODUCT), uri, from, null, to, linkLater, connections);
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private static final Pattern P_CONNECTION_DETAILS_HEAD = Pattern.compile(".*?" //
			+ "<span class=\"bold\">Verbindungsdetails</span>(.*?)<div class=\"rline\"></div>.*?", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("<div class=\"rline haupt(?: rline)?\"[^>]*>\n(.+?>\n)</div>",
			Pattern.DOTALL);
	static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile("<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // departure
			+ "(?:" //
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span>.*?" // line
			+ "ab\\s+(?:<span[^>]*>.*?</span>)?\\s*(\\d{1,2}:\\d{2})\\s*(?:<span[^>]*>.*?</span>)?" // departureTime
			+ "\\s*(?:Gl\\. (.+?))?\\s*\n" // departurePosition
			+ "am\\s+(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // departureDate
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />.*?" // arrival
			+ "an\\s+(?:<span[^>]*>.*?</span>)?\\s*(\\d{1,2}:\\d{2})\\s*(?:<span[^>]*>.*?</span>)?" // arrivalTime
			+ "\\s*(?:Gl\\. (.+?))?\\s*\n" // arrivalPosition
			+ "am\\s+(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // arrivalDate
			+ "|" //
			+ "(\\d+) Min\\..*?" // footway
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />\n" // arrival
			+ "|" //
			+ "&#220;bergang.*?" //
			+ "<span class=\"bold\">\\s*(.+?)\\s*</span><br />\n" // arrival
			+ ")", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_ERROR = Pattern.compile("(zwischenzeitlich nicht mehr gespeichert)");
	private static final Pattern P_CONNECTION_DETAILS_MESSAGES = Pattern
			.compile("<div class=\"him\">|Dauer: \\d+:\\d+|Anschlusszug nicht mehr rechtzeitig|Anschlusszug jedoch erreicht werden|nur teilweise dargestellt|L&#228;ngerer Aufenthalt|&#228;quivalentem Bahnhof|Bahnhof wird mehrfach durchfahren|Aktuelle Informationen zu der Verbindung");

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mError = P_CONNECTION_DETAILS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				throw new SessionExpiredException();
		}

		final Matcher mHead = P_CONNECTION_DETAILS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final List<Connection.Part> parts = new ArrayList<Connection.Part>(4);

			Date firstDepartureTime = null;
			Location firstDeparture = null;
			Date lastArrivalTime = null;
			Location lastArrival = null;

			final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(mHead.group(1));
			while (mDetCoarse.find())
			{
				final String section = mDetCoarse.group(1);
				if (P_CONNECTION_DETAILS_MESSAGES.matcher(section).find())
				{
					// ignore message for now
				}
				else
				{
					final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(section);
					if (mDetFine.matches())
					{
						final Location departure = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mDetFine.group(1)));
						if (departure != null && firstDeparture == null)
							firstDeparture = departure;

						if (mDetFine.group(2) != null)
						{
							final String lineStr = normalizeLine(ParserUtils.resolveEntities(mDetFine.group(2)));
							final Line line = new Line(null, lineStr, lineColors(lineStr));

							final Calendar departureTime = new GregorianCalendar(timeZone());
							departureTime.clear();
							ParserUtils.parseEuropeanTime(departureTime, mDetFine.group(3));
							ParserUtils.parseGermanDate(departureTime, mDetFine.group(5));

							final String departurePosition = ParserUtils.resolveEntities(mDetFine.group(4));

							final Location arrival = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mDetFine.group(6)));

							final Calendar arrivalTime = new GregorianCalendar(timeZone());
							arrivalTime.clear();
							ParserUtils.parseEuropeanTime(arrivalTime, mDetFine.group(7));
							ParserUtils.parseGermanDate(arrivalTime, mDetFine.group(9));

							final String arrivalPosition = ParserUtils.resolveEntities(mDetFine.group(8));

							parts.add(new Connection.Trip(line, null, departureTime.getTime(), departurePosition, departure, arrivalTime.getTime(),
									arrivalPosition, arrival, null, null));

							if (firstDepartureTime == null)
								firstDepartureTime = departureTime.getTime();

							lastArrival = arrival;
							lastArrivalTime = arrivalTime.getTime();
						}
						else if (mDetFine.group(10) != null)
						{
							final String min = mDetFine.group(10);

							final Location arrival = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mDetFine.group(11)));

							if (parts.size() > 0 && parts.get(parts.size() - 1) instanceof Connection.Footway)
							{
								final Connection.Footway lastFootway = (Connection.Footway) parts.remove(parts.size() - 1);
								parts.add(new Connection.Footway(lastFootway.min + Integer.parseInt(min), lastFootway.departure, arrival, null));
							}
							else
							{
								parts.add(new Connection.Footway(Integer.parseInt(min), departure, arrival, null));
							}

							lastArrival = arrival;
						}
						else
						{
							final Location arrival = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mDetFine.group(12)));

							parts.add(new Connection.Footway(0, departure, arrival, null));
						}
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + section + "' on " + uri);
					}
				}
			}

			// verify
			if (firstDepartureTime == null || lastArrivalTime == null)
				throw new IllegalStateException("could not parse all parts of:\n" + mHead.group(1) + "\n" + parts);

			return new GetConnectionDetailsResult(new GregorianCalendar(timeZone()).getTime(), new Connection(
					AbstractHafasProvider.extractConnectionId(uri), uri, firstDepartureTime, lastArrivalTime, firstDeparture, lastArrival, parts,
					null, null));
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	@Override
	protected char normalizeType(String type)
	{
		final String ucType = type.toUpperCase();

		if ("RNV".equals(ucType))
			return 'R';
		if ("DZ".equals(ucType)) // Dampfzug
			return 'R';

		if ("LTT".equals(ucType))
			return 'B';

		if (ucType.startsWith("AST")) // Anruf-Sammel-Taxi
			return 'P';
		if (ucType.startsWith("ALT")) // Anruf-Linien-Taxi
			return 'P';
		if (ucType.startsWith("RFB")) // Rufbus
			return 'P';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if ("E".equals(ucType))
			return '?';

		return 0;
	}

	private static final Pattern P_LINE_BUS_SPECIAL = Pattern.compile("Bus([A-Z]/[\\dA-Z]+)");
	private static final Pattern P_LINE_RUSSIA = Pattern
			.compile("\\d{3}(?:AJ|BJ|DJ|FJ|IJ|KJ|LJ|NJ|MJ|OJ|RJ|SJ|TJ|VJ|ZJ|CH|KH|ZH|EI|JA|JI|MZ|SH|PC|Y)");
	private static final Pattern P_LINE_NUMBER = Pattern.compile("\\d{2,5}");

	@Override
	protected final String normalizeLine(final String line)
	{

		if ("Schw-B".equals(line)) // Schwebebahn, gilt als "Straßenbahn besonderer Bauart"
			return 'T' + line;

		if (P_LINE_BUS_SPECIAL.matcher(line).matches())
			return "B" + line;

		if (P_LINE_RUSSIA.matcher(line).matches())
			return 'R' + line;

		if (P_LINE_NUMBER.matcher(line).matches())
			return "?" + line;

		return super.normalizeLine(line);
	}
}
