 /*******************************************************************************
  * Copyright (c) 2005 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.report.engine.script.internal.instance;
 
 import org.eclipse.birt.report.engine.api.script.instance.IReportItemInstance;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.executor.ExecutionContext;
 
 /**
  * A class representing the runtime state of a report item
  */
 public class ReportItemInstance extends ReportElementInstance implements
 		IReportItemInstance
 {
 
 	public ReportItemInstance( IContent content, ExecutionContext context )
 	{
 		super( content, context );
 	}
 
 	protected void setContent( IContent content )
 	{
 		this.content = content;
 	}
 
 	protected ReportItemInstance( ExecutionContext context )
 	{
 		super( context );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.script.instance.IReportInstance#getHyperlink()
 	 */
 	public String getHyperlink( )
 	{
		return content.getHyperlinkAction( ).getHyperlink( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.script.instance.IReportInstance#getName()
 	 */
 	public String getName( )
 	{
 		return content.getName( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.script.instance.IReportInstance#setName(java.lang.String)
 	 */
 	public void setName( String name )
 	{
 		content.setName( name );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.script.instance.IReportInstance#getHelpText()
 	 */
 	public String getHelpText( )
 	{
 		return content.getHelpText( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.script.instance.IReportInstance#setHelpText(java.lang.String)
 	 */
 	public void setHelpText( String helpText )
 	{
 		content.setHelpText( helpText );
 	}
 
 }
