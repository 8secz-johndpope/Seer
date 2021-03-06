 package org.red5.server;
 
 /*
  * RED5 Open Source Flash Server - http://www.osflash.org/red5
  * 
  * Copyright (c) 2006 by respective authors (see below). All rights reserved.
  * 
  * This library is free software; you can redistribute it and/or modify it under the 
  * terms of the GNU Lesser General Public License as published by the Free Software 
  * Foundation; either version 2.1 of the License, or (at your option) any later 
  * version. 
  * 
  * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
  * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public License along 
  * with this library; if not, write to the Free Software Foundation, Inc., 
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
  */
 
 import org.red5.server.api.IGlobalScope;
 import org.red5.server.api.IScope;
 import org.red5.server.api.IScopeResolver;
 import org.red5.server.exception.ScopeNotFoundException;
 
 public class ScopeResolver implements IScopeResolver {
 
 	public static final String DEFAULT_HOST = "";
 	
 	protected IGlobalScope globalScope;
 
 	public IGlobalScope getGlobalScope() {
 		return globalScope;
 	}
 
 	public void setGlobalScope(IGlobalScope root) {
 		this.globalScope = root;
 	}
 
 	public IScope resolveScope(String path){
 		IScope scope = globalScope;
 		if(path == null) return scope;
 		final String[] parts = path.split("/");
 		for(int i=0; i < parts.length; i++){
 			final String room = parts[i];
 			if (room.equals(""))
 				// Skip empty path elements
 				continue;
			if (scope.hasChildScope(room)){
				scope = scope.getScope(room);
			} else if (!scope.equals(globalScope) && scope.createChildScope(room)){
				scope = scope.getScope(room);
			} else throw new ScopeNotFoundException(scope,parts[i]);
 		}
 		return scope;
 	}
 	
 }
