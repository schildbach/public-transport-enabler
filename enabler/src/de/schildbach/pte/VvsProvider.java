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

import de.schildbach.pte.dto.Point;

/**
 * @author Andreas Schildbach
 */
public class VvsProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://www2.vvs.de/vvs/";

	public VvsProvider()
	{
		this(API_BASE);
	}

	public VvsProvider(final String apiBase)
	{
		super(NetworkId.VVS, apiBase);

		setIncludeRegionId(false);
		setNumTripsRequested(4);
	}

	@Override
	public Point[] getArea()
	{
		return new Point[] { Point.fromDouble(48.784068, 9.181713) };
	}
}
