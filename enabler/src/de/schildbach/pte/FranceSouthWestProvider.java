/*
 * Copyright 2015 the original author or authors.
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

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

import okhttp3.HttpUrl;

/**
 * @author Nicolas Derive
 */
public class FranceSouthWestProvider extends AbstractNavitiaProvider {
    private static String API_REGION = "fr-sw";

    public FranceSouthWestProvider(final HttpUrl apiBase, final String authorization) {
        super(NetworkId.FRANCESOUTHWEST, apiBase, authorization);

        setTimeZone("Europe/Paris");
    }

    public FranceSouthWestProvider(final String authorization) {
        super(NetworkId.FRANCESOUTHWEST, authorization);

        setTimeZone("Europe/Paris");
    }

    @Override
    public String region() {
        return API_REGION;
    }
}
