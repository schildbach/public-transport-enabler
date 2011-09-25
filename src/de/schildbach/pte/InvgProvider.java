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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class InvgProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.INVG;
	private static final String API_BASE = "http://fpa.invg.de/bin/";
	private static final String API_URI = "http://fpa.invg.de/bin/extxml.exe";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public InvgProvider()
	{
		super(API_URI, 10, null);
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
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		throw new UnsupportedOperationException();
	}

	private static final String[] PLACES = { "Ingolstadt", "München" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
			else if (name.startsWith(place + ", "))
				return new String[] { place, name.substring(place.length() + 2) };
		}

		return super.splitPlaceAndName(name);
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

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern
			.compile(
					".*?" //
							+ "(?:" //
							+ "<div class=\"summary clearfix\">.*?<div class=\"block\">.*?(<div>.*?</div>.*?<div class=\"last\">.*?</div>).*?</div>.*?" //
							+ "<div class=\"linkGroup\">.*?input=(\\d+).*?" // locationId
							+ "(?:<table class=\"resultTable\" cellspacing=\"0\">(.*?)</table>|(verkehren an dieser Haltestelle keine))"//
							+ "|(Eingabe kann nicht interpretiert)|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
							+ ".*?" //
					, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<span class=\"output\">(.*?)<.*?" // location
			+ "<span class=\"output\">\n(\\d{2}\\.\\d{2}\\.\\d{2}),\n" // date
			+ "Abfahrt (\\d{1,2}:\\d{2}).*?" // time
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<td class=\"time\">(\\d{1,2}:\\d{2})</td>\n" // plannedTime
			+ "(?:<td class=\"[\\w ]*prognosis[\\w ]*\">\n" //
			+ "(?:&nbsp;|<span class=\"rtLimit\\d\">(p&#252;nktlich|\\d{1,2}:\\d{2})</span>)\n</td>\n" // predictedTime
			+ ")?.*?" //
			+ "<img class=\"product\" src=\"/hafas-res/img/products/(\\w+)_pic\\.gif\"[^>]*>\\s*(.*?)\\s*</.*?" // type,
																												// line
			+ "<strong>\n" //
			+ "<a href=\"http://fpa\\.invg\\.de/bin/stboard\\.exe/dn\\?input=(\\d+)&[^>]*>" // destinationId
			+ "\\s*(.*?)\\s*</a>\n" // destination
			+ "</strong>.*?" //
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
			if (mHeadCoarse.group(4) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			else if (mHeadCoarse.group(5) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final int locationId = Integer.parseInt(mHeadCoarse.group(2));

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

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(3));
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
							if (prognosis.equals("pünktlich"))
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

						final Line line = parseLine(lineType, ParserUtils.resolveEntities(mDepFine.group(4)));

						final int destinationId = mDepFine.group(5) != null ? Integer.parseInt(mDepFine.group(5)) : 0;

						final String destination = ParserUtils.resolveEntities(mDepFine.group(6));

						final String position = mDepFine.group(7) != null ? "Gl. " + ParserUtils.resolveEntities(mDepFine.group(7)) : null;

						final Departure dep = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
								position, destinationId, destination, null, null);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
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

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	protected static final Pattern P_NORMALIZE_LINE_BUS = Pattern.compile("Bus\\s*(\\d+)");
	protected static final Pattern P_NORMALIZE_LINE_NACHTBUS = Pattern.compile("Bus\\s*N\\s*(\\d+)");
	protected static final Pattern P_NORMALIZE_LINE_BUS_S = Pattern.compile("Bus\\s*S\\s*(\\d+)");
	protected static final Pattern P_NORMALIZE_LINE_BUS_X = Pattern.compile("Bus\\s*X\\s*(\\d+)");

	@Override
	protected Line parseLine(final String type, final String line)
	{
		if ("1".equals(type))
		{
			final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(line);
			if (mBus.matches())
			{
				final String lineStr = "B" + mBus.group(1);
				return new Line(null, lineStr, lineColors(lineStr));
			}

			final Matcher mNachtbus = P_NORMALIZE_LINE_NACHTBUS.matcher(line);
			if (mNachtbus.matches())
			{
				final String lineStr = "BN" + mNachtbus.group(1);
				return new Line(null, lineStr, lineColors(lineStr));
			}

			final Matcher mBusS = P_NORMALIZE_LINE_BUS_S.matcher(line);
			if (mBusS.matches())
			{
				final String lineStr = "BS" + mBusS.group(1);
				return new Line(null, lineStr, lineColors(lineStr));
			}

			final Matcher mBusX = P_NORMALIZE_LINE_BUS_X.matcher(line);
			if (mBusX.matches())
			{
				final String lineStr = "BX" + mBusX.group(1);
				return new Line(null, lineStr, lineColors(lineStr));
			}
		}

		return super.parseLine(type, line);
	}

	@Override
	protected char normalizeType(final String type)
	{
		if ("1".equals(type))
			return 'B';

		return 0;
	}

	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();

	static
	{
		LINES.put("B10", new int[] { Color.parseColor("#DA2510"), Color.WHITE });
		LINES.put("B11", new int[] { Color.parseColor("#EE9B78"), Color.BLACK });
		LINES.put("B15", new int[] { Color.parseColor("#84C326"), Color.BLACK });
		LINES.put("B16", new int[] { Color.parseColor("#5D452E"), Color.WHITE });
		LINES.put("B17", new int[] { Color.parseColor("#AAAAAA"), Color.BLACK });
		LINES.put("B20", new int[] { Color.parseColor("#EA891C"), Color.BLACK });
		LINES.put("B21", new int[] { Color.parseColor("#31B2EA"), Color.BLACK });
		LINES.put("B25", new int[] { Color.parseColor("#7F65A0"), Color.WHITE });
		LINES.put("B26", new int[] { Color.parseColor("#00BF73"), Color.WHITE });
		LINES.put("B30", new int[] { Color.parseColor("#901E78"), Color.WHITE });
		LINES.put("B31", new int[] { Color.parseColor("#DCE722"), Color.BLACK });
		LINES.put("B40", new int[] { Color.parseColor("#009240"), Color.WHITE });
		LINES.put("B41", new int[] { Color.parseColor("#7BC5B1"), Color.BLACK });
		LINES.put("B44", new int[] { Color.parseColor("#EA77A6"), Color.WHITE });
		LINES.put("B50", new int[] { Color.parseColor("#FACF00"), Color.BLACK });
		LINES.put("B53", new int[] { Color.parseColor("#BEB405"), Color.BLACK });
		LINES.put("B55", new int[] { Color.parseColor("#FFF500"), Color.BLACK });
		LINES.put("B60", new int[] { Color.parseColor("#0072B7"), Color.WHITE });
		LINES.put("B61", new int[] { Color.rgb(204, 184, 122), Color.BLACK });
		LINES.put("B62", new int[] { Color.rgb(204, 184, 122), Color.BLACK });
		LINES.put("B65", new int[] { Color.parseColor("#B7DDD2"), Color.BLACK });
		LINES.put("B70", new int[] { Color.parseColor("#D49016"), Color.BLACK });
		LINES.put("B71", new int[] { Color.parseColor("#996600"), Color.BLACK });
		LINES.put("B85", new int[] { Color.parseColor("#F6BAD3"), Color.BLACK });
		LINES.put("B9221", new int[] { Color.rgb(217, 217, 255), Color.BLACK });
		LINES.put("B9226", new int[] { Color.rgb(191, 255, 255), Color.BLACK });

		LINES.put("BN1", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN2", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN3", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN4", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN5", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN6", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN7", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN8", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN9", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN10", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN11", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN12", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN13", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN14", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN15", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN16", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN17", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN18", new int[] { Color.parseColor("#00116C"), Color.WHITE });
		LINES.put("BN19", new int[] { Color.parseColor("#00116C"), Color.WHITE });

		LINES.put("BS1", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS2", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS3", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS4", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS5", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS6", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS7", new int[] { Color.rgb(178, 25, 0), Color.WHITE });
		LINES.put("BS8", new int[] { Color.rgb(178, 25, 0), Color.WHITE });

		// BX109?
		LINES.put("BX11", new int[] { Color.parseColor("#EE9B78"), Color.BLACK });
		LINES.put("BX80", new int[] { Color.parseColor("#FFFF40"), Color.BLACK });
	}

	@Override
	public int[] lineColors(final String line)
	{
		final int[] lineColors = LINES.get(line);
		if (lineColors != null)
			return lineColors;
		else
			return super.lineColors(line);
	}
}
