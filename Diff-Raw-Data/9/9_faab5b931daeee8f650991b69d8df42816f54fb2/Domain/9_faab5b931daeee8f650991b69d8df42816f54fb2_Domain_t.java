 /*
  * Copyright (C) 2003-2009 eXo Platform SAS.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.chromattic.core;
 
 import org.chromattic.api.BuilderException;
 import org.chromattic.common.ObjectInstantiator;
 import org.chromattic.common.jcr.Path;
 import org.chromattic.common.jcr.PathException;
 import org.chromattic.core.mapper.MapperBuilder;
 import org.chromattic.core.mapper.ObjectMapper;
 import org.chromattic.core.jcr.type.TypeManager;
 import org.chromattic.core.query.QueryManager;
 import org.chromattic.metamodel.mapping.BeanMapping;
 import org.chromattic.metamodel.type.SimpleTypeResolver;
 import org.chromattic.spi.instrument.Instrumentor;
 import org.chromattic.api.format.ObjectFormatter;
 
 import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import java.lang.annotation.Annotation;
 import java.lang.reflect.UndeclaredThrowableException;
 import java.util.*;
 import org.chromattic.common.collection.Collections;
 import org.chromattic.spi.instrument.MethodHandler;
 import org.chromattic.spi.instrument.ProxyType;
 
 /**
  * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
  * @version $Revision$
  */
 public class Domain {
 
   /** . */
   private static final ProxyType<?> NULL_PROXY_TYPE = new ProxyType<Object>() {
     public Object createProxy(MethodHandler handler) {
       throw new UnsupportedOperationException("Cannot create proxy for " + handler);
     }
 
     public Class<? extends Object> getType() {
       throw new UnsupportedOperationException("Cannot get proxy type for NULL_PROXY_TYPE");
     }
   };
 
   /** . */
   private static final Instrumentor NULL_INSTRUMENTOR = new Instrumentor() {
 
     // This is OK as the class is *stateless*
     @SuppressWarnings("unchecked")
     public <O> ProxyType<O> getProxyClass(Class<O> clazz) {
       return (ProxyType<O>) NULL_PROXY_TYPE;
     }
 
     public MethodHandler getInvoker(Object proxy) {
       throw new UnsupportedOperationException();
     }
   };
 
   /** . */
   public static int LAZY_CREATE_MODE = 0;
 
   /** . */
   public static int CREATE_MODE = 1;
 
   /** . */
   public static int NO_CREATE_MODE = 2;
 
   /** . */
   private static final Set<Integer> CREATE_MODES = Collections.set(LAZY_CREATE_MODE, CREATE_MODE, NO_CREATE_MODE);
 
   /** . */
   private final Map<String, ObjectMapper> typeMapperByNodeType;
 
   /** . */
   private final Map<Class<?>, ObjectMapper> typeMapperByClass;
 
   /** . */
   private final Instrumentor  defaultInstrumentor;
 
   /** . */
   private final Map<Class<?>, Instrumentor> instrumentors = new HashMap<Class<?>, Instrumentor>();
 
   /** . */
   final Collection<BeanMapping> mappings;
   
   /** . */
   final ObjectFormatter objectFormatter;
 
   /** . */
   final boolean propertyCacheEnabled;
 
   /** . */
   final boolean propertyReadAheadEnabled;
 
   /** . */
   final boolean hasPropertyOptimized;
   /** . */
 
   final boolean hasNodeOptimized;
 
   /** . */
   final String rootNodePath;
 
   /** . */
   final List<String> rootNodePathSegments;
 
   /** . */
   final String rootNodeType;
 
   /** . */
   final int rootCreateMode;
 
   /** . */
   final TypeManager nodeInfoManager;
 
   /** . */
   final QueryManager queryManager;
 
   public Domain(
     SimpleTypeResolver resolver,
     Collection<BeanMapping> mappings,
     Instrumentor defaultInstrumentor,
     ObjectFormatter objectFormatter,
     boolean propertyCacheEnabled,
     boolean propertyReadAheadEnabled,
     boolean hasPropertyOptimized,
     boolean hasNodeOptimized,
     String rootNodePath,
     int rootCreateMode,
     String rootNodeType) {
 
     //
     if (!CREATE_MODES.contains(rootCreateMode)) {
       throw new IllegalArgumentException("Invalid create mode " + rootCreateMode);
     }
     
     //
     Map<BeanMapping, Instrumentor> instrumentorMapping = new HashMap<BeanMapping, Instrumentor>();
     mapping: for (BeanMapping beanMapping : mappings) {
       Class<?> clazz = (Class<?>)beanMapping.getBean().getClassType().unwrap();
       for (Annotation annotation : clazz.getAnnotations()) {
         if ("org.chromattic.groovy.annotations.GroovyInstrumentor".equals(annotation.annotationType().getName())) {
           Class<?> instrumentorClass = null;
           try {
             instrumentorClass = (Class<?>)annotation.annotationType().getMethod("value").invoke(annotation);
           } catch (Exception ignore) {}
           Instrumentor i = ObjectInstantiator.newInstance(instrumentorClass.getName(), Instrumentor.class);
           instrumentors.put(i.getProxyClass(clazz).getType(), i);
           instrumentorMapping.put(beanMapping, i);
           continue mapping;
         }
       }
       if (Object.class.equals(clazz)) {
         instrumentors.put(clazz, defaultInstrumentor);
         instrumentorMapping.put(beanMapping, NULL_INSTRUMENTOR);
       } else {
         instrumentors.put(defaultInstrumentor.getProxyClass(clazz).getType(), defaultInstrumentor);
         instrumentorMapping.put(beanMapping, defaultInstrumentor);
       }
     }
 
     //
     MapperBuilder builder = new MapperBuilder(resolver, instrumentorMapping);
     Collection<ObjectMapper<?>> mappers = builder.build(mappings);
 
     //
     Map<String, ObjectMapper> typeMapperByNodeType = new HashMap<String, ObjectMapper>();
     Map<Class<?>, ObjectMapper> typeMapperByClass = new HashMap<Class<?>, ObjectMapper>();
     for (ObjectMapper typeMapper : mappers) {
       if (typeMapperByNodeType.containsKey(typeMapper.getNodeTypeName())) {
         throw new IllegalStateException("Duplicate node type name " + typeMapper);
       }
       typeMapperByNodeType.put(typeMapper.getNodeTypeName(), typeMapper);
       typeMapperByClass.put(typeMapper.getObjectClass(), typeMapper);
     }
 
     //
     final List<String> rootNodePathSegments;
     try {
       rootNodePathSegments = Path.splitAbsolutePath(Path.normalizeAbsolutePath(rootNodePath));
     }
     catch (PathException e) {
       throw new BuilderException("Root node path must be valid");
     }
 
     //
     this.mappings = mappings;
     this.typeMapperByClass = typeMapperByClass;
     this.typeMapperByNodeType = typeMapperByNodeType;
     this.defaultInstrumentor = defaultInstrumentor;
     this.objectFormatter = objectFormatter;
     this.propertyCacheEnabled = propertyCacheEnabled;
     this.propertyReadAheadEnabled = propertyReadAheadEnabled;
     this.hasPropertyOptimized = hasPropertyOptimized;
     this.hasNodeOptimized = hasNodeOptimized;
     this.rootNodePath = rootNodePath;
     this.rootNodePathSegments = rootNodePathSegments;
     this.nodeInfoManager = new TypeManager();
     this.queryManager = new QueryManager(rootNodePath);
     this.rootCreateMode = rootCreateMode;
     this.rootNodeType = rootNodeType;
   }
 
   public Collection<BeanMapping> getMappings() {
     return mappings;
   }
 
   public boolean isHasPropertyOptimized() {
     return hasPropertyOptimized;
   }
 
   public boolean isHasNodeOptimized() {
     return hasNodeOptimized;
   }
 
   public MethodHandler getHandler(Object o) {
    Instrumentor instrumentor = instrumentors.get(o.getClass());
    return instrumentor != null ? instrumentor.getInvoker(o) : null;
   }
 
   public ObjectMapper getTypeMapper(String nodeTypeName) {
     return typeMapperByNodeType.get(nodeTypeName);
   }
 
   public ObjectMapper getTypeMapper(Class<?> clazz) {
     return typeMapperByClass.get(clazz);
   }
 
   public QueryManager getQueryManager() {
     return queryManager;
   }
 
   String decodeName(EntityContext ctx, String internal, NameKind nameKind) throws RepositoryException {
     if (ctx == null) {
       throw new NullPointerException();
     }
     return decodeName(ctx.state.getNode(), internal, nameKind);
   }
 
   /**
    * Decodes an internal name that is owned by the specified node.
    *
    * @param ownerNode the owner node
    * @param internal the internal name
    * @param nameKind the kind of name
    * @return the external name or null
    * @throws RepositoryException any repository exception
    */
   String decodeName(Node ownerNode, String internal, NameKind nameKind) throws RepositoryException {
     if (ownerNode == null) {
       throw new NullPointerException();
     }
     if (nameKind == NameKind.PROPERTY) {
       return internal;
     }
 
     //
     ObjectFormatter formatter = null;
     String nodeTypeName = ownerNode.getPrimaryNodeType().getName();
     ObjectMapper parentMapper = getTypeMapper(nodeTypeName);
     if (parentMapper != null) {
       formatter = parentMapper.getFormatter();
     }
     if (formatter == null) {
       formatter = objectFormatter;
     }
 
     //
     String external;
     try {
       if (nameKind == NameKind.OBJECT) {
         external = formatter.decodeNodeName(null, internal);
       } else {
         // external = formatter.decodePropertyName(null, internal);
         throw new UnsupportedOperationException();
       }
     }
     catch (Exception e) {
       if (e instanceof IllegalStateException) {
         throw (IllegalStateException)e;
       }
       throw new UndeclaredThrowableException(e);
     }
     if (external == null) {
       if (nameKind == NameKind.OBJECT) {
         throw new IllegalStateException();
       }
     }
     return external;
   }
 
   /**
    * Encodes the name for the specified context.
    *
    * @param ownerCtx the context
    * @param externalName the external name
    * @param nameKind the name kind
    * @return the encoded name
    * @throws NullPointerException if the owner context argument is null
    * @throws RepositoryException any repository exception
    */
   String encodeName(EntityContext ownerCtx, String externalName, NameKind nameKind) throws NullPointerException, RepositoryException {
     if (ownerCtx == null) {
       throw new NullPointerException();
     }
     return encodeName(ownerCtx.state.getNode(), externalName, nameKind);
   }
 
   /**
    * Encodes the name for the specified context.
    *
    * @param ownerNode the node
    * @param externalName the external name
    * @param nameKind the name kind
    * @return the encoded name
    * @throws NullPointerException if any argument is null
    * @throws RepositoryException any repository exception
    */
   String encodeName(Node ownerNode, String externalName, NameKind nameKind) throws NullPointerException, RepositoryException {
     if (ownerNode == null) {
       throw new NullPointerException("No null owner node accepted");
     }
     if (externalName == null) {
       throw new NullPointerException("No null name accepted");
     }
     if (nameKind == null) {
       throw new NullPointerException("No null name kind accepted");
     }
 
     //
     if (nameKind == NameKind.PROPERTY) {
       return externalName;
     }
 
     //
     ObjectFormatter formatter = null;
     String nodeTypeName = ownerNode.getPrimaryNodeType().getName();
     ObjectMapper parentMapper = getTypeMapper(nodeTypeName);
     if (parentMapper != null) {
       formatter = parentMapper.getFormatter();
     }
     if (formatter == null) {
       formatter = objectFormatter;
     }
 
     //
     String internal;
     try {
       if (nameKind == NameKind.OBJECT) {
         internal = formatter.encodeNodeName(null, externalName);
       } else {
         // internal = formatter.encodePropertyName(null, external);
         throw new UnsupportedOperationException();
       }
     }
     catch (Exception e) {
       if (e instanceof NullPointerException) {
         throw (NullPointerException)e;
       }
       if (e instanceof IllegalArgumentException) {
         throw (IllegalArgumentException)e;
       }
       throw new UndeclaredThrowableException(e);
     }
     if (internal == null) {
       throw new IllegalArgumentException("Name " + externalName + " was converted to null");
     }
     Path.validateName(internal);
     return internal;
   }
 }
