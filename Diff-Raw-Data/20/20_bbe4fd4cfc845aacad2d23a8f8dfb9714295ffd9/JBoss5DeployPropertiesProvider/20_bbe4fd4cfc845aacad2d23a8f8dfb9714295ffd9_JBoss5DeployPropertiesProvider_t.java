 package pl.softwaremill.common.conf;
 
 import org.jboss.mx.util.MBeanServerLocator;
 
 import javax.management.*;
 
 /**
  * JBoss 5 configuration provider
  */
 public class JBoss5DeployPropertiesProvider extends FolderPropertiesProvider {
     @Override
     public String lookupConfigDirectory() {
         try {
             MBeanServer server = MBeanServerLocator.locate();
             return server.getAttribute(new ObjectName("jboss.system:type=ServerConfig"), "ServerConfigURL").toString();
         } catch (RuntimeException e) {
             return null;
         } catch (MBeanException e) {
             return null;
         } catch (AttributeNotFoundException e) {
             return null;
         } catch (InstanceNotFoundException e) {
             return null;
         } catch (ReflectionException e) {
            log.error("Cannot get server conf directory", e);
             return null;
         } catch (MalformedObjectNameException e) {
            log.error("Cannot get server conf directory", e);
             return null;
         }
     }
 
     @Override
     public boolean providerAvailable() {
         try {
             Thread.currentThread().getContextClassLoader().loadClass("org.jboss.mx.util.MBeanServerLocator");
 
             return true;
         } catch (ClassNotFoundException e) {
             // Class not found - not registering.
             return false;
         }
     }
 }
