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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import de.schildbach.pte.dto.Point;

/**
 * @author Andreas Schildbach
 */
public class PolylineFormatTest {
    @Test
    public void test() {
        final List<Point> polyline = PolylineFormat.decode(
                "}qfeHyn|bBnBdA\\R]xBzA|@r@f@u@hCWS{@bCe@t@e@v@h@vCIFu@`@MPDJ@L?NAPIZXf@|@`Br@pAHLZp@~@jBbArBbBjDLTTd@fAzBcFnH[d@Vf@iA`BWb@t@zAb@~@LTNNdCzE~A{BAA??");
        assertEquals(44, polyline.size());
        assertEquals(Point.fromDouble(48.2078300, 16.3711700), polyline.get(0));
        assertEquals(Point.fromDouble(48.2051400, 16.3579600), polyline.get(43));
    }
}
