/*
 * Copyright 2010-2013 the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Fare.Type;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.exception.InvalidDataException;
import de.schildbach.pte.exception.NotFoundException;
import de.schildbach.pte.exception.ParserException;
import de.schildbach.pte.exception.ProtocolException;
import de.schildbach.pte.exception.SessionExpiredException;
import de.schildbach.pte.util.ParserUtils;
import de.schildbach.pte.util.XmlPullUtil;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractEfaProvider extends AbstractNetworkProvider
{
	protected static final String DEFAULT_DEPARTURE_MONITOR_ENDPOINT = "XSLT_DM_REQUEST";
	protected static final String DEFAULT_TRIP_ENDPOINT = "XSLT_TRIP_REQUEST2";
	protected static final String DEFAULT_STOPFINDER_ENDPOINT = "XML_STOPFINDER_REQUEST";
	protected static final String DEFAULT_COORD_ENDPOINT = "XML_COORD_REQUEST";

	protected static final String SERVER_PRODUCT = "efa";

	private final String departureMonitorEndpoint;
	private final String tripEndpoint;
	private final String stopFinderEndpoint;
	private final String coordEndpoint;

	private String additionalQueryParameter = null;
	private boolean canAcceptPoiId = false;
	private boolean needsSpEncId = false;
	private boolean includeRegionId = true;
	private Charset requestUrlEncoding = ISO_8859_1;
	private String httpReferer = null;
	private String httpRefererTrip = null;
	private boolean httpPost = false;
	private boolean suppressPositions = false;
	private boolean useRouteIndexAsTripId = true;
	private boolean useLineRestriction = true;

	private final XmlPullParserFactory parserFactory;

	private static class Context implements QueryTripsContext
	{
		private final String context;

		private Context(final String context)
		{
			this.context = context;
		}

		public boolean canQueryLater()
		{
			return context != null;
		}

		public boolean canQueryEarlier()
		{
			return false; // TODO enable earlier querying
		}

		@Override
		public String toString()
		{
			return getClass().getName() + "[" + context + "]";
		}
	}

	public AbstractEfaProvider(final String apiBase)
	{
		this(apiBase, null, null, null, null);
	}

	public AbstractEfaProvider(final String apiBase, final String departureMonitorEndpoint, final String tripEndpoint,
			final String stopFinderEndpoint, final String coordEndpoint)
	{
		this(apiBase + (departureMonitorEndpoint != null ? departureMonitorEndpoint : DEFAULT_DEPARTURE_MONITOR_ENDPOINT), //
				apiBase + (tripEndpoint != null ? tripEndpoint : DEFAULT_TRIP_ENDPOINT), //
				apiBase + (stopFinderEndpoint != null ? stopFinderEndpoint : DEFAULT_STOPFINDER_ENDPOINT), //
				apiBase + (coordEndpoint != null ? coordEndpoint : DEFAULT_COORD_ENDPOINT));
	}

	private AbstractEfaProvider(final String departureMonitorEndpoint, final String tripEndpoint, final String stopFinderEndpoint,
			final String coordEndpoint)
	{
		try
		{
			parserFactory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
		}
		catch (final XmlPullParserException x)
		{
			throw new RuntimeException(x);
		}

		this.departureMonitorEndpoint = departureMonitorEndpoint;
		this.tripEndpoint = tripEndpoint;
		this.stopFinderEndpoint = stopFinderEndpoint;
		this.coordEndpoint = coordEndpoint;
	}

	protected void setRequestUrlEncoding(final Charset requestUrlEncoding)
	{
		this.requestUrlEncoding = requestUrlEncoding;
	}

	protected void setHttpReferer(final String httpReferer)
	{
		this.httpReferer = httpReferer;
		this.httpRefererTrip = httpReferer;
	}

	public void setHttpRefererTrip(final String httpRefererTrip)
	{
		this.httpRefererTrip = httpRefererTrip;
	}

	protected void setHttpPost(final boolean httpPost)
	{
		this.httpPost = httpPost;
	}

	protected void setIncludeRegionId(final boolean includeRegionId)
	{
		this.includeRegionId = includeRegionId;
	}

	protected void setSuppressPositions(final boolean suppressPositions)
	{
		this.suppressPositions = suppressPositions;
	}

	protected void setUseRouteIndexAsTripId(final boolean useRouteIndexAsTripId)
	{
		this.useRouteIndexAsTripId = useRouteIndexAsTripId;
	}

	protected void setUseLineRestriction(final boolean useLineRestriction)
	{
		this.useLineRestriction = useLineRestriction;
	}

	protected void setCanAcceptPoiId(final boolean canAcceptPoiId)
	{
		this.canAcceptPoiId = canAcceptPoiId;
	}

	protected void setNeedsSpEncId(final boolean needsSpEncId)
	{
		this.needsSpEncId = needsSpEncId;
	}

	protected void setAdditionalQueryParameter(final String additionalQueryParameter)
	{
		this.additionalQueryParameter = additionalQueryParameter;
	}

	protected TimeZone timeZone()
	{
		return TimeZone.getTimeZone("Europe/Berlin");
	}

	private final void appendCommonRequestParams(final StringBuilder uri, final String outputFormat)
	{
		uri.append("?outputFormat=").append(outputFormat);
		uri.append("&coordOutputFormat=WGS84");
		if (additionalQueryParameter != null)
			uri.append('&').append(additionalQueryParameter);
	}

	protected List<Location> jsonStopfinderRequest(final Location constraint) throws IOException
	{
		final StringBuilder parameters = new StringBuilder();
		appendCommonRequestParams(parameters, "JSON");
		parameters.append("&locationServerActive=1");
		if (includeRegionId)
			parameters.append("&regionID_sf=1"); // prefer own region
		appendLocation(parameters, constraint, "sf");
		if (constraint.type == LocationType.ANY)
			// 1=place 2=stop 4=street 8=address 16=crossing 32=poi 64=postcode
			parameters.append("&anyObjFilter_sf=").append(2 + 4 + 8 + 16 + 32 + 64);
		parameters.append("&anyMaxSizeHitList=500");

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		final CharSequence page = ParserUtils.scrape(uri.toString(), httpPost ? parameters.substring(1) : null, UTF_8, null);

		try
		{
			final List<Location> results = new ArrayList<Location>();

			final JSONObject head = new JSONObject(page.toString());
			final JSONObject stopFinder = head.optJSONObject("stopFinder");
			final JSONArray stops;
			if (stopFinder == null)
			{
				stops = head.getJSONArray("stopFinder");
			}
			else
			{
				final JSONObject points = stopFinder.optJSONObject("points");
				if (points != null)
				{
					final JSONObject stop = points.getJSONObject("point");
					final Location location = parseJsonStop(stop);
					results.add(location);
					return results;
				}

				stops = stopFinder.getJSONArray("points");
			}

			final int nStops = stops.length();

			for (int i = 0; i < nStops; i++)
			{
				final JSONObject stop = stops.optJSONObject(i);
				final Location location = parseJsonStop(stop);
				results.add(location);
			}

			return results;
		}
		catch (final JSONException x)
		{
			throw new RuntimeException("cannot parse: '" + page + "' on " + uri, x);
		}
	}

	private Location parseJsonStop(final JSONObject stop) throws JSONException
	{
		String type = stop.getString("type");
		if ("any".equals(type))
			type = stop.getString("anyType");
		final String name = normalizeLocationName(stop.getString("object"));
		final JSONObject ref = stop.getJSONObject("ref");
		String place = ref.getString("place");
		if (place != null && place.length() == 0)
			place = null;
		final String coords = ref.optString("coords", null);
		final int lat;
		final int lon;
		if (coords != null)
		{
			final String[] coordParts = coords.split(",");
			lat = Math.round(Float.parseFloat(coordParts[1]));
			lon = Math.round(Float.parseFloat(coordParts[0]));
		}
		else
		{
			lat = 0;
			lon = 0;
		}

		if ("stop".equals(type))
			return new Location(LocationType.STATION, stop.getInt("stateless"), lat, lon, place, name);
		else if ("poi".equals(type))
			return new Location(LocationType.POI, 0, lat, lon, place, name);
		else if ("crossing".equals(type))
			return new Location(LocationType.ADDRESS, 0, lat, lon, place, name);
		else if ("street".equals(type) || "address".equals(type) || "singlehouse".equals(type))
			return new Location(LocationType.ADDRESS, 0, lat, lon, place, normalizeLocationName(stop.getString("name")));
		else
			throw new JSONException("unknown type: " + type);
	}

	private StringBuilder stopfinderRequestParameters(final Location constraint)
	{
		final StringBuilder parameters = new StringBuilder();
		appendCommonRequestParams(parameters, "XML");
		parameters.append("&locationServerActive=1");
		if (includeRegionId)
			parameters.append("&regionID_sf=1"); // prefer own region
		appendLocation(parameters, constraint, "sf");
		if (constraint.type == LocationType.ANY)
		{
			if (needsSpEncId)
				parameters.append("&SpEncId=0");
			// 1=place 2=stop 4=street 8=address 16=crossing 32=poi 64=postcode
			parameters.append("&anyObjFilter_sf=").append(2 + 4 + 8 + 16 + 32 + 64);
			parameters.append("&reducedAnyPostcodeObjFilter_sf=64&reducedAnyTooManyObjFilter_sf=2");
			parameters.append("&useHouseNumberList=true");
		}

		return parameters;
	}

	protected List<Location> xmlStopfinderRequest(final Location constraint) throws IOException
	{
		final StringBuilder parameters = stopfinderRequestParameters(constraint);

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			enterItdRequest(pp);

			final List<Location> results = new ArrayList<Location>();

			XmlPullUtil.enter(pp, "itdStopFinderRequest");

			XmlPullUtil.require(pp, "itdOdv");
			if (!"sf".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"sf\" />");
			XmlPullUtil.enter(pp, "itdOdv");

			XmlPullUtil.require(pp, "itdOdvPlace");
			XmlPullUtil.next(pp);

			XmlPullUtil.require(pp, "itdOdvName");
			final String nameState = pp.getAttributeValue(null, "state");
			XmlPullUtil.enter(pp, "itdOdvName");

			if (XmlPullUtil.test(pp, "itdMessage"))
				XmlPullUtil.next(pp);

			if ("identified".equals(nameState) || "list".equals(nameState))
			{
				while (XmlPullUtil.test(pp, "odvNameElem"))
					results.add(processOdvNameElem(pp, null));
			}
			else if ("notidentified".equals(nameState))
			{
				// do nothing
			}
			else
			{
				throw new RuntimeException("unknown nameState '" + nameState + "' on " + uri);
			}

			XmlPullUtil.exit(pp, "itdOdvName");

			XmlPullUtil.exit(pp, "itdOdv");

			XmlPullUtil.exit(pp, "itdStopFinderRequest");

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private class LocationAndQuality implements Serializable, Comparable<LocationAndQuality>
	{
		public final Location location;
		public final int quality;

		public LocationAndQuality(final Location location, final int quality)
		{
			this.location = location;
			this.quality = quality;
		}

		public int compareTo(final LocationAndQuality other)
		{
			// prefer quality
			if (this.quality > other.quality)
				return -1;
			else if (this.quality < other.quality)
				return 1;

			// prefer stations
			final int compareLocationType = this.location.type.compareTo(other.location.type);
			if (compareLocationType != 0)
				return compareLocationType;

			return 0;
		}

		@Override
		public boolean equals(final Object o)
		{
			if (o == this)
				return true;
			if (!(o instanceof LocationAndQuality))
				return false;
			final LocationAndQuality other = (LocationAndQuality) o;
			return location.equals(other.location);
		}

		@Override
		public int hashCode()
		{
			return location.hashCode();
		}

		@Override
		public String toString()
		{
			return quality + ":" + location;
		}
	}

	protected List<Location> mobileStopfinderRequest(final Location constraint) throws IOException
	{
		final StringBuilder parameters = stopfinderRequestParameters(constraint);

		final StringBuilder uri = new StringBuilder(stopFinderEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			enterEfa(pp);

			final List<LocationAndQuality> locations = new ArrayList<LocationAndQuality>();

			XmlPullUtil.enter(pp, "sf");

			while (XmlPullUtil.test(pp, "p"))
			{
				XmlPullUtil.enter(pp, "p");

				final String name = normalizeLocationName(requireValueTag(pp, "n"));
				final String u = requireValueTag(pp, "u");
				if (!"sf".equals(u))
					throw new RuntimeException("unknown usage: " + u);
				final String ty = requireValueTag(pp, "ty");
				final LocationType type;
				if ("stop".equals(ty))
					type = LocationType.STATION;
				else if ("poi".equals(ty))
					type = LocationType.POI;
				else if ("loc".equals(ty))
					type = LocationType.ADDRESS;
				else if ("street".equals(ty))
					type = LocationType.ADDRESS;
				else if ("singlehouse".equals(ty))
					type = LocationType.ADDRESS;
				else
					throw new RuntimeException("unknown type: " + ty);

				XmlPullUtil.enter(pp, "r");

				final int id = Integer.parseInt(requireValueTag(pp, "id"));
				requireValueTag(pp, "omc");
				final String place = normalizeLocationName(optValueTag(pp, "pc"));
				requireValueTag(pp, "pid");
				final Point coord = coordStrToPoint(optValueTag(pp, "c"));

				XmlPullUtil.exit(pp, "r");

				final String qal = optValueTag(pp, "qal");
				final int quality = qal != null ? Integer.parseInt(qal) : 0;

				XmlPullUtil.exit(pp, "p");

				final Location location = new Location(type, type == LocationType.STATION ? id : 0, coord != null ? coord.lat : 0,
						coord != null ? coord.lon : 0, place, name);
				final LocationAndQuality locationAndQuality = new LocationAndQuality(location, quality);
				locations.add(locationAndQuality);
			}

			XmlPullUtil.exit(pp, "sf");

			Collections.sort(locations);

			final List<Location> results = new ArrayList<Location>(locations.size());
			for (final LocationAndQuality location : locations)
				results.add(location.location);

			return results;
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private StringBuilder xmlCoordRequestParameters(final int lat, final int lon, final int maxDistance, final int maxStations)
	{
		final StringBuilder parameters = new StringBuilder();
		appendCommonRequestParams(parameters, "XML");
		parameters.append("&coord=").append(String.format(Locale.ENGLISH, "%2.6f:%2.6f:WGS84", latLonToDouble(lon), latLonToDouble(lat)));
		parameters.append("&coordListOutputFormat=STRING");
		parameters.append("&max=").append(maxStations != 0 ? maxStations : 50);
		parameters.append("&inclFilter=1&radius_1=").append(maxDistance != 0 ? maxDistance : 1320);
		parameters.append("&type_1=STOP"); // ENTRANCE, BUS_POINT, POI_POINT

		return parameters;
	}

	protected NearbyStationsResult xmlCoordRequest(final int lat, final int lon, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder parameters = xmlCoordRequestParameters(lat, lon, maxDistance, maxStations);

		final StringBuilder uri = new StringBuilder(coordEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			final ResultHeader header = enterItdRequest(pp);

			XmlPullUtil.enter(pp, "itdCoordInfoRequest");

			XmlPullUtil.enter(pp, "itdCoordInfo");

			XmlPullUtil.enter(pp, "coordInfoRequest");
			XmlPullUtil.exit(pp, "coordInfoRequest");

			final List<Location> stations = new ArrayList<Location>();

			if (XmlPullUtil.test(pp, "coordInfoItemList"))
			{
				XmlPullUtil.enter(pp, "coordInfoItemList");

				while (XmlPullUtil.test(pp, "coordInfoItem"))
				{
					if (!"STOP".equals(pp.getAttributeValue(null, "type")))
						throw new RuntimeException("unknown type");

					final int id = XmlPullUtil.intAttr(pp, "id");
					final String name = normalizeLocationName(XmlPullUtil.attr(pp, "name"));
					final String place = normalizeLocationName(XmlPullUtil.attr(pp, "locality"));

					XmlPullUtil.enter(pp, "coordInfoItem");

					// FIXME this is always only one coordinate
					final Point coord = processItdPathCoordinates(pp).get(0);

					XmlPullUtil.exit(pp, "coordInfoItem");

					stations.add(new Location(LocationType.STATION, id, coord.lat, coord.lon, place, name));
				}

				XmlPullUtil.exit(pp, "coordInfoItemList");
			}

			return new NearbyStationsResult(header, stations);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	protected NearbyStationsResult mobileCoordRequest(final int lat, final int lon, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder parameters = xmlCoordRequestParameters(lat, lon, maxDistance, maxStations);

		final StringBuilder uri = new StringBuilder(coordEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			final ResultHeader header = enterEfa(pp);

			XmlPullUtil.enter(pp, "ci");

			XmlPullUtil.enter(pp, "request");
			XmlPullUtil.exit(pp, "request");

			final List<Location> stations = new ArrayList<Location>();

			if (XmlPullUtil.test(pp, "pis"))
			{
				XmlPullUtil.enter(pp, "pis");

				while (XmlPullUtil.test(pp, "pi"))
				{
					XmlPullUtil.enter(pp, "pi");

					final String name = normalizeLocationName(requireValueTag(pp, "de"));
					final String type = requireValueTag(pp, "ty");
					if (!"STOP".equals(type))
						throw new RuntimeException("unknown type");

					final int id = Integer.parseInt(requireValueTag(pp, "id"));
					requireValueTag(pp, "omc");
					requireValueTag(pp, "pid");
					final String place = normalizeLocationName(requireValueTag(pp, "locality"));
					requireValueTag(pp, "layer");
					requireValueTag(pp, "gisID");
					requireValueTag(pp, "ds");
					final Point coord = coordStrToPoint(requireValueTag(pp, "c"));

					stations.add(new Location(LocationType.STATION, id, coord.lat, coord.lon, place, name));

					XmlPullUtil.exit(pp, "pi");
				}

				XmlPullUtil.exit(pp, "pis");
			}

			XmlPullUtil.exit(pp, "ci");

			return new NearbyStationsResult(header, stations);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return jsonStopfinderRequest(new Location(LocationType.ANY, 0, null, constraint.toString()));
	}

	private String processItdOdvPlace(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.test(pp, "itdOdvPlace"))
			throw new IllegalStateException("expecting <itdOdvPlace />");

		final String placeState = XmlPullUtil.attr(pp, "state");

		XmlPullUtil.enter(pp, "itdOdvPlace");
		String place = null;
		if ("identified".equals(placeState))
		{
			if (XmlPullUtil.test(pp, "odvPlaceElem"))
			{
				XmlPullUtil.enter(pp, "odvPlaceElem");
				place = normalizeLocationName(pp.getText());
				XmlPullUtil.exit(pp, "odvPlaceElem");
			}
		}
		XmlPullUtil.exit(pp, "itdOdvPlace");

		return place;
	}

	private Location processOdvNameElem(final XmlPullParser pp, final String defaultPlace) throws XmlPullParserException, IOException
	{
		if (!XmlPullUtil.test(pp, "odvNameElem"))
			throw new IllegalStateException("expecting <odvNameElem />");

		final String anyType = pp.getAttributeValue(null, "anyType");
		final String idStr = pp.getAttributeValue(null, "id");
		final String stopIdStr = pp.getAttributeValue(null, "stopID");
		final String poiIdStr = pp.getAttributeValue(null, "poiID");
		final String streetIdStr = pp.getAttributeValue(null, "streetID");
		final String place = !"loc".equals(anyType) ? normalizeLocationName(pp.getAttributeValue(null, "locality")) : null;
		final String name = normalizeLocationName(pp.getAttributeValue(null, "objectName"));

		final String mapName = XmlPullUtil.optAttr(pp, "mapName", null);
		final float x = XmlPullUtil.optFloatAttr(pp, "x", 0);
		final float y = XmlPullUtil.optFloatAttr(pp, "y", 0);

		final int lat;
		final int lon;
		if (mapName == null || (x == 0 && y == 0))
		{
			lat = 0;
			lon = 0;
		}
		else if ("WGS84".equals(mapName))
		{
			lat = Math.round(y);
			lon = Math.round(x);
		}
		else
		{
			throw new IllegalStateException("unknown mapName=" + mapName + " x=" + x + " y=" + y);
		}

		LocationType type;
		int id;
		if ("stop".equals(anyType))
		{
			type = LocationType.STATION;
			id = Integer.parseInt(idStr);
		}
		else if ("poi".equals(anyType) || "poiHierarchy".equals(anyType))
		{
			type = LocationType.POI;
			id = Integer.parseInt(idStr);
		}
		else if ("loc".equals(anyType))
		{
			type = LocationType.ANY;
			id = 0;
		}
		else if ("postcode".equals(anyType) || "street".equals(anyType) || "crossing".equals(anyType) || "address".equals(anyType)
				|| "singlehouse".equals(anyType) || "buildingname".equals(anyType))
		{
			type = LocationType.ADDRESS;
			id = 0;
		}
		else if (stopIdStr != null)
		{
			type = LocationType.STATION;
			id = Integer.parseInt(stopIdStr);
		}
		else if (poiIdStr != null)
		{
			type = LocationType.POI;
			id = Integer.parseInt(poiIdStr);
		}
		else if (stopIdStr == null && idStr == null && (lat != 0 || lon != 0))
		{
			type = LocationType.ADDRESS;
			id = 0;
		}
		else if (streetIdStr != null)
		{
			type = LocationType.ADDRESS;
			id = Integer.parseInt(streetIdStr);
		}
		else
		{
			throw new IllegalArgumentException("unknown type: " + anyType + " " + idStr + " " + stopIdStr);
		}

		XmlPullUtil.enter(pp, "odvNameElem");
		final String longName = normalizeLocationName(pp.getText());
		XmlPullUtil.exit(pp, "odvNameElem");

		return new Location(type, id, lat, lon, place != null ? place : defaultPlace, name != null ? name : longName);
	}

	private Location processItdOdvAssignedStop(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		final int id = Integer.parseInt(pp.getAttributeValue(null, "stopID"));

		final String mapName = XmlPullUtil.optAttr(pp, "mapName", null);
		final float x = XmlPullUtil.optFloatAttr(pp, "x", 0);
		final float y = XmlPullUtil.optFloatAttr(pp, "y", 0);

		final int lat;
		final int lon;
		if (mapName == null || (x == 0 && y == 0))
		{
			lat = 0;
			lon = 0;
		}
		else if ("WGS84".equals(mapName))
		{
			lat = Math.round(y);
			lon = Math.round(x);
		}
		else
		{
			throw new IllegalStateException("unknown mapName=" + mapName + " x=" + x + " y=" + y);
		}

		final String place = normalizeLocationName(XmlPullUtil.attr(pp, "place"));

		XmlPullUtil.enter(pp, "itdOdvAssignedStop");
		final String name = normalizeLocationName(pp.getText());
		XmlPullUtil.exit(pp, "itdOdvAssignedStop");

		return new Location(LocationType.STATION, id, lat, lon, place, name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
			return xmlCoordRequest(location.lat, location.lon, maxDistance, maxStations);

		if (location.type != LocationType.STATION)
			throw new IllegalArgumentException("cannot handle: " + location.type);

		if (!location.hasId())
			throw new IllegalArgumentException("at least one of stationId or lat/lon must be given");

		return nearbyStationsRequest(location.id, maxStations);
	}

	private NearbyStationsResult nearbyStationsRequest(final int stationId, final int maxStations) throws IOException
	{
		final StringBuilder parameters = new StringBuilder();
		appendCommonRequestParams(parameters, "XML");
		parameters.append("&type_dm=stop&name_dm=").append(stationId);
		parameters.append("&itOptionsActive=1");
		parameters.append("&ptOptionsActive=1");
		parameters.append("&useProxFootSearch=1");
		parameters.append("&mergeDep=1");
		parameters.append("&useAllStops=1");
		parameters.append("&mode=direct");

		final StringBuilder uri = new StringBuilder(departureMonitorEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, "NSC_", 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			final ResultHeader header = enterItdRequest(pp);

			if (!XmlPullUtil.jumpToStartTag(pp, null, "itdOdv") || !"dm".equals(pp.getAttributeValue(null, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"dm\" />");
			XmlPullUtil.enter(pp, "itdOdv");

			final String place = processItdOdvPlace(pp);

			XmlPullUtil.require(pp, "itdOdvName");
			final String nameState = pp.getAttributeValue(null, "state");
			XmlPullUtil.enter(pp, "itdOdvName");
			if ("identified".equals(nameState))
			{
				final Location ownLocation = processOdvNameElem(pp, place);
				final Location ownStation = ownLocation.type == LocationType.STATION ? ownLocation : null;

				final List<Location> stations = new ArrayList<Location>();

				if (XmlPullUtil.jumpToStartTag(pp, null, "itdOdvAssignedStops"))
				{
					XmlPullUtil.enter(pp, "itdOdvAssignedStops");
					while (XmlPullUtil.test(pp, "itdOdvAssignedStop"))
					{
						final Location newStation = processItdOdvAssignedStop(pp);

						if (!stations.contains(newStation))
							stations.add(newStation);
					}
					XmlPullUtil.exit(pp, "itdOdvAssignedStops");
				}

				if (ownStation != null && !stations.contains(ownStation))
					stations.add(ownStation);

				if (maxStations == 0 || maxStations >= stations.size())
					return new NearbyStationsResult(header, stations);
				else
					return new NearbyStationsResult(header, stations.subList(0, maxStations));
			}
			else if ("list".equals(nameState))
			{
				final List<Location> stations = new ArrayList<Location>();

				if (XmlPullUtil.test(pp, "itdMessage"))
					XmlPullUtil.next(pp);
				while (XmlPullUtil.test(pp, "odvNameElem"))
				{
					final Location newLocation = processOdvNameElem(pp, place);
					if (newLocation.type == LocationType.STATION && !stations.contains(newLocation))
						stations.add(newLocation);
				}

				return new NearbyStationsResult(header, stations);
			}
			else if ("notidentified".equals(nameState))
			{
				return new NearbyStationsResult(header, NearbyStationsResult.Status.INVALID_STATION);
			}
			else
			{
				throw new RuntimeException("unknown nameState '" + nameState + "' on " + uri);
			}
			// XmlPullUtil.exit(pp, "itdOdvName");
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_LINE_RE = Pattern.compile("RE ?\\d+");
	private static final Pattern P_LINE_RB = Pattern.compile("RB ?\\d+");
	private static final Pattern P_LINE_R = Pattern.compile("R ?\\d+");
	private static final Pattern P_LINE_S = Pattern.compile("^(?:%)?(S\\d+)");
	private static final Pattern P_LINE_NUMBER = Pattern.compile("\\d+");

	protected String parseLine(final String mot, String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if (mot == null)
		{
			if (trainName != null)
			{
				final String str = name != null ? name : "";
				if (trainName.equals("S-Bahn"))
					return 'S' + str;
				if (trainName.equals("U-Bahn"))
					return 'U' + str;
				if (trainName.equals("Straßenbahn"))
					return 'T' + str;
				if (trainName.equals("Badner Bahn"))
					return 'T' + str;
				if (trainName.equals("Stadtbus"))
					return 'B' + str;
				if (trainName.equals("Citybus"))
					return 'B' + str;
				if (trainName.equals("Regionalbus"))
					return 'B' + str;
				if (trainName.equals("ÖBB-Postbus"))
					return 'B' + str;
				if (trainName.equals("Autobus"))
					return 'B' + str;
				if (trainName.equals("Discobus"))
					return 'B' + str;
				if (trainName.equals("Nachtbus"))
					return 'B' + str;
				if (trainName.equals("Anrufsammeltaxi"))
					return 'B' + str;
				if (trainName.equals("Ersatzverkehr"))
					return 'B' + str;
				if (trainName.equals("Vienna Airport Lines"))
					return 'B' + str;
			}

			throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name + "' long='" + longName
					+ "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='" + trainName + "'");
		}
		else if ("0".equals(mot))
		{
			if ("EC".equals(trainType) || "EuroCity".equals(trainName) || "Eurocity".equals(trainName))
				return "IEC" + trainNum;
			if ("EN".equals(trainType) || "EuroNight".equals(trainName))
				return "IEN" + trainNum;
			if ("IC".equals(trainType) || "InterCity".equals(trainName))
				return "IIC" + trainNum;
			if ("ICE".equals(trainType) || "Intercity-Express".equals(trainName))
				return "IICE" + trainNum;
			if ("X".equals(trainType) || "InterConnex".equals(trainName))
				return "IX" + trainNum;
			if ("CNL".equals(trainType) || "CityNightLine".equals(trainName)) // City Night Line
				return "ICNL" + trainNum;
			if ("THA".equals(trainType) || "Thalys".equals(trainName))
				return "ITHA" + trainNum;
			if ("TGV".equals(trainType) || "TGV".equals(trainName))
				return "ITGV" + trainNum;
			if ("RJ".equals(trainType) || "railjet".equals(trainName)) // railjet
				return "IRJ" + trainNum;
			if ("OIC".equals(trainType) || "ÖBB InterCity".equals(trainName))
				return 'I' + symbol;
			if ("HKX".equals(trainType) || "Hamburg-Köln-Express".equals(trainName))
				return "IHKX" + trainNum;
			if ("INT".equals(trainType)) // SVV, VAGFR
				return "IINT" + trainNum;
			if ("SC".equals(trainType) || "SC Pendolino".equals(trainName)) // SuperCity, Tschechien
				return "ISC" + trainNum;
			if ("ECB".equals(trainType)) // EC, Verona-München
				return "IECB" + trainNum;
			if ("ES".equals(trainType)) // Eurostar Italia
				return "IES" + trainNum;
			if ("EST".equals(trainType) || "EUROSTAR".equals(trainName))
				return "IEST" + trainNum;

			if ("Zug".equals(trainName))
				return 'R' + symbol;
			if ("Zuglinie".equals(trainName))
				return 'R' + symbol;
			if ("IR".equals(trainType) || "Interregio".equals(trainName) || "InterRegio".equals(trainName))
				return "RIR" + trainNum;
			if ("IRE".equals(trainType) || "Interregio-Express".equals(trainName))
				return "RIRE" + trainNum;
			if ("RE".equals(trainType) || "Regional-Express".equals(trainName))
				return "RRE" + trainNum;
			if (trainType == null && trainNum != null && P_LINE_RE.matcher(trainNum).matches())
				return 'R' + trainNum;
			if ("Regionalexpress".equals(trainName))
				return 'R' + symbol;
			if ("R-Bahn".equals(trainName))
				return 'R' + symbol;
			if ("RB-Bahn".equals(trainName))
				return 'R' + symbol;
			if ("RE-Bahn".equals(trainName))
				return 'R' + symbol;
			if ("REX".equals(trainType)) // RegionalExpress, Österreich
				return "RREX" + trainNum;
			if ("RB".equals(trainType) || "Regionalbahn".equals(trainName))
				return "RRB" + trainNum;
			if (trainType == null && trainNum != null && P_LINE_RB.matcher(trainNum).matches())
				return 'R' + trainNum;
			if ("Abellio-Zug".equals(trainName))
				return "R" + symbol;
			if ("Westfalenbahn".equals(trainName))
				return 'R' + symbol;
			if ("R".equals(trainType) || "Regionalzug".equals(trainName))
				return "RR" + trainNum;
			if (trainType == null && trainNum != null && P_LINE_R.matcher(trainNum).matches())
				return 'R' + trainNum;
			if ("D".equals(trainType) || "Schnellzug".equals(trainName))
				return "RD" + trainNum;
			if ("E".equals(trainType) || "Eilzug".equals(trainName))
				return "RE" + trainNum;
			if ("WFB".equals(trainType) || "WestfalenBahn".equals(trainName))
				return "RWFB" + trainNum;
			if ("NWB".equals(trainType) || "NordWestBahn".equals(trainName))
				return "RNWB" + trainNum;
			if ("WB".equals(trainType) || "WESTbahn".equals(trainName))
				return "RWB" + trainNum;
			if ("WES".equals(trainType) || "Westbahn".equals(trainName))
				return "RWES" + trainNum;
			if ("ERB".equals(trainType) || "eurobahn".equals(trainName))
				return "RERB" + trainNum;
			if ("CAN".equals(trainType) || "cantus Verkehrsgesellschaft".equals(trainName))
				return "RCAN" + trainNum;
			if ("HEX".equals(trainType) || "Veolia Verkehr Sachsen-Anhalt".equals(trainName))
				return "RHEX" + trainNum;
			if ("EB".equals(trainType) || "Erfurter Bahn".equals(trainName))
				return "REB" + trainNum;
			if ("EBx".equals(trainType) || "Erfurter Bahn Express".equals(trainName))
				return "REBx" + trainNum;
			if ("MRB".equals(trainType) || "Mitteldeutsche Regiobahn".equals(trainName))
				return "RMRB" + trainNum;
			if ("ABR".equals(trainType) || "ABELLIO Rail NRW GmbH".equals(trainName))
				return "RABR" + trainNum;
			if ("NEB".equals(trainType) || "NEB Niederbarnimer Eisenbahn".equals(trainName))
				return "RNEB" + trainNum;
			if ("OE".equals(trainType) || "Ostdeutsche Eisenbahn GmbH".equals(trainName))
				return "ROE" + trainNum;
			if ("ODE".equals(trainType))
				return 'R' + symbol;
			if ("OLA".equals(trainType) || "Ostseeland Verkehr GmbH".equals(trainName))
				return "ROLA" + trainNum;
			if ("UBB".equals(trainType) || "Usedomer Bäderbahn".equals(trainName))
				return "RUBB" + trainNum;
			if ("EVB".equals(trainType) || "ELBE-WESER GmbH".equals(trainName))
				return "REVB" + trainNum;
			if ("RTB".equals(trainType) || "Rurtalbahn GmbH".equals(trainName))
				return "RRTB" + trainNum;
			if ("STB".equals(trainType) || "Süd-Thüringen-Bahn".equals(trainName))
				return "RSTB" + trainNum;
			if ("HTB".equals(trainType) || "Hellertalbahn".equals(trainName))
				return "RHTB" + trainNum;
			if ("VBG".equals(trainType) || "Vogtlandbahn".equals(trainName))
				return "RVBG" + trainNum;
			if ("CB".equals(trainType) || "City-Bahn Chemnitz".equals(trainName))
				return "RCB" + trainNum;
			if ("VEC".equals(trainType) || "vectus Verkehrsgesellschaft".equals(trainName))
				return "RVEC" + trainNum;
			if ("HzL".equals(trainType) || "Hohenzollerische Landesbahn AG".equals(trainName))
				return "RHzL" + trainNum;
			if ("SBB".equals(trainType) || "SBB GmbH".equals(trainName))
				return "RSBB" + trainNum;
			if ("MBB".equals(trainType) || "Mecklenburgische Bäderbahn Molli".equals(trainName))
				return "RMBB" + trainNum;
			if ("OS".equals(trainType)) // Osobní vlak
				return "ROS" + trainNum;
			if ("SP".equals(trainType) || "Sp".equals(trainType)) // Spěšný vlak
				return "RSP" + trainNum;
			if ("Dab".equals(trainType) || "Daadetalbahn".equals(trainName))
				return "RDab" + trainNum;
			if ("FEG".equals(trainType) || "Freiberger Eisenbahngesellschaft".equals(trainName))
				return "RFEG" + trainNum;
			if ("ARR".equals(trainType) || "ARRIVA".equals(trainName))
				return "RARR" + trainNum;
			if ("HSB".equals(trainType) || "Harzer Schmalspurbahn".equals(trainName))
				return "RHSB" + trainNum;
			if ("ALX".equals(trainType) || "alex - Länderbahn und Vogtlandbahn GmbH".equals(trainName))
				return "RALX" + trainNum;
			if ("EX".equals(trainType) || "Fatra".equals(trainName))
				return "REX" + trainNum;
			if ("ME".equals(trainType) || "metronom".equals(trainName))
				return "RME" + trainNum;
			if ("MEr".equals(trainType))
				return "RMEr" + trainNum;
			if ("AKN".equals(trainType) || "AKN Eisenbahn AG".equals(trainName))
				return "RAKN" + trainNum;
			if ("SOE".equals(trainType) || "Sächsisch-Oberlausitzer Eisenbahngesellschaft".equals(trainName))
				return "RSOE" + trainNum;
			if ("VIA".equals(trainType) || "VIAS GmbH".equals(trainName))
				return "RVIA" + trainNum;
			if ("BRB".equals(trainType) || "Bayerische Regiobahn".equals(trainName))
				return "RBRB" + trainNum;
			if ("BLB".equals(trainType) || "Berchtesgadener Land Bahn".equals(trainName))
				return "RBLB" + trainNum;
			if ("HLB".equals(trainType) || "Hessische Landesbahn".equals(trainName))
				return "RHLB" + trainNum;
			if ("NOB".equals(trainType) || "NordOstseeBahn".equals(trainName))
				return "RNOB" + trainNum;
			if ("NBE".equals(trainType) || "Nordbahn Eisenbahngesellschaft".equals(trainName))
				return "RNBE" + trainNum;
			if ("VEN".equals(trainType) || "Rhenus Veniro".equals(trainName))
				return "RVEN" + trainType;
			if ("DPN".equals(trainType) || "Nahreisezug".equals(trainName))
				return "RDPN" + trainNum;
			if ("RBG".equals(trainType) || "Regental Bahnbetriebs GmbH".equals(trainName))
				return "RRBG" + trainNum;
			if ("BOB".equals(trainType) || "Bodensee-Oberschwaben-Bahn".equals(trainName))
				return "RBOB" + trainNum;
			if ("VE".equals(trainType) || "Vetter".equals(trainName))
				return "RVE" + trainNum;
			if ("SDG".equals(trainType) || "SDG Sächsische Dampfeisenbahngesellschaft mbH".equals(trainName))
				return "RSDG" + trainNum;
			if ("PRE".equals(trainType) || "Pressnitztalbahn".equals(trainName))
				return "RPRE" + trainNum;
			if ("VEB".equals(trainType) || "Vulkan-Eifel-Bahn".equals(trainName))
				return "RVEB" + trainNum;
			if ("neg".equals(trainType) || "Norddeutsche Eisenbahn Gesellschaft".equals(trainName))
				return "Rneg" + trainNum;
			if ("AVG".equals(trainType) || "Felsenland-Express".equals(trainName))
				return "RAVG" + trainNum;
			if ("P".equals(trainType) || "BayernBahn Betriebs-GmbH".equals(trainName) || "Brohltalbahn".equals(trainName)
					|| "Kasbachtalbahn".equals(trainName))
				return "RP" + trainNum;
			if ("SBS".equals(trainType) || "Städtebahn Sachsen".equals(trainName))
				return "RSBS" + trainNum;
			if ("SB-".equals(trainType)) // Städtebahn Sachsen
				return "RSB" + trainNum;
			if ("ag".equals(trainType)) // agilis
				return "Rag" + trainNum;
			if ("agi".equals(trainType) || "agilis".equals(trainName))
				return "Ragi" + trainNum;
			if ("as".equals(trainType) || "agilis-Schnellzug".equals(trainName))
				return "Ras" + trainNum;
			if ("TLX".equals(trainType) || "TRILEX".equals(trainName)) // Trilex (Vogtlandbahn)
				return "RTLX" + trainNum;
			if ("MSB".equals(trainType) || "Mainschleifenbahn".equals(trainName))
				return "RMSB" + trainNum;
			if ("BE".equals(trainType) || "Bentheimer Eisenbahn".equals(trainName))
				return "RBE" + trainNum;
			if ("erx".equals(trainType) || "erixx - Der Heidesprinter".equals(trainName))
				return "Rerx" + trainNum;
			if ("SWEG-Zug".equals(trainName)) // Südwestdeutschen Verkehrs-Aktiengesellschaft
				return "RSWEG" + trainNum;
			if ("ÖBB".equals(trainType) || "ÖBB".equals(trainName))
				return "RÖBB" + trainNum;
			if ("CAT".equals(trainType)) // City Airport Train Wien
				return "RCAT" + trainNum;
			if ("DZ".equals(trainType) || "Dampfzug".equals(trainName))
				return "RDZ" + trainNum;
			if ("CD".equals(trainType)) // Tschechien
				return "RCD" + trainNum;
			if ("VR".equals(trainType)) // Polen
				return 'R' + symbol;
			if ("PR".equals(trainType)) // Polen
				return 'R' + symbol;
			if ("KD".equals(trainType)) // Koleje Dolnośląskie (Niederschlesische Eisenbahn)
				return 'R' + symbol;
			if ("OO".equals(trainType) || "Ordinary passenger (o.pas.)".equals(trainName)) // GB
				return "ROO" + trainNum;
			if ("XX".equals(trainType) || "Express passenger    (ex.pas.)".equals(trainName)) // GB
				return "RXX" + trainNum;
			if ("XZ".equals(trainType) || "Express passenger sleeper".equals(trainName)) // GB
				return "RXZ" + trainNum;
			if ("ATB".equals(trainType)) // Autoschleuse Tauernbahn
				return "RATB" + trainNum;
			if ("ATZ".equals(trainType)) // Autozug
				return "RATZ" + trainNum;
			if ("DWE".equals(trainType) || "Dessau-Wörlitzer Eisenbahn".equals(trainName))
				return "RDWE" + trainNum;
			if ("KTB".equals(trainType) || "Kandertalbahn".equals(trainName))
				return "RKTB" + trainNum;
			if ("CBC".equals(trainType) || "CBC".equals(trainName)) // City-Bahn Chemnitz
				return "RCBC" + trainNum;
			if ("Bernina Express".equals(trainName))
				return 'R' + trainNum;
			if ("STR".equals(trainType)) // Harzquerbahn, Nordhausen
				return "RSTR" + trainNum;
			if ("EXT".equals(trainType) || "Extrazug".equals(trainName))
				return "REXT" + trainNum;
			if ("Heritage Railway".equals(trainName)) // GB
				return 'R' + symbol;
			if ("WTB".equals(trainType) || "Wutachtalbahn".equals(trainName))
				return "RWTB" + trainNum;
			if ("DB".equals(trainType) || "DB Regio".equals(trainName))
				return "RDB" + trainNum;

			if ("BSB-Zug".equals(trainName)) // Breisgau-S-Bahn
				return 'S' + trainNum;
			if ("RSB".equals(trainType)) // Regionalschnellbahn, Wien
				return "SRSB" + trainNum;
			if ("RER".equals(trainName) && symbol.length() == 1) // Réseau Express Régional, Frankreich
				return 'S' + symbol;
			if ("S".equals(trainType))
				return "SS" + trainNum;

			if ("RT".equals(trainType) || "RegioTram".equals(trainName))
				return "TRT" + trainNum;

			if ("SEV".equals(trainType) || "SEV".equals(trainNum) || "SEV".equals(symbol) || "Ersatzverkehr".equals(trainName))
				return "BSEV";
			if ("Bus replacement".equals(trainName)) // GB
				return "BBR";
			if ("BR".equals(trainType) && trainName.startsWith("Bus")) // GB
				return "BBR" + trainNum;

			if ("GB".equals(trainType)) // Gondelbahn
				return "CGB" + trainNum;

			if (trainType == null && trainName == null && P_LINE_NUMBER.matcher(symbol).matches())
				return '?' + symbol;
			if (trainType == null && trainName == null && symbol.equals(name) && symbol.equals(longName))
				return '?' + symbol;
			if ("N".equals(trainType) && trainName == null && symbol.length() == 0)
				return "?N" + trainNum;
			if ("Train".equals(trainName))
				return "?";

			throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name + "' long='" + longName
					+ "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='" + trainName + "'");
		}
		else if ("1".equals(mot))
		{
			final Matcher m = P_LINE_S.matcher(name);
			if (m.find())
				return 'S' + m.group(1);
			else
				return 'S' + name;
		}
		else if ("2".equals(mot))
		{
			return 'U' + name;
		}
		else if ("3".equals(mot) || "4".equals(mot))
		{
			return 'T' + name;
		}
		else if ("5".equals(mot) || "6".equals(mot) || "7".equals(mot) || "10".equals(mot))
		{
			if (name.equals("Schienenersatzverkehr"))
				return "BSEV";
			else
				return 'B' + name;
		}
		else if ("8".equals(mot))
		{
			return 'C' + name;
		}
		else if ("9".equals(mot))
		{
			return 'F' + name;
		}
		else if ("11".equals(mot))
		{
			return '?' + ParserUtils.firstNotEmpty(symbol, name);
		}

		throw new IllegalStateException("cannot normalize mot='" + mot + "' symbol='" + symbol + "' name='" + name + "' long='" + longName
				+ "' trainType='" + trainType + "' trainNum='" + trainNum + "' trainName='" + trainName + "'");
	}

	protected StringBuilder queryDeparturesParameters(final int stationId, final int maxDepartures, final boolean equivs)
	{
		final StringBuilder parameters = new StringBuilder();
		appendCommonRequestParams(parameters, "XML");
		parameters.append("&type_dm=stop");
		parameters.append("&name_dm=").append(stationId);
		parameters.append("&useRealtime=1");
		parameters.append("&mode=direct");
		parameters.append("&ptOptionsActive=1");
		parameters.append("&deleteAssignedStops_dm=").append(equivs ? '0' : '1');
		parameters.append("&mergeDep=1"); // merge departures
		if (maxDepartures > 0)
			parameters.append("&limit=").append(maxDepartures);

		return parameters;
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder parameters = queryDeparturesParameters(stationId, maxDepartures, equivs);

		final StringBuilder uri = new StringBuilder(departureMonitorEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			final ResultHeader header = enterItdRequest(pp);

			XmlPullUtil.enter(pp, "itdDepartureMonitorRequest");

			if (!XmlPullUtil.test(pp, "itdOdv") || !"dm".equals(XmlPullUtil.attr(pp, "usage")))
				throw new IllegalStateException("cannot find <itdOdv usage=\"dm\" />");
			XmlPullUtil.enter(pp, "itdOdv");

			final String place = processItdOdvPlace(pp);

			XmlPullUtil.require(pp, "itdOdvName");
			final String nameState = pp.getAttributeValue(null, "state");
			XmlPullUtil.enter(pp, "itdOdvName");
			if ("identified".equals(nameState))
			{
				final QueryDeparturesResult result = new QueryDeparturesResult(header);

				final Location location = processOdvNameElem(pp, place);
				result.stationDepartures.add(new StationDepartures(location, new LinkedList<Departure>(), new LinkedList<LineDestination>()));

				XmlPullUtil.exit(pp, "itdOdvName");

				if (XmlPullUtil.test(pp, "itdOdvAssignedStops"))
				{
					XmlPullUtil.enter(pp, "itdOdvAssignedStops");
					while (XmlPullUtil.test(pp, "itdOdvAssignedStop"))
					{
						final Location assignedLocation = processItdOdvAssignedStop(pp);
						if (findStationDepartures(result.stationDepartures, assignedLocation.id) == null)
							result.stationDepartures.add(new StationDepartures(assignedLocation, new LinkedList<Departure>(),
									new LinkedList<LineDestination>()));
					}
					XmlPullUtil.exit(pp, "itdOdvAssignedStops");
				}

				XmlPullUtil.exit(pp, "itdOdv");

				if (XmlPullUtil.test(pp, "itdDateTime"))
					XmlPullUtil.next(pp);

				if (XmlPullUtil.test(pp, "itdDMDateTime"))
					XmlPullUtil.next(pp);

				if (XmlPullUtil.test(pp, "itdDateRange"))
					XmlPullUtil.next(pp);

				if (XmlPullUtil.test(pp, "itdTripOptions"))
					XmlPullUtil.next(pp);

				if (XmlPullUtil.test(pp, "itdMessage"))
					XmlPullUtil.next(pp);

				final Calendar plannedDepartureTime = new GregorianCalendar(timeZone());
				final Calendar predictedDepartureTime = new GregorianCalendar(timeZone());

				XmlPullUtil.require(pp, "itdServingLines");
				if (!pp.isEmptyElementTag())
				{
					XmlPullUtil.enter(pp, "itdServingLines");
					while (XmlPullUtil.test(pp, "itdServingLine"))
					{
						final String assignedStopIdStr = pp.getAttributeValue(null, "assignedStopID");
						final int assignedStopId = assignedStopIdStr != null ? Integer.parseInt(assignedStopIdStr) : 0;
						final String destinationName = normalizeLocationName(pp.getAttributeValue(null, "direction"));
						final String destinationIdStr = pp.getAttributeValue(null, "destID");
						final int destinationId = (destinationIdStr != null && destinationIdStr.length() > 0) ? Integer.parseInt(destinationIdStr)
								: 0;
						final Location destination = new Location(destinationId > 0 ? LocationType.STATION : LocationType.ANY,
								destinationId > 0 ? destinationId : 0, null, destinationName);
						final LineDestination line = new LineDestination(processItdServingLine(pp), destination);

						StationDepartures assignedStationDepartures;
						if (assignedStopId == 0)
							assignedStationDepartures = result.stationDepartures.get(0);
						else
							assignedStationDepartures = findStationDepartures(result.stationDepartures, assignedStopId);

						if (assignedStationDepartures == null)
							assignedStationDepartures = new StationDepartures(new Location(LocationType.STATION, assignedStopId),
									new LinkedList<Departure>(), new LinkedList<LineDestination>());

						if (!assignedStationDepartures.lines.contains(line))
							assignedStationDepartures.lines.add(line);
					}
					XmlPullUtil.exit(pp, "itdServingLines");
				}
				else
				{
					XmlPullUtil.next(pp);
				}

				XmlPullUtil.require(pp, "itdDepartureList");
				if (!pp.isEmptyElementTag())
				{
					XmlPullUtil.enter(pp, "itdDepartureList");
					while (XmlPullUtil.test(pp, "itdDeparture"))
					{
						final int assignedStopId = XmlPullUtil.intAttr(pp, "stopID");

						StationDepartures assignedStationDepartures = findStationDepartures(result.stationDepartures, assignedStopId);
						if (assignedStationDepartures == null)
						{
							final String mapName = XmlPullUtil.optAttr(pp, "mapName", null);
							final float x = XmlPullUtil.optFloatAttr(pp, "x", 0);
							final float y = XmlPullUtil.optFloatAttr(pp, "y", 0);

							final int lat;
							final int lon;
							if (mapName == null || (x == 0 && y == 0))
							{
								lat = 0;
								lon = 0;
							}
							else if ("WGS84".equals(mapName))
							{
								lat = Math.round(y);
								lon = Math.round(x);
							}
							else
							{
								throw new IllegalStateException("unknown mapName=" + mapName + " x=" + x + " y=" + y);
							}

							// final String name = normalizeLocationName(XmlPullUtil.attr(pp, "nameWO"));

							assignedStationDepartures = new StationDepartures(new Location(LocationType.STATION, assignedStopId, lat, lon),
									new LinkedList<Departure>(), new LinkedList<LineDestination>());
						}

						final String position;
						if (!suppressPositions)
							position = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null, "platformName"));
						else
							position = null;

						XmlPullUtil.enter(pp, "itdDeparture");

						XmlPullUtil.require(pp, "itdDateTime");
						plannedDepartureTime.clear();
						processItdDateTime(pp, plannedDepartureTime);

						predictedDepartureTime.clear();
						if (XmlPullUtil.test(pp, "itdRTDateTime"))
							processItdDateTime(pp, predictedDepartureTime);

						if (XmlPullUtil.test(pp, "itdFrequencyInfo"))
							XmlPullUtil.next(pp);

						XmlPullUtil.require(pp, "itdServingLine");
						final boolean isRealtime = pp.getAttributeValue(null, "realtime").equals("1");
						final String destinationName = normalizeLocationName(pp.getAttributeValue(null, "direction"));
						final String destinationIdStr = pp.getAttributeValue(null, "destID");
						final int destinationId = destinationIdStr != null ? Integer.parseInt(destinationIdStr) : 0;
						final Location destination = new Location(destinationId > 0 ? LocationType.STATION : LocationType.ANY,
								destinationId > 0 ? destinationId : 0, null, destinationName);
						final Line line = processItdServingLine(pp);

						if (isRealtime && !predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY))
							predictedDepartureTime.setTimeInMillis(plannedDepartureTime.getTimeInMillis());

						XmlPullUtil.exit(pp, "itdDeparture");

						final Departure departure = new Departure(plannedDepartureTime.getTime(),
								predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY) ? predictedDepartureTime.getTime() : null, line, position,
								destination, null, null);
						assignedStationDepartures.departures.add(departure);
					}

					XmlPullUtil.exit(pp, "itdDepartureList");
				}
				else
				{
					XmlPullUtil.next(pp);
				}

				return result;
			}
			else if ("notidentified".equals(nameState) || "list".equals(nameState))
			{
				return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
			}
			else
			{
				throw new RuntimeException("unknown nameState '" + nameState + "' on " + uri);
			}
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	protected QueryDeparturesResult queryDeparturesMobile(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder parameters = queryDeparturesParameters(stationId, maxDepartures, equivs);

		final StringBuilder uri = new StringBuilder(departureMonitorEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpReferer, null, 3);

			final XmlPullParser pp = parserFactory.newPullParser();
			pp.setInput(is, null);
			final ResultHeader header = enterEfa(pp);
			final QueryDeparturesResult result = new QueryDeparturesResult(header);

			XmlPullUtil.require(pp, "dps");
			if (!pp.isEmptyElementTag())
			{
				XmlPullUtil.enter(pp, "dps");

				final Calendar plannedDepartureTime = new GregorianCalendar(timeZone());
				final Calendar predictedDepartureTime = new GregorianCalendar(timeZone());

				while (XmlPullUtil.test(pp, "dp"))
				{
					XmlPullUtil.enter(pp, "dp");

					// misc
					/* final String stationName = */normalizeLocationName(requireValueTag(pp, "n"));
					final boolean isRealtime = requireValueTag(pp, "realtime").equals("1");

					XmlPullUtil.optSkip(pp, "dt");

					// time
					parseMobileSt(pp, plannedDepartureTime, predictedDepartureTime);

					final LineDestination lineDestination = parseMobileM(pp, true);

					XmlPullUtil.enter(pp, "r");
					final int assignedId = Integer.parseInt(requireValueTag(pp, "id"));
					requireValueTag(pp, "a");
					final String position = optValueTag(pp, "pl");
					XmlPullUtil.exit(pp, "r");

					/* final Point positionCoordinate = */coordStrToPoint(optValueTag(pp, "c"));

					// TODO messages

					StationDepartures stationDepartures = findStationDepartures(result.stationDepartures, assignedId);
					if (stationDepartures == null)
					{
						stationDepartures = new StationDepartures(new Location(LocationType.STATION, assignedId), new ArrayList<Departure>(
								maxDepartures), null);
						result.stationDepartures.add(stationDepartures);
					}

					stationDepartures.departures.add(new Departure(plannedDepartureTime.getTime(),
							predictedDepartureTime.isSet(Calendar.HOUR_OF_DAY) ? predictedDepartureTime.getTime() : null, lineDestination.line,
							position, lineDestination.destination, null, null));

					XmlPullUtil.exit(pp, "dp");
				}

				XmlPullUtil.exit(pp, "dps");

				return result;
			}
			else
			{
				return new QueryDeparturesResult(header, QueryDeparturesResult.Status.INVALID_STATION);
			}
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_MOBILE_M_SYMBOL = Pattern.compile("([^\\s]*)\\s+([^\\s]*)");

	private LineDestination parseMobileM(final XmlPullParser pp, final boolean tyOrCo) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "m");

		final String n = optValueTag(pp, "n");
		final String productNu = requireValueTag(pp, "nu");
		final String ty = requireValueTag(pp, "ty");

		final Line line;
		final Location destination;
		if ("100".equals(ty) || "99".equals(ty))
		{
			destination = null;
			line = Line.FOOTWAY;
		}
		else if ("98".equals(ty))
		{
			destination = null;
			line = Line.SECURE_CONNECTION;
		}
		else if ("97".equals(ty))
		{
			destination = null;
			line = Line.DO_NOT_CHANGE;
		}
		else
		{
			final String co = requireValueTag(pp, "co");
			final String productType = tyOrCo ? ty : co;
			final String destinationName = normalizeLocationName(requireValueTag(pp, "des"));
			destination = new Location(LocationType.ANY, 0, null, destinationName);
			optValueTag(pp, "dy");
			final String de = optValueTag(pp, "de");
			final String productName = n != null ? n : de;
			final String lineId = parseMobileDv(pp);

			final String symbol = productNu.endsWith(" " + productName) ? productNu.substring(0, productNu.length() - productName.length() - 1)
					: productNu;
			final String trainType;
			final String trainNum;
			final Matcher mSymbol = P_MOBILE_M_SYMBOL.matcher(symbol);
			if (mSymbol.matches())
			{
				trainType = mSymbol.group(1);
				trainNum = mSymbol.group(2);
			}
			else
			{
				trainType = null;
				trainNum = null;
			}

			final String lineLabel = parseLine(productType, symbol, symbol, null, trainType, trainNum, productName);
			line = new Line(lineId, lineLabel, lineStyle(lineLabel));
		}

		XmlPullUtil.exit(pp, "m");

		return new LineDestination(line, destination);
	}

	private String parseMobileDv(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "dv");
		optValueTag(pp, "branch");
		final String lineIdLi = requireValueTag(pp, "li");
		final String lineIdSu = requireValueTag(pp, "su");
		final String lineIdPr = requireValueTag(pp, "pr");
		final String lineIdDct = requireValueTag(pp, "dct");
		final String lineIdNe = requireValueTag(pp, "ne");
		XmlPullUtil.exit(pp, "dv");

		return lineIdNe + ":" + lineIdLi + ":" + lineIdSu + ":" + lineIdDct + ":" + lineIdPr;
	}

	private void parseMobileSt(final XmlPullParser pp, final Calendar plannedDepartureTime, final Calendar predictedDepartureTime)
			throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "st");

		plannedDepartureTime.clear();
		ParserUtils.parseIsoDate(plannedDepartureTime, requireValueTag(pp, "da"));
		ParserUtils.parseIsoTime(plannedDepartureTime, requireValueTag(pp, "t"));

		predictedDepartureTime.clear();
		if (XmlPullUtil.test(pp, "rda"))
		{
			ParserUtils.parseIsoDate(predictedDepartureTime, requireValueTag(pp, "rda"));
			ParserUtils.parseIsoTime(predictedDepartureTime, requireValueTag(pp, "rt"));
		}

		XmlPullUtil.exit(pp, "st");
	}

	private StationDepartures findStationDepartures(final List<StationDepartures> stationDepartures, final int id)
	{
		for (final StationDepartures stationDeparture : stationDepartures)
			if (stationDeparture.location.id == id)
				return stationDeparture;

		return null;
	}

	private Location processItdPointAttributes(final XmlPullParser pp)
	{
		final int id = Integer.parseInt(pp.getAttributeValue(null, "stopID"));

		final String place = normalizeLocationName(pp.getAttributeValue(null, "locality"));
		String name = normalizeLocationName(pp.getAttributeValue(null, "nameWO"));
		if (name == null)
			name = normalizeLocationName(pp.getAttributeValue(null, "name"));

		final String mapName = XmlPullUtil.optAttr(pp, "mapName", null);
		final float x = XmlPullUtil.optFloatAttr(pp, "x", 0);
		final float y = XmlPullUtil.optFloatAttr(pp, "y", 0);

		final int lat;
		final int lon;
		if (mapName == null || (x == 0 && y == 0))
		{
			lat = 0;
			lon = 0;
		}
		else if ("WGS84".equals(mapName))
		{
			lat = Math.round(y);
			lon = Math.round(x);
		}
		else
		{
			throw new IllegalStateException("unknown mapName=" + mapName + " x=" + x + " y=" + y);
		}

		return new Location(LocationType.STATION, id, lat, lon, place, name);
	}

	private boolean processItdDateTime(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp);
		calendar.clear();
		final boolean success = processItdDate(pp, calendar);
		if (success)
			processItdTime(pp, calendar);
		XmlPullUtil.exit(pp);

		return success;
	}

	private boolean processItdDate(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		XmlPullUtil.require(pp, "itdDate");
		final int year = Integer.parseInt(pp.getAttributeValue(null, "year"));
		final int month = Integer.parseInt(pp.getAttributeValue(null, "month")) - 1;
		final int day = Integer.parseInt(pp.getAttributeValue(null, "day"));
		final int weekday = Integer.parseInt(pp.getAttributeValue(null, "weekday"));
		XmlPullUtil.next(pp);

		if (weekday < 0)
			return false;
		if (year == 0)
			return false;
		if (year < 1900 || year > 2100)
			throw new InvalidDataException("invalid year: " + year);
		if (month < 0 || month > 11)
			throw new InvalidDataException("invalid month: " + month);
		if (day < 1 || day > 31)
			throw new InvalidDataException("invalid day: " + day);

		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		return true;
	}

	private void processItdTime(final XmlPullParser pp, final Calendar calendar) throws XmlPullParserException, IOException
	{
		XmlPullUtil.require(pp, "itdTime");
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(pp.getAttributeValue(null, "hour")));
		calendar.set(Calendar.MINUTE, Integer.parseInt(pp.getAttributeValue(null, "minute")));
		XmlPullUtil.next(pp);
	}

	private Line processItdServingLine(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		XmlPullUtil.require(pp, "itdServingLine");
		final String slMotType = pp.getAttributeValue(null, "motType");
		final String slSymbol = pp.getAttributeValue(null, "symbol");
		final String slNumber = pp.getAttributeValue(null, "number");
		final String slStateless = pp.getAttributeValue(null, "stateless");
		final String slTrainType = pp.getAttributeValue(null, "trainType");
		final String slTrainName = pp.getAttributeValue(null, "trainName");
		final String slTrainNum = pp.getAttributeValue(null, "trainNum");

		XmlPullUtil.enter(pp, "itdServingLine");
		String itdTrainName = null;
		String itdTrainType = null;
		String itdMessage = null;
		if (XmlPullUtil.test(pp, "itdTrain"))
		{
			itdTrainName = pp.getAttributeValue(null, "name");
			itdTrainType = pp.getAttributeValue(null, "type");
			if (!pp.isEmptyElementTag())
			{
				XmlPullUtil.enter(pp, "itdTrain");
				XmlPullUtil.exit(pp, "itdTrain");
			}
			else
			{
				XmlPullUtil.next(pp);
			}
		}
		if (XmlPullUtil.test(pp, "itdNoTrain"))
		{
			itdTrainName = pp.getAttributeValue(null, "name");
			itdTrainType = pp.getAttributeValue(null, "type");
			if (!pp.isEmptyElementTag())
			{
				XmlPullUtil.enter(pp, "itdNoTrain");
				final String text = pp.getText();
				if (itdTrainName.toLowerCase().contains("ruf") && text.toLowerCase().contains("ruf"))
					itdMessage = text;
				XmlPullUtil.exit(pp, "itdNoTrain");
			}
			else
			{
				XmlPullUtil.next(pp);
			}
		}
		XmlPullUtil.exit(pp, "itdServingLine");

		final String trainType = ParserUtils.firstNotEmpty(slTrainType, itdTrainType);
		final String trainName = ParserUtils.firstNotEmpty(slTrainName, itdTrainName);

		final String label = parseLine(slMotType, slSymbol, slNumber, slNumber, trainType, slTrainNum, trainName);

		return new Line(slStateless, label, lineStyle(label), itdMessage);
	}

	private static final Pattern P_STATION_NAME_WHITESPACE = Pattern.compile("\\s+");

	protected String normalizeLocationName(final String name)
	{
		if (name == null || name.length() == 0)
			return null;

		return P_STATION_NAME_WHITESPACE.matcher(name).replaceAll(" ");
	}

	protected static double latLonToDouble(final int value)
	{
		return (double) value / 1000000;
	}

	protected String xsltTripRequestParameters(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int numTrips, final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options)
	{
		final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);
		final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmm", Locale.US);

		final StringBuilder uri = new StringBuilder();
		appendCommonRequestParams(uri, "XML");

		uri.append("&sessionID=0");
		uri.append("&requestID=0");
		uri.append("&language=de");

		appendCommonXsltTripRequest2Params(uri);

		appendLocation(uri, from, "origin");
		appendLocation(uri, to, "destination");
		if (via != null)
			appendLocation(uri, via, "via");

		uri.append("&itdDate=").append(ParserUtils.urlEncode(DATE_FORMAT.format(date)));
		uri.append("&itdTime=").append(ParserUtils.urlEncode(TIME_FORMAT.format(date)));
		uri.append("&itdTripDateTimeDepArr=").append(dep ? "dep" : "arr");

		uri.append("&calcNumberOfTrips=").append(numTrips);

		uri.append("&ptOptionsActive=1"); // enable public transport options
		uri.append("&itOptionsActive=1"); // enable individual transport options
		uri.append("&changeSpeed=").append(WALKSPEED_MAP.get(walkSpeed));

		if (accessibility == Accessibility.BARRIER_FREE)
			uri.append("&imparedOptionsActive=1").append("&wheelchair=on").append("&noSolidStairs=on");
		else if (accessibility == Accessibility.LIMITED)
			uri.append("&imparedOptionsActive=1").append("&wheelchair=on").append("&lowPlatformVhcl=on").append("&noSolidStairs=on");

		if (products != null)
		{
			uri.append("&includedMeans=checkbox");

			boolean hasI = false;
			for (final Product p : products)
			{
				if (p == Product.HIGH_SPEED_TRAIN || p == Product.REGIONAL_TRAIN)
				{
					uri.append("&inclMOT_0=on");
					if (p == Product.HIGH_SPEED_TRAIN)
						hasI = true;
				}

				if (p == Product.SUBURBAN_TRAIN)
					uri.append("&inclMOT_1=on");

				if (p == Product.SUBWAY)
					uri.append("&inclMOT_2=on");

				if (p == Product.TRAM)
					uri.append("&inclMOT_3=on&inclMOT_4=on");

				if (p == Product.BUS)
					uri.append("&inclMOT_5=on&inclMOT_6=on&inclMOT_7=on");

				if (p == Product.ON_DEMAND)
					uri.append("&inclMOT_10=on");

				if (p == Product.FERRY)
					uri.append("&inclMOT_9=on");

				if (p == Product.CABLECAR)
					uri.append("&inclMOT_8=on");
			}

			uri.append("&inclMOT_11=on"); // TODO always show 'others', for now

			// workaround for highspeed trains: fails when you want highspeed, but not regional
			if (useLineRestriction && !hasI)
				uri.append("&lineRestriction=403"); // means: all but ice
		}

		if (options != null && options.contains(Option.BIKE))
			uri.append("&bikeTakeAlong=1");

		uri.append("&locationServerActive=1");
		uri.append("&useRealtime=1");
		uri.append("&useProxFootSearch=1"); // walk if it makes journeys quicker
		uri.append("&nextDepsPerLeg=1"); // next departure in case previous was missed

		return uri.toString();
	}

	private String commandLink(final String sessionId, final String requestId)
	{
		final StringBuilder uri = new StringBuilder(tripEndpoint);

		uri.append("?sessionID=").append(sessionId);
		uri.append("&requestID=").append(requestId);
		appendCommonXsltTripRequest2Params(uri);

		return uri.toString();
	}

	private static final void appendCommonXsltTripRequest2Params(final StringBuilder uri)
	{
		uri.append("&coordListOutputFormat=STRING");
		uri.append("&calcNumberOfTrips=4");
	}

	public QueryTripsResult queryTrips(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int numTrips, final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options) throws IOException
	{

		final String parameters = xsltTripRequestParameters(from, via, to, date, dep, numTrips, products, walkSpeed, accessibility, options);

		final StringBuilder uri = new StringBuilder(tripEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpRefererTrip, "NSC_", 3);
			return queryTrips(uri.toString(), is);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		catch (final RuntimeException x)
		{
			throw new RuntimeException("uncategorized problem while processing " + uri, x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	protected QueryTripsResult queryTripsMobile(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final int numTrips, final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility,
			final Set<Option> options) throws IOException
	{

		final String parameters = xsltTripRequestParameters(from, via, to, date, dep, numTrips, products, walkSpeed, accessibility, options);

		final StringBuilder uri = new StringBuilder(tripEndpoint);
		if (!httpPost)
			uri.append(parameters);

		// System.out.println(uri);
		// System.out.println(parameters);

		InputStream is = null;
		try
		{
			is = ParserUtils.scrapeInputStream(uri.toString(), httpPost ? parameters.substring(1) : null, null, httpRefererTrip, "NSC_", 3);
			return queryTripsMobile(uri.toString(), from, via, to, is);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		catch (final RuntimeException x)
		{
			throw new RuntimeException("uncategorized problem while processing " + uri, x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private static final Pattern P_SESSION_EXPIRED = Pattern.compile("Your session has expired");

	public QueryTripsResult queryMoreTrips(final QueryTripsContext contextObj, final boolean later, final int numTrips) throws IOException
	{
		final Context context = (Context) contextObj;
		final String commandUri = context.context;
		final StringBuilder uri = new StringBuilder(commandUri);
		uri.append("&command=").append(later ? "tripNext" : "tripPrev");

		InputStream is = null;
		try
		{
			is = new BufferedInputStream(ParserUtils.scrapeInputStream(uri.toString(), null, null, httpRefererTrip, "NSC_", 3));
			is.mark(512);

			return queryTrips(uri.toString(), is);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		catch (final NotFoundException x)
		{
			throw new SessionExpiredException();
		}
		catch (final ProtocolException x) // must be html content
		{
			is.reset();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

			String line;
			while ((line = reader.readLine()) != null)
				if (P_SESSION_EXPIRED.matcher(line).find())
					throw new SessionExpiredException();

			throw x;
		}
		catch (final RuntimeException x)
		{
			throw new RuntimeException("uncategorized problem while processing " + uri, x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	protected QueryTripsResult queryMoreTripsMobile(final QueryTripsContext contextObj, final boolean later, final int numConnections)
			throws IOException
	{
		final Context context = (Context) contextObj;
		final String commandUri = context.context;
		final StringBuilder uri = new StringBuilder(commandUri);
		uri.append("&command=").append(later ? "tripNext" : "tripPrev");

		InputStream is = null;
		try
		{
			is = new BufferedInputStream(ParserUtils.scrapeInputStream(uri.toString(), null, null, httpRefererTrip, "NSC_", 3));
			is.mark(512);

			return queryTripsMobile(uri.toString(), null, null, null, is);
		}
		catch (final XmlPullParserException x)
		{
			throw new ParserException(x);
		}
		catch (final ProtocolException x) // must be html content
		{
			is.reset();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

			String line;
			while ((line = reader.readLine()) != null)
				if (P_SESSION_EXPIRED.matcher(line).find())
					throw new SessionExpiredException();

			throw x;
		}
		catch (final RuntimeException x)
		{
			throw new RuntimeException("uncategorized problem while processing " + uri, x);
		}
		finally
		{
			if (is != null)
				is.close();
		}
	}

	private QueryTripsResult queryTrips(final String uri, final InputStream is) throws XmlPullParserException, IOException
	{
		// System.out.println(uri);

		final XmlPullParser pp = parserFactory.newPullParser();
		pp.setInput(is, null);
		final ResultHeader header = enterItdRequest(pp);
		final Object context = header.context;

		if (XmlPullUtil.test(pp, "itdLayoutParams"))
			XmlPullUtil.next(pp);

		XmlPullUtil.require(pp, "itdTripRequest");
		final String requestId = XmlPullUtil.attr(pp, "requestID");
		XmlPullUtil.enter(pp, "itdTripRequest");

		if (XmlPullUtil.test(pp, "itdMessage"))
		{
			final int code = XmlPullUtil.intAttr(pp, "code");
			if (code == -4000) // no trips
				return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
			XmlPullUtil.next(pp);
		}
		if (XmlPullUtil.test(pp, "itdPrintConfiguration"))
			XmlPullUtil.next(pp);
		if (XmlPullUtil.test(pp, "itdAddress"))
			XmlPullUtil.next(pp);

		// parse odv name elements
		List<Location> ambiguousFrom = null, ambiguousTo = null, ambiguousVia = null;
		Location from = null, via = null, to = null;

		while (XmlPullUtil.test(pp, "itdOdv"))
		{
			final String usage = XmlPullUtil.attr(pp, "usage");
			XmlPullUtil.enter(pp, "itdOdv");

			final String place = processItdOdvPlace(pp);

			if (!XmlPullUtil.test(pp, "itdOdvName"))
				throw new IllegalStateException("cannot find <itdOdvName /> inside " + usage);
			final String nameState = XmlPullUtil.attr(pp, "state");
			XmlPullUtil.enter(pp, "itdOdvName");
			if (XmlPullUtil.test(pp, "itdMessage"))
				XmlPullUtil.next(pp);

			if ("list".equals(nameState))
			{
				if ("origin".equals(usage))
				{
					ambiguousFrom = new ArrayList<Location>();
					while (XmlPullUtil.test(pp, "odvNameElem"))
						ambiguousFrom.add(processOdvNameElem(pp, place));
				}
				else if ("via".equals(usage))
				{
					ambiguousVia = new ArrayList<Location>();
					while (XmlPullUtil.test(pp, "odvNameElem"))
						ambiguousVia.add(processOdvNameElem(pp, place));
				}
				else if ("destination".equals(usage))
				{
					ambiguousTo = new ArrayList<Location>();
					while (XmlPullUtil.test(pp, "odvNameElem"))
						ambiguousTo.add(processOdvNameElem(pp, place));
				}
				else
				{
					throw new IllegalStateException("unknown usage: " + usage);
				}
			}
			else if ("identified".equals(nameState))
			{
				if (!XmlPullUtil.test(pp, "odvNameElem"))
					throw new IllegalStateException("cannot find <odvNameElem /> inside " + usage);

				if ("origin".equals(usage))
					from = processOdvNameElem(pp, place);
				else if ("via".equals(usage))
					via = processOdvNameElem(pp, place);
				else if ("destination".equals(usage))
					to = processOdvNameElem(pp, place);
				else
					throw new IllegalStateException("unknown usage: " + usage);
			}
			else if ("notidentified".equals(nameState))
			{
				if ("origin".equals(usage))
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_FROM);
				else if ("via".equals(usage))
					// return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_VIA);
					throw new UnsupportedOperationException();
				else if ("destination".equals(usage))
					return new QueryTripsResult(header, QueryTripsResult.Status.UNKNOWN_TO);
				else
					throw new IllegalStateException("unknown usage: " + usage);
			}
			XmlPullUtil.exit(pp, "itdOdvName");
			XmlPullUtil.exit(pp, "itdOdv");
		}

		if (ambiguousFrom != null || ambiguousTo != null || ambiguousVia != null)
			return new QueryTripsResult(header, ambiguousFrom, ambiguousVia, ambiguousTo);

		XmlPullUtil.enter(pp, "itdTripDateTime");
		XmlPullUtil.enter(pp, "itdDateTime");
		XmlPullUtil.require(pp, "itdDate");
		if (!pp.isEmptyElementTag())
		{
			XmlPullUtil.enter(pp, "itdDate");
			if (XmlPullUtil.test(pp, "itdMessage"))
			{
				final String message = XmlPullUtil.nextText(pp, null, "itdMessage");

				if ("invalid date".equals(message))
					return new QueryTripsResult(header, QueryTripsResult.Status.INVALID_DATE);
				else
					throw new IllegalStateException("unknown message: " + message);
			}
			XmlPullUtil.exit(pp, "itdDate");
		}
		else
		{
			XmlPullUtil.next(pp);
		}
		XmlPullUtil.exit(pp, "itdDateTime");

		final Calendar time = new GregorianCalendar(timeZone());
		final List<Trip> trips = new ArrayList<Trip>();

		if (XmlPullUtil.jumpToStartTag(pp, null, "itdRouteList"))
		{
			XmlPullUtil.enter(pp, "itdRouteList");

			while (XmlPullUtil.test(pp, "itdRoute"))
			{
				final String id = useRouteIndexAsTripId ? pp.getAttributeValue(null, "routeIndex") + "-"
						+ pp.getAttributeValue(null, "routeTripIndex") : null;
				final int numChanges = XmlPullUtil.intAttr(pp, "changes");
				XmlPullUtil.enter(pp, "itdRoute");

				while (XmlPullUtil.test(pp, "itdDateTime"))
					XmlPullUtil.next(pp);
				if (XmlPullUtil.test(pp, "itdMapItemList"))
					XmlPullUtil.next(pp);

				XmlPullUtil.enter(pp, "itdPartialRouteList");
				final List<Trip.Leg> legs = new LinkedList<Trip.Leg>();
				Location firstDepartureLocation = null;
				Location lastArrivalLocation = null;

				boolean cancelled = false;

				while (XmlPullUtil.test(pp, "itdPartialRoute"))
				{
					final String partialRouteType = XmlPullUtil.attr(pp, "type");
					final int distance = XmlPullUtil.optIntAttr(pp, "distance", 0);
					XmlPullUtil.enter(pp, "itdPartialRoute");

					XmlPullUtil.test(pp, "itdPoint");
					if (!"departure".equals(pp.getAttributeValue(null, "usage")))
						throw new IllegalStateException();
					final Location departureLocation = processItdPointAttributes(pp);
					if (firstDepartureLocation == null)
						firstDepartureLocation = departureLocation;
					final String departurePosition;
					if (!suppressPositions)
						departurePosition = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null, "platformName"));
					else
						departurePosition = null;
					XmlPullUtil.enter(pp, "itdPoint");
					if (XmlPullUtil.test(pp, "itdMapItemList"))
						XmlPullUtil.next(pp);
					XmlPullUtil.require(pp, "itdDateTime");
					processItdDateTime(pp, time);
					final Date departureTime = time.getTime();
					final Date departureTargetTime;
					if (XmlPullUtil.test(pp, "itdDateTimeTarget"))
					{
						processItdDateTime(pp, time);
						departureTargetTime = time.getTime();
					}
					else
					{
						departureTargetTime = null;
					}
					XmlPullUtil.exit(pp, "itdPoint");

					XmlPullUtil.test(pp, "itdPoint");
					if (!"arrival".equals(pp.getAttributeValue(null, "usage")))
						throw new IllegalStateException();
					final Location arrivalLocation = processItdPointAttributes(pp);
					lastArrivalLocation = arrivalLocation;
					final String arrivalPosition;
					if (!suppressPositions)
						arrivalPosition = normalizePlatform(pp.getAttributeValue(null, "platform"), pp.getAttributeValue(null, "platformName"));
					else
						arrivalPosition = null;
					XmlPullUtil.enter(pp, "itdPoint");
					if (XmlPullUtil.test(pp, "itdMapItemList"))
						XmlPullUtil.next(pp);
					XmlPullUtil.require(pp, "itdDateTime");
					processItdDateTime(pp, time);
					final Date arrivalTime = time.getTime();
					final Date arrivalTargetTime;
					if (XmlPullUtil.test(pp, "itdDateTimeTarget"))
					{
						processItdDateTime(pp, time);
						arrivalTargetTime = time.getTime();
					}
					else
					{
						arrivalTargetTime = null;
					}
					XmlPullUtil.exit(pp, "itdPoint");

					XmlPullUtil.test(pp, "itdMeansOfTransport");
					final String productName = pp.getAttributeValue(null, "productName");
					if ("IT".equals(partialRouteType) || "Fussweg".equals(productName) || "Taxi".equals(productName))
					{
						final Trip.Individual.Type type = "Taxi".equals(productName) ? Trip.Individual.Type.TRANSFER : Trip.Individual.Type.WALK;

						XmlPullUtil.enter(pp, "itdMeansOfTransport");
						XmlPullUtil.exit(pp, "itdMeansOfTransport");

						if (XmlPullUtil.test(pp, "itdStopSeq"))
							XmlPullUtil.next(pp);

						if (XmlPullUtil.test(pp, "itdFootPathInfo"))
							XmlPullUtil.next(pp);

						List<Point> path = null;
						if (XmlPullUtil.test(pp, "itdPathCoordinates"))
							path = processItdPathCoordinates(pp);

						final Trip.Leg lastLeg = legs.size() > 0 ? legs.get(legs.size() - 1) : null;
						if (lastLeg != null && lastLeg instanceof Trip.Individual && ((Trip.Individual) lastLeg).type == type)
						{
							final Trip.Individual lastIndividual = (Trip.Individual) legs.remove(legs.size() - 1);
							if (path != null && lastIndividual.path != null)
								path.addAll(0, lastIndividual.path);
							legs.add(new Trip.Individual(type, lastIndividual.departure, lastIndividual.departureTime, arrivalLocation, arrivalTime,
									path, distance));
						}
						else
						{
							legs.add(new Trip.Individual(type, departureLocation, departureTime, arrivalLocation, arrivalTime, path, distance));
						}
					}
					else if ("gesicherter Anschluss".equals(productName) || "nicht umsteigen".equals(productName)) // type97
					{
						// ignore

						XmlPullUtil.enter(pp, "itdMeansOfTransport");
						XmlPullUtil.exit(pp, "itdMeansOfTransport");
					}
					else if ("PT".equals(partialRouteType))
					{
						final String destinationName = normalizeLocationName(pp.getAttributeValue(null, "destination"));
						final String destinationIdStr = pp.getAttributeValue(null, "destID");
						final int destinationId = (destinationIdStr != null && destinationIdStr.length() > 0) ? Integer.parseInt(destinationIdStr)
								: 0;
						final Location destination = new Location(destinationId > 0 ? LocationType.STATION : LocationType.ANY,
								destinationId > 0 ? destinationId : 0, null, destinationName);
						final String lineLabel;
						final String motSymbol = pp.getAttributeValue(null, "symbol");
						if ("AST".equals(motSymbol))
						{
							lineLabel = "BAST";
						}
						else
						{
							final String motType = pp.getAttributeValue(null, "motType");
							final String motShortName = pp.getAttributeValue(null, "shortname");
							final String motName = pp.getAttributeValue(null, "name");
							final String motTrainName = pp.getAttributeValue(null, "trainName");
							final String motTrainType = pp.getAttributeValue(null, "trainType");

							lineLabel = parseLine(motType, motSymbol, motShortName, motName, motTrainType, motShortName, motTrainName);
						}
						XmlPullUtil.enter(pp, "itdMeansOfTransport");
						XmlPullUtil.require(pp, "motDivaParams");
						final String divaNetwork = XmlPullUtil.attr(pp, "network");
						final String divaLine = XmlPullUtil.attr(pp, "line");
						final String divaSupplement = XmlPullUtil.optAttr(pp, "supplement", "");
						final String divaDirection = XmlPullUtil.attr(pp, "direction");
						final String divaProject = XmlPullUtil.attr(pp, "project");
						final String lineId = divaNetwork + ':' + divaLine + ':' + divaSupplement + ':' + divaDirection + ':' + divaProject;
						XmlPullUtil.exit(pp, "itdMeansOfTransport");

						final Integer departureDelay;
						final Integer arrivalDelay;
						if (XmlPullUtil.test(pp, "itdRBLControlled"))
						{
							departureDelay = XmlPullUtil.optIntAttr(pp, "delayMinutes", 0);
							arrivalDelay = XmlPullUtil.optIntAttr(pp, "delayMinutesArr", 0);

							cancelled |= (departureDelay == -9999 || arrivalDelay == -9999);

							XmlPullUtil.next(pp);
						}
						else
						{
							departureDelay = null;
							arrivalDelay = null;
						}

						boolean lowFloorVehicle = false;
						String message = null;
						if (XmlPullUtil.test(pp, "itdInfoTextList"))
						{
							if (!pp.isEmptyElementTag())
							{
								XmlPullUtil.enter(pp, "itdInfoTextList");
								while (XmlPullUtil.test(pp, "infoTextListElem"))
								{
									XmlPullUtil.enter(pp, "infoTextListElem");
									final String text = pp.getText();
									if ("Niederflurwagen soweit verfügbar".equals(text)) // KVV
										lowFloorVehicle = true;
									else if (text != null && text.toLowerCase().contains("ruf")) // RufBus, RufTaxi
										message = text;
									XmlPullUtil.exit(pp, "infoTextListElem");
								}
								XmlPullUtil.exit(pp, "itdInfoTextList");
							}
							else
							{
								XmlPullUtil.next(pp);
							}
						}

						if (XmlPullUtil.test(pp, "itdFootPathInfo"))
							XmlPullUtil.next(pp);
						if (XmlPullUtil.test(pp, "infoLink"))
							XmlPullUtil.next(pp);

						List<Stop> intermediateStops = null;
						if (XmlPullUtil.test(pp, "itdStopSeq"))
						{
							XmlPullUtil.enter(pp, "itdStopSeq");
							intermediateStops = new LinkedList<Stop>();
							while (XmlPullUtil.test(pp, "itdPoint"))
							{
								final Location stopLocation = processItdPointAttributes(pp);

								final String stopPosition;
								if (!suppressPositions)
									stopPosition = normalizePlatform(pp.getAttributeValue(null, "platform"),
											pp.getAttributeValue(null, "platformName"));
								else
									stopPosition = null;

								XmlPullUtil.enter(pp, "itdPoint");
								XmlPullUtil.require(pp, "itdDateTime");

								final Date plannedStopArrivalTime;
								final Date predictedStopArrivalTime;
								if (processItdDateTime(pp, time))
								{
									plannedStopArrivalTime = time.getTime();
									if (arrivalDelay != null)
									{
										time.add(Calendar.MINUTE, arrivalDelay);
										predictedStopArrivalTime = time.getTime();
									}
									else
									{
										predictedStopArrivalTime = null;
									}
								}
								else
								{
									plannedStopArrivalTime = null;
									predictedStopArrivalTime = null;
								}

								final Date plannedStopDepartureTime;
								final Date predictedStopDepartureTime;
								if (XmlPullUtil.test(pp, "itdDateTime") && processItdDateTime(pp, time))
								{
									plannedStopDepartureTime = time.getTime();
									if (departureDelay != null)
									{
										time.add(Calendar.MINUTE, departureDelay);
										predictedStopDepartureTime = time.getTime();
									}
									else
									{
										predictedStopDepartureTime = null;
									}
								}
								else
								{
									plannedStopDepartureTime = null;
									predictedStopDepartureTime = null;
								}

								final Stop stop = new Stop(stopLocation, plannedStopArrivalTime, predictedStopArrivalTime, stopPosition, null,
										plannedStopDepartureTime, predictedStopDepartureTime, stopPosition, null);

								intermediateStops.add(stop);

								XmlPullUtil.exit(pp, "itdPoint");
							}
							XmlPullUtil.exit(pp, "itdStopSeq");

							// remove first and last, because they are not intermediate
							final int size = intermediateStops.size();
							if (size >= 2)
							{
								if (intermediateStops.get(size - 1).location.id != arrivalLocation.id)
									throw new IllegalStateException();
								intermediateStops.remove(size - 1);

								if (intermediateStops.get(0).location.id != departureLocation.id)
									throw new IllegalStateException();
								intermediateStops.remove(0);
							}
						}

						List<Point> path = null;
						if (XmlPullUtil.test(pp, "itdPathCoordinates"))
							path = processItdPathCoordinates(pp);

						boolean wheelChairAccess = false;
						if (XmlPullUtil.test(pp, "genAttrList"))
						{
							XmlPullUtil.enter(pp, "genAttrList");
							while (XmlPullUtil.test(pp, "genAttrElem"))
							{
								XmlPullUtil.enter(pp, "genAttrElem");
								XmlPullUtil.enter(pp, "name");
								final String name = pp.getText();
								XmlPullUtil.exit(pp, "name");
								XmlPullUtil.enter(pp, "value");
								final String value = pp.getText();
								XmlPullUtil.exit(pp, "value");
								XmlPullUtil.exit(pp, "genAttrElem");

								// System.out.println("genAttrElem: name='" + name + "' value='" + value + "'");

								if ("PlanWheelChairAccess".equals(name) && "1".equals(value))
									wheelChairAccess = true;
							}
							XmlPullUtil.exit(pp, "genAttrList");
						}

						if (XmlPullUtil.test(pp, "nextDeps"))
						{
							XmlPullUtil.enter(pp, "nextDeps");
							while (XmlPullUtil.test(pp, "itdDateTime"))
							{
								processItdDateTime(pp, time);
								/* final Date nextDepartureTime = */time.getTime();
							}
							XmlPullUtil.exit(pp, "nextDeps");
						}

						final Set<Line.Attr> lineAttrs = new HashSet<Line.Attr>();
						if (wheelChairAccess || lowFloorVehicle)
							lineAttrs.add(Line.Attr.WHEEL_CHAIR_ACCESS);
						final Line line = new Line(lineId, lineLabel, lineStyle(lineLabel), lineAttrs);

						final Stop departure = new Stop(departureLocation, true, departureTargetTime != null ? departureTargetTime : departureTime,
								departureTime != null ? departureTime : null, departurePosition, null);
						final Stop arrival = new Stop(arrivalLocation, false, arrivalTargetTime != null ? arrivalTargetTime : arrivalTime,
								arrivalTime != null ? arrivalTime : null, arrivalPosition, null);

						legs.add(new Trip.Public(line, destination, departure, arrival, intermediateStops, path, message));
					}
					else
					{
						throw new IllegalStateException("unknown type: '" + partialRouteType + "' '" + productName + "'");
					}

					XmlPullUtil.exit(pp, "itdPartialRoute");
				}

				XmlPullUtil.exit(pp, "itdPartialRouteList");

				final List<Fare> fares = new ArrayList<Fare>(2);
				if (XmlPullUtil.test(pp, "itdFare"))
				{
					if (!pp.isEmptyElementTag())
					{
						XmlPullUtil.enter(pp, "itdFare");
						if (XmlPullUtil.test(pp, "itdSingleTicket"))
						{
							final String net = XmlPullUtil.attr(pp, "net");
							final Currency currency = parseCurrency(XmlPullUtil.attr(pp, "currency"));
							final String fareAdult = XmlPullUtil.attr(pp, "fareAdult");
							final String fareChild = XmlPullUtil.attr(pp, "fareChild");
							final String unitName = XmlPullUtil.attr(pp, "unitName");
							final String unitsAdult = XmlPullUtil.attr(pp, "unitsAdult");
							final String unitsChild = XmlPullUtil.attr(pp, "unitsChild");
							final String levelAdult = pp.getAttributeValue(null, "levelAdult");
							final boolean hasLevelAdult = levelAdult != null && levelAdult.length() > 0;
							final String levelChild = pp.getAttributeValue(null, "levelChild");
							final boolean hasLevelChild = levelChild != null && levelChild.length() > 0;
							if (fareAdult != null && fareAdult.length() > 0)
								fares.add(new Fare(net, Type.ADULT, currency, Float.parseFloat(fareAdult), hasLevelAdult ? null : unitName,
										hasLevelAdult ? levelAdult : unitsAdult));
							if (fareChild != null && fareChild.length() > 0)
								fares.add(new Fare(net, Type.CHILD, currency, Float.parseFloat(fareChild), hasLevelChild ? null : unitName,
										hasLevelChild ? levelChild : unitsChild));

							if (!pp.isEmptyElementTag())
							{
								XmlPullUtil.enter(pp, "itdSingleTicket");
								if (XmlPullUtil.test(pp, "itdGenericTicketList"))
								{
									XmlPullUtil.enter(pp, "itdGenericTicketList");
									while (XmlPullUtil.test(pp, "itdGenericTicketGroup"))
									{
										final Fare fare = processItdGenericTicketGroup(pp, net, currency);
										if (fare != null)
											fares.add(fare);
									}
									XmlPullUtil.exit(pp, "itdGenericTicketList");
								}
								XmlPullUtil.exit(pp, "itdSingleTicket");
							}
							else
							{
								XmlPullUtil.next(pp);
							}
						}
						XmlPullUtil.exit(pp, "itdFare");
					}
					else
					{
						XmlPullUtil.next(pp);
					}
				}

				XmlPullUtil.exit(pp, "itdRoute");

				final Trip trip = new Trip(id, firstDepartureLocation, lastArrivalLocation, legs, fares.isEmpty() ? null : fares, null, numChanges);

				if (!cancelled)
					trips.add(trip);
			}

			XmlPullUtil.exit(pp, "itdRouteList");

			return new QueryTripsResult(header, uri, from, via, to, new Context(commandLink((String) context, requestId)), trips);
		}
		else
		{
			return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
		}
	}

	private QueryTripsResult queryTripsMobile(final String uri, final Location from, final Location via, final Location to, final InputStream is)
			throws XmlPullParserException, IOException
	{
		// System.out.println(uri);

		final XmlPullParser pp = parserFactory.newPullParser();
		pp.setInput(is, null);
		final ResultHeader header = enterEfa(pp);

		final Calendar plannedTime = new GregorianCalendar(timeZone());
		final Calendar predictedTime = new GregorianCalendar(timeZone());

		final List<Trip> trips = new ArrayList<Trip>();

		if (XmlPullUtil.test(pp, "ts"))
		{
			XmlPullUtil.enter(pp, "ts");

			while (XmlPullUtil.test(pp, "tp"))
			{
				XmlPullUtil.enter(pp, "tp");

				XmlPullUtil.optSkip(pp, "attrs");

				requireValueTag(pp, "d"); // duration
				final int numChanges = Integer.parseInt(requireValueTag(pp, "ic"));
				final String tripId = requireValueTag(pp, "de");

				XmlPullUtil.enter(pp, "ls");

				final List<Trip.Leg> legs = new LinkedList<Trip.Leg>();
				Location firstDepartureLocation = null;
				Location lastArrivalLocation = null;

				while (XmlPullUtil.test(pp, "l"))
				{
					XmlPullUtil.enter(pp, "l");

					XmlPullUtil.enter(pp, "ps");

					Stop departure = null;
					Stop arrival = null;

					while (XmlPullUtil.test(pp, "p"))
					{
						XmlPullUtil.enter(pp, "p");

						final String name = requireValueTag(pp, "n");
						final String usage = requireValueTag(pp, "u");
						optValueTag(pp, "de");

						XmlPullUtil.requireSkip(pp, "dt");

						parseMobileSt(pp, plannedTime, predictedTime);

						XmlPullUtil.requireSkip(pp, "lis");

						XmlPullUtil.enter(pp, "r");
						final int id = Integer.parseInt(requireValueTag(pp, "id"));
						optValueTag(pp, "a");
						final String position = optValueTag(pp, "pl");
						final String place = normalizeLocationName(optValueTag(pp, "pc"));
						final Point coord = coordStrToPoint(requireValueTag(pp, "c"));
						XmlPullUtil.exit(pp, "r");

						final Location location;
						if (id == 99999997 || id == 99999998)
							location = new Location(LocationType.ADDRESS, 0, coord.lat, coord.lon, place, name);
						else
							location = new Location(LocationType.STATION, id, coord.lat, coord.lon, place, name);

						XmlPullUtil.exit(pp, "p");

						if ("departure".equals(usage))
						{
							departure = new Stop(location, true, plannedTime.isSet(Calendar.HOUR_OF_DAY) ? plannedTime.getTime()
									: predictedTime.getTime(), predictedTime.isSet(Calendar.HOUR_OF_DAY) ? predictedTime.getTime() : null, position,
									null);
							if (firstDepartureLocation == null)
								firstDepartureLocation = location;
						}
						else if ("arrival".equals(usage))
						{
							arrival = new Stop(location, false, plannedTime.isSet(Calendar.HOUR_OF_DAY) ? plannedTime.getTime()
									: predictedTime.getTime(), predictedTime.isSet(Calendar.HOUR_OF_DAY) ? predictedTime.getTime() : null, position,
									null);
							lastArrivalLocation = location;
						}
						else
						{
							throw new IllegalStateException("unknown usage: " + usage);
						}
					}

					XmlPullUtil.exit(pp, "ps");

					final boolean isRealtime = requireValueTag(pp, "realtime").equals("1");

					final LineDestination lineDestination = parseMobileM(pp, false);

					final List<Point> path;
					if (XmlPullUtil.test(pp, "pt"))
						path = processCoordinateStrings(pp, "pt");
					else
						path = null;

					XmlPullUtil.require(pp, "pss");

					final List<Stop> intermediateStops;

					if (!pp.isEmptyElementTag())
					{
						XmlPullUtil.enter(pp, "pss");

						intermediateStops = new LinkedList<Stop>();

						while (XmlPullUtil.test(pp, "s"))
						{
							plannedTime.clear();
							predictedTime.clear();

							final String s = requireValueTag(pp, "s");
							final String[] intermediateParts = s.split(";");
							final int id = Integer.parseInt(intermediateParts[0]);
							if (id != departure.location.id && id != arrival.location.id)
							{
								final String name = normalizeLocationName(intermediateParts[1]);

								if (!("0000-1".equals(intermediateParts[2]) && "000-1".equals(intermediateParts[3])))
								{
									ParserUtils.parseIsoDate(plannedTime, intermediateParts[2]);
									ParserUtils.parseIsoTime(plannedTime, intermediateParts[3]);

									if (isRealtime)
									{
										ParserUtils.parseIsoDate(predictedTime, intermediateParts[2]);
										ParserUtils.parseIsoTime(predictedTime, intermediateParts[3]);

										if (intermediateParts.length > 5)
										{
											final int delay = Integer.parseInt(intermediateParts[5]);
											predictedTime.add(Calendar.MINUTE, delay);
										}
									}
								}
								final String coord = intermediateParts[4];

								final Location location;
								if (!"::".equals(coord))
								{
									final String[] coordParts = coord.split(":");
									if (!"WGS84".equals(coordParts[2]))
										throw new IllegalStateException("unknown map name: " + coordParts[2]);
									final int lat = Math.round(Float.parseFloat(coordParts[1]));
									final int lon = Math.round(Float.parseFloat(coordParts[0]));
									location = new Location(LocationType.STATION, id, lat, lon, null, name);
								}
								else
								{
									location = new Location(LocationType.STATION, id, null, name);
								}

								final Stop stop = new Stop(location, false, plannedTime.isSet(Calendar.HOUR_OF_DAY) ? plannedTime.getTime()
										: predictedTime.getTime(), predictedTime.isSet(Calendar.HOUR_OF_DAY) ? predictedTime.getTime() : null, null,
										null);

								intermediateStops.add(stop);
							}
						}

						XmlPullUtil.exit(pp, "pss");
					}
					else
					{
						intermediateStops = null;

						XmlPullUtil.next(pp);
					}

					XmlPullUtil.optSkip(pp, "interchange");

					XmlPullUtil.requireSkip(pp, "ns");
					// TODO messages

					XmlPullUtil.exit(pp, "l");

					if (lineDestination.line == Line.FOOTWAY)
					{
						legs.add(new Trip.Individual(Trip.Individual.Type.WALK, departure.location, departure.getDepartureTime(), arrival.location,
								arrival.getArrivalTime(), path, 0));
					}
					else if (lineDestination.line == Line.SECURE_CONNECTION || lineDestination.line == Line.DO_NOT_CHANGE)
					{
						// ignore
					}
					else
					{
						legs.add(new Trip.Public(lineDestination.line, lineDestination.destination, departure, arrival, intermediateStops, path, null));
					}
				}

				XmlPullUtil.exit(pp, "ls");

				XmlPullUtil.require(pp, "tcs");

				final List<Fare> fares;

				if (!pp.isEmptyElementTag())
				{
					XmlPullUtil.enter(pp, "tcs");

					fares = new ArrayList<Fare>(2);

					while (XmlPullUtil.test(pp, "tc"))
					{
						XmlPullUtil.enter(pp, "tc");
						// TODO fares
						XmlPullUtil.exit(pp, "tc");
					}

					XmlPullUtil.exit(pp, "tcs");
				}
				else
				{
					fares = null;

					XmlPullUtil.next(pp);
				}

				final Trip trip = new Trip(tripId, firstDepartureLocation, lastArrivalLocation, legs, fares, null, numChanges);
				trips.add(trip);

				XmlPullUtil.exit(pp, "tp");
			}

			XmlPullUtil.exit(pp, "ts");
		}

		if (trips.size() > 0)
		{
			final String[] context = (String[]) header.context;
			return new QueryTripsResult(header, uri, from, via, to, new Context(commandLink(context[0], context[1])), trips);
		}
		else
		{
			return new QueryTripsResult(header, QueryTripsResult.Status.NO_TRIPS);
		}
	}

	private List<Point> processItdPathCoordinates(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, "itdPathCoordinates");

		XmlPullUtil.enter(pp, "coordEllipsoid");
		final String ellipsoid = pp.getText();
		XmlPullUtil.exit(pp, "coordEllipsoid");

		if (!"WGS84".equals(ellipsoid))
			throw new IllegalStateException("unknown ellipsoid: " + ellipsoid);

		XmlPullUtil.enter(pp, "coordType");
		final String type = pp.getText();
		XmlPullUtil.exit(pp, "coordType");

		if (!"GEO_DECIMAL".equals(type))
			throw new IllegalStateException("unknown type: " + type);

		final List<Point> path = processCoordinateStrings(pp, "itdCoordinateString");

		XmlPullUtil.exit(pp, "itdPathCoordinates");

		return path;
	}

	private List<Point> processCoordinateStrings(final XmlPullParser pp, final String tag) throws XmlPullParserException, IOException
	{
		final List<Point> path = new LinkedList<Point>();

		final String value = requireValueTag(pp, tag);
		for (final String coordStr : value.split(" +"))
			path.add(coordStrToPoint(coordStr));

		return path;
	}

	private Point coordStrToPoint(final String coordStr)
	{
		if (coordStr == null)
			return null;

		final String[] parts = coordStr.split(",");
		return new Point(Math.round(Float.parseFloat(parts[1])), Math.round(Float.parseFloat(parts[0])));
	}

	private Fare processItdGenericTicketGroup(final XmlPullParser pp, final String net, final Currency currency) throws XmlPullParserException,
			IOException
	{
		XmlPullUtil.enter(pp, "itdGenericTicketGroup");

		Type type = null;
		float fare = 0;

		while (XmlPullUtil.test(pp, "itdGenericTicket"))
		{
			XmlPullUtil.enter(pp, "itdGenericTicket");

			XmlPullUtil.enter(pp, "ticket");
			final String key = pp.getText().trim();
			XmlPullUtil.exit(pp, "ticket");

			String value = null;
			XmlPullUtil.require(pp, "value");
			if (!pp.isEmptyElementTag())
			{
				XmlPullUtil.enter(pp, "value");
				value = pp.getText();
				if (value != null)
					value = value.trim();
				XmlPullUtil.exit(pp, "value");
			}
			else
			{
				XmlPullUtil.next(pp);
			}

			if (key.equals("FOR_RIDER"))
			{
				final String typeStr = value.split(" ")[0].toUpperCase();
				if (typeStr.equals("REGULAR"))
					type = Type.ADULT;
				else
					type = Type.valueOf(typeStr);
			}
			else if (key.equals("PRICE"))
			{
				fare = Float.parseFloat(value) * (currency.getCurrencyCode().equals("USD") ? 0.01f : 1);
			}

			XmlPullUtil.exit(pp, "itdGenericTicket");
		}

		XmlPullUtil.exit(pp, "itdGenericTicketGroup");

		if (type != null)
			return new Fare(net, type, currency, fare, null, null);
		else
			return null;
	}

	private Currency parseCurrency(final String currencyStr)
	{
		if (currencyStr.equals("US$"))
			return Currency.getInstance("USD");
		if (currencyStr.equals("Dirham"))
			return Currency.getInstance("AED");
		return Currency.getInstance(currencyStr);
	}

	private static final Pattern P_PLATFORM = Pattern.compile("#?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern P_PLATFORM_NAME = Pattern.compile("(?:Gleis|Gl\\.|Bstg\\.)?\\s*" + //
			"(\\d+)\\s*" + //
			"(?:([A-Z])\\s*(?:-\\s*([A-Z]))?)?", Pattern.CASE_INSENSITIVE);

	private static final String normalizePlatform(final String platform, final String platformName)
	{
		if (platform != null && platform.length() > 0)
		{
			final Matcher m = P_PLATFORM.matcher(platform);
			if (m.matches())
			{
				return Integer.toString(Integer.parseInt(m.group(1)));
			}
			else
			{
				return platform;
			}
		}

		if (platformName != null && platformName.length() > 0)
		{
			final Matcher m = P_PLATFORM_NAME.matcher(platformName);
			if (m.matches())
			{
				final String simple = Integer.toString(Integer.parseInt(m.group(1)));
				if (m.group(2) != null && m.group(3) != null)
					return simple + m.group(2) + "-" + m.group(3);
				else if (m.group(2) != null)
					return simple + m.group(2);
				else
					return simple;
			}
			else
			{
				return platformName;
			}
		}

		return null;
	}

	private void appendLocation(final StringBuilder uri, final Location location, final String paramSuffix)
	{
		if (canAcceptPoiId && location.type == LocationType.POI && location.hasId())
		{
			uri.append("&type_").append(paramSuffix).append("=poiID");
			uri.append("&name_").append(paramSuffix).append("=").append(location.id);
		}
		else if ((location.type == LocationType.POI || location.type == LocationType.ADDRESS) && location.hasLocation())
		{
			uri.append("&type_").append(paramSuffix).append("=coord");
			uri.append("&name_").append(paramSuffix).append("=")
					.append(String.format(Locale.ENGLISH, "%.6f:%.6f", location.lon / 1E6, location.lat / 1E6)).append(":WGS84");
		}
		else
		{
			uri.append("&type_").append(paramSuffix).append("=").append(locationTypeValue(location));
			uri.append("&name_").append(paramSuffix).append("=").append(ParserUtils.urlEncode(locationValue(location), requestUrlEncoding));
		}
	}

	protected static final String locationTypeValue(final Location location)
	{
		final LocationType type = location.type;
		if (type == LocationType.STATION)
			return "stop";
		if (type == LocationType.ADDRESS)
			return "any"; // strange, matches with anyObjFilter
		if (type == LocationType.POI)
			return "poi";
		if (type == LocationType.ANY)
			return "any";
		throw new IllegalArgumentException(type.toString());
	}

	protected static final String locationValue(final Location location)
	{
		if ((location.type == LocationType.STATION || location.type == LocationType.POI) && location.hasId())
			return Integer.toString(location.id);
		else
			return location.name;
	}

	protected static final Map<WalkSpeed, String> WALKSPEED_MAP = new HashMap<WalkSpeed, String>();

	static
	{
		WALKSPEED_MAP.put(WalkSpeed.SLOW, "slow");
		WALKSPEED_MAP.put(WalkSpeed.NORMAL, "normal");
		WALKSPEED_MAP.put(WalkSpeed.FAST, "fast");
	}

	private ResultHeader enterItdRequest(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() != XmlPullParser.START_DOCUMENT)
			throw new IllegalStateException("start of document expected");

		try
		{
			pp.next();
		}
		catch (final XmlPullParserException x)
		{
			if (x.getMessage().startsWith("Expected a quoted string"))
				throw new ProtocolException("html");
		}

		if (pp.getEventType() == XmlPullParser.DOCDECL)
			pp.next();

		if (XmlPullUtil.test(pp, "html"))
			throw new ProtocolException("html");

		XmlPullUtil.require(pp, "itdRequest");

		final String serverVersion = XmlPullUtil.attr(pp, "version");
		final String now = XmlPullUtil.attr(pp, "now");
		final String sessionId = XmlPullUtil.attr(pp, "sessionID");

		final Calendar serverTime = new GregorianCalendar(timeZone());
		ParserUtils.parseIsoDate(serverTime, now.substring(0, 10));
		ParserUtils.parseEuropeanTime(serverTime, now.substring(11));

		final ResultHeader header = new ResultHeader(SERVER_PRODUCT, serverVersion, serverTime.getTimeInMillis(), sessionId);

		XmlPullUtil.enter(pp, "itdRequest");

		if (XmlPullUtil.test(pp, "clientHeaderLines"))
			XmlPullUtil.next(pp);

		if (XmlPullUtil.test(pp, "itdVersionInfo"))
			XmlPullUtil.next(pp);

		if (XmlPullUtil.test(pp, "itdInfoLinkList"))
			XmlPullUtil.next(pp);

		if (XmlPullUtil.test(pp, "serverMetaInfo"))
			XmlPullUtil.next(pp);

		return header;
	}

	private ResultHeader enterEfa(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		if (pp.getEventType() != XmlPullParser.START_DOCUMENT)
			throw new IllegalStateException("start of document expected");

		pp.next();

		XmlPullUtil.enter(pp, "efa");

		XmlPullUtil.enter(pp, "now");
		final String now = pp.getText();
		final Calendar serverTime = new GregorianCalendar(timeZone());
		ParserUtils.parseIsoDate(serverTime, now.substring(0, 10));
		ParserUtils.parseEuropeanTime(serverTime, now.substring(11));
		XmlPullUtil.exit(pp, "now");

		final Map<String, String> params = processPas(pp);
		final String sessionId = params.get("sessionID");
		final String requestId = params.get("requestID");

		final ResultHeader header = new ResultHeader(SERVER_PRODUCT, null, serverTime.getTimeInMillis(), new String[] { sessionId, requestId });

		return header;
	}

	private Map<String, String> processPas(final XmlPullParser pp) throws XmlPullParserException, IOException
	{
		final Map<String, String> params = new HashMap<String, String>();

		XmlPullUtil.enter(pp, "pas");

		while (XmlPullUtil.test(pp, "pa"))
		{
			XmlPullUtil.enter(pp, "pa");
			final String name = requireValueTag(pp, "n");
			final String value = requireValueTag(pp, "v");
			params.put(name, value);
			XmlPullUtil.exit(pp, "pa");
		}

		XmlPullUtil.exit(pp, "pas");

		return params;
	}

	private String optValueTag(final XmlPullParser pp, final String name) throws XmlPullParserException, IOException
	{
		if (XmlPullUtil.test(pp, name))
		{
			if (!pp.isEmptyElementTag())
			{
				return requireValueTag(pp, name);
			}
			else
			{
				pp.next();
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	private String requireValueTag(final XmlPullParser pp, final String name) throws XmlPullParserException, IOException
	{
		XmlPullUtil.enter(pp, name);
		final String value = pp.getText();
		XmlPullUtil.exit(pp, name);

		return value != null ? value.trim() : null;
	}
}
