/*
 * Copyright 2012-2015 the original author or authors.
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

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public class InvalidDataException extends ParserException {
    public InvalidDataException() {
        super();
    }

    public InvalidDataException(final String message) {
        super(message);
    }

    public InvalidDataException(final String message, final Throwable cause) {
        super(message);
        super.initCause(cause);
    }

    public InvalidDataException(final Throwable cause) {
        super(cause == null ? null : cause.toString());
        super.initCause(cause);
    }
}
