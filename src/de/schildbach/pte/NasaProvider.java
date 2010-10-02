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
 * @author Andreas Schildbach
 */
public class NasaProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://www.nasa.de/delfi52/";

	public boolean hasCapabilities(Capability... capabilities)
	{
		throw new UnsupportedOperationException();
	}

	public List<Autocomplete> autocompleteStations(CharSequence constraint) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private final String NEARBY_URI = API_BASE + "stboard.exe/dn?input=%s&selectDate=today&boardType=dep&productsFilter=11111111&distance=50&near=Anzeigen";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_URI, stationId);
	}

	public StationLocationResult stationLocation(String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(String stationId, int maxDepartures)
	{
		throw new UnsupportedOperationException();
	}

	public QueryDeparturesResult queryDepartures(String queryUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(LocationType fromType, String from, LocationType viaType, String via, LocationType toType,
			String to, Date date, boolean dep, WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public int[] lineColors(String line)
	{
		throw new UnsupportedOperationException();
	}
}
