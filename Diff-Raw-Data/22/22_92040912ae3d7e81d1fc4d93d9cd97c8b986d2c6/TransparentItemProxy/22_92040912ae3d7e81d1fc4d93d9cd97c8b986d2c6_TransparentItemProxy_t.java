 package gnu.cajo.utils.extra;
 
 import gnu.cajo.invoke.*;
 import java.lang.reflect.*;
 import java.io.IOException;
 
 import java.rmi.RemoteException;
 import java.rmi.NotBoundException;
 import java.net.MalformedURLException;
 import java.lang.reflect.InvocationTargetException;
 
 /*
  * Item Transparent Dynamic Proxy (requires JRE 1.3+)
  * Copyright (c) 2005 John Catherino
  * The cajo project: https://cajo.dev.java.net
  *
  * For issues or suggestions mailto:cajo@dev.java.net
  *
  * This file TransparentItemProxy.java is part of the cajo library.
  *
  * The cajo library is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public Licence as published
  * by the Free Software Foundation, at version 3 of the licence, or (at your
  * option) any later version.
  *
 * The cajo library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU Lesser General Public Licence for more details.
  *
  * You should have received a copy of the GNU Lesser General Public Licence
  * along with this library. If not, see http://www.gnu.org/licenses/lgpl.html
  */
 
 /**
  * This class creates an object, representing a server item. The returned
  * object will implement a list of interfaces defined by the client. The
  * interfaces are completely independent of interfaces the server object
  * implements, if any; they are simply logical groupings, of interest solely
  * to the client. The interface methods <i>'should'</i> declare that they
  * throw <tt>java.lang.Exception</tt>: However, the Java dynamic proxy
  * mechanism very <i>dubiously</i> allows this to be optional.
  *
  * <p>Clients can dynamically create an item wrapper classes implementing
  * <i>any</i> combination of interfaces. Combining interfaces provides a very
  * important second order of abstraction. Whereas an interface is considered
  * to be a grouping of functionality; the <i>'purpose'</i> of an item, could
  * be determined by the group of interfaces it implements. Clients can
  * customise the way they use remote items as a function of the interfaces
  * implemented.
  *
  * <p>If an item can be best represented with a single interface, it would be
  * well to consider using a {@link Wrapper Wrapper} class instead. It is
  * conceptually much simpler.
  *
  * <p><i><u>Note</u>:</i> Unfortunately, this class only works with JREs 1.3
  * and higher. Therefore I was reluctant to include it in the official
  * codebase. However, some very convincing <a href=https://cajo.dev.java.net/servlets/ProjectForumMessageView?forumID=475&messageID=10199>
  * discussion</a> in the cajo project Developer's forum, with project member
  * Bharavi Gade, caused me to reconsider.
  *
  * <p><i><u>Also note</u>:</i> The three fundamental object methods; hashCode,
  * equals, and toString, are performed locally. This is because the remote
  * invocation could fail, most unintuitively. This is just to make the class
  * operation much more intuitive.
  *
  * @version 1.1, 11-Nov-05 Support multiple interfaces
  * @author John Catherino
  */
 public final class TransparentItemProxy implements InvocationHandler {
    private final Object item;
    private final Object name;
   private Integer hashcode;
    private TransparentItemProxy(Object item) {
       this.item = item;
       Object temp = null;
       try { temp = Remote.invoke(item, "toString", null); }
       catch(Exception x) {
          throw new IllegalArgumentException(x.getLocalizedMessage());
       }
       name = temp;
    }
    /**
     * An optional centralised invocation error handler. If an invocation on
     * a remote object results in a checked or unchecked exception being thrown;
     * this object, if assigned, will be called to deal with it. This allows
     * all retry/recovery/rollback logic, etc. to be located in a single place.
     * It is expected that this object will implement a method of the following
     * signature:<p>
     * <blockquote><tt>
     * public Object handle(Object proxy, String method, Object args[], Throwable t)
     * throws Throwable; </tt></blockquote><p>
     * The first arguments are as follows:<ul>
     * <li>the local proxy to the remote object generating the error
     * <li>the method that was called on the remote object
     * <li>the arguments that were provided in the method call
     * <li>the error that resulted from the invocation</ul>
     * The handler will either successfully recover from the error, and return
     * the appropriate result, or throw a hopefully more descriptive error.
     * <i><u>Note</u>:</i> the <tt>toString()</tt> method can be invoked
     * on the proxy object, to determine its identity, or the handler could
     * maintain a map of proxies, to proxy-specific handler objects.     
     */
     public static Object handler;
    /**
     * This method, inherited from InvocationHandler, simply passes all object
     * method invocations on to the remote object, automatically and
     * transparently. This allows the local runtime to perform remote item
     * invocations, while appearing syntactically identical to local ones.
     * @param proxy The local object on which the method was invoked, it is
     * <i>ignored</i> in this context.
     * @param method The method to invoke on the object, in this case the
     * server item.
     * @param args The arguments to provide to the method, if any.
     * @return The resulting data from the method invocation, if any.
     * @throws java.rmi.RemoteException For network communication related
     * reasons.
     * @throws NoSuchMethodException If no matching method can be found
     * on the server item.
     * @throws Exception If the server item rejected the invocation, for
     * application specific reasons.
     * @throws Throwable For some <i>very</i> unlikely reasons, not outlined
     * above. <i>(required, sorry)</i>
     */
    public Object invoke(Object proxy, Method method, Object args[])
       throws Throwable {
       String name = method.getName();
      if (name.equals("toString") && (args == null || args.length == 0)) {
         // perform shallow toString...
          return this.name;
      } else if (name.equals("hashCode") && (args == null || args.length == 0)) {
         // perform shallow hashCode...
         if (hashcode == null) hashcode = new Integer(this.hashCode());
         return hashcode;
      } else if (name.equals("equals") && args.length == 1) {
          // perform shallow equals...
         return args[0].equals(this) ? Boolean.TRUE : Boolean.FALSE;
      } else try { return Remote.invoke(item, name, args); }
       catch(Throwable t) {
          if (handler != null)
             return Remote.invoke(handler, "handle",
                new Object[] { proxy, method, args, t });
          else throw t;
       }
    }
    /**
     * This generates a class definition for a remote object reference at
     * runtime, and returns a local object instance. The resulting dynamic
     * proxy object will implement all the interfaces provided.
     * @param item A reference to a remote server object.
     * @param interfaces The list of interface classes for the dynamic proxy
     * to implement. Typically, these are provided thus; <tt>new Class[] {
     * Interface1.class, Interface2.class, ... }</tt>
     * @return A reference to the server item, wrapped in the local object,
     * created at runtime. It can then be typecast into any of the interfaces,
     * as needed by the client.
     */
    public static Object getItem(Object item, Class interfaces[]) {
       return Proxy.newProxyInstance(
          interfaces[0].getClassLoader(), interfaces,
          new TransparentItemProxy(item)
       );
    }
    /**
     * This method fetches a server item reference, generates a class
     * definition for it at runtime, and returns a local object instance.
     * The resulting dynamic proxy object will implement all the interfaces
     * provided. Technically, it invokes the other getItem method, after
     * fetching the object reference from the remote JVM specified.
     * @param url The URL where to get the object: //[host][:port]/[name].
     * The host, port, and name, are all optional. If missing the host is
     * presumed local, the port 1099, and the name "main". If the URL is
     * null, it will be assumed to be ///.
     * @param interfaces The list of interface classes for the dynamic proxy
     * to implement. Typically, these are provided thus; <tt>new Class[] {
     * Interface1.class, Interface2.class, ... }</tt>
     * @return A reference to the server item, wrapped in the object created
     * at runtime, implementing all of the interfaces provided. It can then
     * be typecast into any of the interfaces, as needed by the client.
     * @throws RemoteException if the remote registry could not be reached.
     * @throws NotBoundException if the requested name is not in the registry.
     * @throws IOException if the zedmob format is invalid.
     * @throws ClassNotFoundException if a proxy was sent to the VM, and
     * proxy hosting was not enabled.
     * @throws InstantiationException when the URL specifies a class name
     * which cannot be instantiated at runtime.
     * @throws IllegalAccessException when the url specifies a class name
     * and it does not support a no-arg constructor.
     * @throws MalformedURLException if the URL is not in the format explained
     */
    public static Object getItem(String url, Class interfaces[])
       throws RemoteException, NotBoundException, IOException,
       ClassNotFoundException, InstantiationException, IllegalAccessException,
       MalformedURLException {
       return getItem(Remote.getItem(url), interfaces);
    }
 }
