 /*
  *                    BioJava development code
  *
  * This code may be freely distributed and modified under the
  * terms of the GNU Lesser General Public Licence.  This should
  * be distributed with the code.  If you do not have a copy,
  * see:
  *
  *      http://www.gnu.org/copyleft/lesser.html
  *
  * Copyright for this code is held jointly by the individual
  * authors.  These should be listed in @author doc comments.
  *
  * For more information on the BioJava project and its aims,
  * or to join the biojava-l mailing list, visit the home page
  * at:
  *
  *      http://www.biojava.org/
  *
  */
 
 package org.biojavax;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
import org.biojavax.bio.db.HibernateCrossReferenceResolver;
import org.biojavax.bio.db.HibernateRichObjectBuilder;
 import org.biojavax.bio.seq.PositionResolver;
 import org.biojavax.bio.seq.PositionResolver.AverageResolver;
 import org.biojavax.ontology.ComparableOntology;
 import org.biojavax.ontology.SimpleComparableOntology;
 
 
 /**
  * Runs a service that builds rich objects, and provides some default values
  * for things like default ontology, default namespace, etc.
  * @author Richard Holland
  */
 public class RichObjectFactory {
     
     private static RichObjectBuilder builder = new SimpleRichObjectBuilder();
     
     private static String defaultOntologyName = "biojavax";
     private static String defaultNamespaceName = "lcl";
     private static PositionResolver defaultPositionResolver = new AverageResolver();
     private static CrossReferenceResolver defaultCrossRefResolver = new DummyCrossReferenceResolver();
     
     // the LRU cache - keys are classes, entries are maps of param sets to objects
     private static int defaultLRUcacheSize = 20;
     private static Map LRUcacheSizes = new HashMap();
     private static Map cache = new HashMap();
     
     // Constructor is private as this is all static.
     private RichObjectFactory() {}
     
     /**
      * Sets the builder to use when instantiating new Rich objects. The basic,
      * default, one is a SimpleRichObjectBuilder, which just calls the constructor.
      * Another useful one is HibernateRichObjectBuilder, which attempts to load
      * objects from the database. The latter is required if you are working with
      * Hibernate as it will not work without it.
      * @param b the builder to use.
      * @see SimpleRichObjectBuilder
      * @see org.biojavax.bio.db.HibernateRichObjectBuilder
      */
     public static synchronized void setRichObjectBuilder(RichObjectBuilder b) {
         builder = b;
     }
     
     /**
      * Delegates to a RichObjectBuilder to construct/retrieve the object, and returns it.
      * To increase efficiency, it keeps a list of recently requested objects. If it
      * receives further requests for the same object, it returns them from the cache. The
      * size of the cache can be altered using setLRUCacheSize(). The default cache size is 20
      * objects for each type of class requested.
      * @param clazz the class to build
      * @param params the parameters to pass to the class' constructor
      * @return the instantiated object
      */
     public static synchronized Object getObject(final Class clazz, Object[] params) {
         List paramsList = Arrays.asList(params);
         if (!cache.containsKey(clazz)) {
             int LRUcacheSize = defaultLRUcacheSize;
             if (LRUcacheSizes.containsKey(clazz)) LRUcacheSize = ((Integer)LRUcacheSizes.get(clazz)).intValue();
             else LRUcacheSizes.put(clazz,new Integer(LRUcacheSize));
             cache.put(clazz, new LinkedHashMap(LRUcacheSize, 0.75f, true) {
                 protected boolean removeEldestEntry(Map.Entry eldest) {
                     return this.size() > ((Integer)LRUcacheSizes.get(clazz)).intValue();
                 }
             });
         }
         Map m = (Map)cache.get(clazz);
         if (!m.containsKey(paramsList)) {
             m.put(paramsList,builder.buildObject(clazz, paramsList));
         }
         return m.get(paramsList);
     }
     
     /**
      * Sets the size of the LRU cache. This is the size per class of object requested, so
      * if you set it to 20 and request 3 different types of object, you will get 20*3=60
      * entries in the cache. The default cache size is 20. Setting this value will undo 
      * any previous changes made using the setLRUCacheSize(Class,int) method below, but will not
      * override future ones.
      * @param size the size of the cache.
      */
     public static void setLRUCacheSize(int size) {
         defaultLRUcacheSize = size;
         for (Iterator i = LRUcacheSizes.keySet().iterator(); i.hasNext(); ) LRUcacheSizes.put(i.next(), new Integer(size));
     }
     
     /**
      * Sets the size of the LRU cache. This is the size for the specific class of object 
      * requested, so does not affect the size of caches of other objects.
      * If this method is not called, then the cache size defaults to 20, or whatever value
      * was passed to setLRUCacheSize(int) above.
      * @param size the size of the cache.
      */
     public static void setLRUCacheSize(Class clazz, int size) {
         LRUcacheSizes.put(clazz,new Integer(size));
     }
     
     /**
      * Sets the default namespace name to use when loading sequences. Defaults to "lcl".
      * @param name the namespace name to use.
      */
     public static void setDefaultNamespaceName(String name) { defaultNamespaceName = name; }
     
     /**
      * Sets the default ontology name to use when loading sequences. Defaults to "biojavax".
      * @param name the ontology name to use.
      */
     public static void setDefaultOntologyName(String name) { defaultOntologyName = name; }
     
     /**
      * Sets the default position resolver to use when creating new rich feature locations.
      * Defaults to the AverageResolver
      * @param pr the position resolver to use.
      * @see org.biojavax.bio.seq.PositionResolver
      * @see org.biojavax.bio.seq.PositionResolver.AverageResolver
      * @see org.biojavax.bio.seq.RichLocation
      */
     public static void setDefaultPositionResolver(PositionResolver pr) { defaultPositionResolver = pr; }
     
     /**
      * Sets the default crossref resolver to use when resolving remote entries.
      * Defaults to the DummyCrossReferenceResolver.
      * @param crr the resolver to use.
      * @see org.biojavax.CrossReferenceResolver
      * @see org.biojavax.DummyCrossReferenceResolver
      */
     public static void setDefaultCrossReferenceResolver(CrossReferenceResolver crr) { defaultCrossRefResolver = crr; }
     
     /**
      * Returns the default namespace object. Defaults to "lcl".
      * @return the default namespace.
      */
     public static Namespace getDefaultNamespace() {
         return (Namespace)getObject(SimpleNamespace.class, new Object[]{defaultNamespaceName});
     }
     
     /**
      * Returns the default ontology object. Defaults to "biojavax".
      * @return the default ontology.
      */
     public static ComparableOntology getDefaultOntology() {
         return (ComparableOntology)getObject(SimpleComparableOntology.class, new Object[]{defaultOntologyName});
     }
     
     /**
      * Returns the default position resolver object. Defaults to PositionResolver.AverageResolver
      * @return the default position resolver.
      * @see org.biojavax.bio.seq.PositionResolver.AverageResolver
      */
     public static PositionResolver getDefaultPositionResolver() { return defaultPositionResolver; }
     
     /**
      * Returns the default cross ref resolver object. Defaults to DummyCrossReferenceResolver
      * @return the default resolver.
      * @see org.biojavax.DummyCrossReferenceResolver
      */
     public static CrossReferenceResolver getDefaultCrossReferenceResolver() { return defaultCrossRefResolver; }
         
    /** 
     * A utility method that configures the RichObjectFactory for use with a Hibernate session.
     * @param session an object containing a Hibernate session.
     */
    public static void connectToHibernate(Object session) {
        RichObjectFactory.setRichObjectBuilder(new HibernateRichObjectBuilder(session));
        RichObjectFactory.setDefaultCrossReferenceResolver(new HibernateCrossReferenceResolver(session));       
    }
 }
