 /**
  *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 package org.stjs.generator.writer;
 
 import static japa.parser.ast.body.ModifierSet.isAbstract;
 import static japa.parser.ast.body.ModifierSet.isStatic;
 import static org.stjs.generator.ast.ASTNodeData.checkParent;
 import static org.stjs.generator.ast.ASTNodeData.parent;
 import static org.stjs.generator.ast.ASTNodeData.resolvedMethod;
 import static org.stjs.generator.ast.ASTNodeData.resolvedType;
 import static org.stjs.generator.ast.ASTNodeData.resolvedVariable;
 import static org.stjs.generator.ast.ASTNodeData.scope;
 import japa.parser.ast.BlockComment;
 import japa.parser.ast.Comment;
 import japa.parser.ast.CompilationUnit;
 import japa.parser.ast.ImportDeclaration;
 import japa.parser.ast.LineComment;
 import japa.parser.ast.Node;
 import japa.parser.ast.PackageDeclaration;
 import japa.parser.ast.TypeParameter;
 import japa.parser.ast.body.AnnotationDeclaration;
 import japa.parser.ast.body.AnnotationMemberDeclaration;
 import japa.parser.ast.body.BodyDeclaration;
 import japa.parser.ast.body.ClassOrInterfaceDeclaration;
 import japa.parser.ast.body.ConstructorDeclaration;
 import japa.parser.ast.body.EmptyMemberDeclaration;
 import japa.parser.ast.body.EmptyTypeDeclaration;
 import japa.parser.ast.body.EnumConstantDeclaration;
 import japa.parser.ast.body.EnumDeclaration;
 import japa.parser.ast.body.FieldDeclaration;
 import japa.parser.ast.body.InitializerDeclaration;
 import japa.parser.ast.body.JavadocComment;
 import japa.parser.ast.body.MethodDeclaration;
 import japa.parser.ast.body.ModifierSet;
 import japa.parser.ast.body.Parameter;
 import japa.parser.ast.body.TypeDeclaration;
 import japa.parser.ast.body.VariableDeclarator;
 import japa.parser.ast.body.VariableDeclaratorId;
 import japa.parser.ast.expr.ArrayAccessExpr;
 import japa.parser.ast.expr.ArrayCreationExpr;
 import japa.parser.ast.expr.ArrayInitializerExpr;
 import japa.parser.ast.expr.AssignExpr;
 import japa.parser.ast.expr.BinaryExpr;
 import japa.parser.ast.expr.BinaryExpr.Operator;
 import japa.parser.ast.expr.BooleanLiteralExpr;
 import japa.parser.ast.expr.CastExpr;
 import japa.parser.ast.expr.CharLiteralExpr;
 import japa.parser.ast.expr.ClassExpr;
 import japa.parser.ast.expr.ConditionalExpr;
 import japa.parser.ast.expr.DoubleLiteralExpr;
 import japa.parser.ast.expr.EnclosedExpr;
 import japa.parser.ast.expr.Expression;
 import japa.parser.ast.expr.FieldAccessExpr;
 import japa.parser.ast.expr.InstanceOfExpr;
 import japa.parser.ast.expr.IntegerLiteralExpr;
 import japa.parser.ast.expr.IntegerLiteralMinValueExpr;
 import japa.parser.ast.expr.LongLiteralExpr;
 import japa.parser.ast.expr.LongLiteralMinValueExpr;
 import japa.parser.ast.expr.MarkerAnnotationExpr;
 import japa.parser.ast.expr.MemberValuePair;
 import japa.parser.ast.expr.MethodCallExpr;
 import japa.parser.ast.expr.NameExpr;
 import japa.parser.ast.expr.NormalAnnotationExpr;
 import japa.parser.ast.expr.NullLiteralExpr;
 import japa.parser.ast.expr.ObjectCreationExpr;
 import japa.parser.ast.expr.QualifiedNameExpr;
 import japa.parser.ast.expr.SingleMemberAnnotationExpr;
 import japa.parser.ast.expr.StringLiteralExpr;
 import japa.parser.ast.expr.SuperExpr;
 import japa.parser.ast.expr.ThisExpr;
 import japa.parser.ast.expr.UnaryExpr;
 import japa.parser.ast.expr.VariableDeclarationExpr;
 import japa.parser.ast.stmt.AssertStmt;
 import japa.parser.ast.stmt.BlockStmt;
 import japa.parser.ast.stmt.BreakStmt;
 import japa.parser.ast.stmt.CatchClause;
 import japa.parser.ast.stmt.ContinueStmt;
 import japa.parser.ast.stmt.DoStmt;
 import japa.parser.ast.stmt.EmptyStmt;
 import japa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
 import japa.parser.ast.stmt.ExpressionStmt;
 import japa.parser.ast.stmt.ForStmt;
 import japa.parser.ast.stmt.ForeachStmt;
 import japa.parser.ast.stmt.IfStmt;
 import japa.parser.ast.stmt.LabeledStmt;
 import japa.parser.ast.stmt.ReturnStmt;
 import japa.parser.ast.stmt.Statement;
 import japa.parser.ast.stmt.SwitchEntryStmt;
 import japa.parser.ast.stmt.SwitchStmt;
 import japa.parser.ast.stmt.SynchronizedStmt;
 import japa.parser.ast.stmt.ThrowStmt;
 import japa.parser.ast.stmt.TryStmt;
 import japa.parser.ast.stmt.TypeDeclarationStmt;
 import japa.parser.ast.stmt.WhileStmt;
 import japa.parser.ast.type.ClassOrInterfaceType;
 import japa.parser.ast.type.PrimitiveType;
 import japa.parser.ast.type.ReferenceType;
 import japa.parser.ast.type.VoidType;
 import japa.parser.ast.type.WildcardType;
 import japa.parser.ast.visitor.VoidVisitor;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.lang.reflect.Modifier;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 
 import org.stjs.generator.GenerationContext;
 import org.stjs.generator.GeneratorConstants;
 import org.stjs.generator.JavascriptFileGenerationException;
 import org.stjs.generator.ast.ASTNodeData;
 import org.stjs.generator.ast.SourcePosition;
 import org.stjs.generator.name.DefaultNameProvider;
 import org.stjs.generator.name.NameProvider;
 import org.stjs.generator.scope.ClassScope;
 import org.stjs.generator.scope.Scope;
 import org.stjs.generator.type.ClassWrapper;
 import org.stjs.generator.type.FieldWrapper;
 import org.stjs.generator.type.MethodWrapper;
 import org.stjs.generator.type.ParameterizedTypeWrapper;
 import org.stjs.generator.type.TypeWrapper;
 import org.stjs.generator.type.TypeWrappers;
 import org.stjs.generator.utils.ClassUtils;
 import org.stjs.generator.utils.Lists;
 import org.stjs.generator.utils.NodeUtils;
 import org.stjs.generator.utils.Option;
 import org.stjs.generator.utils.PreConditions;
 import org.stjs.generator.variable.Variable;
 import org.stjs.javascript.Array;
 import org.stjs.javascript.annotation.GlobalScope;
 import org.stjs.javascript.annotation.Native;
 
 import com.google.common.base.Defaults;
 
 /**
  * This class visits the AST corresponding to a Java file and generates the corresponding Javascript code. It presumes
  * the {@link org.stjs.generator.scope.ScopeBuilder} previously visited the tree and set the resolved name of certain
  * nodes.
  */
 // PMD mistakes AST.Statement for jdbc.Statement (WTF!)
 // for excessive long class -> a whole new design is need
 @SuppressWarnings({ "PMD.ExcessivePublicCount", "PMD.ExcessiveClassLength", "PMD.CloseResource" })
 public class JavascriptWriterVisitor implements VoidVisitor<GenerationContext> {
 
 	private static final String EQUALS = " = ";
 
 	private static final int INLINE_CREATION_PARENT_LEVEL = 3;
 
 	private final MethodCallTemplates specialMethodHandlers;
 
 	private final NameProvider names;
 
 	private final JavascriptWriter printer;
 
 	private List<Comment> comments;
 
 	private int currentComment;
 
 	public JavascriptWriterVisitor(ClassLoader builtProjectClassLoader, boolean generateSourceMap) {
 		specialMethodHandlers = new MethodCallTemplates(builtProjectClassLoader);
 		names = new DefaultNameProvider();
 		printer = new JavascriptWriter(generateSourceMap);
 	}
 
 	public String getGeneratedSource() {
 		return printer.getSource();
 	}
 
 	@Override
 	public void visit(CompilationUnit n, GenerationContext context) {
 		comments = n.getComments();
 		if (n.getTypes() != null) {
 			for (Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
 				i.next().accept(this, context);
 				printer.printLn();
 				if (i.hasNext()) {
 					printer.printLn();
 				}
 			}
 		}
 		printer.addSourceMapURL(context);
 	}
 
 	@Override
 	public void visit(ClassOrInterfaceType n, GenerationContext context) {
 		printer.print(names.getTypeName(resolvedType(n)));
 	}
 
 	@Override
 	public void visit(ReferenceType n, GenerationContext context) {
 		// skip
 	}
 
 	@Override
 	public void visit(ImportDeclaration n, GenerationContext context) {
 		// skip
 	}
 
 	@Override
 	public void visit(PackageDeclaration n, GenerationContext context) {
 		// skip
 	}
 
 	@Override
 	public void visit(MarkerAnnotationExpr n, GenerationContext context) {
 		// skip
 	}
 
 	@Override
 	public void visit(SynchronizedStmt n, GenerationContext context) {
 		throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(n),
 				"synchronized blocks are not supported by Javascript");
 	}
 
 	@Override
 	public void visit(CastExpr n, GenerationContext context) {
 		boolean integerType = ClassUtils.isIntegerType(n.getType());
 
 		if (integerType) {
 			printer.print("stjs.trunc(");
 		}
 		// skip to cast type - continue with the expression
 		if (n.getExpr() != null) {
 			n.getExpr().accept(this, context);
 		}
 		if (integerType) {
 			printer.print(")");
 		}
 	}
 
 	@Override
 	public void visit(IntegerLiteralExpr n, GenerationContext context) {
 		printer.printNumberLiteral(n.getValue());
 	}
 
 	@Override
 	public void visit(LongLiteralExpr n, GenerationContext context) {
 		printer.printNumberLiteral(n.getValue());
 	}
 
 	@Override
 	public void visit(StringLiteralExpr n, GenerationContext context) {
 		printer.printStringLiteral(n.getValue());
 	}
 
 	@Override
 	public void visit(CharLiteralExpr n, GenerationContext context) {
 		printer.printCharLiteral(n.getValue());
 	}
 
 	@Override
 	public void visit(DoubleLiteralExpr n, GenerationContext context) {
 		printer.printNumberLiteral(n.getValue());
 	}
 
 	@Override
 	public void visit(BooleanLiteralExpr n, GenerationContext context) {
 		printer.printLiteral(Boolean.toString(n.getValue()));
 	}
 
 	public void print(StringLiteralExpr n) {
 		// java has some more syntax to declare integers :
 		// 0x0, 0b0, (java7) 1_000_000
 		// TODO : convert it to plain numbers for javascript
 		printer.printLiteral(n.getValue());
 	}
 
 	private void printEnumEntries(EnumDeclaration n) {
 		if (n.getEntries() != null) {
 			for (Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
 				EnumConstantDeclaration e = i.next();
 				printer.printStringLiteral(e.getName());
 				if (i.hasNext()) {
 					printer.printLn(", ");
 				}
 			}
 		}
 	}
 
 	@Override
 	public void visit(EnumDeclaration n, GenerationContext context) {
 
 		Scope scope = scope(n);
 		ClassWrapper type = (ClassWrapper) scope.resolveType(n.getName()).getType();
 		String namespace = ClassUtils.getNamespace(type);
 		if (namespace != null) {
 			printer.printLn("stjs.ns(\"" + namespace + "\");");
 		}
 
 		printComments(n, context);
 		// printer.print(n.getName());
 		ClassWrapper outerType = type.getDeclaringClass().getOrNull();
 		boolean isDeepInnerType = type.isInnerType() && outerType.isInnerType();
 		if (!type.isInnerType() && namespace == null) {
 			printer.print("var ");
 		}
 		if (isDeepInnerType) {
 			printer.print("constructor.");
 			printer.print(type.getSimpleName());
 			printer.print(EQUALS);
 		} else {
 			printer.print(names.getTypeName(type));
 			printer.print(EQUALS);
 		}
 
 		// TODO implements not considered
 		printer.printLn("stjs.enumeration(");
 		printer.indent();
 		printEnumEntries(n);
 
 		// TODO members not considered
 		printer.printLn("");
 		printer.unindent();
 		printer.print(")");
 		if (!isDeepInnerType) {
 			printer.print(";");
 		}
 	}
 
 	@Override
 	public void visit(ForeachStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("for (");
 		n.getVariable().accept(this, context);
 		printer.print(" in ");
 		n.getIterable().accept(this, context);
 		printer.print(") ");
 
 		printer.addSouceMapping(context);
 
 		boolean hasBlockBody = n.getBody() instanceof BlockStmt;
 
 		// add braces when we have one line statement
 		if (!hasBlockBody) {
 			printer.printLn("{");
 			printer.indent();
 			generateArrayHasOwnProperty(n, context);
 		}
 		n.getBody().accept(this, context);
 
 		if (!hasBlockBody) {
 			printer.printLn();
 			printer.unindent();
 			printer.print("}");
 		}
 	}
 
 	@Override
 	public void visit(IfStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("if (");
 		n.getCondition().accept(this, context);
 		printer.print(") ");
 		printer.addSouceMapping(context);
 		n.getThenStmt().accept(this, context);
 		if (n.getElseStmt() != null) {
 			printer.print(" else ");
 			n.getElseStmt().accept(this, context);
 		}
 	}
 
 	@Override
 	public void visit(WhileStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("while (");
 		n.getCondition().accept(this, context);
 		printer.print(") ");
 		printer.addSouceMapping(context);
 		n.getBody().accept(this, context);
 	}
 
 	@Override
 	public void visit(ContinueStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("continue");
 		if (n.getId() != null) {
 			printer.print(" ");
 			printer.print(n.getId());
 		}
 		printer.print(";");
 		printer.addSouceMapping(context);
 	}
 
 	@Override
 	public void visit(DoStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("do ");
 		printer.addSouceMapping(context);
 		n.getBody().accept(this, context);
 		printer.print(" while (");
 		n.getCondition().accept(this, context);
 		printer.print(");");
 	}
 
 	private void printForInit(ForStmt n, GenerationContext context) {
 		if (n.getInit() != null) {
 			for (Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
 				Expression e = i.next();
 				e.accept(this, context);
 				if (i.hasNext()) {
 					printer.print(", ");
 				}
 			}
 		}
 	}
 
 	private void printForUpdate(ForStmt n, GenerationContext context) {
 		if (n.getUpdate() != null) {
 			for (Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
 				Expression e = i.next();
 				e.accept(this, context);
 				if (i.hasNext()) {
 					printer.print(", ");
 				}
 			}
 		}
 
 	}
 
 	@Override
 	public void visit(ForStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("for (");
 		printForInit(n, context);
 		printer.print("; ");
 		if (n.getCompare() != null) {
 			n.getCompare().accept(this, context);
 		}
 		printer.print("; ");
 		printForUpdate(n, context);
 		printer.print(") ");
 		printer.addSouceMapping(context);
 		n.getBody().accept(this, context);
 	}
 
 	@Override
 	public void visit(VariableDeclaratorId n, GenerationContext context) {
 		if (parent(n) instanceof Parameter && n.getName().equals(GeneratorConstants.ARGUMENTS_PARAMETER)) {
 			// add an "_" for the arguments parameter to no override to arguments one.
 			printer.print("_");
 		}
 		printer.print(n.getName());
 	}
 
 	@Override
 	public void visit(VariableDeclarator n, GenerationContext context) {
 		throw new IllegalStateException("Unexpected visit in a VariableDeclarator node:" + n);
 	}
 
 	private void printVariableDeclarator(VariableDeclarator n, GenerationContext context, boolean forceInitNull) {
 		n.getId().accept(this, context);
 		if (n.getInit() == null) {
 			if (forceInitNull) {
 				printer.print(EQUALS);
 				TypeWrapper type = resolvedType(n);
 				if (type instanceof ClassWrapper && ((ClassWrapper) type).getClazz().isPrimitive()) {
 					Object defaultValue = Defaults.defaultValue(((ClassWrapper) type).getClazz());
 					printer.printLiteral(defaultValue.toString());
 				} else {
 					printer.print(JavascriptKeywords.NULL);
 				}
 			}
 		} else {
 			printer.print(EQUALS);
 			n.getInit().accept(this, context);
 		}
 	}
 
 	@Override
 	public void visit(VariableDeclarationExpr n, GenerationContext context) {
 		// skip type
 		printer.print("var ");
 
 		for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
 			VariableDeclarator v = i.next();
 			printVariableDeclarator(v, context, false);
 			if (i.hasNext()) {
 				printer.print(", ");
 			}
 		}
 	}
 
 	@Override
 	public void visit(FieldDeclaration n, GenerationContext context) {
 		TypeWrapper type = resolvedType(parent(n));
 		boolean global = isGlobal(type) && isStatic(n.getModifiers());

 		for (VariableDeclarator v : n.getVariables()) {
			if (global) {
				printer.print(JavascriptKeywords.VAR).print(" ");
			} else {
 				if (isStatic(n.getModifiers())) {
 					printer.print(JavascriptKeywords.CONSTRUCTOR).print(".");
 				} else {
 					printer.print(JavascriptKeywords.PROTOTYPE).print(".");
 				}
 			}
 			printVariableDeclarator(v, context, true);
			printer.printLn(";");
 		}
 	}
 
 	private void printJavadoc(JavadocComment javadoc, GenerationContext context) {
 		if (javadoc != null) {
 			javadoc.accept(this, context);
 		}
 	}
 
 	private void printComments(Node n, GenerationContext context) {
 		if (comments == null) {
 			return;
 		}
 		// the problem is that the comments are all attached to the root node
 		// so this method will display all the comments before the given node.
 		while (currentComment < comments.size()) {
 			if (comments.get(currentComment).getBeginLine() < n.getBeginLine()) {
 				comments.get(currentComment).accept(this, context);
 			} else {
 				break;
 			}
 			currentComment++;
 		}
 	}
 
 	private void printMethodName(String name, int modifiers, TypeWrapper type, boolean anonymous, boolean isInnerClassConstructor) {
 		// no type appears for global scopes
 		boolean global = isGlobal(type) && isStatic(modifiers);
 		if (!anonymous) {
 			if (!global) {
 				if (isStatic(modifiers)) {
 					printer.print("constructor.");
 				} else {
 					printer.print("prototype.");
 				}
 			}
 			printer.print(name);
 			printer.print(EQUALS);
 		}
 		printer.print("function");
 		if (isInnerClassConstructor) {
 			printer.print(" ");
 			printer.print(name);
 		}
 	}
 
 	private void printMethodParameters(List<Parameter> parameters, GenerationContext context) {
 		if (parameters != null) {
 			boolean first = true;
 			for (Parameter p : parameters) {
 				// don't display the special THIS parameter
 				if (GeneratorConstants.SPECIAL_THIS.equals(p.getId().getName())) {
 					continue;
 				}
 				if (!first) {
 					printer.print(", ");
 				}
 				p.accept(this, context);
 				first = false;
 			}
 		}
 	}
 
 	// i need this method with more parameters than usual
 	// CHECKSTYLE:OFF
 	private void printMethod(String name, List<Parameter> parameters, int modifiers, BlockStmt body, GenerationContext context,
 			TypeWrapper type, boolean anonymous, boolean isInnerClassConstructor) {
 		// CHECKSTYLE:ON
 
 		if (ModifierSet.isAbstract(modifiers) || ModifierSet.isNative(modifiers)) {
 			return;
 		}
 
 		printMethodName(name, modifiers, type, anonymous, isInnerClassConstructor);
 		printer.print("(");
 		printMethodParameters(parameters, context);
 		printer.print(")");
 		// skip throws
 		if (body == null) {
 			printer.print("{}");
 		} else {
 			printer.print(" ");
 			body.accept(this, context);
 		}
 		if (!anonymous) {
 			printer.print(";");
 		}
 	}
 
 	private MethodDeclaration getMethodDeclaration(ObjectCreationExpr n) {
 		MethodDeclaration singleMethod = null;
 		for (BodyDeclaration d : n.getAnonymousClassBody()) {
 			if (d instanceof MethodDeclaration) {
 				if (singleMethod != null) {
 					// there are more methods -> back to standard declaration
 					return null;
 				}
 				singleMethod = (MethodDeclaration) d;
 			} else if (d instanceof FieldDeclaration) {
 				// back to standard declaration
 				return null;
 			}
 		}
 		return singleMethod;
 	}
 
 	public void printArguments(List<Expression> expressions, GenerationContext context) {
 		printArguments(Collections.<String>emptyList(), expressions, Collections.<String>emptyList(), context);
 	}
 
 	public void printArguments(Collection<String> beforeParams, Collection<Expression> expressions, Collection<String> afterParams,
 			GenerationContext context) {
 		printer.print("(");
 		printer.printList(beforeParams);
 		boolean first = beforeParams.isEmpty();
 		if (expressions != null) {
 			for (Expression e : expressions) {
 				if (!first) {
 					printer.print(", ");
 				}
 				e.accept(this, context);
 				first = false;
 			}
 		}
 		if (!first && !afterParams.isEmpty()) {
 			printer.print(", ");
 		}
 		printer.printList(afterParams);
 		printer.print(")");
 	}
 
 	private InitializerDeclaration getInitializerDeclaration(ObjectCreationExpr n) {
 		if (n.getAnonymousClassBody() == null) {
 			return null;
 		}
 		for (BodyDeclaration d : n.getAnonymousClassBody()) {
 			if (d instanceof InitializerDeclaration) {
 				return (InitializerDeclaration) d;
 			}
 		}
 		return null;
 	}
 
 	private ClassOrInterfaceDeclaration buildClassDeclaration(String className, ClassOrInterfaceType extendsFrom, List<BodyDeclaration> members) {
 		ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration();
 		decl.setName(className);
 		ClassWrapper baseClass = (ClassWrapper) resolvedType(extendsFrom);
 		if (baseClass.getClazz().isInterface()) {
 			decl.setImplements(Collections.singletonList(extendsFrom));
 		} else {
 			decl.setExtends(Collections.singletonList(extendsFrom));
 		}
 		decl.setMembers(members);
 		// TODO add constructor if needed to call the super with the constructorArguments
 		return decl;
 	}
 
 	private void printInlineFunction(ObjectCreationExpr n, GenerationContext context) {
 		MethodDeclaration method = getMethodDeclaration(n);
 		PreConditions.checkStateNode(n, method != null, "A single method was expected for an inline function");
 		if (method != null) {
 			printMethod(method.getName(), method.getParameters(), method.getModifiers(), method.getBody(), context, resolvedType(n), true, false);
 		}
 	}
 
 	@Override
 	public void visit(ObjectCreationExpr n, GenerationContext context) {
 		InitializerDeclaration block = getInitializerDeclaration(n);
 		if (block != null) {
 			// special construction for object initialization new Object(){{x = 1; y = 2; }};
 			block.getBlock().accept(this, context);
 			return;
 		}
 
 		TypeWrapper clazz = resolvedType(n.getType());
 
 		if (!Lists.isNullOrEmpty(n.getAnonymousClassBody())) {
 			// special construction for inline function definition
 			if (ClassUtils.isJavascriptFunction(clazz)) {
 				printInlineFunction(n, context);
 				return;
 			}
 
 			// special construction to handle the inline body
 			printer.print("new ");
 			ClassOrInterfaceDeclaration inlineFakeClass = buildClassDeclaration(GeneratorConstants.SPECIAL_INLINE_TYPE, n.getType(),
 					n.getAnonymousClassBody());
 			inlineFakeClass.setData(n.getData());
 			inlineFakeClass.accept(this, context);
 
 			printArguments(n.getArgs(), context);
 			return;
 		}
 
 		if (clazz instanceof ClassWrapper && ClassUtils.isSyntheticType(clazz)) {
 			// this is a call to an mock type
 			printer.print("{}");
 			return;
 		}
 		printer.print("new ");
 		n.getType().accept(this, context);
 		printArguments(n.getArgs(), context);
 	}
 
 	@Override
 	public void visit(Parameter n, GenerationContext context) {
 		// skip type
 		n.getId().accept(this, context);
 	}
 
 	@Override
 	public void visit(MethodDeclaration n, GenerationContext context) {
 		if (ModifierSet.isNative(n.getModifiers())) {
 			// native methods are there only to indicate already existing javascript code - or to allow method
 			// overloading
 			return;
 		}
 		printComments(n, context);
 		printMethod(names.getMethodName(resolvedMethod(n)), n.getParameters(), n.getModifiers(), n.getBody(), context, resolvedType(parent(n)),
 				false, false);
 	}
 
 	private void addCallToSuper(ClassScope classScope, GenerationContext context, Collection<Expression> args, boolean apply) {
 		PreConditions.checkNotNull(classScope);
 
 		Option<ClassWrapper> superClass = classScope.getClazz().getSuperclass();
 
 		if (superClass.isDefined()) {
 			if (ClassUtils.isSyntheticType(superClass.getOrThrow())) {
 				// do not add call to super class is it's a synthetic
 				return;
 			}
 
 			if (superClass.getOrThrow().getClazz().equals(Object.class)) {
 				// avoid useless call to super() when the super class is Object
 				return;
 			}
 			printer.print(names.getTypeName(superClass.getOrThrow()));
 			if (apply) {
 				printer.print(".apply(this, arguments)");
 			} else {
 				printer.print(".call");
 				printArguments(Collections.singleton("this"), args, Collections.<String>emptyList(), context);
 			}
 			printer.print(";");
 
 		}
 	}
 
 	private void printCallToSuper(ConstructorDeclaration n) {
 		boolean addCallToSuper = false;
 		if (n.getBlock().getStmts() != null && n.getBlock().getStmts().size() > 0) {
 			Statement firstStatement = n.getBlock().getStmts().get(0);
 			if (!(firstStatement instanceof ExplicitConstructorInvocationStmt)) {
 				addCallToSuper = true;
 			}
 		} else {
 			// empty constructor
 			addCallToSuper = true;
 		}
 
 		if (addCallToSuper) {
 			// generate possibly missing super() call
 			Statement callSuper = new ExplicitConstructorInvocationStmt();
 			callSuper.setData(new ASTNodeData());
 			parent(callSuper, n.getBlock());
 			scope(callSuper, scope(n.getBlock()));
 			if (n.getBlock().getStmts() == null) {
 				n.getBlock().setStmts(new ArrayList<Statement>());
 			}
 			n.getBlock().getStmts().add(0, callSuper);
 		}
 	}
 
 	@Override
 	public void visit(ConstructorDeclaration n, GenerationContext context) {
 		MethodWrapper c = ASTNodeData.resolvedMethod(n);
 		if (c != null && c.getAnnotationDirectly(Native.class) != null) {
 			// this is a "native" constructor - no code is generator
 			return;
 		}
 		printComments(n, context);
 		Option<ClassWrapper> superClass = scope(n).closest(ClassScope.class).getClazz().getSuperclass();
 		if (superClass.isDefined() && !ClassUtils.isSyntheticType(superClass.getOrThrow())) {
 			printCallToSuper(n);
 		}
 		ClassWrapper type = (ClassWrapper) resolvedType(parent(n));
 		printMethod(type.getSimpleBinaryName(), n.getParameters(), n.getModifiers(), n.getBlock(), context, type, true, type.isInnerType()
 				|| type.isAnonymousClass());
 	}
 
 	@Override
 	public void visit(TypeParameter n, GenerationContext context) {
 		// skip
 
 	}
 
 	@Override
 	public void visit(LineComment n, GenerationContext context) {
 		printer.print("//");
 		if (n.getContent().endsWith("\n")) {
 			// remove trailing enter and printLn
 			// to keep indentation
 			printer.printLn(n.getContent().substring(0, n.getContent().length() - 1));
 		}
 
 	}
 
 	@Override
 	public void visit(BlockComment n, GenerationContext context) {
 		printer.print("/*");
 		printer.print(n.getContent());
 		printer.printLn("*/");
 	}
 
 	private List<TypeWrapper> getImplements(ClassOrInterfaceDeclaration n) {
 		List<TypeWrapper> types = new ArrayList<TypeWrapper>();
 		if (n.getImplements() != null) {
 			for (ClassOrInterfaceType impl : n.getImplements()) {
 				TypeWrapper type = resolvedType(impl);
 				if (!ClassUtils.isSyntheticType(type)) {
 					types.add(type);
 				}
 			}
 		}
 		return types;
 	}
 
 	private List<TypeWrapper> getExtends(ClassOrInterfaceDeclaration n) {
 		List<TypeWrapper> types = new ArrayList<TypeWrapper>();
 		if (n.getExtends() != null) {
 			for (ClassOrInterfaceType ext : n.getExtends()) {
 				TypeWrapper type = resolvedType(ext);
 				if (!ClassUtils.isSyntheticType(type)) {
 					types.add(type);
 				}
 
 			}
 		}
 		return types;
 	}
 
 	private String printNamespace(ClassWrapper type) {
 		String namespace = null;
 		if (!ClassUtils.isInnerType(type)) {
 			namespace = ClassUtils.getNamespace(type);
 			if (namespace != null) {
 				printer.printLn("stjs.ns(\"" + namespace + "\");");
 			}
 		}
 		return namespace;
 	}
 
 	private void printIntefaces(ClassOrInterfaceDeclaration n) {
 		// print the implemented interfaces
 		List<TypeWrapper> interfaces;
 		if (n.isInterface()) {
 			interfaces = getExtends(n);
 		} else {
 			interfaces = getImplements(n);
 		}
 		printer.print("[");
 		for (int i = 0; i < interfaces.size(); i++) {
 			if (i > 0) {
 				printer.print(", ");
 			}
 			printer.print(names.getTypeName(interfaces.get(i)));
 		}
 		printer.print("]");
 	}
 
 	private void printSuperClass(ClassOrInterfaceDeclaration n) {
 		if (n.isInterface()) {
 			// interfaces do not have super classes. For interfaces, extends actually means implements
 			printer.print("null, ");
 		} else {
 			List<TypeWrapper> superClass = getExtends(n);
 			if (superClass.isEmpty()) {
 				printer.print(JavascriptKeywords.NULL);
 			} else {
 				printer.print(names.getTypeName(superClass.get(0)));
 			}
 			printer.print(", ");
 		}
 	}
 
 	private void printTypeName(ClassOrInterfaceDeclaration n, GenerationContext context, String namespace) {
 		ClassWrapper type = (ClassWrapper) resolvedType(n);
 		ClassScope scope = (ClassScope) scope(n);
 		String className = null;
 		if (type.isAnonymousClass()) {
 			printer.print("(");
 		} else {
 			if (!type.isInnerType() && namespace == null) {
 				printer.print("var ");
 			}
 			className = names.getTypeName(type);
 			if (type.isInnerType()) {
 				printer.print("constructor.");
 				printer.print(type.getSimpleName());
 			} else {
 				printer.print(className);
 			}
 			printer.print(EQUALS);
 			if (!type.hasAnonymousDeclaringClass()) {
 				printConstructorImplementation(n, context, scope, type.isAnonymousClass());
 				printer.printLn(";");
 			}
 		}
 	}
 
 	private void printNonGlobalClass(ClassOrInterfaceDeclaration n, ClassWrapper type, ClassScope scope, GenerationContext context) {
 		String namespace = printNamespace(type);
 		printTypeName(n, context, namespace);
 		printer.print("stjs.extend(");
 		if (type.isAnonymousClass() || type.hasAnonymousDeclaringClass()) {
 			printConstructorImplementation(n, context, scope, type.isAnonymousClass());
 		} else {
 			String className = names.getTypeName(type);
 			printer.print(className);
 		}
 		printer.print(", ");
 
 		printSuperClass(n);
 		printIntefaces(n);
 		printer.print(", ");
 
 		printMembers(n.getMembers(), context);
 		printer.print(", ");
 
 		printTypeDescription(n);
 		printer.print(")");
 
 		if (type.isAnonymousClass()) {
 			printer.print(")");
 		} else {
 			printer.printLn(";");
 			if (!type.isInnerType()) {
 				printStaticInitializers(n, context);
 				printMainMethodCall(n, type);
 			}
 		}
 	}
 
 	@Override
 	public void visit(ClassOrInterfaceDeclaration n, GenerationContext context) {
 		printComments(n, context);
 
 		ClassScope scope = (ClassScope) scope(n);
 
 		if (resolvedType(n) == null) {
 			// for anonymous object creation the type is set already
 			resolvedType(n, scope.resolveType(n.getName()).getType());
 		}
 
 		ClassWrapper type = (ClassWrapper) resolvedType(n);
 
 		// the @GlobalScope classes will not be displayed with extends definition
 		if (isGlobal(type)) {
 			printGlobals(filterGlobals(n, type), context);
 			printStaticInitializers(n, context);
 			printMainMethodCall(n, type);
 			return;
 		}
 
 		printNonGlobalClass(n, type, scope, context);
 	}
 
 	private boolean isTypeOrStaticMember(BodyDeclaration decl) {
 		return isClassOrInterface(decl) || isEnum(decl) || isStaticField(decl) || isStaticMethod(decl);
 	}
 
 	private List<BodyDeclaration> filterGlobals(ClassOrInterfaceDeclaration n, ClassWrapper outerType) {
 		if (!isGlobal(outerType)) {
 			return Collections.emptyList();
 		}
 		List<BodyDeclaration> decls = new ArrayList<BodyDeclaration>();
 		for (BodyDeclaration decl : n.getMembers()) {
 			if (isTypeOrStaticMember(decl)) {
 				decls.add(decl);
 			}
 		}
 		return decls;
 	}
 
 	private void printConstructorImplementation(ClassOrInterfaceDeclaration n, GenerationContext context, ClassScope scope, boolean inlineType) {
 		ConstructorDeclaration constr = getConstructor(n.getMembers(), context);
 		if (constr == null) {
 			ClassWrapper type = (ClassWrapper) resolvedType(n);
 			printer.print("function");
 			if (type.isInnerType() || type.isAnonymousClass()) {
 				printer.print(" ");
 				printer.print(type.getSimpleBinaryName());
 			}
 			printer.print("(){");
 			addCallToSuper(scope, context, Collections.<Expression>emptyList(), inlineType);
 			printer.print("}");
 		} else {
 			constr.accept(this, context);
 		}
 	}
 
 	private void printStaticInitializers(ClassOrInterfaceDeclaration n, GenerationContext context) {
 		if (n.getMembers() == null) {
 			return;
 		}
 		for (BodyDeclaration decl : n.getMembers()) {
 			if (decl instanceof InitializerDeclaration && ((InitializerDeclaration) decl).isStatic()) {
 				printStaticInitializer((InitializerDeclaration) decl, context);
 			}
 		}
 
 	}
 
 	private void printStaticInitializer(InitializerDeclaration n, GenerationContext context) {
 		// we have to wrap the static initialization block into a function to prevent the local variables
 		// to leak into the global scope
 		printer.print("(function()");
 		n.getBlock().accept(this, context);
 		printer.printLn(")();");
 	}
 
 	private void appendTypeArguments(StringBuilder s, ParameterizedTypeWrapper pt) {
 		boolean first = true;
 		for (TypeWrapper arg : pt.getActualTypeArguments()) {
 			if (!first) {
 				s.append(',');
 			}
 			s.append(stjsNameInfo(arg));
 			first = false;
 		}
 	}
 
 	/**
 	 * @param typeWrapper
 	 * @return the name of the given type. if the type is a parameterized type it returns {name:"type-name",
 	 *         arguments:[args..]}
 	 */
 	private String stjsNameInfo(TypeWrapper typeWrapper) {
 		// We may want to use a more complex naming scheme, to avoid conflicts across packages
 		if (typeWrapper instanceof ParameterizedTypeWrapper) {
 			ParameterizedTypeWrapper pt = (ParameterizedTypeWrapper) typeWrapper;
 			StringBuilder s = new StringBuilder();
 			s.append("{name:\"").append(pt.getExternalName()).append('\"');
 
 			s.append(", arguments:[");
 			appendTypeArguments(s, pt);
 			s.append(']');
 			s.append('}');
 			return s.toString();
 		}
 
 		if (typeWrapper instanceof ClassWrapper && ((ClassWrapper) typeWrapper).getClazz().isEnum()) {
 			StringBuilder s = new StringBuilder();
 			s.append("{name:\"Enum\"");
 			s.append(", arguments:[");
 			s.append("\"" + names.getTypeName(typeWrapper) + "\"");
 			s.append(']');
 			s.append('}');
 			return s.toString();
 		}
 		if (ClassUtils.isBasicType(typeWrapper)) {
 			return JavascriptKeywords.NULL;
 		}
 		return "\"" + names.getTypeName(typeWrapper) + "\"";
 	}
 
 	private boolean printFieldDescription(FieldDeclaration field, boolean prevFirst) {
 		TypeWrapper fieldType = resolvedType(field.getType());
 
 		if (ClassUtils.isBasicType(fieldType)) {
 			return prevFirst;
 		}
 		boolean first = prevFirst;
 		for (VariableDeclarator v : field.getVariables()) {
 			if (!first) {
 				printer.print(", ");
 			}
 			printer.print("\"").print(v.getId().getName()).print("\":");
 			printer.print(stjsNameInfo(fieldType));
 			first = false;
 		}
 		return first;
 	}
 
 	/**
 	 * print the information needed to deserialize type-safe from json
 	 * 
 	 * @param n
 	 * @param context
 	 */
 	private void printTypeDescription(ClassOrInterfaceDeclaration n) {
 		TypeWrapper type = resolvedType(n);
 		if (isGlobal(type)) {
 			printer.print(JavascriptKeywords.NULL);
 			return;
 		}
 
 		printer.print("{");
 		if (n.getMembers() != null) {
 			boolean first = true;
 			for (BodyDeclaration member : n.getMembers()) {
 				if (member instanceof FieldDeclaration) {
 					FieldDeclaration field = (FieldDeclaration) member;
 					first = printFieldDescription(field, first);
 				}
 			}
 		}
 		printer.print("}");
 	}
 
 	private void printMembers(List<BodyDeclaration> members, GenerationContext context) {
 		// the following members must not appear in the initializer function:
 		// - constructors (they are printed elsewhere)
 		// - abstract methods (they should be omitted)
 
 		List<BodyDeclaration> nonConstructors = new ArrayList<BodyDeclaration>();
 		for (BodyDeclaration member : members) {
 			if (!isConstructor(member) && !isAbstractInstanceMethod(member)) {
 				nonConstructors.add(member);
 			}
 		}
 
 		if (nonConstructors.isEmpty()) {
 			printer.print(JavascriptKeywords.NULL);
 		} else {
 			printer.print("function(").print(JavascriptKeywords.CONSTRUCTOR).print(", ").print(JavascriptKeywords.PROTOTYPE).print("){");
 			printer.indent();
 			for (BodyDeclaration member : nonConstructors) {
 				printer.printLn();
 				member.accept(this, context);
 			}
 			printer.printLn();
 			printer.unindent();
 			printer.print("}");
 		}
 	}
 
 	private void printGlobals(List<BodyDeclaration> globals, GenerationContext context) {
 		for (BodyDeclaration global : globals) {
 			global.accept(this, context);
 			printer.printLn();
 		}
 	}
 
 	private ConstructorDeclaration getConstructor(List<BodyDeclaration> members, GenerationContext context) {
 		ConstructorDeclaration constr = null;
 		for (BodyDeclaration member : members) {
 			if (member instanceof ConstructorDeclaration) {
 				MethodWrapper constructorWrapper = ASTNodeData.resolvedMethod(member);
 				if (constructorWrapper.getAnnotationDirectly(Native.class) != null) {
 					continue;
 				}
 				if (constr == null) {
 					constr = (ConstructorDeclaration) member;
 				} else {
 					throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(member),
 							"Only maximum one constructor is allowed");
 				}
 			}
 		}
 		return constr;
 	}
 
 	private void printStaticMembersPrefix(ClassScope scope) {
 		printer.print(names.getTypeName(scope.getClazz()));
 	}
 
 	private void printMainMethodCall(ClassOrInterfaceDeclaration n, ClassWrapper clazz) {
 		if (n.getMembers() == null) {
 			return;
 		}
 		ClassScope scope = (ClassScope) scope(n);
 		List<BodyDeclaration> members = n.getMembers();
 		for (BodyDeclaration member : members) {
 			if (member instanceof MethodDeclaration) {
 				MethodDeclaration methodDeclaration = (MethodDeclaration) member;
 				if (NodeUtils.isMainMethod(methodDeclaration)) {
 					printer.printLn();
 					printer.print("if (!stjs.mainCallDisabled) ");
 					if (!isGlobal(clazz)) {
 						printStaticMembersPrefix(scope);
 						printer.print(".");
 					}
 					printer.print("main();");
 				}
 			}
 		}
 	}
 
 	@Override
 	public void visit(EmptyTypeDeclaration n, GenerationContext context) {
 		printJavadoc(n.getJavaDoc(), context);
 		printer.print(";");
 	}
 
 	@Override
 	public void visit(EnumConstantDeclaration n, GenerationContext context) {
 		// the enum constants are processed within the EnumDeclaration node. So this node should not be visited
 		throw new IllegalStateException("Unexpected visit in a EnumConstantDeclaration node:" + n);
 
 	}
 
 	@Override
 	public void visit(AnnotationDeclaration n, GenerationContext context) {
 		// skip
 
 	}
 
 	@Override
 	public void visit(AnnotationMemberDeclaration n, GenerationContext context) {
 		// skip
 
 	}
 
 	@Override
 	public void visit(EmptyMemberDeclaration n, GenerationContext context) {
 		printer.print(";");
 	}
 
 	@Override
 	public void visit(InitializerDeclaration n, GenerationContext context) {
 		if (!n.isStatic()) {
 			// should find a way to implement these blocks. For the moment forbid them
 			throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(n),
 					"Initializing blocks are not supported by Javascript");
 		}
 		// the static initializers are treated inside the class declaration to be able to execute them at the end of
 		// the definition of the type
 	}
 
 	@Override
 	public void visit(JavadocComment n, GenerationContext context) {
 		printer.print("/**");
 		printer.print(n.getContent());
 		printer.printLn("*/");
 	}
 
 	@Override
 	public void visit(PrimitiveType n, GenerationContext context) {
 		throw new IllegalStateException("Unexpected visit in a PrimitiveType node:" + n);
 
 	}
 
 	@Override
 	public void visit(VoidType n, GenerationContext context) {
 		throw new IllegalStateException("Unexpected visit in a VoidType node:" + n);
 	}
 
 	@Override
 	public void visit(WildcardType n, GenerationContext context) {
 		throw new IllegalStateException("Unexpected visit in a WildcardType node:" + n);
 	}
 
 	@Override
 	public void visit(ArrayAccessExpr n, GenerationContext context) {
 		n.getName().accept(this, context);
 		printer.print("[");
 		n.getIndex().accept(this, context);
 		printer.print("]");
 	}
 
 	@Override
 	public void visit(ArrayCreationExpr n, GenerationContext context) {
 		// skip the new type[][]
 		n.getInitializer().accept(this, context);
 	}
 
 	@Override
 	public void visit(ArrayInitializerExpr n, GenerationContext context) {
 		printer.print("[");
 		if (n.getValues() != null) {
 			for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
 				Expression expr = i.next();
 				expr.accept(this, context);
 				if (i.hasNext()) {
 					printer.print(", ");
 				}
 			}
 
 		}
 		printer.print("]");
 	}
 
 	/**
 	 * @param n
 	 * @return true if the node is a direct child following the path:
 	 *         //ObjectCreationExpr/InitializerDeclaration/BlockStmt/Child
 	 */
 	private boolean isInlineObjectCreationChild(Node n, int upLevel) {
 		return isInlineObjectCreationBlock(parent(n, upLevel));
 
 	}
 
 	/**
 	 * @param n
 	 * @return true if the node is a block statement //ObjectCreationExpr/InitializerDeclaration/BlockStmt
 	 */
 	private boolean isInlineObjectCreationBlock(Node n) {
 		if (!(n instanceof BlockStmt)) {
 			return false;
 		}
 		Node p = checkParent(n, InitializerDeclaration.class);
 		if (p == null) {
 			return false;
 		}
 		p = checkParent(p, ObjectCreationExpr.class);
 		if (p == null) {
 			return false;
 		}
 		return true;
 	}
 
 	private void printSpecialMapAssign(AssignExpr n, GenerationContext context) {
 		if (n.getTarget() instanceof FieldAccessExpr) {
 			// in inline object creation "this." should be removed
 			printer.print(((FieldAccessExpr) n.getTarget()).getField());
 		} else {
 			n.getTarget().accept(this, context);
 		}
 		printer.print(" ");
 		if (n.getOperator() == AssignExpr.Operator.assign) {
 			printer.print(":");
 		} else {
 			throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(n),
 					"Cannot have this assign operator inside an inline object creation block");
 		}
 		printer.print(" ");
 		n.getValue().accept(this, context);
 	}
 
 	@Override
 	public void visit(AssignExpr n, GenerationContext context) {
 		if (isInlineObjectCreationChild(n, 2)) {
 			printSpecialMapAssign(n, context);
 			return;
 		}
 
 		TypeWrapper leftType = ASTNodeData.resolvedType(n.getTarget());
 		TypeWrapper rightType = ASTNodeData.resolvedType(n.getValue());
 		boolean integerDivision = n.getOperator() == AssignExpr.Operator.slash && ClassUtils.isIntegerType(leftType)
 				&& ClassUtils.isIntegerType(rightType);
 
 		if (integerDivision) {
 			n.getTarget().accept(this, context);
 			printer.print(" = ");
 			printer.print("stjs.trunc(");
 			n.getTarget().accept(this, context);
 			printer.print(BinaryExpr.Operator.divide);
 			printer.print("(");
 			n.getValue().accept(this, context);
 			printer.print("))");
 		} else {
 			n.getTarget().accept(this, context);
 			printer.print(" ");
 			printer.print(n.getOperator());
 			printer.print(" ");
 			n.getValue().accept(this, context);
 		}
 	}
 
 	@Override
 	public void visit(BinaryExpr n, GenerationContext context) {
 		TypeWrapper leftType = ASTNodeData.resolvedType(n.getLeft());
 		TypeWrapper rightType = ASTNodeData.resolvedType(n.getRight());
 		boolean integerDivision = n.getOperator() == Operator.divide && ClassUtils.isIntegerType(leftType)
 				&& ClassUtils.isIntegerType(rightType);
 
 		if (integerDivision) {
 			printer.print("stjs.trunc(");
 		}
 		n.getLeft().accept(this, context);
 		printer.print(" ");
 		printer.print(n.getOperator());
 		printer.print(" ");
 		n.getRight().accept(this, context);
 		if (integerDivision) {
 			printer.print(")");
 		}
 	}
 
 	@Override
 	public void visit(ClassExpr n, GenerationContext context) {
 		String typeName = names.getTypeName(resolvedType(n.getType()));
 		printer.print(typeName);
 		// printer.print(".prototype");
 	}
 
 	@Override
 	public void visit(ConditionalExpr n, GenerationContext context) {
 		n.getCondition().accept(this, context);
 		printer.print(" ? ");
 		n.getThenExpr().accept(this, context);
 		printer.print(" : ");
 		n.getElseExpr().accept(this, context);
 	}
 
 	@Override
 	public void visit(EnclosedExpr n, GenerationContext context) {
 		printer.print("(");
 		n.getInner().accept(this, context);
 		printer.print(")");
 	}
 
 	@Override
 	public void visit(InstanceOfExpr n, GenerationContext context) {
 		printer.print("stjs.isInstanceOf(");
 		n.getExpr().accept(this, context);
 		printer.print(".constructor,");
 		TypeWrapper type = scope(n).resolveType(((ReferenceType) n.getType()).getType().toString()).getType();
 		printer.print(names.getTypeName(type));
 		printer.print(")");
 	}
 
 	@Override
 	public void visit(IntegerLiteralMinValueExpr n, GenerationContext context) {
 		printer.print(n.getValue());
 	}
 
 	@Override
 	public void visit(LongLiteralMinValueExpr n, GenerationContext context) {
 		printer.print(n.getValue());
 	}
 
 	@Override
 	public void visit(NullLiteralExpr n, GenerationContext context) {
 		printer.print(JavascriptKeywords.NULL);
 	}
 
 	@Override
 	public void visit(FieldAccessExpr n, GenerationContext context) {
 		boolean withScopeSuper = n.getScope() != null && n.getScope().toString().equals(GeneratorConstants.SUPER);
 
 		TypeWrapper scopeType = resolvedType(n.getScope());
 		FieldWrapper field = (FieldWrapper) resolvedVariable(n);
 		boolean skipType = field != null && Modifier.isStatic(field.getModifiers()) && isGlobal(scopeType);
 
 		if (scopeType == null || !skipType) {
 			if (withScopeSuper) {
 				// super.field does not make sense, so convert it to this
 				printer.print("this");
 			} else {
 				n.getScope().accept(this, context);
 			}
 			printer.print(".");
 		}
 		printer.print(n.getField());
 	}
 
 	@Override
 	public void visit(final MethodCallExpr n, final GenerationContext context) {
 		if (specialMethodHandlers.handleMethodCall(this, n, context)) {
 			// already handled by a special handler
 			return;
 		}
 		MethodWrapper method = resolvedMethod(n);
 		TypeWrapper methodDeclaringClass = method.getOwnerType();
 		if (Modifier.isStatic(method.getModifiers())) {
 			printStaticFieldOrMethodAccessPrefix(methodDeclaringClass, true);
 			printer.print(names.getMethodName(method));
 			printArguments(n.getArgs(), context);
 			return;
 		}
 		// this scope is either implicit (no scope at all) or explicit "this."
 		boolean withScopeThis = n.getScope() == null || n.getScope() != null && n.getScope().toString().equals(GeneratorConstants.THIS);
 		boolean withScopeSuper = n.getScope() != null && n.getScope().toString().equals(GeneratorConstants.SUPER);
 		boolean withOtherScope = !withScopeSuper && !withScopeThis;
 
 		if (withOtherScope) {
 			n.getScope().accept(this, context);
 			printer.print(".");
 		} else if (withScopeThis) {
 			// Non static reference to current enclosing type.
 			printer.print(JavascriptKeywords.THIS).print(".");
 		} else {
 			// Non static reference to parent type
 			printer.print(names.getTypeName(method.getOwnerType()));
 			printer.print(".").print(JavascriptKeywords.PROTOTYPE).print(".").print(names.getMethodName(method)).print(".call");
 			printArguments(Collections.singleton(JavascriptKeywords.THIS), n.getArgs(), Collections.<String>emptyList(), context);
 			return;
 		}
 		printer.print(names.getMethodName(method));
 		printArguments(n.getArgs(), context);
 
 	}
 
 	private void printStaticFieldOrMethodAccessPrefix(TypeWrapper type, boolean addDot) {
 		if (!isGlobal(type)) {
 			printer.print(names.getTypeName(type));
 			if (addDot) {
 				printer.print(".");
 			}
 		}
 	}
 
 	private boolean isGlobal(TypeWrapper clazz) {
 		return clazz.hasAnnotation(GlobalScope.class);
 	}
 
 	private boolean isStaticField(BodyDeclaration decl) {
 		return decl instanceof FieldDeclaration && isStatic(((FieldDeclaration) decl).getModifiers());
 	}
 
 	private boolean isStaticMethod(BodyDeclaration decl) {
 		return decl instanceof MethodDeclaration && isStatic(((MethodDeclaration) decl).getModifiers());
 	}
 
 	private boolean isAbstractInstanceMethod(BodyDeclaration decl) {
 		return decl instanceof MethodDeclaration && !isStatic(((MethodDeclaration) decl).getModifiers())
 				&& isAbstract(((MethodDeclaration) decl).getModifiers());
 	}
 
 	private boolean isConstructor(BodyDeclaration decl) {
 		return decl instanceof ConstructorDeclaration;
 	}
 
 	private boolean isClassOrInterface(BodyDeclaration decl) {
 		return decl instanceof ClassOrInterfaceDeclaration;
 	}
 
 	private boolean isEnum(BodyDeclaration decl) {
 		return decl instanceof EnumDeclaration;
 	}
 
 	private void visitField(FieldWrapper field, NameExpr n) {
 		if (Modifier.isStatic(field.getModifiers())) {
 			printStaticFieldOrMethodAccessPrefix(field.getOwnerType(), true);
 		} else if (!isInlineObjectCreationChild(n, INLINE_CREATION_PARENT_LEVEL)) {
 			printer.print("this.");
 		}
 	}
 
 	@Override
 	public void visit(NameExpr n, GenerationContext context) {
 		if (GeneratorConstants.SPECIAL_THIS.equals(n.getName())) {
 			printer.print(GeneratorConstants.THIS);
 			return;
 		}
 		Variable var = resolvedVariable(n);
 		if (var == null) {
 			if (!(parent(n) instanceof SwitchEntryStmt)) {
 				TypeWrapper type = resolvedType(n);
 				if (type != null) {
 					printStaticFieldOrMethodAccessPrefix(type, false);
 					return;
 				}
 			}
 		} else {
 			if (var instanceof FieldWrapper) {
 				visitField((FieldWrapper) var, n);
 			}
 		}
 
 		printer.print(n.getName());
 	}
 
 	@Override
 	public void visit(QualifiedNameExpr n, GenerationContext context) {
 		n.getQualifier().accept(this, context);
 		printer.print(".");
 		printer.print(n.getName());
 	}
 
 	@Override
 	public void visit(ThisExpr n, GenerationContext context) {
 		if (n.getClassExpr() != null) {
 			n.getClassExpr().accept(this, context);
 			printer.print(".");
 		}
 		printer.print("this");
 	}
 
 	@Override
 	public void visit(SuperExpr n, GenerationContext context) {
 		throw new IllegalStateException("The [super] node should've been already handled:" + n);
 	}
 
 	@Override
 	@SuppressWarnings("PMD.CyclomaticComplexity")
 	public void visit(UnaryExpr n, GenerationContext context) {
 		switch (n.getOperator()) {
 		case positive:
 			printer.print("+");
 			break;
 		case negative:
 			printer.print("-");
 			break;
 		case inverse:
 			printer.print("~");
 			break;
 		case not:
 			printer.print("!");
 			break;
 		case preIncrement:
 			printer.print("++");
 			break;
 		case preDecrement:
 			printer.print("--");
 			break;
 		default:
 			break;
 		}
 
 		n.getExpr().accept(this, context);
 
 		switch (n.getOperator()) {
 		case posIncrement:
 			printer.print("++");
 			break;
 		case posDecrement:
 			printer.print("--");
 			break;
 		default:
 			break;
 		}
 	}
 
 	@Override
 	public void visit(SingleMemberAnnotationExpr n, GenerationContext context) {
 		// skip
 
 	}
 
 	@Override
 	public void visit(NormalAnnotationExpr n, GenerationContext context) {
 		// skip
 
 	}
 
 	@Override
 	public void visit(MemberValuePair n, GenerationContext context) {
 		// skip (annotations)
 	}
 
 	@Override
 	public void visit(ExplicitConstructorInvocationStmt n, GenerationContext context) {
 		if (n.isThis()) {
 			// This should not happen as another constructor is forbidden
 			throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(n), "Only one constructor is allowed");
 		}
 
 		ClassScope classScope = scope(n).closest(ClassScope.class);
 		addCallToSuper(classScope, context, n.getArgs(), false);
 	}
 
 	@Override
 	public void visit(TypeDeclarationStmt n, GenerationContext context) {
 		n.getTypeDeclaration().accept(this, context);
 	}
 
 	@Override
 	public void visit(AssertStmt n, GenerationContext context) {
 		throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(n),
 				"Assert statement is not supported by Javascript");
 	}
 
 	private void checkAssignStatement(Statement s, GenerationContext context) {
 		if (s instanceof ExpressionStmt && ((ExpressionStmt) s).getExpression() instanceof AssignExpr) {
 			return;
 		}
 		throw new JavascriptFileGenerationException(context.getInputFile(), new SourcePosition(s),
 				"Only assign expression are allowed in an object creation block");
 	}
 
 	private void generateArrayHasOwnProperty(ForeachStmt n, GenerationContext context) {
 		if (!context.getConfiguration().isGenerateArrayHasOwnProperty()) {
 			return;
 		}
 
 		TypeWrapper iterated = resolvedType(n.getIterable());
 
 		if (!iterated.isAssignableFrom(TypeWrappers.wrap(Array.class))) {
 			return;
 		}
 		printer.print("if (!(");
 		n.getIterable().accept(this, context);
 		printer.print(").hasOwnProperty(");
 		printer.print(n.getVariable().getVars().get(0).getId().getName());
 		printer.printLn(")) continue;");
 	}
 
 	@Override
 	public void visit(BlockStmt n, GenerationContext context) {
 		printer.printLn("{");
 		if (n.getStmts() != null) {
 			printer.indent();
 			if (parent(n) instanceof ForeachStmt) {
 				generateArrayHasOwnProperty((ForeachStmt) parent(n), context);
 			}
 			for (int i = 0; i < n.getStmts().size(); ++i) {
 				Statement s = n.getStmts().get(i);
 				printComments(s, context);
 				if (isInlineObjectCreationChild(s, 1)) {
 					checkAssignStatement(s, context);
 					s.accept(this, context);
 					if (i < n.getStmts().size() - 1) {
 						printer.print(",");
 					}
 				} else {
 					s.accept(this, context);
 				}
 				printer.printLn();
 			}
 			printer.unindent();
 		}
 		printer.print("}");
 
 	}
 
 	@Override
 	public void visit(LabeledStmt n, GenerationContext context) {
 		printer.print(n.getLabel());
 		printer.print(": ");
 		n.getStmt().accept(this, context);
 	}
 
 	@Override
 	public void visit(EmptyStmt n, GenerationContext context) {
 		printer.print(";");
 	}
 
 	@Override
 	public void visit(ExpressionStmt n, GenerationContext context) {
 		// the expression can be very long. have the marker on the start of the expression only
 		printer.setSourceNode(n);
 		n.getExpression().accept(this, context);
 		if (!isInlineObjectCreationChild(n, 1)) {
 			printer.print(";");
 		}
 		printer.addSouceMapping(context);
 	}
 
 	@Override
 	public void visit(SwitchStmt n, GenerationContext context) {
 		printer.print("switch(");
 		n.getSelector().accept(this, context);
 		printer.printLn(") {");
 		if (n.getEntries() != null) {
 			printer.indent();
 			for (SwitchEntryStmt e : n.getEntries()) {
 				e.accept(this, context);
 			}
 			printer.unindent();
 		}
 		printer.print("}");
 
 	}
 
 	@Override
 	public void visit(SwitchEntryStmt n, GenerationContext context) {
 		if (n.getLabel() == null) {
 			printer.print("default:");
 		} else {
 			printer.print("case ");
 			TypeWrapper selectorType = resolvedType(((SwitchStmt) parent(n)).getSelector());
 			PreConditions.checkState(selectorType != null, "The selector of the switch %s should have a type", parent(n));
 			if (selectorType instanceof ClassWrapper && ((ClassWrapper) selectorType).getClazz().isEnum()) {
 				printer.print(names.getTypeName(selectorType));
 				printer.print(".");
 			}
 			n.getLabel().accept(this, context);
 			printer.print(":");
 		}
 		printer.printLn();
 		printer.indent();
 		if (n.getStmts() != null) {
 			for (Statement s : n.getStmts()) {
 				s.accept(this, context);
 				printer.printLn();
 			}
 		}
 		printer.unindent();
 	}
 
 	@Override
 	public void visit(BreakStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("break");
 		if (n.getId() != null) {
 			printer.print(" ");
 			printer.print(n.getId());
 		}
 		printer.print(";");
 		printer.addSouceMapping(context);
 	}
 
 	@Override
 	public void visit(ReturnStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("return");
 		if (n.getExpr() != null) {
 			printer.print(" ");
 			n.getExpr().accept(this, context);
 		}
 		printer.print(";");
 		printer.addSouceMapping(context);
 	}
 
 	@Override
 	public void visit(ThrowStmt n, GenerationContext context) {
 		printer.setSourceNode(n);
 		printer.print("throw ");
 		n.getExpr().accept(this, context);
 		printer.print(";");
 		printer.addSouceMapping(context);
 	}
 
 	@Override
 	public void visit(TryStmt n, GenerationContext context) {
 		printer.print("try ");
 		n.getTryBlock().accept(this, context);
 		if (n.getCatchs() != null) {
 			for (CatchClause c : n.getCatchs()) {
 				c.accept(this, context);
 			}
 		}
 		if (n.getFinallyBlock() != null) {
 			printer.print(" finally ");
 			n.getFinallyBlock().accept(this, context);
 		}
 	}
 
 	@Override
 	public void visit(CatchClause n, GenerationContext context) {
 		printer.print(" catch (");
 		n.getExcept().accept(this, context);
 		printer.print(") ");
 		n.getCatchBlock().accept(this, context);
 	}
 
 	public void writeSourceMap(GenerationContext context, Writer sourceMapWriter) throws IOException {
 		printer.writeSourceMap(context, sourceMapWriter);
 
 	}
 
 	public JavascriptWriter getPrinter() {
 		return printer;
 	}
 }
