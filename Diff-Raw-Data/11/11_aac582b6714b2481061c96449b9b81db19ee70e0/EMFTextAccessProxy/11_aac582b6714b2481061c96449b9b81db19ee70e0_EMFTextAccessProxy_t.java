 /*******************************************************************************
  * Copyright (c) 2006-2009 
  * Software Technology Group, Dresden University of Technology
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0 
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *   Software Technology Group - TU Dresden, Germany 
  *      - initial API and implementation
  ******************************************************************************/
 package org.emftext.access;
 
 import java.lang.reflect.Array;
 import java.lang.reflect.InvocationHandler;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.lang.reflect.Proxy;
 
 import org.emftext.access.resource.IColorManager;
 import org.emftext.access.resource.IConfigurable;
 import org.emftext.access.resource.IEditor;
 import org.emftext.access.resource.ILocationMap;
 import org.emftext.access.resource.IMetaInformation;
 import org.emftext.access.resource.INewFileContentProvider;
 import org.emftext.access.resource.IParseResult;
 import org.emftext.access.resource.IParser;
 import org.emftext.access.resource.IPrinter;
 import org.emftext.access.resource.IResource;
 import org.emftext.access.resource.IScanner;
 import org.emftext.access.resource.IToken;
 
 /**
  * The EMFTextAccessProxy class can be used to access generated
  * text resource plug-ins via a set of interfaces. This is 
  * particularly useful for tools that need to perform the same
  * operations over a set of languages.
  * 
  * The access to generated classes using interfaces that are not
  * declared to be implemented by those classes is established by
  * Java's reflection mechanisms. However, the EMFTextAccessProxy
  * class allows to make transparent use of this mechanisms. The
  * basic idea is to provide an object together with an interface
  * the shall be used to access the methods in the object. The 
  * objects class must therefore contain the methods declared in 
  * the interface (i.e., the methods must have the same signature),
  * but the class does not need to declare that it implements the
  * interface.
  * 
  * The central starting point to gather reflective access are the
  * get() methods.
  * 
  * Note that not all kinds of interfaces can be handled by the
  * EMFTextAccessProxy! Methods that use generic types as return
  * type or parameter can not be handled. This is due to Java's
  * type erasure which removes type arguments during compilation.
  * Thus, the type arguments of generic types are not known at
  * run-time, which makes wrapping and unwrapping of objects
  * impossible.
  * Furthermore, the current implementation of the EMFTextAccessProxy
  * can not handle multidimensional arrays.  
  */
 public class EMFTextAccessProxy implements InvocationHandler {
 
	public static Class<?> [] DEFAULT_ACCESS_INTERFACES = {
 			IColorManager.class,
 			IConfigurable.class,
 			IEditor.class,
 			ILocationMap.class,
 			IMetaInformation.class,
 			INewFileContentProvider.class,
 			IParser.class,
 			IParseResult.class,
 			IPrinter.class,
 			IResource.class,
 			IScanner.class,
 			IToken.class
 	};
 
 	protected Object impl;
 	protected Class<?> accessInterface;
 	private Class<?>[] accessInterfaces;
 
 	private EMFTextAccessProxy(Object impl, Class<?> accessInterface, Class<?>[] accessInterfaces) {
 		this.impl = impl;
 		this.accessInterface = accessInterface;
 		this.accessInterfaces = accessInterfaces;
 	}
 
 	/**
 	 * Returns an instance of the given access interface that can be
 	 * used to call the methods of 'impl'.  
 	 * 
 	 * @param impl
 	 * @param accessInterface
 	 * 
 	 * @return a dynamic proxy object implementing 'accessInterface' that
 	 *         delegates all method calls to 'impl'
 	 */
 	public static Object get(Object impl, Class<?> accessInterface) {
 		//proxies are not cached because also new objects (e.g., Parser, Printer) might be created
		return get(impl, accessInterface, DEFAULT_ACCESS_INTERFACES);
 	}
 
 	/**
 	 * Returns an instance of the given access interface that can be
 	 * used to call the methods of 'impl'. In addition to the default
 	 * set of interfaces that are declared to be used to access the 
 	 * object 'impl', more interfaces can be passed to this method.  
 	 * 
 	 * @param impl
 	 * @param accessInterface
 	 * 
 	 * @return a dynamic proxy object implementing 'accessInterface' that
 	 *         delegates all method calls to 'impl'
 	 */
 	public static Object get(Object impl, Class<?> accessInterface, Class<?>[] additionalAccessInterfaces) {
		Class<?>[] allAccessInterfaces = new Class<?>[DEFAULT_ACCESS_INTERFACES.length + additionalAccessInterfaces.length];
 		int i = 0;
		for (Class<?> nextInterface : DEFAULT_ACCESS_INTERFACES) {
 			allAccessInterfaces[i] = nextInterface;
 			i++;
 		}
 		for (Class<?> nextInterface : additionalAccessInterfaces) {
 			allAccessInterfaces[i] = nextInterface;
 			i++;
 		}
 		Object implObject = impl;
 		if (implObject instanceof Proxy) {
 			EMFTextAccessProxy accessProxy = (EMFTextAccessProxy) Proxy.getInvocationHandler(implObject);
 			implObject = accessProxy.impl;
 		}
 		//proxies are not cached because also new objects (e.g., Parser, Printer) might be created
 		return Proxy.newProxyInstance(
 				implObject.getClass().getClassLoader(),
 				new Class[] { accessInterface },
 				new EMFTextAccessProxy(implObject, accessInterface, allAccessInterfaces));
 	}
 
 	private boolean isAccessInterface(Class<?> type) {
 		for(int i = 0; i < accessInterfaces.length; i++) {
 			if (accessInterfaces[i] == type) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
 		//currently access interface arguments are not support
 		Object result = null;
 		Object[] proxyArgs = null;
 		try {
 			Method implMethod;
 			implMethod = getMethod(method);
 			if (args != null) {
 				proxyArgs = new Object[args.length];
 				for (int a = 0; a < args.length; a++) {
 					Object arg = args[a];
 					proxyArgs[a] = unwrapIfNeeded(arg);
 					proxyArgs[a] = unwrapArrayIfNeeded(implMethod, proxyArgs[a], implMethod.getParameterTypes()[a]);
  				}
 			}
 			result = implMethod.invoke(impl, proxyArgs);
 			Class<?> returnType = method.getReturnType();
 			// if the return type of a method is a access interface
 			// we need to wrap the returned object behind a proxy
 			result = wrapIfNeeded(returnType, result);
 			// if the return type of a method is an array containing
 			// elements of an access interface, we need to create
 			// a new array typed with the interface and create
 			// wrapper proxies for all elements contained in the
 			// returned result
 			result = wrapArrayIfNeeded(result, returnType);
 		} catch (IllegalArgumentException e) {
 			e.printStackTrace();
 		} catch (NoSuchMethodException e) {
 			EMFTextAccessPlugin.logError("Required method not defined: " + impl.getClass().getCanonicalName() + "." + method.getName(), null);
 		} catch (InvocationTargetException e) {
 			Throwable cause = e.getCause();
 			if (cause instanceof IllegalArgumentException) {
 				throw (IllegalArgumentException) cause;
 			}
 		}
 		return result;
 	}
 
 	private Object unwrapArrayIfNeeded(Method implMethod, Object arg, Class<?> parameterType) {
 		Object unwrappedArray = arg;
 		// handle args that are arrays
 		if (arg != null && arg.getClass().isArray()) {
 			Class<?> componentType = parameterType.getComponentType();
 			unwrappedArray = Array.newInstance(componentType, Array.getLength(arg));
 			for (int i = 0; i < Array.getLength(arg); i++) {
 				Array.set(unwrappedArray, i, unwrapIfNeeded(Array.get(arg, i)));
 			}
 		}
 		return unwrappedArray;
 	}
 
 	private Object unwrapIfNeeded(Object arg) {
 		Object unwrappedArg = arg;
 		if (arg instanceof Proxy) {
 			Proxy argProxy = (Proxy) arg;
 			InvocationHandler handler = Proxy.getInvocationHandler(argProxy);
 			if (handler instanceof EMFTextAccessProxy) {
 				EMFTextAccessProxy emfHandler = (EMFTextAccessProxy) handler;
 				if (isAccessInterface(emfHandler.accessInterface)) {
 					unwrappedArg = emfHandler.impl;
 				}
 			}
 		}
 		return unwrappedArg;
 	}
 
 	private Object wrapArrayIfNeeded(Object result, Class<?> returnType) {
 		if (result != null && returnType.isArray()) {
 			Class<?> componentType = returnType.getComponentType();
 			if (isAccessInterface(componentType)) {
 				Object wrappingArray = Array.newInstance(componentType, Array.getLength(result));
 				for (int i = 0; i < Array.getLength(result); i++) {
 					Array.set(wrappingArray, i, wrapIfNeeded(componentType, Array.get(result, i)));
 				}
 				result = wrappingArray;
 			}
 		}
 		return result;
 	}
 
 	private Object wrapIfNeeded(Class<?> type, Object result) {
 		if (result != null && isAccessInterface(type)) {
 			result = EMFTextAccessProxy.get(result, type, accessInterfaces);
 		}
 		return result;
 	}
 
 	private Method getMethod(Method method) throws NoSuchMethodException {
 		String methodName = method.getName();
 		Class<?>[] parameterTypes = method.getParameterTypes();
 		// first look for the exact method
 		try {
 			return impl.getClass().getMethod(methodName, parameterTypes);
 		} catch (NoSuchMethodException e) {
 			// ignore exception and continue to search
 		}
 		Method[] methods = impl.getClass().getMethods();
 		// then look for a method with the same name (do not care about parameter types)
 		// this is needed to find methods that use types from the generated plug-ins as
 		// parameters
 		for (Method nextMethod : methods) {
 			if (methodName.equals(nextMethod.getName())) {
 				return nextMethod;
 			}
 		}
 		throw new NoSuchMethodException();
 	}
 }
