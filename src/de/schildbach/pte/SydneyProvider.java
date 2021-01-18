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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class SydneyProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://transportnsw.info/web/");
    private static final String TRIP_ENDPOINT = "XML_TRIP_REQUEST2";

    public SydneyProvider() {
        super(NetworkId.SYDNEY, API_BASE, null, TRIP_ENDPOINT, null, null);
        setLanguage("en");
        setTimeZone("Australia/Sydney");
        setUseProxFootSearch(false);
        setUseRouteIndexAsTripId(false);
        setStyles(STYLES);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        if (options != null && options.products != null) {
            for (final Product p : options.products) {
                if (p == Product.BUS)
                    url.addEncodedQueryParameter("inclMOT_11", "on"); // school bus
            }
        }
        url.addEncodedQueryParameter("inclMOT_17", "on");
    }

    @Override
    protected String normalizeLocationName(final String name) {
        if (name == null || name.length() == 0)
            return null;

        return super.normalizeLocationName(name).replace("$XINT$", "&");
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("1".equals(mot)) {
            if ("BMT".equals(symbol) || "Blue Mountains Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "BMT");
            if ("CCN".equals(symbol) || "Central Coast & Newcastle Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "CCN");
            if ("SHL".equals(symbol) || "Southern Highlands Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "SHL");
            if ("SCO".equals(symbol) || "South Coast Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "SCO");
            if ("HUN".equals(symbol) || "Hunter Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "HUN");
            if ("SWR".equals(symbol)) // South West Rail Link
                return new Line(id, network, Product.SUBURBAN_TRAIN, "SWR");
            if ("NRC".equals(symbol) || (symbol != null && symbol.startsWith("North Coast NSW Line")))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "NRC");
            if ("WST".equals(symbol) || (symbol != null && symbol.startsWith("Western NSW Line")))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "WST");
            if ("STH".equals(symbol) || (symbol != null && symbol.startsWith("Southern NSW Line")))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "STH");
            if ("NRW".equals(symbol) || (symbol != null && symbol.startsWith("North Western NSW Line")))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "NRW");

            if ("T1".equals(symbol) || "T1 North Shore & Northern Line".equals(symbol)
                    || "T1 North Shore and Northern Line".equals(symbol) || "T1 Northern Line".equals(symbol)
                    || "T1 Western Line".equals(symbol) || "T1 North Shore, Northern & Western Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T1");
            if ("T2".equals(symbol) || "T2 Inner West & South Line".equals(symbol) || "T2 Airport Line".equals(symbol)
                    || "T2 Airport, Inner West & South Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T2");
            if ("T3".equals(symbol) || "T3 Bankstown Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T3");
            if ("T4".equals(symbol) || "T4 Eastern Suburbs & Illawarra Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T4");
            if ("T5".equals(symbol) || "T5 Cumberland Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T5");
            if ("T6".equals(symbol) || "T6 Carlingford Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T6");
            if ("T7".equals(symbol) || "T7 Olympic Park Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T7");
            if ("T8".equals(symbol) || "T8 Airport & South Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T8");
            if ("T9".equals(symbol) || "T9 Northern Line".equals(symbol))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "T9");

            if (("31".equals(symbol) || "36".equals(symbol) || "621".equals(symbol) || "622".equals(symbol)
                    || "635".equals(symbol) || "636".equals(symbol))
                    && ((trainName != null && trainName.startsWith("Regional Trains"))
                            || (longName != null && longName.startsWith("Regional Trains"))))
                return new Line(id, network, null, symbol);

            throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name
                    + "' long='" + longName + "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='"
                    + trainName + "'");
        } else if ("2".equals(mot)) {
            if ("M".equals(symbol) || "M Metro North West Line".equals(symbol))
                return new Line(id, network, Product.SUBWAY, "M");

            throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name
                    + "' long='" + longName + "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='"
                    + trainName + "'");
        } else if ("4".equals(mot)) {
            if ("L1".equals(symbol) || "L1 Dulwich Hill Line".equals(symbol))
                return new Line(id, network, Product.TRAM, "L1");
            if ("L2".equals(symbol) || "L2 Randwick Line".equals(symbol))
                return new Line(id, network, Product.TRAM, "L2");
            if ("L3".equals(symbol) || "L3 Kingsford Line".equals(symbol))
                return new Line(id, network, Product.TRAM, "L3");

            throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name
                    + "' long='" + longName + "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='"
                    + trainName + "'");
        } else if ("9".equals(mot)) {
            if ("F1".equals(symbol) || "F1 Manly".equals(symbol))
                return new Line(id, network, Product.FERRY, "F1");
            if ("F2".equals(symbol) || "F2 Taronga Zoo".equals(symbol))
                return new Line(id, network, Product.FERRY, "F2");
            if ("F3".equals(symbol) || "F3 Parramatta River".equals(symbol))
                return new Line(id, network, Product.FERRY, "F3");
            if ("F4".equals(symbol) || "F4 Darling Harbour".equals(symbol))
                return new Line(id, network, Product.FERRY, "F4");
            if ("F5".equals(symbol) || "F5 Neutral Bay".equals(symbol))
                return new Line(id, network, Product.FERRY, "F5");
            if ("F6".equals(symbol) || "F6 Mosman Bay".equals(symbol))
                return new Line(id, network, Product.FERRY, "F6");
            if ("F7".equals(symbol) || "F7 Eastern Suburbs".equals(symbol))
                return new Line(id, network, Product.FERRY, "F7");
            if ("F8".equals(symbol) || "F8 Cockatoo Island".equals(symbol))
                return new Line(id, network, Product.FERRY, "F8");
            if (("Private ferry servic".equals(trainName) || "Private ferry and fa".equals(trainName))
                    && symbol != null)
                return new Line(id, network, Product.FERRY, symbol);
            if ("MFF".equals(symbol) || "Manly Fast Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "MFF");
            if ("LneCv".equals(symbol) || "Lane Cove Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "LneCv");
            if ("EmpBa".equals(symbol) || "Woy Woy to Empire Bay Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "EmpBa");
            if ("Stkn".equals(symbol) || "Stockton Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "Stkn");
            if ("CCWB".equals(symbol) || "Circular Quay to Watsons Bay".equals(name))
                return new Line(id, network, Product.FERRY, "CCWB");
            if ("CCZC".equals(symbol) || "City to Taronga Zoo".equals(name))
                return new Line(id, network, Product.FERRY, "CCZC");
            if ("CCGD".equals(symbol) || "City to Garden Island and Manly".equals(name))
                return new Line(id, network, Product.FERRY, "CCGD");
            if ("CCWM".equals(symbol) || "Manly to Watsons Bay".equals(name))
                return new Line(id, network, Product.FERRY, "CCWM");
            if ("MDH".equals(symbol) || "MDH Manly to Darling Harbour Loop Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "MDH");
            if ("CCGZ".equals(symbol) || "CCGZ Garden Island & Taronga Zoo Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "CCGZ");
            if ("CCDH".equals(symbol) || "CCDH Circular Quay Luna Park Darling Harbour Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "CCDH");
            if ("CCDM".equals(symbol) || "CCDM Manly Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "CCDM");
            if ("CCSH".equals(symbol) || "CCSH Shark Island Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "CCSH");
            if ("CCTZ".equals(symbol) || "CCTZ Taronga Zoo Ferry".equals(name))
                return new Line(id, network, Product.FERRY, "CCTZ");

            throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name
                    + "' long='" + longName + "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='"
                    + trainName + "'");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Values from https://opendata.transport.nsw.gov.au/resources

        STYLES.put("SBMT", new Style(Style.parseColor("#f99d1c"), Style.WHITE));
        STYLES.put("SCCN", new Style(Style.parseColor("#d11f2f"), Style.WHITE));
        STYLES.put("SSHL", new Style(Style.parseColor("#833134"), Style.WHITE));
        STYLES.put("SSCO", new Style(Style.parseColor("#005aa3"), Style.WHITE));
        STYLES.put("SHUN", new Style(Style.parseColor("#00954c"), Style.WHITE));

        STYLES.put("ST1", new Style(Style.parseColor("#f99d1c"), Style.WHITE));
        STYLES.put("ST2", new Style(Style.parseColor("#0098cd"), Style.WHITE));
        STYLES.put("ST3", new Style(Style.parseColor("#df4825"), Style.WHITE));
        STYLES.put("ST4", new Style(Style.parseColor("#005aa3"), Style.WHITE));
        STYLES.put("ST5", new Style(Style.parseColor("#c4258f"), Style.WHITE));
        STYLES.put("ST6", new Style(Style.parseColor("#456caa"), Style.WHITE));
        STYLES.put("ST7", new Style(Style.parseColor("#6f818e"), Style.WHITE));
        STYLES.put("ST8", new Style(Style.parseColor("#00954c"), Style.WHITE));
        STYLES.put("ST9", new Style(Style.parseColor("#d11f2e"), Style.WHITE));

        STYLES.put("UM", new Style(Style.parseColor("#168388"), Style.WHITE));

        STYLES.put("TL1", new Style(Style.parseColor("#be1622"), Style.WHITE));
        STYLES.put("TL2", new Style(Style.parseColor("#dd1e25"), Style.WHITE));
        STYLES.put("TL3", new Style(Style.parseColor("#781140"), Style.WHITE));

        STYLES.put("B", new Style(Style.parseColor("#00b5ef"), Style.WHITE));

        STYLES.put("FF1", new Style(Style.parseColor("#00774b"), Style.WHITE));
        STYLES.put("FF2", new Style(Style.parseColor("#144734"), Style.WHITE));
        STYLES.put("FF3", new Style(Style.parseColor("#648c3c"), Style.WHITE));
        STYLES.put("FF4", new Style(Style.parseColor("#97c93d"), Style.WHITE));
        STYLES.put("FF5", new Style(Style.parseColor("#286142"), Style.WHITE));
        STYLES.put("FF6", new Style(Style.parseColor("#00ab51"), Style.WHITE));
        STYLES.put("FF7", new Style(Style.parseColor("#00b189"), Style.WHITE));
        STYLES.put("FF8", new Style(Style.parseColor("#55622b"), Style.WHITE));
    }
}
