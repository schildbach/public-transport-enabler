/*
 * Copyright 2010-2014 the original author or authors.
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
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class InvgProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.INVG;
	private static final String API_BASE = "http://fpa.invg.de/bin/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public InvgProvider()
	{
		super(API_BASE + "stboard.exe/dn", null, API_BASE + "query.exe/dn", 10);

		setStyles(STYLES);
		setExtXmlEndpoint(API_BASE + "extxml.exe");
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
	protected void setProductBits(final StringBuilder productBits, final Product product)
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
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(location.id);

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	private String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
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
			+ "<span class=\"output\">\n(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // date
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

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
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

						final Line line = parseLine(lineType, ParserUtils.resolveEntities(mDepFine.group(4)), false);

						final int destinationId = mDepFine.group(5) != null ? Integer.parseInt(mDepFine.group(5)) : 0;
						final String destinationName = ParserUtils.resolveEntities(mDepFine.group(6));
						final Location destination = new Location(destinationId > 0 ? LocationType.STATION : LocationType.ANY, destinationId, null,
								destinationName);

						final Position position = mDepFine.group(7) != null ? new Position("Gl. " + ParserUtils.resolveEntities(mDepFine.group(7)))
								: null;

						final Departure dep = new Departure(plannedTime.getTime(), predictedTime != null ? predictedTime.getTime() : null, line,
								position, destination, null, null);

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
	protected Line parseLine(final String type, final String line, final boolean wheelchairAccess)
	{
		if ("1".equals(type))
		{
			final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(line);
			if (mBus.matches())
			{
				final String lineStr = "B" + mBus.group(1);
				return new Line(null, lineStr, lineStyle(null, lineStr));
			}

			final Matcher mNachtbus = P_NORMALIZE_LINE_NACHTBUS.matcher(line);
			if (mNachtbus.matches())
			{
				final String lineStr = "BN" + mNachtbus.group(1);
				return new Line(null, lineStr, lineStyle(null, lineStr));
			}

			final Matcher mBusS = P_NORMALIZE_LINE_BUS_S.matcher(line);
			if (mBusS.matches())
			{
				final String lineStr = "BS" + mBusS.group(1);
				return new Line(null, lineStr, lineStyle(null, lineStr));
			}

			final Matcher mBusX = P_NORMALIZE_LINE_BUS_X.matcher(line);
			if (mBusX.matches())
			{
				final String lineStr = "BX" + mBusX.group(1);
				return new Line(null, lineStr, lineStyle(null, lineStr));
			}
		}

		return super.parseLine(type, line, wheelchairAccess);
	}

	@Override
	protected char normalizeType(final String type)
	{
		if ("1".equals(type))
			return 'B';

		return 0;
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		STYLES.put("B10", new Style(Style.parseColor("#DA2510"), Style.WHITE));
		STYLES.put("B11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
		STYLES.put("B15", new Style(Style.parseColor("#84C326"), Style.BLACK));
		STYLES.put("B16", new Style(Style.parseColor("#5D452E"), Style.WHITE));
		STYLES.put("B17", new Style(Style.parseColor("#E81100"), Style.BLACK));
		STYLES.put("B18", new Style(Style.parseColor("#79316C"), Style.WHITE));
		STYLES.put("B20", new Style(Style.parseColor("#EA891C"), Style.BLACK));
		STYLES.put("B21", new Style(Style.parseColor("#31B2EA"), Style.BLACK));
		STYLES.put("B25", new Style(Style.parseColor("#7F65A0"), Style.WHITE));
		STYLES.put("B26", new Style(Style.parseColor("#00BF73"), Style.WHITE)); // not present in Fahrplan 2012/2013
		STYLES.put("B30", new Style(Style.parseColor("#901E78"), Style.WHITE));
		STYLES.put("B31", new Style(Style.parseColor("#DCE722"), Style.BLACK));
		STYLES.put("B40", new Style(Style.parseColor("#009240"), Style.WHITE));
		STYLES.put("B41", new Style(Style.parseColor("#7BC5B1"), Style.BLACK));
		STYLES.put("B44", new Style(Style.parseColor("#EA77A6"), Style.WHITE));
		STYLES.put("B50", new Style(Style.parseColor("#FACF00"), Style.BLACK));
		STYLES.put("B51", new Style(Style.parseColor("#C13C00"), Style.WHITE));
		STYLES.put("B52", new Style(Style.parseColor("#94F0D4"), Style.BLACK));
		STYLES.put("B53", new Style(Style.parseColor("#BEB405"), Style.BLACK));
		STYLES.put("B55", new Style(Style.parseColor("#FFF500"), Style.BLACK));
		STYLES.put("B60", new Style(Style.parseColor("#0072B7"), Style.WHITE));
		STYLES.put("B61", new Style(Style.rgb(204, 184, 122), Style.BLACK)); // not present in Fahrplan 2012/2013
		STYLES.put("B62", new Style(Style.rgb(204, 184, 122), Style.BLACK)); // not present in Fahrplan 2012/2013
		STYLES.put("B65", new Style(Style.parseColor("#B7DDD2"), Style.BLACK));
		STYLES.put("B70", new Style(Style.parseColor("#D49016"), Style.BLACK));
		STYLES.put("B71", new Style(Style.parseColor("#996600"), Style.BLACK)); // not present in Fahrplan 2012/2013
		STYLES.put("B85", new Style(Style.parseColor("#F6BAD3"), Style.BLACK));
		STYLES.put("B111", new Style(Style.parseColor("#EE9B78"), Style.BLACK));

		STYLES.put("B9221", new Style(Style.rgb(217, 217, 255), Style.BLACK));
		STYLES.put("B9226", new Style(Style.rgb(191, 255, 255), Style.BLACK));

		STYLES.put("BN1", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN2", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN3", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN4", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN5", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN6", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN7", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN8", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN9", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN10", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN11", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN12", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN13", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN14", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN15", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN16", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN17", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN18", new Style(Style.parseColor("#00116C"), Style.WHITE));
		STYLES.put("BN19", new Style(Style.parseColor("#00116C"), Style.WHITE));

		STYLES.put("BS1", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS2", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS3", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS4", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS5", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS6", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS7", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS8", new Style(Style.rgb(178, 25, 0), Style.WHITE));
		STYLES.put("BS9", new Style(Style.rgb(178, 25, 0), Style.WHITE));

		STYLES.put("BX11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
		STYLES.put("BX12", new Style(Style.parseColor("#B11839"), Style.BLACK));
		STYLES.put("BX80", new Style(Style.parseColor("#FFFF40"), Style.BLACK));
		STYLES.put("BX109", new Style(Style.WHITE, Style.BLACK, Style.BLACK));
	}
}
