/*
 * Copyright 2010-2014 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class ZvvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.ZVV;
	private static final String API_BASE = "http://online.fahrplan.zvv.ch/bin/";

	public ZvvProvider()
	{
		super(API_BASE + "stboard.exe/dn", null, API_BASE + "query.exe/dn", 10, UTF_8, UTF_8);

		setStyles(STYLES);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 2)
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'F';
		if (value == 32)
			return 'S';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'C';
		if (value == 256)
			return 'U';
		if (value == 512)
			return 'T';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // ICE/EN/CNL/CIS/ES/MET/NZ/PEN/TGV/THA/X2
			productBits.setCharAt(1, '1'); // EuroCity/InterCity/InterCityNight/SuperCity
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // InterRegio
			productBits.setCharAt(3, '1'); // Schnellzug/RegioExpress
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(5, '1'); // S-Bahn/StadtExpress/Eilzug/Regionalzug
		}
		else if (product == Product.SUBWAY)
		{
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(9, '1'); // Tram
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(6, '1'); // Postauto/Bus
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(4, '1'); // Schiff/Fähre/Dampfschiff
		}
		else if (product == Product.CABLECAR)
		{
			productBits.setCharAt(7, '1'); // Luftseilbahn/Standseilbahn/Bergbahn
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] OPERATORS = { "SBB", "SZU" };
	private static final String[] PLACES = { "Zürich", "Winterthur" };

	@Override
	protected String[] splitPlaceAndName(String name)
	{
		for (final String operator : OPERATORS)
		{
			if (name.endsWith(" " + operator))
			{
				name = name.substring(0, name.length() - operator.length() - 1);
				break;
			}

			if (name.endsWith(" (" + operator + ")"))
			{
				name = name.substring(0, name.length() - operator.length() - 3);
				break;
			}
		}

		for (final String place : PLACES)
		{
			if (name.startsWith(place + ", "))
				return new String[] { place, name.substring(place.length() + 2) };
			if (name.startsWith(place + " ") || name.startsWith(place + ","))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			uri.append(jsonNearbyStationsParameters(location, maxDistance, maxStations));

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append(xmlNearbyStationsParameters(location.id));

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		uri.append(xmlQueryDeparturesParameters(stationId));

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		final Matcher m = P_NORMALIZE_LINE_AND_TYPE.matcher(lineAndType);
		if (m.matches())
		{
			final String number = m.group(1).replaceAll("\\s+", " ");
			final String type = m.group(2);

			if ("Bus".equals(type))
				return newLine('B', stripPrefix(number, "Bus"), null);
			if ("Bus-NF".equals(type))
				return newLine('B', stripPrefix(number, "Bus", "Bus-NF"), null, Line.Attr.WHEEL_CHAIR_ACCESS);
			if ("Tro".equals(type) || "Trolley".equals(type))
				return newLine('B', stripPrefix(number, "Tro"), null);
			if ("Tro-NF".equals(type))
				return newLine('B', stripPrefix(number, "Tro", "Tro-NF"), null, Line.Attr.WHEEL_CHAIR_ACCESS);
			if ("Trm".equals(type))
				return newLine('T', stripPrefix(number, "Trm"), null);
			if ("Trm-NF".equals(type))
				return newLine('T', stripPrefix(number, "Trm", "Trm-NF"), null, Line.Attr.WHEEL_CHAIR_ACCESS);

			if (type.length() > 0)
			{
				final char normalizedType = normalizeType(type);
				if (normalizedType != 0)
					return newLine(normalizedType, number, null);
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line#type " + lineAndType);
		}

		throw new IllegalStateException("cannot normalize line#type " + lineAndType);
	}

	private String stripPrefix(final String str, final String... prefixes)
	{
		for (final String prefix : prefixes)
		{
			if (str.equals(prefix))
				return "";
			if (str.startsWith(prefix + ' '))
				return str.substring(prefix.length() + 1);
		}

		return str;
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// E-Bus: Bus, Tram oder Zug?

		if ("S-BAHN".equals(ucType))
			return 'S';

		if ("T".equals(ucType))
			return 'T';
		if ("TRM".equals(ucType))
			return 'T';
		if ("TRM-NF".equals(ucType)) // Niederflur
			return 'T';

		if ("BUS-NF".equals(ucType)) // Niederflur
			return 'B';
		if ("TRO-NF".equals(ucType)) // Niederflur
			return 'B';
		if ("N".equals(ucType)) // Nachtbus
			return 'B';
		if ("BUXI".equals(ucType))
			return 'B';
		if ("TX".equals(ucType))
			return 'B';
		if ("E-BUS".equals(ucType))
			return 'B';
		if ("TROLLEY".equals(ucType))
			return 'B';
		if ("KB".equals(ucType)) // Kleinbus?
			return 'B';
		if ("EE".equals(ucType))
			return 'B';

		if ("D-SCHIFF".equals(ucType))
			return 'F';

		if ("BERGBAHN".equals(ucType))
			return 'C';
		if ("LSB".equals(ucType)) // Luftseilbahn
			return 'C';
		if ("SLB".equals(ucType)) // Sesselliftbahn
			return 'C';

		if ("UNB".equals(ucType))
			return '?';
		if ("UUU".equals(ucType))
			return '?';
		if ("???".equals(ucType))
			return '?';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// S-Bahn
		STYLES.put("SS2", new Style(Shape.RECT, Style.parseColor("#70c82c"), Style.WHITE));
		STYLES.put("SS3", new Style(Shape.RECT, Style.parseColor("#587AC2"), Style.WHITE));
		STYLES.put("SS4", new Style(Shape.RECT, Style.parseColor("#EE7267"), Style.WHITE));
		STYLES.put("SS5", new Style(Shape.RECT, Style.parseColor("#6aadc3"), Style.WHITE));
		STYLES.put("SS6", new Style(Shape.RECT, Style.parseColor("#6f41a4"), Style.WHITE));
		STYLES.put("SS7", new Style(Shape.RECT, Style.parseColor("#fbb809"), Style.BLACK));
		STYLES.put("SS8", new Style(Shape.RECT, Style.parseColor("#562691"), Style.WHITE));
		STYLES.put("SS9", new Style(Shape.RECT, Style.parseColor("#069A5D"), Style.WHITE));
		STYLES.put("SS10", new Style(Shape.RECT, Style.parseColor("#fbc434"), Style.BLACK));
		STYLES.put("SS11", new Style(Shape.RECT, Style.parseColor("#ae90cf"), Style.WHITE));
		STYLES.put("SS12", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("SS13", new Style(Shape.RECT, Style.parseColor("#905723"), Style.WHITE));
		STYLES.put("SS14", new Style(Shape.RECT, Style.parseColor("#753c0c"), Style.WHITE));
		STYLES.put("SS15", new Style(Shape.RECT, Style.parseColor("#c79f73"), Style.WHITE));
		STYLES.put("SS16", new Style(Shape.RECT, Style.parseColor("#68c971"), Style.WHITE));
		STYLES.put("SS17", new Style(Shape.RECT, Style.parseColor("#3b99b5"), Style.WHITE));
		STYLES.put("SS18", new Style(Shape.RECT, Style.parseColor("#f14337"), Style.WHITE));
		STYLES.put("SS21", new Style(Shape.RECT, Style.parseColor("#9acaee"), Style.WHITE));
		STYLES.put("SS22", new Style(Shape.RECT, Style.parseColor("#8dd24e"), Style.WHITE));
		STYLES.put("SS24", new Style(Shape.RECT, Style.parseColor("#ab7745"), Style.WHITE));
		STYLES.put("SS26", new Style(Shape.RECT, Style.parseColor("#0e87aa"), Style.WHITE));
		STYLES.put("SS29", new Style(Shape.RECT, Style.parseColor("#3dba56"), Style.WHITE));
		STYLES.put("SS30", new Style(Shape.RECT, Style.parseColor("#0b8ed8"), Style.WHITE));
		STYLES.put("SS33", new Style(Shape.RECT, Style.parseColor("#51aae3"), Style.WHITE));
		STYLES.put("SS35", new Style(Shape.RECT, Style.parseColor("#81c0eb"), Style.WHITE));
		STYLES.put("SS40", new Style(Shape.RECT, Style.parseColor("#ae90cf"), Style.WHITE));
		STYLES.put("SS41", new Style(Shape.RECT, Style.parseColor("#f89a83"), Style.WHITE));
		STYLES.put("SS55", new Style(Shape.RECT, Style.parseColor("#905723"), Style.WHITE));

		// Tram
		STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#19ae48"), Style.WHITE));
		STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#453fa0"), Style.WHITE));
		STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#8c5a2c"), Style.WHITE));
		STYLES.put("T6", new Style(Shape.RECT, Style.parseColor("#d6973c"), Style.WHITE));
		STYLES.put("T7", new Style(Shape.RECT, Style.parseColor("#231f20"), Style.WHITE));
		STYLES.put("T8", new Style(Shape.RECT, Style.parseColor("#99d420"), Style.BLACK));
		STYLES.put("T9", new Style(Shape.RECT, Style.parseColor("#453fa0"), Style.WHITE));
		STYLES.put("T10", new Style(Shape.RECT, Style.parseColor("#ee1998"), Style.WHITE));
		STYLES.put("T11", new Style(Shape.RECT, Style.parseColor("#19ae48"), Style.WHITE));
		STYLES.put("T12", new Style(Shape.RECT, Style.parseColor("#85d7e3"), Style.BLACK));
		STYLES.put("T13", new Style(Shape.RECT, Style.parseColor("#fdd205"), Style.BLACK));
		STYLES.put("T14", new Style(Shape.RECT, Style.parseColor("#2cbbf2"), Style.WHITE));
		STYLES.put("T15", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T17", new Style(Shape.RECT, Style.parseColor("#9e1a6e"), Style.WHITE));

		// Bus/Trolley
		STYLES.put("B31", new Style(Shape.RECT, Style.parseColor("#999bd3"), Style.WHITE));
		STYLES.put("B32", new Style(Shape.RECT, Style.parseColor("#d8a1d6"), Style.BLACK));
		STYLES.put("B33", new Style(Shape.RECT, Style.parseColor("#e4e793"), Style.BLACK));
	}
}
