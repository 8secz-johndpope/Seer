 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: Resources.java
  *
  * Copyright (c) 2004 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.tool.user;
 
 import javax.swing.ImageIcon;
 import java.net.URL;
 
 /**
  * public class to handle resources like icons/images.
  */
 public class Resources {
 	private static final String resourceLocation = "resources/";
 
 	// Location of valid 3D plugin
 	private static final String plugin3D = "com.sun.electric.plugins.j3d";
     private static final String pluginJMF = "com.sun.electric.plugins.jmf";
 	/**
 	 * Method to load a valid icon stored in resources package under the given class.
 	 * @param theClass class path where the icon resource is stored under
 	 * @param iconName icon name
 	 */
 	public static ImageIcon getResource(Class theClass, String iconName)
 	{
 		return (new ImageIcon(getURLResource(theClass, iconName)));
 	}
 
 	/**
 	 * Method to get URL path for a resource stored in resources package under the given class.
 	 * @param theClass class path where resource is stored under
 	 * @param resourceName resource name
 	 * @return a URL for the requested resource.
 	 */
 	public static URL getURLResource(Class theClass, String resourceName)
 	{
 		return (theClass.getResource(resourceLocation+resourceName));
 	}
 
     public static Class getJMFClass(String name)
     {
 		return (getClass(name, pluginJMF));
     }
 
     public static Class get3DClass(String name)
     {
         return (getClass(name, plugin3D));
     }
 
     private static Class getClass(String name, String plugin)
     {
        Class jmfClass = null;
 		try
         {
            jmfClass = Class.forName(plugin+"."+name);
 
         } catch (ClassNotFoundException e)
         {
            System.out.println("Can't find class '" + name +
                     "' from " + plugin + " plugin: " + e.getMessage());
         } catch (Error e)
         {
             System.out.println(plugin + " not installed: " + e.getMessage());
         }
		return (jmfClass);
     }
 
     private static Class view3DClass = null;
 	/**
 	 * Method to obtain main 3D class. Singlenton pattern
 	 * @return the main 3D class.
 	 */
 	public static Class get3DMainClass()
 	{
         if (view3DClass == null)
             view3DClass = get3DClass("View3DWindow");
         return (view3DClass);
 	}
 }
