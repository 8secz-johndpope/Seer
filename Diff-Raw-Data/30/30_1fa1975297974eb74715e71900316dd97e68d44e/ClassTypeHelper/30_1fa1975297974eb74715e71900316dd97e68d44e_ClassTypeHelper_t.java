 /*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *     Markus Schorn (Wind River Systems)
  *     Bryan Wilkinson (QNX)
  *     Sergey Prigogin (Google)
  *     Andrew Ferguson (Symbian)
  *******************************************************************************/
 package org.eclipse.cdt.internal.core.dom.parser.cpp;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 
 import org.eclipse.cdt.core.dom.ast.DOMException;
 import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
 import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
 import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
 import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
 import org.eclipse.cdt.core.dom.ast.IASTName;
 import org.eclipse.cdt.core.dom.ast.IASTNode;
 import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
 import org.eclipse.cdt.core.dom.ast.IBasicType;
 import org.eclipse.cdt.core.dom.ast.IBinding;
 import org.eclipse.cdt.core.dom.ast.IField;
 import org.eclipse.cdt.core.dom.ast.IFunctionType;
 import org.eclipse.cdt.core.dom.ast.IProblemBinding;
 import org.eclipse.cdt.core.dom.ast.IQualifierType;
 import org.eclipse.cdt.core.dom.ast.IScope;
 import org.eclipse.cdt.core.dom.ast.IType;
 import org.eclipse.cdt.core.dom.ast.ITypedef;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDeclaration;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPBinding;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPField;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPReferenceType;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDeclaration;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
 import org.eclipse.cdt.core.index.IIndex;
 import org.eclipse.cdt.core.index.IIndexName;
 import org.eclipse.cdt.core.parser.util.ArrayUtil;
 import org.eclipse.cdt.core.parser.util.CharArrayUtils;
 import org.eclipse.cdt.core.parser.util.ObjectSet;
 import org.eclipse.cdt.internal.core.dom.parser.ASTInternal;
 import org.eclipse.cdt.internal.core.dom.parser.ASTQueries;
 import org.eclipse.cdt.internal.core.dom.parser.ProblemBinding;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPClassType.CPPClassTypeProblem;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPSemantics;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;
 import org.eclipse.core.runtime.CoreException;
 
 /**
  * Holds common implementation of methods for ICPPClassType implementations that have
  * a corresponding textual definition in the source code. 
  * 
  *  @see CPPClassType
  *  @see CPPClassTemplate
  */
 public class ClassTypeHelper {
 	public static IBinding[] getFriends(ICPPInternalClassTypeMixinHost host) {
 		if (host.getDefinition() == null) {
 			host.checkForDefinition();
 			if (host.getDefinition() == null) {
 				try {
 					ICPPClassType backup= getBackupDefinition(host);
 					if (backup != null)
 						return backup.getFriends();
 				} catch (DOMException e) {}
 				IASTNode[] declarations= host.getDeclarations();
 				IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 				return new IBinding[] { new ProblemBinding(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 			}
 		}
 		ObjectSet<IBinding> resultSet = new ObjectSet<IBinding>(2);
 		IASTDeclaration[] members = host.getCompositeTypeSpecifier().getMembers();
 		for (IASTDeclaration decl : members) {
 			while (decl instanceof ICPPASTTemplateDeclaration)
 				decl = ((ICPPASTTemplateDeclaration)decl).getDeclaration();
 
 			if (decl instanceof IASTSimpleDeclaration) {
 				ICPPASTDeclSpecifier declSpec = (ICPPASTDeclSpecifier) ((IASTSimpleDeclaration)decl).getDeclSpecifier();
 				if (declSpec.isFriend()) {
 					IASTDeclarator[] dtors = ((IASTSimpleDeclaration)decl).getDeclarators();
 					if (declSpec instanceof ICPPASTElaboratedTypeSpecifier && dtors.length == 0) {
 						resultSet.put(((ICPPASTElaboratedTypeSpecifier)declSpec).getName().resolveBinding());
 					} else {
 						for (IASTDeclarator dtor : dtors) {
 							if (dtor == null) break;
 							dtor= ASTQueries.findInnermostDeclarator(dtor);
 							resultSet.put(dtor.getName().resolveBinding());
 						}    
 					}
 				}
 			} else if (decl instanceof IASTFunctionDefinition) {
 				ICPPASTDeclSpecifier declSpec = (ICPPASTDeclSpecifier) ((IASTFunctionDefinition)decl).getDeclSpecifier();
 				if (declSpec.isFriend()) {
 					IASTDeclarator dtor = ((IASTFunctionDefinition)decl).getDeclarator();
 					dtor= ASTQueries.findInnermostDeclarator(dtor);
 					resultSet.put(dtor.getName().resolveBinding());
 				}
 			}
 		}
 
 		return resultSet.keyArray(IBinding.class);
 	}
 
 	/**
 	 * A host maybe backed up with a definition from the index.
 	 * @throws DOMException 
 	 */
 	private static ICPPClassType getBackupDefinition(ICPPInternalClassTypeMixinHost host) throws DOMException {
 		ICPPClassScope scope = host.getCompositeScope();
 		if (scope != null) {
 			ICPPClassType b = scope.getClassType();
 			if (!(b instanceof ICPPInternalClassTypeMixinHost))
 				return b;
 		}
 		return null;
 	}
 
 	public static ICPPBase[] getBases(ICPPInternalClassTypeMixinHost host) {
 		if (host.getDefinition() == null) {
 			host.checkForDefinition();
 			if (host.getDefinition() == null) {
 				try {
 					ICPPClassType backup= getBackupDefinition(host);
 					if (backup != null)
 						return backup.getBases();
 				} catch (DOMException e) {}
 				
 				IASTNode[] declarations= host.getDeclarations();
 				IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 				return new ICPPBase[] { new CPPBaseClause.CPPBaseProblem(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 			}
 		}
 		ICPPASTBaseSpecifier[] bases = host.getCompositeTypeSpecifier().getBaseSpecifiers();
 		if (bases.length == 0)
 			return ICPPBase.EMPTY_BASE_ARRAY;
 
 		ICPPBase[] bindings = new ICPPBase[bases.length];
 		for (int i = 0; i < bases.length; i++) {
 			bindings[i] = new CPPBaseClause(bases[i]);
 		}
 
 		return bindings; 
 	}
 
 	public static ICPPField[] getDeclaredFields(ICPPInternalClassTypeMixinHost host) throws DOMException {
 		if (host.getDefinition() == null) {
 			host.checkForDefinition();
 			if (host.getDefinition() == null) {
 				try {
 					ICPPClassType backup= getBackupDefinition(host);
 					if (backup != null)
 						return backup.getDeclaredFields();
 				} catch (DOMException e) {}
 				
 				IASTNode[] declarations= host.getDeclarations();
 				IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 				return new ICPPField[] { new CPPField.CPPFieldProblem(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 			}
 		}
 		IBinding binding = null;
 		ICPPField[] result = null;
 
 		IASTDeclaration[] decls = host.getCompositeTypeSpecifier().getMembers();
 		for (IASTDeclaration decl : decls) {
 			if (decl instanceof IASTSimpleDeclaration) {
 				IASTDeclarator[] dtors = ((IASTSimpleDeclaration)decl).getDeclarators();
 				for (IASTDeclarator dtor : dtors) {
 					binding = ASTQueries.findInnermostDeclarator(dtor).getName().resolveBinding();
 					if (binding instanceof ICPPField)
 						result = (ICPPField[]) ArrayUtil.append(ICPPField.class, result, binding);
 				}
 			} else if (decl instanceof ICPPASTUsingDeclaration) {
 				IASTName n = ((ICPPASTUsingDeclaration)decl).getName();
 				binding = n.resolveBinding();
 				if (binding instanceof ICPPUsingDeclaration) {
 					IBinding[] bs = ((ICPPUsingDeclaration)binding).getDelegates();
 					for (IBinding element : bs) {
 						if (element instanceof ICPPField)
 							result = (ICPPField[]) ArrayUtil.append(ICPPField.class, result, element);
 					}
 				} else if (binding instanceof ICPPField) {
 					result = (ICPPField[]) ArrayUtil.append(ICPPField.class, result, binding);
 				}
 			}
 		}
 		return (ICPPField[]) ArrayUtil.trim(ICPPField.class, result);
 	}
 	
 	public static ICPPClassType[] getAllBases(ICPPClassType ct) throws DOMException {
 		HashSet<ICPPClassType> result= new HashSet<ICPPClassType>();
 		result.add(ct);
 		getAllBases(ct, result);
 		result.remove(ct);
 		return result.toArray(new ICPPClassType[result.size()]);
 	}
 	
 	private static void getAllBases(ICPPClassType ct, HashSet<ICPPClassType> result) throws DOMException {
 		ICPPBase[] bases= ct.getBases();
 		for (ICPPBase base : bases) {
 			IBinding b= base.getBaseClass();
 			if (b instanceof ICPPClassType) {
 				final ICPPClassType ctbase = (ICPPClassType) b;
 				if (result.add(ctbase)) { 
 					getAllBases(ctbase, result);
 				}
 			}
 		}
 	}
 	
 	public static ICPPMethod[] getAllDeclaredMethods(ICPPClassType ct) throws DOMException {
 		ICPPMethod[] methods= ct.getDeclaredMethods();
 		ICPPClassType[] bases= getAllBases(ct);
 		for (ICPPClassType base : bases) {
 			methods = (ICPPMethod[]) ArrayUtil.addAll(ICPPMethod.class, methods, base.getDeclaredMethods());
 		}
 		return (ICPPMethod[]) ArrayUtil.trim(ICPPMethod.class, methods);
 	}
 	
 	public static ICPPMethod[] getMethods(ICPPClassType ct) throws DOMException {
 		ObjectSet<ICPPMethod> set= new ObjectSet<ICPPMethod>(4);
 		set.addAll(ct.getDeclaredMethods());
 		ICPPClassScope scope= (ICPPClassScope) ct.getCompositeScope();
 		set.addAll(scope.getImplicitMethods());
 		
 		ICPPClassType[] bases= getAllBases(ct);
 		for (ICPPClassType base : bases) {
 			set.addAll(base.getDeclaredMethods());
 			final IScope compositeScope = base.getCompositeScope();
 			if (compositeScope instanceof ICPPClassScope) {
 				set.addAll(((ICPPClassScope) compositeScope).getImplicitMethods());
 			}
 		}
 		return set.keyArray(ICPPMethod.class);
 	}
 	
 	public static ICPPMethod[] getDeclaredMethods(ICPPInternalClassTypeMixinHost host) throws DOMException {
 		if (host.getDefinition() == null) {
 			host.checkForDefinition();
 			if (host.getDefinition() == null) {
 				ICPPClassType backup= getBackupDefinition(host);
 				if (backup != null)
 					return backup.getDeclaredMethods();
 
 				IASTNode[] declarations= host.getDeclarations();
 				IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 				return new ICPPMethod[] { new CPPMethod.CPPMethodProblem(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 			}
 		}
 		IBinding binding = null;
 		ICPPMethod[] result = null;
 
 		IASTDeclaration[] decls = host.getCompositeTypeSpecifier().getMembers();
 		for (IASTDeclaration decl : decls) {
 			while (decl instanceof ICPPASTTemplateDeclaration)
 				decl = ((ICPPASTTemplateDeclaration)decl).getDeclaration();
 			if (decl instanceof IASTSimpleDeclaration) {
				final IASTSimpleDeclaration sdecl = (IASTSimpleDeclaration)decl;
				if (!((ICPPASTDeclSpecifier) sdecl.getDeclSpecifier()).isFriend()) {
					IASTDeclarator[] dtors = sdecl.getDeclarators();
					for (IASTDeclarator dtor : dtors) {
						binding = ASTQueries.findInnermostDeclarator(dtor).getName().resolveBinding();
						if (binding instanceof ICPPMethod)
							result = (ICPPMethod[]) ArrayUtil.append(ICPPMethod.class, result, binding);
					}
 				}
 			} else if (decl instanceof IASTFunctionDefinition) {
				final IASTFunctionDefinition fdef = (IASTFunctionDefinition)decl;
				if (!((ICPPASTDeclSpecifier) fdef.getDeclSpecifier()).isFriend()) {
					IASTDeclarator dtor = fdef.getDeclarator();
					dtor = ASTQueries.findInnermostDeclarator(dtor);
					binding = dtor.getName().resolveBinding();
					if (binding instanceof ICPPMethod) {
						result = (ICPPMethod[]) ArrayUtil.append(ICPPMethod.class, result, binding);
					}
 				}
 			} else if (decl instanceof ICPPASTUsingDeclaration) {
 				IASTName n = ((ICPPASTUsingDeclaration)decl).getName();
 				binding = n.resolveBinding();
 				if (binding instanceof ICPPUsingDeclaration) {
 					IBinding[] bs = ((ICPPUsingDeclaration)binding).getDelegates();
 					for (IBinding element : bs) {
 						if (element instanceof ICPPMethod)
 							result = (ICPPMethod[]) ArrayUtil.append(ICPPMethod.class, result, element);
 					}
 				} else if (binding instanceof ICPPMethod) {
 					result = (ICPPMethod[]) ArrayUtil.append(ICPPMethod.class, result, binding);
 				}
 			}
 		}
 		return (ICPPMethod[]) ArrayUtil.trim(ICPPMethod.class, result);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType#getConstructors()
 	 */
 	public static ICPPConstructor[] getConstructors(ICPPInternalClassTypeMixinHost host) throws DOMException {
 		ICPPClassScope scope = host.getCompositeScope();
 		if (scope == null) {
 			IASTNode[] declarations= host.getDeclarations();
 			IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 			return new ICPPConstructor[] { new CPPConstructor.CPPConstructorProblem(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 		}
 		return scope.getConstructors();
 	}
 
 	public static ICPPClassType[] getNestedClasses(ICPPInternalClassTypeMixinHost host) {
 		if (host.getDefinition() == null) {
 			host.checkForDefinition();
 			if (host.getDefinition() == null) {
 				try {
 					ICPPClassType backup= getBackupDefinition(host);
 					if (backup != null)
 						return backup.getNestedClasses();
 				} catch (DOMException e) {}
 				
 				IASTNode[] declarations= host.getDeclarations();
 				IASTNode node = (declarations != null && declarations.length > 0) ? declarations[0] : null;
 				return new ICPPClassType[] { new CPPClassTypeProblem(node, IProblemBinding.SEMANTIC_DEFINITION_NOT_FOUND, host.getNameCharArray()) };
 			}
 		}
 
 		ICPPClassType[] result = null;
 
 		IASTDeclaration[] decls = host.getCompositeTypeSpecifier().getMembers();
 		for (IASTDeclaration decl : decls) {
 			while (decl instanceof ICPPASTTemplateDeclaration)
 				decl = ((ICPPASTTemplateDeclaration)decl).getDeclaration();
 			if (decl instanceof IASTSimpleDeclaration) {
 				IBinding binding = null;
 				IASTDeclSpecifier declSpec = ((IASTSimpleDeclaration) decl).getDeclSpecifier();
 				if (declSpec instanceof ICPPASTCompositeTypeSpecifier) {
 					binding = ((ICPPASTCompositeTypeSpecifier)declSpec).getName().resolveBinding();
 				} else if (declSpec instanceof ICPPASTElaboratedTypeSpecifier &&
 						((IASTSimpleDeclaration)decl).getDeclarators().length == 0) {
 					binding = ((ICPPASTElaboratedTypeSpecifier)declSpec).getName().resolveBinding();
 				}
 				if (binding instanceof ICPPClassType)
 					result = (ICPPClassType[])ArrayUtil.append(ICPPClassType.class, result, binding);
 			} 
 		}
 		return (ICPPClassType[]) ArrayUtil.trim(ICPPClassType.class, result);
 	}
 
 	public static IField[] getFields(ICPPClassType ct) throws DOMException {
 		IField[] fields = ct.getDeclaredFields();
 		ICPPClassType[] bases = getAllBases(ct);
 		for (ICPPClassType base : bases) {
 			fields = (IField[]) ArrayUtil.addAll(IField.class, fields, base.getFields());
 		}
 		return (IField[]) ArrayUtil.trim(IField.class, fields);
 	}
 
 	public static IField findField(ICPPClassType ct, String name) throws DOMException {
 		IBinding[] bindings = CPPSemantics.findBindings(ct.getCompositeScope(), name, true);
 		IField field = null;
 		for (IBinding binding : bindings) {
 			if (binding instanceof IField) {
 				if (field == null) {
 					field = (IField) binding;
 				} else {
 					IASTNode[] decls= ASTInternal.getDeclarationsOfBinding(ct);
 					IASTNode node= (decls != null && decls.length > 0) ? decls[0] : null;
 					return new CPPField.CPPFieldProblem(node, IProblemBinding.SEMANTIC_AMBIGUOUS_LOOKUP, name.toCharArray());
 				}
 			}
 		}
 		return field;
 	}
 	
 	
 	/**
 	 * Returns whether {@code method} is virtual. This is the case if it is declared to be virtual or
 	 * overrides another virtual method.
 	 */
 	public static boolean isVirtual(ICPPMethod m) throws DOMException {
 		if (m instanceof ICPPConstructor)
 			return false;
 		if (m.isVirtual()) 
 			return true;
 		
 		final char[] mname= m.getNameCharArray();
 		final ICPPClassType mcl= m.getClassOwner();
 		if (mcl != null) {
 			final IFunctionType mft= m.getType();
 			ICPPMethod[] allMethods= mcl.getMethods();
 			for (ICPPMethod method : allMethods) {
 				if (CharArrayUtils.equals(mname, method.getNameCharArray()) && mft.isSameType(method.getType())) {
 					if (method.isVirtual()) {
 						return true;
 					}
 				}
 			}
 		}
 		return false;
 	}
 
 	/**
 	 * Returns {@code true} if {@code source} overrides {@code target}.
 	 * @throws DOMException 
 	 */
 	public static boolean isOverrider(ICPPMethod source, ICPPMethod target) throws DOMException {
 		if (source instanceof ICPPConstructor || target instanceof ICPPConstructor) 
 			return false;
 		if (!isVirtual(target)) 
 			return false;
 		if (!source.getType().isSameType(target.getType()))
 			return false;
 	
 		final ICPPClassType sourceClass= source.getClassOwner();
 		final ICPPClassType targetClass= target.getClassOwner();
 		if (sourceClass == null || targetClass == null)
 			return false;
 		
 		ICPPClassType[] bases= getAllBases(sourceClass);
 		for (ICPPClassType base : bases) {
 			if (base.isSameType(targetClass))
 				return true;
 		}
 		
 		return false;
 	}
 
 	/**
 	 * Returns all methods that are overridden by the given {@code method}.
 	 * @throws DOMException 
 	 */
 	public static ICPPMethod[] findOverridden(ICPPMethod method) throws DOMException {
 		if (method instanceof ICPPConstructor)
 			return ICPPMethod.EMPTY_CPPMETHOD_ARRAY;
 		
 		final char[] mname= method.getNameCharArray();
 		final ICPPClassType mcl= method.getClassOwner();
 		if (mcl == null) 
 			return ICPPMethod.EMPTY_CPPMETHOD_ARRAY;
 		
 		final ArrayList<ICPPMethod> result= new ArrayList<ICPPMethod>();
 		final HashMap<ICPPClassType, Boolean> virtualInClass= new HashMap<ICPPClassType, Boolean>();
 		final IFunctionType mft= method.getType();
 
 		virtualInClass.put(mcl, method.isVirtual());
 		ICPPBase[] bases= mcl.getBases();
 		for (ICPPBase base : bases) {
 			IBinding b= base.getBaseClass();
 			if (b instanceof ICPPClassType) {
 				findOverridden((ICPPClassType) b, mname, mft, virtualInClass, result);
 			}
 		}
 		
 		// list is filled from most derived up to here, reverse it
 		Collections.reverse(result);
 		return result.toArray(new ICPPMethod[result.size()]);
 	}
 
 	/**
 	 * Searches for overridden methods starting in {@code cl}. The map {@code virtualInClass} contains a mapping
 	 * of classes that have been visited to the information whether they (or a base-class) contain an overridden
 	 * method.
 	 * Returns whether {@code cl} contains an overridden method.
 	 */
 	private static boolean findOverridden(ICPPClassType cl, char[] mname, IFunctionType mft,
 			HashMap<ICPPClassType, Boolean> virtualInClass, ArrayList<ICPPMethod> result) throws DOMException {
 		Boolean visitedBefore= virtualInClass.get(cl);
 		if (visitedBefore != null)
 			return visitedBefore;
 		
 		ICPPMethod[] methods= cl.getDeclaredMethods();
 		ICPPMethod candidate= null;
 		boolean hasOverridden= false;
 		for (ICPPMethod method : methods) {
 			if (CharArrayUtils.equals(mname, method.getNameCharArray()) && mft.isSameType(method.getType())) {
 				candidate= method;
 				hasOverridden= method.isVirtual();
 				break;
 			}
 		}
 		
 		// prevent recursion
 		virtualInClass.put(cl, hasOverridden);
 		ICPPBase[] bases= cl.getBases();
 		for (ICPPBase base : bases) {
 			IBinding b= base.getBaseClass();
 			if (b instanceof ICPPClassType) {
 				if (findOverridden((ICPPClassType) b, mname, mft, virtualInClass, result)) {
 					hasOverridden= true;
 				}
 			}
 		}
 		if (hasOverridden) {
 			// the candidate is virtual
 			if (candidate != null)
 				result.add(candidate);
 			virtualInClass.put(cl, hasOverridden);
 		}
 		return hasOverridden;
 	}
 
 	/**
 	 * Returns all methods found in the index, that override the given {@code method}.
 	 * @throws DOMException 
 	 * @throws CoreException 
 	 */
 	public static ICPPMethod[] findOverriders(IIndex index, ICPPMethod method) throws DOMException, CoreException {
 		if (!isVirtual(method)) 
 			return ICPPMethod.EMPTY_CPPMETHOD_ARRAY;
 
 		final ICPPClassType mcl= method.getClassOwner();
 		if (mcl == null) 
 			return ICPPMethod.EMPTY_CPPMETHOD_ARRAY;
 		
 		final ArrayList<ICPPMethod> result= new ArrayList<ICPPMethod>();
 		final char[] mname= method.getNameCharArray();
 		final IFunctionType mft= method.getType();
 		ICPPClassType[] subclasses= getSubClasses(index, mcl);
 		for (ICPPClassType subClass : subclasses) {
 			ICPPMethod[] methods= subClass.getDeclaredMethods();
 			for (ICPPMethod candidate : methods) {
 				if (CharArrayUtils.equals(mname, candidate.getNameCharArray()) &&
 						mft.isSameType(candidate.getType())) {
 					result.add(candidate);
 				}
 			}
 		}
 		return result.toArray(new ICPPMethod[result.size()]);
 	}
 
 	private static ICPPClassType[] getSubClasses(IIndex index, ICPPClassType mcl) throws CoreException {
 		List<ICPPBinding> result= new LinkedList<ICPPBinding>();
 		HashSet<String> handled= new HashSet<String>();
 		getSubClasses(index, mcl, result, handled);
 		result.remove(0);
 		return result.toArray(new ICPPClassType[result.size()]);
 	}
 
 	private static void getSubClasses(IIndex index, ICPPBinding classOrTypedef, List<ICPPBinding> result, HashSet<String> handled) throws CoreException {
 		try {
 			final String key = CPPVisitor.renderQualifiedName(classOrTypedef.getQualifiedName());
 			if (!handled.add(key)) {
 				return;
 			}
 		} catch (DOMException e) {
 			return;
 		}
 
 		if (classOrTypedef instanceof ICPPClassType) {
 			result.add(classOrTypedef);
 		}
 
 		IIndexName[] names= index.findNames(classOrTypedef, IIndex.FIND_REFERENCES | IIndex.FIND_DEFINITIONS);
 		for (IIndexName indexName : names) {
 			if (indexName.isBaseSpecifier()) {
 				IIndexName subClassDef= indexName.getEnclosingDefinition();
 				if (subClassDef != null) {
 					IBinding subClass= index.findBinding(subClassDef);
 					if (subClass instanceof ICPPBinding) {
 						getSubClasses(index, (ICPPBinding) subClass, result, handled);
 					}
 				}
 			}
 		}
 	}
 
 	private static final int KIND_DEFAULT_CTOR= 0;
 	private static final int KIND_COPY_CTOR= 1;
 	private static final int KIND_ASSIGNMENT_OP= 2;
 	private static final int KIND_DTOR= 3;
 	private static final int KIND_OTHER= 4;
 	
 	/**
 	 * For implicit methods the exception specification is inherited, search it
 	 * @throws DOMException 
 	 */
 	public static IType[] getInheritedExceptionSpecification(ICPPMethod implicitMethod) throws DOMException {
 		// See 15.4.13
 		ICPPClassType owner= implicitMethod.getClassOwner();
 		if (owner == null || owner.getBases().length == 0) 
 			return null;
 
 		// we use a list as types aren't comparable, and can have duplicates (15.4.6)
 		int kind= getImplicitMethodKind(owner, implicitMethod);
 		if (kind == KIND_OTHER)
 			return null;
 		
 		List<IType> inheritedTypeids = new ArrayList<IType>();
 		ICPPClassType[] bases= getAllBases(owner);
 		for (ICPPClassType base : bases) {
 			if (!(base instanceof ICPPDeferredClassInstance)) {
 				ICPPMethod  baseMethod= getMethodInClass(base, kind);
 				if (baseMethod != null) {
 					IType[] baseExceptionSpec= baseMethod.getExceptionSpecification();
 					if (baseExceptionSpec == null) 
 						return null;
 					for (IType baseTypeId : baseMethod.getExceptionSpecification()) {
 						inheritedTypeids.add(baseTypeId);
 					}
 				}
 			}
 		}
 		return inheritedTypeids.toArray(new IType[inheritedTypeids.size()]);
 	}
 
 	private static int getImplicitMethodKind(ICPPClassType ct, ICPPMethod method) throws DOMException {
 		if (method instanceof ICPPConstructor) {
 			final IFunctionType type= method.getType();
 			final IType[] params= type.getParameterTypes();
 			if (params.length == 0)
 				return KIND_DEFAULT_CTOR;
 			if (params.length == 1) {
 				IType t= params[0];
 				if (t instanceof IBasicType && ((IBasicType) t).getType() == IBasicType.t_void)
 					return KIND_DEFAULT_CTOR;
 
 				if (isRefToConstClass(ct, t))
 					return KIND_COPY_CTOR;
 			}
 			return KIND_OTHER;
 		}
 		
 		if (method.isDestructor())
 			return KIND_DTOR;
 		
 		if (CharArrayUtils.equals(method.getNameCharArray(), OverloadableOperator.ASSIGN.toCharArray())) {
 			final IFunctionType type= method.getType();
 			final IType[] params= type.getParameterTypes();
 			if (params.length == 1) {
 				IType t= params[0];
 				if (isRefToConstClass(ct, t))
 					return KIND_ASSIGNMENT_OP;
 			}
 			return KIND_OTHER;
 		}
 		return KIND_OTHER;	
 	}
 
 	private static boolean isRefToConstClass(ICPPClassType ct, IType t) throws DOMException {
 		while (t instanceof ITypedef)
 			t= ((ITypedef) t).getType();
 		
 		if (t instanceof ICPPReferenceType) {
 			t= ((ICPPReferenceType) t).getType();
 			while (t instanceof ITypedef)
 				t= ((ITypedef) t).getType();
 			if (t instanceof IQualifierType) {
 				t= ((IQualifierType) t).getType();
 				return ct.isSameType(t);
 			}
 		}
 		return false;
 	}
 
 	private static ICPPMethod getMethodInClass(ICPPClassType ct, int kind) throws DOMException {
 		switch(kind) {
 		case KIND_DEFAULT_CTOR:
 		case KIND_COPY_CTOR:
 			for (ICPPConstructor ctor : ct.getConstructors()) {
 				if (!ctor.isImplicit() && getImplicitMethodKind(ct, ctor) == kind)
 					return ctor;
 			}
 			return null;
 		case KIND_ASSIGNMENT_OP:
 			for (ICPPMethod method : ct.getDeclaredMethods()) {
 				if (method instanceof ICPPConstructor)
 					continue;
 				if (getImplicitMethodKind(ct, method) == kind)
 					return method;
 			}
 			return null;
 		case KIND_DTOR:
 			for (ICPPMethod method : ct.getDeclaredMethods()) {
 				if (method.isDestructor())
 					return method;
 			}
 			return null;
 		}
 		return null;
 	}
 }
