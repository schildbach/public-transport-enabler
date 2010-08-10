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

import java.util.Date;

import org.junit.Test;

import de.schildbach.pte.QueryConnectionsResult;
import de.schildbach.pte.SbbProvider;

/**
 * @author Andreas Schildbach
 */
public class SbbProviderLiveTest
{
	private SbbProvider provider = new SbbProvider();

	@Test
	public void fastConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections("Zürich!", null, "Bern", new Date(), true);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}

	@Test
	public void slowConnection() throws Exception
	{
		final QueryConnectionsResult result = provider.queryConnections("Schocherswil, Alte Post!", null, "Laconnex, Mollach", new Date(), true);
		System.out.println(result);
		final QueryConnectionsResult moreResult = provider.queryMoreConnections(result.linkLater);
		System.out.println(moreResult);
	}
}
