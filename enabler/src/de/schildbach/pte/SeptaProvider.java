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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

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
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class SeptaProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://airs1.septa.org/bin/";
	private static final Product[] PRODUCTS_MAP = { Product.SUBWAY, Product.TRAM, Product.BUS, Product.SUBURBAN_TRAIN };
	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public SeptaProvider()
	{
		super(NetworkId.SEPTA, API_BASE, "en", PRODUCTS_MAP);

		setStationBoardCanDoEquivs(false);
		setTimeZone("EST");
	}

	@Override
	public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location, final int maxDistance,
			final int maxLocations) throws IOException
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

	@Override
	protected void appendDateTimeParameters(final StringBuilder uri, final Date time, final String dateParamName, final String timeParamName)
	{
		final Calendar c = new GregorianCalendar(timeZone);
		c.setTime(time);
		final int year = c.get(Calendar.YEAR);
		final int month = c.get(Calendar.MONTH) + 1;
		final int day = c.get(Calendar.DAY_OF_MONTH);
		final int hour = c.get(Calendar.HOUR);
		final int minute = c.get(Calendar.MINUTE);
		final String amPm = c.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
		uri.append('&').append(dateParamName).append('=');
		uri.append(ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%02d%02d%04d", month, day, year)));
		uri.append('&').append(timeParamName).append('=');
		uri.append(ParserUtils.urlEncode(String.format(Locale.ENGLISH, "%02d:%02d %s", hour, minute, amPm)));
	}

	private static final Pattern P_DEPARTURES_PAGE_COARSE = Pattern
			.compile(
					".*?" //
							+ "(?:" //
							+ "<div class=\"hfsTitleText\">([^<]*)<.*?" // location
							+ "\n(\\d{2}/\\d{2}/\\d{4})[^\n]*\n" // date
							+ "Departure (\\d{1,2}:\\d{2} [AP]M)\n.*?" // time
							+ "(?:<table class=\"resultTable\"[^>]*>(.+?)</table>|(No trains in this space of time))" //
							+ "|(input cannot be interpreted)|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
							+ ".*?" //
					, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<td class=\"time\">(\\d{1,2}:\\d{2} [AP]M)</td>\n" // plannedTime
			+ "(?:<td class=\"[\\w ]*prognosis[\\w ]*\">\n" //
			+ "(?:&nbsp;|<span class=\"rtLimit\\d\">(p&#252;nktlich|\\d{1,2}:\\d{2})</span>)\n</td>\n" // predictedTime
			+ ")?.*?" //
			+ "<img class=\"product\" src=\"/hafas-res/img/products/(\\w+)_pic\\.gif\" width=\"\\d+\" height=\"\\d+\" alt=\"([^\"]*)\".*?" // type,
			// line
			+ "<strong>\n" //
			+ "<a href=\"http://airs1\\.septa\\.org/bin/stboard\\.exe/en\\?input=(\\d+)&[^>]*>" // destinationId
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
		final Matcher mPageCoarse = P_DEPARTURES_PAGE_COARSE.matcher(page);
		if (mPageCoarse.matches())
		{
			// messages
			if (mPageCoarse.group(5) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			else if (mPageCoarse.group(6) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mPageCoarse.group(7) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final String[] placeAndName = splitStationName(ParserUtils.resolveEntities(mPageCoarse.group(1)));
			final Calendar currentTime = new GregorianCalendar(timeZone);
			currentTime.clear();
			ParserUtils.parseAmericanDate(currentTime, mPageCoarse.group(2));
			ParserUtils.parseAmericanTime(currentTime, mPageCoarse.group(3));

			final List<Departure> departures = new ArrayList<Departure>(8);
			String oldZebra = null;

			final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mPageCoarse.group(4));
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
					ParserUtils.parseAmericanTime(plannedTime, mDepFine.group(1));

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
							ParserUtils.parseAmericanTime(predictedTime, prognosis);
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

			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, placeAndName[0], placeAndName[1]),
					departures, null));
			return result;
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

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(address);
		if (m.matches())
			return new String[] { m.group(2), m.group(1) };

		return super.splitStationName(address);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// Regional
		if (ucType.equals("RAI"))
			return Product.REGIONAL_TRAIN;

		// Subway
		if (ucType.equals("BSS"))
			return Product.SUBWAY;
		if (ucType.equals("BSL"))
			return Product.SUBWAY;
		if (ucType.equals("MFL"))
			return Product.SUBWAY;

		// Tram
		if (ucType.equals("TRM"))
			return Product.TRAM;
		if (ucType.equals("NHS")) // Tro NHSL
			return Product.TRAM;

		// Bus
		if (ucType.equals("BUS"))
			return Product.BUS;
		if (ucType.equals("TRO"))
			return Product.BUS;

		// from Connections:

		if (ucType.equals("RAIL"))
			return Product.REGIONAL_TRAIN;

		if (ucType.equals("SUBWAY"))
			return Product.SUBWAY;

		if (ucType.equals("TROLLEY"))
			return Product.BUS;

		// skip parsing of "common" lines, because this is America
		throw new IllegalStateException("cannot normalize type '" + type + "'");
	}
}
