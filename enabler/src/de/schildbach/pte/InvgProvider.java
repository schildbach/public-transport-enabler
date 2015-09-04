/*
 * Copyright 2010-2015 the original author or authors.
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class InvgProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://fpa.invg.de/bin/";
	// http://invg.hafas.de/bin/
	private static final Product[] PRODUCTS_MAP = { null, null, null, null, null, null, null, null, null, null };
	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public InvgProvider()
	{
		super(NetworkId.INVG, API_BASE, "dn", PRODUCTS_MAP);

		setStationBoardCanDoEquivs(false);
		setJsonGetStopsEncoding(Charsets.UTF_8);
		setJsonNearbyLocationsEncoding(Charsets.UTF_8);
		setStyles(STYLES);
		setExtXmlEndpoint(API_BASE + "extxml.exe");
	}

	@Override
	protected boolean hasCapability(final Capability capability)
	{
		if (capability == Capability.TRIPS)
			return false;
		else
			return super.hasCapability(capability);
	}

	private static final String[] PLACES = { "Ingolstadt", "München" };

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		for (final String place : PLACES)
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitPOI(final String poi)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(poi);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(1), m.group(2) };

		return super.splitStationName(address);
	}

	@Override
	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxStations) throws IOException
	{
		if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(normalizeStationId(location.id));

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location);
		}
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

	@Override
	public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time, final int maxDepartures, final boolean equivs)
			throws IOException
	{
		checkNotNull(Strings.emptyToNull(stationId));

		final ResultHeader header = new ResultHeader(network, SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		// scrape page
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		appendXmlStationBoardParameters(uri, time, stationId, maxDepartures, false, null);
		final CharSequence page = httpClient.get(uri.toString());

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

			final String locationId = mHeadCoarse.group(2);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String[] placeAndName = splitStationName(ParserUtils.resolveEntities(mHeadFine.group(1)));
				final Calendar currentTime = new GregorianCalendar(timeZone);
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
						final Calendar plannedTime = new GregorianCalendar(timeZone);
						plannedTime.setTimeInMillis(currentTime.getTimeInMillis());
						ParserUtils.parseEuropeanTime(plannedTime, mDepFine.group(1));

						if (plannedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							plannedTime.add(Calendar.DAY_OF_MONTH, 1);

						final Calendar predictedTime;
						final String prognosis = ParserUtils.resolveEntities(mDepFine.group(2));
						if (prognosis != null)
						{
							predictedTime = new GregorianCalendar(timeZone);
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

						final String destinationId = mDepFine.group(5);
						final String destinationName = ParserUtils.resolveEntities(mDepFine.group(6));
						final Location destination;
						if (destinationId != null)
						{
							final String[] destinationPlaceAndName = splitStationName(destinationName);
							destination = new Location(LocationType.STATION, destinationId, destinationPlaceAndName[0], destinationPlaceAndName[1]);
						}
						else
						{
							destination = new Location(LocationType.ANY, null, null, destinationName);
						}

						final Position position = parsePosition(ParserUtils.resolveEntities(mDepFine.group(7)));

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

	@Override
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options) throws IOException
	{
		return queryTripsXml(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return queryMoreTripsXml(context, later);
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
				final String label = mBus.group(1);
				return new Line(null, null, Product.BUS, label, lineStyle(null, Product.BUS, label));
			}

			final Matcher mNachtbus = P_NORMALIZE_LINE_NACHTBUS.matcher(line);
			if (mNachtbus.matches())
			{
				final String label = "N" + mNachtbus.group(1);
				return new Line(null, null, Product.BUS, label, lineStyle(null, Product.BUS, label));
			}

			final Matcher mBusS = P_NORMALIZE_LINE_BUS_S.matcher(line);
			if (mBusS.matches())
			{
				final String label = "S" + mBusS.group(1);
				return new Line(null, null, Product.BUS, label, lineStyle(null, Product.BUS, label));
			}

			final Matcher mBusX = P_NORMALIZE_LINE_BUS_X.matcher(line);
			if (mBusX.matches())
			{
				final String label = "X" + mBusX.group(1);
				return new Line(null, null, Product.BUS, label, lineStyle(null, Product.BUS, label));
			}
		}

		return super.parseLine(type, line, wheelchairAccess);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		if ("1".equals(type))
			return Product.BUS;

		// skip parsing of "common" lines
		throw new IllegalStateException("cannot normalize type '" + type + "'");
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
