 /*
  * Copyright 2010 FatWire Corporation. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.fatwire.gst.foundation.controller;
 
 import javax.servlet.http.HttpServletResponse;
 
 import COM.FutureTense.Interfaces.FTVAL;
 import COM.FutureTense.Interfaces.FTValList;
 import COM.FutureTense.Interfaces.ICS;
 import COM.FutureTense.Interfaces.IPS;
 import COM.FutureTense.Util.ftErrors;
 import COM.FutureTense.XML.Template.Seed2;
 
 import com.fatwire.gst.foundation.CSRuntimeException;
 import com.fatwire.gst.foundation.DebugHelper;
 import com.fatwire.gst.foundation.facade.runtag.render.Unknowndeps;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import static COM.FutureTense.Interfaces.Utilities.goodString;
 
 /**
  * @author Tony Field
  * @author Dolf Dijkstra
  * @since Jun 16, 2010
  */
 
 public abstract class AbstractController implements Seed2 {
     protected static final Log LOG = LogFactory.getLog("com.fatwire.gst.foundation.controller");
 
     public static final String STATUS_HEADER = "X-Fatwire-Status";
 
     protected ICS ics;
     private FTValList vIn;
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * COM.FutureTense.XML.Template.Seed2#SetAppLogic(COM.FutureTense.Interfaces
      * .IPS)
      */
 
     public void SetAppLogic(final IPS ips) {
         ics = ips.GetICSObject();
 
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * COM.FutureTense.XML.Template.Seed#Execute(COM.FutureTense.Interfaces.
      * FTValList, COM.FutureTense.Interfaces.FTValList)
      */
 
     public final String Execute(final FTValList vIn, final FTValList vOut) {
         this.vIn = vIn;
         try {
             doExecute();
         } catch (final Exception e) {
             handleException(e);
         }
 
         return "";
     }
 
     protected final FTVAL getInputArgument(String name) {
         return vIn == null ? null : vIn.getVal(name);
     }
 
     protected final String getInputArgumentAsString(String name) {
         return vIn == null ? null : vIn.getValString(name);
     }
 
     /**
      * Sends the http status code to the user-agent.
      * 
      * 
      * @param code the http response code
      * @return String to stream
      */
 
     protected final String sendError(final int code, final Exception e) {
         LOG.debug(code + " status code sent due to exception " + e.toString(), e);
         if (LOG.isTraceEnabled()) {
             DebugHelper.dumpVars(ics, LOG);
         }
         switch (code) { // all the http status codes, we may restrict the list
             // to error and redirect
             case HttpServletResponse.SC_ACCEPTED:
             case HttpServletResponse.SC_BAD_GATEWAY:
             case HttpServletResponse.SC_BAD_REQUEST:
             case HttpServletResponse.SC_CONFLICT:
             case HttpServletResponse.SC_CONTINUE:
             case HttpServletResponse.SC_CREATED:
             case HttpServletResponse.SC_EXPECTATION_FAILED:
             case HttpServletResponse.SC_FORBIDDEN:
             case HttpServletResponse.SC_FOUND:
             case HttpServletResponse.SC_GATEWAY_TIMEOUT:
             case HttpServletResponse.SC_GONE:
             case HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED:
             case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
             case HttpServletResponse.SC_LENGTH_REQUIRED:
             case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
             case HttpServletResponse.SC_MOVED_PERMANENTLY:
                 // case HttpServletResponse.SC_MOVED_TEMPORARILY : //SC_FOUND is
                 // preferred
             case HttpServletResponse.SC_MULTIPLE_CHOICES:
             case HttpServletResponse.SC_NO_CONTENT:
             case HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION:
             case HttpServletResponse.SC_NOT_ACCEPTABLE:
             case HttpServletResponse.SC_NOT_FOUND:
             case HttpServletResponse.SC_NOT_IMPLEMENTED:
             case HttpServletResponse.SC_NOT_MODIFIED:
             case HttpServletResponse.SC_OK:
             case HttpServletResponse.SC_PARTIAL_CONTENT:
             case HttpServletResponse.SC_PAYMENT_REQUIRED:
             case HttpServletResponse.SC_PRECONDITION_FAILED:
             case HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED:
             case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE:
             case HttpServletResponse.SC_REQUEST_TIMEOUT:
             case HttpServletResponse.SC_REQUEST_URI_TOO_LONG:
             case HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
             case HttpServletResponse.SC_RESET_CONTENT:
             case HttpServletResponse.SC_SEE_OTHER:
             case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
             case HttpServletResponse.SC_SWITCHING_PROTOCOLS:
             case HttpServletResponse.SC_TEMPORARY_REDIRECT:
             case HttpServletResponse.SC_UNAUTHORIZED:
             case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE:
             case HttpServletResponse.SC_USE_PROXY:
                 ics.StreamHeader(STATUS_HEADER, Integer.toString(code));
                 break;
             default:
                 ics.StreamHeader(STATUS_HEADER, "500");
                 break;
         }
         Unknowndeps.unknonwDeps(ics);// failure case might be corrected on next
         // publish or save
         String element = null;
 
         if (goodString(ics.GetVar("site")) && ics.IsElement(ics.GetVar("site") + "/ErrorHandler/" + code)) {
             element = ics.GetVar("site") + "/ErrorHandler/" + code;
         } else if (ics.IsElement("GST/ErrorHandler/" + code)) {
             element = "GST/ErrorHandler/" + code;
         } else if (ics.IsElement("GST/ErrorHandler")) {
             element = "GST/ErrorHandler";
         }
         if (element != null) {
             ics.SetObj("com.fatwire.gst.foundation.exception", e);
             ics.CallElement(element, null);
            ics.SetObj("com.fatwire.gst.foundation.exception", null);
         }
         ics.SetErrno(ftErrors.exceptionerr);
 
         return null;
 
     }
 
     /**
      * Executes the core business logic of the controller.
      * 
      * @throws CSRuntimeException may throw a CSRuntimeException which is
      *             handled by handleCSRuntimeException
      */
     abstract protected void doExecute();
 
     /**
      * Handles the exception, doing what is required
      * 
      * @param e exception
      */
     abstract protected void handleException(Exception e);
 }
