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

public class OebbProvider implements NetworkProvider
{
	public static final String NETWORK_ID = "fahrplan.oebb.at";

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

	public QueryConnectionsResult queryConnections(LocationType fromType, String from, LocationType viaType, String via, LocationType toType,
			String to, Date date, boolean dep) throws IOException
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

		uri.append("http://fahrplan.oebb.at/bin/stboard.exe/dn");
		uri.append("?input=").append(stationId);
		uri.append("&boardType=dep");
		uri.append("&productsFilter=111111111111");
		if (maxDepartures != 0)
			uri.append("&maxJourneys=").append(maxDepartures);
		uri.append("&start=yes");

		return uri.toString();
	}

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" // 
			+ "<table class=\"hafasResult\".*?>(.+?)</table>.*?" //
			+ "(?:<table cellspacing=\"0\" class=\"hafasResult\".*?>(.+?)</table>|(verkehren an dieser Haltestelle keine))"//
			+ "|(Eingabe kann nicht interpretiert))" //
			+ ".*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_HEAD_FINE = Pattern.compile(".*?" //
			+ "<td class=\"querysummary screennowrap\">\\s*(.*?)\\s*<a.*?" // location
			+ "(\\d{2}\\.\\d{2}\\.\\d{2}).*?" // date
			+ "Abfahrt (\\d+:\\d+).*?" // time
			+ "%23(\\d+)&.*?" // locationId
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"depboard-\\w*\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile(".*?" //
			+ "<td class=\"[\\w ]*\">(\\d+:\\d+)</td>.*?" // time
			+ "<img src=\"/img/vs_oebb/(\\w+)_pic\\.gif\"\\s+alt=\".*?\">\\s*(.*?)\\s*</.*?" // type, line
			+ "<span class=\"bold\">\n?" //
			+ "<a href=\"http://fahrplan\\.oebb\\.at/bin/stboard\\.exe/dn\\?ld=web25&input=[^%]*?(?:%23(\\d+))?&.*?\">" // destinationId
			+ "\\s*(.*?)\\s*</a>\n?" // destination
			+ "</span>.*?" //
			+ "(?:<td class=\"center sepline top\">\\s*(.*?)\\s*</td>.*?)?" // other stop
	, Pattern.DOTALL);

	public QueryDeparturesResult queryDepartures(final String uri) throws IOException
	{
		// scrape page
		final CharSequence page = ParserUtils.scrape(uri);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(3) != null)
				return new QueryDeparturesResult(uri, Status.NO_INFO);
			else if (mHeadCoarse.group(4) != null)
				return new QueryDeparturesResult(uri, Status.INVALID_STATION);

			final Matcher mHeadFine = P_DEPARTURES_HEAD_FINE.matcher(mHeadCoarse.group(1));
			if (mHeadFine.matches())
			{
				final String location = ParserUtils.resolveEntities(mHeadFine.group(1));
				final Date currentTime = ParserUtils.joinDateTime(ParserUtils.parseDate(mHeadFine.group(2)), ParserUtils
						.parseTime(mHeadFine.group(3)));
				final int stationId = Integer.parseInt(mHeadFine.group(4));
				final List<Departure> departures = new ArrayList<Departure>(8);

				final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(2));
				while (mDepCoarse.find())
				{
					final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(1));
					if (mDepFine.matches())
					{
						final String otherStop = mDepFine.group(6);

						if (otherStop == null || otherStop.contains("&nbsp;"))
						{
							final Calendar current = new GregorianCalendar();
							current.setTime(currentTime);
							final Calendar parsed = new GregorianCalendar();
							parsed.setTime(ParserUtils.parseTime(mDepFine.group(1)));
							parsed.set(Calendar.YEAR, current.get(Calendar.YEAR));
							parsed.set(Calendar.MONTH, current.get(Calendar.MONTH));
							parsed.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
							if (ParserUtils.timeDiff(parsed.getTime(), currentTime) < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
								parsed.add(Calendar.DAY_OF_MONTH, 1);

							final String lineType = mDepFine.group(2);

							final String line = normalizeLine(lineType, ParserUtils.resolveEntities(mDepFine.group(3)));

							final int destinationId = mDepFine.group(4) != null ? Integer.parseInt(mDepFine.group(4)) : 0;

							final String destination = ParserUtils.resolveEntities(mDepFine.group(5));

							final Departure dep = new Departure(parsed.getTime(), line, line != null ? LINES.get(line.charAt(0)) : null,
									destinationId, destination);

							if (!departures.contains(dep))
								departures.add(dep);
						}
					}
					else
					{
						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
					}
				}

				return new QueryDeparturesResult(uri, stationId, location, currentTime, departures);
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

	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüß]+)[\\s-]*(.*)");

	private static String normalizeLine(final String type, final String line)
	{
		final Matcher m = P_NORMALIZE_LINE.matcher(line);
		final String strippedLine = m.matches() ? m.group(1) + m.group(2) : line;

		final char normalizedType = normalizeType(type);
		if (normalizedType != 0)
			return normalizedType + strippedLine;

		throw new IllegalStateException("cannot normalize type " + type + " line " + line);
	}

	private static char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if (ucType.equals("OEC"))
			return 'I';

		if (ucType.equals("R"))
			return 'R';
		if (ucType.equals("REX"))
			return 'R';
		if (ucType.equals("EZ")) // Erlebniszug
			return 'R';

		if (ucType.equals("S"))
			return 'S';

		if (ucType.equals("U"))
			return 'U';

		if (ucType.equals("STR"))
			return 'T';
		if (ucType.equals("LKB"))
			return 'T';

		if (ucType.equals("BUS"))
			return 'B';
		if (ucType.equals("OBU"))
			return 'B';
		if (ucType.equals("AST"))
			return 'B';
		if (ucType.equals("ICB")) // ICBus
			return 'B';

		if (ucType.equals("SCH"))
			return 'F';

		if (ucType.equals("SB"))
			return 'C';
		if (ucType.equals("LIF"))
			return 'C';

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
