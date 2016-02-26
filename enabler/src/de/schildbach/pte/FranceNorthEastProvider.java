/*
 * Copyright 2016 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Nicolas Derive
 * @author Stéphane Guillou
 */
public class FranceNorthEastProvider extends AbstractNavitiaProvider
{
	private static String API_REGION = "fr-ne";
	// dataset available at: https://navitia.opendatasoft.com/explore/dataset/fr-ne/

	public FranceNorthEastProvider(final String apiBase, final String authorization)
	{
		super(NetworkId.FRANCENORTHEAST, apiBase, authorization);

		setTimeZone("Europe/Paris");
	}

	public FranceNorthEastProvider(final String authorization)
	{
		super(NetworkId.FRANCENORTHEAST, authorization);

		setTimeZone("Europe/Paris");
	}

	@Override
	public String region()
	{
		return API_REGION;
	}

	@Override
	protected Style getLineStyle(final Product product, final String code, final String color)
	{
		// set defaults in case "color" is empty string, to avoid NumberFormatException error
		int bg = Style.RED;
		int fg = Style.WHITE;
		if(!color.equals(""))
		{
			bg = Style.parseColor(color);
			fg = computeForegroundColor(color);
		}
		switch (product)
		{
			case REGIONAL_TRAIN:
			{
				// Rail (route_type = 2) for TER and Corail Intercité/Lunéa (all SNCF)
				return new Style(bg, fg);
			}
			case SUBURBAN_TRAIN:
			{
				// Rail (route_type = 2) for Transilien (SNCF)
				return new Style(bg, fg);
			}
			case TRAM:
			{
				// Tram (route_type = 0) for Strasboug (CTS) and Nancy (Stan)
				return new Style(Shape.RECT, bg, fg);
			}
			case BUS:
			{
				// Bus  (route_type = 3)
				return new Style(Shape.ROUNDED, bg, fg);
			}
			case SUBWAY:
			{
				// Subway (route_type = 1) for Lille (Transpole)
				return new Style(Shape.CIRCLE, Style.TRANSPARENT, bg, bg);
			}
			default:
				throw new IllegalArgumentException("Unhandled product: " + product);
		}
	}
}
