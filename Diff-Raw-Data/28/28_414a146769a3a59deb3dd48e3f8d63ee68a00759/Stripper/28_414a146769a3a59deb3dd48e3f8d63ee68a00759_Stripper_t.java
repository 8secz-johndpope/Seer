 package net.sf.beezle.maven.plugins.application;
 
 import javassist.ClassPool;
 import javassist.CtBehavior;
 import javassist.CtClass;
 import javassist.CtMethod;
 import javassist.NotFoundException;
 import javassist.bytecode.BadBytecode;
 import javassist.bytecode.CodeAttribute;
 import javassist.bytecode.CodeIterator;
 import javassist.bytecode.ConstPool;
 import javassist.bytecode.Descriptor;
 import javassist.bytecode.ExceptionTable;
 import javassist.bytecode.ExceptionsAttribute;
 import javassist.bytecode.Opcode;
 import net.sf.beezle.sushi.archive.Archive;
 import net.sf.beezle.sushi.util.Strings;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 /** See also: http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#16491 */
 public class Stripper {
     /** @param roots class.method names */
     public static Stripper run(Archive archive, List<String> roots) throws IOException {
         /*
         Repository repository;
         MethodRef m;
         Stripper stripper;
 
         repository = new Repository();
         repository.addAllLazy(archive.data);
         // TODO: Java classes outside of runtime.jar ...
         repository.addAllLazy(archive.data.getWorld().locateClasspathItem(Object.class));
         stripper = new Stripper(repository);
         for (String root : roots) {
             int idx;
             ClassRef ref;
             ClassDef def;
 
             idx = root.lastIndexOf('.');
             ref = new ClassRef(root.substring(0, idx));
             try {
                 def = (ClassDef) ref.resolve(repository);
             } catch (ResolveException e) {
                 throw new IllegalArgumentException("unknown class: " + ref.toString());
             }
             for (MethodDef method : def.methods) {
                 if (method.name.equals(root.substring(idx + 1))) {
                     stripper.add(method.reference(ref, false));
                 }
             }
         }
         stripper.closure();
         for (Node cf : archive.data.find("** /*.class")) {
             if (!stripper.referenced(cf.getRelative(archive.data))) {
                 cf.delete();
             }
         }
         return stripper;*/ return null;
     }
 
     private final ClassPool pool;
     private final List<CtBehavior> methods;
 
     /** only classes in classpath */
     public final List<CtClass> classes;
 
     public Stripper(ClassPool pool) {
         this.pool = pool;
         this.methods = new ArrayList<CtBehavior>();
         this.classes = new ArrayList<CtClass>();
 
     }
 
     public void closure() throws NotFoundException {
         CtBehavior m;
         CodeAttribute code;
         CodeIterator iter;
         int pos;
         ConstPool pool;
         int index;
         CtClass clazz;
         CtBehavior b;
 
         // size grows!
         for (int i = 0; i < methods.size(); i++) {
             m = methods.get(i);
             code = m.getMethodInfo().getCodeAttribute();
             if (code == null) {
                 continue;
             }
             pool = code.getConstPool();
             iter = code.iterator();
             while (iter.hasNext()) {
                 try {
                     pos = iter.next();
                 } catch (BadBytecode e) {
                     throw new IllegalStateException(e);
                 }
                 switch (iter.byteAt(pos)) {
                     case Opcode.GETSTATIC:
                     case Opcode.PUTSTATIC:
                     case Opcode.GETFIELD:
                     case Opcode.PUTFIELD:
                         add(Descriptor.toCtClass(pool.getFieldrefType(iter.u16bitAt(pos + 1)), this.pool));
                         break;
                     case Opcode.INVOKEVIRTUAL:
                     case Opcode.INVOKESTATIC:
                     case Opcode.INVOKESPECIAL:
                         index = iter.u16bitAt(pos + 1);
                         clazz = Descriptor.toCtClass(pool.getMethodrefClassName(index), this.pool);
                         try {
                             b = clazz.getMethod(pool.getMethodrefName(index), pool.getMethodrefType(index));
                         } catch (NotFoundException e) {
                             b = clazz.getConstructor(pool.getMethodrefType(index));
                         }
                         add(b);
                         break;
                     case Opcode.INVOKEINTERFACE:
                         index = iter.u16bitAt(pos + 1);
                         clazz = Descriptor.toCtClass(pool.getInterfaceMethodrefClassName(index), this.pool);
                         add(clazz.getMethod(pool.getInterfaceMethodrefName(index), pool.getInterfaceMethodrefType(index)));
                         break;
                     case Opcode.ANEWARRAY:
                     case Opcode.CHECKCAST:
                     case Opcode.MULTIANEWARRAY:
                     case Opcode.NEW:
                         add(this.pool.getCtClass(pool.getClassInfo(iter.u16bitAt(pos + 1))));
                         break;
                     default:
                         // nothing
                 }
             }
         }
     }
 
     private CtMethod interfaceMethodInfo(ConstPool pool, int index) throws NotFoundException {
         CtClass ifc;
 
         ifc = Descriptor.toCtClass(pool.getInterfaceMethodrefClassName(index), this.pool);
         return ifc.getMethod(pool.getInterfaceMethodrefName(index), pool.getInterfaceMethodrefType(index));
     }
 
 
     public void add(CtBehavior method) throws NotFoundException {
         ExceptionTable exceptions;
         ExceptionsAttribute ea;
         int size;
         CodeAttribute code;
 
        if (!contains(methods, method)) {
             add(method.getDeclaringClass());
            methods.add(method);
             for (CtClass p : method.getParameterTypes()) {
                 add(p);
             }
             if (method instanceof CtMethod) {
                 add(((CtMethod) method).getReturnType());
             }
             code = method.getMethodInfo().getCodeAttribute();
             if (code != null) {
                 exceptions = code.getExceptionTable();
                 size = exceptions.size();
                 for (int i = 0; i < size; i++) {
                     String name = method.getMethodInfo().getConstPool().getClassInfo(exceptions.catchType(i));
                     if (name != null) {
                         add(pool.get(name));
                     }
                 }
             }
             ea = method.getMethodInfo().getExceptionsAttribute();
             if (ea != null) {
                 // I've tested this: java loads exceptions declared via throws, even if they're never thrown!
                 for (String exception : ea.getExceptions()) {
                     add(pool.get(exception));
                 }
             }
             if (method instanceof CtMethod) {
                 for (CtMethod derived : derived((CtMethod) method)) {
                     add(derived);
                 }
             }
         }
     }
 
    /** CtBehavior.equals compare method name and arguments only ... */
    private static boolean contains(List<CtBehavior> lst, CtBehavior right) {
        for (CtBehavior left : lst) {
            if (left.equals(right)) {
                if (left.getDeclaringClass().equals(right.getDeclaringClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(Object[] objects, Object element) {
         for (Object obj : objects) {
             if (obj.equals(element)) {
                 return true;
             }
         }
         return false;
     }
 
     /** @return methodRefs to already visited classes that directly override baseMethod */
     public List<CtMethod> derived(CtMethod baseMethod) throws NotFoundException {
         List<CtMethod> result;
         CtClass baseClass;
 
         result = new ArrayList<CtMethod>();
         baseClass = baseMethod.getDeclaringClass();
         for (CtClass derivedClass : classes) {
             if (baseClass.equals(derivedClass.getSuperclass()) || contains(derivedClass.getInterfaces(), baseClass)) {
                 try {
                     result.add(derivedClass.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes()));
                 } catch (NotFoundException e) {
                     // nothing to do
                 }
             }
         }
         return result;
     }
 
     public void add(CtClass clazz) throws NotFoundException {
         if (!classes.contains(clazz)) {
             classes.add(clazz);
             if (clazz.getSuperclass() != null) {
                 add(clazz.getSuperclass());
             }
             for (CtBehavior method : clazz.getDeclaredBehaviors()) {
                 if (method.getName().equals("<clinit>")) {
                     add(method);
                 }
             }
             for (CtMethod derived : clazz.getDeclaredMethods()) {
                 for (CtBehavior base : new ArrayList<CtBehavior>(methods)) { // TODO: copy ...
                     if (base instanceof CtMethod) {
                         if (overrides((CtMethod) base, derived)) {
                             add(derived);
                         }
                     }
                 }
             }
         }
     }
 
     private boolean overrides(CtMethod base, CtMethod derivedMethod) throws NotFoundException {
         CtClass baseClass;
         CtClass derivedClass;
 
         baseClass = base.getDeclaringClass();
         derivedClass = derivedMethod.getDeclaringClass();
         if (baseClass.equals(derivedClass.getSuperclass()) || contains(derivedClass.getInterfaces(), baseClass)) {
             return sameSignature(base, derivedMethod);
         }
         return false;
     }
 
     private boolean sameSignature(CtMethod left, CtMethod right) throws NotFoundException {
         CtClass[] leftParams;
         CtClass[] rightParams;
 
         if (left.getName().equals(right.getName())) {
             leftParams = left.getParameterTypes();
             rightParams = right.getParameterTypes();
             if (left.getParameterTypes().length == right.getParameterTypes().length) {
                 // the return type is not checked - it doesn't matter!
 
                 for (int i = 0; i < left.getParameterTypes().length; i++) {
                     if (!leftParams[i].equals(rightParams[i])) {
                         return false;
                     }
                 }
                 return true;
             }
         }
         return false;
     }
 
     public boolean referenced(String resourceName) throws NotFoundException {
         String name;
 
         name = Strings.removeRight(resourceName, ".class");
         name = name.replace('/', '.');
         return classes.contains(pool.get(name));
     }
 
     public void warnings() {
         String name;
 
         for (CtBehavior m : methods) {
             name = m.getDeclaringClass().getName();
             if (name.startsWith("java.lang.reflect.")) {
                 System.out.println("CAUTION: " + m);
             }
             if (name.equals("java.lang.Class") && m.getName().equals("forName")) {
                 System.out.println("CAUTION: " + m);
             }
             if (name.equals("java.lang.ClassLoader") && m.getName().equals("loadClass")) {
                 System.out.println("CAUTION: " + m);
             }
         }
     }
 
     public ClassPool getPool() {
         return pool;
     }
 }
