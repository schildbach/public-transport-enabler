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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import java.util.Set;
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class SbbProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("http://fahrplan.sbb.ch/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, Product.FERRY, Product.SUBURBAN_TRAIN, Product.BUS,
            Product.CABLECAR, Product.REGIONAL_TRAIN, Product.TRAM };

    public SbbProvider() {
        super(NetworkId.SBB, API_BASE, "dn", PRODUCTS_MAP);

        setStationBoardHasStationTable(false);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

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

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if ("IN".equals(ucType)) // Italien Roma-Lecce
            return Product.HIGH_SPEED_TRAIN;
        if ("IT".equals(ucType)) // Italien Roma-Venezia
            return Product.HIGH_SPEED_TRAIN;

        if ("T".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("TE2".equals(ucType)) // Basel - Strasbourg
            return Product.REGIONAL_TRAIN;

        if ("TX".equals(ucType))
            return Product.BUS;
        if ("NFO".equals(ucType))
            return Product.BUS;
        if ("KB".equals(ucType)) // Kleinbus?
            return Product.BUS;

        return super.normalizeType(type);
    }
}
