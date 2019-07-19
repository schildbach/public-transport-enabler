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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class BsvagProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://bsvg.efa.de/bsvagstd/");

    public BsvagProvider() {
        super(NetworkId.BSVAG, API_BASE);

        setRequestUrlEncoding(Charsets.UTF_8);
        setUseRouteIndexAsTripId(false);
        setStyles(STYLES);
        setSessionCookieName("HASESSIONID");
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        url.addEncodedQueryParameter("inclMOT_11", "on");
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Braunschweig
        STYLES.put("TM1", new Style(Style.parseColor("#62c2a2"), Style.WHITE));
        STYLES.put("TM2", new Style(Style.parseColor("#b35e89"), Style.WHITE));
        STYLES.put("TM3", new Style(Style.parseColor("#f9b5b9"), Style.WHITE));
        STYLES.put("TM4", new Style(Style.parseColor("#811114"), Style.WHITE));
        STYLES.put("TM5", new Style(Style.parseColor("#ffd00b"), Style.WHITE));

        STYLES.put("BM11", new Style(Style.parseColor("#88891e"), Style.WHITE));
        STYLES.put("BM13", new Style(Style.parseColor("#24a06d"), Style.WHITE));
        STYLES.put("BM16", new Style(Style.parseColor("#f8991b"), Style.WHITE));
        STYLES.put("BM19", new Style(Style.parseColor("#2c2768"), Style.WHITE));
        STYLES.put("BM29", new Style(Style.parseColor("#2c2768"), Style.WHITE));

        STYLES.put("B412", new Style(Style.parseColor("#094f34"), Style.WHITE));
        STYLES.put("B414", new Style(Style.parseColor("#00bce4"), Style.WHITE));
        STYLES.put("B415", new Style(Style.parseColor("#b82837"), Style.WHITE));
        STYLES.put("B417", new Style(Style.parseColor("#2a2768"), Style.WHITE));
        STYLES.put("B418", new Style(Style.parseColor("#c12056"), Style.WHITE));
        STYLES.put("B420", new Style(Style.parseColor("#b7d55b"), Style.WHITE));
        STYLES.put("B422", new Style(Style.parseColor("#16bce4"), Style.WHITE));
        STYLES.put("B424", new Style(Style.parseColor("#ffdf65"), Style.WHITE));
        STYLES.put("B427", new Style(Style.parseColor("#b5d55b"), Style.WHITE));
        STYLES.put("B431", new Style(Style.parseColor("#fddb62"), Style.WHITE));
        STYLES.put("B433", new Style(Style.parseColor("#ed0e65"), Style.WHITE));
        STYLES.put("B434", new Style(Style.parseColor("#bf2555"), Style.WHITE));
        STYLES.put("B436", new Style(Style.parseColor("#0080a2"), Style.WHITE));
        STYLES.put("B437", new Style(Style.parseColor("#fdd11a"), Style.WHITE));
        STYLES.put("B442", new Style(Style.parseColor("#cc3f68"), Style.WHITE));
        STYLES.put("B443", new Style(Style.parseColor("#405a80"), Style.WHITE));
        STYLES.put("B445", new Style(Style.parseColor("#3ca14a"), Style.WHITE));
        STYLES.put("B450", new Style(Style.parseColor("#f2635a"), Style.WHITE));
        STYLES.put("B451", new Style(Style.parseColor("#f5791e"), Style.WHITE));
        STYLES.put("B452", new Style(Style.parseColor("#f0a3ca"), Style.WHITE));
        STYLES.put("B455", new Style(Style.parseColor("#395f95"), Style.WHITE));
        STYLES.put("B461", new Style(Style.parseColor("#00b8a0"), Style.WHITE));
        STYLES.put("B464", new Style(Style.parseColor("#00a14b"), Style.WHITE));
        STYLES.put("B465", new Style(Style.parseColor("#77234b"), Style.WHITE));
        STYLES.put("B471", new Style(Style.parseColor("#380559"), Style.WHITE));
        STYLES.put("B480", new Style(Style.parseColor("#2c2768"), Style.WHITE));
        STYLES.put("B481", new Style(Style.parseColor("#007ec1"), Style.WHITE));
        STYLES.put("B484", new Style(Style.parseColor("#dc8998"), Style.WHITE));
        STYLES.put("B485", new Style(Style.parseColor("#ea8d52"), Style.WHITE));
        STYLES.put("B493", new Style(Style.parseColor("#f24825"), Style.WHITE));
        STYLES.put("B560", new Style(Style.parseColor("#9f6fb0"), Style.WHITE));

        // Wolfsburg
        STYLES.put("B201", new Style(Style.parseColor("#f1471c"), Style.WHITE));
        STYLES.put("B202", new Style(Style.parseColor("#127bca"), Style.WHITE));
        STYLES.put("B203", new Style(Style.parseColor("#f35c95"), Style.WHITE));
        STYLES.put("B204", new Style(Style.parseColor("#00a650"), Style.WHITE));
        STYLES.put("B205", new Style(Style.parseColor("#f67c13"), Style.WHITE));
        STYLES.put("B206", new Style(Style.WHITE, Style.parseColor("#00adef"), Style.parseColor("#00adef")));
        STYLES.put("B207", new Style(Style.parseColor("#94d221"), Style.WHITE));
        STYLES.put("B208", new Style(Style.parseColor("#00adef"), Style.WHITE));
        STYLES.put("B209", new Style(Style.parseColor("#bf7f50"), Style.WHITE));
        STYLES.put("B211", new Style(Style.parseColor("#be65ba"), Style.WHITE));
        STYLES.put("B212", new Style(Style.parseColor("#be65ba"), Style.WHITE));
        STYLES.put("B213", new Style(Style.parseColor("#918f90"), Style.WHITE));
        STYLES.put("B218", new Style(Style.parseColor("#a950ae"), Style.WHITE));
        STYLES.put("B219", new Style(Style.parseColor("#bf7f50"), Style.WHITE));
        STYLES.put("B230", new Style(Style.parseColor("#ca93d0"), Style.WHITE));
        STYLES.put("B231", new Style(Style.WHITE, Style.parseColor("#fab20a"), Style.parseColor("#fab20a")));
        STYLES.put("B244", new Style(Style.parseColor("#66cef6"), Style.WHITE));
        STYLES.put("B267", new Style(Style.parseColor("#918f90"), Style.WHITE));
    }
}
