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

package de.schildbach.pte;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class MerseyProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "http://jp.merseytravel.gov.uk/nwm/";

	public MerseyProvider()
	{
		super(NetworkId.MERSEY, API_BASE);

		setLanguage("en");
		setTimeZone("Europe/London");
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final Pattern P_POSITION_BOUND = Pattern.compile("([NESW]+)-bound", Pattern.CASE_INSENSITIVE);

	@Override
	protected Position parsePosition(final String position)
	{
		if (position == null)
			return null;

		final Matcher m = P_POSITION_BOUND.matcher(position);
		if (m.matches())
			return new Position(m.group(1));

		return super.parsePosition(position);
	}
}
