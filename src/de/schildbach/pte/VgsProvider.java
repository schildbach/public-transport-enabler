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
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class VgsProvider extends AbstractHafasProvider
{
	public static final String OLD_NETWORK_ID = "www.vgs-online.de";
	private static final String API_BASE = "http://www.vgs-online.de/cgi-bin/"; // "http://www.saarfahrplan.de/cgi-bin/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public VgsProvider()
	{
		super(null, null);
	}

	public NetworkId id()
	{
		return NetworkId.VGS;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES)
				return true;

		return false;
	}

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "ajax-getstop.exe/eny?start=1&tpl=suggest2json&REQ0JourneyStopsS0A=1&getstop=1&noSession=yes&REQ0JourneyStopsB=12&REQ0JourneyStopsS0G=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	@Override
	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return ajaxGetStops(uri);
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
		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=11111111111");
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
			+ "<img src=\"/hafas-res/img/(\\w+)_pic\\.gif\"[^>]*>\\s*(.*?)\\s*</.*?" // type, line
			+ "<span class=\"bold\">\n" //
			+ "<a href=\"http://www\\.vgs-online\\.de/cgi-bin/stboard\\.exe/dn\\?input=(\\d+)&[^>]*>" // destinationId
			+ "\\s*(.*?)\\s*</a>\n" // destination
			+ "</span>.*?" //
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final QueryDeparturesResult result = new QueryDeparturesResult();

		// scrape page
		final String uri = departuresQueryUri(stationId, maxDepartures);
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, Integer.parseInt(stationId)), Collections
						.<Departure> emptyList(), null));
				return result;
			}
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(Status.INVALID_STATION);
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(Status.SERVICE_DOWN);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				ParserUtils.parseGermanDate(currentTime, mHeadFine.group(2));
				ParserUtils.parseEuropeanTime(currentTime, mHeadFine.group(3));
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
						final Calendar plannedTime = new GregorianCalendar(timeZone());
						plannedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(plannedTime, mDepFine.group(1));

						if (plannedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							plannedTime.add(Calendar.DAY_OF_MONTH, 1);

						final Calendar predictedTime;
						final String prognosis = ParserUtils.resolveEntities(mDepFine.group(2));
						if (prognosis != null)
						{
							predictedTime = new GregorianCalendar(timeZone());
							if (!prognosis.equals("pünktlich"))
							{
								predictedTime.setTimeInMillis(plannedTime.getTimeInMillis());
							}
							else
							{
								predictedTime.setTimeInMillis(currentTime.getTimeInMillis());
								ParserUtils.parseEuropeanTime(predictedTime, prognosis);
							}
						}
						else
						{
							predictedTime = null;
						}

						final String lineType = mDepFine.group(3);

						final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDepFine.group(4)));

						final int destinationId = mDepFine.group(5) != null ? Integer.parseInt(mDepFine.group(5)) : 0;

						final String destination = ParserUtils.resolveEntities(mDepFine.group(6));

						final String position = mDepFine.group(7) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(7)) : null;

						final Departure dep = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
								line != null ? lineColors(line) : null, null, position, destinationId, destination, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, Integer.parseInt(stationId), null, location),
						departures, null));
				return result;
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

		if (ucType.equals("INT")) // Zürich-Brüssel
			return 'I';

		if (ucType.equals("SBS"))
			return 'S';
		if (ucType.equals("E")) // Stadtbahn Karlsruhe: S4/S31/xxxxx
			return 'S';

		if (ucType.equals("BSS"))
			return 'B';
		if (ucType.equals("BOV"))
			return 'B';

		if (ucType.equals("T84")) // U.K.
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
