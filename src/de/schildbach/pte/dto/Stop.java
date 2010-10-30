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
