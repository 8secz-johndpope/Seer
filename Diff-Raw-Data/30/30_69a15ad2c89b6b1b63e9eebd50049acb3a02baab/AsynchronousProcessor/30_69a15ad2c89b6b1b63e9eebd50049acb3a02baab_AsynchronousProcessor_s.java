 /*
  * Copyright 2013 Jeanfrancois Arcand
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 /*
  * 
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
  * 
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
  * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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
  *
  */
 package org.atmosphere.cpr;
 
 import org.atmosphere.config.service.AtmosphereHandlerService;
 import org.atmosphere.config.service.ManagedService;
 import org.atmosphere.config.service.MeteorService;
 import org.atmosphere.handler.AnnotatedProxy;
 import org.atmosphere.handler.ReflectorServletProcessor;
 import org.atmosphere.util.EndpointMapper;
 import org.atmosphere.util.Utils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.servlet.Servlet;
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpSession;
 import java.io.IOException;
 import java.util.List;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
 
 import static org.atmosphere.cpr.Action.TYPE.SKIP_ATMOSPHEREHANDLER;
 import static org.atmosphere.cpr.ApplicationConfig.MAX_INACTIVE;
 import static org.atmosphere.cpr.AtmosphereFramework.AtmosphereHandlerWrapper;
 import static org.atmosphere.cpr.HeaderConfig.X_ATMOSPHERE_ERROR;
 
 /**
  * Base class which implement the semantics of suspending and resuming of a
  * Comet/WebSocket Request.
  *
  * @author Jeanfrancois Arcand
  */
 public abstract class AsynchronousProcessor implements AsyncSupport<AtmosphereResourceImpl> {
 
     private static final Logger logger = LoggerFactory.getLogger(AsynchronousProcessor.class);
     protected static final Action timedoutAction = new Action(Action.TYPE.TIMEOUT);
     protected static final Action cancelledAction = new Action(Action.TYPE.CANCELLED);
     protected final AtmosphereConfig config;
     protected final ConcurrentHashMap<AtmosphereRequest, AtmosphereResource>
             aliveRequests = new ConcurrentHashMap<AtmosphereRequest, AtmosphereResource>();
     private boolean trackActiveRequest = false;
     private final ScheduledExecutorService closedDetector = Executors.newScheduledThreadPool(1);
     private final EndpointMapper<AtmosphereHandlerWrapper> mapper;
 
     public AsynchronousProcessor(AtmosphereConfig config) {
         this.config = config;
         mapper = config.framework().endPointMapper();
     }
 
     @Override
     public void init(ServletConfig sc) throws ServletException {
 
         String maxInactive = sc.getInitParameter(MAX_INACTIVE) != null ? sc.getInitParameter(MAX_INACTIVE) :
                 config.getInitParameter(MAX_INACTIVE);
 
         if (maxInactive != null) {
             trackActiveRequest = true;
             final long maxInactiveTime = Long.parseLong(maxInactive);
             if (maxInactiveTime <= 0) return;
 
             closedDetector.scheduleAtFixedRate(new Runnable() {
                 public void run() {
                     for (AtmosphereRequest req : aliveRequests.keySet()) {
                         long l = (Long) req.getAttribute(MAX_INACTIVE);
                         if (l > 0 && System.currentTimeMillis() - l > maxInactiveTime) {
                             try {
                                 logger.debug("Close detector disconnecting {}. Current size {}", req.resource(), aliveRequests.size());
                                 AtmosphereResourceImpl r = (AtmosphereResourceImpl) aliveRequests.remove(req);
                                 cancelled(req, r.getResponse(false));
                             } catch (Throwable e) {
                                 logger.warn("closedDetector", e);
                             } finally {
                                 try {
                                     req.setAttribute(MAX_INACTIVE, (long) -1);
                                 } catch (Throwable t) {
                                     logger.trace("closedDetector", t);
                                 }
                             }
                         }
                     }
                 }
             }, 0, 1, TimeUnit.SECONDS);
         }
     }
 
 
     /**
      * Is {@link HttpSession} supported
      *
      * @return true if supported
      */
     protected boolean supportSession() {
         return config.isSupportSession();
     }
 
     /**
      * Return the container's name.
      */
     public String getContainerName() {
         return config.getServletConfig().getServletContext().getServerInfo();
     }
 
     /**
      * All proprietary Comet based {@link Servlet} must invoke the suspended
      * method when the first request comes in. The returned value, of type
      * {@link Action}, tells the proprietary Comet {@link Servlet}
      * to suspended or not the current {@link AtmosphereResponse}.
      *
      * @param request  the {@link AtmosphereRequest}
      * @param response the {@link AtmosphereResponse}
      * @return action the Action operation.
      * @throws java.io.IOException
      * @throws javax.servlet.ServletException
      */
     public Action suspended(AtmosphereRequest request, AtmosphereResponse response) throws IOException, ServletException {
         return action(request, response);
     }
 
     /**
      * Invoke the {@link AtmosphereHandler#onRequest} method.
      *
      * @param req the {@link AtmosphereRequest}
      * @param res the {@link AtmosphereResponse}
      * @return action the Action operation.
      * @throws java.io.IOException
      * @throws javax.servlet.ServletException
      */
     Action action(AtmosphereRequest req, AtmosphereResponse res) throws IOException, ServletException {
 
         if (Utils.webSocketEnabled(req) && !supportWebSocket()) {
             res.setStatus(501);
             res.addHeader(X_ATMOSPHERE_ERROR, "Websocket protocol not supported");
             res.flushBuffer();
             return new Action();
         }
 
         if (config.handlers().isEmpty()) {
             logger.error("No AtmosphereHandler found. Make sure you define it inside WEB-INF/atmosphere.xml or annotate using @AtmosphereHandlerService");
             throw new AtmosphereMappingException("No AtmosphereHandler found. Make sure you define it inside WEB-INF/atmosphere.xml or annotate using @AtmosphereHandlerService");
         }
 
         if (res.request() == null) {
             res.request(req);
         }
 
         if (supportSession()) {
             // Create the session needed to support the Resume
             // operation from disparate requests.
             HttpSession session = req.getSession(true);
         }
 
         req.setAttribute(FrameworkConfig.SUPPORT_SESSION, supportSession());
 
         AtmosphereHandlerWrapper handlerWrapper = map(req);
         if (config.getBroadcasterFactory() == null) {
             logger.error("Atmosphere is misconfigured and will not work. BroadcasterFactory is null");
             return Action.CANCELLED;
         }
         config.getBroadcasterFactory().add(handlerWrapper.broadcaster, handlerWrapper.broadcaster.getID());
 
         // Check Broadcaster state. If destroyed, replace it.
         Broadcaster b = handlerWrapper.broadcaster;
         if (b.isDestroyed()) {
             BroadcasterFactory f = config.getBroadcasterFactory();
             synchronized (f) {
                 f.remove(b, b.getID());
                 try {
                     handlerWrapper.broadcaster = f.get(b.getID());
                 } catch (IllegalStateException ex) {
                     // Something wrong occurred, let's not fail and loookup the value
                     logger.trace("", ex);
                     // fallback to lookup
                     handlerWrapper.broadcaster = f.lookup(b.getID(), true);
                 }
             }
         }
 
         AtmosphereResourceImpl resource = (AtmosphereResourceImpl) req.getAttribute(FrameworkConfig.INJECTED_ATMOSPHERE_RESOURCE);
         if (resource == null) {
             // TODO: cast is dangerous
             resource = (AtmosphereResourceImpl)
                     AtmosphereResourceFactory.getDefault().create(config, handlerWrapper.broadcaster, res, this, handlerWrapper.atmosphereHandler);
         } else {
             // TODO: This piece of code can be removed, but for backward compat with existing extension we needs it for now.
             try {
                 // Make sure it wasn't set before
                 resource.getBroadcaster();
             } catch (IllegalStateException ex) {
                 resource.setBroadcaster(handlerWrapper.broadcaster);
             }
             resource.atmosphereHandler(handlerWrapper.atmosphereHandler);
         }
 
         req.setAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE, resource);
         req.setAttribute(FrameworkConfig.ATMOSPHERE_HANDLER, handlerWrapper.atmosphereHandler);
         req.setAttribute(SKIP_ATMOSPHEREHANDLER.name(), Boolean.FALSE);
 
         // Globally defined
         Action a = invokeInterceptors(config.framework().interceptors(), resource);
         if (a.type() != Action.TYPE.CONTINUE) {
             return a;
         }
 
         // Per AtmosphereHandler
         a = invokeInterceptors(handlerWrapper.interceptors, resource);
         if (a.type() != Action.TYPE.CONTINUE) {
             return a;
         }
 
         //Unit test mock the request and will throw NPE.
         boolean skipAtmosphereHandler = req.getAttribute(SKIP_ATMOSPHEREHANDLER.name()) != null
                 ?  (Boolean) req.getAttribute(SKIP_ATMOSPHEREHANDLER.name()) : Boolean.FALSE;
         if (!skipAtmosphereHandler) {
             try {
                 handlerWrapper.atmosphereHandler.onRequest(resource);
             } catch (IOException t) {
                 resource.onThrowable(t);
                 throw t;
             }
         }
 
         postInterceptors(handlerWrapper.interceptors, resource);
         postInterceptors(config.framework().interceptors(), resource);
 
         if (trackActiveRequest && resource.getAtmosphereResourceEvent().isSuspended() && req.getAttribute(FrameworkConfig.CANCEL_SUSPEND_OPERATION) == null) {
             req.setAttribute(MAX_INACTIVE, System.currentTimeMillis());
             aliveRequests.put(req, resource);
         }
 
         Action action = skipAtmosphereHandler ? Action.CANCELLED : resource.action();
         if (supportSession() && action.type().equals(Action.TYPE.SUSPEND)) {
             // Do not allow times out.
             SessionTimeoutSupport.setupTimeout(req.getSession());
         }
         logger.trace("Action for {} was {}", req.resource() != null ? req.resource().uuid() : "null", action);
         return action;
     }
 
     private Action invokeInterceptors(List<AtmosphereInterceptor> c, AtmosphereResource r) {
         Action a = Action.CONTINUE;
         for (AtmosphereInterceptor arc : c) {
             a = arc.inspect(r);
             if (a == null) {
                 a = Action.CANCELLED;
             }
 
             boolean skip = a.type() == SKIP_ATMOSPHEREHANDLER;
             if (skip) {
                 logger.debug("AtmosphereInterceptor {} asked to skip the AtmosphereHandler", arc);
                 r.getRequest().setAttribute(SKIP_ATMOSPHEREHANDLER.name(), Boolean.TRUE);
             }
 
             if (a.type() != Action.TYPE.CONTINUE) {
                 logger.trace("Interceptor {} interrupted the dispatch with {}", arc, a);
                 return a;
             }
         }
         return a;
     }
 
     private void postInterceptors(List<AtmosphereInterceptor> c, AtmosphereResource r) {
         for (int i = c.size() - 1; i > -1; i--) {
             c.get(i).postInspect(r);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public void action(AtmosphereResourceImpl r) {
         if (trackActiveRequest) {
             aliveRequests.remove(r.getRequest(false));
         }
     }
 
     /**
      * Return the {@link AtmosphereHandler} mapped to the passed servlet-path.
      *
      * @param req the {@link AtmosphereResponse}
      * @return the {@link AtmosphereHandler} mapped to the passed servlet-path.
      * @throws javax.servlet.ServletException
      */
     protected AtmosphereHandlerWrapper map(AtmosphereRequest req) throws ServletException {
         AtmosphereHandlerWrapper atmosphereHandlerWrapper = mapper.map(req, config.handlers());
         if (atmosphereHandlerWrapper == null) {
             logger.debug("No AtmosphereHandler maps request for {} with mapping {}", req.getRequestURI(), config.handlers());
             throw new AtmosphereMappingException("No AtmosphereHandler maps request for " + req.getRequestURI());
         }
         config.getBroadcasterFactory().add(atmosphereHandlerWrapper.broadcaster,
                 atmosphereHandlerWrapper.broadcaster.getID());
         return postProcessMapping(req, atmosphereHandlerWrapper);
     }
 
     protected AtmosphereHandlerWrapper postProcessMapping(AtmosphereRequest request, AtmosphereHandlerWrapper w) {
 
         String path;
         String pathInfo = null;
         try {
             pathInfo = request.getPathInfo();
         } catch (IllegalStateException ex) {
             // http://java.net/jira/browse/GRIZZLY-1301
         }
 
         if (pathInfo != null) {
             path = request.getServletPath() + pathInfo;
         } else {
             path = request.getServletPath();
         }
 
         if (path == null || path.isEmpty()) {
             path = "/";
         }
 
         if (config.handlers().get(path) == null) {
             // ManagedService
             if (AnnotatedProxy.class.isAssignableFrom(w.atmosphereHandler.getClass())) {
                 AnnotatedProxy ap = AnnotatedProxy.class.cast(w.atmosphereHandler);
                 if (ap.target().getClass().getAnnotation(ManagedService.class) != null) {
                     String targetPath = ap.target().getClass().getAnnotation(ManagedService.class).path();
                     if (targetPath.indexOf("{") != -1 && targetPath.indexOf("}") != -1) {
                         try {
                             config.framework().addAtmosphereHandler(path, w.atmosphereHandler.getClass().getConstructor(Object.class)
                                     .newInstance(ap.target().getClass().newInstance()), w.interceptors);
                             return config.handlers().get(path);
                         } catch (Throwable e) {
                             logger.warn("Unable to create AtmosphereHandler", e);
                         }
                     }
                 }
             }
 
             // AtmosphereHandlerService
             if (w.atmosphereHandler.getClass().getAnnotation(AtmosphereHandlerService.class) != null) {
                 String targetPath = w.atmosphereHandler.getClass().getAnnotation(AtmosphereHandlerService.class).path();
                 if (targetPath.indexOf("{") != -1 && targetPath.indexOf("}") != -1) {
                     try {
                         config.framework().addAtmosphereHandler(path, w.atmosphereHandler.getClass().newInstance());
                         return config.handlers().get(path);
                     } catch (Throwable e) {
                         logger.warn("Unable to create AtmosphereHandler", e);
                     }
                 }
             }
        }

        // MeteorService
        if (ReflectorServletProcessor.class.isAssignableFrom(w.atmosphereHandler.getClass())) {
            Servlet s = ReflectorServletProcessor.class.cast(w.atmosphereHandler).getServlet();
            if (s.getClass().getAnnotation(MeteorService.class) != null) {
                String targetPath = s.getClass().getAnnotation(MeteorService.class).path();
                if (targetPath.indexOf("{") != -1 && targetPath.indexOf("}") != -1) {
                    try {
                        ReflectorServletProcessor r = new ReflectorServletProcessor();
 
                        r.setServlet(s.getClass().newInstance());
                        config.framework().addAtmosphereHandler(path, r);
                        return config.handlers().get(path);
                    } catch (Throwable e) {
                        logger.warn("Unable to create AtmosphereHandler", e);
                     }
                 }
             }
         }
         return w;
     }
 
     /**
      * All proprietary Comet based {@link Servlet} must invoke the resume
      * method when the Atmosphere's application decide to resume the {@link AtmosphereResponse}.
      * The returned value, of type
      * {@link Action}, tells the proprietary Comet {@link Servlet}
      * to resume (again), suspended or do nothing with the current {@link AtmosphereResponse}.
      *
      * @param request  the {@link AtmosphereRequest}
      * @param response the {@link AtmosphereResponse}
      * @return action the Action operation.
      * @throws java.io.IOException
      * @throws javax.servlet.ServletException
      */
     public Action resumed(AtmosphereRequest request, AtmosphereResponse response)
             throws IOException, ServletException {
 
         AtmosphereResourceImpl r =
                 (AtmosphereResourceImpl) request.getAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE);
 
         if (r == null) return Action.CANCELLED; // We are cancelled already
 
         AtmosphereHandler atmosphereHandler =
                 (AtmosphereHandler)
                         request.getAttribute(FrameworkConfig.ATMOSPHERE_HANDLER);
 
         AtmosphereResourceEvent event = r.getAtmosphereResourceEvent();
         if (event != null && event.isResuming() && !event.isCancelled()) {
             synchronized (r) {
                 atmosphereHandler.onStateChange(event);
             }
         }
         return Action.RESUME;
     }
 
     /**
      * All proprietary Comet based {@link Servlet} must invoke the timedout
      * method when the underlying WebServer time out the {@link AtmosphereResponse}.
      * The returned value, of type
      * {@link Action}, tells the proprietary Comet {@link Servlet}
      * to resume (again), suspended or do nothing with the current {@link AtmosphereResponse}.
      *
      * @param req the {@link AtmosphereRequest}
      * @param res the {@link AtmosphereResponse}
      * @return action the Action operation.
      * @throws java.io.IOException
      * @throws javax.servlet.ServletException
      */
     public Action timedout(AtmosphereRequest req, AtmosphereResponse res)
             throws IOException, ServletException {
 
         logger.trace("Timing out {}", req);
         if (trackActiveRequest(req) && completeLifecycle(req.resource(), false)) {
             config.framework().notify(Action.TYPE.TIMEOUT, req, res);
         }
         return timedoutAction;
     }
 
     protected boolean trackActiveRequest(AtmosphereRequest req) {
         if (trackActiveRequest) {
             try {
                 long l = (Long) req.getAttribute(MAX_INACTIVE);
                 if (l == -1) {
                     // The closedDetector closed the connection.
                     return false;
                 }
                 req.setAttribute(MAX_INACTIVE, (long) -1);
                 // GlassFish
             } catch (Throwable ex) {
                 logger.trace("Request already recycled", req);
                 // Request is no longer active, return
                 return false;
 
             }
         }
         return true;
     }
 
     /**
      * Cancel or times out an {@link AtmosphereResource} by invoking it's associated {@link AtmosphereHandler#onStateChange(AtmosphereResourceEvent)}
      *
      * @param r         an {@link AtmosphereResource}
      * @param cancelled true if cancelled, false if timedout
      * @return true if the operation was executed.
      */
     protected boolean completeLifecycle(final AtmosphereResource r, boolean cancelled) {
         if (r != null) {
             logger.trace("Finishing lifecycle for AtmosphereResource {}", r.uuid());
             final AtmosphereResourceImpl impl = AtmosphereResourceImpl.class.cast(r);
             synchronized (impl) {
                 try {
                     if (impl.isCancelled()) {
                         logger.trace("{} is already cancelled", impl.uuid());
                         return false;
                     }
 
                     if (cancelled) {
                         impl.getAtmosphereResourceEvent().setCancelled(true);
                     } else {
                         impl.getAtmosphereResourceEvent().setIsResumedOnTimeout(true);
 
                         Broadcaster b = r.getBroadcaster();
                         if (b instanceof DefaultBroadcaster) {
                             ((DefaultBroadcaster) b).broadcastOnResume(r);
                         }
 
                         // TODO: Was it there for legacy reason?
                         // impl.getAtmosphereResourceEvent().setIsResumedOnTimeout(impl.resumeOnBroadcast());
                     }
                     invokeAtmosphereHandler(impl);
 
                     try {
                         impl.getResponse().getOutputStream().close();
                     } catch (Throwable t) {
                         try {
                             impl.getResponse().getWriter().close();
                         } catch (Throwable t2) {
                         }
                     }
                 } catch (Throwable ex) {
                     // Something wrong happenned, ignore the exception
                     logger.trace("Failed to cancel resource: {}", impl.uuid(), ex);
                 } finally {
                     try {
                         impl.notifyListeners();
                         impl.setIsInScope(false);
                         impl.cancel();
                     } catch (Throwable t) {
                         logger.trace("completeLifecycle", t);
                     } finally {
                         impl._destroy();
                     }
                 }
             }
             return true;
         } else {
             logger.debug("AtmosphereResource was null, failed to cancel AtmosphereRequest {}");
             return false;
         }
     }
 
     /**
      * Invoke the associated {@link AtmosphereHandler}. This method must be synchronized on an AtmosphereResource
      *
      * @param r a {@link AtmosphereResourceImpl}
      * @throws IOException
      */
     protected void invokeAtmosphereHandler(AtmosphereResourceImpl r) throws IOException {
         if (!r.isInScope()) {
             logger.trace("AtmosphereResource out of scope {}", r.uuid());
             return;
         }
 
         AtmosphereRequest req = r.getRequest(false);
         String disableOnEvent = r.getAtmosphereConfig().getInitParameter(ApplicationConfig.DISABLE_ONSTATE_EVENT);
         r.getAtmosphereResourceEvent().setMessage(r.writeOnTimeout());
         try {
             if (disableOnEvent == null || !disableOnEvent.equals(String.valueOf(true))) {
                 AtmosphereHandler atmosphereHandler =
                         (AtmosphereHandler)
                                 req.getAttribute(FrameworkConfig.ATMOSPHERE_HANDLER);
 
                 if (atmosphereHandler != null) {
                     try {
                         atmosphereHandler.onStateChange(r.getAtmosphereResourceEvent());
                     } finally {
                         Meteor m = (Meteor) req.getAttribute(AtmosphereResourceImpl.METEOR);
                         if (m != null) {
                             m.destroy();
                         }
                         req.removeAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE);
                     }
                 }
             }
         } catch (IOException ex) {
             try {
                 r.onThrowable(ex);
             } catch (Throwable t) {
                 logger.warn("failed calling onThrowable()", ex);
             }
         }
     }
 
     /**
      * All proprietary Comet based {@link Servlet} must invoke the cancelled
      * method when the underlying WebServer detect that the client closed
      * the connection.
      *
      * @param req the {@link AtmosphereRequest}
      * @param res the {@link AtmosphereResponse}
      * @return action the Action operation.
      * @throws java.io.IOException
      * @throws javax.servlet.ServletException
      */
     public Action cancelled(AtmosphereRequest req, AtmosphereResponse res)
             throws IOException, ServletException {
 
         logger.trace("Cancelling {}", req);
         if (trackActiveRequest(req) && completeLifecycle(req.resource(), true)) {
             config.framework().notify(Action.TYPE.CANCELLED, req, res);
         }
         return cancelledAction;
     }
 
     protected void shutdown() {
         closedDetector.shutdownNow();
         for (AtmosphereResource resource : aliveRequests.values()) {
             try {
                 resource.resume();
             } catch (Throwable t) {
                 // Something wrong happenned, ignore the exception
                 logger.debug("failed on resume: " + resource, t);
             }
         }
     }
 
     @Override
     public boolean supportWebSocket() {
         return false;
     }
 
     /**
      * An Callback class that can be used by Framework integrator to handle the close/timedout/resume life cycle
      * of an {@link AtmosphereResource}. This class support only support {@link AsyncSupport} implementation that
      * extends {@link AsynchronousProcessor}
      */
     public final static class AsynchronousProcessorHook {
 
         private final AtmosphereResourceImpl r;
 
         public AsynchronousProcessorHook(AtmosphereResourceImpl r) {
             this.r = r;
             if (!AsynchronousProcessor.class.isAssignableFrom(r.asyncSupport.getClass())) {
                 throw new IllegalStateException("AsyncSupport must extends AsynchronousProcessor");
             }
         }
 
         public void closed() {
             try {
                 ((AsynchronousProcessor) r.asyncSupport).cancelled(r.getRequest(false), r.getResponse(false));
             } catch (IOException e) {
                 logger.debug("", e);
             } catch (ServletException e) {
                 logger.debug("", e);
             }
         }
 
         public void timedOut() {
             try {
                 ((AsynchronousProcessor) r.asyncSupport).timedout(r.getRequest(false), r.getResponse(false));
             } catch (IOException e) {
                 logger.debug("", e);
             } catch (ServletException e) {
                 logger.debug("", e);
             }
         }
 
         public void resume() {
             ((AsynchronousProcessor) r.asyncSupport).action(r);
         }
     }
 }
