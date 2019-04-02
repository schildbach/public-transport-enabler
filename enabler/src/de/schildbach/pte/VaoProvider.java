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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

/**
 * Provider implementation for the Verkehrsauskunft Österreich (Austria).
 * 
 * @author Andreas Schildbach
 */
public class VaoProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://app.verkehrsauskunft.at/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY,
            null, Product.TRAM, Product.REGIONAL_TRAIN, Product.BUS, Product.BUS, Product.TRAM, Product.FERRY,
            Product.ON_DEMAND, Product.BUS, Product.REGIONAL_TRAIN, null, null, null };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"VAO\",\"l\":\"vs_vao\",\"type\":\"AND\"}";

    public VaoProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public VaoProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.VAO, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiExt("VAO.6");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setStyles(STYLES);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    private static final Pattern P_SPLIT_NAME_ONE_COMMA = Pattern.compile("([^,]*), ([^,]{3,64})");

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };

        return super.splitStationName(name);
    }

    @Override
    protected String[] splitPOI(final String poi) {
        final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(poi);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };

        return super.splitPOI(poi);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        return super.splitAddress(address);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Salzburg S-Bahn
        STYLES.put("Salzburg AG|SS1", new Style(Style.parseColor("#b61d33"), Style.WHITE));
        STYLES.put("Salzburg AG|SS11", new Style(Style.parseColor("#b61d33"), Style.WHITE));
        STYLES.put("OEBB|SS2", new Style(Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("OEBB|SS3", new Style(Style.parseColor("#0aa537"), Style.WHITE));
        STYLES.put("BLB|SS4", new Style(Style.parseColor("#a862a4"), Style.WHITE));

        // Salzburg Bus
        STYLES.put("Salzburg AG|B1", new Style(Style.parseColor("#e3000f"), Style.WHITE));
        STYLES.put("Salzburg AG|B2", new Style(Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("Salzburg AG|B3", new Style(Style.parseColor("#956b27"), Style.WHITE));
        STYLES.put("Salzburg AG|B4", new Style(Style.parseColor("#ffcc00"), Style.WHITE));
        STYLES.put("Salzburg AG|B5", new Style(Style.parseColor("#04bbee"), Style.WHITE));
        STYLES.put("Salzburg AG|B6", new Style(Style.parseColor("#85bc22"), Style.WHITE));
        STYLES.put("Salzburg AG|B7", new Style(Style.parseColor("#009a9b"), Style.WHITE));
        STYLES.put("Salzburg AG|B8", new Style(Style.parseColor("#f39100"), Style.WHITE));
        STYLES.put("Salzburg AG|B10", new Style(Style.parseColor("#f8baa2"), Style.BLACK));
        STYLES.put("Salzburg AG|B12", new Style(Style.parseColor("#b9dfde"), Style.WHITE));
        STYLES.put("Salzburg AG|B14", new Style(Style.parseColor("#cfe09a"), Style.WHITE));
    }
}
