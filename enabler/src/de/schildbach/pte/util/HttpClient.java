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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import de.schildbach.pte.exception.BlockedException;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.exception.UnexpectedRedirectException;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

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
    private Cookie sessionCookie = null;
    private boolean sslAcceptAllHostnames = false;

    private static final OkHttpClient OKHTTP_CLIENT;
    static {
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(final String message) {
                        log.debug(message);
                    }
                });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followRedirects(false);
        builder.followSslRedirects(true);
        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.writeTimeout(5, TimeUnit.SECONDS);
        builder.readTimeout(15, TimeUnit.SECONDS);
        builder.addNetworkInterceptor(loggingInterceptor);
        OKHTTP_CLIENT = builder.build();
    }

    private static final String SCRAPE_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    public static final int SCRAPE_INITIAL_CAPACITY = 4096;
    private static final int SCRAPE_PEEK_SIZE = 4096;

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

    public CharSequence get(final HttpUrl url) throws IOException {
        return get(url, null);
    }

    public CharSequence get(final HttpUrl url, final Charset requestEncoding) throws IOException {
        return get(url, null, null, requestEncoding);
    }

    public CharSequence get(final HttpUrl url, final String postRequest, final String requestContentType,
            Charset requestEncoding) throws IOException {
        if (requestEncoding == null)
            requestEncoding = Charsets.ISO_8859_1;

        final StringBuilder buffer = new StringBuilder(SCRAPE_INITIAL_CAPACITY);
        final Callback callback = new Callback() {
            @Override
            public void onSuccessful(final CharSequence bodyPeek, final ResponseBody body) throws IOException {
                buffer.append(body.string());
            }
        };
        getInputStream(callback, url, postRequest, requestContentType, requestEncoding, null);
        return buffer;
    }

    public interface Callback {
        void onSuccessful(CharSequence bodyPeek, ResponseBody body) throws IOException;
    }

    public void getInputStream(final Callback callback, final HttpUrl url) throws IOException {
        getInputStream(callback, url, null, null);
    }

    public void getInputStream(final Callback callback, final HttpUrl url, final Charset requestEncoding,
            final String referer) throws IOException {
        getInputStream(callback, url, null, null, requestEncoding, referer);
    }

    public void getInputStream(final Callback callback, final HttpUrl url, final String postRequest,
            final String requestContentType, Charset requestEncoding, final String referer) throws IOException {
        if (requestEncoding == null)
            requestEncoding = Charsets.ISO_8859_1;

        int tries = 3;

        while (true) {
            final Request.Builder request = new Request.Builder();
            request.url(url);
            request.headers(Headers.of(headers));
            if (postRequest != null)
                request.post(RequestBody.create(MediaType.parse(requestContentType), postRequest));
            request.header("Accept", SCRAPE_ACCEPT);
            if (userAgent != null)
                request.header("User-Agent", userAgent);
            if (referer != null)
                request.header("Referer", referer);
            final Cookie sessionCookie = this.sessionCookie;
            if (sessionCookie != null && sessionCookie.name().equals(sessionCookieName))
                request.header("Cookie", sessionCookie.toString());

            final OkHttpClient okHttpClient;
            if (sslAcceptAllHostnames)
                okHttpClient = OKHTTP_CLIENT.newBuilder().hostnameVerifier(SSL_ACCEPT_ALL_HOSTNAMES).build();
            else
                okHttpClient = OKHTTP_CLIENT;

            final Call call = okHttpClient.newCall(request.build());
            Response response = null;
            try {
                response = call.execute();
                final int responseCode = response.code();
                final String bodyPeek = response.peekBody(SCRAPE_PEEK_SIZE).string().replaceAll("\\p{C}", "");
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    final HttpUrl redirectUrl = testRedirect(url, bodyPeek);
                    if (redirectUrl != null)
                        throw new UnexpectedRedirectException(url, redirectUrl);

                    if (testExpired(bodyPeek))
                        throw new SessionExpiredException();
                    if (testInternalError(bodyPeek))
                        throw new InternalErrorException(url, bodyPeek);

                    // save cookie
                    if (sessionCookieName != null) {
                        final List<Cookie> cookies = Cookie.parseAll(url, response.headers());
                        for (final Iterator<Cookie> i = cookies.iterator(); i.hasNext();) {
                            final Cookie cookie = i.next();
                            if (cookie.name().equals(sessionCookieName)) {
                                this.sessionCookie = cookie;
                                break;
                            }
                        }
                    }

                    callback.onSuccessful(bodyPeek, response.body());
                    return;
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST
                        || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
                        || responseCode == HttpURLConnection.HTTP_FORBIDDEN
                        || responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE
                        || responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                    throw new BlockedException(url, bodyPeek);
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new NotFoundException(url, bodyPeek);
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    throw new UnexpectedRedirectException(url, HttpUrl.parse(response.header("Location")));
                } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    throw new InternalErrorException(url, bodyPeek);
                } else {
                    final String message = "got response: " + responseCode + " " + response.message();
                    if (tries-- > 0)
                        log.info("{}, retrying...", message);
                    else
                        throw new IOException(message + ": " + url);
                }
            } finally {
                if (response != null)
                    response.close();
            }
        }
    }

    private static final Pattern P_REDIRECT_HTTP_EQUIV = Pattern.compile(
            "<META\\s+http-equiv=\"?refresh\"?\\s+content=\"\\d+;\\s*URL=([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_REDIRECT_SCRIPT = Pattern.compile(
            "<script\\s+(?:type=\"text/javascript\"|language=\"javascript\")>\\s*(?:window.location|location.href)\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    public static HttpUrl testRedirect(final HttpUrl base, final String content) {
        // check for redirect by http-equiv meta tag header
        final Matcher mHttpEquiv = P_REDIRECT_HTTP_EQUIV.matcher(content);
        if (mHttpEquiv.find())
            return base.resolve(mHttpEquiv.group(1));

        // check for redirect by window.location javascript
        final Matcher mScript = P_REDIRECT_SCRIPT.matcher(content);
        if (mScript.find())
            return base.resolve(mScript.group(1));

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
