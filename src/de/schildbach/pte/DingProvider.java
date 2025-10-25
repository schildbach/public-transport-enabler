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

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import okhttp3.HttpUrl;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * @author Andreas Schildbach
 */
public class DingProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.ding.eu/ding3/");
    // https://www.ding.eu/swu/

    public DingProvider() {
        super(NetworkId.DING, API_BASE);
        setRequestUrlEncoding(StandardCharsets.UTF_8);
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
                             final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
                             final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if (trainType == null && "RS 7".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RS7");
            if (trainType == null && "RS 71".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RS71");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }
}
