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

import de.schildbach.pte.dto.Position;

/**
 * @author Andreas Schildbach
 */
public class MvgProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://mobil.mvg-online.de/mvgMobil/";

	public MvgProvider()
	{
		super(NetworkId.MVG, API_BASE);
	}

	@Override
	protected Position parsePosition(final String position)
	{
		if (position == null)
			return null;

		if (position.startsWith(" - "))
			return super.parsePosition(position.substring(3));

		return super.parsePosition(position);
	}
}
