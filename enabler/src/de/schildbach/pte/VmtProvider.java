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
 * Provider implementation for the Verkehrsverbund Mittelthüringen (Thuringia, Germany).
 * 
 * @author Andreas Schildbach
 */
public class VmtProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://vmt.hafas.de/bin/");
    // http://www.routenplaner.nahverkehr-jena.de/bin/
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.TRAM, Product.FERRY,
            Product.BUS /* guessing */, Product.BUS, null };
    private static final String DEFAULT_API_CLIENT = "{\"name\":\"VMT\",\"type\":\"AND\"}";

    public VmtProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public VmtProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.VMT, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.15");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }

    private static final String[] PLACES = { "Erfurt", "Jena", "Gera", "Weimar", "Gotha" };

    @Override
    protected String[] splitStationName(String name) {
        if (name.endsWith(" [Zug]"))
            name = name.substring(0, name.length() - 6);

        for (final String place : PLACES) {
            if (name.startsWith(place + ", "))
                return new String[] { place, name.substring(place.length() + 2) };
            if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
        }
        return super.splitStationName(name);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        return super.splitStationName(address);
    }
}
