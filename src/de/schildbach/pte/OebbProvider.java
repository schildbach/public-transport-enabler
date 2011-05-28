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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.OEBB;
	public static final String OLD_NETWORK_ID = "fahrplan.oebb.at";
	private static final String API_BASE = "http://fahrplan.oebb.at/bin/";
	private static final String URL_ENCODING = "ISO-8859-1";

	public OebbProvider()
	{
		super(null, 12, null);
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
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/dn?near=Suchen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=").append(equivs ? "no" : "yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/dny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=255&REQ0JourneyStopsB=12&S=%s?&js=true&";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), URL_ENCODING));

		return jsonGetStops(uri);
	}

	private static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();
	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "115");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "100");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "85");
	}

	private String connectionsQuery(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		final Calendar c = new GregorianCalendar(timeZone());
		c.setTime(date);

		final StringBuilder uri = new StringBuilder();

		uri.append("queryPageDisplayed=yes");
		uri.append("&ignoreTypeCheck=yes");
		uri.append("&REQ0JourneyStopsS0ID=").append(ParserUtils.urlEncode(locationId(from), URL_ENCODING));
		if (via != null)
			uri.append("&REQ0JourneyStops1.0ID=").append(ParserUtils.urlEncode(locationId(via), URL_ENCODING));
		uri.append("&REQ0JourneyStopsZ0ID=").append(ParserUtils.urlEncode(locationId(to), URL_ENCODING));
		uri.append("&REQ0JourneyDate=").append(
				String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) - 2000));
		uri.append("&wDayExt0=").append(ParserUtils.urlEncode("Mo|Di|Mi|Do|Fr|Sa|So"));
		uri.append("&REQ0JourneyTime=").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&existHafasDemo3=yes");
		uri.append("&REQ0JourneyDep_Foot_speed=").append(WALKSPEED_MAP.get(walkSpeed));
		uri.append("&existBikeEverywhere=yes");
		uri.append("&existHafasAttrInc=yes");
		uri.append("&start=Verbindungen+suchen");

		if (products != null)
		{
			for (final char p : products.toCharArray())
			{
				if (p == 'I')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_0=1&REQ0JourneyProduct_prod_section_0_1=1&REQ0JourneyProduct_prod_section_0_2=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_0=1&REQ0JourneyProduct_prod_section_1_1=1&REQ0JourneyProduct_prod_section_1_2=1");
				}
				if (p == 'R')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_3=1&REQ0JourneyProduct_prod_section_0_4=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_3=1&REQ0JourneyProduct_prod_section_1_4=1");
				}
				if (p == 'S')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_5=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_5=1");
				}
				if (p == 'U')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_8=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_8=1");
				}
				if (p == 'T')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_9=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_9=1");
				}
				if (p == 'B')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_6=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_6=1");
				}
				if (p == 'P')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_11=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_11=1");
				}
				if (p == 'F')
				{
					uri.append("&REQ0JourneyProduct_prod_section_0_7=1");
					if (via != null)
						uri.append("&REQ0JourneyProduct_prod_section_1_7=1");
				}
				// FIXME if (p == 'C')
			}
		}

		return uri.toString();
	}

	private static final String QUERY_CONNECTIONS_FORM_URL = API_BASE + "query.exe/dn?";
	private static final Pattern P_QUERY_CONNECTIONS_FORM_ACTION = Pattern
			.compile("<form id=\"HFSQuery\" action=\"(http://fahrplan\\.oebb\\.at/bin/query\\.exe[^#]*)#");
	private static final Pattern P_QUERY_CONNECTIONS_ERROR = Pattern
			.compile("(keine Verbindung gefunden|kein Weg gefunden)|(liegt nach dem Ende der Fahrplanperiode|liegt vor Beginn der Fahrplanperiode)|(zwischenzeitlich nicht mehr gespeichert)");
	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select.*? name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>\n(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*([^<\\[]*)(?:\\[[^\\[]*\\])?\\s*</option>", Pattern.DOTALL);

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		// get base url and cookies from form
		final CharSequence form = ParserUtils.scrape(QUERY_CONNECTIONS_FORM_URL, false, null, null, true);
		final Matcher m = P_QUERY_CONNECTIONS_FORM_ACTION.matcher(form);
		if (!m.find())
			throw new IllegalStateException("cannot find form: '" + form + "' on " + QUERY_CONNECTIONS_FORM_URL);
		final String baseUri = m.group(1);

		// query
		final String query = connectionsQuery(from, via, to, date, dep, products, walkSpeed);
		final CharSequence page = ParserUtils.scrape(baseUri, true, query, null, true);

		final Matcher mError = P_QUERY_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(2) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(3) != null)
				throw new SessionExpiredException();
		}

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
			return new QueryConnectionsResult(fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(baseUri, page);
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri, false, null, null, true);

		final Matcher mError = P_QUERY_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(2) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(3) != null)
				throw new SessionExpiredException();
		}

		return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_ALL_DETAILS = Pattern.compile("" //
			+ "<a id=\"showAllDetails\" class=\"[^\"]*\" href=\"(http://fahrplan\\.oebb\\.at[^\"]*)\">");
	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "<span class=\"label\">von:</span>\n<span class=\"output\">\\s*(.*?)\\s*</span>.*?" // from
			+ "<span class=\"label\">nach:</span>\n<span class=\"output\">\\s*(.*?)\\s*</span>.*?" // to
			+ "<span class=\"label\">\nDatum:\n</span>\n<span class=\"output\">.., (\\d{2}\\.\\d{2}\\.\\d{2})</span>.*?" // date
			+ "(?:<a href=\"(http://fahrplan\\.oebb\\.at/bin/query\\.exe/dn?.*?&REQ0HafasScrollDir=2)\".*?)?" // linkEarlier
			+ "(?:<a href=\"(http://fahrplan\\.oebb\\.at/bin/query\\.exe/dn?.*?&REQ0HafasScrollDir=1)\".*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("" //
			+ "<tr id=\"trOverview(C\\d+-\\d+)\" [^>]*>\n(.*?)</tr>\n" //
			+ "<tr class=\"[^\"]*\" id=\"tr\\1\">\n(.*?)Seitenanfang.*?</tr>" //
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "<td class=\"date\" headers=\"hafasOVDate\"[^>]*>(\\d{2}\\.\\d{2}\\.\\d{2})" // departureDate
			+ "(?:<br />(\\d{2}\\.\\d{2}\\.\\d{2}))?.*?" // arrivalDate
			+ "(\\d{1,2}:\\d{2}) ab.*?" // departureTime
			+ "(\\d{1,2}:\\d{2}) an.*?" // arrivalTime
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("" //
			+ "<tr class=\"tpDetails (?:conFirstSecFirstRow|intermediateSection|conLastSecLastRow)\">\n(.*?)</tr>\n" //
			+ "<tr class=\"tpDetails (?:conFirstSecFirstRow|intermediateSection|conLastSecLastRow)\">\n(.*?)</tr>\n" //
			+ "<tr class=\"tpDetails sectionInfo\">\n(.*?)</tr>\n" //
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile(".*?" //
			+ "<td class=\"station\">\n?(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn.*?input=(\\d+)&[^>]*>)?" // departureId
			+ "([^\n<]*).*?" // departure
			+ "<td class=\"date\">(?:(\\d{2}\\.\\d{2}\\.\\d{2})|&nbsp;)</td>.*?" // departureDate
			+ "<td class=\"timeValue\">\n?<span>ab (\\d{2}:\\d{2}).*?" // departureTime
			+ "<td class=\"platform\">\\s*(?:&nbsp;|(.*?))\\s*</td>.*?" // departurePosition
			+ "<img class=\"product\" src=\"/img/vs_oebb/(\\w+?)_pic.gif\".*?" // lineType
			+ "(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/traininfo\\.exe/dn[^>]*>(.*?)</a>.*?)?" // line
			+ "<td class=\"station\">\n?(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn.*?input=(\\d+)&[^>]*>)?" // arrivalId
			+ "([^\n<]*).*?" // arrival
			+ "<td class=\"date\">(?:(\\d{2}\\.\\d{2}\\.\\d{2})|&nbsp;)</td>.*?" // arrivalDate
			+ "<td class=\"timeValue\">\n?<span>an (\\d{2}:\\d{2}).*?" // arrivalTime
			+ "<td class=\"platform\">\\s*(?:&nbsp;|(.*?))\\s*</td>.*?" // arrivalPosition
			+ "<td[^>]* class=\"section_remarks\">(?:.*?Richtung\\:\\s*([^\n]*)\n)?.*?</td>?.*?" // destination
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String firstUri, final CharSequence firstPage) throws IOException
	{
		// ugly workaround to fetch all details
		final Matcher mAllDetailsAction = P_CONNECTIONS_ALL_DETAILS.matcher(firstPage);
		if (!mAllDetailsAction.find())
			throw new IOException("cannot find all details link in '" + firstPage + "' on " + firstUri);
		final String allDetailsUri = mAllDetailsAction.group(1);
		final CharSequence page = ParserUtils.scrape(allDetailsUri, false, null, null, true);

		final Matcher mError = P_QUERY_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(2) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(3) != null)
				throw new SessionExpiredException();
		}

		// parse page
		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
		if (mHead.matches())
		{
			final Location from = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(1)));
			final Location to = new Location(LocationType.ANY, 0, null, ParserUtils.resolveEntities(mHead.group(2)));
			final Calendar time = new GregorianCalendar(timeZone());
			time.clear();
			ParserUtils.parseGermanDate(time, mHead.group(3));
			// final String linkEarlier = mHead.group(4) != null ? ParserUtils.resolveEntities(mHead.group(4)) : null;
			final String linkLater = mHead.group(5) != null ? ParserUtils.resolveEntities(mHead.group(5)) : null;
			final List<Connection> connections = new ArrayList<Connection>();

			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
			while (mConCoarse.find())
			{
				final String id = mConCoarse.group(1);
				final String overview = mConCoarse.group(2);
				final String details = mConCoarse.group(3);

				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(overview);
				if (mConFine.matches())
				{
					final Calendar overviewDepartureTime = new GregorianCalendar(timeZone());
					overviewDepartureTime.clear();
					ParserUtils.parseGermanDate(overviewDepartureTime, mConFine.group(1));
					ParserUtils.parseEuropeanTime(overviewDepartureTime, mConFine.group(3));

					final Calendar overviewArrivalTime = new GregorianCalendar(timeZone());
					overviewArrivalTime.setTimeInMillis(overviewDepartureTime.getTimeInMillis());
					if (mConFine.group(2) != null)
						ParserUtils.parseGermanDate(overviewArrivalTime, mConFine.group(2));
					ParserUtils.parseEuropeanTime(overviewArrivalTime, mConFine.group(4));

					final String link = allDetailsUri; // TODO use print link?

					final Connection connection = new Connection(id, link, overviewDepartureTime.getTime(), overviewArrivalTime.getTime(), from, to,
							new ArrayList<Connection.Part>(1), null);
					connections.add(connection);

					final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(details);
					while (mDetCoarse.find())
					{
						final String set = mDetCoarse.group(1) + mDetCoarse.group(2) + mDetCoarse.group(3);

						final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(set);
						if (mDetFine.matches())
						{
							final int departureId = mDetFine.group(1) != null ? Integer.parseInt(mDetFine.group(1)) : 0;

							final Location departure = new Location(departureId != 0 ? LocationType.STATION : LocationType.ANY, departureId, null,
									ParserUtils.resolveEntities(mDetFine.group(2)));

							if (mDetFine.group(3) != null)
								ParserUtils.parseGermanDate(time, mDetFine.group(3));
							ParserUtils.parseEuropeanTime(time, mDetFine.group(4));
							final Date detailsDepartureTime = time.getTime();

							final String lineType = mDetFine.group(6);

							final int arrivalId = mDetFine.group(8) != null ? Integer.parseInt(mDetFine.group(8)) : 0;

							final Location arrival = new Location(arrivalId != 0 ? LocationType.STATION : LocationType.ANY, arrivalId, null,
									ParserUtils.resolveEntities(mDetFine.group(9)));

							if (mDetFine.group(10) != null)
								ParserUtils.parseGermanDate(time, mDetFine.group(10));
							ParserUtils.parseEuropeanTime(time, mDetFine.group(11));
							final Date detailsArrivalTime = time.getTime();

							if (!("fuss".equals(lineType) || "transfer".equals(lineType)))
							{
								if (departureId == 0)
									throw new IllegalStateException("departureId");

								final String departurePosition = mDetFine.group(5) != null ? ParserUtils.resolveEntities(mDetFine.group(5)) : null;

								final String lineStr = normalizeLine(lineType, ParserUtils.resolveEntities(mDetFine.group(7)));
								final Line line = new Line(lineStr, lineColors(lineStr));

								if (arrivalId == 0)
									throw new IllegalStateException("arrivalId");

								final String arrivalPosition = mDetFine.group(12) != null ? ParserUtils.resolveEntities(mDetFine.group(12)) : null;

								final Location destination = mDetFine.group(13) != null ? new Location(LocationType.ANY, 0, null,
										ParserUtils.resolveEntities(mDetFine.group(13))) : null;

								final Connection.Trip trip = new Connection.Trip(line, destination, detailsDepartureTime, departurePosition,
										departure, detailsArrivalTime, arrivalPosition, arrival, null, null);
								connection.parts.add(trip);
							}
							else
							{
								final int min = (int) (detailsArrivalTime.getTime() - detailsDepartureTime.getTime()) / 1000 / 60;

								final Connection.Footway footway = new Connection.Footway(min, departure, arrival, null);
								connection.parts.add(footway);
							}
						}
						else
						{
							throw new IllegalArgumentException("cannot parse '" + set + "' on " + allDetailsUri);
						}
					}
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + overview + "' on " + allDetailsUri);
				}

			}

			return new QueryConnectionsResult(allDetailsUri, from, null, to, linkLater, connections);
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + allDetailsUri);
		}
	}

	@Override
	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private static final Pattern P_NORMALIZE_LINE_AND_TYPE = Pattern.compile("([^#]*)#(.*)");
	private static final Pattern P_NORMALIZE_LINE_NUMBER = Pattern.compile("\\d{2,5}");

	@Override
	protected String normalizeLine(final String line)
	{
		final Matcher m = P_NORMALIZE_LINE_AND_TYPE.matcher(line);
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
			}
			else
			{
				final char normalizedType = normalizeType(type);
				if (normalizedType != 0)
					return normalizedType + number;
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("INT")) // Rußland, Connections only?
			return 'I';
		if (ucType.equals("RR")) // Finnland, Connections only?
			return 'I';
		if (ucType.equals("TLK")) // Tanie Linie Kolejowe, Polen
			return 'I';
		if (ucType.equals("EE")) // Rumänien, Connections only?
			return 'I';
		if (ucType.equals("SC")) // SuperCity, Tschechien
			return 'I';
		if (ucType.equals("TLG")) // Spanien, Madrid
			return 'I';
		if (ucType.equals("HOT")) // Spanien, Nacht
			return 'I';
		if (ucType.equals("OZ")) // Schweden, Oeresundzug, Connections only?
			return 'I';
		if (ucType.equals("LYN")) // Dänemark
			return 'I';
		if (ucType.equals("UUU")) // Italien, Nacht, Connections only?
			return 'I';

		if (ucType.equals("S2")) // Helsinki-Turku, Connections only?
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress Deutschland
			return 'R';
		if (ucType.equals("DPN")) // Connections only? TODO nicht evtl. doch eher ne S-Bahn?
			return 'R';
		if (ucType.equals("PCC")) // Polen
			return 'R';
		if (ucType.equals("KM")) // Polen
			return 'R';
		if (ucType.equals("SKM")) // Polen
			return 'R';
		if (ucType.equals("SKW")) // Polen
			return 'R';
		if (ucType.equals("E")) // Budapest, Ungarn
			return 'R';
		if (ucType.equals("IP")) // Ozd, Ungarn
			return 'R';
		if (ucType.equals("ZR")) // Bratislava, Slovakai
			return 'R';
		if (ucType.equals("N")) // Frankreich, Tours
			return 'R';
		if (ucType.equals("DPF")) // VX=Vogtland Express, Connections only?
			return 'R';
		// if (ucType.equals("SBE")) // Zittau-Seifhennersdorf, via JSON API
		// return 'R';
		// if (ucType.equals("VX")) // Vogtland Express, via JSON API
		// return 'R';
		// if (ucType.equals("RNV")) // Rhein-Neckar-Verkehr GmbH, via JSON API
		// return 'R';
		if ("UAU".equals(ucType)) // Rußland
			return 'R';

		if (ucType.equals("RSB")) // Schnellbahn Wien
			return 'S';
		// if (ucType.equals("DPN")) // S3 Bad Reichenhall-Freilassing, via JSON API
		// return 'S';
		if (ucType.equals("RER")) // Réseau Express Régional, Frankreich
			return 'S';
		if (ucType.equals("WKD")) // Warszawska Kolej Dojazdowa (Warsaw Suburban Railway)
			return 'S';

		if (ucType.equals("LKB")) // Connections only?
			return 'T';
		// if (ucType.equals("WLB")) // via JSON API
		// return 'T';

		if (ucType.equals("OBU")) // Connections only?
			return 'B';
		// if (ucType.equals("ASTSV")) // via JSON API
		// return 'B';
		if (ucType.equals("ICB")) // ÖBB ICBus
			return 'B';
		if (ucType.equals("BSV")) // Deutschland, Connections only?
			return 'B';
		if (ucType.equals("O-BUS")) // Stadtbus
			return 'B';

		if (ucType.equals("SCH")) // Connections only?
			return 'F';
		if (ucType.equals("F")) // Fähre
			return 'F';

		if (ucType.equals("LIF"))
			return 'C';
		if (ucType.equals("SSB")) // Graz Schlossbergbahn
			return 'C';
		// if (ucType.equals("HBB")) // Innsbruck Hungerburgbahn, via JSON API
		// return 'C';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if (ucType.equals("U70")) // U.K., Connections only?
			return '?';
		if (ucType.equals("R84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("S84")) // U.K., Connections only?
			return '?';
		if (ucType.equals("T84")) // U.K., Connections only?
			return '?';

		return 0;
	}
}
