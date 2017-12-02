/*
 * Copyright 2010-2017 the original author or authors.
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

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class DsbProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.rejseplanen.dk/bin/");
    // http://dk.hafas.de/bin/fat/
    // http://www.dsb.dk/Rejseplan/bin/
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.BUS,
            Product.BUS, Product.BUS, Product.FERRY, Product.SUBWAY };

    public DsbProvider() {
        super(NetworkId.DSB, API_BASE, "mn", PRODUCTS_MAP);

        setStationBoardHasStationTable(false);
    }

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_PAREN.matcher(name);
        if (m.matches())
            return new String[] { m.group(2), m.group(1) };

        return super.splitStationName(name);
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if ("ICL".equals(ucType))
            return Product.HIGH_SPEED_TRAIN;
        if ("IB".equals(ucType))
            return Product.HIGH_SPEED_TRAIN;
        if ("SJ".equals(ucType))
            return Product.HIGH_SPEED_TRAIN;

        if ("ØR".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("RA".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("RX".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("PP".equals(ucType))
            return Product.REGIONAL_TRAIN;

        if ("S-TOG".equals(ucType))
            return Product.SUBURBAN_TRAIN;

        if ("BYBUS".equals(ucType))
            return Product.BUS;
        if ("X-BUS".equals(ucType) || "X BUS".equals(ucType))
            return Product.BUS;
        if ("HV-BUS".equals(ucType)) // Havnebus
            return Product.BUS;
        if ("T-BUS".equals(ucType)) // Togbus
            return Product.BUS;
        if ("TOGBUS".equals(ucType))
            return Product.BUS;

        if ("TELEBUS".equals(ucType))
            return Product.ON_DEMAND;
        if ("TELETAXI".equals(ucType))
            return Product.ON_DEMAND;

        if ("FÆRGE".equals(ucType))
            return Product.FERRY;

        return super.normalizeType(type);
    }

    // Busses line name is formatted as "42#Bus 42" but we just want "42"
    private static final Pattern P_NORMALIZE_LINE_NAME_BUS_DSB = Pattern.compile(".*?#Bus (.*)", Pattern.CASE_INSENSITIVE);

    @Override
    protected String normalizeLineName(final String lineName) {
        final Matcher mBus = P_NORMALIZE_LINE_NAME_BUS_DSB.matcher(lineName);
        if (mBus.matches())
            return mBus.group(1);

        return super.normalizeLineName(lineName);
    }

}
