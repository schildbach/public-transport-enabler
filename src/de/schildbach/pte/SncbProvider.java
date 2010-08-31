package de.schildbach.pte;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.QueryDeparturesResult.Status;

public class SncbProvider implements NetworkProvider
{
	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public boolean hasCapabilities(final Capability... capabilities)
	{
		throw new UnsupportedOperationException();
	}

	public List<String> autoCompleteStationName(final CharSequence constraint) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public List<Station> nearbyStations(final double lat, final double lon, final int maxDistance, final int maxStations) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public StationLocationResult stationLocation(final String stationId) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryConnections(final LocationType fromType, final String from, final LocationType viaType, final String via,
			final LocationType toType, final String to, final Date date, final boolean dep) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public GetConnectionDetailsResult getConnectionDetails(final String connectionUri) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public String departuresQueryUri(final String stationId, final int maxDepartures)
	{
		final StringBuilder uri = new StringBuilder();
		uri.append("http://hari.b-rail.be/hari3/webserver1/bin/stboard.exe/dox");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50); // maximum taken from SNCB site
		uri.append("&productsFilter=1:1111111111111111");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&start=yes");
		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "<div id=\"hfs_content\">\r\n" //
			+ "<p class=\"qs\">\r\n(.*?)\r\n</p>\r\n" // head
			+ "(.*?)\r\n</div>" // departures
			+ "|(Eingabe kann nicht interpretiert)|(Verbindung zum Server konnte leider nicht hergestellt werden))" //
			+ ".*?", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile("" // 
			+ "<strong>(.*?)</strong><br />\r\n" // location
			+ "Abfahrt (\\d{1,2}:\\d{2}),\r\n" // time
			+ "(\\d{2}/\\d{2}/\\d{2})" // date
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<p class=\"journey\">\r\n(.*?)</p>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<strong>(.*?)</strong>.*?" // line
			+ "&gt;&gt;\r\n" //
			+ "(.*?)\r\n" // destination
			+ "<br />\r\n" //
			+ "<strong>(\\d{1,2}:\\d{2})</strong>\r\n" // time
			+ "(?:<span class=\"delay\">([+-]?\\d+|Ausfall)</span>\r\n)?" // delay
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(uri, Status.SERVICE_DOWN);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDateSlash(mHeadFine.group(3)), ParserUtils.parseTime(mHeadFine
						.group(2)));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(1)));

						final String destination = ParserUtils.resolveEntities(mDepFine.group(2));

						final Calendar current = new GregorianCalendar();
						current.setTime(currentTime);
						final Calendar parsed = new GregorianCalendar();
						parsed.setTime(ParserUtils.parseTime(mDepFine.group(3)));
						parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
						parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
						parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
						if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
							parsed.add(Calendar.DAY_OF_MONTH, 1);

						mDepFine.group(4); // TODO delay

						final Departure dep = new Departure(parsed.getTime(), line, line != null ? LINES.get(line.charAt(0)) : null, null, 0,
								destination);

						if (!departures.contains(dep))
							departures.add(dep);
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, 0, location, currentTime, departures);
			}
			else
			{
				throw new IllegalArgumentException("cannot parse '" + mHeadCoarse.group(1) + "' on " + uri);
			}
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
		}
	}

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû]+)[\\s-]*(.*)");

	private static String normalizeLine(final String line)
	{
		if (line == null || line.length() == 0)
			return null;

		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		if (m.matches())
		{
			final String type = m.group(1);
			final String number = m.group(2);

			final char normalizedType = normalizeType(type);
			if (normalizedType != 0)
				return normalizedType + type + number;

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line " + line);
		}

		throw new IllegalStateException("cannot normalize line " + line);
	}

	private static char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("ICE")) // InterCityExpress
			return 'I';
		if (ucType.equals("IC")) // InterCity
			return 'I';
		if (ucType.equals("EST")) // Eurostar Frankreich
			return 'I';
		if (ucType.equals("THA")) // Thalys
			return 'I';
		if (ucType.equals("TGV")) // Train à Grande Vitesse
			return 'I';
		if (ucType.equals("INT")) // Zürich-Brüssel
			return 'I';

		if (ucType.equals("IR"))
			return 'R';
		if (ucType.equals("L"))
			return 'R';
		if (ucType.equals("P"))
			return 'R';
		if (ucType.equals("CR"))
			return 'R';
		if (ucType.equals("ICT")) // Brügge
			return 'R';
		if (ucType.equals("TRN")) // Mons
			return 'R';

		if (ucType.equals("MÉT"))
			return 'U';

		if (ucType.equals("TRA"))
			return 'T';

		if (ucType.equals("BUS"))
			return 'B';

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

	public int[] lineColors(final String line)
	{
		return LINES.get(line.charAt(0));
	}
}
