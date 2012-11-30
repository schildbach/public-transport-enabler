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

package de.schildbach.pte.dto;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Andreas Schildbach
 */
public final class GetConnectionDetailsResult implements Serializable
{
	public final Date currentDate;
	public final Connection connection;

	public GetConnectionDetailsResult(Date currentDate, Connection connection)
	{
		this.currentDate = currentDate;
		this.connection = connection;
	}

	@Override
	public String toString()
	{
		final SimpleDateFormat FORMAT = new SimpleDateFormat("EE dd.MM.yy", Locale.US);
		return FORMAT.format(currentDate) + "|" + connection.toString();
	}
}
