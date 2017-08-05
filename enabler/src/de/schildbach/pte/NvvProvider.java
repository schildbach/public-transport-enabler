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

import java.io.IOException;
import java.util.EnumSet;
import java.util.regex.Matcher;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class NvvProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://auskunft.nvv.de/auskunft/bin/jp/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.BUS,
            Product.FERRY, Product.ON_DEMAND, Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN };

    public NvvProvider() {
        super(NetworkId.NVV, API_BASE, "dn", PRODUCTS_MAP);

        setRequestUrlEncoding(Charsets.UTF_8);
        setJsonNearbyLocationsEncoding(Charsets.UTF_8);
        httpClient.setSslAcceptAllHostnames(true);
        httpClient.setTrustAllCertificates(true);
    }

    private static final String[] PLACES = { "Frankfurt (Main)", "Offenbach (Main)", "Mainz", "Wiesbaden", "Marburg",
            "Kassel", "Hanau", "Göttingen", "Darmstadt", "Aschaffenburg", "Berlin", "Fulda" };

    @Override
    protected String[] splitStationName(final String name) {
        if (name.startsWith("F "))
            return new String[] { "Frankfurt", name.substring(2) };
        if (name.startsWith("OF "))
            return new String[] { "Offenbach", name.substring(3) };
        if (name.startsWith("MZ "))
            return new String[] { "Mainz", name.substring(3) };

        for (final String place : PLACES) {
            if (name.startsWith(place + " - "))
                return new String[] { place, name.substring(place.length() + 3) };
            else if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
        }

        return super.splitStationName(name);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };

        return super.splitStationName(address);
    }

    @Override
    public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasLocation()) {
            return nearbyLocationsByCoordinate(types, location.lat, location.lon, maxDistance, maxLocations);
        } else if (location.type == LocationType.STATION && location.hasId()) {
            final HttpUrl.Builder url = stationBoardEndpoint.newBuilder().addPathSegment(apiLanguage);
            url.addQueryParameter("near", "Anzeigen");
            url.addQueryParameter("distance", Integer.toString(maxDistance != 0 ? maxDistance / 1000 : 50));
            url.addQueryParameter("input", normalizeStationId(location.id));
            url.addQueryParameter("L", "vs_rmv");
            return htmlNearbyStations(url.build());
        } else {
            throw new IllegalArgumentException("cannot handle: " + location);
        }
    }

    @Override
    protected void addCustomReplaces(final StringReplaceReader reader) {
        reader.replace("<ul>", " ");
        reader.replace("</ul>", " ");
        reader.replace("<li>", " ");
        reader.replace("</li>", " ");
        reader.replace("Park&Ride", "Park&amp;Ride");
        reader.replace("C&A", "C&amp;A");
    }

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if ("U-BAHN".equals(ucType))
            return Product.SUBWAY;

        if ("AT".equals(ucType)) // Anschluß Sammel Taxi, Anmeldung nicht erforderlich
            return Product.BUS;
        if ("LTAXI".equals(ucType))
            return Product.BUS;

        if ("MOFA".equals(ucType)) // Mobilfalt-Fahrt
            return Product.ON_DEMAND;

        if ("64".equals(ucType))
            return null;
        if ("65".equals(ucType))
            return null;

        return super.normalizeType(type);
    }
}
