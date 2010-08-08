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

package de.schildbach.pte;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Interface to be implemented by providers of transportation networks
 * 
 * @author Andreas Schildbach
 */
public interface NetworkProvider
{
	public enum Capability
	{
		NEARBY_STATIONS, DEPARTURES, CONNECTIONS
	}

	boolean hasCapabilities(Capability... capabilities);

	/**
	 * Meant for auto-completion of station names, like in an {@link android.widget.AutoCompleteTextView}
	 * 
	 * @param constraint
	 *            input by user so far
	 * @return auto-complete suggestions
	 * @throws IOException
	 */
	List<String> autoCompleteStationName(CharSequence constraint) throws IOException;

	/**
	 * Determine stations near to given location
	 * 
	 * @param lat
	 *            latitude
	 * @param lon
	 *            longitude
	 * @param maxDistance
	 *            maximum distance in meters, or {@code 0}
	 * @param maxStations
	 *            maximum number of stations, or {@code 0}
	 * @return nearby stations
	 * @throws IOException
	 */
	List<Station> nearbyStations(double lat, double lon, int maxDistance, int maxStations) throws IOException;

	/**
	 * Look up location of station.
	 * 
	 * @param stationId
	 *            id of station to look up
	 * @return location
	 * @throws IOException
	 */
	StationLocationResult stationLocation(String stationId) throws IOException;

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
	 * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible connections
	 * @throws IOException
	 */
	QueryConnectionsResult queryConnections(String from, String via, String to, Date date, boolean dep) throws IOException;

	/**
	 * Query more connections (e.g. earlier or later)
	 * 
	 * @param uri
	 *            uri to query more connections from
	 * @return result object that contains possible connections
	 * @throws IOException
	 */
	QueryConnectionsResult queryMoreConnections(String uri) throws IOException;

	/**
	 * Get details about a connection
	 * 
	 * @param connectionUri
	 *            uri returned via {@link NetworkProvider#queryConnections}
	 * @return result object containing the details of the connection
	 * @throws IOException
	 */
	GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException;

	/**
	 * Construct an Uri for getting departures
	 * 
	 * @param stationId
	 *            id of the station
	 * @param maxDepartures
	 *            maximum number of departures to get or {@code 0}
	 * @return uri for getting departures
	 */
	String departuresQueryUri(String stationId, int maxDepartures);

	/**
	 * Get departures at a given station, probably live
	 * 
	 * @param queryUri
	 *            uri constructed by {@link NetworkProvider#departuresQueryUri}
	 * @return result object containing the departures
	 * @throws IOException
	 */
	QueryDeparturesResult queryDepartures(String queryUri) throws IOException;

	/**
	 * Get colors of line
	 * 
	 * @param line
	 *            line to get color of
	 * @return array containing background, foreground and border (optional) colors
	 */
	int[] lineColors(String line);
}
