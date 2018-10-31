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

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class NsProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://hafas.bene-system.com/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY,
            Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };

    public NsProvider() {
        super(NetworkId.NS, API_BASE, "nn", PRODUCTS_MAP);
        setJsonNearbyLocationsEncoding(Charsets.UTF_8);
        setStationBoardHasLocation(true);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if (ucType.equals("SPR"))
            return Product.REGIONAL_TRAIN;
        if (ucType.equals("N")) // Avignon
            return Product.REGIONAL_TRAIN;

        if ("FERRY".equals(ucType))
            return Product.FERRY;

        return super.normalizeType(type);
    }
}
