 package org.openqa.selenium.firefox.internal;
 
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertNotNull;
 
 import org.junit.Test;
import org.junit.runner.RunWith;
 import org.openqa.selenium.testing.NeedsLocalEnvironment;
import org.openqa.selenium.testing.SeleniumTestRunner;
 import org.openqa.selenium.testing.drivers.SauceDriver;
 
 import java.io.File;
 
@RunWith(SeleniumTestRunner.class)
 public class ExecutableTest {
 
   @Test
   @NeedsLocalEnvironment(reason = "Requires local browser launching environment")
   public void testEnvironmentDiscovery() {
     Executable env = new Executable(null);
     File exe = env.getFile();
     assertNotNull(exe);
     assertFalse(exe.isDirectory());
   }
 }
