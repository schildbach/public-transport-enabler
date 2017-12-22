/*
 * Copyright 2010-2015 the original author or authors.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import de.schildbach.pte.dto.Product;
import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class IvbProvider extends AbstractHafasMobileProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://fahrplan.ivb.at/bin/");
    // TODO review
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY,
            null, Product.TRAM, Product.REGIONAL_TRAIN, Product.BUS, Product.BUS, Product.TRAM, Product.FERRY,
            Product.ON_DEMAND, Product.BUS, Product.REGIONAL_TRAIN, null, null, null };

    public IvbProvider(final String apiAuthorization) {
        super(NetworkId.IVB, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.20");
        setApiAuthorization(apiAuthorization);
        setApiClient("{\"id\":\"VAO\",\"l\":\"vs_ivb\",\"type\":\"AND\"}");
    }
}
