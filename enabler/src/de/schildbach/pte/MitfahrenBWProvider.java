/*
 * Copyright 2010-2018 the original author or authors.
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

import com.google.common.collect.Multimap;

import java.util.Locale;

import de.schildbach.pte.dto.Product;
import okhttp3.HttpUrl;

/**
 * @author Holger Bruch
 * @author Kavitha Ravi
 */
public class MitfahrenBWProvider extends AbstractOpenTripPlannerProvider {

    public MitfahrenBWProvider() {
        super(NetworkId.MFBW, HttpUrl.parse("http://api.mfdz.de/").newBuilder().addPathSegment("otp")
                .build());
        setLocale(Locale.GERMAN);
        setNumTripsRequested(3);
    }

    protected Multimap<Product, String> defaultProductToOTPModeMapping() {
        Multimap<Product, String> productsToModeMap = super.defaultProductToOTPModeMapping();
        // TODO ON_DEMAND is no official OTP Mode yet. Mitfahren-BW supports RIDESHARING
        // (in the sense of CARPOOLING, not Ride-Hailing). Probably it's better to subsume this
        // under mode ON_DEMAND since characteristics as seen from rider are mostly equivalent
        productsToModeMap.put(Product.ON_DEMAND, "RIDESHARING");
        return productsToModeMap;
    }
}