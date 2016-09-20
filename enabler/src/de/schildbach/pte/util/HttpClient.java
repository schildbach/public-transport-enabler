/*
 * Copyright 2015 the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import de.schildbach.pte.exception.BlockedException;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.exception.UnexpectedRedirectException;

/**
 * @author Andreas Schildbach
 */
public final class HttpClient {
    @Nullable
    private String userAgent = null;
    private Map<String, String> headers = new HashMap<String, String>();
    @Nullable
    private String sessionCookieName = null;
    @Nullable
    private HttpCookie sessionCookie = null;
    private boolean sslAcceptAllHostnames = false;

    private static final String SCRAPE_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    public static final int SCRAPE_INITIAL_CAPACITY = 4096;
    private static final int SCRAPE_COPY_SIZE = 2048;
    private static final int SCRAPE_PEEK_SIZE = 4096;
    private static final int SCRAPE_CONNECT_TIMEOUT = 5000;
    private static final int SCRAPE_READ_TIMEOUT = 15000;

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    public void setHeader(final String headerName, final String headerValue) {
        this.headers.put(headerName, headerValue);
    }

    public void setSessionCookieName(final String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public void setSslAcceptAllHostnames(final boolean sslAcceptAllHostnames) {
        this.sslAcceptAllHostnames = sslAcceptAllHostnames;
    }

    public CharSequence get(final String url) throws IOException {
        return get(url, null);
    }

    public CharSequence get(final String urlStr, final Charset requestEncoding) throws IOException {
        return get(urlStr, null, null, requestEncoding);
    }

    public CharSequence get(final String urlStr, final String postRequest, final String requestContentType,
            Charset requestEncoding) throws IOException {
        if (requestEncoding == null)
            requestEncoding = Charsets.ISO_8859_1;

        final StringBuilder buffer = new StringBuilder(SCRAPE_INITIAL_CAPACITY);
        final InputStream is = getInputStream(urlStr, postRequest, requestContentType, requestEncoding, null);
        final Reader pageReader = new InputStreamReader(is, requestEncoding);
        copy(pageReader, buffer);
        pageReader.close();
        return buffer;
    }

    public InputStream getInputStream(final String url) throws IOException {
        return getInputStream(url, null, null);
    }

    public InputStream getInputStream(final String urlStr, final Charset requestEncoding, final String referer)
            throws IOException {
        return getInputStream(urlStr, null, null, requestEncoding, referer);
    }

    public InputStream getInputStream(final String urlStr, final String postRequest, final String requestContentType,
            Charset requestEncoding, final String referer) throws IOException {
        log.debug("{}: {}", postRequest != null ? "POST" : "GET", urlStr);

        if (requestEncoding == null)
            requestEncoding = Charsets.ISO_8859_1;

        int tries = 3;

        while (true) {
            final URL url = new URL(urlStr);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (connection instanceof HttpsURLConnection && sslAcceptAllHostnames)
                ((HttpsURLConnection) connection).setHostnameVerifier(SSL_ACCEPT_ALL_HOSTNAMES);

            connection.setDoInput(true);
            connection.setDoOutput(postRequest != null);
            connection.setConnectTimeout(SCRAPE_CONNECT_TIMEOUT);
            connection.setReadTimeout(SCRAPE_READ_TIMEOUT);
            for (final Map.Entry<String, String> entry : headers.entrySet())
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            if (userAgent != null)
                connection.addRequestProperty("User-Agent", userAgent);
            connection.addRequestProperty("Accept", SCRAPE_ACCEPT);
            connection.addRequestProperty("Accept-Encoding", "gzip");
            // workaround to disable Vodafone compression
            connection.addRequestProperty("Cache-Control", "no-cache");

            if (referer != null)
                connection.addRequestProperty("Referer", referer);

            final HttpCookie sessionCookie = this.sessionCookie;
            if (sessionCookie != null && sessionCookie.getName().equals(sessionCookieName))
                connection.addRequestProperty("Cookie", sessionCookie.toString());

            if (postRequest != null) {
                final byte[] postRequestBytes = postRequest.getBytes(requestEncoding.name());

                connection.setRequestMethod("POST");
                connection.addRequestProperty("Content-Type", requestContentType);
                connection.addRequestProperty("Content-Length", Integer.toString(postRequestBytes.length));

                final OutputStream os = connection.getOutputStream();
                os.write(postRequestBytes);
                os.close();
            }

            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                final String contentType = connection.getContentType();
                final String contentEncoding = connection.getContentEncoding();

                InputStream is = new BufferedInputStream(connection.getInputStream());

                if ("gzip".equalsIgnoreCase(contentEncoding)
                        || "application/octet-stream".equalsIgnoreCase(contentType))
                    is = wrapGzip(is);

                if (!url.getHost().equals(connection.getURL().getHost()))
                    throw new UnexpectedRedirectException(url, connection.getURL());

                final String firstChars = peekFirstChars(is);

                final URL redirectUrl = testRedirect(url, firstChars);
                if (redirectUrl != null)
                    throw new UnexpectedRedirectException(url, redirectUrl);

                if (testExpired(firstChars))
                    throw new SessionExpiredException();

                if (testInternalError(firstChars))
                    throw new InternalErrorException(url, new InputStreamReader(is, requestEncoding));

                // save cookie
                if (sessionCookieName != null) {
                    c: for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                        if ("set-cookie".equalsIgnoreCase(entry.getKey())
                                || "set-cookie2".equalsIgnoreCase(entry.getKey())) {
                            for (final String value : entry.getValue()) {
                                for (final HttpCookie cookie : HttpCookie.parse(value)) {
                                    if (cookie.getName().equals(sessionCookieName)) {
                                        this.sessionCookie = cookie;
                                        break c;
                                    }
                                }
                            }
                        }
                    }
                }

                return is;
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST
                    || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
                    || responseCode == HttpURLConnection.HTTP_FORBIDDEN
                    || responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE
                    || responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                throw new BlockedException(url, new InputStreamReader(connection.getErrorStream(), requestEncoding));
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new NotFoundException(url, new InputStreamReader(connection.getErrorStream(), requestEncoding));
            } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                throw new UnexpectedRedirectException(url, connection.getURL());
            } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                throw new InternalErrorException(url,
                        new InputStreamReader(connection.getErrorStream(), requestEncoding));
            } else {
                final String message = "got response: " + responseCode + " " + connection.getResponseMessage();
                if (tries-- > 0)
                    log.info("{}, retrying...", message);
                else
                    throw new IOException(message + ": " + url);
            }
        }
    }

    public static final long copy(final Reader reader, final StringBuilder builder) throws IOException {
        final char[] buffer = new char[SCRAPE_COPY_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = reader.read(buffer))) {
            builder.append(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private static InputStream wrapGzip(final InputStream is) throws IOException {
        is.mark(2);
        final int byte0 = is.read();
        final int byte1 = is.read();
        is.reset();

        // check for gzip header
        if (byte0 == 0x1f && byte1 == 0x8b) {
            final BufferedInputStream is2 = new BufferedInputStream(new GZIPInputStream(is));
            is2.mark(2);
            final int byte0_2 = is2.read();
            final int byte1_2 = is2.read();
            is2.reset();

            // check for gzip header again
            if (byte0_2 == 0x1f && byte1_2 == 0x8b) {
                // double gzipped
                return new BufferedInputStream(new GZIPInputStream(is2));
            } else {
                // gzipped
                return is2;
            }
        } else {
            // uncompressed
            return is;
        }
    }

    public static String peekFirstChars(final InputStream is) throws IOException {
        is.mark(SCRAPE_PEEK_SIZE);
        final byte[] firstBytes = new byte[SCRAPE_PEEK_SIZE];
        final int read = is.read(firstBytes);
        if (read == -1)
            return "";
        is.reset();
        return new String(firstBytes, 0, read).replaceAll("\\p{C}", "");
    }

    private static final Pattern P_REDIRECT_HTTP_EQUIV = Pattern.compile(
            "<META\\s+http-equiv=\"?refresh\"?\\s+content=\"\\d+;\\s*URL=([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_REDIRECT_SCRIPT = Pattern.compile(
            "<script\\s+(?:type=\"text/javascript\"|language=\"javascript\")>\\s*(?:window.location|location.href)\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    public static URL testRedirect(final URL context, final String content) throws MalformedURLException {
        // check for redirect by http-equiv meta tag header
        final Matcher mHttpEquiv = P_REDIRECT_HTTP_EQUIV.matcher(content);
        if (mHttpEquiv.find())
            return new URL(context, mHttpEquiv.group(1));

        // check for redirect by window.location javascript
        final Matcher mScript = P_REDIRECT_SCRIPT.matcher(content);
        if (mScript.find())
            return new URL(context, mScript.group(1));

        return null;
    }

    private static final Pattern P_EXPIRED = Pattern.compile(
            ">\\s*(Your session has expired\\.|Session Expired|Ihre Verbindungskennung ist nicht mehr g.ltig\\.)\\s*<");

    public static boolean testExpired(final String content) {
        // check for expired session
        final Matcher mSessionExpired = P_EXPIRED.matcher(content);
        if (mSessionExpired.find())
            return true;

        return false;
    }

    private static final Pattern P_INTERNAL_ERROR = Pattern.compile(
            ">\\s*(Internal Error|Server ein Fehler aufgetreten|Internal error in gateway|VRN - Keine Verbindung zum Server m.glich)\\s*<");

    public static boolean testInternalError(final String content) {
        // check for internal error
        final Matcher m = P_INTERNAL_ERROR.matcher(content);
        if (m.find())
            return true;

        return false;
    }

    private static final HostnameVerifier SSL_ACCEPT_ALL_HOSTNAMES = new HostnameVerifier() {
        @Override
        public boolean verify(final String hostname, final SSLSession session) {
            return true;
        }
    };
}
