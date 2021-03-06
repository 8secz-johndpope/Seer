 /*
  * Copyright 2006 Wyona
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.wyona.org/licenses/APACHE-LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 package org.wyona.yanel.impl.map;
 
 import org.wyona.yanel.core.Path;
 import org.wyona.yanel.core.map.Map;
 
 import org.wyona.yarep.core.Repository;
 import org.wyona.yarep.core.RepositoryFactory;
 
 /**
  *
  */
 public class MapImpl implements Map {
 
     /**
      *
      */
     public MapImpl() {
         try {
             Repository repo = new RepositoryFactory().newRepository("yanel");
         } catch(Exception e) {
            System.err.println(e);
         }
     }
 
     /**
      *
      */
     public String getUUID() {
         return "sugus";
     }
 
     /**
      * See James Clark's explanation on namespaces: http://www.jclark.com/xml/xmlns.htm
      */
     public String getResourceTypeIdentifier(Path path) {
         return "<{http://www.wyona.org/yanel/resource/1.0}invoice/>";
     }
 }
