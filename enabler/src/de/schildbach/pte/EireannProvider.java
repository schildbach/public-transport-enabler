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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * Ireland, Dublin
 * 
 * @author Andreas Schildbach
 */
public class EireannProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://journeyplanner.buseireann.ie/jp/bin/";

	public EireannProvider()
	{
		super(NetworkId.EIREANN, API_BASE + "stboard.exe/en", API_BASE + "ajax-getstop.exe/en", API_BASE + "query.exe/en", 4);

		setStationBoardHasStationTable(false);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
		}
		else if (product == Product.SUBWAY)
		{
		}
		else if (product == Product.TRAM)
		{
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(3, '1');
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final @Nullable Location via, final Location to, final Date date, final boolean dep,
			final @Nullable Set<Product> products, final @Nullable WalkSpeed walkSpeed, final @Nullable Accessibility accessibility,
			final @Nullable Set<Option> options) throws IOException
	{
		return queryTripsXml(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return queryMoreTripsXml(context, later);
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([^#]+)#");

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		final Matcher mLine = P_NORMALIZE_LINE.matcher(lineAndType);
		if (mLine.matches())
			return newLine(Product.BUS, mLine.group(1), null);

		return super.parseLineAndType(lineAndType);
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("COA".equals(ucType))
			return Product.BUS;
		if ("CIT".equals(ucType))
			return Product.BUS;

		return null;
	}
}
