 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jface.text;
 
 
 
 /**
  * Default implementation of <code>ITypedRegion</code>.
  */
 public class TypedRegion extends Region implements ITypedRegion {
 	
 	/** The region's type */
 	private String fType;
 	
 	/**
 	 * Creates a typed region based on the given specification.
 	 *
 	 * @param offset the region's offset
 	 * @param length the region's length
 	 * @param type the region's type
 	 */
 	public TypedRegion(int offset, int length, String type) {
 		super(offset, length);
 		fType= type;
 	}
 	
 	/*
 	 * @see ITypedRegion#getType()
 	 */
 	public String getType() {
 		return fType;
 	}
 	
 	/**
 	 * Two typed positions are equal if they have the same offset, length, and type.
 	 *
	 * @see Object#equals()
 	 */
 	public boolean equals(Object o) {
 		if (o instanceof TypedRegion) {
 			TypedRegion r= (TypedRegion) o;
 			return super.equals(r) && ((fType == null && r.getType() == null) || fType.equals(r.getType()));
 		}
 		return false;
 	}
 	
 	/*
 	 * @see Object#hashCode()
 	 */
 	 public int hashCode() {
 	 	int type= fType == null ? 0 : fType.hashCode();
 	 	return super.hashCode() | type;
 	 }	
 }
