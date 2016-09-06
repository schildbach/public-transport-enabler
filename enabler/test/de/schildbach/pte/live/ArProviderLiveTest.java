/*
 * Copyright 2010-2016 the original author or authors.
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

package de.schildbach.pte.live;

import org.junit.Test;
import de.schildbach.pte.ArProvider;

/**
 * @author Ignacio Caamaño <ignacio167@gmail.com>
 */

public class ArProviderLiveTest extends AbstractNavitiaProviderLiveTest 
{

	public ArProviderLiveTest() 
	{
		super(new ArProvider(secretProperty("navitia.authorization")));
	}

	@Test
	public void nearbyStationsAddress() throws Exception 
	{
		nearbyStationsAddress(-34618072, -58436443);
	}

	@Test
	public void suggestLocations() throws Exception 
	{
		suggestLocationsFromName("plaza de mayo");
	}
}
