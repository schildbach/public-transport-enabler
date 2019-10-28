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

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * Provider implementation for the Österreichische Bundesbahnen (Austria).
 * 
 * @author Andreas Schildbach
 */
public class OebbProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://fahrplan.oebb.at/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN,
            Product.BUS, Product.FERRY, Product.SUBWAY, Product.TRAM, Product.HIGH_SPEED_TRAIN, Product.ON_DEMAND,
            Product.HIGH_SPEED_TRAIN };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"OEBB\",\"type\":\"AND\"}";

    public OebbProvider(final String apiAuthorization) {
        this(API_BASE, apiAuthorization);
    }

    public OebbProvider(final HttpUrl apiBase, final String apiAuthorization) {
        this(apiBase, DEFAULT_API_CLIENT, apiAuthorization);
    }

    public OebbProvider(final HttpUrl apiBase, final String apiClient, final String apiAuthorization) {
        super(NetworkId.OEBB, apiBase, PRODUCTS_MAP);
        setApiVersion("1.16");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }

    private static final String[] PLACES = { "Wien", "Graz", "Linz/Donau", "Salzburg", "Innsbruck" };

    @Override
    protected String[] splitStationName(final String name) {
        for (final String place : PLACES)
            if (name.startsWith(place + " "))
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

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }
}
