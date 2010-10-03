/*
 * Copyright 2010 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider implements NetworkProvider
{
	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE_COORDS = Pattern
			.compile("&REQMapRoute0\\.Location0\\.X=(-?\\d+)&REQMapRoute0\\.Location0\\.Y=(-?\\d+)&");
	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("[\\?&]input=(\\d+)&[^\"]*\">([^<]*)<");

	protected abstract String nearbyStationUri(String stationId);

	public List<Station> nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
			throws IOException
	{
		if (stationId == null)
			throw new IllegalArgumentException("stationId must be given");

		final List<Station> stations = new ArrayList<Station>();

		final String uri = nearbyStationUri(stationId);
		final CharSequence page = ParserUtils.scrape(uri);
		String oldZebra = null;

		final Matcher mCoarse = P_NEARBY_COARSE.matcher(page);

		while (mCoarse.find())
		{
			final String zebra = mCoarse.group(1);
			if (oldZebra != null && zebra.equals(oldZebra))
				throw new IllegalArgumentException("missed row? last:" + zebra);
			else
				oldZebra = zebra;

			final Matcher mFineLocation = P_NEARBY_FINE_LOCATION.matcher(mCoarse.group(2));

			if (mFineLocation.find())
			{
				int parsedLon = 0;
				int parsedLat = 0;
				final int parsedId = Integer.parseInt(mFineLocation.group(1));
				final String parsedName = ParserUtils.resolveEntities(mFineLocation.group(2));

				final Matcher mFineCoords = P_NEARBY_FINE_COORDS.matcher(mCoarse.group(2));

				if (mFineCoords.find())
				{
					parsedLon = Integer.parseInt(mFineCoords.group(1));
					parsedLat = Integer.parseInt(mFineCoords.group(2));
				}

				stations.add(new Station(parsedId, parsedName, parsedLat, parsedLon, 0, null, null));
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mCoarse.group(2) + "' on " + uri);
			}
		}

		if (maxStations == 0 || maxStations >= stations.size())
			return stations;
		else
			return stations.subList(0, maxStations);
	}

	protected static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû/-]+)[\\s-]*(.*)");

	protected final String normalizeLine(final String type, final String line)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

		final char normalizedType = normalizeType(type);
		if (normalizedType != 0)
			return normalizedType + strippedLine;

		throw new IllegalStateException("cannot normalize type " + type + " line " + line);
	}

	protected abstract char normalizeType(String type);

	protected final char normalizeCommonTypes(final String ucType)
	{
		// Intercity
		if (ucType.equals("EC")) // EuroCity
			return 'I';
		if (ucType.equals("EN")) // EuroNight
			return 'I';
		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("EN")) // EuroNight
			return 'I';
		if (ucType.equals("CNL")) // CityNightLine
			return 'I';
		if (ucType.equals("OEC")) // ÖBB-EuroCity
			return 'I';
		if (ucType.equals("OIC")) // ÖBB-InterCity
			return 'I';
		if (ucType.equals("RJ")) // RailJet, Österreichische Bundesbahnen
			return 'I';
		if (ucType.equals("THA")) // Thalys
			return 'I';
		if (ucType.equals("TGV")) // Train à Grande Vitesse
			return 'I';
		if (ucType.equals("DNZ")) // Berlin-Saratov, Berlin-Moskva, Connections only?
			return 'I';
		if (ucType.equals("AIR")) // Generic Flight
			return 'I';
		if (ucType.equals("ECB")) // EC, Verona-München
			return 'I';
		if (ucType.equals("INZ")) // Nacht
			return 'I';
		if (ucType.equals("RHI")) // ICE
			return 'I';
		if (ucType.equals("RHT")) // TGV
			return 'I';
		if (ucType.equals("TGD")) // TGV
			return 'I';
		if (ucType.equals("IRX")) // IC
			return 'I';

		// Regional Germany
		if (ucType.equals("ZUG")) // Generic Train
			return 'R';
		if (ucType.equals("R")) // Generic Regional Train
			return 'R';
		if (ucType.equals("DPN")) // Dritter Personen Nahverkehr
			return 'R';
		if (ucType.equals("RB")) // RegionalBahn
			return 'R';
		if (ucType.equals("RE")) // RegionalExpress
			return 'R';
		if (ucType.equals("IR")) // Interregio
			return 'R';
		if (ucType.equals("IRE")) // Interregio Express
			return 'R';
		if (ucType.equals("HEX")) // Harz-Berlin-Express, Veolia
			return 'R';
		if (ucType.equals("WFB")) // Westfalenbahn
			return 'R';
		if (ucType.equals("RT")) // RegioTram
			return 'R';
		if (ucType.equals("REX")) // RegionalExpress, Österreich
			return 'R';

		// Regional Poland
		if (ucType.equals("OS")) // Chop-Cierna nas Tisou
			return 'R';
		if (ucType.equals("SP")) // Polen
			return 'R';

		// Suburban Trains
		if (ucType.equals("S")) // Generic S-Bahn
			return 'S';

		// Subway
		if (ucType.equals("U")) // Generic U-Bahn
			return 'U';

		// Tram
		if (ucType.equals("STR")) // Generic Tram
			return 'T';

		// Bus
		if (ucType.equals("BUS")) // Generic Bus
			return 'B';
		if (ucType.equals("AST")) // Anruf-Sammel-Taxi
			return 'B';
		if (ucType.equals("SEV")) // Schienen-Ersatz-Verkehr
			return 'B';
		if (ucType.equals("FB")) // Luxemburg-Saarbrücken
			return 'B';

		// Ferry
		if (ucType.equals("AS")) // SyltShuttle, eigentlich Autoreisezug
			return 'F';

		return 0;
	}

	private static final Map<Character, int[]> LINES = new HashMap<Character, int[]>();

	static
	{
		LINES.put('I', new int[] { Color.WHITE, Color.RED, Color.RED });
		LINES.put('R', new int[] { Color.GRAY, Color.WHITE });
		LINES.put('S', new int[] { Color.parseColor("#006e34"), Color.WHITE });
		LINES.put('U', new int[] { Color.parseColor("#003090"), Color.WHITE });
		LINES.put('T', new int[] { Color.parseColor("#cc0000"), Color.WHITE });
		LINES.put('B', new int[] { Color.parseColor("#993399"), Color.WHITE });
		LINES.put('F', new int[] { Color.BLUE, Color.WHITE });
		LINES.put('?', new int[] { Color.DKGRAY, Color.WHITE });
	}

	public final int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
