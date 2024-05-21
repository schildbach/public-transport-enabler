/*
 * Copyright 2010-2015 the original author or authors.
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
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class TlemProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://nationaljourneyplanner.travelinesw.com/swe/");
    // http://www.travelineeastmidlands.co.uk/em/
    // http://www.travelinesoutheast.org.uk/se/
    // http://www.travelineeastanglia.org.uk/ea/
    // http://www.travelinemidlands.co.uk/wmtis/
    // http://jp.networkwestmidlands.com/centro/

    public TlemProvider() {
        super(NetworkId.TLEM, API_BASE);
        setLanguage("en");
        setTimeZone("Europe/London");
        setUseProxFootSearch(false);
        setStyles(STYLES);
    }

    @Override
    protected String normalizeLocationName(final String name) {
        final String normalizedName = super.normalizeLocationName(name);
        if (normalizedName != null && normalizedName.endsWith(" ()"))
            return normalizedName.substring(0, normalizedName.length() - 3);
        else
            return normalizedName;
    }

    @Override
    public Set<Product> defaultProducts() {
        return Product.ALL;
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("1".equals(mot)) {
            if (trainType == null && ("DLR".equals(trainNum) || "Light Railway".equals(trainName)))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "DLR");
            
            //adding elizabeth line right here and i don't know how to test that. it just looks like it might make some sense.
            //i've looked at a JSON response from traveline and it says following:
            //product	"Elizabeth line"
            //productId	"13"
            //type	"9"
            //code	"18"
            //trainType	"OO"
            //this might be a good template to improve routing in UK, to be honest
            
        if ("1".equals(mot)) {
            if (trainType == "OO" && ("Elizabeth line".equals(product)))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "Elizabeth Line");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // London
        STYLES.put("UBakerloo", new Style(Style.parseColor("#9D5324"), Style.WHITE));
        STYLES.put("UCentral", new Style(Style.parseColor("#D52B1E"), Style.WHITE));
        STYLES.put("UCircle", new Style(Style.parseColor("#FECB00"), Style.BLACK));
        STYLES.put("UDistrict", new Style(Style.parseColor("#007934"), Style.WHITE));
        STYLES.put("UEast London", new Style(Style.parseColor("#FFA100"), Style.WHITE));
        STYLES.put("UHammersmith & City", new Style(Style.parseColor("#C5858F"), Style.BLACK));
        STYLES.put("UJubilee", new Style(Style.parseColor("#818A8F"), Style.WHITE));
        STYLES.put("UMetropolitan", new Style(Style.parseColor("#850057"), Style.WHITE));
        STYLES.put("UNorthern", new Style(Style.BLACK, Style.WHITE));
        STYLES.put("UPiccadilly", new Style(Style.parseColor("#0018A8"), Style.WHITE));
        STYLES.put("UVictoria", new Style(Style.parseColor("#00A1DE"), Style.WHITE));
        STYLES.put("UWaterloo & City", new Style(Style.parseColor("#76D2B6"), Style.BLACK));

        STYLES.put("SDLR", new Style(Style.parseColor("#00B2A9"), Style.WHITE));
        STYLES.put("SLO", new Style(Style.parseColor("#f46f1a"), Style.WHITE));

        //  here goes nothing with Lizz
        STYLES.put("SElizabeth Line", new Style(Style.parseColor("#6950a1"), Style.WHITE));

        STYLES.put("TTramlink 1", new Style(Style.rgb(193, 215, 46), Style.WHITE));
        STYLES.put("TTramlink 2", new Style(Style.rgb(193, 215, 46), Style.WHITE));
        STYLES.put("TTramlink 3", new Style(Style.rgb(124, 194, 66), Style.BLACK));
    }
}
