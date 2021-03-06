 /*
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  *
  * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
  *
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
  * or jersey/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  *
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at jersey/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  *
  * Contributor(s):
  *
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 
 package com.sun.jersey.oauth.client;
 
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
 import javax.ws.rs.core.MultivaluedMap;
 import javax.ws.rs.ext.Providers;
 import com.sun.jersey.api.client.ClientRequest;
 import com.sun.jersey.core.util.MultivaluedMapImpl;
 import com.sun.jersey.oauth.signature.OAuthRequest;
 
 /**
  * Implements the OAuth signature library Request interface, wrapping a Jersey
  * client request object.
  *
  * @author Paul C. Bryan <pbryan@sun.com>
  */
 class RequestWrapper implements OAuthRequest {
 
     /** The wrapped Jersey client request. */
     private final ClientRequest clientRequest;
 
     /** The registered providers. */
     private final Providers providers;
 
     /** Form and query parameters from the request (lazily initialized). */
     private MultivaluedMap<String, String> parameters = null;
 
     private void setParameters() {
         parameters = new MultivaluedMapImpl();
         parameters.putAll(RequestUtil.getQueryParameters(clientRequest));
         parameters.putAll(RequestUtil.getEntityParameters(clientRequest, providers));
     }
 
     /**
      * Constructs a new OAuth client request wrapper around the specified
      * Jersey client request object.
      *
      * @param request the Jersey client request object to be wrapped.
      */
     public RequestWrapper(final ClientRequest clientRequest, final Providers providers) {
         this.clientRequest = clientRequest;
         this.providers = providers;
         setParameters(); // stored because parsing query/entity parameters too much work for each value-get
     }
 
     public String getRequestMethod() {
         return clientRequest.getMethod();
     }
 
    public String getRequestURL() {
        final URI uri = clientRequest.getURI();
        return uri.getScheme() + "://" + uri.getAuthority() + uri.getPath();
     }
 
     public Set<String> getParameterNames() {
         return parameters.keySet();
     }
 
     public List<String> getParameterValues(final String name) {
         return parameters.get(name);
     }
 
     public List<String> getHeaderValues(final String name) {
 
         ArrayList<String> list = new ArrayList();
 
         for (Object header : clientRequest.getMetadata().get(name)) {
             list.add(ClientRequest.getHeaderValue(header));
         }
 
         return list;
     }
 
     public void addHeaderValue(final String name, final String value) {
         clientRequest.getMetadata().add(name, value);
     }
 }
 
