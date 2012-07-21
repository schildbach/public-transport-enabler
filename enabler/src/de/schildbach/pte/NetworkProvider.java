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

package de.schildbach.pte;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.QueryConnectionsContext;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.Style;

/**
 * Interface to be implemented by providers of transportation networks.
 * 
 * @author Andreas Schildbach
 */
public interface NetworkProvider
{
	public enum Capability
	{
		/* can autocomplete locations */
		AUTOCOMPLETE_ONE_LINE,
		/* can determine nearby stations */
		NEARBY_STATIONS,
		/* can query for departures */
		DEPARTURES,
		/* can query connections */
		CONNECTIONS
	}

	public enum WalkSpeed
	{
		SLOW, NORMAL, FAST
	}

	public enum Accessibility
	{
		NEUTRAL, LIMITED, BARRIER_FREE
	}

	public enum Option
	{
		BIKE
	}

	NetworkId id();

	boolean hasCapabilities(Capability... capabilities);

	/**
	 * Determine stations near to given location. At least one of stationId or lat/lon pair must be present.
	 * 
	 * @param location
	 *            location to determine nearby stations (optional)
	 * @param maxDistance
	 *            maximum distance in meters, or {@code 0}
	 * @param maxStations
	 *            maximum number of stations, or {@code 0}
	 * @return nearby stations
	 * @throws IOException
	 */
	NearbyStationsResult queryNearbyStations(Location location, int maxDistance, int maxStations) throws IOException;

	/**
	 * Get departures at a given station, probably live
	 * 
	 * @param stationId
	 *            id of the station
	 * @param maxDepartures
	 *            maximum number of departures to get or {@code 0}
	 * @param equivs
	 *            also query equivalent stations?
	 * @return result object containing the departures
	 * @throws IOException
	 */
	QueryDeparturesResult queryDepartures(int stationId, int maxDepartures, boolean equivs) throws IOException;

	/**
	 * Meant for auto-completion of station names, like in an {@link android.widget.AutoCompleteTextView}
	 * 
	 * @param constraint
	 *            input by user so far
	 * @return auto-complete suggestions
	 * @throws IOException
	 */
	List<Location> autocompleteStations(CharSequence constraint) throws IOException;

	/**
	 * Query connections, asking for any ambiguousnesses
	 * 
	 * @param from
	 *            location to route from, mandatory
	 * @param via
	 *            location to route via, may be {@code null}
	 * @param to
	 *            location to route to, mandatory
	 * @param date
	 *            desired date for departing, mandatory
	 * @param dep
	 *            date is departure date? {@code true} for departure, {@code false} for arrival
	 * @param numConnections
	 *            number of connections to query
	 * @param products
	 *            products to take into account
	 * @param walkSpeed
	 *            how fast can you walk?
	 * @param accessibility
	 *            how accessible do you need the route to be?
	 * @param options
	 *            additional options
	 * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible connections
	 * @throws IOException
	 */
	QueryConnectionsResult queryConnections(Location from, Location via, Location to, Date date, boolean dep, int numConnections, String products,
			WalkSpeed walkSpeed, Accessibility accessibility, Set<Option> options) throws IOException;

	/**
	 * Query more connections (e.g. earlier or later)
	 * 
	 * @param context
	 *            context to query more connections from
	 * @param next
	 *            {@code true} for get next connections, {@code false} for get previous connections
	 * @param numConnections
	 *            number of connections to query
	 * @return result object that contains possible connections
	 * @throws IOException
	 */
	QueryConnectionsResult queryMoreConnections(QueryConnectionsContext context, boolean later, int numConnections) throws IOException;

	/**
	 * Get style of line
	 * 
	 * @param line
	 *            line to get style of
	 * @return object containing background, foreground and border (optional) colors
	 */
	Style lineStyle(String line);

	/**
	 * Gets the primary covered area of the network
	 * 
	 * @return array containing points of a polygon (special case: just one coordinate defines just a center point)
	 */
	Point[] getArea();
}
