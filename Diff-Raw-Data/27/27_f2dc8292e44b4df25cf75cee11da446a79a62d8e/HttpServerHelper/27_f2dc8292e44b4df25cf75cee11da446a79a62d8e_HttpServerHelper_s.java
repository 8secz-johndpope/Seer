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
 
 import java.net.InetAddress;
 
 import org.restlet.Server;
 import org.restlet.data.Protocol;
 
 import com.sun.grizzly.Controller;
import com.sun.grizzly.DefaultInstanceHandler;
 import com.sun.grizzly.DefaultProtocolChain;
 import com.sun.grizzly.ProtocolChain;
 import com.sun.grizzly.TCPSelectorHandler;
 import com.sun.grizzly.filter.ReadFilter;
 
 /**
  * HTTP connector based on Grizzly.
  * 
  * @author Jerome Louvel (contact@noelios.com)
  */
 public class HttpServerHelper extends GrizzlyServerHelper {
 
     /**
      * Constructor.
      * 
      * @param server
      *            The helped server.
      */
     public HttpServerHelper(Server server) {
         super(server);
         getProtocols().add(Protocol.HTTP);
     }
 
     @Override
     protected void configure(Controller controller) throws Exception {
         // Create and configure a select handler
         TCPSelectorHandler selectHandler = new TCPSelectorHandler();
         selectHandler.setPort(getServer().getPort());
         if (getServer().getAddress() != null) {
             selectHandler.setInet(InetAddress.getByName(getServer()
                     .getAddress()));
         }
 
         // Create the Grizzly filters
         final ReadFilter readFilter = new ReadFilter();
         final HttpParserFilter httpParserFilter = new HttpParserFilter(this);
 
         // Create the Grizzly controller
         controller.setSelectHandler(selectHandler);
        controller.setInstanceHandler(new DefaultInstanceHandler() {
            public ProtocolChain poll() {
                ProtocolChain protocolChain = protocolChains.poll();
                if (protocolChain == null) {
                    protocolChain = new DefaultProtocolChain();
                    protocolChain.addFilter(readFilter);
                    protocolChain.addFilter(httpParserFilter);
                }
                return protocolChain;
            }
        });
     }
 
 }
