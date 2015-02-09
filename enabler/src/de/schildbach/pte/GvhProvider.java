/*
 * Copyright 2010-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class GvhProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://bhb.efa.de/bhb/";

	// http://www.efa.de/efaws2/cmsembedded_gvh/
	// http://bhb.efa.de/bhb/
	// http://mobil.efa.de/mobile3/

	public GvhProvider(final String additionalQueryParameter)
	{
		super(NetworkId.GVH, API_BASE);

		setAdditionalQueryParameter(additionalQueryParameter);
		setStyles(STYLES);
		setSessionCookieName("HASESSIONID");
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Hamburg
		STYLES.put("SS1", new Style(Style.parseColor("#00933B"), Style.WHITE));
		STYLES.put("SS11", new Style(Style.WHITE, Style.parseColor("#00933B"), Style.parseColor("#00933B")));
		STYLES.put("SS2", new Style(Style.WHITE, Style.parseColor("#9D271A"), Style.parseColor("#9D271A")));
		STYLES.put("SS21", new Style(Style.parseColor("#9D271A"), Style.WHITE));
		STYLES.put("SS3", new Style(Style.parseColor("#411273"), Style.WHITE));
		STYLES.put("SS31", new Style(Style.parseColor("#411273"), Style.WHITE));

		STYLES.put("UU1", new Style(Style.parseColor("#044895"), Style.WHITE));
		STYLES.put("UU2", new Style(Style.parseColor("#DC2B19"), Style.WHITE));
		STYLES.put("UU3", new Style(Style.parseColor("#EE9D16"), Style.WHITE));
		STYLES.put("UU4", new Style(Style.parseColor("#13A59D"), Style.WHITE));
	}
}
