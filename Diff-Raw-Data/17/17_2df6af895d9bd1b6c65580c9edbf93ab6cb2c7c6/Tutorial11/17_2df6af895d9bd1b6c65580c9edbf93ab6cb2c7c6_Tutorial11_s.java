 /*
  * Copyright 2005 Jrme LOUVEL
  * 
  * The contents of this file are subject to the terms 
  * of the Common Development and Distribution License 
  * (the "License").  You may not use this file except 
  * in compliance with the License.
  * 
  * You can obtain a copy of the license at 
  * http://www.opensource.org/licenses/cddl1.txt 
  * See the License for the specific language governing 
  * permissions and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL 
  * HEADER in each file and include the License file at 
  * http://www.opensource.org/licenses/cddl1.txt
  * If applicable, add the following below this CDDL 
  * HEADER, with the fields enclosed by brackets "[]"
  * replaced with your own identifying information: 
  * Portions Copyright [yyyy] [name of copyright owner]
  */
 
 package com.noelios.restlet.tutorial;
 
 import org.restlet.AbstractRestlet;
 import org.restlet.DefaultMaplet;
 import org.restlet.Maplet;
 import org.restlet.Restlet;
 import org.restlet.RestletCall;
 import org.restlet.RestletException;
 import org.restlet.component.DefaultRestletContainer;
 import org.restlet.component.RestletContainer;
 import org.restlet.data.MediaTypes;
 
 import com.noelios.restlet.DirectoryRestlet;
 import com.noelios.restlet.GuardChainlet;
 import com.noelios.restlet.LogChainlet;
 import com.noelios.restlet.StatusChainlet;
 import com.noelios.restlet.data.StringRepresentation;
 import com.noelios.restlet.ext.jetty.JettyServer;
 
 /**
  * Maplets and hierarchical URIs
  */
 public class Tutorial11
 {
    public static void main(String[] args)
    {
       try
       {
          // Registering the Restlet API implementation
          com.noelios.restlet.Engine.register();
       
          // Create a new Restlet container
          RestletContainer myContainer = new DefaultRestletContainer("My container");
 
          // Create the HTTP server connector, then add it as a server connector 
          // to the Restlet container. Note that the container is the call handler.
          JettyServer httpServer = new JettyServer("My connector", 8182, myContainer);
          myContainer.addServer(httpServer);
 
          // Attach a log Chainlet to the container
          LogChainlet log = new LogChainlet(myContainer, "com.noelios.restlet.tutorial");
          myContainer.attach("http://localhost:8182", log);
 
          // Attach a status Chainlet to the log Chainlet
          StatusChainlet status = new StatusChainlet(myContainer, true, "webmaster@mysite.org", "http://www.mysite.org");
          log.attach(status);
 
          // Attach a root Maplet to the status Chainlet.
          Maplet rootMaplet = new DefaultMaplet(myContainer);
          status.attach(rootMaplet);
 
          // Attach a guard Chainlet to secure access the the chained directory Restlet 
          GuardChainlet guard = new GuardChainlet(myContainer, "com.noelios.restlet.tutorial", "Restlet tutorial")
             {
               protected boolean authorize(String userId, String password)
                {
                   return userId.equals("scott") && password.equals("tiger");
                }
             };
          
          rootMaplet.attach("/docs/", guard);
          
          // Create a directory Restlet able to return a deep hierarchy of Web files 
          DirectoryRestlet dirRestlet = new DirectoryRestlet(myContainer, "D:/Restlet/www/docs/api/", true, "index");
          dirRestlet.addExtension("html", MediaTypes.TEXT_HTML);
          dirRestlet.addExtension("css", MediaTypes.TEXT_CSS);
          dirRestlet.addExtension("gif", MediaTypes.IMAGE_GIF);
          guard.attach(dirRestlet);
 
          // Create the users Maplet
          Maplet usersMaplet = new DefaultMaplet(myContainer);
          rootMaplet.attach("/users", usersMaplet);
 
          // Create the user Maplet
          Maplet userMaplet = new DefaultMaplet(myContainer)
             {
                public void handle(RestletCall call) throws RestletException
                {
                  // Print the requested URI path
                  String output = "Account of user named: " + call.getPath(1, true);
                  call.setOutput(new StringRepresentation(output, MediaTypes.TEXT_PLAIN));
                  
                  // Continue processing
                  delegate(call);
                }
             };
          usersMaplet.attach("/[a-z]+", userMaplet);
 
          // Create the orders Restlet
          Restlet ordersRestlet = new AbstractRestlet(myContainer)
             {
                public void handle(RestletCall call) throws RestletException
                {
                   // Print the requested URI path
                   String output = "Orders of user named: " + call.getPath(2, true);
                   call.setOutput(new StringRepresentation(output, MediaTypes.TEXT_PLAIN));
                }
             };
          userMaplet.attach("/orders$", ordersRestlet);
             
          // Now, let's start the container! 
          myContainer.start();
       }
       catch(Exception e)
       {
          e.printStackTrace();
       }
    }
 
 }
