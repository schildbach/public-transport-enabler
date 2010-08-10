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
public class SbbProviderTest
{
	@Test
	public void tripWithoutDate()
	{
		assertFineConnectionDetails("<td headers=\"stops-2\" class=\"stop-station-icon\" valign=\"top\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/query.exe/dn?ld=&i=38.03520220.1281445666&n=1&uscid=14\"><img src=\"/img/2/icon_map_location.gif\" width=\"12\" height=\"12\" border=\"0\" alt=\"Umgebungskarte: Aarau\" hspace=\"3\" style=\"vertical-align:middle;margin-right:4px;\" /></a>\n" //
				+ "</td>\n" //
				+ "<td headers=\"stops-2\" class=\"stop-station\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/bhftafel.exe/dn?seqnr=1&ident=38.03520220.1281445666&input=8502113&boardType=dep&time=15:47\" title=\"Haltestelleninformation: Aarau\">Aarau</a></td>\n" //
				+ "<td headers=\"date-2\" class=\"date\" align=\"left\">\n" //
				+ "</td>\n" //
				+ "<td headers=\"time-2\" class=\"time prefix timeLeft\" align=\"left\" nowrap=\"nowrap\">ab</td><td headers=\"time-2\" class=\"time timeRight\" align=\"left\" nowrap=\"nowrap\">15:47</td><td headers=\"platform-2\" class=\"platform\" align=\"left\">\n" //
				+ "5       \n" //
				+ "</td>\n" //
				+ "<td headers=\"products-2\" class=\"products last\" style=\"white-space:nowrap;\" rowspan=\"2\" valign=\"top\">\n" //
				+ "<img src=\"/img/2/products/ir_pic.gif\" width=\"18\" height=\"18\" alt=\"IR 1928\" style=\"margin-top:2px;\"><br />\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/traininfo.exe/dn/485775/162441/812040/244101/85?seqnr=1&ident=38.03520220.1281445666&date=10.08.10&station_evaId=8502113&station_type=dep&journeyStartIdx=3&journeyEndIdx=6&\" title=\"Fahrtinformation\">\n" //
				+ "IR 1928\n" //
				+ "</a>\n" //
				+ "</td>\n" //
				+ "<td headers=\"capacity-2\" class=\"capacity last\" style=\"white-space:nowrap;\" rowspan=\"2\" valign=\"top\">\n" //
				+ "<div style=\"width:65px;height:15px;line-height:15px;\">\n" //
				+ "<div style=\"float:left;width:30px;height:15px;line-height:15px;\">\n" //
				+ "1. <img src=\"/img/2/icon_capacity1.gif\" alt=\"Tiefe bis mittlere Belegung erwartet\" title=\"Tiefe bis mittlere Belegung erwartet\" style=\"border:0px;width:14px;height12:px\" />\n" //
				+ "</div>\n" //
				+ "<div style=\"float:left;width:30px;height:15px;line-height:15px;margin-left:4px;\">\n" //
				+ "2. <img src=\"/img/2/icon_capacity1.gif\" alt=\"Tiefe bis mittlere Belegung erwartet\" title=\"Tiefe bis mittlere Belegung erwartet\" style=\"border:0px;width:14px;height12:px\" />\n" //
				+ "</div>\n" //
				+ "</div>\n" //
				+ "</td>\n" //
				+ "<td headers=\"remarks-2\" class=\"remarks last\" rowspan=\"2\" valign=\"top\">\n" //
				+ "InterRegio </td>\n" //
				+ "\n" //
				+ "<td headers=\"stops-2\"  class=\"stop-station-icon last\" valign=\"top\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/query.exe/dn?ld=&i=38.03520220.1281445666&n=1&uscid=15\"><img src=\"/img/2/icon_map_location.gif\" width=\"12\" height=\"12\" border=\"0\" alt=\"Umgebungskarte: Bern\" hspace=\"3\" style=\"vertical-align:middle;margin-right:4px;\" /></a></td>\n" //
				+ "<td headers=\"stops-2\" class=\"stop-station last\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/bhftafel.exe/dn?seqnr=1&ident=38.03520220.1281445666&input=8507000&boardType=arr&time=16:25\" title=\"Haltestelleninformation: Bern\">Bern</a></td>\n" //
				+ "<td headers=\"date-2\" class=\"date last\" align=\"left\">\n" //
				+ "</td>\n" //
				+ "<td headers=\"time-2\" class=\"time prefix last timeLeft\" align=\"left\" nowrap=\"nowrap\">an</td><td headers=\"time-2\" class=\"time last timeRight\" align=\"left\" nowrap=\"nowrap\">16:25</td><td headers=\"platform-2\" class=\"platform last\" align=\"left\">\n" //
				+ "10      \n" //
				+ "</td>");
	}

