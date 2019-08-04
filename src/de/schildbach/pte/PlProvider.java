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
import de.schildbach.pte.util.StringReplaceReader;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("http://mobil.rozklad-pkp.pl/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, // High speed trains from other
                                                                              // countries
            Product.HIGH_SPEED_TRAIN, // EIP, EIC, EC and international equivalents
            Product.HIGH_SPEED_TRAIN, // IC, TLK, IR and international equivalents
            Product.REGIONAL_TRAIN, // R (Regio), Os (Osobowy) and other regional and suburban trains
            Product.BUS, Product.BUS, Product.FERRY };

    public PlProvider() {
        super(NetworkId.PL, API_BASE, "pn", PRODUCTS_MAP);
        setRequestUrlEncoding(Charsets.UTF_8);
        setJsonNearbyLocationsEncoding(Charsets.UTF_8);
        setSessionCookieName("SERVERID");
    }

    private static final String[] PLACES = { "Warszawa", "Kraków" };

    @Override
    protected String[] splitStationName(final String name) {
        for (final String place : PLACES) {
            if (name.endsWith(", " + place))
                return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
            if (name.startsWith(place + " ") || name.startsWith(place + "-"))
                return new String[] { place, name.substring(place.length() + 1) };
        }

        return super.splitStationName(name);
    }

    @Override
    protected void addCustomReplaces(final StringReplaceReader reader) {
        reader.replace("dir=\"Sp ", " "); // Poland
        reader.replace("dir=\"B ", " "); // Poland
        reader.replace("dir=\"K ", " "); // Poland
        reader.replace("dir=\"Eutingen i. G ", "dir=\"Eutingen\" "); // Poland
        reader.replace("StargetLoc", "Süd\" targetLoc"); // Poland
        reader.replace("platform=\"K ", " "); // Poland
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if ("AR".equals(ucType)) // Arriva Polaczen
            return Product.REGIONAL_TRAIN;
        if ("N".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("REG".equals(ucType))
            return Product.REGIONAL_TRAIN;
        if ("L".equals(ucType) || "LS".equals(ucType)) // Łódzka Kolej Aglomeracyjna
            return Product.REGIONAL_TRAIN;

        if ("SKM".equals(ucType)) // SKM Trojmiasto in Gdansk Metropolitan Area
            return Product.SUBURBAN_TRAIN;
        if ("SKW".equals(ucType)) // SKM Warszawa
            return Product.SUBURBAN_TRAIN;
        if ("WKD".equals(ucType)) // Warsaw Commuter Railway (Warszawa Kolej Dojazdowa)
            return Product.SUBURBAN_TRAIN;

        if ("IRB".equals(ucType)) // interREGIO Bus
            return Product.BUS;
        if ("ZKA".equals(ucType)) // Zastępcza Komunikacja Autobusowa (Schienenersatzverkehr)
            return Product.BUS;

        if ("FRE".equals(ucType))
            return Product.FERRY;

        return super.normalizeType(type);
    }
}
