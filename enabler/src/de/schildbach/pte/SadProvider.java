package de.schildbach.pte;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Connection.Footway;
import de.schildbach.pte.dto.Connection.Part;
import de.schildbach.pte.dto.Connection.Trip;
import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Fare.Type;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryConnectionsContext;
import de.schildbach.pte.dto.QueryConnectionsResult;
import de.schildbach.pte.dto.QueryConnectionsResult.Status;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.Style;

public class SadProvider extends AbstractNetworkProvider {

	public static final NetworkId NETWORK_ID = NetworkId.SAD;
	private static final String API_BASE = "http://timetables.sad.it/SIITimetablesMobile.php";
	private static final String SERVER_PRODUCT = "SOAP";
	private static final ResultHeader RESULT_HEADER = new ResultHeader(SERVER_PRODUCT);
	private static final int SOAP_VERSION = SoapEnvelope.VER11;
	private static final SimpleDateFormat RESPONSE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
	private static final SimpleDateFormat REQUEST_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
	private static final SimpleDateFormat VALIDITY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-ddZ", Locale.US);
	private static final Style DEFAULT_STYLE = new Style(Style.parseColor("#0000cc"), Style.WHITE);
	private static final int HOURS_AFTER_START = 5;

	// Specifies in what language e.g. strings for station names are returned
	private static final Language LANGUAGE = Language.GERMAN;

	// Languages supplied by SOAP API (sometimes also English or Ladin, but not consistently)
	private enum Language {
		GERMAN, ITALIAN
	}

	public static class Context implements QueryConnectionsContext
	{
		public final String context;

		public Context(final String context)
		{
			this.context = context;
		}

		public boolean canQueryLater()
		{
			return context != null;
		}

		public boolean canQueryEarlier()
		{
			return context != null;
		}
	}

