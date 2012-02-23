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

/**
 * @author Andreas Schildbach
 */
public final class ResultHeader implements Serializable
{
	public final String serverProduct;
	public final String serverVersion;
	public final long serverTime;
	public final String context;

	public ResultHeader(final String serverProduct)
	{
		this.serverProduct = serverProduct;
		this.serverVersion = null;
		this.serverTime = 0;
		this.context = null;
	}

	public ResultHeader(final String serverProduct, final String serverVersion, final long serverTime, final String context)
	{
		this.serverProduct = serverProduct;
		this.serverVersion = serverVersion;
		this.serverTime = serverTime;
		this.context = context;
	}
}
