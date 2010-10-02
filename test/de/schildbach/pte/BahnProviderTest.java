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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class BahnProviderTest
{
	@Test
	public void coarseDeparture()
	{
		assertCoarseDepartures("" //
				+ "<Journey fpTime=\"17:45\" fpDate=\"02.10.10\" \n" //
				+ "delay=\"k.A\" \n" //
				+ "platform =\"1\" \n" //
				+ "targetLoc=\"Berlin Südkreuz (S)\" \n" //
				+ "prod=\"S     41\" \n" //
				+ "depStation=\"Berlin Sonnenallee\"\n" //
				+ "delayReason=\" \"\n" //
				+ "/>\n");
	}

	@Test
	public void coarseDepartureWithoutTrailingNewLine()
	{
		assertCoarseDepartures("" //
				+ "<Journey fpTime=\"17:45\" fpDate=\"02.10.10\" \n" //
				+ "delay=\"k.A\" \n" //
				+ "platform =\"1\" \n" //
				+ "targetLoc=\"Berlin Südkreuz (S)\" \n" //
				+ "prod=\"S     41\" \n" //
				+ "depStation=\"Berlin Sonnenallee\"\n" //
				+ "delayReason=\" \"\n" //
				+ "/>");
	}

	@Test
	public void coarseDepartureWithInvalidClosingBracket()
	{
		assertCoarseDepartures("" //
				+ "<Journey fpTime=\"17:45\" fpDate=\"02.10.10\" \n" //
				+ "delay=\"k.A\" \n" //
				+ "platform =\"1\" \n" //
				+ "targetLoc=\"Berlin Südkreuz (S)\" \n" //
				+ "prod=\"S     41\" \n" //
				+ "dir=\"Ringbahn ->\"\n" //
				+ "depStation=\"Berlin Sonnenallee\"\n" //
				+ "delayReason=\" \"\n" //
				+ "/>\n");
	}

	@Test
	public void connectionUebergang()
	{
		assertFineConnectionDetails("" //
				+ "<span class=\"bold\">Berlin Hbf</span><br />\n" //
				+ "&#220;bergang\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">Berlin-Lichtenberg</span><br />\n");
	}

	@Test
	public void newDepartureWithMessage()
	{
		final Matcher m = assertFineDepartures("" //
				+ "fpTime=\"21:10\" fpDate=\"01.10.10\" \n" //
				+ "delay=\"cancel\" \n" //
				+ "platform =\"1\" \n" //
				+ "targetLoc=\"Magdeburg Hbf\" \n" //
				+ "prod=\"RE 38090\" \n" //
				+ "delayReason=\" Notarzteinsatz am Gleis\"\n");

		assertNotNull(m.group(4)); // position
		assertNotNull(m.group(9)); // message
	}

	private void assertFineConnectionDetails(final String s)
	{
		Matcher m = BahnProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());

		// ParserUtils.printGroups(m);
	}

	private Matcher assertCoarseDepartures(final String s)
	{
		Matcher m = BahnProvider.P_DEPARTURES_COARSE.matcher(s);
		assertTrue(m.find());
		assertFineDepartures(m.group(1));

		return m;
	}

	private Matcher assertFineDepartures(final String s)
	{
		Matcher m = BahnProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());

		// ParserUtils.printGroups(m);

		assertNotNull(m.group(1)); // time
		assertNotNull(m.group(2)); // date
		assertNotNull(m.group(6)); // destination
		assertNotNull(m.group(7)); // line

		return m;
	}
}
