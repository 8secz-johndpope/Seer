 /* *******************************************************************
  * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
  * All rights reserved. 
  * This program and the accompanying materials are made available 
  * under the terms of the Common Public License v1.0 
  * which accompanies this distribution and is available at 
  * http://www.eclipse.org/legal/cpl-v10.html 
  *  
  * Contributors: 
  *     PARC     initial implementation 
  * ******************************************************************/
 
 
 package org.aspectj.ajdt.internal.core.builder;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.Stack;
 
 import org.aspectj.ajdt.internal.compiler.ast.AdviceDeclaration;
 import org.aspectj.ajdt.internal.compiler.ast.AspectDeclaration;
 import org.aspectj.ajdt.internal.compiler.ast.DeclareDeclaration;
 import org.aspectj.ajdt.internal.compiler.ast.InterTypeDeclaration;
 import org.aspectj.ajdt.internal.compiler.ast.PointcutDeclaration;
 import org.aspectj.ajdt.internal.compiler.lookup.EclipseWorld;
 import org.aspectj.asm.ProgramElementNode;
 import org.aspectj.asm.StructureModel;
 import org.aspectj.asm.StructureModelManager;
 import org.aspectj.asm.StructureNode;
 import org.aspectj.bridge.ISourceLocation;
 import org.aspectj.bridge.SourceLocation;
 import org.aspectj.weaver.Member;
 import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
 import org.eclipse.jdt.internal.compiler.CompilationResult;
 import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.AstNode;
 import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.ImportReference;
 import org.eclipse.jdt.internal.compiler.ast.Initializer;
 import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.MemberTypeDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
 import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
 import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
 import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
 import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
 import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
 import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
 
 public class AsmBuilder extends AbstractSyntaxTreeVisitorAdapter {
 	
 	private Stack stack  = new Stack();
 	private CompilationResult currCompilationResult = null;
 	
 	public static void build(
 		CompilationUnitDeclaration unit,
 		StructureModel structureModel) {
 		new AsmBuilder().internalBuild(unit, structureModel);
 	}
 	
 	private void internalBuild(CompilationUnitDeclaration unit, StructureModel structureModel) {
 		currCompilationResult = unit.compilationResult();
 		File file = new File(new String(unit.getFileName()));
 		// AMC - use the source start and end from the compilation unit decl
 		//ISourceLocation sourceLocation = new SourceLocation(file, 1);
 		int startLine = getStartLine(unit);
 		int endLine = getEndLine(unit);		
 		ISourceLocation sourceLocation = new SourceLocation(file, 
 			startLine, endLine);		
 
 		ProgramElementNode cuNode = new ProgramElementNode(
 			new String(file.getName()),
 			ProgramElementNode.Kind.FILE_JAVA,
 			sourceLocation,
 			0,
 			"",
 			new ArrayList());
 
 		ImportReference currentPackage = unit.currentPackage;
 		if (currentPackage != null) {
 			StringBuffer nameBuffer = new StringBuffer();
 			for (int i = 0; i < currentPackage.getImportName().length; i++) {
 				nameBuffer.append(new String(currentPackage.getImportName()[i]));
 				if (i < currentPackage.getImportName().length-1) nameBuffer.append('.');
 			}
 			String pkgName = nameBuffer.toString();
 			
 			boolean found = false;
 			ProgramElementNode pkgNode = null;
 			for (Iterator it = StructureModelManager.INSTANCE.getStructureModel().getRoot().getChildren().iterator(); it.hasNext(); ) {
 				ProgramElementNode currNode = (ProgramElementNode)it.next();
 				if (currNode.getName().equals(pkgName)) pkgNode = currNode;
 			}
 			if (pkgNode == null) {
 				pkgNode = new ProgramElementNode(
 					pkgName, 
 					ProgramElementNode.Kind.PACKAGE, 
 					new ArrayList());
 				StructureModelManager.INSTANCE.getStructureModel().getRoot().addChild(pkgNode);
 			}	
 			pkgNode.addChild(cuNode);
 		} else {
 			StructureModelManager.INSTANCE.getStructureModel().getRoot().addChild(cuNode);
 		}
 		
 		stack.push(cuNode);
 		unit.traverse(this, unit.scope);  
 		
 		try {
 	        //StructureModelManager.INSTANCE.getStructureModel().getFileMap().put(
 			StructureModelManager.INSTANCE.getStructureModel().addToFileMap(
 	        	file.getCanonicalPath(),//.replace('\\', '/'),
 	        	cuNode
 	        );
 		} catch (IOException ioe) { }
 //		if (currImports != null) peNode.addChild(0, currImports);
 //		currImports = null;
 	}
 
 	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
 		String name = new String(typeDeclaration.name);
 		//System.err.println("type with name: " + name);
 		ProgramElementNode.Kind kind = ProgramElementNode.Kind.CLASS;
 		if (typeDeclaration instanceof AspectDeclaration) kind = ProgramElementNode.Kind.ASPECT;
 		else if (typeDeclaration.isInterface()) kind = ProgramElementNode.Kind.INTERFACE;
 
 		ProgramElementNode peNode = new ProgramElementNode(
 			name,
 			kind,
 			makeLocation(typeDeclaration),
 			typeDeclaration.modifiers,
 			"",
 			new ArrayList());
 		
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		return true;
 	}
 	public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
 		stack.pop();
 	}
 	
 	// ??? share impl with visit(TypeDeclaration, ..) ?
 	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
 		String name = new String(memberTypeDeclaration.name);
 		//System.err.println("member type with name: " + name);
 		
 		ProgramElementNode.Kind kind = ProgramElementNode.Kind.CLASS;
 		if (memberTypeDeclaration instanceof AspectDeclaration) kind = ProgramElementNode.Kind.ASPECT;
 		else if (memberTypeDeclaration.isInterface()) kind = ProgramElementNode.Kind.INTERFACE;
 
 		ProgramElementNode peNode = new ProgramElementNode(
 			name,
 			kind,
 			makeLocation(memberTypeDeclaration),
 			memberTypeDeclaration.modifiers,
 			"",
 			new ArrayList());
 		
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		return true;
 	}
 	public void endVisit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
 		stack.pop();
 	}
 	
 	public boolean visit(LocalTypeDeclaration memberTypeDeclaration, BlockScope scope) {
 		String name = new String(memberTypeDeclaration.name);
 		String fullName = new String(memberTypeDeclaration.binding.constantPoolName());
 		int dollar = fullName.indexOf('$');
 		fullName = fullName.substring(dollar+1);
 //		
 //		System.err.println("member type with name: " + name + ", " + 
 //				new String(fullName));
 		
 		ProgramElementNode.Kind kind = ProgramElementNode.Kind.CLASS;
 		if (memberTypeDeclaration.isInterface()) kind = ProgramElementNode.Kind.INTERFACE;
 
 		ProgramElementNode peNode = new ProgramElementNode(
 			fullName,
 			kind,
 			makeLocation(memberTypeDeclaration),
 			memberTypeDeclaration.modifiers,
 			"",
 			new ArrayList());
 		
 		//??? we add this to the compilation unit
 		findEnclosingClass(stack).addChild(peNode);
 		stack.push(peNode);
 		return true;
 	}
 	public void endVisit(LocalTypeDeclaration memberTypeDeclaration, BlockScope scope) {
 		stack.pop();
 	}
 	
 	public boolean visit(AnonymousLocalTypeDeclaration memberTypeDeclaration, BlockScope scope) {
 		return visit((LocalTypeDeclaration)memberTypeDeclaration, scope);
 	}
 
 	public void endVisit(AnonymousLocalTypeDeclaration memberTypeDeclaration, BlockScope scope) {
 		stack.pop();
 	}
 	
 	private StructureNode findEnclosingClass(Stack stack) {
 		for (int i = stack.size()-1; i >= 0; i--) {
 			ProgramElementNode pe = (ProgramElementNode)stack.get(i);
 			if (pe.getProgramElementKind() == ProgramElementNode.Kind.CLASS) {
 				return pe;
 			}
 			
 		}
 		return (StructureNode)stack.peek();
 	}	
 	
 	// !!! improve name and type generation
 	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
 		ProgramElementNode.Kind kind = ProgramElementNode.Kind.METHOD;
 		String label = new String(methodDeclaration.selector);
 		
 		if (methodDeclaration instanceof AdviceDeclaration) { 
 			kind = ProgramElementNode.Kind.ADVICE;
 			label = translateAdviceName(label);
 		} else if (methodDeclaration instanceof PointcutDeclaration) { 
 			kind = ProgramElementNode.Kind.POINTCUT;
 			label = translatePointcutName(label);
 		} else if (methodDeclaration instanceof DeclareDeclaration) { 
 			DeclareDeclaration declare = (DeclareDeclaration)methodDeclaration;
 			label = translateDeclareName(declare.toString());
 			if (label.indexOf("warning") != -1) kind = ProgramElementNode.Kind.DECLARE_WARNING;
 			if (label.indexOf("error") != -1) kind = ProgramElementNode.Kind.DECLARE_ERROR;
 		} else if (methodDeclaration instanceof InterTypeDeclaration) {
 			kind = ProgramElementNode.Kind.INTRODUCTION;
 			label = translateInterTypeDecName(new String(((InterTypeDeclaration)methodDeclaration).selector));
 		} 
 		
 		ProgramElementNode peNode = new ProgramElementNode(
 			label,
 			kind,
 			makeLocation(methodDeclaration),
 			methodDeclaration.modifiers,
 			"",
 			new ArrayList());
 
 		if (kind == ProgramElementNode.Kind.METHOD) {
			// !! should probably discriminate more
 			if (label.equals("main")) {
				((ProgramElementNode)stack.peek()).setRunnable(true);
 			}	
 		}
 
 		if (methodDeclaration.binding != null) {
 			Member member = EclipseWorld.makeResolvedMember(methodDeclaration.binding);
 			peNode.setBytecodeName(member.getName());
 			peNode.setBytecodeSignature(member.getSignature());
 		}
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		
 		return true;
 	}
 
 	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
 		stack.pop();
 	}
 
 	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {		
 		ProgramElementNode peNode = new ProgramElementNode(
 			new String(fieldDeclaration.name),
 			ProgramElementNode.Kind.FIELD,	
 			makeLocation(fieldDeclaration),
 			fieldDeclaration.modifiers,
 			"",
 			new ArrayList());	
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		return true;		
 	}
 	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
 		stack.pop();
 	}
 
 
 //	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
 //		ProgramElementNode peNode = new ProgramElementNode(
 //			new String(importRef.toString()),
 //			ProgramElementNode.Kind.,	
 //			makeLocation(importRef),
 //			0,
 //			"",
 //			new ArrayList());	
 //		((StructureNode)stack.peek()).addChild(0, peNode);
 //		stack.push(peNode);
 //		return true;	
 //	}
 //	public void endVisit(ImportReference importRef,CompilationUnitScope scope) {
 //		stack.pop();		
 //	}
 
 	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
 		if (constructorDeclaration.isDefaultConstructor) {
 			stack.push(null); // a little wierd but does the job
 			return true;	
 		}
 		ProgramElementNode peNode = new ProgramElementNode(
 			new String(constructorDeclaration.selector),
 			ProgramElementNode.Kind.CONSTRUCTOR,	
 			makeLocation(constructorDeclaration),
 			constructorDeclaration.modifiers,
 			"",
 			new ArrayList());	
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		return true;	
 	}
 	public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
 		stack.pop();
 	}
 
 //	public boolean visit(Clinit clinit, ClassScope scope) {
 //		ProgramElementNode peNode = new ProgramElementNode(
 //			"<clinit>",
 //			ProgramElementNode.Kind.INITIALIZER,	
 //			makeLocation(clinit),
 //			clinit.modifiers,
 //			"",
 //			new ArrayList());	
 //		((StructureNode)stack.peek()).addChild(peNode);
 //		stack.push(peNode);  
 //		return false;	
 //	}
 //	public void endVisit(Clinit clinit, ClassScope scope) {
 //		stack.pop();
 //	}
 
 	/** This method works-around an odd traverse implementation on Initializer
 	 */
 	private Initializer inInitializer = null;
 	public boolean visit(Initializer initializer, MethodScope scope) {
 		if (initializer == inInitializer) return false;
 		inInitializer = initializer;
 		
 		ProgramElementNode peNode = new ProgramElementNode(
 			"...",
 			ProgramElementNode.Kind.INITIALIZER,	
 			makeLocation(initializer),
 			initializer.modifiers,
 			"",
 			new ArrayList());	
 		((StructureNode)stack.peek()).addChild(peNode);
 		stack.push(peNode);
 		initializer.block.traverse(this, scope);
 		stack.pop();
 		return false;	
 	}
 
 	// ??? handle non-existant files
 	private ISourceLocation makeLocation(AstNode node) {
 		String fileName = new String(currCompilationResult.getFileName());
 		// AMC - different strategies based on node kind
 		int startLine = getStartLine(node);
 		int endLine = getEndLine(node);
 		ISourceLocation loc = null;
 		if ( startLine <= endLine ) {
 			// found a valid end line for this node...
 			loc = new SourceLocation(new File(fileName), startLine, endLine);			
 		} else {
 			loc = new SourceLocation(new File(fileName), startLine);
 		}
 		return loc;
 	}
 
 
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getStartLine( AstNode n){
 //		if (  n instanceof AbstractVariableDeclaration ) return getStartLine( (AbstractVariableDeclaration)n);
 //		if (  n instanceof AbstractMethodDeclaration ) return getStartLine( (AbstractMethodDeclaration)n);
 //		if (  n instanceof TypeDeclaration ) return getStartLine( (TypeDeclaration)n);
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			n.sourceStart);		
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getEndLine( AstNode n){
 		if (  n instanceof AbstractVariableDeclaration ) return getEndLine( (AbstractVariableDeclaration)n);
 		if (  n instanceof AbstractMethodDeclaration ) return getEndLine( (AbstractMethodDeclaration)n);
 		if (  n instanceof TypeDeclaration ) return getEndLine( (TypeDeclaration)n);	
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			n.sourceEnd);
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getStartLine( AbstractVariableDeclaration avd ) {
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			avd.declarationSourceStart);
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getEndLine( AbstractVariableDeclaration avd ){
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			avd.declarationSourceEnd);		
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getStartLine( AbstractMethodDeclaration amd ){
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			amd.declarationSourceStart);
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getEndLine( AbstractMethodDeclaration amd) {
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			amd.declarationSourceEnd);
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getStartLine( TypeDeclaration td ){
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			td.declarationSourceStart);
 	}
 	
 	// AMC - overloaded set of methods to get start and end lines for
 	// various ASTNode types. They have no common ancestor in the
 	// hierarchy!!
 	private int getEndLine( TypeDeclaration td){
 		return ProblemHandler.searchLineNumber(
 			currCompilationResult.lineSeparatorPositions,
 			td.declarationSourceEnd);
 	}
 	
 
 	// !!! move or replace
 	private String translateAdviceName(String label) {
 		if (label.indexOf("before") != -1) return "before";
 		if (label.indexOf("returning") != -1) return "after returning";
 		if (label.indexOf("after") != -1) return "after";
 		if (label.indexOf("around") != -1) return "around";
 		else return "<advice>";
 	}
 	
 	// !!! move or replace
 	private String translateDeclareName(String name) {
 		int colonIndex = name.indexOf(":");
 		if (colonIndex != -1) {
 			return name.substring(0, colonIndex);
 		} else { 
 			return name;
 		}
 	}
 
 	// !!! move or replace
 	private String translateInterTypeDecName(String name) {
 		int index = name.lastIndexOf('$');
 		if (index != -1) {
 			return name.substring(index+1);
 		} else { 
 			return name;
 		}
 	}
 
 	// !!! move or replace
 	private String translatePointcutName(String name) {
 		int index = name.indexOf("$$")+2;
 		int endIndex = name.lastIndexOf('$');
 		if (index != -1 && endIndex != -1) {
 			return name.substring(index, endIndex);
 		} else { 
 			return name;
 		}
 	}
 
 }
