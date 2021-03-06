 /*
  * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *
  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.
  *
  * This code is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  * version 2 for more details (a copy is included in the LICENSE file that
  * accompanied this code).
  *
  * You should have received a copy of the GNU General Public License version
  * 2 along with this work; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
  * CA 95054 USA or visit www.sun.com if you need additional information or
  * have any questions.
  */
 
 package com.sun.tools.javafx.code;
 import com.sun.tools.javac.code.*;
 import com.sun.tools.javac.util.*;
 import com.sun.tools.javac.code.Type.*;
 import java.util.HashMap;
 import com.sun.tools.javafx.tree.*;
 import com.sun.tools.javac.code.Symbol.*;
 import static com.sun.tools.javac.code.Kinds.*;
 import static com.sun.tools.javac.code.Flags.*;
 import static com.sun.tools.javac.code.TypeTags.*;
 import java.util.HashSet;
 import java.util.Set;
 /**
  *
  * @author bothner
  */
 public class JavafxTypes extends Types {
     JavafxSymtab syms;
 
     private HashMap<ClassSymbol, JFXClassDeclaration> fxClasses;
 
     public static void preRegister(final Context context) {
         if (context.get(typesKey) == null)
             context.put(typesKey, new Context.Factory<Types>() {
                 public Types make() {
                     return new JavafxTypes(context);
                 }
             });
     }
 
     public static void preRegister(final Context context, JavafxTypes types) {
         context.put(typesKey, types);
     }
 
     public static JavafxTypes instance(Context context) {
         JavafxTypes instance = (JavafxTypes) context.get(typesKey);
         if (instance == null)
             instance = new JavafxTypes(context);
         return instance;
     }
 
     protected JavafxTypes(Context context) {
         super(context);
         syms = (JavafxSymtab) JavafxSymtab.instance(context);
     }
 
    public boolean isSequence(Type type) {
        return type != Type.noType && type != null
                 && type.tag != TypeTags.ERROR 
                 && type.tag != TypeTags.METHOD && type.tag != TypeTags.FORALL
                 && erasure(type) == syms.javafx_SequenceTypeErasure;
     }
 
     public Type sequenceType(Type elemType) {
         return sequenceType(elemType, true);
     }
      public Type sequenceType(Type elemType, boolean withExtends) {
         if (elemType.isPrimitive())
             elemType = boxedClass(elemType).type;
         if (withExtends)
             elemType = new WildcardType(elemType, BoundKind.EXTENDS, syms.boundClass);
         Type seqtype = syms.javafx_SequenceType;
         List<Type> actuals = List.of(elemType);
         Type clazzOuter = seqtype.getEnclosingType();
         return new ClassType(clazzOuter, actuals, seqtype.tsym);
     }
 
     public Type elementType(Type seqType) {
         Type elemType = seqType.getTypeArguments().head;
         if (elemType instanceof CapturedType)
             elemType = ((CapturedType) elemType).wildcard;
         if (elemType instanceof WildcardType)
             elemType = ((WildcardType) elemType).type;
         if (elemType == null)
             return syms.javafx_AnyType;
         Type unboxed = unboxedType(elemType);
         if (unboxed.tag != TypeTags.NONE)
             elemType = unboxed;
         return elemType;
     }
 
     public Type unboxedTypeOrType(Type t) {
         Type ubt = unboxedType(t);
         return ubt==Type.noType? t : ubt;
     }
 
     public void getSupertypes(Symbol clazz, ListBuffer<Type> supertypes,Set<Type> dupSet) {
         if (clazz != null) {
             Type supType = supertype(clazz.type);
             if (supType != null && supType != Type.noType && !dupSet.contains(supType)) {
                 supertypes.append(supType);
                 dupSet.add(supType);
                 getSupertypes(supType.tsym, supertypes,dupSet);
             }
 
             if (clazz instanceof JavafxClassSymbol) {
                 for (Type superType : ((JavafxClassSymbol)clazz).getSuperTypes()) {
                     if (!dupSet.contains(superType)) {
                         supertypes.append(superType);
                         dupSet.add(superType);
                         getSupertypes(superType.tsym, supertypes,dupSet);
                     }
                 }
             }
         }
     }
 
     public List<Type> supertypes(Symbol clazz) {
         return supertypes(clazz, null);
     }
 
     public List<Type> supertypes(Symbol clazz, Type includeOrNull) {
         ListBuffer<Type> supertypes = ListBuffer.<Type>lb();
         Set<Type> superSet = new HashSet<Type>();
         if (includeOrNull != null) {
             supertypes.append(includeOrNull);
             superSet.add(includeOrNull);
         }
 
         getSupertypes(clazz, supertypes, superSet);
 
         return supertypes.toList();
     }
 
     public boolean isSuperType (Type maybeSuper, ClassSymbol sym) {
         ListBuffer<Type> supertypes = ListBuffer.<Type>lb();
         Set superSet = new HashSet<Type>();
         supertypes.append(sym.type);
         superSet.add(sym.type);
         getSupertypes(sym, supertypes, superSet);
         for (Type t : supertypes) {
             if (isSameType(t,maybeSuper)) {
                 return true;
             }
         }
         return false;
     }
     
     @Override
     public boolean isSubtype(Type t, Type s, boolean capture) {
         boolean b = super.isSubtype(t, s, capture);
         if (!b && s.isCompound()) {
             for (Type s2 : interfaces(s).prepend(supertype(s))) {
                 if (!isSubtype(t, s2, capture))
                     return false;
             }
             return true;
         }
         else
             return b;
     }
 
     @Override
     public Type asSuper(Type t, Symbol sym) {
         if (isCompoundClass(t.tsym)) {
             JavafxClassSymbol tsym = (JavafxClassSymbol) t.tsym;
             List<Type> supers = tsym.getSuperTypes();
             for (List<Type> l = supers; l.nonEmpty(); l = l.tail) {
                 Type x = asSuper(l.head, sym);
                 if (x != null)
                     return x;
             }
         }
         return super.asSuper(t, sym);
     }
     
     public Type superType(JFXClassDeclaration cDecl) {
         //TODO: this is in drastic need of cleaning up
         if (! (cDecl.type instanceof ClassType))
             return null;
         ClassType cType = (ClassType) cDecl.type;
         Type superType = cType.supertype_field;
         if (superType != null &&
             superType.tsym instanceof ClassSymbol &&
             (superType.tsym.flags_field & JavafxFlags.COMPOUND_CLASS) == 0) {
             if (superType == syms.objectType &&
                     (cType.tsym.flags_field & JavafxFlags.COMPOUND_CLASS) != 0) {
                 // Pick first inherited compound class.
                 for (Type iface : cType.interfaces_field) {
                     if ((iface.tsym.flags_field & JavafxFlags.COMPOUND_CLASS) != 0) {
                         return iface;
                     }
                 }
             }
         } else if ((cDecl.mods.flags & Flags.FINAL) != 0L && cDecl.getExtending().nonEmpty()) {
             Symbol sym1 = JavafxTreeInfo.symbol(cDecl.getExtending().head);
             if (sym1 != null &&
                     (sym1.flags_field & JavafxFlags.COMPOUND_CLASS) == 0) {
                 superType = cDecl.getExtending().head.type;
             }
         }
         return superType;
     }
 
     @Override
     public boolean isConvertible (Type t, Type s, Warner warn) {
         if (super.isConvertible(t, s, warn))
             return true;
         if (isSequence(t) && isArray(s))
             return isConvertible(elementType(t), elemtype(s), warn);
         if (isArray(t) && isSequence(s))
             return isConvertible(elemtype(t), elementType(s), warn);

         // Allow all numeric conversion, for now (some should warn)
         if (isNumeric(t) && isNumeric(s)) {
             return true;
         }

         return false;
     }
 
     @Override
     public boolean isCastable(Type t, Type s, Warner warn) {
         Type target = isSequence(s) ? elementType(s) : s;
         Type source = isSequence(t) ? elementType(t) : t;
         if (target.isPrimitive() && ! source.isPrimitive())
             target = boxedClass(target).type;
         if (source.isPrimitive() && ! target.isPrimitive())
             source = boxedClass(source).type;
 
         if (source == syms.botType ||
             target == syms.botType)
             return true;
 
         boolean isSourceFinal = (source.tsym.flags() & FINAL) != 0;
         boolean isTargetFinal = (target.tsym.flags() & FINAL) != 0;
         if (isJFXClass(source.tsym) && isJFXClass(target.tsym))
             return true;
         else if (isJFXClass(source.tsym) &&
             !isTargetFinal || 
             target.isInterface())
             return true;
         else if (isJFXClass(target.tsym) &&
             !isSourceFinal ||
             target.isInterface())
             return true;
         else //conversion between two primitives/Java classes
             return super.isCastable(source, target, warn);
     }
     
     public boolean isCompoundClass(Symbol sym) {
         if (! (sym instanceof JavafxClassSymbol))
             return false;
         sym.complete();
         return (sym.flags_field & JavafxFlags.COMPOUND_CLASS) != 0;
     }
 
     public boolean isJFXClass(Symbol sym) {
         if (! (sym instanceof JavafxClassSymbol))
             return false;
         sym.complete();
         return (sym.flags_field & JavafxFlags.FX_CLASS) != 0;
     }
     
     public void addFxClass(ClassSymbol csym, JFXClassDeclaration cdecl) {
         if (fxClasses == null) {
             fxClasses = new HashMap<ClassSymbol, JFXClassDeclaration>();
         }
         csym.flags_field |= JavafxFlags.FX_CLASS;
         fxClasses.put(csym, cdecl);
     }
     
     public JFXClassDeclaration getFxClass (ClassSymbol csym) {
        return fxClasses.get(csym);
     }
     
     /** The implementation of this (abstract) symbol in class origin;
      *  null if none exists. Synthetic methods are not considered
      *  as possible implementations.
      *  Based on the Javac implementation method in MethodSymbol,
      *  but modified to handle multiple inheritance.
      */
     public MethodSymbol implementation(MethodSymbol msym, TypeSymbol origin, boolean checkResult) {
         msym.complete();
         if (origin instanceof JavafxClassSymbol && isCompoundClass(origin)) {
             JavafxClassSymbol c = (JavafxClassSymbol) origin;
             for (Scope.Entry e = c.members().lookup(msym.name);
                      e.scope != null;
                      e = e.next()) {
                 if (e.sym.kind == MTH) {
                         MethodSymbol m = (MethodSymbol) e.sym;
                         m.complete();
                         if (m.overrides(msym, origin, this, checkResult) &&
                             (m.flags() & SYNTHETIC) == 0)
                             return m;
                 }
             }
             List<Type> supers = c.getSuperTypes();
             for (List<Type> l = supers; l.nonEmpty(); l = l.tail) {
                 MethodSymbol m = implementation(msym, l.head.tsym, checkResult);
                 if (m != null)
                     return m;
             }
             return null;
         }
         else
             return msym.implementation(origin, this, checkResult);
     }
 
     /** A replacement for MethodSymbol.overrides. */
     public boolean overrides(Symbol sym, Symbol _other, TypeSymbol origin, boolean checkResult) {
         if (sym.isConstructor() || _other.kind != MTH) return false;
 
         if (sym == _other) return true;
         MethodSymbol other = (MethodSymbol)_other;
 
         // assert types.asSuper(origin.type, other.owner) != null;
         Type mt = this.memberType(origin.type, sym);
         Type ot = this.memberType(origin.type, other);
         return
             this.isSubSignature(mt, ot) &&
             (!checkResult || this.resultSubtype(mt, ot, Warner.noWarnings));
     }
 
     public void clearCaches() {
         fxClasses = null;
     }
     
     public String toJavaFXString(Type type) {
         StringBuilder buffer = new StringBuilder();
         try {
             toJavaFXString(type, buffer);
         } catch (java.io.IOException ioe) {
             throw new RuntimeException(ioe);
         }
         return buffer.toString();
     }
     
     public String toJavaFXString(List<Type> ts) {
         if (ts.isEmpty())
             return "";
         StringBuilder buffer = new StringBuilder();
         try {
             toJavaFXString(ts.head, buffer);
             for (List<Type> l = ts.tail; l.nonEmpty(); l = l.tail) {
                 buffer.append(",");
                 toJavaFXString(l.head, buffer);
             }
         } catch (java.io.IOException ioe) {
             throw new RuntimeException(ioe);
         }
         return buffer.toString();
     }
 
     public boolean isNumeric(Type type) {
         return (isSameType(type, syms.javafx_ByteType) ||
                 isSameType(type, syms.javafx_ShortType) ||
                 isSameType(type, syms.javafx_IntegerType) ||
                 isSameType(type, syms.javafx_LongType) ||
                 isSameType(type, syms.javafx_FloatType) ||
                 isSameType(type, syms.javafx_DoubleType));
     }
 
     private boolean isJavaFXBoolean(Type type) {
         boolean result = false;
         if (type.tag == BOOLEAN) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaLong(Type type) {
         boolean result = false;
         if (type.tag == LONG) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXInteger(Type type) {
         boolean result = false;
         if (type.tag == BYTE) {
             result = true;
         } else if (type.tag == SHORT) {
             result = true;
         } else if (type.tag == INT) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXNumber(Type type){
         boolean result = false;
         if (type.tag == FLOAT) {
             result = true;
         } else if (type.tag == DOUBLE) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXString(Type type) {
         boolean result = false;
         if ((type.tag == CLASS) && (type.toString().equals("java.lang.String"))) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXObject(Type type) {
         boolean result = false;
         if ((type.tag == CLASS) && (type.toString().equals("java.lang.Object"))) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXUnknown(Type type) {
         boolean result = false;
         if (type.tag == UNKNOWN) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXSequence(Type type) {
         boolean result = false;
         if (isSequence(type)) {
             result = true;
         }
         return result;
     }
     
     private boolean isJavaFXMethod(Type type) {
         boolean result = false;
         if ((type instanceof MethodType) || (type instanceof FunctionType)) {
             result = true;
         }
         return result;
     }
     
     private void sequenceToJavaFXString(Type type, Appendable buffer) throws java.io.IOException {
         toJavaFXString(elementType(type), buffer);
         buffer.append("[]");
     }
     
     private void methodToJavaFXString(MethodType type, Appendable buffer) throws java.io.IOException {
         if (type.getReturnType() == null) {
             buffer.append("function(?):?");
             return;
         }
         buffer.append("function(");
         List<Type> args = type.getParameterTypes();
         for (List<Type> l = args; l.nonEmpty(); l = l.tail) {
             if (l != args) {
                 buffer.append(",");
             }
             buffer.append(":");
             toJavaFXString(l.head, buffer);
         }
         buffer.append("):");
         toJavaFXString(type.getReturnType(), buffer);
     }
 
     private void toJavaFXString(Type type, Appendable buffer) throws java.io.IOException {
         if (isJavaFXBoolean(type)) {
             buffer.append("Boolean");
         } else if (isJavaLong(type)) {
             buffer.append("java.lang.Long");
         } else if (isJavaFXInteger(type)) {
             buffer.append("Integer");
         } else if (isJavaFXNumber(type)) {
             buffer.append("Number");
         } else if (isJavaFXString(type)) {
             buffer.append("String");
         } else if (isJavaFXObject(type)) {
             buffer.append("Object");
         } else if (isJavaFXUnknown(type)) {
             // Is this right?
             buffer.append("Object");
         } else if (type == Type.noType) {
             buffer.append("<unknown>");
         } else if (isJavaFXSequence(type)) {
             sequenceToJavaFXString(type, buffer);
         } else if (isJavaFXMethod(type)) {
             MethodType methodType = type.asMethodType();
             methodToJavaFXString(methodType, buffer);
         } else if (type.isCompound()) {
             toJavaFXString(supertype(type), buffer);
         } else {
             buffer.append(type.toString());
         }
     }
 
     public String toJavaFXString(MethodSymbol sym, List<VarSymbol> params) {
         StringBuilder builder = new StringBuilder();
         try {
             toJavaFXString(sym, params, builder);
         } catch (java.io.IOException ioe) {
             throw new RuntimeException(ioe);
         }
         return builder.toString();
     }
 
     public void toJavaFXString(MethodSymbol sym, List<VarSymbol> params,
             Appendable buffer) throws java.io.IOException {
         if ((sym.flags() & BLOCK) != 0)
             buffer.append(sym.owner.name);
         else {
             buffer.append(sym.name == sym.name.table.init ? sym.owner.name : sym.name);
             if (sym.type != null) {
                 buffer.append('(');
                 // FUTURE: check (flags() & VARARGS) != 0
                 List<Type> args = sym.type.getParameterTypes();
                 for (List<Type> l = args; l.nonEmpty(); l = l.tail) {
                     if (l != args)
                         buffer.append(",");
                     if (params != null && params.nonEmpty()) {
                         VarSymbol param = params.head;
                         if (param != null)
                             buffer.append(param.name);
                         params = params.tail;
                     }
                     buffer.append(":");
                     toJavaFXString(l.head, buffer);
                 }
                 buffer.append(')');
             }
         }
     }
 
     public String location (Symbol sym, Type site) {
         while ((sym.owner.flags() & BLOCK) != 0 ||
                 syms.isRunMethod(sym.owner))
             sym = sym.owner;
         return sym.location(site, this);
     }
 }
