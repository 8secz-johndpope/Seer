 /*
  * Copyright (c) 2003, jMonkeyEngine - Mojo Monkey Coding All rights reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * 
  * Redistributions of source code must retain the above copyright notice, this
  * list of conditions and the following disclaimer.
  * 
  * Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  * 
  * Neither the name of the Mojo Monkey Coding, jME, jMonkey Engine, nor the
  * names of its contributors may be used to endorse or promote products derived
  * from this software without specific prior written permission.
  * 
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  * POSSIBILITY OF SUCH DAMAGE.
  *  
  */
 package com.jme.effects.transients;
 import com.jme.image.Texture;
 import com.jme.scene.Controller;
 import com.jme.scene.Node;
 import com.jme.scene.state.TextureState;
 /**
  * <code>Transient</code>
  * 
  * @author Ahmed
 * @version $Id: Transient.java,v 1.3 2004-04-04 18:16:51 darkprophet Exp $
  *          provides a base at which furthur effects can be made of
  */
 public class Transient extends Node {
 	
 	private int maxNumOfStages, currentStage;
 	private TextureState inTex, outTex;
 	
	public Transient(String name, Texture out, Texture in) {
 		super(name);
 	}
 
 	protected int getCurrentStage() {
 		return currentStage;
 	}
 
 	protected void setCurrentStage(int stage) {
 		currentStage = stage;
 	}
 
 	protected int getNumOfStages() {
 		return maxNumOfStages;
 	}
 
 	protected void setNumOfStages(int num) {
 		maxNumOfStages = num;
 	}
 
 	protected TextureState getOutTexture() {
 		return outTex;
 	}
 	
 	protected void setOutTexture(TextureState out) {
 		outTex = out;
 	}
 	
 	protected TextureState getInTexture() {
 		return inTex;
 	}
 	
 	protected void setInTexture(TextureState in) {
 		inTex = in;
 	}
 }
