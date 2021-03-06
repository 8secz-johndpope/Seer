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
  * File created on 11.02.2005.
  *
  * $Id: ListObject.java 230 2007-01-16 20:36:33Z lehni $
  */
 
 package com.scratchdisk.script.rhino;
 
 import org.mozilla.javascript.Context;
 import org.mozilla.javascript.Scriptable;
 import org.mozilla.javascript.Wrapper;
 
 import com.scratchdisk.list.ReadOnlyList;
 import com.scratchdisk.list.SimpleList;
 import com.scratchdisk.list.StringIndexList;
 
 /**
  * Wrapper class for com.scriptographer.util.List objects It adds array-like
  * properties, so it is possible to access lists like this: list[i] It also
  * defines getIds(), so enumeration is possible too: for (var i in list) ...
  * 
  * @author lehni
  */
 public class ListWrapper extends ExtendedJavaObject {
 	public ListWrapper(Scriptable scope, ReadOnlyList list,
 			Class staticType, boolean unsealed) {
 		super(scope, list, staticType, unsealed);
 	}
 
 	public Object[] getIds() {
 		if (javaObject != null) {
 			// act like a JS javaObject:
 			Integer[] ids = new Integer[((ReadOnlyList) javaObject).size()];
 			for (int i = 0; i < ids.length; i++) {
 				ids[i] = new Integer(i);
 			}
 			return ids;
 		} else {
 			return new Object[] {};
 		}
 	}
 
 	public boolean has(int index, Scriptable start) {
 		return javaObject != null && index < ((ReadOnlyList) javaObject).size();
 	}
 
 	public Object get(int index, Scriptable scriptable) {
 		if (javaObject != null) {
 			Object obj = ((ReadOnlyList) javaObject).get(index);
 			if (obj != null)
				return toObject(obj, scriptable);
 		}
 		return Scriptable.NOT_FOUND;
 	}
 
 	public boolean has(String name, Scriptable start) {
 		return super.has(name, start) || // TODO: needed? name.equals("length") ||
 			javaObject instanceof StringIndexList && javaObject != null && 
 				((StringIndexList) javaObject).get(name) != null;
 	}
 
 	public Object get(String name, Scriptable scriptable) {
 		Object obj = super.get(name, scriptable);
 		if (obj == Scriptable.NOT_FOUND && javaObject != null) {
 			 if (name.equals("length")) {
 				 return new Integer(((ReadOnlyList) javaObject).size());
 			 } else if (javaObject instanceof StringIndexList) {
 				obj = ((StringIndexList) javaObject).get(name);
 				if (obj != null)
					obj = toObject(obj, scriptable);
 				else
 					obj = Scriptable.NOT_FOUND;
 			}
 		}
 		return obj;
 	}
 
 	public void put(int index, Scriptable start, Object value) {
 		if (javaObject != null && javaObject instanceof SimpleList) {
 			SimpleList list = ((SimpleList) javaObject);
 			if (value instanceof Wrapper)
 				value = ((Wrapper) value).unwrap();
 			int size = list.size();
 			if (index > size) {
 				for (int i = size; i < index; i++)
 					list.add(i, null);
 				list.add(index, value);
 			} else {
 				list.set(index, value);
 			}
 		}
 	}
 }
