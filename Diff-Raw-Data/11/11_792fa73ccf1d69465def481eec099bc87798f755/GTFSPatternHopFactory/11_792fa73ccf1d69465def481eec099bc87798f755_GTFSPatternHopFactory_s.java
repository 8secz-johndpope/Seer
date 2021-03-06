 /* This program is free software: you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public License
  as published by the Free Software Foundation, either version 3 of
  the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>. */
 
 package org.opentripplanner.routing.edgetype.factory;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Vector;
 
 import org.onebusaway.gtfs.model.AgencyAndId;
 import org.onebusaway.gtfs.model.ShapePoint;
 import org.onebusaway.gtfs.model.Stop;
 import org.onebusaway.gtfs.model.StopTime;
 import org.onebusaway.gtfs.model.Transfer;
 import org.onebusaway.gtfs.model.Trip;
 import org.onebusaway.gtfs.services.GtfsRelationalDao;
 import org.opentripplanner.common.geometry.PackedCoordinateSequence;
 import org.opentripplanner.gtfs.GtfsContext;
 import org.opentripplanner.gtfs.GtfsLibrary;
 import org.opentripplanner.routing.core.Graph;
 import org.opentripplanner.routing.core.TraverseMode;
 import org.opentripplanner.routing.core.Vertex;
 import org.opentripplanner.routing.edgetype.Alight;
 import org.opentripplanner.routing.edgetype.Board;
 import org.opentripplanner.routing.edgetype.Dwell;
 import org.opentripplanner.routing.edgetype.Hop;
 import org.opentripplanner.routing.edgetype.PatternAlight;
 import org.opentripplanner.routing.edgetype.PatternBoard;
 import org.opentripplanner.routing.edgetype.PatternDwell;
 import org.opentripplanner.routing.edgetype.PatternHop;
 import org.opentripplanner.routing.edgetype.TripPattern;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.vividsolutions.jts.geom.Coordinate;
 import com.vividsolutions.jts.geom.CoordinateSequence;
 import com.vividsolutions.jts.geom.Geometry;
 import com.vividsolutions.jts.geom.GeometryFactory;
 import com.vividsolutions.jts.geom.LineString;
 import com.vividsolutions.jts.linearref.LinearLocation;
 import com.vividsolutions.jts.linearref.LocationIndexedLine;
 
 /** 
  * 
  * A StopPattern is an intermediate object used when processing GTFS files.  It represents an ordered list 
  * of stops and a service ID.  Any two trips with the same stops in the same order, and that operates on the 
  * same days, can be combined using a TripPattern to save memory.  
  */
 
 class StopPattern {
     Vector<Stop> stops;
 
     AgencyAndId calendarId;
 
     public StopPattern(Vector<Stop> stops, AgencyAndId calendarId) {
         this.stops = stops;
         this.calendarId = calendarId;
     }
 
     public boolean equals(Object other) {
         if (other instanceof StopPattern) {
             StopPattern pattern = (StopPattern) other;
             return pattern.stops.equals(stops) && pattern.calendarId.equals(calendarId);
         } else {
             return false;
         }
     }
 
     public int hashCode() {
         return this.stops.hashCode() ^ this.calendarId.hashCode();
     }
 
     public String toString() {
         return "StopPattern(" + stops + ", " + calendarId + ")";
     }
 }
 
 /**
  * An EncodedTrip is an intermediate object used during GTFS processing.  It represents a trip as it will be 
  * put into a TripPattern.  It's used during interlining processing, to create that the extra PatternDwell edges
  * where someone stays on a vehicle as its number changes.
  */
 class EncodedTrip {
     Trip trip;
 
     int patternIndex;
 
     TripPattern pattern;
 
     public EncodedTrip(Trip trip, int i, TripPattern pattern) {
         this.trip = trip;
         this.patternIndex = i;
         this.pattern = pattern;
     }
 
     public boolean equals(Object o) {
         if (!(o instanceof EncodedTrip))
             return false;
         EncodedTrip eto = (EncodedTrip) o;
         return trip.equals(eto.trip) && patternIndex == eto.patternIndex
                 && pattern.equals(eto.pattern);
     }
 
     public String toString() {
         return "EncodedTrip(" + this.trip + ", " + this.patternIndex + ", " + this.pattern + ")";
     }
 }
 
 /**
  * Generates a set of edges from GTFS.
  */
 public class GTFSPatternHopFactory {
 
     private final Logger _log = LoggerFactory.getLogger(GTFSPatternHopFactory.class);
 
     private static GeometryFactory _factory = new GeometryFactory();
 
     private GtfsRelationalDao _dao;
 
     private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();
 
     private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();
 
     private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();
 
     public GTFSPatternHopFactory(GtfsContext context) {
         _dao = context.getDao();
     }
 
     public static StopPattern stopPatternfromTrip(Trip trip, GtfsRelationalDao dao) {
         Vector<Stop> stops = new Vector<Stop>();
 
         for (StopTime stoptime : dao.getStopTimesForTrip(trip)) {
             stops.add(stoptime.getStop());
         }
         StopPattern pattern = new StopPattern(stops, trip.getServiceId());
         return pattern;
     }
 
     private String id(AgencyAndId id) {
         return GtfsLibrary.convertIdToString(id);
     }
 
     /**
      * Generate the edges.  Assumes that there are already vertices in the graph for the stops.
      */
     public void run(Graph graph) {
 
         clearCachedData();
 
         /*
          * For each trip, create either pattern edges, the entries in a trip pattern's list of
          * departures, or simple hops
          */
 
         // Load hops
         Collection<Trip> trips = _dao.getAllTrips();
 
         HashMap<StopPattern, TripPattern> patterns = new HashMap<StopPattern, TripPattern>();
 
         int index = 0;
 
         HashMap<String, ArrayList<EncodedTrip>> tripsByBlock = new HashMap<String, ArrayList<EncodedTrip>>();
 
         for (Trip trip : trips) {
 
             if (index % 100 == 0)
                 _log.debug("trips=" + index + "/" + trips.size());
             index++;
 
             List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
 
             if (stopTimes.isEmpty())
                 continue;

             StopPattern stopPattern = stopPatternfromTrip(trip, _dao);
             TripPattern tripPattern = patterns.get(stopPattern);
            int lastStop = stopTimes.size() - 1;
             TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
             int departureTime = -1, prevDepartureTime = -1;
             int numInterpStops = -1, firstInterpStop = -1; 
             int interpStep = 0;
             boolean tripWheelchairAccessible = trip.getWheelchairAccessible() != 0;
             if (tripPattern == null) {
 
                 tripPattern = new TripPattern(trip, stopTimes);
 
                 int i;
                 Stop s1 = null;
                 for (i = 0; i < lastStop; i++) {
                     StopTime st0 = stopTimes.get(i);
                     Stop s0 = st0.getStop();
                     StopTime st1 = stopTimes.get(i + 1);
                     s1 = st1.getStop();
                     int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                     // create journey vertices
 
                     Vertex startJourneyDepart = graph.addVertex(id(s0.getId()) + "_"
                             + id(trip.getId()) + "_D", s0.getLon(), s0.getLat());
                     Vertex endJourneyArrive = graph.addVertex(id(s1.getId()) + "_"
                             + id(trip.getId()) + "_A", s1.getLon(), s1.getLat());
                     Vertex startJourneyArrive;
                     if (i != 0) {
                         startJourneyArrive = graph.addVertex(id(s0.getId()) + "_"
                                 + id(trip.getId()) + "_A", s0.getLon(), s0.getLat());
 
                         PatternDwell dwell = new PatternDwell(startJourneyArrive,
                                 startJourneyDepart, i, tripPattern);
                         graph.addEdge(dwell);
                     }
 
                     PatternHop hop = new PatternHop(startJourneyDepart, endJourneyArrive, s0, s1,
                             i, tripPattern);
 
                     hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                             endJourneyArrive));
 
                     prevDepartureTime = departureTime;
                     departureTime = st0.getDepartureTime();
                     int arrivalTime = st1.getArrivalTime();
                     
                     /* Interpolate, if necessary, the times of non-timepoint stops */
                     if (!(st0.isDepartureTimeSet() && st1.isArrivalTimeSet() ) ) {
                         
                         if (numInterpStops == -1) {
                             //figure out how many such stops there are in a row.
                             int j; 
                             for (j = i + 1; j < lastStop + 1; ++j) {
                                 StopTime st = stopTimes.get(j);
                                 if (st.isDepartureTimeSet()) {
                                     break;
                                 }
                             }
                             if (j == lastStop + 1) {
                                 throw new RuntimeException ("Could not interpolate arrival/departure time on stop " + i + " on trip " + trip);
                             }
                             StopTime st = stopTimes.get(j);
                             numInterpStops = j - i - 1;
                             firstInterpStop = i + 1;
                             interpStep = (st.getArrivalTime() - departureTime) / (numInterpStops + 2);
                         }
                         if (i >= firstInterpStop) {
                             departureTime = prevDepartureTime + interpStep * (i + 1 - firstInterpStop);
                         } 
                         if (i < firstInterpStop + numInterpStops - 1) {
                             arrivalTime = departureTime + interpStep;
                         }
                         if (i == firstInterpStop + numInterpStops - 1) {
                             // done interpolating
                             numInterpStops = -1; 
                         }
                     }
                     
                     int runningTime = arrivalTime - departureTime;
 
 		    boolean stopWheelchairBoarding = s0.getWheelchairBoarding() != 0;
                     tripPattern.addHop(i, 0, departureTime, runningTime,
                             arrivalTime, dwellTime, 
                             stopWheelchairBoarding && tripWheelchairAccessible);
                     graph.addEdge(hop);
 
                     Vertex startStation = graph.getVertex(id(s0.getId()));
                     Vertex endStation = graph.getVertex(id(s1.getId()));
 
                     PatternBoard boarding = new PatternBoard(startStation, startJourneyDepart,
                             tripPattern, i, mode);
                     graph.addEdge(boarding);
                     graph.addEdge(new PatternAlight(endJourneyArrive, endStation, tripPattern, i, mode));
                 }
                 patterns.put(stopPattern, tripPattern);
 
 		boolean stopWheelchairBoarding = s1.getWheelchairBoarding() != 0;
                 tripPattern.setWheelchairAccessible(i, 0, stopWheelchairBoarding && tripWheelchairAccessible);
 
                 String blockId = trip.getBlockId();
                 if (blockId != null && !blockId.equals("")) {
                     ArrayList<EncodedTrip> blockTrips = tripsByBlock.get(blockId);
                     if (blockTrips == null) {
                         blockTrips = new ArrayList<EncodedTrip>();
                         tripsByBlock.put(blockId, blockTrips);
                     }
                     blockTrips.add(new EncodedTrip(trip, 0, tripPattern));
                 }                
             } else {
                 int insertionPoint = tripPattern.getDepartureTimeInsertionPoint(stopTimes.get(0)
                         .getDepartureTime());
                 if (insertionPoint < 0) {
                     // There's already a departure at this time on this trip pattern. This means
                     // that either (a) this will have all the same stop times as that one, and thus
                     // will be a duplicate of it, or (b) it will have different stops, and thus
                     // break the assumption that trips are non-overlapping.
                     _log.warn("duplicate first departure time for trip " + trip.getId()
                             + ".  This will be handled correctly but inefficiently.");
 
                     createSimpleHops(graph, trip, stopTimes);
 
                 } else {
 
                     // try to insert this trip at this location
                     boolean simple = false;
                     StopTime st1 = null;
                     int i;
                     for (i = 0; i < lastStop; i++) {
                         StopTime st0 = stopTimes.get(i);
                         st1 = stopTimes.get(i + 1);
                         int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                         int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                         try {
 			    boolean s0WheelchairBoarding = st0.getStop().getWheelchairBoarding() != 0;
                             tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                     runningTime, st1.getArrivalTime(), dwellTime, s0WheelchairBoarding
                                             && tripWheelchairAccessible);
                         } catch (TripOvertakingException e) {
                             _log
                                     .warn("trip "
                                             + trip.getId()
                                             + " overtakes another trip with the same stops.  This will be handled correctly but inefficiently.");
                             // back out trips and revert to the simple method
                             for (i=i-1; i >= 0; --i) {
                                 tripPattern.removeHop(i, insertionPoint);
                             }
                             createSimpleHops(graph, trip, stopTimes);
                             simple = true;
                             break;
                         }
                     }
                     if (!simple) {
 			boolean s1WheelchairBoarding = st1.getStop().getWheelchairBoarding() != 0;
                         tripPattern.setWheelchairAccessible(i, insertionPoint, s1WheelchairBoarding && tripWheelchairAccessible);
                         String blockId = trip.getBlockId();
                         if (blockId != null && !blockId.equals("")) {
                             ArrayList<EncodedTrip> blockTrips = tripsByBlock.get(blockId);
                             if (blockTrips == null) {
                                 blockTrips = new ArrayList<EncodedTrip>();
                                 tripsByBlock.put(blockId, blockTrips);
                             }
                             blockTrips.add(new EncodedTrip(trip, 0, tripPattern));
                         }
                     }
                 }
             }
         }
 
         /* for interlined trips, add final dwell edge */
         for (ArrayList<EncodedTrip> blockTrips : tripsByBlock.values()) {
             HashMap<Stop, EncodedTrip> starts = new HashMap<Stop, EncodedTrip>();
             for (EncodedTrip encoded : blockTrips) {
                 Trip trip = encoded.trip;
                 List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
                 Stop start = stopTimes.get(0).getStop();
                 starts.put(start, encoded);
             }
             for (EncodedTrip encoded : blockTrips) {
                 Trip trip = encoded.trip;
                 List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
                 StopTime endTime = stopTimes.get(stopTimes.size() - 1);
                 Stop end = endTime.getStop();
 
                 if (starts.containsKey(end)) {
                     EncodedTrip nextTrip = starts.get(end);
 
                     Vertex arrive = graph.addVertex(
                             id(end.getId()) + "_" + id(trip.getId()) + "_A", end.getLon(), end
                                     .getLat());
 
                     Vertex depart = graph.addVertex(id(end.getId()) + "_"
                             + id(nextTrip.trip.getId()) + "_D", end.getLon(), end.getLat());
                     PatternDwell dwell = new PatternDwell(arrive, depart, nextTrip.patternIndex,
                             encoded.pattern);
 
                     graph.addEdge(dwell);
 
                     List<StopTime> nextStopTimes = _dao.getStopTimesForTrip(nextTrip.trip);
                     StopTime startTime = nextStopTimes.get(0);
                     // startTime.getArrivalTime() is the arrival time of the *next* trip (i.e.,
                     // right when the bus changes to its new trip). endtime.getDepartureTime() is
                     // the final time point along its current trip; the difference is the dwell
                     // time.
                     int dwellTime = startTime.getArrivalTime() - endTime.getDepartureTime();
                     encoded.pattern.setDwellTime(stopTimes.size() - 2, encoded.patternIndex,
                             dwellTime);
 
                 }
             }
         }
 
         loadTransfers(graph);
 
         clearCachedData();
     }
 
     private void clearCachedData() {
         _log.debug("shapes=" + _geometriesByShapeId.size());
         _log.debug("segments=" + _geometriesByShapeSegmentKey.size());
         _geometriesByShapeId.clear();
         _distancesByShapeId.clear();
         _geometriesByShapeSegmentKey.clear();
     }
 
     private void loadTransfers(Graph graph) {
         Collection<Transfer> transfers = _dao.getAllTransfers();
         Set<org.opentripplanner.routing.edgetype.Transfer> createdTransfers = new HashSet<org.opentripplanner.routing.edgetype.Transfer>();
         for (Transfer t : transfers) {
             Stop fromStop = t.getFromStop();
             Stop toStop = t.getToStop();
             Vertex fromStation = graph.getVertex(id(fromStop.getId()));
             Vertex toStation = graph.getVertex(id(toStop.getId()));
             int transferTime = 0;
             if (t.getTransferType() < 3) {
                 if (t.getTransferType() == 2) {
                     transferTime = t.getMinTransferTime();
                 }
                 org.opentripplanner.routing.edgetype.Transfer edge = new org.opentripplanner.routing.edgetype.Transfer(
                         fromStation, toStation, transferTime);
                 if (createdTransfers.contains(edge)) {
                     continue;
                 }
                 GeometryFactory factory = new GeometryFactory();
                 LineString geometry = factory.createLineString(new Coordinate[] {
                         new Coordinate(fromStop.getLon(), fromStop.getLat()),
                         new Coordinate(toStop.getLon(), toStop.getLat()) });
                 edge.setGeometry(geometry);
                 createdTransfers.add(edge);
                 graph.addEdge(edge);
             }
         }
     }
 
     private void createSimpleHops(Graph graph, Trip trip, List<StopTime> stopTimes) {
 
         String tripId = id(trip.getId());
         ArrayList<Hop> hops = new ArrayList<Hop>();
         boolean tripWheelchairAccessible = trip.getWheelchairAccessible() != 0;
         
         for (int i = 0; i < stopTimes.size() - 1; i++) {
             StopTime st0 = stopTimes.get(i);
             Stop s0 = st0.getStop();
             StopTime st1 = stopTimes.get(i + 1);
             Stop s1 = st1.getStop();
             Vertex startStation = graph.getVertex(id(s0.getId()));
             Vertex endStation = graph.getVertex(id(s1.getId()));
 
             // create journey vertices
             Vertex startJourneyArrive = graph.addVertex(id(s0.getId()) + "_" + tripId, s0.getLon(),
                     s0.getLat());
             Vertex startJourneyDepart = graph.addVertex(id(s0.getId()) + "_" + tripId, s0.getLon(),
                     s0.getLat());
             Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + tripId, s1.getLon(), s1
                     .getLat());
 
             Dwell dwell = new Dwell(startJourneyArrive, startJourneyDepart, st0);
             graph.addEdge(dwell);
             Hop hop = new Hop(startJourneyDepart, endJourney, st0, st1);
             hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                     endJourney));
             hops.add(hop);
             Board boarding = new Board(startStation, startJourneyDepart, hop, tripWheelchairAccessible && s0.getWheelchairBoarding() != 0);
             graph.addEdge(boarding);
             graph.addEdge(new Alight(endJourney, endStation, hop, tripWheelchairAccessible && s1.getWheelchairBoarding() != 0));
         }
     }
 
     private Geometry getHopGeometry(AgencyAndId shapeId, StopTime st0, StopTime st1,
             Vertex startJourney, Vertex endJourney) {
 
         if (shapeId == null || shapeId.getId() == null || shapeId.getId().equals(""))
             return null;
 
         double startDistance = st0.getShapeDistTraveled();
         double endDistance = st1.getShapeDistTraveled();
 
         boolean hasShapeDist = st0.isShapeDistTraveledSet() && st1.isShapeDistTraveledSet();
 
         if (hasShapeDist) {
 
             ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
             LineString geometry = _geometriesByShapeSegmentKey.get(key);
             if (geometry != null)
                 return geometry;
 
             double[] distances = getDistanceForShapeId(shapeId);
 
             if (distances != null) {
 
                 LinearLocation startIndex = getSegmentFraction(distances, startDistance);
                 LinearLocation endIndex = getSegmentFraction(distances, endDistance);
 
                 LineString line = getLineStringForShapeId(shapeId);
                 LocationIndexedLine lol = new LocationIndexedLine(line);
 
                 return getSegmentGeometry(shapeId, lol, startIndex, endIndex, startDistance,
                         endDistance);
             }
         }
 
         LineString line = getLineStringForShapeId(shapeId);
         LocationIndexedLine lol = new LocationIndexedLine(line);
 
         LinearLocation startCoord = lol.indexOf(startJourney.getCoordinate());
         LinearLocation endCoord = lol.indexOf(endJourney.getCoordinate());
 
         double distanceFrom = startCoord.getSegmentLength(line);
         double distanceTo = endCoord.getSegmentLength(line);
 
         return getSegmentGeometry(shapeId, lol, startCoord, endCoord, distanceFrom, distanceTo);
     }
 
     private Geometry getSegmentGeometry(AgencyAndId shapeId,
             LocationIndexedLine locationIndexedLine, LinearLocation startIndex,
             LinearLocation endIndex, double startDistance, double endDistance) {
 
         ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
 
         LineString geometry = _geometriesByShapeSegmentKey.get(key);
         if (geometry == null) {
 
             geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex); 
 
             // Pack the resulting line string
             CoordinateSequence sequence = new PackedCoordinateSequence.Float(geometry
                     .getCoordinates(), 2);
             geometry = _factory.createLineString(sequence);
 
             _geometriesByShapeSegmentKey.put(key, geometry);
         }
 
         return geometry;
     }
 
     private LineString getLineStringForShapeId(AgencyAndId shapeId) {
 
         LineString geometry = _geometriesByShapeId.get(shapeId);
 
         if (geometry != null)
             return geometry;
 
         List<ShapePoint> points = _dao.getShapePointsForShapeId(shapeId);
         Coordinate[] coordinates = new Coordinate[points.size()];
         double[] distances = new double[points.size()];
 
         boolean hasAllDistances = true;
 
         int i = 0;
         for (ShapePoint point : points) {
             coordinates[i] = new Coordinate(point.getLon(), point.getLat());
             distances[i] = point.getDistTraveled();
             if (! point.isDistTraveledSet() )
                 hasAllDistances = false;
             i++;
         }
 
         /**
          * If we don't have distances here, we can't calculate them ourselves because we can't
          * assume the units will match
          */
 
         if (!hasAllDistances) {
             distances = null;
         }
 
         CoordinateSequence sequence = new PackedCoordinateSequence.Float(coordinates, 2);
         geometry = _factory.createLineString(sequence);
         _geometriesByShapeId.put(shapeId, geometry);
         _distancesByShapeId.put(shapeId, distances);
 
         return geometry;
     }
 
     private double[] getDistanceForShapeId(AgencyAndId shapeId) {
         getLineStringForShapeId(shapeId);
         return _distancesByShapeId.get(shapeId);
     }
 
     private LinearLocation getSegmentFraction(double[] distances, double distance) {
         int index = Arrays.binarySearch(distances, distance);
         if (index < 0)
             index = -(index + 1);
         if (index == 0)
             return new LinearLocation(0, 0.0);
         if (index == distances.length)
             return new LinearLocation(distances.length, 0.0);
 
         double indexPart = (distance - distances[index - 1])
                 / (distances[index] - distances[index - 1]);
         return new LinearLocation(index - 1, indexPart);
     }
 }
