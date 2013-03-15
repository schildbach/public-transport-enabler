/*
 * Copyright 2010-2013 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

/**
 * @author Andreas Schildbach
 */
public enum Product
{
	HIGH_SPEED_TRAIN('I'), REGIONAL_TRAIN('R'), SUBURBAN_TRAIN('S'), SUBWAY('U'), TRAM('T'), BUS('B'), FERRY('F'), CABLECAR('C'), ON_DEMAND('P');

	public static final char UNKNOWN = '?';
	public static final List<Product> ALL = Arrays.asList(values());

	public final char code;

	private Product(final char code)
	{
		this.code = code;
	}

	public static Product fromCode(char code)
	{
		if (code == HIGH_SPEED_TRAIN.code)
			return HIGH_SPEED_TRAIN;
		else if (code == REGIONAL_TRAIN.code)
			return REGIONAL_TRAIN;
		else if (code == SUBURBAN_TRAIN.code)
			return SUBURBAN_TRAIN;
		else if (code == SUBWAY.code)
			return SUBWAY;
		else if (code == TRAM.code)
			return TRAM;
		else if (code == BUS.code)
			return BUS;
		else if (code == FERRY.code)
			return FERRY;
		else if (code == CABLECAR.code)
			return CABLECAR;
		else if (code == ON_DEMAND.code)
			return ON_DEMAND;
		else
			throw new IllegalArgumentException(Character.toString(code));
	}
}
