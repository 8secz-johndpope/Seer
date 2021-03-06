 /*
  * Copyright 2000-2011 JetBrains s.r.o.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package jetbrains.buildServer.nuget.tests;
 
 import jetbrains.buildServer.BaseTestCase;
 import jetbrains.buildServer.nuget.server.install.PackagesInstallerRunType;
 import jetbrains.buildServer.web.openapi.PluginDescriptor;
 import org.jmock.Mockery;
 import org.testng.Assert;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.Test;
 
 /**
  * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
  * Date: 21.07.11 19:03
  */
 public class RunTypeNameTest extends BaseTestCase {
   private Mockery m;
   private PluginDescriptor descr;
 
   @BeforeMethod
   @Override
   protected void setUp() throws Exception {
     super.setUp();
     m = new Mockery();
     descr = m.mock(PluginDescriptor.class);
   }
 
   @Test
   public void test_installPackagesRunTypeIdLendth() {
     final String type = new PackagesInstallerRunType(descr).getType();
     Assert.assertTrue(type.length() < 30);
   }
 }
