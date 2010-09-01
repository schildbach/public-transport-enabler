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
public class BahnProviderTest
{
	@Test
	public void connectionUebergang()
	{
		assertFineConnectionDetails("<span class=\"bold\">Berlin Hbf</span><br />\n" //
				+ "&#220;bergang\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">Berlin-Lichtenberg</span><br />");
	}

	@Test
	public void departureWithPlatform()
	{
		assertFineDepartures("<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/731061/244203/590672/51649/80/si=8100352&amp;bt=dep&amp;ti=10:42&amp;pt=10:42&amp;p=1111111111&amp;date=01.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">S      1</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "G&#228;nserndorf\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">10:42</span>Gl. 1");
	}

	private void assertFineConnectionDetails(String s)
	{
		Matcher m = BahnProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}

	private void assertFineDepartures(String s)
	{
		Matcher m = BahnProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}
}
