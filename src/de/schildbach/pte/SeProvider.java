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

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * Provider implementation for Samtrafiken (Sweden).
 * 
 * @author Andreas Schildbach
 */
public class SeProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://reseplanerare.resrobot.se/bin/");
    // https://samtrafiken.hafas.de/bin/
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN /* Air */, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.BUS, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS,
            Product.FERRY, Product.ON_DEMAND /* Taxi */ };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"SAMTRAFIKEN\",\"type\":\"WEB\"}";

    public SeProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public SeProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.SE, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setStyles(STYLES);
    }

    private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{3,}?) kn\\)");

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
        if (mParen.matches())
            return new String[] { mParen.group(2), mParen.group(1) };
        return super.splitStationName(name);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };
        return super.splitStationName(address);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }
    
    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // SL Tunnelbanan (Metro)
        // Blue lines
        STYLES.put("SL|ULänstrafik -Tunnelbana 10", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|ULänstrafik -Tunnelbana 11", new Style(Style.parseColor("#007db8"), Style.WHITE));
        // Red lines
        STYLES.put("SL|ULänstrafik -Tunnelbana 13", new Style(Style.parseColor("#d71d24"), Style.WHITE));
        STYLES.put("SL|ULänstrafik -Tunnelbana 14", new Style(Style.parseColor("#d71d24"), Style.WHITE));
        // Green lines
        STYLES.put("SL|ULänstrafik -Tunnelbana 17", new Style(Style.parseColor("#148541"), Style.WHITE));
        STYLES.put("SL|ULänstrafik -Tunnelbana 18", new Style(Style.parseColor("#148541"), Style.WHITE));
        STYLES.put("SL|ULänstrafik -Tunnelbana 19", new Style(Style.parseColor("#148541"), Style.WHITE));

        // SL Pendeltåg (Commuter rail)
        STYLES.put("SL|SLänstrafik - Tåg 40", new Style(Style.parseColor("#9a295c"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 41", new Style(Style.parseColor("#9a295c"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 42X", new Style(Style.parseColor("#9a295c"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 43", new Style(Style.parseColor("#9a295c"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 43X", new Style(Style.parseColor("#9a295c"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 48", new Style(Style.parseColor("#9a295c"), Style.WHITE));

        // SL Spårvagn/Lokalbana (Tram/Light rail)
        STYLES.put("SL|T7", new Style(Style.parseColor("#747770"), Style.WHITE));
        STYLES.put("SL|T12", new Style(Style.parseColor("#627892"), Style.WHITE));
        STYLES.put("SL|T21", new Style(Style.parseColor("#a54905"), Style.WHITE));
        STYLES.put("SL|T30", new Style(Style.parseColor("#b65f1f"), Style.WHITE));
        STYLES.put("SL|T31", new Style(Style.parseColor("#b65f1f"), Style.WHITE));

        STYLES.put("SL|SLänstrafik - Tåg 25", new Style(Style.parseColor("#008f93"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 26", new Style(Style.parseColor("#008f93"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 27", new Style(Style.parseColor("#9f599a"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 27S", new Style(Style.parseColor("#9f599a"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 28", new Style(Style.parseColor("#9f599a"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 28S", new Style(Style.parseColor("#9f599a"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 28X", new Style(Style.parseColor("#9f599a"), Style.WHITE));
        STYLES.put("SL|SLänstrafik - Tåg 29", new Style(Style.parseColor("#9f599a"), Style.WHITE));

        // SL Bät (Commuter ferry lines)
        STYLES.put("SL|FLänstrafik - Färja 80", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|FLänstrafik - Färja 82", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|FLänstrafik - Färja 83", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|FLänstrafik - Färja 83X", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|FLänstrafik - Färja 84", new Style(Style.parseColor("#007db8"), Style.WHITE));
        STYLES.put("SL|FLänstrafik - Färja 89", new Style(Style.parseColor("#007db8"), Style.WHITE));

        // SL Buss (Buses)
        STYLES.put("SL|B", new Style(Style.parseColor("#000000"), Style.WHITE));
    }
}
