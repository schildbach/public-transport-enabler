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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractNetworkProvider implements NetworkProvider
{
	protected static final Set<Product> ALL_EXCEPT_HIGHSPEED = EnumSet.complementOf(EnumSet.of(Product.HIGH_SPEED_TRAIN));

	protected TimeZone timeZone = TimeZone.getTimeZone("CET");
	protected int numTripsRequested = 6;
	private Map<String, Style> styles = null;
	protected String sessionCookieName = null;

	public final boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (!hasCapability(capability))
				return false;

		return true;
	}

	protected abstract boolean hasCapability(Capability capability);

	public Set<Product> defaultProducts()
	{
		return ALL_EXCEPT_HIGHSPEED;
	}

	protected void setTimeZone(final String timeZoneId)
	{
		this.timeZone = TimeZone.getTimeZone(timeZoneId);
	}

	protected void setNumTripsRequested(final int numTripsRequested)
	{
		this.numTripsRequested = numTripsRequested;
	}

	protected void setStyles(final Map<String, Style> styles)
	{
		this.styles = styles;
	}

	protected void setSessionCookieName(final String sessionCookieName)
	{
		this.sessionCookieName = sessionCookieName;
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

	public Point[] getArea() throws IOException
	{
		return null;
	}

	protected static String normalizeStationId(final String stationId)
	{
		if (stationId == null || stationId.length() == 0)
			return null;

		if (stationId.charAt(0) != '0')
			return stationId;

		final StringBuilder normalized = new StringBuilder(stationId);
		while (normalized.length() > 0 && normalized.charAt(0) == '0')
			normalized.deleteCharAt(0);

		return normalized.toString();
	}

	private static final Pattern P_NAME_SECTION = Pattern.compile("(\\d+)\\s*" + //
			"([A-Z](?:\\s*-?\\s*[A-Z])?)?", Pattern.CASE_INSENSITIVE);

	private static final Pattern P_NAME_NOSW = Pattern.compile("(\\d+)\\s*" + //
			"(Nord|Süd|Ost|West)", Pattern.CASE_INSENSITIVE);

	protected Position parsePosition(final String position)
	{
		if (position == null)
			return null;

		final Matcher mSection = P_NAME_SECTION.matcher(position);
		if (mSection.matches())
		{
			final String name = Integer.toString(Integer.parseInt(mSection.group(1)));
			if (mSection.group(2) != null)
				return new Position(name, mSection.group(2).replaceAll("\\s+", ""));
			else
				return new Position(name);
		}

		final Matcher mNosw = P_NAME_NOSW.matcher(position);
		if (mNosw.matches())
			return new Position(Integer.toString(Integer.parseInt(mNosw.group(1))), mNosw.group(2).substring(0, 1));

		return new Position(position);
	}
}
