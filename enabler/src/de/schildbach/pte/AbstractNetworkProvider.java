/*
 * Copyright 2010-2012 the original author or authors.
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

package de.schildbach.pte;

import java.io.IOException;
import java.nio.charset.Charset;

import de.schildbach.pte.dto.GetConnectionDetailsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractNetworkProvider implements NetworkProvider
{
	protected static final Charset UTF_8 = Charset.forName("UTF-8");
	protected static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	public Style lineStyle(final String line)
	{
		if (line.length() == 0)
			return null;
		return StandardColors.LINES.get(line.charAt(0));
	}

	public Point[] getArea()
	{
		return null;
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
