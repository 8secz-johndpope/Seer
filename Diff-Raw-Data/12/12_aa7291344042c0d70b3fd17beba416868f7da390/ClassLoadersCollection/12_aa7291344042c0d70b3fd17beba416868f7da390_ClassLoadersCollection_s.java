 package org.sonar.core.classloaders;
 
 import org.apache.commons.lang.StringUtils;
 import org.codehaus.classworlds.ClassRealm;
 import org.codehaus.classworlds.ClassWorld;
 import org.codehaus.classworlds.DuplicateRealmException;
 import org.codehaus.classworlds.NoSuchRealmException;
 import org.sonar.api.utils.Logs;
 import org.sonar.api.utils.SonarException;
 
 import java.net.URL;
 import java.util.Arrays;
 import java.util.Collection;
 
 /**
  * EXPERIMENTAL!
  * 
  * Encapsulates manipulations with ClassLoaders, such as creation and establishing dependencies.
  * Current implementation based on {@link ClassWorld}.
  * 
  * <h3>IMPORTANT</h3>
  * <ul>
  * <li>If we have pluginA , then all classes and resources from packages and subpackages of org.sonar.plugins.pluginA will be visible for
  * all other plugins.</li>
  * <li>If pluginA depends on lib.jar which contains org.sonar.plugins.pluginA.SomeClass , then SomeClass will be visible for all other
  * plugins.</li>
  * </ul>
  * 
  * @since 2.4
  */
 public class ClassLoadersCollection {
 
  private static final String EXPORTED_PREFIX = "org.sonar.plugins.";
 
   private ClassWorld world = new ClassWorld();
   private ClassLoader baseClassLoader;
 
   public ClassLoadersCollection(ClassLoader baseClassLoader) {
     this.baseClassLoader = baseClassLoader;
   }
 
   /**
    * Generates URLClassLoaders, which use the parent first delegation mode.
    * Actually this method shouldn't return anything, because dependencies must be established - see {@link #done()}.
    */
   public ClassLoader createClassLoader(String key, Collection<URL> urls) {
     try {
       ClassRealm realm = world.newRealm(key, baseClassLoader);
       for (URL constituent : urls) {
         realm.addConstituent(constituent);
       }
      // export(realm, EXPORTED_PREFIX + key);
       return realm.getClassLoader();
     } catch (DuplicateRealmException e) {
       throw new SonarException(e);
     }
   }
 
   /**
    * Establishes dependencies among ClassLoaders.
    */
   public void done() {
     for (Object o : world.getRealms()) {
       ClassRealm realm = (ClassRealm) o;
      export(realm, EXPORTED_PREFIX + realm.getId());
     }
   }
 
   /**
    * Exports specified packages from given ClassRealm to all others.
    */
   private void export(ClassRealm realm, String... packages) {
     Logs.INFO.info("Exporting " + Arrays.toString(packages) + " from " + realm.getId());
     for (Object o : world.getRealms()) {
       ClassRealm dep = (ClassRealm) o;
       if ( !StringUtils.equals(dep.getId(), realm.getId())) {
         try {
           for (String packageName : packages) {
             dep.importFrom(realm.getId(), packageName);
           }
         } catch (NoSuchRealmException e) {
           // should never happen
           throw new SonarException(e);
         }
       }
     }
   }
 
   /**
    * Note that this method should be called only after creation of all ClassLoaders - see {@link #done()}.
    */
   public ClassLoader get(String key) {
     try {
       return world.getRealm(key).getClassLoader();
     } catch (NoSuchRealmException e) {
       return null;
     }
   }
 
 }
