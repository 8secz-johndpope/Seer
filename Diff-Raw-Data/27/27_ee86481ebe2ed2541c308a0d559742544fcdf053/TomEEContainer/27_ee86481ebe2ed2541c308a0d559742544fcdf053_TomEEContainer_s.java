 /**
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
 package org.apache.openejb.arquillian;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Writer;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 
import javax.naming.Context;
import javax.naming.InitialContext;

 import org.apache.catalina.startup.Bootstrap;
 import org.apache.openejb.AppContext;
import org.apache.openejb.BeanContext;
 import org.apache.openejb.assembler.Deployer;
 import org.apache.openejb.assembler.classic.AppInfo;
 import org.apache.openejb.assembler.classic.Assembler;
 import org.apache.openejb.config.ConfigurationFactory;
 import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
 import org.apache.openejb.tomcat.catalina.TomcatLoader;
 import org.apache.velocity.Template;
 import org.apache.velocity.VelocityContext;
 import org.apache.velocity.app.Velocity;
 import org.apache.velocity.runtime.log.Log4JLogChute;
 import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
 import org.jboss.arquillian.spi.client.container.DeployableContainer;
 import org.jboss.arquillian.spi.client.container.DeploymentException;
 import org.jboss.arquillian.spi.client.container.LifecycleException;
 import org.jboss.arquillian.spi.client.protocol.ProtocolDescription;
 import org.jboss.arquillian.spi.client.protocol.metadata.HTTPContext;
 import org.jboss.arquillian.spi.client.protocol.metadata.ProtocolMetaData;
 import org.jboss.shrinkwrap.api.Archive;
 import org.jboss.shrinkwrap.api.exporter.ZipExporter;
 import org.jboss.shrinkwrap.descriptor.api.Descriptor;
 
 public class TomEEContainer implements DeployableContainer<TomEEConfiguration> {
 
     private Bootstrap bootstrap;
     private TomEEConfiguration configuration;
     private File catalinaDirectory;
    private Map<String, AppContext> moduleIds = new HashMap<String, AppContext>();
     private Deployer deployer;
     private ConfigurationFactory configurationFactory;
     private Assembler assembler;
 
     public Class<TomEEConfiguration> getConfigurationClass() {
         return TomEEConfiguration.class;
     }
 
     public void setup(TomEEConfiguration configuration) {
         this.configuration = configuration;
     }
 
     public static void main(String[] args) {
     	try {
 			TomEEContainer tomEEContainer = new TomEEContainer();
 			TomEEConfiguration cfg = new TomEEConfiguration();
 
             File dir = FileUtils.createTempDir();
             cfg.setDir(dir.getAbsolutePath());
 			tomEEContainer.setup(cfg);
 			tomEEContainer.start();
 			Thread.sleep(120000);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
     }
     
     public void start() throws LifecycleException {
         try {
             catalinaDirectory = new File(configuration.getDir());
             if (catalinaDirectory.exists()) {
                 catalinaDirectory.delete();
             }
 
             catalinaDirectory.mkdirs();
             catalinaDirectory.deleteOnExit();
 
             createTomcatDirectories(catalinaDirectory);
             copyConfigs(catalinaDirectory);
 
             // Bootstrap Tomcat
             System.out.println("Starting TomEE from: " + catalinaDirectory.getAbsolutePath());
 
             String catalinaBase = catalinaDirectory.getAbsolutePath();
             System.setProperty("openejb.deployments.classpath.exclude", ".*");
             System.setProperty("openejb.deployments.classpath.include", "");
             System.setProperty("catalina.home", catalinaBase);
             System.setProperty("catalina.base", catalinaBase);
             System.setProperty("openejb.home", catalinaBase);
             System.setProperty("openejb.base", catalinaBase);
             System.setProperty("openejb.servicemanager.enabled", "true");
             
             bootstrap = new Bootstrap();
             bootstrap.start();
             
             // Bootstrap OpenEJB
             Properties properties = new Properties();
             properties.setProperty("openejb.loader", "tomcat-system");
             properties.setProperty("openejb.home", catalinaBase);
             properties.setProperty("openejb.base", catalinaBase);
             properties.setProperty("openejb.servicemanager.enabled", "true");
             
             try {
                 Properties tomcatServerInfo = new Properties();
                 ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                 tomcatServerInfo.load(classLoader.getResourceAsStream("org/apache/catalina/util/ServerInfo.properties"));
 
                 String serverNumber = tomcatServerInfo.getProperty("server.number");
                 if (serverNumber == null) {
                     // Tomcat5 only has server.info
                     String serverInfo = tomcatServerInfo.getProperty("server.info");
                     if (serverInfo != null) {
                         int slash = serverInfo.indexOf('/');
                         serverNumber = serverInfo.substring(slash + 1);
                     }
                 }
                 if (serverNumber != null) {
                     System.setProperty("tomcat.version", serverNumber);
                 }
 
                 String serverBuilt = tomcatServerInfo.getProperty("server.built");
                 if (serverBuilt != null) {
                     System.setProperty("tomcat.built", serverBuilt);
                 }
             } catch (Throwable e) {
             }
 
 			new TomcatLoader().init(properties);
 
             assembler = SystemInstance.get().getComponent(Assembler.class);
             configurationFactory = new ConfigurationFactory();
 
         } catch (Exception e) {
             e.printStackTrace();
             throw new LifecycleException("Something went wrong", e);
         }
     }
 
     public void stop() throws LifecycleException {
         try {
             bootstrap.stopServer();
             deleteTree(catalinaDirectory);
         } catch (Exception e) {
             throw new LifecycleException("Unable to stop server", e);
         }
     }
 
     public ProtocolDescription getDefaultProtocol() {
         return new ProtocolDescription("Servlet 3.0");
     }
 
     public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
     	try {
 
    		String tmpDir = System.getProperty("java.io.tmpdir");
    		File file = new File(tmpDir + File.separator + archive.getName());
         	archive.as(ZipExporter.class).exportTo(file, true);
 
 
             AppInfo appInfo = configurationFactory.configureApplication(file);
            AppContext application = assembler.createApplication(appInfo);
            moduleIds.put(archive.getName(), application);
            
             HTTPContext httpContext = new HTTPContext("0.0.0.0", configuration.getHttpPort());
             return new ProtocolMetaData().addContext(httpContext);
         } catch (Exception e) {
             e.printStackTrace();
             throw new DeploymentException("Unable to deploy", e);
         }
     }
 
     public void undeploy(Archive<?> archive) throws DeploymentException {
     	try {
            AppContext appContext = moduleIds.get(archive.getName());
            assembler.destroyApplication(appContext);
         } catch (Exception e) {
             e.printStackTrace();
             throw new DeploymentException("Unable to undeploy", e);
         }
     }
 
     private void deleteTree(File file) {
         if (file == null)
             return;
         if (!file.exists())
             return;
 
         if (file.isFile()) {
             file.delete();
             return;
         }
 
         if (file.isDirectory()) {
             if (".".equals(file.getName()))
                 return;
             if ("..".equals(file.getName()))
                 return;
 
             File[] children = file.listFiles();
 
             for (File child : children) {
                 deleteTree(child);
             }
 
             file.delete();
         }
     }
 
     public void deploy(Descriptor descriptor) throws DeploymentException {
         throw new UnsupportedOperationException("Not implemented");
     }
 
     public void undeploy(Descriptor descriptor) throws DeploymentException {
         throw new UnsupportedOperationException("Not implemented");
     }
 
     private void copyConfigs(File directory) throws Exception {
         File confDir = new File(directory, "conf");
         copyFileTo(confDir, "catalina.policy");
         copyTemplateTo(confDir, "catalina.properties");
         copyFileTo(confDir, "context.xml");
         copyFileTo(confDir, "logging.properties");
         copyFileTo(confDir, "openejb.xml");
         copyFileTo(confDir, "server.xml");
         copyFileTo(confDir, "tomcat-users.xml");
         copyFileTo(confDir, "web.xml");
     }
 
     private void copyTemplateTo(File targetDir, String filename) throws Exception {
         Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new Log4JLogChute());
         Velocity.setProperty(Velocity.RESOURCE_LOADER, "class");
         Velocity.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader");
         Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
         Velocity.init();
         Template template = Velocity.getTemplate("/org/apache/openejb/tomee/configs/" + filename);
         VelocityContext context = new VelocityContext();
         context.put("tomcatHttpPort", Integer.toString(configuration.getHttpPort()));
         context.put("tomcatShutdownPort", Integer.toString(configuration.getStopPort()));
         Writer writer = new FileWriter(new File(targetDir, filename));
         template.merge(context, writer);
         writer.flush();
         writer.close();
     }
 
     private void copyFileTo(File targetDir, String filename) throws IOException {
         InputStream is = getClass().getResourceAsStream("/org/apache/openejb/tomee/configs/" + filename);
         FileOutputStream os = new FileOutputStream(new File(targetDir, filename));
 
         copyStream(is, os);
     }
 
     private void copyStream(InputStream is, FileOutputStream os) throws IOException {
         byte[] buffer = new byte[8192];
         int bytesRead = -1;
 
         while ((bytesRead = is.read(buffer)) > -1) {
             os.write(buffer, 0, bytesRead);
         }
 
         is.close();
         os.close();
     }
 
     private void createTomcatDirectories(File directory) {
         createDirectory(directory, "apps");
         createDirectory(directory, "conf");
         createDirectory(directory, "lib");
         createDirectory(directory, "logs");
         createDirectory(directory, "webapps");
         createDirectory(directory, "temp");
         createDirectory(directory, "work");
     }
 
     private void createDirectory(File parent, String directory) {
         new File(parent, directory).mkdirs();
     }
 }
