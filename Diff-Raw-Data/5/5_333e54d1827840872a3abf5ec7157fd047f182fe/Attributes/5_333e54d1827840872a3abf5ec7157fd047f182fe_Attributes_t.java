 package brooklyn.entity.basic;
 
 import java.util.List;
 
 import brooklyn.event.Sensor;
 import brooklyn.event.basic.BasicAttributeSensor;
 import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
 import brooklyn.event.basic.BasicNotificationSensor;
 import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
 import brooklyn.location.basic.PortRanges;
 
 import com.google.common.collect.ImmutableList;
 
 /**
  * This interface should be used to access {@link Sensor} definitions.
  */
 public interface Attributes {
     
     BasicNotificationSensor<Void> LOCATION_CHANGED = new BasicNotificationSensor<Void>(
             Void.class, "entity.locationChanged", "Indicates that an entity's location has been changed");
 
 
     /*
      * Application information sensors.
      * @see SoftwareProcessEntities.SUGGESTED_VERSION
      */
     BasicAttributeSensor<String> VERSION = new BasicAttributeSensor<String>(String.class, "version", "Version information");
 
     /*
      * JMX attributes.
      */
 
     // 1099 is standard, sometimes 9999
     PortAttributeSensorAndConfigKey JMX_PORT = new PortAttributeSensorAndConfigKey(
             "jmx.port", "JMX port (RMI registry port)", PortRanges.fromString("1099, 31099+"));
     
     // usually chosen by java; setting this will often not have any effect
     PortAttributeSensorAndConfigKey RMI_SERVER_PORT = new PortAttributeSensorAndConfigKey(
             "rmi.server.port", "RMI server port", PortRanges.fromString("9001, 39001+"));
 
     @Deprecated // since 0.4, use RMI_REGISTRY_PORT instead
     PortAttributeSensorAndConfigKey RMI_PORT = RMI_SERVER_PORT;
 
     BasicAttributeSensorAndConfigKey<String> JMX_USER = new BasicAttributeSensorAndConfigKey<String>(
             String.class, "jmx.user", "JMX username");
     
     BasicAttributeSensorAndConfigKey<String> JMX_PASSWORD = new BasicAttributeSensorAndConfigKey<String>(
             String.class, "jmx.password", "JMX password");
     
     BasicAttributeSensorAndConfigKey<String> JMX_CONTEXT = new BasicAttributeSensorAndConfigKey<String>(
             String.class, "jmx.context", "JMX context path", "jmxrmi");
     
     BasicAttributeSensorAndConfigKey<String> JMX_SERVICE_URL = new BasicAttributeSensorAndConfigKey<String>(
             String.class, "jmx.serviceurl", "The URL for connecting to the MBean Server");
     
     /*
      * Port number attributes.
      */
 
     BasicAttributeSensor<List<Integer>> PORT_NUMBERS = new BasicAttributeSensor(
             List.class, "port.list", "List of port numbers");
     
     BasicAttributeSensor<List<Sensor<Integer>>> PORT_SENSORS = new BasicAttributeSensor(
             List.class, "port.list.sensors", "List of port number attributes");
 
     PortAttributeSensorAndConfigKey HTTP_PORT = new PortAttributeSensorAndConfigKey(
            "http.port", "HTTP port", ImmutableList.of(8080,"18080+"));
     
     PortAttributeSensorAndConfigKey HTTPS_PORT = new PortAttributeSensorAndConfigKey(
            "https.port", "HTTP port (with SSL/TLS)", ImmutableList.of(8443,"18443+"));
                     
     PortAttributeSensorAndConfigKey SSH_PORT = new PortAttributeSensorAndConfigKey("ssh.port", "SSH port", 22);
     PortAttributeSensorAndConfigKey SMTP_PORT = new PortAttributeSensorAndConfigKey("smtp.port", "SMTP port", 25);
     PortAttributeSensorAndConfigKey DNS_PORT = new PortAttributeSensorAndConfigKey("dns.port", "DNS port", 53);
     PortAttributeSensorAndConfigKey AMQP_PORT = new PortAttributeSensorAndConfigKey("amqp.port", "AMQP port", "5672+");
 
     /*
      * Location/connection attributes.
      */
 
     BasicAttributeSensor<String> HOSTNAME = new BasicAttributeSensor<String>(String.class, "host.name", "Host name");
     BasicAttributeSensor<String> ADDRESS = new BasicAttributeSensor<String>(String.class, "host.address", "Host IP address");
 	
     /*
      * Lifecycle attributes
      */
     
     BasicAttributeSensor<Lifecycle> SERVICE_STATE = new BasicAttributeSensor<Lifecycle>(
             Lifecycle.class, "service.state", "Service lifecycle state");
     
 	/** optional */
     BasicAttributeSensor<String> LOG_FILE_LOCATION = new BasicAttributeSensor<String>(
             String.class, "log.location", "log file location");
 }
