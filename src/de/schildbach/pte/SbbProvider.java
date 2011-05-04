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
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SbbProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SBB;
	public static final String OLD_NETWORK_ID = "fahrplan.sbb.ch";
	private static final String API_BASE = "http://fahrplan.sbb.ch/bin/";
	private static final String API_URI = "http://fahrplan.sbb.ch/bin/extxml.exe"; // xmlfahrplan.sbb.ch

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public SbbProvider(final String accessId)
	{
		super(API_URI, accessId);
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

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	private final static String NEARBY_URI = API_BASE + "bhftafel.exe/dn?input=%s&distance=50&near=Anzeigen";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, ParserUtils.urlEncode(stationId));
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("bhftafel.exe/dox");
		uri.append("?start=");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=1111111111000000");
		uri.append("&input=").append(stationId);
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<p class=\"querysummary\">\n(.*?)</p>\n" // head
			+ "(?:(.*?)|(an dieser Haltestelle keines))\n" // departures
			+ "<p class=\"link1\">\n(.*?)</p>\n" // footer
			+ "|(Informationen zu)" // messages
			+ "|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden)" // messages
			+ ").*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" //
			+ "<strong>([^<]*)</strong>(?:<br />)?\n" // location
			+ "Abfahrt (\\d{1,2}:\\d{2})\n" // time
			+ "Uhr, (\\d{2}\\.\\d{2}\\.\\d{2})\n" // date
			+ ".*?input=(\\d+)&.*?" // locationId
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"(journey con_\\d+)\">\n(.*?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<strong>(.*?)</strong>\n" // line
			+ "&gt;&gt;\n" //
			+ "(.*?)\n" // destination
			+ "<br />\n" //
			+ "<strong>(\\d+:\\d+)</strong>\n" // time
			+ "(?:Gl\\. (" + ParserUtils.P_PLATFORM + ")\n)?" // position
			+ ".*?" //
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
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(Status.INVALID_STATION);
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult(Status.SERVICE_DOWN);

			final String head = mHeadCoarse.group(1) + mHeadCoarse.group(4);
			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(head);
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				ParserUtils.parseEuropeanTime(currentTime, mHeadFine.group(2));
				ParserUtils.parseGermanDate(currentTime, mHeadFine.group(3));
				final int locationId = Integer.parseInt(mHeadFine.group(4));
				final List<Departure> departures = new ArrayList<Departure>(8);
				// String oldZebra = null;

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					// zebra mechanism is currently broken on service
					// final String zebra = mDepCoarse.group(1);
					// if (oldZebra != null && zebra.equals(oldZebra))
					// throw new IllegalArgumentException("missed row? last:" + zebra);
					// else
					// oldZebra = zebra;

					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(2));
					if (mDepFine.matches())
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						final Calendar parsedTime = new GregorianCalendar(timeZone());
						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(3));

						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsedTime.add(Calendar.DAY_OF_MONTH, 1);

						final String position = ParserUtils.resolveEntities(mDepFine.group(4));

						final Departure dep = new Departure(parsedTime.getTime(), line, line != null ? lineColors(line) : null, position, 0,
								destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, locationId, null, location), departures, null));
				return result;
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + head + "' on " + stationId);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + stationId);
		}
	}

	private static final Pattern P_NORMALIZE_TYPE_SBAHN = Pattern.compile("SN?\\d*");
	private static final Pattern P_NORMALIZE_TYPE_BUS = Pattern.compile("BUS\\w*");

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		final char t = normalizeCommonTypes(ucType);
		if (t != 0)
			return t;

		if (ucType.equals("ICN")) // Intercity-Neigezug, Schweiz
			return 'I';
		if (ucType.equals("X")) // InterConnex
			return 'I';
		if (ucType.equals("ES")) // Eurostar Italia
			return 'I';
		if (ucType.equals("EST")) // Eurostar Frankreich
			return 'I';
		if (ucType.equals("NZ")) // Nachtzug?
			return 'I';
		if (ucType.equals("IN")) // Oslo
			return 'I';
		if (ucType.equals("AVE")) // Alta Velocidad Española, Spanien
			return 'I';
		if (ucType.equals("TAL")) // Talgo, Spanien
			return 'I';
		if (ucType.equals("EM")) // Barcelona-Alicante, Spanien
			return 'I';
		if (ucType.equals("FYR")) // Fyra, Amsterdam-Schiphol-Rotterdam
			return 'I';
		if (ucType.equals("ARZ")) // Frankreich, Nacht
			return 'I';

		if (ucType.equals("D"))
			return 'R';
		if (ucType.equals("E"))
			return 'R';
		if (ucType.equals("EXT"))
			return 'R';
		if (ucType.equals("ATZ"))
			return 'R';
		if (ucType.equals("RSB"))
			return 'R';
		if (ucType.equals("SN"))
			return 'R';
		if (ucType.equals("CAT")) // City Airport Train Wien
			return 'R';
		if (ucType.equals("ALS")) // Spanien
			return 'R';
		if (ucType.equals("ARC")) // Spanien
			return 'R';
		if (ucType.equals("ATR")) // Spanien
			return 'R';

		if (P_NORMALIZE_TYPE_SBAHN.matcher(ucType).matches())
			return 'S';

		if (ucType.equals("MET")) // Lausanne
			return 'U';

		if (ucType.equals("TRAM"))
			return 'T';
		if (ucType.equals("TRA"))
			return 'T';
		if (ucType.equals("M")) // Lausanne
			return 'T';
		if (ucType.equals("T"))
			return 'T';
		if (ucType.equals("NTR"))
			return 'T';

		if (ucType.equals("TRO"))
			return 'B';
		if (ucType.equals("NTO")) // Niederflurtrolleybus zwischen Bern, Bahnhofsplatz und Bern, Wankdorf Bahnhof
			return 'B';
		if (ucType.equals("NFB"))
			return 'B';
		if (ucType.equals("NBU"))
			return 'B';
		if (ucType.equals("MIN"))
			return 'B';
		if (ucType.equals("MID"))
			return 'B';
		if (ucType.equals("N"))
			return 'B';
		if (ucType.equals("TX"))
			return 'B';
		if (ucType.equals("TAXI"))
			return 'B';
		if (ucType.equals("BUXI"))
			return 'B';
		if (P_NORMALIZE_TYPE_BUS.matcher(ucType).matches())
			return 'B';

		if (ucType.equals("BAT"))
			return 'F';
		if (ucType.equals("BAV"))
			return 'F';
		if (ucType.equals("FAE"))
			return 'F';
		if (ucType.equals("KAT")) // z.B. Friedrichshafen <-> Konstanz
			return 'F';

		if (ucType.equals("GB")) // Gondelbahn
			return 'C';
		if (ucType.equals("SL")) // Sessel-Lift
			return 'C';
		if (ucType.equals("LB"))
			return 'C';
		if (ucType.equals("FUN")) // Standseilbahn
			return 'C';

		if (ucType.equals("P"))
			return '?';

		return 0;
	}
}
