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

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class Standard {
    public static final int COLOR_BACKGROUND_HIGH_SPEED_TRAIN = Style.WHITE;
    public static final int COLOR_BACKGROUND_REGIONAL_TRAIN = Style.GRAY;
    public static final int COLOR_BACKGROUND_SUBURBAN_TRAIN = Style.parseColor("#006e34");
    public static final int COLOR_BACKGROUND_SUBWAY = Style.parseColor("#003090");
    public static final int COLOR_BACKGROUND_TRAM = Style.parseColor("#cc0000");
    public static final int COLOR_BACKGROUND_BUS = Style.parseColor("#993399");
    public static final int COLOR_BACKGROUND_ON_DEMAND = Style.parseColor("#00695c");
    public static final int COLOR_BACKGROUND_FERRY = Style.BLUE;

    public static final Map<Product, Style> STYLES = new HashMap<>();

    static {
        STYLES.put(Product.HIGH_SPEED_TRAIN,
                new Style(Shape.RECT, COLOR_BACKGROUND_HIGH_SPEED_TRAIN, Style.RED, Style.RED));
        STYLES.put(Product.REGIONAL_TRAIN, new Style(Shape.RECT, COLOR_BACKGROUND_REGIONAL_TRAIN, Style.WHITE));
        STYLES.put(Product.SUBURBAN_TRAIN, new Style(Shape.CIRCLE, COLOR_BACKGROUND_SUBURBAN_TRAIN, Style.WHITE));
        STYLES.put(Product.SUBWAY, new Style(Shape.RECT, COLOR_BACKGROUND_SUBWAY, Style.WHITE));
        STYLES.put(Product.TRAM, new Style(Shape.RECT, COLOR_BACKGROUND_TRAM, Style.WHITE));
        STYLES.put(Product.BUS, new Style(COLOR_BACKGROUND_BUS, Style.WHITE));
        STYLES.put(Product.ON_DEMAND, new Style(COLOR_BACKGROUND_ON_DEMAND, Style.WHITE));
        STYLES.put(Product.FERRY, new Style(Shape.CIRCLE, COLOR_BACKGROUND_FERRY, Style.WHITE));
        STYLES.put(null, new Style(Style.DKGRAY, Style.WHITE));
    }
}
