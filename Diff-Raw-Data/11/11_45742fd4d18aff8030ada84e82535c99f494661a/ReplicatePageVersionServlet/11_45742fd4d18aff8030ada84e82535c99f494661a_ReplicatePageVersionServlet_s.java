 /*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2013 - 2014 Adobe
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 package com.adobe.acs.commons.replicatepageversion.impl;
 
 import java.io.IOException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.Map;
 
 import javax.servlet.ServletException;
 
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.sling.SlingServlet;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.servlets.SlingAllMethodsServlet;
 import org.apache.sling.commons.json.JSONException;
 import org.apache.sling.commons.json.JSONObject;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;
 
 @SuppressWarnings("serial")
 @SlingServlet(paths = "/bin/replicatepageversion", methods = "POST",
         generateComponent = true)
 public class ReplicatePageVersionServlet extends SlingAllMethodsServlet {
 
     private static final Logger log = LoggerFactory
             .getLogger(ReplicatePageVersionServlet.class);
 
     @Reference
     private ReplicatePageVersionService rps;
 
     @Override
     public final void doPost(SlingHttpServletRequest req,
             SlingHttpServletResponse res) throws ServletException, IOException {
         log.info("in do post - ReplicatePageVersionServlet ");
         String[] rootPaths = req.getParameterValues("rootPaths");
         Date date = getDate(req.getParameter("datetimecal"));
         String[] agents = req.getParameterValues("cmbAgent");
 
 
         JSONObject obj = null;
 
         try {
             obj = validate(rootPaths, agents, date);
             boolean error = false;
 
             if (!obj.has("error")) {
                 Map<String, String> response = rps.locateVersionAndReplicateResource(
                         req.getResourceResolver(), rootPaths, agents, date);
                 obj = convertToJson(response);
 
             } else {
                 error = true;
             }
 
             if (error) {
               
 
                 try {
                     obj.put("error", "System Error.");
                     obj.put("status", "error");
                 } catch (JSONException e) {
                     log.error("exception occured", e);
                 }
             }
         } catch (JSONException ex) {
             try {
                 if (obj == null) {
                     obj = new JSONObject();
                 }
                 obj.put("error", "System Error.");
                 obj.put("status", "error");
             } catch (JSONException e) {
                 log.error("exception occured", e);
             }
         }
 
         try {
             res.setContentType("text/json");
             obj.write(res.getWriter());
         } catch (JSONException e) {
             log.error("exception occured", e);
         }
     }
 
     private JSONObject convertToJson(Map<String, String> map) throws JSONException {
         JSONObject obj = new JSONObject();
         for (Iterator<Map.Entry<String, String>> iter=map.entrySet().iterator();iter.hasNext();) {
            obj.put(iter.next().getKey(), iter.next().getValue());
         }
         return obj;
     }
     /**
      * 
      * @param svm
      * @return
      * @throws JSONException
      */
     private JSONObject validate(String[] rootPaths, String[] agents, Date date)
             throws JSONException {
         JSONObject obj = new JSONObject();
         if (rootPaths == null || rootPaths.length == 0) {
             obj.put("error", "Select a root path");
             return obj;
         }
         for (int k = 0; k < rootPaths.length; k++) {
             if (rootPaths[k] == null || "".equals(rootPaths[k])) {
                 obj.put("error", "Root paths cannot be empty.");
                 return obj;
             }
         }
 
         if (date == null) {
             obj.put("error", "Enter the time at which you want the versions");
             return obj;
         }
         if (agents == null || agents.length == 0) {
             obj.put("error", "Select the appropriate agents");
             return obj;
         }
         return obj;
     }
 
     private Date getDate(String datetime) {
         Date date = null;
         try {
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,hh:mm:ss");
             date = sdf.parse(datetime);
         } catch (Exception e) {
             log.error("exception occured", e);
         }
         return date;
     }
 }
