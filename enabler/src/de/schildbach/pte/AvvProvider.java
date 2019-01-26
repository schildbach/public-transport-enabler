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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class AvvProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://efa.avv-augsburg.de/avv2/");

    public AvvProvider() {
        super(NetworkId.AVV, API_BASE);
        setUseRouteIndexAsTripId(false);
        setRequestUrlEncoding(Charsets.UTF_8);
        setStyles(STYLES);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        url.addEncodedQueryParameter("inclMOT_11", "on"); // night bus
        url.addEncodedQueryParameter("inclMOT_13", "on");
        url.addEncodedQueryParameter("inclMOT_14", "on");
        url.addEncodedQueryParameter("inclMOT_15", "on");
        url.addEncodedQueryParameter("inclMOT_16", "on");
        url.addEncodedQueryParameter("inclMOT_17", "on");
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("Regionalbahn".equals(trainName) && symbol != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("Staudenbahn SVG".equals(trainNum) && trainType == null && trainName == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "SVG");

            // Streikfahrplan
            if ("R1S".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R4S".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R6S".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R7S".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("R8S".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        STYLES.put("B", new Style(Style.Shape.CIRCLE, Style.parseColor("#abb1b1"), Style.BLACK));
        STYLES.put("BB1", new Style(Style.Shape.CIRCLE, Style.parseColor("#93117e"), Style.WHITE));
        STYLES.put("BB3", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
        STYLES.put("B21", new Style(Style.Shape.CIRCLE, Style.parseColor("#00896b"), Style.WHITE));
        STYLES.put("B22", new Style(Style.Shape.CIRCLE, Style.parseColor("#eb6b59"), Style.WHITE));
        STYLES.put("B23", new Style(Style.Shape.CIRCLE, Style.parseColor("#97bf0d"), Style.parseColor("#d10019")));
        STYLES.put("B27", new Style(Style.Shape.CIRCLE, Style.parseColor("#74b57e"), Style.WHITE));
        STYLES.put("B29", new Style(Style.Shape.CIRCLE, Style.parseColor("#5f689f"), Style.WHITE));
        STYLES.put("B30", new Style(Style.Shape.CIRCLE, Style.parseColor("#829ac3"), Style.WHITE));
        STYLES.put("B31", new Style(Style.Shape.CIRCLE, Style.parseColor("#a3cdb0"), Style.parseColor("#006835")));
        STYLES.put("B32", new Style(Style.Shape.CIRCLE, Style.parseColor("#45a477"), Style.WHITE));
        STYLES.put("B33", new Style(Style.Shape.CIRCLE, Style.parseColor("#a0ca82"), Style.WHITE));
        STYLES.put("B35", new Style(Style.Shape.CIRCLE, Style.parseColor("#0085c5"), Style.WHITE));
        STYLES.put("B36", new Style(Style.Shape.CIRCLE, Style.parseColor("#b1c2e1"), Style.parseColor("#006ab3")));
        STYLES.put("B37", new Style(Style.Shape.CIRCLE, Style.parseColor("#eac26b"), Style.BLACK));
        STYLES.put("B38", new Style(Style.Shape.CIRCLE, Style.parseColor("#c3655a"), Style.WHITE));
        STYLES.put("B41", new Style(Style.Shape.CIRCLE, Style.parseColor("#d26110"), Style.WHITE));
        STYLES.put("B42", new Style(Style.Shape.CIRCLE, Style.parseColor("#d57642"), Style.WHITE));
        STYLES.put("B43", new Style(Style.Shape.CIRCLE, Style.parseColor("#e29241"), Style.WHITE));
        STYLES.put("B44", new Style(Style.Shape.CIRCLE, Style.parseColor("#d0aacc"), Style.parseColor("#6d1f80")));
        STYLES.put("B45", new Style(Style.Shape.CIRCLE, Style.parseColor("#a76da7"), Style.WHITE));
        STYLES.put("B46", new Style(Style.Shape.CIRCLE, Style.parseColor("#52bcc2"), Style.WHITE));
        STYLES.put("B48", new Style(Style.Shape.CIRCLE, Style.parseColor("#a6d7d2"), Style.parseColor("#079098")));
        STYLES.put("B51", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
        STYLES.put("B52", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
        STYLES.put("B54", new Style(Style.Shape.CIRCLE, Style.parseColor("#ee7f00"), Style.WHITE));
        STYLES.put("B56", new Style(Style.Shape.CIRCLE, Style.parseColor("#a86853"), Style.WHITE));
        STYLES.put("B57", new Style(Style.Shape.CIRCLE, Style.parseColor("#a76da7"), Style.WHITE));
        STYLES.put("B58", new Style(Style.Shape.CIRCLE, Style.parseColor("#d0aacc"), Style.parseColor("#6d1f80")));
        STYLES.put("B59", new Style(Style.Shape.CIRCLE, Style.parseColor("#b1c2e1"), Style.parseColor("#00519e")));
        STYLES.put("B70", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
        STYLES.put("B71", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
        STYLES.put("B72", new Style(Style.Shape.CIRCLE, Style.parseColor("#a99990"), Style.WHITE));
        STYLES.put("B76", new Style(Style.Shape.CIRCLE, Style.parseColor("#c3655a"), Style.WHITE));

        STYLES.put("T2", new Style(Style.Shape.RECT, Style.parseColor("#006ab3"), Style.WHITE));
        STYLES.put("T13", new Style(Style.Shape.RECT, Style.parseColor("#e2001a"), Style.WHITE));
        STYLES.put("T64", new Style(Style.Shape.RECT, Style.parseColor("#97bf0d"), Style.WHITE));

        STYLES.put("RR1", new Style(Style.Shape.ROUNDED, Style.parseColor("#1bbbea"), Style.WHITE));
        STYLES.put("RR2", new Style(Style.Shape.ROUNDED, Style.parseColor("#003a80"), Style.WHITE));
        STYLES.put("RR4", new Style(Style.Shape.ROUNDED, Style.parseColor("#bd5619"), Style.WHITE));
        STYLES.put("RR6", new Style(Style.Shape.ROUNDED, Style.parseColor("#0098a1"), Style.WHITE));
        STYLES.put("RR7", new Style(Style.Shape.ROUNDED, Style.parseColor("#80191c"), Style.WHITE));
        STYLES.put("RR8", new Style(Style.Shape.ROUNDED, Style.parseColor("#007d40"), Style.WHITE));
        STYLES.put("RR11", new Style(Style.Shape.ROUNDED, Style.parseColor("#e6a300"), Style.WHITE));
    }
}
