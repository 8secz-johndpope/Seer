 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.apache.sling.core.impl.filter;
 
 import java.io.IOException;
 import java.util.Iterator;
 
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.sling.component.Component;
 import org.apache.sling.component.ComponentException;
 import org.apache.sling.component.ComponentFilterChain;
 import org.apache.sling.component.ComponentRequest;
 import org.apache.sling.component.ComponentResponse;
 import org.apache.sling.component.ComponentResponseWrapper;
 import org.apache.sling.core.Constants;
 import org.apache.sling.core.components.ErrorHandlerComponent;
 import org.apache.sling.core.impl.ContentData;
 import org.apache.sling.core.impl.RequestData;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 
 /**
  * The <code>ErrorHandlerFilter</code> TODO
  *
  * @scr.component immediate="true" label="%errhandler.name"
  *      description="%errhandler.description"
  * @scr.property name="service.description" value="Error Handler Filter"
  * @scr.property name="service.vendor" value="The Apache Software Foundation"
  * @scr.property name="filter.scope" value="request" private="true"
  * @scr.property name="filter.order" value="-1000" type="Integer" private="true"
  * @scr.service
  * @scr.reference name="Components" interface="org.apache.sling.component.Component"
  *                cardinality="0..n" policy="dynamic"
  */
 public class ErrorHandlerFilter extends ComponentBindingFilter {
 
     /** default log */
     private static final Logger log = LoggerFactory.getLogger(ErrorHandlerFilter.class);
 
     public void doFilter(ComponentRequest request, ComponentResponse response,
             ComponentFilterChain filterChain) throws IOException,
             ComponentException {
 
         try {
             filterChain.doFilter(request, new ErrorHandlerResponse(request,
                 response));
         } catch (Throwable throwable) {
            log.error("Error during request processing.", throwable);
             this.handleError(throwable, request, response);
         }
 
     }
 
     protected boolean accept(Component component) {
         return component instanceof ErrorHandlerComponent;
     }
 
     // ---------- internal -----------------------------------------------------
 
     // TODO: this must be called from ComponentResponseImpl...
     private void handleError(int status, String message, ComponentRequest request,
             ComponentResponse response) throws IOException {
 
         // do not handle, if already handling ....
         if (request.getAttribute(Constants.ERROR_REQUEST_URI) == null) {
 
             // find the error handler component
             int checkStatus = status;
             int filter = 10;
             for (;;) {
                 for (Iterator hi = this.getComponents(); hi.hasNext();) {
                     ErrorHandlerComponent handler = (ErrorHandlerComponent) hi.next();
                     if (handler.canHandle(checkStatus)) {
 
                         // set the message properties
                         request.setAttribute(Constants.ERROR_STATUS,
                             new Integer(status));
                         request.setAttribute(Constants.ERROR_MESSAGE, message);
 
                         if (this.handleError(handler, request, response)) {
                             return;
                         }
                     }
                 }
 
                 // nothing more to be done
                 if (checkStatus == 0) {
                     break;
                 }
 
                 // cut off last position
                 checkStatus = (checkStatus / filter) * filter;
                 filter *= 10;
             }
         }
 
         // get here, if we have no handler, let the status go up the chain
         // and if this causes and exception, so what ...
         response.sendError(status, message);
     }
 
     private void handleError(Throwable throwable, ComponentRequest request,
             ComponentResponse response) throws ComponentException, IOException {
 
         // do not handle, if already handling ....
         if (request.getAttribute(Constants.ERROR_REQUEST_URI) == null) {
 
             // find the error handler component
             Class tClass = throwable.getClass();
             while (tClass != null) {
                 String tClassName = tClass.getName();
                 for (Iterator hi = this.getComponents(); hi.hasNext();) {
                     ErrorHandlerComponent handler = (ErrorHandlerComponent) hi.next();
                     if (handler.canHandle(tClassName)) {
 
                         // set the message properties
                         request.setAttribute(Constants.ERROR_EXCEPTION,
                             throwable);
                         request.setAttribute(Constants.ERROR_EXCEPTION_TYPE,
                             throwable.getClass());
                         request.setAttribute(Constants.ERROR_MESSAGE,
                             throwable.getMessage());
 
                         if (this.handleError(handler, request, response)) {
                             return;
                         }
                     }
                 }
 
                 // go to the base class
                 tClass = tClass.getSuperclass();
             }
         }
 
         // get here, if we have no handler, let the throwable go up the chain
         if (throwable instanceof IOException) {
             throw (IOException) throwable;
         } else if (throwable instanceof ComponentException) {
             throw (ComponentException) throwable;
         } else if (throwable instanceof RuntimeException) {
             throw (RuntimeException) throwable;
         } else {
             throw new ComponentException(throwable);
         }
     }
 
     private boolean handleError(ErrorHandlerComponent errorHandler,
             ComponentRequest request, ComponentResponse response) {
 
         // set the message properties
         try {
             RequestData requestData = RequestData.getRequestData(request);
             ContentData contentData = requestData.getContentData();
             if (contentData != null && contentData.getContent() != null) {
                 request.setAttribute(Constants.ERROR_COMPONENT_ID,
                     contentData.getContent().getComponentId());
             }
         } catch (ComponentException ce) {
             log.warn("handleError: Called with wrong request type, ignore for now");
         }
 
         request.setAttribute(Constants.ERROR_REQUEST_URI,
             request.getRequestURI());
         request.setAttribute(Constants.ERROR_SERVLET_NAME,
             errorHandler.getComponentContext().getServerInfo()); // not
         // absolutely
         // correct
 
         // return but keep the attribute to prevent repeated error handling !
         // TODO: Should actually do better for finding about whether we already
         // handle errors -> maybe a RequestData flag ??
         if (errorHandler == null) {
             return false;
         }
 
         // find a component by
         try {
             errorHandler.service(request, response);
             return true;
         } catch (Throwable t) {
             log.error("Cannot handle error", t);
         }
 
         return false;
     }
 
     // ---------- internal class -----------------------------------------------
 
     private class ErrorHandlerResponse extends ComponentResponseWrapper {
 
         private ComponentRequest handlerRequest;
 
         public ErrorHandlerResponse(ComponentRequest handlerRequest,
                 ComponentResponse delegatee) {
             super(delegatee);
             this.handlerRequest = handlerRequest;
         }
 
         public void sendError(int status) throws IOException {
             this.checkCommitted();
 
             ErrorHandlerFilter.this.handleError(status, null, this.handlerRequest, this.getComponentResponse());
         }
 
         public void sendError(int status, String message) throws IOException {
             this.checkCommitted();
 
             ErrorHandlerFilter.this.handleError(status, message, this.handlerRequest, this.getComponentResponse());
         }
 
         public void sendRedirect(String location) {
             this.checkCommitted();
 
             // TODO: location must be converted into an absolute URL and
             // link-checked
 
             this.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
             this.setHeader("Location", location);
         }
 
         private void checkCommitted() {
             if (this.isCommitted()) {
                 throw new IllegalStateException(
                     "Response has already been committed");
             }
         }
     }
 
 }
