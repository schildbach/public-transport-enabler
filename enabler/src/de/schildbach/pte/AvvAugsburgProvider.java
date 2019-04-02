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
 * Provider implementation for the Augsburger Verkehrsverbund (Augsburg, Germany).
 * 
 * @author Andreas Schildbach
 */
public class AvvAugsburgProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://avv-augsburg.hafas.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.BUS, Product.FERRY,
            Product.SUBWAY, Product.TRAM, Product.ON_DEMAND };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"DB-REGIO-AVV\",\"v\":\"100\",\"type\":\"AND\",\"name\":\"RegioNavigator\"}";

    public AvvAugsburgProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public AvvAugsburgProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.AVV_AUGSBURG, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiExt("DB.R16.12.a");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }

    private static final String[] PLACES = { "Augsburg (Bayern)", "Augsburg" };

    @Override
    protected String[] splitStationName(final String name) {
        for (final String place : PLACES) {
            if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
            if (name.endsWith(", " + place))
                return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
        }
        return super.splitStationName(name);
    }

    @Override
    protected String[] splitPOI(final String poi) {
        for (final String place : PLACES) {
            if (poi.startsWith(place + ", "))
                return new String[] { place, poi.substring(place.length() + 2) };
        }
        return super.splitPOI(poi);
    }

    @Override
    protected String[] splitAddress(final String address) {
        for (final String place : PLACES) {
            if (address.startsWith(place + ", "))
                return new String[] { place, address.substring(place.length() + 2) };
        }
        return super.splitAddress(address);
    }
}
