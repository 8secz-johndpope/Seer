 /*
  *  soapUI, copyright (C) 2004-2008 eviware.com 
  *
  *  soapUI is free software; you can redistribute it and/or modify it under the 
  *  terms of version 2.1 of the GNU Lesser General Public License as published by 
  *  the Free Software Foundation.
  *
  *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
  *  See the GNU Lesser General Public License for more details at gnu.org.
  */
 
 package com.eviware.soapui.impl.wsdl.panels.teststeps;
 
 import com.eviware.soapui.impl.EmptyPanelBuilder;
 import com.eviware.soapui.impl.rest.panels.request.AbstractRestRequestDesktopPanel;
 import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
 import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequest;
 import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep;
 import com.eviware.soapui.support.components.JPropertiesTable;
 
 import javax.swing.*;
 
 /**
  * PanelBuilder for HttpTestRequest
  *
  * @author Ole.Matzura
  */
 
 public class HttpTestRequestPanelBuilder extends EmptyPanelBuilder<HttpTestRequestStep>
 {
    public HttpTestRequestPanelBuilder()
    {
    }
 
    public AbstractRestRequestDesktopPanel buildDesktopPanel( HttpTestRequestStep testStep )
    {
       return testStep instanceof RestTestRequestStep ? new RestTestRequestDesktopPanel( (RestTestRequestStep) testStep ) :
               new HttpTestRequestDesktopPanel( testStep );
    }
 
    public boolean hasDesktopPanel()
    {
       return true;
    }
 
    public JPanel buildOverviewPanel( HttpTestRequestStep testStep )
    {
       RestTestRequest request = testStep.getTestRequest();
       JPropertiesTable<RestTestRequest> table = new JPropertiesTable<RestTestRequest>( "REST TestRequest Properties" );
 
       // basic properties
       table.addProperty( "Name", "name", true );
       table.addProperty( "Description", "description", true );
 //   	table.addProperty( "Message Size", "contentLength", false );
       table.addProperty( "Encoding", "encoding", new String[] { null, "UTF-8", "iso-8859-1" } );
 
       if( request.getOperation() != null )
          table.addProperty( "Endpoint", "endpoint", request.getInterface().getEndpoints() );
 
       table.addProperty( "Path", "path", true );
 
       table.addProperty( "Bind Address", "bindAddress", true );
 
       if( request.getOperation() != null )
       {
          table.addProperty( "Service", "service" );
         table.addProperty( "Resource", "path" );
       }
 
       // security / authentication
       table.addProperty( "Username", "username", true );
       table.addProperty( "Password", "password", true );
       table.addProperty( "Domain", "domain", true );
 
       // post-processing
       table.addProperty( "Pretty Print", "prettyPrint", JPropertiesTable.BOOLEAN_OPTIONS );
       table.addProperty( "Dump File", "dumpFile", true ).setDescription( "Dumps response message to specified file" );
       table.addProperty( "Max Size", "maxSize", true ).setDescription( "The maximum number of bytes to receive" );
       table.setPropertyObject( request );
 
       return table;
    }
 
    public boolean hasOverviewPanel()
    {
       return true;
    }
 }
