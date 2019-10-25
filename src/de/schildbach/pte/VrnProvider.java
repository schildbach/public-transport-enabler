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
public class VrnProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://www.vrn.de/mngvrn/");

    public VrnProvider() {
        super(NetworkId.VRN, API_BASE);

        setIncludeRegionId(false);
        setRequestUrlEncoding(Charsets.UTF_8);
        setStyles(STYLES);
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("InterRegio".equals(longName) && symbol == null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "IR");
        }

        if (name != null && name.startsWith("RNV Moonliner "))
            return super.parseLine(id, network, mot, symbol, "M" + name.substring(14), longName, trainType, trainNum,
                    trainName);
        else if (name != null && (name.startsWith("RNV ") || name.startsWith("SWK ")))
            return super.parseLine(id, network, mot, symbol, name.substring(4), longName, trainType, trainNum,
                    trainName);
        else
            return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Straßen- und Stadtbahn Mannheim-Ludwigshafen rnv
        STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#f39b9a"), Style.WHITE));
        STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#b00346"), Style.WHITE));
        STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#d6ad00"), Style.WHITE));
        STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("T4X", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("T4A", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#00975f"), Style.WHITE));
        STYLES.put("T5A", new Style(Shape.RECT, Style.parseColor("#00975f"), Style.WHITE));
        STYLES.put("T5X", new Style(Shape.RECT, Style.parseColor("#00975f"), Style.WHITE));
        STYLES.put("T6", new Style(Shape.RECT, Style.parseColor("#956c29"), Style.WHITE));
        STYLES.put("T6A", new Style(Shape.RECT, Style.parseColor("#956c29"), Style.WHITE));
        STYLES.put("T7", new Style(Shape.RECT, Style.parseColor("#ffcc00"), Style.BLACK));
        STYLES.put("T8", new Style(Shape.RECT, Style.parseColor("#e17600"), Style.WHITE));
        STYLES.put("T9", new Style(Shape.RECT, Style.parseColor("#e6007e"), Style.WHITE));
        STYLES.put("T10", new Style(Shape.RECT, Style.parseColor("#a71680"), Style.WHITE));
        // STYLES.put("T15", new Style(Shape.RECT, Style.parseColor("#7c7c7b"), Style.WHITE));
        STYLES.put("TX", new Style(Shape.RECT, Style.parseColor("#7c7c7b"), Style.WHITE));

        // Busse Mannheim
        STYLES.put("B2", new Style(Shape.CIRCLE, Style.parseColor("#b00346"), Style.WHITE));
        STYLES.put("B4", new Style(Shape.CIRCLE, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("B5", new Style(Shape.CIRCLE, Style.parseColor("#00975f"), Style.BLACK));
        STYLES.put("B7", new Style(Shape.CIRCLE, Style.parseColor("#ffcc00"), Style.BLACK));
        STYLES.put("B40", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B41", new Style(Shape.CIRCLE, Style.parseColor("#82d0f5"), Style.WHITE));
        STYLES.put("B42", new Style(Shape.CIRCLE, Style.parseColor("#a1c3d6"), Style.WHITE));
        STYLES.put("B43", new Style(Shape.CIRCLE, Style.parseColor("#589bd4"), Style.WHITE));
        STYLES.put("B44", new Style(Shape.CIRCLE, Style.parseColor("#009a93"), Style.WHITE));
        STYLES.put("B45", new Style(Shape.CIRCLE, Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("B46", new Style(Shape.CIRCLE, Style.parseColor("#a89bb1"), Style.WHITE));
        STYLES.put("B47", new Style(Shape.CIRCLE, Style.parseColor("#82d0f5"), Style.WHITE));
        STYLES.put("B48", new Style(Shape.CIRCLE, Style.parseColor("#009fe3"), Style.WHITE));
        STYLES.put("B49", new Style(Shape.CIRCLE, Style.parseColor("#009640"), Style.WHITE));
        STYLES.put("B50", new Style(Shape.CIRCLE, Style.parseColor("#a1c3d6"), Style.WHITE));
        STYLES.put("B51", new Style(Shape.CIRCLE, Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("B52", new Style(Shape.CIRCLE, Style.parseColor("#a89bb1"), Style.WHITE));
        STYLES.put("B53", new Style(Shape.CIRCLE, Style.parseColor("#10bbef"), Style.WHITE));
        STYLES.put("B54", new Style(Shape.CIRCLE, Style.parseColor("#b2a0cd"), Style.WHITE));
        STYLES.put("B55", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B56", new Style(Shape.CIRCLE, Style.parseColor("#10bbef"), Style.WHITE));
        STYLES.put("B57", new Style(Shape.CIRCLE, Style.parseColor("#5bc5f2"), Style.WHITE));
        STYLES.put("B57E", new Style(Shape.CIRCLE, Style.parseColor("#5bc5f2"), Style.WHITE));
        STYLES.put("B58", new Style(Shape.CIRCLE, Style.parseColor("#a1c3d6"), Style.WHITE));
        STYLES.put("B59", new Style(Shape.CIRCLE, Style.parseColor("#a89bb1"), Style.WHITE));
        STYLES.put("B60", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B61", new Style(Shape.CIRCLE, Style.parseColor("#4b96d2"), Style.WHITE));
        STYLES.put("B62", new Style(Shape.CIRCLE, Style.parseColor("#a89bb1"), Style.WHITE));
        STYLES.put("B63", new Style(Shape.CIRCLE, Style.parseColor("#a1c3d6"), Style.WHITE));
        STYLES.put("B64", new Style(Shape.CIRCLE, Style.parseColor("#0091a6"), Style.WHITE));

        // Stadtbus Ludwigshafen
        STYLES.put("B70", new Style(Shape.CIRCLE, Style.parseColor("#4b96d2"), Style.WHITE));
        STYLES.put("B71", new Style(Shape.CIRCLE, Style.parseColor("#a89bb1"), Style.WHITE));
        STYLES.put("B72", new Style(Shape.CIRCLE, Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("B73", new Style(Shape.CIRCLE, Style.parseColor("#8bc5bd"), Style.WHITE));
        STYLES.put("B74", new Style(Shape.CIRCLE, Style.parseColor("#82d0f5"), Style.WHITE));
        STYLES.put("B75", new Style(Shape.CIRCLE, Style.parseColor("#008f88"), Style.WHITE));
        STYLES.put("B76", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B77", new Style(Shape.CIRCLE, Style.parseColor("#c290b8"), Style.WHITE));
        STYLES.put("B78", new Style(Shape.CIRCLE, Style.parseColor("#4b96d2"), Style.WHITE));
        STYLES.put("B79E", new Style(Shape.CIRCLE, Standard.COLOR_BACKGROUND_BUS, Style.WHITE));
        STYLES.put("B84", new Style(Shape.CIRCLE, Style.parseColor("#8d2176"), Style.WHITE));
        STYLES.put("B85", new Style(Shape.CIRCLE, Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("B86", new Style(Shape.CIRCLE, Style.parseColor("#82d0f5"), Style.WHITE));
        STYLES.put("B87", new Style(Shape.CIRCLE, Style.parseColor("#69598f"), Style.WHITE));
        STYLES.put("B88", new Style(Shape.CIRCLE, Style.parseColor("#8bc5bd"), Style.WHITE));

        // Nachtbus Ludwigshafen
        STYLES.put("B90", new Style(Shape.CIRCLE, Style.parseColor("#86bc25"), Style.WHITE));
        // STYLES.put("B91", new Style(Shape.CIRCLE, Style.parseColor("#898F93"), Style.WHITE));
        STYLES.put("B94", new Style(Shape.CIRCLE, Style.parseColor("#83d0f5"), Style.WHITE));
        STYLES.put("B96", new Style(Shape.CIRCLE, Style.parseColor("#c05d18"), Style.WHITE));
        STYLES.put("B97", new Style(Shape.CIRCLE, Style.parseColor("#ffed00"), Style.BLACK));
        // Nachtbus Ludwigshafen-Mannheim
        STYLES.put("B6", new Style(Shape.CIRCLE, Style.parseColor("#956c29"), Style.WHITE));

        // Straßenbahn Heidelberg
        STYLES.put("T21", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("T22", new Style(Shape.RECT, Style.parseColor("#fdc300"), Style.BLACK));
        STYLES.put("T23", new Style(Shape.RECT, Style.parseColor("#e48f00"), Style.WHITE));
        STYLES.put("T24", new Style(Shape.RECT, Style.parseColor("#8d2176"), Style.WHITE));
        STYLES.put("T26", new Style(Shape.RECT, Style.parseColor("#f39b9a"), Style.WHITE));

        // Stadtbus Heidelberg rnv
        STYLES.put("B27", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B28", new Style(Shape.CIRCLE, Style.parseColor("#b2a0cd"), Style.WHITE));
        STYLES.put("B29", new Style(Shape.CIRCLE, Style.parseColor("#10bbef"), Style.WHITE));
        STYLES.put("B30", new Style(Shape.CIRCLE, Style.parseColor("#baabd4"), Style.WHITE));
        STYLES.put("B31", new Style(Shape.CIRCLE, Style.parseColor("#4b96d2"), Style.WHITE));
        STYLES.put("B32", new Style(Shape.CIRCLE, Style.parseColor("#a1c3d6"), Style.WHITE));
        STYLES.put("B33", new Style(Shape.CIRCLE, Style.parseColor("#0069b4"), Style.WHITE));
        STYLES.put("B34", new Style(Shape.CIRCLE, Style.parseColor("#009fe3"), Style.WHITE));
        STYLES.put("B35", new Style(Shape.CIRCLE, Style.parseColor("#4e2583"), Style.WHITE));
        STYLES.put("B36", new Style(Shape.CIRCLE, Style.parseColor("#b2a0cd"), Style.WHITE));
        STYLES.put("B37", new Style(Shape.CIRCLE, Style.parseColor("#10bbef"), Style.WHITE));
        STYLES.put("B38", new Style(Shape.CIRCLE, Style.parseColor("#0097b5"), Style.WHITE));
        STYLES.put("B39", new Style(Shape.CIRCLE, Style.parseColor("#512985"), Style.WHITE));

        // Moonliner Heidelberg
        STYLES.put("BM1", new Style(Style.parseColor("#FFCB06"), Style.parseColor("#0A3F88")));
        STYLES.put("BM2", new Style(Style.parseColor("#F9A75E"), Style.parseColor("#0A3F88")));
        STYLES.put("BM3", new Style(Style.parseColor("#FFCB06"), Style.parseColor("#0A3F88")));
        STYLES.put("BM4", new Style(Style.parseColor("#FFCB06"), Style.parseColor("#0A3F88")));
        STYLES.put("BM5", new Style(Style.parseColor("#FFF100"), Style.parseColor("#0A3F88")));

        // Bus Rheinpfalz
        STYLES.put("B484", new Style(Style.parseColor("#BE1E2E"), Style.WHITE));
        STYLES.put("B570", new Style(Style.parseColor("#9B2590"), Style.WHITE));
        STYLES.put("B571", new Style(Style.parseColor("#303192"), Style.WHITE));
        STYLES.put("B572", new Style(Style.parseColor("#00A651"), Style.WHITE));
        STYLES.put("B574", new Style(Style.parseColor("#00ADEE"), Style.WHITE));
        STYLES.put("B580", new Style(Style.parseColor("#00A8E7"), Style.WHITE));
        STYLES.put("B581", new Style(Style.parseColor("#F7941D"), Style.WHITE));

        // S-Bahn Rhein-Neckar
        STYLES.put("SS1", new Style(Style.parseColor("#EE1C25"), Style.WHITE));
        STYLES.put("SS2", new Style(Style.parseColor("#0077C0"), Style.WHITE));
        STYLES.put("SS3", new Style(Style.parseColor("#4F2E92"), Style.WHITE));
        STYLES.put("SS33", new Style(Style.parseColor("#4F2E92"), Style.WHITE));
        STYLES.put("SS4", new Style(Style.parseColor("#00A651"), Style.BLACK));
        STYLES.put("SS5", new Style(Style.parseColor("#F89735"), Style.WHITE));
        STYLES.put("SS51", new Style(Style.parseColor("#F89735"), Style.WHITE));
        STYLES.put("SS6", new Style(Style.parseColor("#007EC5"), Style.WHITE));

        // Bus Bad Bergzabern
        STYLES.put("B540", new Style(Style.parseColor("#FDC500"), Style.WHITE));
        STYLES.put("B541", new Style(Style.parseColor("#C10625"), Style.WHITE));
        STYLES.put("B543", new Style(Style.parseColor("#417B1C"), Style.WHITE));
        STYLES.put("B544", new Style(Style.parseColor("#00527E"), Style.WHITE));

        // Bus Grünstadt und Umgebung
        STYLES.put("B451", new Style(Style.parseColor("#1AA94A"), Style.WHITE));
        STYLES.put("B453", new Style(Style.parseColor("#F495BF"), Style.WHITE));
        STYLES.put("B454", new Style(Style.parseColor("#60B7D4"), Style.WHITE));
        STYLES.put("B455", new Style(Style.parseColor("#FECC2F"), Style.WHITE));
        STYLES.put("B457", new Style(Style.parseColor("#AAA23D"), Style.WHITE));
        STYLES.put("B458", new Style(Style.parseColor("#E54D6F"), Style.WHITE));
        STYLES.put("B460", new Style(Style.parseColor("#9F0833"), Style.WHITE));
        STYLES.put("B461", new Style(Style.parseColor("#F68D31"), Style.WHITE));

        // Bus Sinsheim
        STYLES.put("B741", new Style(Style.parseColor("#459959"), Style.WHITE));
        STYLES.put("B761", new Style(Style.parseColor("#BECE31"), Style.WHITE));
        STYLES.put("B762", new Style(Style.parseColor("#5997C1"), Style.WHITE));
        STYLES.put("B763", new Style(Style.parseColor("#FFC20A"), Style.WHITE));
        STYLES.put("B765", new Style(Style.parseColor("#066D6C"), Style.WHITE));
        STYLES.put("B768", new Style(Style.parseColor("#0FAD99"), Style.WHITE));
        STYLES.put("B782", new Style(Style.parseColor("#3BC1CF"), Style.WHITE));
        STYLES.put("B795", new Style(Style.parseColor("#0056A7"), Style.WHITE));
        STYLES.put("B796", new Style(Style.parseColor("#F47922"), Style.WHITE));
        STYLES.put("B797", new Style(Style.parseColor("#A62653"), Style.WHITE));

        // Bus Wonnegau-Altrhein
        STYLES.put("B427", new Style(Style.parseColor("#00A651"), Style.WHITE));
        STYLES.put("B435", new Style(Style.parseColor("#A3788C"), Style.WHITE));
        STYLES.put("B660", new Style(Style.parseColor("#0FAD99"), Style.WHITE));
        STYLES.put("B436", new Style(Style.parseColor("#8169AF"), Style.WHITE));
        STYLES.put("B663", new Style(Style.parseColor("#7FB6A4"), Style.WHITE));
        STYLES.put("B921", new Style(Style.parseColor("#F7941D"), Style.WHITE));
        STYLES.put("B437", new Style(Style.parseColor("#00ADEE"), Style.WHITE));
        STYLES.put("B418", new Style(Style.parseColor("#BFB677"), Style.WHITE));
        STYLES.put("B434", new Style(Style.parseColor("#A65631"), Style.WHITE));
        STYLES.put("B431", new Style(Style.parseColor("#CA5744"), Style.WHITE));
        STYLES.put("B406", new Style(Style.parseColor("#00A99D"), Style.WHITE));
        STYLES.put("B433", new Style(Style.parseColor("#5D8AC6"), Style.WHITE));
        STYLES.put("B432", new Style(Style.parseColor("#82A958"), Style.WHITE));

        // Bus Odenwald-Mitte
        STYLES.put("B667", new Style(Style.parseColor("#00A651"), Style.WHITE));
        STYLES.put("B684", new Style(Style.parseColor("#039CDB"), Style.WHITE));
        STYLES.put("B687", new Style(Style.parseColor("#86D1D1"), Style.WHITE));
        STYLES.put("B691", new Style(Style.parseColor("#BBAFD6"), Style.WHITE));
        STYLES.put("B697", new Style(Style.parseColor("#002B5C"), Style.WHITE));
        STYLES.put("B698", new Style(Style.parseColor("#AA568D"), Style.WHITE));

        // Bus Saarbrücken und Umland
        STYLES.put("B231", new Style(Style.parseColor("#94C11C"), Style.WHITE));
        STYLES.put("B232", new Style(Style.parseColor("#A12785"), Style.WHITE));
        STYLES.put("B233", new Style(Style.parseColor("#0098D8"), Style.WHITE));
        STYLES.put("B234", new Style(Style.parseColor("#FDC500"), Style.WHITE));
        STYLES.put("B235", new Style(Style.parseColor("#C10525"), Style.WHITE));
        STYLES.put("B236", new Style(Style.parseColor("#104291"), Style.WHITE));
        STYLES.put("B237", new Style(Style.parseColor("#23AD7A"), Style.WHITE));
        STYLES.put("B238", new Style(Style.parseColor("#F39100"), Style.WHITE));
        STYLES.put("B240", new Style(Style.parseColor("#E5007D"), Style.WHITE));

        // Bus Neckargemünd
        STYLES.put("B735", new Style(Style.parseColor("#F47922"), Style.WHITE));
        STYLES.put("B743", new Style(Style.parseColor("#EE1C25"), Style.WHITE));
        STYLES.put("B752", new Style(Style.parseColor("#0D7253"), Style.WHITE));
        STYLES.put("B753", new Style(Style.parseColor("#3BC1CF"), Style.WHITE));
        STYLES.put("B754", new Style(Style.parseColor("#F99D1D"), Style.WHITE));
        STYLES.put("B817", new Style(Style.parseColor("#0080A6"), Style.WHITE));

        // Bus Ladenburg
        STYLES.put("B625", new Style(Style.parseColor("#006F45"), Style.WHITE));
        STYLES.put("B626", new Style(Style.parseColor("#5997C1"), Style.WHITE));
        STYLES.put("B627", new Style(Style.parseColor("#A62653"), Style.WHITE));
        STYLES.put("B628", new Style(Style.parseColor("#EE1C25"), Style.WHITE));
        STYLES.put("B629", new Style(Style.parseColor("#008B9E"), Style.WHITE));

        // Bus Worms
        STYLES.put("B407", new Style(Style.parseColor("#F58581"), Style.WHITE));
        STYLES.put("B402", new Style(Style.parseColor("#078F47"), Style.WHITE));
        STYLES.put("B410", new Style(Style.parseColor("#9D368F"), Style.WHITE));
        STYLES.put("B408", new Style(Style.parseColor("#A79A39"), Style.WHITE));
        STYLES.put("B406", new Style(Style.parseColor("#00A99D"), Style.WHITE));
        STYLES.put("B4906", new Style(Style.parseColor("#BEBEC1"), Style.WHITE));
        STYLES.put("B4905", new Style(Style.parseColor("#BEBEC1"), Style.WHITE));
        STYLES.put("B409", new Style(Style.parseColor("#8691B3"), Style.WHITE));

        // Bus Kaiserslautern
        STYLES.put("B101", new Style(Style.parseColor("#EB690B"), Style.WHITE));
        STYLES.put("B102", new Style(Style.parseColor("#B9418E"), Style.WHITE));
        STYLES.put("B103", new Style(Style.parseColor("#FFED00"), Style.BLACK));
        STYLES.put("B104", new Style(Style.parseColor("#7AB51D"), Style.WHITE));
        STYLES.put("B105", new Style(Style.parseColor("#00712C"), Style.WHITE));
        STYLES.put("B106", new Style(Style.parseColor("#F7AA00"), Style.BLACK));
        STYLES.put("B107", new Style(Style.parseColor("#A05322"), Style.WHITE));
        STYLES.put("B108", new Style(Style.parseColor("#FFE081"), Style.BLACK));
        STYLES.put("B111", new Style(Style.parseColor("#004494"), Style.WHITE));
        STYLES.put("B112", new Style(Style.parseColor("#009EE0"), Style.WHITE));
        STYLES.put("B114", new Style(Style.parseColor("#C33F52"), Style.WHITE));
        STYLES.put("B115", new Style(Style.parseColor("#E2001A"), Style.WHITE));
        STYLES.put("B116", new Style(Style.parseColor("#007385"), Style.WHITE));
        STYLES.put("B117", new Style(Style.parseColor("#622379"), Style.WHITE));

        // Bus Weinheim
        STYLES.put("B631", new Style(Style.parseColor("#949599"), Style.WHITE));
        STYLES.put("B632", new Style(Style.parseColor("#003D72"), Style.WHITE));
        STYLES.put("B632A", new Style(Style.parseColor("#0083C2"), Style.WHITE));
        STYLES.put("B633", new Style(Style.parseColor("#EE1C25"), Style.WHITE));
        STYLES.put("B634", new Style(Style.parseColor("#F58221"), Style.WHITE));
        STYLES.put("B681", new Style(Style.parseColor("#00B7BD"), Style.WHITE));
        STYLES.put("B682", new Style(Style.parseColor("#D1AC75"), Style.WHITE));
        STYLES.put("B688", new Style(Style.parseColor("#72BAAF"), Style.WHITE));

        // Bus Schwetzingen-Hockenheim und Umgebung
        STYLES.put("B710", new Style(Style.parseColor("#C10625"), Style.WHITE));
        STYLES.put("B711", new Style(Style.parseColor("#417B1C"), Style.WHITE));
        STYLES.put("B712", new Style(Style.parseColor("#A12486"), Style.WHITE));
        STYLES.put("B713", new Style(Style.parseColor("#0398D8"), Style.WHITE));
        STYLES.put("B715", new Style(Style.parseColor("#FDC500"), Style.WHITE));
        STYLES.put("B716", new Style(Style.parseColor("#93C11C"), Style.WHITE));
        STYLES.put("B717", new Style(Style.parseColor("#004F7A"), Style.WHITE));
        STYLES.put("B718", new Style(Style.parseColor("#EE7221"), Style.WHITE));
        STYLES.put("B732", new Style(Style.parseColor("#008692"), Style.WHITE));
        STYLES.put("B738", new Style(Style.parseColor("#9C9D9D"), Style.WHITE));
        STYLES.put("B128", new Style(Style.parseColor("#9C9D9D"), Style.WHITE));

        // Bus Odenwald-Süd
        STYLES.put("B686", new Style(Style.parseColor("#E2001A"), Style.WHITE));
        STYLES.put("B683", new Style(Style.parseColor("#C74E1B"), Style.WHITE));
        STYLES.put("B692", new Style(Style.parseColor("#F7A800"), Style.WHITE));
        STYLES.put("B685", new Style(Style.parseColor("#B1C903"), Style.WHITE));
        STYLES.put("B688", new Style(Style.parseColor("#54C3EC"), Style.WHITE));

        // Bus Neustadt/Wstr. und Umgebung
        STYLES.put("B500", new Style(Style.parseColor("#459959"), Style.WHITE));
        STYLES.put("B501", new Style(Style.parseColor("#F57F22"), Style.WHITE));
        STYLES.put("B503", new Style(Style.parseColor("#0058A9"), Style.WHITE));
        STYLES.put("B504", new Style(Style.parseColor("#BECE31"), Style.WHITE));
        STYLES.put("B505", new Style(Style.parseColor("#BECE31"), Style.WHITE));
        STYLES.put("B506", new Style(Style.parseColor("#FFC21C"), Style.WHITE));
        STYLES.put("B507", new Style(Style.parseColor("#A62653"), Style.WHITE));
        STYLES.put("B508", new Style(Style.parseColor("#3BC1CF"), Style.WHITE));
        STYLES.put("B509", new Style(Style.parseColor("#F03F23"), Style.WHITE));
        STYLES.put("B510", new Style(Style.parseColor("#E7ACC6"), Style.WHITE));
        STYLES.put("B512", new Style(Style.parseColor("#5997C1"), Style.WHITE));
        STYLES.put("B517", new Style(Style.parseColor("#066D6C"), Style.WHITE));

        // Bus Neckar-Odenwald-Kreis
        STYLES.put("B821", new Style(Style.parseColor("#263791"), Style.WHITE));
        STYLES.put("B822", new Style(Style.parseColor("#00ADEE"), Style.WHITE));
        STYLES.put("B823", new Style(Style.parseColor("#056736"), Style.WHITE));
        STYLES.put("B824", new Style(Style.parseColor("#9A8174"), Style.WHITE));
        STYLES.put("B828", new Style(Style.parseColor("#9A8174"), Style.WHITE));
        STYLES.put("B832", new Style(Style.parseColor("#F7941D"), Style.WHITE));
        STYLES.put("B833", new Style(Style.parseColor("#C1B404"), Style.WHITE));
        STYLES.put("B834", new Style(Style.parseColor("#90C73E"), Style.WHITE));
        STYLES.put("B835", new Style(Style.parseColor("#662D91"), Style.WHITE));
        STYLES.put("B836", new Style(Style.parseColor("#EE2026"), Style.WHITE));
        STYLES.put("B837", new Style(Style.parseColor("#00A651"), Style.WHITE));
        STYLES.put("B838", new Style(Style.parseColor("#8B711B"), Style.WHITE));
        STYLES.put("B839", new Style(Style.parseColor("#662D91"), Style.WHITE));
        STYLES.put("B841", new Style(Style.parseColor("#C0B296"), Style.WHITE));
        STYLES.put("B843", new Style(Style.parseColor("#DBE122"), Style.WHITE));
        STYLES.put("B844", new Style(Style.parseColor("#93B366"), Style.WHITE));
        STYLES.put("B849", new Style(Style.parseColor("#E19584"), Style.WHITE));
        STYLES.put("B857", new Style(Style.parseColor("#C01B2A"), Style.WHITE));
        STYLES.put("B857", new Style(Style.parseColor("#D2B10C"), Style.WHITE));

        // Bus Landkreis Germersheim
        STYLES.put("B550", new Style(Style.parseColor("#870B36"), Style.WHITE));
        STYLES.put("B552", new Style(Style.parseColor("#96387C"), Style.WHITE));
        STYLES.put("B554", new Style(Style.parseColor("#EE542E"), Style.WHITE));
        STYLES.put("B555", new Style(Style.parseColor("#EC2E6B"), Style.WHITE));
        STYLES.put("B556", new Style(Style.parseColor("#D7DF21"), Style.WHITE));
        STYLES.put("B557", new Style(Style.parseColor("#BD7BB4"), Style.WHITE));
        STYLES.put("B558", new Style(Style.parseColor("#ED5956"), Style.WHITE));
        STYLES.put("B559", new Style(Style.parseColor("#EE4F5E"), Style.WHITE));
        STYLES.put("B595", new Style(Style.parseColor("#00A65E"), Style.WHITE));
        STYLES.put("B596", new Style(Style.parseColor("#73479C"), Style.WHITE));
        STYLES.put("B546", new Style(Style.parseColor("#E81D34"), Style.WHITE));
        STYLES.put("B547", new Style(Style.parseColor("#991111"), Style.WHITE));
        STYLES.put("B548", new Style(Style.parseColor("#974E04"), Style.WHITE));
        STYLES.put("B549", new Style(Style.parseColor("#F7A5AD"), Style.WHITE));
        STYLES.put("B593", new Style(Style.parseColor("#D1B0A3"), Style.WHITE));
        STYLES.put("B594", new Style(Style.parseColor("#FAA86F"), Style.WHITE));
        STYLES.put("B598", new Style(Style.parseColor("#71BF44"), Style.WHITE));
        STYLES.put("B590", new Style(Style.parseColor("#C50A54"), Style.WHITE));
        STYLES.put("B592", new Style(Style.parseColor("#00B6BD"), Style.WHITE));
        STYLES.put("B599", new Style(Style.parseColor("#00AEEF"), Style.WHITE));

        // Bus Südliche Weinstraße
        STYLES.put("B525", new Style(Style.parseColor("#009EE0"), Style.WHITE));
        STYLES.put("B523", new Style(Style.parseColor("#F4A10B"), Style.WHITE));
        STYLES.put("B524", new Style(Style.parseColor("#FFEC00"), Style.BLACK));
        STYLES.put("B531", new Style(Style.parseColor("#2DA84D"), Style.WHITE));
        STYLES.put("B532", new Style(Style.parseColor("#00FD00"), Style.BLACK));
        STYLES.put("B520", new Style(Style.parseColor("#FF3333"), Style.WHITE));
        STYLES.put("B530", new Style(Style.parseColor("#E84A93"), Style.WHITE));

        // Bus Speyer
        STYLES.put("B561", new Style(Style.parseColor("#003D72"), Style.WHITE));
        STYLES.put("B562", new Style(Style.parseColor("#F58221"), Style.WHITE));
        STYLES.put("B563", new Style(Style.parseColor("#EE1C25"), Style.WHITE));
        STYLES.put("B564", new Style(Style.parseColor("#006C3B"), Style.WHITE));
        STYLES.put("B565", new Style(Style.parseColor("#00B7BD"), Style.WHITE));
        STYLES.put("B566", new Style(Style.parseColor("#D1AC75"), Style.WHITE));
        STYLES.put("B567", new Style(Style.parseColor("#95080A"), Style.WHITE));
        STYLES.put("B568", new Style(Style.parseColor("#0067B3"), Style.WHITE));
        STYLES.put("B569", new Style(Style.parseColor("#71BF44"), Style.WHITE));

        // Bus Frankenthal/Pfalz
        STYLES.put("B462", new Style(Style.parseColor("#93C11C"), Style.WHITE));
        STYLES.put("B463", new Style(Style.parseColor("#A12486"), Style.WHITE));
        STYLES.put("B464", new Style(Style.parseColor("#0398D8"), Style.WHITE));
        STYLES.put("B466", new Style(Style.parseColor("#FDC500"), Style.WHITE));
        STYLES.put("B467", new Style(Style.parseColor("#C10625"), Style.WHITE));
    }
}
