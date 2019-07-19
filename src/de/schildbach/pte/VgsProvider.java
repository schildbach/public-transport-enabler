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

import java.util.regex.Matcher;

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * Provider implementation for Saarfahrplan (Saarland, Germany).
 * 
 * @author Andreas Schildbach
 */
public class VgsProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://saarfahrplan.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM,
            Product.BUS, Product.CABLECAR, Product.ON_DEMAND, Product.BUS };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"ZPS-SAAR\",\"type\":\"AND\"}";

    public VgsProvider(final String apiAuthorization, final byte[] salt) {
        this(DEFAULT_API_CLIENT, apiAuthorization, salt);
    }

    public VgsProvider(final String apiClient, final String apiAuthorization, final byte[] salt) {
        super(NetworkId.VGS, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.21");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setRequestMicMacSalt(salt);
    }

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };

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
}
