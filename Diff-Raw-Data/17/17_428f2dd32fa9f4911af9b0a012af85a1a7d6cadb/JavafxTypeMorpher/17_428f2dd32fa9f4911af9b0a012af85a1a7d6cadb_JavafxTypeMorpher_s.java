 /*
  * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *
  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.  Sun designates this
  * particular file as subject to the "Classpath" exception as provided
  * by Sun in the LICENSE file tree accompanied this code.
  *
  * This code is distributed in the hope tree it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  * version 2 for more details (a copy is included in the LICENSE file tree
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
 
 import java.io.OutputStreamWriter;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

 import com.sun.tools.javac.code.*;
 import com.sun.tools.javac.code.Symbol.ClassSymbol;
 import com.sun.tools.javac.code.Symbol.TypeSymbol;
 import com.sun.tools.javac.code.Symbol.VarSymbol;
 import com.sun.tools.javac.code.Type.ClassType;
 import com.sun.tools.javac.code.Type.MethodType;
 import com.sun.tools.javac.jvm.ClassReader;
 import com.sun.tools.javac.tree.JCTree;
 import com.sun.tools.javac.tree.JCTree.*;
 import com.sun.tools.javac.tree.Pretty;
 import com.sun.tools.javac.tree.TreeMaker;
 import com.sun.tools.javac.util.*;
 import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
 import com.sun.tools.javafx.code.JavafxFlags;
 import com.sun.tools.javafx.code.JavafxSymtab;
 import com.sun.tools.javafx.code.JavafxTypes;
 import com.sun.tools.javafx.code.JavafxVarSymbol;
 import static com.sun.tools.javafx.code.JavafxVarSymbol.*;
 import static com.sun.tools.javafx.comp.JavafxDefs.attributeGetMethodNamePrefix;
 import static com.sun.tools.javafx.comp.JavafxDefs.interfaceSuffix;
 import com.sun.tools.javafx.comp.JavafxToJava.Wrapped;
import com.sun.tools.javafx.tree.*/**
  *
  * @author Robert Field
  */
 public class JavafxTypeMorpher {
     protected static final Context.Key<JavafxTypeMorpher> typeMorpherKey =
             new Context.Key<JavafxTypeMorpher>();
 
     private final JavafxDefs defs;
     private final Name.Table names;
     final ClassReader reader;
     private final TreeMaker make;
     private final JavafxSymtab syms;
     private final Log log;
     private final JavafxToJava toJava;
     private final JavafxTypes types;
 
     public static final String locationPackageName = "com.sun.javafx.runtime.location.";
 
     public LocationNameSymType[] varLocation;
     public LocationNameSymType[] exprLocation;
     public LocationNameSymType[] declLocation;
     public LocationNameSymType[] holder;
     public LocationNameSymType   baseLocation;
 
     private final Type[] realTypeByKind;
     private final Object[] defaultValueByKind;
 
     public class LocationNameSymType {
         public final Name name;
         public final ClassSymbol sym;
         public final Type type;
         LocationNameSymType(String which) {
             name = Name.fromString(names, locationPackageName + which);
             sym = reader.enterClass(name);
             type = sym.type;
         }
     }
 
     private Map<Symbol, VarMorphInfo> vmiMap = new HashMap<Symbol, VarMorphInfo>();
 
     class VarMorphInfo extends TypeMorphInfo {
         private final Symbol sym;
         private final boolean isMethod;
         private boolean mustMorph = false;
         private boolean haveDeterminedMorphability = false;
         private boolean isBoundTo = false;
         private boolean isAssignedTo = false;
 
         VarMorphInfo(Symbol sym) {
             super((sym.kind == Kinds.MTH)? ((MethodType)sym.type).getReturnType() : sym.type);
             this.sym = sym;
             this.isMethod = (sym.kind == Kinds.MTH);
         }
 
             private void determineMorphability() {
            if (!isMethod) {
                Symbol owner = getSymbol().owner;
                if (owner.kind == Kinds.MTH) {
                    // non-parameter local vars are morphed if they are bound to or sequencea
                    // (bound functions and their parameters are handled elsewhere)
                    if ((isBoundTo() || isSequence()) && ((getSymbol().flags() & Flags.PARAMETER) == 0)) {
                        markMustMorph();
                    }
                } else if (owner.kind == Kinds.TYP) {
                    if (getSymbol() instanceof JavafxVarSymbol) {
                        markMustMorph(); // we made it, soassume it is from a JavaFX class
                    } else if (sym.flatName() != names._super && sym.flatName() != names._this) {
                        if (types.isJFXClass(owner)) {
                            // this is an attribute: it is owned by a JavaFX class and it isn't 'this' or 'super'
                            markMustMorph();
                        }
                    }
                }
            }
            markDeterminedMorphability();
        }
 
        public boolean mustMorph() {
            if (!haveDeterminedMorphability()) {
                determineMorphability();
            }
            return mustMorph;
        }
 
         private void markMustMorph() { mustMorph = true; }
         private void markDeterminedMorphability() { haveDeterminedMorphability = true; }
         private boolean haveDeterminedMorphability() { return haveDeterminedMorphability; }
         public boolean isBoundTo() { return isBoundTo; }
         public boolean isAssignedTo() { return isAssignedTo; }
         public void markBoundTo() { this.isBoundTo = true; haveDeterminedMorphability = false; }
         public void markAssignedTo() { this.isAssignedTo = true; }
 
         public Symbol getSymbol() {
             return sym;
         }
     }
 
     class TypeMorphInfo {
         final Type realType;
         final Type morphedType;
         int typeKind;
         Type elementType = null;
 
         TypeMorphInfo(Type symType) {
             TypeSymbol realTsym = symType.tsym;
             //check if symbol is already a Location, needed for source class
             Type locationType = null;
             if (realTsym == declLocation[TYPE_KIND_OBJECT].sym) {
                 locationType = ((ClassType) symType).typarams_field.head;
                 typeKind = TYPE_KIND_OBJECT;
             } else if (realTsym == declLocation[TYPE_KIND_SEQUENCE].sym) {
                 locationType = ((ClassType) symType).typarams_field.head;
                 typeKind = TYPE_KIND_SEQUENCE;
             } else if (realTsym == declLocation[TYPE_KIND_BOOLEAN].sym) {
                 locationType = syms.booleanType;
                 typeKind = TYPE_KIND_BOOLEAN;
             } else if (realTsym == declLocation[TYPE_KIND_DOUBLE].sym) {
                 locationType = syms.doubleType;
                 typeKind = TYPE_KIND_DOUBLE;
             } else if (realTsym == declLocation[TYPE_KIND_INT].sym) {
                 locationType = syms.intType;
                 typeKind = TYPE_KIND_INT;
             }
             if (locationType != null) {
                 // External module with a Location type
                 this.morphedType = symType;
                 this.realType = locationType;
             } else {
                 this.realType = symType;
 
                 if (symType.isPrimitive()) {
                     if (realTsym == syms.doubleType.tsym
                             || realTsym == syms.floatType.tsym) {
                         typeKind = TYPE_KIND_DOUBLE;
                     } else if (realTsym == syms.intType.tsym
                             || realTsym == syms.byteType.tsym
                             || realTsym == syms.charType.tsym
                             || realTsym == syms.longType.tsym
                             || realTsym == syms.shortType.tsym) {
                         typeKind = TYPE_KIND_INT;
                     } else if (realTsym == syms.booleanType.tsym) {
                         typeKind = TYPE_KIND_BOOLEAN;
                     } else {
                         assert false : "should not reach here";
                         typeKind = TYPE_KIND_OBJECT;
                     }
                 } else {
                     if (isSequence()) {
                         typeKind = TYPE_KIND_SEQUENCE;
                         elementType = toJava.elementType(symType);
                     } else {
                         typeKind = TYPE_KIND_OBJECT;
                         elementType = realType;
                     }
                 }
 
                 // must be called AFTER typeKind and realType are set in vsym
                 this.morphedType = symType == syms.voidType? symType : generifyIfNeeded(declLocationType(typeKind), this);
             }
         }
 
         protected boolean isSequence() {
             return types.erasure(realType) == syms.javafx_SequenceTypeErasure;
         }
 
         public Type getRealType() { return realType; }
         public Type getMorphedType() { return morphedType; }
         public Object getDefaultValue() { return defaultValueByKind[typeKind]; }
         public Type getElementType() { return elementType; }
 
         public int getTypeKind() { return typeKind; }
     }
 
     VarMorphInfo varMorphInfo(Symbol sym) {
         VarMorphInfo vmi = vmiMap.get(sym);
         if (vmi == null) {
             vmi = new VarMorphInfo(sym);
             vmiMap.put(sym, vmi);
         }
         return vmi;
     }
 
     TypeMorphInfo typeMorphInfo(Type type) {
         return new TypeMorphInfo(type);
     }
 
     public static JavafxTypeMorpher instance(Context context) {
         JavafxTypeMorpher instance = context.get(typeMorpherKey);
         if (instance == null)
             instance = new JavafxTypeMorpher(context);
         return instance;
     }
 
     protected JavafxTypeMorpher(Context context) {
         context.put(typeMorpherKey, this);
 
         defs = JavafxDefs.instance(context);
         syms = (JavafxSymtab)(JavafxSymtab.instance(context));
         types = JavafxTypes.instance(context);
         names = Name.Table.instance(context);
         reader = ClassReader.instance(context);
         make = (JavafxTreeMaker)JavafxTreeMaker.instance(context);
         log = Log.instance(context);
         toJava = JavafxToJava.instance(context);
 
         String[] locClass = new String[TYPE_KIND_COUNT];
         locClass[TYPE_KIND_OBJECT] = "Object";
         locClass[TYPE_KIND_DOUBLE] = "Double";
         locClass[TYPE_KIND_BOOLEAN] = "Boolean";
         locClass[TYPE_KIND_INT] = "Int";
         locClass[TYPE_KIND_SEQUENCE] = "Sequence";
 
         varLocation = new LocationNameSymType[TYPE_KIND_COUNT];
         exprLocation = new LocationNameSymType[TYPE_KIND_COUNT];
         declLocation = new LocationNameSymType[TYPE_KIND_COUNT];
 
         for (int kind = 0; kind < TYPE_KIND_COUNT; ++kind) {
             varLocation[kind]  = new LocationNameSymType(locClass[kind] + "Var");
             exprLocation[kind] = new LocationNameSymType(locClass[kind] + "Expression");
             declLocation[kind] = new LocationNameSymType(locClass[kind] + "Location");
         }
 
         baseLocation = new LocationNameSymType("Location");
 
         realTypeByKind = new Type[TYPE_KIND_COUNT];
         realTypeByKind[TYPE_KIND_OBJECT] = syms.objectType;
         realTypeByKind[TYPE_KIND_DOUBLE] = syms.doubleType;
         realTypeByKind[TYPE_KIND_BOOLEAN] = syms.booleanType;
         realTypeByKind[TYPE_KIND_INT] = syms.intType;
         realTypeByKind[TYPE_KIND_SEQUENCE] = syms.javafx_SequenceType;
 
         defaultValueByKind = new Object[TYPE_KIND_COUNT];
         defaultValueByKind[TYPE_KIND_OBJECT] = null;
         defaultValueByKind[TYPE_KIND_DOUBLE] = 0.0;
         defaultValueByKind[TYPE_KIND_BOOLEAN] = 0;
         defaultValueByKind[TYPE_KIND_INT] = 0;
         defaultValueByKind[TYPE_KIND_SEQUENCE] = null; //TODO: empty sequence
     }
 
     Type declLocationType(int typeKind) {
         return declLocation[typeKind].type;
     }
 
     Type varLocationType(int typeKind) {
         return varLocation[typeKind].type;
     }
 
     Type expressionLocationType(int typeKind) {
         return exprLocation[typeKind].type;
     }
 
     JCExpression sharedLocationId(DiagnosticPosition diagPos, int typeKind, LocationNameSymType[] lnsta) {
         LocationNameSymType lnst = lnsta[typeKind];
         return ((JavafxTreeMaker)make).at(diagPos).Identifier(lnst.name);
     }
 
     JCExpression castToReal(Type realType, JCExpression expr) {
         // cast the expression so that boxing works
         JCExpression typeExpr = make.Type(realType);
         return make.TypeCast(typeExpr, expr);
     }
 
     private Type generifyIfNeeded(Type aLocationType, TypeMorphInfo tmi) {
         Type newType;
         if (tmi.getTypeKind() == TYPE_KIND_OBJECT ||
                 tmi.getTypeKind() == TYPE_KIND_SEQUENCE) {
             List<Type> actuals = List.of(tmi.getElementType());
             Type clazzOuter = declLocationType(tmi.getTypeKind()).getEnclosingType();
 
             List<Type> newActuals = List.nil();
             for (Type t : actuals) {
                 if ((t.tsym instanceof ClassSymbol) &&
                         (t.tsym.flags_field & JavafxFlags.COMPOUND_CLASS) != 0) {
                     String str = t.tsym.flatName().toString().replace("$", ".");
                     String strLookFor = str + interfaceSuffix;
                     Type tp = reader.enterClass(names.fromString(strLookFor)).type;
                     if (tp != null) {
                         tp.tsym.completer = null;
                         newActuals = newActuals.append(tp);
                         break;
                     }
                 }
 
                 newActuals = newActuals.append(t);
             }
 
             newType = new ClassType(clazzOuter, newActuals, aLocationType.tsym);
         } else {
             newType = aLocationType;
         }
         return newType;
     }
 
     public JCExpression convertVariableReference(DiagnosticPosition diagPos,
                                                  JCExpression varRef, Symbol sym, boolean staticReference,
                                                  boolean wantLocation, boolean createDynamicDependencies) {
 
         JCExpression expr = varRef;
 
         if (sym instanceof VarSymbol) {
              VarSymbol vsym = (VarSymbol) sym;
             VarMorphInfo vmi = varMorphInfo(vsym);
             if (toJava.shouldMorph(vmi)) {
                 if (sym.owner.kind == Kinds.TYP && !staticReference) {
                     // this is a non-static reference to an attribute, use the get$ form
                     assert varRef.getTag() == JCTree.SELECT : "attribute must be accessed through receiver";
                     JCFieldAccess select = (JCFieldAccess) varRef;
                     Name attrAccessName = names.fromString(attributeGetMethodNamePrefix + select.name.toString());
                     select = make.at(diagPos).Select(select.getExpression(), attrAccessName);
                     List<JCExpression> emptyArgs = List.nil();
                     expr = make.at(diagPos).Apply(null, select, emptyArgs);
 
                     // if are in a bind context and this is a select of an attribute,
                     // add the result as a dynamic dependency
                     if (createDynamicDependencies) {
                         expr = toJava.callExpression(diagPos, null, defs.addDynamicDependentName, expr);
                     }
                 }
                 if (wantLocation) {
                     // already in correct form-- leave it
                 } else {
                     // non-bind context -- want v1.get()
                     JCFieldAccess getSelect = make.Select(expr, defs.locationGetMethodName[vmi.getTypeKind()]);
                     List<JCExpression> getArgs = List.nil();
                     expr = make.at(diagPos).Apply(null, getSelect, getArgs);
                 }
             }
         } else if (sym instanceof MethodSymbol) {
             if (staticReference) {
                 Name name = toJava.functionName((MethodSymbol)sym);
                 if (expr.getTag() == JCTree.SELECT) {
                     JCFieldAccess select = (JCFieldAccess) expr;
                     expr = make.at(diagPos).Select(select.getExpression(), name);
                 } else if (expr.getTag() == JCTree.IDENT) {
                     expr = make.at(diagPos).Ident(name);
                 }
             }
         }
          return expr;
     }
 
     private void pretty(JCTree tree) {
         OutputStreamWriter osw = new OutputStreamWriter(System.out);
         Pretty pretty = new Pretty(osw, false);
         try {
             pretty.println();
             pretty.print("+++++++++++++++++++++++++++++++++");
             pretty.println();
             pretty.printExpr(tree);
             pretty.println();
             pretty.print("---------------------------------");
             pretty.println();
             osw.flush();
         }catch(Exception ex) {
             System.err.println("Pretty print got: " + ex);
         }
     }
 
     private void fxPretty(JCTree tree) {
         OutputStreamWriter osw = new OutputStreamWriter(System.out);
         Pretty pretty = new JavafxPretty(osw, false);
         try {
             pretty.println();
             pretty.print("+++++++++++++++++++++++++++++++++");
             pretty.println();
             pretty.printExpr(tree);
             pretty.println();
             pretty.print("---------------------------------");
             pretty.println();
             osw.flush();
         }catch(Exception ex) {
             System.err.println("Pretty print got: " + ex);
         }
     }
 
     //========================================================================================================================
     // Bound Expression support
     //========================================================================================================================
 
     /** Make an attributed tree representing a literal. This will be
      *  a Literal node.
      *  @param type       The literal's type.
      *  @param value      The literal's value.
      */
     JCExpression makeLit(Type type, Object value, DiagnosticPosition diagPos) {
         int tag = value==null? TypeTags.BOT : type.tag;
         return make.at(diagPos).Literal(tag, value).setType(type.constType(value));
     }
 
     public BindAnalysis bindAnalysis(JCExpression expr) {
         return new BindAnalysis(expr);
     }
 
     class BindAnalysis extends JavafxTreeScanner {
 
         boolean iterationSeen = false;
         boolean assignmentSeen = false;
         boolean javaCallSeen = false;
         boolean fxCallSeen = false;
         boolean exceptionHandlingSeen = false;
         boolean foreachSeen = false;
         boolean nonTerminalReturnSeen = false;
         private JCReturn terminalReturn = null;
 
         BindAnalysis(JCExpression expr) {
             if (expr.getTag() == JavafxTag.BLOCK_EXPRESSION) {
                 JFXBlockExpression bexpr = (JFXBlockExpression)expr;
                 if (bexpr.value == null) {
                     JCStatement last = bexpr.getStatements().last();
                     if (last != null && last.getTag() == JavafxTag.RETURN)
                         terminalReturn = (JCReturn)last;
                 }
             }
             scan(expr);
         }
 
         boolean isBindPermeable() {
             return !(iterationSeen || assignmentSeen || exceptionHandlingSeen || nonTerminalReturnSeen);
         }
 
         @Override
         public void visitApply(JCMethodInvocation tree) {
             super.visitApply(tree);
             Symbol msym =toJava.expressionSymbol(tree.getMethodSelect());
             if (msym != null && types.isJFXClass(msym.owner)) {
                 fxCallSeen = true;
             } else {
                 javaCallSeen = true;
             }
         }
 
         @Override
         public void visitForExpression(JFXForExpression tree) {
             super.visitForExpression(tree);
             foreachSeen = true;
         }
 
         @Override
         public void visitWhileLoop(JCWhileLoop tree) {
             super.visitWhileLoop(tree);
             iterationSeen = true;
         }
 
         @Override
         public void visitBreak(JCBreak tree) {
             super.visitBreak(tree);
             iterationSeen = true;
         }
 
         @Override
         public void visitContinue(JCContinue tree) {
             super.visitContinue(tree);
             iterationSeen = true;
         }
 
         @Override
         public void visitReturn(JCReturn tree) {
             super.visitReturn(tree);
             if (terminalReturn != tree) {
                 nonTerminalReturnSeen = true;
             }
         }
 
         @Override
         public void visitTry(JCTry tree) {
             super.visitTry(tree);
             exceptionHandlingSeen = true;
         }
 
         @Override
         public void visitThrow(JCThrow tree) {
             super.visitThrow(tree);
             exceptionHandlingSeen = true;
         }
 
         @Override
         public void visitAssign(JCAssign tree) {
             super.visitAssign(tree);
             assignmentSeen = true;
         }
 
         @Override
         public void visitAssignop(JCAssignOp tree) {
             super.visitAssignop(tree);
             assignmentSeen = true;
         }
 
         @Override
         public void visitClassDeclaration(JFXClassDeclaration that) {
             // don't decend into classes
         }
     }
 
     /**
      * Build a list of dependencies for an expression.  It should include
      * any references to variables (which are Locations) but exclude
      * any that are defined internal to the expression.
      * The resulting list is translated.
      */
     List<JCExpression> buildDependencies(JCExpression expr) {
         final Map<VarSymbol, JCExpression> refMap = new HashMap<VarSymbol, JCExpression>();
         final Set<VarSymbol> internalSet = new HashSet<VarSymbol>();
 
         JavafxTreeScanner ts = new JavafxTreeScanner() {
             @Override
             public void visitIdent(JCIdent tree)   {
                 if (tree.sym instanceof VarSymbol) {
                     VarSymbol ivsym = (VarSymbol)tree.sym;
                     if (ivsym.name != names._this && ivsym.name != names._super) {
                         VarMorphInfo vmi = varMorphInfo(ivsym);
                         if (toJava.shouldMorph(vmi)) {
                             refMap.put(ivsym, tree);
                         }
                     }
                 }
             }
             @Override
             public void visitSelect(JCFieldAccess tree)   {
                 super.visitSelect(tree);
                 if (tree.sym instanceof VarSymbol) {
                     VarSymbol ivsym = (VarSymbol)tree.sym;
                     VarMorphInfo vmi = varMorphInfo(ivsym);
                     if (toJava.shouldMorph(vmi)) {
                          if (ivsym.isStatic()) {
                              // this is a static reference, set up for a static dependency
                              // otherwise, this will be handled as a dynamic dependency
                              refMap.put(ivsym, tree);
                         }
                     }
                 }
             }
             @Override
             public void visitVar(JFXVar tree)   {
                 if (tree.sym instanceof VarSymbol) {
                     VarSymbol vvsym = tree.sym;
                     internalSet.add(vvsym);
                 }
             }
         };
         ts.scan(expr);
 
         ListBuffer<JCExpression> depend = ListBuffer.lb();
         for (Map.Entry<VarSymbol, JCExpression> ref : refMap.entrySet()) {
             if (!internalSet.contains(ref.getKey())) {
                 depend.append( toJava.translate( ref.getValue(), Wrapped.InLocation ) );
             }
         }
 
         return depend.toList();
     }
 
     /**
      * Create an ExpressionLocation from "stmt" which is the translation of the expression into
      * a statement.  The ExpressionLocation is created with an anonymous binding expression
      * instance and the static dependencies.
      */
     JCExpression buildExpression(DiagnosticPosition diagPos,
             TypeMorphInfo tmi,
             JCStatement stmt,
             boolean isLazy,
             List<JCExpression> staticDependencies) {
 
         JCStatement clearStmt = toJava.callStatement(diagPos, null, defs.clearDynamicDependenciesName);
         List<JCStatement> stmts;
         if (stmt.getTag() == JavafxTag.BLOCK) {
             JCBlock block = (JCBlock) stmt;
             stmts = block.getStatements();
             stmts = stmts.prepend(clearStmt);
         } else {
             stmts = List.of(clearStmt, stmt);
         }
         JCBlock body = make.at(diagPos).Block(0, stmts);
 
         JCMethodDecl computeValueMethod = make.at(diagPos).MethodDef(
                 make.at(diagPos).Modifiers(Flags.PUBLIC), 
                 defs.computeValueName, 
                 toJava.makeTypeTree(getReturnTypeForGetLocation(tmi), diagPos, true), 
                 List.<JCTypeParameter>nil(), 
                 List.<JCVariableDecl>nil(), 
                 List.<JCExpression>nil(), 
                 body, 
                 null); 
         
         toJava.bindingExpressionDefs.append(computeValueMethod);
         JCClassDecl anon = make.at(diagPos).AnonymousClassDef(
                     make.at(diagPos).Modifiers(0), 
                     toJava.bindingExpressionDefs.toList());
         toJava.bindingExpressionDefs = null;
         ListBuffer<JCExpression> argValues = ListBuffer.lb();
         if (tmi.typeKind == TYPE_KIND_SEQUENCE)
             argValues.append(toJava.makeSequenceClassArg(diagPos, tmi));
         argValues.append(make.Literal(TypeTags.BOOLEAN, isLazy ? 1 : 0));
         argValues.appendList(staticDependencies);
         Type expressionClass =
                 generifyIfNeeded(exprLocation[tmi.typeKind].type, tmi);
         return make.at(diagPos).NewClass(
                 null,                       // enclosing
                 List.<JCExpression>nil(),   // type args
                 // class name
                 toJava.makeTypeTree(expressionClass, diagPos),
                 argValues.toList(),
                 anon);
     }
 
     JCExpression makeCall(TypeMorphInfo tmi, 
                                   DiagnosticPosition diagPos,
                                   List<JCExpression> makeArgs,
                                   LocationNameSymType[] lnsta,
                                   Name makeName) {
         JCExpression locationTypeExp = sharedLocationId(diagPos, tmi.getTypeKind(), lnsta);
         JCFieldAccess makeSelect = make.at(diagPos).Select(locationTypeExp, makeName);
         List<JCExpression> typeArgs = null;
         if (tmi.getTypeKind() == TYPE_KIND_OBJECT) {
             typeArgs = List.of(toJava.makeTypeTree(tmi.getRealType(), diagPos, true));
         }
         else if (tmi.getTypeKind() == TYPE_KIND_SEQUENCE) {
             typeArgs = List.of(toJava.makeTypeTree(tmi.getElementType(), diagPos, true));
         }
         return make.at(diagPos).Apply(typeArgs, makeSelect, makeArgs);
     }
 
     private Type getReturnTypeForGetLocation(TypeMorphInfo tmi) {
         Type ret = tmi.getRealType();
         if (tmi.typeKind == TYPE_KIND_DOUBLE) {
             ret = syms.javafx_NumberType;
         }
         else if (tmi.typeKind == TYPE_KIND_INT) {
             ret = syms.javafx_IntegerType;
         }
         
         return ret;
     }
 }
