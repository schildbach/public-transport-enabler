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

package de.schildbach.pte.util;

import java.util.ArrayList;
import java.util.List;

import de.schildbach.pte.dto.Point;

/**
 * <p>
 * Implementation of the
 * <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Encoded Polyline
 * Algorithm Format</a>.
 * </p>
 * 
 * @author Andreas Schildbach
 */
public final class PolylineFormat {
    public static List<Point> decode(final String encodedPolyline) {
        final int len = encodedPolyline.length();
        final List<Point> path = new ArrayList<>(len / 2);

        int lat = 0;
        int lon = 0;
        int index = 0;
        while (index < len) {
            int latResult = 1;
            int latShift = 0;
            int latB;
            do {
                latB = encodedPolyline.charAt(index++) - 63 - 1;
                latResult += latB << latShift;
                latShift += 5;
            } while (latB >= 0x1f);
            lat += (latResult & 1) != 0 ? ~(latResult >> 1) : (latResult >> 1);

            int lonResult = 1;
            int lonShift = 0;
            int lonB;
            do {
                lonB = encodedPolyline.charAt(index++) - 63 - 1;
                lonResult += lonB << lonShift;
                lonShift += 5;
            } while (lonB >= 0x1f);
            lon += (lonResult & 1) != 0 ? ~(lonResult >> 1) : (lonResult >> 1);

            path.add(Point.from1E5(lat, lon));
        }
        return path;
    }
}
