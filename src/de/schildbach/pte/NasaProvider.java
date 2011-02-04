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
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class NasaProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NASA;
	public static final String OLD_NETWORK_ID = "www.nasa.de";
	private static final String API_BASE = "http://www.nasa.de/delfi52/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public NasaProvider()
	{
		super(null, null);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	@Override
	public List<Location> autocompleteStations(CharSequence constraint) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private final String NEARBY_URI = API_BASE
			+ "stboard.exe/dn?input=%s&selectDate=today&boardType=dep&productsFilter=11111111&distance=50&near=Anzeigen";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, ParserUtils.urlEncode(stationId));
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy");
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
		final Date now = new Date();

		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&time=").append(TIME_FORMAT.format(now));
		uri.append("&date=").append(DATE_FORMAT.format(now));
		uri.append("&productsFilter=11111111");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern
			.compile(
					".*?" //
							+ "(?:" //
							+ "<table class=\"hafasResult\"[^>]*>(.+?)</table>.*?" //
							+ "(?:<table cellspacing=\"0\" class=\"hafasResult\"[^>]*>(.+?)</table>|(verkehren an dieser Haltestelle keine))"//
							+ "|(Eingabe kann nicht interpretiert)|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
							+ ".*?" //
					, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<td class=\"querysummary screennowrap\">\\s*(.*?)\\s*<.*?" // location
			+ "(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // date
			+ "Abfahrt (\\d{1,2}:\\d{2}).*?" // time
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<td class=\"[\\w ]*\">(\\d{1,2}:\\d{2})</td>\n" // plannedTime
			+ "(?:<td class=\"[\\w ]*prognosis[\\w ]*\">\n" //
			+ "(?:&nbsp;|<span class=\"rtLimit\\d\">(p&#252;nktlich|\\d{1,2}:\\d{2})</span>)\n</td>\n" // predictedTime
			+ ")?.*?" //
			+ "<img src=\"/img52/(\\w+)_pic\\.gif\"[^>]*>\\s*(.*?)\\s*</.*?" // type, line
			+ "<span class=\"bold\">\n" //
			+ "<a href=\"/delfi52/stboard\\.exe/dn\\?input=(\\d+)&[^>]*>" // destinationId
			+ "\\s*(.*?)\\s*</a>\n" // destination
			+ "</span>.*?" //
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures) throws IOException
	{
		// scrape page
		final CharSequence page = ParserUtils.scrape(departuresQueryUri(stationId, maxDepartures));

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(new Location(LocationType.STATION, Integer.parseInt(stationId)),
						Collections.<Departure> emptyList(), null);
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(Status.INVALID_STATION, Integer.parseInt(stationId));
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(Status.SERVICE_DOWN, Integer.parseInt(stationId));

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(2)),
						ParserUtils.parseTime(mHeadFine.group(3)));
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

						final Departure dep = new Departure(plannedTime, predictedTime, line, line != null ? lineColors(line) : null, null, position,
								destinationId, destination, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
					}
				}

				return new QueryDeparturesResult(new Location(LocationType.STATION, Integer.parseInt(stationId), null, location), departures, null);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + stationId);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + stationId);
		}
	}

	@Override
	protected char normalizeType(String type)
	{
		final String ucType = type.toUpperCase();

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		if (ucType.equals("D")) // Rußland Schlafwagenzug
			return 'I';

		if (ucType.equals("DPF")) // mit Dampflok bespannter Zug
			return 'R';
		if (ucType.equals("RR")) // Polen
			return 'R';

		if (ucType.equals("E")) // Stadtbahn Karlsruhe: S4/S31/xxxxx
			return 'S';

		if (ucType.equals("BSV"))
			return 'B';
		if (ucType.equals("RBS")) // Rufbus
			return 'B';

		if (ucType.equals("EB")) // Europa-Park, vermutlich "Erlebnisbahn"
			return '?';

		return 0;
	}

	@Override
	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final String products, final WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public QueryConnectionsResult queryMoreConnections(String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
