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
import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class KvvProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "http://213.144.24.66/kvv2/";

	public KvvProvider()
	{
		this(API_BASE);
	}

	public KvvProvider(final String apiBase)
	{
		super(NetworkId.KVV, apiBase);

		setStyles(STYLES);
		setSessionCookieName("HASESSIONID");
	}

	private static final Pattern P_LINE = Pattern.compile("(.*?)\\s+\\([\\w/]+\\)", Pattern.CASE_INSENSITIVE);

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String mot, final @Nullable String symbol, final @Nullable String name,
			final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName)
	{
		if (name != null)
		{
			final Matcher m = P_LINE.matcher(name);
			if (m.matches())
				return super.parseLine(id, mot, symbol, m.group(1), longName, trainType, trainNum, trainName);
		}

		return super.parseLine(id, mot, symbol, name, longName, trainType, trainNum, trainName);
		// TODO check for " (Ersatzverkehr)"
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// S-Bahn
		STYLES.put("SS1", new Style(Style.parseColor("#00a76d"), Style.WHITE));
		STYLES.put("SS11", new Style(Style.parseColor("#00a76d"), Style.WHITE));
		STYLES.put("SS2", new Style(Style.parseColor("#a066aa"), Style.WHITE));
		STYLES.put("SS3", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		STYLES.put("SS31", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		STYLES.put("SS32", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		STYLES.put("SS33", new Style(Style.parseColor("#00a99d"), Style.WHITE));
		STYLES.put("SS4", new Style(Style.parseColor("#9f184c"), Style.WHITE));
		STYLES.put("SS41", new Style(Style.parseColor("#9f184c"), Style.WHITE));
		STYLES.put("SS5", new Style(Style.parseColor("#f69795"), Style.BLACK));
		STYLES.put("SS51", new Style(Style.parseColor("#f69795"), Style.BLACK));
		STYLES.put("SS52", new Style(Style.parseColor("#f69795"), Style.BLACK));
		STYLES.put("SS6", new Style(Style.parseColor("#282268"), Style.WHITE));
		STYLES.put("SS7", new Style(Style.parseColor("#fff200"), Style.BLACK));
		STYLES.put("SS9", new Style(Style.parseColor("#fab49b"), Style.BLACK));

		// Tram
		STYLES.put("T1", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T1E", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("T2", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
		STYLES.put("T2E", new Style(Shape.RECT, Style.parseColor("#0071bc"), Style.WHITE));
		STYLES.put("T3", new Style(Shape.RECT, Style.parseColor("#947139"), Style.WHITE));
		STYLES.put("T3E", new Style(Shape.RECT, Style.parseColor("#947139"), Style.WHITE));
		STYLES.put("T4", new Style(Shape.RECT, Style.parseColor("#ffcb04"), Style.BLACK));
		STYLES.put("T4E", new Style(Shape.RECT, Style.parseColor("#ffcb04"), Style.BLACK));
		STYLES.put("T5", new Style(Shape.RECT, Style.parseColor("#00c0f3"), Style.WHITE));
		STYLES.put("T5E", new Style(Shape.RECT, Style.parseColor("#00c0f3"), Style.WHITE));
		STYLES.put("T6", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE));
		STYLES.put("T6E", new Style(Shape.RECT, Style.parseColor("#80c342"), Style.WHITE));
		STYLES.put("T7", new Style(Shape.RECT, Style.parseColor("#58595b"), Style.WHITE));
		STYLES.put("T7E", new Style(Shape.RECT, Style.parseColor("#58595b"), Style.WHITE));
		STYLES.put("T8", new Style(Shape.RECT, Style.parseColor("#f7931d"), Style.BLACK));
		STYLES.put("T8E", new Style(Shape.RECT, Style.parseColor("#f7931d"), Style.BLACK));

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
		STYLES.put("BNL3", new Style(Style.parseColor("#947139"), Style.WHITE));
		STYLES.put("BNL4", new Style(Style.parseColor("#ffcb04"), Style.BLACK));
		STYLES.put("BNL5", new Style(Style.parseColor("#00c0f3"), Style.WHITE));
		STYLES.put("BNL6", new Style(Style.parseColor("#80c342"), Style.WHITE));

		// Anruf-Linien-Taxi
		STYLES.put("BALT6", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		STYLES.put("BALT11", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		STYLES.put("BALT12", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		STYLES.put("BALT13", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		STYLES.put("BALT14", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
		STYLES.put("BALT16", new Style(Shape.RECT, Style.BLACK, Style.YELLOW));
	}
}
