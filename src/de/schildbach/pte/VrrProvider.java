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

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.dto.TripOptions;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class VrrProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://efa.vrr.de/standard/");
    // http://app.vrr.de/companion-vrr/

    public VrrProvider() {
        this(API_BASE);
    }

    public VrrProvider(final HttpUrl apiBase) {
        super(NetworkId.VRR, apiBase);
        setIncludeRegionId(false);
        setUseProxFootSearch(false);
        setNeedsSpEncId(true);
        setUseRouteIndexAsTripId(false);
        setStyles(STYLES);
        setRequestUrlEncoding(Charsets.UTF_8);
        setSessionCookieName("vrr-ef-lb");
    }

    @Override
    protected void appendTripRequestParameters(final HttpUrl.Builder url, final Location from,
            final @Nullable Location via, final Location to, final Date time, final boolean dep,
            final @Nullable TripOptions options) {
        super.appendTripRequestParameters(url, from, via, to, time, dep, options);
        if (options != null && options.products != null) {
            for (final Product p : options.products) {
                if (p == Product.CABLECAR)
                    url.addEncodedQueryParameter("inclMOT_11", "on"); // Schwebebahn
            }
        }
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("Regionalbahn".equals(trainName) && symbol != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
            if ("NordWestBahn".equals(trainName) && symbol != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, symbol);

            if (trainType == null && "SEV7".equals(trainNum))
                return new Line(id, network, Product.BUS, trainNum);
            if (trainType == null && "3SEV".equals(trainNum))
                return new Line(id, network, Product.BUS, trainNum);
        } else if ("11".equals(mot)) {
            // Wuppertaler Schwebebahn & SkyTrain D'dorf
            if ("Schwebebahn".equals(trainName) || (longName != null && longName.startsWith("Schwebebahn")))
                return new Line(id, network, Product.CABLECAR, name);

            // H-Bahn TU Dortmund
            if ("H-Bahn".equals(trainName) || (longName != null && longName.startsWith("H-Bahn")))
                return new Line(id, network, Product.CABLECAR, name);
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Schnellbusse VRR
        STYLES.put("vrr|BSB", new Style(Style.parseColor("#00919d"), Style.WHITE));

        // Dortmund
        STYLES.put("dsw|UU41", new Style(Shape.RECT, Style.parseColor("#ffe700"), Style.GRAY));
        STYLES.put("dsw|UU42", new Style(Shape.RECT, Style.parseColor("#fcb913"), Style.WHITE));
        STYLES.put("dsw|UU43", new Style(Shape.RECT, Style.parseColor("#409387"), Style.WHITE));
        STYLES.put("dsw|UU44", new Style(Shape.RECT, Style.parseColor("#66a3b1"), Style.WHITE));
        STYLES.put("dsw|UU45", new Style(Shape.RECT, Style.parseColor("#ee1c23"), Style.WHITE));
        STYLES.put("dsw|UU46", new Style(Shape.RECT, Style.parseColor("#756fb3"), Style.WHITE));
        STYLES.put("dsw|UU47", new Style(Shape.RECT, Style.parseColor("#8dc63e"), Style.WHITE));
        STYLES.put("dsw|UU49", new Style(Shape.RECT, Style.parseColor("#f7acbc"), Style.WHITE));
        STYLES.put("dsw|BNE", new Style(Shape.RECT, Style.parseColor("#2e2382"), Style.WHITE));

        // Düsseldorf
        STYLES.put("rbg|UU70", new Style(Shape.RECT, Style.parseColor("#69b0cd"), Style.WHITE));
        STYLES.put("rbg|UU71", new Style(Shape.RECT, Style.parseColor("#66cef6"), Style.WHITE));
        STYLES.put("rbg|UU72", new Style(Shape.RECT, Style.parseColor("#4cc4c5"), Style.WHITE));
        STYLES.put("rbg|UU73", new Style(Shape.RECT, Style.parseColor("#4763b8"), Style.WHITE));
        STYLES.put("rbg|UU74", new Style(Shape.RECT, Style.parseColor("#27297c"), Style.WHITE));
        STYLES.put("rbg|UU75", new Style(Shape.RECT, Style.parseColor("#079acb"), Style.WHITE));
        STYLES.put("rbg|UU76", new Style(Shape.RECT, Style.parseColor("#1969bc"), Style.WHITE));
        STYLES.put("rbg|UU77", new Style(Shape.RECT, Style.parseColor("#6d90d2"), Style.WHITE));
        STYLES.put("rbg|UU78", new Style(Shape.RECT, Style.parseColor("#02a7eb"), Style.WHITE));
        STYLES.put("rbg|UU79", new Style(Shape.RECT, Style.parseColor("#00aaa0"), Style.WHITE));
        STYLES.put("rbg|UU83", new Style(Shape.RECT, Style.parseColor("#2743a0"), Style.WHITE));
        STYLES.put("rbg|T701", new Style(Shape.RECT, Style.parseColor("#f57215"), Style.WHITE));
        STYLES.put("rbg|T704", new Style(Shape.RECT, Style.parseColor("#c01c23"), Style.WHITE));
        STYLES.put("rbg|T705", new Style(Shape.RECT, Style.parseColor("#bd0c8e"), Style.WHITE));
        STYLES.put("rbg|T706", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("rbg|T707", new Style(Shape.RECT, Style.parseColor("#72177a"), Style.WHITE));
        STYLES.put("rbg|T708", new Style(Shape.RECT, Style.parseColor("#f680b4"), Style.WHITE));
        STYLES.put("rbg|T709", new Style(Shape.RECT, Style.parseColor("#ef269d"), Style.WHITE));
        STYLES.put("rbg|BNE1", new Style(Shape.RECT, Style.parseColor("#fec210"), Style.BLACK));
        STYLES.put("rbg|BNE2", new Style(Shape.RECT, Style.parseColor("#f17cb0"), Style.WHITE));
        STYLES.put("rbg|BNE3", new Style(Shape.RECT, Style.parseColor("#99ca3b"), Style.WHITE));
        STYLES.put("rbg|BNE4", new Style(Shape.RECT, Style.parseColor("#ee1d23"), Style.WHITE));
        STYLES.put("rbg|BNE5", new Style(Shape.RECT, Style.parseColor("#90268f"), Style.WHITE));
        STYLES.put("rbg|BNE6", new Style(Shape.RECT, Style.parseColor("#f47921"), Style.WHITE));
        STYLES.put("rbg|BNE7", new Style(Shape.RECT, Style.parseColor("#009247"), Style.WHITE));
        STYLES.put("rbg|BNE8", new Style(Shape.RECT, Style.parseColor("#bdaa8b"), Style.BLACK));
        STYLES.put("rbg|BM1", new Style(Shape.RECT, Style.parseColor("#31b759"), Style.WHITE));
        STYLES.put("rbg|BM2", new Style(Shape.RECT, Style.parseColor("#31b759"), Style.WHITE));
        STYLES.put("rbg|BM3", new Style(Shape.RECT, Style.parseColor("#31b759"), Style.WHITE));

        // Krefeld
        STYLES.put("swk|T041", new Style(Style.parseColor("#ff432f"), Style.WHITE));
        STYLES.put("swk|T042", new Style(Style.parseColor("#ff9296"), Style.BLACK));
        STYLES.put("swk|T043", new Style(Style.parseColor("#bd6eb6"), Style.BLACK));
        STYLES.put("swk|T044", new Style(Style.parseColor("#ff6720"), Style.BLACK));
        STYLES.put("swk|B045", new Style(Style.parseColor("#0bc1ea"), Style.BLACK));
        STYLES.put("swk|B046", new Style(Style.parseColor("#665074"), Style.WHITE));
        STYLES.put("swk|B047", new Style(Style.parseColor("#ffcf9c"), Style.BLACK));
        STYLES.put("swk|B049", new Style(Style.parseColor("#2759af"), Style.WHITE));
        STYLES.put("swk|B051", new Style(Style.parseColor("#9ed269"), Style.BLACK));
        STYLES.put("swk|B052", new Style(Style.parseColor("#ff8b30"), Style.BLACK));
        STYLES.put("swk|B054", new Style(Style.parseColor("#128943"), Style.WHITE));
        STYLES.put("swk|B055", new Style(Style.parseColor("#13bcbc"), Style.BLACK));
        STYLES.put("swk|B056", new Style(Style.parseColor("#9b66a4"), Style.WHITE));
        STYLES.put("swk|B057", new Style(Style.parseColor("#4bcfea"), Style.BLACK));
        STYLES.put("swk|B058", new Style(Style.parseColor("#0086cb"), Style.WHITE));
        STYLES.put("swk|B059", new Style(Style.parseColor("#97d69c"), Style.BLACK));
        STYLES.put("swk|B060", new Style(Style.parseColor("#a6cbc3"), Style.BLACK));
        STYLES.put("swk|B061", new Style(Style.parseColor("#d98a2b"), Style.BLACK));
        STYLES.put("swk|B062", new Style(Style.parseColor("#b67341"), Style.WHITE));
        STYLES.put("swk|B068", new Style(Style.parseColor("#0f52ac"), Style.WHITE));
        STYLES.put("swk|B069", new Style(Style.parseColor("#d7745e"), Style.WHITE));
        STYLES.put("rvn|B076", new Style(Style.parseColor("#4da543"), Style.WHITE));
        STYLES.put("rvn|B077", new Style(Style.parseColor("#ffea40"), Style.BLACK));
        STYLES.put("rvn|B079", new Style(Style.parseColor("#91a8a7"), Style.BLACK));
        STYLES.put("swk|BNE5", new Style(Style.parseColor("#9ed269"), Style.BLACK));
        STYLES.put("swk|BNE6", new Style(Style.parseColor("#ff8b30"), Style.BLACK));
        STYLES.put("swk|BNE7", new Style(Style.parseColor("#4bcfea"), Style.BLACK));
        STYLES.put("swk|BNE8", new Style(Style.parseColor("#0086cb"), Style.WHITE));
        STYLES.put("swk|BNE10", new Style(Style.parseColor("#13bcbc"), Style.BLACK));
        STYLES.put("swk|BNE27", new Style(Style.parseColor("#128943"), Style.WHITE));

        // Essen
        STYLES.put("eva|UU17", new Style(Shape.RECT, Style.parseColor("#68b6e3"), Style.WHITE));
        STYLES.put("eva|T101", new Style(Shape.RECT, Style.parseColor("#986b17"), Style.WHITE));
        STYLES.put("eva|T103", new Style(Shape.RECT, Style.parseColor("#ffcc00"), Style.WHITE));
        STYLES.put("eva|T105", new Style(Shape.RECT, Style.parseColor("#b6cd00"), Style.WHITE));
        STYLES.put("eva|T106", new Style(Shape.RECT, Style.parseColor("#a695ba"), Style.WHITE));
        STYLES.put("eva|T108", new Style(Shape.RECT, Style.parseColor("#eca900"), Style.WHITE));
        STYLES.put("eva|T109", new Style(Shape.RECT, Style.parseColor("#00933a"), Style.WHITE));
        STYLES.put("eva|BNE1", new Style(Shape.RECT, Style.parseColor("#f7a500"), Style.WHITE));
        STYLES.put("eva|BNE2", new Style(Shape.RECT, Style.parseColor("#009dcc"), Style.WHITE));
        STYLES.put("eva|BNE3", new Style(Shape.RECT, Style.parseColor("#534395"), Style.WHITE));
        STYLES.put("eva|BNE4", new Style(Shape.RECT, Style.parseColor("#f29ec4"), Style.WHITE));
        STYLES.put("eva|BNE5", new Style(Shape.RECT, Style.parseColor("#00964e"), Style.WHITE));
        STYLES.put("eva|BNE6", new Style(Shape.RECT, Style.parseColor("#e5007c"), Style.WHITE));
        STYLES.put("eva|BNE7", new Style(Shape.RECT, Style.parseColor("#6e9ed4"), Style.WHITE));
        STYLES.put("eva|BNE8", new Style(Shape.RECT, Style.parseColor("#877bb0"), Style.WHITE));
        STYLES.put("eva|BNE9", new Style(Shape.RECT, Style.parseColor("#ed6da6"), Style.WHITE));
        STYLES.put("eva|BNE10", new Style(Shape.RECT, Style.parseColor("#ab901c"), Style.WHITE));
        STYLES.put("eva|BNE11", new Style(Shape.RECT, Style.parseColor("#e3000b"), Style.WHITE));
        STYLES.put("eva|BNE12", new Style(Shape.RECT, Style.parseColor("#92120a"), Style.WHITE));
        STYLES.put("eva|BNE13", new Style(Shape.RECT, Style.parseColor("#ffde0c"), Style.BLACK));
        STYLES.put("eva|BNE14", new Style(Shape.RECT, Style.parseColor("#ee7100"), Style.WHITE));
        STYLES.put("eva|BNE15", new Style(Shape.RECT, Style.parseColor("#94c11a"), Style.WHITE));
        STYLES.put("eva|BNE16", new Style(Shape.RECT, Style.parseColor("#004e9e"), Style.WHITE));

        // Duisburg
        STYLES.put("dvg|B905", new Style(Style.parseColor("#c8242b"), Style.WHITE));
        STYLES.put("dvg|B906", new Style(Style.parseColor("#b5ab3a"), Style.WHITE));
        STYLES.put("dvg|B907", new Style(Style.parseColor("#6891c3"), Style.WHITE));
        STYLES.put("dvg|B909", new Style(Style.parseColor("#217e5b"), Style.WHITE));
        STYLES.put("dvg|B910", new Style(Style.parseColor("#d48018"), Style.WHITE));
        STYLES.put("dvg|B917", new Style(Style.parseColor("#23b14b"), Style.WHITE));
        STYLES.put("dvg|B919", new Style(Style.parseColor("#078b4a"), Style.WHITE));
        STYLES.put("dvg|B922", new Style(Style.parseColor("#0072bb"), Style.WHITE));
        STYLES.put("dvg|B923", new Style(Style.parseColor("#00b1c4"), Style.WHITE));
        STYLES.put("dvg|B924", new Style(Style.parseColor("#f37921"), Style.WHITE));
        STYLES.put("dvg|B925", new Style(Style.parseColor("#4876b8"), Style.WHITE));
        STYLES.put("dvg|B926", new Style(Style.parseColor("#649b43"), Style.WHITE));
        STYLES.put("dvg|B928", new Style(Style.parseColor("#c4428c"), Style.WHITE));
        STYLES.put("dvg|B933", new Style(Style.parseColor("#975615"), Style.WHITE));
        STYLES.put("dvg|B934", new Style(Style.parseColor("#009074"), Style.WHITE));
        STYLES.put("dvg|B937", new Style(Style.parseColor("#6f78b5"), Style.WHITE));
        STYLES.put("dvg|B940", new Style(Style.parseColor("#bbbb30"), Style.WHITE));
        STYLES.put("dvg|B942", new Style(Style.parseColor("#930408"), Style.WHITE));
        STYLES.put("dvg|B944", new Style(Style.parseColor("#c52157"), Style.WHITE));
        STYLES.put("dvg|B946", new Style(Style.parseColor("#1cbddc"), Style.WHITE));
        STYLES.put("dvg|BNE1", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("dvg|BNE2", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("dvg|BNE3", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("dvg|BNE4", new Style(Style.parseColor("#000000"), Style.WHITE));

        // Oberhausen
        STYLES.put("sto|B952", new Style(Style.parseColor("#f59598"), Style.WHITE));
        STYLES.put("sto|B953", new Style(Style.parseColor("#5eb6d9"), Style.WHITE));
        STYLES.put("sto|B954", new Style(Style.parseColor("#f89d3d"), Style.WHITE));
        STYLES.put("sto|B955", new Style(Style.parseColor("#8879b8"), Style.WHITE));
        STYLES.put("sto|B956", new Style(Style.parseColor("#23b24b"), Style.WHITE));
        STYLES.put("sto|B957", new Style(Style.parseColor("#ebc531"), Style.WHITE));
        STYLES.put("sto|B960", new Style(Style.parseColor("#aed57f"), Style.WHITE));
        STYLES.put("sto|B961", new Style(Style.parseColor("#a46f73"), Style.WHITE));
        STYLES.put("sto|B962", new Style(Style.parseColor("#0a776f"), Style.WHITE));
        STYLES.put("sto|B966", new Style(Style.parseColor("#c8b3d6"), Style.WHITE));
        STYLES.put("sto|B976", new Style(Style.parseColor("#d063a5"), Style.WHITE));
        STYLES.put("sto|BNE1", new Style(Style.parseColor("#e22225"), Style.WHITE));
        STYLES.put("sto|BNE2", new Style(Style.parseColor("#28ad78"), Style.WHITE));
        STYLES.put("sto|BNE3", new Style(Style.parseColor("#85499c"), Style.WHITE));
        STYLES.put("sto|BNE4", new Style(Style.parseColor("#395aa8"), Style.WHITE));
        STYLES.put("sto|BNE5", new Style(Style.parseColor("#ede929"), Style.WHITE));
        STYLES.put("sto|BNE6", new Style(Style.parseColor("#d488ba"), Style.WHITE));
        STYLES.put("sto|BNE7", new Style(Style.parseColor("#fbae3e"), Style.WHITE));
        STYLES.put("sto|BNE10", new Style(Style.parseColor("#270039"), Style.WHITE));

        // Mülheim an der Ruhr
        STYLES.put("vrr|T102", new Style(Style.parseColor("#756fb3"), Style.WHITE));
        STYLES.put("vrr|B132", new Style(Style.parseColor("#a3c3d1"), Style.BLACK));
        STYLES.put("vrr|B133", new Style(Style.parseColor("#a9a575"), Style.BLACK));
        STYLES.put("vrr|B134", new Style(Style.parseColor("#806a63"), Style.WHITE));
        STYLES.put("vrr|B135", new Style(Style.parseColor("#425159"), Style.WHITE));

        // Neuss
        STYLES.put("swn|B842", new Style(Style.parseColor("#fdcc10"), Style.WHITE));
        STYLES.put("swn|B843", new Style(Style.parseColor("#808180"), Style.WHITE));
        STYLES.put("swn|B844", new Style(Style.parseColor("#cb1f25"), Style.WHITE));
        STYLES.put("swn|B848", new Style(Style.parseColor("#be4e26"), Style.WHITE));
        STYLES.put("swn|B849", new Style(Style.parseColor("#c878b1"), Style.WHITE));
        STYLES.put("swn|B854", new Style(Style.parseColor("#35bb93"), Style.WHITE));
        STYLES.put("swn|BNE1", new Style(Style.parseColor("#ff9900"), Style.WHITE));
        STYLES.put("swn|BNE2", new Style(Style.parseColor("#0000ff"), Style.WHITE));
        STYLES.put("swn|BNE3", new Style(Style.parseColor("#ff0000"), Style.WHITE));
        STYLES.put("swn|BNE4", new Style(Style.parseColor("#ff9900"), Style.WHITE));
        STYLES.put("swn|BNE5", new Style(Style.parseColor("#9900cc"), Style.WHITE));
        STYLES.put("swn|BNE6", new Style(Style.parseColor("#00cc99"), Style.WHITE));

        // Remscheid
        STYLES.put("swr|B655", new Style(Style.parseColor("#dbcd00"), Style.WHITE));
        STYLES.put("swr|B657", new Style(Style.parseColor("#deb993"), Style.WHITE));
        STYLES.put("swr|B659", new Style(Style.parseColor("#f59b00"), Style.WHITE));
        STYLES.put("swr|B660", new Style(Style.parseColor("#f5a387"), Style.WHITE));
        STYLES.put("swr|B664", new Style(Style.parseColor("#b1a8d3"), Style.WHITE));
        STYLES.put("swr|B666", new Style(Style.parseColor("#0074be"), Style.WHITE));
        STYLES.put("swr|B673", new Style(Style.parseColor("#ee7555"), Style.WHITE));
        STYLES.put("swr|B675", new Style(Style.parseColor("#004e9e"), Style.WHITE));
        STYLES.put("swr|B680", new Style(Style.parseColor("#c78711"), Style.WHITE));
        STYLES.put("swr|BNE14", new Style(Style.parseColor("#2d247b"), Style.WHITE));
        STYLES.put("swr|BNE17", new Style(Style.parseColor("#ef7c00"), Style.WHITE));
        STYLES.put("swr|BNE18", new Style(Style.parseColor("#e5007c"), Style.WHITE));
        STYLES.put("swr|BNE20", new Style(Style.parseColor("#0a5d34"), Style.WHITE));

        // Solingen
        STYLES.put("sws|B681", new Style(Style.parseColor("#016f42"), Style.WHITE));
        STYLES.put("sws|B682", new Style(Style.parseColor("#009b78"), Style.WHITE));
        STYLES.put("sws|B684", new Style(Style.parseColor("#009247"), Style.WHITE));
        STYLES.put("sws|B685", new Style(Style.parseColor("#539138"), Style.WHITE));
        STYLES.put("sws|B686", new Style(Style.parseColor("#a6c539"), Style.WHITE));
        STYLES.put("sws|B687", new Style(Style.parseColor("#406ab4"), Style.WHITE));
        STYLES.put("sws|B689", new Style(Style.parseColor("#8d5e48"), Style.WHITE));
        STYLES.put("sws|B690", new Style(Style.parseColor("#0099cd"), Style.WHITE));
        STYLES.put("sws|B691", new Style(Style.parseColor("#963838"), Style.WHITE));
        STYLES.put("sws|B693", new Style(Style.parseColor("#9a776f"), Style.WHITE));
        STYLES.put("sws|B695", new Style(Style.parseColor("#bf4b75"), Style.WHITE));
        STYLES.put("sws|B696", new Style(Style.parseColor("#6c77b4"), Style.WHITE));
        STYLES.put("sws|B697", new Style(Style.parseColor("#00baf1"), Style.WHITE));
        STYLES.put("sws|B698", new Style(Style.parseColor("#444fa1"), Style.WHITE));
        STYLES.put("sws|B699", new Style(Style.parseColor("#c4812f"), Style.WHITE));
        STYLES.put("sws|BNE21", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("sws|BNE22", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("sws|BNE24", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("sws|BNE25", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("sws|BNE28", new Style(Style.parseColor("#000000"), Style.WHITE));

        // Busse Wuppertal
        STYLES.put("wsw|B600", new Style(Style.parseColor("#cc4e97"), Style.WHITE));
        STYLES.put("wsw|B603", new Style(Style.parseColor("#a77251"), Style.WHITE));
        STYLES.put("wsw|B604", new Style(Style.parseColor("#f39100"), Style.WHITE));
        STYLES.put("wsw|B606", new Style(Style.parseColor("#88301b"), Style.WHITE));
        STYLES.put("wsw|B607", new Style(Style.parseColor("#629e38"), Style.WHITE));
        STYLES.put("wsw|B609", new Style(Style.parseColor("#53ae2e"), Style.WHITE));
        STYLES.put("wsw|B610", new Style(Style.parseColor("#eb5575"), Style.WHITE));
        STYLES.put("wsw|B611", new Style(Style.parseColor("#896a9a"), Style.WHITE));
        STYLES.put("wsw|B612", new Style(Style.parseColor("#cd7c00"), Style.WHITE));
        STYLES.put("wsw|B613", new Style(Style.parseColor("#491d5c"), Style.WHITE));
        STYLES.put("wsw|B614", new Style(Style.parseColor("#00a7c1"), Style.WHITE));
        STYLES.put("wsw|B616", new Style(Style.parseColor("#e4003a"), Style.WHITE));
        STYLES.put("wsw|B617", new Style(Style.parseColor("#95114d"), Style.WHITE));
        STYLES.put("wsw|B618", new Style(Style.parseColor("#cf8360"), Style.WHITE));
        STYLES.put("wsw|B619", new Style(Style.parseColor("#304c9d"), Style.WHITE));
        STYLES.put("wsw|B622", new Style(Style.parseColor("#aabd81"), Style.WHITE));
        STYLES.put("wsw|B623", new Style(Style.parseColor("#e04a23"), Style.WHITE));
        STYLES.put("wsw|B624", new Style(Style.parseColor("#0e9580"), Style.WHITE));
        STYLES.put("wsw|B625", new Style(Style.parseColor("#7aad3b"), Style.WHITE));
        STYLES.put("wsw|B628", new Style(Style.parseColor("#80753b"), Style.WHITE));
        STYLES.put("wsw|B629", new Style(Style.parseColor("#dd72a1"), Style.WHITE));
        STYLES.put("wsw|B630", new Style(Style.parseColor("#0074be"), Style.WHITE));
        STYLES.put("wsw|B631", new Style(Style.parseColor("#5a8858"), Style.WHITE));
        STYLES.put("wsw|B632", new Style(Style.parseColor("#ebac3d"), Style.WHITE));
        STYLES.put("wsw|B633", new Style(Style.parseColor("#4c2182"), Style.WHITE));
        STYLES.put("wsw|B635", new Style(Style.parseColor("#cb6c2b"), Style.WHITE));
        STYLES.put("wsw|B638", new Style(Style.parseColor("#588d58"), Style.WHITE));
        STYLES.put("wsw|B639", new Style(Style.parseColor("#0097c1"), Style.WHITE));
        STYLES.put("wsw|B640", new Style(Style.parseColor("#89ba7a"), Style.WHITE));
        STYLES.put("wsw|B642", new Style(Style.parseColor("#4b72aa"), Style.WHITE));
        STYLES.put("wsw|B643", new Style(Style.parseColor("#009867"), Style.WHITE));
        STYLES.put("wsw|B644", new Style(Style.parseColor("#a57400"), Style.WHITE));
        STYLES.put("wsw|B645", new Style(Style.parseColor("#aeba0e"), Style.WHITE));
        STYLES.put("wsw|B646", new Style(Style.parseColor("#008db5"), Style.WHITE));
        STYLES.put("wsw|B650", new Style(Style.parseColor("#f5bd00"), Style.WHITE));
        STYLES.put("wsw|BNE1", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE2", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE3", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE4", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE5", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE6", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE7", new Style(Style.parseColor("#000000"), Style.WHITE));
        STYLES.put("wsw|BNE8", new Style(Style.parseColor("#000000"), Style.WHITE));

        // H-Bahn Dortmund
        STYLES.put("dsw|CHB1", new Style(Style.parseColor("#e5007c"), Style.WHITE));
        STYLES.put("dsw|CHB2", new Style(Style.parseColor("#e5007c"), Style.WHITE));

        // Schwebebahn Wuppertal
        STYLES.put("wsw|C60", new Style(Style.parseColor("#003090"), Style.WHITE));

        // Busse Hagen
        STYLES.put("hst|B510", new Style(Style.parseColor("#b06520"), Style.WHITE));
        STYLES.put("ver|B511", new Style(Style.parseColor("#15b6b9"), Style.WHITE));
        STYLES.put("hst|B512", new Style(Style.parseColor("#b06520"), Style.WHITE));
        STYLES.put("hst|B513", new Style(Style.parseColor("#7f4984"), Style.WHITE));
        STYLES.put("hst|B514", new Style(Style.parseColor("#f49b00"), Style.WHITE));
        STYLES.put("hst|B515", new Style(Style.parseColor("#c10004"), Style.WHITE));
        STYLES.put("hst|B516", new Style(Style.parseColor("#7eaf49"), Style.WHITE));
        STYLES.put("hst|B517", new Style(Style.parseColor("#619f4e"), Style.WHITE));
        STYLES.put("hst|B518", new Style(Style.parseColor("#007bc1"), Style.WHITE));
        STYLES.put("hst|B519", new Style(Style.parseColor("#007bc1"), Style.WHITE));
        STYLES.put("hst|B520", new Style(Style.parseColor("#e63758"), Style.WHITE));
        STYLES.put("hst|B521", new Style(Style.parseColor("#df0008"), Style.WHITE));
        STYLES.put("hst|B524", new Style(Style.parseColor("#a36501"), Style.WHITE));
        STYLES.put("hst|B525", new Style(Style.parseColor("#df0008"), Style.WHITE));
        STYLES.put("hst|B527", new Style(Style.parseColor("#7c277d"), Style.WHITE));
        STYLES.put("hst|B528", new Style(Style.parseColor("#1784c1"), Style.WHITE));
        STYLES.put("hst|B530", new Style(Style.parseColor("#e60253"), Style.WHITE));
        STYLES.put("hst|B532", new Style(Style.parseColor("#364a9c"), Style.WHITE));
        STYLES.put("hst|B534", new Style(Style.parseColor("#567b3e"), Style.WHITE));
        STYLES.put("hst|B535", new Style(Style.parseColor("#e14c25"), Style.WHITE));
        STYLES.put("hst|B536", new Style(Style.parseColor("#7fceef"), Style.WHITE));
        STYLES.put("hst|B538", new Style(Style.parseColor("#006cb6"), Style.WHITE));
        STYLES.put("hst|B539", new Style(Style.parseColor("#897300"), Style.WHITE));
        STYLES.put("hst|B541", new Style(Style.parseColor("#9c590f"), Style.WHITE));
        STYLES.put("hst|B542", new Style(Style.parseColor("#71c837"), Style.WHITE));
        STYLES.put("hst|B543", new Style(Style.parseColor("#f49b00"), Style.WHITE));
        STYLES.put("hst|BSB71", new Style(Style.parseColor("#15b6b9"), Style.WHITE));
        STYLES.put("hst|BSB72", new Style(Style.parseColor("#0c919c"), Style.WHITE));
        STYLES.put("hst|BNE1", new Style(Style.parseColor("#ff2a2a"), Style.WHITE));
        STYLES.put("hst|BNE2", new Style(Style.parseColor("#ff6600"), Style.WHITE));
        STYLES.put("hst|BNE3", new Style(Style.parseColor("#ffcc00"), Style.BLACK));
        STYLES.put("hst|BNE4", new Style(Style.parseColor("#2ca02c"), Style.WHITE));
        STYLES.put("hst|BNE5", new Style(Style.parseColor("#5f8dd3"), Style.WHITE));
        STYLES.put("hst|BNE6", new Style(Style.parseColor("#7137c8"), Style.WHITE));
        STYLES.put("hst|BNE7", new Style(Style.parseColor("#a05a2c"), Style.WHITE));
        STYLES.put("bvr|BNE9", new Style(Style.parseColor("#800080"), Style.WHITE));
        STYLES.put("hst|BNE19", new Style(Style.parseColor("#a02c2c"), Style.WHITE));
        STYLES.put("hst|BNE22", new Style(Style.parseColor("#006cb6"), Style.WHITE));
        STYLES.put("hst|BNE31", new Style(Style.parseColor("#803300"), Style.WHITE));
        STYLES.put("hst|BNE32", new Style(Style.parseColor("#364a9c"), Style.WHITE));

        // Stadtbahn Köln-Bonn
        STYLES.put("vrs|T1", new Style(Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("vrs|T3", new Style(Style.parseColor("#f680c5"), Style.WHITE));
        STYLES.put("vrs|T4", new Style(Style.parseColor("#f24dae"), Style.WHITE));
        STYLES.put("vrs|T5", new Style(Style.parseColor("#9c8dce"), Style.WHITE));
        STYLES.put("vrs|T7", new Style(Style.parseColor("#f57947"), Style.WHITE));
        STYLES.put("vrs|T9", new Style(Style.parseColor("#f5777b"), Style.WHITE));
        STYLES.put("vrs|T12", new Style(Style.parseColor("#80cc28"), Style.WHITE));
        STYLES.put("vrs|T13", new Style(Style.parseColor("#9e7b65"), Style.WHITE));
        STYLES.put("vrs|T15", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
        STYLES.put("vrs|T16", new Style(Style.parseColor("#33baab"), Style.WHITE));
        STYLES.put("vrs|T18", new Style(Style.parseColor("#05a1e6"), Style.WHITE));
        STYLES.put("vrs|T61", new Style(Style.parseColor("#80cc28"), Style.WHITE));
        STYLES.put("vrs|T62", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
        STYLES.put("vrs|T63", new Style(Style.parseColor("#73d2f6"), Style.WHITE));
        STYLES.put("vrs|T65", new Style(Style.parseColor("#b3db18"), Style.WHITE));
        STYLES.put("vrs|T66", new Style(Style.parseColor("#ec008c"), Style.WHITE));
        STYLES.put("vrs|T67", new Style(Style.parseColor("#f680c5"), Style.WHITE));
        STYLES.put("vrs|T68", new Style(Style.parseColor("#ca93d0"), Style.WHITE));

        // Stadtbahn Bielefeld
        STYLES.put("owl|T1", new Style(Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("owl|T2", new Style(Style.parseColor("#00a650"), Style.WHITE));
        STYLES.put("owl|T3", new Style(Style.parseColor("#fff200"), Style.BLACK));
        STYLES.put("owl|T4", new Style(Style.parseColor("#e2001a"), Style.WHITE));

        // Busse Bonn
        STYLES.put("vrs|B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("vrs|B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
        STYLES.put("vrs|B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
        STYLES.put("vrs|B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
        STYLES.put("vrs|BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
        STYLES.put("vrs|BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
        STYLES.put("vrs|BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
        STYLES.put("vrs|B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
        STYLES.put("vrs|B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
        STYLES.put("vrs|B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
        STYLES.put("vrs|B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
        STYLES.put("vrs|B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
        STYLES.put("vrs|B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
        STYLES.put("vrs|B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
        STYLES.put("vrs|B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
        STYLES.put("vrs|B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
        STYLES.put("vrs|B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
        STYLES.put("vrs|B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
        STYLES.put("vrs|B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
        STYLES.put("vrs|B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
        STYLES.put("vrs|B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
        STYLES.put("vrs|B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
        STYLES.put("vrs|B614", new Style(Style.parseColor("#806839"), Style.WHITE));
        STYLES.put("vrs|B615", new Style(Style.parseColor("#532700"), Style.WHITE));
        STYLES.put("vrs|B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
        STYLES.put("vrs|B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
        STYLES.put("vrs|B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
        STYLES.put("vrs|B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
        STYLES.put("vrs|B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
        STYLES.put("vrs|B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
        STYLES.put("vrs|B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
        STYLES.put("vrs|B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
        STYLES.put("vrs|BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
        STYLES.put("vrs|BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
        STYLES.put("vrs|BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
        STYLES.put("vrs|B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
    }

    @Override
    public Style lineStyle(final @Nullable String network, final @Nullable Product product,
            final @Nullable String label) {
        if (product == Product.BUS && label != null && label.startsWith("SB"))
            return super.lineStyle(network, product, "SB");

        return super.lineStyle(network, product, label);
    }
}
