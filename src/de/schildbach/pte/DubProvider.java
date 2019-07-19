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

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class DubProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("http://wojhati.rta.ae/dub/");

    public DubProvider() {
        super(NetworkId.DUB, API_BASE);
        setTimeZone("Asia/Dubai");
        setUseRouteIndexAsTripId(false);
        setFareCorrectionFactor(0.01f);
        setSessionCookieName("jp-rta-ae-20480");
    }
}
