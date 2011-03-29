/*
 * Copyright 2010, 2011 the original author or authors.
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

package de.schildbach.pte;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class RmvProviderTest
{
	@Test
	public void departureWithNoPrognosisMessage()
	{
		final Matcher m = assertFineDepartures("<b>Bus 42  </b>\n" //
				+ "&gt;&gt;\n" //
				+ "Frankfurt (Main) Enkheim\n" //
				+ "<br />\n" //
				+ "<b>20:21</b>\n" //
				+ "keine Prognose verf&#252;gbar\n" //
				+ "<span class=\"red\">heute Gl. Enkheim</span><br />\n");

		assertNotNull(m.group(5)); // predictedPosition
	}

	@Test
	public void departureWithMessage()
	{
		final Matcher m = assertFineDepartures("<b>Bus 274 </b>\n" //
				+ "&gt;&gt;\n" //
				+ "Bad Schwalbach Kurhaus\n" //
				+ "<br />\n" //
				+ "<b>15:47</b>\n" //
				+ "<span class=\"red\">Zug f&#228;llt aus</span>\n");

		assertNotNull(m.group(7)); // message
	}

	private Matcher assertFineDepartures(String s)
	{
		Matcher m = RmvProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());

		// ParserUtils.printGroups(m);

		assertNotNull(m.group(1)); // line
		assertNotNull(m.group(2)); // destination
		assertNotNull(m.group(3)); // time

		return m;
	}
}
