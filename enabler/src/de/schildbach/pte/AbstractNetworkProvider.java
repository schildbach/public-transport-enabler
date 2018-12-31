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
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.util.HttpClient;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractNetworkProvider implements NetworkProvider {
    protected final NetworkId network;
    protected final HttpClient httpClient = new HttpClient();

    protected Charset requestUrlEncoding = Charsets.ISO_8859_1;
    protected TimeZone timeZone = TimeZone.getTimeZone("CET");
    protected int numTripsRequested = 6;
    private @Nullable Map<String, Style> styles = null;

    protected static final Set<Product> ALL_EXCEPT_HIGHSPEED = EnumSet
            .complementOf(EnumSet.of(Product.HIGH_SPEED_TRAIN));

    protected AbstractNetworkProvider(final NetworkId network) {
        this.network = network;
    }

    @Override
    public final NetworkId id() {
        return network;
    }

    @Override
    public final boolean hasCapabilities(final Capability... capabilities) {
        for (final Capability capability : capabilities)
            if (!hasCapability(capability))
                return false;

        return true;
    }

    protected abstract boolean hasCapability(Capability capability);

    @Deprecated
    @Override
    public final SuggestLocationsResult suggestLocations(final CharSequence constraint) throws IOException {
        return suggestLocations(constraint, null, 0);
    }

    @Deprecated
    @Override
    public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
            @Nullable Accessibility accessibility, @Nullable Set<TripFlag> flags) throws IOException {
        return queryTrips(from, via, to, date, dep,
                new TripOptions(products, optimize, walkSpeed, accessibility, flags));
    }

    @Override
    public Set<Product> defaultProducts() {
        return ALL_EXCEPT_HIGHSPEED;
    }

    public AbstractNetworkProvider setUserAgent(final String userAgent) {
        httpClient.setUserAgent(userAgent);
        return this;
    }

    public AbstractNetworkProvider setProxy(final Proxy proxy) {
        httpClient.setProxy(proxy);
        return this;
    }

    public AbstractNetworkProvider setTrustAllCertificates(final boolean trustAllCertificates) {
        httpClient.setTrustAllCertificates(trustAllCertificates);
        return this;
    }

    protected AbstractNetworkProvider setRequestUrlEncoding(final Charset requestUrlEncoding) {
        this.requestUrlEncoding = requestUrlEncoding;
        return this;
    }

    protected AbstractNetworkProvider setTimeZone(final String timeZoneId) {
        this.timeZone = TimeZone.getTimeZone(timeZoneId);
        return this;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    protected AbstractNetworkProvider setNumTripsRequested(final int numTripsRequested) {
        this.numTripsRequested = numTripsRequested;
        return this;
    }

    protected AbstractNetworkProvider setStyles(final Map<String, Style> styles) {
        this.styles = styles;
        return this;
    }

    protected AbstractNetworkProvider setSessionCookieName(final String sessionCookieName) {
        httpClient.setSessionCookieName(sessionCookieName);
        return this;
    }

    private static final char STYLES_SEP = '|';

    @Override
    public Style lineStyle(final @Nullable String network, final @Nullable Product product,
            final @Nullable String label) {
        final Map<String, Style> styles = this.styles;
        if (styles != null && product != null) {
            if (network != null) {
                // check for line match
                final Style lineStyle = styles.get(network + STYLES_SEP + product.code + Strings.nullToEmpty(label));
                if (lineStyle != null)
                    return lineStyle;

                // check for product match
                final Style productStyle = styles.get(network + STYLES_SEP + product.code);
                if (productStyle != null)
                    return productStyle;

                // check for night bus, as that's a common special case
                if (product == Product.BUS && label != null && label.startsWith("N")) {
                    final Style nightStyle = styles.get(network + STYLES_SEP + "BN");
                    if (nightStyle != null)
                        return nightStyle;
                }
            }

            // check for line match
            final String string = product.code + Strings.nullToEmpty(label);
            final Style lineStyle = styles.get(string);
            if (lineStyle != null)
                return lineStyle;

            // check for product match
            final Style productStyle = styles.get(Character.toString(product.code));
            if (productStyle != null)
                return productStyle;

            // check for night bus, as that's a common special case
            if (product == Product.BUS && label != null && label.startsWith("N")) {
                final Style nightStyle = styles.get("BN");
                if (nightStyle != null)
                    return nightStyle;
            }
        }

        // standard colors
        return Standard.STYLES.get(product);
    }

    @Override
    public Point[] getArea() throws IOException {
        return null;
    }

    protected static String normalizeStationId(final String stationId) {
        if (stationId == null || stationId.length() == 0)
            return null;

        if (stationId.charAt(0) != '0')
            return stationId;

        final StringBuilder normalized = new StringBuilder(stationId);
        while (normalized.length() > 0 && normalized.charAt(0) == '0')
            normalized.deleteCharAt(0);

        return normalized.toString();
    }

    private static final Pattern P_NAME_SECTION = Pattern.compile("(\\d{1,5})\\s*" + //
            "([A-Z](?:\\s*-?\\s*[A-Z])?)?", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_NAME_NOSW = Pattern.compile("(\\d{1,5})\\s*" + //
            "(Nord|Süd|Ost|West)", Pattern.CASE_INSENSITIVE);

    protected Position parsePosition(final String position) {
        if (position == null)
            return null;

        final Matcher mSection = P_NAME_SECTION.matcher(position);
        if (mSection.matches()) {
            final String name = Integer.toString(Integer.parseInt(mSection.group(1)));
            if (mSection.group(2) != null)
                return new Position(name, mSection.group(2).replaceAll("\\s+", ""));
            else
                return new Position(name);
        }

        final Matcher mNosw = P_NAME_NOSW.matcher(position);
        if (mNosw.matches())
            return new Position(Integer.toString(Integer.parseInt(mNosw.group(1))), mNosw.group(2).substring(0, 1));

        return new Position(position);
    }
}
