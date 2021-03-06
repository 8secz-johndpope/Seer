 /*
  * ################################################################
  *
  * ProActive: The Java(TM) library for Parallel, Distributed,
  *            Concurrent computing with Security and Mobility
  *
  * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
  * Contact: proactive@objectweb.org
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
  * USA
  *
  *  Initial developer(s):               The ProActive Team
  *                        http://www.inria.fr/oasis/ProActive/contacts.html
  *  Contributor(s):
  *
  * ################################################################
  */
 package functionalTests.timit.timers.basic;
 
 import org.junit.After;
 import org.objectweb.proactive.ProActive;
 import org.objectweb.proactive.benchmarks.timit.util.basic.TimItBasicReductor;
 import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
 import org.objectweb.proactive.core.descriptor.data.VirtualNode;
 import org.objectweb.proactive.core.node.Node;
 
 import functionalTests.FunctionalTest;
 import static junit.framework.Assert.assertTrue;
 public class Test extends FunctionalTest {
     private ActiveObjectClass a1;
     private ActiveObjectClass a1bis;
     private ActiveObjectClass a2;
     private ProActiveDescriptor descriptorPad;
     private TimItBasicReductor t;
 
     public void initTest() throws Exception {
         // Access the nodes of the descriptor file
         descriptorPad = ProActive.getProactiveDescriptor(this.getClass()
                                                              .getResource("/functionalTests/timit/timers/basic/descriptor.xml")
                                                              .getPath());
         descriptorPad.activateMappings();
         VirtualNode vnode = descriptorPad.getVirtualNodes()[0];
         Node[] nodes = vnode.getNodes();
 
         this.a1 = (ActiveObjectClass) ProActive.newActive(ActiveObjectClass.class.getName(),
                 new Object[] { "a1" }, nodes[0]);
         this.a1bis = (ActiveObjectClass) ProActive.newActive(ActiveObjectClass.class.getName(),
                 new Object[] { "a1bis" }, nodes[1]);
         // Provide the remote reference of a1 and a1bis to a2
         this.a2 = (ActiveObjectClass) ProActive.newActive(ActiveObjectClass.class.getName(),
                 new Object[] { this.a1, this.a1bis, "a2" }, nodes[1]);
     }
 
     public boolean preConditions() throws Exception {
         return ((this.a1 != null) && (this.a1bis != null)) &&
         ((this.a2 != null) && this.a2.checkRemoteAndLocalReference());
     }
 
     @org.junit.Test
     public void action() throws Exception {
         // Create active objects
         this.initTest();
         // Check their creation
         assertTrue("Problem with the creation of active objects for this test !",
             this.preConditions());
         // Check if the Total timer is started
         String result = this.a2.checkIfTotalIsStarted();
         assertTrue(result, "true".equals(result));
 
        // Check if the WaitForRequest timer is stopped during a service of a request
         result = this.a2.checkIfWfrIsStopped();
         assertTrue(result, "true".equals(result));
 
         // Check if the Serve timer is started during a service of a request
         result = this.a2.checkIfServeIsStarted();
         assertTrue(result, "true".equals(result));
 
         // For the next requests a2 is going to use timers
        // SendRequest, BeforeSerialization, Serialization and AfterSerialization timers must be used 
         result = this.a2.performSyncCallOnRemote();
         assertTrue(result, "true".equals(result));
 
        // SendRequest and LocalCopy timers must be used 
         result = this.a2.performSyncCallOnLocal();
         assertTrue(result, "true".equals(result));
 
         // SendRequest and WaitByNecessity timers must be used
         result = this.a2.performAsyncCallWithWbnOnLocal();
         assertTrue(result, "true".equals(result));
 
         // disable the result output
        //this.t = TimItBasicManager.getInstance().getTimItCommonReductor();
        //t.setGenerateOutputFile(false);
        //t.setPrintOutput(false);            
     }
 
     @After
     public void endTest() throws Exception {
         this.descriptorPad.killall(true);
         Thread.sleep(300);
         this.a1 = null;
         this.a1bis = null;
         this.a2 = null;
         this.descriptorPad = null;
         this.t = null;
     }
 
     public static void main(String[] args) {
         Test test = new Test();
         try {
             test.action();
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             try {
                 System.exit(0);
             } catch (Exception e1) {
                 e1.printStackTrace();
             }
         }
     }
 }
