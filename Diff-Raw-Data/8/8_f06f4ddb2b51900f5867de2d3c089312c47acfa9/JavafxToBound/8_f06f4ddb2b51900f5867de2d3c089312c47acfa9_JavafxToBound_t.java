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
 
 package com.sun.tools.javafx.comp;
 
 import com.sun.javafx.api.JavafxBindStatus;
 import com.sun.javafx.api.tree.SequenceSliceTree;
 import com.sun.tools.javac.code.*;
 import com.sun.tools.javac.code.Symbol;
 import com.sun.tools.javac.code.Type.*;
 import com.sun.tools.javac.tree.JCTree;
 import com.sun.tools.javac.tree.JCTree.*;
 import com.sun.tools.javac.util.*;
 import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
 import com.sun.tools.javafx.code.FunctionType;
 import com.sun.tools.javafx.comp.JavafxToJava.UseSequenceBuilder;
 import com.sun.tools.javafx.comp.JavafxToJava.Translator;
 import com.sun.tools.javafx.comp.JavafxToJava.FunctionCallTranslator;
 import com.sun.tools.javafx.comp.JavafxToJava.InstanciateTranslator;
 import com.sun.tools.javafx.comp.JavafxToJava.InterpolateValueTranslator;
 import com.sun.tools.javafx.comp.JavafxToJava.StringExpressionTranslator;
 import com.sun.tools.javafx.comp.JavafxToJava.Locationness;
 import static com.sun.tools.javafx.code.JavafxVarSymbol.*;
 import static com.sun.tools.javafx.comp.JavafxDefs.*;
 import com.sun.tools.javafx.comp.JavafxTypeMorpher.TypeMorphInfo;
 import com.sun.tools.javafx.comp.JavafxTypeMorpher.VarMorphInfo;
 import com.sun.tools.javafx.tree.*;
 import static com.sun.tools.javac.code.TypeTags.*;
 
 public class JavafxToBound extends JavafxTranslationSupport implements JavafxVisitor {
     protected static final Context.Key<JavafxToBound> jfxToBoundKey =
         new Context.Key<JavafxToBound>();
 
     /*
      * modules imported by context
      */
     private final JavafxToJava toJava;
     private final JavafxOptimizationStatistics optStat;
 
     /*
      * other instance information
      */
     private final Name param1Name;
     private final Name computeElementsName;
 
     /*
      * State
      */
     private TypeMorphInfo tmiTarget = null;
     private JavafxBindStatus bindStatus = null; // should never be accessed before set
 
     /*
      * static information
      */
     private static final String cBoundSequences = sequencePackageNameString + ".BoundSequences";
     private static final String cBoundOperators = locationPackageNameString + ".BoundOperators";
     private static final String cLocations = locationPackageNameString + ".Locations";
     private static final String cFunction0 = functionsPackageNameString + ".Function0";
     private static final String cFunction1 = functionsPackageNameString + ".Function1";
 
     private static final String cOperator = cBoundOperators + ".Operator";
 
     private static final String opPLUS = cOperator + ".PLUS";
     private static final String opMINUS = cOperator + ".MINUS";
     private static final String opTIMES = cOperator + ".TIMES";
     private static final String opDIVIDE = cOperator + ".DIVIDE";
     private static final String opMODULO = cOperator + ".MODULO";
 
     private static final String opLT = cOperator + ".CMP_LT";
     private static final String opLE = cOperator + ".CMP_LE";
     private static final String opGT = cOperator + ".CMP_GT";
     private static final String opGE = cOperator + ".CMP_GE";
 
     private static final String opEQ = cOperator + ".CMP_EQ";
     private static final String opNE = cOperator + ".CMP_NE";
 
     private static final String opNEGATE = cOperator + ".NEGATE";
     private static final String opNOT = cOperator + ".NOT";
 
     public static JavafxToBound instance(Context context) {
         JavafxToBound instance = context.get(jfxToBoundKey);
         if (instance == null)
             instance = new JavafxToBound(context);
         return instance;
     }
 
     protected JavafxToBound(Context context) {
         super(context);
         
         context.put(jfxToBoundKey, this);
 
         toJava = JavafxToJava.instance(context);
         optStat = JavafxOptimizationStatistics.instance(context);
         
         param1Name = names.fromString("x1$");
         computeElementsName = names.fromString("computeElements$");
     }
 
     /*** External entry points ***/
 
     JCExpression translate(JFXExpression tree, JavafxBindStatus bindStatus, TypeMorphInfo tmi) {
         JavafxBindStatus prevBindStatus = this.bindStatus;
         this.bindStatus = bindStatus;
         JCExpression res = translateGeneric(tree, tmi);
         this.bindStatus = prevBindStatus;
         return res;
     }
 
     JCExpression translate(JFXExpression tree, JavafxBindStatus bindStatus, Type type) {
         JavafxBindStatus prevBindStatus = this.bindStatus;
         this.bindStatus = bindStatus;
         JCExpression res = translateGeneric(tree, type);
         this.bindStatus = prevBindStatus;
         return res;
     }
 
     // only for re-entry use by SequenceBuilder
     JCExpression translate(JFXExpression tree) {
         return translateGeneric(tree);
     }
 
     private JCExpression translate(JFXExpression tree, TypeMorphInfo tmi) {
         return translateGeneric(tree, tmi);
     }
 
     JCExpression translate(JFXExpression tree, Type type) {
         return translateGeneric(tree, type);
     }
     
     /** Visitor method: Translate a single node.
      */
     @SuppressWarnings("unchecked")
     private <TFX extends JFXExpression, TC extends JCTree> TC translateGeneric(TFX tree, TypeMorphInfo tmi) {
         TypeMorphInfo tmiPrevTarget = tmiTarget;
         this.tmiTarget = tmi;
         TC ret;
         if (tree == null) {
             ret = null;
         } else {
             JFXTree prevWhere = toJava.attrEnv.where;
             toJava.attrEnv.where = tree;
             tree.accept(this);
             toJava.attrEnv.where = prevWhere;
             ret = (TC) this.result;
             this.result = null;
         }
         this.tmiTarget = tmiPrevTarget;
         return ret;
     }
 
     private <TFX extends JFXExpression, TC extends JCTree> TC translateGeneric(TFX tree, Type type) {
         return translateGeneric(tree, typeMorpher.typeMorphInfo(type));
     }
 
     private <TFX extends JFXExpression, TC extends JCTree> TC translateGeneric(TFX tree) {
         return translateGeneric(tree, (TypeMorphInfo) null);
     }
 
     private List<JCExpression> translate(List<JFXExpression> trees, Type methType, boolean usesVarArgs) {
         return translateGeneric(trees, methType, usesVarArgs);
     }
 
     private <TFX extends JFXExpression, TC extends JCExpression> List<TC> translateGeneric(List<TFX> trees, Type methType, boolean usesVarArgs) {
         ListBuffer<TC> translated = ListBuffer.lb();
         boolean handlingVarargs = false;
         Type formal = null;
         List<Type> t = methType.getParameterTypes();
         for (List<TFX> l = trees; l.nonEmpty(); l = l.tail) {
             if (!handlingVarargs) {
                 formal = t.head;
                 t = t.tail;
                 if (usesVarArgs && t.isEmpty()) {
                     formal = types.elemtype(formal);
                     handlingVarargs = true;
                 }
             }
             TC tree = translateGeneric(l.head, formal);
             if (tree != null) {
                 if (tree.type == null) { // if not set by convert()
                     tree.type = formal; // mark the type to declare the holder of this arg
                 }
                 translated.append(tree);
             }
         }
         List<TC> args = translated.toList();
 
         return args;
     }
 
     private JCExpression convert(Type inType, JCExpression tree) {
         if (tmiTarget == null) {
             tree.type = inType;
             return tree;
         }
         Type targetType = tmiTarget.getRealType();
         return convert(inType, tree, targetType);
     }
 
     private JCExpression convert(Type inType, JCExpression tree, Type targetType) {
         DiagnosticPosition diagPos = tree.pos();
         if (!types.isSameType(inType, targetType)) {
             if (types.isSequence(targetType)) {
                 Type targetElementType = types.elementType(targetType);
                 if (targetElementType == null) {  // runtime classes written in Java do this
                     tree.type = inType;
                     return tree;
                 }
                 if (!types.isSequence(inType)) {
                     JCExpression targetTypeInfo = makeTypeInfo(diagPos, targetElementType);
                     tree = runtime(diagPos, 
                             cBoundSequences,
                             "singleton",
                             List.of(targetTypeInfo, convert(inType, tree, targetElementType)));
                 } else {
                     // this additional test is needed because wildcards compare as different
                     Type sourceElementType = types.elementType(inType);
                     if (!types.isSameType(sourceElementType, targetElementType)) {
                         if (types.isNumeric(sourceElementType) && types.isNumeric(targetElementType)) {
                             tree = convertNumericSequence(diagPos,
                                     cBoundSequences,
                                     tree,
                                     sourceElementType,
                                     targetElementType);
                         } else {
                             JCExpression targetTypeInfo = makeTypeInfo(diagPos, targetElementType);
                             tree = runtime(diagPos, cBoundSequences, "upcast", List.of(targetTypeInfo, tree));
                         }
                     }
                 }
             } else if (targetType.isPrimitive()) {
                 if (inType.isPrimitive()) {
                     JCExpression classTypeExpr = makeIdentifier(diagPos, cLocations + "." + "NumericTo" + primitiveTypePrefix(targetType) + "LocationConversionWrapper");
                     JavafxTypeMorpher.TypeMorphInfo tmi = typeMorpher.typeMorphInfo(inType);
                     JCExpression locationType = makeTypeTree(diagPos, tmi.getLocationType());
                     JCExpression boxType = makeTypeTree(diagPos, syms.boxIfNeeded(inType));
                     tree = make.NewClass(null, List.of(locationType, boxType), classTypeExpr,
                             List.of(tree, makeTypeInfo(diagPos, inType)),
                             null);
                 }
                 //TODO: boxed inType
             } else {
                 List<JCExpression> typeArgs = List.of(makeTypeTree(diagPos, targetType, true),
                         makeTypeTree(diagPos, syms.boxIfNeeded(inType), true));
                 Type inRealType = typeMorpher.typeMorphInfo(inType).getRealType();
                 JCExpression inClass = makeTypeInfo(diagPos, inRealType);
                 tree = runtime(diagPos, cLocations, "upcast", typeArgs, List.of(inClass, tree));
             }
         }
         tree.type = targetType; // as a way of passing it to methods which needs to know the target type
         return tree;
     }
     //where
     private String primitiveTypePrefix(Type type) {
         switch (type.tag) {
             case BYTE:
                 return "Byte";
             case SHORT:
                 return "Short";
             case INT:
                 return "Int";
             case LONG:
                 return "Long";
             case FLOAT:
                 return "Float";
             case DOUBLE:
                 return "Double";
         }
         assert false : "Should not reach here";
         return "Unknown";
     }
 
     /**
      *
      * @param diagPos
      * @return a boolean expression indicating if the bind is lazy
      */
     private JCExpression makeLaziness(DiagnosticPosition diagPos) {
         return makeLaziness(diagPos, bindStatus);
     }
 
     private Type targetType(Type type) {
         return tmiTarget!=null? tmiTarget.getRealType() : type;
     }
 
     private JCVariableDecl translateVar(JFXVar tree) {
         DiagnosticPosition diagPos = tree.pos();
 
         JFXModifiers mods = tree.getModifiers();
         long modFlags = mods == null ? 0L : mods.flags;
         modFlags |= Flags.FINAL;  // Locations are never overwritten
         JCModifiers tmods = make.at(diagPos).Modifiers(modFlags);
 
         VarMorphInfo vmi = typeMorpher.varMorphInfo(tree.sym);
         JCExpression typeExpression = makeTypeTree( diagPos,vmi.getLocationType(), true);
 
         //TODO: handle array initializers (but, really, shouldn't that be somewhere else?)
         JCExpression init;
         if (tree.init == null) {
             init = makeLocationAttributeVariable(vmi, diagPos);
         } else {
             init = translate(tree.init, vmi.getRealFXType());
         }
         return make.at(diagPos).VarDef(tmods, tree.name, typeExpression, init);
     }
 
     private abstract class ClosureTranslator extends Translator {
 
         protected final TypeMorphInfo tmiResult;
         protected final int typeKindResult;
         protected final Type elementTypeResult;
 
         // these only used when fields are built
         ListBuffer<JCTree> members = ListBuffer.lb();
         ListBuffer<JCStatement> fieldInits = ListBuffer.lb();
         ListBuffer<JCExpression> dependents = ListBuffer.lb();
         ListBuffer<JCExpression> callArgs = ListBuffer.lb();
         int argNum = 0;
 
         ClosureTranslator(DiagnosticPosition diagPos, Type resultType) {
             this(diagPos, JavafxToBound.this.toJava, (tmiTarget != null) ? tmiTarget : typeMorpher.typeMorphInfo(resultType));
         }
 
         private ClosureTranslator(DiagnosticPosition diagPos, JavafxToJava toJava, TypeMorphInfo tmiResult) {
             super(diagPos, toJava);
             this.tmiResult = tmiResult;
             typeKindResult = tmiResult.getTypeKind();
             elementTypeResult = boxedElementType(tmiResult.getLocationType()); // want boxed, JavafxTypes version won't work
         }
 
         /**
          * Make a method paramter
          */
         protected JCVariableDecl makeParam(Type type, Name name) {
             return make.at(diagPos).VarDef(
                     m().Modifiers(Flags.PARAMETER | Flags.FINAL),
                     name,
                     makeExpression(type),
                     null);
         }
 
         protected JCTree makeClosureMethod(String methName, JCExpression expr, List<JCVariableDecl> params, Type returnType, long flags) {
             return m().MethodDef(
                     m().Modifiers(flags),
                     names.fromString(methName),
                     makeExpression(returnType),
                     List.<JCTypeParameter>nil(),
                     params==null? List.<JCVariableDecl>nil() : params,
                     List.<JCExpression>nil(),
                     m().Block(0L, List.<JCStatement>of(m().Return(expr))),
                     null);
         }
 
         protected JCTree makeClosureMethod(String methName, JCExpression expr, List<JCVariableDecl> params) {
             return makeClosureMethod(methName, expr, params, tmiResult.getLocationType(), Flags.PROTECTED);
         }
 
         protected abstract List<JCTree> makeBody();
 
         protected abstract JCExpression makeBaseClass();
 
         protected JCExpression makeBaseClass(Type clazzType, Type additionTypeParamOrNull) {
             JCExpression clazz = makeExpression(types.erasure(clazzType));  // type params added below, so erase formals
             ListBuffer<JCExpression> typeParams = ListBuffer.lb();
 
             if (typeKindResult == TYPE_KIND_OBJECT || typeKindResult == TYPE_KIND_SEQUENCE) {
                 typeParams.append(makeExpression(elementTypeResult));
             }
             if (additionTypeParamOrNull != null) {
                 typeParams.append(makeExpression(additionTypeParamOrNull));
             }
             return typeParams.isEmpty()? clazz : m().TypeApply(clazz, typeParams.toList());
         }
         
         protected abstract List<JCExpression> makeConstructorArgs();
 
         protected JCExpression buildClosure() {
             JCClassDecl classDecl = m().AnonymousClassDef(m().Modifiers(0L), makeBody());
             List<JCExpression> typeArgs = List.nil();
             return m().NewClass(null/*encl*/, typeArgs, makeBaseClass(), makeConstructorArgs(), classDecl);
         }
 
         protected JCExpression doit() {
             return buildClosure();
         }
 
 
         // field building support
 
         protected List<JCTree> completeMembers() {
             members.append(m().Block(0L, fieldInits.toList()));
             return members.toList();
         }
 
         protected JCExpression makeGet(JCExpression locExpr, int typeKind) {
             Name getMethodName = defs.locationGetMethodName[typeKind];
             JCFieldAccess select = m().Select(locExpr, getMethodName);
             return m().Apply(null, select, List.<JCExpression>nil());
         }
 
         protected JCExpression makeGetField(Name fieldName, TypeMorphInfo tmiField) {
             return makeGetField(fieldName, tmiField.getTypeKind());
         }
 
         protected JCExpression makeGetField(Name fieldName, int typeKind) {
             return makeGet(m().Ident(fieldName), typeKind);
         }
 
         protected JCTree makeLocationField(JCExpression targ, Name argName, TypeMorphInfo tmiArg) {
             fieldInits.append( m().Exec( m().Assign(m().Ident(argName), targ)) );
             return m().VarDef(
                     m().Modifiers(Flags.PRIVATE),
                     argName,
                     makeExpression(tmiArg.getLocationType()),
                     null);
         }
 
         protected JCExpression buildArgField(JCExpression arg, Type type) {
             return buildArgField(arg, type, false);
         }
 
         protected JCExpression buildArgField(JCExpression arg, Type type, boolean isBound) {
             return buildArgField(arg, type, "arg$" + argNum++, isBound);
         }
 
         protected JCExpression buildArgField(JCExpression arg, Type type, String argLabel) {
             return buildArgField(arg, type, argLabel, false);
         }
 
         protected JCExpression buildArgField(JCExpression arg, Type type, String argLabel, boolean isBound) {
             TypeMorphInfo tmiArg = typeMorpher.typeMorphInfo(type);
             Name argName = names.fromString(argLabel);
 
             // translate the method arg into a Location field of the BindingExpression
             // XxxLocation arg$0 = ...;
             members.append(makeLocationField(arg, argName, tmiArg));
 
             // build a list of these args, for use as dependents -- arg$0, arg$1, ...
             if (isBound) {
                 return m().Ident(argName);
             } else {
                 dependents.append(m().Ident(argName));
 
                 // set up these arg for the call -- arg$0.getXxx()
                 return makeGetField(argName, tmiArg);
             }
          }
 
         protected void buildArgFields(List<JCExpression> targs, boolean isBound) {
             for (JCExpression targ : targs) {
                 assert targ.type != null : "caller is supposed to decorate the translated arg with its type";
                 callArgs.append( buildArgField(targ, targ.type, isBound) );
             }
         }
     }
 
     @Override
     public void visitInstanciate(final JFXInstanciate tree) {
         result = new BindingExpressionClosureTranslator(tree.pos(), tree.type) {
 
             protected JCExpression resultValue() {
                JCExpression rv = new InstanciateTranslator(tree, toJava) {
 
                     protected void processLocalVar(JFXVar var) {
                         JFXExpression init = var.getInitializer();
                         JCExpression tinit = init==null?
                                 makeLocationAttributeVariable(typeMorpher.varMorphInfo(var.sym), diagPos)
                                 : translate(init);
                         // The local variable is built as bound (last param true) because it does not
                         // directly impact the state of the object.  Any indirect immpacts are captured
                         // in the binding of the instance variables.
                         buildArgField(tinit, var.type, var.getName().toString(), true);
                     }
 
                     @Override
                     protected List<JCExpression> translatedConstructorArgs() {
                         List<JFXExpression> args = tree.getArgs();
                         if (args != null && args.size() > 0) {
                             assert tree.constructor != null : "args passed on instanciation of class without constructor";
                             boolean usesVarArgs = (tree.constructor.flags() & Flags.VARARGS) != 0L;
                             buildArgFields(translate(args, tree.constructor.type, usesVarArgs), false);
                             return callArgs.toList();
                         } else {
                             return List.<JCExpression>nil();
                         }
                     }
 
                     @Override
                     void setInstanceVariable(Name instName, JavafxBindStatus bindStatus, VarSymbol vsym, JFXExpression init) {
                         JCExpression initRef = buildArgField(
                                 translate(init, bindStatus, vsym.type),
                                 vsym.type,
                                 vsym.name.toString() + "$attr",
                                 bindStatus.isBound());
                         setInstanceVariable(init.pos(), instName, bindStatus, vsym, initRef);
                     }
                 }.doit();
                return tmiTarget==null? rv : toJava.convertTranslated(rv, diagPos, tree.type, tmiTarget.getRealFXType());

             }
         }.doit();
     }
 
     @Override
     public void visitStringExpression(final JFXStringExpression tree) {
         result = new BindingExpressionClosureTranslator(tree.pos(), syms.stringType) {
 
             protected JCExpression resultValue() {
                 return new StringExpressionTranslator(tree, toJava) {
 
                     protected JCExpression translateArg(JFXExpression arg) {
                         return buildArgField(translate(arg), arg.type);
                     }
                 }.doit();
             }
         }.doit();
     }
 
     @Override
     public void visitFunctionValue(JFXFunctionValue tree) {
         JFXFunctionDefinition def = tree.definition;
         result = makeConstantLocation(tree.pos(), targetType(tree.type), toJava.makeFunctionValue(make.Ident(defs.lambdaName), def, tree.pos(), (MethodType) def.type) );
     }
 
     public void visitBlockExpression(JFXBlock tree) {   //done
         assert (tree.type != syms.voidType) : "void block expressions should be not exist in bind expressions";
         DiagnosticPosition diagPos = tree.pos();
 
         JFXExpression value = tree.value;
         ListBuffer<JCStatement> translatedVars = ListBuffer.lb();
 
         for (JFXExpression stmt : tree.getStmts()) {
             if (stmt.getFXTag() == JavafxTag.VAR_DEF) {
                 JFXVar var = (JFXVar) stmt;
                 translatedVars.append(translateVar(var));
                 optStat.recordLocalVar(var.sym, true, true);
             } else {
                 assert false : "non VAR_DEF in block expression in bind context";
             }
         }
         while (value.getFXTag() == JavafxTag.VAR_DEF) {
             // for now, at least, ignore the declaration part of a terminal var decl.
             //TODO: when vars can be referenced before decl (say in "var: self" replacement)
             // this will need to be changed.
             value = ((JFXVar)value).getInitializer();
         }
         assert value.getFXTag() != JavafxTag.RETURN;
         result = makeBlockExpression(diagPos, //TODO tree.flags lost
                 translatedVars.toList(),
                 translate(value, tmiTarget) );
     }
 
     @Override
     public void visitAssign(JFXAssign tree) {
         //TODO: this should probably not be allowed
         // log.error(tree.pos(), "javafx.not.allowed.in.bind.context", "=");
         DiagnosticPosition diagPos = tree.pos();
         TypeMorphInfo tmi = typeMorpher.typeMorphInfo(tree.type);
         int typeKind = tmi.getTypeKind();
         // create a temp var to hold the RHS
         JCVariableDecl varDecl = makeTmpVar(diagPos, tmi.getLocationType(), translate(tree.rhs));
         // call the set method
         JCStatement setStmt = callStatement(diagPos,
                 translate(tree.lhs),
                 defs.locationSetMethodName[typeKind],
                 callExpression(diagPos,
                     make.at(diagPos).Ident(varDecl.name),
                     defs.locationGetMethodName[typeKind]));
         // bundle it all into a block-expression that looks like --
         // { ObjectLocation tmp = rhs; lhs.set(tmp.get()); tmp }
         result = makeBlockExpression(diagPos,
                 List.of(varDecl, setStmt),
                 make.at(diagPos).Ident(varDecl.name));
     }
 
     @Override
     public void visitAssignop(JFXAssignOp tree) {
         // should have caught this in attribution
         assert false : "Assignment operator in bind context";
     }
 
     private JCExpression makeBoundSelect(final DiagnosticPosition diagPos,
             final Type resultType,
             final JCExpression receiverExpr,
             final Function1ClosureTranslator translator) {
         TypeMorphInfo tmi = (tmiTarget != null) ? tmiTarget : typeMorpher.typeMorphInfo(resultType);
         List<JCExpression> args = List.of(
                 makeLaziness(diagPos),
                 receiverExpr,
                 translator.doit());
         if (tmi.isSequence() || tmi.getTypeKind() == TYPE_KIND_OBJECT) {
             // prepend "Foo.class, "
             args = args.prepend(makeTypeInfo(diagPos, tmi.getElementType()));
         }
         return runtime(diagPos, cBoundOperators, "makeBoundSelect", args);
     }
 
     abstract class Function1ClosureTranslator extends ClosureTranslator {
 
         final Type param1Type;
 
         Function1ClosureTranslator(DiagnosticPosition diagPos,
                 Type resultType,
                 final Type param1Type) {
             super(diagPos, resultType);
             this.param1Type = param1Type;
         }
 
         abstract protected JCExpression makeInvokeMethodBody();
 
         protected List<JCTree> makeBody() {
             List<JCVariableDecl> params = List.of(makeParam(param1Type, param1Name));
             members.append(
                     makeClosureMethod("invoke", makeInvokeMethodBody(), params, tmiResult.getLocationType(), Flags.PUBLIC));
             return completeMembers();
         }
 
         protected JCExpression makeBaseClass() {
             JCExpression objFactory = makeQualifiedTree(diagPos, cFunction1);
             Type clazzType = tmiResult.getLocationType();
             return m().TypeApply(objFactory, List.of(
                     makeExpression(clazzType),
                     makeExpression(param1Type)));
         }
 
         protected List<JCExpression> makeConstructorArgs() {
             return List.<JCExpression>nil();
         }
     }
 
     @Override
     public void visitSelect(final JFXSelect tree) {
         if (tree.type instanceof FunctionType && tree.sym.type instanceof MethodType) {
             result = convert(tree.type, toJava.translateAsLocation(tree)); //TODO -- for now punt, translate like normal case
             return;
         }
         DiagnosticPosition diagPos = tree.pos();
 
         Symbol owner = tree.sym.owner;
         if (types.isJFXClass(owner) && typeMorpher.requiresLocation(tree.sym)) {
             if (tree.sym.isStatic()) {
                 // if this is a static reference to an attribute, eg.   MyClass.myAttribute
                 JCExpression classRef = makeTypeTree( diagPos,types.erasure(tree.sym.owner.type), false);
                 result = convert(tree.type, make.at(diagPos).Select(classRef, attributeFieldName(tree.sym)));
             } else {
                 // this is a dynamic reference to an attribute
                 JFXExpression expr = tree.getExpression();
                 result = makeBoundSelect(diagPos,
                         tree.type,
                         translate(expr),
                         new Function1ClosureTranslator(diagPos, tree.type, expr.type) {
 
                             protected JCExpression makeInvokeMethodBody() {
                                 return convert(tree.type, toJava.convertVariableReference(diagPos,
                                         make.at(diagPos).Select(make.at(diagPos).Ident(param1Name), tree.getIdentifier()),
                                         tree.sym,
                                         Locationness.AsLocation));
                             }
                         });
             }
         } else {
             if (tree.sym.isStatic()) {
                 // This is a static reference to a Java member or elided member e.g. System.out -- do unbound translation, then wrap
                 result = this.makeUnboundLocation(diagPos, targetType(tree.type), toJava.translateAsUnconvertedValue(tree));
             } else {
                 // This is a dynamic reference to a Java member or elided member
                 result = (new BindingExpressionClosureTranslator(diagPos, tree.type) {
 
                     private JFXExpression selector = tree.getExpression();
                     private TypeMorphInfo tmiSelector = typeMorpher.typeMorphInfo(selector.type);
                     private Name selectorName = getSyntheticName("selector");
 
                     protected JCExpression resultValue() {
                         // create two accesses to the value of to selector field -- selector$.blip
                         // one for the method call and one for the nul test
                         JCExpression transSelector = makeGetField(selectorName, tmiSelector);
                         JCExpression toTest = makeGetField(selectorName, tmiSelector);
 
                         // construct the actual select
                         JCExpression selectExpr = m().Select(transSelector, tree.getIdentifier());
 
                         // test the selector for null before attempting to select the field
                         // if it would dereference null, then instead give the default value
                         JCExpression cond = m().Binary(JCTree.NE, toTest, make.Literal(TypeTags.BOT, null));
                         JCExpression defaultExpr = makeDefaultValue(diagPos, tmiResult);
                         return m().Conditional(cond, selectExpr, defaultExpr);
                     }
 
                     @Override
                     protected void buildFields() {
                         // translate the selector into a Location field of the BindingExpression
                         // XxxLocation selector$ = ...;
                         buildArgField(translate(selector), selector.type, selectorName.toString());
                     }
                 }).doit();
             }
         }
     }
 
     @Override
     public void visitIdent(JFXIdent tree)   {  //TODO: don't use toJava
        // assert (tree.sym.flags() & Flags.PARAMETER) != 0 || tree.name == names._this || tree.sym.isStatic() || toJava.requiresLocation(typeMorpher.varMorphInfo(tree.sym)) : "we are bound, so should have been marked to morph: " + tree;
         JCExpression transId = toJava.translateAsLocation(tree);
         result = convert(tree.type, transId );
     }
 
     @Override
     public void visitSequenceExplicit(JFXSequenceExplicit tree) { //done
         ListBuffer<JCStatement> stmts = ListBuffer.lb();
         Type elemType = boxedElementType(targetType(tree.type));
         UseSequenceBuilder builder = toJava.useBoundSequenceBuilder(tree.pos(), elemType, tree.getItems().length());
         stmts.append(builder.makeBuilderVar());
         for (JFXExpression item : tree.getItems()) {
             stmts.append(builder.addElement( item ) );
         }
         result = makeBlockExpression(tree.pos(), stmts, builder.makeToSequence());
     }
 
     @Override
     public void visitSequenceRange(JFXSequenceRange tree) { //done: except for step and exclusive
         DiagnosticPosition diagPos = tree.pos();
         Type elemType = syms.javafx_IntegerType;
         int ltag = tree.getLower().type.tag;
         int utag = tree.getUpper().type.tag;
         int stag = tree.getStepOrNull() == null? TypeTags.INT : tree.getStepOrNull().type.tag;
         if (ltag == TypeTags.FLOAT || ltag == TypeTags.DOUBLE ||
                 utag == TypeTags.FLOAT || utag == TypeTags.DOUBLE ||
                 stag == TypeTags.FLOAT || stag == TypeTags.DOUBLE) {
             elemType = syms.javafx_NumberType;
         }
         TypeMorphInfo tmi = typeMorpher.typeMorphInfo(elemType);
         ListBuffer<JCExpression> args = ListBuffer.lb();
         args.append( translate( tree.getLower(), tmi ));
         args.append( translate( tree.getUpper(), tmi ));
         if (tree.getStepOrNull() != null) {
             args.append( translate( tree.getStepOrNull(), tmi ));
         }
         if (tree.isExclusive()) {
             args.append( make.at(diagPos).Literal(TypeTags.BOOLEAN, 1) );
         }
         result = convert(types.sequenceType(elemType), runtime(diagPos, cBoundSequences, "range", args));
     }
 
     @Override
     public void visitSequenceEmpty(JFXSequenceEmpty tree) { //done
         DiagnosticPosition diagPos = tree.pos();
         if (types.isSequence(tree.type)) {
             Type elemType = types.elementType(targetType(tree.type));
             result = runtime(diagPos, cBoundSequences, "empty", List.of(makeTypeInfo(diagPos, elemType)));
         } else {
             result = makeConstantLocation(diagPos, targetType(tree.type), makeNull(diagPos));
         }
     }
 
     @Override
     public void visitSequenceIndexed(JFXSequenceIndexed tree) {   //done
         DiagnosticPosition diagPos = tree.pos();
         result = convert(tree.type, runtime(diagPos, cBoundSequences, "element",
                 List.of(translate(tree.getSequence()),
                 translate(tree.getIndex(), syms.intType))));
     }
 
     @Override
     public void visitSequenceSlice(JFXSequenceSlice tree) {    //done
         DiagnosticPosition diagPos = tree.pos();
         result = runtime(diagPos, cBoundSequences,
                 tree.getEndKind()==SequenceSliceTree.END_EXCLUSIVE? "sliceExclusive" : "slice",
                 List.of(
                     makeTypeInfo(diagPos, types.elementType(targetType(tree.type))),
                     translate(tree.getSequence()),
                     translate(tree.getFirstIndex()),
                     tree.getLastIndex()==null? makeNull(diagPos) : translate(tree.getLastIndex())
                     ));
     }
 
     /**
      * Generate this template, expanding to handle multiple in-clauses
      *
      *  SequenceLocation<V> derived = new BoundComprehension<T,V>(..., IN_SEQUENCE, USE_INDEX) {
             protected SequenceLocation<V> getMappedElement$(final ObjectLocation<T> IVAR_NAME, final IntLocation INDEXOF_IVAR_NAME) {
                 return SequenceVariable.make(...,
                                              new SequenceBindingExpression<V>() {
                                                  public Sequence<V> computeValue() {
                                                      if (WHERE)
                                                          return BODY with s/indexof IVAR_NAME/INDEXOF_IVAR_NAME/;
                                                      else
                                                         return ....emptySequence;
                                                  }
                                              }, maybe IVAR_NAME, maybe INDEXOF_IVAR_NAME);
             }
         };
      *
      * **/
     @Override
     public void visitForExpression(final JFXForExpression tree) {
         result = (new Translator( tree.pos(), toJava ) {
 
             private final TypeMorphInfo tmiResult = typeMorpher.typeMorphInfo(targetType(tree.type));
 
             /**
              * V
              */
             private final Type resultElementType = tmiResult.getElementType();
 
             /**
              * SequenceLocation<V>
              */
             private final Type resultSequenceLocationType = typeMorpher.generifyIfNeeded(typeMorpher.locationType(TYPE_KIND_SEQUENCE), tmiResult);
             
             /**
              * isSimple -- true if the for-loop is simple enough that it can use SimpleBoundComprehension
              */
             private final boolean isSimple = false;  //TODO
 
             /**
              * Make:  V.class
              */
             private JCExpression makeResultClass() {
                 return makeTypeInfo(diagPos, resultElementType);
             }
 
             /**
              * Make a method parameter
              */
             private JCVariableDecl makeParam(Type type, Name name) {
                 return make.at(diagPos).VarDef(
                         make.Modifiers(Flags.PARAMETER | Flags.FINAL),
                         name,
                         makeExpression(type),
                         null);
 
             }
 
             /**
              * Starting with the body of the comprehension...
              * Wrap in a singleton sequence, if not a sequence.
              * Wrap in a conditional if there are where-clauses:   whereClause? body : []
              */
             private JCExpression makeCore() {
                 JCExpression body = translate(tree.getBodyExpression());
                 if (!types.isSequence(tree.getBodyExpression().type)) {
                     List<JCExpression> args = List.of(makeResultClass(), body);
                     body = runtime(diagPos, cBoundSequences, "singleton", args);
                 }
                 JCExpression whereTest = null;
                 for (JFXForExpressionInClause clause : tree.getForExpressionInClauses()) {
                     JCExpression where = translate(clause.getWhereExpression());
                     if (where != null) {
                         if (whereTest == null) {
                             whereTest = where;
                         } else {
                             whereTest = runtime(diagPos, cBoundOperators, "and_bb", List.of(whereTest, where));
                         }
                     }
                 }
                 if (whereTest != null) {
                     body = makeBoundConditional(diagPos,
                             tree.type,
                             body,
                             runtime(diagPos, cBoundSequences, "empty", List.of(makeTypeInfo(diagPos, resultElementType))),
                             whereTest);
                 }
                 return body;
             }
 
             /**
              *  protected SequenceLocation<V> computeElements$(final ObjectLocation<T> IVAR_NAME, final IntLocation INDEXOF_IVAR_NAME) {
              *     return ...
              *  }
              */
             private JCTree makeComputeElementsMethod(JFXForExpressionInClause clause, JCExpression inner, TypeMorphInfo tmiInduction) {
                 Type iVarType;
                 Type idxVarType;
                 Name computeName;
                 if  (isSimple) {
                    iVarType = tmiInduction.getRealFXType();
                    idxVarType = syms.intType;
                    computeName = computeElementsName;
                 } else {
                    iVarType = tmiInduction.getLocationType();
                    idxVarType = typeMorpher.locationType(TYPE_KIND_INT);
                    computeName = computeElementsName;
                 }
                 ListBuffer<JCStatement> stmts = ListBuffer.lb();
                 Name ivarName = clause.getVar().name;
                 stmts.append(m().Return( inner ));
                 List<JCVariableDecl> params = List.of(
                         makeParam(iVarType, ivarName),
                         makeParam(idxVarType, indexVarName(clause) )
                         );
                 return m().MethodDef(
                         m().Modifiers(Flags.PROTECTED),
                         computeName,
                         makeExpression( resultSequenceLocationType ),
                         List.<JCTypeParameter>nil(),
                         params,
                         List.<JCExpression>nil(),
                         m().Block(0L, stmts.toList()),
                         null);
             }
 
             /**
              * new BoundComprehension<T,V>(V.class, IN_SEQUENCE, USE_INDEX) { ... }
              */
             private JCExpression makeBoundComprehension(JFXForExpressionInClause clause, JCExpression inner) {
                 JFXExpression seq = clause.getSequenceExpression();
                 TypeMorphInfo tmiSeq = typeMorpher.typeMorphInfo(seq.type);
                 TypeMorphInfo tmiInduction = typeMorpher.typeMorphInfo(clause.getVar().type);
                 Type elementType = tmiSeq.getElementType();
                 JCClassDecl classDecl = m().AnonymousClassDef(
                         m().Modifiers(0L),
                         List.<JCTree>of(makeComputeElementsMethod(clause, inner, tmiInduction)));
                 List<JCExpression> typeArgs = List.nil();
                 boolean useIndex = clause.getIndexUsed();
                 JCExpression transSeq = translate( seq );
                 if (!tmiSeq.isSequence()) {
                     transSeq = runtime(diagPos, cBoundSequences, "singleton", List.of(makeResultClass(), transSeq));
                 }
                 List<JCExpression> constructorArgs = List.of(
                         makeResultClass(),
                         transSeq,
                         m().Literal(TypeTags.BOOLEAN, useIndex? 1 : 0) );
                 //JCExpression clazz = makeQualifiedTree(diagPos, "com.sun.javafx.runtime.sequence.BoundComprehension");
                 int typeKind = tmiInduction.getTypeKind();
                 Type bcType = typeMorpher.boundComprehensionNCT[typeKind].type;
                 JCExpression clazz = makeExpression(types.erasure(bcType));  // type params added below, so erase formals
                 ListBuffer<JCExpression> typeParams = ListBuffer.lb();
                 if (typeKind == TYPE_KIND_OBJECT) {
                     typeParams.append( makeExpression(elementType) );
                 }
                 typeParams.append( makeExpression(resultElementType) );
                 clazz = m().TypeApply(clazz, typeParams.toList());
                 return m().NewClass(null,
                         typeArgs,
                         clazz,
                         constructorArgs,
                         classDecl);
             }
 
             /**
              * Put everything together, handle multiple in clauses -- wrap from the inner-most first
              */
             public JCExpression doit() {
                 List<JFXForExpressionInClause> clauses = tree.getForExpressionInClauses();
                 // make the body of loop
                 JCExpression expr = makeCore();
                 // then wrap it in the looping constructs
                 for (int inx = clauses.size() - 1; inx >= 0; --inx) {
                     JFXForExpressionInClause clause = clauses.get(inx);
                     expr = makeBoundComprehension(clause, expr);
                 }
                 return expr;
             }
         }).doit();
     }
 
     public void visitIndexof(JFXIndexof tree) {
         assert tree.clause.getIndexUsed() : "assert that index used is set correctly";
         JCExpression transIndex = make.at(tree.pos()).Ident(indexVarName(tree.fname));
         VarSymbol vsym = (VarSymbol)tree.clause.getVar().sym;
         if (toJava.requiresLocation(vsym)) {
             // from inside the bind, already a Location
             result = convert(tree.type, transIndex);
         } else {
             // it came from outside of the bind, make it into a Location
             result = makeConstantLocation(tree.pos(), targetType(tree.type), transIndex);
         }
     }
 
     /**
      * Build a tree for a conditional.
      * @param diagPos
      * @param resultType
      * @param trueExpr then branch, already translated
      * @param falseExpr else  branch, already translated
      * @param condExpr conditional expression  branch, already translated
      * @return
      */
     private JCExpression makeBoundConditional(final DiagnosticPosition diagPos,
             final Type resultType,
             final JCExpression trueExpr,
             final JCExpression falseExpr,
             final JCExpression condExpr) {
         TypeMorphInfo tmi = (tmiTarget != null) ? tmiTarget : typeMorpher.typeMorphInfo(resultType);
         List<JCExpression> args = List.of(
                 makeLaziness(diagPos),
                 condExpr,
                 makeFunction0(resultType, trueExpr),
                 makeFunction0(resultType, falseExpr));
         if (tmi.isSequence()) {
             // prepend "Foo.class, "
             args = args.prepend(makeTypeInfo(diagPos, tmi.getElementType()));
         }
         return runtime(diagPos, cBoundOperators, "makeBoundIf", args);
     }
 
     private JCExpression makeFunction0(
             final Type resultType,
             final JCExpression bodyExpr) {
         return (new ClosureTranslator(bodyExpr.pos(), resultType) {
 
             protected List<JCTree> makeBody() {
                 return List.<JCTree>of(
                         makeClosureMethod("invoke", bodyExpr, null, tmiResult.getLocationType(), Flags.PUBLIC));
             }
 
             protected JCExpression makeBaseClass() {
                 JCExpression objFactory = makeQualifiedTree(diagPos, cFunction0);
                 Type clazzType = tmiResult.getLocationType();
                 JCExpression clazz = makeExpression(clazzType);
                 return m().TypeApply(objFactory, List.of(clazz));
             }
 
             protected List<JCExpression> makeConstructorArgs() {
                 return List.<JCExpression>nil();
             }
         }).doit();
     }
 
     @Override
     public void visitIfExpression(final JFXIfExpression tree) {
         Type targetType = targetType(tree.type);
         result = makeBoundConditional(tree.pos(),
                 targetType,
                 translate(tree.getTrueExpression(), targetType),
                 translate(tree.getFalseExpression(), targetType),
                 translate(tree.getCondition()) );
     }
 
     @Override
     public void visitParens(JFXParens tree) { //done
         JCExpression expr = translate(tree.expr);
         result = make.at(tree.pos).Parens(expr);
     }
 
     @Override
     public void visitInstanceOf(final JFXInstanceOf tree) {
         result = new BindingExpressionClosureTranslator(tree.pos(), tree.type) {
 
             protected JCExpression resultValue() {
                 return m().TypeTest(
                         buildArgField(translate(tree.expr), tree.expr.type, "toTest"),
                         makeExpression(tree.clazz.type) );
             }
         }.doit();
     }
 
     @Override
     public void visitTypeCast(final JFXTypeCast tree) {
         result = new BindingExpressionClosureTranslator(tree.pos(), tree.type) {
 
            protected JCExpression resultValue() {
                 return makeTypeCast(tree.pos(), tree.clazz.type, tree.expr.type,
                          buildArgField(translate(tree.expr), tree.expr.type, "toBeCast"));
             }
         }.doit();
     }
 
     @Override
     public void visitLiteral(JFXLiteral tree) {
         final DiagnosticPosition diagPos = tree.pos();
         if (tree.typetag == TypeTags.BOT && types.isSequence(tree.type)) {
             Type elemType = types.elementType(targetType(tree.type));
             result = runtime(diagPos, cBoundSequences, "empty", List.of(makeTypeInfo(diagPos, elemType)));
         } else {
             JCExpression unbound = make.at(diagPos).Literal(tree.typetag, tree.value);
             result = makeConstantLocation(diagPos, targetType(tree.type), unbound);
         }
     }
 
     /**
      * Translator for Java method and non-bound JavaFX functions.
      */
     private abstract class BindingExpressionClosureTranslator extends ClosureTranslator {
 
         BindingExpressionClosureTranslator(DiagnosticPosition diagPos, Type resultType) {
             super(diagPos, resultType);
         }
 
         protected abstract JCExpression resultValue();
 
         protected void buildFields() {
             // by default do this dynamically
         }
 
         protected List<JCTree> makeBody() {
             buildFields();
             JCExpression resultVal = resultValue(); // build first since this may add dependencies
 
             // create a getStaticDependents method to set the args as static dependents
             Type locationType = typeMorpher.baseLocation.type;
             JCExpression depsArray = make.NewArray(makeExpression(locationType), List.<JCExpression>nil(), dependents.toList());
             Type depsReturnType = new Type.ArrayType(locationType, syms.arrayClass);
             members.append(makeClosureMethod("getStaticDependents", depsArray, null, depsReturnType, Flags.PROTECTED));
 
             members.append(makeClosureMethod("computeValue", resultVal, null, tmiResult.getRealFXType(), Flags.PUBLIC));
             return completeMembers();
         }
 
         protected JCExpression makeBaseClass() {
             Type clazzType = typeMorpher.bindingExpressionType(typeKindResult);
             return makeBaseClass(clazzType, null);
         }
 
         protected List<JCExpression> makeConstructorArgs() {
             return List.<JCExpression>nil();
         }
 
         @Override
         protected JCExpression doit() {
             ListBuffer<JCExpression> args = ListBuffer.lb();
             if (tmiResult.getTypeKind() == TYPE_KIND_OBJECT) {
                 args.append(makeDefaultValue(diagPos, tmiResult));
             }
             args.append(makeLaziness(diagPos));
             args.append(buildClosure());
             return makeLocationLocalVariable(tmiResult, diagPos, args.toList());
         }
     }
 
     @Override
     public void visitFunctionInvocation(final JFXFunctionInvocation tree) {
         result = (new FunctionCallTranslator(tree, toJava) {
 
             final List<JCExpression> typeArgs = toJava.translateExpressions(tree.typeargs); //TODO: should, I think, be nil list
             final List<JCExpression> targs = translate(tree.args, meth.type, usesVarArgs);
 
             public JCExpression doit() {
                 if (callBound) {
                     if (selectorMutable) {
                         return makeBoundSelect(diagPos,
                                 tree.type,
                                 translate(selector),
                                 new Function1ClosureTranslator(diagPos, tree.type, selector.type) {
 
                                     protected JCExpression makeInvokeMethodBody() {
                                         // create a field in the closure for each argument
                                         buildArgFields(targs, true);
 
                                         // translate the method name -- e.g., foo  to foo$bound
                                         Name name = functionName(msym, false, callBound);
 
                                         // selectors are always Objects
                                         JCExpression transSelect = makeGet(translate(selector), TYPE_KIND_OBJECT);
                                         JCExpression expr = m().Apply(typeArgs,
                                                 m().Select(transSelect, name),
                                                 callArgs.toList());
                                         return convert(tree.type, expr); // convert type, if needed
                                     }
                                 });
                     } else {
                         List<JCExpression> callArgs = targs;
                         if (superToStatic) {  //TODO: should this be higher?
                             // This is a super call, add the receiver so that the impl is called directly
                             callArgs = callArgs.prepend(make.Ident(defs.receiverName));
                         }
                         return convert(tree.type, m().Apply(typeArgs, transMeth(), callArgs));
                     }
                 } else {
                     // call to Java method or unbound JavaFX function
                     //TODO: varargs
                     if (selectorMutable || useInvoke) {
                         return (new BindingExpressionClosureTranslator(diagPos, tree.type) {
 
                             private JFXExpression check = useInvoke? meth : selector;
                             private TypeMorphInfo tmiSelector = typeMorpher.typeMorphInfo(check.type);
                             private Name selectorName = getSyntheticName("selector");
 
                             protected JCExpression resultValue() {
                                 // access the selector field for the method call-- selector$.get()
                                 // selectors are always Objects
                                 JCExpression transSelector = makeGetField(selectorName, TYPE_KIND_OBJECT);
 
                                 // construct the actual method invocation
                                 Name methName = useInvoke? defs.invokeName : ((JFXSelect) tree.meth).name;
                                 JCExpression callMeth = m().Select(transSelector, methName);
                                 JCExpression call = m().Apply(typeArgs, callMeth, callArgs.toList());
 
                                 if (tmiSelector.getTypeKind() == TYPE_KIND_OBJECT) {
                                     // create another access to the selector field for the null test (below)
                                     JCExpression toTest = makeGetField(selectorName, TYPE_KIND_OBJECT);
                                     // test the selector for null before attempting to invoke the method
                                     // if it would dereference null, then instead give the default value
                                     JCExpression cond = m().Binary(JCTree.NE, toTest, make.Literal(TypeTags.BOT, null));
                                     JCExpression defaultExpr = makeDefaultValue(diagPos, tmiResult);
                                     return m().Conditional(cond, call, defaultExpr);
                                 } else {
                                     return call;
                                 }
                             }
 
                             @Override
                             protected void buildFields() {
                                 // create a field in the BindingExpression for each argument
                                 buildArgFields(targs, false);
 
                                 // translate the method selector into a Location field of the BindingExpression
                                 // XxxLocation selector$ = ...;
                                 buildArgField(translate(check), check.type, selectorName.toString());
                             }
                         }).doit();
                     } else {
                         return (new BindingExpressionClosureTranslator(diagPos, tree.type) {
 
                             // construct the actual value computing method (with the method call)
                             protected JCExpression resultValue() {
                                 if (superToStatic) {  //TODO: should this be higher?
                                     // This is a super call, add the receiver so that the impl is called directly
                                     callArgs.prepend(make.Ident(defs.receiverName));
                                 }
                                 JCExpression transMeth = transMeth();
                                 if (msym != null && (msym.flags() & (Flags.PRIVATE|Flags.STATIC)) == Flags.PRIVATE &&
                                     transMeth instanceof JCFieldAccess) {
                                   JCFieldAccess selectTr = (JCFieldAccess) transMeth;
                                   callArgs.prepend(selectTr.getExpression());
                                   JCExpression receiverType = makeTypeTree(diagPos,msym.owner.type, false);
                                   transMeth = make.at(transMeth).Select(receiverType, functionName(msym, true, false));
                                   
                                 }
                                 return m().Apply(null, transMeth, callArgs.toList());
                             }
 
                             @Override
                             protected void buildFields() {
                                 // create a field in the BindingExpression for each argument
                                 buildArgFields(targs, false);
                             }
                         }).doit();
                     }
                 }
             }
 
             public JCExpression transMeth() {
                 assert !useInvoke;
                 JCExpression transMeth;
                 if (renameToSuper) {
                     transMeth = m().Select(m().Select(makeTypeTree(selector, toJava.attrEnv.enclClass.sym.type, false), names._super), msym);
                 } else {
                     transMeth = toJava.translateAsUnconvertedValue(meth);
                     if (superToStatic || callBound) {
                         // translate the method name -- e.g., foo  to foo$bound or foo$impl
                         Name name = functionName(msym, superToStatic, callBound);
                         if (transMeth.getTag() == JCTree.IDENT) {
                             transMeth = m().Ident(name);
                         } else if (transMeth.getTag() == JCTree.SELECT) {
                             JCExpression expr = superToStatic ? makeTypeTree(diagPos, msym.owner.type, false) : ((JCFieldAccess) transMeth).getExpression();
                             transMeth = m().Select(expr, name);
                         }
                     }
                 }
                 return transMeth;
             }
         }).doit();
     }
 
     private class BinaryTranslator {
         final JFXBinary tree;
         final DiagnosticPosition diagPos;
         final JFXExpression l;
         final JFXExpression r;
         final boolean lBoxed;
         final boolean rBoxed;
         final Type lType;
         final Type rType;
 
         BinaryTranslator(final JFXBinary tree) {
             this.tree = tree;
             this.diagPos = tree.pos();
             this.l = tree.lhs;
             this.r = tree.rhs;
             Type tubl = types.unboxedType(tree.lhs.type);
             lBoxed = tubl.tag != TypeTags.NONE;
             lType = lBoxed? tubl : tree.lhs.type;
             Type tubr = types.unboxedType(tree.rhs.type);
             rBoxed = tubr.tag != TypeTags.NONE;
             rType = rBoxed? tubr : tree.rhs.type;
         }
 
         String typeString() {
             if (types.isSameType(rType, syms.doubleType) || types.isSameType(lType, syms.doubleType)) {
                 return "double";
             }
             if (types.isSameType(rType, syms.floatType) || types.isSameType(lType, syms.floatType)) {
                 return "float";
             }
             if (types.isSameType(rType, syms.longType) || types.isSameType(lType, syms.longType)) {
                 return "long";
             }
             return "int";
         }
 
         JCExpression makeBinaryOperator(String op, String prefix) {
             final JCExpression lhs = translate(l);
             final JCExpression rhs = translate(r);
             return runtime(diagPos, cBoundOperators, prefix + typeString(), List.of(makeLaziness(diagPos), lhs, rhs, makeQualifiedTree(diagPos, op)));
         }
 
         JCExpression makeBinaryArithmeticOperator(String op) {
             return makeBinaryOperator(op, "op_");
         }
 
         JCExpression makeBinaryComparisonOperator(String op) {
             return makeBinaryOperator(op, "cmp_");
         }
 
         boolean isNumeric(Type opType) {
             switch (opType.tag) {
                 case BYTE:
                 case CHAR:
                 case SHORT:
                 case INT:
                 case LONG:
                 case FLOAT:
                 case DOUBLE:
                     return true;
             }
             return false;
         }
 
         JCExpression makeBinaryEqualityOperator(String op) {
             if (isNumeric(lType) && isNumeric(rType)) {
                 return makeBinaryComparisonOperator(op);
             } else {
                 final JCExpression lhs = translate(l);
                 final JCExpression rhs = translate(r);
                 String methodName = (lType.tag == BOOLEAN && rType.tag == BOOLEAN) ? "op_boolean" : "cmp_other";
                 return runtime(diagPos, cBoundOperators, methodName, List.of(makeLaziness(diagPos), lhs, rhs, makeQualifiedTree(diagPos, op)));
             }
         }
 
         JCExpression doit() {
             switch (tree.getFXTag()) {
                 case PLUS:
                     return makeBinaryArithmeticOperator(opPLUS);
                 case MINUS:
                     return makeBinaryArithmeticOperator(opMINUS);
                 case DIV:
                     return makeBinaryArithmeticOperator(opDIVIDE);
                 case MUL:
                     return makeBinaryArithmeticOperator(opTIMES);
                 case MOD:
                     return makeBinaryArithmeticOperator(opMODULO);
 
                 case LT:
                     return makeBinaryComparisonOperator(opLT);
                 case LE:
                     return makeBinaryComparisonOperator(opLE);
                 case GT:
                     return makeBinaryComparisonOperator(opGT);
                 case GE:
                     return makeBinaryComparisonOperator(opGE);
 
                 case EQ:
                     return makeBinaryEqualityOperator(opEQ);
                 case NE:
                     return makeBinaryEqualityOperator(opNE);
 
                 case AND:
                     return makeBoundConditional(diagPos,
                             syms.booleanType,
                             translate(r, syms.booleanType),
                             makeConstantLocation(diagPos, syms.booleanType, makeLit(diagPos, syms.booleanType, 0)),
                             translate(l, syms.booleanType));
                 case OR:
                     return makeBoundConditional(diagPos,
                             syms.booleanType,
                             makeConstantLocation(diagPos, syms.booleanType, makeLit(diagPos, syms.booleanType, 1)),
                             translate(r, syms.booleanType),
                             translate(l, syms.booleanType));
                 default:
                     assert false : "unhandled binary operator";
                     return translate(l);
             }
         }
     }
 
     @Override
     public void visitBinary(final JFXBinary tree) {
         result = convert(tree.type, new BinaryTranslator(tree).doit());
     }
 
     @Override
     public void visitUnary(final JFXUnary tree) {
         DiagnosticPosition diagPos = tree.pos();
         JFXExpression expr = tree.getExpression();
         JCExpression transExpr = translate(expr);
         JCExpression res;
 
         switch (tree.getFXTag()) {
             case SIZEOF:
                 res = runtime(diagPos, cBoundSequences, "sizeof", List.of(transExpr) );
                 break;
             case REVERSE:
                 res = runtime(diagPos, cBoundSequences, "reverse", List.of(transExpr) );
                 break;
             case NOT:
                 res = runtime(diagPos, cBoundOperators, "op_boolean", List.of(makeLaziness(diagPos), transExpr, make.Literal(TypeTags.BOT, null), makeQualifiedTree(diagPos, opNOT)));
                 break;
             case NEG:
                 if (types.isSameType(tree.type, syms.javafx_DurationType)) {   
                     //TODO: totally wrong
                     res = make.at(diagPos).Apply(null,
                             make.at(diagPos).Select(translate(tree.arg), names.fromString("negate")), List.<JCExpression>nil());
                 } else {
                     Type t = expr.type;
                     Type tub = types.unboxedType(t);
                     if (tub.tag != TypeTags.NONE) {
                         t = tub;
                     }
                     String typeString = (types.isSameType(t, syms.doubleType)) ? "double" : (types.isSameType(t, syms.floatType)) ? "float" : (types.isSameType(t, syms.longType)) ? "long" : "int";
                     res = runtime(diagPos, cBoundOperators, "op_" + typeString, List.of(makeLaziness(diagPos), transExpr, make.Literal(TypeTags.BOT, null), makeQualifiedTree(diagPos, opNEGATE)));
                 }
                 break;
             case PREINC:
             case PREDEC:
             case POSTINC:
             case POSTDEC:
                 // should have caught this in attribution
                 assert false : "++/-- in bind context f";
                 res = transExpr;
                 break;
             default:
                 assert false : "unhandled unary operator";
                 res = transExpr;
                 break;
         }
         result = convert(tree.type, res);
     }
 
     @Override
     public void visitTimeLiteral(JFXTimeLiteral tree) {
         //TODO: code should be something like the below, but this requires a similar change to visitInterpolateValue
         /***
            DiagnosticPosition diagPos = tree.pos();
            JCExpression unbound = toJava.translate(tree, Wrapped.InNothing);
            result = makeConstantLocation(diagPos, targetType(tree.type), unbound);
          */
 
         // convert this time literal to a javafx.lang.Duration.valueOf() invocation
         JFXFunctionInvocation duration = timeLiteralToDuration(tree);
 
         // now convert that FX invocation to Java
         visitFunctionInvocation(duration); // sets result
     }
 
     public void visitInterpolateValue(final JFXInterpolateValue tree) {
         result = new BindingExpressionClosureTranslator(tree.pos(), tree.type) {
 
             protected JCExpression resultValue() {
                 JCExpression kv = new InterpolateValueTranslator(tree, toJava) {
 
                     @Override
                     void setInstanceVariable(DiagnosticPosition diagPos, Name instName, JavafxBindStatus bindStatus, VarSymbol vsym, JCExpression transInit) {
                         JCExpression initRef = buildArgField(
                                 transInit,
                                 vsym.type,
                                 vsym.name.toString() + "$attr",
                                 bindStatus.isBound());
                         super.setInstanceVariable(diagPos, instName, bindStatus, vsym, initRef);
                     }
 
                     @Override
                     protected JCExpression translateInstanceVariableInit(JFXExpression init, JavafxBindStatus bindStatus, VarSymbol vsym) {
                         return translate(init, bindStatus, vsym.type);
                     }
 
                     protected JCExpression translateTarget() {
                         JCExpression target = translate(tree.attribute);
                         JCExpression val = toJava.callExpression(diagPos, makeExpression(syms.javafx_PointerType), "make", target);
                         return makeConstantLocation(diagPos, syms.javafx_KeyValueTargetType, val);
                     }
                 }.doit();
                return tmiTarget==null? kv : toJava.convertTranslated(kv, diagPos, syms.javafx_KeyValueType, tmiTarget.getRealFXType());
             }
         }.doit();
     }
 
     /***********************************************************************
      *
      * Utilities
      *s
      */
 
     protected String getSyntheticPrefix() {
         return "bfx$";
     }
     
     /***********************************************************************
      *
      * Moot visitors
      *
      */
 
     @Override
     public void visitForExpressionInClause(JFXForExpressionInClause that) {
         assert false : "should be processed by parent tree";
     }
 
     @Override
     public void visitModifiers(JFXModifiers tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitSkip(JFXSkip tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitThrow(JFXThrow tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitTry(JFXTry tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitWhileLoop(JFXWhileLoop tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitOverrideClassVar(JFXOverrideClassVar tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitOnReplace(JFXOnReplace tree) {
         assert false : "should not be processed as part of a binding";
     }
 
 
     @Override
     public void visitScript(JFXScript tree) {
         assert false : "should not be processed as part of a binding";
    }
 
     @Override
     public void visitClassDeclaration(JFXClassDeclaration tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitInitDefinition(JFXInitDefinition tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     public void visitPostInitDefinition(JFXPostInitDefinition tree) {
         assert false : "should not be processed as part of a binding";
     }
 
    @Override
     public void visitFunctionDefinition(JFXFunctionDefinition tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitSequenceInsert(JFXSequenceInsert tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitSequenceDelete(JFXSequenceDelete tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitContinue(JFXContinue tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitReturn(JFXReturn tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitImport(JFXImport tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitBreak(JFXBreak tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitCatch(JFXCatch tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitTypeAny(JFXTypeAny that) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitTypeClass(JFXTypeClass that) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitTypeFunctional(JFXTypeFunctional that) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitTypeUnknown(JFXTypeUnknown that) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitObjectLiteralPart(JFXObjectLiteralPart that) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitVarScriptInit(JFXVarScriptInit tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitVar(JFXVar tree) {
         // this is handled in translarVar
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitKeyFrameLiteral(JFXKeyFrameLiteral tree) {
         assert false : "should not be processed as part of a binding";
     }
 
     @Override
     public void visitErroneous(JFXErroneous tree) {
         assert false : "erroneous nodes shouldn't have gotten this far";
     }
 }
