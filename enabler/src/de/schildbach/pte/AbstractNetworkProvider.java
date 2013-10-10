/*
 * Copyright 2010-2013 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractNetworkProvider implements NetworkProvider
{
	protected static final Charset UTF_8 = Charset.forName("UTF-8");
	protected static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
	protected static final Set<Product> ALL_EXCEPT_HIGHSPEED;

	private Map<String, Style> styles = null;

	static
	{
		ALL_EXCEPT_HIGHSPEED = new HashSet<Product>(Product.ALL);
		ALL_EXCEPT_HIGHSPEED.remove(Product.HIGH_SPEED_TRAIN);
	}

	public Collection<Product> defaultProducts()
	{
		return ALL_EXCEPT_HIGHSPEED;
	}

	protected void setStyles(final Map<String, Style> styles)
	{
		this.styles = styles;
	}

	private static final char STYLES_SEP = '|';

	public Style lineStyle(final String network, final String line)
	{
		if (line == null || line.length() == 0)
			return null;

		if (styles != null)
		{
			if (network != null)
			{
				// check for line match
				final Style lineStyle = styles.get(network + STYLES_SEP + line);
				if (lineStyle != null)
					return lineStyle;

				// check for product match
				final Style productStyle = styles.get(network + STYLES_SEP + line.charAt(0));
				if (productStyle != null)
					return productStyle;

				// check for night bus, as that's a common special case
				if (line.startsWith("BN"))
				{
					final Style nightStyle = styles.get(network + STYLES_SEP + "BN");
					if (nightStyle != null)
						return nightStyle;
				}
			}

			// check for line match
			final Style lineStyle = styles.get(line);
			if (lineStyle != null)
				return lineStyle;

			// check for product match
			final Style productStyle = styles.get(new Character(line.charAt(0)).toString());
			if (productStyle != null)
				return productStyle;

			// check for night bus, as that's a common special case
			if (line.startsWith("BN"))
			{
				final Style nightStyle = styles.get("BN");
				if (nightStyle != null)
					return nightStyle;
			}
		}

		// standard colors
		return Standard.STYLES.get(line.charAt(0));
	}

	public Point[] getArea()
	{
		return null;
	}
}
