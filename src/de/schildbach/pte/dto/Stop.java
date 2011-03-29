/*
 * Copyright 2010, 2011 the original author or authors.
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
import java.util.Date;

/**
 * @author Andreas Schildbach
 */
public final class Stop implements Serializable
{
	public final Location location;
	public final String position;
	public final Date time;

	public Stop(final Location location, final String position, final Date time)
	{
		this.location = location;
		this.position = position;
		this.time = time;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Stop(");
		builder.append(location);
		builder.append(",");
		builder.append(position != null ? position : "null");
		builder.append(",");
		builder.append(time != null ? time : "null");
		builder.append(")");
		return builder.toString();
	}
}
