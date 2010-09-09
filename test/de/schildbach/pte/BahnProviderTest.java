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
	public void connectionUebergang()
	{
		assertFineConnectionDetails("" //
				+ "<span class=\"bold\">Berlin Hbf</span><br />\n" //
				+ "&#220;bergang\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">Berlin-Lichtenberg</span><br />");
	}

	@Test
	public void departureWithPlatform()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/731061/244203/590672/51649/80/si=8100352&amp;bt=dep&amp;ti=10:42&amp;pt=10:42&amp;p=1111111111&amp;date=01.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">S      1</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "G&#228;nserndorf\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">10:42</span>Gl. 1");

		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureWithOnTime()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/438441/245165/958/145668/80/si=8011160&amp;bt=dep&amp;ti=21:47&amp;pt=21:47&amp;p=1111101&amp;date=05.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">RE 38148</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "Rathenow\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">21:58</span>&nbsp;<span class=\"green bold\">p&#252;nktl.</span>,&nbsp;Gl. 13");

		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureWithMessage()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/551037/330609/12448/177455/80/si=405341&amp;bt=dep&amp;ti=07:08&amp;pt=07:08&amp;p=1111111111&amp;date=06.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">ICE  824</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "Dortmund Hbf\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">07:02</span>&nbsp;<span class=\"red\">ca. +5</span>, <span class=\"red\">F&#228;hrt heute nur bis&nbsp;D&#252;sseldorf Hbf</span>,&nbsp;Gl. 10");

		assertNotNull(m.group(6)); // message
		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureUpdatedPosition()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/492282/296365/292060/18065/80/si=8000320&amp;bt=dep&amp;ti=17:08&amp;pt=17:08&amp;p=1111111111&amp;date=07.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">RB 30240</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "Holzkirchen\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">17:10</span>&nbsp;<span class=\"green bold\">p&#252;nktl.</span>,&nbsp;<span class=\"red\">heute Gl. 7       </span>");

		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureMessageAndUpdatedPosition()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/220206/221797/157782/5489/80/si=727269&amp;bt=dep&amp;ti=19:56&amp;pt=19:56&amp;p=1111111111&amp;date=06.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">CNL  450</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "Paris Est\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">19:57</span>&nbsp;<span class=\"green bold\">p&#252;nktl.</span>, <span class=\"red\">&#196;nderung im Zuglauf!</span>,&nbsp;<span class=\"red\">heute Gl. 7       </span>");

		assertNotNull(m.group(6)); // message
		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureWithWeirdMessage()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/760983/402261/557174/24926/80/si=808093&amp;bt=dep&amp;ti=02:41&amp;pt=02:41&amp;p=1111111111&amp;date=06.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">ICE  609</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "Basel SBB\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">04:52</span>&nbsp;k.A.,&nbsp;Gl. 3");

		assertNotNull(m.group(7)); // position
	}

	@Test
	public void departureWithMultipleMessages()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/208035/206616/13568/62563/80/si=624141&amp;bt=dep&amp;ti=09:34&amp;pt=09:34&amp;p=1111111111&amp;date=08.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">S      1</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "M&#252;nchen Ost\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">10:27</span>, <span class=\"red\">Zug f&#228;llt aus</span>,<br/><a class=\"red underline\" href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/144627/793698/280606/92094/80?ld=96159&amp;rt=1&amp;use_realtime_filter=1&amp;date=08.09.10&amp;time=10:27&amp;station_evaId=8001647&amp;station_type=dep&amp;\"><span class=\"red\">Ersatzzug&nbsp;S       </a></span>");

		assertNotNull(m.group(6)); // message
	}

	@Test
	public void departureWithPositionAndMessages()
	{
		final Matcher m = assertFineDepartures("" //
				+ "<a href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/32967/150423/39690/8856/80/si=676819&amp;bt=dep&amp;ti=00:57&amp;pt=00:57&amp;p=1111111111&amp;date=09.09.10&amp;max=10&amp;rt=1&amp;&amp;\">\n" //
				+ "<span class=\"bold\">RB 34075</span>\n" //
				+ "</a>\n" //
				+ "&gt;&gt;\n" //
				+ "N&#252;rnberg Hbf\n" //
				+ "<br />\n" //
				+ "<span class=\"bold\">00:18</span>, <span class=\"red\">Zug f&#228;llt aus</span>,&nbsp;Gl. 4,<br/><a class=\"red underline\" href=\"http://mobile.bahn.de/bin/mobil/traininfo.exe/dox/843399/1026627/462944/49661/80?ld=96159&amp;rt=1&amp;use_realtime_filter=1&amp;date=09.09.10&amp;time=00:18&amp;station_evaId=8001844&amp;station_type=dep&amp;\"><span class=\"red\">Ersatzzug&nbsp;RB 30535</a></span>");

		assertNotNull(m.group(6)); // message
		assertNotNull(m.group(7)); // position
	}

	private void assertFineConnectionDetails(String s)
	{
		Matcher m = BahnProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());

		// ParserUtils.printGroups(m);
	}

	private Matcher assertFineDepartures(String s)
	{
		Matcher m = BahnProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());

		ParserUtils.printGroups(m);

		assertNotNull(m.group(1)); // line
		assertNotNull(m.group(2)); // destination
		assertNotNull(m.group(3)); // time

		return m;
	}
}
