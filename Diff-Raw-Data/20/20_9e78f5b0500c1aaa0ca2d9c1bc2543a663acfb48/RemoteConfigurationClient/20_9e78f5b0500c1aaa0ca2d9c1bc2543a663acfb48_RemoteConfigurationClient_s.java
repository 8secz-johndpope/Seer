 /*
  * This file is part of the OpenSCADA project
  * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
  *
  * OpenSCADA is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License version 3
  * only, as published by the Free Software Foundation.
  *
  * OpenSCADA is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License version 3 for more details
  * (a copy is included in the LICENSE file that accompanied this code).
  *
  * You should have received a copy of the GNU Lesser General Public License
  * version 3 along with OpenSCADA. If not, see
  * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
  */
 
 package org.openscada.ca.client.jaxws;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.xml.namespace.QName;
 import javax.xml.ws.BindingProvider;
 import javax.xml.ws.Service;
 import javax.xml.ws.handler.MessageContext;
 
 import org.openscada.ca.servlet.jaxws.RemoteConfigurationAdministrator;
 
 public class RemoteConfigurationClient
 {
     private static final QName serviceName = QName.valueOf ( "{http://jaxws.servlet.ca.openscada.org/}ConfigurationAdministratorServiceService" );
 
     private final RemoteConfigurationAdministrator port;
 
     public RemoteConfigurationClient ( final String hostname, final int port ) throws MalformedURLException
     {
         this ( new URL ( String.format ( "http://%s:%s/org.openscada.ca.servlet.jaxws?WSDL", hostname, port ) ), serviceName );
     }
 
     public RemoteConfigurationClient ( final String url ) throws MalformedURLException
     {
         this ( new URL ( url ), serviceName );
     }
 
     public RemoteConfigurationClient ( final String url, final String serviceName ) throws MalformedURLException
     {
         this ( new URL ( url ), QName.valueOf ( serviceName ) );
     }
 
     public RemoteConfigurationClient () throws MalformedURLException
     {
         this ( new URL ( "http://localhost:9999/org.openscada.ca.servlet.jaxws?WSDL" ), serviceName );
     }
 
     public RemoteConfigurationClient ( final URL url ) throws MalformedURLException
     {
         this ( url, serviceName );
     }
 
     public RemoteConfigurationClient ( final URL url, final QName serviceName )
     {
         this.port = createPort ( url );
     }
 
     protected RemoteConfigurationAdministrator createPort ( final URL url )
     {
         final Service service = javax.xml.ws.Service.create ( url, serviceName );
 
         final RemoteConfigurationAdministrator port = service.getPort ( RemoteConfigurationAdministrator.class );
 
         // adding "Accept-Encoding"
         final Map<String, List<String>> httpHeaders = new HashMap<String, List<String>> ();
         httpHeaders.put ( "Accept-Encoding", Collections.singletonList ( "gzip" ) );
 
         final Map<String, Object> requestContext = ( (BindingProvider)port ).getRequestContext ();
         requestContext.put ( MessageContext.HTTP_REQUEST_HEADERS, httpHeaders );
 
         // security 
 
         final String user = url.getUserInfo ();
 
        final int findColon = user.indexOf ( ':' );
        if ( findColon > 0 && findColon + 1 < user.length () )
         {
            requestContext.put ( BindingProvider.USERNAME_PROPERTY, user.substring ( 0, findColon ) );
            requestContext.put ( BindingProvider.PASSWORD_PROPERTY, user.substring ( findColon + 1 ) );
        }
        else
        {
            requestContext.put ( BindingProvider.USERNAME_PROPERTY, user );
         }
 
         return port;
     }
 
     public RemoteConfigurationAdministrator getPort ()
     {
         return this.port;
     }
 }
