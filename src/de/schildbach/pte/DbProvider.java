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
 * Provider implementation for Deutsche Bahn (Germany).
 * 
 * @author Andreas Schildbach
 */
public final class DbProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://reiseauskunft.bahn.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, // ICE-Züge
            Product.HIGH_SPEED_TRAIN, // Intercity- und Eurocityzüge
            Product.HIGH_SPEED_TRAIN, // Interregio- und Schnellzüge
            Product.REGIONAL_TRAIN, // Nahverkehr, sonstige Züge
            Product.SUBURBAN_TRAIN, // S-Bahn
            Product.BUS, // Busse
            Product.FERRY, // Schiffe
            Product.SUBWAY, // U-Bahnen
            Product.TRAM, // Straßenbahnen
            Product.ON_DEMAND, // Anruf-Sammeltaxi
            null, null, null, null };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"DB\",\"v\":\"16040000\",\"type\":\"AND\",\"name\":\"DB Navigator\"}";

    public DbProvider(final String apiAuthorization, final byte[] salt) {
        this(DEFAULT_API_CLIENT, apiAuthorization, salt);
    }

    public DbProvider(final String apiClient, final String apiAuthorization, final byte[] salt) {
        super(NetworkId.DB, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.15");
        setApiExt("DB.R18.06.a");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setRequestChecksumSalt(salt);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    private static final Pattern P_SPLIT_NAME_ONE_COMMA = Pattern.compile("([^,]*), ([^,]*)");

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_ONE_COMMA.matcher(name);
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
