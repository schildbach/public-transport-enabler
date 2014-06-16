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

package de.schildbach.pte.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;

/**
 * @author Andreas Schildbach
 */
public class ParserUtilsTest
{
	@Test
	public void vodafoneRedirect() throws Exception
	{
		final URL url = ParserUtils
				.testRedirect("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.1//EN \" \"http://www.openmobilealliance.org/tech/DTD/xhtml-mobile11.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"; xml:lang=\"en\"><head><title>Vodafone Center</title><meta http-equiv=\"Cache-Control\" content=\"no-cache\"/><meta http-equiv=\"refresh\" content=\"1;URL=https://center.vodafone.de/vfcenter/index.html?targetUrl=http%3A%2F%2Fwww.fahrinfo-berlin.de/Fahrinfo/bin/query.bin/dn%3fstart=Suchen&REQ0JourneyStopsS0ID=A%253D1%2540L%253D9083301&REQ0JourneyStopsZ0ID=A%253D1%2540L%253D9195009&REQ0HafasSearchForw=1&REQ0JourneyDate=16.06.14&REQ0JourneyTime=16%253A32&REQ0JourneyProduct_prod_list_1=11111011&h2g-direct=11&L=vs_oeffi\"/><style type=\"text/css\">*{border:none;font-family:Arial,Helvetica,sans-serif} body{font-size:69%;line-height:140%;background-color:#F4F4F4 !important}</style></head><body><h1>Sie werden weitergeleitet ...</h1><p>Sollten Sie nicht weitergeleitet werden, klicken Sie bitte <a href=\"https://center.vodafo");
		assertNotNull(url);
		assertEquals("center.vodafone.de", url.getHost());
	}
}
