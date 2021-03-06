 /*******************************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.report.engine.content;
 
 import org.eclipse.birt.report.engine.api.InstanceID;
 import org.eclipse.birt.report.engine.css.engine.CSSEngine;
 import org.eclipse.birt.report.engine.ir.DimensionType;
 
 /**
  * column definition used by table content.
  * 
  * 
 * @version $Revision: 1.5 $ $Date: 2006/06/13 06:31:10 $
  */
 public interface IColumn
 {
 
 	/**
 	 * @return Returns the width.
 	 */
 	public DimensionType getWidth( );
 
 	public String getStyleClass();
 	
 	/**
 	 * get the instance id of the column.
 	 * the instance id is the unique id of the content.
 	 * @return
 	 */
 	public InstanceID getInstanceID();
 	
 	public String getVisibleFormat( );
 	
 	/**
 	 * @return inline style
 	 */
 	IStyle getInlineStyle( );
 
 	IStyle getStyle( );
 
 	void setInlineStyle( IStyle style );
 	
 	public void setGenerateBy( Object generateBy );
 	
 	public Object getGenerateBy( );
 
 	public boolean hasDataItemsInDetail( );
 }
