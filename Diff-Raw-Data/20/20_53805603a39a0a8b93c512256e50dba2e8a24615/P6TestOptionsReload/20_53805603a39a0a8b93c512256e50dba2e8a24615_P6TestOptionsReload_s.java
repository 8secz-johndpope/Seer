 /*
 Copyright 2013 P6Spy
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
 package com.p6spy.engine.spy.option;
 
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
 
 import java.io.File;
 import java.io.IOException;
 import java.sql.SQLException;
 
 import javax.management.JMException;
 
 import org.apache.commons.io.FileUtils;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import com.j256.simplejmx.client.JmxClient;
 import com.p6spy.engine.common.P6Util;
 import com.p6spy.engine.spy.P6SpyOptions;
 import com.p6spy.engine.spy.P6TestFramework;
 import com.p6spy.engine.spy.P6TestMBean;
 import com.p6spy.engine.spy.option.SpyDotProperties;
 
 public class P6TestOptionsReload {
 
   private static JmxClient jmxClient = null;
 
   @BeforeClass
   public static void connectToJMX() throws JMException, SQLException, IOException,
       InterruptedException {
     // make sure to reinit properly
     new P6TestFramework("reload") {
     };
 
     String jmxPortProperty = System.getProperty(P6TestMBean.COM_SUN_MANAGEMENT_JMXREMOTE_PORT);
     int jmxPort = P6Util.parseInt(jmxPortProperty, P6TestMBean.JMXREMOTE_PORT_DEFAULT);
     jmxClient = new JmxClient(jmxPort);
   }
 
   /**
    * Please note, when modifying this one to check
    * {@link P6TestOptionsReload#testSetPropertyDiscartedOnExplicitReload()} as well.
    * 
    * @throws Exception
    */
   @Test
   public void testJmxSetPropertyDiscartedOnExplicitJmxReload() throws Exception {
     final String domainName = P6SpyOptions.class.getPackage().getName();
     final String beanName = P6SpyOptions.class.getSimpleName();
     final String attributeName = "StackTrace";
 
     // precondition
     assertFalse((Boolean) jmxClient.getAttribute(domainName, beanName, attributeName));
 
     // jmx value modification
     jmxClient.setAttribute(domainName, beanName, attributeName, true);
     assertTrue((Boolean) jmxClient.getAttribute(domainName, beanName, attributeName));
 
     // props reload
     jmxClient.invokeOperation(P6SpyOptions.class.getPackage().getName(),
         P6SpyOptions.class.getSimpleName(), "reload");
 
     // jmx value modification discarted
     assertFalse((Boolean) jmxClient.getAttribute(domainName, beanName, attributeName));
   }
 
   /**
    * Please note, when modifying this one to check
    * {@link P6TestOptionsReload#testJmxSetPropertyDiscartedOnExplicitJmxReload()} as well.
    * 
    * @throws Exception
    */
   @Test
   public void testSetPropertyDiscartedOnExplicitReload() throws Exception {
     // precondition
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // value modification
     P6SpyOptions.getActiveInstance().setStackTrace(true);
     assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // props reload
     P6SpyOptions.getActiveInstance().reload();
 
     // jmx value modification discarted
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
   }
 
   @Test
   public void testSetPropertyDiscartedOnAutoReload() throws Exception {
     // precondition
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // value modification
     P6SpyOptions.getActiveInstance().setStackTrace(true);
     assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // no explicit props reload, just modify timestamp and wait till autoreload happens
     FileUtils.touch(new File(System.getProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY)));
     Thread.sleep(2000);
 
     // jmx value modification discarted
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
   }
 
   @Test
   public void testAutoReloadLifecycle() throws Exception {
     // precondition
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // value modification
     P6SpyOptions.getActiveInstance().setStackTrace(true);
     assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // disable auto reload
     P6SpyOptions.getActiveInstance().setReloadProperties(false);
     FileUtils.touch(new File(System.getProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY)));
     Thread.sleep(2000);
 
     // reload didn't happen
     assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
     // enable auto reload
     P6SpyOptions.getActiveInstance().setReloadProperties(true);
     FileUtils.touch(new File(System.getProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY)));
     Thread.sleep(2000);
 
     // reload did happen
     assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
   }
 
   @Test
   public void testOptionSourcesPriorities() throws Exception {
     // [default] stacktrace=false
     // [SpyDotProperties] #stacktrace=true
     // => false (+ survives across reloads)
     {
       File p6TestProperties = new File(P6TestFramework.TEST_FILE_PATH, "P6Test_reload.properties");
       System
           .setProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY, p6TestProperties.getAbsolutePath());
       P6SpyOptions.getActiveInstance().reload();
 
       assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
 
      System
          .setProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY, p6TestProperties.getAbsolutePath());
       P6SpyOptions.getActiveInstance().reload();
 
       assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
     }
 
     // [default] stacktrace=false
     // [SpyDotProperties] stacktrace=true
     // => true (+ survives across reloads)
     {
       File p6TestProperties = new File(P6TestFramework.TEST_FILE_PATH, "P6Test_reload_2.properties");
       System
           .setProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY, p6TestProperties.getAbsolutePath());
       P6SpyOptions.getActiveInstance().reload();
 
       assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
       P6SpyOptions.getActiveInstance().reload();
 
       assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
     }
 
     // [default] stacktrace=false
     // [SpyDotProperties] stacktrace=true
     // [SystemProperties] stacktrace=false
     // => false (+ survives across reloads)
     {
       System.setProperty(SystemProperties.P6SPY_PREFIX + P6SpyOptions.STACKTRACE,
           Boolean.toString(false));
       P6SpyOptions.getActiveInstance().reload();
 
       assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
 
       P6SpyOptions.getActiveInstance().reload();
 
       assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
     }
 
     // [default] stacktrace=false
     // [SpyDotProperties] stacktrace=true
     // [SystemProperties] stacktrace=false
     // [JMXSetProperties] stacktrace=true
     // => true (+ but jmx doesn't survives across reloads)
     {
       File p6TestProperties = new File(P6TestFramework.TEST_FILE_PATH, "P6Test_reload_2.properties");
       System
           .setProperty(SpyDotProperties.OPTIONS_FILE_PROPERTY, p6TestProperties.getAbsolutePath());
       
       final String domainName = P6SpyOptions.class.getPackage().getName();
       final String beanName = P6SpyOptions.class.getSimpleName();
       final String attributeName = "StackTrace";
       
       // jmx value modification
       jmxClient.setAttribute(domainName, beanName, attributeName, true);
 
       assertTrue(P6SpyOptions.getActiveInstance().getStackTrace());
 
       P6SpyOptions.getActiveInstance().reload();
 
       assertFalse(P6SpyOptions.getActiveInstance().getStackTrace());
     }
   }
 }
