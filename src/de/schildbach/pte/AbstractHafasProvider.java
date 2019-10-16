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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.StationDepartures;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider extends AbstractNetworkProvider {
    protected static final String SERVER_PRODUCT = "hafas";
    protected static final int DEFAULT_MAX_DEPARTURES = 100;
    protected static final int DEFAULT_MAX_LOCATIONS = 50;
    protected static final int DEFAULT_MAX_DISTANCE = 20000;

    protected static final Logger log = LoggerFactory.getLogger(AbstractHafasProvider.class);

    private Product[] productsMap;

    protected AbstractHafasProvider(final NetworkId network, final Product[] productsMap) {
        super(network);
        this.productsMap = productsMap;
    }

    @Override
    protected boolean hasCapability(final Capability capability) {
        return true;
    }

    protected final CharSequence productsString(final Set<Product> products) {
        final StringBuilder productsStr = new StringBuilder(productsMap.length);
        for (int i = 0; i < productsMap.length; i++) {
            if (productsMap[i] != null && products.contains(productsMap[i]))
                productsStr.append('1');
            else
                productsStr.append('0');
        }
        return productsStr;
    }

    protected final CharSequence allProductsString() {
        final StringBuilder productsStr = new StringBuilder(productsMap.length);
        for (int i = 0; i < productsMap.length; i++)
            productsStr.append('1');
        return productsStr;
    }

    protected final int allProductsInt() {
        return (1 << productsMap.length) - 1;
    }

    protected final Product intToProduct(final int productInt) {
        final int allProductsInt = allProductsInt();
        checkArgument(productInt <= allProductsInt,
                "value " + productInt + " cannot be greater than " + allProductsInt);

        int value = productInt;
        Product product = null;
        for (int i = productsMap.length - 1; i >= 0; i--) {
            final int v = 1 << i;
            if (value >= v) {
                final Product p = productsMap[i];
                if ((product == Product.ON_DEMAND && p == Product.BUS)
                        || (product == Product.BUS && p == Product.ON_DEMAND))
                    product = Product.ON_DEMAND;
                else if (product != null && p != product)
                    throw new IllegalArgumentException("ambiguous value: " + productInt);
                else
                    product = p;
                value -= v;
            }
        }
        checkState(value == 0);
        return product;
    }

    protected final Set<Product> intToProducts(int value) {
        final int allProductsInt = allProductsInt();
        checkArgument(value <= allProductsInt, "value " + value + " cannot be greater than " + allProductsInt);

        final Set<Product> products = EnumSet.noneOf(Product.class);
        for (int i = productsMap.length - 1; i >= 0; i--) {
            final int v = 1 << i;
            if (value >= v) {
                if (productsMap[i] != null)
                    products.add(productsMap[i]);
                value -= v;
            }
        }
        checkState(value == 0);
        return products;
    }

    protected static final Pattern P_SPLIT_NAME_FIRST_COMMA = Pattern.compile("([^,]*), (.*)");
    protected static final Pattern P_SPLIT_NAME_LAST_COMMA = Pattern.compile("(.*), ([^,]*)");
    protected static final Pattern P_SPLIT_NAME_NEXT_TO_LAST_COMMA = Pattern.compile("(.*), ([^,]*, [^,]*)");
    protected static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{3,}?)\\)");

    protected String[] splitStationName(final String name) {
        return new String[] { null, name };
    }

    protected String[] splitPOI(final String poi) {
        return new String[] { null, poi };
    }

    protected String[] splitAddress(final String address) {
        return new String[] { null, address };
    }

    private static final Pattern P_POSITION_PLATFORM = Pattern.compile("Gleis\\s*(.*)\\s*", Pattern.CASE_INSENSITIVE);

    protected Position normalizePosition(final String position) {
        if (position == null)
            return null;

        final Matcher m = P_POSITION_PLATFORM.matcher(position);
        if (!m.matches())
            return parsePosition(position);

        return parsePosition(m.group(1));
    }

    protected final StationDepartures findStationDepartures(final List<StationDepartures> stationDepartures,
            final Location location) {
        for (final StationDepartures stationDeparture : stationDepartures)
            if (stationDeparture.location.equals(location))
                return stationDeparture;

        return null;
    }
}
