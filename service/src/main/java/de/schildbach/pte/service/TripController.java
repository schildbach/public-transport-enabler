/*
 * Copyright 2012-2013 the original author or authors.
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
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * @author Andreas Schildbach
 */
@Controller
public class TripController
{
	private final RtProvider provider = new RtProvider();

	@RequestMapping(value = "/trip", method = RequestMethod.GET)
	@ResponseBody
	public QueryTripsResult trip(@RequestParam(value = "fromType", required = false, defaultValue = "ANY") final LocationType fromType,
			@RequestParam(value = "from", required = false) final String from,
			@RequestParam(value = "fromId", required = false, defaultValue = "0") final int fromId,
			@RequestParam(value = "toType", required = false, defaultValue = "ANY") final LocationType toType,
			@RequestParam(value = "to", required = false) final String to,
			@RequestParam(value = "toId", required = false, defaultValue = "0") final int toId) throws IOException
	{
		final Location fromLocation = new Location(fromType, fromId, null, from);
		final Location toLocation = new Location(toType, toId, null, to);
		return provider.queryTrips(fromLocation, null, toLocation, new Date(), true, 4, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL, null);
	}
}
