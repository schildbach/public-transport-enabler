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
package de.schildbach.pte.live;

import java.util.List;

import org.junit.Test;

import de.schildbach.pte.Autocomplete;
import de.schildbach.pte.GvhProvider;
import de.schildbach.pte.Station;

/**
 * @author Andreas Schildbach
 */
public class GvhProviderLiveTest
{
	private final GvhProvider provider = new GvhProvider();

	@Test
	public void autocomplete() throws Exception
	{
		final List<Autocomplete> results = provider.autocompleteStations("Hannover");

		System.out.println(results.size() + "  " + results);
	}

	@Test
	public void nearby() throws Exception
	{
		final List<Station> results = provider.nearbyStations("25000031", 0, 0, 0, 0);

		System.out.println(results.size() + "  " + results);
	}
}
