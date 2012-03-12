/*
 * Copyright 2010-2012 the original author or authors.
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
public class KvvProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.KVV;
	private final static String API_BASE = "http://213.144.24.66/kvv/"; // http://213.144.24.66/kvv2/

	public KvvProvider()
	{
		this(API_BASE);
	}

	public KvvProvider(final String apiBase)
	{
		super(apiBase, null);

		setSuppressPositions(true);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected String parseLine(final String mot, final String name, final String longName, final String noTrainName)
	{
		if (name.endsWith(" (VBK)")) // Verkehrsbetriebe Karlsruhe
			return super.parseLine(mot, name.substring(0, name.length() - 6), longName, noTrainName);
		else
			return super.parseLine(mot, name, longName, noTrainName);
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		// S-Bahn
		LINES.put("SS1", new Style(Style.parseColor("#00a76d"), Style.WHITE));
		LINES.put("SS11", new Style(Style.parseColor("#00a76d"), Style.WHITE));
		LINES.put("SS2", new Style(Style.parseColor("#a066aa"), Style.WHITE));
		LINES.put("SS3", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		LINES.put("SS31", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		LINES.put("SS32", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		LINES.put("SS33", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		LINES.put("SS4", new Style(Style.parseColor("#9f184c"), Style.WHITE));
		LINES.put("SS41", new Style(Style.parseColor("#9f184c"), Style.WHITE));
		LINES.put("SS5", new Style(Style.parseColor("#f69795"), Style.BLACK));
		LINES.put("SS51", new Style(Style.parseColor("#f69795"), Style.BLACK));
		LINES.put("SS52", new Style(Style.parseColor("#f69795"), Style.BLACK));
		LINES.put("SS6", new Style(Style.parseColor("#282268"), Style.WHITE));
		LINES.put("SS7", new Style(Style.parseColor("#fff200"), Style.BLACK));
		LINES.put("SS9", new Style(Style.parseColor("#fab49b"), Style.BLACK));

		// Tram
		LINES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		LINES.put("T2", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
		LINES.put("T2E", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
		LINES.put("T3", new Style(Shape.RECT, Style.parseColor("#947139"), Style.WHITE));
		LINES.put("T4", new Style(Shape.RECT, Style.parseColor("#ffcb04"), Style.BLACK));
		LINES.put("T5", new Style(Shape.RECT, Style.parseColor("#00c0f3"), Style.WHITE));
		LINES.put("T6", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE));
		LINES.put("T7", new Style(Shape.RECT, Style.parseColor("#58595b"), Style.WHITE));
		LINES.put("T8", new Style(Shape.RECT, Style.parseColor("#f7931d"), Style.BLACK));

		// Bus - only used on bus plan
		// LINES.put("B21", new Style(Shape.CIRCLE, Style.parseColor("#2e3092"), Style.WHITE));
		// LINES.put("B22", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
		// LINES.put("B23", new Style(Shape.CIRCLE, Style.parseColor("#56c5d0"), Style.WHITE));
		// LINES.put("B24", new Style(Shape.CIRCLE, Style.parseColor("#a1d1e6"), Style.WHITE));
		// LINES.put("B26", new Style(Shape.CIRCLE, Style.parseColor("#2e3092"), Style.WHITE));
		// LINES.put("B27", new Style(Shape.CIRCLE, Style.parseColor("#00aeef"), Style.WHITE));
		// LINES.put("B30", new Style(Shape.CIRCLE, Style.parseColor("#adbc72"), Style.WHITE));
		// LINES.put("B31", new Style(Shape.CIRCLE, Style.parseColor("#62bb46"), Style.WHITE));
		// LINES.put("B32", new Style(Shape.CIRCLE, Style.parseColor("#177752"), Style.WHITE));
		// LINES.put("B42", new Style(Shape.CIRCLE, Style.parseColor("#177752"), Style.WHITE));
		// LINES.put("B44", new Style(Shape.CIRCLE, Style.parseColor("#62bb46"), Style.WHITE));
		// LINES.put("B47", new Style(Shape.CIRCLE, Style.parseColor("#adbc72"), Style.WHITE));
		// LINES.put("B50", new Style(Shape.CIRCLE, Style.parseColor("#a25641"), Style.WHITE));
		// LINES.put("B51", new Style(Shape.CIRCLE, Style.parseColor("#d2ab67"), Style.WHITE));
		// LINES.put("B52", new Style(Shape.CIRCLE, Style.parseColor("#a25641"), Style.WHITE));
		// LINES.put("B55", new Style(Shape.CIRCLE, Style.parseColor("#806a50"), Style.WHITE));
		// LINES.put("B60", new Style(Shape.CIRCLE, Style.parseColor("#806a50"), Style.WHITE));
		// LINES.put("B62", new Style(Shape.CIRCLE, Style.parseColor("#d2ab67"), Style.WHITE));
		// LINES.put("B70", new Style(Shape.CIRCLE, Style.parseColor("#574187"), Style.WHITE));
		// LINES.put("B71", new Style(Shape.CIRCLE, Style.parseColor("#874487"), Style.WHITE));
		// LINES.put("B72", new Style(Shape.CIRCLE, Style.parseColor("#9b95c9"), Style.WHITE));
		// LINES.put("B73", new Style(Shape.CIRCLE, Style.parseColor("#574187"), Style.WHITE));
		// LINES.put("B74", new Style(Shape.CIRCLE, Style.parseColor("#9b95c9"), Style.WHITE));
		// LINES.put("B75", new Style(Shape.CIRCLE, Style.parseColor("#874487"), Style.WHITE));
		// LINES.put("B107", new Style(Shape.CIRCLE, Style.parseColor("#9d9fa1"), Style.WHITE));
		// LINES.put("B118", new Style(Shape.CIRCLE, Style.parseColor("#9d9fa1"), Style.WHITE));
		// LINES.put("B123", new Style(Shape.CIRCLE, Style.parseColor("#9d9fa1"), Style.WHITE));

		// Nightliner
		LINES.put("BNL3", new Style(Style.parseColor("#947139"), Style.WHITE));
		LINES.put("BNL4", new Style(Style.parseColor("#ffcb04"), Style.BLACK));
		LINES.put("BNL5", new Style(Style.parseColor("#00c0f3"), Style.WHITE));
		LINES.put("BNL6", new Style(Style.parseColor("#80c342"), Style.WHITE));

		// Anruf-Linien-Taxi
		LINES.put("BALT6", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		LINES.put("BALT11", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		LINES.put("BALT12", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		LINES.put("BALT13", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		LINES.put("BALT14", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		LINES.put("BALT16", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		else
			return super.lineStyle(line);
	}
}
