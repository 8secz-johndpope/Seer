 /*
  * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Scott Ferguson
  */
 
 package com.caucho.server.security;
 
 import com.caucho.loader.EnvironmentLocal;
 
 import java.security.Principal;
 import javax.annotation.PostConstruct;
 
 /**
  * Manages role mapping
  */
 public class RoleMapManager
 {
   private static final EnvironmentLocal<RoleMapManager> _localManager
     = new EnvironmentLocal<RoleMapManager>();
 
   private final RoleMapManager _parent;
 
   private ArrayList<RoleMap> _roleMapList
     = new ArrayList<RoleMap>();
 
   private RoleMapManager(RoleMapManager parent)
   {
     _parent = parent;
   }
 
   /**
    * Returns the current manager.
    */
   public static RoleMapManager getCurrent()
   {
     return _localManager.get();
   }
 
   /**
    * Returns the current manager, creating if necessary.
    */
   public static RoleMapManager create()
   {
     synchronized (_localManager) {
       RoleMapManager manager = _localManager.getLevel();
 
       if (manager == null) {
 	RoleMapManager parent = _localManager.get();
 
 	manager = new RoleMapManager(parent);
 	_localManager.set(manager);
       }
 
       return manager;
     }
   }
 
   /**
    * Adds a role map.
    */
   public void addRoleMap(RoleMap roleMap)
   {
     _roleMapList.add(roleMap);
   }
 }
