 /*
 Copyright 2009-2010 Igor Polevoy 
 
 Licensed under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License. 
 You may obtain a copy of the License at 
 
 http://www.apache.org/licenses/LICENSE-2.0 
 
 Unless required by applicable law or agreed to in writing, software 
 distributed under the License is distributed on an "AS IS" BASIS, 
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 See the License for the specific language governing permissions and 
 limitations under the License. 
 */
 package activeweb;
 
 import javalite.common.Util;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.servlet.*;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.*;
 
 import static activeweb.ControllerFactory.getControllerClassName;
 
 /**
  * @author Igor Polevoy
  */
 public class RequestDispatcher implements Filter {
     private Logger logger = LoggerFactory.getLogger(getClass().getName());
     private FilterConfig filterConfig;
     private List<String> exclusions = new ArrayList<String>();
     private ControllerRunner runner = new ControllerRunner();
     private AppContext appContext;
     private Bootstrap appBootstrap;
     private Router router;
 
     public void init(FilterConfig filterConfig) throws ServletException {
         this.filterConfig = filterConfig;        
         ControllerRegistry registry = new ControllerRegistry(filterConfig);
         filterConfig.getServletContext().setAttribute("controllerRegistry", registry);
         
         activeweb.Configuration.getTemplateManager().setServletContext(filterConfig.getServletContext());
         ContextAccess.setControllerRegistry(registry);//bootstrap below requires it
         appContext = new AppContext();
         filterConfig.getServletContext().setAttribute("appContext", appContext);
         initApp(appContext);
 
         String exclusionsParam = filterConfig.getInitParameter("exclusions");
         if (exclusionsParam != null) {
             exclusions.addAll(Arrays.asList(exclusionsParam.split(",")));
             for (int i = 0; i < exclusions.size(); i++) {
                 exclusions.set(i, exclusions.get(i).trim());
             }
         }
 
         router = new Router(filterConfig.getInitParameter("root_controller"));
 
         logger.info("ActiveWeb: starting the app in environment: " + Configuration.getEnv());
     }
 
     protected void initApp(AppContext context){
         initAppConfig(Configuration.getBootstrapClassName(), context, true);
         //these are optional config classes:
         initAppConfig(Configuration.getControllerConfigClassName(), context, false);
         initAppConfig(Configuration.getDbConfigClassName(), context, false);
 
     }
 
     private void initAppConfig(String configClassName, AppContext context, boolean fail){
 
         AppConfig appConfig = null;
         try {
             Class c = Class.forName(configClassName);
             appConfig = (AppConfig) c.newInstance();
             if(appConfig instanceof  Bootstrap){
                 appBootstrap = (Bootstrap) appConfig;
             }
         }
         catch (Throwable e) {
             if(fail){
                 throw new InitException("failed to create a new instance of class: " + configClassName
                     + ", are you sure class exists and it has a default constructor?", e);
             }else{
                 logger.debug("failed to create a new instance of class: " + configClassName
                         + ", proceeding without it. " + e.getMessage());
                 return;
             }
         }
         appConfig.init(context);
     }
 
 
     protected ControllerRegistry getControllerRegistry() {
         return (ControllerRegistry) filterConfig.getServletContext().getAttribute("controllerRegistry");
     }
 
     public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
         try {
 
             HttpServletRequest request = (HttpServletRequest) req;
             HttpServletResponse response = (HttpServletResponse) resp;
             ContextAccess.setTLs(request, response, filterConfig, getControllerRegistry(), appContext);
 
             String uri = request.getServletPath();
             if (Util.blank(uri)) {
                 uri = "/";//different servlet implementations, damn.
             }
 
             boolean excluded = excluded(uri);
             if(excluded){
                 chain.doFilter(req, resp);
                 logger.debug("URI excluded: " + uri);
                 return;
             }
 
             MatchedRoute route = router.recognize(uri, HttpMethod.getMethod(request));
 
             if (route != null) {
                 ContextAccess.setRoute(route);
 
                 if (Configuration.logRequestParams()) {
                     logger.info("================ New request: " + new Date() + " ================");
                 }
 
                 if (route.getId() != null) {
                     request.setAttribute("id", route.getId());
                 }
 
                 runner.run(route, false, true);
             } else {
                 //TODO: theoretically this will never happen, because if the route was not excluded, the router.recognize() would throw some kind
                 // of exception, leading to the a system error page.
                 logger.warn("No matching route for servlet path: " + request.getServletPath() + ", passing down to container.");
                 chain.doFilter(req, resp);//let it fall through
             }
         } catch (CompilationException e) {
             renderSystemError(e);
         } catch (ControllerLoadException e) {
             renderSystemError("/system/404", Configuration.getDefaultLayout(), 404, e);
         }catch (ActionNotFoundException e) {
             renderSystemError("/system/404", Configuration.getDefaultLayout(), 404, e);
         }catch(ViewMissingException e){
             renderSystemError("/system/404", Configuration.getDefaultLayout(), 404, e);
         }catch(ViewException e){
             renderSystemError("/system/error", Configuration.getDefaultLayout(), 500, e);
         }catch (Throwable e) {
             renderSystemError(e);
         }finally {
            ContextAccess.clear();
         }
     }
 
     private Map getMapWithExceptionDataAndSession(Throwable e) {
         Map values = new HashMap();
         values.put("message", e.getMessage() == null ? e.toString() : e.getMessage());
         values.put("stack_trace", getStackTraceString(e));
 
         //Even an error view might rely on a session attribute
         values.put("session", SessionHelper.getSessionAttributes());
 
         return values;
     }
 
     private boolean excluded(String servletPath) {
         for (String exclusion : exclusions) {
             if (servletPath.contains(exclusion))
                 return true;
         }
         return false;
     }
 
 
     private void renderSystemError(Throwable e) {
         renderSystemError("/system/error", null, 500, e);
     }
 
 
     private String getStackTraceString(Throwable e) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         pw.flush();
         return sw.toString();
     }
 
     private void renderSystemError(String template, String layout, int status, Throwable e) {
         logger.error("ActiveWeb ERROR: \n" + getRequestProperties(), e);
         if(ContextAccess.getHttpRequest().getHeader("x-requested-with")!= null
                 || ContextAccess.getHttpRequest().getHeader("X-Requested-With") != null){
 
             try{
                 ContextAccess.getHttpResponse().setStatus(status);
                 ContextAccess.getHttpResponse().getWriter().write(getStackTraceString(e));                
             }catch(Exception ex){
                 logger.error("Failed to send error response to client", ex);
             }                        
         }else{
             RenderTemplateResponse resp = new RenderTemplateResponse(getMapWithExceptionDataAndSession(e), template);
             resp.setLayout(layout);
             resp.setStatus(status);
             resp.setTemplateManager(Configuration.getTemplateManager());
             ParamCopy.copyInto(resp.values());
             resp.process();            
         }
     }
 
 
     private String getRequestProperties(){
         StringBuffer sb = new StringBuffer();
         HttpServletRequest request = ContextAccess.getHttpRequest();
         sb.append("Request URL: ").append(request.getRequestURL()).append("\n");
         sb.append("ContextPath: ").append(request.getContextPath()).append("\n");
         sb.append("Query String: ").append(request.getQueryString()).append("\n");
         sb.append("URI Full Path: ").append(request.getRequestURI()).append("\n");
         sb.append("URI Path: ").append(request.getServletPath()).append("\n");
         sb.append("Method: ").append(request.getMethod()).append("\n");
         return sb.toString();
     }
     
     public void destroy() {
         appBootstrap.destroy(appContext);
     }
 }
