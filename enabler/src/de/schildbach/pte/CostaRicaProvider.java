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

import okhttp3.HttpUrl;

/**
 * @author Jaime Gutiérrez Alfaro
 */
public class CostaRicaProvider extends AbstractNavitiaProvider {

    private static String API_REGION = "default";
    private static HttpUrl API_BASE = HttpUrl.parse("http://98.158.179.5:9191/").newBuilder().addPathSegment(SERVER_VERSION).build();


    public CostaRicaProvider(final HttpUrl apiBase, final String authorization) {
        super(NetworkId.CR, apiBase, authorization);

        setTimeZone("America/Costa_Rica");
    }

    public CostaRicaProvider(final String authorization) {
        super(NetworkId.CR, API_BASE, authorization);

        setTimeZone("America/Costa_Rica");
    }

    @Override
    public String region() {
        return API_REGION;
    }
}
