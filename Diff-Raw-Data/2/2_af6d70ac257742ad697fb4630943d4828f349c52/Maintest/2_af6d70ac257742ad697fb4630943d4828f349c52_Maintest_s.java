 /* 
  *  mod_cluster
  *
  *  Copyright(c) 2008 Red Hat Middleware, LLC,
  *  and individual contributors as indicated by the @authors tag.
  *  See the copyright.txt in the distribution for a
  *  full listing of individual contributors.
  *
  *  This library is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public
  *  License as published by the Free Software Foundation; either
  *  version 2 of the License, or (at your option) any later version.
  *
  *  This library is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *  Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public
  *  License along with this library in the file COPYING.LIB;
  *  if not, write to the Free Software Foundation, Inc.,
  *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
  *
  * @author Jean-Frederic Clere
  * @version $Revision$
  */
 
 package org.jboss.mod_cluster;
 
 import junit.framework.Test;
 import junit.framework.TestCase;
 import junit.framework.TestSuite;
 import junit.textui.TestRunner;
 
 import java.lang.Exception;
 import java.net.Socket;
 
 import org.apache.catalina.ServerFactory;
 import org.apache.catalina.Service;
 import org.apache.catalina.Engine;
 import org.apache.catalina.core.StandardServer;
 import org.apache.catalina.connector.Connector;
 
 import org.apache.catalina.LifecycleListener;
 
 import org.jboss.web.cluster.ClusterListener;
 
 public class Maintest extends TestCase {
 
     static StandardServer server = null;
     static boolean isJBossWEB = true;
     public static void main( String args[] ) {
        TestRunner.run(suite());
     }
     public static Test suite() {
        TestSuite suite = new TestSuite();
        server = (StandardServer) ServerFactory.getServer();
        
        // Read the -Dcluster=true/false.
        String jbossweb = System.getProperty("cluster");
        if (jbossweb != null && jbossweb.equalsIgnoreCase("false")) {
             System.out.println("Running tests with jbossweb listener");
     	    isJBossWEB = true;
        } else {
             System.out.println("Running tests with mod_cluster listener");
     	    isJBossWEB = false;
        }
 
        // Read the -Dtest="value".
        String test = System.getProperty("test");
        if (test != null) {
             System.out.println("Running single test: " + test);
             try {
                 Class clazz = Class.forName("org.jboss.mod_cluster." + test);
                 suite.addTest(new TestSuite(clazz));
             } catch (ClassNotFoundException ex) {
                 System.out.println("Running single test: " + test + " Not found");
                 return null;
             }
        } else {
             suite.addTest(new TestSuite(TestAddDel.class));
             System.gc();
             suite.addTest(new TestSuite(TestBase.class));
             System.gc();
             suite.addTest(new TestSuite(TestFailover.class));
             System.gc();
             suite.addTest(new TestSuite(TestStickyForce.class));
             System.gc();
             suite.addTest(new TestSuite(TestFailAppover.class));
             System.gc();
             suite.addTest(new TestSuite(Testmod_cluster_manager.class));
             System.gc();
             suite.addTest(new TestSuite(TestPing.class));
             System.gc();
             /* XXX The JBWEB_117 tests are not really related to mod_cluster
              * Run them one by one using ant one -Dtest=test
             suite.addTest(new TestSuite(TestJBWEB_117.class));
             System.gc();
             suite.addTest(new TestSuite(Test_Native_JBWEB_117.class));
             System.gc();
             suite.addTest(new TestSuite(Test_Chunk_JBWEB_117.class));
             System.gc();
             */
        }
        return suite;
     }
     static StandardServer getServer() {
         if (server == null) {
             server = (StandardServer) ServerFactory.getServer();
             // Read the -Dcluster=true/false.
             String jbossweb = System.getProperty("cluster");
             if (jbossweb != null && jbossweb.equalsIgnoreCase("false")) {
                  System.out.println("Running tests with jbossweb listener");
     	         isJBossWEB = true;
             } else {
                  System.out.println("Running tests with mod_cluster listener");
     	         isJBossWEB = false;
             }
 
         }
         return server;
     }
     static boolean isJBossWEB() {
     	return isJBossWEB;
     }
 
     /* Print the service and connectors the server knows */
     static void listServices() {
             Service[] services = server.findServices();
             for (int i = 0; i < services.length; i++) {
                 System.out.println("service[" + i + "]: " + services[i]);
                 Engine engine = (Engine) services[i].getContainer();
                 System.out.println("engine: " + engine);
                 System.out.println("connectors: " + services[i].findConnectors());
                 Connector [] connectors = services[i].findConnectors();
                 for (int j = 0; j < connectors.length; j++) {
                     System.out.println("connector: " + connectors[j]);
                 }
             }
     }
     static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl) {
     	return createClusterListener(groupa, groupp, ssl, null);
     }
     static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl, String domain) {
     	return createClusterListener(groupa, groupp, ssl, domain, true, false, true);
     }
     static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl, String domain,
                                                    boolean stickySession, boolean stickySessionRemove,
                                                    boolean stickySessionForce) {
         LifecycleListener lifecycle = null;
         ClusterListener jcluster = null;
         org.jboss.modcluster.ModClusterListener pcluster = null;
 
         if (isJBossWEB) {
             jcluster = new ClusterListener();
             jcluster.setAdvertiseGroupAddress(groupa);
             jcluster.setAdvertisePort(groupp);
             jcluster.setSsl(ssl);
             jcluster.setDomain(domain);
             jcluster.setStickySession(stickySession);
             jcluster.setStickySessionRemove(stickySessionRemove);
             jcluster.setStickySessionForce(stickySessionForce);
             lifecycle = jcluster;
         } else {
             pcluster = new org.jboss.modcluster.ModClusterListener();
             pcluster.setAdvertiseGroupAddress(groupa);
             pcluster.setAdvertisePort(groupp);
             pcluster.setSsl(ssl);
             pcluster.setDomain(domain);
             pcluster.setStickySession(stickySession);
             pcluster.setStickySessionRemove(stickySessionRemove);
             pcluster.setStickySessionForce(stickySessionForce);
             lifecycle = pcluster;
 
         }
 
         return lifecycle;
     }
     static String doProxyPing(LifecycleListener lifecycle, String JvmRoute) {
         String result = null;
         if (isJBossWEB) {
             ClusterListener jcluster = (ClusterListener) lifecycle;
             result = jcluster.doProxyPing(JvmRoute);
         } else {
             org.jboss.modcluster.ModClusterListener pcluster = (org.jboss.modcluster.ModClusterListener) lifecycle;
             result = pcluster.doProxyPing(JvmRoute);
         }
         return result;
     }
     /* Analyse the PING-RSP message: Type=PING-RSP&State=OK&id=1 */
     static boolean checkProxyPing(String result) {
         String [] records = result.split("\n");
         if (records.length != 3)
             return false;
         String [] results = records[1].split("&");
         int ret = 0;
         for (int j=0; j<results.length; j++) {
             String [] data = results[j].split("=");
             if (data[0].compareToIgnoreCase("Type") == 0 &&
                 data[1].compareToIgnoreCase("PING-RSP") == 0)
                 ret++;
             if (data[0].compareToIgnoreCase("State") == 0 &&
                 data[1].compareToIgnoreCase("OK") == 0)
                 ret++;
         }
         if (ret == 2)
            return true;
 
         return false;
     }
     static String getProxyInfo(LifecycleListener lifecycle) {
         String result = null;
         if (isJBossWEB) {
             ClusterListener jcluster = (ClusterListener) lifecycle;
             result = jcluster.getProxyInfo();
         } else {
             org.jboss.modcluster.ModClusterListener pcluster = (org.jboss.modcluster.ModClusterListener) lifecycle;
             result = pcluster.getProxyInfo();
         }
         return result;
     }
     /* Check that the nodes are returned by the INFO command */
     static boolean checkProxyInfo(LifecycleListener lifecycle, String [] nodes) {
         String result = getProxyInfo(lifecycle);
         return checkProxyInfo(result, nodes);
     }
     static boolean checkProxyInfo(String result, String [] nodes) {
         if (result == null) {
             if (nodes == null)
                 return true;
             else
                 return false;
         }
         /* create array to check the nodes */
         boolean [] n = null;
         if (nodes != null && nodes.length>0) {
             n = new boolean[nodes.length];
             for (int i=0; i<nodes.length; i++) {
                 n[i] = false;
             }
         }
 
         String [] records = result.split("\n");
         int l = 0;
         for (int i=0; i<records.length; i++) {
             String [] results = records[i].split(",");
             /* result[0] should be Node: [n] */
             String [] data = results[0].split(": ");
             if ("Node".equals(data[0])) {
                 if (n == null)
                     return false; /* we shouldn't have a node */
                 /* result[1] should be Name: node_name */
                 data = results[1].split(": ");
                 for (int j=0; j<nodes.length; j++) {
                     if (nodes[j].equals(data[1])) {
                         n[j] = true; /* found it */
                     }
                 }
             }
         }
         if (n == null)
             return true; /* done */
         for (int j=0; j<nodes.length; j++) {
             if (! n[j])
                 return false; /* not found */
         }
         return true;
     }
     public static boolean testPort(int port) {
         boolean ret = true;
         Socket s = null;
         try {
             s =  new Socket("localhost", port);
             s.setSoLinger(true, 0);
         } catch (Exception e) {
             System.out.println("Can't connect to " + port);
             ret = false;
         } finally {
             if (s != null) {
                 System.out.println("Was connected to " + port);
                 try {
                     s.close();
                 } catch (Exception e) {
                 }
             }
         }
         return ret;
     }
     static  boolean TestForNodes(LifecycleListener lifecycle, String [] nodes) {
         int countinfo = 0;
         while ((!Maintest.checkProxyInfo(lifecycle, nodes)) && countinfo < 20) {
             try {
                 Thread.sleep(3000);
             } catch (InterruptedException ex) {
                 ex.printStackTrace();
             }
             countinfo++;
         }
         if (countinfo == 20)
             return false;
         else
             return true;
     }
 
     // Wait until we are able to PING httpd.
     // tries maxtries and wait 5 s between retries...
     static int WaitForHttpd(LifecycleListener cluster, int maxtries) {
         String result = null;
         int tries = 0;
         while (result == null && tries<maxtries) {
             result = Maintest.doProxyPing(cluster, null);
             if (result != null) {
                 if (Maintest.checkProxyPing(result))
                     break; // Done
                 return -1; // Failed.
             }
             try {
                 Thread me = Thread.currentThread();
                 me.sleep(5000);
                 tries++;
             } catch (Exception ex) {
             }
         }
         return tries;
     }
 }
