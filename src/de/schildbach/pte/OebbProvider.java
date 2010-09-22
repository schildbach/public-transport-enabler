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

import de.schildbach.pte.QueryDeparturesResult.Status;

public class OebbProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "fahrplan.oebb.at";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS || capability == Capability.LOCATION_STATION_ID)
				return true;

		return false;
	}

	private static final String NAME_URL = "http://fahrplan.oebb.at/bin/stboard.exe/dn?input=";
	private static final Pattern P_SINGLE_NAME = Pattern
			.compile(".*?<input type=\"hidden\" name=\"input\" value=\"(.+?)#(\\d+)\">.*", Pattern.DOTALL);
	private static final Pattern P_MULTI_NAME = Pattern.compile("<option value=\".+?#(\\d+)\">(.+?)</option>", Pattern.DOTALL);

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(NAME_URL + ParserUtils.urlEncode(constraint.toString()));

		final List<Autocomplete> results = new ArrayList<Autocomplete>();

		final Matcher mSingle = P_SINGLE_NAME.matcher(page);
		if (mSingle.matches())
		{
			results.add(new Autocomplete(Integer.parseInt(mSingle.group(2)), ParserUtils.resolveEntities(mSingle.group(1))));
		}
		else
		{
			final Matcher mMulti = P_MULTI_NAME.matcher(page);
			while (mMulti.find())
				results.add(new Autocomplete(Integer.parseInt(mMulti.group(1)), ParserUtils.resolveEntities(mMulti.group(2))));
		}

		return results;
	}

	private final String NEARBY_URI = "http://fahrplan.oebb.at/bin/stboard.exe/dn?distance=50&near=Suchen&input=%d";
	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"zebracol-\\d\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE = Pattern.compile(".*?stboard\\.exe/.*?&input=.*?%23(\\d+)&.*?>(.*?)</a>.*?", Pattern.DOTALL);

	public List<Station> nearbyStations(final String stationId, final double lat, final double lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		if (stationId == null)
			throw new IllegalArgumentException("stationId must be given");

		final List<Station> stations = new ArrayList<Station>();

		final String uri = String.format(NEARBY_URI, stationId);
		final CharSequence page = ParserUtils.scrape(uri);

		final Matcher mCoarse = P_NEARBY_COARSE.matcher(page);
		while (mCoarse.find())
		{
			final Matcher mFine = P_NEARBY_FINE.matcher(mCoarse.group(1));
			if (mFine.matches())
			{
				final int parsedId = Integer.parseInt(mFine.group(1));
				final String parsedName = ParserUtils.resolveEntities(mFine.group(2));

				final Station station = new Station(parsedId, parsedName, 0, 0, 0, null, null);
				stations.add(station);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();
	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "115");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "100");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "85");
	}

	private String connectionsQuery(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final WalkSpeed walkSpeed) throws IOException
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final StringBuilder uri = new StringBuilder();

		uri.append("queryPageDisplayed=yes");
		uri.append("&start.x=0");
		uri.append("&start.y=0");
		uri.append("&start=Suchen");
		uri.append("&REQ0JourneyStopsS0A=").append(locationType(fromType));
		uri.append("&REQ0JourneyStopsS0G=").append(ParserUtils.urlEncode(from));
		uri.append("&REQ0JourneyStopsS0ID="); // "tupel"?
		if (via != null)
		{
			uri.append("&REQ0JourneyStops1.0A=").append(locationType(viaType));
			uri.append("&REQ0JourneyStops1.0G=").append(ParserUtils.urlEncode(via));
			uri.append("&REQ0JourneyStops1.0ID=");
		}
		uri.append("&REQ0JourneyStopsZ0A=").append(locationType(toType));
		uri.append("&REQ0JourneyStopsZ0G=").append(ParserUtils.urlEncode(to));
		uri.append("&REQ0JourneyStopsZ0ID=");
		uri.append("&REQ0JourneyDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&wDayExt0=").append(ParserUtils.urlEncode("Mo|Di|Mi|Do|Fr|Sa|So"));
		uri.append("&REQ0JourneyTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
		uri.append("&existHafasDemo3=yes");
		uri.append("&REQ0JourneyProduct_list=").append(ParserUtils.urlEncode("0:1111111111010000-000000"));
		uri.append("&REQ0JourneyDep_Foot_speed=").append(WALKSPEED_MAP.get(walkSpeed));
		uri.append("&existBikeEverywhere=yes");
		uri.append("&existHafasAttrInc=yes");

		return uri.toString();
	}

	private static int locationType(final LocationType locationType)
	{
		if (locationType == LocationType.STATION)
			return 1;
		if (locationType == LocationType.ADDRESS)
			return 2;
		if (locationType == LocationType.ANY)
			return 255;
		throw new IllegalArgumentException(locationType.toString());
	}

	private static final String QUERY_CONNECTIONS_FORM_URL = "http://fahrplan.oebb.at/bin/query.exe/dn?";
	private static final Pattern P_QUERY_CONNECTIONS_FORM_ACTION = Pattern
			.compile("<form action=\"(http://fahrplan\\.oebb\\.at/bin/query\\.exe[^#]*)#");
	private static final Pattern P_QUERY_CONNECTIONS_ERROR = Pattern
			.compile("(keine Verbindung gefunden werden)|(liegt nach dem Ende der Fahrplanperiode|liegt vor Beginn der Fahrplanperiode)|(zwischenzeitlich nicht mehr gespeichert)");
	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
			"<select.*? name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>(.*?)</select>", Pattern.DOTALL);
	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final WalkSpeed walkSpeed) throws IOException
	{
		// get base url and cookies from form
		final CharSequence form = ParserUtils.scrape(QUERY_CONNECTIONS_FORM_URL, false, null, null, true);
		final Matcher m = P_QUERY_CONNECTIONS_FORM_ACTION.matcher(form);
		if (!m.find())
			throw new IllegalStateException("cannot find form: '" + form + "' on " + QUERY_CONNECTIONS_FORM_URL);
		final String baseUri = m.group(1);

		// query
		final String query = connectionsQuery(fromType, from, viaType, via, toType, to, date, dep, walkSpeed);
		final CharSequence page = ParserUtils.scrape(baseUri, true, query, null, true);

		final Matcher mError = P_QUERY_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(2) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(3) != null)
				return QueryConnectionsResult.SESSION_TIMEOUT;
		}

		List<String> fromAddresses = null;
		List<String> viaAddresses = null;
		List<String> toAddresses = null;

		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
		while (mPreAddress.find())
		{
			final String type = mPreAddress.group(1);
			final String options = mPreAddress.group(2);

			final Matcher mAddresses = P_ADDRESSES.matcher(options);
			final List<String> addresses = new ArrayList<String>();
			while (mAddresses.find())
			{
				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
				if (!addresses.contains(address))
					addresses.add(address);
			}

			if (type.equals("REQ0JourneyStopsS0K"))
				fromAddresses = addresses;
			else if (type.equals("REQ0JourneyStopsZ0K"))
				toAddresses = addresses;
			else if (type.equals("REQ0JourneyStops1.0K"))
				viaAddresses = addresses;
			else
				throw new IOException(type);
		}

		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
			return new QueryConnectionsResult(QueryConnectionsResult.Status.AMBIGUOUS, fromAddresses, viaAddresses, toAddresses);
		else
			return queryConnections(baseUri, page);
	}

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
				return QueryConnectionsResult.SESSION_TIMEOUT;
		}

		return queryConnections(uri, page);
	}

	private static final Pattern P_CONNECTIONS_FORM_ACTION = Pattern
			.compile("<form name=\"tp_results_form\" action=\"(http://fahrplan\\.oebb\\.at/bin/query\\.exe[^#]*)#");
	private static final Pattern P_CONNECTIONS_PAGE = Pattern.compile(".*?" //
			+ "<form name=\"tp_results_form\" action=\"(http://fahrplan.oebb.at/bin/query.exe/.*?)#[^>]*>.*?" // action
			+ "<table class=\"hafasResult\" cellspacing=\"0\" summary=\"Ihre Anfrage\">\n(.*?)\n</table>.*?" // header
			+ "<table cellspacing=\"0\" class=\"hafasResult\" style=\"width:100%;\" summary=\"Verbindungen &#220;bersicht\">\n" //
			+ "(.*?<table cellspacing=\"0\">(.*?)</table>.*?)\n" // connections overview
			+ "</table>.*?" //
			+ "<table cellspacing=\"0\" class=\"hafasResult\" style=\"width: 100%;\" summary=\"Verbindungen Detailansicht\">\n" //
			+ "(.*?)\n" // connection details
			+ "</table>\n<div.*?" //
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
			+ "von:.*?<td [^>]*>\\s*(.*?)\\s*</td>.*?" // from
			+ "Datum:.*?<td [^>]*>.., (\\d{2}\\.\\d{2}\\.\\d{2})</td>.*?" // date
			+ "nach:.*?<td [^>]*>\\s*(.*?)\\s*</td>.*?" // to
			+ "(?:\"(REQ0HafasScrollDir=2&guiVCtrl_connection_detailsOut_add_selection&)\".*?)?" // linkEarlier
			+ "(?:\"(REQ0HafasScrollDir=1&guiVCtrl_connection_detailsOut_add_selection&)\".*?)?" // linkLater
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_COARSE = Pattern.compile("<tr class=\"(?:selected|tpOverview)\">\n(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
			+ "name=\"guiVCtrl_connection_detailsOut_select_([\\w-]+)\".*?" // id
			+ "<td headers=\"hafasOVDate\"[^>]*>(\\d{2}\\.\\d{2}\\.\\d{2})" // departureDate
			+ "(?:<br />(\\d{2}\\.\\d{2}\\.\\d{2}))?.*?" // arrivalDate
			+ "<td class=\"sepline\">(\\d{1,2}:\\d{2})" // departureTime
			+ "<br />(\\d{1,2}:\\d{2}).*?" // arrivalTime
	, Pattern.DOTALL);
	private static final Pattern P_CONNECTIONS_DETAILS_COARSE = Pattern.compile("Detailansicht<a name=\"cis_([\\w-]+)\">" // id
			+ "(.*?)" //
			+ "\nDauer:", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_COARSE = Pattern.compile("<tr class=\"tpDetails\">\n(.*?)\n</tr>\n" //
			+ "<tr class=\"tpDetails(?: special)?\">\n(.*?)\n</tr>\n<tr>\n(.*?)\n</tr>", Pattern.DOTALL);
	private static final Pattern P_CONNECTION_DETAILS_FINE = Pattern.compile(".*?" //
			+ "<td headers=\"hafasDTL\\d+_Stop\"[^>]*>\n" //
			+ "(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn.*?input=.*?%23(\\d+)&[^>]*>)?" // departureId
			+ "([^\n<]*).*?" // departure
			+ "<td headers=\"hafasDTL\\d+_Date\"[^>]*>\n(?:(\\d{2}\\.\\d{2}\\.\\d{2})|&nbsp;)\n</td>.*?" // departureDate
			+ "<td headers=\"hafasDTL\\d+_TimeDep\"[^>]*>(?:(\\d{2}:\\d{2})|&nbsp;)</td>.*?" // departureTime
			+ "<td headers=\"hafasDTL\\d+_Platform\"[^>]*>\\s*(?:&nbsp;|(.*?))\\s*</td>.*?" // departurePosition
			+ "<img src=\"/img/vs_oebb/(\\w+?)_pic.gif\".*?" // lineType
			+ "(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/traininfo\\.exe/dn[^>]*>(.*?)</a>.*?)?" // line
			+ "<td headers=\"hafasDTL\\d+_Stop\"[^>]*>\n" //
			+ "(?:<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn.*?input=.*?%23(\\d+)&[^>]*>)?" // arrivalId
			+ "([^\n<]*).*?" // arrival
			+ "<td headers=\"hafasDTL\\d+_Date\"[^>]*>\n(?:(\\d{2}\\.\\d{2}\\.\\d{2})|&nbsp;)\n</td>.*?" // arrivalDate
			+ "<td headers=\"hafasDTL\\d+_TimeDep\"[^>]*>(?:(\\d{2}:\\d{2})|&nbsp;)</td>.*?" // arrivalTime
			+ "<td headers=\"hafasDTL\\d+_Platform\"[^>]*>\\s*(?:&nbsp;|(.*?))\\s*</td>.*?" // arrivalPosition
			+ "(?:ca\\. (\\d+) Min\\.\n.*?)?" // min
	, Pattern.DOTALL);

	private QueryConnectionsResult queryConnections(final String firstUri, final CharSequence firstPage) throws IOException
	{
		// ugly workaround to fetch all details
		final Matcher mFormAction = P_CONNECTIONS_FORM_ACTION.matcher(firstPage);
		if (!mFormAction.find())
			throw new IOException("cannot find form action in '" + firstPage + "' on " + firstUri);
		final String baseUri = mFormAction.group(1);
		final String query = "sortConnections=minDeparture&guiVCtrl_connection_detailsOut_add_group_overviewOut=yes";
		final CharSequence page = ParserUtils.scrape(baseUri, true, query, null, true);

		final Matcher mError = P_QUERY_CONNECTIONS_ERROR.matcher(page);
		if (mError.find())
		{
			if (mError.group(1) != null)
				return QueryConnectionsResult.NO_CONNECTIONS;
			if (mError.group(2) != null)
				return QueryConnectionsResult.INVALID_DATE;
			if (mError.group(3) != null)
				return QueryConnectionsResult.SESSION_TIMEOUT;
		}

		// parse page
		final Matcher mPage = P_CONNECTIONS_PAGE.matcher(page);
		if (mPage.matches())
		{
			final String action = mPage.group(1);
			final String headSet = mPage.group(2) + mPage.group(4);

			final Matcher mHead = P_CONNECTIONS_HEAD.matcher(headSet);
			if (mHead.matches())
			{
				final String from = ParserUtils.resolveEntities(mHead.group(1));
				final Date currentDate = ParserUtils.parseDate(mHead.group(2));
				final String to = ParserUtils.resolveEntities(mHead.group(3));
				final String linkEarlier = mHead.group(4) != null ? action + "&REQ0HafasScrollDir=2" + ParserUtils.resolveEntities(mHead.group(4))
						: null;
				final String linkLater = mHead.group(5) != null ? action + "&REQ0HafasScrollDir=1" + ParserUtils.resolveEntities(mHead.group(5))
						: null;
				final List<Connection> connections = new ArrayList<Connection>();

				final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(mPage.group(3));
				while (mConCoarse.find())
				{
					final Matcher mConFine = P_CONNECTIONS_FINE.matcher(mConCoarse.group(1));
					if (mConFine.matches())
					{
						final String id = mConFine.group(1);
						final Date departureDate = ParserUtils.parseDate(mConFine.group(2));
						final Date arrivalDate = mConFine.group(3) != null ? ParserUtils.parseDate(mConFine.group(3)) : null;
						final Date departureTime = ParserUtils.joinDateTime(departureDate, ParserUtils.parseTime(mConFine.group(4)));
						final Date arrivalTime = ParserUtils.joinDateTime(arrivalDate != null ? arrivalDate : departureDate, ParserUtils
								.parseTime(mConFine.group(5)));
						final String link = firstUri + "#" + id; // TODO use print link?

						final Connection connection = new Connection(id, link, departureTime, arrivalTime, null, null, 0, from, 0, to,
								new ArrayList<Connection.Part>(1));
						connections.add(connection);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(1) + "' on " + baseUri + " (POST)");
					}
				}

				final Matcher mConDetCoarse = P_CONNECTIONS_DETAILS_COARSE.matcher(mPage.group(5));
				while (mConDetCoarse.find())
				{
					final String id = mConDetCoarse.group(1);
					final Connection connection = findConnection(connections, id);

					Date lastDate = null;

					final Matcher mDetCoarse = P_CONNECTION_DETAILS_COARSE.matcher(mConDetCoarse.group(2));
					while (mDetCoarse.find())
					{
						final String set = mDetCoarse.group(1) + mDetCoarse.group(2) + mDetCoarse.group(3);

						final Matcher mDetFine = P_CONNECTION_DETAILS_FINE.matcher(set);
						if (mDetFine.matches())
						{
							final int departureId = mDetFine.group(1) != null ? Integer.parseInt(mDetFine.group(1)) : 0;

							final String departure = ParserUtils.resolveEntities(mDetFine.group(2));

							Date departureDate = mDetFine.group(3) != null ? ParserUtils.parseDate(mDetFine.group(3)) : null;
							if (departureDate != null)
								lastDate = departureDate;
							else
								departureDate = lastDate;

							final String lineType = mDetFine.group(6);

							final int arrivalId = mDetFine.group(8) != null ? Integer.parseInt(mDetFine.group(8)) : 0;

							final String arrival = ParserUtils.resolveEntities(mDetFine.group(9));

							Date arrivalDate = mDetFine.group(10) != null ? ParserUtils.parseDate(mDetFine.group(10)) : null;
							if (arrivalDate != null)
								lastDate = arrivalDate;
							else
								arrivalDate = lastDate;

							if (!lineType.equals("fuss"))
							{
								if (departureId == 0)
									throw new IllegalStateException("departureId");

								final Date departureTime = ParserUtils.parseTime(mDetFine.group(4));
								final Date departureDateTime = ParserUtils.joinDateTime(departureDate, departureTime);

								final String departurePosition = mDetFine.group(5) != null ? ParserUtils.resolveEntities(mDetFine.group(5)) : null;

								final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDetFine.group(7)));

								if (arrivalId == 0)
									throw new IllegalStateException("arrivalId");

								final Date arrivalTime = ParserUtils.parseTime(mDetFine.group(11));
								final Date arrivalDateTime = ParserUtils.joinDateTime(arrivalDate, arrivalTime);

								final String arrivalPosition = mDetFine.group(12) != null ? ParserUtils.resolveEntities(mDetFine.group(12)) : null;

								final Connection.Trip trip = new Connection.Trip(line, LINES.get(line.charAt(0)), null, departureDateTime,
										departurePosition, departureId, departure, arrivalDateTime, arrivalPosition, arrivalId, arrival);
								connection.parts.add(trip);
							}
							else
							{
								final int min = Integer.parseInt(mDetFine.group(13));

								final Connection.Footway footway = new Connection.Footway(min, departureId, departure, arrivalId, arrival);
								connection.parts.add(footway);
							}
						}
						else
						{
							throw new IllegalArgumentException("cannot parse '" + set + "' on " + baseUri + " (POST)");
						}
					}
				}

				return new QueryConnectionsResult(baseUri, from, to, currentDate, linkEarlier, linkLater, connections);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + headSet + "' on " + baseUri + " (POST)");
			}
		}
		else
		{
			throw new IOException(page.toString());
		}
	}

	private Connection findConnection(final List<Connection> connections, final String id)
	{
		for (final Connection connection : connections)
			if (connection.id.equals(id))
				return connection;

		return null;
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://fahrplan.oebb.at/bin/stboard.exe/dn");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=111111111111");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" // 
			+ "<table class=\"hafasResult\"[^>]*>(.+?)</table>.*?" //
			+ "(?:<table cellspacing=\"0\" class=\"hafasResult\"[^>]*>(.+?)</table>|(verkehren an dieser Haltestelle keine))"//
			+ "|(Eingabe kann nicht interpretiert)|(Verbindung zum Server konnte leider nicht hergestellt werden))" //
			+ ".*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<td class=\"querysummary screennowrap\">\\s*(.*?)\\s*<a.*?" // location
			+ "(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // date
			+ "Abfahrt (\\d+:\\d+).*?" // time
			+ "%23(\\d+)&.*?" // locationId
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"depboard-(\\w*)\">\n(.*?)</tr>", Pattern.DOTALL);
	static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<td class=\"[\\w ]*\">(\\d{1,2}:\\d{2})</td>\n.*?" // plannedTime
			+ "(?:<td class=\"[\\w ]*?prognosis[\\w ]*?\">\n" //
			+ "(?:&nbsp;|<span class=\"rtLimit\\d\">(p&#252;nktlich|\\d{1,2}:\\d{2})</span>)\n</td>\n" // predictedTime
			+ ")?.*?" //
			+ "<img src=\"/img/vs_oebb/(\\w+)_pic\\.gif\"\\s+alt=\"[^\"]*\">\\s*(.*?)\\s*</.*?" // type, line
			+ "<span class=\"bold\">\n" //
			+ "<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn\\?ld=web\\d+&input=[^%]*?(?:%23(\\d+))?&.*?\">" // destinationId
			+ "\\s*(.*?)\\s*</a>\n" // destination
			+ "</span>.*?" //
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		// scrape page
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, Status.NO_INFO);
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(uri, Status.SERVICE_DOWN);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(2)), ParserUtils
						.parseTime(mHeadFine.group(3)));
				final int stationId = Integer.parseInt(mHeadFine.group(4));
				final List<Departure> departures = new ArrayList<Departure>(8);
				String oldZebra = null;

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final String zebra = mDepCoarse.group(1);
					if (oldZebra != null && zebra.equals(oldZebra))
						throw new IllegalArgumentException("missed row? last:" + zebra);
					else
						oldZebra = zebra;

					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(2));
					if (mDepFine.matches())
					{
						final Calendar current = new GregorianCalendar();
						current.setTime(currentTime);
						final Calendar parsed = new GregorianCalendar();
						parsed.setTime(ParserUtils.parseTime(mDepFine.group(1)));
						parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
						parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
						parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
						if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsed.add(Calendar.DAY_OF_MONTH, 1);

						final Date plannedTime = parsed.getTime();

						Date predictedTime = null;
						final String prognosis = ParserUtils.resolveEntities(mDepFine.group(2));
						if (prognosis != null)
						{
							if (prognosis.equals("pünktlich"))
								predictedTime = plannedTime;
							else
								predictedTime = ParserUtils.joinDateTime(currentTime, ParserUtils.parseTime(prognosis));
						}

						final String lineType = mDepFine.group(3);

						final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDepFine.group(4)));

						final int destinationId = mDepFine.group(5) != null ? Integer.parseInt(mDepFine.group(5)) : 0;

						final String destination = ParserUtils.resolveEntities(mDepFine.group(6));

						final String position = mDepFine.group(7) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(7)) : null;

						final Departure dep = new Departure(plannedTime, predictedTime, line, line != null ? LINES.get(line.charAt(0)) : null, null,
								position, destinationId, destination, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, stationId, location, currentTime, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû]+)[\\s-]*(.*)");

	private static String normalizeLine(final String type, final String line)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

		final char normalizedType = normalizeType(type);
		if (normalizedType != 0)
			return normalizedType + strippedLine;

		throw new IllegalStateException("cannot normalize type " + type + " line " + line);
	}

	private static char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("OEC")) // ÖBB-EuroCity
			return 'I';
		if (ucType.equals("OIC")) // ÖBB-InterCity
			return 'I';
		if (ucType.equals("EC")) // EuroCity
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("X")) // Interconnex
			return 'I';
		if (ucType.equals("EN")) // EuroNight
			return 'I';
		if (ucType.equals("CNL")) // CityNightLine
			return 'I';
		if (ucType.equals("DNZ")) // Berlin-Saratov, Berlin-Moskva
			return 'I';
		if (ucType.equals("INT")) // Rußland
			return 'I';
		if (ucType.equals("D")) // Rußland
			return 'I';
		if (ucType.equals("RR")) // Finnland
			return 'I';
		if (ucType.equals("TLK")) // Tanie Linie Kolejowe, Polen
			return 'I';
		if (ucType.equals("EE")) // Rumänien
			return 'I';
		if (ucType.equals("SC")) // SuperCity, Tschechien
			return 'I';
		if (ucType.equals("RJ")) // RailJet, Österreichische Bundesbahnen
			return 'I';
		if (ucType.equals("EST")) // Eurostar Frankreich
			return 'I';
		if (ucType.equals("ALS")) // Spanien
			return 'I';
		if (ucType.equals("ARC")) // Spanien
			return 'I';
		if (ucType.equals("TLG")) // Spanien, Madrid
			return 'I';
		if (ucType.equals("HOT")) // Spanien, Nacht
			return 'I';
		if (ucType.equals("AVE")) // Alta Velocidad Española, Spanien
			return 'I';
		if (ucType.equals("INZ")) // Schweden, Nacht
			return 'I';
		if (ucType.equals("OZ")) // Schweden, Oeresundzug
			return 'I';
		if (ucType.equals("X2")) // Schweden
			return 'I';
		if (ucType.equals("THA")) // Thalys
			return 'I';
		if (ucType.equals("TGV")) // Train à Grande Vitesse
			return 'I';
		if (ucType.equals("LYN")) // Dänemark
			return 'I';
		if (ucType.equals("ARZ")) // Frankreich, Nacht
			return 'I';
		if (ucType.equals("ES")) // Eurostar Italia
			return 'I';
		if (ucType.equals("ICN")) // Italien, Nacht
			return 'I';
		if (ucType.equals("UUU")) // Italien, Nacht
			return 'I';
		if (ucType.equals("RHI")) // ICE
			return 'I';
		if (ucType.equals("RHT")) // TGV
			return 'I';
		if (ucType.equals("TGD")) // TGV
			return 'I';
		if (ucType.equals("ECB")) // EC
			return 'I';
		if (ucType.equals("IRX")) // IC
			return 'I';
		if (ucType.equals("AIR"))
			return 'I';

		if (ucType.equals("R"))
			return 'R';
		if (ucType.equals("REX")) // RegionalExpress
			return 'R';
		if (ucType.equals("ZUG"))
			return 'R';
		if (ucType.equals("EZ")) // Erlebniszug
			return 'R';
		if (ucType.equals("S2")) // Helsinki-Turku
			return 'R';
		if (ucType.equals("RB")) // RegionalBahn
			return 'R';
		if (ucType.equals("RE"))
			return 'R';
		if (ucType.equals("DPN")) // TODO nicht evtl. doch eher ne S-Bahn?
			return 'R';
		if (ucType.equals("VIA"))
			return 'R';
		if (ucType.equals("PCC")) // Polen
			return 'R';
		if (ucType.equals("KM")) // Polen
			return 'R';
		if (ucType.equals("SKM")) // Polen
			return 'R';
		if (ucType.equals("SKW")) // Polen
			return 'R';
		if (ucType.equals("WKD")) // Warszawska Kolej Dojazdowa, Polen
			return 'R';
		if (ucType.equals("IR")) // Polen
			return 'R';
		if (ucType.equals("OS")) // Chop-Cierna nas Tisou
			return 'R';
		if (ucType.equals("SP")) // Polen
			return 'R';
		if (ucType.equals("EX")) // Polen
			return 'R';
		if (ucType.equals("E")) // Budapest, Ungarn
			return 'R';
		if (ucType.equals("IP")) // Ozd, Ungarn
			return 'R';
		if (ucType.equals("ZR")) // Bratislava, Slovakai
			return 'R';
		if (ucType.equals("CAT")) // Stockholm-Arlanda, Arlanda Express
			return 'R';
		if (ucType.equals("RT")) // Deutschland
			return 'R';
		if (ucType.equals("IRE")) // Interregio Express
			return 'R';
		if (ucType.equals("N")) // Frankreich, Tours
			return 'R';
		if (ucType.equals("DPF")) // VX=Vogtland Express
			return 'R';

		if (ucType.equals("S"))
			return 'S';
		if (ucType.equals("RSB")) // Schnellbahn Wien
			return 'S';
		if (ucType.equals("RER")) // Réseau Express Régional, Frankreich
			return 'S';

		if (ucType.equals("U"))
			return 'U';

		if (ucType.equals("STR"))
			return 'T';
		if (ucType.equals("LKB"))
			return 'T';

		if (ucType.equals("BUS"))
			return 'B';
		if (ucType.equals("RFB"))
			return 'B';
		if (ucType.equals("OBU"))
			return 'B';
		if (ucType.equals("AST"))
			return 'B';
		if (ucType.equals("ICB")) // ICBus
			return 'B';
		if (ucType.equals("FB")) // Polen
			return 'B';
		if (ucType.equals("BSV")) // Deutschland
			return 'B';
		if (ucType.equals("LT")) // Linien-Taxi
			return 'B';

		if (ucType.equals("SCH"))
			return 'F';
		if (ucType.equals("AS")) // SyltShuttle
			return 'F';

		if (ucType.equals("SB"))
			return 'C';
		if (ucType.equals("LIF"))
			return 'C';

		if (ucType.equals("U70")) // U.K.
			return '?';
		if (ucType.equals("R84"))
			return '?';
		if (ucType.equals("S84"))
			return '?';
		if (ucType.equals("T84"))
			return '?';

		return 0;
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
		return LINES.get(line.charAt(0));
	}
}
