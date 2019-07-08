/*
 * Copyright the original author or authors.
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
import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.schildbach.pte.RtProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.TripOptions;

/**
 * @author Andreas Schildbach
 * @author Felix Delattre
 */
@Controller
public class TripsController {
    private final RtProvider provider = new RtProvider();

    @RequestMapping(value = "/trips/query", method = RequestMethod.GET)
    @ResponseBody
    public QueryTripsResult query(
            @RequestParam(value = "fromType", required = false, defaultValue = "ANY") final LocationType fromType,
            @RequestParam(value = "from", required = false) final String from,
            @RequestParam(value = "fromId", required = false) final String fromId,
            @RequestParam(value = "viaType", required = false, defaultValue = "ANY") final LocationType viaType,
            @RequestParam(value = "via", required = false) final String via,
            @RequestParam(value = "viaId", required = false) final String viaId,
            @RequestParam(value = "toType", required = false, defaultValue = "ANY") final LocationType toType,
            @RequestParam(value = "to", required = false) final String to,
            @RequestParam(value = "toId", required = false) final String toId,
            @RequestParam(value = "date", required = false) final String date,
            @RequestParam(value = "dep", required = false, defaultValue = "true") final Boolean dep,
            @RequestParam(value = "options", required = false) final TripOptions options)
            throws IOException {
        final Location fromLocation = new Location(fromType, fromId, null, from);
        final Location toLocation = new Location(toType, toId, null, to);
        final Location viaLocation = new Location(viaType, viaId, null, via);
        return provider.queryTrips(fromLocation, viaLocation, toLocation, new Date(), dep, options);
    }

    @RequestMapping(value = "/trips/more", method = RequestMethod.GET)
    @ResponseBody
    public QueryTripsResult more(
            @RequestParam(value = "context", required = true) final QueryTripsContext context,
            @RequestParam(value = "later", required = false, defaultValue = "true") final Boolean later)
            throws IOException {
        return provider.queryMoreTrips(context, later);
    }

    @RequestMapping(value = "/trips/style", method = RequestMethod.GET)
    @ResponseBody
    public Style style(
            @RequestParam(value = "network", required = false) final String network,
            @RequestParam(value = "product", required = false) final Product product,
            @RequestParam(value = "label", required = false) final String label)
            throws IOException {
        return provider.lineStyle(network, product, label);
    }
}