	public NetworkId id() {
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities) {
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	public NearbyStationsResult queryNearbyStations(Location location, int maxDistance, int maxStations) throws IOException {
		// Not supported by SOAP API
		throw new UnsupportedOperationException();
	}

	public QueryDeparturesResult queryDepartures(int stationId, int maxDepartures, boolean equivs) throws IOException {
		// Not supported by SOAP API
		throw new UnsupportedOperationException();
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException {

		// Execute searchNodo SOAP request to get locations corresponding to contraint
		SoapObject response = executeSoap("searchNodo", new Object[] { "searchstring", constraint.toString() });

		ArrayList<Location> list = new ArrayList<Location>();

		// Go through all received locations
		for (int i = 0; i < response.getPropertyCount(); i++) {
			Object property = response.getProperty(i);
			if (property instanceof SoapObject) {
				// Add location to list
				list.add(soapToLocation((SoapObject) property));
			}
		}

		return list;
	}
	
	public QueryConnectionsResult queryConnections(Location from, Location via, Location to, Date date, boolean dep, final int numConnections, String products,
			WalkSpeed walkSpeed, Accessibility accessibility, Set<Option> options) throws IOException {

		// Select correct SOAP method depending on the dep flag
		final String soapMethod = dep ? "searchCollPartenza" : "searchCollArrivo";

		// Create new calendar and put date object into it
		final Calendar cal = new GregorianCalendar(timeZone());
		cal.setTime(date);
		int hours = cal.get(Calendar.HOUR_OF_DAY);
		int minutes = cal.get(Calendar.MINUTE);

		// Calculate ending time depending on the starting time, don't go over day boundaries 
		final String writtenDate = REQUEST_DATE_FORMAT.format(date);
		final String timeMin = hours + ":" + minutes;
		final String timeMax = hours < 24 - HOURS_AFTER_START ? (hours + HOURS_AFTER_START) + ":" + minutes : "23:59";

		// Check if the date is valid by querying the SOAP service
		Status validityStatus = checkDateValidity(date);
		if (validityStatus == Status.SERVICE_DOWN) {
			return new QueryConnectionsResult(RESULT_HEADER, Status.SERVICE_DOWN);
		} else if (validityStatus == Status.INVALID_DATE) {
			return new QueryConnectionsResult(RESULT_HEADER, Status.INVALID_DATE);
		}

		// From and/or to locations have no ID -> use autocomplete metho
		if (!from.hasId() || !to.hasId()) {

			List<Location> froms = Arrays.asList(from), tos = Arrays.asList(to);

			// Get ID(s) from SOAP service corresponding to from's location name
			if (!from.hasId()) {
				froms = autocompleteStations(from.name);
				if (froms.isEmpty()) {
					return new QueryConnectionsResult(RESULT_HEADER, Status.UNKNOWN_FROM);
				}
				// Exactly one match
				else if (froms.size() == 1) {
					from = froms.get(0);
				}
			}
			// Get ID(s) from SOAP service corresponding to to's location name
			if (!to.hasId()) {
				tos = autocompleteStations(to.name);
				if (tos.isEmpty()) {
					return new QueryConnectionsResult(RESULT_HEADER, Status.UNKNOWN_TO);
				}
				// Exactly one match
				else if (tos.size() == 1) {
					to = tos.get(0);
				}
			}

			// Check for ambiguities in which case an ambiguous result is returned 
			if ((froms != null && froms.size() > 1) || (tos != null && tos.size() > 1)) {
				return new QueryConnectionsResult(RESULT_HEADER, froms, null, tos);
			}
		}

		// Check if from and to locations are equal
		if (from.id == to.id) {
			return new QueryConnectionsResult(RESULT_HEADER, Status.TOO_CLOSE);
		}

		// Execute SOAP request to get list of possible connections
		SoapObject response = executeSoap(soapMethod, new Object[] { "partenza", from.id + "", "arrivo", to.id + "", "giorno", writtenDate,
				"orario_min", timeMin, "orario_max", timeMax });

		// Generate a valid response object with the SoapObject obtained from the SOAP service
		return calculateResponse(from, to, response, dep, date);
	}

	public QueryConnectionsResult queryMoreConnections(final QueryConnectionsContext contextObj, final boolean later, final int numConnections) throws IOException
	{
		// Split and parse context
		final Context context = (Context) contextObj;
		final String commandUri = context.context;
		final String[] split = commandUri.split(",");
		if (split.length != 4) {
			return null;
		}
		final int fromId = Integer.parseInt(split[0]);
		final int toId = Integer.parseInt(split[1]);
		final boolean dep = Boolean.parseBoolean(split[2]);
		Date date = null;
		try {
			date = RESPONSE_DATE_FORMAT.parse(split[3]);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		final Calendar cal = new GregorianCalendar(timeZone());
		cal.setTime(date);

		// Calculate new Date, depending on the specified next flag
		cal.add(Calendar.HOUR_OF_DAY, later ? HOURS_AFTER_START : -HOURS_AFTER_START);
		date = cal.getTime();

		// Query for connections with new date/time value
		// NOTE: via, products, walkSpeed, accessibility are set to null
		return queryConnections(new Location(LocationType.STATION, fromId), null, new Location(LocationType.STATION, toId), date, dep,
				0, null, null, null, null);
	}

	protected TimeZone timeZone() {
		// Set to Italian time zone
		return TimeZone.getTimeZone("Europe/Rome");
	}

	/**
	 * Utility function to parse a SoapObject into a Location.
	 * @param nodo the SoapObject to convert
	 * @return the location converted from the SoapObject
	 */
	private Location soapToLocation(SoapObject nodo) {
		
		// Parse SoapObject's properties and create a Location object
		int id = Integer.parseInt(nodo.getPropertyAsString("id"));
		String name;
		if (LANGUAGE == Language.GERMAN) {
			name = (String) nodo.getPropertyAsString("nome_de");
		} else {
			name = (String) nodo.getPropertyAsString("nome_it");
		}
		// NOTE: place is set to null
		return new Location(LocationType.STATION, id, null, name);
	}
	
	/**
	 * Utility function to parse a SoapObject into a list of Date objects.
	 * @param dateObject the SoapObject which is to be parsed into dates
	 * @param propertyNames the names to look for in the SoapObject
	 * @param format the date format which is used in the SoapObject
	 * @return a list of Date Objects representing the input dateObject.
	 * @throws ParseException
	 */
	private List<Date> soapToDate(SoapObject dateObject, String[] propertyNames, SimpleDateFormat format) throws ParseException {

		List<Date> returnDate = new ArrayList<Date>();

		// Go through all property names
		for (String s : propertyNames) {
			// Get property value as a string
			StringBuilder sb = new StringBuilder(dateObject.getPropertyAsString(s));
			// Remove ':' to correspond to valid time zone format, e.g. ...+02:00 -> ...+0200
			sb.deleteCharAt(sb.length() - 3);
			// Parse the date according to the supplied format and add it to the list
			returnDate.add(format.parse(sb.toString()));
		}

		return returnDate;
	}

	/**
	 * Utility function to execute a SOAP request. 
	 * @param soapFunction the SOAP function's name which should be called
	 * @param soapProperties the parameters which should be supplied to the function,
	 * even indexes represent the parameter name and odd indexes represent the parameter value  
	 * @return the SoapObject which gets returned upon executing the SOAP request
	 * @throws IOException
	 */
	private SoapObject executeSoap(final String soapFunction, final Object[] soapProperties) throws IOException {
		
		// Construct SoapObject for making the request by supplying the API URL and the function name
		SoapObject request = new SoapObject(API_BASE, soapFunction);

		// Add parameters to constructed SoapObject
		for (int i = 0; i < soapProperties.length; i += 2) {
			request.addPropertyIfValue(soapProperties[i].toString(), soapProperties[i + 1].toString());
		}

		// Create a SOAP envelope around the SoapObject
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SOAP_VERSION);
		envelope.setOutputSoapObject(request);

		// Execute the SOAP request via HTTP
		HttpTransportSE transport = new HttpTransportSE(API_BASE);
		try {
			transport.call(API_BASE + "#" + soapFunction, envelope);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		// Return the received response SoapObject
		return (SoapObject) envelope.getResponse();
	}

	/**
	 * Utility function which makes a SOAP request to check if the supplied date is in the valid range.
	 * @param date the date whose validity is to be checked
	 * @return if the supplied date is valid
	 * @throws IOException
	 */
	private Status checkDateValidity(Date date) throws IOException {
		
		// Execute the getValidita SOAP method
		SoapObject validityResponse = executeSoap("getValidita", new Object[] {});

		// Parse the received string into a Date object
		List<Date> startEnd = null;
		try {
			startEnd = soapToDate(validityResponse, new String[] { "inizio", "fine" }, VALIDITY_DATE_FORMAT);
			// Check if exactly two dates (start and end) are parsed
			if (startEnd.size() != 2) {
				throw new ParseException("Expected 2 dates for start and end of service but got " + startEnd.size(), 0);
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
			return Status.SERVICE_DOWN;
		}

		// Check if the supplied date is within the valid range
		if (date.before(startEnd.get(0)) || date.after(startEnd.get(1))) {
			return Status.INVALID_DATE;
		} else {
			return Status.OK;
		}
	}

	// Different fare types and their names
	private static final Map<String, String> FARE_TYPES = new HashMap<String, String>();

	static {
		FARE_TYPES.put("aapass_1", "Südtirol Pass < 1000 km");
		FARE_TYPES.put("aapass_2", "Südtirol Pass < 10000 km");
		FARE_TYPES.put("aapass_3", "Südtirol Pass < 20000 km");
		FARE_TYPES.put("aapass_4", "Südtirol Pass > 20000 km");
		FARE_TYPES.put("aapass_fam_1", "Südtirol Pass family < 1000 km");
		FARE_TYPES.put("aapass_fam_2", "Südtirol Pass family < 10000 km");
		FARE_TYPES.put("aapass_fam_3", "Südtirol Pass family < 20000 km");
		FARE_TYPES.put("aapass_fam_4", "Südtirol Pass family > 20000 km");
		FARE_TYPES.put("carta_valore", "Wertkarte");
		FARE_TYPES.put("corsa_singola", "Einzelfahrtkarte");
	}

	/**
	 * Utility function to create a QueryConnectionsResult result object from the supplied parameters.
	 * @param from the request's from location 
	 * @param to the request's to location
	 * @param response the SoapObject which was received from the service as response
	 * @param dep is the date referenced to departure (or arrival) 
	 * @param date the request's date
	 * @return
	 */
	private QueryConnectionsResult calculateResponse(Location from, Location to, SoapObject response, boolean dep, Date date) {
		
		// If no result was found return immediately
		if (response.getPropertyCount() == 0) {
			return new QueryConnectionsResult(RESULT_HEADER, Status.NO_CONNECTIONS);
		}

		// Lists to store the connections and from and to locations
		List<Connection> connections = new ArrayList<Connection>();
		List<Location> fromToLocs = new ArrayList<Location>();

		// Go through all properties of the response's SoapObject
		for (int i = 0; i < response.getPropertyCount(); i++) {
			Object property = response.getProperty(i);
			if (property instanceof SoapObject) {
				SoapObject connection = (SoapObject) property;

				// Get departure and arrival locations for current connection
				fromToLocs.clear();
				for (String prop : new String[] { "nodo_partenza", "nodo_arrivo" }) {
					Object temp = connection.getProperty(prop);
					if (temp instanceof SoapObject) {
						fromToLocs.add(soapToLocation((SoapObject) temp));
					}
				}

				// Get parts of the current connection
				List<Part> parts = new ArrayList<Part>();
				Object temp = connection.getProperty("tratti");
				String networkName = null;
				if (temp instanceof SoapObject) {
					SoapObject tratti = (SoapObject) temp;
					// Go through all connection parts
					for (int j = 0; j < tratti.getPropertyCount(); j++) {
						boolean isFootway = false;
						SoapObject tratto = (SoapObject) tratti.getProperty(j);

						SoapObject conc = (SoapObject) tratto.getProperty("concessionario");
						if (conc != null) {
							// Check if current track is footway (id = 9999)
							if (Integer.parseInt(conc.getPropertyAsString("id")) == 9999) {
								isFootway = true;
							}
							// Use non-footway name as network name
							else if (networkName == null) {
								networkName = conc.getPropertyAsString(LANGUAGE == Language.GERMAN ? "nome_de" : "nome_it");
							}
						}

						// Add footway to parts list
						if (isFootway) {
							// NOTE: path is set to null
							parts.add(new Footway(Integer.parseInt(tratto.getPropertyAsString("durata").split(":")[1]),
									soapToLocation((SoapObject) tratto.getProperty("nodo_partenza")), soapToLocation((SoapObject) tratto
											.getProperty("nodo_arrivo")), null));
						}

						// Add trip to parts list
						else {
							// Get line ID
							String lineId = tratto.getPropertyAsString("linea");
							try {
								// Parse date from response SoapObject 
								List<Date> responseDate = soapToDate(tratto, new String[] { "ora_partenza", "ora_arrivo" },
										RESPONSE_DATE_FORMAT);
								// NOTE: multiple null values: destination,
								// predictedDepartureTime, departurePosition,
								// predictedArrivalTime, arrivalPosition,
								// intermediateStops, path
								parts.add(new Trip(new Line(lineId, lineId, DEFAULT_STYLE), null, responseDate.get(0), null, null,
										soapToLocation((SoapObject) tratto.getProperty("nodo_partenza")), responseDate.get(1), null, null,
										soapToLocation((SoapObject) tratto.getProperty("nodo_arrivo")), null, null));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// Get fares for the current connection
				ArrayList<Fare> fares = new ArrayList<Fare>();
				temp = connection.getProperty("tariffazione_trasporto_integrato");
				if (temp instanceof SoapObject) {
					SoapObject tariffTraspIntegr = (SoapObject) temp;
					// Check if tariff information is supplied in the response
					if (tariffTraspIntegr.hasProperty("aapass_1")) {
						Currency curr = Currency.getInstance("EUR");
						// Go through all fare types
						for (String tariff : FARE_TYPES.keySet()) {
							int cents = Integer.parseInt(tariffTraspIntegr.getPropertyAsString(tariff));
							// NOTE: units is set to null
							fares.add(new Fare(networkName, Type.ADULT, curr, ((float) cents) / 100f, FARE_TYPES.get(tariff), null));
						}
					}
				}

				// Only add to connections list if exactly one to and and one from location were found
				if (fromToLocs.size() == 2) {
					// NOTE: link, capacity set to null
					connections.add(new Connection(fromToLocs.get(0).toString() + fromToLocs.get(1).toString(), null, fromToLocs.get(0),
							fromToLocs.get(1), parts, fares, null, null));
				}
			}
		}

		// Construct query URI to be used as context for queryMoreConnections()
		final String queryUri = from.id + "," + to.id + "," + dep + "," + RESPONSE_DATE_FORMAT.format(date);

		// NOTE: via is set to null
		return new QueryConnectionsResult(RESULT_HEADER, queryUri, from, null, to, new Context(queryUri), connections);
	}
}
