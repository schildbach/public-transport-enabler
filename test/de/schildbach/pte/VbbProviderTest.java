package de.schildbach.pte;

import static junit.framework.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

public class VbbProviderTest
{
	@Test
	public void fusswegWithSpace()
	{
		assertFineConnectionDetails("6 Min. Fussweg<br /><a href=\"/Fahrinfo/bin/stboard.bin/dox?ld=0.1&amp;n=2&amp;i=00.072331.1278280801&amp;rt=0&amp;input=9024106\">"
				+ "<strong>S Messe Nord/ICC (Berlin)</strong> </a><br />");
	}

	@Test
	public void fussweg()
	{
		assertFineConnectionDetails("<a href=\"/Stadtplan/index/mobil?language=d&amp;location=,ADR,WGS84,13426558,52536061&amp;label=10405 Bln Pankow, Christburger Str. 123\">10405 Bln Pankow, Christburger Str. 123</a>\n"
				+ "<br />\n"
				+ "10 Min.\n"
				+ "Fussweg\n"
				+ "<br />\n"
				+ "<a href=\"/Fahrinfo/bin/stboard.bin/dox?ld=0.1&amp;n=2&amp;i=c6.0318411.1278336553&amp;rt=0&amp;input=9110017\">\n"
				+ "<strong>Prenzlauer Allee/Danziger Str. (Berlin)</strong>\n" + "</a>\n" + "<br />");
	}

	@Test
	public void trip()
	{
		assertFineConnectionDetails("<a href=\"/Fahrinfo/bin/stboard.bin/dox?ld=0.1&amp;n=2&amp;i=er.042611.1278315324&amp;rt=0&amp;input=9275402\">\n"
				+ "<strong>Brandenburg, Frhr.-v.-Th&#252;ngen-Str.</strong>\n"
				+ "</a>\n"
				+ "<br />\n"
				+ "ab 09:35\n"
				+ "<br/><strong>BusH/528</strong>\n"
				+ "Ri. Brandenburg, Potsdamer Str.\n"
				+ "<br />\n"
				+ "an 09:41\n"
				+ "<br />\n"
				+ "<a href=\"/Fahrinfo/bin/stboard.bin/dox?ld=0.1&amp;n=2&amp;i=er.042611.1278315324&amp;rt=0&amp;input=9275104\">\n"
				+ "<strong>Brandenburg, Plauer Str.</strong>\n" //
				+ "</a>\n" //
				+ "<br />");
	}

	private void assertFineConnectionDetails(String s)
	{
		Matcher m = VbbProvider.P_CONNECTION_DETAILS_FINE.matcher(s);
		assertTrue(m.matches());
		// ParserUtils.printGroups(m);
	}
}
