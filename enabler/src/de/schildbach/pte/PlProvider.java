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

import java.util.Set;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Product;
import de.schildbach.pte.util.StringReplaceReader;

/**
 * @author Andreas Schildbach
 */
public class PlProvider extends AbstractHafasProvider
{
	private static final String API_BASE = "http://rozklad.bilkom.pl/bin/";

	public PlProvider()
	{
		super(NetworkId.PL, API_BASE + "stboard.exe/pn", API_BASE + "ajax-getstop.exe/pn", API_BASE + "query.exe/pn", 7, Charsets.UTF_8);
	}

	@Override
	protected Product intToProduct(final int value)
	{
		if (value == 1)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 2)
			return Product.HIGH_SPEED_TRAIN;
		if (value == 4)
			return Product.REGIONAL_TRAIN;
		if (value == 8)
			return Product.SUBURBAN_TRAIN;
		if (value == 16) // Bus
			return Product.BUS;
		if (value == 32) // AST, SEV
			return Product.BUS;
		if (value == 64)
			return Product.FERRY;

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Kolej dużych prędkości
			productBits.setCharAt(1, '1'); // EC/IC/EIC/Ex
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // TLK/IR/RE/D/Posp.
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(3, '1'); // Regio/Osobowe
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(6, '1'); // Metro
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(5, '1'); // Tramwaj
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(4, '1'); // Autobus
		}
		else if (product == Product.FERRY || product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Warszawa", "Kraków" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.endsWith(", " + place))
				return new String[] { place, name.substring(0, name.length() - place.length() - 2) };
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitStationName(name);
	}

	@Override
	protected void addCustomReplaces(final StringReplaceReader reader)
	{
		reader.replace("dir=\"Sp ", " "); // Poland
		reader.replace("dir=\"B ", " "); // Poland
		reader.replace("dir=\"K ", " "); // Poland
		reader.replace("dir=\"Eutingen i. G ", "dir=\"Eutingen\" "); // Poland
		reader.replace("StargetLoc", "Süd\" targetLoc"); // Poland
		reader.replace("platform=\"K ", " "); // Poland
	}

	@Override
	public Set<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected Product normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AR".equals(ucType)) // Arriva Polaczen
			return Product.REGIONAL_TRAIN;
		if ("N".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("KW".equals(ucType)) // Koleje Wielkopolskie
			return Product.REGIONAL_TRAIN;
		if ("KS".equals(ucType)) // Koleje Śląskie
			return Product.REGIONAL_TRAIN;
		if ("DB".equals(ucType))
			return Product.REGIONAL_TRAIN;
		if ("REG".equals(ucType))
			return Product.REGIONAL_TRAIN;

		if ("LKA".equals(ucType)) // Łódzka Kolej Aglomeracyjna
			return Product.SUBURBAN_TRAIN;

		if ("IRB".equals(ucType)) // interREGIO Bus
			return Product.BUS;
		if ("ZKA".equals(ucType)) // Zastępcza Komunikacja Autobusowa (Schienenersatzverkehr)
			return Product.BUS;

		if ("FRE".equals(ucType))
			return Product.FERRY;

		return super.normalizeType(type);
	}
}
