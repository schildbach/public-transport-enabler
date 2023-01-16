/*
import de.schildbach.pte.dto.Style.Shape;
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

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * Provider implementation for the Transport Publics Genevois (Geneva, Switzerland).
 * 
 * @author Andreas Schildbach
 */
public class TpgProvider extends AbstractHafasClientInterfaceProvider {
    // private static final Product[] PRODUCTS_MAP = { Product.FERRY, Product.SUBURBAN_TRAIN, Product.BUS, Product.TRAM };
    private static final HttpUrl API_BASE = HttpUrl.parse("https://tpg.hafas.cloud/bin/");
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"HAFAS\",\"l\":\"vs_webapp\",\"name\":\"webapp\",\"type\":\"WEB\"}";

    public TpgProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public TpgProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.TPG, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.24");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }
}
