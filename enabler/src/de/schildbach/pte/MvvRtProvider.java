package de.schildbach.pte;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.HttpClient;

public class MvvRtProvider extends MvvProvider {

	protected static final String API_BASE = "http://apps.mvg-fahrinfo.de/v5/mvgfahrplan";
	protected static final String METHOD_QUERY_DEPARTURES = "/departureSearchService";

	protected static final Pattern departurePattern = Pattern
			.compile("'([0-9A-Za-z_$]+)',[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,([0-9\\-]+),([0-9\\-]+),[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,([0-9\\-]+),[0-9\\-]+,([0-9\\-]+),");
	// protected static final Pattern departurePattern =
	// Pattern.compile("'([0-9A-Za-z_$]+)',[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,([0-9\\-]+),([0-9\\-]+),[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,([0-9\\-]+),[0-9\\-]+,[0-9\\-]+,[0-9\\-]+,([0-9\\-]+),");
	protected static final Pattern stringTablePattern = Pattern.compile("\\[(\\\".*\\\")\\]");

	protected static final Map<String, String> STATION_EQUIVALENTS = new HashMap<String, String>();
	static
	{
		// EFA => MVG
		STATION_EQUIVALENTS.put("Ostbahnhof", "München Ost");
		STATION_EQUIVALENTS.put("Pasing", "München-Pasing");
		STATION_EQUIVALENTS.put("Flughafen München", "München Flughafen Terminal");
	}

	// we instantiate an own HttpClient with different request headers than the one used by MvvProvider 
	protected final HttpClient myHttpClient = new HttpClient();

	public MvvRtProvider()
	{
		super();
		myHttpClient.setHeader("Content-Type", "text/x-gwt-rpc; charset=UTF-8");
		myHttpClient.setHeader("X-GWT-Permutation", "A4EDAB9DFCA57EE028FD119902D8E469");
	}


	// maxDepartures and equivs are ignored
	public QueryDeparturesResult queryRealtimeDepartures(final String stationId, @Nullable final Date time,
			int maxDepartures, boolean equivs) throws IOException {
		checkNotNull(Strings.emptyToNull(stationId));
		if (time != null) {
			// we don't want to filter out departures of the same minute
			time.setSeconds(0);
		}

		int stationIdInt = Integer.parseInt(stationId);
		if (stationIdInt >= 1000000) {
			stationIdInt -= 1000000;
		}
		// 7|0|10|file:///android_asset/www/mvgfahrplan/|469D117246244E191C0E8D6F6C206027|de.swm.mvgfahrplan.services.DepartureSearchService|getDepartures|de.swm.mvgfahrplan.services.dto.DepartureSearchDTO/4120165780|de.swm.mvgfahrplan.services.dto.Location/3979299160|Marienplatz|M\u00fcnchen|de.swm.mvgfahrplan.services.dto.LocationType/3464228424|1.0-SNAPSHOT|1|2|3|4|1|5|5|1|0|6|0|0|0|
		final StringBuilder body = new StringBuilder(
				"7|0|10|file:///android_asset/www/mvgfahrplan/|469D117246244E191C0E8D6F6C206027|de.swm.mvgfahrplan.services.DepartureSearchService|getDepartures|de.swm.mvgfahrplan.services.dto.DepartureSearchDTO/4120165780|de.swm.mvgfahrplan.services.dto.Location/3979299160|||de.swm.mvgfahrplan.services.dto.LocationType/3464228424|1.0-SNAPSHOT|1|2|3|4|1|5|5|1|0|6|0|0|0|");
		body.append(stationIdInt);
		// |P__________|0|1|0|7|8|9|1|0|10|0|0|1|1|
		body.append("|P__________|0|1|0|7|8|9|1|0|10|0|1|1|1|");

		final CharSequence page = myHttpClient.get(API_BASE + METHOD_QUERY_DEPARTURES, body.toString(), Charsets.UTF_8);

		// System.out.println(page);

		final ResultHeader header = new ResultHeader(NetworkId.MVV, SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		Matcher matcher = stringTablePattern.matcher(page);
		if (matcher.find()) {
			final String stringsTable = matcher.group(1);
			List<String> strings = parseStringTable(stringsTable);

			final List<Departure> departures = new ArrayList<Departure>();

			matcher = departurePattern.matcher(page);
			while (matcher.find()) {
				// System.out.println("Found departure " + matcher.group());
				Date predictedTime = new Date(longFromBase64(matcher.group(1)));
				Line line = new Line(null, NetworkId.MVV.toString().toLowerCase(), parseProduct(strings.get(Integer
						.parseInt(matcher.group(2)) - 1)), strings.get(Integer.parseInt(matcher.group(3)) - 1));
				// Position position = new Position(strings.get(Integer.parseInt(matcher.group(5))-1));
				Location destination = new Location(LocationType.STATION, null, null, strings.get(Integer
						.parseInt(matcher.group(4)) - 1));
				Departure departure = new Departure(null, predictedTime, line, null, destination, null, null);
				if (time == null || !time.after(predictedTime)) {
					departures.add(departure);
				}
				Collections.sort(departures, Departure.TIME_COMPARATOR);
				// System.out.println(departure);
			}
			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
					departures, null));
		}

