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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class LinzProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.linzag.at/linz2/");
    // http://www.linzag.at/static/

    public LinzProvider() {
        super(NetworkId.LINZ, API_BASE);

        setUseRouteIndexAsTripId(false);
        setStyles(STYLES);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        STYLES.put("B11", new Style(Style.Shape.RECT, Style.parseColor("#f27b02"), Style.WHITE));
        STYLES.put("B12", new Style(Style.Shape.RECT, Style.parseColor("#00863a"), Style.WHITE));
        STYLES.put("B17", new Style(Style.Shape.RECT, Style.parseColor("#f47a00"), Style.WHITE));
        STYLES.put("B18", new Style(Style.Shape.RECT, Style.parseColor("#0066b5"), Style.WHITE));
        STYLES.put("B19", new Style(Style.Shape.RECT, Style.parseColor("#f36aa8"), Style.WHITE));
        STYLES.put("B25", new Style(Style.Shape.RECT, Style.parseColor("#d29f08"), Style.WHITE));
        STYLES.put("B26", new Style(Style.Shape.RECT, Style.parseColor("#0070b6"), Style.WHITE));
        STYLES.put("B27", new Style(Style.Shape.RECT, Style.parseColor("#96c41c"), Style.WHITE));
        STYLES.put("B33", new Style(Style.Shape.RECT, Style.parseColor("#6d1f82"), Style.WHITE));
        STYLES.put("B38", new Style(Style.Shape.RECT, Style.parseColor("#ef7b02"), Style.WHITE));
        STYLES.put("B43", new Style(Style.Shape.RECT, Style.parseColor("#00ace3"), Style.WHITE));
        STYLES.put("B45", new Style(Style.Shape.RECT, Style.parseColor("#db0c10"), Style.WHITE));
        STYLES.put("B46", new Style(Style.Shape.RECT, Style.parseColor("#00acea"), Style.WHITE));
        STYLES.put("B101", new Style(Style.Shape.RECT, Style.parseColor("#fdba00"), Style.WHITE));
        STYLES.put("B102", new Style(Style.Shape.RECT, Style.parseColor("#9d701f"), Style.WHITE));
        STYLES.put("B103", new Style(Style.Shape.RECT, Style.parseColor("#019793"), Style.WHITE));
        STYLES.put("B104", new Style(Style.Shape.RECT, Style.parseColor("#699c23"), Style.WHITE));
        STYLES.put("B105", new Style(Style.Shape.RECT, Style.parseColor("#004b9e"), Style.WHITE));
        STYLES.put("B191", new Style(Style.Shape.RECT, Style.parseColor("#1293a8"), Style.WHITE));
        STYLES.put("B192", new Style(Style.Shape.RECT, Style.parseColor("#947ab7"), Style.WHITE));
        STYLES.put("BN2", new Style(Style.Shape.RECT, Style.parseColor("#005aac"), Style.WHITE)); // night
        STYLES.put("BN3", new Style(Style.Shape.RECT, Style.parseColor("#b80178"), Style.WHITE)); // night
        STYLES.put("BN4", new Style(Style.Shape.RECT, Style.parseColor("#93be01"), Style.WHITE)); // night

        STYLES.put("T1", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));
        STYLES.put("TN1", new Style(Style.Shape.RECT, Style.parseColor("#db0e16"), Style.WHITE)); // night
        STYLES.put("T2", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));
        STYLES.put("T3", new Style(Style.Shape.RECT, Style.parseColor("#dd0b12"), Style.WHITE));

        STYLES.put("C50", new Style(Style.Shape.RECT, Style.parseColor("#4eae2c"), Style.WHITE)); // Pöstlingbergbahn
    }
}
