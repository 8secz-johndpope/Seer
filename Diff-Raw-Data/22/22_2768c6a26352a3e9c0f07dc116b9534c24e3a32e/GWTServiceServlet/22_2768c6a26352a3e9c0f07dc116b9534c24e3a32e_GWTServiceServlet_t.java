 /*
  * Copyright 2004-2009 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  * either express or implied. See the License for the specific language
  * governing permissions and limitations under the License.
  */
 package org.slim3.gwt.server;
 
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.slim3.controller.HotReloadingClassLoader;
 import org.slim3.jdo.CurrentPersistenceManager;
 import org.slim3.util.ClassUtil;
 import org.slim3.util.RequestLocator;
 import org.slim3.util.ResponseLocator;
import org.slim3.util.ServletContextLocator;
 
 import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
 import com.google.gwt.user.client.rpc.RemoteService;
 import com.google.gwt.user.client.rpc.SerializationException;
 import com.google.gwt.user.server.rpc.RPC;
 import com.google.gwt.user.server.rpc.RPCRequest;
 import com.google.gwt.user.server.rpc.RemoteServiceServlet;
 import com.google.gwt.user.server.rpc.SerializationPolicy;
 import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
 
 /**
  * The remote service servlet for Slim3.
  * 
  * @author higa
  * @since 3.0
  * 
  */
 public class GWTServiceServlet extends RemoteServiceServlet {
 
     private static final long serialVersionUID = 1L;
 
     private static HotSerializationPolicy hotSerializationPolicy =
         new HotSerializationPolicy();
 
    /**
     * Whether the servlet context is set to {@link ServletContextLocator}.
     */
    protected boolean servletContextSet = false;

     @Override
     public void init() throws ServletException {
         super.init();
        if (ServletContextLocator.get() == null) {
            ServletContextLocator.set(getServletContext());
            servletContextSet = true;
        }
    }

    @Override
    public void destroy() {
        if (servletContextSet) {
            ServletContextLocator.set(null);
        }
     }
 
     /**
      * Process a call originating from the given request. Uses the
      * {@link RPC#invokeAndEncodeResponse(Object, java.lang.reflect.Method, Object[])}
      * method to do the actual work.
      * <p>
      * Subclasses may optionally override this method to handle the payload in
      * any way they desire (by routing the request to a framework component, for
      * instance). The {@link HttpServletRequest} and {@link HttpServletResponse}
      * can be accessed via the {@link #getThreadLocalRequest()} and
      * {@link #getThreadLocalResponse()} methods.
      * </p>
      * This is public so that it can be unit tested easily without HTTP.
      * 
      * @param payload
      *            the UTF-8 request payload
      * @return a string which encodes either the method's return, an exception
      *         thrown by the method
      * @throws SerializationException
      *             if we cannot serialize the response
      */
     @Override
     public String processCall(String payload) throws SerializationException {
         HttpServletRequest previousRequest = RequestLocator.get();
         if (previousRequest == null) {
             RequestLocator.set(getThreadLocalRequest());
             ResponseLocator.set(getThreadLocalResponse());
         }
         try {
             S3RPCRequest request = decodeRequest(payload);
             RPCRequest rpcRequest = request.getOriginalRequest();
             onAfterRequestDeserialized(rpcRequest);
             Object result =
                 invoke(request.getService(), rpcRequest.getMethod(), rpcRequest
                     .getParameters());
             onAfterMethodInvoked(request, result);
             return RPC.encodeResponseForSuccess(
                 rpcRequest.getMethod(),
                 result,
                 rpcRequest.getSerializationPolicy());
         } catch (IncompatibleRemoteServiceException ex) {
             log(
                 "An IncompatibleRemoteServiceException was thrown while processing this call.",
                 ex);
             return RPC.encodeResponseForFailure(null, ex);
         } catch (InvocationTargetException ex) {
             Throwable cause = ex.getCause();
             log("An exception was thrown while processing this call.", cause);
             return RPC.encodeResponseForFailure(null, cause);
         } finally {
             if (previousRequest == null) {
                 RequestLocator.set(null);
                 ResponseLocator.set(null);
             }
         }
     }
 
     /**
      * Returns RPC request.
      * 
      * @param encodedRequest
      *            a string that encodes the service interface, the service
      *            method, and the arguments
      * @return RPC request
      * 
      * @throws NullPointerException
      *             if the encodedRequest is <code>null</code>
      * @throws IllegalArgumentException
      *             if the encodedRequest is an empty string
      * @throws IncompatibleRemoteServiceException
      *             if any of the following conditions apply:
      *             <ul>
      *             <li>if the types in the encoded request cannot be
      *             deserialized</li>
      *             <li>if the {@link ClassLoader} acquired from
      *             <code>Thread.currentThread().getContextClassLoader()</code>
      *             cannot load the service interface or any of the types
      *             specified in the encodedRequest</li>
      *             <li>the requested interface is not assignable to
      *             {@link RemoteService}</li>
      *             <li>the service method requested in the encodedRequest is not
      *             a member of the requested service interface</li>
      *             <li>the type parameter is not <code>null</code> and is not
      *             assignable to the requested {@link RemoteService} interface
      *             </ul>
      */
     protected S3RPCRequest decodeRequest(String encodedRequest) {
         if (encodedRequest == null) {
             throw new NullPointerException(
                 "The encodedRequest parameter is null.");
         }
         if (encodedRequest.length() == 0) {
             throw new IllegalArgumentException(
                 "The encodedRequest parameter is empty.");
         }
         ClassLoader classLoader =
             Thread.currentThread().getContextClassLoader();
         try {
             ServerSerializationStreamReader streamReader =
                 new ServerSerializationStreamReader(classLoader, this);
             streamReader.prepareToRead(encodedRequest);
             String interfaceName = streamReader.readString();
             Class<?> serviceClass = getServiceClass(interfaceName);
             Object service = getService(serviceClass);
             SerializationPolicy serializationPolicy =
                 streamReader.getSerializationPolicy();
             String methodName = streamReader.readString();
             int paramCount = streamReader.readInt();
             Class<?>[] parameterTypes = new Class[paramCount];
             for (int i = 0; i < parameterTypes.length; i++) {
                 String paramClassName = streamReader.readString();
                 parameterTypes[i] = getClass(paramClassName);
             }
 
             try {
                 Method method =
                     serviceClass.getMethod(methodName, parameterTypes);
                 Object[] parameterValues = new Object[parameterTypes.length];
                 for (int i = 0; i < parameterValues.length; i++) {
                     parameterValues[i] =
                         streamReader.deserializeValue(parameterTypes[i]);
                 }
                 return new S3RPCRequest(service, new RPCRequest(
                     method,
                     parameterValues,
                     serializationPolicy));
 
             } catch (NoSuchMethodException e) {
                 throw new IncompatibleRemoteServiceException(e.getMessage(), e);
             }
         } catch (SerializationException ex) {
             throw new IncompatibleRemoteServiceException(ex.getMessage(), ex);
         }
     }
 
     /**
      * Invokes the service method.
      * 
      * @param service
      *            the service
      * @param method
      *            the method
      * @param args
      *            the arguments
      * @return the return value of the method
      * @throws InvocationTargetException
      *             if the method throws an exception
      */
     protected Object invoke(Object service, Method method, Object[] args)
             throws InvocationTargetException {
         try {
             return method.invoke(service, args);
         } catch (IllegalArgumentException e) {
             throw new IncompatibleRemoteServiceException(e.getMessage(), e);
         } catch (IllegalAccessException e) {
             throw new IncompatibleRemoteServiceException(e.getMessage(), e);
         }
     }
 
     /**
      * This method is invoked after the service method is invoked.
      * 
      * @param request
      *            the request
      * @param result
      *            the result
      */
     protected void onAfterMethodInvoked(S3RPCRequest request, Object result) {
         CurrentPersistenceManager.destroy();
     }
 
     @Override
     protected SerializationPolicy doGetSerializationPolicy(
             HttpServletRequest request, String moduleBaseURL, String strongName) {
         if (Thread.currentThread().getContextClassLoader() instanceof HotReloadingClassLoader) {
             return hotSerializationPolicy;
         }
         return super.doGetSerializationPolicy(
             request,
             moduleBaseURL,
             strongName);
     }
 
     /**
      * Returns the class specified by the name.
      * 
      * @param className
      *            the class name
      * @return the class
      * @throws NullPointerException
      *             if the className parameter is null
      */
     protected Class<?> getClass(String className) throws NullPointerException {
         if (className == null) {
             throw new NullPointerException("The className parameter is null.");
         }
         try {
             return Class.forName(className, false, Thread
                 .currentThread()
                 .getContextClassLoader());
         } catch (ClassNotFoundException e) {
             throw new IncompatibleRemoteServiceException(
                 "Could not load the class("
                     + className
                     + ") in context classloader",
                 e);
         }
     }
 
     /**
      * Returns the service class specified by the interface name.
      * 
      * @param interfaceName
      *            the interface name
      * @return the service class
      * @throws NullPointerException
      *             if the interfaceName parameter is null
      */
     protected Class<?> getServiceClass(String interfaceName)
             throws NullPointerException {
         if (interfaceName == null) {
             throw new NullPointerException(
                 "The interfaceName parameter is null.");
         }
         String className =
             interfaceName.replace(".client.", ".server.") + "Impl";
         return getClass(className);
     }
 
     /**
      * Returns the service.
      * 
      * @param serviceClass
      *            the class name
      * @return the service
      * @throws NullPointerException
      *             if the serviceClass parameter is null
      */
     protected Object getService(Class<?> serviceClass)
             throws NullPointerException {
         if (serviceClass == null) {
             throw new NullPointerException(
                 "The serviceClass parameter is null.");
         }
         return ClassUtil.newInstance(serviceClass);
     }
 }
