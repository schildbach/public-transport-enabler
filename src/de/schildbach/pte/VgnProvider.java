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

import java.util.Date;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class VgnProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://efa.vgn.de/vgn/");
    private static final String DEPARTURE_MONITOR_ENDPOINT = "XML_DM_REQUEST";
    private static final String TRIP_ENDPOINT = "XML_TRIP_REQUEST2";

    public VgnProvider() {
        this(API_BASE);
    }

    public VgnProvider(final HttpUrl apiBase) {
        super(NetworkId.VGN, apiBase, DEPARTURE_MONITOR_ENDPOINT, TRIP_ENDPOINT, null, null);
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("R5(z)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "R5(z)");
            if ("R7(z)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "R7(z)");
            if ("R8(z)".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "R8(z)");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date date, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, date, dep, options);
        url.addEncodedQueryParameter("itdLPxx_showTariffLevel", "1");
    }
}
