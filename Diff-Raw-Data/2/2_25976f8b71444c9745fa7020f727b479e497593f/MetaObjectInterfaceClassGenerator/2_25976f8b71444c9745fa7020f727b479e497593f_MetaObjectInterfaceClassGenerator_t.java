 /*
  * ################################################################
  *
  * ProActive: The Java(TM) library for Parallel, Distributed,
  *            Concurrent computing with Security and Mobility
  *
  * Copyright (C) 1997-2004 INRIA/University of Nice-Sophia Antipolis
  * Contact: proactive-support@inria.fr
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
 package org.objectweb.proactive.core.component.gen;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 
 import javassist.CannotCompileException;
 import javassist.CtClass;
 import javassist.CtField;
 import javassist.CtMethod;
 import javassist.CtNewMethod;
 import javassist.Modifier;
 import javassist.NotFoundException;
 
 import org.objectweb.fractal.api.Component;
 import org.objectweb.fractal.api.type.InterfaceType;
 import org.objectweb.proactive.core.component.ProActiveInterface;
 import org.objectweb.proactive.core.component.ProActiveInterfaceImpl;
 import org.objectweb.proactive.core.component.exceptions.InterfaceGenerationFailedException;
 import org.objectweb.proactive.core.mop.JavassistByteCodeStubBuilder;
 import org.objectweb.proactive.core.mop.StubObject;
 import org.objectweb.proactive.core.util.log.ProActiveLogger;
 
 
 /**
  * Creates Interface implementations for the functional interfaces of the
  * component metaobject.
  *<br>
  * The functional calls are delegated to the "impl" field, whose value is set during
  * binding operations.
  *<br>
  * - In case of a primitive component, the impl field will be the reified object to
  * which the body is attached.<br>
  * - In case of a composite component, the impl field will be a component
  * representative.<br>
  * - For a parallel component, the impl field will be a group of component representatives.<br>
  *
  *  @author Matthieu Morel
  *
  */
 public class MetaObjectInterfaceClassGenerator
     extends AbstractInterfaceClassGenerator {
     protected static final String IMPL_FIELD_NAME = "impl"; //delegatee
     private static MetaObjectInterfaceClassGenerator instance;
 
     // this boolean for deciding of a possible indirection for the functionnal calls
     protected boolean isPrimitive = false;
 
     public MetaObjectInterfaceClassGenerator() {
     }
 
     public static MetaObjectInterfaceClassGenerator instance() {
         if (instance == null) {
             return new MetaObjectInterfaceClassGenerator();
         } else {
             return instance;
         }
     }
 
     public ProActiveInterface generateInterface(final String interfaceName,
         Component owner, InterfaceType interfaceType, boolean isInternal,
         boolean isFunctionalInterface)
         throws InterfaceGenerationFailedException {
         try {
             if (ProActiveLogger.getLogger("components.bytecodegeneration")
                                    .isDebugEnabled()) {
                 ProActiveLogger.getLogger("components.bytecodegeneration")
                                .debug("generating metaobject interface reference");
             }
 
             String generatedClassFullName = org.objectweb.proactive.core.component.gen.Utils.getMetaObjectClassName(interfaceName,
                     interfaceType.getFcItfSignature());
 
             Class generated_class;
 
             // check whether class has already been generated
             try {
                 generated_class = loadClass(generatedClassFullName);
             } catch (ClassNotFoundException cnfe) {
                 CtMethod[] reifiedMethods;
                 CtClass generatedCtClass = pool.makeClass(generatedClassFullName);
 
                 //this.fcInterfaceName = fcInterfaceName;
                 //isPrimitive = ((ProActiveComponentRepresentativeImpl) owner).getHierarchicalType()
                 //                                                    .equals(ComponentParameters.PRIMITIVE);
                 List interfacesToImplement = new ArrayList();
 
                 // add interface to reify
                 CtClass functional_itf = pool.get(interfaceType.getFcItfSignature());
                 generatedCtClass.addInterface(functional_itf);
 
                 interfacesToImplement.add(functional_itf);
 
                 // add Serializable interface
                 interfacesToImplement.add(pool.get(Serializable.class.getName()));
                 generatedCtClass.addInterface(pool.get(
                         Serializable.class.getName()));
 
                 // add StubObject, so we can set the proxy
                 generatedCtClass.addInterface(pool.get(
                         StubObject.class.getName()));
 
                 //interfacesToImplement.add(pool.get(StubObject.class.getName()));
                 List interfacesToImplementAndSuperInterfaces = new ArrayList(interfacesToImplement);
                 addSuperInterfaces(interfacesToImplementAndSuperInterfaces);
                 generatedCtClass.setSuperclass(pool.get(
                         ProActiveInterfaceImpl.class.getName()));
                 JavassistByteCodeStubBuilder.createStubObjectMethods(generatedCtClass);
 
                 CtField implField = new CtField(pool.get(Object.class.getName()),
                         IMPL_FIELD_NAME, generatedCtClass);
                 generatedCtClass.addField(implField);
                 CtMethod implGetter = CtNewMethod.getter("getFcItfImpl",
                         implField);
                 generatedCtClass.addMethod(implGetter);
                 CtMethod implSetter = CtNewMethod.setter("setFcItfImpl",
                         implField);
                 generatedCtClass.addMethod(implSetter);
 
                 CtField methodsField = new CtField(pool.get(
                             "java.lang.reflect.Method[]"), "overridenMethods",
                         generatedCtClass);
                 methodsField.setModifiers(Modifier.STATIC);
                 generatedCtClass.addField(methodsField);
 
                 // list all methods to implement
                 Map methodsToImplement = new HashMap();
                 List classesIndexer = new Vector();
 
                 CtClass[] params;
                 CtClass itf;
 
                 // now get the methods from implemented interfaces
                 Iterator it = interfacesToImplementAndSuperInterfaces.iterator();
                 while (it.hasNext()) {
                     itf = (CtClass) it.next();
                     if (!classesIndexer.contains(itf.getName())) {
                         classesIndexer.add(itf.getName());
                     }
 
                     CtMethod[] declaredMethods = itf.getDeclaredMethods();
 
                     for (int i = 0; i < declaredMethods.length; i++) {
                         CtMethod currentMethod = declaredMethods[i];
 
                         // Build a key with the simple name of the method
                         // and the names of its parameters in the right order
                         String key = "";
                         key = key + currentMethod.getName();
                         params = currentMethod.getParameterTypes();
                         for (int k = 0; k < params.length; k++) {
                             key = key + params[k].getName();
                         }
 
                         // this gives the actual declaring Class of this method
                         methodsToImplement.put(key, currentMethod);
                     }
                 }
 
                 reifiedMethods = (CtMethod[]) (methodsToImplement.values()
                                                                  .toArray(new CtMethod[methodsToImplement.size()]));
 
                 // Determines which reifiedMethods are valid for reification
                 // It is the responsibility of method checkMethod in class Utils
                 // to decide if a method is valid for reification or not
                 Vector v = new Vector();
                 int initialNumberOfMethods = reifiedMethods.length;
 
                 for (int i = 0; i < initialNumberOfMethods; i++) {
                     if (JavassistByteCodeStubBuilder.checkMethod(
                                 reifiedMethods[i])) {
                         v.addElement(reifiedMethods[i]);
                     }
                 }
                 CtMethod[] validMethods = new CtMethod[v.size()];
                 v.copyInto(validMethods);
 
                 reifiedMethods = validMethods;
 
                 JavassistByteCodeStubBuilder.createStaticInitializer(generatedCtClass,
                     reifiedMethods, classesIndexer);
 
                 createMethods(generatedCtClass, reifiedMethods, interfaceType);
 
                 //                generatedCtClass.writeFile("generated/");
                 //                System.out.println("[JAVASSIST] generated class : " +
                 //                    generatedClassFullName);
                 byte[] bytecode = generatedCtClass.toBytecode();
                 RepresentativeInterfaceClassGenerator.generatedClassesCache.put(generatedClassFullName,
                     generatedCtClass.toBytecode());
                 if (logger.isDebugEnabled()) {
                     logger.debug("added " + generatedClassFullName +
                         " to cache");
                 }
                 if (logger.isDebugEnabled()) {
                     logger.debug("generated classes cache is : " +
                         generatedClassesCache.toString());
                 }
 
                 // convert the bytes into a Class
                 generated_class = defineClass(generatedClassFullName, bytecode);
             }
 
             ProActiveInterfaceImpl reference = (ProActiveInterfaceImpl) generated_class.newInstance();
             reference.setFcItfName(interfaceName);
             reference.setFcItfOwner(owner);
             reference.setFcType(interfaceType);
             reference.setFcIsInternal(isInternal);
 
             return reference;
         } catch (Exception e) {
             e.printStackTrace();
             throw new InterfaceGenerationFailedException("Cannot generate representative with javassist",
                 e);
         }
     }
 
     private void createMethods(CtClass generatedCtClass,
         CtMethod[] reifiedMethods, InterfaceType interfaceType)
         throws CannotCompileException, NotFoundException {
         for (int i = 0; i < reifiedMethods.length; i++) {
             CtClass[] paramTypes = reifiedMethods[i].getParameterTypes();
 
             String body = "return ";
             body += ("((" + interfaceType.getFcItfSignature() + ")");
             body += (IMPL_FIELD_NAME + ")." + reifiedMethods[i].getName() +
             "(");
             for (int j = 0; j < paramTypes.length; j++) {
                body += ("(" + paramTypes[j].getName() + ")" + ("$" + (j +1)));
                 if (j < (paramTypes.length - 1)) {
                     body += ",";
                 }
             }
             body += ")";
 
             body += ";";
             body += "\n}";
             //            System.out.println("method : " + reifiedMethods[i].getName() +
             //                " : \n" + body);
             CtMethod methodToGenerate = CtNewMethod.make(reifiedMethods[i].getReturnType(),
                     reifiedMethods[i].getName(),
                     reifiedMethods[i].getParameterTypes(),
                     reifiedMethods[i].getExceptionTypes(), body,
                     generatedCtClass);
             generatedCtClass.addMethod(methodToGenerate);
         }
     }
 }
