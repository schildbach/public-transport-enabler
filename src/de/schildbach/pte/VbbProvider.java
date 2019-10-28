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

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * Provider implementation for the Verkehrsverbund Berlin-Brandenburg (Brandenburg, Germany).
 * 
 * @author Andreas Schildbach
 */
public class VbbProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://fahrinfo.vbb.de/bin/");
    private static final Product[] PRODUCTS_MAP = { Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS,
            Product.FERRY, Product.HIGH_SPEED_TRAIN, Product.REGIONAL_TRAIN, null, null, Product.BUS /* BEV */ };
    private static final Set<Product> ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND = EnumSet
            .complementOf(EnumSet.of(Product.HIGH_SPEED_TRAIN, Product.ON_DEMAND));
    private static final String DEFAULT_API_CLIENT = "{\"id\":\"VBB\",\"type\":\"AND\"}";

    public VbbProvider(final String apiAuthorization, final byte[] salt) {
        this(DEFAULT_API_CLIENT, apiAuthorization, salt);
    }

    public VbbProvider(final String apiClient, final String apiAuthorization, final byte[] salt) {
        super(NetworkId.VBB, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.18");
        setApiExt("VBB.4");
        setApiClient(apiClient);
        setApiAuthorization(apiAuthorization);
        setRequestMicMacSalt(salt);
    }

    private static final Pattern P_SPLIT_NAME_SU = Pattern.compile("(.*?)(?:\\s+\\((S|U|S\\+U)\\))?");
    private static final Pattern P_SPLIT_NAME_BUS = Pattern.compile("(.*?)(\\s+\\[[^\\]]+\\])?");

    @Override
    protected String[] splitStationName(String name) {
        final Matcher mSu = P_SPLIT_NAME_SU.matcher(name);
        if (!mSu.matches())
            throw new IllegalStateException(name);
        name = mSu.group(1);
        final String su = mSu.group(2);

        final Matcher mBus = P_SPLIT_NAME_BUS.matcher(name);
        if (!mBus.matches())
            throw new IllegalStateException(name);
        name = mBus.group(1);

        final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
        if (mParen.matches())
            return new String[] { normalizePlace(mParen.group(2)), (su != null ? su + " " : "") + mParen.group(1) };

        final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
        if (mComma.matches())
            return new String[] { normalizePlace(mComma.group(1)), mComma.group(2) };

        return super.splitStationName(name);
    }

    private String normalizePlace(final String place) {
        if ("Bln".equals(place))
            return "Berlin";
        else
            return place;
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
    protected String normalizeFareName(final String fareName) {
        return fareName.replaceAll("Tarifgebiet ", "");
    }

    @Override
    public Set<Product> defaultProducts() {
        return ALL_EXCEPT_HIGHSPEED_AND_ONDEMAND;
    }
}
