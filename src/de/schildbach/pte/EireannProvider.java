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

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * Ireland, Dublin
 * 
 * @author Andreas Schildbach
 */
public class EireannProvider extends AbstractHafasLegacyProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("http://journeyplanner.buseireann.ie/jp/bin/");
    private static final Product[] PRODUCTS_MAP = { null, null, null, Product.BUS };

    public EireannProvider() {
        super(NetworkId.EIREANN, API_BASE, "en", PRODUCTS_MAP);

        setStationBoardHasStationTable(false);
    }

    @Override
    public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to,
            final Date date, final boolean dep, final @Nullable TripOptions options) throws IOException {
        return queryTripsXml(from, via, to, date, dep, options);
    }

    @Override
    public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException {
        return queryMoreTripsXml(context, later);
    }

    private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([^#]+)#");

    @Override
    protected Line parseLineAndType(final String lineAndType) {
        final Matcher mLine = P_NORMALIZE_LINE.matcher(lineAndType);
        if (mLine.matches())
            return newLine(Product.BUS, mLine.group(1), null);

        return super.parseLineAndType(lineAndType);
    }

    @Override
    protected Product normalizeType(final String type) {
        final String ucType = type.toUpperCase();

        if ("COA".equals(ucType))
            return Product.BUS;
        if ("CIT".equals(ucType))
            return Product.BUS;

        // skip parsing of "common" lines
        throw new IllegalStateException("cannot normalize type '" + type + "'");
    }
}
