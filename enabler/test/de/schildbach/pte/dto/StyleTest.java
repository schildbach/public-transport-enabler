/*
 * Copyright 2014-2015 the original author or authors.
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

package de.schildbach.pte.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class StyleTest {
    @Test
    public void roundTripRGB() {
        final int color = Style.parseColor("#123456");
        assertEquals(color, Style.rgb(Style.red(color), Style.green(color), Style.blue(color)));
    }

    @Test
    public void parseColor() {
        assertEquals(0x11223344, Style.parseColor("#11223344"));
    }

    @Test
    public void parseColor_noOverflow() {
        assertEquals(0xffffffff, Style.parseColor("#ffffffff"));
    }

    @Test
    public void parseColor_amendAlpha() {
        assertEquals(0xff000000, Style.parseColor("#000000"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseColor_failTooShort() {
        Style.parseColor("#");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseColor_failNotANumber() {
        Style.parseColor("#11111z");
    }

    @Test
    public void deriveForegroundColorForLightBackground() {
        final int acacia = Style.rgb(205, 200, 63);
        final int lilac = Style.rgb(197, 163, 202);
        final int mint = Style.rgb(121, 187, 146);
        final int ochre = Style.rgb(223, 176, 57);
        final int orange = Style.rgb(222, 139, 83);
        final int ranunculus = Style.rgb(242, 201, 49);
        final int rose = Style.rgb(223, 154, 177);
        final int vinca = Style.rgb(137, 199, 214);

        assertEquals(Style.BLACK, Style.deriveForegroundColor(acacia));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(lilac));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(mint));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(ochre));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(orange));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(ranunculus));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(rose));
        assertEquals(Style.BLACK, Style.deriveForegroundColor(vinca));
    }

    @Test
    public void deriveForegroundColorForDarkBackground() {
        final int azure = Style.rgb(33, 110, 180);
        final int brown = Style.rgb(141, 101, 56);
        final int iris = Style.rgb(103, 50, 142);
        final int parme = Style.rgb(187, 77, 152);
        final int sapin = Style.rgb(50, 142, 91);

        assertEquals(Style.WHITE, Style.deriveForegroundColor(azure));
        assertEquals(Style.WHITE, Style.deriveForegroundColor(brown));
        assertEquals(Style.WHITE, Style.deriveForegroundColor(iris));
        assertEquals(Style.WHITE, Style.deriveForegroundColor(parme));
        assertEquals(Style.WHITE, Style.deriveForegroundColor(sapin));
    }
}
