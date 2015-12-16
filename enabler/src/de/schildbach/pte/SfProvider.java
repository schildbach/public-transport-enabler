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

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class SfProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "http://tripplanner.transit.511.org/mtc/";

	public SfProvider()
	{
		super(NetworkId.SF, API_BASE);

		setLanguage("en");
		setTimeZone("America/Los_Angeles");
		setUseRouteIndexAsTripId(false);
		setFareCorrectionFactor(0.01f);
		setStyles(STYLES);
	}

	@Override
	protected String normalizeLocationName(final String name)
	{
		if (name == null || name.length() == 0)
			return null;

		return super.normalizeLocationName(name).replace("$XINT$", "&");
	}

	@Override
	protected Position parsePosition(final String position)
	{
		if (position == null)
			return null;

		final int i = position.lastIndexOf("##");
		if (i < 0)
			return position.length() < 16 ? super.parsePosition(position) : null;

		final String name = position.substring(i + 2).trim();
		if (name.isEmpty())
			return null;

		return new Position(name);
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if (("XAA".equals(symbol) || "Daly City / Fremont".equals(symbol)) && ("DALY/FREMONT".equals(name) || "Daly City / Fremont".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "DALY/FRMT");
			if (("FRE".equals(symbol) || "Fremont / Daly City".equals(symbol)) && ("FREMONT/DALY".equals(name) || "Fremont / Daly City".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "FRMT/DALY");
			if (("XAC".equals(symbol) || "Fremont / Richmond".equals(symbol)) && "Fremont / Richmond".equals(name))
				return new Line(id, network, Product.REGIONAL_TRAIN, "FRMT/RICH");
			if (("XAD".equals(symbol) || "Richmond / Fremont".equals(symbol)) && "Richmond / Fremont".equals(name))
				return new Line(id, network, Product.REGIONAL_TRAIN, "RICH/FRMT");
			if (("XAE".equals(symbol) || "Pittsburg Bay Point / SFO".equals(symbol))
					&& ("BAY PT/SFIA".equals(name) || "Pittsburg Bay Point / SFO".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "PITT/SFIA");
			if (("SFI".equals(symbol) || "SFO / Pittsburg Bay Point".equals(symbol))
					&& ("SFIA/BAY PT".equals(name) || "SFO / Pittsburg Bay Point".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "SFIA/PITT");
			if (("XAF".equals(symbol) || "Millbrae / Richmond".equals(symbol)) && ("MILL/RICH".equals(name) || "Millbrae / Richmond".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "MLBR/RICH");
			if (("XAG".equals(symbol) || "Richmond / Millbrae".equals(symbol)) && ("RICH/MILL".equals(name) || "Richmond / Millbrae".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "RICH/MLBR");
			if (("XAH".equals(symbol) || "Daly City / Dublin Pleasanton".equals(symbol))
					&& ("DALY/DUBLIN".equals(name) || "Daly City / Dublin Pleasanton".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "DALY/DUBL");
			if (("XAI".equals(symbol) || "Dublin Pleasanton / Daly City".equals(symbol))
					&& ("DUBLIN/DALY".equals(name) || "Dublin Pleasanton / Daly City".equals(name)))
				return new Line(id, network, Product.REGIONAL_TRAIN, "DUBL/DALY");

			if ("LOC".equals(symbol) && "LOCAL".equals(name))
				return new Line(id, network, Product.REGIONAL_TRAIN, "Local");
			if ("CAP".equals(symbol) && "CAPITOL".equals(name))
				return new Line(id, network, Product.REGIONAL_TRAIN, "Capitol");
			if ("OAK".equals(symbol) && "OAK / Coliseum".equals(name))
				return new Line(id, network, Product.REGIONAL_TRAIN, "OAK/Coliseum");

			if ("Muni Rail".equals(trainName) && symbol != null) // Muni
				return new Line(id, network, Product.TRAM, symbol);
			if (trainType == null && "E".equals(trainNum)) // Muni Rail E
				return new Line(id, network, Product.TRAM, "E");
			if (trainType == null && "F".equals(trainNum)) // Muni Historic Streetcar
				return new Line(id, network, Product.TRAM, "F");
			if (trainType == null && "J".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "J");
			if (trainType == null && "K".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "K");
			if (trainType == null && "KT".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "KT");
			if (trainType == null && "L".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "L");
			if (trainType == null && "M".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "M");
			if (trainType == null && "N".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "N");
			if (trainType == null && "T".equals(trainNum)) // Muni Metro
				return new Line(id, network, Product.TRAM, "T");
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// BART
		STYLES.put("RDALY/FRMT", new Style(Style.parseColor("#4EBF49"), Style.WHITE));
		STYLES.put("RFRMT/DALY", new Style(Style.parseColor("#4EBF49"), Style.WHITE));

		STYLES.put("RFRMT/RICH", new Style(Style.parseColor("#FAA61A"), Style.WHITE));
		STYLES.put("RRICH/FRMT", new Style(Style.parseColor("#FAA61A"), Style.WHITE));

		STYLES.put("RSFIA/PITT", new Style(Style.parseColor("#FFE800"), Style.BLACK));
		STYLES.put("RPITT/SFIA", new Style(Style.parseColor("#FFE800"), Style.BLACK));

		STYLES.put("RMLBR/RICH", new Style(Style.parseColor("#F81A23"), Style.WHITE));
		STYLES.put("RRICH/MLBR", new Style(Style.parseColor("#F81A23"), Style.WHITE));

		STYLES.put("RDALY/DUBL", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
		STYLES.put("RDUBL/DALY", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
	}
}
