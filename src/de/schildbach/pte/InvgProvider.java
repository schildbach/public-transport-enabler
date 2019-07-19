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

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

/**
 * Provider implementation for the Ingolstädter Verkehrsgesellschaft (Ingolstadt, Germany).
 * 
 * @author Andreas Schildbach
 */
public class InvgProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://fpa.invg.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.BUS, null, null, Product.REGIONAL_TRAIN };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"INVG\",\"type\":\"AND\"}";

    public InvgProvider(final String apiAuthorization, final byte[] salt) {
        this(DEFAULT_API_CLIENT, apiAuthorization, salt);
    }

    public InvgProvider(final String apiClient, final String apiAuthorization, final byte[] salt) {
        super(NetworkId.INVG, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.16");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setRequestMicMacSalt(salt);
        setStyles(STYLES);
    }

    private static final String[] PLACES = { "Ingolstadt", "München" };

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        for (final String place : PLACES)
            if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };

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
        STYLES.put("B10", new Style(Style.parseColor("#DA2510"), Style.WHITE));
        STYLES.put("B11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
        STYLES.put("B15", new Style(Style.parseColor("#84C326"), Style.BLACK));
        STYLES.put("B16", new Style(Style.parseColor("#5D452E"), Style.WHITE));
        STYLES.put("B17", new Style(Style.parseColor("#E81100"), Style.BLACK));
        STYLES.put("B18", new Style(Style.parseColor("#79316C"), Style.WHITE));
        STYLES.put("B20", new Style(Style.parseColor("#EA891C"), Style.BLACK));
        STYLES.put("B21", new Style(Style.parseColor("#31B2EA"), Style.BLACK));
        STYLES.put("B25", new Style(Style.parseColor("#7F65A0"), Style.WHITE));
        STYLES.put("B26", new Style(Style.parseColor("#00BF73"), Style.WHITE)); // not present in Fahrplan
                                                                                // 2012/2013
        STYLES.put("B30", new Style(Style.parseColor("#901E78"), Style.WHITE));
        STYLES.put("B31", new Style(Style.parseColor("#DCE722"), Style.BLACK));
        STYLES.put("B40", new Style(Style.parseColor("#009240"), Style.WHITE));
        STYLES.put("B41", new Style(Style.parseColor("#7BC5B1"), Style.BLACK));
        STYLES.put("B44", new Style(Style.parseColor("#EA77A6"), Style.WHITE));
        STYLES.put("B50", new Style(Style.parseColor("#FACF00"), Style.BLACK));
        STYLES.put("B51", new Style(Style.parseColor("#C13C00"), Style.WHITE));
        STYLES.put("B52", new Style(Style.parseColor("#94F0D4"), Style.BLACK));
        STYLES.put("B53", new Style(Style.parseColor("#BEB405"), Style.BLACK));
        STYLES.put("B55", new Style(Style.parseColor("#FFF500"), Style.BLACK));
        STYLES.put("B60", new Style(Style.parseColor("#0072B7"), Style.WHITE));
        STYLES.put("B61", new Style(Style.rgb(204, 184, 122), Style.BLACK)); // not present in Fahrplan
                                                                             // 2012/2013
        STYLES.put("B62", new Style(Style.rgb(204, 184, 122), Style.BLACK)); // not present in Fahrplan
                                                                             // 2012/2013
        STYLES.put("B65", new Style(Style.parseColor("#B7DDD2"), Style.BLACK));
        STYLES.put("B70", new Style(Style.parseColor("#D49016"), Style.BLACK));
        STYLES.put("B71", new Style(Style.parseColor("#996600"), Style.BLACK)); // not present in Fahrplan
                                                                                // 2012/2013
        STYLES.put("B85", new Style(Style.parseColor("#F6BAD3"), Style.BLACK));
        STYLES.put("B111", new Style(Style.parseColor("#EE9B78"), Style.BLACK));

        STYLES.put("B9221", new Style(Style.rgb(217, 217, 255), Style.BLACK));
        STYLES.put("B9226", new Style(Style.rgb(191, 255, 255), Style.BLACK));

        STYLES.put("BN1", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN2", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN3", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN4", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN5", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN6", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN7", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN8", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN9", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN10", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN11", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN12", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN13", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN14", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN15", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN16", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN17", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN18", new Style(Style.parseColor("#00116C"), Style.WHITE));
        STYLES.put("BN19", new Style(Style.parseColor("#00116C"), Style.WHITE));

        STYLES.put("BS1", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS2", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS3", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS4", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS5", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS6", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS7", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS8", new Style(Style.rgb(178, 25, 0), Style.WHITE));
        STYLES.put("BS9", new Style(Style.rgb(178, 25, 0), Style.WHITE));

        STYLES.put("BX11", new Style(Style.parseColor("#EE9B78"), Style.BLACK));
        STYLES.put("BX12", new Style(Style.parseColor("#B11839"), Style.BLACK));
        STYLES.put("BX80", new Style(Style.parseColor("#FFFF40"), Style.BLACK));
        STYLES.put("BX109", new Style(Style.WHITE, Style.BLACK, Style.BLACK));
    }
}
