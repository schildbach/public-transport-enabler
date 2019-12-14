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

/**
 * Provider implementation for the Capital Metropolitan Transportation Authority (Austin, Texas, US).
 * 
 * @author Colin Murphy
 */
public class CmtaProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://capmetro.hafas.cloud/bin/");
    // MetroRail: 8, MetroBus: 32, MetroRapid: 4096
    private static final Product[] PRODUCTS_MAP = { null, null, null, Product.REGIONAL_TRAIN, null, Product.BUS, null,
            null, null, null, null, null, Product.BUS };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"CMTA\",\"type\":\"AND\"}";

    public CmtaProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public CmtaProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.CMTA, API_BASE, PRODUCTS_MAP);
        setTimeZone("America/Chicago");
        setApiVersion("1.14");
        setApiExt("SBB.TZT.1");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }
}
