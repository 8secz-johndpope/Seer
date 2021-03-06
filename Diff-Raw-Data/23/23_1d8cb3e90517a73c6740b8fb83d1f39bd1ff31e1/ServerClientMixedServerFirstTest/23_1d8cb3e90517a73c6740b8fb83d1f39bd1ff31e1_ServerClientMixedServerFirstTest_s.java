 /*
  *
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  *
  * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
  *
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
  * or jersey/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  *
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at jersey/legal/LICENSE.txt.
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
  */
 
 package com.sun.jerseyosgird;
 
 import com.sun.grizzly.http.embed.GrizzlyWebServer;
 import com.sun.grizzly.http.servlet.ServletAdapter;
 import com.sun.jersey.api.client.Client;
 import com.sun.jersey.api.client.WebResource;
 import com.sun.jersey.api.core.ClassNamesResourceConfig;
 import com.sun.jersey.osgi.rdtestbundle.SimpleResource;
 import com.sun.jersey.spi.container.servlet.ServletContainer;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URL;
 
 import javax.ws.rs.core.UriBuilder;
 
 import java.net.URI;
 import java.net.URLConnection;
 import static org.ops4j.pax.exam.CoreOptions.options;
 import static org.ops4j.pax.exam.CoreOptions.systemProperty;
 import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
 import static org.ops4j.pax.exam.CoreOptions.felix;
 
 
 import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;
 
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.ops4j.pax.exam.Inject;
 import org.ops4j.pax.exam.Option;
 import org.ops4j.pax.exam.junit.Configuration;
 import org.ops4j.pax.exam.junit.JUnit4TestRunner;
 import org.osgi.framework.BundleContext;
 
 import static org.junit.Assert.assertEquals;
 
 
 @RunWith(JUnit4TestRunner.class)
 public class ServerClientMixedServerFirstTest {
 
     private static final int port = getEnvVariable("JERSEY_HTTP_PORT", 8080);
     private static final String CONTEXT = "/jersey";
     private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT).build();
 
     @Inject
     protected BundleContext bundleContext;
 
     @Test
     public void testSimpleResource() throws Exception {
 
         GrizzlyWebServer gws = startGrizzlyWebServer();
 
         final URL jerseyResourceURL = UriBuilder.fromUri(baseUri).path("simple").build().toURL();
         final URLConnection conn = jerseyResourceURL.openConnection();
 
         String response = getResponseFromOpenedConnection(conn);
 
         System.out.println("1st RESPONSE = " + response);
         assertEquals("jersey resource", response);
 
         WebResource r = createResource().path("/simple");
         response = r.get(String.class);
 
         System.out.println("2st RESPONSE = " + response);
         assertEquals("jersey resource", response);
 
         gws.stop();
     }
 
 
     private GrizzlyWebServer startGrizzlyWebServer() throws IOException {
         GrizzlyWebServer gws = new GrizzlyWebServer(port);
 
         ServletAdapter jerseyAdapter = new ServletAdapter();
 
         jerseyAdapter.addInitParameter("com.sun.jersey.config.property.classnames", SimpleResource.class.getName());
         jerseyAdapter.addInitParameter("com.sun.jersey.config.property.resourceConfigClass", ClassNamesResourceConfig.class.getName());
         jerseyAdapter.setContextPath("/jersey");
         jerseyAdapter.setServletInstance(new ServletContainer());
 
         gws.addGrizzlyAdapter(jerseyAdapter, new String[]{"/jersey"});
 
         gws.start();
 
         return gws;
     }
 
     public WebResource createResource() {
         final Client c = Client.create();
         final WebResource rootResource = c.resource(baseUri);
         return rootResource;
     }
 
 
     @Configuration
     public static Option[] configuration() {
 
         Option[] options = options(
         		
                 systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),//"DEBUG"),
                 systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                 
                 // define maven repository
                 repositories(
                         "http://repo1.maven.org/maven2", 
                         "http://repository.apache.org/content/groups/snapshots-group",
                         "http://repository.ops4j.org/maven2",
                         "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                         "http://repository.springsource.com/maven/bundles/release",
                         "http://repository.springsource.com/maven/bundles/external"
                    ),
 
                 // log
                 mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
                 mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),
 
                 //mvn: url handler
                 mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.1.2"),
 
                 // felix config admin
                 mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.4"),
                 
                 // felix preference service
                 mavenBundle("org.apache.felix", "org.apache.felix.prefs","1.0.2"),
                 
                 // blueprint
                 mavenBundle("org.apache.geronimo.blueprint", "geronimo-blueprint", "1.0.0"),
                 
                 // bundles
                 mavenBundle("org.apache.mina", "mina-core", "2.0.0-RC1"),
                 mavenBundle("org.apache.sshd", "sshd-core", "0.3.0"),
                 
                 // HTTP SPEC
                 mavenBundle("org.apache.geronimo.specs","geronimo-servlet_2.5_spec","1.1.2"),
                  
                 
                 // load PAX url war
                 mavenBundle("org.ops4j.pax.url","pax-url-war","1.1.2"),
                 
                 mavenBundle("javax.ws.rs","jsr311-api","1.1.1"),
                 mavenBundle("com.sun.jersey.osgi","jersey-core","1.2-SNAPSHOT"),
         	mavenBundle("com.sun.jersey.osgi","jersey-server", "1.2-SNAPSHOT"),
         	mavenBundle("com.sun.jersey.osgi","jersey-client", "1.2-SNAPSHOT"),
        	mavenBundle("com.sun.jersey.osgi","runtime-delegate-test-bundle", "1.2-SNAPSHOT"),
 
                 mavenBundle("com.sun.grizzly", "grizzly-servlet-webserver", "1.9.18-i"),
 
                 // start felix framework
                 felix());
 
         return options;
     }
 
     public static int getEnvVariable(final String varName, int defaultValue) {
         if (null == varName) {
             return defaultValue;
         }
         String varValue = System.getenv(varName);
         if (null != varValue) {
             try {
                 return Integer.parseInt(varValue);
             }catch (NumberFormatException e) {
                 // will return default value bellow
             }
         }
         return defaultValue;
     }
 
     
     private String getResponseFromOpenedConnection(URLConnection connection) throws IOException {
 
         final InputStream is = connection.getInputStream();
         final byte[] buf = new byte[4096];
         final ByteArrayOutputStream result = new ByteArrayOutputStream();
 
         try {
             for (int bytesRead = is.read(buf); bytesRead >= 0 ; bytesRead = is.read(buf)) {
                 result.write(buf, 0, bytesRead);
             }
             return result.toString();
         } finally {
             is.close();
         }
     }
 }
