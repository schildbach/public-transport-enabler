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

package de.schildbach.pte.exception;

import java.io.IOException;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public abstract class AbstractHttpException extends IOException {
    private final HttpUrl url;
    private final CharSequence bodyPeek;

    public AbstractHttpException(final HttpUrl url) {
        this(url, null);
    }

    public AbstractHttpException(final HttpUrl url, final CharSequence bodyPeek) {
        super(url.toString());
        this.url = url;
        this.bodyPeek = bodyPeek;
    }

    public HttpUrl getUrl() {
        return url;
    }

    public CharSequence getBodyPeek() {
        return bodyPeek;
    }
}
