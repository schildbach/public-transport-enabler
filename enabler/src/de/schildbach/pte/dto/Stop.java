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
import java.util.Date;

/**
 * @author Andreas Schildbach
 */
public final class Stop implements Serializable
{
	public final Location location;
	public final Date plannedArrivalTime;
	public final Date predictedArrivalTime;
	public final String plannedArrivalPosition;
	public final String predictedArrivalPosition;
	public final Date time; // TODO rename to plannedDepartureTime
	public final Date predictedDepartureTime;
	public final String position; // TODO rename to plannedPosition
	public final String predictedDeparturePosition;

	public Stop(final Location location, final Date plannedArrivalTime, final Date predictedArrivalTime, final String plannedArrivalPosition,
			final String predictedArrivalPosition, final Date plannedDepartureTime, final Date predictedDepartureTime,
			final String plannedDeparturePosition, final String predictedDeparturePosition)
	{
		this.location = location;
		this.plannedArrivalTime = plannedArrivalTime;
		this.predictedArrivalTime = predictedArrivalTime;
		this.plannedArrivalPosition = plannedArrivalPosition;
		this.predictedArrivalPosition = predictedArrivalPosition;
		this.time = plannedDepartureTime;
		this.predictedDepartureTime = predictedDepartureTime;
		this.position = plannedDeparturePosition;
		this.predictedDeparturePosition = predictedDeparturePosition;
	}

	public Stop(final Location location, final Date plannedArrivalTime, final String plannedArrivalPosition, final Date plannedDepartureTime,
			final String plannedDeparturePosition)
	{
		this.location = location;
		this.plannedArrivalTime = plannedArrivalTime;
		this.predictedArrivalTime = null;
		this.plannedArrivalPosition = plannedArrivalPosition;
		this.predictedArrivalPosition = null;
		this.time = plannedDepartureTime;
		this.predictedDepartureTime = null;
		this.position = plannedDeparturePosition;
		this.predictedDeparturePosition = null;
	}

	public Date getArrivalTime()
	{
		if (predictedArrivalTime != null)
			return predictedArrivalTime;
		else if (plannedArrivalTime != null)
			return plannedArrivalTime;
		else
			return null;
	}

	public boolean isArrivalTimePredicted()
	{
		return predictedArrivalTime != null;
	}

	public Long getArrivalDelay()
	{
		if (plannedArrivalTime != null && predictedArrivalTime != null)
			return predictedArrivalTime.getTime() - plannedArrivalTime.getTime();
		else
			return null;
	}

	public String getArrivalPosition()
	{
		if (predictedArrivalPosition != null)
			return predictedArrivalPosition;
		else if (plannedArrivalPosition != null)
			return plannedArrivalPosition;
		else
			return null;
	}

	public boolean isArrivalPositionPredicted()
	{
		return predictedArrivalPosition != null;
	}

	public Date getDepartureTime()
	{
		if (predictedDepartureTime != null)
			return predictedDepartureTime;
		else if (time != null)
			return time;
		else
			return null;
	}

	public boolean isDepartureTimePredicted()
	{
		return predictedDepartureTime != null;
	}

	public Long getDepartureDelay()
	{
		if (time != null && predictedDepartureTime != null)
			return predictedDepartureTime.getTime() - time.getTime();
		else
			return null;
	}

	public String getDeparturePosition()
	{
		if (predictedDeparturePosition != null)
			return predictedDeparturePosition;
		else if (position != null)
			return position;
		else
			return null;
	}

	public boolean isDeparturePositionPredicted()
	{
		return predictedDeparturePosition != null;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Stop(");
		builder.append(location);
		builder.append(",");
		builder.append(plannedArrivalTime != null ? plannedArrivalTime : "null");
		builder.append(",");
		builder.append(predictedArrivalTime != null ? predictedArrivalTime : "null");
		builder.append(",");
		builder.append(plannedArrivalPosition != null ? plannedArrivalPosition : "null");
		builder.append(",");
		builder.append(predictedArrivalPosition != null ? predictedArrivalPosition : "null");
		builder.append(",");
		builder.append(time != null ? time : "null");
		builder.append(",");
		builder.append(predictedDepartureTime != null ? predictedDepartureTime : "null");
		builder.append(",");
		builder.append(position != null ? position : "null");
		builder.append(",");
		builder.append(predictedDeparturePosition != null ? predictedDeparturePosition : "null");
		builder.append(")");
		return builder.toString();
	}
}
