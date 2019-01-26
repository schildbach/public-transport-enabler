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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class NvbwProvider extends AbstractEfaProvider {
    private final static HttpUrl API_BASE = HttpUrl.parse("https://www.efa-bw.de/nvbw3L/");
    // https://efaserver.vag-freiburg.de/vagfr/
    // http://efa2.naldo.de/naldo/

    public NvbwProvider() {
        super(NetworkId.NVBW, API_BASE);
        setIncludeRegionId(false);
        setStyles(STYLES);
        setSessionCookieName("EFABWLB");
    }

    private static final Pattern P_LINE_S_AVG_VBK = Pattern.compile("(S\\d+) \\((?:AVG|VBK)\\)");

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if (("ICE".equals(trainName) || "InterCityExpress".equals(trainName)) && trainNum == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "ICE");
            if ("InterCity".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "IC");
            if (("IC3".equals(trainNum) || "IC4".equals(trainNum) || "IC5".equals(trainNum) || "IC8".equals(trainNum))
                    && trainType == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, trainNum);
            if ("Fernreisezug externer EU".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, null);
            if ("SuperCity".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.HIGH_SPEED_TRAIN, "SC");
            if ("InterRegio".equals(longName) && symbol == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "IR");
            if ("REGIOBAHN".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, null);
            if ("Meridian".equals(trainName) && symbol != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("CityBahn".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "CB");
            if ("Trilex".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "TLX");
            if ("Bay. Seenschifffahrt".equals(trainName) && symbol != null)
                return new Line(id, network, Product.FERRY, symbol);
            if ("Nahverkehrszug von Dritten".equals(trainName) && trainNum == null)
                return new Line(id, network, null, "Zug");
            if ("DB".equals(trainName) && trainNum == null)
                return new Line(id, network, null, "DB");
        } else if ("1".equals(mot)) {
            if (symbol != null && symbol.equals(name)) {
                final Matcher m = P_LINE_S_AVG_VBK.matcher(symbol);
                if (m.matches())
                    return new Line(id, network, Product.SUBURBAN_TRAIN, m.group(1));
            }
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Tram
        STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#33b540"), Style.WHITE));
        STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#f79210"), Style.WHITE));
        STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#ef58a1"), Style.WHITE));
        STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#0994ce"), Style.WHITE));

        // Nachtbus
        STYLES.put("N46", new Style(Style.parseColor("#28bda5"), Style.WHITE));
        STYLES.put("N47", new Style(Style.parseColor("#d6de20"), Style.WHITE));
    }
}
