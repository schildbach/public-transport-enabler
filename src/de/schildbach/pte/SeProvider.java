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
}
