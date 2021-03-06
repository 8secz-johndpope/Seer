 /*
  * Copyright 1999-2007 Sun Microsystems, Inc.  All Rights Reserved.
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
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.ArrayList;
 import java.util.Set;
 import javax.lang.model.element.ElementKind;
 import javax.tools.JavaFileObject;
 
 import com.sun.javafx.api.tree.ForExpressionInClauseTree;
 import com.sun.javafx.api.tree.TypeTree.Cardinality;
 import com.sun.tools.javac.code.*;
 import static com.sun.tools.javac.code.Flags.*;
 import static com.sun.tools.javac.code.Flags.ANNOTATION;
 import static com.sun.tools.javac.code.Flags.BLOCK;
 import static com.sun.tools.javac.code.Kinds.*;
 import static com.sun.tools.javac.code.Kinds.ERRONEOUS;
 import com.sun.tools.javac.code.Symbol.*;
 import com.sun.tools.javac.code.Type.*;
 import static com.sun.tools.javac.code.TypeTags.*;
 import static com.sun.tools.javac.code.TypeTags.WILDCARD;
 import com.sun.tools.javac.comp.*;
 import com.sun.tools.javac.jvm.ByteCodes;
 import com.sun.tools.javac.jvm.Target;
 import com.sun.tools.javac.util.*;
 import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
 import com.sun.tools.javafx.code.*;
 import com.sun.tools.javafx.tree.*;
 import com.sun.tools.javafx.util.MsgSym;
 import com.sun.tools.javafx.util.NotImplementedException;
 import static com.sun.tools.javafx.code.JavafxFlags.SCRIPT_LEVEL_SYNTH_STATIC;
 import com.sun.tools.javafx.comp.JavafxCheck.WriteKind;
 
 /** This is the main context-dependent analysis phase in GJC. It
  *  encompasses name resolution, type checking and constant folding as
  *  subtasks. Some subtasks involve auxiliary classes.
  *  @see Check
  *  @see Resolve
  *  @see ConstFold
  *  @see Infer
  *
  * This class is interleaved with {@link JavafxMemberEnter}, which is used
  * to enter declarations into a local scope.
  *
  *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
  *  you write code that depends on this, you do so at your own risk.
  *  This code and its internal interfaces are subject to change or
  *  deletion without notice.</b>
  */
 public class JavafxAttr implements JavafxVisitor {
     protected static final Context.Key<JavafxAttr> javafxAttrKey =
         new Context.Key<JavafxAttr>();
 
     /*
      * modules imported by context
      */
     private final JavafxDefs defs;
     private final Name.Table names;
     private final Log log;
     private final JavafxResolve rs;
     private final JavafxSymtab syms;
     private final JavafxCheck chk;
     private final JavafxMemberEnter memberEnter;
     private final JavafxTreeMaker fxmake;
     private final ConstFold cfolder;
     private final JavafxEnter enter;
     private final Target target;
     private final JavafxTypes types;
     private final Annotate annotate;

     /*
      * other instance information
      */
     private final Source source;
     
     Map<JavafxVarSymbol, JFXVar> varSymToTree =
             new HashMap<JavafxVarSymbol, JFXVar>();
     Map<MethodSymbol, JFXFunctionDefinition> methodSymToTree =
             new HashMap<MethodSymbol, JFXFunctionDefinition>();
 
     public static JavafxAttr instance(Context context) {
         JavafxAttr instance = context.get(javafxAttrKey);
         if (instance == null)
             instance = new JavafxAttr(context);
         return instance;
     }
 
     protected JavafxAttr(Context context) {
         context.put(javafxAttrKey, this);
 
         defs = JavafxDefs.instance(context);
         syms = (JavafxSymtab)JavafxSymtab.instance(context);
         names = Name.Table.instance(context);
         log = Log.instance(context);
         rs = JavafxResolve.instance(context);
         chk = JavafxCheck.instance(context);
         memberEnter = JavafxMemberEnter.instance(context);
         fxmake = (JavafxTreeMaker)JavafxTreeMaker.instance(context);
         enter = JavafxEnter.instance(context);
         cfolder = ConstFold.instance(context);
         target = Target.instance(context);
         types = JavafxTypes.instance(context);
         annotate = Annotate.instance(context);
 
         Options options = Options.instance(context);
 
         source = Source.instance(context);
         allowGenerics = source.allowGenerics();
         allowVarargs = source.allowVarargs();
         allowBoxing = source.allowBoxing();
         allowCovariantReturns = source.allowCovariantReturns();
         allowAnonOuterThis = source.allowAnonOuterThis();
         relax = (options.get("-retrofit") != null ||
                  options.get("-relax") != null);
 
     }
     /** Switch: relax some constraints for retrofit mode.
      */
     private boolean relax;
 
     /** Switch: support generics?
      */
     private boolean allowGenerics;
 
     /** Switch: allow variable-arity methods.
      */
     private boolean allowVarargs;
 
     /** Switch: support boxing and unboxing?
      */
     private boolean allowBoxing;
 
     /** Switch: support covariant result types?
      */
     private boolean allowCovariantReturns;
 
     /** Switch: allow references to surrounding object from anonymous
      * objects during constructor call?
      */
     private boolean allowAnonOuterThis;
 
     enum Sequenceness {
         DISALLOWED,
         PERMITTED,
         REQUIRED
     }
 
     /** Check kind and type of given tree against protokind and prototype.
      *  If check succeeds, store type in tree and return it.
      *  If check fails, store errType in tree and return it.
      *  No checks are performed if the prototype is a method type.
      *  Its not necessary in this case since we know that kind and type
      *  are correct.
      *
      *  @param tree     The tree whose kind and type is checked
      *  @param owntype  The computed type of the tree
      *  @param ownkind  The computed kind of the tree
      *  @param pkind    The expected kind (or: protokind) of the tree
      *  @param pt       The expected type (or: prototype) of the tree
      */
     Type check(JFXTree tree, Type owntype, int ownkind, int pkind, Type pt, Sequenceness pSequenceness) {
         if (owntype != null && owntype != syms.javafx_UnspecifiedType && owntype.tag != ERROR && pt.tag != METHOD && pt.tag != FORALL) {
 //        if (owntype.tag != ERROR && pt.tag != METHOD && pt.tag != FORALL) {
             if ((pkind & VAL) != 0 && ownkind == MTH) {
                 ownkind = VAL;
                 if (owntype instanceof MethodType) {
                     owntype = chk.checkFunctionType(tree.pos(), (MethodType)owntype);
                 }
             }
             if ((ownkind & ~pkind) == 0) {
                 owntype = chk.checkType(tree.pos(), owntype, pt, pSequenceness);
             } else {
                 log.error(tree.pos(), MsgSym.MESSAGE_UNEXPECTED_TYPE,
                           Resolve.kindNames(pkind),
                           Resolve.kindName(ownkind));
                 owntype = syms.errType;
             }
         }
         tree.type = owntype;
         return owntype;
     }
 
     /** Is this symbol a type?
      */
     static boolean isType(Symbol sym) {
         return sym != null && sym.kind == TYP;
     }
 
     /** The current `this' symbol.
      *  @param env    The current environment.
      */
     Symbol thisSym(DiagnosticPosition pos, JavafxEnv<JavafxAttrContext> env) {
         return rs.resolveSelf(pos, env, env.enclClass.sym, names._this);
     }
 
 /* ************************************************************************
  * Visitor methods
  *************************************************************************/
 
     /** Visitor argument: the current environment.
      */
     private JavafxEnv<JavafxAttrContext> env;
 
     /** Visitor argument: the currently expected proto-kind.
      */
     int pkind;
 
     /** Visitor argument: the currently expected proto-type.
      */
     Type pt;
 
     /** Visitor argument: is a sequence permitted
      */
     private Sequenceness pSequenceness;
 
     /** Visitor result: the computed type.
      */
     private Type result;
 
     /** Visitor method: attribute a tree, catching any completion failure
      *  exceptions. Return the tree's type.
      *
      *  @param tree    The tree to be visited.
      *  @param env     The environment visitor argument.
      *  @param pkind   The protokind visitor argument.
      *  @param pt      The prototype visitor argument.
      */
     Type attribTree(JFXTree tree, JavafxEnv<JavafxAttrContext> env, int pkind, Type pt) {
         return attribTree(tree, env, pkind, pt, pSequenceness);
     }
 
     Type attribTree(JFXTree tree, JavafxEnv<JavafxAttrContext> env, int pkind, Type pt, Sequenceness pSequenceness) {
         JavafxEnv<JavafxAttrContext> prevEnv = this.env;
         int prevPkind = this.pkind;
         Type prevPt = this.pt;
         Sequenceness prevSequenceness = this.pSequenceness;
         try {
             this.env = env;
             this.pkind = pkind;
             this.pt = pt;
             this.pSequenceness = pSequenceness;
             if (tree != null )tree.accept(this);
             if (tree == breakTree)
                 throw new BreakAttr(env);
             return result;
         } catch (CompletionFailure ex) {
             tree.type = syms.errType;
             return chk.completionError(tree.pos(), ex);
         } finally {
             this.env = prevEnv;
             this.pkind = prevPkind;
             this.pt = prevPt;
             this.pSequenceness = prevSequenceness;
         }
     }
 
     /** Derived visitor method: attribute an expression tree.
      */
     public Type attribExpr(JFXTree tree, JavafxEnv<JavafxAttrContext> env, Type pt, Sequenceness pSequenceness) {
         return attribTree(tree, env, VAL, pt.tag != ERROR ? pt : Type.noType, pt.tag != ERROR ? pSequenceness : Sequenceness.PERMITTED);
     }
 
     /** Derived visitor method: attribute an expression tree.
      *  allow a sequence is no proto-type is specified, the proto-type is a seqeunce,
      *  or the proto-type is an error.
      */
     public Type attribExpr(JFXTree tree, JavafxEnv<JavafxAttrContext> env, Type pt) {
         return attribTree(tree, env, VAL, pt.tag != ERROR ? pt : Type.noType,
                 (pt.tag == ERROR || pt == Type.noType)?
                         Sequenceness.PERMITTED :
                         types.isSequence(pt)? Sequenceness.REQUIRED : Sequenceness.DISALLOWED);
     }
 
     /** Derived visitor method: attribute an expression tree with
      *  no constraints on the computed type.
      */
     Type attribExpr(JFXTree tree, JavafxEnv<JavafxAttrContext> env) {
         return attribTree(tree, env, VAL, Type.noType, Sequenceness.PERMITTED);
     }
 
     /** Derived visitor method: attribute a type tree.
      */
     Type attribType(JFXTree tree, JavafxEnv<JavafxAttrContext> env) {
         Type localResult = attribTree(tree, env, TYP, Type.noType, Sequenceness.DISALLOWED);
         return localResult;
     }
 
     /** Derived visitor method: attribute a statement or definition tree.
      */
     public Type attribDecl(JFXTree tree, JavafxEnv<JavafxAttrContext> env) {
         return attribTree(tree, env, NIL, Type.noType, Sequenceness.DISALLOWED);
     }
 
     public Type attribVar(JFXVar tree, JavafxEnv<JavafxAttrContext> env) {
         memberEnter.memberEnter(tree, env);
         return attribExpr(tree, env);
     }
 
     /** Attribute a list of expressions, returning a list of types.
      */
     List<Type> attribExprs(List<JFXExpression> trees, JavafxEnv<JavafxAttrContext> env, Type pt) {
         ListBuffer<Type> ts = new ListBuffer<Type>();
         for (List<JFXExpression> l = trees; l.nonEmpty(); l = l.tail)
             ts.append(attribExpr(l.head, env, pt));
         return ts.toList();
     }
 
     /** Attribute the arguments in a method call, returning a list of types.
      */
     List<Type> attribArgs(List<JFXExpression> trees, JavafxEnv<JavafxAttrContext> env) {
         ListBuffer<Type> argtypes = new ListBuffer<Type>();
         for (List<JFXExpression> l = trees; l.nonEmpty(); l = l.tail)
             argtypes.append(chk.checkNonVoid(
                 l.head.pos(), types.upperBound(attribTree(l.head, env, VAL, Infer.anyPoly))));
         return argtypes.toList();
     }
 
     /** Does tree represent a static reference to an identifier?
      *  It is assumed that tree is either a SELECT or an IDENT.
      *  We have to weed out selects from non-type names here.
      *  @param tree    The candidate tree.
      */
     boolean isStaticReference(JFXTree tree) {
         if (tree.getFXTag() == JavafxTag.SELECT) {
             Symbol lsym = JavafxTreeInfo.symbol(((JFXSelect) tree).selected);
             if (lsym == null || lsym.kind != TYP) {
                 return false;
             }
         }
         return true;
     }
 
     /** Attribute a list of statements, returning nothing.
      */
     <T extends JFXTree> void attribDecls(List<T> trees, JavafxEnv<JavafxAttrContext> env) {
         for (List<T> l = trees; l.nonEmpty(); l = l.tail)
             attribDecl(l.head, env);
     }
 
     /** Attribute a type argument list, returning a list of types.
      */
     List<Type> attribTypes(List<JFXExpression> trees, JavafxEnv<JavafxAttrContext> env) {
         ListBuffer<Type> argtypes = new ListBuffer<Type>();
         for (List<JFXExpression> l = trees; l.nonEmpty(); l = l.tail)
             argtypes.append(chk.checkRefType(l.head.pos(), attribType(l.head, env)));
         return argtypes.toList();
     }
 
     /** Attribute type reference in an `extends' or `implements' clause.
      *
      *  @param tree              The tree making up the type reference.
      *  @param env               The environment current at the reference.
      *  @param classExpected     true if only a class is expected here.
      *  @param interfaceExpected true if only an interface is expected here.
      */
     Type attribBase(JFXTree tree,
                     JavafxEnv<JavafxAttrContext> env,
                     boolean classExpected,
                     boolean interfaceExpected,
                     boolean checkExtensible) {
         Type t = attribType(tree, env);
         return checkBase(t, tree, env, classExpected, interfaceExpected, checkExtensible);
     }
 
     Type checkBase(Type t,
                    JFXTree tree,
                    JavafxEnv<JavafxAttrContext> env,
                    boolean classExpected,
                    boolean interfaceExpected,
                    boolean checkExtensible) {
         if (t.tag == TYPEVAR && !classExpected && !interfaceExpected) {
             // check that type variable is already visible
             if (t.getUpperBound() == null) {
                 log.error(tree.pos(), MsgSym.MESSAGE_ILLEGAL_FORWARD_REF);
                 return syms.errType;
             }
         } else {
             t = chk.checkClassType(tree.pos(), t, checkExtensible|!allowGenerics);
         }
         if (interfaceExpected && (t.tsym.flags() & INTERFACE) == 0) {
             log.error(tree.pos(), MsgSym.MESSAGE_INTF_EXPECTED_HERE);
             // return errType is necessary since otherwise there might
             // be undetected cycles which cause attribution to loop
             return syms.errType;
         } else if (checkExtensible &&
                    classExpected &&
                    (t.tsym.flags() & INTERFACE) != 0) {
             log.error(tree.pos(), MsgSym.MESSAGE_NO_INTF_EXPECTED_HERE);
             return syms.errType;
         }
         if (checkExtensible &&
             ((t.tsym.flags() & FINAL) != 0)) {
             log.error(tree.pos(),
                       MsgSym.MESSAGE_CANNOT_INHERIT_FROM_FINAL, t.tsym);
         }
         chk.checkNonCyclic(tree.pos(), t);
         return t;
     }
 
     private JavafxEnv<JavafxAttrContext> newLocalEnv(JFXTree tree) {
         JavafxEnv<JavafxAttrContext> localEnv =
                 env.dup(tree, env.info.dup(env.info.scope.dupUnshared()));
         localEnv.outer = env;
         localEnv.info.scope.owner = new MethodSymbol(BLOCK, names.empty, null, env.enclClass.sym);
         return localEnv;
     }
 
     @Override
     public void visitTypeCast(JFXTypeCast tree) {
         Type clazztype = attribType(tree.clazz, env);
         Type exprtype = attribExpr(tree.expr, env, Infer.anyPoly);
         if (clazztype.isPrimitive() && ! exprtype.isPrimitive())
             clazztype = types.boxedClass(clazztype).type;
         Type owntype = chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
         if (exprtype.constValue() != null)
             owntype = cfolder.coerce(exprtype, owntype);
         result = check(tree, capture(owntype), VAL, pkind, pt, Sequenceness.DISALLOWED);
     }
 
     @Override
     public void visitInstanceOf(JFXInstanceOf tree) {
         Type exprtype = attribExpr(tree.expr, env);
         Type clazztype = chk.checkReifiableReferenceType(
             tree.clazz.pos(), attribType(tree.clazz, env));
         chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
         result = check(tree, syms.booleanType, VAL, pkind, pt, Sequenceness.DISALLOWED);
     }
 
     private void checkTypeCycle(JFXTree tree, Symbol sym) {
         if (sym.type == null) {
             JFXVar var = varSymToTree.get(sym);
             if (var != null)
                 log.note(var, MsgSym.MESSAGE_JAVAFX_TYPE_INFER_CYCLE_VAR_DECL, sym.name);
             log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_TYPE_INFER_CYCLE_VAR_REF, sym.name);
             sym.type = syms.objectType;
         }
         else if (sym.type instanceof MethodType &&
                 sym.type.getReturnType() == syms.unknownType) {
             JFXFunctionDefinition fun = methodSymToTree.get(sym);
             if (fun != null)
                 log.note(fun, MsgSym.MESSAGE_JAVAFX_TYPE_INFER_CYCLE_FUN_DECL, sym.name);
             log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_TYPE_INFER_CYCLE_VAR_REF, sym.name);
             if (pt instanceof MethodType)
                 ((MethodType)pt).restype = new ErrorType();
             sym.type = syms.objectType;
         }
     }
 
     @Override
     public void visitIdent(JFXIdent tree) {
         Symbol sym;
         boolean varArgs = false;
 
         // Find symbol
         if (tree.sym != null && tree.sym.kind != VAR) {
             sym = tree.sym;
         } else {
             sym = rs.resolveIdent(tree.pos(), env, tree.name, pkind, pt);
         }
         tree.sym = sym;
         sym.complete();
         checkTypeCycle(tree, sym);
 
         // (1) Also find the environment current for the class where
         //     sym is defined (`symEnv').
         // Only for pre-tiger versions (1.4 and earlier):
         // (2) Also determine whether we access symbol out of an anonymous
         //     class in a this or super call.  This is illegal for instance
         //     members since such classes don't carry a this$n link.
         //     (`noOuterThisPath').
         JavafxEnv<JavafxAttrContext> symEnv = env;
         boolean noOuterThisPath = false;
         if (env.enclClass.sym.owner.kind != PCK && // we are in an inner class
             (sym.kind & (VAR | MTH | TYP)) != 0 &&
             sym.owner.kind == TYP &&
             tree.name != names._this && tree.name != names._super) {
 
             // Find environment in which identifier is defined.
             while (symEnv.outer != null &&
                    !sym.isMemberOf(symEnv.enclClass.sym, types)) {
                 if ((symEnv.enclClass.sym.flags() & NOOUTERTHIS) != 0)
                     noOuterThisPath = !allowAnonOuterThis;
                 symEnv = symEnv.outer;
             }
         }
 
         // In a constructor body,
         // if symbol is a field or instance method, check that it is
         // not accessed before the supertype constructor is called.
         if ((symEnv.info.isSelfCall || noOuterThisPath) &&
             (sym.kind & (VAR | MTH)) != 0 &&
             sym.owner.kind == TYP &&
             (sym.flags() & STATIC) == 0) {
             chk.earlyRefError(tree.pos(), sym.kind == VAR ? sym : thisSym(tree.pos(), env));
         }
 	JavafxEnv<JavafxAttrContext> env1 = env;
 	if (sym.kind != ERR && sym.owner != null && sym.owner != env1.enclClass.sym) {
 	    // If the found symbol is inaccessible, then it is
 	    // accessed through an enclosing instance.  Locate this
 	    // enclosing instance:
 	    while (env1.outer != null && !rs.isAccessible(env, env1.enclClass.sym.type, sym))
 		env1 = env1.outer;
 	}
 
         // If symbol is a variable, ...
         if (sym.kind == VAR) {
             VarSymbol v = (VarSymbol)sym;
 
             // ..., evaluate its initializer, if it has one, and check for
             // illegal forward reference.
             checkInit(tree, env, v, false);
 
             // If we are expecting a variable (as opposed to a value), check
             // that the variable is assignable in the current environment.
             if (pkind == VAR)
                 chk.checkAssignable(tree.pos(), v, null, env1.enclClass.sym.type, env, WriteKind.ASSIGN);
         }
 
         result = checkId(tree, env1.enclClass.sym.type, sym, env, pkind, pt, pSequenceness, varArgs);
     }
 
     @Override
     public void visitSelect(JFXSelect tree) {
         // Determine the expected kind of the qualifier expression.
         int skind = 0;
         if (tree.name == names._this || tree.name == names._super ||
             tree.name == names._class)
         {
             skind = TYP;
         } else {
             if ((pkind & PCK) != 0) skind = skind | PCK;
             if ((pkind & TYP) != 0) skind = skind | TYP | PCK;
             if ((pkind & (VAL | MTH)) != 0) skind = skind | VAL | TYP;
         }
 
         // Attribute the qualifier expression, and determine its symbol (if any).
         Type site = attribTree(tree.selected, env, skind, Infer.anyPoly);
         if ((pkind & (PCK | TYP)) == 0)
             site = capture(site); // Capture field access
 
         // don't allow T.class T[].class, etc
         if (skind == TYP) {
             Type elt = site;
             while (elt.tag == ARRAY)
                 elt = ((ArrayType)elt).elemtype;
             if (elt.tag == TYPEVAR) {
                 log.error(tree.pos(), MsgSym.MESSAGE_TYPE_VAR_CANNOT_BE_DEREF);
                 result = syms.errType;
                 return;
             }
         }
 
         // If qualifier symbol is a type or `super', assert `selectSuper'
         // for the selection. This is relevant for determining whether
         // protected symbols are accessible.
         Symbol sitesym = JavafxTreeInfo.symbol(tree.selected);
         boolean selectSuperPrev = env.info.selectSuper;
         env.info.selectSuper =
             sitesym != null &&
             sitesym.name == names._super;
 
         // If selected expression is polymorphic, strip
         // type parameters and remember in env.info.tvars, so that
         // they can be added later (in Attr.checkId and Infer.instantiateMethod).
         if (tree.selected.type.tag == FORALL) {
             ForAll pstype = (ForAll)tree.selected.type;
             env.info.tvars = pstype.tvars;
             site = tree.selected.type = pstype.qtype;
         }
 
         // Determine the symbol represented by the selection.
         env.info.varArgs = false;
         if (sitesym instanceof ClassSymbol &&
                 env.enclClass.sym.isSubClass(sitesym, types))
             env.info.selectSuper = true;
         Symbol sym = selectSym(tree, site, env, pt, pkind);
         sym.complete();
         if (sym.exists() && !isType(sym) && (pkind & (PCK | TYP)) != 0) {
             site = capture(site);
             sym = selectSym(tree, site, env, pt, pkind);
         }
         boolean varArgs = env.info.varArgs;
         tree.sym = sym;
 
         checkTypeCycle(tree, sym);
 
         if (site.tag == TYPEVAR && !isType(sym) && sym.kind != ERR)
             site = capture(site.getUpperBound());
 
         // If that symbol is a variable, ...
         if (sym.kind == VAR) {
             VarSymbol v = (VarSymbol)sym;
 
             // ..., evaluate its initializer, if it has one, and check for
             // illegal forward reference.
             checkInit(tree, env, v, true);
 
             // If we are expecting a variable (as opposed to a value), check
             // that the variable is assignable in the current environment.
             if (pkind == VAR)
                 chk.checkAssignable(tree.pos(), v, tree.selected, site, env, WriteKind.ASSIGN);
         }
 
         // Disallow selecting a type from an expression
         if (isType(sym) && (sitesym==null || (sitesym.kind&(TYP|PCK)) == 0)) {
             tree.type = check(tree.selected, pt,
                               sitesym == null ? VAL : sitesym.kind, TYP|PCK, pt, pSequenceness);
         }
 
         if (isType(sitesym)) {
             if (sym.name == names._this) {
                 // If `C' is the currently compiled class, check that
                 // C.this' does not appear in a call to a super(...)
                 if (env.info.isSelfCall &&
                     site.tsym == env.enclClass.sym) {
                     chk.earlyRefError(tree.pos(), sym);
                 }
             }
         }
 
         // If we are selecting an instance member via a `super', ...
         if (env.info.selectSuper && (sym.flags() & STATIC) == 0) {
 
             // Check that super-qualified symbols are not abstract (JLS)
             rs.checkNonAbstract(tree.pos(), sym);
 
             if (site.isRaw()) {
                 // Determine argument types for site.
                 Type site1 = types.asSuper(env.enclClass.sym.type, site.tsym);
                 if (site1 != null) site = site1;
             }
         }
 
         env.info.selectSuper = selectSuperPrev;
         result = checkId(tree, site, sym, env, pkind, pt, pSequenceness, varArgs);
         env.info.tvars = List.nil();
     }
     //where
         /** Determine symbol referenced by a Select expression,
          *
          *  @param tree   The select tree.
          *  @param site   The type of the selected expression,
          *  @param env    The current environment.
          *  @param pt     The current prototype.
          *  @param pkind  The expected kind(s) of the Select expression.
          */
     @SuppressWarnings("fallthrough")
         private Symbol selectSym(JFXSelect tree,
                                  Type site,
                                  JavafxEnv<JavafxAttrContext> env,
                                  Type pt,
                                  int pkind) {
             DiagnosticPosition pos = tree.pos();
             Name name = tree.name;
 
             switch (site.tag) {
             case PACKAGE:
                 return rs.access(
                     rs.findIdentInPackage(env, site.tsym, name, pkind),
                     pos, site, name, true);
             case ARRAY:
             case CLASS:
                 if (pt.tag == METHOD || pt.tag == FORALL) {
                     return rs.resolveQualifiedMethod(pos, env, site, name, pt);
                 } else if (name == names._this || name == names._super) {
                     return rs.resolveSelf(pos, env, site.tsym, name);
                 } else if (name == names._class) {
                     // In this case, we have already made sure in
                     // visitSelect that qualifier expression is a type.
                     Type t = syms.classType;
                     List<Type> typeargs = allowGenerics
                         ? List.of(types.erasure(site))
                         : List.<Type>nil();
                     t = new ClassType(t.getEnclosingType(), typeargs, t.tsym);
                     return new VarSymbol(
                         STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                 } else {
                     // We are seeing a plain identifier as selector.
                     Symbol sym = rs.findIdentInType(env, site, name, pkind);
                     if ((pkind & ERRONEOUS) == 0)
                         sym = rs.access(sym, pos, site, name, true);
                     return sym;
                 }
             case WILDCARD:
                 throw new AssertionError(tree);
             case TYPEVAR:
                 // Normally, site.getUpperBound() shouldn't be null.
                 // It should only happen during memberEnter/attribBase
                 // when determining the super type which *must* be
                 // done before attributing the type variables.  In
                 // other words, we are seeing this illegal program:
                 // class B<T> extends A<T.foo> {}
                 Symbol sym = (site.getUpperBound() != null)
                     ? selectSym(tree, capture(site.getUpperBound()), env, pt, pkind)
                     : null;
                 if (sym == null || isType(sym)) {
                     log.error(pos, MsgSym.MESSAGE_TYPE_VAR_CANNOT_BE_DEREF);
                     return syms.errSymbol;
                 } else {
                     return sym;
                 }
             case ERROR:
                 // preserve identifier names through errors
                 return new ErrorType(name, site.tsym).tsym;
             case INT:
             case DOUBLE:
             case BOOLEAN:
                 if (pt.tag == METHOD || pt.tag == FORALL) {
                     Type boxedSite = types.boxedClass(site).type;
                     return rs.resolveQualifiedMethod(pos, env, boxedSite, name, pt);
                 }
                 // Fall through to default
             default:
                 // The qualifier expression is of a primitive type -- only
                 // .class is allowed for these.
                 if (name == names._class) {
                     // In this case, we have already made sure in Select that
                     // qualifier expression is a type.
                     Type t = syms.classType;
                     Type arg = types.boxedClass(site).type;
                     t = new ClassType(t.getEnclosingType(), List.of(arg), t.tsym);
                     return new VarSymbol(
                         STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                 } else {
                     log.error(pos, MsgSym.MESSAGE_CANNOT_DEREF, site);
                     return syms.errSymbol;
                 }
             }
         }
 
 
     @Override
     public void visitParens(JFXParens tree) {
         Type owntype = attribTree(tree.expr, env, pkind, pt);
         result = check(tree, owntype, pkind, pkind, pt, pSequenceness);
         Symbol sym = JavafxTreeInfo.symbol(tree);
         if (sym != null && (sym.kind&(TYP|PCK)) != 0)
             log.error(tree.pos(), MsgSym.MESSAGE_ILLEGAL_START_OF_TYPE);
     }
 
     @Override
     public void visitAssign(JFXAssign tree) {
         Type owntype = null;
         JavafxEnv<JavafxAttrContext> dupEnv = env.dup(tree);
         dupEnv.outer = env;
         owntype = attribTree(tree.lhs, dupEnv, VAR, Type.noType);
         boolean hasLhsType = false;
         if (owntype == null || owntype == syms.javafx_UnspecifiedType) {
             owntype = attribExpr(tree.rhs, env, Type.noType);
             hasLhsType = false;
         }
         else {
             hasLhsType = true;
         }
         Type capturedType = capture(owntype);
 
         Symbol lhsSym = JavafxTreeInfo.symbol(tree.lhs);
         if (lhsSym == null) {
             log.error(tree, MsgSym.MESSAGE_JAVAFX_INVALID_ASSIGNMENT);
             return;
         }
 
         if (hasLhsType) {
             attribExpr(tree.rhs, dupEnv, owntype);
         }
         else {
             if (tree.lhs.getFXTag() == JavafxTag.SELECT) {
                 JFXSelect fa = (JFXSelect)tree.lhs;
                 fa.type = owntype;
             }
             else if (tree.lhs.getFXTag() == JavafxTag.IDENT) {
                 JFXIdent id = (JFXIdent)tree.lhs;
                 id.type = owntype;
             }
 
             attribTree(tree.lhs, dupEnv, VAR, owntype);
             lhsSym.type = owntype;
         }
         result = check(tree, capturedType, VAL, pkind, pt, pSequenceness);
 
         if (tree.rhs != null && tree.lhs.getFXTag() == JavafxTag.IDENT) {
             JFXVar lhsVar = varSymToTree.get(lhsSym);
             if (lhsVar != null && (lhsVar.getJFXType() instanceof JFXTypeUnknown)) {
                 if (lhsVar.type == null ||
                         lhsVar.type == syms.javafx_AnyType/* ??? */ ||
                         lhsVar.type == syms.javafx_UnspecifiedType) {
                     if (tree.rhs.type != null && lhsVar.type != tree.rhs.type) {
                         lhsVar.type = lhsSym.type = types.upperBound(tree.rhs.type);
                         JFXExpression jcExpr = fxmake.at(tree.pos()).Ident(lhsSym);
                         lhsVar.setJFXType(fxmake.at(tree.pos()).TypeClass(jcExpr, lhsVar.getJFXType().getCardinality()));
                 }
             }
         }
     }
     }
 
     public void finishVar(JFXVar tree, JavafxEnv<JavafxAttrContext> env) {
         VarSymbol v = tree.sym;
 
         // The info.lint field in the envs stored in enter.typeEnvs is deliberately uninitialized,
         // because the annotations were not available at the time the env was created. Therefore,
         // we look up the environment chain for the first enclosing environment for which the
         // lint value is set. Typically, this is the parent env, but might be further if there
         // are any envs created as a result of TypeParameter nodes.
         JavafxEnv<JavafxAttrContext> lintEnv = env;
         while (lintEnv.info.lint == null)
             lintEnv = lintEnv.next;
         Lint lint = lintEnv.info.lint.augment(v.attributes_field, v.flags());
         Lint prevLint = chk.setLint(lint);
         JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
 
         try {
             Type declType = attribType(tree.getJFXType(), env);
             declType = chk.checkNonVoid(tree.getJFXType(), declType);
             if (declType != syms.javafx_UnspecifiedType) {
                 result = tree.type = v.type = declType;
             }
             // Check that the variable's declared type is well-formed.
             //        chk.validate(tree.vartype);
 
             Type initType;
             if (tree.init == null && (tree.getModifiers().flags & JavafxFlags.IS_DEF) != 0) {
                 log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_DEF_MUST_HAVE_INIT, v);
             }
             if (tree.init != null) {
                 // Attribute initializer in a new environment.
                 // Check that initializer conforms to variable's declared type.
                 Scope initScope = new Scope(new MethodSymbol(BLOCK, v.name, null, env.enclClass.sym));
                 initScope.next = env.info.scope;
                 JavafxEnv<JavafxAttrContext> initEnv =
                     env.dup(tree, env.info.dup(initScope));
                 initEnv.outer = env;
                 initEnv.info.lint = lint;
                 if ((tree.getModifiers().flags & STATIC) != 0)
                     initEnv.info.staticLevel++;
 
                 // In order to catch self-references, we set the variable's
                 // declaration position to maximal possible value, effectively
                 // marking the variable as undefined.
                 v.pos = Position.MAXPOS;
                 initType = attribExpr(tree.init, initEnv, declType);
                 initType = chk.checkNonVoid(tree.pos(), initType);
                 if (declType.tag <= LONG && initType.tag >= LONG && initType.tag <= DOUBLE) {
                     // Temporary kludge to supress duplicate warnings.
                     // (The kludge won't be needed if we make Number->Integer and error.)
                 }
                 else
                     chk.checkType(tree.pos(), initType, declType, Sequenceness.DISALLOWED);
                 if (initType == syms.botType
                         || initType == syms.unreachableType)
                     initType = syms.objectType;
                 else if (types.isArray(initType)) {
                     initType = types.elemtype(initType);                    
                     if (initType.isPrimitive()) {
                         if (initType == syms.shortType ||
                                 initType == syms.byteType)
                             initType = syms.javafx_IntegerType;
                         else if (initType == syms.floatType)
                             initType = syms.javafx_NumberType;
                     }
                     initType = types.sequenceType(initType);
                 }
                 chk.checkBidiBind(tree.init, tree.getBindStatus(), initEnv);
             }
             else if (tree.type != null)
                 initType = tree.type;
             else
                 initType = syms.objectType;  // nothing to go on, so we assume Object
             if (declType == syms.javafx_UnspecifiedType && v.type == null)
                 result = tree.type = v.type = types.upperBound(initType);
             //chk.validateAnnotations(tree.mods.annotations, v);
         }
         finally {
             chk.setLint(prevLint);
             log.useSource(prev);
             v.pos = tree.pos;
         }
     }
 
     @Override
     public void visitVarScriptInit(JFXVarScriptInit tree) {
         result = tree.type = attribExpr(tree.getVar(), env);
     }
             
     @Override
     public void visitVar(JFXVar tree) {
         long flags = tree.getModifiers().flags;
         Symbol sym = tree.sym;
         if (sym == null) {
             log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_VAR_NOT_SUPPORTED_HERE, (flags & JavafxFlags.IS_DEF) == 0 ? "var" : "def", tree.getName());
             return;
         }
         sym.complete();
         boolean isClassVar = env.info.scope.owner.kind == TYP;
         
         JFXOnReplace onReplace = tree.getOnReplace();
         if (onReplace != null) {
             JFXVar oldValue = onReplace.getOldValue();
 	    if (oldValue != null && oldValue.type == null) {
                     oldValue.type =  tree.type;
             }
 
             JFXVar newElements = onReplace.getNewElements();
             if (newElements != null && newElements.type == null)
                 newElements.type = tree.type;
 
 
             if (isClassVar) {
                     // let the owner of the environment be a freshly
                     // created BLOCK-method.
                     JavafxEnv<JavafxAttrContext> localEnv = newLocalEnv(tree);
                     if ((flags & STATIC) != 0) {
                         localEnv.info.staticLevel++;
                     }
                     attribDecl(onReplace, localEnv);
             } else {
                     // Create a new local environment with a local scope.
                     JavafxEnv<JavafxAttrContext> localEnv = env.dup(tree, env.info.dup(env.info.scope.dup()));
                     attribDecl(onReplace, localEnv);
                     localEnv.info.scope.leave();
             }
 
         }
        warnOnStaticUse(tree.pos(), tree.getModifiers(), sym);
         // type is the type of the variable unless the variable is bound
         result = tree.isBound()? syms.voidType : tree.type;
     }
 
     private void warnOnStaticUse(DiagnosticPosition pos, JFXModifiers mods, Symbol sym) {
         // temporary warning for the use of 'static'
         if ((mods.flags & (STATIC | SCRIPT_LEVEL_SYNTH_STATIC)) == STATIC) {
             log.warning(pos, MsgSym.MESSAGE_JAVAFX_STATIC_DEPRECATED, sym);
         }
     }
 
     /**
      * OK, this is a not really "finish" as in the completer, at least not now.
      * But it does finish the attribution of the override by attributing the
      * default initialization.
      *
      * @param tree
      * @param env
      */
     public void finishOverrideAttribute(JFXOverrideClassVar tree, JavafxEnv<JavafxAttrContext> env) {
         VarSymbol v = tree.sym;
         Type declType = tree.getId().type;
         result = tree.type = declType;
 
         // The info.lint field in the envs stored in enter.typeEnvs is deliberately uninitialized,
         // because the annotations were not available at the time the env was created. Therefore,
         // we look up the environment chain for the first enclosing environment for which the
         // lint value is set. Typically, this is the parent env, but might be further if there
         // are any envs created as a result of TypeParameter nodes.
         JavafxEnv<JavafxAttrContext> lintEnv = env;
         while (lintEnv.info.lint == null) {
             lintEnv = lintEnv.next;
         }
         Lint lint = lintEnv.info.lint.augment(v.attributes_field, v.flags());
         Lint prevLint = chk.setLint(lint);
         JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
 
         try {
             JFXExpression init = tree.getInitializer();
             if (init != null) {
                 // Attribute initializer in a new environment/
                 // Check that initializer conforms to variable's declared type.
                 Scope initScope = new Scope(new MethodSymbol(BLOCK, v.name, null, env.enclClass.sym));
                 initScope.next = env.info.scope;
                 JavafxEnv<JavafxAttrContext> initEnv =
                     env.dup(tree, env.info.dup(initScope));
                 initEnv.outer = env;
 
                 // In order to catch self-references, we set the variable's
                 // declaration position to maximal possible value, effectively
                 // marking the variable as undefined.
                 v.pos = Position.MAXPOS;
 
                 chk.checkNonVoid(init, attribExpr(init, initEnv, declType));
                 chk.checkBidiBind(tree.getInitializer(), tree.getBindStatus(), initEnv);
             }
         } finally {
             chk.setLint(prevLint);
             log.useSource(prev);
         }
     }
 
     @Override
     public void visitOverrideClassVar(JFXOverrideClassVar tree) {
         //TODO: handle static triggers
         JFXIdent id = tree.getId();
         JFXOnReplace onr = tree.getOnReplace();
 
         // let the owner of the environment be a freshly
         // created BLOCK-method.
         JavafxEnv<JavafxAttrContext> localEnv = newLocalEnv(tree);
 
         Type type = attribExpr(id, localEnv);
         tree.type = type;
         Symbol sym = id.sym;
 
         if (onr != null) {
             attribDecl(onr, localEnv);
             JFXVar oldValue = onr.getOldValue();
             if (oldValue != null && oldValue.type == null) {
                 oldValue.type = type;
             }
             JFXVar newElements = onr.getNewElements();
             if (newElements != null && newElements.type == null) {
                 newElements.type = type;
             }
         }
 
         // Must reference an attribute
         if (sym.kind != VAR) {
             log.error(id.pos(), MsgSym.MESSAGE_JAVAFX_MUST_BE_AN_ATTRIBUTE, id.name);
         } else {
             VarSymbol v = (VarSymbol) sym;
             tree.sym = v;
             finishOverrideAttribute(tree, env);
         }
     }
 
     @Override
     public void visitOnReplace(JFXOnReplace tree) {
         JFXVar lastIndex = tree.getLastIndex();
         if (lastIndex != null) {
             lastIndex.mods.flags |= Flags.FINAL;
             attribVar(lastIndex, env);
             lastIndex.sym.type = syms.intType;
         }
         JFXVar newElements = tree.getNewElements();
         if (newElements != null) {
             newElements.mods.flags |= Flags.FINAL;
             attribVar(newElements, env);
         }
 
         JFXVar firstIndex = tree.getFirstIndex();
         if (firstIndex != null) {
             firstIndex.mods.flags |= Flags.FINAL;
             attribVar(firstIndex, env);
             firstIndex.sym.type = syms.intType;
         }
 
         JFXVar oldValue = tree.getOldValue();
 	if (oldValue != null) {
             oldValue.mods.flags |= Flags.FINAL;
             attribVar(oldValue, env);
         }
         attribExpr(tree.getBody(), env);
     }
 
 
 
     private ArrayList<JFXForExpressionInClause> forClauses = null;
 
     @Override
     public void visitForExpression(JFXForExpression tree) {
         JavafxEnv<JavafxAttrContext> forExprEnv =
             env.dup(tree, env.info.dup(env.info.scope.dup()));
 
         if (forClauses == null)
             forClauses = new ArrayList<JFXForExpressionInClause>();
         int forClausesOldSize = forClauses.size();
 
         for (ForExpressionInClauseTree cl : tree.getInClauses()) {
 
             // Don't try to examine erroneous in clauses. We don't wish to
             // place the entire for expression into error nodes, just because
             // one or more in clauses was in error, so we jsut skip any
             // erroneous ones.
             //
             if  (cl instanceof JFXErroneousForExpressionInClause) continue;
 
             JFXForExpressionInClause clause = (JFXForExpressionInClause)cl;
             forClauses.add(clause);
             
             JFXVar var = clause.getVar();
 
             // Don't try to examine erroneous loop controls, such as
             // when a variable was missing. Again, this is because the IDE may
             // try to attribute a node that is mostly correct, but contains
             // one or more components that are in error.
             //
             if  (var == null || var instanceof JFXErroneousVar) continue;
 
             Type declType = attribType(var.getJFXType(), forExprEnv);
             JFXExpression expr = (JFXExpression)clause.getSequenceExpression();
             Type exprType = types.upperBound(attribExpr(expr, forExprEnv));
             attribVar(var, forExprEnv);
             chk.checkNonVoid(((JFXTree)clause).pos(), exprType);
 
             Type elemtype;
             // must implement Sequence<T>?
             Type base = types.asSuper(exprType, syms.javafx_SequenceType.tsym);
             if (base == null)
                 base = types.asSuper(exprType, syms.iterableType.tsym);
             if (base == null) {
                 log.warning(expr, MsgSym.MESSAGE_JAVAFX_ITERATING_NON_SEQUENCE);
                 elemtype = exprType;
             } else {
                 List<Type> iterableParams = base.allparams();
                 if (iterableParams.isEmpty()) {
                     elemtype = syms.errType;
                 } else {
                     elemtype = types.upperBound(iterableParams.last());
                 }
             }
             if (elemtype == syms.errType) {
                 log.error(((JFXTree)(clause.getSequenceExpression())).pos(), MsgSym.MESSAGE_FOREACH_NOT_APPLICABLE_TO_TYPE);
             } else if (elemtype == syms.botType || elemtype == syms.unreachableType) {
                 elemtype = syms.objectType;
             } else {
                 // if it is a primitive type, unbox it
                 Type unboxed = types.unboxedType(elemtype);
                 if (unboxed != Type.noType) {
                     elemtype = unboxed;
                 }
                 chk.checkType(clause.getSequenceExpression().pos(), elemtype, declType, Sequenceness.DISALLOWED);
             }
             if (declType == syms.javafx_UnspecifiedType) {
                 var.type = elemtype;
                 var.sym.type = elemtype;
             }
 
             if (clause.getWhereExpression() != null) {
                 attribExpr(clause.getWhereExpression(), env, syms.booleanType);
             }
         }
 
         forExprEnv.tree = tree; // before, we were not in loop!
         attribTree(tree.getBodyExpression(), forExprEnv, VAL, pt.tag != ERROR ? pt : Type.noType, Sequenceness.PERMITTED);
 
         Type bodyType = tree.getBodyExpression().type;
         if (bodyType == syms.unreachableType)
             log.error(tree.getBodyExpression(), MsgSym.MESSAGE_UNREACHABLE_STMT);
         Type owntype = (bodyType == null || bodyType == syms.voidType)?
             syms.voidType :
             types.isSequence(bodyType) ?
             bodyType :
             types.sequenceType(bodyType);
         while (forClauses.size() > forClausesOldSize)
             forClauses.remove(forClauses.size()-1);
         forExprEnv.info.scope.leave();
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitForExpressionInClause(JFXForExpressionInClause that) {
         // Do not assert that we cannot reach here as this unit can
         // be visited by virtue of visiting JFXErronous which
         // will attempt to visit each Erroneous node that it has
         // encapsualted.
         //
     }
 
     public void visitIndexof(JFXIndexof tree) {
         for (int n = forClauses == null ? 0 : forClauses.size(); ; ) {
             if (--n < 0) {
                  log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_INDEXOF_NOT_FOUND, tree.fname.name);
                  break;
             }
             JFXForExpressionInClause clause = forClauses.get(n);
 
             // Don't try to examine erroneous in clauses. We don't wish to
             // place the entire for expression into error nodes, just because
             // one or more in clauses was in error, so we jsut skip any
             // erroneous ones.
             //
             if  (clause == null || clause instanceof JFXErroneousForExpressionInClause) continue;
 
             JFXVar v = clause.getVar();
 
             // Don't try to deal with Erroneous or missing variables
             //
             if (v == null || v instanceof JFXErroneousVar) continue;
             
             if (clause.getVar().getName() == tree.fname.name) {
                 tree.clause = clause;
                 clause.setIndexUsed(true);
                 break;
             }
         }
         result = check(tree, syms.javafx_IntegerType, VAL,
                 pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitSkip(JFXSkip tree) {
         result = syms.voidType;
         tree.type = result;
     }
 
     @Override
     public void visitBlockExpression(JFXBlock tree) {
         // Create a new local environment with a local scope.
         JavafxEnv<JavafxAttrContext> localEnv;
         if (env.info.scope.owner.kind == TYP) {
             // Block is a static or instance initializer;
             // let the owner of the environment be a freshly
             // created BLOCK-method.
             localEnv = newLocalEnv(tree);
             if ((tree.flags & STATIC) != 0) {
                 localEnv.info.staticLevel++;
             }
         } else {
             Scope localScope = new Scope(env.info.scope.owner);
             localScope.next = env.info.scope;
             localEnv = env.dup(tree, env.info.dup(localScope));
             localEnv.outer = env;
 
             if (env.tree instanceof JFXFunctionDefinition &&
                     env.enclClass.runMethod == env.tree) {
                         env.enclClass.runBodyScope = localEnv.info.scope;
             } else {
                 localEnv.info.scope.owner = new MethodSymbol(BLOCK, names.empty, null, env.enclClass.sym);
                     }
             }
         memberEnter.memberEnter(tree.getStmts(), localEnv);
         if (tree.getValue() != null) {
             memberEnter.memberEnter(tree.getValue(), localEnv);
         }
         boolean canReturn = true;
         boolean unreachableReported = false;
         tree.type = syms.javafx_UnspecifiedType;
         for (List<JFXExpression> l = tree.stats; l.nonEmpty(); l = l.tail) {
             if (! canReturn && ! unreachableReported) {
                 unreachableReported = true;
                 log.error(l.head.pos(), MsgSym.MESSAGE_UNREACHABLE_STMT);
             }
             Type stype = attribExpr(l.head, localEnv);
             if (stype == syms.unreachableType)
                 canReturn = false;
         }
         Type owntype = null;
         if (tree.value != null) {
             if (!canReturn && !unreachableReported) {
                 log.error(tree.value.pos(), MsgSym.MESSAGE_UNREACHABLE_STMT);
             }
             Type valueType = attribExpr(tree.value, localEnv);
             owntype = valueType != syms.unreachableType ?
                 unionType(tree.pos(), tree.type, valueType) :
                 syms.unreachableType;
         }        
         if (owntype == null) {
             owntype = syms.voidType;
         }
         if (!canReturn) {
             owntype = syms.unreachableType;
         }
         owntype = owntype.baseType();
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
         if (env.info.scope.owner.kind != TYP)
             localEnv.info.scope.leave();
     }
 
     /**
      * @param tree
      */
     @Override
     public void visitWhileLoop(JFXWhileLoop tree) {
         attribExpr(tree.cond, env, syms.booleanType);
         attribExpr(tree.body, env.dup(tree));
         result = syms.voidType;
         tree.type = result;
     }
 
     @Override
     public void visitInstanciate(JFXInstanciate tree) {
         Type owntype = syms.errType;
 
         // The local environment of a class creation is
         // a new environment nested in the current one.
         JavafxEnv<JavafxAttrContext> localEnv = newLocalEnv(tree);
 
         List<JFXVar> vars = tree.getLocalvars();
         memberEnter.memberEnter(vars, localEnv);
         for (List<JFXVar> l = vars; l.nonEmpty(); l = l.tail)
             attribExpr(l.head, localEnv);
 
         // The anonymous inner class definition of the new expression,
         // if one is defined by it.
         JFXClassDeclaration cdef = tree.getClassBody();
 
         // If enclosing class is given, attribute it, and
         // complete class name to be fully qualified
         JFXExpression clazz = tree.getIdentifier(); // Class field following new
 
         // Attribute clazz expression and store
         // symbol + type back into the attributed tree.
         Type clazztype = chk.checkClassType(
             clazz.pos(), attribType(clazz, env), true);
         chk.validate(clazz);
         if (!clazztype.tsym.isInterface() &&
                    clazztype.getEnclosingType().tag == CLASS) {
             // Check for the existence of an apropos outer instance
             rs.resolveImplicitThis(tree.pos(), env, clazztype);
         }
 
         // Attribute constructor arguments.
         List<Type> argtypes = attribArgs(tree.getArgs(), localEnv);
 
         // If we have made no mistakes in the class type...
         if (clazztype.tag == CLASS) {
             // Check that class is not abstract
             if (cdef == null &&
                 (clazztype.tsym.flags() & (ABSTRACT | INTERFACE)) != 0) {
                 log.error(tree.pos(), MsgSym.MESSAGE_ABSTRACT_CANNOT_BE_INSTANTIATED,
                           clazztype.tsym);
             } else if (cdef != null && clazztype.tsym.isInterface()) {
                 // Check that no constructor arguments are given to
                 // anonymous classes implementing an interface
                 if (!argtypes.isEmpty())
                     log.error(tree.getArgs().head.pos(), MsgSym.MESSAGE_ANON_CLASS_IMPL_INTF_NO_ARGS);
 
 
                 // Error recovery: pretend no arguments were supplied.
                 argtypes = List.nil();
             }
 
             // Resolve the called constructor under the assumption
             // that we are referring to a superclass instance of the
             // current instance (JLS ???).
             else {
                 localEnv.info.selectSuper = cdef != null;
                 localEnv.info.varArgs = false;
 
                 if (! types.isJFXClass(clazztype.tsym))
                     tree.constructor = rs.resolveConstructor(
                         tree.pos(), localEnv, clazztype, argtypes, null);
                 /**
                 List<Type> emptyTypeargtypes = List.<Type>nil();
                 tree.constructor = rs.resolveConstructor(
                     tree.pos(), localEnv, clazztype, argtypes, emptyTypeargtypes);
                 Type ctorType = checkMethod(clazztype,
                                             tree.constructor,
                                             localEnv,
                                             tree.getArguments(),
                                             argtypes,
                                             emptyTypeargtypes,
                                             localEnv.info.varArgs);
                 if (localEnv.info.varArgs)
                     assert ctorType.isErroneous();
                  * ***/
 
             }
 
             if (cdef != null) {
                 // We are seeing an anonymous class instance creation.
                 // In this case, the class instance creation
                 // expression
                 //
                 //    E.new <typeargs1>C<typargs2>(args) { ... }
                 //
                 // is represented internally as
                 //
                 //    E . new <typeargs1>C<typargs2>(args) ( class <empty-name> { ... } )  .
                 //
                 // This expression is then *transformed* as follows:
                 //
                 // (1) add a STATIC flag to the class definition
                 //     if the current environment is static
                 // (2) add an extends or implements clause
                 // (3) add a constructor.
                 //
                 // For instance, if C is a class, and ET is the type of E,
                 // the expression
                 //
                 //    E.new <typeargs1>C<typargs2>(args) { ... }
                 //
                 // is translated to (where X is a fresh name and typarams is the
                 // parameter list of the super constructor):
                 //
                 //   new <typeargs1>X(<*nullchk*>E, args) where
                 //     X extends C<typargs2> {
                 //       <typarams> X(ET e, args) {
                 //         e.<typeargs1>super(args)
                 //       }
                 //       ...
                 //     }
 //               if (JavafxResolve.isStatic(env)) cdef.mods.flags |= STATIC;
 
                 // always need to be static, because they will have generated static members
                 cdef.mods.flags |= STATIC;
 
 //              now handled in class processing
 //                if (clazztype.tsym.isInterface()) {
 //                    cdef.implementing = List.of(clazz);
 //                } else {
 //                    cdef.extending = clazz;
 //                }
 
                 if (cdef.sym == null)
                     enter.classEnter(cdef, env);
 
                  attribDecl(cdef, localEnv);
                  attribClass(cdef.pos(), null, cdef.sym);
 
                 // Reassign clazztype and recompute constructor.
                 clazztype = cdef.sym.type;
                 Symbol sym = rs.resolveConstructor(
                     tree.pos(), localEnv, clazztype, argtypes,
                     List.<Type>nil(), true, false);
 
                 tree.constructor = sym;
             }
 
 //         if (tree.constructor != null && tree.constructor.kind == MTH)
               owntype = clazz.type;  // this give declared type, where clazztype would give anon type
         }
 
         for (JFXObjectLiteralPart localPt : tree.getParts()) {
             JFXObjectLiteralPart part = (JFXObjectLiteralPart)localPt;
             Symbol memberSym = rs.findIdentInType(env, clazz.type, part.name, VAR);
             memberSym = rs.access(memberSym, localPt.pos(), clazz.type, part.name, true);
             memberSym.complete();
             Type memberType = memberSym.type;
             if (!(memberSym instanceof VarSymbol) ) {
                 log.error(localPt.pos(), MsgSym.MESSAGE_JAVAFX_INVALID_ASSIGNMENT);
                 memberType = Type.noType;
             }
             attribExpr(part.getExpression(), localEnv, memberType);
             if (memberSym instanceof VarSymbol) {
                 VarSymbol v = (VarSymbol) memberSym;
                 WriteKind kind = part.isBound() ? WriteKind.INIT_BIND : WriteKind.INIT_NON_BIND;
                 chk.checkAssignable(part.pos(), v, part, clazz.type, localEnv, kind);
                 chk.checkBidiBind(part.getExpression(), part.getBindStatus(), localEnv);
             }
             part.type = memberType;
             part.sym = memberSym;
         }
 
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
         localEnv.info.scope.leave();
     }
 
     /** Make an attributed null check tree.
      */
     public JFXExpression makeNullCheck(JFXExpression arg) {
         // optimization: X.this is never null; skip null check
         Name name = JavafxTreeInfo.name(arg);
         if (name == names._this || name == names._super) return arg;
 
         JavafxTag optag = JavafxTag.NULLCHK;
         JFXUnary tree = fxmake.at(arg.pos).Unary(optag, arg);
         tree.operator = syms.nullcheck;
         tree.type = arg.type;
         return tree;
     }
 
     @Override
     public void visitFunctionValue(JFXFunctionValue tree) {
         JFXFunctionDefinition def = new JFXFunctionDefinition(fxmake.Modifiers(Flags.SYNTHETIC), defs.lambdaName, tree);
         def.pos = tree.pos;
         tree.definition = def;
         MethodSymbol m = new MethodSymbol(SYNTHETIC, def.name, null, env.enclClass.sym);
         // m.flags_field = chk.checkFlags(def.pos(), def.mods.flags, m, def);
         def.sym = m;
         finishFunctionDefinition(def, env);
         result = tree.type = syms.makeFunctionType((MethodType) def.type);
     }
 
     @Override
     public void visitFunctionDefinition(JFXFunctionDefinition tree) {
         MethodSymbol m = tree.sym;
         m.complete();
         warnOnStaticUse(tree.pos(), tree.getModifiers(), m);
 
     }
 
     /** Search super-clases for a parameter type in a matching method.
      * The idea is that when a formal parameter isn't specified in a class
      * function, see if there is a method with the same name in a superclass,
      * and use that method's parameter type.  If there are multiple methods
      * in super-classes that all have the same name and argument count,
      * the parameter types have to be the same in all of them.
      * @param csym Class to search.
      * @param name Name of matching methods.
      * @param paramCount Number of parameters of matching methods.
      * @param paramNum The parameter number we're concerned about,
      *    or -1 if we're searching for the return type.
      * @return The found type.  Null is we found no match.
      *   Notype if we found an ambiguity.
      */
     private Type searchSupersForParamType (ClassSymbol c, Name name, int paramCount, int paramNum) {
         Type found = null;
 
         for (Scope.Entry e = c.members().lookup(name);
                  e.scope != null;
                  e = e.next()) {
             if ((e.sym.kind & MTH) == 0 ||
                         (e.sym.flags_field & (STATIC|SYNTHETIC)) != 0)
                 continue;
             Type mt = types.memberType(c.type, e.sym);
             if (mt == null)
                 continue;
             List<Type> formals = mt.getParameterTypes();
             if (formals.size() != paramCount)
                 continue;
             Type t = paramNum >= 0 ? formals.get(paramNum) : mt.getReturnType();
             if (t == Type.noType)
                 return t;
             if (found == null)
                 found = t;
             else if (t != null && found != t)
                 return Type.noType;
         }
 
         Type st = types.supertype(c.type);
         if (st.tag == CLASS) {
             Type t = searchSupersForParamType((ClassSymbol)st.tsym, name, paramCount, paramNum);
             if (t == Type.noType)
                 return t;
             if (found == null)
                 found = t;
             else if (t != null && found != t)
                 return Type.noType;
         }
 	for (List<Type> l = types.interfaces(c.type);
 		     l.nonEmpty();
 		     l = l.tail) {
             Type t = searchSupersForParamType((ClassSymbol)l.head.tsym, name, paramCount, paramNum);
             if (t == Type.noType)
                 return t;
             if (found == null)
                 found = t;
             else if (t != null && found != t)
                 return Type.noType;
         }
         return found;
     }
 
     public void finishFunctionDefinition(JFXFunctionDefinition tree, JavafxEnv<JavafxAttrContext> env) {
         MethodSymbol m = tree.sym;
         JFXFunctionValue opVal = tree.operation;
         JavafxEnv<JavafxAttrContext> localEnv = memberEnter.methodEnv(tree, env);
         Type returnType;
         // Create a new environment with local scope
         // for attributing the method.
 
         JavafxEnv<JavafxAttrContext> lintEnv = env;
         while (lintEnv.info.lint == null)
             lintEnv = lintEnv.next;
 
         JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
         Lint lint = lintEnv.info.lint.augment(m.attributes_field, m.flags());
         Lint prevLint = chk.setLint(lint);
         try {
             localEnv.info.lint = lint;
 
             ClassSymbol owner = env.enclClass.sym;
             if ((owner.flags() & ANNOTATION) != 0 &&
                 tree.operation.funParams.nonEmpty())
                 log.error(tree.operation.funParams.head.pos(),
                           MsgSym.MESSAGE_INTF_ANNOTATION_MEMBERS_CANNOT_HAVE_PARAMS);
 
             // Attribute all value parameters.
             ListBuffer<Type> argbuf = new ListBuffer<Type>();
             List<Type> pparam = null;
             MethodType mtype = null;
             if (pt.tag == TypeTags.METHOD || pt instanceof FunctionType) {
                 mtype = pt.asMethodType();
                 pparam = mtype.getParameterTypes();
             }
             int paramNum = 0;
             List<JFXVar> params = tree.getParameters();
             int paramCount = params.size();
             for (List<JFXVar> l = params; l.nonEmpty(); l = l.tail) {
                 JFXVar pvar = l.head;
                 Type type;
                 if (pparam != null && pparam.nonEmpty()) {
                     type = pparam.head;
                     pparam = pparam.tail;
                 }
                 else {
                     type = syms.objectType;
                     if (pvar.getJFXType() instanceof JFXTypeUnknown) {
                         Type t = searchSupersForParamType (owner, m.name, paramCount, paramNum);
                         if (t == Type.noType)
                             log.warning(pvar.pos(), MsgSym.MESSAGE_JAVAFX_AMBIGUOUS_PARAM_TYPE_FROM_SUPER);
                         else if (t != null)
                             type = t;
                     }
                 }
                 pvar.type = type;
                 type = chk.checkNonVoid(pvar, attribVar(pvar, localEnv));
                 argbuf.append(type);
                 paramNum++;
             }
             returnType = syms.unknownType;
             if (opVal.getJFXReturnType().getFXTag() != JavafxTag.TYPEUNKNOWN)
                 returnType = attribType(tree.getJFXReturnType(), localEnv);
             else if (mtype != null) {
                 Type mrtype = mtype.getReturnType();
                 if (mrtype != null && mrtype.tag != TypeTags.NONE)
                     returnType = mrtype;
             } else {
                 // If we made use of the parameter types to select a matching
                 // method, we could presumably get a non-ambiguoys return type.
                 // But this is pretty close, in practice.
                 Type t = searchSupersForParamType (owner, m.name, paramCount, -1);
                 if (t == Type.noType)
                     log.warning(tree.pos(), MsgSym.MESSAGE_JAVAFX_AMBIGUOUS_RETURN_TYPE_FROM_SUPER);
                 else if (t != null)
                     returnType = t;
             }
             if (returnType == syms.javafx_java_lang_VoidType)
                 returnType = syms.voidType;
             mtype = new MethodType(argbuf.toList(),
                                     returnType, // may be unknownType
                                     List.<Type>nil(),
                                     syms.methodClass);
             m.type = mtype;
 
             if (tree.getBodyExpression() == null) {
                 // Empty bodies are only allowed for
                 // abstract, native, or interface methods, or for methods
                 // in a retrofit signature class.
                 if ((owner.flags() & INTERFACE) == 0 &&
                     (tree.mods.flags & (ABSTRACT | NATIVE)) == 0 &&
                     !relax)
                     log.error(tree.pos(), MsgSym.MESSAGE_MISSING_METH_BODY_OR_DECL_ABSTRACT);
                 else if (returnType == syms.unknownType)
                     // no body, can't infer, assume Any
                     // FIXME Should this be Void or an error?
                     returnType = syms.javafx_AnyType;
             } else if ((owner.flags() & INTERFACE) != 0) {
                 log.error(tree.getBodyExpression().pos(), MsgSym.MESSAGE_INTF_METH_CANNOT_HAVE_BODY);
             } else if ((tree.mods.flags & ABSTRACT) != 0) {
                 log.error(tree.pos(), MsgSym.MESSAGE_ABSTRACT_METH_CANNOT_HAVE_BODY);
             } else if ((tree.mods.flags & NATIVE) != 0) {
                 log.error(tree.pos(), MsgSym.MESSAGE_NATIVE_METH_CANNOT_HAVE_BODY);
             } else {
                 JFXBlock body = opVal.getBodyExpression();
                 if (body.value instanceof JFXReturn) {
                     body.value = ((JFXReturn) body.value).expr;
                 }
                 // Attribute method bodyExpression
                 Type typeToCheck = returnType;
                 if(tree.name == defs.internalRunFunctionName) {
                     typeToCheck = Type.noType;
                 }
                 else if (returnType == syms.voidType) {
                     typeToCheck = Type.noType;
                 }
 
                 Type bodyType = attribExpr(body, localEnv, typeToCheck); // Special handling for the run function. Its body is empty at this point.
                 if (body.value == null) {
                     if (returnType == syms.unknownType)
                         returnType = syms.javafx_VoidType; //TODO: this is wrong if there is a return statement
                 } else {
                     if (returnType == syms.unknownType)
                         returnType = bodyType == syms.unreachableType ? syms.javafx_VoidType : bodyType;
                     else if (returnType != syms.javafx_VoidType && tree.getName() != defs.internalRunFunctionName
                             // Temporary hack to suppress duplicate warning on Number->Integer.
                             // Hack can go away if/when we make it an error.  FIXME.
                             && ! (typeToCheck.tag <= LONG && bodyType.tag >= FLOAT && bodyType.tag <= DOUBLE))
                         chk.checkType(tree.pos(), bodyType, returnType, Sequenceness.DISALLOWED);
                 }
                 if (tree.isBound() && returnType == syms.javafx_VoidType) {
                     log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_BOUND_FUNCTION_MUST_NOT_BE_VOID);
                 }                
             }
             localEnv.info.scope.leave();
 
             mtype.restype = returnType;
             result = tree.type = mtype;
             
             // If we override any other methods, check that we do so properly.
             // JLS ???
             if (m.owner instanceof ClassSymbol) {
                 // Fix primitive/number types so overridden Java methods will have the correct types.
                 fixOverride(tree, m);
                 chk.checkOverride(tree, m);
             } else {
                 if ((m.flags() & JavafxFlags.OVERRIDE) != 0) {
                     log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_DECLARED_OVERRIDE_DOES_NOT, m);
                 }
             }
         }
         finally {
             chk.setLint(prevLint);
             log.useSource(prev);
         }
 
         // mark the method varargs, if necessary
         // if (isVarArgs) m.flags_field |= Flags.VARARGS;
 
         // Set the inferred types in the MethodType.argtypes and in correct symbols in MethodSymbol
         List<VarSymbol> paramSyms = List.<VarSymbol>nil();
         List<Type> paramTypes = List.<Type>nil();
         for (JFXVar var : tree.getParameters()) {
             paramSyms = paramSyms.append(var.sym);
             paramTypes = paramTypes.append(var.type);
         }
 
         m.params = paramSyms;
         if (m.type != null && m.type instanceof MethodType) {
            ((MethodType)m.type).argtypes = paramTypes;
         }
     }
 
     @Override
     public void visitTry(JFXTry tree) {
         boolean canReturn = false;
         // Attribute body
         Type stype = attribExpr(tree.body, env.dup(tree, env.info.dup()));
         if (stype != syms.unreachableType)
             canReturn = true;
 
         // Attribute catch clauses
         for (List<JFXCatch> l = tree.catchers; l.nonEmpty(); l = l.tail) {
 
             if (l == null )  continue;    // Don't try to handle erroneous catch blocks
 
             JFXCatch c = l.head;
 
             if (c == null) continue;    // Don't try to handle erroneous catch blocks
 
             JavafxEnv<JavafxAttrContext> catchEnv = newLocalEnv(c);
             memberEnter.memberEnter(c.param, env);
 
             if (c.param == null) continue;    // Don't try to handle erroneous catch blocks
 
             if (c.param.type == null)
                 c.param.sym.type = c.param.type = syms.throwableType;
             Type ctype = attribDecl((JFXVar) c.param, catchEnv);
             if (c.param.type.tsym.kind == Kinds.VAR) {
                 c.param.sym.setData(ElementKind.EXCEPTION_PARAMETER);
             }
 //uses vartype
 //            chk.checkType(c.param.vartype.pos(),
 //                          chk.checkClassType(c.param.vartype.pos(), ctype),
 //                          syms.throwableType);
             ctype = attribExpr(c.body, catchEnv);
             if (ctype != syms.unreachableType)
                 canReturn = true;
         }
 
         // Attribute finalizer
         if (tree.finalizer != null) attribExpr(tree.finalizer, env);
         result = canReturn ? syms.voidType : syms.unreachableType;
         tree.type = result;
     }
 
     @Override
     public void visitIfExpression(JFXIfExpression tree) {
         attribExpr(tree.cond, env, syms.booleanType);
         attribTree(tree.truepart, env, VAL, pt, pSequenceness);
         Type falsepartType;
         if (tree.falsepart == null) {
             falsepartType = syms.voidType;
         } else {
             falsepartType = attribTree(tree.falsepart, env, VAL, pt, pSequenceness);
             {   //TODO: ...
                 // A kludge, which can go away if we change things so that
                 // the compiler and runtime accepts null and [] equivalently.
                 // Well, actually, look at JFXC-925.
                 // Also, in a bind context, we need to know th etype of null
                 if (tree.truepart instanceof JFXSequenceEmpty
                         || tree.truepart.type.tag == BOT)
                     tree.truepart.type = falsepartType;
                 else if (tree.falsepart instanceof JFXSequenceEmpty
                         || falsepartType.tag == BOT)
                     falsepartType = tree.falsepart.type = tree.truepart.type;
             }
         }
 
         result = check(tree,
                        capture(condType(tree.pos(), tree.cond.type,
                                         tree.truepart.type, falsepartType)),
                        VAL, pkind, pt, pSequenceness);
     }
     //where
         /** Compute the type of a conditional expression, after
          *  checking that it exists. See Spec 15.25.
          *
          *  @param pos      The source position to be used for
          *                  error diagnostics.
          *  @param condtype The type of the expression's condition.
          *  @param type1 The type of the expression's then-part.
          *  @param type2 The type of the expression's else-part.
          */
         private Type condType(DiagnosticPosition pos,
                               Type condtype,
                               Type thentype,
                               Type elsetype) {
             Type ctype = unionType(pos, thentype, elsetype);
 
             // If condition and both arms are numeric constants,
             // evaluate at compile-time.
             return ((condtype.constValue() != null) &&
                     (thentype.constValue() != null) &&
                     (elsetype.constValue() != null))
                 ? cfolder.coerce(condtype.isTrue()?thentype:elsetype, ctype)
                 : ctype;
         }
         /** Compute the type of a conditional expression, after
          *  checking that it exists.  Does not take into
          *  account the special case where condition and both arms
          *  are constants.
          *
          *  @param pos      The source position to be used for error
          *                  diagnostics.
          *  @param condtype The type of the expression's condition.
          *  @param type1 The type of the expression's then-part.
          *  @param type2 The type of the expression's else-part.
          */
         private Type unionType(DiagnosticPosition pos,
                                Type type1, Type type2) {
             if (type1 == syms.unreachableType || type1 == syms.javafx_UnspecifiedType)
                 return type2;
             if (type2 == syms.unreachableType || type2 == syms.javafx_UnspecifiedType)
                 return type1;
             if (type1 == type2)
                 return type1;
 
             // Ensure that we don't NPE if either of the inputs were from
             // Erroneous nodes such as missing blocks on conditionals and so on.
             //
             if (type1 == null ) {
                 if  (type2 == null) {
                     return syms.voidType;
                 } else {
                     return type2;
                 }
             } else  if (type2 == null) {
                         return type1;
                     }
             
             if (type1.tag == VOID || type2.tag == VOID)
                 return syms.voidType;
 
             boolean isSequence1 = types.isSequence(type1);
             boolean isSequence2 = types.isSequence(type2);
             if (isSequence1 || isSequence2) {
                 if (isSequence1)
                     type1 = types.elementType(type1);
                 if (isSequence2)
                     type2 = types.elementType(type2);
                 Type union = unionType(pos, type1, type2);
                 return union.tag == ERROR ? union : types.sequenceType(union);
             }
             // If same type, that is the result
             if (types.isSameType(type1, type2))
                 return type1.baseType();
 
             Type thenUnboxed = (!allowBoxing || type1.isPrimitive())
                 ? type1 : types.unboxedType(type1);
             Type elseUnboxed = (!allowBoxing || type2.isPrimitive())
                 ? type2 : types.unboxedType(type2);
 
             // Otherwise, if both arms can be converted to a numeric
             // type, return the least numeric type that fits both arms
             // (i.e. return larger of the two, or return int if one
             // arm is short, the other is char).
             if (thenUnboxed.isPrimitive() && elseUnboxed.isPrimitive()) {
                 // If one arm has an integer subrange type (i.e., byte,
                 // short, or char), and the other is an integer constant
                 // that fits into the subrange, return the subrange type.
                 if (thenUnboxed.tag < INT && elseUnboxed.tag == INT &&
                     types.isAssignable(elseUnboxed, thenUnboxed))
                     return thenUnboxed.baseType();
                 if (elseUnboxed.tag < INT && thenUnboxed.tag == INT &&
                     types.isAssignable(thenUnboxed, elseUnboxed))
                     return elseUnboxed.baseType();
 
                 for (int i = BYTE; i < VOID; i++) {
                     Type candidate = syms.typeOfTag[i];
                     if (types.isSubtype(thenUnboxed, candidate) &&
                         types.isSubtype(elseUnboxed, candidate))
                         return candidate;
                 }
             }
 
             // Those were all the cases that could result in a primitive
             if (allowBoxing) {
                 type1 = syms.boxIfNeeded(type1);
                 type2 = syms.boxIfNeeded(type2);
             }
 
             if (types.isSubtype(type1, type2))
                 return type2.baseType();
             if (types.isSubtype(type2, type1))
                 return type1.baseType();
 
             if (!allowBoxing) {
                 log.error(pos, MsgSym.MESSAGE_NEITHER_CONDITIONAL_SUBTYPE,
                           type1, type2);
                 return type1.baseType();
             }
 
             // both are known to be reference types.  The result is
             // lub(type1,type2). This cannot fail, as it will
             // always be possible to infer "Object" if nothing better.
             return types.lub(type1.baseType(), type2.baseType());
         }
 
     @Override
     public void visitBreak(JFXBreak tree) {
         tree.target = findJumpTarget(tree.pos(), tree.getFXTag(), tree.label, env);
         result = tree.type = syms.unreachableType;
     }
 
     @Override
     public void visitContinue(JFXContinue tree) {
         tree.target = findJumpTarget(tree.pos(), tree.getFXTag(), tree.label, env);
         result = tree.type = syms.unreachableType;
     }
     //where
         /** Return the target of a break or continue statement, if it exists,
          *  report an error if not.
          *  Note: The target of a labelled break or continue is the
          *  (non-labelled) statement tree referred to by the label,
          *  not the tree representing the labelled statement itself.
          *
          *  @param pos     The position to be used for error diagnostics
          *  @param tag     The tag of the jump statement. This is either
          *                 Tree.BREAK or Tree.CONTINUE.
          *  @param label   The label of the jump statement, or null if no
          *                 label is given.
          *  @param env     The environment current at the jump statement.
          */
         private JFXTree findJumpTarget(DiagnosticPosition pos,
                                     JavafxTag tag,
                                     Name label,
                                     JavafxEnv<JavafxAttrContext> env) {
             // Search environments outwards from the point of jump.
             JavafxEnv<JavafxAttrContext> env1 = env;
             LOOP:
             while (env1 != null) {
                 switch (env1.tree.getFXTag()) {
                 case WHILELOOP:
                 case FOR_EXPRESSION:
                     if (label == null) return env1.tree;
                     break;
                 default:
                 }
                 env1 = env1.next;
             }
             if (label != null)
                 log.error(pos, MsgSym.MESSAGE_UNDEF_LABEL, label);
             else if (tag == JavafxTag.CONTINUE)
                 log.error(pos, MsgSym.MESSAGE_CONT_OUTSIDE_LOOP);
             else
                 log.error(pos, MsgSym.MESSAGE_BREAK_OUTSIDE_SWITCH_LOOP);
             return null;
         }
 
     @Override
     public void visitReturn(JFXReturn tree) {
         if (env.enclMethod == null) {
             log.error(tree.pos(), MsgSym.MESSAGE_RETURN_OUTSIDE_METH);
 
         } else {
             // Attribute return expression, if it exists, and check that
             // it conforms to result type of enclosing method.
             Symbol m = env.enclMethod.sym;
             Type rtype = m.type.getReturnType();
             JFXBlock enclBlock = env.enclMethod.operation.bodyExpression;
             if (rtype == null)
                 log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_CANNOT_INFER_RETURN_TYPE);
             else if (rtype.tag == VOID) {
                 if (tree.expr != null) {
                     log.error(tree.expr.pos(),
                         MsgSym.MESSAGE_CANNOT_RET_VAL_FROM_METH_DECL_VOID);
                 }
             } else if (tree.expr == null) {
                 if (enclBlock.type == syms.javafx_UnspecifiedType)
                     enclBlock.type = syms.javafx_VoidType;
                 else
                     log.error(tree.pos(), MsgSym.MESSAGE_MISSING_RET_VAL);
             } else {
                 if (enclBlock.type.tag == VOID) {
                     log.error(tree.pos(), MsgSym.MESSAGE_CANNOT_RET_VAL_FROM_METH_DECL_VOID);
                 }
                 Type exprType = attribExpr(tree.expr, env);
                 enclBlock.type = unionType(tree.pos(), enclBlock.type, exprType);
             }
         }    
         result = tree.type = syms.unreachableType;
     }
 
     @Override
     public void visitThrow(JFXThrow tree) {
 
         if  (tree != null && tree.expr != null && !(tree.expr instanceof JFXErroneous)) {
             attribExpr(tree.expr, env, syms.throwableType);
         }
         result = tree.type = syms.unreachableType;
     }
 
     private void searchParameterTypes (JFXExpression meth, Type[] paramTypes) {
         // FUTURE: Search for matching overloaded methods/functions that
         // would be a match for meth, and number of arguments==paramTypes.length.
         // If all the candidates have the same type for parameter # i,
         // set paramTypes[i] to that type.
         // Otherwise, leave paramTypes[i]==null.
     }
 
     @Override
     public void visitFunctionInvocation(JFXFunctionInvocation tree) {
         // The local environment of a method application is
         // a new environment nested in the current one.
         JavafxEnv<JavafxAttrContext> localEnv = env.dup(tree, env.info.dup());
 
         // The types of the actual method type arguments.
         List<Type> typeargtypes;
 
         Name methName = JavafxTreeInfo.name(tree.meth);
 
         int argcount = tree.args.size();
 
         Type[] paramTypes = new Type[argcount];
         searchParameterTypes(tree.meth, paramTypes);
 
         ListBuffer<Type> argtypebuffer = new ListBuffer<Type>();
         int i = 0;
         for (List<JFXExpression> l = tree.args; l.nonEmpty(); l = l.tail, i++) {
             Type argtype = paramTypes[i];
             if (argtype != null)
                 attribExpr(l.head, env, argtype);
             else
                 argtype = chk.checkNonVoid(l.head.pos(),
                         types.upperBound(attribTree(l.head, env, VAL, Infer.anyPoly)));
             argtypebuffer.append(argtype);
         }
         List<Type> argtypes = argtypebuffer.toList();
 
         typeargtypes = attribTypes(tree.typeargs, localEnv);
 
             // ... and attribute the method using as a prototype a methodtype
             // whose formal argument types is exactly the list of actual
             // arguments (this will also set the method symbol).
             Type mpt = new MethodType(argtypes, pt, null, syms.methodClass);
             if (typeargtypes.nonEmpty()) mpt = new ForAll(typeargtypes, mpt);
             localEnv.info.varArgs = false;
             Type mtype = attribExpr(tree.meth, localEnv, mpt);
             if (localEnv.info.varArgs)
                 assert mtype.isErroneous() || tree.varargsElement != null;
 
             // Compute the result type.
             Type restype = mtype.getReturnType();
             if (restype == syms.unknownType) {
                 log.error(tree.meth.pos(), MsgSym.MESSAGE_JAVAFX_FUNC_TYPE_INFER_CYCLE, methName);
                 restype = syms.objectType;
             }
             // as a special case, array.clone() has a result that is
             // the same as static type of the array being cloned
             if (tree.meth.getFXTag() == JavafxTag.SELECT &&
                 allowCovariantReturns &&
                 methName == names.clone &&
                 types.isArray(((JFXSelect) tree.meth).selected.type))
                 restype = ((JFXSelect) tree.meth).selected.type;
 
             // as a special case, x.getClass() has type Class<? extends |X|>
             if (allowGenerics &&
                 methName == names.getClass && tree.args.isEmpty()) {
                 Type qualifier = (tree.meth.getFXTag() == JavafxTag.SELECT)
                     ? ((JFXSelect) tree.meth).selected.type
                     : env.enclClass.sym.type;
                 qualifier = syms.boxIfNeeded(qualifier);
                 restype = new
                     ClassType(restype.getEnclosingType(),
                               List.<Type>of(new WildcardType(types.erasure(qualifier),
                                                                BoundKind.EXTENDS,
                                                                syms.boundClass)),
                               restype.tsym);
             }
 
             if (mtype instanceof ErrorType) {
                 tree.type = mtype;
                 result = mtype;
             }
             else if (mtype instanceof MethodType || mtype instanceof FunctionType) {
                 // If the "method" has a symbol, we've already checked for
                 // formal/actual consistency.  So doing it again would be
                 // wasteful - plus varargs hasn't been properly implemented.
                 if (tree.meth.getFXTag() != JavafxTag.SELECT &&
                     tree.meth.getFXTag() != JavafxTag.IDENT &&
                     ! rs.argumentsAcceptable(argtypes, mtype.getParameterTypes(),
                         true, false, Warner.noWarnings))
                     log.error(tree,
                               MsgSym.MESSAGE_JAVAFX_CANNOT_APPLY_FUNCTION,
                               mtype.getParameterTypes(), argtypes);
                 // Check that value of resulting type is admissible in the
                 // current context.  Also, capture the return type
                 result = check(tree, capture(restype), VAL, pkind, pt, pSequenceness);
             }
             else {
                 log.error(tree,
                         MsgSym.MESSAGE_JAVAFX_NOT_A_FUNC,
                         mtype, typeargtypes, Type.toString(argtypes));
                 tree.type = pt;
                 result = pt;
             }
 
         Symbol msym = JavafxTreeInfo.symbol(tree.meth);
         if (msym!=null && msym.owner!=null && msym.owner.type!=null &&
                 msym.owner.type.tsym == syms.javafx_AutoImportRuntimeType.tsym &&
                 methName == defs.isInitializedName) {
             for (List<JFXExpression> l = tree.args; l.nonEmpty(); l = l.tail, i++) {
                 JFXExpression arg = l.head;
                 Symbol asym = JavafxTreeInfo.symbol(arg);
                 if (asym == null || !(asym.type instanceof ErrorType)) {
                     if (asym == null ||
                             !(asym instanceof VarSymbol) ||
                             (arg.getFXTag() != JavafxTag.IDENT && arg.getFXTag() != JavafxTag.SELECT) ||
                             (asym.flags() & JavafxFlags.IS_DEF) != 0 ||
                             asym.owner == null ||
                             asym.owner.kind != TYP) {
                         log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_APPLIED_TO_INSTANCE_VAR, defs.isInitializedName);
                     } else {
                         // check that we have write access
                         // unless it is a public-init or public-read var, that was already handled 
                         // the regular access check
                         if ((asym.flags() & (JavafxFlags.PUBLIC_INIT | JavafxFlags.PUBLIC_READ)) != 0) {
                             Type site;
                             JFXTree base;
                             switch (arg.getFXTag()) {
                                 case IDENT:
                                     base = null;
                                     site = env.enclClass.sym.type;
                                     break;
                                 case SELECT:
                                     base = ((JFXSelect)arg).selected;
                                     site = base.type;
                                     break;
                                 default:
                                     throw new AssertionError(); // see above, should not occur
                             }
                             chk.checkAssignable(tree.pos(), (VarSymbol) asym, base, site, env, WriteKind.VAR_QUERY);
                         }
                     }
                 }
             }
         }
         chk.validate(tree.typeargs);
     }
 
     @Override
     public void visitAssignop(JFXAssignOp tree) {
         // Attribute arguments.
         Type owntype = attribTree(tree.lhs, env, VAR, Type.noType);
         Type operand = attribExpr(tree.rhs, env);
 
         // Fix types of numeric arguments with non -specified type.
         Symbol lhsSym = JavafxTreeInfo.symbol(tree.lhs);
         if (lhsSym != null &&
                 (lhsSym.type == null || lhsSym.type == Type.noType || lhsSym.type == syms.javafx_AnyType)) {
             JFXVar lhsVarTree = varSymToTree.get(lhsSym);
             owntype = setBinaryTypes(tree.getFXTag(), tree.lhs, lhsVarTree, lhsSym.type, lhsSym);
         }
 
         Symbol rhsSym = JavafxTreeInfo.symbol(tree.rhs);
         if (rhsSym != null  &&
                 (rhsSym.type == null || rhsSym.type == Type.noType || rhsSym.type == syms.javafx_AnyType)) {
             JFXVar rhsVarTree = varSymToTree.get(rhsSym);
             operand = setBinaryTypes(tree.getFXTag(), tree.rhs, rhsVarTree, rhsSym.type, rhsSym);
         }
 
         // Find operator.
         Symbol operator = tree.operator = rs.resolveBinaryOperator(
             tree.pos(), tree.getNormalOperatorFXTag(), env,
             owntype, operand);
 
         if (operator.kind == MTH) {
             if (operator instanceof OperatorSymbol) {
                 chk.checkOperator(tree.pos(),
                                   (OperatorSymbol)operator,
                                   tree.getFXTag(),
                                   owntype,
                                   operand);
             }
             if (types.isSameType(operator.type.getReturnType(), syms.stringType)) {
                 // String assignment; make sure the lhs is a string
                 chk.checkType(tree.lhs.pos(),
                               owntype,
                               syms.stringType, Sequenceness.DISALLOWED);
             } else {
                 chk.checkDivZero(tree.rhs.pos(), operator, operand);
                 chk.checkCastable(tree.rhs.pos(),
                                   operator.type.getReturnType(),
                                   owntype);
             }
         }
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
 
         if (lhsSym != null && tree.rhs != null) {
             JFXVar lhsVar = varSymToTree.get(lhsSym);
             if (lhsVar != null && (lhsVar.getJFXType() instanceof JFXTypeUnknown)) {
                 if ((lhsVar.type == null || lhsVar.type == syms.javafx_AnyType)) {
                     if (tree.rhs.type != null && lhsVar.type != tree.rhs.type) {
                         lhsVar.type = lhsSym.type = tree.rhs.type;
                         JFXExpression jcExpr = fxmake.at(tree.pos()).Ident(lhsSym);
                         lhsVar.setJFXType(fxmake.at(tree.pos()).TypeClass(jcExpr, lhsVar.getJFXType().getCardinality()));
                     }
                 }
             }
         }
     }
 
     @Override
     public void visitUnary(JFXUnary tree) {
         switch (tree.getFXTag()) {
             case SIZEOF: {
                 attribExpr(tree.arg, env);
                 result = check(tree, syms.javafx_IntegerType, VAL, pkind, pt, pSequenceness);
                 return;
             }
             case REVERSE: {
                 Type argtype = chk.checkNonVoid(tree.arg.pos(), attribExpr(tree.arg, env));
                 result = check(tree, argtype, VAL, pkind, pt, pSequenceness);
                 return;
             }
         }
         boolean isIncDec = tree.getFXTag().isIncDec();
 
         // Attribute arguments.
         Type argtype = isIncDec
             ? attribTree(tree.arg, env, VAR, Type.noType)
             : chk.checkNonVoid(tree.arg.pos(), attribExpr(tree.arg, env));
 
         //TODO: redundant now, but if we want to deferentiate error for increment/decremenet
         // from assignment, this code may be useful
         /***
         if (isIncDec) {
             Symbol argSym = JavafxTreeInfo.symbol(tree.arg);
             if (argSym == null) {
                 log.error(tree, MsgSym.MESSAGE_JAVAFX_INVALID_ASSIGNMENT);
                 return;
             }
             if ((argSym.flags() & JavafxFlags.IS_DEF) != 0L) {
                 log.error(tree, MsgSym.MESSAGE_JAVAFX_CANNOT_ASSIGN_TO_DEF, argSym);
                 return;
             }
             if ((argSym.flags() & Flags.PARAMETER) != 0L) {
                 log.error(tree, MsgSym.MESSAGE_JAVAFX_CANNOT_ASSIGN_TO_PARAMETER, argSym);
                 return;
             }
         }
         ***/
 
         Symbol sym =  rs.resolveUnaryOperator(tree.pos(), tree.getFXTag(), env, argtype);
         Type owntype = syms.errType;
         if (sym instanceof OperatorSymbol) {
             // Find operator.
             Symbol operator = tree.operator = sym;
             if (operator.kind == MTH) {
                 owntype = isIncDec
                     ? tree.arg.type
                     : operator.type.getReturnType();
 
             /*** no constants or folding
                 int opc = ((OperatorSymbol)operator).opcode;
 
                 // If the argument is constant, fold it.
                 if (argtype.constValue() != null) {
                     Type ctype = cfolder.fold1(opc, argtype);
                     if (ctype != null) {
                         owntype = cfolder.coerce(ctype, owntype);
 
                         // Remove constant types from arguments to
                         // conserve space. The parser will fold concatenations
                         // of string literals; the code here also
                         // gets rid of intermediate results when some of the
                         // operands are constant identifiers.
                         if (tree.arg.type.tsym == syms.stringType.tsym) {
                             tree.arg.type = syms.stringType;
                         }
                     }
                 }
             *****/
             }
         } else {
             owntype = sym.type.getReturnType();
         }
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
     }
 
     private Type setBinaryTypes(JavafxTag opcode, JFXExpression tree, JFXVar var, Type type, Symbol treeSym) {
         Type newType = type;
         JFXExpression jcExpression = null;
         // boolean type
         if (opcode == JavafxTag.OR ||
             opcode == JavafxTag.AND) {
             newType = syms.javafx_BooleanType;
             jcExpression = fxmake.at(tree.pos()).Ident(syms.javafx_BooleanType.tsym);
         }
         // Integer type
         else if (opcode == JavafxTag.MOD) {
             newType = syms.javafx_IntegerType;
             jcExpression = fxmake.at(tree.pos()).Ident(syms.javafx_IntegerType.tsym);
         }
         // Number type
         else if (opcode == JavafxTag.LT ||
                  opcode == JavafxTag.GT ||
                  opcode == JavafxTag.LE ||
                  opcode == JavafxTag.GE ||
                  opcode == JavafxTag.PLUS ||
                  opcode == JavafxTag.MINUS ||
                  opcode == JavafxTag.MUL ||
                  opcode == JavafxTag.DIV ||
                  opcode == JavafxTag.PLUS_ASG ||
                  opcode == JavafxTag.MINUS_ASG ||
                  opcode == JavafxTag.MUL_ASG ||
                  opcode == JavafxTag.DIV_ASG) {
             newType = syms.javafx_NumberType;
             jcExpression = fxmake.at(tree.pos()).Ident(syms.javafx_NumberType.tsym);
         }
         else
             return newType;
 
         // tree is not null here
         tree.setType(newType);
         treeSym.type = newType;
 
         if (var != null) {
             var.setType(newType);
             JFXType jfxType = fxmake.at(tree.pos()).TypeClass(jcExpression, Cardinality.SINGLETON);
             jfxType.type = newType;
             var.setJFXType(jfxType);
             var.sym.type = newType;
         }
 
         return newType;
     }
 
     @Override
     public void visitBinary(JFXBinary tree) {
         // Attribute arguments.
         Type left = chk.checkNonVoid(tree.lhs.pos(), attribExpr(tree.lhs, env));
         Type right = chk.checkNonVoid(tree.rhs.pos(), attribExpr(tree.rhs, env));
 
         if (left == syms.javafx_UnspecifiedType) {
             left = setEffectiveExpressionType(tree.lhs, newTypeFromType(getEffectiveExpressionType(right)));
         }
         else if (right == syms.javafx_UnspecifiedType) {
             right = setEffectiveExpressionType(tree.rhs, newTypeFromType(getEffectiveExpressionType(left)));
         }
 
         // Fix types of numeric arguments with non -specified type.
         boolean lhsSet = false;
 
         // If an operand is untyped AND it's a var or attribute, constrain the
         // operand based on the operator.  Rather a special-case kludge.
         Symbol lhsSym = JavafxTreeInfo.symbol(tree.lhs);
         if (lhsSym != null &&
                 (lhsSym.type == null || lhsSym.type == Type.noType || lhsSym.type == syms.javafx_AnyType)) {
             JFXVar lhsVarTree = varSymToTree.get(lhsSym);
             left = setBinaryTypes(tree.getFXTag(), tree.lhs, lhsVarTree, lhsSym.type, lhsSym);
             lhsSet = true;
         }
         Symbol rhsSym = JavafxTreeInfo.symbol(tree.rhs);
         if (rhsSym != null  &&
                 (rhsSym.type == null || rhsSym.type == Type.noType || rhsSym.type == syms.javafx_AnyType) || (lhsSet && lhsSym == rhsSym)) {
             JFXVar rhsVarTree = varSymToTree.get(rhsSym);
             right = setBinaryTypes(tree.getFXTag(), tree.rhs, rhsVarTree, rhsSym.type, rhsSym);
         }
 
         Symbol sym =
             rs.resolveBinaryOperator(tree.pos(), tree.getFXTag(), env, left, right);
         Type owntype = syms.errType;
         if (sym instanceof OperatorSymbol) {
             // Find operator.
             Symbol operator = tree.operator = sym;
 
             if (operator.kind == MTH) {
                 owntype = operator.type.getReturnType();
                 int opc = chk.checkOperator(tree.lhs.pos(),
                                             (OperatorSymbol)operator,
                                             tree.getFXTag(),
                                             left,
                                             right);
 
                 // If both arguments are constants, fold them.
                 if (left.constValue() != null && right.constValue() != null) {
                     Type ctype = cfolder.fold2(opc, left, right);
                     if (ctype != null) {
                         owntype = cfolder.coerce(ctype, owntype);
 
                         // Remove constant types from arguments to
                         // conserve space. The parser will fold concatenations
                         // of string literals; the code here also
                         // gets rid of intermediate results when some of the
                         // operands are constant identifiers.
                         if (tree.lhs.type.tsym == syms.stringType.tsym) {
                             tree.lhs.type = syms.stringType;
                         }
                         if (tree.rhs.type.tsym == syms.stringType.tsym) {
                             tree.rhs.type = syms.stringType;
                         }
                     }
                 }
 
                 // Check that argument types of a reference ==, != are
                 // castable to each other, (JLS???).
                 if ((opc == ByteCodes.if_acmpeq || opc == ByteCodes.if_acmpne)) {
                     if (!types.isCastable(left, right, new Warner(tree.pos()))) {
                         boolean isError = true;
                         if (right.tsym != null && right.tsym instanceof JavafxClassSymbol) {
                             ListBuffer<Type> supertypes = ListBuffer.<Type>lb();
                             Set superSet = new HashSet<Type>();
                             supertypes.append(right);
                             superSet.add(right);
 
                             types.getSupertypes(right.tsym, supertypes, superSet);
                             for (Type baseType : supertypes) {
                                 if (types.isCastable(left, baseType, new Warner(tree.pos()))){
                                     isError = false;
                                     break;
                                 }
                             }
                         }
 
                         if (isError) {
                             log.error(tree.pos(), MsgSym.MESSAGE_INCOMPARABLE_TYPES, left, right);
                         }
                     }
                 }
                 chk.checkDivZero(tree.rhs.pos(), operator, right);
             }
         } else {
             owntype = sym.type.getReturnType();
         }
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
         if (tree.getFXTag() == JavafxTag.PLUS && owntype == syms.stringType) {
             log.error(tree.pos(), MsgSym.MESSAGE_JAVAFX_STRING_CONCATENATION, expressionToString(tree));
         }
     }
     //where
     private String expressionToString(JFXExpression expr) {
         if (expr.type == syms.stringType) {
             if (expr.getFXTag() == JavafxTag.LITERAL) {
                 return (String) (((JFXLiteral) expr).getValue());
             } else if (expr.getFXTag() == JavafxTag.PLUS) {
                 JFXBinary plus = (JFXBinary) expr;
                 return expressionToString(plus.lhs) + expressionToString(plus.rhs);
             }
         }
         return "{" + expr.toString() + "}";
     }
 
 
     @Override
     public void visitLiteral(JFXLiteral tree) {
         if (tree.typetag == TypeTags.BOT && types.isSequence(pt))
             result = tree.type = pt;
         else
             result = check(
                 tree, litType(tree.typetag, pt), VAL, pkind, pt, pSequenceness);
     }
     //where
     /** Return the type of a literal with given type tag.
      */
     private Type litType(int tag, Type pt) {
         return (tag == TypeTags.CLASS) ? syms.stringType : // a class literal can only be a String
             (tag == TypeTags.BOT && pt.tag == TypeTags.CLASS) ? pt : // for null, make the type the expected type
                 syms.typeOfTag[tag];
     }
 
     @Override
     public void visitErroneous(JFXErroneous tree) {
       //  if (tree.getErrorTrees() != null)
        //     for (JFXTree err : tree.getErrorTrees())
          //       attribTree(err, env, ERR, pt);
         result = tree.type = syms.errType;
     }
 
     /** Main method: attribute class definition associated with given class symbol.
      *  reporting completion failures at the given position.
      *  @param pos The source position at which completion errors are to be
      *             reported.
      *  @param c   The class symbol whose definition will be attributed.
      */
     public void attribClass(DiagnosticPosition pos, JFXClassDeclaration tree, ClassSymbol c) {
         try {
             annotate.flush();
             attribClass(tree, c);
         } catch (CompletionFailure ex) {
             chk.completionError(pos, ex);
         }
     }
 
     /** Attribute class definition associated with given class symbol.
      *  @param c   The class symbol whose definition will be attributed.
      */
     void attribClass(JFXClassDeclaration tree, ClassSymbol c) throws CompletionFailure {
         if (c.type.tag == ERROR) return;
 
         // Check for cycles in the inheritance graph, which can arise from
         // ill-formed class files.
         chk.checkNonCyclic(null, c.type);
 
         if (tree != null) {
             attribSupertypes(tree, c);
         }
 
         // The previous operations might have attributed the current class
         // if there was a cycle. So we test first whether the class is still
         // UNATTRIBUTED.
         if ((c.flags_field & UNATTRIBUTED) != 0) {
             c.flags_field &= ~UNATTRIBUTED;
 
             // Get environment current at the point of class definition.
             JavafxEnv<JavafxAttrContext> localEnv = enter.typeEnvs.get(c);
 
             // The info.lint field in the envs stored in enter.typeEnvs is deliberately uninitialized,
             // because the annotations were not available at the time the env was created. Therefore,
             // we look up the environment chain for the first enclosing environment for which the
             // lint value is set. Typically, this is the parent env, but might be further if there
             // are any envs created as a result of TypeParameter nodes.
             JavafxEnv<JavafxAttrContext> lintEnv = localEnv;
             while (lintEnv.info.lint == null)
                 lintEnv = lintEnv.next;
 
             // Having found the enclosing lint value, we can initialize the lint value for this class
             localEnv.info.lint = lintEnv.info.lint.augment(c.attributes_field, c.flags());
 
             Lint prevLint = chk.setLint(localEnv.info.lint);
             JavaFileObject prev = log.useSource(c.sourcefile);
 
             try {
                 attribClassBody(localEnv, c);
             } finally {
                 log.useSource(prev);
                 chk.setLint(prevLint);
             }
 
         }
     }
 
     /** Clones a type without copiyng constant values
      * @param t the type that needs to be cloned.
      * @return  the cloned type with no cloned constants.
      */
     public Type newTypeFromType(Type t) {
         if (t == null) return null;
         switch (t.tag) {
             case BYTE:
                 return syms.byteType;
             case CHAR:
                 return syms.charType;
             case SHORT:
                 return syms.shortType;
             case INT:
                 return syms.intType;
             case LONG:
                 return syms.longType;
             case FLOAT:
                 return syms.floatType;
             case DOUBLE:
                 return syms.doubleType;
             case BOOLEAN:
                 return syms.booleanType;
             case VOID:
                 return syms.voidType;
             default:
                 return t;
         }
     }
 
     /**
      * Gets the effective type of a type. If MethodType - the return type,
      * otherwise the passed in type.
      */
     private Type getEffectiveExpressionType(Type type) {
         if (type.tag == TypeTags.METHOD) {
             return type.getReturnType();
         }
 
         return type;
     }
 
     /**
      * Sets the effective type of an expression. If MethodType - the return type,
      * otherwise the whole type of the expression is set.
      */
     private Type setEffectiveExpressionType(JFXExpression expression, Type type) {
         if (expression.type.tag == TypeTags.METHOD) {
             ((MethodType)expression.type).restype = type;
         }
         else {
             expression.type = type;
         }
 
         return expression.type;
     }
 
 // Begin JavaFX trees
     @Override
     public void visitClassDeclaration(JFXClassDeclaration tree) {
         // Local classes have not been entered yet, so we need to do it now:
         if ((env.info.scope.owner.kind & (VAR | MTH)) != 0)
             enter.classEnter(tree, env);
 
         ClassSymbol c = tree.sym;
         if (c == null) {
             // exit in case something drastic went wrong during enter.
             result = null;
         } else {
             // make sure class has been completed:
             c.complete();
 
             attribSupertypes(tree, c);
 
             attribClass(tree.pos(), tree, c);
 
             result = tree.type = c.type;
 
             types.addFxClass(c, tree);
         }
         result = syms.voidType;
     }
 
     private void attribSupertypes(JFXClassDeclaration tree, ClassSymbol c) {
         JavafxClassSymbol javafxClassSymbol = null;
         if (c instanceof JavafxClassSymbol) {
             javafxClassSymbol = (JavafxClassSymbol)c;
         }
 
         Symbol javaSupertypeSymbol = null;
         boolean addToSuperTypes = true;
 
         for (JFXExpression superClass : tree.getSupertypes()) {
             Type supType = superClass.type == null ? attribType(superClass, env)
                                                    : superClass.type;
             // java.lang.Enum may not be subclassed by a non-enum
             if (supType.tsym == syms.enumSym &&
                 ((c.flags_field & (Flags.ENUM|Flags.COMPOUND)) == 0))
                 log.error(superClass.pos(), MsgSym.MESSAGE_ENUM_NO_SUBCLASSING);
 
             // Enums may not be extended by source-level classes
             if (supType.tsym != null &&
                 ((supType.tsym.flags_field & Flags.ENUM) != 0) &&
                 ((c.flags_field & Flags.ENUM) == 0) &&
                 !target.compilerBootstrap(c)) {
                 log.error(superClass.pos(), MsgSym.MESSAGE_ENUM_TYPES_NOT_EXTENSIBLE);
             }
             if (supType != null && !supType.isInterface() &&
                     !types.isJFXClass(supType.tsym) &&
                     !supType.isPrimitive() &&
                     javafxClassSymbol.type instanceof ClassType) {
                 if (javaSupertypeSymbol == null) {
                     javaSupertypeSymbol = supType.tsym;
                     // Verify there is a non-parametric constructor.
                     boolean hasNonParamCtor = true; // If there is no non-param constr we will create one later.
                     for (Scope.Entry e1 = javaSupertypeSymbol.members().elems;
                              e1 != null;
                              e1 = e1.sibling) {
                             Symbol s1 = e1.sym;
                             if (s1 != null &&
                                     s1.name == names.init &&
                                     s1.kind == Kinds.MTH) {
                                 MethodType mtype = ((MethodSymbol)s1).type.asMethodType();
                                 if (mtype != null && mtype.getParameterTypes().isEmpty()) {
                                     hasNonParamCtor = true;
                                     break;
                                 }
                                 else {
                                     hasNonParamCtor = false;
                                 }
                             }
                     }
 
                     if (hasNonParamCtor) {
                         ((ClassType)javafxClassSymbol.type).supertype_field = supType;
                         addToSuperTypes = false;
                     }
                     else {
                         log.error(superClass.pos(), MsgSym.MESSAGE_JAVAFX_BASE_JAVA_CLASS_NON_PAPAR_CTOR, supType.tsym.name);
 
                     }
                 }
                 else {
                     // We are already extending one Java class. No more than one is allowed. Report an error.
                     log.error(superClass.pos(), MsgSym.MESSAGE_JAVAFX_ONLY_ONE_BASE_JAVA_CLASS_ALLOWED, supType.tsym.name);
                 }
             }
 
             if (addToSuperTypes &&
                     supType != null &&
                     javafxClassSymbol != null) {
                 javafxClassSymbol.addSuperType(supType);
             }
             addToSuperTypes = true;
         }
 
     }
 
     @Override
     public void visitInitDefinition(JFXInitDefinition that) {
         Symbol symOwner = env.info.scope.owner;
         try {
             MethodType mt = new MethodType(List.<Type>nil(), syms.voidType, List.<Type>nil(), (TypeSymbol)symOwner);
             that.sym = new MethodSymbol(0L, defs.initDefName, mt, symOwner);
             env.info.scope.owner = that.sym;
             attribExpr(that.getBody(), env);
         }
         finally {
             env.info.scope.owner = symOwner;
         }
     }
 
     public void visitPostInitDefinition(JFXPostInitDefinition that) {
         Symbol symOwner = env.info.scope.owner;
         try {
             MethodType mt = new MethodType(List.<Type>nil(), syms.voidType, List.<Type>nil(), (TypeSymbol)symOwner);
             that.sym = new MethodSymbol(0L, defs.postInitDefName, mt, symOwner);
             env.info.scope.owner = that.sym;
             attribExpr(that.getBody(), env);
         }
         finally {
             env.info.scope.owner = symOwner;
         }
     }
 
     @Override
     public void visitSequenceEmpty(JFXSequenceEmpty tree) {
         boolean isSeq = false;
         if (pt.tag != NONE && pt != syms.javafx_UnspecifiedType && !(isSeq = types.isSequence(pt)) && pSequenceness == Sequenceness.DISALLOWED) {
             log.error(tree.pos(), MsgSym.MESSAGE_ARRAY_REQ_BUT_FOUND, pt); //TODO: msg
             result = syms.errType;
         } else {
             Type owntype = pt.tag == NONE || pt.tag == UNKNOWN ? syms.botType :
                     isSeq ? pt : types.sequenceType(pt);
             result = check(tree, owntype, VAL, pkind, Type.noType, pSequenceness);
         }
     }
 
     @Override
     public void visitSequenceRange(JFXSequenceRange tree) {
         Type lowerType =  attribExpr(tree.getLower(), env);
         Type upperType = attribExpr(tree.getUpper(), env);
         Type stepType = tree.getStepOrNull() == null? syms.javafx_IntegerType : attribExpr(tree.getStepOrNull(), env);
         boolean allInt = true;
         if (lowerType != syms.javafx_IntegerType) {
             allInt = false;
             if (lowerType != syms.javafx_NumberType) {
                 log.error(tree.getLower().pos(), MsgSym.MESSAGE_JAVAFX_RANGE_START_INT_OR_NUMBER);
             }
         }
         if (upperType != syms.javafx_IntegerType) {
             allInt = false;
             if (upperType != syms.javafx_NumberType) {
                 log.error(tree.getLower().pos(), MsgSym.MESSAGE_JAVAFX_RANGE_END_INT_OR_NUMBER);
             }
         }
         if (stepType != syms.javafx_IntegerType) {
             allInt = false;
             if (stepType != syms.javafx_NumberType) {
                 log.error(tree.getStepOrNull().pos(), MsgSym.MESSAGE_JAVAFX_RANGE_STEP_INT_OR_NUMBER);
             }
         }
 		if (tree.getLower().getFXTag() == JavafxTag.LITERAL && tree.getUpper().getFXTag() == JavafxTag.LITERAL
                 && (tree.getStepOrNull() == null || tree.getStepOrNull().getFXTag() == JavafxTag.LITERAL)) {
             chk.warnEmptyRangeLiteral(tree.pos(), (JFXLiteral)tree.getLower(), (JFXLiteral)tree.getUpper(), (JFXLiteral)tree.getStepOrNull(), tree.isExclusive());
 		}
         Type owntype = types.sequenceType(allInt? syms.javafx_IntegerType : syms.javafx_NumberType);
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitSequenceExplicit(JFXSequenceExplicit tree) {
         Type elemType = null;
         Type expected = pt;
         if (types.isSequence(expected))
             expected = types.elementType(expected);
         for (JFXExpression expr : tree.getItems()) {
                 Type itemType = attribTree(expr, env, VAL,
                         expected, Sequenceness.PERMITTED);
                 if (types.isSequence(itemType) || types.isArray(itemType)) {
                     itemType = types.isSequence(itemType) ? types.elementType(itemType) : types.elemtype(itemType);
                 }
                 itemType = chk.checkNonVoid(expr, itemType);
                 if (elemType == null || itemType.tag == NONE || itemType.tag == ERROR)
                     elemType = itemType;
                 else
                     elemType = unionType(tree, itemType, elemType);
             }
         Type owntype = types.sequenceType(elemType);
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
         if (owntype == result && pt.tag != NONE && pt != syms.javafx_UnspecifiedType) {
             expected = types.sequenceType(expected);
             result = tree.type = expected;
         }
     }
 
     @Override
     public void visitSequenceSlice(JFXSequenceSlice tree) {
         JFXExpression seq = tree.getSequence();
         Type seqType = attribExpr(seq, env);
 
         attribExpr(tree.getFirstIndex(), env, syms.javafx_IntegerType);
         if (tree.getLastIndex() != null) {
             attribExpr(tree.getLastIndex(), env, syms.javafx_IntegerType);
         }
         result = check(tree, seqType, VAR, pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitSequenceIndexed(JFXSequenceIndexed tree) {
         JFXExpression seq = tree.getSequence();
         Type seqType = attribExpr(seq, env);
 
         attribExpr(tree.getIndex(), env, syms.javafx_IntegerType);
         Type owntype;
         if (seqType.tag == TypeTags.ARRAY) {
             owntype = ((ArrayType)seqType).elemtype;
         }
         else {
             owntype = chk.checkSequenceElementType(seq, seqType);
         }
         result = check(tree, owntype, VAR, pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitSequenceInsert(JFXSequenceInsert tree) {
         JFXExpression seq = tree.getSequence();
         Type seqType = attribTree(seq, env, VAR, Type.noType, Sequenceness.REQUIRED);
         attribExpr(tree.getElement(), env, seqType);
         if (tree.getPosition() != null) {
             attribExpr(tree.getPosition(), env, syms.javafx_IntegerType);
         }
         result = syms.voidType;
         tree.type = result;
     }
 
     @Override
     public void visitSequenceDelete(JFXSequenceDelete tree) {
         JFXExpression seq = tree.getSequence();
         if (tree.getElement() == null) {
             if (seq instanceof JFXSequenceIndexed) {
                 // delete seq[index];
                 JFXSequenceIndexed si = (JFXSequenceIndexed)seq;
                 JFXExpression seqseq = si.getSequence();
                 JFXExpression index = si.getIndex();
                 attribTree(seqseq, env, VAR, Type.noType, Sequenceness.REQUIRED);
                 attribExpr(index, env, syms.javafx_IntegerType);
             } else if (seq instanceof JFXSequenceSlice) {
                 // delete seq[first..last];
                 JFXSequenceSlice slice = (JFXSequenceSlice)seq;
                 JFXExpression seqseq = slice.getSequence();
                 JFXExpression first = slice.getFirstIndex();
                 JFXExpression last = slice.getLastIndex();
                 attribTree(seqseq, env, VAR, Type.noType, Sequenceness.REQUIRED);
                 attribExpr(first, env, syms.javafx_IntegerType);
                 if (last != null) {
                     attribExpr(last, env, syms.javafx_IntegerType);
                 }
             } else {
                 // delete seq;   // that is, all the elements
                 attribTree(seq, env, VAR, Type.noType, Sequenceness.REQUIRED);
             }
         } else {
             Type seqType = attribTree(seq, env, VAR, Type.noType, Sequenceness.REQUIRED);
             attribExpr(tree.getElement(), env,
                     chk.checkSequenceElementType(seq.pos(), seqType));
         }
         result = syms.voidType;
         tree.type = result;
     }
 
     @Override
     public void visitStringExpression(JFXStringExpression tree) {
         List<JFXExpression> parts = tree.getParts();
         attribExpr(parts.head, env, syms.javafx_StringType);
         parts = parts.tail;
         while (parts.nonEmpty()) {
             // First the format specifier:
             attribExpr(parts.head, env, syms.javafx_StringType);
             parts = parts.tail;
             // Next the enclosed expression:
             chk.checkNonVoid(parts.head.pos(), attribExpr(parts.head, env, Type.noType));
             parts = parts.tail;
             // Next the following string literal part:
             attribExpr(parts.head, env, syms.javafx_StringType);
             parts = parts.tail;
         }
         result = check(tree, syms.javafx_StringType, VAL, pkind, pt, pSequenceness);
     }
 
     @Override
     public void visitObjectLiteralPart(JFXObjectLiteralPart that) {

         // Note that this method can be reached legitimately if visitErroneous is
         // called and the error nodes contain an objectLiteralPart. Hence this
         // just sets the result to errType.
         //
         result = syms.errType;
     }
 
     @Override
     public void visitTypeAny(JFXTypeAny tree) {
         assert false : "MUST IMPLEMENT";
     }
 
     @Override
     public void visitTypeClass(JFXTypeClass tree) {
         Type type = null;
         JFXExpression classNameExpr = ((JFXTypeClass) tree).getClassName();
         if (classNameExpr instanceof JFXIdent) {
             Name className = ((JFXIdent) classNameExpr).getName();
             if (className == syms.numberTypeName) {
                 type = syms.javafx_NumberType;
             } else if (className == syms.integerTypeName) {
                 type = syms.javafx_IntegerType;
             } else if (className == syms.booleanTypeName) {
                 type = syms.javafx_BooleanType;
             } else if (className == syms.voidTypeName) {
                 type = syms.javafx_VoidType;
             } else if (className == syms.stringTypeName) {
                 type = syms.javafx_StringType;
             }
         }
         if (type == null) {
             type = attribType(classNameExpr, env);
         }
         Cardinality cardinality = tree.getCardinality();
         if (cardinality != Cardinality.SINGLETON &&
                 type == syms.voidType) {
             log.error(tree, MsgSym.MESSAGE_JAVAFX_VOID_SEQUENCE_NOT_ALLOWED);
             cardinality = Cardinality.SINGLETON;
         }
         type = sequenceType(type, cardinality);
         tree.type = type;
         result = type;
     }
 
     @Override
     public void visitTypeFunctional(JFXTypeFunctional tree) {
         Type restype = attribType(tree.restype, env);
         if (restype == syms.unknownType)
             restype = syms.voidType;
         Type rtype = restype == syms.voidType ? syms.javafx_java_lang_VoidType
                 : new WildcardType(syms.boxIfNeeded(restype), BoundKind.EXTENDS, syms.boundClass);
         ListBuffer<Type> typarams = new ListBuffer<Type>();
         ListBuffer<Type> argtypes = new ListBuffer<Type>();
         typarams.append(rtype);
         int nargs = 0;
         for (JFXType param : (List<JFXType>)tree.params) {
             Type argtype = attribType(param, env);
             if (argtype == syms.javafx_UnspecifiedType)
                 argtype = syms.objectType;
             argtypes.append(argtype);
             Type ptype = syms.boxIfNeeded(argtype);
             ptype = new WildcardType(ptype, BoundKind.SUPER, syms.boundClass);
             typarams.append(ptype);
             nargs++;
         }
         MethodType mtype = new MethodType(argtypes.toList(), restype, null, syms.methodClass);
         if (nargs > JavafxSymtab.MAX_FIXED_PARAM_LENGTH) {
             log.error(tree, MsgSym.MESSAGE_TOO_MANY_PARAMETERS);
             tree.type = result = syms.objectType;
             return;
         }
         FunctionType ftype = syms.makeFunctionType(typarams.toList(), mtype);
         Type type = sequenceType(ftype, tree.getCardinality());
         tree.type = type;
         result = type;
     }
 
     @Override
     public void visitTypeUnknown(JFXTypeUnknown tree) {
         result = tree.type = syms.javafx_UnspecifiedType;
     }
 
     Type sequenceType(Type elemType, Cardinality cardinality) {
         return cardinality == cardinality.ANY
                 ? types.sequenceType(elemType)
                 : elemType;
     }
 
         /** Determine type of identifier or select expression and check that
          *  (1) the referenced symbol is not deprecated
          *  (2) the symbol's type is safe (@see checkSafe)
          *  (3) if symbol is a variable, check that its type and kind are
          *      compatible with the prototype and protokind.
          *  (4) if symbol is an instance field of a raw type,
          *      which is being assigned to, issue an unchecked warning if its
          *      type changes under erasure.
          *  (5) if symbol is an instance method of a raw type, issue an
          *      unchecked warning if its argument types change under erasure.
          *  If checks succeed:
          *    If symbol is a constant, return its constant type
          *    else if symbol is a method, return its result type
          *    otherwise return its type.
          *  Otherwise return errType.
          *
          *  @param tree       The syntax tree representing the identifier
          *  @param site       If this is a select, the type of the selected
          *                    expression, otherwise the type of the current class.
          *  @param sym        The symbol representing the identifier.
          *  @param env        The current environment.
          *  @param pkind      The set of expected kinds.
          *  @param pt         The expected type.
          */
         Type checkId(JFXTree tree,
                      Type site,
                      Symbol sym,
                      JavafxEnv<JavafxAttrContext> env,
                      int pkind,
                      Type pt,
                      Sequenceness pSequenceness,
                      boolean useVarargs) {
             if (pt.isErroneous()) return syms.errType;
             Type owntype; // The computed type of this identifier occurrence.
             switch (sym.kind) {
             case TYP:
                 // For types, the computed type equals the symbol's type,
                 // except for two situations:
                 owntype = sym.type;
                 if (owntype.tag == CLASS) {
                     Type ownOuter = owntype.getEnclosingType();
 
                     // (a) If the symbol's type is parameterized, erase it
                     // because no type parameters were given.
                     // We recover generic outer type later in visitTypeApply.
                     if (owntype.tsym.type.getTypeArguments().nonEmpty()) {
                         owntype = types.erasure(owntype);
                     }
 
                     // (b) If the symbol's type is an inner class, then
                     // we have to interpret its outer type as a superclass
                     // of the site type. Example:
                     //
                     // class Tree<A> { class Visitor { ... } }
                     // class PointTree extends Tree<Point> { ... }
                     // ...PointTree.Visitor...
                     //
                     // Then the type of the last expression above is
                     // Tree<Point>.Visitor.
                     else if (ownOuter.tag == CLASS && site != ownOuter) {
                         Type normOuter = site;
                         if (normOuter.tag == CLASS)
                             normOuter = types.asEnclosingSuper(site, ownOuter.tsym);
                         if (normOuter == null) // perhaps from an import
                             normOuter = types.erasure(ownOuter);
                         if (normOuter != ownOuter)
                             owntype = new ClassType(
                                 normOuter, List.<Type>nil(), owntype.tsym);
                     }
                 }
                 break;
             case VAR:
                 VarSymbol v = (VarSymbol)sym;
                 // Test (4): if symbol is an instance field of a raw type,
                 // which is being assigned to, issue an unchecked warning if
                 // its type changes under erasure.
                 if (allowGenerics &&
                     pkind == VAR &&
                     v.owner.kind == TYP &&
                     (v.flags() & STATIC) == 0 &&
                     (site.tag == CLASS || site.tag == TYPEVAR)) {
                     Type s = types.asOuterSuper(site, v.owner);
                     if (s != null &&
                         s.isRaw() &&
                         !types.isSameType(v.type, v.erasure(types))) {
                         chk.warnUnchecked(tree.pos(),
                                           MsgSym.MESSAGE_UNCHECKED_ASSIGN_TO_VAR,
                                           v, s);
                     }
                 }
                 // The computed type of a variable is the type of the
                 // variable symbol, taken as a member of the site type.
                 owntype = (sym.owner.kind == TYP &&
                            sym.name != names._this && sym.name != names._super)
                     ? types.memberType(site, sym)
                     : sym.type;
 
                 if (env.info.tvars.nonEmpty()) {
                     Type owntype1 = new ForAll(env.info.tvars, owntype);
                     for (List<Type> l = env.info.tvars; l.nonEmpty(); l = l.tail)
                         if (!owntype.contains(l.head)) {
                             log.error(tree.pos(), MsgSym.MESSAGE_UNDETERMINDED_TYPE, owntype1);
                             owntype1 = syms.errType;
                         }
                     owntype = owntype1;
                 }
 
                 // If the variable is a constant, record constant value in
                 // computed type.
                 //if (v.getConstValue() != null && isStaticReference(tree))
                 //    owntype = owntype.constType(v.getConstValue());
 
                 if (pkind == VAL) {
                     owntype = capture(owntype); // capture "names as expressions"
                 }
                 break;
             case MTH: {
                 owntype = sym.type;
                 // This is probably wrong now that we have function expressions.
                 // Instead, we should checkMethod in visitFunctionInvocation.
                 // In that case we should also handle FunctionType. FIXME.
                 if (pt instanceof MethodType || pt instanceof ForAll) {
                     JFXFunctionInvocation app = (JFXFunctionInvocation)env.tree;
                     owntype = checkMethod(site, sym, env, app.args,
                                       pt.getParameterTypes(), pt.getTypeArguments(),
                                       env.info.varArgs);
                 }
                 break;
             }
             case PCK: case ERR:
                 owntype = sym.type;
                 break;
             default:
                 throw new AssertionError("unexpected kind: " + sym.kind +
                                          " in tree " + tree);
             }
 
             // Test (1): emit a `deprecation' warning if symbol is deprecated.
             // (for constructors, the error was given when the constructor was
             // resolved)
             if (sym.name != names.init &&
                 (sym.flags() & DEPRECATED) != 0 &&
                 (env.info.scope.owner.flags() & DEPRECATED) == 0 &&
                 sym.outermostClass() != env.info.scope.owner.outermostClass())
                 chk.warnDeprecated(tree.pos(), sym);
 
             if ((sym.flags() & PROPRIETARY) != 0)
                 log.strictWarning(tree.pos(), MsgSym.MESSAGE_SUN_PROPRIETARY, sym);
 
             // Test (3): if symbol is a variable, check that its type and
             // kind are compatible with the prototype and protokind.
             return check(tree, owntype, sym.kind, pkind, pt, pSequenceness);
         }
 
         /** Check that variable is initialized and evaluate the variable's
          *  initializer, if not yet done. Also check that variable is not
          *  referenced before it is defined.
          *  @param tree    The tree making up the variable reference.
          *  @param env     The current environment.
          *  @param v       The variable's symbol.
          */
         private void checkInit(JFXTree tree,
                                JavafxEnv<JavafxAttrContext> env,
                                VarSymbol v,
                                boolean onlyWarning) {
 //          System.err.println(v + " " + ((v.flags() & STATIC) != 0) + " " +
 //                             tree.pos + " " + v.pos + " " +
 //                             Resolve.isStatic(env));//DEBUG
 
             // A forward reference is diagnosed if the declaration position
             // of the variable is greater than the current tree position
             // and the tree and variable definition occur in the same class
             // definition.  Note that writes don't count as references.
             // This check applies only to class and instance
             // variables.  Local variables follow different scope rules,
             // and are subject to definite assignment checking.
             if (v.pos > tree.pos &&
                 v.owner.kind == TYP &&
                 canOwnInitializer(env.info.scope.owner) &&
                 v.owner == env.info.scope.owner.enclClass() &&
                 ((v.flags() & STATIC) != 0) == JavafxResolve.isStatic(env) &&
                 (env.tree.getFXTag() != JavafxTag.ASSIGN ||
                  JavafxTreeInfo.skipParens(((JFXAssign) env.tree).lhs) != tree)) {
         }
 
             v.getConstValue(); // ensure initializer is evaluated
 
             checkEnumInitializer(tree, env, v);
         }

         /**
          * Check for illegal references to static members of enum.  In
          * an enum type, constructors and initializers may not
          * reference its static members unless they are constant.
          *
          * @param tree    The tree making up the variable reference.
          * @param env     The current environment.
          * @param v       The variable's symbol.
          * @see JLS 3rd Ed. (8.9 Enums)
          */
         private void checkEnumInitializer(JFXTree tree, JavafxEnv<JavafxAttrContext> env, VarSymbol v) {
             // JLS 3rd Ed.:
             //
             // "It is a compile-time error to reference a static field
             // of an enum type that is not a compile-time constant
             // (15.28) from constructors, instance initializer blocks,
             // or instance variable initializer expressions of that
             // type. It is a compile-time error for the constructors,
             // instance initializer blocks, or instance variable
             // initializer expressions of an enum constant e to refer
             // to itself or to an enum constant of the same type that
             // is declared to the right of e."
             if (isNonStaticEnumField(v)) {
                 ClassSymbol enclClass = env.info.scope.owner.enclClass();
 
                 if (enclClass == null || enclClass.owner == null)
                     return;
 
                 // See if the enclosing class is the enum (or a
                 // subclass thereof) declaring v.  If not, this
                 // reference is OK.
                 if (v.owner != enclClass && !types.isSubtype(enclClass.type, v.owner.type))
                     return;
 
                 // If the reference isn't from an initializer, then
                 // the reference is OK.
                 if (!JavafxResolve.isInitializer(env))
                     return;
 
                 log.error(tree.pos(), MsgSym.MESSAGE_ILLEGAL_ENUM_STATIC_REF);
             }
         }
 
         private boolean isNonStaticEnumField(VarSymbol v) {
             return Flags.isEnum(v.owner) && Flags.isStatic(v) && !Flags.isConstant(v);
         }
 
         /** Can the given symbol be the owner of code which forms part
          *  if class initialization? This is the case if the symbol is
          *  a type or field, or if the symbol is the synthetic method.
          *  owning a block.
          */
         private boolean canOwnInitializer(Symbol sym) {
             return
                 (sym.kind & (VAR | TYP)) != 0 ||
                 (sym.kind == MTH && (sym.flags() & BLOCK) != 0);
         }
 
     Warner noteWarner = new Warner();
 
     /**
      * Check that method arguments conform to its instantation.
      **/
     public Type checkMethod(Type site,
                             Symbol sym,
                             JavafxEnv<JavafxAttrContext> env,
                             final List<JFXExpression> argtrees,
                             List<Type> argtypes,
                             List<Type> typeargtypes,
                             boolean useVarargs) {
         // Test (5): if symbol is an instance method of a raw type, issue
         // an unchecked warning if its argument types change under erasure.
         if (allowGenerics &&
             (sym.flags() & STATIC) == 0 &&
             (site.tag == CLASS || site.tag == TYPEVAR)) {
             Type s = types.asOuterSuper(site, sym.owner);
             if (s != null && s.isRaw() &&
                 !types.isSameTypes(sym.type.getParameterTypes(),
                                    sym.erasure(types).getParameterTypes())) {
                 chk.warnUnchecked(env.tree.pos(),
                                   MsgSym.MESSAGE_UNCHECKED_CALL_MBR_OF_RAW_TYPE,
                                   sym, s);
             }
         }
 
         // Compute the identifier's instantiated type.
         // For methods, we need to compute the instance type by
         // Resolve.instantiate from the symbol's type as well as
         // any type arguments and value arguments.
         noteWarner.warned = false;
         Type owntype = rs.instantiate(env,
                                       site,
                                       sym,
                                       argtypes,
                                       typeargtypes,
                                       true,
                                       useVarargs,
                                       noteWarner);
         boolean warned = noteWarner.warned;
 
         // If this fails, something went wrong; we should not have
         // found the identifier in the first place.
         if (owntype == null) {
             if (!pt.isErroneous())
                 log.error(env.tree.pos(),
                           MsgSym.MESSAGE_INTERNAL_ERROR_CANNOT_INSTANTIATE,
                           sym, site,
                           Type.toString(pt.getParameterTypes()));
             owntype = syms.errType;
         } else {
             // System.out.println("call   : " + env.tree);
             // System.out.println("method : " + owntype);
             // System.out.println("actuals: " + argtypes);
             List<Type> formals = owntype.getParameterTypes();
             Type last = useVarargs ? formals.last() : null;
             if (sym.name==names.init &&
                 sym.owner == syms.enumSym)
                 formals = formals.tail.tail;
             List<JFXExpression> args = argtrees;
             while (formals.head != last) {
                 JFXTree arg = args.head;
                 Warner warn = chk.convertWarner(arg.pos(), arg.type, formals.head);
                 assertConvertible(arg, arg.type, formals.head, warn);
                 warned |= warn.warned;
                 args = args.tail;
                 formals = formals.tail;
             }
             if (useVarargs) {
                 Type varArg = types.elemtype(last);
                 while (args.tail != null) {
                     JFXTree arg = args.head;
                     Warner warn = chk.convertWarner(arg.pos(), arg.type, varArg);
                     assertConvertible(arg, arg.type, varArg, warn);
                     warned |= warn.warned;
                     args = args.tail;
                 }
             } else if ((sym.flags() & VARARGS) != 0 && allowVarargs) {
                 // non-varargs call to varargs method
                 Type varParam = owntype.getParameterTypes().last();
                 Type lastArg = argtypes.last();
                 if (types.isSubtypeUnchecked(lastArg, types.elemtype(varParam)) &&
                     !types.isSameType(types.erasure(varParam), types.erasure(lastArg)))
                     log.warning(argtrees.last().pos(), MsgSym.MESSAGE_INEXACT_NON_VARARGS_CALL,
                                 types.elemtype(varParam),
                                 varParam);
             }
 
             if (warned && sym.type.tag == FORALL) {
                 String typeargs = "";
                 if (typeargtypes != null && typeargtypes.nonEmpty()) {
                     typeargs = "<" + Type.toString(typeargtypes) + ">";
                 }
                 chk.warnUnchecked(env.tree.pos(),
                                   MsgSym.MESSAGE_UNCHECKED_METH_INVOCATION_APPLIED,
                                   sym,
                                   sym.location(),
                                   typeargs,
                                   Type.toString(argtypes));
                 owntype = new MethodType(owntype.getParameterTypes(),
                                          types.erasure(owntype.getReturnType()),
                                          owntype.getThrownTypes(),
                                          syms.methodClass);
             }
             if (useVarargs) {
                 JFXTree tree = env.tree;
                 Type argtype = owntype.getParameterTypes().last();
                 if (!types.isReifiable(argtype))
                     chk.warnUnchecked(env.tree.pos(),
                                       MsgSym.MESSAGE_UNCHECKED_GENERIC_ARRAY_CREATION,
                                       argtype);
                 Type elemtype = types.elemtype(argtype);
                 switch (tree.getFXTag()) {
                 case APPLY:
                     ((JFXFunctionInvocation) tree).varargsElement = elemtype;
                     break;
                 default:
                     throw new AssertionError(""+tree);
                 }
             }
         }
         return owntype;
     }
 
     private void assertConvertible(JFXTree tree, Type actual, Type formal, Warner warn) {
         if (types.isConvertible(actual, formal, warn))
             return;
 
         if (formal.isCompound()
             && types.isSubtype(actual, types.supertype(formal))
             && types.isSubtypeUnchecked(actual, types.interfaces(formal), warn))
             return;
 
         if (false) {
             // TODO: make assertConvertible work
             chk.typeError(tree.pos(), JCDiagnostic.fragment(MsgSym.MESSAGE_INCOMPATIBLE_TYPES), actual, formal);
             throw new AssertionError("Tree: " + tree
                                      + " actual:" + actual
                                      + " formal: " + formal);
         }
     }
 
     @Override
     public void visitImport(JFXImport tree) {
         // nothing to do
     }
 
     /** Finish the attribution of a class. */
     public void attribClassBody(JavafxEnv<JavafxAttrContext> env, ClassSymbol c) {
         JFXClassDeclaration tree = (JFXClassDeclaration)env.tree;
         assert c == tree.sym;
 
         // Validate annotations
         //chk.validateAnnotations(tree.mods.annotations, c);
 
         // Validate type parameters, supertype and interfaces.
         //attribBounds(tree.getEmptyTypeParameters());
         //chk.validateTypeParams(tree.getEmptyTypeParameters());
         chk.validate(tree.getSupertypes());
 
         // Check that class does not import the same parameterized interface
         // with two different argument lists.
         chk.checkClassBounds(tree.pos(), c.type);
 
         tree.type = c.type;
 
         // Check that a generic class doesn't extend Throwable
         if (!c.type.allparams().isEmpty() && types.isSubtype(c.type, syms.throwableType))
             log.error(tree.getExtending().head.pos(), MsgSym.MESSAGE_GENERIC_THROWABLE);
 
         for (List<JFXTree> l = tree.getMembers(); l.nonEmpty(); l = l.tail) {
             // Attribute declaration
             attribDecl(l.head, env);
             // Check that declarations in inner classes are not static (JLS 8.1.2)
             // Make an exception for static constants.
             // Javafx allows that.
 //            if (c.owner.kind != PCK &&
 //                ((c.flags() & STATIC) == 0 || c.name == names.empty) &&
 //                (JavafxTreeInfo.flags(l.head) & (STATIC | INTERFACE)) != 0) {
 //                Symbol sym = null;
 //                if (l.head.getFXTag() == JavafxTag.VARDEF) sym = ((JCVariableDecl) l.head).sym;
 //                if (sym == null ||
 //                    sym.kind != VAR ||
 //                    ((VarSymbol) sym).getConstValue() == null)
 //                    log.error(l.head.pos(), "icls.cant.have.static.decl");
 //            }
         }
 
         // If this is a non-abstract class, check that it has no abstract
         // methods or unimplemented methods of an implemented interface.
         if ((c.flags() & (ABSTRACT | INTERFACE)) == 0) {
             if (!relax)
                 chk.checkAllDefined(tree.pos(), c);
         }
 
         // Check that all extended classes and interfaces
         // are compatible (i.e. no two define methods with same arguments
         // yet different return types).  (JLS 8.4.6.3)
         chk.checkCompatibleSupertypes(tree.pos(), c.type);
 
         // Check that all methods which implement some
         // method conform to the method they implement.
         chk.checkImplementations(tree);
 
         Scope enclScope = JavafxEnter.enterScope(env);
         for (List<JFXTree> l = tree.getMembers(); l.nonEmpty(); l = l.tail) {
             if (l.head instanceof JFXFunctionDefinition)
                 chk.checkUnique(l.head.pos(), ((JFXFunctionDefinition) l.head).sym, enclScope);
         }
 
         // Check for proper use of serialVersionUID
         if (env.info.lint.isEnabled(Lint.LintCategory.SERIAL) &&
             isSerializable(c) &&
             (c.flags() & Flags.ENUM) == 0 &&
             (c.flags() & ABSTRACT) == 0) {
             checkSerialVersionUID(tree, c);
         }
     }
         // where
         /** check if a class is a subtype of Serializable, if that is available. */
         private boolean isSerializable(ClassSymbol c) {
             try {
                 syms.serializableType.complete();
             }
             catch (CompletionFailure e) {
                 return false;
             }
             return types.isSubtype(c.type, syms.serializableType);
         }
 
         /** Check that an appropriate serialVersionUID member is defined. */
         private void checkSerialVersionUID(JFXClassDeclaration tree, ClassSymbol c) {
 
             // check for presence of serialVersionUID
             Scope.Entry e = c.members().lookup(names.serialVersionUID);
             while (e.scope != null && e.sym.kind != VAR) e = e.next();
             if (e.scope == null) {
                 log.warning(tree.pos(), MsgSym.MESSAGE_MISSING_SVUID, c);
                 return;
             }
 
             // check that it is static final
             VarSymbol svuid = (VarSymbol)e.sym;
             if ((svuid.flags() & (STATIC | FINAL)) !=
                 (STATIC | FINAL))
                 log.warning(JavafxTreeInfo.diagnosticPositionFor(svuid, tree), MsgSym.MESSAGE_IMPROPER_SVUID, c);
 
             // check that it is long
             else if (svuid.type.tag != TypeTags.LONG)
                 log.warning(JavafxTreeInfo.diagnosticPositionFor(svuid, tree), MsgSym.MESSAGE_LONG_SVUID, c);
 
             // check constant
             else if (svuid.getConstValue() == null)
                 log.warning(JavafxTreeInfo.diagnosticPositionFor(svuid, tree), MsgSym.MESSAGE_CONSTANT_SVUID, c);
         }
 
     private Type capture(Type type) {
         Type ctype = types.capture(type);
         if (type instanceof FunctionType)
             ctype = new FunctionType((FunctionType) type);
         return ctype;
     }
 
     public void clearCaches() {
         varSymToTree = null;
         methodSymToTree = null;
     }
 
     private void fixOverride(JFXFunctionDefinition tree, MethodSymbol m) {
 	ClassSymbol origin = (ClassSymbol)m.owner;
 	if ((origin.flags() & ENUM) != 0 && names.finalize.equals(m.name))
 	    if (m.overrides(syms.enumFinalFinalize, origin, types, false)) {
 		log.error(tree.pos(), MsgSym.MESSAGE_ENUM_NO_FINALIZE);
 		return;
 	    }
 
         ListBuffer<Type> supertypes = ListBuffer.<Type>lb();
         Set superSet = new HashSet<Type>();
         types.getSupertypes(origin, supertypes, superSet);
 
         for (Type t : supertypes) {
             if (t.tag == CLASS) {
                 TypeSymbol c = t.tsym;
                 Scope.Entry e = c.members().lookup(m.name);
                 while (e.scope != null) {
                     e.sym.complete();
                     if (m.overrides(e.sym, origin, types, false))
                         if (fixOverride(tree, m, (MethodSymbol)e.sym, origin))
                             break;
                     e = e.next();
                 }
             }
 	}
     }
 
     public boolean fixOverride(JFXFunctionDefinition tree,
 		       MethodSymbol m,
 		       MethodSymbol other,
 		       ClassSymbol origin) {
 
 	Type mt = types.memberType(origin.type, m);
 	Type ot = types.memberType(origin.type, other);
 	// Error if overriding result type is different
 	// (or, in the case of generics mode, not a subtype) of
 	// overridden result type. We have to rename any type parameters
 	// before comparing types.
 	List<Type> mtvars = mt.getTypeArguments();
 	List<Type> otvars = ot.getTypeArguments();
 	Type mtres = mt.getReturnType();
 	Type otres = types.subst(ot.getReturnType(), otvars, mtvars);
 
 	boolean resultTypesOK =
 	    types.returnTypeSubstitutable(mt, ot, otres, noteWarner);
 	if (!resultTypesOK) {
 	    if (!source.allowCovariantReturns() &&
 		m.owner != origin &&
 		m.owner.isSubClass(other.owner, types)) {
 		// allow limited interoperability with covariant returns
 	    }
             else {
                 Type setReturnType = null;
                 if (mtres == syms.javafx_NumberType && otres == syms.floatType) {
                     setReturnType = syms.floatType;
                 }
                 else if ((mtres == syms.javafx_IntegerType || mtres == syms.javafx_NumberType) && otres == syms.byteType) {
                     setReturnType = syms.byteType;
                 }
                 else if ((mtres == syms.javafx_IntegerType || mtres == syms.javafx_NumberType) && otres == syms.charType) {
                     setReturnType = syms.charType;
                 }
                 else if ((mtres == syms.javafx_IntegerType || mtres == syms.javafx_NumberType) && otres == syms.shortType) {
                     setReturnType = syms.shortType;
                 }
                 else if ((mtres == syms.javafx_IntegerType || mtres == syms.javafx_NumberType) && otres == syms.longType) {
                     setReturnType = syms.longType;
                 }
 
                 if (setReturnType != null) {
                     JFXType oldType = tree.operation.getJFXReturnType();
                     tree.operation.rettype = fxmake.TypeClass(fxmake.Type(setReturnType), oldType.getCardinality());
                     if (mt instanceof MethodType) {
                         ((MethodType)mt).restype = setReturnType;
                     }
 
                     if (tree.type != null && tree.type instanceof MethodType) {
                         ((MethodType)tree.type).restype = setReturnType;
                     }
                 }
             }
 	}
 
         // now fix up the access modifiers
         long origFlags = m.flags();
         long flags = origFlags;
         if ((flags & JavafxFlags.JavafxExplicitAccessFlags) == 0) {
             flags |= other.flags() & (JavafxFlags.JavafxExplicitAccessFlags | JavafxFlags.JavafxAccessFlags);
         }
         if (flags != origFlags) {
             m.flags_field = flags;
             tree.getModifiers().flags = flags;
         }
         return true;
     }
 
     public void visitTimeLiteral(JFXTimeLiteral tree) {
         result = check(tree, syms.javafx_DurationType, VAL, pkind, pt, pSequenceness);
     }
 
     public void visitInterpolate(JFXInterpolate tree) {
         throw new NotImplementedException();
         /*
         tree.getVariable().accept(this);
         for (InterpolateValueTree t : tree.getInterpolateValues()) {
             checkInterpolationValue((JFXInterpolateValue)t, tree.getVariable());
         }
         Type owntype = tree.getVariable().type;
         result = check(tree, owntype, VAL, pkind, pt, pSequenceness);
         */
     }
 
     public void visitInterpolateValue(JFXInterpolateValue tree) {
         attribExpr(tree.attribute, env);
         attribExpr(tree.value, env, tree.attribute.type);
         if (tree.interpolation != null)
             attribExpr(tree.interpolation, env);
         result = check(tree, syms.javafx_KeyValueType, VAL, pkind, pt, pSequenceness);
     }
 
     /*
     private void checkInterpolationValue(JFXInterpolateValue tree, JFXExpression var) {
         final Type targetType;
         if (tree.getAttribute() != null) {
             JFXExpression t = tree.getAttribute();
             JavafxEnv<JavafxAttrContext> localEnv = newLocalEnv(tree);
             Name attribute = names.fromString(t.toString());
             Symbol memberSym = rs.findIdentInType(env, var.type, attribute, VAR);
             memberSym = rs.access(memberSym, t.pos(), var.type, attribute, true);
             memberSym.complete();
             t.type = memberSym.type;
             t.sym = memberSym;
             targetType = t.type;
         } else
             targetType = var.type;
         Type valueType = attribExpr(tree.getValue(), env, Infer.anyPoly);
 
         Type interpolateType = syms.errType;
         if (types.isAssignable(valueType, syms.javafx_ColorType)) {
             interpolateType = syms.javafx_ColorInterpolatorType;
         } else if (types.isAssignable(valueType, syms.javafx_NumberType) ||
                    types.isAssignable(valueType, syms.javafx_IntegerType)) {
             interpolateType = syms.javafx_NumberInterpolatorType;
         } else {
             log.error(tree.pos(), "unexpected.type", Resolve.kindNames(pkind), Resolve.kindName(pkind));
             interpolateType = syms.errType;
         }
         tree.type = interpolateType;
         result = tree.type;
     }
     */
 
     public void visitKeyFrameLiteral(JFXKeyFrameLiteral tree) {
         attribExpr(tree.start, env);
         for (JFXExpression e:tree.values) {
             attribExpr(e, env);
         }
         result = check(tree, syms.javafx_KeyFrameType, VAL, pkind, pt, pSequenceness);
     }
 
     private JFXTree breakTree = null;
 
     public JavafxEnv<JavafxAttrContext> attribExprToTree(JFXTree expr, JavafxEnv<JavafxAttrContext> env, JFXTree tree) {
         breakTree = tree;
         JavaFileObject prev = log.useSource(null);
         try {
             attribExpr(expr, env);
         } catch (BreakAttr b) {
             return b.env;
         } finally {
             breakTree = null;
             log.useSource(prev);
         }
         return env;
     }
 
     public JavafxEnv<JavafxAttrContext> attribStatToTree(JFXTree stmt, JavafxEnv<JavafxAttrContext> env, JFXTree tree) {
         breakTree = tree;
         JavaFileObject prev = log.useSource(null);
         try {
             attribDecl(stmt, env);
         } catch (BreakAttr b) {
             return b.env;
         } finally {
             breakTree = null;
             log.useSource(prev);
         }
         return env;
     }
 
     private static class BreakAttr extends RuntimeException {
         static final long serialVersionUID = -6924771130405446405L;
         private JavafxEnv<JavafxAttrContext> env;
         private BreakAttr(JavafxEnv<JavafxAttrContext> env) {
             this.env = env;
         }
     }
 
     public void visitScript(JFXScript tree) {
 
         // Do not assert that we cannot reach here as this unit can
         // be visited by virtue of visiting JFXErronous which
         // will attempt to visit each Erroneous node that it has
         // encapsualted.
         //
     }
 
     public void visitCatch(JFXCatch tree) {
         // Do not assert that we cannot reach here as this unit can
         // be visited by virtue of visiting JFXErronous which
         // will attempt to visit each Erroneous node that it has
         // encapsualted.
         //
     }
 
     public void visitModifiers(JFXModifiers tree) {
         // Do not assert that we cannot reach here as this unit can
         // be visited by virtue of visiting JFXErronous which
         // will attempt to visit each Erroneous node that it has
         // encapsualted.
         //
     }
 }
