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

package de.schildbach.pte.live;

import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractProviderLiveTest
{
	protected final void print(final QueryDeparturesResult result)
	{
		System.out.println(result.status + (result.status == Status.OK ? " " + result.stationDepartures : ""));

		// for (final StationDepartures stationDepartures : result.stationDepartures)
		// for (final Departure departures : stationDepartures.departures)
		// System.out.println(departures.line);
	}
}
