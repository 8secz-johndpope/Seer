 /**
  * Copyright (C) 2010 Daniel Manzke <daniel.manzke@googlemail.com>
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package de.devsurf.injection.guice.integrations.test.rocoto.file;
 
 import static org.junit.Assert.assertNotNull;
 import junit.framework.Assert;
 
 import org.junit.Test;
 
 import com.google.inject.Guice;
 import com.google.inject.Inject;
 import com.google.inject.Injector;
 import com.google.inject.name.Named;
 
 import de.devsurf.injection.guice.DynamicModule;
 import de.devsurf.injection.guice.configuration.Configuration.PathType;
 import de.devsurf.injection.guice.integrations.rocoto.RocotoListener;
 import de.devsurf.injection.guice.scanner.StartupModule;
 import de.devsurf.injection.guice.scanner.annotations.AutoBind;
 import de.devsurf.injection.guice.scanner.asm.VirtualClasspathReader;
 
 public class FileConfigTests {
     @Test
     public void createDynamicModule() {
 	StartupModule startup = StartupModule.create(VirtualClasspathReader.class,
 	    FileConfigTests.class.getPackage().getName(), RocotoListener.class.getPackage().getName());
 	startup.addFeature(RocotoListener.class);
 
 	Injector injector = Guice.createInjector(startup);
 	assertNotNull(injector);
 
 	DynamicModule dynamicModule = injector.getInstance(DynamicModule.class);
 	assertNotNull(dynamicModule);
 
 	injector = Guice.createInjector(dynamicModule);
 	assertNotNull(injector);
     }
 
     @Test
     public void createPListConfiguration() {
 	StartupModule startup = StartupModule.create(VirtualClasspathReader.class,
 	    FileConfigTests.class.getPackage().getName(), RocotoListener.class.getPackage().getName());
 	startup.addFeature(RocotoListener.class);
 
 	Injector injector = Guice.createInjector(startup);
 	assertNotNull(injector);
 
 	DynamicModule dynamicModule = injector.getInstance(DynamicModule.class);
 	assertNotNull(dynamicModule);
 
 	injector = Guice.createInjector(dynamicModule);
 	assertNotNull(injector);
 
 	TestInterface instance = injector.getInstance(TestInterface.class);
 	Assert.assertTrue("sayHello() - yeahh!!".equals(instance.sayHello()));
     }
 
    @de.devsurf.injection.guice.configuration.Configuration(name = "config", path = "./src/test/resources/configuration.properties", pathType = PathType.FILE)
     public interface TestConfiguration {
     }
 
     public static interface TestInterface {
 	String sayHello();
     }
 
     @AutoBind
     public static class TestImplementations implements TestInterface {
 	@Inject
 	@Named("message")
 	private String message;
 
 	@Override
 	public String sayHello() {
 	    return "sayHello() - " + message;
 	}
     }
 }
