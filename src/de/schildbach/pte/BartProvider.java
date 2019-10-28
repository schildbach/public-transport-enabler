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

import de.schildbach.pte.dto.Product;
import okhttp3.HttpUrl;

import java.util.regex.Matcher;

/**
 * Provider implementation for the Bay Area Rapid Transit (San Francisco, USA).
 *
 * @author Andreas Schildbach
 */
public class BartProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://planner.bart.gov/bin/");
    private static final Product[] PRODUCTS_MAP = { null, null, Product.CABLECAR, Product.REGIONAL_TRAIN, null,
            Product.BUS, Product.FERRY, Product.SUBURBAN_TRAIN, Product.TRAM };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"BART\",\"type\":\"WEB\"}";

    public BartProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public BartProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.BART, API_BASE, PRODUCTS_MAP);
        setTimeZone("America/Los_Angeles");
        setApiVersion("1.18");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };
        return super.splitStationName(name);
    }

    @Override
    protected String[] splitPOI(final String name) {
        final Matcher m = P_SPLIT_NAME_LAST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };
        return super.splitPOI(name);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_NEXT_TO_LAST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };
        return super.splitAddress(address);
    }
}
