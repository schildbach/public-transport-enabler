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
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("https://efa.mvv-muenchen.de/mobile/");

    public MvvProvider() {
        this(API_BASE);
    }

    public MvvProvider(final HttpUrl apiBase) {
        super(NetworkId.MVV, apiBase);
        setIncludeRegionId(false);
        setRequestUrlEncoding(Charsets.UTF_8);
        setStyles(STYLES);
        setSessionCookieName("SIDefaalt"); // SIDefa
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("Mittelrheinbahn (trans regio)".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "MiRhBa");
            if ("Süd-Thüringen-Bahn".equals(longName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "STB");
            if ("agilis".equals(longName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "agilis");
            if ("SBB".equals(trainName))
                return new Line(id, network, Product.REGIONAL_TRAIN, "SBB");
            if ("A".equals(trainNum))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "A");
            if ("DB AG".equals(trainName))
                return new Line(id, network, null, symbol);
        } else if ("1".equals(mot)) {
            if ("S".equals(symbol) && "Pendelverkehr".equals(name))
                return new Line(id, network, Product.SUBURBAN_TRAIN, "S⇆");
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }

    private static final Pattern P_POSITION = Pattern.compile("(Fern|Regio|S-Bahn|U-Bahn|U\\d(?:/U\\d)*)\\s+(.*)");

    @Override
    protected Position parsePosition(final String position) {
        if (position == null)
            return null;

        final Matcher m = P_POSITION.matcher(position);
        if (m.matches()) {
            final char t = m.group(1).charAt(0);
            final Position p = super.parsePosition(m.group(2));
            if (t == 'S' || t == 'U')
                return new Position(p.name + "(" + t + ")", p.section);
            else
                return p;
        }

        return super.parsePosition(position);
    }

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        STYLES.put("R", new Style(Shape.RECT, Style.parseColor("#36397f"), Style.WHITE));

        STYLES.put("SS1", new Style(Shape.CIRCLE, Style.parseColor("#16bae7"), Style.WHITE));
        STYLES.put("SS2", new Style(Shape.CIRCLE, Style.parseColor("#76b82a"), Style.WHITE));
        STYLES.put("SS3", new Style(Shape.CIRCLE, Style.parseColor("#951b81"), Style.WHITE));
        STYLES.put("SS4", new Style(Shape.CIRCLE, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("SS6", new Style(Shape.CIRCLE, Style.parseColor("#00975f"), Style.WHITE));
        STYLES.put("SS7", new Style(Shape.CIRCLE, Style.parseColor("#943126"), Style.WHITE));
        STYLES.put("SS8", new Style(Shape.CIRCLE, Style.BLACK, Style.parseColor("#ffcc00")));
        STYLES.put("SS18",
                new Style(Shape.CIRCLE, Style.parseColor("#16bae7"), Style.parseColor("#f0aa00"), Style.WHITE, 0));
        STYLES.put("SS20", new Style(Shape.CIRCLE, Style.parseColor("#ea516d"), Style.WHITE));

        STYLES.put("T12", new Style(Shape.RECT, Style.parseColor("#96368b"), Style.WHITE));
        STYLES.put("T15", new Style(Shape.RECT, Style.WHITE, Style.parseColor("#f1919c"), Style.parseColor("#f1919c")));
        STYLES.put("T16", new Style(Shape.RECT, Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("T17", new Style(Shape.RECT, Style.parseColor("#8b563e"), Style.WHITE));
        STYLES.put("T18", new Style(Shape.RECT, Style.parseColor("#13a538"), Style.WHITE));
        STYLES.put("T19", new Style(Shape.RECT, Style.parseColor("#e30613"), Style.WHITE));
        STYLES.put("T20", new Style(Shape.RECT, Style.parseColor("#16bae7"), Style.WHITE));
        STYLES.put("T21", new Style(Shape.RECT, Style.parseColor("#bc7a00"), Style.WHITE));
        STYLES.put("T22", new Style(Shape.RECT, Style.WHITE, Style.parseColor("#16bae7"), Style.parseColor("#16bae7")));
        STYLES.put("T23", new Style(Shape.RECT, Style.parseColor("#bccf00"), Style.WHITE));
        STYLES.put("T25", new Style(Shape.RECT, Style.parseColor("#f1919c"), Style.WHITE));
        STYLES.put("T27", new Style(Shape.RECT, Style.parseColor("#f7a600"), Style.WHITE));
        STYLES.put("T28", new Style(Shape.RECT, Style.WHITE, Style.parseColor("#f7a600"), Style.parseColor("#f7a600")));
        STYLES.put("T29", new Style(Shape.RECT, Style.WHITE, Style.parseColor("#e30613"), Style.parseColor("#e30613")));
        STYLES.put("T31",
                new Style(Shape.RECT, Style.parseColor("#e30613"), Style.parseColor("#bc7a00"), Style.WHITE, 0));
        STYLES.put("T38",
                new Style(Shape.RECT, Style.parseColor("#1fa22e"), Style.parseColor("#23bae2"), Style.WHITE, 0));
        STYLES.put("TN17", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("TN19", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("TN20", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));
        STYLES.put("TN27", new Style(Shape.RECT, Style.parseColor("#999999"), Style.parseColor("#ffff00")));

        STYLES.put("UU1", new Style(Shape.RECT, Style.parseColor("#52822f"), Style.WHITE));
        STYLES.put("UU2", new Style(Shape.RECT, Style.parseColor("#c20831"), Style.WHITE));
        STYLES.put("UU2E", new Style(Shape.RECT, Style.parseColor("#c20831"), Style.WHITE));
        STYLES.put("UU3", new Style(Shape.RECT, Style.parseColor("#ec6726"), Style.WHITE));
        STYLES.put("UU4", new Style(Shape.RECT, Style.parseColor("#00a984"), Style.WHITE));
        STYLES.put("UU5", new Style(Shape.RECT, Style.parseColor("#bc7a00"), Style.WHITE));
        STYLES.put("UU6", new Style(Shape.RECT, Style.parseColor("#0065ae"), Style.WHITE));
        STYLES.put("UU7",
                new Style(Shape.RECT, Style.parseColor("#52822f"), Style.parseColor("#c20831"), Style.WHITE, 0));
        STYLES.put("UU8",
                new Style(Shape.RECT, Style.parseColor("#c20831"), Style.parseColor("#ec6726"), Style.WHITE, 0));

        STYLES.put("B", new Style(Shape.RECT, Style.parseColor("#005262"), Style.WHITE));
        STYLES.put("BX", new Style(Shape.RECT, Style.parseColor("#4e917a"), Style.WHITE));
    }

    @Override
    public Point[] getArea() {
        return new Point[] { Point.fromDouble(48.140377, 11.560643) };
    }
}
