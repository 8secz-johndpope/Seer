 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.itmill.toolkit.launcher;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import org.mortbay.jetty.Connector;
 import org.mortbay.jetty.Server;
 import org.mortbay.jetty.nio.SelectChannelConnector;
 import org.mortbay.jetty.webapp.WebAppContext;
 
 import com.itmill.toolkit.launcher.util.BrowserLauncher;
 
 /**
  * Class for running Jetty servlet container within Eclipse project.
  * 
  */
 public class ITMillToolkitWebMode {
 
     private final static String serverPort = "8888";
 
     /**
      * Main function for running Jetty.
      * 
      * Command line Arguments are passed through to Jetty, see runServer method
      * for options.
      * 
      * @param args
      */
     public static void main(String[] args) {
 
         // Pass-through of arguments for Jetty
         final Map serverArgs = parseArguments(args);
 
         // Start Jetty
         System.out.println("Starting Jetty servlet container.");
         final String url = runServer(serverArgs, "Web Mode");
 
         // Start Browser
         System.out.println("Starting Web Browser.");
         if (url != null) {
             BrowserLauncher.openBrowser(url);
         }
 
     }
 
     /**
      * Run the server with specified arguments.
      * 
      * @param serverArgs
      * @return
      */
     protected static String runServer(Map serverArgs, String mode) {
 
         // Add help for System.out
         System.out
                 .println("-------------------------------------------------\n"
                         + "Starting IT Mill Toolkit in "
                         + mode
                         + ".\n"
                         + "Running in http://localhost:"
                         + serverPort
                         + "\n-------------------------------------------------\n");
 
         // Assign default values for some arguments
         assignDefault(serverArgs, "webroot", "WebContent");
         assignDefault(serverArgs, "httpPort", serverPort);
 
         try {
            final long started = System.currentTimeMillis();

             final Server server = new Server();
 
            // String threadPoolName =
            // System.getProperty("jetty.threadpool.name",
            // "Jetty thread");
            // int maxIdleTimeMs = Integer.getInteger(
            // "jetty.threadpool.maxIdleTimeMs", 60000);
            // int maxThreads =
            // Integer.getInteger("jetty.threadpool.maxThreads",
            // 100);
            // int minThreads =
            // Integer.getInteger("jetty.threadpool.minThreads",
            // 1);
            // int lowThreads = Integer.getInteger(
            // "jetty.threadpool.maxIdleTimeMs", 25);
            // BoundedThreadPool threadPool = new BoundedThreadPool();
            // threadPool.setName(threadPoolName);
            // threadPool.setMaxIdleTimeMs(maxIdleTimeMs);
            // threadPool.setMaxThreads(maxThreads);
            // threadPool.setMinThreads(minThreads);
            // threadPool.setLowThreads(lowThreads);
            // server.setThreadPool(threadPool);

             final Connector connector = new SelectChannelConnector();
            // FIXME httpPort hardcoded to 8888
            // connector.setPort(Integer.valueOf(serverArgs.get("httpPort")
            // .toString()));
             connector.setPort(8888);
             server.setConnectors(new Connector[] { connector });
 
             final WebAppContext webappcontext = new WebAppContext();
             webappcontext.setContextPath("");
             webappcontext.setWar(serverArgs.get("webroot").toString());
            // enable hot code replace
            webappcontext.setCopyWebDir(true);
 
             server.setHandler(webappcontext);
 
             server.start();
            // System.err.println("Started Jetty in "
            // + (System.currentTimeMillis() - started) + "ms.");
         } catch (final Exception e) {
             e.printStackTrace();
             return null;
         }
 
         return "http://localhost:" + serverArgs.get("httpPort");
     }
 
     /**
      * Assign default value for given key.
      * 
      * @param map
      * @param key
      * @param value
      */
     private static void assignDefault(Map map, String key, String value) {
         if (!map.containsKey(key)) {
             map.put(key, value);
         }
     }
 
     /**
      * Parse all command line arguments into a map.
      * 
      * Arguments format "key=value" are put into map.
      * 
      * @param args
      * @return map of arguments key value pairs.
      */
     protected static Map parseArguments(String[] args) {
         final Map map = new HashMap();
         for (int i = 0; i < args.length; i++) {
             final int d = args[i].indexOf("=");
             if (d > 0 && d < args[i].length() && args[i].startsWith("--")) {
                 final String name = args[i].substring(2, d);
                 final String value = args[i].substring(d + 1);
                 map.put(name, value);
             }
         }
         return map;
     }
 
 }
