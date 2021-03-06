 /*
  * Scriptographer
  *
  * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
  *
  * Copyright (c) 2002-2007 Juerg Lehni, http://www.scratchdisk.com.
  * All rights reserved.
  *
  * Please visit http://scriptographer.com/ for updates and contact.
  *
  * -- GPL LICENSE NOTICE --
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  * -- GPL LICENSE NOTICE --
  *
  * File created on 21.12.2004.
  *
  * $Id$
  */
 
 package com.scriptographer.ai;
 
 import com.scriptographer.script.ScriptScope;
 import com.scriptographer.script.ScriptEngine;
 import com.scriptographer.script.ScriptException;
 import com.scriptographer.script.ScriptMethod;
 import com.scriptographer.util.IntMap;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Iterator;
 
 /**
  * @author lehni
  */
 public class Tool extends AIObject {
 	private int index;
 
 	private static IntMap tools = null;
 
 	protected Tool(int handle, int index) {
 		super(handle);
 		this.index = index;
 	}
 	
 	private ScriptScope scope;
 
 	private Event event = new Event();
 	private Object[] eventArgs = new Object[] { event };
 	
 	public void setScript(File file) throws ScriptException, IOException {
 		onInit = null;
 		onOptions = null;
 		onSelect = null;
 		onDeselect = null;
 		onReselect = null;
 		onMouseDown = null;
 		onMouseUp = null;
 		onMouseDrag = null;
 		onMouseMove = null;
 		ScriptEngine engine = ScriptEngine.getInstanceByFile(file);
 		// Execute in the tool's scope so setIdleInterval can be called
 		scope = engine.getScope(this);
 		ScriptEngine.executeFile(file, scope);
 		if (scope != null) {
 			setIdleEventInterval(-1);
 			try {
 				onInit();
 			} catch (ScriptException e) {
 				// rethrow
 				throw e;
 			} catch (Exception e) {
 				// cannot happen with scripts
 			}
 		}
 	}
 
 	private static IntMap getTools() {
 		if (tools == null)
 			tools = nativeGetTools();
 		return tools;
 	}
 
 	/**
 	 * Returns all tools that have been created by this plugin. This is
 	 * necessary because the java part of the plugin may be reloaded. The plugin
 	 * needs to be capable of reestablish the connections between the wrappers
 	 * and the real objects.
 	 * 
 	 * @return
 	 */
 	private static native IntMap nativeGetTools();
 
 	public native boolean hasPressure();
 	
 	// interval time in milliseconds
 	public native int getIdleEventInterval();
 	
 	public native void setIdleEventInterval(int interval);
 
 	private ScriptMethod onInit;
 
 	public ScriptMethod getOnInit() {
 		return onInit;
 	}
 
 	public void setOnInit(ScriptMethod onInit) {
 		this.onInit = onInit;
 	}
 	
 	protected void onInit() throws Exception {
 		if (scope != null && onInit != null)
 			onInit.execute(this);
 	}
 
 	/*
 	 * TODO: onOptions should be called onEditOptions, or both onOptions,
 	 * but at least the same.
 	 */
 	private ScriptMethod onOptions;
 
 	public ScriptMethod getOnOptions() {
 		return onOptions;
 	}
 
 	public void setOnOptions(ScriptMethod onOptions) {
 		this.onOptions = onOptions;
 	}
 
 	protected void onOptions() throws Exception {
 		if (scope != null && onOptions != null)
 			onOptions.execute(this);
 	}
 
 	private ScriptMethod onSelect;
 
 	public ScriptMethod getOnSelect() {
 		return onSelect;
 	}
 
 	public void setOnSelect(ScriptMethod onSelect) {
 		this.onSelect = onSelect;
 	}
 
 	protected void onSelect() throws Exception {
 		if (scope != null && onSelect != null)
 			onSelect.execute(this);
 	}
 	
 	private ScriptMethod onDeselect;
 
 	public ScriptMethod getOnDeselect() {
 		return onDeselect;
 	}
 
 	public void setOnDeselect(ScriptMethod onDeselect) {
 		this.onDeselect = onDeselect;
 	}
 	
 	protected void onDeselect() throws Exception {
 		if (scope != null && onDeselect != null)
 			onDeselect.execute(this);
 	}
 
 	private ScriptMethod onReselect;
 
 	public ScriptMethod getOnReselect() {
 		return onReselect;
 	}
 
 	public void setOnReselect(ScriptMethod onReselect) {
 		this.onReselect = onReselect;
 	}
 	
 	protected void onReselect() throws Exception {
 		if (scope != null && onReselect != null)
 			onReselect.execute(this);
 	}
 
 	private ScriptMethod onMouseDown;
 
 	public ScriptMethod getOnMouseDown() {
 		return onMouseDown;
 	}
 
 	public void setOnMouseDown(ScriptMethod onMouseDown) {
 		this.onMouseDown = onMouseDown;
 	}
 	
 	protected void onMouseDown(float x, float y, int pressure) throws Exception {
 		if (scope != null && onMouseDown != null) {
 			event.setValues(x, y, pressure);
			onMouseDown.execute(this, eventArgs);
 		}
 	}
 
 	private ScriptMethod onMouseDrag;
 
 	public ScriptMethod getOnMouseDrag() {
 		return onMouseDrag;
 	}
 
 	public void setOnMouseDrag(ScriptMethod onMouseDrag) {
 		this.onMouseDrag = onMouseDrag;
 	}
 	
 	protected void onMouseDrag(float x, float y, int pressure) throws Exception {
 		if (scope != null && onMouseDrag != null) {
 			event.setValues(x, y, pressure);
			onMouseDrag.execute(this, eventArgs);
 		}
 	}
 
 	private ScriptMethod onMouseMove;
 
 	public ScriptMethod getOnMouseMove() {
 		return onMouseMove;
 	}
 
 	public void setOnMouseMove(ScriptMethod onMouseMove) {
 		this.onMouseMove = onMouseMove;
 	}
 	
 	protected void onMouseMove(float x, float y, int pressure) throws Exception {
 		if (scope != null && onMouseMove != null) {
 			event.setValues(x, y, pressure);
			onMouseMove.execute(this, eventArgs);
 		}
 	}
 
 	private ScriptMethod onMouseUp;
 
 	public ScriptMethod getOnMouseUp() {
 		return onMouseUp;
 	}
 
 	public void setOnMouseUp(ScriptMethod onMouseUp) {
 		this.onMouseUp = onMouseUp;
 	}
 		
 	protected void onMouseUp(float x, float y, int pressure) throws Exception {
 		if (scope != null && onMouseUp != null) {
 			event.setValues(x, y, pressure);
			onMouseUp.execute(this, eventArgs);
 		}
 	}
 
 	private static final int EVENT_EDIT_OPTIONS = 0;
 	private static final int EVENT_TRACK_CURSOR = 1;
 	private static final int EVENT_MOUSE_DOWN = 2;
 	private static final int EVENT_MOUSE_DRAG = 3;
 	private static final int EVENT_MOUSE_UP = 4;
 	private static final int EVENT_SELECT = 5;
 	private static final int EVENT_DESELECT = 6;
 	private static final int EVENT_RESELECT = 7;
 
 	private final static String[] eventTypes = {
 		"AI Edit Options",
 		"AI Track Cursor",
 		"AI Mouse Down",
 		"AI Mouse Drag",
 		"AI Mouse Up",
 		"AI Select",
 		"AI Deselect",
 		"AI Reselect"
 	};
 	// hashmap for conversation to unique ids that can be compared with ==
 	// instead of .equals
 	private static HashMap events = new HashMap();
 
 	static {
 		for (int i = 0; i < eventTypes.length; i++)
 			events.put(eventTypes[i], new Integer(i));
 	}
 
 	/**
 	 * To be called from the native environment:
 	 */
 	private static void onHandleEvent(int handle, String selector, float x,
 			float y, int pressure) throws Exception {
 		Tool tool = getToolByHandle(handle);
 		if (tool != null) {
 			Integer event = (Integer) events.get(selector); 
 			if (event != null) {
 				switch(event.intValue()) {
 					case EVENT_EDIT_OPTIONS:
 						tool.onOptions();
 						break;
 					case EVENT_TRACK_CURSOR:
 						tool.onMouseMove(x, y, pressure);
 						break;
 					case EVENT_MOUSE_DOWN:
 						tool.onMouseDown(x, y, pressure);
 						break;
 					case EVENT_MOUSE_DRAG:
 						tool.onMouseDrag(x, y, pressure);
 						break;
 					case EVENT_MOUSE_UP:
 						tool.onMouseUp(x, y, pressure);
 						break;
 					case EVENT_SELECT:
 						tool.onSelect();
 						break;
 					case EVENT_DESELECT:
 						tool.onDeselect();
 						break;
 					case EVENT_RESELECT:
 						tool.onReselect();
 						break;
 				}
 			}
 		}
 	}
 
 	public static Tool getTool(int index) {
 		for (Iterator iterator = getTools().values().iterator();
 			iterator.hasNext();) {
 			Tool tool = (Tool) iterator.next();
 			if (tool.index == index)
 				return tool;
 		}
 		return null;
 	}
 
 	private static Tool getToolByHandle(int handle) {
 		return (Tool) getTools().get(handle);
 	}
 }
