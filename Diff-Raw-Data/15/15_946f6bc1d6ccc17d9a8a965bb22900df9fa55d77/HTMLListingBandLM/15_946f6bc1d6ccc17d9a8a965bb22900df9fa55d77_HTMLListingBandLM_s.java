 /***********************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Actuate Corporation - initial API and implementation
  ***********************************************************************/
 
 package org.eclipse.birt.report.engine.layout.html;
 
 import java.util.Collection;
 
 import org.eclipse.birt.report.engine.content.IBandContent;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IElement;
 import org.eclipse.birt.report.engine.content.IGroupContent;
 import org.eclipse.birt.report.engine.content.IListContent;
 import org.eclipse.birt.report.engine.content.ITableContent;
 import org.eclipse.birt.report.engine.emitter.IContentEmitter;
 import org.eclipse.birt.report.engine.executor.IReportItemExecutor;
 import org.eclipse.birt.report.engine.internal.executor.dom.DOMReportItemExecutor;
 
 public class HTMLListingBandLM extends HTMLBlockStackingLM
 {
 
 	public HTMLListingBandLM( HTMLLayoutManagerFactory factory )
 	{
 		super( factory );
 	}
 
 	public int getType( )
 	{
 		return LAYOUT_MANAGER_LIST_BAND;
 	}
 
 	boolean repeatHeader;
 
 	public void initialize( HTMLAbstractLM parent, IContent content,
 			IReportItemExecutor executor, IContentEmitter emitter )
 	{
 		super.initialize( parent, content, executor, emitter );
 		repeatHeader = false;
 		intializeHeaderContent( );
 	}
 
 	public void close( )
 	{
 		super.close( );
 		if ( repeatHeader )
 		{
 			assert executor instanceof DOMReportItemExecutor;
 			executor.close( );
 		}
 	}
 
 	private void intializeHeaderContent( )
 	{
 		assert content != null;
 		IElement pContent = content.getParent( );
 		assert pContent != null;
 		assert content instanceof IBandContent;
 
 		int type = ( (IBandContent) content ).getBandType( );
 		repeatHeader = false;
 		if ( type == IBandContent.BAND_HEADER
 				|| type == IBandContent.BAND_GROUP_HEADER )
 		{
 			if ( pContent instanceof IGroupContent )
 			{
 				IGroupContent groupContent = (IGroupContent) pContent;
 				repeatHeader = groupContent.isHeaderRepeat( );
 			}
 			else if ( pContent instanceof IListContent )
 			{
 				IListContent list = (IListContent) pContent;
 				repeatHeader = list.isHeaderRepeat( );
 			}
 			else if ( pContent instanceof ITableContent )
 			{
 				ITableContent table = (ITableContent) pContent;
 				repeatHeader = table.isHeaderRepeat( );
 			}
 		}
 
 		if ( repeatHeader )
 		{
 			Collection children = content.getChildren( );
 			if ( children == null || children.isEmpty( ) )
 			{
 				// fill the contents
 				execute( content, executor );
				pContent.getChildren( ).add( content );
 			}
 			executor = new DOMReportItemExecutor( content );
 			executor.execute( );
 		}
 	}
 }
