 /******************************************************************************* 
  * Copyright (c) 2009 Red Hat, Inc. 
  * Distributed under license by Red Hat, Inc. All rights reserved. 
  * This program is made available under the terms of the 
  * Eclipse Public License v1.0 which accompanies this distribution, 
  * and is available at http://www.eclipse.org/legal/epl-v10.html 
  * 
  * Contributors: 
  * Red Hat, Inc. - initial API and implementation 
  ******************************************************************************/ 
 package org.jboss.tools.cdi.core;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.IPath;
 
 /**
  * Common interface for objects of CDI model.
  * 
  * @author Alexey Kazakov
  */
 public interface ICDIElement {
 
 	/**
 	 * Returns CDI project that contains this object.
 	 * @return
 	 */
 	ICDIProject getCDIProject();
 
 	/**
 	 * Returns path of resource that declares this object.
 	 * @return
 	 */
 	IPath getSourcePath();
 
 	/**
 	 * Returns resource that declares this object.
 	 * @return resource 
 	 */
 	IResource getResource();
 }
