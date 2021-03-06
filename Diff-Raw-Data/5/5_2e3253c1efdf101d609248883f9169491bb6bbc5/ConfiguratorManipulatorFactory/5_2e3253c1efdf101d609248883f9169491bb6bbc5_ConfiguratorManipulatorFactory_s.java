 /*******************************************************************************
  * Copyright (c) 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.configuratormanipulator;
 
 /**
  * Factory class for creating ConfiguratorManipulator object from Java programs.
  * 
  *  @see FrameworkAdmin
  */
 public abstract class ConfiguratorManipulatorFactory {
 	public final static String SYSTEM_PROPERTY_KEY = "org.eclipse.equinox.configuratorManipulatorFactory";
 
 	abstract protected ConfiguratorManipulator createConfiguratorManipulator();
 
 	public static ConfiguratorManipulator getInstance(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
 		ConfiguratorManipulatorFactory factory = (ConfiguratorManipulatorFactory) Class.forName(className).newInstance();
		return (ConfiguratorManipulator) factory.createConfiguratorManipulator();
 	}
 }
