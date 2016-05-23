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
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Andreas Schildbach
 */
public class VrrProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://efa.vrr.de/standard/";

	// http://app.vrr.de/companion-vrr/

	public VrrProvider()
	{
		this(API_BASE);
	}

	public VrrProvider(final String apiBase)
	{
		super(NetworkId.VRR, apiBase);

		setIncludeRegionId(false);
		setUseProxFootSearch(false);
		setNeedsSpEncId(true);
		setUseRouteIndexAsTripId(false);
		setStyles(STYLES);
		setRequestUrlEncoding(Charsets.ISO_8859_1);
		setSessionCookieName("vrr-efa-lb");
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
			if ("RE6a".equals(trainNum) && trainType == null && trainName == null)
				return new Line(id, network, Product.REGIONAL_TRAIN, trainNum);

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
		STYLES.put("vrr|UU41", new Style(Shape.RECT, Style.parseColor("#ffe700"), Style.GRAY));
		STYLES.put("vrr|UU42", new Style(Shape.RECT, Style.parseColor("#fcb913"), Style.WHITE));
		STYLES.put("vrr|UU43", new Style(Shape.RECT, Style.parseColor("#409387"), Style.WHITE));
		STYLES.put("vrr|UU44", new Style(Shape.RECT, Style.parseColor("#66a3b1"), Style.WHITE));
		STYLES.put("vrr|UU45", new Style(Shape.RECT, Style.parseColor("#ee1c23"), Style.WHITE));
		STYLES.put("vrr|UU46", new Style(Shape.RECT, Style.parseColor("#756fb3"), Style.WHITE));
		STYLES.put("vrr|UU47", new Style(Shape.RECT, Style.parseColor("#8dc63e"), Style.WHITE));
		STYLES.put("vrr|UU49", new Style(Shape.RECT, Style.parseColor("#f7acbc"), Style.WHITE));

		// Düsseldorf
		STYLES.put("vrr|UU70", new Style(Shape.RECT, Style.parseColor("#69b0cd"), Style.WHITE));
		STYLES.put("vrr|UU71", new Style(Shape.RECT, Style.parseColor("#66cef6"), Style.WHITE));
		STYLES.put("vrr|UU72", new Style(Shape.RECT, Style.parseColor("#4cc4c5"), Style.WHITE));
		STYLES.put("vrr|UU73", new Style(Shape.RECT, Style.parseColor("#4763b8"), Style.WHITE));
		STYLES.put("vrr|UU74", new Style(Shape.RECT, Style.parseColor("#27297c"), Style.WHITE));
		STYLES.put("vrr|UU75", new Style(Shape.RECT, Style.parseColor("#079acb"), Style.WHITE));
		STYLES.put("vrr|UU76", new Style(Shape.RECT, Style.parseColor("#1969bc"), Style.WHITE));
		STYLES.put("vrr|UU77", new Style(Shape.RECT, Style.parseColor("#6d90d2"), Style.WHITE));
		STYLES.put("vrr|UU78", new Style(Shape.RECT, Style.parseColor("#02a7eb"), Style.WHITE));
		STYLES.put("vrr|UU79", new Style(Shape.RECT, Style.parseColor("#00aaa0"), Style.WHITE));
		STYLES.put("vrr|UU83", new Style(Shape.RECT, Style.parseColor("#2743a0"), Style.WHITE));
		STYLES.put("vrr|T701", new Style(Shape.RECT, Style.parseColor("#f57215"), Style.WHITE));
		STYLES.put("vrr|T704", new Style(Shape.RECT, Style.parseColor("#c01c23"), Style.WHITE));
		STYLES.put("vrr|T705", new Style(Shape.RECT, Style.parseColor("#bd0c8e"), Style.WHITE));
		STYLES.put("vrr|T706", new Style(Shape.RECT, Style.parseColor("#ed1c24"), Style.WHITE));
		STYLES.put("vrr|T707", new Style(Shape.RECT, Style.parseColor("#72177a"), Style.WHITE));
		STYLES.put("vrr|T708", new Style(Shape.RECT, Style.parseColor("#f680b4"), Style.WHITE));
		STYLES.put("vrr|T709", new Style(Shape.RECT, Style.parseColor("#ef269d"), Style.WHITE));

		// Krefeld
		STYLES.put("vrr|T041", new Style(Style.parseColor("#ee4036"), Style.WHITE));
		STYLES.put("vrr|T042", new Style(Style.parseColor("#f49392"), Style.WHITE));
		STYLES.put("vrr|T043", new Style(Style.parseColor("#bc6ead"), Style.WHITE));
		STYLES.put("vrr|T044", new Style(Style.parseColor("#f36c21"), Style.WHITE));
		STYLES.put("vrr|B045", new Style(Style.parseColor("#00b5e6"), Style.WHITE));
		STYLES.put("vrr|B046", new Style(Style.parseColor("#695073"), Style.WHITE));
		STYLES.put("vrr|B047", new Style(Style.parseColor("#fbce99"), Style.WHITE));
		STYLES.put("vrr|B051", new Style(Style.parseColor("#a1cf73"), Style.WHITE));
		STYLES.put("vrr|B052", new Style(Style.parseColor("#f68f2a"), Style.WHITE));
		STYLES.put("vrr|B054", new Style(Style.parseColor("#048546"), Style.WHITE));
		STYLES.put("vrr|B055", new Style(Style.parseColor("#00b2b7"), Style.WHITE));
		STYLES.put("vrr|B056", new Style(Style.parseColor("#a2689d"), Style.WHITE));
		STYLES.put("vrr|B057", new Style(Style.parseColor("#3bc4e6"), Style.WHITE));
		STYLES.put("vrr|B058", new Style(Style.parseColor("#0081c6"), Style.WHITE));
		STYLES.put("vrr|B059", new Style(Style.parseColor("#9ad099"), Style.WHITE));
		STYLES.put("vrr|B060", new Style(Style.parseColor("#aac3bf"), Style.WHITE));
		STYLES.put("vrr|B061", new Style(Style.parseColor("#ce8d29"), Style.WHITE));
		STYLES.put("vrr|B062", new Style(Style.parseColor("#ae7544"), Style.WHITE));
		STYLES.put("vrr|B068", new Style(Style.parseColor("#1857a7"), Style.WHITE));
		STYLES.put("vrr|B069", new Style(Style.parseColor("#cd7762"), Style.WHITE));
		STYLES.put("vrr|B076", new Style(Style.parseColor("#56a44d"), Style.WHITE));
		STYLES.put("vrr|B077", new Style(Style.parseColor("#fcef08"), Style.WHITE));
		STYLES.put("vrr|B079", new Style(Style.parseColor("#98a3a4"), Style.WHITE));

		// Essen
		STYLES.put("vrr|UU17", new Style(Shape.RECT, Style.parseColor("#68b6e3"), Style.WHITE));
		STYLES.put("vrr|T101", new Style(Shape.RECT, Style.parseColor("#986b17"), Style.WHITE));
		STYLES.put("vrr|T103", new Style(Shape.RECT, Style.parseColor("#ffcc00"), Style.WHITE));
		STYLES.put("vrr|T105", new Style(Shape.RECT, Style.parseColor("#b6cd00"), Style.WHITE));
		STYLES.put("vrr|T106", new Style(Shape.RECT, Style.parseColor("#a695ba"), Style.WHITE));
		STYLES.put("vrr|T108", new Style(Shape.RECT, Style.parseColor("#eca900"), Style.WHITE));
		STYLES.put("vrr|T109", new Style(Shape.RECT, Style.parseColor("#00933a"), Style.WHITE));

		// Duisburg
		STYLES.put("vrr|B905", new Style(Style.parseColor("#c8242b"), Style.WHITE));
		STYLES.put("vrr|B906", new Style(Style.parseColor("#b5ab3a"), Style.WHITE));
		STYLES.put("vrr|B907", new Style(Style.parseColor("#6891c3"), Style.WHITE));
		STYLES.put("vrr|B909", new Style(Style.parseColor("#217e5b"), Style.WHITE));
		STYLES.put("vrr|B910", new Style(Style.parseColor("#d48018"), Style.WHITE));
		STYLES.put("vrr|B917", new Style(Style.parseColor("#23b14b"), Style.WHITE));
		STYLES.put("vrr|B919", new Style(Style.parseColor("#078b4a"), Style.WHITE));
		STYLES.put("vrr|B922", new Style(Style.parseColor("#0072bb"), Style.WHITE));
		STYLES.put("vrr|B923", new Style(Style.parseColor("#00b1c4"), Style.WHITE));
		STYLES.put("vrr|B924", new Style(Style.parseColor("#f37921"), Style.WHITE));
		STYLES.put("vrr|B925", new Style(Style.parseColor("#4876b8"), Style.WHITE));
		STYLES.put("vrr|B926", new Style(Style.parseColor("#649b43"), Style.WHITE));
		STYLES.put("vrr|B928", new Style(Style.parseColor("#c4428c"), Style.WHITE));
		STYLES.put("vrr|B933", new Style(Style.parseColor("#975615"), Style.WHITE));
		STYLES.put("vrr|B934", new Style(Style.parseColor("#009074"), Style.WHITE));
		STYLES.put("vrr|B937", new Style(Style.parseColor("#6f78b5"), Style.WHITE));
		STYLES.put("vrr|B940", new Style(Style.parseColor("#bbbb30"), Style.WHITE));
		STYLES.put("vrr|B942", new Style(Style.parseColor("#930408"), Style.WHITE));
		STYLES.put("vrr|B944", new Style(Style.parseColor("#c52157"), Style.WHITE));
		STYLES.put("vrr|B946", new Style(Style.parseColor("#1cbddc"), Style.WHITE));

		// Oberhausen
		STYLES.put("vrr|B952", new Style(Style.parseColor("#f59598"), Style.WHITE));
		STYLES.put("vrr|B953", new Style(Style.parseColor("#5eb6d9"), Style.WHITE));
		STYLES.put("vrr|B954", new Style(Style.parseColor("#f89d3d"), Style.WHITE));
		STYLES.put("vrr|B955", new Style(Style.parseColor("#8879b8"), Style.WHITE));
		STYLES.put("vrr|B956", new Style(Style.parseColor("#23b24b"), Style.WHITE));
		STYLES.put("vrr|B957", new Style(Style.parseColor("#ebc531"), Style.WHITE));
		STYLES.put("vrr|B960", new Style(Style.parseColor("#aed57f"), Style.WHITE));
		STYLES.put("vrr|B961", new Style(Style.parseColor("#a46f73"), Style.WHITE));
		STYLES.put("vrr|B962", new Style(Style.parseColor("#ae5823"), Style.WHITE));
		STYLES.put("vrr|B966", new Style(Style.parseColor("#c8b3d6"), Style.WHITE));
		STYLES.put("vrr|B976", new Style(Style.parseColor("#d063a5"), Style.WHITE));

		// Mülheim an der Ruhr
		STYLES.put("vrr|T102", new Style(Style.parseColor("#756fb3"), Style.WHITE));
		STYLES.put("vrr|B132", new Style(Style.parseColor("#a3c3d1"), Style.BLACK));
		STYLES.put("vrr|B133", new Style(Style.parseColor("#a9a575"), Style.BLACK));
		STYLES.put("vrr|B134", new Style(Style.parseColor("#806a63"), Style.WHITE));
		STYLES.put("vrr|B135", new Style(Style.parseColor("#425159"), Style.WHITE));

		// Neuss
		STYLES.put("vrr|B842", new Style(Style.parseColor("#fdcc10"), Style.WHITE));
		STYLES.put("vrr|B843", new Style(Style.parseColor("#808180"), Style.WHITE));
		STYLES.put("vrr|B844", new Style(Style.parseColor("#cb1f25"), Style.WHITE));
		STYLES.put("vrr|B848", new Style(Style.parseColor("#be4e26"), Style.WHITE));
		STYLES.put("vrr|B849", new Style(Style.parseColor("#c878b1"), Style.WHITE));
		STYLES.put("vrr|B854", new Style(Style.parseColor("#35bb93"), Style.WHITE));

		// Remscheid
		STYLES.put("vrr|B655", new Style(Style.parseColor("#dbcd00"), Style.WHITE));
		STYLES.put("vrr|B657", new Style(Style.parseColor("#deb993"), Style.WHITE));
		STYLES.put("vrr|B659", new Style(Style.parseColor("#f59b00"), Style.WHITE));
		STYLES.put("vrr|B660", new Style(Style.parseColor("#f5a387"), Style.WHITE));
		STYLES.put("vrr|B664", new Style(Style.parseColor("#b1a8d3"), Style.WHITE));
		STYLES.put("vrr|B666", new Style(Style.parseColor("#0074be"), Style.WHITE));
		STYLES.put("vrr|B673", new Style(Style.parseColor("#ee7555"), Style.WHITE));
		STYLES.put("vrr|B675", new Style(Style.parseColor("#004e9e"), Style.WHITE));
		STYLES.put("vrr|B680", new Style(Style.parseColor("#c78711"), Style.WHITE));

		// Solingen
		STYLES.put("vrr|B681", new Style(Style.parseColor("#016f42"), Style.WHITE));
		STYLES.put("vrr|B682", new Style(Style.parseColor("#009b78"), Style.WHITE));
		STYLES.put("vrr|B684", new Style(Style.parseColor("#009247"), Style.WHITE));
		STYLES.put("vrr|B685", new Style(Style.parseColor("#539138"), Style.WHITE));
		STYLES.put("vrr|B686", new Style(Style.parseColor("#a6c539"), Style.WHITE));
		STYLES.put("vrr|B687", new Style(Style.parseColor("#406ab4"), Style.WHITE));
		STYLES.put("vrr|B689", new Style(Style.parseColor("#8d5e48"), Style.WHITE));
		STYLES.put("vrr|B690", new Style(Style.parseColor("#0099cd"), Style.WHITE));
		STYLES.put("vrr|B691", new Style(Style.parseColor("#963838"), Style.WHITE));
		STYLES.put("vrr|B693", new Style(Style.parseColor("#9a776f"), Style.WHITE));
		STYLES.put("vrr|B695", new Style(Style.parseColor("#bf4b75"), Style.WHITE));
		STYLES.put("vrr|B696", new Style(Style.parseColor("#6c77b4"), Style.WHITE));
		STYLES.put("vrr|B697", new Style(Style.parseColor("#00baf1"), Style.WHITE));
		STYLES.put("vrr|B698", new Style(Style.parseColor("#444fa1"), Style.WHITE));
		STYLES.put("vrr|B699", new Style(Style.parseColor("#c4812f"), Style.WHITE));

		// Busse Wuppertal
		STYLES.put("vrr|B600", new Style(Style.parseColor("#cc4e97"), Style.WHITE));
		STYLES.put("vrr|B603", new Style(Style.parseColor("#a77251"), Style.WHITE));
		STYLES.put("vrr|B604", new Style(Style.parseColor("#f39100"), Style.WHITE));
		STYLES.put("vrr|B606", new Style(Style.parseColor("#88301b"), Style.WHITE));
		STYLES.put("vrr|B607", new Style(Style.parseColor("#629e38"), Style.WHITE));
		STYLES.put("vrr|B609", new Style(Style.parseColor("#53ae2e"), Style.WHITE));
		STYLES.put("vrr|B610", new Style(Style.parseColor("#eb5575"), Style.WHITE));
		STYLES.put("vrr|B611", new Style(Style.parseColor("#896a9a"), Style.WHITE));
		STYLES.put("vrr|B612", new Style(Style.parseColor("#cd7c00"), Style.WHITE));
		STYLES.put("vrr|B613", new Style(Style.parseColor("#491d5c"), Style.WHITE));
		STYLES.put("vrr|B614", new Style(Style.parseColor("#00a7c1"), Style.WHITE));
		STYLES.put("vrr|B616", new Style(Style.parseColor("#e4003a"), Style.WHITE));
		STYLES.put("vrr|B617", new Style(Style.parseColor("#95114d"), Style.WHITE));
		STYLES.put("vrr|B618", new Style(Style.parseColor("#cf8360"), Style.WHITE));
		STYLES.put("vrr|B619", new Style(Style.parseColor("#304c9d"), Style.WHITE));
		STYLES.put("vrr|B622", new Style(Style.parseColor("#aabd81"), Style.WHITE));
		STYLES.put("vrr|B623", new Style(Style.parseColor("#e04a23"), Style.WHITE));
		STYLES.put("vrr|B624", new Style(Style.parseColor("#0e9580"), Style.WHITE));
		STYLES.put("vrr|B625", new Style(Style.parseColor("#7aad3b"), Style.WHITE));
		STYLES.put("vrr|B628", new Style(Style.parseColor("#80753b"), Style.WHITE));
		STYLES.put("vrr|B629", new Style(Style.parseColor("#dd72a1"), Style.WHITE));
		STYLES.put("vrr|B630", new Style(Style.parseColor("#0074be"), Style.WHITE));
		STYLES.put("vrr|B631", new Style(Style.parseColor("#5a8858"), Style.WHITE));
		STYLES.put("vrr|B632", new Style(Style.parseColor("#ebac3d"), Style.WHITE));
		STYLES.put("vrr|B633", new Style(Style.parseColor("#4c2182"), Style.WHITE));
		STYLES.put("vrr|B635", new Style(Style.parseColor("#cb6c2b"), Style.WHITE));
		STYLES.put("vrr|B638", new Style(Style.parseColor("#588d58"), Style.WHITE));
		STYLES.put("vrr|B639", new Style(Style.parseColor("#0097c1"), Style.WHITE));
		STYLES.put("vrr|B640", new Style(Style.parseColor("#89ba7a"), Style.WHITE));
		STYLES.put("vrr|B642", new Style(Style.parseColor("#4b72aa"), Style.WHITE));
		STYLES.put("vrr|B643", new Style(Style.parseColor("#009867"), Style.WHITE));
		STYLES.put("vrr|B644", new Style(Style.parseColor("#a57400"), Style.WHITE));
		STYLES.put("vrr|B645", new Style(Style.parseColor("#aeba0e"), Style.WHITE));
		STYLES.put("vrr|B646", new Style(Style.parseColor("#008db5"), Style.WHITE));
		STYLES.put("vrr|B650", new Style(Style.parseColor("#f5bd00"), Style.WHITE));

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
