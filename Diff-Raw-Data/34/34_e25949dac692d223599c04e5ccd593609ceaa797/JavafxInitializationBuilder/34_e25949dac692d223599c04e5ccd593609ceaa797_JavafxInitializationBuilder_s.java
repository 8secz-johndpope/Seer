 /*
  * Copyright 1999-2007 Sun Microsystems, Inc.  All Rights Reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *
  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.  Sun designates this
  * particular file as subject to the "Classpath" exception as provided
  * by Sun in the LICENSE file that accompanied this code.
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
 package com.sun.tools.javafx.comp;
 
 import com.sun.tools.javac.code.*;
 import com.sun.tools.javac.code.Scope.Entry;
 import com.sun.tools.javac.code.Symbol.ClassSymbol;
 import com.sun.tools.javac.code.Symbol.MethodSymbol;
 import com.sun.tools.javac.code.Symbol.VarSymbol;
 import com.sun.tools.javac.code.Type.MethodType;
 import com.sun.tools.javac.tree.JCTree;
 import com.sun.tools.javac.tree.JCTree.*;
 import com.sun.tools.javac.util.*;
 import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
 import com.sun.tools.javafx.code.JavafxSymtab;
 import static com.sun.tools.javafx.comp.JavafxDefs.*;
 import com.sun.tools.javafx.comp.JavafxTypeMorpher.VarMorphInfo;
 import com.sun.tools.javafx.tree.*;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 public class JavafxInitializationBuilder {
     protected static final Context.Key<JavafxInitializationBuilder> javafxInitializationBuilderKey =
         new Context.Key<JavafxInitializationBuilder>();
 
     private final JavafxDefs defs;
     private final JavafxTreeMaker make;
     public final Name.Table names;
     private final JavafxToJava toJava;
     private final JavafxSymtab syms;
     private final JavafxTypeMorpher typeMorpher;
     
     private final Name addChangeListenerName;
     private final Name changeListenerInterfaceName;
     private final Name sequenceChangeListenerInterfaceName;
     private static final String initHelperClassName = "com.sun.javafx.runtime.InitHelper";
     private final Name locationName;
     private final Name setDefaultsName;
     final Name userInitName;
     private final Name getNumFieldsName;
     private final Name initHelperName;
     private final Name getPreviousValueName;
     private static final String assertNonNullName = "assertNonNull";
     private static final String addName = "add";
     private final Name initializeNonSyntheticName;
     private final Name onChangeArgName;
     private static final String fullLocationName = "com.sun.javafx.runtime.location.Location";
     private static final String addDependenciesName = "addDependencies";
     Name outerAccessorName;
     Name outerAccessorFieldName;
     
     private Map<ClassSymbol, JFXClassDeclaration> fxClasses;
     Map<ClassSymbol, java.util.List<Symbol>> fxClassAttributes;
 
     public static JavafxInitializationBuilder instance(Context context) {
         JavafxInitializationBuilder instance = context.get(javafxInitializationBuilderKey);
         if (instance == null)
             instance = new JavafxInitializationBuilder(context);
         return instance;
     }
 
     protected JavafxInitializationBuilder(Context context) {
         context.put(javafxInitializationBuilderKey, this);
 
         defs = JavafxDefs.instance(context);
         make = (JavafxTreeMaker)JavafxTreeMaker.instance(context);
         names = Name.Table.instance(context);
         toJava = JavafxToJava.instance(context);
         syms = (JavafxSymtab)(JavafxSymtab.instance(context));
         typeMorpher = JavafxTypeMorpher.instance(context);
         
         addChangeListenerName = names.fromString("addChangeListener");
         changeListenerInterfaceName = names.fromString(JavafxTypeMorpher.locationPackageName + "ChangeListener");
         sequenceChangeListenerInterfaceName = names.fromString(JavafxTypeMorpher.locationPackageName + "SequenceChangeListener");
         locationName = names.fromString("location");
         setDefaultsName = names.fromString("setDefaults$");
         userInitName = names.fromString("userInit$");
         getNumFieldsName = names.fromString("getNumFields$");
         initHelperName = names.fromString("initHelper$");
         getPreviousValueName = names.fromString("getPreviousValue");
         initializeNonSyntheticName = names.fromString("initialize");
         onChangeArgName = names.fromString("$location");
         outerAccessorName = names.fromString("accessOuter$");
         outerAccessorFieldName = names.fromString("accessOuterField$");
     }
     
     static class TranslatedAttributeInfo {
         private final JFXVar attribute;
         private final Type elemType;
         final JCExpression initExpr;
         final List<JFXAbstractOnChange> onChanges;
         final List<JCExpression> args;
         TranslatedAttributeInfo(JFXVar attribute, Type elemType,
                 JCExpression initExpr, final List<JCExpression> args, List<JFXAbstractOnChange> onChanges) {
             this.attribute = attribute;
             this.initExpr = initExpr;
             this.onChanges = onChanges;
             this.elemType = elemType;
             this.args = args;
         }
         
         Name name() { return attribute.getName(); }
         Type type() { return attribute.type; }
         Type elemType() { return elemType; }
         DiagnosticPosition diagPos() { return attribute.pos(); }
     }
   
     JCStatement makeStaticChangeListenerCall(TranslatedAttributeInfo info) {
 	// TBD static attribute triggers
 	return null;
     }
     /**
      * Non-destructive creation of "on change" change listener set-up call.
      */
     JCStatement makeChangeListenerCall(TranslatedAttributeInfo info) {
         JFXOnReplace onReplace = null;
         JFXOnReplaceElement onReplaceElement = null;
         JFXOnInsertElement onInsertElement = null;
         JFXOnDeleteElement onDeleteElement = null;
         DiagnosticPosition diagPos = info.diagPos();
         ListBuffer<JCTree> defs = ListBuffer.<JCTree>lb();
         
         if (!info.onChanges.nonEmpty()) {
             return null;
         } 
         
         for (JFXAbstractOnChange onc : info.onChanges) {
             switch (onc.getTag()) {
                 case JavafxTag.ON_REPLACE:
                     onReplace = (JFXOnReplace)onc;
                     break;
                 case JavafxTag.ON_REPLACE_ELEMENT:
                     onReplaceElement = (JFXOnReplaceElement)onc;
                     break;
                 case JavafxTag.ON_INSERT_ELEMENT:
                     onInsertElement = (JFXOnInsertElement)onc;
                     break;
                 case JavafxTag.ON_DELETE_ELEMENT:
                     onDeleteElement = (JFXOnDeleteElement)onc;
                     break;
             }
         }
        
         JCExpression changeListener;
         List<JCExpression> emptyTypeArgs = List.nil();
         if (onReplaceElement != null || onInsertElement != null || onDeleteElement != null) {
             changeListener = make.at(diagPos).Identifier(sequenceChangeListenerInterfaceName);
             changeListener = make.at(diagPos).TypeApply(changeListener, 
                     List.<JCExpression>of(toJava.makeTypeTree(info.elemType(), diagPos)));
             defs.append(makeSequenceChangeListenerMethod(
                     diagPos, 
                     onReplaceElement, 
                     "onReplace", 
                     List.<JCVariableDecl>of(
                         makeIndexParam(diagPos, onReplaceElement), 
                         makeParam(diagPos, info.elemType(),
                                   onReplaceElement == null ? null : onReplaceElement.getOldValue(),
                                   "$oldValue$"),
                         makeParam(diagPos, info.elemType(), null, "$newValue$")), 
                     TypeTags.VOID));
             defs.append(makeSequenceChangeListenerMethod(
                     diagPos, 
                     onInsertElement, 
                     "onInsert", 
                     List.<JCVariableDecl>of(
                         makeIndexParam(diagPos, onInsertElement), 
                         makeParam(diagPos, info.elemType(),
                                   onInsertElement == null ? null : onInsertElement.getOldValue(),
                                   "$newValue$")), 
                     TypeTags.VOID));
             defs.append(makeSequenceChangeListenerMethod(
                     diagPos, 
                     onDeleteElement, 
                     "onDelete", 
                     List.<JCVariableDecl>of(
                         makeIndexParam(diagPos, onDeleteElement), 
                         makeParam(diagPos, info.elemType(),
                                   onDeleteElement == null ? null : onDeleteElement.getOldValue(),
                                   "$oldValue$")), 
                     TypeTags.VOID));
         }
         else {
             changeListener = make.at(diagPos).Identifier(changeListenerInterfaceName);
             defs.append(makeOnReplaceChangeListenerMethod(diagPos, onReplace));
         }
 
         JCNewClass anonymousChangeListener = make.NewClass(
                 null, 
                 emptyTypeArgs, 
                 changeListener, 
                 List.<JCExpression>nil(), 
                 make.at(diagPos).AnonymousClassDef(make.Modifiers(0L), defs.toList()));
 
         JCExpression attrRef = toJava.makeAttributeAccess(diagPos, info.attribute);
         JCFieldAccess tmpSelect = make.at(diagPos).Select(attrRef, addChangeListenerName);
 
         List<JCExpression> args = List.<JCExpression>of(anonymousChangeListener);
         return make.at(diagPos).Exec(make.at(diagPos).Apply(emptyTypeArgs, tmpSelect, args));
     }
     
     private JCVariableDecl makeParam(DiagnosticPosition diagPos, Type type, JFXVar var, String nameDefault) {
         Name name;
         if (var != null) {
             name = var.getName();
             diagPos = var.pos();
         } else {
             name = names.fromString(nameDefault);
         }
         long flags = Flags.PARAMETER;
         if (var != null && var.mods != null) {
             flags |= var.mods.flags;
         }
         return make.at(diagPos).VarDef(
                 make.Modifiers(flags),
                 name,
                 toJava.makeTypeTree(type, diagPos),
                 null);
         
     }
 
     private JCVariableDecl makeIndexParam(DiagnosticPosition diagPos, JFXAbstractOnChange onChange) {
         JCVariableDecl ret = makeParam(diagPos, syms.intType, onChange == null ? null : onChange.getIndex(), "$index$");
         ret.mods.flags |= Flags.FINAL;
         return ret;
     }
 
     /**
      * construct a change listener method for sequence triggers.  Insert in a listener anon class.
      *   void onInsert(...);
      *   void on Delete(...); ...
      */
     private JCMethodDecl makeSequenceChangeListenerMethod(
             DiagnosticPosition diagPos,
             JFXAbstractOnChange onChange, 
             String methodName, 
             List<JCVariableDecl> args, 
             int returnTypeTag) {
         return makeChangeListenerMethod(
              diagPos,
              onChange, 
              ListBuffer.<JCStatement>lb(),
              methodName, 
              args, 
              returnTypeTag);
     }
     
     /**
      * construct a change listener method for insertion in a listener anon class.
      *   void onReplace(...); ...
      */
     private JCMethodDecl makeOnReplaceChangeListenerMethod(
             DiagnosticPosition diagPos,
             JFXOnReplace onReplace) {
         List<JCVariableDecl> onChangeArgs = List.<JCVariableDecl>nil();
         onChangeArgs = onChangeArgs.append(make.VarDef(make.Modifiers(0L), onChangeArgName, make.Identifier(fullLocationName), null));
         ListBuffer<JCStatement> setUpStmts = ListBuffer.<JCStatement>lb();
         if (onReplace != null && onReplace.getOldValue() != null) {
             // an oldValue variable was specificied, create it.  For example:
             //   int oldValue = ((IntLocation) location).getPreviousValue();
             JFXVar oldValue = onReplace.getOldValue();
             VarMorphInfo vmi = typeMorpher.varMorphInfo(oldValue.sym);
             Type locationType = vmi.getMorphedType();
 
             setUpStmts.append( 
                     make.at(diagPos).VarDef(
                         make.Modifiers(0L), 
                         oldValue.getName(), 
                         toJava.makeTypeTree(vmi.getRealType(), diagPos, false), 
                         make.at(diagPos).Apply(
                             List.<JCExpression>nil(),       // no type args
                             make.at(diagPos).Select(
                                 make.at(diagPos).TypeCast(   // cast to the specific Location type -- eg: (IntLocation) $location
                                     toJava.makeTypeTree(locationType, diagPos, false), 
                                     make.at(diagPos).Ident(onChangeArgName)),
                                 getPreviousValueName),
                             List.<JCExpression>nil()        // no args
                             )));
         }
          return makeChangeListenerMethod(
              diagPos,
              onReplace, 
              setUpStmts,
              "onChange", 
              onChangeArgs, 
              TypeTags.BOOLEAN);
     }
     
     /**
      * construct a change listener method for insertion in a listener anon class.
      *   boolean onChange();
      *   void onInsert(...);
      *   void on Delete(...); ...
      */
     private JCMethodDecl makeChangeListenerMethod(
             DiagnosticPosition diagPos,
             JFXAbstractOnChange onChange, 
             ListBuffer<JCStatement> prefixStmts,
             String methodName, 
             List<JCVariableDecl> args, 
             int returnTypeTag) {
         ListBuffer<JCStatement> ocMethStmts = ListBuffer.<JCStatement>lb();
         ocMethStmts.appendList(prefixStmts);
         if (onChange != null) {
             diagPos = onChange.pos();
             ocMethStmts.appendList(onChange.getBody().getStatements());
         }
         if (returnTypeTag == TypeTags.BOOLEAN) {
             ocMethStmts.append(make.at(diagPos).Return(make.at(diagPos).Literal(TypeTags.BOOLEAN, 1)));
         }
 
         return make.at(diagPos).MethodDef(
                 make.at(diagPos).Modifiers(Flags.PUBLIC), 
                 names.fromString(methodName), 
                 make.at(diagPos).TypeIdent(returnTypeTag), 
                 List.<JCTypeParameter>nil(), 
                 args,
                 List.<JCExpression>nil(), 
                 make.at(diagPos).Block(0L, ocMethStmts.toList()), 
                 null);
     }
 
     /**
      * Return the generated interface name corresponding to the class
      * */
     Name interfaceName(JFXClassDeclaration cDecl) {
         Name name = cDecl.getName();
         if (cDecl.generateClassOnly())
             return name;
         return names.fromString(name.toString() + interfaceSuffix);
     }
     
     /**
      * Hold the result of analyzing the class.
      * */
     static class JavafxClassModel {
         final Name interfaceName;
         final ListBuffer<JCExpression> interfaces;
         final List<JCTree> iDefinitions;
         final List<JCTree> additionalClassMembers;
         final List<JCExpression> additionalImports;
   
         JavafxClassModel(
                 Name interfaceName,
                 ListBuffer<JCExpression> interfaces,
                 List<JCTree> iDefinitions,
                 ListBuffer<JCTree> addedClassMembers,
                 ListBuffer<JCExpression> additionalImports) {
             this.interfaceName = interfaceName;
             this.interfaces = interfaces;
             this.iDefinitions = iDefinitions;
             this.additionalClassMembers = addedClassMembers.toList();
             this.additionalImports = additionalImports.toList();
         }
     }
 
     /**
      * Analyze the class.
      * 
      * Determine what methods will be needed to access attributes.
      * Determine what methods will be needed to proxy to the static implementations of functions.
      * Determine what other misc fields and methods will be needed.
      * Create the corresponding interface.
      * 
      * Return all this as a JavafxClassModel for use in translation.
      * */
    JavafxClassModel createJFXClassModel(JFXClassDeclaration cDecl, List<TranslatedAttributeInfo> translatedAttrInfo) {
         boolean classIsFinal = (cDecl.getModifiers().flags & Flags.FINAL) != 0;
         boolean classOnly = cDecl.generateClassOnly();
         
         CollectAttributeAndMethods collection = new CollectAttributeAndMethods(cDecl.sym);        
         java.util.List<ClassSymbol> baseClasses = collection.baseClasses;
         java.util.List<Symbol> attributes = collection.attributes;
 
         addFxClassAttributes(cDecl.sym, attributes);
         
         ListBuffer<JCExpression> implementing = new ListBuffer<JCExpression>();
         implementing.append(make.Identifier(fxObjectString));
 
         // Add import statements for all the base classes and basClass $Intf(s).
         // There might be references to them when the methods/attributes are rolled up.
         ListBuffer<JCExpression> additionalImports = new ListBuffer<JCExpression>();
         for (ClassSymbol baseClass : baseClasses) {
             if (!baseClass.name.toString().endsWith(interfaceSuffix) && 
                     baseClass.fullname != names.fromString(fxObjectString) &&
                     isJFXClass(baseClass) && baseClass.type != null) {
                 implementing.append(toJava.makeTypeTree(baseClass.type, cDecl.pos(), true));
                 if (baseClass.type != null && baseClass.type.tsym != null &&
                         baseClass.type.tsym.packge() != syms.unnamedPackage) {    // Work around javac bug. the visitImport of Attr 
                                                                                   // is casting to JCFieldAcces, but if you have imported an
                                                                                   // JCIdent only a ClastCastException is thrown.
                     additionalImports.append(toJava.makeTypeTree(baseClass.type, cDecl.pos(), false));
                     additionalImports.append(toJava.makeTypeTree(baseClass.type, cDecl.pos(), true));
                 }
             }
         }
 
         ListBuffer<JCTree> cDefinitions = ListBuffer.lb();  // additional class members needed
         cDefinitions.append( makeSetDefaultsMethod(cDecl, baseClasses,  translatedAttrInfo) );
         cDefinitions.append( make.Block(Flags.STATIC, makeStaticSetDefaultsMethod(cDecl, translatedAttrInfo) ));
 
         ListBuffer<JCTree> iDefinitions = new ListBuffer<JCTree>();
         ListBuffer<AttributeWrapper> attrInfos = new ListBuffer<AttributeWrapper>();
         for (Symbol attrSym : attributes) {
             if (attrSym.kind == Kinds.VAR) {
                 VarSymbol varSym = (VarSymbol)attrSym;
                 VarMorphInfo vmi = typeMorpher.varMorphInfo(varSym);
                 attrInfos.append(new AttributeWrapper(vmi.getMorphedType(), varSym.name));
             }
             else {
                 if (attrSym.kind != Kinds.MTH) {
                     throw new AssertionError("Invalid attribute type collected");
                 }
                 
                 MethodSymbol methodSym = (MethodSymbol)attrSym;
                 attrInfos.append(new AttributeWrapper(((MethodType)methodSym.type).restype,
                         names.fromString(methodSym.name.toString().substring(attributeGetMethodNamePrefix.length()))));
             }
         }
         
         addInterfaceAttributeMethods(iDefinitions, attrInfos);
         addClassAttributeMethods(cDecl, attrInfos, cDefinitions, attributes.size() );
 
         Name interfaceName = classOnly ? null : interfaceName(cDecl);
 
         for (boolean bound = toJava.generateBoundFunctions;  ; bound = false) {
 
             // add interface methods for each method defined in this class
             for (MethodSymbol mth : collection.needInterfaceMethods) {
                     if (mth.owner == cDecl.sym) {
                        iDefinitions.append(makeMethod(cDecl, mth, null, bound));
                     }
             }
 
             // add proxies which redirect to the static implementation for every concrete method
             for (MethodSymbol mth : collection.needDispatchMethods) {
                     if ((!classIsFinal && (mth.flags() & (Flags.STATIC)) == 0L) || mth.owner != cDecl.sym) { //TODO: this test is wrong
                        cDefinitions.append(makeMethod(cDecl, mth, makeDispatchBody(cDecl, mth, bound, false), bound));
                     }
             }
             
             for (MethodSymbol mth : collection.needStaticDispatchMethods) {
                     if ((!classIsFinal && (mth.flags() & (Flags.STATIC)) == 0L) || mth.owner != cDecl.sym) {  //TODO: this test is wrong
                        cDefinitions.append(makeMethod(cDecl, mth, makeDispatchBody(cDecl, mth, bound, true), bound));
                     }
             }
             
             if (!bound) {
                 break;
             }
         }
 
         addInterfaceOuterAccessorMethod(iDefinitions, cDecl);
         addClassOuterAccessorMethod(cDefinitions, cDecl);
 
         for (List<JCExpression> l = cDecl.getImplementing(); l.nonEmpty(); l = l.tail) {
             implementing.append(toJava.makeTypeTree(l.head.type, null));
         }
 
         return new JavafxClassModel(interfaceName, implementing, iDefinitions.toList(), cDefinitions, additionalImports);
     }
     
     // Add the methods and field for accessing the outer members. Also add a constructor with an extra parameter to handle the instantiation of the classes that access outer members
     private void addClassOuterAccessorMethod( ListBuffer<JCTree> members, JFXClassDeclaration cdecl) {
         if (cdecl.sym != null && toJava.hasOuters.contains(cdecl.sym)) {
             Symbol typeOwner = cdecl.sym.owner;
             while (typeOwner != null && typeOwner.kind != Kinds.TYP) {
                 typeOwner = typeOwner.owner;
             }
             
             if (typeOwner != null) {
                 ClassSymbol returnSym = typeMorpher.reader.enterClass(names.fromString(typeOwner.type.toString() + interfaceSuffix));
                 // Create the field to store the outer instance reference
                 JCVariableDecl accessorField = make.VarDef(make.Modifiers(Flags.PUBLIC), outerAccessorFieldName, make.Ident(returnSym), null);
                 VarSymbol vs = new VarSymbol(Flags.PUBLIC, outerAccessorFieldName, returnSym.type, cdecl.sym);
                 accessorField.type = returnSym.type;
                 accessorField.sym = vs;
 
                 // Create the interface method with it's type(s) and symbol(s)
                 ListBuffer<JCStatement> mStats = new ListBuffer<JCStatement>();
                 JCIdent retIdent = make.Ident(vs);
                 JCReturn retRet = make.Return(retIdent);
                 retRet.type = vs.type;
                 mStats.append(retRet);
 
                 JCMethodDecl accessorMethod = make.MethodDef(make.Modifiers(Flags.PUBLIC), outerAccessorName, make.Ident(returnSym), List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(),
                         List.<JCExpression>nil(), make.Block(0L, mStats.toList()), null);
 
                 MethodType mt = new MethodType(List.<Type>nil(), returnSym.type, List.<Type>nil(), returnSym);
                 accessorMethod.type = mt;
                 MethodSymbol ms = new MethodSymbol(Flags.PUBLIC, outerAccessorName, returnSym.type, returnSym);
                 accessorMethod.sym = ms;
                 members.append(accessorMethod);
                 members.append(accessorField);
 
                 // Now add the construcotr taking the outer instance reference
                 JCVariableDecl accessorParam = make.VarDef(make.Modifiers(0L), outerAccessorFieldName, make.Ident(returnSym), null);
                 VarSymbol vs1 = new VarSymbol(0L, outerAccessorFieldName, returnSym.type, cdecl.sym);
                 accessorParam.type = returnSym.type;
                 accessorParam.sym = vs1;
 
                 ListBuffer<JCStatement> cStats = new ListBuffer<JCStatement>();
                 JCIdent cSelected = make.Ident(names._this);
                 cSelected.type = returnSym.type;
                 cSelected.sym = returnSym;
 
                 JCFieldAccess cSelect = make.Select(cSelected, outerAccessorFieldName);
                 cSelect.sym = accessorField.sym;
                 cSelect.type = accessorField.type;
 
                 JCIdent paramIdent = make.Ident(outerAccessorFieldName);
                 paramIdent.type = accessorParam.type;
                 paramIdent.sym = accessorParam.sym;
 
                 JCAssign assignStat = make.Assign(cSelect, paramIdent);
                 assignStat.type = returnSym.type;
 
                 JCStatement assignWrapper = make.Exec(assignStat);
                 assignWrapper.type = assignStat.type;
 
                 cStats.append(assignWrapper);
 
                 JCMethodDecl ctor = make.MethodDef(make.Modifiers(Flags.PUBLIC), names.init, make.TypeIdent(TypeTags.VOID), List.<JCTypeParameter>nil(), List.<JCVariableDecl>of(accessorParam),
                         List.<JCExpression>nil(), make.Block(0L, cStats.toList()), null);
 
                 MethodType ct = new MethodType(List.<Type>of(accessorParam.type), returnSym.type, List.<Type>nil(), returnSym);
                 accessorMethod.type = ct;
                 MethodSymbol cs = new MethodSymbol(Flags.PUBLIC, outerAccessorName, returnSym.type, returnSym);
                 accessorMethod.sym = cs;
                 members.append(ctor);
             }
         }
     }
 
     // Add the methods for accessing the outer members.
     private void addInterfaceOuterAccessorMethod(ListBuffer<JCTree> iDefinitions, JFXClassDeclaration cdecl) {
         if (cdecl.sym != null && toJava.hasOuters.contains(cdecl.sym)) {
             Symbol typeOwner = cdecl.sym.owner;
             while (typeOwner != null && typeOwner.kind != Kinds.TYP) {
                 typeOwner = typeOwner.owner;
             }
 
             if (typeOwner != null) {
                 ClassSymbol returnSym = typeMorpher.reader.enterClass(names.fromString(typeOwner.type.toString() + interfaceSuffix));
                 JCMethodDecl accessorMethod = make.MethodDef(make.Modifiers(Flags.PUBLIC), outerAccessorName, make.Ident(returnSym), List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(),
                         List.<JCExpression>nil(), null, null);
 
                 MethodType mt = new MethodType(List.<Type>nil(), returnSym.type, List.<Type>nil(), returnSym);
                 accessorMethod.type = mt;
                 MethodSymbol ms = new MethodSymbol(Flags.PUBLIC, outerAccessorName, returnSym.type, returnSym);
                 accessorMethod.sym = ms;
 
                 iDefinitions.append(accessorMethod);
             }
         }
     }
 
     /**
      * Make a method from a MethodSymbol and an optional method body.
      * Make a bound version if "isBound" is set.
      */
     private JCMethodDecl makeMethod(DiagnosticPosition diagPos, MethodSymbol mth, JCBlock mthBody, boolean isBound) {
         // build the parameter list
         ListBuffer<JCVariableDecl> params = ListBuffer.lb();
         for (VarSymbol vsym : mth.getParameters()) {
             Type vtype = vsym.asType();
             if (isBound) {
                 VarMorphInfo vmi = typeMorpher.varMorphInfo(vsym);
                 vtype = vmi.getMorphedType();
             }
             params.append(make.VarDef(
                     make.Modifiers(0L), 
                     vsym.name, 
                     toJava.makeTypeTree(vtype, diagPos), 
                     null // no initial value
                     ));
         }
         
         // make the method
         return make.at(diagPos).MethodDef(
                         make.Modifiers(Flags.PUBLIC | (mthBody==null? Flags.ABSTRACT : 0L)), 
                         toJava.functionName(mth, false, isBound), 
                         toJava.makeReturnTypeTree(diagPos, mth, isBound), 
                         List.<JCTypeParameter>nil(), 
                         params.toList(), 
                         List.<JCExpression>nil(), 
                         mthBody, 
                         null);
     }
     
     /**
      * Make a method body which redirects to the actual implementation in a static method of the defining class.
      */
     private JCBlock makeDispatchBody(DiagnosticPosition diagPos, MethodSymbol mth, boolean isBound, boolean isStatic) {
         ListBuffer<JCExpression> args = ListBuffer.lb();
         if (!isStatic) {
             // Add the this argument, so the static implementation method is invoked
             args.append(make.Ident(names._this));
         }
         for (VarSymbol var : mth.params) {
             args.append(make.Ident(var.name));
         }
         String receiver = mth.owner.name.toString();
         JCExpression expr = toJava.callExpression(diagPos, make.Identifier(receiver), toJava.functionName(mth, !isStatic, isBound), args);
         JCStatement statement = (mth.getReturnType() == syms.voidType) ? make.Exec(expr) : make.Return(expr);
         return make.at(diagPos).Block(0L, List.<JCStatement>of(statement));
      }
     
     private void addInterfaceAttributeMethods(ListBuffer<JCTree> idefs, ListBuffer<AttributeWrapper> attrInfos) {
         for (AttributeWrapper attrInfo : attrInfos) {            
             idefs.append(make.MethodDef(
                     make.Modifiers(Flags.PUBLIC | Flags.ABSTRACT),
                     names.fromString(attributeGetMethodNamePrefix + attrInfo.name.toString()),
                     toJava.makeTypeTree(attrInfo.type, null),
                     List.<JCTypeParameter>nil(), 
                     List.<JCVariableDecl>nil(), 
                     List.<JCExpression>nil(), 
                     null, null));
             List<JCVariableDecl> locationVarDeclList = List.<JCVariableDecl>nil();
                 locationVarDeclList = locationVarDeclList.append(make.VarDef(make.Modifiers(0L),
                     locationName, toJava.makeTypeTree(attrInfo.type, null), null));
                 
             idefs.append(make.MethodDef(
                     make.Modifiers(Flags.PUBLIC | Flags.ABSTRACT),
                     names.fromString(attributeInitMethodNamePrefix + attrInfo.name.toString()),
                     toJava.makeTypeTree(syms.voidType, null),
                     List.<JCTypeParameter>nil(), 
                     locationVarDeclList, 
                     List.<JCExpression>nil(), 
                     null, null));
         }
     }
 
     private void addClassAttributeMethods(JFXClassDeclaration cDecl, ListBuffer<AttributeWrapper> attrInfos, 
                                                                                ListBuffer<JCTree> members,
                                                                                int numFields) {
         boolean classIsFinal = (cDecl.getModifiers().flags & Flags.FINAL) != 0;
         for (AttributeWrapper attrInfo : attrInfos) { 
 // TODO: Add attributes gotten from interface introspection.
             List<JCStatement> stats = List.<JCStatement>nil();
             
             // Add the return stastement for the attribute
             JCBlock statBlock = make.Block(0L, stats);
             
             JCReturn returnStat = make.Return(make.Ident(attrInfo.name));
             stats = stats.append(returnStat);
             statBlock.stats = stats;
             
             // Add the method for this class' attributes
             members.append(make.MethodDef(
                     make.Modifiers(Flags.PUBLIC),
                     names.fromString(attributeGetMethodNamePrefix + attrInfo.name.toString()),
                     toJava.makeTypeTree(attrInfo.type, null),
                     List.<JCTypeParameter>nil(), 
                     List.<JCVariableDecl>nil(), 
                     List.<JCExpression>nil(), 
                     statBlock, null));
 
             // Add the init$ method
             // Add the InitHelper.assertNonNull(...) call
             List<JCStatement> initBlockStats = List.<JCStatement>nil();
             List<JCExpression> initAssertArgs = List.<JCExpression>nil();
             initAssertArgs = initAssertArgs.append(make.Ident(attrInfo.name));
             initAssertArgs = initAssertArgs.append(make.Literal( cDecl.getName().toString() + "." + attrInfo.name.toString() ));
             
             initBlockStats = initBlockStats.append(toJava.callStatement(cDecl.pos(), make.Identifier(initHelperClassName), assertNonNullName, initAssertArgs));
 
             // Add the initHelper$.add(...) call
             List<JCExpression> initAddArgs = List.<JCExpression>nil();
             initAddArgs = initAddArgs.append(make.Assign(make.Ident(attrInfo.name), make.Ident(locationName)));
             
             initBlockStats = initBlockStats.append(toJava.callStatement(cDecl.pos(), make.Ident(initHelperName), addName, initAddArgs));
             
             JCBlock initBlock = make.Block(0L, initBlockStats);
             List<JCVariableDecl> locationVarDeclList = List.<JCVariableDecl>nil();
             locationVarDeclList = locationVarDeclList.append(
                     make.VarDef(make.Modifiers(0L),
                     locationName,
                     toJava.makeTypeTree(attrInfo.type, null),
                     null));
             members.append(make.MethodDef(
                     make.Modifiers(Flags.PUBLIC),
                     names.fromString(attributeInitMethodNamePrefix + attrInfo.name.toString()),
                     toJava.makeTypeTree(syms.voidType, null),
                     List.<JCTypeParameter>nil(), 
                     locationVarDeclList, 
                     List.<JCExpression>nil(), 
                     initBlock, null));
         }
         
         // Add the getNumFields$ method
         List<JCStatement> numFieldsStats = List.<JCStatement>nil();
         numFieldsStats = numFieldsStats.append(make.Return(make.Literal(numFields)));
         
         JCBlock numFieldsBlock = make.Block(0L, numFieldsStats);
         members.append(make.MethodDef(
                 make.Modifiers(classIsFinal? Flags.PUBLIC  : Flags.PUBLIC | Flags.STATIC),
                 getNumFieldsName,
                 toJava.makeTypeTree(syms.intType, null),
                 List.<JCTypeParameter>nil(), 
                 List.<JCVariableDecl>nil(), 
                 List.<JCExpression>nil(), 
                 numFieldsBlock, null));
 
         // Add the InitHelper field
         List<JCExpression> ncArgs = List.<JCExpression>nil();
         ncArgs = ncArgs.append( make.Literal( attrInfos.size() ) );
         
         JCNewClass newIHClass = make.NewClass(null, List.<JCExpression>nil(), make.Identifier(initHelperClassName), ncArgs, null);
         
         members.append(make.VarDef(make.Modifiers(Flags.PRIVATE),
                 initHelperName, make.Identifier(initHelperClassName), newIHClass));
         
         // Add the initialize$ method
         List<JCStatement> initializeStats = List.<JCStatement>nil();
 
         // Add calls to do the the default value initialization and user init code (validation for example.)
         initializeStats = initializeStats.append(toJava.callStatement(
                 cDecl.pos(), 
                 make.Ident(classIsFinal? names._this : cDecl.getName()),
                 setDefaultsName, 
                 make.TypeCast(make.Ident(interfaceName(cDecl)), make.Ident(names._this))));
         initializeStats = initializeStats.append(toJava.callStatement(
                 cDecl.pos(), 
                 make.Ident(classIsFinal? names._this : cDecl.getName()),
                 userInitName, 
                 make.TypeCast(make.Ident(interfaceName(cDecl)), make.Ident(names._this))));
         
         // Add a call to initialize the attributes using the initHelper$.initialize();
         initializeStats = initializeStats.append(toJava.callStatement(cDecl.pos(), make.Ident(initHelperName), 
             initializeNonSyntheticName));
         
         // Set the initHelper = null;
         initializeStats = initializeStats.append(make.Exec(make.Assign(make.Ident(initHelperName), make.Literal(TypeTags.BOT, null))));
         
         JCBlock initializeBlock = make.Block(0L, initializeStats);
         members.append(make.MethodDef(
                 make.Modifiers(Flags.PUBLIC),
                 defs.initializeName,
                 toJava.makeTypeTree(syms.voidType, null),
                 List.<JCTypeParameter>nil(), 
                 List.<JCVariableDecl>nil(), 
                 List.<JCExpression>nil(), 
                 initializeBlock, null));
     }
 
     /**
      * Construct the SetDefaults method
      * */
     private JCMethodDecl makeSetDefaultsMethod(JFXClassDeclaration cDecl, java.util.List<ClassSymbol> baseClasses, 
                                                                                             List<TranslatedAttributeInfo> translatedAttrInfo) {
         boolean classIsFinal =(cDecl.getModifiers().flags & Flags.FINAL) != 0;
         // Add the initialization of this class' attributes
         List<JCStatement> setDefStats = addSetDefaultAttributeInitialization(translatedAttrInfo, cDecl);
         if (setDefStats.nonEmpty()) {
             setDefStats = setDefStats.appendList(addSetDefaultAttributeDependencies(translatedAttrInfo, cDecl));
         }
         
         // call the superclasses SetDefaults
         for (ClassSymbol csym : baseClasses) {
             if (isJFXClass(csym)) {
                 String className = csym.fullname.toString();
                 if (className.endsWith(interfaceSuffix)) {
                     className = className.substring(0, className.length() - interfaceSuffix.length());
                 }
                 
                 List<JCExpression> args1 = List.<JCExpression>nil();
                 args1 = args1.append(make.Ident(defs.receiverName));
                 setDefStats = setDefStats.append(toJava.callStatement(cDecl.pos(), make.Identifier(className), setDefaultsName, args1));
             }
         }
         
         // add any change listeners (if there are any triggers)
         for (TranslatedAttributeInfo info : translatedAttrInfo) {
             JCStatement stat = makeChangeListenerCall(info);
             if (stat != null) {
                 setDefStats = setDefStats.append(stat);
             }
         }
                 
         return make.MethodDef(
                 make.Modifiers(classIsFinal? Flags.PUBLIC  : Flags.PUBLIC | Flags.STATIC),
                 setDefaultsName,
                 toJava.makeTypeTree(syms.voidType, null),
                 List.<JCTypeParameter>nil(), 
                 List.<JCVariableDecl>of( toJava.makeReceiverParam(cDecl) ), 
                 List.<JCExpression>nil(), 
                 make.Block(0L, setDefStats), 
                 null);
     }
 
     /**
      * Construct the SetDefaults method
      * */
      private List<JCStatement> makeStaticSetDefaultsMethod(JFXClassDeclaration cDecl, 
                                                                                             List<TranslatedAttributeInfo> translatedAttrInfo) {
         boolean classIsFinal =(cDecl.getModifiers().flags & Flags.FINAL) != 0;
         // Add the initialization of this class' attributesa
         List<JCStatement> setDefStats = addStaticSetDefaultAttributeInitialization(translatedAttrInfo, cDecl);
         if (setDefStats.nonEmpty()) {
             // TBD ?
             //setDefStats = setDefStats.appendList(addSetDefaultAttributeDependencies(translatedAttrInfo, cDecl));
         }
         // add any change listeners (if there are any triggers)
         for (TranslatedAttributeInfo info : translatedAttrInfo) {
             JCStatement stat = makeStaticChangeListenerCall(info);
             if (stat != null) {
                 setDefStats = setDefStats.append(stat);
             }
         }
         return setDefStats;
     }
 
     private List<JCStatement> addStaticSetDefaultAttributeInitialization(List<TranslatedAttributeInfo> translatedAttrInfo, JFXClassDeclaration cdef) {
         List<JCStatement> ret = List.<JCStatement>nil();
         for (TranslatedAttributeInfo tai : translatedAttrInfo) {
             if (tai.attribute != null && tai.attribute.getTag() == JavafxTag.VAR_DEF && tai.attribute.pos != Position.NOPOS) {
 		if ((tai.attribute.getModifiers().flags & Flags.STATIC) == 0) {
 		    continue;
 		}
                 if (tai.attribute.sym != null && tai.attribute.sym.owner == cdef.sym) {
 		    ret = ret.append(make.Exec(make.Assign(make.Ident(tai.attribute.name), tai.initExpr)));
                 }
             }
         }
         return ret;
     }
 
     // Add the initialization of this class' attributes
     private List<JCStatement> addSetDefaultAttributeInitialization(List<TranslatedAttributeInfo> translatedAttrInfo, JFXClassDeclaration cdef) {
         List<JCStatement> ret = List.<JCStatement>nil();
         for (TranslatedAttributeInfo tai : translatedAttrInfo) {
             if (tai.attribute != null && tai.attribute.getTag() == JavafxTag.VAR_DEF && tai.attribute.pos != Position.NOPOS) {
 		if ((tai.attribute.getModifiers().flags & Flags.STATIC) != 0) {
 		    continue;
 		}
                 if (tai.attribute.sym != null && tai.attribute.sym.owner == cdef.sym) {
                     DiagnosticPosition diagPos = tai.attribute.pos();
                     JCExpression cond = make.at(diagPos).Binary(JCTree.EQ, 
                             toJava.makeAttributeAccess(cdef, tai.attribute), 
                             make.Literal(TypeTags.BOT, null));
                     
                     JCStatement thenStat = toJava.callStatement(diagPos, make.Ident(defs.receiverName), attributeInitMethodNamePrefix + tai.attribute.name.toString(), tai.initExpr);
                     JCIf defInitIf = make.If(cond, thenStat, null);
                     ret = ret.append(defInitIf);
                 }
             }
         }
         return ret;
     }
 
     // Add the initialization of this class' dependencies
    private  List<JCStatement> addSetDefaultAttributeDependencies(List<TranslatedAttributeInfo> attrInfo, JFXClassDeclaration cdef) {
         List<JCStatement> ret = List.<JCStatement>nil();
         for (TranslatedAttributeInfo tai : attrInfo) {
             if (tai.attribute != null && tai.attribute.getTag() == JavafxTag.VAR_DEF && tai.attribute.pos != Position.NOPOS &&
                     tai.args != null) {
                 if (tai.attribute.sym != null && tai.attribute.sym.owner == cdef.sym) {
                     ret = ret.append(toJava.callStatement(tai.attribute.pos(), 
                             toJava.makeAttributeAccess(cdef, tai.attribute),
                             addDependenciesName, tai.args));
                 }
             }
         }
         return ret;
     }
    
 
 /**
 * Walk the supertypes of this class, collecting the attributes and functions that need to be rolled up
 * into the class representation.
 * For source classes this means walking the explicit supertype in the AST.  For classes from class files,
 * this means looking at the interfaces and the parallel class.
 */
    class CollectAttributeAndMethods {
 
        java.util.List<Symbol> attributes = new java.util.ArrayList<Symbol>();
        java.util.List<MethodSymbol> needInterfaceMethods = new java.util.ArrayList<MethodSymbol>();
        java.util.List<MethodSymbol> needDispatchMethods = new java.util.ArrayList<MethodSymbol>();
        java.util.List<MethodSymbol> needStaticDispatchMethods = new java.util.ArrayList<MethodSymbol>();
        java.util.List<ClassSymbol> baseClasses = new java.util.ArrayList<ClassSymbol>();
 
        private Set<String> visitedAttributes = new HashSet<String>();
        private Set<String> visitedFunctions = new HashSet<String>();
        private java.util.List<ClassSymbol> classesToVisit = new java.util.ArrayList<ClassSymbol>();
        private Set<ClassSymbol> addedBaseClasses = new HashSet<ClassSymbol>();
         
        CollectAttributeAndMethods(ClassSymbol csym) {
            classesToVisit.add(csym);
            while (!classesToVisit.isEmpty()) {
                ClassSymbol cSym = classesToVisit.get(0);
                classesToVisit.remove(0);
                if (isJFXClass(cSym)) {
                    JFXClassDeclaration cDecl = fxClasses.get(cSym); // get the corresponding AST, null if from class file
                    if (cDecl == null) { 
                        // this is a JavaFX class from a class file
                        if ((cSym.flags_field & Flags.INTERFACE) == 0 && cSym.members() != null) {
                            // we want to base this solely on (non-empty) implementation (not interfaces)
                            for (Entry e = cSym.members().elems; e != null && e.sym != null; e = e.sibling) {
                                if (e.sym.kind == Kinds.MTH) {
                                    processMethodFromClassFile((MethodSymbol) e.sym);
                                }
                            }
                        }
                        // now visit the super-types (both interface and class forms)
                        for (Type supertype : cSym.getInterfaces()) {
                            ClassSymbol iSym = (ClassSymbol) supertype.tsym;
                            addToVisitList(iSym);
                            String iName = iSym.fullname.toString();
                            if (iName.endsWith(defs.interfaceSuffix)) {
                                // enter and visit the class parallel to this interface
                                String sName = iName.substring(0, iName.length() - defs.interfaceSuffix.length());
                                ClassSymbol sSym = typeMorpher.reader.enterClass(names.fromString(sName));
                                addToVisitList(sSym);
                            }
                        }
                    } else {
                        // this is a JavaFX source file
                        for (JCTree def : cDecl.getMembers()) {
                            if (def.getTag() == JavafxTag.FUNCTION_DEF) {
                                processFunctionFromSource((JFXOperationDefinition) def);
                            } else if (def.getTag() == JavafxTag.VAR_DEF) {
                                processAttributeFromSource((JFXVar) def);
                            }
                        }
                        // now visit the super-types
                        for (JCExpression supertype : cDecl.getSupertypes()) {
                            addToVisitList(supertype.type.tsym);
                        }
                    }
                }
            }
        }
 
        private void addToVisitList(Symbol sSym) {
            if (sSym != null && sSym.kind == Kinds.TYP) {
                ClassSymbol cSym = (ClassSymbol) sSym;
                if (!addedBaseClasses.contains(cSym)) {
 
                    addedBaseClasses.add(cSym);
                    classesToVisit.add(cSym);
                    baseClasses.add(cSym);
                }
            }
        }
        
        private void processMethodFromClassFile(MethodSymbol meth) {
            String methName = meth.name.toString();
            if (methName.startsWith(attributeGetMethodNamePrefix)) {
                // this is an attrubute get method, collect it as an attribute
                String nameSig = methName.substring(attributeGetMethodNamePrefix.length());
                if (!visitedAttributes.contains(nameSig)) {
                    visitedAttributes.add(nameSig);
                    attributes.add(meth);
                }
            } else if (methName.endsWith(defs.implFunctionSuffix)) {
                // implementation method
                methName = methName.substring(0, methName.length() - defs.implFunctionSuffix.length());
                int cnt = 0;
                ListBuffer<VarSymbol> params = ListBuffer.lb();
                ListBuffer<Type> paramTypes = ListBuffer.lb();
                for (VarSymbol param : meth.getParameters()) {
                    cnt++;
                    if (cnt > 1) {
                        params.append(param);
                        paramTypes.append(param.type);
                    }
                }
                MethodType mtype = new MethodType(paramTypes.toList(), meth.type.getReturnType(), meth.type.getThrownTypes(), meth.type.tsym);
                MethodSymbol fixedMeth = new MethodSymbol(
                     meth.flags() & ~Flags.STATIC, 
                     names.fromString(methName),
                     mtype, 
                     meth.owner);
                fixedMeth.params = params.toList();
                String nameSig = methodSignature(fixedMeth);
                if (!visitedFunctions.contains(nameSig)) {
                    visitedFunctions.add(nameSig);
                    needDispatchMethods.add(fixedMeth);
                }
            }
        }
      
        private void processFunctionFromSource(JFXOperationDefinition def) {
            MethodSymbol meth = def.sym;
            if (def.pos == Position.NOPOS) {
                return; //TODO: this looks REALLY dangerous, but this brabch is taken.  FIXME
            }
            String nameSig = methodSignature(meth).toString();
            if (!visitedFunctions.contains(nameSig)) {
                visitedFunctions.add(nameSig);
                if ((meth.flags() & (Flags.SYNTHETIC|Flags.STATIC)) == 0) {
                    needInterfaceMethods.add(meth);
                }
                if ((meth.flags() & (Flags.SYNTHETIC | Flags.ABSTRACT)) == 0) {
                    if ((meth.flags() & (Flags.STATIC)) != 0) {
                        needStaticDispatchMethods.add(meth);
                    } else {
                        needDispatchMethods.add(meth);
                    }
                }
            }
        }
        
        private  void processAttributeFromSource(JFXVar def) {
             VarSymbol var = def.sym;
 
             if (var.owner.kind == Kinds.TYP) {
                 String name = var.name.toString();
 
                 if (!visitedAttributes.contains(name)) {
                     visitedAttributes.add(name);
                     attributes.add(var);
                 }
             }
         }
 
         private String methodSignature(MethodSymbol meth) {
             StringBuilder nameSigBld = new StringBuilder();
             nameSigBld.append(meth.name.toString());
             nameSigBld.append(":");
             nameSigBld.append(meth.getReturnType().toString());
             nameSigBld.append(":");
             for (VarSymbol param : meth.getParameters()) {
                 nameSigBld.append(param.type.toString());
                 nameSigBld.append(":");
             }
             return nameSigBld.toString();
         }
     }
 
     boolean isJFXClass(Symbol sym) {
         if (!(sym instanceof ClassSymbol)) {
             return false;
         }
         
         ClassSymbol cSym = (ClassSymbol)sym;
         if ((cSym.flags_field & Flags.INTERFACE) != 0) {
             for (List<Type> intfs = cSym.getInterfaces(); intfs.nonEmpty(); intfs = intfs.tail) {
                 if (intfs.head.tsym.type == syms.javafx_FXObjectType) {
                     return true;
                 }
             }
         }
         else {
             if (fxClasses != null) {
                 if (fxClasses.containsKey(cSym)) {
                     return true;
                 }
                 
                 for (List<Type> intfs = cSym.getInterfaces(); intfs.nonEmpty(); intfs = intfs.tail) {
                     if (intfs.head.tsym.type == syms.javafx_FXObjectType) {
                         return true;
                     }
                 }
             }
         }
         
         return false;
     }
     
     void addFxClass(ClassSymbol csym, JFXClassDeclaration cdecl) {
         if (fxClasses == null) {
             fxClasses = new HashMap<ClassSymbol, JFXClassDeclaration>();
         }
         
         fxClasses.put(csym, cdecl);
     }
 
     void addFxClassAttributes(ClassSymbol csym, java.util.List<Symbol> attrs) {
         if (fxClassAttributes == null) {
             fxClassAttributes = new HashMap<ClassSymbol, java.util.List<Symbol>>();
         }
         
         fxClassAttributes.put(csym, attrs);
     }
 
     public void clearCaches() {
         fxClasses = null;
         fxClassAttributes = null;
     }
     
     static class AttributeWrapper {
         Type type;
         Name name;
         
         AttributeWrapper(Type type, Name name) {
              this.type = type;
             this.name = name;
         }
     }
 }
