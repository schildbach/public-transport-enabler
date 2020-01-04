/*
 * Copyright the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class BayernProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://mobile.defas-fgi.de/beg/");
    // http://mobile.defas-fgi.de/xml/

    private static final String DEPARTURE_MONITOR_ENDPOINT = "XML_DM_REQUEST";
    private static final String TRIP_ENDPOINT = "XML_TRIP_REQUEST2";
    private static final String STOP_FINDER_ENDPOINT = "XML_STOPFINDER_REQUEST";

    public BayernProvider() {
        super(NetworkId.BAYERN, API_BASE, DEPARTURE_MONITOR_ENDPOINT, TRIP_ENDPOINT, STOP_FINDER_ENDPOINT, null);

        setRequestUrlEncoding(Charsets.UTF_8);
        setIncludeRegionId(false);
        setNumTripsRequested(12);
        setStyles(STYLES);
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("M".equals(trainType) && trainNum != null && trainName != null && trainName.endsWith("Meridian"))
                return new Line(id, network, Product.REGIONAL_TRAIN, "M" + trainNum);
            if ("ZUG".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);
        } else if ("1".equals(mot)) {
            if ("ABR".equals(trainType) || "ABELLIO Rail NRW GmbH".equals(trainName))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "ABR" + trainNum);
            if ("SBB".equals(trainType) || "SBB GmbH".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SBB" + Strings.nullToEmpty(trainNum));
        } else if ("5".equals(mot)) {
            if (name != null && name.startsWith("Stadtbus Linie ")) // Lindau
                return super.parseLine(id, network, mot, symbol, name.substring(15), longName, trainType, trainNum,
                        trainName);
            else
                return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
        } else if ("16".equals(mot)) {
            if ("EC".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "EC" + trainNum);
            if ("IC".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "IC" + trainNum);
            if ("ICE".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ICE" + trainNum);
            if ("CNL".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "CNL" + trainNum);
            if ("THA".equals(trainType) && trainNum != null) // Thalys
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "THA" + trainNum);
            if ("TGV".equals(trainType) && trainNum != null) // Train a grande Vitesse
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "TGV" + trainNum);
            if ("RJ".equals(trainType) && trainNum != null) // railjet
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "RJ" + trainNum);
            if ("WB".equals(trainType) && trainNum != null) // WESTbahn
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "WB" + trainNum);
            if ("HKX".equals(trainType) && trainNum != null) // Hamburg-Köln-Express
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "HKX" + trainNum);
            if ("D".equals(trainType) && trainNum != null) // Schnellzug
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "D" + trainNum);

            if ("IR".equals(trainType) && trainNum != null) // InterRegio
                return new Line(id, network, Product.REGIONAL_TRAIN, "IR" + trainNum);
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final Set<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasCoord())
            return mobileCoordRequest(types, location.coord, maxDistance, maxLocations);

        if (location.type != LocationType.STATION)
            throw new IllegalArgumentException("cannot handle: " + location.type);

        throw new IllegalArgumentException("station"); // TODO
    }

    @Override
    public QueryDeparturesResult queryDepartures(final String stationId, final @Nullable Date time,
            final int maxDepartures, final boolean equivs) throws IOException {
        checkNotNull(Strings.emptyToNull(stationId));

        return queryDeparturesMobile(stationId, time, maxDepartures, equivs);
    }

    @Override
    public SuggestLocationsResult suggestLocations(final CharSequence constraint,
            final @Nullable Set<LocationType> types, final int maxLocations) throws IOException {
        return mobileStopfinderRequest(constraint, types, maxLocations);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        if (options != null && options.products != null) {
            for (final Product p : options.products) {
                if (p == Product.HIGH_SPEED_TRAIN)
                    url.addEncodedQueryParameter("inclMOT_15", "on").addEncodedQueryParameter("inclMOT_16", "on");
                if (p == Product.REGIONAL_TRAIN)
                    url.addEncodedQueryParameter("inclMOT_13", "on");
            }
        }
        url.addEncodedQueryParameter("inclMOT_11", "on");
        url.addEncodedQueryParameter("inclMOT_14", "on");
        url.addEncodedQueryParameter("calcOneDirection", "1");
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        return queryTripsMobile(from, via, to, date, dep, options);
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later) throws IOException {
        return queryMoreTripsMobile(contextObj, later);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Munich
        STYLES.put("swm|T12", new Style(Shape.RECT, Style.parseColor("#96368b"), Style.WHITE));
        STYLES.put("swm|T15",
                new Style(Shape.RECT, Style.WHITE, Style.parseColor("#f1919c"), Style.parseColor("#f1919c")));
        STYLES.put("swm|T16", new Style(Shape.RECT, Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("swm|T17", new Style(Shape.RECT, Style.parseColor("#8b563e"), Style.WHITE));
        STYLES.put("swm|T18", new Style(Shape.RECT, Style.parseColor("#13a538"), Style.WHITE));
        STYLES.put("swm|T19", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("swm|T20", new Style(Shape.RECT, Style.parseColor("#16bae7"), Style.WHITE));
        STYLES.put("swm|T21", new Style(Shape.RECT, Style.parseColor("#bc7a00"), Style.WHITE));
        STYLES.put("swm|T22",
                new Style(Shape.RECT, Style.WHITE, Style.parseColor("#16bae7"), Style.parseColor("#16bae7")));
        STYLES.put("swm|T23", new Style(Shape.RECT, Style.parseColor("#bccf00"), Style.WHITE));
        STYLES.put("swm|T25", new Style(Shape.RECT, Style.parseColor("#f1919c"), Style.WHITE));
        STYLES.put("swm|T27", new Style(Shape.RECT, Style.parseColor("#f7a600"), Style.WHITE));
        STYLES.put("swm|T28",
                new Style(Shape.RECT, Style.WHITE, Style.parseColor("#f7a600"), Style.parseColor("#f7a600")));
        STYLES.put("swm|T29", new Style(Shape.RECT, Style.WHITE, Style.parseColor("#e30613"), Style.parseColor(
                "#e30613")));
        STYLES.put("swm|T31",
                new Style(Shape.RECT, Style.parseColor("#e30613"), Style.parseColor("#bc7a00"), Style.WHITE, 0));
        STYLES.put("swm|TN17", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("swm|TN19", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("swm|TN20", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("swm|TN27", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));

        STYLES.put("swm|UU1", new Style(Shape.RECT, Style.parseColor("#52822f"), Style.WHITE));
        STYLES.put("swm|UU2", new Style(Shape.RECT, Style.parseColor("#c20831"), Style.WHITE));
        STYLES.put("swm|UU3", new Style(Shape.RECT, Style.parseColor("#ec6726"), Style.WHITE));
        STYLES.put("swm|UU4", new Style(Shape.RECT, Style.parseColor("#00a984"), Style.WHITE));
        STYLES.put("swm|UU5", new Style(Shape.RECT, Style.parseColor("#bc7a00"), Style.WHITE));
        STYLES.put("swm|UU6", new Style(Shape.RECT, Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("swm|UU7",
                new Style(Shape.RECT, Style.parseColor("#52822f"), Style.parseColor("#c20831"), Style.WHITE, 0));
        STYLES.put("swm|UU8",
                new Style(Shape.RECT, Style.parseColor("#c20831"), Style.parseColor("#ec6726"), Style.WHITE, 0));

        STYLES.put("swm|B", new Style(Shape.RECT, Style.parseColor("#005262"), Style.WHITE));
        STYLES.put("swm|BX", new Style(Shape.RECT, Style.parseColor("#4e917a"), Style.WHITE));

        // Ingolstadt
        STYLES.put("inv|B10", new Style(Style.parseColor("#DA2510"), Style.WHITE));
        STYLES.put("inv|B11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
        STYLES.put("inv|B15", new Style(Style.parseColor("#84C326"), Style.BLACK));
        STYLES.put("inv|B16", new Style(Style.parseColor("#5D452E"), Style.WHITE));
        STYLES.put("inv|B17", new Style(Style.parseColor("#E81100"), Style.BLACK));
        STYLES.put("inv|B18", new Style(Style.parseColor("#79316C"), Style.WHITE));
        STYLES.put("inv|B20", new Style(Style.parseColor("#EA891C"), Style.BLACK));
        STYLES.put("inv|B21", new Style(Style.parseColor("#31B2EA"), Style.BLACK));
        STYLES.put("inv|B25", new Style(Style.parseColor("#7F65A0"), Style.WHITE));
        STYLES.put("inv|B26", new Style(Style.parseColor("#00BF73"), Style.WHITE));
        STYLES.put("inv|B30", new Style(Style.parseColor("#901E78"), Style.WHITE));
        STYLES.put("inv|B31", new Style(Style.parseColor("#DCE722"), Style.BLACK));
        STYLES.put("inv|B40", new Style(Style.parseColor("#009240"), Style.WHITE));
        STYLES.put("inv|B41", new Style(Style.parseColor("#7BC5B1"), Style.BLACK));
        STYLES.put("inv|B44", new Style(Style.parseColor("#EA77A6"), Style.WHITE));
        STYLES.put("inv|B50", new Style(Style.parseColor("#FACF00"), Style.BLACK));
        STYLES.put("inv|B51", new Style(Style.parseColor("#C13C00"), Style.WHITE));
        STYLES.put("inv|B52", new Style(Style.parseColor("#94F0D4"), Style.BLACK));
        STYLES.put("inv|B53", new Style(Style.parseColor("#BEB405"), Style.BLACK));
        STYLES.put("inv|B55", new Style(Style.parseColor("#FFF500"), Style.BLACK));
        STYLES.put("inv|B60", new Style(Style.parseColor("#0072B7"), Style.WHITE));
        STYLES.put("inv|B61", new Style(Style.rgb(204, 184, 122), Style.BLACK));
        STYLES.put("inv|B62", new Style(Style.rgb(204, 184, 122), Style.BLACK));
        STYLES.put("inv|B65", new Style(Style.parseColor("#B7DDD2"), Style.BLACK));
        STYLES.put("inv|B70", new Style(Style.parseColor("#D49016"), Style.BLACK));
        STYLES.put("inv|B71", new Style(Style.parseColor("#996600"), Style.BLACK));
        STYLES.put("inv|B85", new Style(Style.parseColor("#F6BAD3"), Style.BLACK));
        STYLES.put("inv|B111", new Style(Style.parseColor("#EE9B78"), Style.BLACK));

        STYLES.put("inv|B9221", new Style(Style.rgb(217, 217, 255), Style.BLACK));
        STYLES.put("inv|B9226", new Style(Style.rgb(191, 255, 255), Style.BLACK));

        STYLES.put("inv|BN1", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN2", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN3", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN4", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN5", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN6", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN7", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN8", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN9", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN10", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN11", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN12", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN13", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN14", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN15", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN16", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN17", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN18", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("inv|BN19", new Style(Style.parseColor("#00116C"), Style.WHITE));

        STYLES.put("inv|BS1", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS2", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS3", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS4", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS5", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS6", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS7", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS8", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("inv|BS9", new Style(Style.rgb(178, 25, 0), Style.WHITE));

        STYLES.put("inv|BX11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
        STYLES.put("inv|BX12", new Style(Style.parseColor("#B11839"), Style.BLACK));
        STYLES.put("inv|BX80", new Style(Style.parseColor("#FFFF40"), Style.BLACK));
        STYLES.put("inv|BX109", new Style(Style.WHITE, Style.BLACK, Style.BLACK));
    }
}
