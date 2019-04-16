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

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class VmsProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.vms.de/vms2/");

    public VmsProvider() {
        this(API_BASE);
    }

    public VmsProvider(final HttpUrl apiBase) {
        super(NetworkId.VMS, apiBase);
        setRequestUrlEncoding(Charsets.UTF_8);
        setUseLineRestriction(false);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        url.addEncodedQueryParameter("inclMOT_11", "on");
        url.addEncodedQueryParameter("inclMOT_13", "on");
        url.addEncodedQueryParameter("inclMOT_14", "on");
        url.addEncodedQueryParameter("inclMOT_15", "on");
        url.addEncodedQueryParameter("inclMOT_16", "on");
        url.addEncodedQueryParameter("inclMOT_17", "on");
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("Ilztalbahn".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "ITB");
            if ("Meridian".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "M");
            if ("CityBahn".equals(trainName) && trainNum == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "CB");
            if ("CityBahn".equals(longName) && "C11".equals(symbol))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("Zug".equals(longName)
                    && ("C11".equals(symbol) || "C13".equals(symbol) || "C14".equals(symbol) || "C15".equals(symbol)))
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);

            if ("RE 3".equals(symbol) && "Zug".equals(longName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "RE3");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }
}
