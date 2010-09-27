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

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://mobil.gvh.de/mobile2/";

	private static final String AUTOCOMPLETE_URI = API_BASE
			+ "XML_STOPFINDER_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&name_sf=%s&type_sf=any";
	private static final String ENCODING = "ISO-8859-1";

	@Override
	protected String autocompleteUri(final CharSequence constraint)
	{
		return String.format(AUTOCOMPLETE_URI, ParserUtils.urlEncode(constraint.toString(), ENCODING));
	}

	private static final String NEARBY_STATION_URI = API_BASE
			+ "XSLT_DM_REQUEST?outputFormat=XML&coordOutputFormat=WGS84&name_dm=%s&type_dm=stop&itOptionsActive=1&ptOptionsActive=1&useProxFootSearch=1&mergeDep=1&useAllStops=1&mode=direct";

	@Override
	protected String nearbyStationUri(final String stationId)
	{
		return String.format(NEARBY_STATION_URI, stationId);
	}

	@Override
	protected String nearbyLatLonUri(final double lat, final double lon)
	{
		return null;
	}

	public String departuresQueryUri(String stationId, int maxDepartures)
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		throw new UnsupportedOperationException();
	}

	public int[] lineColors(String line)
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(LocationType fromType, String from, LocationType viaType, String via, LocationType toType,
			String to, Date date, boolean dep, WalkSpeed walkSpeed) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryDeparturesResult queryDepartures(String queryUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public StationLocationResult stationLocation(String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
