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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class KvvProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://projekte.kvv-efa.de/sl3/");

    public KvvProvider() {
        this(API_BASE);
    }

    public KvvProvider(final HttpUrl apiBase) {
        super(NetworkId.KVV, apiBase);
        setRequestUrlEncoding(Charsets.UTF_8);
        setStyles(STYLES);
        setSessionCookieName("HASESSIONID");
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            @Nullable String symbol, @Nullable String name, @Nullable String longName, final @Nullable String trainType,
            final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if (trainName != null && trainName.startsWith("TRILEX"))
                return new Line(id, network, Product.REGIONAL_TRAIN, trainName);
            if ("IRE 1".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "IRE1");
            if ("IRE 3".equals(trainNum))
                return new Line(id, network, Product.REGIONAL_TRAIN, "IRE3");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Stadtbahn AVG
        STYLES.put("SS1", new Style(Style.parseColor("#00a76c"), Style.WHITE));
        STYLES.put("SS11", new Style(Style.parseColor("#00a76c"), Style.WHITE));
        STYLES.put("SS12", new Style(Style.parseColor("#00a76c"), Style.WHITE));
        STYLES.put("SS2", new Style(Style.parseColor("#b286bc"), Style.WHITE));
        STYLES.put("SS31", new Style(Style.parseColor("#00a99d"), Style.WHITE));
        STYLES.put("SS32", new Style(Style.parseColor("#00a99d"), Style.WHITE));
        STYLES.put("SS4", new Style(Style.parseColor("#9f184c"), Style.WHITE));
        STYLES.put("SS41", new Style(Style.parseColor("#bed730"), Style.WHITE));
        STYLES.put("SS42", new Style(Style.parseColor("#0097bb"), Style.WHITE));
        STYLES.put("SS5", new Style(Style.parseColor("#f69795"), Style.BLACK));
        STYLES.put("SS51", new Style(Style.parseColor("#f69795"), Style.BLACK));
        STYLES.put("SS52", new Style(Style.parseColor("#f69795"), Style.BLACK));
        STYLES.put("SS6", new Style(Style.parseColor("#282268"), Style.WHITE));
        STYLES.put("SS7", new Style(Style.parseColor("#fff200"), Style.BLACK));
        STYLES.put("SS71", new Style(Style.parseColor("#fff200"), Style.BLACK));
        STYLES.put("SS8", new Style(Style.parseColor("#6e692a"), Style.WHITE));
        STYLES.put("SS81", new Style(Style.parseColor("#6e692a"), Style.WHITE));

        // S-Bahn Rhein-Neckar
        STYLES.put("ddb|SS1", new Style(Style.parseColor("#e11a22"), Style.WHITE));
        STYLES.put("ddb|SS2", new Style(Style.parseColor("#0077c0"), Style.WHITE));
        STYLES.put("ddb|SS3", new Style(Style.parseColor("#ffdd00"), Style.BLACK));
        STYLES.put("ddb|SS33", new Style(Style.parseColor("#8d5ca6"), Style.WHITE));
        STYLES.put("ddb|SS4", new Style(Style.parseColor("#00a650"), Style.WHITE));
        STYLES.put("ddb|SS44", new Style(Style.parseColor("#00a650"), Style.WHITE));
        STYLES.put("ddb|SS5", new Style(Style.parseColor("#f79433"), Style.WHITE));
        STYLES.put("ddb|SS51", new Style(Style.parseColor("#f79433"), Style.WHITE));
        STYLES.put("ddb|SS9", new Style(Style.parseColor("#a6ce42"), Style.WHITE));

        // Tram
        STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("T1E", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
        STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
        STYLES.put("T2E", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
        STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#947139"), Style.WHITE));
        STYLES.put("T3E", new Style(Shape.RECT, Style.parseColor("#947139"), Style.WHITE));
        STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#ffcb04"), Style.BLACK));
        STYLES.put("T4E", new Style(Shape.RECT, Style.parseColor("#ffcb04"), Style.BLACK));
        STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#00c0f3"), Style.WHITE));
        STYLES.put("T5E", new Style(Shape.RECT, Style.parseColor("#00c0f3"), Style.WHITE));
        STYLES.put("T6", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE)); // Baustellenlinie
        STYLES.put("T6E", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE)); // Baustellenlinie
        STYLES.put("T7", new Style(Shape.RECT, Style.parseColor("#58595b"), Style.WHITE)); // Baustellenlinie
        STYLES.put("T7E", new Style(Shape.RECT, Style.parseColor("#58595b"), Style.WHITE)); // Baustellenlinie
        STYLES.put("T8", new Style(Shape.RECT, Style.parseColor("#f7931d"), Style.BLACK));
        STYLES.put("T8E", new Style(Shape.RECT, Style.parseColor("#f7931d"), Style.BLACK));
        STYLES.put("T10", new Style(Shape.RECT, Style.parseColor("#a4d7bb"), Style.BLACK)); // Baustellenlinie
        STYLES.put("T10E", new Style(Shape.RECT, Style.parseColor("#a4d7bb"), Style.BLACK)); // Baustellenlinie

        // Stadtbus Karlsruhe
        STYLES.put("B21", new Style(Shape.CIRCLE, Style.parseColor("#177752"), Style.WHITE));
        STYLES.put("B22", new Style(Shape.CIRCLE, Style.parseColor("#62bb46"), Style.WHITE));
        STYLES.put("B22A", new Style(Shape.CIRCLE, Style.parseColor("#62bb46"), Style.WHITE));
        STYLES.put("B22B", new Style(Shape.CIRCLE, Style.parseColor("#62bb46"), Style.WHITE));
        STYLES.put("B23", new Style(Shape.CIRCLE, Style.parseColor("#adbc72"), Style.WHITE));
        STYLES.put("B24", new Style(Shape.CIRCLE, Style.parseColor("#004a21"), Style.WHITE));
        STYLES.put("B26", new Style(Shape.CIRCLE, Style.parseColor("#d7df23"), Style.WHITE));
        STYLES.put("B27", new Style(Shape.CIRCLE, Style.parseColor("#00a851"), Style.WHITE));
        STYLES.put("B27E", new Style(Shape.CIRCLE, Style.parseColor("#00a851"), Style.WHITE));
        STYLES.put("B29", new Style(Shape.CIRCLE, Style.parseColor("#90ab98"), Style.WHITE));
        STYLES.put("B30", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("B31", new Style(Shape.CIRCLE, Style.parseColor("#a1d1e6"), Style.WHITE));
        STYLES.put("BALT31", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B31X", new Style(Shape.CIRCLE, Style.parseColor("#a1d1e6"), Style.WHITE));
        STYLES.put("BALT31X", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B32", new Style(Shape.CIRCLE, Style.parseColor("#485e88"), Style.WHITE));
        STYLES.put("BALT32", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B42", new Style(Shape.CIRCLE, Style.parseColor("#485e88"), Style.WHITE));
        STYLES.put("BALT42", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B44", new Style(Shape.CIRCLE, Style.parseColor("#a1d1e6"), Style.WHITE));
        STYLES.put("B44X", new Style(Shape.CIRCLE, Style.parseColor("#a1d1e6"), Style.WHITE));
        STYLES.put("B47", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("B47A", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("B47X", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
        STYLES.put("B50", new Style(Shape.CIRCLE, Style.parseColor("#874487"), Style.WHITE));
        STYLES.put("B51", new Style(Shape.CIRCLE, Style.parseColor("#b592b9"), Style.WHITE));
        STYLES.put("B52", new Style(Shape.CIRCLE, Style.parseColor("#874487"), Style.WHITE));
        STYLES.put("BALT53", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("BALT54", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B55", new Style(Shape.CIRCLE, Style.parseColor("#574187"), Style.WHITE));
        STYLES.put("B60", new Style(Shape.CIRCLE, Style.parseColor("#574187"), Style.WHITE));
        STYLES.put("B62", new Style(Shape.CIRCLE, Style.parseColor("#9b95c9"), Style.WHITE));
        STYLES.put("BALT64", new Style(Shape.CIRCLE, Style.parseColor("#231f20"), Style.YELLOW)); // Anruf-Linien-Taxi
        STYLES.put("B70", new Style(Shape.CIRCLE, Style.parseColor("#806a50"), Style.WHITE));
        STYLES.put("B71", new Style(Shape.CIRCLE, Style.parseColor("#b2a291"), Style.WHITE));
        STYLES.put("B72", new Style(Shape.CIRCLE, Style.parseColor("#d2ab67"), Style.WHITE));
        STYLES.put("B73", new Style(Shape.CIRCLE, Style.parseColor("#806a50"), Style.WHITE));
        STYLES.put("B74", new Style(Shape.CIRCLE, Style.parseColor("#d2ab67"), Style.WHITE));
        STYLES.put("B75", new Style(Shape.CIRCLE, Style.parseColor("#a25641"), Style.WHITE));
        STYLES.put("B83", new Style(Shape.CIRCLE, Style.parseColor("#808285"), Style.WHITE));

        // Nightliner
        STYLES.put("TNL1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE)); // Tram
        STYLES.put("TNL1E", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE)); // Tram
        STYLES.put("TNL2", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE)); // Tram
        STYLES.put("TNL2E", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE)); // Tram
        STYLES.put("BNL3", new Style(Shape.CIRCLE, Style.parseColor("#806a50"), Style.WHITE)); // Bus
        STYLES.put("BNL11", new Style(Shape.RECT, Style.parseColor("#00aeef"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL12", new Style(Shape.RECT, Style.parseColor("#2e3092"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL13", new Style(Shape.RECT, Style.parseColor("#9b95c9"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL14", new Style(Shape.RECT, Style.parseColor("#a25641"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL15", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL16", new Style(Shape.RECT, Style.parseColor("#177752"), Style.WHITE)); // Anruf-Linien-Taxi
        STYLES.put("BNL17", new Style(Shape.RECT, Style.parseColor("#574187"), Style.WHITE)); // Anruf-Linien-Taxi
    }
}