		return result;
	}


	protected static List<String> parseStringTable(final String stringsTable) {
		List<String> strings = new ArrayList<String>();
		boolean inString = false;
		String currentString = "";
		for (int i = 0; i < stringsTable.length(); i++) {
			char c = stringsTable.charAt(i);
			if (c == '"') {
				if (inString) {
					strings.add(currentString);
					// System.out.println("String [" + currentString + "]");
					currentString = "";
				}
				inString = !inString;
			} else if (inString) {
				currentString += c;
			}
		}
		return strings;
	}


	private static long longFromBase64(String value) {
		int pos = 0;
		long longVal = base64Value(value.charAt(pos++));
		int len = value.length();
		while (pos < len) {
			longVal <<= 6;
			longVal |= base64Value(value.charAt(pos++));
		}
		return longVal;
	}


	private static int base64Value(char digit) {
		if (digit >= 'A' && digit <= 'Z')
			return digit - 'A';
		if (digit >= 'a')
			return digit - 'a' + 26;
		if (digit >= '0' && digit <= '9')
			return digit - '0' + 52;
		if (digit == '$')
			return 62;
		return 63;
	}


	private static Product parseProduct(String product) {
		if (product.equals("U_BAHN") || product.equals("U")) {
			return Product.SUBWAY;
		} else if (product.equals("S_BAHN") || product.equals("S")) {
			return Product.SUBURBAN_TRAIN;
		} else if (product.equals("TRAM") || product.equals("T")) {
			return Product.TRAM;
		} else if (product.equals("BUS") || product.equals("B")) {
			return Product.BUS;
		} else {
			throw new IllegalArgumentException("unknown product: '" + product + "'");
		}
	}


	@Override
	public QueryDeparturesResult queryDepartures(final String stationId, @Nullable final Date time, int maxDepartures,
			boolean equivs) throws IOException
	{
		final QueryDeparturesResult efaResult = super.queryDepartures(stationId, time, maxDepartures, equivs);
		if (efaResult.stationDepartures.size() == 0) {
			return efaResult;
		}
		final QueryDeparturesResult mvgResult = queryRealtimeDepartures(stationId, time, maxDepartures, equivs);
		// System.out.println("efaResult: " + efaResult);
		// System.out.println("mvgResult: " + mvgResult);
		// TODO do we need to handle the case of > 1 stationDepartures?
		// TODO optimize loop
		List<Departure> efaDepartures = efaResult.stationDepartures.get(0).departures;
		Map<Integer, Long> bestDeltaPerEfaDeparture = new TreeMap<Integer, Long>();
		for (Departure mvgDeparture : mvgResult.stationDepartures.get(0).departures) {
			long bestDelta = -1;
			int bestEfaIndex = -1;
			for (int i = 0; i < efaDepartures.size(); i++) {
				Departure efaDeparture = efaDepartures.get(i);
				if (destinationMatches(efaDeparture, mvgDeparture)) {
					// System.out.println("Match!");
					long delta = mvgDeparture.getTime().getTime() - efaDeparture.getTime().getTime();
					if ((bestDelta == -1 || Math.abs(delta) < Math.abs(bestDelta)) && delta > -120000) {
						bestDelta = delta;
						bestEfaIndex = i;
					}
				}
			}
			if (bestEfaIndex != -1) {
				Long previousBestEfaDelta = bestDeltaPerEfaDeparture.get(bestEfaIndex);
				/* if (previousBestEfaDelta != null) {
					System.out.println(bestEfaIndex + ": " + previousBestEfaDelta + " vs. " + bestDelta);
				} */
				if (previousBestEfaDelta == null || Math.abs(previousBestEfaDelta) > Math.abs(bestDelta)) {
					// System.out.println((previousBestEfaDelta == null ? "Inserting" : "Overwriting") + " predictedTime " + mvgDeparture.predictedTime + " into " + efaDepartures.get(bestEfaIndex));
					efaDepartures.get(bestEfaIndex).predictedTime = mvgDeparture.predictedTime;
					bestDeltaPerEfaDeparture.put(bestEfaIndex, bestDelta);
				}
			} else {
				// System.out.println("No match found for " + mvgDeparture);
				// TODO Should we insert those MVG Departures? E.g. Ausrueck- and Einrueckfahrten, some SEV departures are unknown to EFA. 
			}
		}

		return efaResult;
	}


	private static boolean destinationMatches(Departure efaDeparture, Departure mvgDeparture) {
		final Location efaDestination = efaDeparture.destination;
		final Location mvgDestination = mvgDeparture.destination;
		if (efaDestination != null && mvgDestination != null) {
			final String efaDestinationName = efaDestination.name;
			final String mvgDestinationName = mvgDestination.name;
			final String equivDestinationName = STATION_EQUIVALENTS.get(efaDestinationName);
			return efaDeparture.line.equals(mvgDeparture.line) &&
					((equivDestinationName != null && mvgDestinationName.equals(equivDestinationName))
					|| (efaDestinationName.substring(0, 3).equals(mvgDestinationName.substring(0, 3)))
					|| (mvgDestinationName.startsWith("SEV ") && efaDestinationName.substring(0, 3).equals(mvgDestinationName.substring(4, 7))));
		} else {
			return (efaDestination == null && mvgDestination == null);
		}
	}
}
