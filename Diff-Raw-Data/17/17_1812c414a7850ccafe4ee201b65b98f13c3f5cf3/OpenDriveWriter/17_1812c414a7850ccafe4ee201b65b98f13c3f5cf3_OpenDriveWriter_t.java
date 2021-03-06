 /**
  * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
  *                             <movsim.org@gmail.com>
  * ---------------------------------------------------------------------------------------------------------------------
  * 
  *  This file is part of 
  *  
  *  MovSim - the multi-model open-source vehicular-traffic simulator 
  *
  *  MovSim is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
  *  as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
  *  version.
  *
  *  MovSim is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
  *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License along with MovSim.
  *  If not, see <http://www.gnu.org/licenses/> or <http://www.movsim.org>.
  *  
  * ---------------------------------------------------------------------------------------------------------------------
  */
 
 package org.movsim.input.file.opendrive;
 
 import java.util.ArrayList;
 
 import org.movsim.input.file.xml.XMLWriterBase;
 import org.movsim.roadmappings.RoadMappingArc;
 import org.movsim.roadmappings.RoadMappingBezier;
 import org.movsim.roadmappings.RoadMappingCircle;
 import org.movsim.roadmappings.RoadMappingLine;
 import org.movsim.roadmappings.RoadMappingPoly;
 import org.movsim.roadmappings.RoadMappingPolyBezier;
 import org.movsim.roadmappings.RoadMappingPolyLine;
 import org.movsim.roadmappings.RoadMappingSpiral;
 import org.movsim.simulator.roadnetwork.Lane;
 import org.movsim.simulator.roadnetwork.RoadMapping;
 import org.movsim.simulator.roadnetwork.RoadMapping.PosTheta;
 import org.movsim.simulator.roadnetwork.RoadNetwork;
 import org.movsim.simulator.roadnetwork.RoadSegment;
 
 /**
  * <p>
  * Saves the road network to an OpenDRIVE format file, see: http://www.opendrive.org/docs/OpenDRIVEFormatSpecRev1.3D.pdf
  * </p>
  * <p>
  * Currently only a very primitive implementation to show proof of concept.
  * </p>
  */
 @SuppressWarnings({ "nls", "boxing" })
 public class OpenDriveWriter extends XMLWriterBase {
 
     static class Junction {
         static final String NOT_JUNCTION = "-1";
         static int nextId = 1001;
         String id;
         String name;
 
         static class Connection {
             String id;
             String incomingRoadId;
             String connectingRoadId;
 
             static enum ContactPoint {
                 START, END
             }
 
             ContactPoint contactPoint = ContactPoint.START;
 
             static class LaneLink {
                 int from;
                 int to;
 
                 LaneLink(int from, int to) {
                     this.from = from;
                     this.to = to;
                 }
             }
 
             ArrayList<LaneLink> laneLinks = new ArrayList<LaneLink>();
         }
 
         ArrayList<Connection> connections = new ArrayList<Connection>();
 
         Connection getConnection(String incommingRoadId, String connectingRoadId) {
             for (final Connection connection : connections) {
                 if (connection.incomingRoadId.equals(incommingRoadId) && connection.connectingRoadId.equals(connectingRoadId)) {
                     return connection;
                 }
             }
             final Connection ret = new Connection();
             ret.incomingRoadId = incommingRoadId;
             ret.connectingRoadId = connectingRoadId;
             connections.add(ret);
             return ret;
         }
     }
 
     ArrayList<Junction> junctions = new ArrayList<Junction>();
 
     private OpenDriveWriter() {
     }
 
     private static final String geometryFormat = "s=\"%f\" x=\"%f\" y=\"%f\" hdg=\"%f\" length=\"%f\"";
     private static final String geometryCommentFormat = " hdgDegrees=\"%f\"";
     private static final String poly3Format = "a=\"%f\" b=\"%f\" c=\"%f\" d=\"%f\"";
 
     /**
      * Save the road network to file.
      * 
      * @param roadNetwork
      */
     public static void saveRoadNetwork(RoadNetwork roadNetwork) {
         final OpenDriveWriter writer = new OpenDriveWriter();
         write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
         writer.startTag("OpenDRIVE");
         writer.writeTag(
                 "header",
                 "revMajor=\"1\" revMinor=\"2\" name=\"\" version=\"1.00\" date=\"Thu Sep 2 20:31:10 2010\" north=\"0.0\" south=\"0.0\" east=\"0.0\" west=\"0.0\"");
         writer.externalize(roadNetwork);
         writer.endTag("OpenDRIVE");
     }
 
     private void externalize(RoadNetwork roadNetwork) {
         assert roadNetwork != null;
         createJunctions(roadNetwork);
         for (final RoadSegment roadSegment : roadNetwork) {
             externalize(roadSegment);
         }
         externalize(junctions);
     }
 
     private void createJunctions(RoadNetwork roadNetwork) {
         for (final RoadSegment roadSegment : roadNetwork) {
             if (predecessorId2(roadSegment) == null) {
                 // need a junction for the predecessor
                 // connection is from incomingRoad to connectingRoad
                 final Junction junction = new Junction();
                 final String connectingRoadId = roadSegment.userId();
                 final int laneCount = roadSegment.laneCount();
                 for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
                     final RoadSegment sourceRoadSegment = roadSegment.sourceRoadSegment(lane);
                     if (sourceRoadSegment != null) {
                         final String incomingRoadId = sourceRoadSegment.userId();
                         final Junction.Connection connection = junction.getConnection(incomingRoadId, connectingRoadId);
                         connection.laneLinks.add(new Junction.Connection.LaneLink(laneToRightLaneId(sourceRoadSegment,
                                 roadSegment.sourceLane(lane)), laneToRightLaneId(roadSegment, lane)));
                     }
                 }
                 if (junction.connections.size() > 0) {
                     junctions.add(junction);
                     junction.id = Integer.toString(Junction.nextId++);
                 }
             }
             if (successorId2(roadSegment) == null) {
                 // need a junction for the successor
                 // connection is from connectingRoad to incomingRoad
                 final Junction junction = new Junction();
                 final String connectingRoadId = roadSegment.userId();
                 final int laneCount = roadSegment.laneCount();
                 for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
                     final RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(lane);
                     if (sinkRoadSegment != null) {
                         final String incomingRoadId = sinkRoadSegment.userId();
                         final Junction.Connection connection = junction.getConnection(incomingRoadId, connectingRoadId);
                         connection.laneLinks.add(new Junction.Connection.LaneLink(laneToRightLaneId(roadSegment, lane),
                                 laneToRightLaneId(sinkRoadSegment, roadSegment.sinkLane(lane))));
                     }
                 }
                 if (junction.connections.size() > 0) {
                     junction.id = Integer.toString(Junction.nextId++);
                     junctions.add(junction);
                 }
             }
         }
     }
 
     private void externalize(ArrayList<Junction> junctions) {
         if (junctions.size() == 0) {
             return;
         }
         final String junctionFormat = "id=\"%s\" name=\"\"";
        final String connectionFormat = "id=\"%s\" incomingRoad=\"%s\" connectingRoad=\"%s\" contactPoint=\"%s\"";
         final String laneLinkFormat = "from=\"%d\" to=\"%d\"";
         for (final Junction junction : junctions) {
             startTag("junction", String.format(junctionFormat, junction.id));
             int id = 0;
             for (final Junction.Connection connection : junction.connections) {
                 final String contactPoint = connection.contactPoint == Junction.Connection.ContactPoint.START ? "start"
                         : "end";
                 startTag("connection", String.format(connectionFormat, id, connection.incomingRoadId,
                         connection.connectingRoadId, contactPoint));
                 for (final Junction.Connection.LaneLink laneLink : connection.laneLinks) {
                     writeTag("laneLink", String.format(laneLinkFormat, laneLink.from, laneLink.to));
                 }
                 endTag("connection");
                 ++id;
             }
             endTag("junction");
         }
     }
 
     private void externalize(RoadSegment roadSegment) {
         assert roadSegment != null;
         final RoadMapping roadMapping = roadSegment.roadMapping();
         assert roadMapping != null;
        final String roadFormat = "id=\"%s\" name=\"R%s\" length=\"%f\" junction=\"%s\"";
         final String junctionId = findJunctionByConnectingRoadId(roadSegment.userId());
         final String s = String.format(roadFormat, roadSegment.userId(), roadSegment.userId(), roadSegment.roadLength(),
                 junctionId);
         startTag("road", s);
         startTag("link");
        final String junctionLinkFormat = "elementType=\"junction\" elementId=\"%s\"";
        final String roadLinkFormat = "elementType=\"road\" elementId=\"%s\" contactPoint=\"%s\"";
         final String predecessorJunctionId;
         final RoadSegment aPredecessor = aPredecessor(roadSegment);
         if (aPredecessor == null) {
             predecessorJunctionId = Junction.NOT_JUNCTION;
         } else {
             predecessorJunctionId = findJunctionByIncomingRoadId(aPredecessor.userId());
             if (predecessorJunctionId.equals(Junction.NOT_JUNCTION)) {
                 final String predecessorId = predecessorId(roadSegment);
                 writeTag("predecessor", String.format(roadLinkFormat, predecessorId, "end"));
             } else {
                 writeTag("predecessor", String.format(junctionLinkFormat, junctionId));
             }
         }
         final String successorJunctionId;
         final RoadSegment aSuccessor = aSuccessor(roadSegment);
         if (aSuccessor == null) {
             successorJunctionId = Junction.NOT_JUNCTION;
         } else {
             successorJunctionId = findJunctionByIncomingRoadId(aSuccessor.userId());
             if (successorJunctionId.equals(Junction.NOT_JUNCTION)) {
                 final String successorId = successorId(roadSegment);
                 writeTag("successor", String.format(roadLinkFormat, successorId, "start"));
             } else {
                 writeTag("successor", String.format(junctionLinkFormat, junctionId));
             }
         }
         endTag("link");
         startTag("planView");
         externalize(roadMapping);
         endTag("planView");
         startTag("lanes");
         startTag("laneSection", "s=\"0.0\"");
         startTag("right");
         final String laneFormat = "id=\"%d\" type=\"%s\" level=\"false\"";
         final String laneWidthFormat = "sOffset=\"0.0\" a=\"%f\" b=\"0.0\" c=\"0.0\" d=\"0.0\"";
         for (int lane = roadSegment.laneCount() - 1; lane >= Lane.LANE1; --lane) {
             final String type = type(roadSegment, lane);
             startTag("lane", String.format(laneFormat, laneToRightLaneId(roadSegment, lane), type));
             final int sourceLane = roadSegment.sourceLane(lane);
             final RoadSegment sourceRoadSegment = roadSegment.sourceRoadSegment(lane);
             final int sinkLane = roadSegment.sinkLane(lane);
             final RoadSegment sinkRoadSegment = roadSegment.sourceRoadSegment(lane);
             boolean linkTag = false;
             final String idFormat = "id=\"%d\"";
             if (sourceLane != Lane.NONE && sourceRoadSegment != null && predecessorJunctionId.equals(Junction.NOT_JUNCTION)) {
                 linkTag = true;
                 startTag("link");
                 writeTag("predecessor", String.format(idFormat, laneToRightLaneId(sourceRoadSegment, sourceLane)));
             }
             if (sinkLane != Lane.NONE && sinkRoadSegment != null && successorJunctionId.equals(Junction.NOT_JUNCTION)) {
                 if (linkTag == false) {
                     linkTag = true;
                     startTag("link");
                 }
                 writeTag("successor", String.format(idFormat, laneToRightLaneId(sinkRoadSegment, sinkLane)));
             }
             if (linkTag == true) {
                 endTag("link");
             }
             writeTag("width", String.format(laneWidthFormat, roadMapping.laneWidth()));
             endTag("lane");
         }
         endTag("right");
         endTag("laneSection");
         endTag("lanes");
         endTag("road");
     }
 
     private void externalize(RoadMapping roadMapping) {
 
         if (roadMapping.getClass().equals(RoadMappingCircle.class)) {
             externalize((RoadMappingCircle) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingArc.class)) {
             externalize((RoadMappingArc) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingLine.class)) {
             externalize((RoadMappingLine) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingSpiral.class)) {
             externalize((RoadMappingSpiral) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingPoly.class)) {
             externalize((RoadMappingPoly) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingPolyLine.class)) {
             externalize((RoadMappingPolyLine) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingBezier.class)) {
             externalize((RoadMappingBezier) roadMapping);
         } else if (roadMapping.getClass().equals(RoadMappingPolyBezier.class)) {
             externalize((RoadMappingPolyBezier) roadMapping);
         } else {
             write("<!--RoadMappingXML-->");
         }
     }
 
     private void externalize(RoadMappingCircle roadMapping) {
         final PosTheta posTheta = roadMapping.startPos();
         startTagWithComment("geometry",
                 String.format(geometryFormat, 0.0, posTheta.x, posTheta.y, posTheta.theta(), roadMapping.roadLength()),
                 String.format(geometryCommentFormat, Math.toDegrees(posTheta.theta())));
         final String curvatureCommentFormat = " radius=\"%f\"";
         writeTagWithComment("arc", String.format("curvature=\"%f\"", 1.0 / roadMapping.radius()),
                 String.format(curvatureCommentFormat, roadMapping.radius()));
         endTag("geometry");
     }
 
     private void externalize(RoadMappingLine roadMapping) {
         final PosTheta posTheta = roadMapping.startPos();
         startTagWithComment("geometry",
                 String.format(geometryFormat, 0.0, posTheta.x, posTheta.y, posTheta.theta(), roadMapping.roadLength()),
                 String.format(geometryCommentFormat, Math.toDegrees(posTheta.theta())));
         writeTag("line");
         endTag("geometry");
     }
 
     private void externalize(RoadMappingArc roadMapping) {
         final PosTheta posTheta = roadMapping.startPos();
         startTagWithComment("geometry",
                 String.format(geometryFormat, 0.0, posTheta.x, posTheta.y, posTheta.theta(), roadMapping.roadLength()),
                 String.format(geometryCommentFormat, Math.toDegrees(posTheta.theta())));
         double curvature = 1.0 / roadMapping.radius();
         if (roadMapping.arcAngle() < 0.0) {
             curvature = -curvature;
         }
         final String curvatureCommentFormat = " radius=\"%f\"";
         writeTagWithComment("arc", String.format("curvature=\"%f\"", curvature),
                 String.format(curvatureCommentFormat, roadMapping.radius()));
         endTag("geometry");
     }
 
     private void externalize(RoadMappingSpiral roadMapping) {
         final PosTheta posTheta = roadMapping.startPos();
         startTagWithComment("geometry",
                 String.format(geometryFormat, 0.0, posTheta.x, posTheta.y, posTheta.theta(), roadMapping.roadLength()),
                 String.format(geometryCommentFormat, Math.toDegrees(posTheta.theta())));
         final String commentFormat = " radiusStart=\"%f\" radiusEnd=\"%f\"";
         writeTagWithComment(
                 "spiral",
                 String.format("curvStart=\"%f\" curvEnd=\"%f\"", roadMapping.startCurvature(),
                         roadMapping.endCurvature()),
                 String.format(commentFormat, 1.0 / roadMapping.startCurvature(), 1.0 / roadMapping.endCurvature()));
         endTag("geometry");
     }
 
     private void externalize(RoadMappingPoly roadMappings) {
         for (final RoadMapping roadMapping : roadMappings) {
             externalize(roadMapping);
         }
     }
 
     private void externalize(RoadMappingPolyLine roadMappings) {
         double start = 0.0;
         for (final RoadMappingLine roadMapping : roadMappings) {
             final PosTheta posTheta = roadMapping.startPos();
             startTag(
                     "geometry",
                     String.format(geometryFormat, start, posTheta.x, posTheta.y, posTheta.theta(),
                             roadMapping.roadLength()));
             start += roadMapping.roadLength();
             writeTag("line");
             endTag("geometry");
         }
     }
 
     private void externalize(RoadMappingBezier roadMapping) {
         final PosTheta posTheta = roadMapping.startPos();
         startTag("geometry",
                 String.format(geometryFormat, 0.0, posTheta.x, posTheta.y, posTheta.theta(), roadMapping.roadLength()));
         writeTag("poly3", String.format(poly3Format, 0.0, 0.0, 0.0, 0.0));
         endTag("geometry");
     }
 
     private void externalize(RoadMappingPolyBezier roadMappings) {
         double start = 0.0;
         for (final RoadMappingBezier roadMapping : roadMappings) {
             final PosTheta startPosTheta = roadMapping.startPos();
             startTag("geometry", String.format(geometryFormat, start, startPosTheta.x, startPosTheta.y,
                     startPosTheta.theta(), roadMapping.roadLength()));
             start += roadMapping.roadLength();
             final PosTheta endPosTheta = roadMapping.endPos();
             final double cX = roadMapping.controlX(0);
             final double cY = roadMapping.controlY(0);
             // TODO - currently erroneously saving polyBezier end pos in attributes a & b
             writeTag("poly3", String.format(poly3Format, endPosTheta.x, endPosTheta.y, cX, cY));
             endTag("geometry");
         }
     }
 
     private final RoadSegment aPredecessor(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sourceRoadSegment = roadSegment.sourceRoadSegment(lane);
             if (sourceRoadSegment != null) {
                 return sourceRoadSegment;
             }
         }
         return null;
     }
 
     private final RoadSegment aSuccessor(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(lane);
             if (sinkRoadSegment != null) {
                 return sinkRoadSegment;
             }
         }
         return null;
     }
 
     private final String predecessorId(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sourceRoadSegment = roadSegment.sourceRoadSegment(lane);
             if (sourceRoadSegment != null) {
                 return sourceRoadSegment.userId();
             }
         }
         return null;
     }
 
     private final String predecessorId2(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         String prevId = null;
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sourceRoadSegment = roadSegment.sourceRoadSegment(lane);
             final String id = sourceRoadSegment == null ? null : sourceRoadSegment.userId();
             if (prevId != null && id != null && !prevId.equals(id)) {
                 return null;
             }
             if (id != null) {
                 prevId = id;
             }
         }
         return prevId;
     }
 
     private final String successorId(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(lane);
             if (sinkRoadSegment != null) {
                 return sinkRoadSegment.userId();
             }
         }
         return null;
     }
 
     private final String successorId2(RoadSegment roadSegment) {
         final int laneCount = roadSegment.laneCount();
         String prevId = null;
         for (int lane = Lane.LANE1; lane < laneCount; ++lane) {
             final RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(lane);
             final String id = sinkRoadSegment == null ? null : sinkRoadSegment.userId();
             if (prevId != null && id != null && !prevId.equals(id)) {
                 return null;
             }
             if (id != null) {
                 prevId = id;
             }
         }
         return prevId;
     }
 
     private String findJunctionByIncomingRoadId(String incomingRoadId) {
         for (final Junction junction : junctions) {
             for (final Junction.Connection connection : junction.connections) {
                 if (connection.incomingRoadId.equals(incomingRoadId)) {
                     return junction.id;
                 }
             }
         }
         return Junction.NOT_JUNCTION;
     }
 
     private String findJunctionByConnectingRoadId(String connectingRoadId) {
         for (final Junction junction : junctions) {
             for (final Junction.Connection connection : junction.connections) {
                 if (connection.connectingRoadId.equals(connectingRoadId)) {
                     return junction.id;
                 }
             }
         }
         return Junction.NOT_JUNCTION;
     }
 
     private static int laneToRightLaneId(RoadSegment roadSegment, int lane) {
         final int rightLaneId = lane - roadSegment.laneCount();
         assert rightLaneId < 0;
         return rightLaneId;
     }
 
     private static String type(RoadSegment roadSegment, int lane) {
         final Lane.Type laneType = roadSegment.laneType(lane);
         // OpenDrive lane types not currently supported: none stop sidewalk border parking special1 special2 special3
         switch (laneType) {
         case TRAFFIC:
             return "driving";
         case ENTRANCE:
             return "mwyEntry";
         case EXIT:
             return "mwyExit";
         case SHOULDER:
             return "shoulder";
         case RESTRICTED:
             return "restricted";
         case BICYCLE:
             return "biking";
         }
         return "none";
     }
 }
