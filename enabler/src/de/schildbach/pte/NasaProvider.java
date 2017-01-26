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
public class NasaProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://reiseauskunft.insa.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.TRAM, Product.BUS,
            Product.ON_DEMAND };

    public NasaProvider() {
        super(NetworkId.NASA, API_BASE, "dn", PRODUCTS_MAP);

        setRequestUrlEncoding(Charsets.UTF_8);
        setJsonNearbyLocationsEncoding(Charsets.UTF_8);
        setStationBoardHasLocation(true);
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
    public NearbyLocationsResult queryNearbyLocations(final EnumSet<LocationType> types, final Location location,
            final int maxDistance, final int maxLocations) throws IOException {
        if (location.hasLocation()) {
            return nearbyLocationsByCoordinate(types, location.lat, location.lon, maxDistance, maxLocations);
        } else if (location.type == LocationType.STATION && location.hasId()) {
            final HttpUrl.Builder url = stationBoardEndpoint.newBuilder().addPathSegment(apiLanguage);
            url.addQueryParameter("near", "Anzeigen");
            url.addQueryParameter("distance", Integer.toString(maxDistance != 0 ? maxDistance / 1000 : 50));
            url.addQueryParameter("input", normalizeStationId(location.id));
            return htmlNearbyStations(url.build());
        } else {
            throw new IllegalArgumentException("cannot handle: " + location);
        }
    }

    @Override
    protected void addCustomReplaces(final StringReplaceReader reader) {
        reader.replace("\"Florian Geyer\"", "Florian Geyer");
    }

    @Override
    protected Product normalizeType(String type) {
        final String ucType = type.toUpperCase();

        if ("ECW".equals(ucType))
            return Product.HIGH_SPEED_TRAIN;
        if ("IXB".equals(ucType)) // ICE International
            return Product.HIGH_SPEED_TRAIN;
        if ("RRT".equals(ucType))
            return Product.HIGH_SPEED_TRAIN;

        if ("DPF".equals(ucType)) // mit Dampflok bespannter Zug
            return Product.REGIONAL_TRAIN;
        if ("DAM".equals(ucType)) // Harzer Schmalspurbahnen: mit Dampflok bespannter Zug
            return Product.REGIONAL_TRAIN;
        if ("TW".equals(ucType)) // Harzer Schmalspurbahnen: Triebwagen
            return Product.REGIONAL_TRAIN;
        if ("RR".equals(ucType)) // Polen
            return Product.REGIONAL_TRAIN;
        if ("BAHN".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("ZUGBAHN".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("DAMPFZUG".equals(ucType))
            return Product.REGIONAL_TRAIN;

        if ("DPS".equals(ucType))
            return Product.SUBURBAN_TRAIN;

        if ("RUFBUS".equals(ucType)) // Rufbus
            return Product.BUS;
        if ("RBS".equals(ucType)) // Rufbus
            return Product.BUS;

        return super.normalizeType(type);
    }
}
