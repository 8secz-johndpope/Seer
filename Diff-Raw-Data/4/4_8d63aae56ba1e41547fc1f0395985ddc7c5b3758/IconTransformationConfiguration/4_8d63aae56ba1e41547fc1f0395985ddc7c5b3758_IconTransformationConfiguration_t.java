 /*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.xwiki.rendering.transformation.icon;
 
 import org.xwiki.component.annotation.ComponentRole;
 
 import java.util.Properties;
 
 /**
  * Configuration properties for the Icon transformation.
  *
  * @version $Id$
  * @since 2.6RC1
  */
 @ComponentRole
 public interface IconTransformationConfiguration
 {
     /**
      * @return the mappings between a set of characters representing an icon (eg ":)", "(y)") and an icon name
      *         (eg "emoticon_smile", "thumbs_up")
      */
     Properties getMappings();
 
     /**
      * Adds a new mapping between a set of characters representing an icon (eg ":)", "(y)") and an icon name
      * (eg "emoticon_smile", "thumbs_up").
      *
      * @param iconCharacters the set of characters representing the icon
      * @param iconName the icon name based on a well-known list of names
      */
     void addMapping(String iconCharacters, String iconName);
 }
