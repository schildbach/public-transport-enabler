/*
 * Copyright 2014 the original author or authors.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class StyleTest
{
	@Test
	public void roundTripRGB()
	{
		final int color = Style.parseColor("#123456");
		assertEquals(color, Style.rgb(Style.red(color), Style.green(color), Style.blue(color)));
	}

	@Test
	public void foregroudColorForClearBackground ()
	{
		final int acacia = Style.rgb (205, 200, 63);
		final int lilac = Style.rgb (197, 163, 202);
		final int mint = Style.rgb (121, 187, 146);
		final int ochre = Style.rgb (223, 176, 57);
		final int orange = Style.rgb (222, 139, 83);
		final int ranunculus = Style.rgb (242, 201, 49);
		final int rose = Style.rgb (223, 154, 177);
		final int vinca = Style.rgb (137, 199, 214);

		assertEquals (Style.BLACK, Style.computeForegroundColor (acacia));
		assertEquals (Style.BLACK, Style.computeForegroundColor (lilac));
		assertEquals (Style.BLACK, Style.computeForegroundColor (mint));
		assertEquals (Style.BLACK, Style.computeForegroundColor (ochre));
		assertEquals (Style.BLACK, Style.computeForegroundColor (orange));
		assertEquals (Style.BLACK, Style.computeForegroundColor (ranunculus));
		assertEquals (Style.BLACK, Style.computeForegroundColor (rose));
		assertEquals (Style.BLACK, Style.computeForegroundColor (vinca));
	}

	@Test
	public void foregroudColorForDarkBackground ()
	{
		final int azure = Style.rgb (33, 110, 180);
		final int brown = Style.rgb (141, 101, 56);
		final int iris = Style.rgb (103, 50, 142);
		final int parme = Style.rgb (187, 77, 152);
		final int sapin = Style.rgb (50, 142, 91);

		assertEquals (Style.WHITE, Style.computeForegroundColor (azure));
		assertEquals (Style.WHITE, Style.computeForegroundColor (brown));
		assertEquals (Style.WHITE, Style.computeForegroundColor (iris));
		assertEquals (Style.WHITE, Style.computeForegroundColor (parme));
		assertEquals (Style.WHITE, Style.computeForegroundColor (sapin));
	}
}
