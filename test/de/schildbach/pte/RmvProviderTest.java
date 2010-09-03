/*
 * Copyright 2010 the original author or authors.
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
		assertFineDepartures("<b>Bus 42  </b>\n" //
				+ "&gt;&gt;\n" //
				+ "Frankfurt (Main) Enkheim\n" //
				+ "<br />\n" //
				+ "<b>20:21</b>\n" //
				+ "keine Prognose verf&#252;gbar\n" //
				+ "<span class=\"red\">heute Gl. Enkheim</span><br />\n");
	}

	private void assertFineDepartures(String s)
	{
		Matcher m = RmvProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}
}
