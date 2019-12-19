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
        url.addEncodedQueryParameter("inclMOT_13", "on");
        url.addEncodedQueryParameter("inclMOT_14", "on");
        url.addEncodedQueryParameter("inclMOT_15", "on");
        url.addEncodedQueryParameter("inclMOT_16", "on");
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
        } else if ("4".equals(mot)) {
            if ("L1".equals(symbol) || "L1 Dulwich Hill Line".equals(symbol))
                return new Line(id, network, Product.TRAM, "L1");
            if ("L2".equals(symbol) || "L2 Randwick Line".equals(symbol))
                return new Line(id, network, Product.TRAM, "L2");

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
        STYLES.put("SBMT", new Style(Style.parseColor("#f5a81d"), Style.WHITE));
        STYLES.put("SCCN", new Style(Style.parseColor("#d11f2f"), Style.WHITE));
        STYLES.put("SSHL", new Style(Style.parseColor("#843135"), Style.WHITE));
        STYLES.put("SSCO", new Style(Style.parseColor("#0083bf"), Style.WHITE));
        STYLES.put("SHUN", new Style(Style.parseColor("#509e45"), Style.WHITE));

        STYLES.put("ST1", new Style(Style.parseColor("#f4a00e"), Style.WHITE));
        STYLES.put("ST2", new Style(Style.parseColor("#48a338"), Style.WHITE));
        STYLES.put("ST3", new Style(Style.parseColor("#f25223"), Style.WHITE));
        STYLES.put("ST4", new Style(Style.parseColor("#1081c5"), Style.WHITE));
        STYLES.put("ST5", new Style(Style.parseColor("#c72c9e"), Style.WHITE));
        STYLES.put("ST6", new Style(Style.parseColor("#3a5b9a"), Style.WHITE));
        STYLES.put("ST7", new Style(Style.parseColor("#97a2ad"), Style.WHITE));

        STYLES.put("TL1", new Style(Style.parseColor("#c01a2c"), Style.WHITE));

        STYLES.put("B130", new Style(Style.parseColor("#878787"), Style.WHITE));
        STYLES.put("B131", new Style(Style.parseColor("#bcbcbc"), Style.WHITE));
        STYLES.put("B132", new Style(Style.parseColor("#f08cb5"), Style.WHITE));
        STYLES.put("B135", new Style(Style.parseColor("#80ba27"), Style.WHITE));
        STYLES.put("B136", new Style(Style.parseColor("#f18f00"), Style.WHITE));
        STYLES.put("B137", new Style(Style.parseColor("#f18f00"), Style.WHITE));
        STYLES.put("B139", new Style(Style.parseColor("#009fc8"), Style.WHITE));
        STYLES.put("BL60", new Style(Style.parseColor("#cad400"), Style.WHITE));
        STYLES.put("B140", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B142", new Style(Style.parseColor("#80ba27"), Style.WHITE));
        STYLES.put("B143", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B144", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE50", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B151", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B153", new Style(Style.parseColor("#80ba27"), Style.WHITE));
        STYLES.put("B155", new Style(Style.parseColor("#1d1d1b"), Style.WHITE));
        STYLES.put("B156", new Style(Style.parseColor("#1d1d1b"), Style.WHITE));
        STYLES.put("B158", new Style(Style.parseColor("#a05c00"), Style.WHITE));
        STYLES.put("B159", new Style(Style.parseColor("#a05c00"), Style.WHITE));
        STYLES.put("BE65", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("BE66", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B168", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE68", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B169", new Style(Style.parseColor("#a0c9ed"), Style.WHITE));
        STYLES.put("BE69", new Style(Style.parseColor("#a0c9ed"), Style.WHITE));
        STYLES.put("BE70", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B171", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE71", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B173", new Style(Style.parseColor("#a0c9ed"), Style.WHITE));
        STYLES.put("B175", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B176", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE76", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE77", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B178", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE78", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BL78", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B179", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BE79", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B180", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("BL80", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B183", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE83", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B184", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BL84", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE84", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B185", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BL85", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE85", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("BE86", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B187", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BL87", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE87", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B188", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BL88", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BE88", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("BE89", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B190", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("BL90", new Style(Style.parseColor("#e63329"), Style.WHITE));
        STYLES.put("B200", new Style(Style.parseColor("#c27ab1"), Style.WHITE));
        STYLES.put("B201", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B202", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B203", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B204", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B205", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B206", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B207", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B208", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B209", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B225", new Style(Style.parseColor("#f08cb5"), Style.WHITE));
        STYLES.put("B227", new Style(Style.parseColor("#ffed00"), Style.BLACK));
        STYLES.put("B228", new Style(Style.parseColor("#ffed00"), Style.BLACK));
        STYLES.put("B229", new Style(Style.parseColor("#ffed00"), Style.BLACK));
        STYLES.put("B230", new Style(Style.parseColor("#ffed00"), Style.BLACK));
        STYLES.put("B236", new Style(Style.parseColor("#aa6500"), Style.WHITE));
        STYLES.put("B238", new Style(Style.parseColor("#fbc684"), Style.BLACK));
        STYLES.put("B243", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B244", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B245", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B246", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B247", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B248", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B249", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B251", new Style(Style.parseColor("#747b0e"), Style.WHITE));
        STYLES.put("B252", new Style(Style.parseColor("#747b0e"), Style.WHITE));
        STYLES.put("B253", new Style(Style.parseColor("#004a9a"), Style.WHITE));
        STYLES.put("B254", new Style(Style.parseColor("#004a9a"), Style.WHITE));
        STYLES.put("B257", new Style(Style.parseColor("#fbba00"), Style.BLACK));
        STYLES.put("B261", new Style(Style.parseColor("#e56606"), Style.WHITE));
        STYLES.put("B263", new Style(Style.parseColor("#d0043c"), Style.WHITE));
        STYLES.put("B265", new Style(Style.parseColor("#c8007c"), Style.WHITE));
        STYLES.put("B267", new Style(Style.parseColor("#b8d484"), Style.WHITE));
        STYLES.put("B269", new Style(Style.parseColor("#cad400"), Style.BLACK));
        STYLES.put("B273", new Style(Style.parseColor("#b5aba1"), Style.WHITE));
        STYLES.put("B272", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B275", new Style(Style.parseColor("#aa6500"), Style.WHITE));
        STYLES.put("B285", new Style(Style.parseColor("#cad400"), Style.WHITE));
        STYLES.put("B286", new Style(Style.parseColor("#b2b2b2"), Style.WHITE));
        STYLES.put("B288", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B290", new Style(Style.parseColor("#e5007d"), Style.WHITE));
        STYLES.put("B292", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B293", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B294", new Style(Style.parseColor("#8ac199"), Style.WHITE));
        STYLES.put("B295", new Style(Style.parseColor("#bee4f6"), Style.BLACK));
        STYLES.put("B297", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B300", new Style(Style.parseColor("#636466"), Style.WHITE));
        STYLES.put("B301", new Style(Style.parseColor("#e4358b"), Style.WHITE));
        STYLES.put("B302", new Style(Style.parseColor("#e4358b"), Style.WHITE));
        STYLES.put("B303", new Style(Style.parseColor("#00704a"), Style.WHITE));
        STYLES.put("BX03", new Style(Style.parseColor("#00704a"), Style.WHITE));
        STYLES.put("B305", new Style(Style.parseColor("#abcc58"), Style.WHITE));
        STYLES.put("B308", new Style(Style.parseColor("#ad208e"), Style.WHITE));
        STYLES.put("B309", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("BX09", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("BL09", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("B310", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("BX10", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("B311", new Style(Style.parseColor("#cab900"), Style.WHITE));
        STYLES.put("B313", new Style(Style.parseColor("#e4358b"), Style.WHITE));
        STYLES.put("B314", new Style(Style.parseColor("#f59d21"), Style.WHITE));
        STYLES.put("B316", new Style(Style.parseColor("#f59d21"), Style.WHITE));
        STYLES.put("B317", new Style(Style.parseColor("#f59d21"), Style.WHITE));
        STYLES.put("B323", new Style(Style.parseColor("#009fe3"), Style.WHITE));
        STYLES.put("B324", new Style(Style.parseColor("#4da32f"), Style.WHITE));
        STYLES.put("BL24", new Style(Style.parseColor("#4da32f"), Style.WHITE));
        STYLES.put("B325", new Style(Style.parseColor("#22317f"), Style.WHITE));
        STYLES.put("B326", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B327", new Style(Style.parseColor("#d54b0b"), Style.WHITE));
        STYLES.put("B333", new Style(Style.parseColor("#009fe3"), Style.WHITE));
        STYLES.put("B339", new Style(Style.parseColor("#9d5da2"), Style.WHITE));
        STYLES.put("BX39", new Style(Style.parseColor("#9d5da2"), Style.WHITE));
        STYLES.put("BX40", new Style(Style.parseColor("#9d5da2"), Style.WHITE));
        STYLES.put("B343", new Style(Style.parseColor("#f192b3"), Style.WHITE));
        STYLES.put("B348", new Style(Style.parseColor("#007db1"), Style.WHITE));
        STYLES.put("B352", new Style(Style.parseColor("#ad208e"), Style.WHITE));
        STYLES.put("B353", new Style(Style.parseColor("#abcc58"), Style.WHITE));
        STYLES.put("B355", new Style(Style.parseColor("#ad208e"), Style.WHITE));
        STYLES.put("B360", new Style(Style.parseColor("#f192b3"), Style.WHITE));
        STYLES.put("B361", new Style(Style.parseColor("#f192b3"), Style.WHITE));
        STYLES.put("B370", new Style(Style.parseColor("#009ddc"), Style.WHITE));
        STYLES.put("B372", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("B373", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("BX73", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("B374", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("BX74", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("B376", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("B377", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("BX77", new Style(Style.parseColor("#006944"), Style.WHITE));
        STYLES.put("B378", new Style(Style.parseColor("#4da32f"), Style.WHITE));
        STYLES.put("B380", new Style(Style.parseColor("#0070ba"), Style.WHITE));
        STYLES.put("B381", new Style(Style.parseColor("#eb5b25"), Style.WHITE));
        STYLES.put("B382", new Style(Style.parseColor("#f9b122"), Style.WHITE));
        STYLES.put("B386", new Style(Style.parseColor("#943e01"), Style.WHITE));
        STYLES.put("B387", new Style(Style.parseColor("#943e01"), Style.WHITE));
        STYLES.put("B389", new Style(Style.parseColor("#ffdd00"), Style.BLACK));
        STYLES.put("BX84", new Style(Style.parseColor("#ffdd00"), Style.BLACK));
        STYLES.put("BX89", new Style(Style.parseColor("#ffdd00"), Style.BLACK));
        STYLES.put("B391", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B392", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BX92", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B393", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B394", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BL94", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BX94", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B395", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B396", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BX96", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B397", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BX97", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B399", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("BX99", new Style(Style.parseColor("#009984"), Style.WHITE));
        STYLES.put("B400", new Style(Style.parseColor("#eb4498"), Style.WHITE));
        STYLES.put("B401", new Style(Style.parseColor("#f06597"), Style.WHITE));
        STYLES.put("B406", new Style(Style.parseColor("#ec881d"), Style.WHITE));
        STYLES.put("B407", new Style(Style.parseColor("#a8005b"), Style.WHITE));
        STYLES.put("B408", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B410", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("B412", new Style(Style.parseColor("#006838"), Style.WHITE));
        STYLES.put("B413", new Style(Style.parseColor("#006838"), Style.WHITE));
        STYLES.put("B415", new Style(Style.parseColor("#39b54a"), Style.WHITE));
        STYLES.put("B418", new Style(Style.parseColor("#8a5e3c"), Style.WHITE));
        STYLES.put("B422", new Style(Style.parseColor("#39b54a"), Style.WHITE));
        STYLES.put("B423", new Style(Style.parseColor("#39b54a"), Style.WHITE));
        STYLES.put("B426", new Style(Style.parseColor("#39b54a"), Style.WHITE));
        STYLES.put("B428", new Style(Style.parseColor("#39b54a"), Style.WHITE));
        STYLES.put("BL23", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("B430", new Style(Style.parseColor("#f06597"), Style.WHITE));
        STYLES.put("B431", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B433", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B436", new Style(Style.parseColor("#f06597"), Style.WHITE));
        STYLES.put("B438", new Style(Style.parseColor("#2b3990"), Style.WHITE));
        STYLES.put("B439", new Style(Style.parseColor("#2b3990"), Style.WHITE));
        STYLES.put("B440", new Style(Style.parseColor("#f06597"), Style.WHITE));
        STYLES.put("B441", new Style(Style.parseColor("#fbb040"), Style.WHITE));
        STYLES.put("B442", new Style(Style.parseColor("#fbb040"), Style.WHITE));
        STYLES.put("B443", new Style(Style.parseColor("#1c75bc"), Style.WHITE));
        STYLES.put("B444", new Style(Style.parseColor("#1c75bc"), Style.WHITE));
        STYLES.put("B445", new Style(Style.parseColor("#1c75bc"), Style.WHITE));
        STYLES.put("B448", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("B458", new Style(Style.parseColor("#0093d0"), Style.WHITE));
        STYLES.put("B459", new Style(Style.parseColor("#0093d0"), Style.WHITE));
        STYLES.put("B460", new Style(Style.parseColor("#ef4136"), Style.WHITE));
        STYLES.put("B461", new Style(Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("B462", new Style(Style.parseColor("#603913"), Style.WHITE));
        STYLES.put("B463", new Style(Style.parseColor("#ec881d"), Style.WHITE));
        STYLES.put("B464", new Style(Style.parseColor("#603913"), Style.WHITE));
        STYLES.put("B466", new Style(Style.parseColor("#006838"), Style.WHITE));
        STYLES.put("B470", new Style(Style.parseColor("#603913"), Style.WHITE));
        STYLES.put("B473", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B476", new Style(Style.parseColor("#1c75bc"), Style.WHITE));
        STYLES.put("B477", new Style(Style.parseColor("#1c75bc"), Style.WHITE));
        STYLES.put("B478", new Style(Style.parseColor("#fbb040"), Style.WHITE));
        STYLES.put("B479", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B480", new Style(Style.parseColor("#0054a6"), Style.WHITE));
        STYLES.put("B483", new Style(Style.parseColor("#0054a6"), Style.WHITE));
        STYLES.put("B487", new Style(Style.parseColor("#ec008c"), Style.WHITE));
        STYLES.put("B490", new Style(Style.parseColor("#a54399"), Style.WHITE));
        STYLES.put("B491", new Style(Style.parseColor("#fdb913"), Style.WHITE));
        STYLES.put("B492", new Style(Style.parseColor("#a54399"), Style.WHITE));
        STYLES.put("B495", new Style(Style.parseColor("#ed1d24"), Style.WHITE));
        STYLES.put("B500", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("BX00", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B501", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B502", new Style(Style.parseColor("#27aae1"), Style.WHITE));
        STYLES.put("B504", new Style(Style.parseColor("#27aae1"), Style.WHITE));
        STYLES.put("BX04", new Style(Style.parseColor("#27aae1"), Style.WHITE));
        STYLES.put("B505", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B506", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("BX06", new Style(Style.parseColor("#701c74"), Style.WHITE));
        STYLES.put("B507", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B508", new Style(Style.parseColor("#e5007d"), Style.WHITE));
        STYLES.put("B510", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B513", new Style(Style.parseColor("#cad400"), Style.WHITE));
        STYLES.put("B515", new Style(Style.parseColor("#8ab2df"), Style.WHITE));
        STYLES.put("BX15", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B518", new Style(Style.parseColor("#8ab2df"), Style.WHITE));
        STYLES.put("BX18", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B520", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("B521", new Style(Style.parseColor("#56af31"), Style.WHITE));
        STYLES.put("B523", new Style(Style.parseColor("#56af31"), Style.WHITE));
        STYLES.put("B524", new Style(Style.parseColor("#56af31"), Style.WHITE));
        STYLES.put("B525", new Style(Style.parseColor("#fdb913"), Style.WHITE));
        STYLES.put("B526", new Style(Style.parseColor("#fdb913"), Style.WHITE));
        STYLES.put("BL37", new Style(Style.parseColor("#c95c30"), Style.WHITE));
        STYLES.put("BL38", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("BL39", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("B544", new Style(Style.parseColor("#b59d00"), Style.WHITE));
        STYLES.put("B545", new Style(Style.parseColor("#e5007d"), Style.WHITE));
        STYLES.put("B546", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B547", new Style(Style.parseColor("#ffdd00"), Style.BLACK));
        STYLES.put("B549", new Style(Style.parseColor("#f28c00"), Style.WHITE));
        STYLES.put("B550", new Style(Style.parseColor("#e5007d"), Style.WHITE));
        STYLES.put("B551", new Style(Style.parseColor("#cad400"), Style.WHITE));
        STYLES.put("B552", new Style(Style.parseColor("#ab3a8d"), Style.WHITE));
        STYLES.put("B555", new Style(Style.parseColor("#00b3f0"), Style.WHITE));
        STYLES.put("B890", new Style(Style.parseColor("#eb5b25"), Style.parseColor("#ffed00")));
        STYLES.put("B891", new Style(Style.parseColor("#eb5b25"), Style.parseColor("#ffed00")));
        STYLES.put("B892", new Style(Style.parseColor("#eb5b25"), Style.parseColor("#ffed00")));
        STYLES.put("B895", new Style(Style.parseColor("#eb5b25"), Style.parseColor("#ffed00")));
        STYLES.put("BL28", new Style(Style.parseColor("#6c207e"), Style.WHITE));
        STYLES.put("BM10", new Style(Style.parseColor("#e9559f"), Style.WHITE));
        STYLES.put("BM20", new Style(Style.parseColor("#f7941e"), Style.WHITE));
        STYLES.put("BM30", new Style(Style.parseColor("#ad208e"), Style.WHITE));
        STYLES.put("BM40", new Style(Style.parseColor("#ca220e"), Style.WHITE));
        STYLES.put("BM41", new Style(Style.parseColor("#b2d33e"), Style.WHITE));
        STYLES.put("BM50", new Style(Style.parseColor("#92278f"), Style.WHITE));
        STYLES.put("BM52", new Style(Style.parseColor("#9c8dc3"), Style.WHITE));
        STYLES.put("BM54", new Style(Style.parseColor("#0070ba"), Style.WHITE));
        STYLES.put("BN", new Style(Style.parseColor("#051925"), Style.WHITE));

        STYLES.put("FF1", new Style(Style.parseColor("#0c754b"), Style.WHITE));
        STYLES.put("FF2", new Style(Style.parseColor("#1e4230"), Style.WHITE));
        STYLES.put("FF3", new Style(Style.parseColor("#8acf24"), Style.WHITE));
        STYLES.put("FF4", new Style(Style.parseColor("#0b974a"), Style.WHITE));
        STYLES.put("FF5", new Style(Style.parseColor("#2a5d3d"), Style.WHITE));
        STYLES.put("FF6", new Style(Style.parseColor("#19ae4d"), Style.WHITE));
        STYLES.put("FF7", new Style(Style.parseColor("#2bb683"), Style.WHITE));
    }
}
