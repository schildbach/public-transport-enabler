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
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class VmvProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://80.146.180.107/vmv2/";

	// http://80.146.180.107/vmv/
	// http://80.146.180.107/delfi/

	public VmvProvider()
	{
		super(NetworkId.VMV, API_BASE);

		setUseRouteIndexAsTripId(false);
		setStyles(STYLES);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Rostock
		STYLES.put("ddb|SS1", new Style(Shape.CIRCLE, Style.parseColor("#108449"), Style.WHITE));
		STYLES.put("ddb|SS2", new Style(Shape.CIRCLE, Style.parseColor("#66a933"), Style.WHITE));
		STYLES.put("ddb|SS3", new Style(Shape.CIRCLE, Style.parseColor("#a6d71c"), Style.WHITE));

		STYLES.put("vvw|T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("vvw|T2", new Style(Shape.RECT, Style.parseColor("#ca5497"), Style.WHITE));
		STYLES.put("vvw|T3", new Style(Shape.RECT, Style.parseColor("#f04145"), Style.WHITE));
		STYLES.put("vvw|T4", new Style(Shape.RECT, Style.parseColor("#c40070"), Style.WHITE));
		STYLES.put("vvw|T5", new Style(Shape.RECT, Style.parseColor("#9c1116"), Style.WHITE));
		STYLES.put("vvw|T6", new Style(Shape.RECT, Style.parseColor("#ee255c"), Style.WHITE));

		STYLES.put("vvw|B15", new Style(Style.parseColor("#008dc6"), Style.WHITE));
		STYLES.put("vvw|B16", new Style(Style.parseColor("#1d3c85"), Style.WHITE));
		STYLES.put("vvw|B17", new Style(Style.parseColor("#5784cc"), Style.WHITE));
		STYLES.put("vvw|B18", new Style(Style.parseColor("#0887c9"), Style.WHITE));
		STYLES.put("vvw|B19", new Style(Style.parseColor("#202267"), Style.WHITE));
		STYLES.put("vvw|B19A", new Style(Style.parseColor("#80d6f7"), Style.WHITE));
		STYLES.put("vvw|B20A", new Style(Style.parseColor("#1959a6"), Style.WHITE));
		STYLES.put("vvw|B22", new Style(Style.parseColor("#3871c1"), Style.WHITE));
		STYLES.put("vvw|B23", new Style(Style.parseColor("#009ddb"), Style.WHITE));
		STYLES.put("vvw|B25", new Style(Style.parseColor("#066ba3"), Style.WHITE));
		STYLES.put("vvw|B26", new Style(Style.parseColor("#0994dc"), Style.WHITE));
		STYLES.put("vvw|B27", new Style(Style.parseColor("#6e87cd"), Style.WHITE));
		STYLES.put("vvw|B28", new Style(Style.parseColor("#4fc6f4"), Style.WHITE));
		STYLES.put("vvw|B30A", new Style(Style.parseColor("#80d6f7"), Style.WHITE));
		STYLES.put("vvw|B31", new Style(Style.parseColor("#3a9fdf"), Style.WHITE));
		STYLES.put("vvw|B33", new Style(Style.parseColor("#4081cb"), Style.WHITE));
		STYLES.put("vvw|B33A", new Style(Style.parseColor("#80d6f7"), Style.WHITE));
		STYLES.put("vvw|B35", new Style(Style.parseColor("#254aa5"), Style.WHITE));
		STYLES.put("vvw|B35A", new Style(Style.parseColor("#005e8a"), Style.WHITE));
		STYLES.put("vvw|B36", new Style(Style.parseColor("#2c6d8b"), Style.WHITE));
		STYLES.put("vvw|B37", new Style(Style.parseColor("#36aee8"), Style.WHITE));
		STYLES.put("vvw|B38", new Style(Style.parseColor("#10508c"), Style.WHITE));
		STYLES.put("vvw|B45", new Style(Style.parseColor("#6ab0cc"), Style.WHITE));
		STYLES.put("vvw|B45A", new Style(Style.parseColor("#5784cc"), Style.WHITE));
		STYLES.put("vvw|B49", new Style(Style.parseColor("#1959a6"), Style.WHITE));
		STYLES.put("vvw|BF1", new Style(Style.parseColor("#f8640e"), Style.WHITE));
		STYLES.put("vvw|BF1A", new Style(Style.parseColor("#f8640e"), Style.WHITE));
		STYLES.put("vvw|BF2", new Style(Style.parseColor("#ffaf00"), Style.WHITE));
	}
}
