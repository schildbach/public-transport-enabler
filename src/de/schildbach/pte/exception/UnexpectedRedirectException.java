/*
 * Copyright 2010-2015 the original author or authors.
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

package de.schildbach.pte.exception;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public class UnexpectedRedirectException extends AbstractHttpException {
    private final HttpUrl redirectedUrl;

    public UnexpectedRedirectException(final HttpUrl originalUrl, final HttpUrl redirectedUrl) {
        super(originalUrl);
        this.redirectedUrl = redirectedUrl;
    }

    public HttpUrl getRedirectedUrl() {
        return redirectedUrl;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getUrl() + " -> " + redirectedUrl;
    }
}
