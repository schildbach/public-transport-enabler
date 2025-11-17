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

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import okhttp3.HttpUrl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Provider implementation for the Rhein-Main-Verkehrsverbund (Germany).
 */
public class RmvProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.rmv.de/auskunft/bin/jp/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.BUS,
            Product.FERRY, Product.ON_DEMAND, Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"RMV\",\"type\":\"WEB\",\"name\":\"webapp\",\"l\":\"vs_webapp\"}";

    public RmvProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public RmvProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.RMV, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.79");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setStyles(STYLES);
    }

    private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg",
            "Kassel", "Hanau", "Göttingen", "Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

    @Override
    protected String[] splitStationName(final String name) {
        if (name.startsWith("F "))
            return new String[] { "Frankfurt", name.substring(2) };
        if (name.startsWith("OF "))
            return new String[] { "Offenbach", name.substring(3) };
        if (name.startsWith("MZ "))
            return new String[] { "Mainz", name.substring(3) };

        for (final String place : PLACES) {
            if (name.startsWith(place + " - "))
                return new String[] { place, name.substring(place.length() + 3) };
            else if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
        }

        return super.splitStationName(name);
    }

    @Override
    protected String[] splitPOI(final String poi) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        return super.splitStationName(poi);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        return super.splitStationName(address);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        STYLES.put("UU1", new Style(Style.Shape.RECT, Style.rgb(184, 41, 47), Style.WHITE));
        STYLES.put("UU2", new Style(Style.Shape.RECT, Style.rgb(0, 166, 81), Style.WHITE));
        STYLES.put("UU3", new Style(Style.Shape.RECT, Style.rgb(75, 93, 170), Style.WHITE));
        STYLES.put("UU4", new Style(Style.Shape.RECT, Style.rgb(240, 92, 161), Style.WHITE));
        STYLES.put("UU5", new Style(Style.Shape.RECT, Style.rgb(1, 122, 67), Style.WHITE));
        STYLES.put("UU6", new Style(Style.Shape.RECT, Style.rgb(1, 125, 198), Style.WHITE));
        STYLES.put("UU7", new Style(Style.Shape.RECT, Style.rgb(228, 161, 35), Style.WHITE));
        STYLES.put("UU8", new Style(Style.Shape.RECT, Style.rgb(199, 125, 181), Style.WHITE));
        STYLES.put("UU9", new Style(Style.Shape.RECT, Style.rgb(255, 222, 1), Style.BLACK));
        STYLES.put("SS1", new Style(Style.Shape.CIRCLE, Style.rgb(0, 136, 195), Style.WHITE));
        STYLES.put("SS2", new Style(Style.Shape.CIRCLE, Style.rgb(210, 33, 41), Style.WHITE));
        STYLES.put("SS3", new Style(Style.Shape.CIRCLE, Style.rgb(0, 157, 135), Style.WHITE));
        STYLES.put("SS4", new Style(Style.Shape.CIRCLE, Style.rgb(255, 222, 1), Style.BLACK, Style.BLACK));
        STYLES.put("SS5", new Style(Style.Shape.CIRCLE, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("SS6", new Style(Style.Shape.CIRCLE, Style.rgb(229, 113, 42), Style.WHITE));
        STYLES.put("SS7", new Style(Style.Shape.CIRCLE, Style.rgb(37, 75, 58), Style.WHITE));
        STYLES.put("SS8", new Style(Style.Shape.CIRCLE, Style.rgb(131, 191, 66), Style.WHITE));
        STYLES.put("SS9", new Style(Style.Shape.CIRCLE, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("T11", new Style(Style.Shape.RECT, Style.rgb(136, 129, 189), Style.WHITE));
        STYLES.put("T12", new Style(Style.Shape.RECT, Style.rgb(231, 185, 9), Style.WHITE));
        STYLES.put("T14", new Style(Style.Shape.RECT, Style.rgb(0, 166, 222), Style.WHITE));
        STYLES.put("T15", new Style(Style.Shape.RECT, Style.rgb(245, 130, 32), Style.WHITE));
        STYLES.put("T16", new Style(Style.Shape.RECT, Style.rgb(81, 184, 72), Style.WHITE));
        STYLES.put("T17", new Style(Style.Shape.RECT, Style.rgb(237, 29, 37), Style.WHITE));
        STYLES.put("T18", new Style(Style.Shape.RECT, Style.rgb(22, 71, 158), Style.WHITE));
        STYLES.put("T19", new Style(Style.Shape.RECT, Style.WHITE, Style.rgb(120, 205, 208), Style.rgb(120, 205, 208)));
        STYLES.put("T20", new Style(Style.Shape.RECT, Style.WHITE, Style.rgb(148, 149, 152), Style.rgb(148, 149, 152)));
        STYLES.put("T21", new Style(Style.Shape.RECT, Style.rgb(242, 135, 183), Style.WHITE));
        STYLES.put("RRB2", new Style(Style.Shape.RECT, Style.rgb(100, 183, 117), Style.WHITE));
        STYLES.put("RRB5", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB6", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB7", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB10", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB11", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB12", new Style(Style.Shape.RECT, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("RRB15", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB16", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB17", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB21", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB22", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB23", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRB26", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB29", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB31", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRB33", new Style(Style.Shape.RECT, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("RRB34", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB35", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB38", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB39", new Style(Style.Shape.RECT, Style.rgb(0, 183, 223), Style.WHITE));
        STYLES.put("RRB40", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB41", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB42", new Style(Style.Shape.RECT, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("RRB44", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB45", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB46", new Style(Style.Shape.RECT, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("RRB47", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB48", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB49", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB50", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB51", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB52", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB53", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB56", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB58", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB60", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB61", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB62", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB63", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRB65", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRB66", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB67", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB68", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB69", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRB75", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB81", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB82", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB85", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB86", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRB90", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRB94", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRB95", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRB96", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRE2", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE3", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE4", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE12", new Style(Style.Shape.RECT, Style.rgb(133, 84, 55), Style.WHITE));
        STYLES.put("RRE13", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRE14", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE15", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE17", new Style(Style.Shape.RECT, Style.rgb(187, 117, 163), Style.WHITE));
        STYLES.put("RRE20", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE25", new Style(Style.Shape.RECT, Style.rgb(0, 115, 176), Style.WHITE));
        STYLES.put("RRE30", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE31", new Style(Style.Shape.RECT, Style.rgb(0, 184, 224), Style.WHITE));
        STYLES.put("RRE50", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE51", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE54", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRE55", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRE59", new Style(Style.Shape.RECT, Style.rgb(129, 43, 124), Style.WHITE));
        STYLES.put("RRE60", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE70", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE80", new Style(Style.Shape.RECT, Style.rgb(217, 34, 42), Style.WHITE));
        STYLES.put("RRE85", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE98", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
        STYLES.put("RRE99", new Style(Style.Shape.RECT, Style.rgb(216, 154, 47), Style.WHITE));
    }
}
