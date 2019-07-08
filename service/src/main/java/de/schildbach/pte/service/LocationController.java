/*
 * Copyright 2012-2015 the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.service;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.schildbach.pte.RtProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.SuggestLocationsResult;

/**
 * @author Andreas Schildbach
 * @author Felix Delattre
 */
@Controller
public class LocationController {
    private final RtProvider provider = new RtProvider();

    @RequestMapping(value = "/location/nearby", method = RequestMethod.GET)
    @ResponseBody
    public NearbyLocationsResult nearby(
            @RequestParam(value = "types", required = false, defaultValue = "ANY") final EnumSet<LocationType> types,
            @RequestParam(value = "lat", required = true) final int lat,
            @RequestParam(value = "lon", required = true) final int lon,
            @RequestParam(value = "maxDistance", required = false, defaultValue = "5000") final Integer maxDistance,
            @RequestParam(value = "maxLocations", required = false, defaultValue = "100") final Integer maxLocations)
            throws IOException {
        final Location location = Location.coord(lat, lon);
        return provider.queryNearbyLocations(types, location, maxDistance, maxLocations);
    }

    @RequestMapping(value = "/location/departures", method = RequestMethod.GET)
    @ResponseBody
    public QueryDeparturesResult departures(
            @RequestParam(value = "stationId", required = true) final String stationId,
            //@RequestParam(value = "time", required = false, defaultValue = "100") final Date time,
            @RequestParam(value = "maxDepartures", required = false, defaultValue = "25") final Integer maxDepartures,
            @RequestParam(value = "equivs", required = false, defaultValue = "false") final Boolean equivs)
            throws IOException {
        return provider.queryDepartures(stationId, new Date(), maxDepartures, equivs);
    }

    @RequestMapping(value = "/location/suggest", method = RequestMethod.GET)
    @ResponseBody
    public SuggestLocationsResult suggest(
            @RequestParam(value = "constraint", required = true) final CharSequence constraint,
            @RequestParam(value = "types", required = false, defaultValue = "ANY") final EnumSet<LocationType> types,
            @RequestParam(value = "maxLocations", required = false, defaultValue = "25") final Integer maxLocations)
            throws IOException {
        return provider.suggestLocations(constraint, types, maxLocations);
    }
}
