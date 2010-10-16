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
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Station;
import de.schildbach.pte.util.Color;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractHafasProvider implements NetworkProvider
{
	private static final String DEFAULT_ENCODING = "ISO-8859-1";

	private final String autocompleteUri;
	private final String prod;
	private final String accessId;

	public AbstractHafasProvider(final String autocompleteUri, final String prod, final String accessId)
	{
		this.autocompleteUri = autocompleteUri;
		this.prod = prod;
		this.accessId = accessId;
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		final String request = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" //
				+ "<ReqC ver=\"1.1\" prod=\"" + prod + "\" lang=\"DE\"" + (accessId != null ? " accessId=\"" + accessId + "\"" : "") + ">" //
				+ "<LocValReq id=\"station\" maxNr=\"10\"><ReqLoc match=\"" + constraint + "\" type=\"ST\"/></LocValReq>" //
				+ "<LocValReq id=\"poi\" maxNr=\"10\"><ReqLoc match=\"" + constraint + "\" type=\"POI\"/></LocValReq>" //
				+ "</ReqC>";

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(autocompleteUri, request);

			final List<Location> results = new ArrayList<Location>();

			final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			final XmlPullParser pp = factory.newPullParser();
			pp.setInput(is, DEFAULT_ENCODING);

			XmlPullUtil.jump(pp, "ResC");
			XmlPullUtil.enter(pp);

			XmlPullUtil.require(pp, "LocValRes");
			XmlPullUtil.requireAttr(pp, "id", "station");
			XmlPullUtil.enter(pp);

			while (XmlPullUtil.test(pp, "Station"))
			{
				final String name = pp.getAttributeValue(null, "name");
				final int id = Integer.parseInt(pp.getAttributeValue(null, "externalStationNr"));
				final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
				final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));
				results.add(new Location(LocationType.STATION, id, y, x, name));

				XmlPullUtil.skipTree(pp);
			}

			XmlPullUtil.exit(pp);

			XmlPullUtil.require(pp, "LocValRes");
			XmlPullUtil.requireAttr(pp, "id", "poi");
			XmlPullUtil.enter(pp);

			while (XmlPullUtil.test(pp, "Poi"))
			{
				final String name = pp.getAttributeValue(null, "name");
				final int x = Integer.parseInt(pp.getAttributeValue(null, "x"));
				final int y = Integer.parseInt(pp.getAttributeValue(null, "y"));
				results.add(new Location(LocationType.POI, 0, y, x, name));

				XmlPullUtil.skipTree(pp);
			}

			XmlPullUtil.exit(pp);

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}
		catch (final SocketTimeoutException x)
		{
			throw new RuntimeException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr class=\"(zebra[^\"]*)\">(.*?)</tr>", Pattern.DOTALL);
	private final static Pattern P_NEARBY_FINE_COORDS = Pattern
			.compile("&REQMapRoute0\\.Location0\\.X=(-?\\d+)&REQMapRoute0\\.Location0\\.Y=(-?\\d+)&");
	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("[\\?&]input=(\\d+)&[^\"]*\">([^<]*)<");

	protected abstract String nearbyStationUri(String stationId);

	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
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
			return new NearbyStationsResult(uri, stations);
		else
			return new NearbyStationsResult(uri, stations.subList(0, maxStations));
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
		if (ucType.equals("BUSSEV")) // Schienen-Ersatz-Verkehr
			return 'B';
		if (ucType.equals("FB")) // Luxemburg-Saarbrücken
			return 'B';

		// Ferry
		if (ucType.equals("AS")) // SyltShuttle, eigentlich Autoreisezug
			return 'F';

		return 0;
	}

	private static final Pattern P_CONNECTION_ID = Pattern.compile("co=(C\\d+-\\d+)&");

	protected static String extractConnectionId(final String link)
	{
		final Matcher m = P_CONNECTION_ID.matcher(link);
		if (m.find())
			return m.group(1);
		else
			throw new IllegalArgumentException("cannot extract id from " + link);
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
