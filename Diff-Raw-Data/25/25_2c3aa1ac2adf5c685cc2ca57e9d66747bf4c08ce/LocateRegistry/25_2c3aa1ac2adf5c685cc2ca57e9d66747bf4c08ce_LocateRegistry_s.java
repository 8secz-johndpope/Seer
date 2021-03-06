 package ibis.rmi.registry;
 
 import ibis.rmi.impl.RTS;
 import ibis.rmi.RemoteException;
 
 /**
  * The <code>LocateRegistry</code> class is a container class for methods that
  * are used to obtain a reference to a remote registry or to create a registry.
  */
 public final class LocateRegistry
 {
     private LocateRegistry() {}
 
     /**
      * Returns a reference to the remote <code>Registry</code> for the local
      * host on the default port.
      * @return a stub for the remote registry.
      * @exception RemoteException if the stub could not be created.
      */
     public static Registry getRegistry() throws RemoteException
     {
 	return getRegistry(null, Registry.REGISTRY_PORT);
     }
 
     /**
      * Returns a reference to the remote <code>Registry</code> for the local
      * host on the specified port.
      * @param port the port on which the registry accepts requests
      * @return a stub for the remote registry.
      * @exception RemoteException if the stub could not be created.
      */
     public static Registry getRegistry(int port) throws RemoteException
     {
 	return getRegistry(null, port);
     }
     
     /**
      * Returns a reference to the remote <code>Registry</code> for the
      * specified host on the default port.
      * @param host host for the remote registry
      * @return a stub for the remote registry.
      * @exception RemoteException if the stub could not be created.
      */
     public static Registry getRegistry(String host) throws RemoteException
     {
 	return getRegistry(host, Registry.REGISTRY_PORT);
     }
     
     /**
      * Returns a reference to the remote <code>Registry</code> for the
      * specified host on the specified port.
      * @param host host for the remote registry
      * @param port the port on which the registry accepts requests
      * @return a stub for the remote registry.
      * @exception RemoteException if the stub could not be created.
      */
     public static Registry getRegistry(String host, int port) throws RemoteException
     {
 	return (Registry) new ibis.rmi.registry.impl.RegistryImpl(host, port);
     }
 
     /**
      * Creates and exports a <code>Registry</code> on the local host.
      * This registry will accept requests on the specified port.
      * @param port the port on which the registry accepts requests
      * @return the registry.
      */
     public static Registry createRegistry(int port) throws RemoteException
     {
 	return (Registry) new ibis.rmi.registry.impl.RegistryImpl(port);
     }
 
     static {
 	try {
 	    String hostname = RTS.getHostname();
 	    // Just to make sure that RTS is initialized and that there is
 	    // an Ibis.
 	} catch (Exception e) {
 	}
     }
 }
