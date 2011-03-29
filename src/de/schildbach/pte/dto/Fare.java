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
import java.util.Currency;

/**
 * @author Andreas Schildbach
 */
public final class Fare implements Serializable
{
	public enum Type
	{
		ADULT, CHILD, YOUTH, STUDENT, MILITARY, SENIOR, DISABLED
	}

	public final String network;
	public final Type type;
	public final Currency currency;
	public final float fare;
	public final String unitName;
	public final String units;

	public Fare(final String network, final Type type, final Currency currency, final float fare, final String unitName, final String units)
	{
		this.network = network;
		this.type = type;
		this.currency = currency;
		this.fare = fare;
		this.unitName = unitName;
		this.units = units;
	}
}
