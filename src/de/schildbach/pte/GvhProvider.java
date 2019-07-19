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

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://app.efa.de/mdv_server/app_gvh/");

    public GvhProvider() {
        this(API_BASE);
    }

    public GvhProvider(final HttpUrl apiBase) {
        super(NetworkId.GVH, apiBase);

        setIncludeRegionId(false);
        setStyles(STYLES);
        setSessionCookieName("HASESSIONID");
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("RX".equals(trainType) && trainNum != null) // Express, Czech Republic
                return new Line(id, network, Product.REGIONAL_TRAIN, "RX" + trainNum);
            if ("S4".equals(trainNum))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "S4");
            if (longName != null && longName.startsWith("Bus ") && name != null)
                return new Line(id, network, Product.BUS, name);
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Hannover
        STYLES.put("SS1", new Style(Style.Shape.CIRCLE, Style.parseColor("#816ba8"), Style.WHITE));
        STYLES.put("SS2", new Style(Style.Shape.CIRCLE, Style.parseColor("#007a3b"), Style.WHITE));
        STYLES.put("SS21", new Style(Style.Shape.CIRCLE, Style.parseColor("#007a3b"), Style.WHITE));
        STYLES.put("SS3", new Style(Style.Shape.CIRCLE, Style.parseColor("#cc68a6"), Style.WHITE));
        STYLES.put("SS4", new Style(Style.Shape.CIRCLE, Style.parseColor("#9b2a48"), Style.WHITE));
        STYLES.put("SS5", new Style(Style.Shape.CIRCLE, Style.parseColor("#f18700"), Style.WHITE));
        STYLES.put("SS51", new Style(Style.Shape.CIRCLE, Style.parseColor("#f18700"), Style.WHITE));
        STYLES.put("SS6", new Style(Style.Shape.CIRCLE, Style.parseColor("#004e9e"), Style.WHITE));
        STYLES.put("SS7", new Style(Style.Shape.CIRCLE, Style.parseColor("#afcb25"), Style.WHITE));

        STYLES.put("T1", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#e40039")));
        STYLES.put("T2", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#e40039")));
        STYLES.put("T3", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#0069b4")));
        STYLES.put("T4", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#f9b000")));
        STYLES.put("T5", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#f9b000")));
        STYLES.put("T6", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#f9b000")));
        STYLES.put("T16", new Style(Style.Shape.RECT, Style.WHITE, Style.GRAY, Style.parseColor("#f9b000")));
        STYLES.put("T7", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#0069b4")));
        STYLES.put("T8", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#e40039")));
        STYLES.put("T18", new Style(Style.Shape.RECT, Style.WHITE, Style.GRAY, Style.parseColor("#e40039")));
        STYLES.put("T9", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#0069b4")));
        STYLES.put("T10", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#76b828")));
        STYLES.put("T11", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#f9b000")));
        STYLES.put("T17", new Style(Style.Shape.RECT, Style.WHITE, Style.BLACK, Style.parseColor("#76b828")));

        STYLES.put("B100", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1eb5ea")));
        STYLES.put("B120", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#2eab5c")));
        STYLES.put("B121", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#2eab5c")));
        STYLES.put("B122", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e3001f")));
        STYLES.put("B123", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#2eab5c")));
        STYLES.put("B124", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e3001f")));
        STYLES.put("B125", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1eb5ea")));
        STYLES.put("B126", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B127", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B128", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#9e348b")));
        STYLES.put("B129", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B130", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B133", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B134", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e21f34")));
        STYLES.put("B135", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#2eab5c")));
        STYLES.put("B136", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1eb5ea")));
        STYLES.put("B137", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#9e348b")));
        STYLES.put("B200", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B300", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B330", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B340", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("B341", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("B350", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B360", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e3001f")));
        STYLES.put("B363", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B365", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1eb5ea")));
        STYLES.put("B366", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B370", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("B420", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1eb5ea")));
        STYLES.put("B440", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("B450", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#2eab5c")));
        STYLES.put("B460", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B461", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e3001f")));
        STYLES.put("B470", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#1a70b8")));
        STYLES.put("B490", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B491", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#9e348b")));
        STYLES.put("B500", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B570", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#9e348b")));
        STYLES.put("B571", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#a2c613")));
        STYLES.put("B574", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B580", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B581", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#e3001f")));
        STYLES.put("B620", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#fbba00")));
        STYLES.put("B631", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("B700", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.BLACK, Style.parseColor("#f39100")));
        STYLES.put("BN31", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#9e348b")));
        STYLES.put("BN41", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#1a70b8")));
        STYLES.put("BN43", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#1a70b8")));
        STYLES.put("BN56", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#2eab5c")));
        STYLES.put("BN57", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#1eb5ea")));
        STYLES.put("BN70", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#1a70b8")));
        STYLES.put("BN62", new Style(Style.Shape.CIRCLE, Style.WHITE, Style.parseColor("#1a70b8")));

        // Hamburg
        // STYLES.put("SS1", new Style(Style.parseColor("#00933B"), Style.WHITE));
        // STYLES.put("SS11", new Style(Style.WHITE, Style.parseColor("#00933B"),
        // Style.parseColor("#00933B")));
        // STYLES.put("SS2", new Style(Style.WHITE, Style.parseColor("#9D271A"),
        // Style.parseColor("#9D271A")));
        // STYLES.put("SS21", new Style(Style.parseColor("#9D271A"), Style.WHITE));
        // STYLES.put("SS3", new Style(Style.parseColor("#411273"), Style.WHITE));
        // STYLES.put("SS31", new Style(Style.parseColor("#411273"), Style.WHITE));

        STYLES.put("UU1", new Style(Style.parseColor("#044895"), Style.WHITE));
        STYLES.put("UU2", new Style(Style.parseColor("#DC2B19"), Style.WHITE));
        STYLES.put("UU3", new Style(Style.parseColor("#EE9D16"), Style.WHITE));
        STYLES.put("UU4", new Style(Style.parseColor("#13A59D"), Style.WHITE));
    }
}
