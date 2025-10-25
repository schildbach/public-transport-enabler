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

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class VmvProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://fahrplanauskunft-mv.de/vmv-efa/");

    public VmvProvider() {
        super(NetworkId.VMV, API_BASE);
        setIncludeRegionId(false);
        setUseRouteIndexAsTripId(false);
        setRequestUrlEncoding(StandardCharsets.UTF_8);
        setSessionCookieName("EFABWLB");
        httpClient.setTrustAllCertificates(true);
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        url.addEncodedQueryParameter("inclMOT_11", "on");
    }
}
