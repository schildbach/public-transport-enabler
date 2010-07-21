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

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class MvvProviderTest
{
	@Test
	public void trip()
	{
		assertFineConnectionDetails("\n" //
				+ "<td colspan=\"4\">ab 04:27 Machern (Sachs) Gleis 2<br />\n" //
				+ "</td>\n" //
				+ "\n" //
				+ "<td width=\"15\" valign=\"middle\">\n" //
				+ "<img src=\"images/means/zug.gif\" alt=\"Zug\" />\n" //
				+ "</td>\n" //
				+ "<td width=\"1\" valign=\"middle\" />\n" //
				+ "<td>MRB 88040 Mitteldeutsche Regiobahn <br />Richtung Leipzig Hbf</td>\n" //
				+ "<td width=\"1\"> </td>\n" //
				+ "\n" //
				+ "<td colspan=\"4\">an 04:47 Leipzig Hbf Gleis 19</td>\n");
	}

	@Test
	@Ignore("deactivated because there is no time")
	public void tripWithoutTime()
	{
		assertFineConnectionDetails("\n" //
				+ "<td colspan=\"4\">ab Neufahrn  <a class=\"imgLink\" href=\"XSLT_TRIP_REQUEST2?language=de&amp;tripSelector2=on&amp;sessionID=MVV2_1678243657&amp;requestID=1&amp;tripSelection=on&amp;itdLPxx_view=map_2&amp;itdLPxx_img=FILELOAD?Filename=mvv2_4C45BE6910.png&amp;itdLPxx_partialRoute=2&amp;imageFormat=PNG&amp;imageWidth=400&amp;imageHeight=300&amp;imageOnly=1&amp;imageNoTiles=1&amp;itdLPxx_usage=departure\"><img src=\"images/pdf.gif\" border=\"0\" alt=\"Karte\" /></a>\n" //
				+ "<br />\n" //
				+ "</td>\n" //
				+ "\n" //
				+ "<td width=\"15\" valign=\"middle\">\n" //
				+ "<img src=\"images/means/seat.gif\" alt=\"Sitzenbleiber\" />\n" //
				+ "</td>\n" //
				+ "<td width=\"1\" valign=\"middle\" />\n" //
				+ "<td>nicht umsteigen</td>\n" //
				+ "<td width=\"1\"> </td>\n" //
				+ "\n" //
				+ "<td colspan=\"4\">an Neufahrn  <a class=\"imgLink\" href=\"XSLT_TRIP_REQUEST2?language=de&amp;tripSelector2=on&amp;sessionID=MVV2_1678243657&amp;requestID=1&amp;tripSelection=on&amp;itdLPxx_view=map_2&amp;itdLPxx_img=FILELOAD?Filename=mvv2_4C45BE6911.png&amp;itdLPxx_partialRoute=2&amp;imageFormat=PNG&amp;imageWidth=400&amp;imageHeight=300&amp;imageOnly=1&amp;imageNoTiles=1&amp;command=nop&amp;itdLPxx_usage=arrival\"><img src=\"images/pdf.gif\" border=\"0\" alt=\"Karte\" /></a>\n" //
				+ "</td>\n");
	}

	private void assertFineConnectionDetails(String s)
	{
		Matcher m = MvvProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}
}
