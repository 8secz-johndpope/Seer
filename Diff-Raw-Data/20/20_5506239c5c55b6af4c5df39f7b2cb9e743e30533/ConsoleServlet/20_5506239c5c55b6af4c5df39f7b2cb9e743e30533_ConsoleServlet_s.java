 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.tomee.webapp.servlet;
 
 import org.apache.openejb.util.OpenEJBScripter;
 import org.apache.tomee.webapp.JsonExecutor;
 
 import javax.script.Bindings;
 import javax.script.SimpleBindings;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 import java.io.IOException;
 import java.util.Map;
 
 public class ConsoleServlet extends HttpServlet {
    private static final OpenEJBScripter SCRIPTER = new OpenEJBScripter();
 
     @Override
     protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
         JsonExecutor.execute(resp, new JsonExecutor.Executor() {
             @Override
             public void call(Map<String, Object> json) throws Exception {
                 final HttpSession session = req.getSession();
 
                 String engineName = req.getParameter("engineName");
                 if (engineName == null || "".equals(engineName.trim())) {
                     engineName = "js";
                 }
 
                 final Bindings bindings = new SimpleBindings();
                 bindings.put("req", req);
                 bindings.put("resp", resp);
 
                 bindings.put("util", new Utility() {
                     @Override
                     public void write(Object obj) throws Exception {
                         resp.getWriter().write(String.valueOf(obj));
                     }
 
                     @Override
                     public void save(String key, Object obj) {
                         Map<String, Object> objects = (Map<String, Object>) session.getAttribute("objects");
                         objects.put(key, obj);
                     }
                 });
 
                String scriptCode = req.getParameter("scriptCode");
                if (scriptCode == null || "".equals(scriptCode.trim())) {
                    scriptCode = "var a = 0;";
                }
                 SCRIPTER.evaluate(engineName, scriptCode, bindings);
             }
         });
     }
 
     private interface Utility {
         void write(Object obj) throws Exception;
 
         void save(String key, Object obj);
     }
 }
