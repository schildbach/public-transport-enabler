/*
 * Copyright 2017 the original author or authors.
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

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

import okhttp3.HttpUrl;

public class AustraliaProvider extends AbstractNavitiaProvider {

    public static final String NETWORK_PTV = "PTV - Public Transport Victoria";
    public static final String NETWORK_TAS = "Metro Tasmania";
    public static final String NETWORK_QLD = "TransLink";
    public static final String NETWORK_SA = "Adelaide Metro";
    public static final String NETWORK_WA = "Transperth";

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // Melbourne train networks.
        // https://static.ptv.vic.gov.au/Maps/1482457134/PTV_Train-Network-Map_2017.pdf
        STYLES.put(NETWORK_PTV + "|SBelgrave", new Style(Style.Shape.RECT, Style.parseColor("#094B8D"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SLilydale", new Style(Style.Shape.RECT, Style.parseColor("#094B8D"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SAlamein", new Style(Style.Shape.RECT, Style.parseColor("#094B8D"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SGlen Waverly",
                new Style(Style.Shape.RECT, Style.parseColor("#094B8D"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SSunbury", new Style(Style.Shape.RECT, Style.parseColor("#FFB531"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|SCraigieburn",
                new Style(Style.Shape.RECT, Style.parseColor("#FFB531"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|SUpfield", new Style(Style.Shape.RECT, Style.parseColor("#FFB531"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|SSouth Morang",
                new Style(Style.Shape.RECT, Style.parseColor("#E42B23"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SHurstbridge",
                new Style(Style.Shape.RECT, Style.parseColor("#E42B23"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SPakenham", new Style(Style.Shape.RECT, Style.parseColor("#16B4E8"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SCranbourne", new Style(Style.Shape.RECT, Style.parseColor("#16B4E8"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SFrankston", new Style(Style.Shape.RECT, Style.parseColor("#149943"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SWerribee", new Style(Style.Shape.RECT, Style.parseColor("#149943"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|SWilliamstown",
                new Style(Style.Shape.RECT, Style.parseColor("#149943"), Style.WHITE));

        // Truncated version of "Sandringham". Not sure if this is a bug/limitation in either Navitia or the
        // GTFS feed from PTV.
        STYLES.put(NETWORK_PTV + "|SSandringha", new Style(Style.Shape.RECT, Style.parseColor("#FC7EBB"), Style.BLACK));

        // Difficult to test this, because the line is open only for select periods of the year (e.g. for the
        // Melbourne Show in September) and at time of writing, the GTFS feed did not have data up until then.
        STYLES.put(NETWORK_PTV + "|SFlemington Racecourse",
                new Style(Style.Shape.RECT, Style.parseColor("#9A9B9F"), Style.BLACK));

        // Melbourne Trams.
        // https://static.ptv.vic.gov.au/Maps/1493356745/PTV_Tram-Network-Map_2017.pdf
        STYLES.put(NETWORK_PTV + "|T1", new Style(Style.Shape.RECT, Style.parseColor("#B8C53A"), Style.BLACK));

        // The GTFS feed seems to combine 3 + 3a like this, but for completeness we include 3 and 3a
        // separately too.
        STYLES.put(NETWORK_PTV + "|T3", new Style(Style.Shape.RECT, Style.parseColor("#87D9F2"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T3a", new Style(Style.Shape.RECT, Style.parseColor("#87D9F2"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T3/3a", new Style(Style.Shape.RECT, Style.parseColor("#87D9F2"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T5", new Style(Style.Shape.RECT, Style.parseColor("#F44131"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T6", new Style(Style.Shape.RECT, Style.parseColor("#004969"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T11", new Style(Style.Shape.RECT, Style.parseColor("#7ECBA4"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T12", new Style(Style.Shape.RECT, Style.parseColor("#008A99"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T16", new Style(Style.Shape.RECT, Style.parseColor("#FFD86C"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T19", new Style(Style.Shape.RECT, Style.parseColor("#87457A"), Style.WHITE)); // Night
        STYLES.put(NETWORK_PTV + "|T30", new Style(Style.Shape.RECT, Style.parseColor("#3343A3"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T35", new Style(Style.Shape.RECT, Style.parseColor("#6E351C"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T48", new Style(Style.Shape.RECT, Style.parseColor("#45474C"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T57", new Style(Style.Shape.RECT, Style.parseColor("#45C6CE"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T58", new Style(Style.Shape.RECT, Style.parseColor("#878E94"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T59", new Style(Style.Shape.RECT, Style.parseColor("#438459"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T64", new Style(Style.Shape.RECT, Style.parseColor("#2EB070"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T67", new Style(Style.Shape.RECT, Style.parseColor("#B47962"), Style.WHITE)); // Night
        STYLES.put(NETWORK_PTV + "|T70", new Style(Style.Shape.RECT, Style.parseColor("#FC8BC1"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T72", new Style(Style.Shape.RECT, Style.parseColor("#97BAA6"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T75", new Style(Style.Shape.RECT, Style.parseColor("#00A8DF"), Style.WHITE)); // Night
        STYLES.put(NETWORK_PTV + "|T78", new Style(Style.Shape.RECT, Style.parseColor("#7B7EC0"), Style.WHITE));
        STYLES.put(NETWORK_PTV + "|T82", new Style(Style.Shape.RECT, Style.parseColor("#BCD649"), Style.BLACK));
        STYLES.put(NETWORK_PTV + "|T86", new Style(Style.Shape.RECT, Style.parseColor("#FFB730"), Style.BLACK)); // Night
        STYLES.put(NETWORK_PTV + "|T96", new Style(Style.Shape.RECT, Style.parseColor("#F2428F"), Style.WHITE)); // Night
        STYLES.put(NETWORK_PTV + "|T109", new Style(Style.Shape.RECT, Style.parseColor("#FF7B24"), Style.WHITE)); // Night

        STYLES.put(NETWORK_PTV + "|B", new Style(Style.Shape.RECT, Style.parseColor("#EA8D1E"), Style.WHITE));

        // NOTE: This is a work around for poor GTFS data. We should instead say "All REGIONAL_TRAINs are
        // purple", but the GTFS feed from Navitia instead returns rural trains as SUBURBAN_TRAINs. Given we
        // have already provided colours for all suburban trains above, this more general statement about
        // suburban trains results in all regional trains being coloured purple, as intented.
        STYLES.put(NETWORK_PTV + "|S", new Style(Style.Shape.RECT, Style.parseColor("#782F9A"), Style.WHITE));

        // Sydney train networks.
        // http://www.sydneytrains.info/stations/pdf/suburban_map.pdf
        // Navitia is not returning enough info in "display_informations" to colourise correctly.
        // Specifically, they are not returning "code" which usually is the display name of the line.
        // At any rate, the EFA provider for Sydney is likely going to be better than this Navitia provider
        // anyway.

        // Adelaide train/tram networks.
        // These are already colourised correctly by the GTFS data given to Navitia.
        // But for reference, the map is available at https://www.adelaidemetro.com.au/Timetables-Maps/Maps.

        // Brisbane train/tram/bus/ferry networks.
        // These are already colourised correctly by the GTFS data given to Navitia.
        // But for reference, the maps are available at https://translink.com.au/plan-your-journey/maps.

        // Perth train/bus/ferry networks.
        // These are already colourised correctly by the GTFS data given to Navitia.
        // The styles do not include "display_informations" with a proper "code" though, so the names are not
        // displayed. But for reference, the maps are available at
        // http://www.transperth.wa.gov.au/Journey-Planner/Network-Maps.

        // Tasmania bus networks.
        // Somewhat colourised in Navitia (e.g. Launceston has green buses), but it is incorrect (e.g.
        // Launceston should have all sorts of different coloured buses). Maps are available at
        // https://www.metrotas.com.au/timetables/.
    }

    public AustraliaProvider(final HttpUrl apiBase, final String authorization) {
        super(NetworkId.AUSTRALIA, apiBase, authorization);

        setTimeZone("Australia/Melbourne");
        setStyles(STYLES);
    }

    public AustraliaProvider(final String authorization) {
        super(NetworkId.AUSTRALIA, authorization);

        setTimeZone("Australia/Melbourne");
        setStyles(STYLES);
    }

    @Override
    public String region() {
        return "au";
    }

    @Override
    protected Style getLineStyle(String network, Product product, String code, String backgroundColor,
            String foregroundColor) {
        final Style overridenStyle = lineStyle(network, product, code);
        if (overridenStyle != Standard.STYLES.get(product))
            return overridenStyle;
        else
            return super.getLineStyle(network, product, code, backgroundColor, foregroundColor);
    }
}
