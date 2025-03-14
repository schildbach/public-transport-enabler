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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://mobil.rozklad-pkp.pl:8019/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, // High speed trains from other
                                                                              // countries
            Product.HIGH_SPEED_TRAIN, // EIP, EIC, EC and international equivalents
            Product.HIGH_SPEED_TRAIN, // IC, TLK, IR and international equivalents
            Product.REGIONAL_TRAIN, // R (Regio), Os (Osobowy) and other regional and suburban trains
            Product.BUS, Product.BUS, Product.FERRY };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"HAFAS\",\"type\":\"AND\"}";

    public PlProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public PlProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.PL, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.21");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setSessionCookieName("SERVERID");
        setUserAgent("Dalvik/2.1.0 (Linux; U; Android 15)");
    }

    private static final String[] PLACES = { "Warszawa", "Kraków" };

    @Override
    protected String[] splitStationName(final String name) {
        for (final String place : PLACES) {
            if (name.endsWith(", " + place))
                return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
            if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
        }

        return super.splitStationName(name);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }
}
