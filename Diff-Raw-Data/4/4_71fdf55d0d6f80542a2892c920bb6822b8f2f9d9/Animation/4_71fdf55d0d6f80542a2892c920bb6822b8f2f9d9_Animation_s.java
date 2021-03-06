 /*
  * Copyright (c) 2009-2011 jMonkeyEngine
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
 package com.jme3.animation;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.Collection;
 import java.util.List;
 
 import com.jme3.export.InputCapsule;
 import com.jme3.export.JmeExporter;
 import com.jme3.export.JmeImporter;
 import com.jme3.export.OutputCapsule;
 import com.jme3.export.Savable;
 import com.jme3.scene.Geometry;
 import com.jme3.scene.Mesh;
 import com.jme3.scene.Node;
 import com.jme3.scene.Spatial;
 
 /**
  * The animation class updates the animation target with the tracks of a given type.
  * @author Kirill Vainer, Marcin Roguski (Kaelthas)
  */
 public class Animation implements Savable, Cloneable {
 	/** The name of the animation. */
 	private String name;
 	/** The length of the animation. */
     private float length;
     /** The tracks of the animation. */
     private Track<?>[] tracks;
     
     /**
      * Serialization-only. Do not use.
      */
     public Animation() {}
     
     /**
      * Creates a new BoneAnimation with the given name and length.
      * 
      * @param name The name of the bone animation.
      * @param length Length in seconds of the bone animation.
      */
     public Animation(String name, float length) {
         this.name = name;
         this.length = length;
     }
     
     /**
      * @return the name of the animation
      */
     public String getName() {
     	return name;
     }
     
     /**
      * @return the length of the animation
      */
     public float getLength() {
     	return length;
     }
     
     /**
      * This method sets the current time of the animation.
      * This method behaves differently for every known track type.
      * Override this method if you have your own type of track.
      * @param time the time of the animation
      * @param blendAmount the blend amount factor
      * @param control the nimation control
      * @param channel the animation channel
      */
     void setTime(float time, float blendAmount, AnimControl control, AnimChannel channel) {
     	if(tracks != null && tracks.length > 0) {
     		Track<?> trackInstance = tracks[0];
     		if(trackInstance instanceof SpatialTrack) {
     			Spatial spatial = control.getSpatial();
     			if (spatial != null) {
     				((SpatialTrack)tracks[0]).setTime(time, spatial, blendAmount);
     			}
     		} else if(trackInstance instanceof BoneTrack) {
     			BitSet affectedBones = channel.getAffectedBones();
     	        Skeleton skeleton = control.getSkeleton();
     	        for (int i = 0; i < tracks.length; ++i) {
     	            if (affectedBones == null || affectedBones.get(((BoneTrack)tracks[i]).getTargetIndex())) {
     	            	((BoneTrack)tracks[i]).setTime(time, skeleton, blendAmount);
     	            }
     	        }
     		} else if(trackInstance instanceof PoseTrack) {
     			Spatial spatial = control.getSpatial();
     			List<Mesh> meshes = new ArrayList<Mesh>();
     			this.getMeshes(spatial, meshes);
     			if(meshes.size() > 0) {
     				Mesh[] targets = meshes.toArray(new Mesh[meshes.size()]);
     				for (int i = 0; i < tracks.length; ++i){
         	            ((PoseTrack)tracks[i]).setTime(time, targets, blendAmount);
         	        }
     			}
     		}
     	}
     }
     
     /**
      * This method returns the meshes within the given spatial.
      * @param spatial the spatial to search the meshes from
      * @param meshes the collection that will have the found meshes
      */
     private void getMeshes(Spatial spatial, Collection<Mesh> meshes) {
     	if(spatial instanceof Geometry) {
     		meshes.add(((Geometry) spatial).getMesh());
 		} else if(spatial instanceof Node) {
 			for(Spatial child : ((Node) spatial).getChildren()) {
 				this.getMeshes(child, meshes);
 			}
 		}
     }
     
     /**
      * Set the {@link Track}s to be used by this animation.
      * <p>
      * The array should be organized so that the appropriate BoneTrack can
      * be retrieved based on a bone index. 
      * 
      * @param tracks the tracks to set
      */
     public void setTracks(Track<?>[] tracks){
         this.tracks = tracks;
     }
     
     /**
      * @return the tracks of the animation
      */
     public Track<?>[] getTracks() {
     	return tracks;
     }
     
     /**
      * This method creates a clone of the current object.
      * @return a clone of the current object
      */
 	public Animation clone() {
     	try {
     		Animation result = (Animation) super.clone();
     		if (tracks != null) {
     			result.tracks = tracks.clone();
     			for (int i = 0; i < tracks.length; ++i) {
     				result.tracks[i] = this.tracks[i].clone();
                 }
             }
     		return result;
         } catch (CloneNotSupportedException e) {
             throw new AssertionError();
         }
     }
 
     @Override
     public String toString() {
         return "Animation[name=" + name + ", length=" + length + ']';
     }
     
 	@Override
 	public void write(JmeExporter ex) throws IOException {
 		OutputCapsule out = ex.getCapsule(this);
         out.write(name, "name", null);
         out.write(length, "length", 0f);
         out.write(tracks, "tracks", null);
 	}
 
 	@Override
 	public void read(JmeImporter im) throws IOException {
 		InputCapsule in = im.getCapsule(this);
         name = in.readString("name", null);
         length = in.readFloat("length", 0f);
        tracks = (Track<?>[]) in.readSavableArray("tracks", null);
 	}
 }
