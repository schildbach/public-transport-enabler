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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Autocomplete;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.StationLocationResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class TflProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "journeyplanner.tfl.gov.uk";

	public boolean hasCapabilities(final Capability... capabilities)
	{
		return false;
	}

	public List<Autocomplete> autocompleteStations(final CharSequence constraint) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep, final WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://journeyplanner.tfl.gov.uk/user/XSLT_DM_REQUEST?typeInfo_dm=stopID&mode=direct");
		uri.append("&nameInfo_dm=").append(stationId);
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<div id=\"main-content\">(.*?)" // head
			+ "<table id=\"timetable\">(.*?)</table>.*?" // departures
			+ "|(hat diesen Ort nicht gefunden|Geben Sie einen Bahnhof)" // messages
			+ ").*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<h2>\\s*(?:Selected )?[Dd]epartures from: (.*?)</h2>.*?" // location
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr>(<td>\\d.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<td>(\\d{1,2}:\\d{2})</td>" // time
			+ "<td><img src=\"assets/images/icon-\\w*.gif\" alt=\"(\\w*)\" hspace=\"1\" /><br />" // lineType
			+ "(?:<span class=\"\\w*\">)?(.*?)(?:</span>)?</td>" // line
			+ "<td>(.*?)</td>" // destination
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final List<Departure> departures = new ArrayList<Departure>(8);
				final Date now = new Date();

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final Date departureTime = ParserUtils.parseTime(mDepFine.group(1));
						final Date departureDateTime = ParserUtils.joinDateTime(now, departureTime);

						final String lineType = ParserUtils.resolveEntities(mDepFine.group(2));

						final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDepFine.group(3)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(4));

						final Departure dep = new Departure(departureDateTime, line, line != null ? LINES.get(line.charAt(0)) : null, null, 0,
								destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, 0, location, departures);
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

	private String normalizeLine(final String type, final String line)
	{
		final char normalizedType = normalizeType(type);
		final String normalizedLine = normalizeLine(line);

		if (normalizedType != 0 && normalizedLine != null)
			return normalizedType + normalizedLine;

		throw new IllegalStateException("cannot normalize type '" + type + "' line '" + line + "'");
	}

	private char normalizeType(final String type)
	{
		if (type.equals("Tube"))
			return 'U';
		if (type.equals("DLR")) // Docklands Light Railway
			return 'U';
		if (type.equals("Bus"))
			return 'B';
		if (type.equals("Tram"))
			return 'T';
		if (type.equals("Rail"))
			return '?';

		return 0;
	}

	private static final Pattern P_LINE = Pattern.compile("(.*?) Line");
	private static final Pattern P_ROUTE = Pattern.compile("Route (.*?)");

	private String normalizeLine(final String line)
	{
		if (line.length() == 0)
			return "";
		if (line.equals("Docklands Light Railway"))
			return "DLR";
		if (line.equals("London Tramlink"))
			return "Tramlink";

		final Matcher mLine = P_LINE.matcher(line);
		if (mLine.matches())
			return mLine.group(1);
		final Matcher mRoute = P_ROUTE.matcher(line);
		if (mRoute.matches())
			return mRoute.group(1);

		return null;
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
