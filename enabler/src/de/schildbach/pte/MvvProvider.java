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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://efa.mvv-muenchen.de/mobile/";

	public MvvProvider()
	{
		this(API_BASE);
	}

	public MvvProvider(final String apiBase)
	{
		super(NetworkId.MVV, apiBase);

		setIncludeRegionId(false);
		setStyles(STYLES);
		setSessionCookieName("SIDefaalt");
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Mittelrheinbahn (trans regio)".equals(trainName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "MiRhBa");
			if ("Süd-Thüringen-Bahn".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "STB");
			if ("agilis".equals(longName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "agilis");
			if ("SBB".equals(trainName))
				return new Line(id, network, Product.REGIONAL_TRAIN, "SBB");
			if ("A".equals(trainNum))
				return new Line(id, network, Product.SUBURBAN_TRAIN, "A");
			if ("DB AG".equals(trainName))
				return new Line(id, network, null, symbol);
		}
		else if ("1".equals(mot))
		{
			if ("S".equals(symbol) && "Pendelverkehr".equals(name))
				return new Line(id, network, Product.SUBURBAN_TRAIN, "S⇆");
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Pattern P_POSITION = Pattern.compile("(Fern|Regio|S-Bahn|U-Bahn|U\\d(?:/U\\d)*)\\s+(.*)");

	@Override
	protected Position parsePosition(final String position)
	{
		if (position == null)
			return null;

		final Matcher m = P_POSITION.matcher(position);
		if (m.matches())
		{
			final char t = m.group(1).charAt(0);
			final Position p = super.parsePosition(m.group(2));
			if (t == 'S' || t == 'U')
				return new Position(p.name + "(" + t + ")", p.section);
			else
				return p;
		}

		return super.parsePosition(position);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		STYLES.put("SS1", new Style(Style.parseColor("#00ccff"), Style.WHITE));
		STYLES.put("SS2", new Style(Style.parseColor("#66cc00"), Style.WHITE));
		STYLES.put("SS3", new Style(Style.parseColor("#880099"), Style.WHITE));
		STYLES.put("SS4", new Style(Style.parseColor("#ff0033"), Style.WHITE));
		STYLES.put("SS6", new Style(Style.parseColor("#00aa66"), Style.WHITE));
		STYLES.put("SS7", new Style(Style.parseColor("#993333"), Style.WHITE));
		STYLES.put("SS8", new Style(Style.BLACK, Style.parseColor("#ffcc00")));
		STYLES.put("SS20", new Style(Style.BLACK, Style.parseColor("#ffaaaa")));
		STYLES.put("SS27", new Style(Style.parseColor("#ffaaaa"), Style.WHITE));
		STYLES.put("SA", new Style(Style.parseColor("#231f20"), Style.WHITE));

		STYLES.put("T12", new Style(Style.parseColor("#883388"), Style.WHITE));
		STYLES.put("T15", new Style(Style.parseColor("#3366CC"), Style.WHITE));
		STYLES.put("T16", new Style(Style.parseColor("#CC8833"), Style.WHITE));
		STYLES.put("T17", new Style(Style.parseColor("#993333"), Style.WHITE));
		STYLES.put("T18", new Style(Style.parseColor("#66bb33"), Style.WHITE));
		STYLES.put("T19", new Style(Style.parseColor("#cc0000"), Style.WHITE));
		STYLES.put("T20", new Style(Style.parseColor("#00bbee"), Style.WHITE));
		STYLES.put("T21", new Style(Style.parseColor("#33aa99"), Style.WHITE));
		STYLES.put("T23", new Style(Style.parseColor("#fff000"), Style.WHITE));
		STYLES.put("T25", new Style(Style.parseColor("#ff9999"), Style.WHITE));
		STYLES.put("T27", new Style(Style.parseColor("#ff6600"), Style.WHITE));
		STYLES.put("TN17", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		STYLES.put("TN19", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		STYLES.put("TN20", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		STYLES.put("TN27", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));

		STYLES.put("UU1", new Style(Style.parseColor("#227700"), Style.WHITE));
		STYLES.put("UU2", new Style(Style.parseColor("#bb0000"), Style.WHITE));
		STYLES.put("UU2E", new Style(Style.parseColor("#bb0000"), Style.WHITE));
		STYLES.put("UU3", new Style(Style.parseColor("#ee8800"), Style.WHITE));
		STYLES.put("UU4", new Style(Style.parseColor("#00ccaa"), Style.WHITE));
		STYLES.put("UU5", new Style(Style.parseColor("#bb7700"), Style.WHITE));
		STYLES.put("UU6", new Style(Style.parseColor("#0000cc"), Style.WHITE));
		STYLES.put("UU7", new Style(Style.parseColor("#227700"), Style.parseColor("#bb0000"), Style.WHITE, 0));
	}

	@Override
	public Point[] getArea()
	{
		return new Point[] { Point.fromDouble(48.140377, 11.560643) };
	}
}
