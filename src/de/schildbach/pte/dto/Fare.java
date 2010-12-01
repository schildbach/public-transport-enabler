/*
 * Copyright 2010 the original author or authors.
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
import java.util.Currency;

/**
 * @author Andreas Schildbach
 */
public final class Fare implements Serializable
{
	public final String network;
	public final Currency currency;
	public final String unitName;
	public final float fareAdult;
	public final float fareChild;
	public final int unitsAdult;
	public final int unitsChild;

	public Fare(final String network, final Currency currency, final String unitName, final float fareAdult, final float fareChild,
			final int unitsAdult, final int unitsChild)
	{
		this.network = network;
		this.currency = currency;
		this.unitName = unitName;
		this.fareAdult = fareAdult;
		this.fareChild = fareChild;
		this.unitsAdult = unitsAdult;
		this.unitsChild = unitsChild;
	}
}
