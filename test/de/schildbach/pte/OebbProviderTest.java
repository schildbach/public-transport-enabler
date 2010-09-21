package de.schildbach.pte;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

public class OebbProviderTest
{
	@Test
	public void testDepartures()
	{
		assertFineDepartures("" //
				+ "<td class=\"bold center sepline top\">09:12</td>\n" //
				+ "<td class=\"bold top nowrap sepline\">\n" //
				+ "<a href=\"http://fahrplan.oebb.at/bin/traininfo.exe/dn/152499/163408/534164/216252/81?ld=web05&date=21.09.10&station_evaId=912101&station_type=dep\"><img src=\"/img/vs_oebb/bus_pic.gif\"  alt=\"Bus  16A\"> Bus  16A</a>\n" //
				+ "</td>\n" //
				+ "<td class=\"sepline top\">\n" //
				+ "<span class=\"bold\">\n" //
				+ "<a href=\"http://fahrplan.oebb.at/bin/stboard.exe/dn?ld=web05&input=Wien Hetzendorf Bahnhst&boardType=dep&time=09:13&maxJourneys=10&productsFilter=111111111111&\">\n" //
				+ "Wien Hetzendorf Bahnhst\n" //
				+ "</a>\n" //
				+ "</span>\n" //
				+ "<br />\n" //
				+ "<a href=\"http://fahrplan.oebb.at/bin/stboard.exe/dn?ld=web05&input=Wien Breitenfurter Stra&#223;e/Hetzendorfer Stra&#223;e%23912101&boardType=dep&time=09:12&maxJourneys=10&productsFilter=111111111111&\">\n" //
				+ "Wien Breitenfurter Stra&#223;e/Hetzendorfer Stra&#223;e\n" //
				+ "</a>\n" //
				+ "09:12\n" //
				+ "-\n" //
				+ "<a href=\"http://fahrplan.oebb.at/bin/stboard.exe/dn?ld=web05&input=Wien Hetzendorf Bahnhst (Eckartsaugasse)%23912018&boardType=dep&time=09:13&maxJourneys=10&productsFilter=111111111111&\">\n" //
				+ "Wien Hetzendorf Bahnhst (Eckartsaugasse)\n" //
				+ "</a>\n" //
				+ "09:13\n" //
				+ "</td>\n");
	}

	private Matcher assertFineDepartures(String s)
	{
		Matcher m = OebbProvider.P_DEPARTURES_FINE.matcher(s);
		assertTrue(m.matches());

		// ParserUtils.printGroups(m);

		assertNotNull(m.group(1)); // time
		assertNotNull(m.group(3)); // lineType
		assertNotNull(m.group(4)); // line
		assertNotNull(m.group(6)); // destination

		return m;
	}
}
