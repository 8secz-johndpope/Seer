 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: InvisibleLayerConfiguration.java
  *
  * Copyright (c) 2009 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
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
 package com.sun.electric.tool.user.ui;
 
 import com.sun.electric.database.text.Pref;
 import com.sun.electric.technology.Layer;
 import com.sun.electric.technology.Technology;
 import com.sun.electric.tool.user.menus.WindowMenu;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Class to manage saved collections of invisible layers.
  * Each configuration has a name, a list of Layers, and an associated Technology
  * (all Layers must be in the same Technology).
  */
 public class InvisibleLayerConfiguration
 {
 	private static InvisibleLayerConfiguration onlyOne = new InvisibleLayerConfiguration();
 
 	private Map<String,String> configurations;
 	private Pref savedConfigurations;
 
 	/**
 	 * Constructor gets the saved configurations from the Preferences.
 	 */
 	private InvisibleLayerConfiguration()
 	{
 		configurations = new HashMap<String,String>();
 		Pref.Group prefs = Pref.groupForPackage(InvisibleLayerConfiguration.class);
 		savedConfigurations = Pref.makeStringPref("LayerVisibilityConfigurations", prefs, "");
 		String sc = savedConfigurations.getString();
 		for(;;)
 		{
 			int openPos = sc.indexOf('[');
 			if (openPos < 0) break;
 			int closePos = sc.indexOf(']');
 			if (closePos < 0) break;
 			String config = sc.substring(openPos+1, closePos);
 			sc = sc.substring(closePos+1);
 			int tabPos = config.indexOf('\t');
 			if (tabPos < 0) continue;
 			String cName = config.substring(0, tabPos);
 			String con = config.substring(tabPos+1);
 			configurations.put(cName, con);
 		}
 	}
 
 	/**
 	 * Method to save the configuration state to the Preferences.
 	 */
 	private void saveConfigurations()
 	{
 		StringBuffer sb = new StringBuffer();
 		for(String cName : configurations.keySet())
 		{
 			String invisLayers = configurations.get(cName);
 			sb.append('[');
 			sb.append(cName);
 			sb.append('\t');
 			sb.append(invisLayers);
 			sb.append(']');
 		}
 		savedConfigurations.setString(sb.toString());
 		WindowMenu.setDynamicVisibleLayerMenus();
 	}
 
 	/**
 	 * Method to return the singleton of this class.
 	 * @return the only instance of this class.
 	 */
 	public static InvisibleLayerConfiguration getOnly() { return onlyOne; }
 
 	/**
 	 * Method to tell whether a invisible layer configuration name exists.
 	 * @param cName the name of the invisible layer configuration.
 	 * @return true if there is a invisible layer configuration with that name.
 	 */
 	public boolean exists(String cName)
 	{
 		String invisLayers = configurations.get(cName);
 		return invisLayers != null;
 	}
 
 	/**
 	 * Method to add a invisible layer configuration.
 	 * @param cName the name of the new invisible layer configuration.
 	 * @param tech the Technology in which these layers reside.
 	 * @param layers the list of invisible layers in the configuration.
 	 */
 	public void addConfiguration(String cName, Technology tech, List<Layer> layers)
 	{
 		StringBuffer sb = new StringBuffer();
 		for(Layer layer : layers)
 		{
			if (sb.length() == 0) sb.append(tech.getTechName());
			sb.append(',');
 			sb.append(layer.getName());
 		}
 		configurations.put(cName, sb.toString());
 		saveConfigurations();
 	}
 
 	/**
 	 * Method to delete an invisible layer configuration.
 	 * @param cName the name of the invisible layer configuration to delete.
 	 */
 	public void deleteConfiguration(String cName)
 	{
 		String invisLayers = configurations.get(cName);
 		if (invisLayers == null) return;
 		configurations.remove(cName);
 		saveConfigurations();
 	}
 
 	/**
 	 * Method to return the names of all invisible layer configurations.
 	 * @return a List of invisible layer configuration names.
 	 */
 	public List<String> getConfigurationNames()
 	{
 		List<String> configs = new ArrayList<String>();
 		for(String key : configurations.keySet())
 			configs.add(key);
         Collections.sort(configs);
 		return configs;
 	}
 
 	/**
 	 * Method to get the Technology associated with a invisible layer configuration.
 	 * @param cName the name of the invisible layer configuration.
 	 * @return the Technology associated with that invisible layer configuration
 	 * (may be null).
 	 */
 	public Technology getConfigurationTechnology(String cName)
 	{
 		Technology tech = null;
 		String invisLayers = configurations.get(cName);
 		if (invisLayers != null)
 		{
 			// make a set of all invisible layers
 			String[] iLayers = invisLayers.split(",");
 			if (iLayers.length != 0)
 			{
 				String techName = iLayers[0];
 				tech = Technology.findTechnology(techName);
 			}
 		}
 		return tech;
 	}
 
 	/**
 	 * Method to return the invisible layers in an invisible layer configuration.
 	 * @param cName the name of the invisible layer configuration.
 	 * @return a Set of Layers (may be empty).
 	 */
 	public Set<Layer> getConfigurationValue(String cName)
 	{
 		Set<Layer> invisibleLayers = new HashSet<Layer>();
 		String invisLayers = configurations.get(cName);
 		if (invisLayers != null)
 		{
 			// make a set of all invisible layers
 			String[] iLayers = invisLayers.split(",");
 			if (iLayers.length != 0)
 			{
 				String techName = iLayers[0];
 				Technology tech = Technology.findTechnology(techName);
 				for(int i=0; i<iLayers.length; i++)
 				{
 					String iLayer = iLayers[i];
 					Layer lay = tech.findLayer(iLayer);
 					if (lay == null) continue;
 					invisibleLayers.add(lay);
 				}
 			}
 		}
 		return invisibleLayers;
 	}
 }
