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
 * Provider implementation for the Oberösterreichischer Verkehrsverbund (Upper Austria, Austria).
 * 
 * @author Andreas Schildbach
 */
public class OoevvProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://verkehrsauskunft.ooevv.at/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY,
            null, Product.TRAM, Product.REGIONAL_TRAIN, Product.BUS, Product.BUS, Product.TRAM, Product.FERRY,
            Product.ON_DEMAND, Product.BUS, Product.REGIONAL_TRAIN, null, null, null };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"VAO\",\"l\":\"vs_ooevv\",\"type\":\"AND\"}";

    public OoevvProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public OoevvProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.OOEVV, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiExt("VAO.6");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
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
}
