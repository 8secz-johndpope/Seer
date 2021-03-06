 package org.eclipse.recommenders.commons.client;
 
 /**
  * Copyright (c) 2010 Darmstadt University of Technology.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Johannes Lerch - initial API and implementation.
  */
 
 import javax.xml.bind.annotation.XmlRootElement;
 
 @XmlRootElement
 public class ResultObject<T> {
 
     public String id;
    // TODO: key not required by anyone?
    // public String key;
     public T value;
 }
