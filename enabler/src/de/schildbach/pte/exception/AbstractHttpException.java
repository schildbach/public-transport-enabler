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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.exception;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public abstract class AbstractHttpException extends ParserException
{
	private final URL url;
	private final Reader errorReader;

	public AbstractHttpException(final URL url, final Reader errorReader)
	{
		super();
		this.url = url;
		this.errorReader = errorReader;
	}

	public AbstractHttpException(final URL url, final String message)
	{
		super(message);
		this.url = url;
		this.errorReader = null;
	}

	public URL getUrl()
	{
		return url;
	}

	public Reader getErrorReader()
	{
		return errorReader;
	}

	public CharSequence scrapeErrorStream() throws IOException
	{
		if (errorReader == null)
			return null;

		final StringBuilder error = new StringBuilder(ParserUtils.SCRAPE_INITIAL_CAPACITY);
		ParserUtils.copy(errorReader, error);
		errorReader.close();

		return error;
	}
}
