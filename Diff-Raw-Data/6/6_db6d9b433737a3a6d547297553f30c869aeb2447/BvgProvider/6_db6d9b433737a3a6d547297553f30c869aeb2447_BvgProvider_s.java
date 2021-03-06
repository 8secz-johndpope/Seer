 /*
  * Copyright 2010, 2011 the original author or authors.
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
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import de.schildbach.pte.dto.Connection;
 import de.schildbach.pte.dto.Departure;
 import de.schildbach.pte.dto.GetConnectionDetailsResult;
 import de.schildbach.pte.dto.Line;
 import de.schildbach.pte.dto.Location;
 import de.schildbach.pte.dto.LocationType;
 import de.schildbach.pte.dto.NearbyStationsResult;
 import de.schildbach.pte.dto.QueryConnectionsResult;
 import de.schildbach.pte.dto.QueryDeparturesResult;
 import de.schildbach.pte.dto.QueryDeparturesResult.Status;
 import de.schildbach.pte.dto.StationDepartures;
 import de.schildbach.pte.dto.Stop;
 import de.schildbach.pte.exception.SessionExpiredException;
 import de.schildbach.pte.util.Color;
 import de.schildbach.pte.util.ParserUtils;
 
 /**
  * @author Andreas Schildbach
  */
 public final class BvgProvider extends AbstractHafasProvider
 {
 	public static final NetworkId NETWORK_ID = NetworkId.BVG;
 	public static final String OLD_NETWORK_ID = "mobil.bvg.de";
 
 	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;
 
 	private static final String BASE_URL = "http://mobil.bvg.de";
 	private static final String API_BASE = BASE_URL + "/Fahrinfo/bin/";
 
 	public BvgProvider()
 	{
 		super(null, null);
 	}
 
 	public NetworkId id()
 	{
 		return NETWORK_ID;
 	}
 
 	public boolean hasCapabilities(final Capability... capabilities)
 	{
 		for (final Capability capability : capabilities)
 			if (capability == Capability.NEARBY_STATIONS)
 				return false;
 
 		return true;
 	}
 
 	private static final String AUTOCOMPLETE_NAME_URL = API_BASE + "stboard.bin/dox/dox?input=%s";
 	private static final Pattern P_SINGLE_NAME = Pattern.compile(".*?Haltestelleninfo.*?<strong>(.*?)</strong>.*?input=(\\d+)&.*?", Pattern.DOTALL);
 	private static final Pattern P_MULTI_NAME = Pattern.compile("<a href=\\\"/Fahrinfo/bin/stboard\\.bin/dox.*?input=(\\d+)&.*?\">\\s*(.*?)\\s*</a>",
 			Pattern.DOTALL);
 
 	@Override
 	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
 	{
 		final List<Location> results = new ArrayList<Location>();
 
 		final String uri = String.format(AUTOCOMPLETE_NAME_URL, ParserUtils.urlEncode(constraint.toString()));
 		final CharSequence page = ParserUtils.scrape(uri);
 
 		final Matcher mSingle = P_SINGLE_NAME.matcher(page);
 		if (mSingle.matches())
 		{
 			results.add(new Location(LocationType.STATION, Integer.parseInt(mSingle.group(2)), null, ParserUtils.resolveEntities(mSingle.group(1))));
 		}
 		else
 		{
 			final Matcher mMulti = P_MULTI_NAME.matcher(page);
 			while (mMulti.find())
 				results.add(new Location(LocationType.STATION, Integer.parseInt(mMulti.group(1)), null, ParserUtils.resolveEntities(mMulti.group(2))));
 		}
 
 		return results;
 	}
 
 	private final String NEARBY_URI = API_BASE + "stboard.bin/dn?distance=50&near&input=%s";
 
 	@Override
 	protected String nearbyStationUri(final String stationId)
 	{
 		return String.format(NEARBY_URI, ParserUtils.urlEncode(stationId));
 	}
 
 	private final static Pattern P_NEARBY_OWN = Pattern
 			.compile("/Stadtplan/index.*?location=(\\d+),HST,WGS84,(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)&amp;label=([^\"]*)\"");
 	private final static Pattern P_NEARBY_PAGE = Pattern.compile("<table class=\"ivuTableOverview\".*?<tbody>(.*?)</tbody>", Pattern.DOTALL);
 	private final static Pattern P_NEARBY_COARSE = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
 	private final static Pattern P_NEARBY_FINE_LOCATION = Pattern.compile("input=(\\d+)&[^\"]*\">([^<]*)<");
 	private static final Pattern P_NEARBY_ERRORS = Pattern.compile("(derzeit leider nicht bearbeitet werden)");
 
 	@Override
 	public NearbyStationsResult nearbyStations(final String stationId, final int lat, final int lon, final int maxDistance, final int maxStations)
 			throws IOException
 	{
 		if (stationId == null)
 			throw new IllegalArgumentException("stationId must be given");
 
 		final List<Location> stations = new ArrayList<Location>();
 
 		final String uri = nearbyStationUri(stationId);
 		final CharSequence page = ParserUtils.scrape(uri);
 
 		final Matcher mError = P_NEARBY_ERRORS.matcher(page);
 		if (mError.find())
 		{
 			if (mError.group(1) != null)
 				return new NearbyStationsResult(NearbyStationsResult.Status.INVALID_STATION);
 		}
 
 		final Matcher mOwn = P_NEARBY_OWN.matcher(page);
 		if (mOwn.find())
 		{
 			final int parsedId = Integer.parseInt(mOwn.group(1));
 			final int parsedLon = (int) (Float.parseFloat(mOwn.group(2)) * 1E6);
 			final int parsedLat = (int) (Float.parseFloat(mOwn.group(3)) * 1E6);
 			final String[] parsedPlaceAndName = splitNameAndPlace(ParserUtils.urlDecode(mOwn.group(4), "ISO-8859-1"));
 			stations.add(new Location(LocationType.STATION, parsedId, parsedLat, parsedLon, parsedPlaceAndName[0], parsedPlaceAndName[1]));
 		}
 
 		final Matcher mPage = P_NEARBY_PAGE.matcher(page);
 		if (mPage.find())
 		{
 			final Matcher mCoarse = P_NEARBY_COARSE.matcher(mPage.group(1));
 
 			while (mCoarse.find())
 			{
 				final Matcher mFineLocation = P_NEARBY_FINE_LOCATION.matcher(mCoarse.group(1));
 
 				if (mFineLocation.find())
 				{
 					final int parsedId = Integer.parseInt(mFineLocation.group(1));
 					final String[] parsedPlaceAndName = splitNameAndPlace(ParserUtils.resolveEntities(mFineLocation.group(2)));
 					final Location station = new Location(LocationType.STATION, parsedId, parsedPlaceAndName[0], parsedPlaceAndName[1]);
 					if (!stations.contains(station))
 						stations.add(station);
 				}
 				else
 				{
 					throw new IllegalArgumentException("cannot parse '" + mCoarse.group(1) + "' on " + uri);
 				}
 			}
 
 			if (maxStations == 0 || maxStations >= stations.size())
 				return new NearbyStationsResult(stations);
 			else
 				return new NearbyStationsResult(stations.subList(0, maxStations));
 		}
 		else
 		{
 			throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
 		}
 	}
 
 	@Override
 	protected String[] splitNameAndPlace(final String name)
 	{
 		if (name.endsWith(" (Berlin)"))
 			return new String[] { "Berlin", name.substring(0, name.length() - 9) };
 		else if (name.startsWith("Potsdam, "))
 			return new String[] { "Potsdam", name.substring(9) };
 		else if (name.startsWith("Cottbus, "))
 			return new String[] { "Cottbus", name.substring(9) };
 		else if (name.startsWith("Brandenburg, "))
 			return new String[] { "Brandenburg", name.substring(13) };
 		else if (name.startsWith("Frankfurt (Oder), "))
 			return new String[] { "Frankfurt (Oder)", name.substring(18) };
 
 		return super.splitNameAndPlace(name);
 	}
 
 	private String connectionsQueryUri(final Location from, final Location via, final Location to, final Date date, final boolean dep,
 			final String products)
 	{
 		final Calendar c = new GregorianCalendar(timeZone());
 		c.setTime(date);
 
 		final StringBuilder uri = new StringBuilder();
 
 		uri.append(API_BASE).append("query.bin/dn");
 
 		uri.append("?start=Suchen");
 
 		appendLocationBvg(uri, from, "S0");
 		appendLocationBvg(uri, to, "Z0");
 		if (via != null)
 			appendLocationBvg(uri, via, "1.0");
 
 		uri.append("&REQ0HafasSearchForw=").append(dep ? "1" : "0");
 		uri.append("&REQ0JourneyDate=").append(
 				String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) - 2000));
 		uri.append("&REQ0JourneyTime=").append(String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
 
 		for (final char p : products.toCharArray())
 		{
 			if (p == 'I')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_5=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_5=1");
 			}
 			if (p == 'R')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_6=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_6=1");
 			}
 			if (p == 'S')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_0=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_0=1");
 			}
 			if (p == 'U')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_1=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_1=1");
 			}
 			if (p == 'T')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_2=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_2=1");
 			}
 			if (p == 'B')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_3=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_3=1");
 			}
 			if (p == 'F')
 			{
 				uri.append("&REQ0JourneyProduct_prod_section_0_4=1");
 				if (via != null)
 					uri.append("&REQ0JourneyProduct_prod_section_1_4=1");
 			}
 			// FIXME if (p == 'C')
 			// TODO Ruftaxi wäre wohl &REQ0JourneyProduct_prod_section_0_7=1
 		}
 
 		return uri.toString();
 	}
 
 	private static final void appendLocationBvg(final StringBuilder uri, final Location location, final String paramSuffix)
 	{
 		uri.append("&REQ0JourneyStops").append(paramSuffix).append("A=").append(locationTypeValue(location));
 
 		if (location.type == LocationType.STATION && location.hasId() && location.id >= 1000000)
 			uri.append("&REQ0JourneyStops").append(paramSuffix).append("L=").append(location.id);
 
 		if (location.hasLocation())
 		{
 			uri.append("&REQ0JourneyStops").append(paramSuffix).append("X=").append(location.lon);
 			uri.append("&REQ0JourneyStops").append(paramSuffix).append("Y=").append(location.lat);
 		}
 
 		if (location.name != null)
 			uri.append("&REQ0JourneyStops").append(paramSuffix).append("G=").append(ParserUtils.urlEncode(location.name));
 	}
 
 	private static final int locationTypeValue(final Location location)
 	{
 		final LocationType type = location.type;
 		if (type == LocationType.STATION)
 			return 1;
 		if (type == LocationType.ADDRESS)
 			return 2;
 		if (type == LocationType.ANY)
 			return 255;
 		throw new IllegalArgumentException(type.toString());
 	}
 
 	private static final Pattern P_PRE_ADDRESS = Pattern.compile(
 			"<select[^>]*name=\"(REQ0JourneyStopsS0K|REQ0JourneyStopsZ0K|REQ0JourneyStops1\\.0K)\"[^>]*>\n(.*?)</select>", Pattern.DOTALL);
 	private static final Pattern P_ADDRESSES = Pattern.compile("<option[^>]*>\\s*(.*?)\\s*</option>", Pattern.DOTALL);
 
 	@Override
 	public QueryConnectionsResult queryConnections(final Location from, final Location via, final Location to, final Date date, final boolean dep,
 			final String products, final WalkSpeed walkSpeed) throws IOException
 	{
 		final String uri = connectionsQueryUri(from, via, to, date, dep, products);
 		final CharSequence page = ParserUtils.scrape(uri);
 
 		List<Location> fromAddresses = null;
 		List<Location> viaAddresses = null;
 		List<Location> toAddresses = null;
 
 		final Matcher mPreAddress = P_PRE_ADDRESS.matcher(page);
 		while (mPreAddress.find())
 		{
 			final String type = mPreAddress.group(1);
 			final String options = mPreAddress.group(2);
 
 			final Matcher mAddresses = P_ADDRESSES.matcher(options);
 			final List<Location> addresses = new ArrayList<Location>();
 			while (mAddresses.find())
 			{
 				final String address = ParserUtils.resolveEntities(mAddresses.group(1)).trim();
 				if (!addresses.contains(address))
 					addresses.add(new Location(LocationType.ANY, 0, null, address + "!"));
 			}
 
 			if (type.equals("REQ0JourneyStopsS0K"))
 				fromAddresses = addresses;
 			else if (type.equals("REQ0JourneyStopsZ0K"))
 				toAddresses = addresses;
 			else if (type.equals("REQ0JourneyStops1.0K"))
 				viaAddresses = addresses;
 			else
 				throw new IllegalStateException(type);
 		}
 
 		if (fromAddresses != null || viaAddresses != null || toAddresses != null)
 			return new QueryConnectionsResult(fromAddresses, viaAddresses, toAddresses);
 		else
 			return queryConnections(uri, page);
 	}
 
 	@Override
 	public QueryConnectionsResult queryMoreConnections(final String uri) throws IOException
 	{
 		final CharSequence page = ParserUtils.scrape(uri);
 		return queryConnections(uri, page);
 	}
 
 	private Location location(final String typeStr, final String idStr, final String latStr, final String lonStr, final String nameStr)
 	{
 		final int id = idStr != null ? Integer.parseInt(idStr) : 0;
 		final int lat = latStr != null ? (int) (Float.parseFloat(latStr) * 1E6) : 0;
 		final int lon = lonStr != null ? (int) (Float.parseFloat(lonStr) * 1E6) : 0;
 		final String[] placeAndName = splitNameAndPlace(nameStr);
 
 		final LocationType type;
 		if (typeStr == null)
 			type = LocationType.ANY;
 		else if ("HST".equals(typeStr))
 			type = LocationType.STATION;
 		else if ("ADR".equals(typeStr))
 			type = LocationType.ADDRESS;
 		else
 			throw new IllegalArgumentException("cannot handle: " + typeStr);
 
 		return new Location(type, id, lat, lon, placeAndName[0], placeAndName[1]);
 	}
 
 	private Location location(final String[] track)
 	{
 		final int id = track[4].length() > 0 ? Integer.parseInt(track[4]) : 0;
 		final int lat = Integer.parseInt(track[6]);
 		final int lon = Integer.parseInt(track[5]);
 		final String[] placeAndName = splitNameAndPlace(ParserUtils.resolveEntities(track[9]));
 		final String typeStr = track[1];
 
 		final LocationType type;
 		if ("STATION".equals(typeStr))
 			type = LocationType.STATION;
 		else if ("ADDRESS".equals(typeStr))
 			type = LocationType.ADDRESS;
 		else
 			throw new IllegalArgumentException("cannot handle: " + Arrays.toString(track));
 
 		return new Location(type, id, lat, lon, placeAndName[0], placeAndName[1]);
 	}
 
 	private static final Pattern P_CONNECTIONS_ALL_DETAILS = Pattern.compile("<a href=\"([^\"]*)\"[^>]*>Details f&uuml;r alle</a>");
 	private static final Pattern P_CONNECTIONS_HEAD = Pattern.compile(".*?" //
 			+ "<td headers=\"ivuAnfFrom\"[^>]*>\n" //
 			+ "([^\n]*)\n" // from name
 			+ "<a href=\"[^\"]*location=(?:(\\d+)|),(?:(\\w+)|),WGS84,(\\d+\\.\\d+),(\\d+\\.\\d+)&.*?" // from id, lat,
 																										// lon
 			+ "(?:<td headers=\"ivuAnfVia1\"[^>]*>\n" //
 			+ "([^\n]*)<.*?)?" // via name
 			+ "<td headers=\"ivuAnfTo\"[^>]*>\n" //
 			+ "([^\n]*)\n" // to name
 			+ "<a href=\"[^\"]*location=(?:(\\d+)|),(?:(\\w+)|),WGS84,(\\d+\\.\\d+),(\\d+\\.\\d+)&.*?" // to id, lat,
 																										// lon
 			+ "<td headers=\"ivuAnfTime\"[^>]*>.., (\\d{2}\\.\\d{2}\\.\\d{2}) \\d{1,2}:\\d{2}</td>.*?" // date
 			+ "(?:<a href=\"([^\"]*)\" title=\"fr&uuml;here Verbindungen\"[^>]*?>.*?)?" // linkEarlier
 			+ "(?:<a href=\"([^\"]*)\" title=\"sp&auml;tere Verbindungen\"[^>]*?>.*?)?" // linkLater
 	, Pattern.DOTALL);
 	private static final Pattern P_CONNECTIONS_COARSE = Pattern
 			.compile("<form [^>]*name=\"ivuTrackListForm(\\d)\"[^>]*>(.+?)</form>", Pattern.DOTALL);
 	private static final Pattern P_CONNECTIONS_FINE = Pattern.compile(".*?" //
 			+ "Verbindungen - Detailansicht - Abfahrt: am (\\d{2}\\.\\d{2}\\.\\d{2}) um \\d{1,2}:\\d{2}.*?" // date
 			+ "guiVCtrl_connection_detailsOut_setStatus_([^_]+)_allHalts=yes.*?" // id
 			+ "<input type=\"hidden\" name=\"fitrack\" value=\"\\*([^\"]*)\" />" // track
 			+ ".*?", Pattern.DOTALL);
 	private static final Pattern P_CONNECTION_DETAILS = Pattern.compile("" //
 			+ "<td[^>]*headers=\"hafasDTL\\d+_Platform\"[^>]*>\n\\s*([^\\s\n]*?)\\s*\n</td>.*?" // departure platform
 			+ "(?:\nRichtung: ([^\n]*)\n.*?)?" // destination
 			+ "<td[^>]*headers=\"hafasDTL\\d+_Platform\"[^>]*>\n\\s*([^\\s\n]*?)\\s*\n</td>" // arrival platform
 	, Pattern.DOTALL);
 
 	private static final Pattern P_CHECK_CONNECTIONS_ERROR = Pattern.compile("(zu dicht beieinander|mehrfach vorhanden oder identisch)|"
 			+ "(keine geeigneten Haltestellen)|(keine Verbindung gefunden)|"
 			+ "(derzeit nur Ausk&uuml;nfte vom)|(zwischenzeitlich nicht mehr gespeichert)|(http-equiv=\"refresh\")", Pattern.CASE_INSENSITIVE);
 
 	private QueryConnectionsResult queryConnections(final String firstUri, CharSequence firstPage) throws IOException
 	{
 		final Matcher mError = P_CHECK_CONNECTIONS_ERROR.matcher(firstPage);
 		if (mError.find())
 		{
 			if (mError.group(1) != null)
 				return QueryConnectionsResult.TOO_CLOSE;
 			if (mError.group(2) != null)
 				return QueryConnectionsResult.UNRESOLVABLE_ADDRESS;
 			if (mError.group(3) != null)
 				return QueryConnectionsResult.NO_CONNECTIONS;
 			if (mError.group(4) != null)
 				return QueryConnectionsResult.INVALID_DATE;
 			if (mError.group(5) != null)
 				throw new SessionExpiredException();
 			if (mError.group(6) != null)
 				throw new IOException("connected to private wlan");
 		}
 
 		final Matcher mAllDetailsAction = P_CONNECTIONS_ALL_DETAILS.matcher(firstPage);
 		if (!mAllDetailsAction.find())
 			throw new IOException("cannot find all details link in '" + firstPage + "' on " + firstUri);
 
 		final String allDetailsUri = BASE_URL + ParserUtils.resolveEntities(mAllDetailsAction.group(1));
 		final CharSequence page = ParserUtils.scrape(allDetailsUri);
 
 		final Matcher mHead = P_CONNECTIONS_HEAD.matcher(page);
 		if (mHead.matches())
 		{
 			final Location from = location(mHead.group(3), mHead.group(2), mHead.group(5), mHead.group(4),
 					ParserUtils.resolveEntities(mHead.group(1)));
 			final Location via = mHead.group(6) != null ? location(null, null, null, null, ParserUtils.resolveEntities(mHead.group(6))) : null;
 			final Location to = location(mHead.group(9), mHead.group(8), mHead.group(11), mHead.group(10),
 					ParserUtils.resolveEntities(mHead.group(7)));
 			final Calendar currentDate = new GregorianCalendar(timeZone());
 			currentDate.clear();
 			ParserUtils.parseGermanDate(currentDate, mHead.group(12));
 			// final String linkEarlier = mHead.group(13) != null ? BVG_BASE_URL +
 			// ParserUtils.resolveEntities(mHead.group(13)) : null;
 			final String linkLater = mHead.group(14) != null ? BASE_URL + ParserUtils.resolveEntities(mHead.group(14)) : null;
 			final List<Connection> connections = new ArrayList<Connection>();
 
 			final Matcher mConCoarse = P_CONNECTIONS_COARSE.matcher(page);
 			int iCon = 0;
 			while (mConCoarse.find())
 			{
 				if (++iCon != Integer.parseInt(mConCoarse.group(1)))
 					throw new IllegalStateException("missing connection: " + iCon);
 				final Matcher mConFine = P_CONNECTIONS_FINE.matcher(mConCoarse.group(2));
 				if (mConFine.matches())
 				{
 					final Calendar time = new GregorianCalendar(timeZone());
 					time.clear();
 					ParserUtils.parseGermanDate(time, mConFine.group(1));
 					Date lastTime = null;
 
 					Date firstDepartureTime = null;
 					Date lastArrivalTime = null;
 
 					final String id = mConFine.group(2);
 
 					final String[] trackParts = mConFine.group(3).split("\\*");
 
 					final List<List<String[]>> tracks = new ArrayList<List<String[]>>();
 					for (final String trackPart : trackParts)
 					{
 						final String[] partElements = trackPart.split("\\|");
 						if (partElements.length != 10)
 							throw new IllegalStateException("cannot parse: '" + trackPart + "'");
 						final int i = Integer.parseInt(partElements[0]);
 						if (i >= tracks.size())
 							tracks.add(new ArrayList<String[]>());
 						tracks.get(i).add(partElements);
 					}
 
 					final Matcher mDetails = P_CONNECTION_DETAILS.matcher(mConCoarse.group(2));
 
 					final List<Connection.Part> parts = new ArrayList<Connection.Part>(tracks.size());
 					for (int iTrack = 0; iTrack < tracks.size(); iTrack++)
 					{
 						mDetails.find();
 
 						final List<String[]> track = tracks.get(iTrack);
 						final String[] tDep = track.get(0);
 						final String[] tArr = track.get(track.size() - 1);
 
 						final Location departure = location(tDep);
 
 						ParserUtils.parseEuropeanTime(time, tDep[8]);
 						if (lastTime != null && time.getTime().before(lastTime))
 							time.add(Calendar.DAY_OF_YEAR, 1);
 						lastTime = time.getTime();
 						final Date departureTime = time.getTime();
 						if (firstDepartureTime == null)
 							firstDepartureTime = departureTime;
 
 						final String departurePosition = !mDetails.group(1).equals("&nbsp;") ? ParserUtils.resolveEntities(mDetails.group(1)) : null;
 
 						if (tArr[2].equals("walk"))
 						{
							final String[] tArr2 = track.size() > 1 ? tArr : tracks.get(iTrack + 1).get(0);
 
 							final Location arrival = location(tArr2);
 
							ParserUtils.parseEuropeanTime(time, tArr2[7]);
 							if (lastTime != null && time.getTime().before(lastTime))
 								time.add(Calendar.DAY_OF_YEAR, 1);
 							lastTime = time.getTime();
 							final Date arrivalTime = time.getTime();
 							lastArrivalTime = arrivalTime;
 
 							final int mins = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);
 
 							parts.add(new Connection.Footway(mins, departure, arrival, null));
 						}
 						else
 						{
 							final List<Stop> intermediateStops = new LinkedList<Stop>();
 							for (final String[] tStop : track.subList(1, track.size() - 1))
 							{
 								ParserUtils.parseEuropeanTime(time, tStop[8]);
 								if (lastTime != null && time.getTime().before(lastTime))
 									time.add(Calendar.DAY_OF_YEAR, 1);
 								lastTime = time.getTime();
 								intermediateStops.add(new Stop(location(tStop), null, time.getTime()));
 							}
 
 							final Location arrival = location(tArr);
 
 							ParserUtils.parseEuropeanTime(time, tArr[7]);
 							if (lastTime != null && time.getTime().before(lastTime))
 								time.add(Calendar.DAY_OF_YEAR, 1);
 							lastTime = time.getTime();
 							final Date arrivalTime = time.getTime();
 							lastArrivalTime = arrivalTime;
 
 							final String arrivalPosition = !mDetails.group(3).equals("&nbsp;") ? ParserUtils.resolveEntities(mDetails.group(3))
 									: null;
 
 							final String lineStr = normalizeLine(ParserUtils.resolveEntities(tDep[3]));
 							final Line line = new Line(lineStr, lineColors(lineStr));
 
 							final Location destination;
 							if (mDetails.group(2) != null)
 							{
 								final String[] destinationPlaceAndName = splitNameAndPlace(ParserUtils.resolveEntities(mDetails.group(2)));
 								destination = new Location(LocationType.ANY, 0, destinationPlaceAndName[0], destinationPlaceAndName[1]);
 							}
 							else
 							{
 								destination = null;
 							}
 
 							parts.add(new Connection.Trip(line, destination, departureTime, departurePosition, departure, arrivalTime,
 									arrivalPosition, arrival, intermediateStops, null));
 						}
 					}
 
 					connections.add(new Connection(id, firstUri, firstDepartureTime, lastArrivalTime, from, to, parts, null));
 				}
 				else
 				{
 					throw new IllegalArgumentException("cannot parse '" + mConCoarse.group(2) + "' on " + allDetailsUri);
 				}
 			}
 
 			return new QueryConnectionsResult(firstUri, from, via, to, linkLater, connections);
 		}
 		else
 		{
 			throw new IOException(page.toString());
 		}
 	}
 
 	@Override
 	public GetConnectionDetailsResult getConnectionDetails(final String uri) throws IOException
 	{
 		throw new UnsupportedOperationException();
 	}
 
 	private static final String DEPARTURE_URL_LIVE = BASE_URL + "/IstAbfahrtzeiten/index/mobil?";
 
 	private String departuresQueryLiveUri(final String stationId)
 	{
 		final StringBuilder uri = new StringBuilder();
 		uri.append(DEPARTURE_URL_LIVE);
 		uri.append("input=").append(stationId);
 		return uri.toString();
 	}
 
 	private static final String DEPARTURE_URL_PLAN = API_BASE + "stboard.bin/dox/dox?boardType=dep&disableEquivs=yes&start=yes&";
 
 	private String departuresQueryPlanUri(final String stationId, final int maxDepartures)
 	{
 		final StringBuilder uri = new StringBuilder();
 		uri.append(DEPARTURE_URL_PLAN);
 		uri.append("input=").append(stationId);
 		uri.append("&maxJourneys=").append(maxDepartures != 0 ? maxDepartures : 50);
 		return uri.toString();
 	}
 
 	private static final Pattern P_DEPARTURES_PLAN_HEAD = Pattern.compile(".*?" //
 			+ "<strong>(.*?)</strong>.*?Datum:\\s*([^<\n]+)[<\n].*?" //
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_PLAN_COARSE = Pattern.compile("" //
 			+ "<tr class=\"ivu_table_bg\\d\">\\s*((?:<td class=\"ivu_table_c_dep\">|<td>).+?)\\s*</tr>" //
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_PLAN_FINE = Pattern.compile("" //
 			+ "<td><strong>(\\d{1,2}:\\d{2})</strong></td>.*?" // time
 			+ "<strong>\\s*(.*?)[\\s\\*]*</strong>.*?" // line
 			+ "(?:\\((Gl\\. " + ParserUtils.P_PLATFORM + ")\\).*?)?" // position
 			+ "<a href=\"/Fahrinfo/bin/stboard\\.bin/dox/dox.*?evaId=(\\d+)&[^>]*>" // destinationId
 			+ "\\s*(.*?)\\s*</a>.*?" // destination
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_PLAN_ERRORS = Pattern.compile("(derzeit leider nicht bearbeitet werden)|(Wartungsarbeiten)|"
 			+ "(http-equiv=\"refresh\")", Pattern.CASE_INSENSITIVE);
 
 	private static final Pattern P_DEPARTURES_LIVE_HEAD = Pattern.compile(".*?" //
 			+ "<strong>(.*?)</strong>.*?Datum:\\s*([^<\n]+)[<\n].*?" //
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_LIVE_COARSE = Pattern.compile("" //
 			+ "<tr class=\"ivu_table_bg\\d\">\\s*((?:<td class=\"ivu_table_c_dep\">|<td>).+?)\\s*</tr>" //
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_LIVE_FINE = Pattern.compile("" //
 			+ "<td class=\"ivu_table_c_dep\">\\s*(\\d{1,2}:\\d{2})\\s*" // time
 			+ "(\\*)?\\s*</td>\\s*" // planned
 			+ "<td class=\"ivu_table_c_line\">\\s*(.*?)\\s*</td>\\s*" // line
 			+ "<td>.*?<a.*?[^-]>\\s*(.*?)\\s*</a>.*?</td>" // destination
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_LIVE_MSGS_COARSE = Pattern.compile("" //
 			+ "<tr class=\"ivu_table_bg\\d\">\\s*(<td class=\"ivu_table_c_line\">.+?)\\s*</tr>" //
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_LIVE_MSGS_FINE = Pattern.compile("" //
 			+ "<td class=\"ivu_table_c_line\">\\s*(.*?)\\s*</td>\\s*" // line
 			+ "<td class=\"ivu_table_c_dep\">\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s*</td>\\s*" // date
 			+ "<td>([^<]*)</td>" // message
 	, Pattern.DOTALL);
 	private static final Pattern P_DEPARTURES_LIVE_ERRORS = Pattern.compile("(Haltestelle:)|(Wartungsgr&uuml;nden)|(http-equiv=\"refresh\")",
 			Pattern.CASE_INSENSITIVE);
 
 	public QueryDeparturesResult queryDepartures(final String stationId, final int maxDepartures, final boolean equivs) throws IOException
 	{
 		final QueryDeparturesResult result = new QueryDeparturesResult();
 
 		if (stationId.length() == 6) // live
 		{
 			// scrape page
 			final String uri = departuresQueryLiveUri(stationId);
 			final CharSequence page = ParserUtils.scrape(uri);
 
 			final Matcher mError = P_DEPARTURES_LIVE_ERRORS.matcher(page);
 			if (mError.find())
 			{
 				if (mError.group(1) != null)
 					return new QueryDeparturesResult(Status.INVALID_STATION);
 				if (mError.group(2) != null)
 					return new QueryDeparturesResult(Status.SERVICE_DOWN);
 				if (mError.group(3) != null)
 					throw new IOException("connected to private wlan");
 			}
 
 			// parse page
 			final Matcher mHead = P_DEPARTURES_LIVE_HEAD.matcher(page);
 			if (mHead.matches())
 			{
 				final String[] placeAndName = splitNameAndPlace(ParserUtils.resolveEntities(mHead.group(1)));
 				final Calendar currentTime = new GregorianCalendar(timeZone());
 				currentTime.clear();
 				parseDateTime(currentTime, mHead.group(2));
 
 				final Map<String, String> messages = new HashMap<String, String>();
 
 				final Matcher mMsgsCoarse = P_DEPARTURES_LIVE_MSGS_COARSE.matcher(page);
 				while (mMsgsCoarse.find())
 				{
 					final Matcher mMsgsFine = P_DEPARTURES_LIVE_MSGS_FINE.matcher(mMsgsCoarse.group(1));
 					if (mMsgsFine.matches())
 					{
 						final String line = normalizeLine(ParserUtils.resolveEntities(mMsgsFine.group(1)));
 						final String message = ParserUtils.resolveEntities(mMsgsFine.group(3)).replace('\n', ' ');
 						messages.put(line, message);
 					}
 					else
 					{
 						throw new IllegalArgumentException("cannot parse '" + mMsgsCoarse.group(1) + "' on " + uri);
 					}
 				}
 
 				final List<Departure> departures = new ArrayList<Departure>(8);
 
 				final Matcher mDepCoarse = P_DEPARTURES_LIVE_COARSE.matcher(page);
 				while (mDepCoarse.find())
 				{
 					final Matcher mDepFine = P_DEPARTURES_LIVE_FINE.matcher(mDepCoarse.group(1));
 					if (mDepFine.matches())
 					{
 						final Calendar parsedTime = new GregorianCalendar(timeZone());
 						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
 						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(1));
 
 						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
 							parsedTime.add(Calendar.DAY_OF_MONTH, 1);
 
 						boolean isPlanned = mDepFine.group(2) != null;
 
 						Date plannedTime = null;
 						Date predictedTime = null;
 						if (!isPlanned)
 							predictedTime = parsedTime.getTime();
 						else
 							plannedTime = parsedTime.getTime();
 
 						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(3)));
 
 						final String position = null;
 
 						final int destinationId = 0;
 
 						final String destination = ParserUtils.resolveEntities(mDepFine.group(4));
 
 						final Departure dep = new Departure(plannedTime, predictedTime, line, line != null ? lineColors(line) : null, null, position,
 								destinationId, destination, messages.get(line));
 						if (!departures.contains(dep))
 							departures.add(dep);
 					}
 					else
 					{
 						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
 					}
 				}
 
 				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, Integer.parseInt(stationId), placeAndName[0],
 						placeAndName[1]), departures, null));
 				return result;
 			}
 			else
 			{
 				throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
 			}
 		}
 		else
 		{
 			// scrape page
 			final String uri = departuresQueryPlanUri(stationId, maxDepartures);
 			final CharSequence page = ParserUtils.scrape(uri);
 
 			final Matcher mError = P_DEPARTURES_PLAN_ERRORS.matcher(page);
 			if (mError.find())
 			{
 				if (mError.group(1) != null)
 					return new QueryDeparturesResult(Status.INVALID_STATION);
 				if (mError.group(2) != null)
 					return new QueryDeparturesResult(Status.SERVICE_DOWN);
 				if (mError.group(3) != null)
 					throw new IOException("connected to private wlan");
 			}
 
 			// parse page
 			final Matcher mHead = P_DEPARTURES_PLAN_HEAD.matcher(page);
 			if (mHead.matches())
 			{
 				final String[] placeAndName = splitNameAndPlace(ParserUtils.resolveEntities(mHead.group(1)));
 				final Calendar currentTime = new GregorianCalendar(timeZone());
 				currentTime.clear();
 				ParserUtils.parseGermanDate(currentTime, mHead.group(2));
 				final List<Departure> departures = new ArrayList<Departure>(8);
 
 				final Matcher mDepCoarse = P_DEPARTURES_PLAN_COARSE.matcher(page);
 				while (mDepCoarse.find())
 				{
 					final Matcher mDepFine = P_DEPARTURES_PLAN_FINE.matcher(mDepCoarse.group(1));
 					if (mDepFine.matches())
 					{
 						final Calendar parsedTime = new GregorianCalendar(timeZone());
 						parsedTime.setTimeInMillis(currentTime.getTimeInMillis());
 						ParserUtils.parseEuropeanTime(parsedTime, mDepFine.group(1));
 
 						if (parsedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
 							parsedTime.add(Calendar.DAY_OF_MONTH, 1);
 
 						final Date plannedTime = parsedTime.getTime();
 
 						final String line = normalizeLine(ParserUtils.resolveEntities(mDepFine.group(2)));
 
 						final String position = ParserUtils.resolveEntities(mDepFine.group(3));
 
 						final int destinationId = Integer.parseInt(mDepFine.group(4));
 
 						final String destination = ParserUtils.resolveEntities(mDepFine.group(5));
 
 						final Departure dep = new Departure(plannedTime, null, line, line != null ? lineColors(line) : null, null, position,
 								destinationId, destination, null);
 						if (!departures.contains(dep))
 							departures.add(dep);
 					}
 					else
 					{
 						throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(1) + "' on " + uri);
 					}
 				}
 
 				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, Integer.parseInt(stationId), placeAndName[0],
 						placeAndName[1]), departures, null));
 				return result;
 			}
 			else
 			{
 				throw new IllegalArgumentException("cannot parse '" + page + "' on " + uri);
 			}
 		}
 	}
 
 	private static final Pattern P_DATE_TIME = Pattern.compile("([^,]*), (.*?)");
 
 	private static final void parseDateTime(final Calendar calendar, final CharSequence str)
 	{
 		final Matcher m = P_DATE_TIME.matcher(str);
 		if (!m.matches())
 			throw new RuntimeException("cannot parse: '" + str + "'");
 
 		ParserUtils.parseGermanDate(calendar, m.group(1));
 		ParserUtils.parseEuropeanTime(calendar, m.group(2));
 	}
 
 	private static final Pattern P_NORMALIZE_LINE = Pattern.compile("([A-Za-zÄÖÜäöüßáàâéèêíìîóòôúùû]+)[\\s-]*(.*)");
 	private static final Pattern P_NORMALIZE_LINE_SPECIAL_NUMBER = Pattern.compile("\\d{4,}");
 	private static final Pattern P_NORMALIZE_LINE_SPECIAL_BUS = Pattern.compile("Bus[A-Z]");
 
 	private static String normalizeLine(final String line)
 	{
 		if (line == null || line.length() == 0)
 			return null;
 
 		if (line.startsWith("RE") || line.startsWith("RB") || line.startsWith("NE") || line.startsWith("OE") || line.startsWith("MR")
 				|| line.startsWith("PE"))
 			return "R" + line;
 		if (line.equals("11"))
 			return "?11";
 		if (P_NORMALIZE_LINE_SPECIAL_NUMBER.matcher(line).matches())
 			return "R" + line;
 
 		final Matcher m = P_NORMALIZE_LINE.matcher(line);
 		if (m.matches())
 		{
 			final String type = m.group(1);
 			final String number = m.group(2).replace(" ", "");
 
 			if (type.equals("ICE")) // InterCityExpress
 				return "IICE" + number;
 			if (type.equals("IC")) // InterCity
 				return "IIC" + number;
 			if (type.equals("EC")) // EuroCity
 				return "IEC" + number;
 			if (type.equals("EN")) // EuroNight
 				return "IEN" + number;
 			if (type.equals("CNL")) // CityNightLine
 				return "ICNL" + number;
 			if (type.equals("IR"))
 				return "RIR" + number;
 			if (type.equals("IRE"))
 				return "RIRE" + number;
 			if (type.equals("Zug"))
 				return "R" + number;
 			if (type.equals("ZUG"))
 				return "R" + number;
 			if (type.equals("D")) // D-Zug?
 				return "RD" + number;
 			if (type.equals("DNZ")) // unklar, aber vermutlich Russland
 				return "RDNZ" + (number.equals("DNZ") ? "" : number);
 			if (type.equals("KBS")) // Kursbuchstrecke
 				return "RKBS" + number;
 			if (type.equals("BKB")) // Buckower Kleinbahn
 				return "RBKB" + number;
 			if (type.equals("Ausfl")) // Umgebung Berlin
 				return "RAusfl" + number;
 			if (type.equals("PKP")) // Polen
 				return "RPKP" + number;
 			if (type.equals("S"))
 				return "SS" + number;
 			if (type.equals("U"))
 				return "UU" + number;
 			if (type.equals("Tra") || type.equals("Tram"))
 				return "T" + number;
 			if (type.equals("Bus"))
 				return "B" + number;
 			if (P_NORMALIZE_LINE_SPECIAL_BUS.matcher(type).matches()) // workaround for weird scheme BusF/526
 				return "B" + line.substring(3);
 			if (type.equals("Fäh"))
 				return "F" + number;
 			if (type.equals("F"))
 				return "FF" + number;
 
 			throw new IllegalStateException("cannot normalize type '" + type + "' number '" + number + "' line '" + line + "'");
 		}
 
 		throw new IllegalStateException("cannot normalize line '" + line + "'");
 	}
 
 	@Override
 	protected char normalizeType(final String type)
 	{
 		throw new UnsupportedOperationException();
 	}
 
 	private static final Map<String, int[]> LINES = new HashMap<String, int[]>();
 
 	static
 	{
 		LINES.put("SS1", new int[] { Color.rgb(221, 77, 174), Color.WHITE });
 		LINES.put("SS2", new int[] { Color.rgb(16, 132, 73), Color.WHITE });
 		LINES.put("SS25", new int[] { Color.rgb(16, 132, 73), Color.WHITE });
 		LINES.put("SS3", new int[] { Color.rgb(22, 106, 184), Color.WHITE });
 		LINES.put("SS41", new int[] { Color.rgb(162, 63, 48), Color.WHITE });
 		LINES.put("SS42", new int[] { Color.rgb(191, 90, 42), Color.WHITE });
 		LINES.put("SS45", new int[] { Color.rgb(191, 128, 55), Color.WHITE });
 		LINES.put("SS46", new int[] { Color.rgb(191, 128, 55), Color.WHITE });
 		LINES.put("SS47", new int[] { Color.rgb(191, 128, 55), Color.WHITE });
 		LINES.put("SS5", new int[] { Color.rgb(243, 103, 23), Color.WHITE });
 		LINES.put("SS7", new int[] { Color.rgb(119, 96, 176), Color.WHITE });
 		LINES.put("SS75", new int[] { Color.rgb(119, 96, 176), Color.WHITE });
 		LINES.put("SS8", new int[] { Color.rgb(85, 184, 49), Color.WHITE });
 		LINES.put("SS85", new int[] { Color.rgb(85, 184, 49), Color.WHITE });
 		LINES.put("SS9", new int[] { Color.rgb(148, 36, 64), Color.WHITE });
 
 		LINES.put("UU1", new int[] { Color.rgb(84, 131, 47), Color.WHITE });
 		LINES.put("UU2", new int[] { Color.rgb(215, 25, 16), Color.WHITE });
 		LINES.put("UU3", new int[] { Color.rgb(47, 152, 154), Color.WHITE });
 		LINES.put("UU4", new int[] { Color.rgb(255, 233, 42), Color.BLACK });
 		LINES.put("UU5", new int[] { Color.rgb(91, 31, 16), Color.WHITE });
 		LINES.put("UU55", new int[] { Color.rgb(91, 31, 16), Color.WHITE });
 		LINES.put("UU6", new int[] { Color.rgb(127, 57, 115), Color.WHITE });
 		LINES.put("UU7", new int[] { Color.rgb(0, 153, 204), Color.WHITE });
 		LINES.put("UU8", new int[] { Color.rgb(24, 25, 83), Color.WHITE });
 		LINES.put("UU9", new int[] { Color.rgb(255, 90, 34), Color.WHITE });
 
 		LINES.put("TM1", new int[] { Color.rgb(204, 51, 0), Color.WHITE });
 		LINES.put("TM2", new int[] { Color.rgb(116, 192, 67), Color.WHITE });
 		LINES.put("TM4", new int[] { Color.rgb(208, 28, 34), Color.WHITE });
 		LINES.put("TM5", new int[] { Color.rgb(204, 153, 51), Color.WHITE });
 		LINES.put("TM6", new int[] { Color.rgb(0, 0, 255), Color.WHITE });
 		LINES.put("TM8", new int[] { Color.rgb(255, 102, 0), Color.WHITE });
 		LINES.put("TM10", new int[] { Color.rgb(0, 153, 51), Color.WHITE });
 		LINES.put("TM13", new int[] { Color.rgb(51, 153, 102), Color.WHITE });
 		LINES.put("TM17", new int[] { Color.rgb(153, 102, 51), Color.WHITE });
 
 		LINES.put("B12", new int[] { Color.rgb(153, 102, 255), Color.WHITE });
 		LINES.put("B16", new int[] { Color.rgb(0, 0, 255), Color.WHITE });
 		LINES.put("B18", new int[] { Color.rgb(255, 102, 0), Color.WHITE });
 		LINES.put("B21", new int[] { Color.rgb(153, 102, 255), Color.WHITE });
 		LINES.put("B27", new int[] { Color.rgb(153, 102, 51), Color.WHITE });
 		LINES.put("B37", new int[] { Color.rgb(153, 102, 51), Color.WHITE });
 		LINES.put("B50", new int[] { Color.rgb(51, 153, 102), Color.WHITE });
 		LINES.put("B60", new int[] { Color.rgb(0, 153, 51), Color.WHITE });
 		LINES.put("B61", new int[] { Color.rgb(0, 153, 51), Color.WHITE });
 		LINES.put("B62", new int[] { Color.rgb(0, 102, 51), Color.WHITE });
 		LINES.put("B63", new int[] { Color.rgb(51, 153, 102), Color.WHITE });
 		LINES.put("B67", new int[] { Color.rgb(0, 102, 51), Color.WHITE });
 		LINES.put("B68", new int[] { Color.rgb(0, 153, 51), Color.WHITE });
 
 		LINES.put("FF1", new int[] { Color.BLUE, Color.WHITE }); // Potsdam
 		LINES.put("FF10", new int[] { Color.BLUE, Color.WHITE });
 		LINES.put("FF11", new int[] { Color.BLUE, Color.WHITE });
 		LINES.put("FF12", new int[] { Color.BLUE, Color.WHITE });
 		LINES.put("FF21", new int[] { Color.BLUE, Color.WHITE });
 		LINES.put("FF23", new int[] { Color.BLUE, Color.WHITE });
 		LINES.put("FF24", new int[] { Color.BLUE, Color.WHITE });
 
 		// Regional lines Brandenburg:
 		LINES.put("RRE1", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
 		LINES.put("RRE2", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
 		LINES.put("RRE3", new int[] { Color.parseColor("#F57921"), Color.WHITE });
 		LINES.put("RRE4", new int[] { Color.parseColor("#952D4F"), Color.WHITE });
 		LINES.put("RRE5", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
 		LINES.put("RRE6", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
 		LINES.put("RRE7", new int[] { Color.parseColor("#00854A"), Color.WHITE });
 		LINES.put("RRE10", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
 		LINES.put("RRE11", new int[] { Color.parseColor("#059EDB"), Color.WHITE });
 		LINES.put("RRE11", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
 		LINES.put("RRE15", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
 		LINES.put("RRE18", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
 		LINES.put("RRB10", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
 		LINES.put("RRB12", new int[] { Color.parseColor("#A3238E"), Color.WHITE });
 		LINES.put("RRB13", new int[] { Color.parseColor("#F68B1F"), Color.WHITE });
 		LINES.put("RRB13", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
 		LINES.put("RRB14", new int[] { Color.parseColor("#A3238E"), Color.WHITE });
 		LINES.put("RRB20", new int[] { Color.parseColor("#00854A"), Color.WHITE });
 		LINES.put("RRB21", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
 		LINES.put("RRB22", new int[] { Color.parseColor("#0087CB"), Color.WHITE });
 		LINES.put("ROE25", new int[] { Color.parseColor("#0087CB"), Color.WHITE });
 		LINES.put("RNE26", new int[] { Color.parseColor("#00A896"), Color.WHITE });
 		LINES.put("RNE27", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
 		LINES.put("RRB30", new int[] { Color.parseColor("#00A65E"), Color.WHITE });
 		LINES.put("RRB31", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
 		LINES.put("RMR33", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
 		LINES.put("ROE35", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
 		LINES.put("ROE36", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
 		LINES.put("RRB43", new int[] { Color.parseColor("#5E6DB3"), Color.WHITE });
 		LINES.put("RRB45", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
 		LINES.put("ROE46", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
 		LINES.put("RMR51", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
 		LINES.put("RRB51", new int[] { Color.parseColor("#DB6EAB"), Color.WHITE });
 		LINES.put("RRB54", new int[] { Color.parseColor("#FFD403"), Color.parseColor("#333333") });
 		LINES.put("RRB55", new int[] { Color.parseColor("#F57921"), Color.WHITE });
 		LINES.put("ROE60", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
 		LINES.put("ROE63", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
 		LINES.put("ROE65", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
 		LINES.put("RRB66", new int[] { Color.parseColor("#60BB46"), Color.WHITE });
 		LINES.put("RPE70", new int[] { Color.parseColor("#FFD403"), Color.BLACK });
 		LINES.put("RPE73", new int[] { Color.parseColor("#00A896"), Color.WHITE });
 		LINES.put("RPE74", new int[] { Color.parseColor("#0072BC"), Color.WHITE });
 		LINES.put("T89", new int[] { Color.parseColor("#EE1C23"), Color.WHITE });
 		LINES.put("RRB91", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
 		LINES.put("RRB93", new int[] { Color.parseColor("#A7653F"), Color.WHITE });
 	}
 
 	@Override
 	public int[] lineColors(final String line)
 	{
 		final int[] lineColors = LINES.get(line);
 		if (lineColors != null)
 			return lineColors;
 		else
 			return super.lineColors(line);
 	}
 }
