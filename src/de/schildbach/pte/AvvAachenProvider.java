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
 * Provider implementation for the Aachener Verkehrsverbund (Aachen, Germany).
 * 
 * @author Andreas Schildbach
 */
public class AvvAachenProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://auskunft.avv.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.REGIONAL_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.BUS, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS,
            Product.BUS, Product.ON_DEMAND, Product.FERRY };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"AVV_AACHEN\",\"type\":\"WEB\"}";

    public AvvAachenProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public AvvAachenProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.AVV_AACHEN, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.16");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
    }

    private static final String[] PLACES = { "AC", "Aachen" };

    @Override
    protected String[] splitStationName(final String name) {
        // Some stations in Aachen city have 3 names: "Aachen, station name", "station name, AC" and "AC, station name".
        // Some (other) stations has 2 variants: "Aachen, station name" and "station name, Aachen"
        // If you type the station name first, you get the variant "station name, AC" resp. "station name, Aachen",
        // which would be parsed as a station AC resp. Aachen in "station name".
        for (final String place: PLACES) {
            if (name.endsWith(", " + place)) {
                return new String[] { "Aachen" , name.substring(0, name.length() - place.length() - 2) };
            }
        }
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
        if (m.matches()) {
            // also remove the abbreviating variant, just for consistency
            if (m.group(1).equals("AC"))
                return new String[] { "Aachen", m.group(2) };
            return new String[] { m.group(1), m.group(2) };
        }
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
}
