 /*
  * Copyright (c) 2003-2005 jMonkeyEngine
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are
  * met:
  *
  * * Redistributions of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  *
  * * Redistributions in binary form must reproduce the above copyright
  *   notice, this list of conditions and the following disclaimer in the
  *   documentation and/or other materials provided with the distribution.
  *
  * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
  *   may be used to endorse or promote products derived from this software 
  *   without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package com.jme.scene.shadow;
 
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 import java.util.ArrayList;
 import java.util.BitSet;
 
 import com.jme.light.DirectionalLight;
 import com.jme.light.Light;
 import com.jme.light.PointLight;
 import com.jme.math.Plane;
 import com.jme.math.Quaternion;
 import com.jme.math.Vector3f;
 import com.jme.renderer.Camera;
 import com.jme.scene.TriMesh;
 import com.jme.scene.state.LightState;
 import com.jme.system.DisplaySystem;
 import com.jme.util.geom.BufferUtils;
 
 /**
  * <code>MeshShadows</code> A grouping of the ShadowVolumes for a single
  * TriMesh.
  * 
  * @author Mike Talbot (some code from a shadow implementation written Jan 2005)
  * @author Joshua Slack
 * @version $Id: MeshShadows.java,v 1.2 2005-11-23 03:58:04 renanse Exp $
  */
 public class MeshShadows {
     private static final long serialVersionUID = 1L;
 
     /** the distance to which shadow volumes will be projected */
     protected float projectionLength = 1000;
 
     /** The triangles of our occluding mesh (one per triangle in the mesh) */
     protected ArrayList faces;
 
     /** A bitset used for storing directional flags. */
     protected BitSet facing;
 
     /** The mesh that is the target of this shadow volume */
     protected TriMesh target = null;
 
     /** The arraylist of shadowvolumes in this grouping */
     protected ArrayList volumes = new ArrayList();
 
     /** The world rotation of the target at the last mesh construction */
     protected Quaternion oldWorldRotation = new Quaternion();
 
     /** The world translation of the trimesh at the last mesh construction */
     protected Vector3f oldWorldTranslation = new Vector3f();
 
     /** The world scale of the trimesh at the last mesh construction */
     protected Vector3f oldWorldScale = new Vector3f();
 
     /** Static computation field */
     protected static Vector3f compVect = new Vector3f();
 
     /**
      * Constructor for <code>MeshShadows</code>
      * 
      * @param target
      *            the mesh that will be the target of the shadow volumes held in
      *            this grouping
      */
     public MeshShadows(TriMesh target) {
         this.target = target;
         recreateFaces();
     }
 
     /**
      * <code>createGeometry</code> creates or updates the ShadowVolume
      * geometries for the target TriMesh - one for each applicable Light in the
      * given LightState. Only Directional and Point lights are currently
      * supported.
      * 
      * ShadowVolume geometry is only regen'd when light or occluder aspects
      * change.
      * 
      * @param lightState
      *            is the current lighting state
      */
     public void createGeometry(LightState lightState) {
         if (target.getTriangleQuantity() != facing.size())
             recreateFaces();
 
         // Holds a copy of the vertices transformed to world coordinates
         FloatBuffer vertex = null;
         Camera cam = DisplaySystem.getDisplaySystem().getRenderer().getCamera();
         Plane viewPlane = new Plane();
         viewPlane.getNormal().set(cam.getDirection());
 
         float con = viewPlane.getNormal().dot(cam.getLocation())
                 + cam.getFrustumNear();
         viewPlane.setConstant(con + 0.001f);
 
         // Ensure that we have some lights to cast shadows!
         if (lightState.getQuantity() != 0) {
             LightState lights = lightState;
 
             // Update the cache of lights - if still sane, return
             if (updateCache(lights)) return;
 
             // Now scan through each light and create the shadow volume
             for (int l = 0; l < lights.getQuantity(); l++) {
                 Light light = lights.get(l);
                 // Make sure we can handle this type of light
                 if (!(light.getType() == Light.LT_DIRECTIONAL)
                         && !(light.getType() == Light.LT_POINT))
                     continue;
 
                 // Get the volume assoicated with this light
                 ShadowVolume lv = getShadowVolume(light);
 
                 // See if this light has been seen before!
                 if (lv == null) {
                     // Create a new light volume
                     lv = new ShadowVolume(light);
                     volumes.add(lv);
                 }
 
                 // See if the volume requires updating
                 if (lv.isUpdate()) {
                     lv.setUpdate(false);
 
                     // Translate the vertex information from the mesh to world
                     // coordinates if
                     // we are going to do any work
 
                     if (vertex == null)
                         vertex = target.getWorldCoords(null);
 
                     // Find out which triangles are facing the light
                     // triangle will be set true for faces towards the light
                     processFaces(vertex, light, target);
 
                     // Get the edges that are in shadow
                     ShadowEdge[] edges = getShadowEdges();
 
                     // Now we need to develop a mesh based on projecting these
                     // edges
                     // to infinity in the direction of the light
                     int length = edges.length;
 
                     // Create arrays to hold the shadow mesh
                     FloatBuffer shadowVertex = lv.getVertexBuffer();
                     if (shadowVertex == null
                             || shadowVertex.capacity() < length * 12)
                         shadowVertex = BufferUtils
                                 .createVector3Buffer(length * 4);
                     FloatBuffer shadowNormal = lv.getNormalBuffer();
                     if (shadowNormal == null
                             || shadowNormal.capacity() < length * 12)
                         shadowNormal = BufferUtils
                                 .createVector3Buffer(length * 4);
                     IntBuffer shadowIndex = lv.getIndexBuffer();
                     if (shadowIndex == null
                             || shadowIndex.capacity() < length * 6)
                         shadowIndex = BufferUtils.createIntBuffer(length * 6);
 
                     shadowVertex.limit(length * 12);
                     shadowNormal.limit(length * 12);
                     shadowIndex.limit(length * 6);
 
                     // Create quads out of the edge vertices
                     ArrayList cappingZones = createShadowQuads(viewPlane,
                             vertex, edges, shadowVertex, shadowNormal,
                             shadowIndex, light);
 
                     // Do we need to cap the volume
                     if (cappingZones != null) {
                         int extraVertices = 0;
                         int extraIndices = 0;
 
                         // Calculate the number of extra vertices and indices
                         for (int z = 0; z < cappingZones.size(); z++) {
                             int size = ((ArrayList) cappingZones.get(z)).size();
                             extraVertices += size;
                             extraIndices += ((size - 2) * 3);
                         }
                         extraVertices *= 3;
 
                         // find the insertion location
                         int vOffset = length * 4;
                         int iOffset = length * 6;
 
                         // Copy the existing ones to a new array
                         if (shadowVertex.capacity() < length * 12
                                 + extraVertices) {
                             FloatBuffer tempVertex = BufferUtils
                                     .createFloatBuffer(length * 12
                                             + extraVertices);
                             tempVertex.put(shadowVertex);
                             shadowVertex = tempVertex;
                         } else
                             shadowVertex.limit(length * 12 + extraVertices);
 
                         if (shadowNormal.capacity() < length * 12
                                 + extraVertices) {
                             FloatBuffer tempNormal = BufferUtils
                                     .createFloatBuffer(length * 12
                                             + extraVertices);
                             tempNormal.put(shadowNormal);
                             shadowNormal = tempNormal;
                         } else
                             shadowNormal.limit(length * 12 + extraVertices);
 
                         if (shadowIndex.capacity() < length * 6 + extraIndices) {
                             IntBuffer tempIndex = BufferUtils
                                     .createIntBuffer(length * 6 + extraIndices);
                             tempIndex.put(shadowIndex);
                             shadowIndex = tempIndex;
                         } else
                             shadowIndex.limit(length * 6 + extraIndices);
 
                         // get a normal for points on the plane
                         Vector3f normal = viewPlane.normal.negate();
 
                         // Loop through each cap we should make (these should be
                         // continuous loops)
                         for (int z = 0; z < cappingZones.size(); z++) {
                             ArrayList points = (ArrayList) cappingZones.get(z);
 
                             // Setup the first point - we will fan from here
                             int root = vOffset;
                             BufferUtils.setInBuffer((Vector3f) points.get(0),
                                     shadowVertex, vOffset);
                             BufferUtils.setInBuffer(normal, shadowNormal,
                                     vOffset);
                             vOffset++;
 
                             // loop for the remaining points
                             for (int p = 1, pSize = points.size(); p < pSize; p++) {
                                 // add the point
                                 Vector3f point = (Vector3f) points.get(p);
                                 BufferUtils.setInBuffer(point, shadowVertex,
                                         vOffset);
                                 BufferUtils.setInBuffer(normal, shadowNormal,
                                         vOffset);
                                 vOffset++;
                                 // if we are ready to form triangles then off we
                                 // go
                                 if (p >= 2) {
                                     // MikeT: Is winding order important here -
                                     // can I actually fix this?
                                     shadowIndex.put(iOffset++, root);
                                     shadowIndex.put(iOffset++, vOffset - 2);
                                     shadowIndex.put(iOffset++, vOffset - 1);
                                 }
                             }
                             shadowIndex.rewind();
                         }
                     }
 
                     // Rebuild the TriMesh
                     lv.reconstruct(shadowVertex, shadowNormal, null, null,
                             shadowIndex);
                     shadowVertex.rewind();
                     lv.setVertQuantity(shadowVertex.remaining() / 3);
                     shadowIndex.rewind();
                     lv.setTriangleQuantity(shadowIndex.remaining() / 3);
                 }
 
             }
 
         } else {
             // There are no volumes
             volumes.clear();
         }
 
     }
 
     /**
      * void <code>createShadowQuad</code> Creates projected quads from a
      * series of edges and vertices and stores them in the output shadowXXXX
      * arrays
      * 
      * @param vertex
      *            array of world coordinate vertices for the target TriMesh
      * @param edges
      *            a collection of edges that will be projected
      * @param shadowVertex
      * @param shadowNormal
      * @param shadowIndex
      * @param light
      *            light casting shadow
      */
     private ArrayList createShadowQuads(Plane viewPlane, FloatBuffer vertex,
             ShadowEdge[] edges, FloatBuffer shadowVertex,
             FloatBuffer shadowNormal, IntBuffer shadowIndex, Light light) {
         Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
 
         // Setup a flag to indicate which type of light this is
         boolean directional = (light.getType() == Light.LT_DIRECTIONAL);
 
         Vector3f direction = new Vector3f();
         Vector3f location = new Vector3f();
         if (directional) {
             direction = ((DirectionalLight) light).getDirection();
         } else {
             location = ((PointLight) light).getLocation();
         }
 
         boolean allSame = true;
 
         // Loop for each edge
         for (int e = 0; e < edges.length; e++) {
             // get the two known vertices
             BufferUtils.populateFromBuffer(p0, vertex, edges[e].p0);
             BufferUtils.populateFromBuffer(p3, vertex, edges[e].p1);
 
             // Calculate the projection of p0
             if (!directional) {
                 direction = p0.subtract(location, direction).normalizeLocal();
             }
             // Project the other edges to infinity
             p1 = direction.mult(projectionLength, p1).addLocal(p0);
             if (!directional) {
                 direction = p3.subtract(location, direction).normalizeLocal();
             }
             p2 = direction.mult(projectionLength).addLocal(p3);
 
             int side = viewPlane.whichSide(p0);
             if (viewPlane.whichSide(p1) != side)
                 allSame = false;
             if (viewPlane.whichSide(p2) != side)
                 allSame = false;
             if (viewPlane.whichSide(p3) != side)
                 allSame = false;
 
             // Now we need to add a quad to the model
             int vertexOffset = e * 4;
             BufferUtils.setInBuffer(p0, shadowVertex, vertexOffset);
             BufferUtils.setInBuffer(p1, shadowVertex, vertexOffset + 1);
             BufferUtils.setInBuffer(p2, shadowVertex, vertexOffset + 2);
             BufferUtils.setInBuffer(p3, shadowVertex, vertexOffset + 3);
 
             // Calculate the normal
             Vector3f n = p1.subtract(p0).normalizeLocal().crossLocal(
                     p3.subtract(p0).normalizeLocal()).normalizeLocal();
             BufferUtils.setInBuffer(n, shadowNormal, vertexOffset);
             BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 1);
             BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 2);
             BufferUtils.setInBuffer(n, shadowNormal, vertexOffset + 3);
 
             // Add the indices
             int indexOffset = e * 6;
             shadowIndex.put(indexOffset + 0, vertexOffset + 0);
             shadowIndex.put(indexOffset + 1, vertexOffset + 1);
             shadowIndex.put(indexOffset + 2, vertexOffset + 3);
             shadowIndex.put(indexOffset + 3, vertexOffset + 3);
             shadowIndex.put(indexOffset + 4, vertexOffset + 1);
             shadowIndex.put(indexOffset + 5, vertexOffset + 2);
         }
 
         // Now see if we have a problem
         if (allSame == false) {
             // If we do then we need to build in some capping
             ArrayList edgeLoops = getEdgeLoops(edges);
             ArrayList pointCollection = new ArrayList();
             // The edgeLoops arraylist now contains all of our points, but split
             // into convex loops (I hope)
             for (int loop = 0; loop < edgeLoops.size(); loop++) {
                 float t;
                 Vector3f v;
                 ArrayList loopEdges = (ArrayList) edgeLoops.get(loop);
                 // Create an arraylist to hold the intersection points
                 ArrayList points = new ArrayList();
                 for (int e = 0; e < loopEdges.size(); e++) {
                     // get the two known vertices
                     ShadowEdge edge = (ShadowEdge) loopEdges.get(e);
                     BufferUtils.populateFromBuffer(p0, vertex, edge.p0);
                     BufferUtils.populateFromBuffer(p3, vertex, edge.p1);
                     // Calculate the projection of p0
                     if (!directional) {
                         direction = p0.subtract(location, direction).normalizeLocal();
                     }
                     // Project the other edges to infinity
                     p1 = p0.add(direction.mult(projectionLength, p1));
                     if (!directional) {
                         direction = p3.subtract(location, direction).normalizeLocal();
                     }
                     p2 = direction.mult(projectionLength, p2).addLocal(p3);
 
                     // If the line between two points intersects the plane then
                     // store
                     // the location of the intersection
                     if (viewPlane.whichSide(p0) != viewPlane.whichSide(p1)) {
                         v = p1.subtract(p0);
                         t = getIntersectTime(viewPlane, p0, v);
                         if (t >= 0 && t <= 1) {
                             points.add(v.multLocal(t).addLocal(p0));
                         }
                     }
                     if (viewPlane.whichSide(p1) != viewPlane.whichSide(p2)) {
                         v = p2.subtract(p1);
                         t = getIntersectTime(viewPlane, p1, v);
                         if (t >= 0 && t <= 1) {
                             points.add(v.multLocal(t).addLocal(p1));
                         }
                     }
                     if (viewPlane.whichSide(p2) != viewPlane.whichSide(p3)) {
                         v = p3.subtract(p2);
                         t = getIntersectTime(viewPlane, p2, v);
                         if (t >= 0 && t <= 1) {
                             points.add(v.multLocal(t).addLocal(p2));
                         }
 
                     }
                     if (viewPlane.whichSide(p3) != viewPlane.whichSide(p0)) {
                         v = p0.subtract(p3);
                         t = getIntersectTime(viewPlane, p3, v);
                         if (t >= 0 && t <= 1) {
                             points.add(v.multLocal(t).addLocal(p3));
                         }
 
                     }
 
                 }
                 if (points.size() > 2)
                     pointCollection.add(points);
             }
             if (pointCollection.size() != 0)
                 return pointCollection;
             else
                 return null;
         } else
             return null;
 
     }
 
     /**
      * void <code>getEdgeLoops</code> creates an ArrayList of ArrayLists that
      * contain continous loops of points in potential silhouette edges
      * 
      * @param edges
      *            the edges that are potential silhouettes
      * @return an ArrayList containing an ArrayList of Vector3f points for each
      *         of the loops
      */
     private ArrayList getEdgeLoops(ShadowEdge[] edges) {
         // Create an array list the contains all of the edges
         ArrayList allEdges = new ArrayList();
         // Create an arraylist to hold the loops
         ArrayList arLoops = new ArrayList();
         // Create a first entry for the first loop
         ArrayList loopEdges = new ArrayList();
         // Add the first loop arraylist
         arLoops.add(loopEdges);
         // Loop while we still have edges
 
         // for now presume that there is only one loop
         for (int e = 0; e < edges.length; e++) {
             allEdges.add(edges[e]);
             loopEdges.add(edges[e]);
         }
 
         // //Initialise the test item
         // ShadowEdge test = null;
         // while(allEdges.size()>0)
         // {
         // //Check if we are intialising a new loop
         // if( test == null )
         // {
         // //If so then get the first edge
         // test = (ShadowEdge) allEdges.get(0);
         // allEdges.remove(0);
         // //add it to the loop
         // loopEdges.add(test);
         // }
         // //Scan all remaining edges
         // int e=0;
         // for(e = 0;e<allEdges.size();e++)
         // {
         // ShadowEdge scan = (ShadowEdge) allEdges.get(e);
         // //Test to see if the edges match up
         // if( test.p0 == scan.p0 || test.p1 == scan.p0 || test.p0 == scan.p1 ||
         // test.p1 == scan.p1)
         // {
         // //If so then add the new edge and make it the test one
         // test = scan;
         // loopEdges.add(scan);
         // allEdges.remove(e);
         // e = 0;
         // }
         // }
         // //If we found no matching edges and there are edges left then
         // //we need to start a new loop
         // if( allEdges.size() != 0 && e >= allEdges.size() )
         // {
         // test = null;
         // loopEdges = new ArrayList();
         // arLoops.add(loopEdges);
         // }
         // }
         // //Return the arraylist of loops
         // System.out.println(arLoops.size());
         return arLoops;
     }
 
     // Get the intersection of a line segment and a plane in terms of t>=0 t<=1
     // for positions within the segment
     protected float getIntersectTime(Plane p, Vector3f p0, Vector3f v) {
 
         float divider = p.normal.dot(v);
         if (divider == 0)
             return -Float.MAX_VALUE;
         return p.normal.dot(p.normal.mult(p.constant, compVect).subtractLocal(p0))
                 / divider;
 
     }
 
     /**
      * <code>getShadowEdges</code>
      * 
      * @return an array of the edges which are in shadow
      */
     private ShadowEdge[] getShadowEdges() {
         // Create a dynamic structure to contain the vertices
         ArrayList shadowEdges = new ArrayList();
         // Now work through the faces
        for (int t = 0, fSize = facing.size(); t < fSize; t++) {
             // Check whether this is a front facing triangle
             if (facing.get(t)) {
                 ShadowTriangle tri = (ShadowTriangle) faces.get(t);
                 // If it is then check if any of the edges are connected to a
                 // back facing triangle or are unconnected
                 checkAndAdd(tri.edge1, shadowEdges);
                 checkAndAdd(tri.edge2, shadowEdges);
                 checkAndAdd(tri.edge3, shadowEdges);
             }
         }
         return (ShadowEdge[]) shadowEdges.toArray(new ShadowEdge[0]);
     }
 
     private void checkAndAdd(ShadowEdge edge, ArrayList shadowEdges) {
         // Is the edge connected
         if (edge.triangle == ShadowTriangle.INVALID_TRIANGLE) {
             // if not then add the edge
             shadowEdges.add(edge);
 
         }
         // check if the connected triangle is back facing
         else if (!facing.get(edge.triangle)) {
             // if it is then add the edge
             shadowEdges.add(edge);
 
         }
     }
 
     /**
      * <code>processFaces</code> Determines whether faces of a TriMesh face
      * the light
      * 
      * @param triangle
      *            an array of boolean values that will indicate whether a
      *            triangle is front or back facing
      * @param light
      *            the light to use
      * @param target
      *            the TriMesh that will be shadowed and holds the triangles for
      *            testing
      */
     private void processFaces(FloatBuffer vertex, Light light, TriMesh target) {
         Vector3f v0 = new Vector3f();
         Vector3f v1 = new Vector3f();
         boolean directional = light.getType() == Light.LT_DIRECTIONAL;
         Vector3f vLight = null;
         int[] index = BufferUtils.getIntArray(target.getIndexBuffer());
 
         if (directional) {
             vLight = ((DirectionalLight) light).getDirection();
         }
 
         // Loop through each triangle and see if it is back or front facing
         for (int t = 0, tri = 0; t < index.length; tri++, t += 3) {
             // Calculate a normal to the plane
             BufferUtils.populateFromBuffer(v0, vertex, index[t + 1]);
             BufferUtils.populateFromBuffer(v1, vertex, index[t + 2]);
             BufferUtils.populateFromBuffer(compVect, vertex, index[t]);
             v1.subtractLocal(v0).normalizeLocal();
             v0.subtractLocal(compVect).normalizeLocal();
             Vector3f n = v1.cross(v0);
 
             // Some kind of bodge for a direction to a point light - TODO
             // improve this
             if (!directional) {
                 vLight = compVect.subtract(((PointLight) light).getLocation())
                         .normalizeLocal();
             }
             // See if it is back facing
             facing.set(tri, (n.dot(vLight) >= 0));
         }
     }
 
     /**
      * <code>updateCache</code> Updates the cache to show which models need
      * rebuilding
      * 
      * @param lights
      *            a LightState for the lights to use
      * @return returns <code>true</code> if the cache was not invalidated
      */
     private boolean updateCache(LightState lights) {
         boolean voidAll = false;
         boolean same = true;
 
         // First see if we need to void all volumes as the target has changed
         if (!target.getWorldRotation().equals(oldWorldRotation))
             voidAll = true;
         if (!target.getWorldScale().equals(oldWorldScale))
             voidAll = true;
         if (!target.getWorldTranslation().equals(oldWorldTranslation))
             voidAll = true;
         // Configure the current settings
         oldWorldRotation.set(target.getWorldRotation());
         oldWorldScale.set(target.getWorldScale());
         oldWorldTranslation.set(target.getWorldTranslation());
 
         // See if we need to update all of the volumes
         if (voidAll) {
             for (int v = 0, vSize = volumes.size(); v < vSize; v++) {
                 ((ShadowVolume) volumes.get(v)).setUpdate(true);
             }
             return false;
         }
 
         // Loop through the lights to see if any have changed
         for (int i = 0; i < lights.getQuantity(); i++) {
             Light testLight = lights.get(i);
             ShadowVolume v = getShadowVolume(testLight);
             if (v != null) {
                 if (testLight.getType() == Light.LT_DIRECTIONAL) {
                     DirectionalLight dl = (DirectionalLight) testLight;
                     if (!v.direction.equals(dl.getDirection())) {
                         v.setUpdate(true);
                         v.setDirection(dl.getDirection());
                         same = false;
                     }
                 } else if (testLight.getType() == Light.LT_POINT) {
                     PointLight pl = (PointLight) testLight;
                     if (!v.getPosition().equals(pl.getLocation())) {
                         v.setUpdate(true);
                         v.setPosition(pl.getLocation());
                         same = false;
                     }
                 }
             }
         }
         return same;
     }
 
     // Checks whether two edges are connected and sets triangle field if they
     // are.
     private void edgeConnected(int face, IntBuffer index, int index1, int index2,
             ShadowEdge edge) {
         edge.p0 = index1;
         edge.p1 = index2;
 
        for (int t = 0, fSize = facing.size(); t < fSize; t++) {
             if (t != face) {
                 int offset = t * 3;
                 int t0 = index.get(offset), t1 = index.get(offset+1), t2 = index.get(offset+2);
                 if ((t0 == index1 && t1 == index2)
                         || (t1 == index1 && t2 == index2)
                         || (t2 == index1 && t0 == index2)
                         || (t0 == index2 && t1 == index1)
                         || (t1 == index2 && t2 == index1)
                         || (t2 == index2 && t0 == index1)) {
                     // Edges are connected
                     edge.triangle = t;
                     return;
                 }
             }
         }
     }
 
     /**
      * <code>recreateFaces</code> creates a triangle array for every triangle
      * in the target occluder mesh and stores it in the faces field. This is
      * only done rarely in general.
      */
     public void recreateFaces() {
         // make a copy of the original indices
         IntBuffer index = BufferUtils.clone(target.getIndexBuffer());
         index.clear();
 
 // TODO: To be useful, still needs to actually strip out the vertices unused.
 //        // holds the vertices
 //        FloatBuffer vertex = target.getVertexBuffer();
 //
 //        // holds the number of real vertices
 //        int validVertices = 1;
 //
 //        // Optimise out shared vertices to reduce the complexity of the shadow
 //        // volumes
 //        Vector3f test = new Vector3f();
 //        for (int i = 1, iSize = index.capacity(); i < iSize; i++) {
 //            BufferUtils.populateFromBuffer(test, vertex, index.get(i));
 //            for (int j = 0; j < i; j++) {
 //                BufferUtils.populateFromBuffer(compVect, vertex, index.get(j));
 //                // See if the tested vector is the same
 //                if (compVect.equals(test)) {
 //                    // swap the vertex for the duplicate one
 //                    index.put(i, index.get(j));
 //                    validVertices--;
 //                    break;
 //                }
 //            }
 //            validVertices++;
 //        }
 //        
 
         // Create a ShadowTriangle object for each face
         faces = new ArrayList();
 
         // Create a bitset for holding direction flags
        facing = new BitSet(index.capacity() / 3);
 
         // Loop through all of the triangles
        for (int t = 0, fSize = facing.size(); t < fSize; t++) {
             ShadowTriangle tri = new ShadowTriangle();
             faces.add(tri);
             int offset = t * 3;
             int t0 = index.get(offset), t1 = index.get(offset+1), t2 = index.get(offset+2);
             edgeConnected(t, index, t0, t1, tri.edge1);
             edgeConnected(t, index, t1, t2, tri.edge2);
             edgeConnected(t, index, t2, t0, tri.edge3);
         }
     }
 
     /**
      * <code>getShadowVolume</code> returns the shadow volume contained in
      * this grouping for a particular light
      * 
      * @param light
      *            the light whose shadow volume should be returned
      * @return a shadow volume for the light or null if one does not exist
      */
     public ShadowVolume getShadowVolume(Light light) {
         for (int v = 0, vSize = volumes.size(); v < vSize; v++) {
             ShadowVolume vol = (ShadowVolume) volumes.get(v);
             if (vol.light.equals(light))
                 return vol;
         }
         return null;
     }
 
     /**
      * @return Returns the projectionLength.
      */
     public float getProjectionLength() {
         return projectionLength;
     }
 
     /**
      * @param projectionLength The projectionLength to set.
      */
     public void setProjectionLength(float projectionLength) {
         this.projectionLength = projectionLength;
         // force update of volumes
         for (int v = 0, vSize = volumes.size(); v < vSize; v++) {
             ((ShadowVolume) volumes.get(v)).setUpdate(true);
         }
     }
 
     /**
      * @return Returns the volumes.
      */
     public ArrayList getVolumes() {
         return volumes;
     }
 
 }
