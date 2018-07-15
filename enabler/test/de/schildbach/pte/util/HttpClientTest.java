/*
 * Copyright 2014-2015 the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class HttpClientTest {
    private HttpUrl base;

    @Before
    public void setUp() throws Exception {
        base = HttpUrl.parse("http://example.com");
    }

    @Test
    public void vodafoneRedirect() throws Exception {
        final HttpUrl url = HttpClient.testRedirect(base,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.1//EN \" \"http://www.openmobilealliance.org/tech/DTD/xhtml-mobile11.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"; xml:lang=\"en\"><head><title>Vodafone Center</title><meta http-equiv=\"Cache-Control\" content=\"no-cache\"/><meta http-equiv=\"refresh\" content=\"1;URL=https://center.vodafone.de/vfcenter/index.html?targetUrl=http%3A%2F%2Fwww.fahrinfo-berlin.de/Fahrinfo/bin/query.bin/dn%3fstart=Suchen&REQ0JourneyStopsS0ID=A%253D1%2540L%253D9083301&REQ0JourneyStopsZ0ID=A%253D1%2540L%253D9195009&REQ0HafasSearchForw=1&REQ0JourneyDate=16.06.14&REQ0JourneyTime=16%253A32&REQ0JourneyProduct_prod_list_1=11111011&h2g-direct=11&L=vs_oeffi\"/><style type=\"text/css\">*{border:none;font-family:Arial,Helvetica,sans-serif} body{font-size:69%;line-height:140%;background-color:#F4F4F4 !important}</style></head><body><h1>Sie werden weitergeleitet ...</h1><p>Sollten Sie nicht weitergeleitet werden, klicken Sie bitte <a href=\"https://center.vodafo");
        assertNotNull(url);
        assertEquals("center.vodafone.de", url.host());
    }

    public void kabelDeutschlandRedirect() throws Exception {
        final HttpUrl url = HttpClient.testRedirect(base,
                "<script type=\"text/javascript\"> window.location = \"http://www.hotspot.kabeldeutschland.de/portal/?RequestedURI=http%3A%2F%2Fwww.fahrinfo-berlin.de%2FFahrinfo%2Fbin%2Fajax-getstop.bin%2Fdny%3Fgetstop%3D1%26REQ0JourneyStopsS0A%3D255%26REQ0JourneyStopsS0G%3Dgneisenustra%25DFe%3F%26js%3Dtrue&RedirectReason=Policy&RedirectAqpId=100&DiscardAqpId=100&SubscriberId=4fa432d4a653e5f8b2acb27aa862f98d&SubscriberType=ESM&ClientIP=10.136.25.241&SystemId=10.143.181.2-1%2F2&GroupId=1&PartitionId=2&Application=Unknown&ApplicationGroup=Unknown\" </script>");
        assertNotNull(url);
        assertEquals("www.hotspot.kabeldeutschland.de", url.host());
    }

    @Test
    public void tplinkRedirect() throws Exception {
        final HttpUrl url = HttpClient.testRedirect(base,
                "<body><script language=\"javaScript\">location.href=\"http://tplinkextender.net/\";</script></body></html>");
        assertNotNull(url);
        assertEquals("tplinkextender.net", url.host());
    }

    @Test
    public void mshtmlRedirect() throws Exception {
        final HttpUrl url = HttpClient.testRedirect(base,
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><HEAD><TITLE>HTML Redirection</TITLE><META http-equiv=Content-Type content=\"text/html; \"><META http-equiv=Refresh content=\"0;URL=/cgi-bin/index.cgi\"><META content=\"MSHTML 6.00.2900.2873\" name=GENERATOR></HEAD><BODY >  <NOSCRIPT>   If your browser can not redirect you to home page automatically.<br>   Please click <a href=/cgi-bin/welcome.cgi?lang=0>here</a>.   </NOSCRIPT></BODY></HTML>");
        assertNotNull(url);
        assertEquals("example.com", url.host());
    }

    @Test
    public void efaExpired() throws Exception {
        assertTrue(HttpClient.testExpired(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; \"/><meta http-equiv=\"Expires\" content=\"0\"/><title>Efa9 Internal Error</title><style>.BOLD {font: bold large Arial;}.NORMAL {font: normal x-small Arial;}</style></head><body><div class=\"BOLD\">Internal Error</div><div class=\"NORMAL\">Your session has expired.</div><!--<p>&nbsp;</p><div class=\"NORMAL\">.\\EfaHttpServer.cpp</div><div class=\"NORMAL\">Line: 2043</div>--></body></html>"));
    }

    @Test
    public void tflExpired() throws Exception {
        assertTrue(HttpClient.testExpired(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><title>Session Expired</title><style type=\"text/css\">body{  font-family:Verdana, Arial, Helvetica, sans-serif}</style></head><body bgcolor=\"#FFFFFF\" leftmargin=\"0\" topmargin=\"0\" rightmargin=\"0\" bottommargin=\"0\" marginwidth=\"0\" marginheight=\"0\"><!--Logo--><table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>    <td width=\"100%\" height=\"40\" valign=\"top\" class=\"fenster\"><table width=\"389\" height=\"40\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>    <td width=\"93\" valign=\"top\"><span>&nbsp;</span></td><td width=\"296\" valign=\"top\"><img src=\"images/logo.gif\" alt=\"\" width=\"372\" height=\"86\" border=\"0\"></td></tr></table></td></tr></table><!--/ Logo--><!--Content--><span><!--Headline--><table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td width=\"18\" valign=\"top\"><span>&nbsp;</span></td><td width=\"566\" valign=\"top\"><span class=\"headline\"><b>Session Expire"));
    }

    @Test
    public void nvbwExpired() throws Exception {
        assertTrue(HttpClient.testExpired("<h2>Ihre Verbindungskennung ist nicht mehr gültig.</h2>"));
    }

    @Test
    public void internalError() throws Exception {
        assertTrue(HttpClient.testInternalError(
                "<?xml version=\"1.0\"?>     <!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\"    \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">      <html xmlns=\"http://www.w3.org/1999/xhtml\">      <head>        <title>          Internal error in gateway     </title>       </head>       <body>        <h1>          Internal error in gateway     </h1>       </body>      </html>"));
    }

    @Test
    public void vgnInternalError() throws Exception {
        assertTrue(HttpClient.testInternalError(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/><meta http-equiv=\"Expires\" content=\"0\"/><title>Efa9 Internal Error</title></head><body><div style=\"font: bold large Arial;\">Internal Error</div><div style=\"font: normal x-small Arial;\">.\\EfaHttpServer.cpp</div><div style=\"font: normal x-small Arial;\">Line: 2507</div></body></html>"));
    }

    @Test
    public void vrnInternalError() throws Exception {
        assertTrue(HttpClient.testInternalError(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><title>VRN - Keine Verbindung zum Server möglich</title></head><body><center><table border=\"0\" width=\"450\" cellpadding=\"5\"><tr><td height=\"50\">&nbsp;</td></tr><tr><td align=\"center\"><img src=\"/vrn/ExceptionFiles/cookies.jpg\"></td></tr></table></center></body></html>"));
    }
}
