 /*******************************************************************************
  * Copyright (c) 2007 IBM Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package com.ibm.wala.classLoader;
 
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import com.ibm.wala.ipa.cha.ClassHierarchyException;
 import com.ibm.wala.ipa.cha.ClassHierarchyWarning;
 import com.ibm.wala.ipa.cha.IClassHierarchy;
 import com.ibm.wala.shrikeCT.InvalidClassFileException;
 import com.ibm.wala.types.FieldReference;
 import com.ibm.wala.types.MethodReference;
 import com.ibm.wala.types.Selector;
 import com.ibm.wala.types.TypeName;
 import com.ibm.wala.types.TypeReference;
 import com.ibm.wala.types.annotations.Annotation;
 import com.ibm.wala.util.collections.BimodalMap;
 import com.ibm.wala.util.collections.HashMapFactory;
 import com.ibm.wala.util.collections.HashSetFactory;
 import com.ibm.wala.util.collections.SmallMap;
 import com.ibm.wala.util.debug.Assertions;
 import com.ibm.wala.util.strings.Atom;
 import com.ibm.wala.util.strings.ImmutableByteArray;
 import com.ibm.wala.util.warnings.Warning;
 import com.ibm.wala.util.warnings.Warnings;
 
 /**
  * A class representing which originates in some form of bytecode.
  *
  * @param <T> type of classloader which loads this format of class. 
  */
 public abstract class BytecodeClass<T extends IClassLoader> implements IClass {
 
   protected BytecodeClass(T loader, IClassHierarchy cha) {
     this.loader = loader;
     this.cha = cha;
   }
 
   /**
    * An Atom which holds the name of the super class. We cache this for efficiency reasons.
    */
   protected ImmutableByteArray superName;
 
   /**
    * The names of interfaces for this class. We cache this for efficiency reasons.
    */
   protected ImmutableByteArray[] interfaceNames;
 
   /**
    * The object that loaded this class.
    */
   protected final T loader;
 
   /**
    * Governing class hierarchy for this class
    */
   protected final IClassHierarchy cha;
 
   /**
    * A mapping from Selector to IMethod
    * 
    * TODO: get rid of this for classes (though keep it for interfaces) instead ... use a VMT.
    */
   protected Map<Selector, IMethod> methodMap;
 
   /**
    * A mapping from Selector to IMethod used to cache method lookups from superclasses
    */
   protected Map<Selector, IMethod> inheritCache;
 
   /**
    * Canonical type representation
    */
   protected TypeReference typeReference;
 
   /**
    * superclass
    */
   protected IClass superClass;
 
   /**
    * Compute the superclass lazily.
    */
   protected boolean superclassComputed = false;
 
   /**
    * The IClasses that represent all interfaces this class implements (if it's a class) or extends (it it's an interface)
    */
   protected Collection<IClass> allInterfaces = null;
 
   /**
    * The instance fields declared in this class.
    */
   protected IField[] instanceFields;
 
   /**
    * The static fields declared in this class.
    */
   protected IField[] staticFields;
 
   /**
    * hash code; cached here for efficiency
    */
   protected int hashCode;
 
   private final HashMap<Atom, IField> fieldMap = HashMapFactory.make(5);
 
   /**
    * A warning for when we get a class not found exception
    */
   private static class ClassNotFoundWarning extends Warning {
 
     final ImmutableByteArray className;
 
     ClassNotFoundWarning(ImmutableByteArray className) {
       super(Warning.SEVERE);
       this.className = className;
     }
 
     @Override
     public String getMsg() {
       return getClass().toString() + " : " + className;
     }
 
     public static ClassNotFoundWarning create(ImmutableByteArray className) {
       return new ClassNotFoundWarning(className);
     }
   }
 
   public IClassLoader getClassLoader() {
     return loader;
   }
 
   protected abstract IMethod[] computeDeclaredMethods() throws InvalidClassFileException;
 
 
   public TypeReference getReference() {
     return typeReference;
   }
 
   public String getSourceFileName() {
     return loader.getSourceFileName(this);
   }
   
   public InputStream getSource() {
     return loader.getSource(this);
   }
 
   @Override
   public int hashCode() {
     return hashCode;
   }
 
   @Override
   public String toString() {
     return getReference().toString();
   }
 
   public boolean isArrayClass() {
     return false;
   }
 
   public IClassHierarchy getClassHierarchy() {
     return cha;
   }
 
   public TypeName getName() {
     return getReference().getName();
   }
 
   public boolean isReferenceType() {
     return getReference().isReferenceType();
   }
 
   public IField getField(Atom name) {
     if (fieldMap.containsKey(name)) {
       return fieldMap.get(name);
     } else {
       IField f = findDeclaredField(name);
       if (f != null) {
         fieldMap.put(name, f);
         return f;
       } else if (superClass != null) {
         f = superClass.getField(name);
         if (f != null) {
           fieldMap.put(name, f);
           return f;
         }
       }
       // try superinterfaces
       try {
         for (IClass i : getAllImplementedInterfaces()) {
           f = i.getField(name);
           if (f != null) {
             fieldMap.put(name, f);
             return f;
           }
         }
       } catch (ClassHierarchyException e) {
         // skip
       }
     }
 
     return null;
   }
 
   private void computeSuperclass() {
     superclassComputed = true;
 
     if (superName == null) {
       if (!getReference().equals(loader.getLanguage().getRootType())) {
         superClass = loader.lookupClass(loader.getLanguage().getRootType().getName());
       }
       return;
     }
 
     superClass = loader.lookupClass(TypeName.findOrCreate(superName));
   }
 
   public IClass getSuperclass() throws ClassHierarchyException {
     if (!superclassComputed) {
       computeSuperclass();
     }
     if (superClass == null && !getReference().equals(TypeReference.JavaLangObject)) {
       throw new ClassHierarchyException("No superclass found for " + this+" Superclass name "+superName);
     }
     return superClass;
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getAllFields()
    */
   public Collection<IField> getAllFields() throws ClassHierarchyException {
     Collection<IField> result = new LinkedList<IField>();
     result.addAll(getAllInstanceFields());
     result.addAll(getAllStaticFields());
     return result;
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
    */
   public Collection<IClass> getAllImplementedInterfaces() throws ClassHierarchyException {
     if (allInterfaces != null) {
       return allInterfaces;
     } else {
       Collection<IClass> C = computeAllInterfacesAsCollection();
       allInterfaces = Collections.unmodifiableCollection(C);
       return allInterfaces;
     }
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
    */
   public Collection<IField> getDeclaredInstanceFields() {
     return Collections.unmodifiableList(Arrays.asList(instanceFields));
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
    */
   public Collection<IField> getDeclaredStaticFields() {
     return Collections.unmodifiableList(Arrays.asList(staticFields));
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
    */
   public Collection<IClass> getDirectInterfaces() {
     return array2IClassSet(interfaceNames);
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
    */
   public Collection<IField> getAllInstanceFields() throws ClassHierarchyException {
     Collection<IField> result = new LinkedList<IField>(getDeclaredInstanceFields());
     IClass s = getSuperclass();
     while (s != null) {
       result.addAll(s.getDeclaredInstanceFields());
       s = s.getSuperclass();
     }
     return result;
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
    */
   public Collection<IField> getAllStaticFields() throws ClassHierarchyException {
     Collection<IField> result = new LinkedList<IField>(getDeclaredStaticFields());
     IClass s = getSuperclass();
     while (s != null) {
       result.addAll(s.getDeclaredStaticFields());
       s = s.getSuperclass();
     }
     return result;
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getAllMethods()
    */
   public Collection<IMethod> getAllMethods() throws ClassHierarchyException {
     Collection<IMethod> result = new LinkedList<IMethod>();
     Iterator<IMethod> declaredMethods = getDeclaredMethods().iterator();
     while (declaredMethods.hasNext()) {
       result.add(declaredMethods.next());
     }
     IClass s = getSuperclass();
     while (s != null) {
       Iterator<IMethod> superDeclaredMethods = s.getDeclaredMethods().iterator();
       while (superDeclaredMethods.hasNext()) {
         result.add(superDeclaredMethods.next());
       }
       s = s.getSuperclass();
     }
     return result;
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
    */
   public Collection<IMethod> getDeclaredMethods() {
     if (methodMap == null) {
       try {
         computeMethodMap();
       } catch (InvalidClassFileException e) {
         e.printStackTrace();
         Assertions.UNREACHABLE();
       }
     }
     return Collections.unmodifiableCollection(methodMap.values());
   }
 
   /*
    * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.types.Selector)
    */
   public IMethod getMethod(Selector selector) {
     if (methodMap == null) {
       try {
         computeMethodMap();
       } catch (InvalidClassFileException e1) {
         e1.printStackTrace();
         Assertions.UNREACHABLE();
       }
     }
 
     // my methods + cached parent stuff
     IMethod result = methodMap.get(selector);
     if (result != null) {
       return result;
     }
     if (inheritCache != null) {
       result = inheritCache.get(selector);
       if (result != null) {
         return result;
       }
     }
 
     // check parent, caching if found
     try {
       if (!selector.equals(MethodReference.clinitSelector) && !selector.equals(MethodReference.initSelector)) {
         IClass superclass = getSuperclass();
         if (superclass != null) {
           IMethod inherit = superclass.getMethod(selector);
           if (inherit != null) {
             if (inheritCache == null) {
               inheritCache = new BimodalMap<Selector, IMethod>(5);
             }
             inheritCache.put(selector, inherit);
             return inherit;
           }
         }
       }
     } catch (ClassHierarchyException e) {
       Assertions.UNREACHABLE();
     }
 
     // didn't find it yet. special logic for interfaces
     try {
       if (isInterface() || isAbstract()) {
         final Iterator<IClass> it = getAllImplementedInterfaces().iterator();
         // try each superinterface
         while (it.hasNext()) {
           IClass k = it.next();
           result = k.getMethod(selector);
           if (result != null) {
             return result;
           }
         }
       }
     } catch (ClassHierarchyException e) {
       e.printStackTrace();
       Assertions.UNREACHABLE("Bad method lookup in " + this);
     }
     return null;
 
   }
 
   protected void populateFieldArrayFromList(List<FieldImpl> L, IField[] A) {
     Iterator<FieldImpl> it = L.iterator();
     for (int i = 0; i < A.length; i++) {
       A[i] = it.next();
     }
   }
 
   /**
    * @return Collection of IClasses, representing the interfaces this class implements.
    */
   protected Collection<IClass> computeAllInterfacesAsCollection() throws ClassHierarchyException {
     Collection<IClass> c = getDirectInterfaces();
     Set<IClass> result = HashSetFactory.make();
     for (Iterator<IClass> it = c.iterator(); it.hasNext();) {
       IClass klass = it.next();
       if (klass.isInterface()) {
         result.add(klass);
       } else {
         Warnings.add(ClassHierarchyWarning.create("expected an interface " + klass));
       }
     }
     
     // at this point result holds all interfaces the class directly extends.
     // now expand to a fixed point.
     Set<IClass> last = null;
     do {
       last = HashSetFactory.make(result);
       for (IClass i : last) {
         result.addAll(i.getDirectInterfaces());
       }
     } while (last.size() < result.size());
     
 
     // now add any interfaces implemented by the super class
     IClass sup = null;
     try {
       sup = getSuperclass();
     } catch (ClassHierarchyException e1) {
       Assertions.UNREACHABLE();
     }
     if (sup != null) {
       result.addAll(sup.getAllImplementedInterfaces());
     }
     return result;
   }
 
   /**
    * @param interfaces a set of class names
    * @return Set of all IClasses that can be loaded corresponding to the class names in the interfaces array; raise warnings if
    *         classes can not be loaded
    */
   private Collection<IClass> array2IClassSet(ImmutableByteArray[] interfaces) {
     ArrayList<IClass> result = new ArrayList<IClass>(interfaces.length);
     for (int i = 0; i < interfaces.length; i++) {
       ImmutableByteArray name = interfaces[i];
       IClass klass = null;
       klass = loader.lookupClass(TypeName.findOrCreate(name));
       if (klass == null) {
         Warnings.add(ClassNotFoundWarning.create(name));
       } else {
         result.add(klass);
       }
     }
     return result;
   }
 
   protected IField findDeclaredField(Atom name) {
     for (int i = 0; i < instanceFields.length; i++) {
       if (instanceFields[i].getName() == name) {
         return instanceFields[i];
       }
     }
 
     for (int i = 0; i < staticFields.length; i++) {
       if (staticFields[i].getName() == name) {
         return staticFields[i];
       }
     }
 
     return null;
   }
 
   protected void addFieldToList(List<FieldImpl> L, Atom name, ImmutableByteArray fieldType, int accessFlags,
       Collection<Annotation> annotations) {
     TypeName T = null;
     if (fieldType.get(fieldType.length() - 1) == ';') {
       T = TypeName.findOrCreate(fieldType, 0, fieldType.length() - 1);
     } else {
       T = TypeName.findOrCreate(fieldType);
     }
     TypeReference type = TypeReference.findOrCreate(getClassLoader().getReference(), T);
     FieldReference fr = FieldReference.findOrCreate(getReference(), name, type);
     FieldImpl f = new FieldImpl(this, fr, accessFlags, annotations);
     L.add(f);
   }
 
   /**
    * set up the methodMap mapping
    */
   protected void computeMethodMap() throws InvalidClassFileException {
     if (methodMap == null) {
       IMethod[] methods = computeDeclaredMethods();
       if (methods.length > 5) {
         methodMap = HashMapFactory.make(methods.length);
       } else {
         methodMap = new SmallMap<Selector, IMethod>();
       }
       for (int i = 0; i < methods.length; i++) {
         IMethod m = methods[i];
         methodMap.put(m.getReference().getSelector(), m);
       }
     }
   }
 
 }
