 /*
  * The JBoss Webtools contribution
  *
  * Copyright (c) 2004 JBoss, Inc.
  * Distributable under Common Public License v1.0
  * See terms of license at http:/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
  * 02110-1301 USA, or see the FSF site: http:/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
  * 02110-1301 USA, or see the FSF site: http:/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
 package org.jboss.ide.eclipse.as.ui;
 
 import java.util.Hashtable;
 import java.util.Iterator;
 
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.swt.graphics.Image;
 import org.osgi.framework.Bundle;
 
 /**
  * @author Marshall
  *
  * A class that keeps references and disposes of the UI plugin's images
  */
 public class JBossServerUISharedImages {
 
 	public static final String IMG_JBOSS = "jboss";
 	public static final String IMG_JBOSS_CONFIGURATION = "jbossconfiguration";
 
 	public static final String WIZBAN_JBOSS32_LOGO = "jboss32logo";
 	public static final String WIZBAN_JBOSS40_LOGO = "jboss40logo";
 	
 	public static final String CONSOLE_IMAGE = "CONSOLE_IMAGE";
 	public static final String PUBLISH_IMAGE = "PUBLISH_IMAGE";
 	public static final String UNPUBLISH_IMAGE = "UNPUBLISH_IMAGE";
 	public static final String TWIDDLE_IMAGE = "TWIDDLE_IMAGE";
 	
 	
 	private static JBossServerUISharedImages instance;
 	
 	private Hashtable images, descriptors;
 	
 	private JBossServerUISharedImages ()
 	{
		instance = this;
 		images = new Hashtable();
 		descriptors = new Hashtable();
 		Bundle pluginBundle = JBossServerUIPlugin.getDefault().getBundle();
 		
 		descriptors.put(IMG_JBOSS, createImageDescriptor(pluginBundle, "/icons/jboss.gif"));
 		descriptors.put(IMG_JBOSS_CONFIGURATION, createImageDescriptor(pluginBundle, "/icons/jboss-configuration.gif"));
 		
 		descriptors.put(WIZBAN_JBOSS32_LOGO, createImageDescriptor(pluginBundle, "/icons/logo32.gif"));
 		descriptors.put(WIZBAN_JBOSS40_LOGO, createImageDescriptor(pluginBundle, "/icons/logo40.gif"));
 		
 		
 		descriptors.put(CONSOLE_IMAGE, createImageDescriptor(pluginBundle, "/icons/console.gif"));
 		descriptors.put(PUBLISH_IMAGE, createImageDescriptor(pluginBundle, "/icons/publish.gif"));
 		descriptors.put(UNPUBLISH_IMAGE, createImageDescriptor(pluginBundle, "/icons/unpublish.gif"));
 		descriptors.put(TWIDDLE_IMAGE, createImageDescriptor(pluginBundle, "/icons/twiddle.gif"));
 		
 		
 		Iterator iter = descriptors.keySet().iterator();
 		while (iter.hasNext())
 		{
 			String key = (String) iter.next();
 			ImageDescriptor descriptor = descriptor(key);
 			images.put(key,  descriptor.createImage());	
 		}
 		
 	}
 	
 	private ImageDescriptor createImageDescriptor (Bundle pluginBundle, String relativePath)
 	{
 		return ImageDescriptor.createFromURL(pluginBundle.getEntry(relativePath));
 	}
 	
 	private static JBossServerUISharedImages instance() {
 		if (instance == null)
 			return new JBossServerUISharedImages();
 		
 		return instance;
 	}
 	
 	public static Image getImage(String key)
 	{
 		return instance().image(key);
 	}
 	
 	public static ImageDescriptor getImageDescriptor(String key)
 	{
 		return instance().descriptor(key);
 	}
 	
 	public Image image(String key)
 	{
 		return (Image) images.get(key);
 	}
 	
 	public ImageDescriptor descriptor(String key)
 	{
 		return (ImageDescriptor) descriptors.get(key);
 	}
 	
 	protected void finalize() throws Throwable {
 		Iterator iter = images.keySet().iterator();
 		while (iter.hasNext())
 		{
 			Image image = (Image) images.get(iter.next());
 			image.dispose();
 		}
 		super.finalize();
 	}
 }
