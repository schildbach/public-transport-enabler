/*
 * Copyright 2018 Erik Uhlmann.
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

import okhttp3.HttpUrl;

/**
 * @author Erik Uhlmann
 */
public class MassachusettsProvider extends AbstractNavitiaProvider {
    private static final String API_REGION = "us-ma";

    public MassachusettsProvider(HttpUrl apiBase, String authorization) {
        super(NetworkId.MASSACHUSETTS, apiBase, authorization);
        setTimeZone("America/New_York");
    }

    public MassachusettsProvider(final String authorization) {
        super(NetworkId.MASSACHUSETTS, authorization);
        setTimeZone("America/New_York");
    }

    @Override
    protected String region() {
        return API_REGION;
    }
}
