/*
 * Copyright 2012 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.schildbach.pte.NetworkProvider.Accessibility;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.RtProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsResult;

/**
 * @author Andreas Schildbach
 */
@Controller
public class LocationController
{
	private final RtProvider provider = new RtProvider();

	@RequestMapping(value = "/location/suggest", method = RequestMethod.GET)
	@ResponseBody
	public List<Location> autocomplete(@RequestParam("q") final String query) throws IOException
	{
		return provider.autocompleteStations(query);
	}

	@RequestMapping(value = "/location/nearby", method = RequestMethod.GET)
	@ResponseBody
	public NearbyStationsResult nearby(@RequestParam("lat") final int lat, @RequestParam("lon") final int lon) throws IOException
	{
		final Location location = new Location(LocationType.ANY, lat, lon);
		return provider.queryNearbyStations(location, 5000, 100);
	}

	@RequestMapping(value = "/connection", method = RequestMethod.GET)
	@ResponseBody
	public QueryConnectionsResult connection(@RequestParam("from") final String from, @RequestParam("fromId") final int fromId,
			@RequestParam("to") final String to, @RequestParam("toId") final int toId) throws IOException
	{
		final Location fromLocation = new Location(LocationType.ANY, fromId, null, from);
		final Location toLocation = new Location(LocationType.ANY, toId, null, to);
		final String products = "IRSUTBFC";
		return provider.queryConnections(fromLocation, null, toLocation, new Date(), true, products, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
	}
}
