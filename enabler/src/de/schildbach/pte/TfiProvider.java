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

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class TfiProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.journeyplanner.transportforireland.ie/nta/");
    // https://www.journeyplanner.transportforireland.ie/ultraLite/

    public TfiProvider() {
        super(NetworkId.TFI, API_BASE);
        setLanguage("en");
        setTimeZone("Europe/London");
        setSessionCookieName("NSC_ttm_kqmboofs-usbotqpsu");
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("DART".equals(name))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "DART");
            if ("Rail".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "Rail" + Strings.nullToEmpty(trainNum));
            if ("Train".equals(name) && "Train".equals(symbol))
                return new Line(id, network, null, "Train");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }
}
