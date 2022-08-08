/*
 * Copyright the original author or authors.
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;

import de.schildbach.pte.exception.BlockedException;
import de.schildbach.pte.exception.InternalErrorException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.exception.UnexpectedRedirectException;

import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Response.Builder;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author Andreas Schildbach
 */
public final class HttpClient {
    @Nullable
    private String userAgent = null;
    private Map<String, String> headers = new HashMap<>();
    @Nullable
    private String sessionCookieName = null;
    @Nullable
    private Cookie sessionCookie = null;
    @Nullable
    private Proxy proxy = null;
    private boolean trustAllCertificates = false;
    @Nullable
    private CertificatePinner certificatePinner = null;

    private static final List<Integer> RESPONSE_CODES_BLOCKED = Ints.asList(HttpURLConnection.HTTP_BAD_REQUEST,
            HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN,
            HttpURLConnection.HTTP_NOT_ACCEPTABLE, HttpURLConnection.HTTP_UNAVAILABLE);
    private static final List<Integer> RESPONSE_CODES_NOT_FOUND = Ints.asList(HttpURLConnection.HTTP_NOT_FOUND);
    private static final List<Integer> RESPONSE_CODES_REDIRECT = Ints.asList(HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP, 307, 308);
    private static final List<Integer> RESPONSE_CODES_INTERNAL_ERROR = Ints
            .asList(HttpURLConnection.HTTP_INTERNAL_ERROR, HttpURLConnection.HTTP_BAD_GATEWAY);

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

        final Interceptor xmlEncodingInterceptor = new Interceptor() {
            private final Pattern P_XML_PRAGMA = Pattern.compile("<\\?xml.*?encoding=\"(.*?)\".*?\\?>");
            private final String HEADER_CONTENT_TYPE = "Content-Type";

            @Override
            public Response intercept(final Interceptor.Chain chain) throws IOException {
                Response response = chain.proceed(chain.request());
                final MediaType originalContentType = response.body().contentType();
                if (originalContentType != null && "text".equalsIgnoreCase(originalContentType.type())
                        && "xml".equalsIgnoreCase(originalContentType.subtype())
                        && originalContentType.charset() == null) {
                    final String peek = response.peekBody(64).string();
                    final Matcher matcher = P_XML_PRAGMA.matcher(peek);
                    if (matcher.find()) {
                        final String encoding = matcher.group(1);
                        final MediaType contentType = MediaType.get(originalContentType.type() + '/'
                                + originalContentType.subtype() + ";charset=" + encoding);
                        final ResponseBody body = response.body();
                        final Builder responseBuilder = response.newBuilder();
                        responseBuilder.header(HEADER_CONTENT_TYPE, contentType.toString());
                        responseBuilder.body(ResponseBody.create(contentType, body.contentLength(), body.source()));
                        response = responseBuilder.build();
                        log.debug("Deriving missing {} encoding from XML pragma", encoding);
                    }
                }
                return response;
            }
        };

        final Interceptor retryInterceptor = new Interceptor() {
            @Override
            public Response intercept(final Chain chain) throws IOException {
                final Request request = chain.request();
                Response response = null;
                try {
                    response = chain.proceed(request);
                } catch (final IOException x) {
                    if (Throwables.getRootCause(x) instanceof EOFException)
                        return chain.proceed(request); // retry
                    throw x;
                }
                if (response.isSuccessful() && response.peekBody(1).bytes().length == 0) {
                    log.info("Got empty response, retrying {}", request.url());
                    response.close();
                    return chain.proceed(request); // retry
                }
                return response;
            }
        };

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followRedirects(false);
        builder.followSslRedirects(true);
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.addNetworkInterceptor(loggingInterceptor);
        builder.addInterceptor(retryInterceptor);
        builder.addInterceptor(xmlEncodingInterceptor);
        OKHTTP_CLIENT = builder.build();
    }

    private static final String SCRAPE_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final int SCRAPE_PEEK_SIZE = 8192;

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

    public void setProxy(final Proxy proxy) {
        this.proxy = proxy;
    }

    public void setTrustAllCertificates(final boolean trustAllCertificates) {
        this.trustAllCertificates = trustAllCertificates;
    }

    public void setCertificatePin(final String host, final String... hashes) {
        this.certificatePinner = new CertificatePinner.Builder().add(host, hashes).build();
    }

    public CharSequence get(final HttpUrl url) throws IOException {
        return get(url, null, null);
    }

    public CharSequence get(final HttpUrl url, final String postRequest, final String requestContentType)
            throws IOException {
        final StringBuilder buffer = new StringBuilder();
        final Callback callback = (bodyPeek, body) -> buffer.append(body.string());
        getInputStream(callback, url, postRequest, requestContentType, null);
        return buffer;
    }

    public interface Callback {
        void onSuccessful(CharSequence bodyPeek, ResponseBody body) throws IOException;
    }

    public void getInputStream(final Callback callback, final HttpUrl url) throws IOException {
        getInputStream(callback, url, null);
    }

    public void getInputStream(final Callback callback, final HttpUrl url, final String referer) throws IOException {
        getInputStream(callback, url, null, null, referer);
    }

    public void getInputStream(final Callback callback, final HttpUrl url, final String postRequest,
            final String requestContentType, final String referer) throws IOException {
        checkNotNull(callback);
        checkNotNull(url);

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
        if (proxy != null || trustAllCertificates || certificatePinner != null) {
            final OkHttpClient.Builder builder = OKHTTP_CLIENT.newBuilder();
            if (proxy != null)
                builder.proxy(proxy);
            if (trustAllCertificates)
                trustAllCertificates(builder);
            if (certificatePinner != null)
                builder.certificatePinner(certificatePinner);
            okHttpClient = builder.build();
        } else {
            okHttpClient = OKHTTP_CLIENT;
        }

        final Call call = okHttpClient.newCall(request.build());
        try (final Response response = call.execute()) {
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
            } else if (RESPONSE_CODES_BLOCKED.contains(responseCode)) {
                throw new BlockedException(url, bodyPeek);
            } else if (RESPONSE_CODES_NOT_FOUND.contains(responseCode)) {
                throw new NotFoundException(url, bodyPeek);
            } else if (RESPONSE_CODES_REDIRECT.contains(responseCode)) {
                throw new UnexpectedRedirectException(url, HttpUrl.parse(response.header("Location")));
            } else if (RESPONSE_CODES_INTERNAL_ERROR.contains(responseCode)) {
                throw new InternalErrorException(url, bodyPeek);
            } else {
                final String message = "got response: " + responseCode + " " + response.message();
                throw new IOException(message + ": " + url);
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

    private void trustAllCertificates(final OkHttpClient.Builder okHttpClientBuilder) {
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { TRUST_ALL_CERTIFICATES }, null);
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            okHttpClientBuilder.sslSocketFactory(sslSocketFactory, TRUST_ALL_CERTIFICATES);
        } catch (final Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static final X509TrustManager TRUST_ALL_CERTIFICATES = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
