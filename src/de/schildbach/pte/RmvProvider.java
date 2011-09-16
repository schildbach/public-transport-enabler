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
public class RmvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.RMV;
	public static final String OLD_NETWORK_ID = "mobil.rmv.de";
	private static final String API_BASE = "http://www.rmv.de/auskunft/bin/jp/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public RmvProvider()
	{
		super(API_BASE + "query.exe/dn", 16, null, "UTF-8", null);
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
			productBits.setCharAt(0, '1'); // ICE
			productBits.setCharAt(1, '1'); // Zug, scheinbar IC?
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // Zug
			productBits.setCharAt(10, '1'); // Zug
		}
		else if (product == 'S')
		{
			productBits.setCharAt(3, '1'); // S-Bahn
		}
		else if (product == 'U')
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == 'T')
		{
			productBits.setCharAt(5, '1'); // Straßenbahn
		}
		else if (product == 'B')
		{
			productBits.setCharAt(6, '1'); // Niederflurbus
			productBits.setCharAt(7, '1'); // Bus
		}
		else if (product == 'P')
		{
			productBits.setCharAt(9, '1'); // AST/Rufbus
		}
		else if (product == 'F')
		{
			productBits.setCharAt(8, '1'); // Fähre/Schiff
		}
		else if (product == 'C')
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg", "Kassel", "Hanau", "Göttingen",
			"Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitPlaceAndName(name);
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
			uri.append("&look_nv=get_stopweight|yes");
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/dn?L=vs_rmv&near=Anzeigen");
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
		final Calendar c = new GregorianCalendar(timeZone());

		final StringBuilder uri = new StringBuilder();

		uri.append(API_BASE).append("stboard.exe/dox");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep"); // show departures
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50); // maximum taken from RMV site
		uri.append("&date=").append(
				String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) - 2000));
		uri.append("&time=").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<p class=\"qs\">\n(.*?)</p>\n" // head
			+ "(.*?)<p class=\"links\">.*?" // departures
			+ "input=(\\d+).*?" // locationId
			+ "|(Eingabe kann nicht interpretiert|Eingabe ist nicht eindeutig)" // messages
			+ "|(Internal Error)" // messages
			+ ").*?", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" //
			+ "<b>(.*?)</b><br />.*?" //
			+ "Abfahrt (\\d+:\\d+).*?" //
			+ "Uhr, (\\d+\\.\\d+\\.\\d+).*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"sq\">\n(.+?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("" //
			+ "<b>([^<]*)</b>\n" // line
			+ "&gt;&gt;\n" //
			+ "([^\n]*)\n" // destination
			+ "<br />\n" //
			+ "<b>(\\d{1,2}:\\d{2})</b>\n" // plannedTime
			+ "(?:keine Prognose verf&#252;gbar\n)?" //
			+ "(?:<span class=\"red\">ca\\. (\\d{1,2}:\\d{2})</span>\n)?" // predictedTime
			+ "(?:<span class=\"red\">heute (Gl\\. " + ParserUtils.P_PLATFORM + ")</span><br />\n)?" // predictedPosition
			+ "(?:(Gl\\. " + ParserUtils.P_PLATFORM + ")<br />\n)?" // position
			+ "(?:<span class=\"red\">([^>]*)</span>\n)?" // message
			+ "(?:<img src=\".+?\" alt=\"\" />\n<b>[^<]*</b>\n<br />\n)*" // (messages)
			+ ".*?", Pattern.DOTALL);

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
			if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final int locationId = Integer.parseInt(mHeadCoarse.group(3));

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Calendar currentTime = new GregorianCalendar(timeZone());
				currentTime.clear();
				ParserUtils.parseEuropeanTime(currentTime, mHeadFine.group(2));
				ParserUtils.parseGermanDate(currentTime, mHeadFine.group(3));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						final Calendar plannedTime = new GregorianCalendar(timeZone());
						plannedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(plannedTime, mDepFine.group(3));

						if (plannedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							plannedTime.add(Calendar.DAY_OF_MONTH, 1);

						final Calendar predictedTime;
						if (mDepFine.group(4) != null)
						{
							predictedTime = new GregorianCalendar(timeZone());
							predictedTime.setTimeInMillis(currentTime.getTimeInMillis());
							ParserUtils.parseEuropeanTime(predictedTime, mDepFine.group(4));

							if (predictedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								predictedTime.add(Calendar.DAY_OF_MONTH, 1);
						}
						else
						{
							predictedTime = null;
						}

						final String position = ParserUtils.resolveEntities(ParserUtils.selectNotNull(mDepFine.group(5), mDepFine.group(6)));

						final Departure dep = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, new Line(
								null, line, line != null ? lineColors(line) : null), position, 0, destination, null, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + stationId);
					}
				}

				final String[] placeAndName = splitPlaceAndName(location);
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, locationId, placeAndName[0], placeAndName[1]),
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

	private static final String AUTOCOMPLETE_URI = API_BASE + "ajax-getstop.exe/dn?getstop=1&REQ0JourneyStopsS0A=255&S=%s?&js=true&";
	private static final String ENCODING = "ISO-8859-1";

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String uri = String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));

		return jsonGetStops(uri);
	}

	@Override
	protected String normalizeLine(final String line)
	{
		if (line == null || line.length() == 0)
			return null;

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2).replace(" ", "");

			if (type.equals("ICE")) // InterCityExpress
				return "IICE" + number;
			if (type.equals("IC")) // InterCity
				return "IIC" + number;
			if (type.equals("EC")) // EuroCity
				return "IEC" + number;
			if (type.equals("EN")) // EuroNight
				return "IEN" + number;
			if (type.equals("CNL")) // CityNightLine
				return "ICNL" + number;
			if (type.equals("DNZ")) // Basel-Minsk, Nacht
				return "IDNZ" + number;
			if (type.equals("D")) // Prag-Fulda
				return "ID" + number;
			if (type.equals("RB")) // RegionalBahn
				return "RRB" + number;
			if (type.equals("RE")) // RegionalExpress
				return "RRE" + number;
			if (type.equals("IRE")) // Interregio Express
				return "RIRE" + number;
			if (type.equals("SE")) // StadtExpress
				return "RSE" + number;
			if (type.equals("R"))
				return "R" + number;
			if (type.equals("ZUG"))
				return "R" + number;
			if (type.equals("S"))
				return "SS" + number;
			if (type.equals("U"))
				return "UU" + number;
			if (type.equals("Tram"))
				return "T" + number;
			if (type.equals("RT")) // RegioTram
				return "TRT" + number;
			if (type.startsWith("Bus"))
				return "B" + type.substring(3) + number;
			if (type.equals("B"))
				return "B" + number;
			if (type.equals("BN"))
				return "BN" + number;
			if ("BuFB".equals(type))
				return "BBuFB" + number;
			if ("A".equals(type))
				return "BA" + number;
			if (type.equals("N"))
				return "BN" + number;
			if (type.equals("AS")) // Anruf-Sammel-Taxi
				return "BAS" + number;
			if (type.equals("ASOF")) // Anruf-Sammel-Taxi
				return "BASOF" + number;
			if (type.startsWith("AST")) // Anruf-Sammel-Taxi
				return "BAST" + type.substring(3) + number;
			if (type.startsWith("ALT")) // Anruf-Linien-Taxi
				return "BALT" + type.substring(3) + number;
			if (type.equals("ALFB")) // Anruf-Linien-Taxi
				return "BALFB" + number;
			if (type.equals("LTaxi"))
				return "BLTaxi" + number;
			if (type.equals("AT")) // AnschlußSammelTaxi
				return "BAT" + number;
			if (type.equals("SCH"))
				return "FSCH" + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		if ("11".equals(line))
			return "T11";
		if ("12".equals(line))
			return "T12";

		throw new IllegalStateException("cannot normalize line " + line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("U-BAHN".equals(ucType))
			return 'U';

		if ("LTAXI".equals(ucType))
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
