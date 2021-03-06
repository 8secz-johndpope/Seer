 /*
  * Copyright 2009-2011 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package griffon.core;
 
 import groovy.lang.Binding;
 import groovy.lang.Closure;
 import groovy.util.ConfigObject;
 import groovy.util.FactoryBuilderSupport;
 import griffon.util.Metadata;
 import griffon.util.GriffonNameUtils;
 import griffon.util.RunnableWithArgs;
 
 import java.util.Map;
 import java.util.List;
 import java.util.Locale;
 
 import java.util.concurrent.Callable;
 import java.util.concurrent.Future;
 import java.util.concurrent.ExecutorService;
 
 import org.slf4j.Logger;
 
 /**
  * Defines the basic contract of a Griffon application.<p>
  *
  * @author Danno Ferrin
  * @author Andres Almiray
  */
 public interface GriffonApplication {
     /**
      * Defines the names of the configuration scripts.
      *
      * @author Andres Almiray
      * @since 0.9.2
      */
     public enum Configuration {
         APPLICATION, CONFIG, BUILDER, EVENTS;
 
         /** Display friendly name */
         private String name;
 
         /**
          * Returns the capitalized String representation of this Configuration object.
          *
          * @return a capitalized String
          */
         public String getName() {
             if(name == null) {
                 return GriffonNameUtils.capitalize(this.toString().toLowerCase(Locale.getDefault()));
             }
             return name;
         }
     }
 
     /**
      * Defines the names of the lifecycle scripts.
      *
      * @author Andres Almiray
      * @since 0.9.2
      */
     public enum Lifecycle {
         INITIALIZE, STARTUP, READY, SHUTDOWN, STOP;
 
         /** Display friendly name */
         private String name;
 
         /**
          * Returns the capitalized String representation of this Lifecycle object.
          *
          * @return a capitalized String
          */
         public String getName() {
             if(name == null) {
                 return GriffonNameUtils.capitalize(this.toString().toLowerCase(Locale.getDefault()));
             }
             return name;
         }
     }
 
     /**
      * Defines all the events triggered by the application.
      *
      * @author Andres Almiray
      * @since 0.9.2
      */
     public enum Event {
         LOG4J_CONFIG_START("Log4jConfigStart"), UNCAUGHT_EXCEPTION_THROWN,
         LOAD_ADDONS_START, LOAD_ADDONS_END, LOAD_ADDON_START, LOAD_ADDON_END,
         BOOTSTRAP_START, BOOTSTRAP_END,
         STARTUP_START, STARTUP_END,
         READY_START, READY_END,
         SHUTDOWN_REQUESTED, SHUTDOWN_ABORTED, SHUTDOWN_START,
         NEW_INSTANCE,
         CREATE_MVC_GROUP("CreateMVCGroup"), DESTROY_MVC_GROUP("DestroyMVCGroup"),
         WINDOW_SHOWN, WINDOW_HIDDEN;
 
         /** Display friendly name */
         private String name;
 
         Event() {
             String name = name().toLowerCase().replaceAll("_","-");
             this.name = GriffonNameUtils.getClassNameForLowerCaseHyphenSeparatedName(name);
         }
 
         Event(String name) {
             this.name = name;
         }
 
         /**
          * Returns the capitalized String representation of this Event object.
          *
          * @return a capitalized String
          */
         public String getName() {
             return this.name;
         }
     }
 
     /**
      * Gets the application's configuration set on 'application.properties'.<p>
      */
     Metadata getMetadata();
 
     /**
      * Gets the script class that holds the MVC configuration (i.e. {@code Application.groovy})
      */
     Class getAppConfigClass();
     /**
      * Gets the script class that holds additional configuration (i.e. {@code Config.groovy})
      */
     Class getConfigClass();
     /**
      * Returns the merged runtime configuration from {@code appConfig} and {@code config}
      */
     ConfigObject getConfig();
     void setConfig(ConfigObject config);
 
     /**
      * Gets the script class that holds builder configuration (i.e. {@code Builder.groovy})
      */
     Class getBuilderClass();
     /**
      * Returns the runtime configuration required for instantiating a {@CompositeBuilder}
      */
     ConfigObject getBuilderConfig();
     void setBuilderConfig(ConfigObject builderConfig);
 
     /**
      * Gets the script class that holds global event handler configuration (i.e. {@code Events.groovy})
      */
     Class getEventsClass();
     /**
      * Returns the runtime configuration for global event handlers.
      */
     Object getEventsConfig();
     void setEventsConfig(Object eventsConfig);
 
     Binding getBindings();
     void setBindings(Binding bindings);
 
     /**
      * Return's the set of available MVC groups.
      */
     Map<String, Map<String, String>> getMvcGroups();
     /**
      * Register an MVC group for instantiation.<p>
      */
     void addMvcGroup(String mvcType, Map<String, String> mvcPortions);
 
     /**
      * Returns all currently available addon instances, keyed by addon name.<p>
      * @deprecated use getAddonManager().getAddons()
      */
     @Deprecated
     Map<String, ?> getAddons();
 
     /**
      * @deprecated without replacement. Use the AddonManager to query for addons
      */
     @Deprecated
     Map<String, String> getAddonPrefixes();
 
     /**
      * Returns the application's AddonManager instance.
      *
      * @return the application's AddonManager
      */
     AddonManager getAddonManager();
 
     /**
      * Returns all currently available model instances, keyed by group name.<p>
      */
     Map<String, ? extends GriffonModel> getModels();
 
     /**
      * Returns all currently available view instances, keyed by group name.<p>
      */
     Map<String, ? extends GriffonView> getViews();
 
     /**
      * Returns all currently available controller instances, keyed by group name.<p>
      */
     Map<String, ? extends GriffonController> getControllers();
 
     /**
      * Returns all currently available builder instances, keyed by group name.<p>
      */
     Map<String, ? extends FactoryBuilderSupport> getBuilders();
 
     /**
      * Returns all currently available groups, keyed by group name.<p>
      */
     Map<String, Map<String, Object>> getGroups();
 
     Object createApplicationContainer();
 
     /**
      * Executes the 'Initilaize' life cycle phase.
      */
     void initialize();
 
     /**
      * Executes the 'Startup' life cycle phase.
      */
     void startup();
 
     /**
      * Executes the 'Ready' life cycle phase.
      */
     void ready();
 
     /**
      * Executes the 'Shutdown' life cycle phase.
      *
      * @return false if the shutdown sequence was aborted
      */
     boolean shutdown();
 
     /**
      * Queries any available ShutdownHandlers.
      *
      * @return true if the shutdown sequence can proceed, false otherwise
      */
     boolean canShutdown();
 
     /**
      * Adds an application event listener.<p>
      * Accepted types are: Script, Map and Object.
      *
      * @param listener an application event listener
      */
     void addApplicationEventListener(Object listener);
 
     /**
      * Adds a closure as an application event listener.<p>
      *
      * @param eventName the name of the event
      * @param listener an application event listener
      */
     void addApplicationEventListener(String eventName, Closure listener);
 
     /**
      * Adds a runnable as an application event listener.<p>
      *
      * @param eventName the name of the event
      * @param listener an application event listener
      */
     void addApplicationEventListener(String eventName, RunnableWithArgs listener);
 
     /**
      * Removes an application event listener.<p>
      * Accepted types are: Script, Map and Object.
      *
      * @param listener an application event listener
      */
     void removeApplicationEventListener(Object listener );
 
     /**
      * Removes a closure as an application event listener.<p>
      *
      * @param eventName the name of the event
      * @param listener an application event listener
      */
     void removeApplicationEventListener(String eventName, Closure listener);
 
     /**
      * Removes a runnable as an application event listener.<p>
      *
      * @param eventName the name of the event
      * @param listener an application event listener
      */
     void removeApplicationEventListener(String eventName, RunnableWithArgs listener);
 
     /**
      * Publishes an application event.<p>
      *
      * @param eventName the name of the event
      */
     void event(String eventName);
 
     /**
      * Publishes an application event.<p>
      *
      * @param eventName the name of the event
      * @param params event arguments sent to listeners
      */
     void event(String eventName, List params);
 
     /**
      * Publishes an application event asynchronously off the UI thread.<p>
      *
      * @param eventName the name of the event
      */
     void eventOutside(String eventName);
 
     /**
      * Publishes an application event asynchronously off the UI thread.<p>
      *
      * @param eventName the name of the event
      * @param params event arguments sent to listeners
      */
     void eventOutside(String eventName, List params);
 
     /**
      * Publishes an application event asynchronously off the publisher's thread.<p>
      *
      * @param eventName the name of the event
      */
     void eventAsync(String eventName);
 
     /**
      * Publishes an application event asynchronously off the publisher's thread.<p>
      *
      * @param eventName the name of the event
      * @param params event arguments sent to listeners
      */
     void eventAsync(String eventName, List params);
 
     /**
      * Registers a ShutdownHandler on this application
      *
      * @param handler the shutdown handler to be registered; null and/or
      *                duplicated values should be ignored
      */
     void addShutdownHandler(ShutdownHandler handler);    
 
     /**
      * Removes a ShutdownHandler from this application
      *
      * @param handler the shutdown handler to be removed; null and/or
      *                duplicated values should be ignored
      */
     void removeShutdownHandler(ShutdownHandler handler);    
 
     /**
      * Gets the application locale.
      */    
     Locale getLocale();
 
     /**
      * Sets the application locale.<p>
      * This is a bound property.
      */
     void setLocale(Locale locale);
 
     /**
      * Returns the current phase.
      */
     ApplicationPhase getPhase();
 
     // ----------------------------
 
     /**
      * Returns the application's ArtifactManager instance.
      *
      * @return the application's ArtifactManager
      */
     ArtifactManager getArtifactManager();
 
     /**
      * True if the current thread is the UI thread.
      */
     boolean isUIThread();
 
     /**
      * Executes a code block asynchronously on the UI thread.
      */
     void execAsync(Runnable runnable);
 
     /**
      * Executes a code block synchronously on the UI thread.
      */
     void execSync(Runnable runnable);
 
     /**
      * Executes a code block outside of the UI thread.
      */
     void execOutside(Runnable runnable);
 
     /**
      * Executes a code block as a Future on an ExecutorService.
      */
     Future execFuture(ExecutorService executorService, Closure closure);
 
     /**
      * Executes a code block as a Future on a default ExecutorService.
      */
     Future execFuture(Closure closure);
 
     /**
      * Executes a code block as a Future on an ExecutorService.
      */
     Future execFuture(ExecutorService executorService, Callable callable);
 
     /**
      * Executes a code block as a Future on a default ExecutorService.
      */
     Future execFuture(Callable callable);
 
     /**
      * Creates a new instance of the specified class and type.
      */
     Object newInstance(Class clazz, String type);
 
     /**
      * Instantiates an MVC group of the specified type.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be created as follows
      *
      * <pre>
      * Map<String, Object> fooGroup = buildMVCGroup('foo')
      * assert (fooGroup.controller instanceof FooController)
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @return a Map with every member of the group keyed by type
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     Map<String, Object> buildMVCGroup(String mvcType);
 
     /**
      * Instantiates an MVC group of the specified type with a particular name.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be created as follows
      *
      * <pre>
      * Map<String, Object> fooGroup = buildMVCGroup('foo', 'foo' + System.currentTimeMillis())
      * assert (fooGroup.controller instanceof FooController)
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @return a Map with every member of the group keyed by type
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     Map<String, Object> buildMVCGroup(String mvcType, String mvcName);
 
     /**
      * Instantiates an MVC group of the specified type with additional variables.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      *     'bar' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.BarView'
      *         controller = 'com.acme.BarController'
      *     }
      * }
      * </pre>
      *
      * Notice that groups "foo" and "bar share the same model type, We can make them share the same model
      * instance by creating the groups in the following way:
      *
      * <pre>
      * Map<String, Object> fooGroup = buildMVCGroup('foo')
      * Map<String, Object> barGroup = buildMVCGroup('bar', model: fooGroup.model)
      * assert fooGroup.model == barGroup.model
      * </pre>
      *
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @param mvcType the type of group to build.
      * @return a Map with every member of the group keyed by type
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     Map<String, Object> buildMVCGroup(Map<String, Object> args, String mvcType);
 
     /**
      * Instantiates an MVC group of the specified type with a particular name.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * We can create two instances of the same group that share the same model instance in the following way:
      *
      * <pre>
      * Map<String, Object> fooGroup1 = buildMVCGroup('foo', 'foo1')
      * Map<String, Object> fooGroup2 = buildMVCGroup('bar', 'foo2', model: fooGroup1.model)
      * assert fooGroup1.model == fooGroup2.model
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @return a Map with every member of the group keyed by type
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     Map<String, Object> buildMVCGroup(Map<String, Object> args, String mvcType, String mvcName);
 
     /**
      * Instantiates an MVC group of the specified type returning only the MVC parts.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be created as follows
      *
      * <pre>
      * def (m, v, c) = createMVCGroup('foo')
      * assert (c instanceof FooController)
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(String mvcType);
 
     /**
      * Instantiates an MVC group of the specified type with additional variables.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      *     'bar' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.BarView'
      *         controller = 'com.acme.BarController'
      *     }
      * }
      * </pre>
      *
      * Notice that groups "foo" and "bar share the same model type, We can make them share the same model
      * instance by creating the groups in the following way:
      *
      * <pre>
      * def (m1, v1, c1) = createMVCGroup('foo')
      * def (m2, v2, c2) = createMVCGroup('bar', model: m1)
      * assert fm1 == m2
      * </pre>
      *
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @param mvcType the type of group to build.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(Map<String, Object> args, String mvcType);
 
     /**
      * Instantiates an MVC group of the specified type with additional variables.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      *     'bar' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.BarView'
      *         controller = 'com.acme.BarController'
      *     }
      * }
      * </pre>
      *
      * Notice that groups "foo" and "bar share the same model type, We can make them share the same model
      * instance by creating the groups in the following way:
      *
      * <pre>
      * def (m1, v1, c1) = createMVCGroup('foo')
      * def (m2, v2, c2) = createMVCGroup('bar', model: m1)
      * assert fm1 == m2
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(String mvcType, Map<String, Object> args);
 
     /**
      * Instantiates an MVC group of the specified type with a particular name.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *      }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be created as follows
      *
      * <pre>
      * def (m, v, c) = createMVCGroup('foo', 'foo' + System.currenttimeMillis())
      * assert (c instanceof FooController)
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(String mvcType, String mvcName);
 
     /**
      * Instantiates an MVC group of the specified type with a particular name.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * We can create two instances of the same group that share the same model instance in the following way:
      *
      * <pre>
      * def (m1, v1, c1) = createMVCGroup('foo', 'foo1')
      * def (m2, v2, c2) = createMVCGroup('foo', 'foo2', model: m1)
      * assert fm1 == m2
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(Map<String, Object> args, String mvcType, String mvcName);
 
     /**
      * Instantiates an MVC group of the specified type with a particular name.<p>
      * MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.<p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * </ul>
      *
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * We can create two instances of the same group that share the same model instance in the following way:
      *
      * <pre>
      * def (m1, v1, c1) = createMVCGroup('foo', 'foo1')
      * def (m2, v2, c2) = createMVCGroup('foo', 'foo2', model: m1)
      * assert fm1 == m2
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @return a List with the canonical MVC members of the group
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration.
      */
     List<? extends GriffonMvcArtifact> createMVCGroup(String mvcType, String mvcName, Map<String, Object> args);
 
     /**
      * Destroys an MVC group identified by a particular name.<p>
      * <b>ATTENTION:</b> make sure to call the super implementation if you override this method
      * otherwise group references will not be kept up to date.
      *
      * @param mvcName the name of the group to destroy and dispose.
      */
     void destroyMVCGroup(String mvcName);
 
     /**
      * Returns the arguments set on the command line (if any).<p>
      *
      * @since 0.9.2
      * @return an array of command line arguments. Never returns null.
      */
     String[] getStartupArgs();
 
     /**
      * Returns a Logger instance suitable for this application.
      *
      * @since 0.9.2
      * @return a Logger instance.
      */
     Logger getLog();
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
     * withMVCGroup('foo') { m, v c ->
      *     m.someProperty = someValue
      *     c.invokeAnAction()
      * }
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     void withMVCGroup(String mvcType, Closure handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
     * withMVCGroup('foo', 'foo1') { m, v c ->
      *     m.someProperty = someValue
      *     c.invokeAnAction()
      * }
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     void withMVCGroup(String mvcType, String mvcName, Closure handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
     * withMVCGroup('foo', 'foo1') { m, v c ->
      *     m.someProperty = someValue
      *     c.invokeAnAction()
      * }
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     void withMVCGroup(String mvcType, String mvcName, Map<String, Object> args, Closure handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
     * withMVCGroup('foo', 'foo1') { m, v c ->
      *     m.someProperty = someValue
      *     c.invokeAnAction()
      * }
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     void withMVCGroup(String mvcType, Map<String, Object> args, Closure handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *    }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
      * withMVCGroup("foo", new MVCClosure&lt;FooModel, FooView, FooController&gt;() {
      *     public void call(FooModel m, FooView v, FooController c) {
      *         m.setSomeProperty(someValue);
      *         c.invokeAnAction();
      *     }
      * });
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     <M extends GriffonModel, V extends GriffonView, C extends GriffonController> void withMVCGroup(String mvcType, MVCClosure<M, V, C> handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
      * withMVCGroup("foo", "foo1", new MVCClosure&lt;FooModel, FooView, FooController&gt;() {
      *     public void call(FooModel m, FooView v, FooController c) {
      *         m.setSomeProperty(someValue);
      *         c.invokeAnAction();
      *     }
      * });
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     <M extends GriffonModel, V extends GriffonView, C extends GriffonController> void withMVCGroup(String mvcType, String mvcName, MVCClosure<M, V, C> handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *     }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
      * Map<String, Object> map = ... // initialized elsewhere
      * withMVCGroup("foo", "foo1", map, new MVCClosure&lt;FooModel, FooView, FooController&gt;() {
      *    public void call(FooModel m, FooView v, FooController c) {
      *        m.setSomeProperty(someValue);
      *        c.invokeAnAction();
      *    }
      * });
      * </pre>
      *
      * MVC groups must have an unique name.
      *
      * @param mvcType the type of group to build.
      * @param mvcName the name to assign to the built group.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     <M extends GriffonModel, V extends GriffonView, C extends GriffonController> void withMVCGroup(String mvcType, String mvcName, Map<String, Object> args, MVCClosure<M, V, C> handler);
 
     /**
      * Instantiates an MVC group of the specified type then destroys it after it has been handled.<p>
      * <p>This method is of particular interest when working with short lived MVC groups such as
      * those used to build dialogs.<p/>
      * <p>MVC Groups must be previously configured with the application's metadata
      * before they can be used. This registration process usually takes place automatically
      * at boot time. The type of the group can be normally found in the application's
      * configuration file.</p>
      * The <tt>args</tt> Map can contain any value that will be used in one of the following
      * scenarios <ul>
      * <li>The key matches a member definition; the value will be used as the instance of such member.</li>
      * <li>The key does not match a member definition, the value is assumed to be a property that can be set
      * on any MVC member of the group.</li>
      * For example, with the following entry available in {@code Application.groovy}
      *
      * <pre>
      * mvcGroups {
      *     'foo' {
      *         model      = 'com.acme.FooModel'
      *         view       = 'com.acme.FooView'
      *         controller = 'com.acme.FooController'
      *    }
      * }
      * </pre>
      *
      * An instance of the "foo" group can be used as follows
      *
      * <pre>
      * Map<String, Object> map = ... // initialized elsewhere
      * withMVCGroup("foo", map, new MVCClosure&lt;FooModel, FooView, FooController&gt;() {
      *    public void call(FooModel m, FooView v, FooController c) {
      *        m.setSomeProperty(someValue);
      *        c.invokeAnAction();
      *    }
      * });
      * </pre>
      *
      * @param mvcType the type of group to build.
      * @param args any useful values that can be set as properties on each MVC member or that
      * identify a member that can be shared with other groups.
      * @throws IllegalArgumentException if the type specified is not found in the application's
      * configuration
      *
      * @since 0.9.3
      */
     <M extends GriffonModel, V extends GriffonView, C extends GriffonController> void withMVCGroup(String mvcType, Map<String, Object> args, MVCClosure<M, V, C> handler);
 }