	@Test
	public void footway()
	{
		assertFineConnectionDetails("\n" //
				+ "<td headers=\"stops-0\" class=\"stop-station-icon\" valign=\"top\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/query.exe/dn?ld=&i=9q.030782220.1281450202&n=1&uscid=10\"><img src=\"/img/2/icon_map_location.gif\" width=\"12\" height=\"12\" border=\"0\" alt=\"Umgebungskarte: Amriswil, Bahnhof\" hspace=\"3\" style=\"vertical-align:middle;margin-right:4px;\" /></a>\n" //
				+ "</td>\n" //
				+ "<td headers=\"stops-0\" class=\"stop-station\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/bhftafel.exe/dn?seqnr=1&ident=9q.030782220.1281450202&input=8587138&boardType=dep&time=16:38\" title=\"Haltestelleninformation: Amriswil, Bahnhof\">Amriswil, Bahnhof</a></td>\n" //
				+ "<td headers=\"date-0\" class=\"date\" align=\"left\">\n" //
				+ "</td>\n" //
				+ "<td headers=\"time-0\" class=\"time prefix timeLeft\" align=\"left\" nowrap=\"nowrap\">&nbsp;</td><td headers=\"time-0\" class=\"time timeRight\" align=\"left\" nowrap=\"nowrap\">&nbsp;</td><td headers=\"platform-0\" class=\"platform\" align=\"left\">\n" //
				+ "</td>\n" //
				+ "<td headers=\"products-0\" class=\"products last\" style=\"white-space:nowrap;\" rowspan=\"2\" valign=\"top\">\n" //
				+ "<img src=\"/img/2/products/fuss_pic.gif\" width=\"18\" height=\"18\" border=\"0\" vspace=\"2\" alt=\"Fussweg\" /><br />\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/query.exe/dn?ld=&i=9q.030782220.1281450202&n=1&uscid=11\">Fussweg</a>\n" //
				+ "</td>\n" //
				+ "<td headers=\"capacity-0\" class=\"capacity last\" style=\"white-space:nowrap;\" rowspan=\"2\" valign=\"top\">\n" //
				+ "<div style=\"width:65px;height:15px;line-height:15px;\">\n" //
				+ "<div style=\"float:left;width:30px;height:15px;line-height:15px;\">\n" //
				+ "</div>\n" //
				+ "<div style=\"float:left;width:30px;height:15px;line-height:15px;margin-left:4px;\">\n" //
				+ "</div>\n" //
				+ "</div>\n" //
				+ "</td>\n" //
				+ "<td headers=\"remarks-0\" class=\"remarks last\" rowspan=\"2\" valign=\"top\">\n" //
				+ "1 Min., Y  </td>\n" //
				+ "\n" //
				+ "<td headers=\"stops-0\"  class=\"stop-station-icon last\" valign=\"top\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/query.exe/dn?ld=&i=9q.030782220.1281450202&n=1&uscid=12\"><img src=\"/img/2/icon_map_location.gif\" width=\"12\" height=\"12\" border=\"0\" alt=\"Umgebungskarte: Amriswil\" hspace=\"3\" style=\"vertical-align:middle;margin-right:4px;\" /></a></td>\n" //
				+ "<td headers=\"stops-0\" class=\"stop-station last\">\n" //
				+ "<a href=\"http://fahrplan.sbb.ch/bin/bhftafel.exe/dn?seqnr=1&ident=9q.030782220.1281450202&input=8506109&boardType=arr&time=16:39\" title=\"Haltestelleninformation: Amriswil\">Amriswil</a></td>\n" //
				+ "<td headers=\"date-0\" class=\"date last\" align=\"left\">\n" //
				+ "</td>\n" //
				+ "<td headers=\"time-0\" class=\"time prefix last timeLeft\" align=\"left\" nowrap=\"nowrap\">&nbsp;</td><td headers=\"time-0\" class=\"time last timeRight\" align=\"left\" nowrap=\"nowrap\">&nbsp;</td><td headers=\"platform-0\" class=\"platform last\" align=\"left\">\n" //
				+ "</td>\n");
	}

	private void assertFineConnectionDetails(String s)
	{
		Matcher m = SbbProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}
}
