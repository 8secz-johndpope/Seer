 /*
  * Copyright 2005-2007 Noelios Consulting.
  * 
  * The contents of this file are subject to the terms of the Common Development
  * and Distribution License (the "License"). You may not use this file except in
  * compliance with the License.
  * 
  * You can obtain a copy of the license at
  * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL HEADER in each file and
  * include the License file at http://www.opensource.org/licenses/cddl1.txt If
  * applicable, add the following below this CDDL HEADER, with the fields
  * enclosed by brackets "[]" replaced with your own identifying information:
  * Portions Copyright [yyyy] [name of copyright owner]
  */
 
 package com.noelios.restlet.ext.grizzly;
 
 import org.restlet.Server;
 
 import com.noelios.restlet.http.HttpServerHelper;
 import com.sun.grizzly.Controller;
 
 /**
  * Base Grizzly connector.
  * 
  * @author Jerome Louvel (contact@noelios.com)
  */
 public abstract class GrizzlyServerHelper extends HttpServerHelper {
     /** The Grizzly controller. */
     private Controller controller;
 
     /**
      * Constructor.
      * 
      * @param server
      *            The server to help.
      */
     public GrizzlyServerHelper(Server server) {
         super(server);
         this.controller = null;
     }
 
     @Override
     public void start() throws Exception {
         super.start();
 
         if (this.controller == null) {
             // Configure a new controller
             this.controller = new Controller();
             configure(this.controller);
         }
 
         getLogger().info("Starting the Grizzly HTTP server");
        this.controller.start();
     }
 
     /**
      * Configures the Grizzly controller.
      * 
      * @param controller
      *            The controller to configure.
      */
     protected abstract void configure(Controller controller) throws Exception;
 
     @Override
     public void stop() throws Exception {
         super.stop();
 
         if (this.controller != null) {
             getLogger().info("Stopping the Grizzly HTTP server");
             this.controller.stop();
         }
     }
 }
