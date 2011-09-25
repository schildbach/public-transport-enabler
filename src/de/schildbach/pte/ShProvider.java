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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class ShProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SH;
	private static final String API_BASE = "http://scout.hafas.de/bin/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public ShProvider()
	{
		super(API_BASE + "query.exe/dn", 10, null, null, "UTF-8");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(1, '1'); // IC/EC
			productBits.setCharAt(2, '1'); // Fernverkehrszug
		}
		else if (product == 'R')
		{
			productBits.setCharAt(3, '1'); // Regionalverkehrszug
		}
		else if (product == 'S')
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(7, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
			productBits.setCharAt(8, '1'); // Stadtbahn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(5, '1'); // Bus
		}
		else if (product == 'P')
		{
			productBits.setCharAt(9, '1'); // Anruf-Sammel-Taxi
		}
		else if (product == 'F')
		{
			productBits.setCharAt(6, '1'); // Schiff
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/dn?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	private String departuresQueryUri(final int stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=").append(allProductsString());
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "Bhf\\./Haltest\\.:</span>\\n<span class=\"output\">([^<]*)<.*?" // location
			+ "Fahrplan:</span>\\n<span class=\"output\">\n" //
			+ "(\\d{2}\\.\\d{2}\\.\\d{2})[^\n]*,\n" // date
			+ "Abfahrt (\\d{1,2}:\\d{2}).*?" // time
			+ "<table class=\"resultTable\"[^>]*>(.+?)</table>" // content
			+ "|(verkehren an dieser Haltestelle keine)" //
			+ "|(Eingabe kann nicht interpretiert)" //
			+ "|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
			+ ".*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("\n" //
			+ "<td class=\"time\">(\\d{1,2}:\\d{2}).*?" // plannedTime
			+ "<img class=\"product\" src=\"/hafas-res/[^\"]*?(\\w+)_pic\\.png\"[^>]*>\\s*([^<]*)<.*?" // type,line
			+ "<a href=\"http://nah\\.sh\\.hafas\\.de/bin/stboard\\.exe/dn\\?input=(\\d+)&[^>]*>\n" // destinationId
			+ "([^\n]*)\n.*?" // destination
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final ResultHeader header = new ResultHeader(SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		// scrape page
		final String uri = departuresQueryUri(stationId, maxDepartures);
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(5) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mHeadCoarse.group(7) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final String location = ParserUtils.resolveEntities(mHeadCoarse.group(1));
			final Calendar currentTime = new GregorianCalendar(timeZone());
			currentTime.clear();
			ParserUtils.parseGermanDate(currentTime, mHeadCoarse.group(2));
			ParserUtils.parseEuropeanTime(currentTime, mHeadCoarse.group(3));

			final List<Departure> departures = new ArrayList<Departure>(8);
			String oldZebra = null;

			final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(4));
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

					final String lineType = mDepFine.group(2);

					final Line line = parseLine(lineType, ParserUtils.resolveEntities(mDepFine.group(3).trim()));

					final int destinationId = mDepFine.group(4) != null ? Integer.parseInt(mDepFine.group(4)) : 0;

					final String destination = ParserUtils.resolveEntities(mDepFine.group(5));

					final String position = mDepFine.group(6) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(6)) : null;

					final Departure dep = new Departure(plannedTime.getTime(), null, line, position, destinationId, destination, null, null);

					if (!departures.contains(dep))
						departures.add(dep);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
				}
			}

			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, null, location), departures, null));
			return result;
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + stationId);
		}
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("KBS".equals(ucType))
			return 'B';
		if ("KB1".equals(ucType))
			return 'B';
		if ("KLB".equals(ucType))
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
