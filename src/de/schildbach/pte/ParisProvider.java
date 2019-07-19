/*
 * Copyright 2014-2015 the original author or authors.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;
import de.schildbach.pte.util.WordUtils;

import okhttp3.HttpUrl;

/**
 * @author Antonio El Khoury
 */
public class ParisProvider extends AbstractNavitiaProvider {
    private static final String API_REGION = "fr-idf";

    public ParisProvider(final HttpUrl apiBase, final String authorization) {
        super(NetworkId.PARIS, apiBase, authorization);

        setTimeZone("Europe/Paris");
    }

    public ParisProvider(final String authorization) {
        super(NetworkId.PARIS, authorization);

        setTimeZone("Europe/Paris");
    }

    @Override
    public String region() {
        return API_REGION;
    }

    @Override
    protected Style getLineStyle(final String network, final Product product, final String code, final String color) {
        switch (product) {
        case SUBURBAN_TRAIN: {
            // RER
            if (code.compareTo("F") < 0) {
                return new Style(Shape.CIRCLE, Style.TRANSPARENT, Style.parseColor(color), Style.parseColor(color));
            }
            // Transilien
            else {
                return new Style(Shape.ROUNDED, Style.TRANSPARENT, Style.parseColor(color), Style.parseColor(color));
            }
        }
        case REGIONAL_TRAIN: {
            // TER + Intercités
            return new Style(Style.parseColor(color), computeForegroundColor(color));
        }
        case SUBWAY: {
            // Metro
            return new Style(Shape.CIRCLE, Style.parseColor(color), computeForegroundColor(color));
        }
        case TRAM: {
            // Tram
            return new Style(Shape.RECT, Style.parseColor(color), computeForegroundColor(color));
        }
        case BUS: {
            // Bus + Noctilien
            return new Style(Shape.RECT, Style.parseColor(color), computeForegroundColor(color));
        }
        case CABLECAR: {
            // Orlyval
            return new Style(Shape.ROUNDED, Style.parseColor(color), computeForegroundColor(color));
        }
        default:
            return super.getLineStyle(network, product, code, color);
        }
    }

    @Override
    protected String getLocationName(String name) {
        return WordUtils.capitalizeFully(name);
    }

    @Override
    protected String getAddressName(final String name, final String houseNumber) {
        return houseNumber + " " + name;
    }

}
