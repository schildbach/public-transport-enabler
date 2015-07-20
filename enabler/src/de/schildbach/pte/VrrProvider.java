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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class VrrProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://app.vrr.de/standard/";

	// http://app.vrr.de/companion-vrr/

	public VrrProvider()
	{
		super(NetworkId.VRR, API_BASE);

		setIncludeRegionId(false);
		setUseProxFootSearch(false);
		setNeedsSpEncId(true);
		setUseRouteIndexAsTripId(false);
		setStyles(STYLES);
		setRequestUrlEncoding(Charsets.ISO_8859_1);
	}

	@Override
	protected String xsltTripRequestParameters(final Location from, final @Nullable Location via, final Location to, final Date time,
			final boolean dep, final @Nullable Collection<Product> products, final @Nullable Optimize optimize, final @Nullable WalkSpeed walkSpeed,
			final @Nullable Accessibility accessibility, final @Nullable Set<Option> options)
	{
		final StringBuilder uri = new StringBuilder(super.xsltTripRequestParameters(from, via, to, time, dep, products, optimize, walkSpeed,
				accessibility, options));

		if (products != null)
		{
			for (final Product p : products)
			{
				if (p == Product.CABLECAR)
					uri.append("&inclMOT_11=on"); // Schwebebahn
			}
		}

		return uri.toString();
	}

	@Override
	protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot, final @Nullable String symbol,
			final @Nullable String name, final @Nullable String longName, final @Nullable String trainType, final @Nullable String trainNum,
			final @Nullable String trainName)
	{
		if ("0".equals(mot))
		{
			if ("Regionalbahn".equals(trainName) && symbol != null)
				return new Line(id, network, Product.REGIONAL_TRAIN, symbol);
			if ("NordWestBahn".equals(trainName) && symbol != null)
				return new Line(id, network, Product.REGIONAL_TRAIN, symbol);

			if (trainType == null && "SEV7".equals(trainNum))
				return new Line(id, network, Product.BUS, trainNum);

			if ("Zug".equals(longName))
				return new Line(id, network, null, "Zug");
		}
		else if ("11".equals(mot))
		{
			// Wuppertaler Schwebebahn & SkyTrain D'dorf
			if ("Schwebebahn".equals(trainName) || (longName != null && longName.startsWith("Schwebebahn")))
				return new Line(id, network, Product.CABLECAR, name);

			// H-Bahn TU Dortmund
			if ("H-Bahn".equals(trainName) || (longName != null && longName.startsWith("H-Bahn")))
				return new Line(id, network, Product.CABLECAR, name);
		}

		return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// Schnellbusse VRR
		STYLES.put("vrr|BSB", new Style(Style.parseColor("#00919d"), Style.WHITE));

		// Stadtbahn Dortmund
		STYLES.put("vrr|UU41", new Style(Style.parseColor("#ffe700"), Style.GRAY));
		STYLES.put("vrr|UU42", new Style(Style.parseColor("#fcb913"), Style.WHITE));
		STYLES.put("vrr|UU43", new Style(Style.parseColor("#409387"), Style.WHITE));
		STYLES.put("vrr|UU44", new Style(Style.parseColor("#66a3b1"), Style.WHITE));
		STYLES.put("vrr|UU45", new Style(Style.parseColor("#ee1c23"), Style.WHITE));
		STYLES.put("vrr|UU46", new Style(Style.parseColor("#756fb3"), Style.WHITE));
		STYLES.put("vrr|UU47", new Style(Style.parseColor("#8dc63e"), Style.WHITE));
		STYLES.put("vrr|UU49", new Style(Style.parseColor("#f7acbc"), Style.WHITE));

		// H-Bahn Dortmund
		STYLES.put("vrr|CHB1", new Style(Style.parseColor("#e5007c"), Style.WHITE));
		STYLES.put("vrr|CHB2", new Style(Style.parseColor("#e5007c"), Style.WHITE));

		// Schwebebahn Wuppertal
		STYLES.put("vrr|C60", new Style(Style.parseColor("#003090"), Style.WHITE));

		// Stadtbahn Köln-Bonn
		STYLES.put("vrs|T1", new Style(Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("vrs|T3", new Style(Style.parseColor("#f680c5"), Style.WHITE));
		STYLES.put("vrs|T4", new Style(Style.parseColor("#f24dae"), Style.WHITE));
		STYLES.put("vrs|T5", new Style(Style.parseColor("#9c8dce"), Style.WHITE));
		STYLES.put("vrs|T7", new Style(Style.parseColor("#f57947"), Style.WHITE));
		STYLES.put("vrs|T9", new Style(Style.parseColor("#f5777b"), Style.WHITE));
		STYLES.put("vrs|T12", new Style(Style.parseColor("#80cc28"), Style.WHITE));
		STYLES.put("vrs|T13", new Style(Style.parseColor("#9e7b65"), Style.WHITE));
		STYLES.put("vrs|T15", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
		STYLES.put("vrs|T16", new Style(Style.parseColor("#33baab"), Style.WHITE));
		STYLES.put("vrs|T18", new Style(Style.parseColor("#05a1e6"), Style.WHITE));
		STYLES.put("vrs|T61", new Style(Style.parseColor("#80cc28"), Style.WHITE));
		STYLES.put("vrs|T62", new Style(Style.parseColor("#4dbd38"), Style.WHITE));
		STYLES.put("vrs|T63", new Style(Style.parseColor("#73d2f6"), Style.WHITE));
		STYLES.put("vrs|T65", new Style(Style.parseColor("#b3db18"), Style.WHITE));
		STYLES.put("vrs|T66", new Style(Style.parseColor("#ec008c"), Style.WHITE));
		STYLES.put("vrs|T67", new Style(Style.parseColor("#f680c5"), Style.WHITE));
		STYLES.put("vrs|T68", new Style(Style.parseColor("#ca93d0"), Style.WHITE));

		// Stadtbahn Bielefeld
		STYLES.put("owl|T1", new Style(Style.parseColor("#00aeef"), Style.WHITE));
		STYLES.put("owl|T2", new Style(Style.parseColor("#00a650"), Style.WHITE));
		STYLES.put("owl|T3", new Style(Style.parseColor("#fff200"), Style.BLACK));

		// Busse Bonn
		STYLES.put("vrs|B63", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B16", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B66", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B67", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B68", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B18", new Style(Style.parseColor("#0065ae"), Style.WHITE));
		STYLES.put("vrs|B61", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B62", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|B65", new Style(Style.parseColor("#e4000b"), Style.WHITE));
		STYLES.put("vrs|BSB55", new Style(Style.parseColor("#00919e"), Style.WHITE));
		STYLES.put("vrs|BSB60", new Style(Style.parseColor("#8f9867"), Style.WHITE));
		STYLES.put("vrs|BSB69", new Style(Style.parseColor("#db5f1f"), Style.WHITE));
		STYLES.put("vrs|B529", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B537", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B541", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B550", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B163", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B551", new Style(Style.parseColor("#2e2383"), Style.WHITE));
		STYLES.put("vrs|B600", new Style(Style.parseColor("#817db7"), Style.WHITE));
		STYLES.put("vrs|B601", new Style(Style.parseColor("#831b82"), Style.WHITE));
		STYLES.put("vrs|B602", new Style(Style.parseColor("#dd6ba6"), Style.WHITE));
		STYLES.put("vrs|B603", new Style(Style.parseColor("#e6007d"), Style.WHITE));
		STYLES.put("vrs|B604", new Style(Style.parseColor("#009f5d"), Style.WHITE));
		STYLES.put("vrs|B605", new Style(Style.parseColor("#007b3b"), Style.WHITE));
		STYLES.put("vrs|B606", new Style(Style.parseColor("#9cbf11"), Style.WHITE));
		STYLES.put("vrs|B607", new Style(Style.parseColor("#60ad2a"), Style.WHITE));
		STYLES.put("vrs|B608", new Style(Style.parseColor("#f8a600"), Style.WHITE));
		STYLES.put("vrs|B609", new Style(Style.parseColor("#ef7100"), Style.WHITE));
		STYLES.put("vrs|B610", new Style(Style.parseColor("#3ec1f1"), Style.WHITE));
		STYLES.put("vrs|B611", new Style(Style.parseColor("#0099db"), Style.WHITE));
		STYLES.put("vrs|B612", new Style(Style.parseColor("#ce9d53"), Style.WHITE));
		STYLES.put("vrs|B613", new Style(Style.parseColor("#7b3600"), Style.WHITE));
		STYLES.put("vrs|B614", new Style(Style.parseColor("#806839"), Style.WHITE));
		STYLES.put("vrs|B615", new Style(Style.parseColor("#532700"), Style.WHITE));
		STYLES.put("vrs|B630", new Style(Style.parseColor("#c41950"), Style.WHITE));
		STYLES.put("vrs|B631", new Style(Style.parseColor("#9b1c44"), Style.WHITE));
		STYLES.put("vrs|B633", new Style(Style.parseColor("#88cdc7"), Style.WHITE));
		STYLES.put("vrs|B635", new Style(Style.parseColor("#cec800"), Style.WHITE));
		STYLES.put("vrs|B636", new Style(Style.parseColor("#af0223"), Style.WHITE));
		STYLES.put("vrs|B637", new Style(Style.parseColor("#e3572a"), Style.WHITE));
		STYLES.put("vrs|B638", new Style(Style.parseColor("#af5836"), Style.WHITE));
		STYLES.put("vrs|B640", new Style(Style.parseColor("#004f81"), Style.WHITE));
		STYLES.put("vrs|BT650", new Style(Style.parseColor("#54baa2"), Style.WHITE));
		STYLES.put("vrs|BT651", new Style(Style.parseColor("#005738"), Style.WHITE));
		STYLES.put("vrs|BT680", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B800", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B812", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B843", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B845", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B852", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B855", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B856", new Style(Style.parseColor("#4e6578"), Style.WHITE));
		STYLES.put("vrs|B857", new Style(Style.parseColor("#4e6578"), Style.WHITE));
	}

	@Override
	public Style lineStyle(final @Nullable String network, final @Nullable Product product, final @Nullable String label)
	{
		if (product == Product.BUS && label != null && label.startsWith("SB"))
			return super.lineStyle(network, product, "SB");

		return super.lineStyle(network, product, label);
	}
}
