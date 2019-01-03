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
import java.util.regex.Matcher;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class AvvAachenProvider extends AbstractHafasClientInterfaceProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://auskunft.avv.de/bin/mgate.exe");
    private static final Product[] PRODUCTS_MAP = { Product.REGIONAL_TRAIN, Product.HIGH_SPEED_TRAIN,
            Product.HIGH_SPEED_TRAIN, Product.BUS, Product.SUBURBAN_TRAIN, Product.SUBWAY, Product.TRAM, Product.BUS,
            Product.BUS, Product.ON_DEMAND, Product.FERRY };

    protected static final Map<String, Style> STYLES = new HashMap<>();


    public AvvAachenProvider(final String jsonApiAuthorization) {
        super(NetworkId.AVV_AACHEN, API_BASE, PRODUCTS_MAP);
        setApiVersion("1.16");
	    setStyles(STYLES);
	    setApiClient("{\"id\":\"AVV_AACHEN\",\"type\":\"AND\"}");
        setApiAuthorization(jsonApiAuthorization);
    }

    @Override
    protected String[] splitStationName(final String name) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };
        return super.splitStationName(name);
    }

    @Override
    protected String[] splitPOI(final String poi) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(poi);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };
        return super.splitStationName(poi);
    }

    @Override
    protected String[] splitAddress(final String address) {
        final Matcher m = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
        if (m.matches())
            return new String[] { m.group(1), m.group(2) };
        return super.splitStationName(address);
    }

    @Override
    protected Line newLine(final String operator, final Product product, final @Nullable String name,
                           final @Nullable String shortName, final @Nullable String number) {
		final String normalizedName;
		if (product == Product.ON_DEMAND && name.startsWith("ALT")) { // bsp. ALT74ALT -> 74ALT
			normalizedName = name.substring(3);
			return new Line(null, operator, product, normalizedName, lineStyle(operator, product, normalizedName));

		} else if (product == Product.REGIONAL_TRAIN && name.startsWith("IC")) { // AVV hat belgische und niederländische ICs als Regionalbahn drin
			return new Line(null, operator, Product.HIGH_SPEED_TRAIN, name, lineStyle(operator, Product.HIGH_SPEED_TRAIN, name));
		}

		return super.newLine(operator, product, name, shortName, number);
	}


    static {
        STYLES.put("BSEV", new Style(Style.parseColor("#ED028C"), Style.GRAY, Style.WHITE, 0));

        // braun
        STYLES.put("B3", new Style(Style.parseColor("#CF9C46"), Style.WHITE));
        STYLES.put("B3A", new Style(Style.parseColor("#CF9C46"), Style.WHITE));
        STYLES.put("B3B", new Style(Style.parseColor("#CF9C46"), Style.WHITE));
        STYLES.put("B13", new Style(Style.parseColor("#CF9C46"), Style.WHITE));
        STYLES.put("B13A", new Style(Style.parseColor("#CF9C46"), Style.WHITE));
        STYLES.put("B13B", new Style(Style.parseColor("#CF9C46"), Style.WHITE));

        // rot
        STYLES.put("B4", new Style(Style.RED, Style.WHITE));
        STYLES.put("B16", new Style(Style.RED, Style.WHITE));

        // pink
        STYLES.put("B1", new Style(Style.parseColor("#ED028C"), Style.WHITE));
        STYLES.put("B11", new Style(Style.parseColor("#ED028C"), Style.WHITE));
        STYLES.put("B21", new Style(Style.parseColor("#ED028C"), Style.WHITE));
        STYLES.put("B41", new Style(Style.parseColor("#ED028C"), Style.WHITE));
        STYLES.put("B51", new Style(Style.parseColor("#ED028C"), Style.WHITE));

        // rosa
        STYLES.put("B33", new Style(Style.parseColor("#F499C2"), Style.WHITE));
        STYLES.put("B34", new Style(Style.parseColor("#F499C2"), Style.WHITE));
        STYLES.put("B54", new Style(Style.parseColor("#F499C2"), Style.WHITE));
        STYLES.put("B73", new Style(Style.parseColor("#F499C2"), Style.WHITE));

        // blau grau
        STYLES.put("B5", new Style(Style.parseColor("#6F92AE"), Style.WHITE));
        STYLES.put("B45", new Style(Style.parseColor("#6F92AE"), Style.WHITE));

        // blau
        STYLES.put("B15", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
        STYLES.put("B25", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
        STYLES.put("B35", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
        STYLES.put("B43", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
        STYLES.put("B55", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
        STYLES.put("B65", new Style(Style.parseColor("#00AEEF"), Style.WHITE));

        // hellblau
        STYLES.put("B63", new Style(Style.parseColor("#6BCFF6"), Style.WHITE));
        STYLES.put("B66", new Style(Style.parseColor("#6BCFF6"), Style.WHITE));

        // lila
        STYLES.put("B7", new Style(Style.parseColor("#802990"), Style.WHITE));
        STYLES.put("B17", new Style(Style.parseColor("#802990"), Style.WHITE));
        STYLES.put("B27", new Style(Style.parseColor("#802990"), Style.WHITE));
        STYLES.put("B37", new Style(Style.parseColor("#802990"), Style.WHITE));
        STYLES.put("B47", new Style(Style.parseColor("#802990"), Style.WHITE));

        // braun?
        STYLES.put("B14", new Style(Style.parseColor("#B96730"), Style.WHITE));
        STYLES.put("B24", new Style(Style.parseColor("#B96730"), Style.WHITE));
        STYLES.put("B74", new Style(Style.parseColor("#802990"), Style.WHITE));

        // orange
        STYLES.put("BAL1", new Style(Style.parseColor("#F7931D"), Style.WHITE));
        STYLES.put("44", new Style(Style.parseColor("#F7931D"), Style.WHITE));
        STYLES.put("67", new Style(Style.parseColor("#F7931D"), Style.WHITE));

        // navy
        STYLES.put("Wü 1", new Style(Style.parseColor("#303A74"), Style.WHITE));

        // grün, hellgrün und noch ein grün
        STYLES.put("B31", new Style(Style.parseColor("#9FD05F"), Style.WHITE));
        STYLES.put("B36", new Style(Style.parseColor("#9FD05F"), Style.WHITE));

        STYLES.put("B53", new Style(Style.parseColor("#00B59D"), Style.WHITE));

        STYLES.put("B50", new Style(Style.parseColor("#00A54F"), Style.WHITE));
        STYLES.put("B70", new Style(Style.parseColor("#00A54F"), Style.WHITE));
        STYLES.put("B80", new Style(Style.parseColor("#00A54F"), Style.WHITE));

        // gelb
        STYLES.put("B2", new Style(Style.YELLOW, Style.WHITE));
        STYLES.put("B12", new Style(Style.YELLOW, Style.WHITE));
        STYLES.put("B22", new Style(Style.YELLOW, Style.WHITE));
        STYLES.put("B23", new Style(Style.YELLOW, Style.WHITE));

        // schnellbusse
        STYLES.put("B103", new Style(Style.Shape.ROUNDED, Style.parseColor("#CF9C46"), Style.WHITE, Style.parseColor("#444444")));

        STYLES.put("B125", new Style(Style.Shape.ROUNDED, Style.parseColor("#00AEEF"), Style.WHITE, Style.parseColor("#444444")));
        STYLES.put("B135", new Style(Style.Shape.ROUNDED, Style.parseColor("#00AEEF"), Style.WHITE, Style.parseColor("#444444")));
        STYLES.put("B147", new Style(Style.Shape.ROUNDED, Style.parseColor("#802990"), Style.WHITE, Style.parseColor("#444444")));

        STYLES.put("B151", new Style(Style.Shape.ROUNDED, Style.parseColor("#ED028C"), Style.WHITE, Style.parseColor("#444444")));

        STYLES.put("B173", new Style(Style.Shape.ROUNDED, Style.parseColor("#F499C2"), Style.WHITE, Style.parseColor("#444444")));

        STYLES.put("B220", new Style(Style.Shape.ROUNDED, Style.parseColor("#ED028C"), Style.WHITE, Style.parseColor("#444444")));
        STYLES.put("BSB20", new Style(Style.Shape.ROUNDED, Style.parseColor("#ED028C"), Style.WHITE, Style.parseColor("#444444")));

        STYLES.put("BSB63", new Style(Style.Shape.ROUNDED, Style.parseColor("#6BCFF6"), Style.WHITE, Style.parseColor("#444444")));
        STYLES.put("BSB66", new Style(Style.Shape.ROUNDED, Style.parseColor("#6BCFF6"), Style.WHITE, Style.parseColor("#444444")));

//        // regionalexpress
//        STYLES.put("RRE1", new Style(Style.Shape.RECT, Style.parseColor("#F44336"), Style.WHITE));
//        STYLES.put("RRE4", new Style(Style.Shape.RECT, Style.parseColor("#F7931D"), Style.WHITE));
//        STYLES.put("RRE9", new Style(Style.Shape.RECT, Style.parseColor("#802990"), Style.WHITE));
//        // euregiobahn blau
//        STYLES.put("RRB20", new Style(Style.Shape.RECT, Style.parseColor("#2196F3"), Style.WHITE));

    }

}
