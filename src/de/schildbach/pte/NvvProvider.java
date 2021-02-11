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
import de.schildbach.pte.dto.Style;
import okhttp3.HttpUrl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Provider implementation for the Nordhessischer Verkehrsverbund (North Hesse, Germany).
 * 
 * @author Andreas Schildbach
 */
public class NvvProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://auskunft.nvv.de/auskunft/bin/app/");
    private static final Product[] PRODUCTS_MAP = { Product.HIGH_SPEED_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.REGIONAL_TRAIN, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS, Product.BUS,
            Product.FERRY, Product.ON_DEMAND, Product.REGIONAL_TRAIN, Product.REGIONAL_TRAIN };
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"NVV\",\"type\":\"AND\"}";

    public NvvProvider(final String apiAuthorization) {
        this(DEFAULT_API_CLIENT, apiAuthorization);
    }

    public NvvProvider(final String apiClient, final String apiAuthorization) {
        super(NetworkId.NVV, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiExt("NVV.6.0");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setStyles(STYLES);
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

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS1", new Style(Style.parseColor("#009edd"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS2", new Style(Style.parseColor("#ff2e17"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS3", new Style(Style.parseColor("#00b098"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS4", new Style(Style.parseColor("#ffc734"), Style.parseColor("#2c2e35"), Style.parseColor("#2c2e35")));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS5", new Style(Style.parseColor("#95542a"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS6", new Style(Style.parseColor("#ff7322"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS7", new Style(Style.parseColor("#214d36"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS8", new Style(Style.parseColor("#88c946"), Style.WHITE));
        STYLES.put("DB Regio AG S-Bahn Rhein-Main|SS9", new Style(Style.parseColor("#872996"), Style.WHITE));

        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU1", new Style(Style.parseColor("#c52b1e"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU2", new Style(Style.parseColor("#00ab4f"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU3", new Style(Style.parseColor("#345aaf"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU4", new Style(Style.parseColor("#fc5cac"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU5", new Style(Style.parseColor("#0c7d3e"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU6", new Style(Style.parseColor("#0082ca"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU7", new Style(Style.parseColor("#f19e2d"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU8", new Style(Style.parseColor("#ca7fbe"), Style.WHITE));
        STYLES.put("Stadtwerke Verkehrsgesellschaft Frankfurt|UU9", new Style(Style.parseColor("#f4d039"), Style.parseColor("#2c2e35"), Style.parseColor("#2c2e35")));
    }
}
