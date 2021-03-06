 /* *******************************************************************
  * Copyright (c) 2006 Contributors
  * All rights reserved.
  * This program and the accompanying materials are made available
  * under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Andy Clement                 initial implementation
  * ******************************************************************/
 package org.aspectj.ajdt.internal.compiler.lookup;
 
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.Annotation;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.Argument;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.Expression;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.IntLiteral;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.StringLiteral;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
 import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
 import org.aspectj.org.eclipse.jdt.internal.compiler.impl.IntConstant;
 import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.Binding;
 import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
 import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
 import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
 import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
 import org.aspectj.weaver.AnnotationAJ;
 import org.aspectj.weaver.BCException;
 import org.aspectj.weaver.MemberKind;
 import org.aspectj.weaver.ResolvedMemberImpl;
 import org.aspectj.weaver.ResolvedType;
 import org.aspectj.weaver.UnresolvedType;
 import org.aspectj.weaver.World;
 
 /**
  * In the pipeline world, we can be weaving before all types have come through from compilation. In some cases this means the weaver
  * will want to ask questions of eclipse types and this subtype of ResolvedMemberImpl is here to answer some of those questions - it
  * is backed by the real eclipse MethodBinding object and can translate from Eclipse -> Weaver information.
  */
 public class EclipseResolvedMember extends ResolvedMemberImpl {
 
 	private static String[] NO_ARGS = new String[] {};
 
 	private Binding realBinding;
 	private String[] argumentNames;
 	private World w;
 	private ResolvedType[] cachedAnnotationTypes;
 	private EclipseFactory eclipseFactory;
 
	
 	public EclipseResolvedMember(MethodBinding binding, MemberKind memberKind, ResolvedType realDeclaringType, int modifiers,
 			UnresolvedType rettype, String name, UnresolvedType[] paramtypes, UnresolvedType[] extypes,
 			EclipseFactory eclipseFactory) {
 		super(memberKind, realDeclaringType, modifiers, rettype, name, paramtypes, extypes);
 		this.realBinding = binding;
 		this.eclipseFactory = eclipseFactory;
 		this.w = realDeclaringType.getWorld();
 	}
 
 	public EclipseResolvedMember(FieldBinding binding, MemberKind field, ResolvedType realDeclaringType, int modifiers,
 			ResolvedType type, String string, UnresolvedType[] none) {
 		super(field, realDeclaringType, modifiers, type, string, none);
 		this.realBinding = binding;
 		this.w = realDeclaringType.getWorld();
 	}
 
 	public boolean hasAnnotation(UnresolvedType ofType) {
 		ResolvedType[] annotationTypes = getAnnotationTypes();
 		if (annotationTypes == null)
 			return false;
 		for (int i = 0; i < annotationTypes.length; i++) {
 			ResolvedType type = annotationTypes[i];
 			if (type.equals(ofType))
 				return true;
 		}
 		return false;
 	}
 
 	public AnnotationAJ[] getAnnotations() {
 		// long abits =
 		realBinding.getAnnotationTagBits(); // ensure resolved
 		Annotation[] annos = getEclipseAnnotations();
 		if (annos == null) {
 			return null;
 		}
 		AnnotationAJ[] annoAJs = new AnnotationAJ[annos.length];
 		for (int i = 0; i < annos.length; i++) {
 			annoAJs[i] = EclipseAnnotationConvertor.convertEclipseAnnotation(annos[i], w, eclipseFactory);
 		}
 		return annoAJs;
 	}
 
 	public AnnotationAJ getAnnotationOfType(UnresolvedType ofType) {
 		// long abits =
 		realBinding.getAnnotationTagBits(); // ensure resolved
 		Annotation[] annos = getEclipseAnnotations();
 		if (annos == null)
 			return null;
 		for (int i = 0; i < annos.length; i++) {
 			Annotation anno = annos[i];
 			UnresolvedType ut = UnresolvedType.forSignature(new String(anno.resolvedType.signature()));
 			if (w.resolve(ut).equals(ofType)) {
 				// Found the one
 				return EclipseAnnotationConvertor.convertEclipseAnnotation(anno, w, eclipseFactory);
 			}
 		}
 		return null;
 	}
 
 	public String getAnnotationDefaultValue() {
 		if (realBinding instanceof MethodBinding) {
 			AbstractMethodDeclaration methodDecl = getTypeDeclaration().declarationOf((MethodBinding) realBinding);
 			if (methodDecl instanceof AnnotationMethodDeclaration) {
 				AnnotationMethodDeclaration annoMethodDecl = (AnnotationMethodDeclaration) methodDecl;
 				Expression e = annoMethodDecl.defaultValue;
 				if (e.resolvedType == null)
 					e.resolve(methodDecl.scope);
 				// TODO does not cope with many cases...
 				if (e instanceof QualifiedNameReference) {
 
 					QualifiedNameReference qnr = (QualifiedNameReference) e;
 					if (qnr.binding instanceof FieldBinding) {
 						FieldBinding fb = (FieldBinding) qnr.binding;
 						StringBuffer sb = new StringBuffer();
 						sb.append(fb.declaringClass.signature());
 						sb.append(fb.name);
 						return sb.toString();
 					}
 				} else if (e instanceof TrueLiteral) {
 					return "true";
 				} else if (e instanceof FalseLiteral) {
 					return "false";
 				} else if (e instanceof StringLiteral) {
 					return new String(((StringLiteral) e).source());
 				} else if (e instanceof IntLiteral) {
 					return Integer.toString(((IntConstant) e.constant).intValue());
 				} else {
 					throw new BCException("EclipseResolvedMember.getAnnotationDefaultValue() not implemented for value of type '"
 							+ e.getClass() + "' - raise an AspectJ bug !");
 				}
 			}
 		}
 		return null;
 	}
 
 	public ResolvedType[] getAnnotationTypes() {
 		if (cachedAnnotationTypes == null) {
 			// long abits =
 			realBinding.getAnnotationTagBits(); // ensure resolved
 			Annotation[] annos = getEclipseAnnotations();
 			if (annos == null) {
 				cachedAnnotationTypes = ResolvedType.EMPTY_RESOLVED_TYPE_ARRAY;
 			} else {
 				cachedAnnotationTypes = new ResolvedType[annos.length];
 				for (int i = 0; i < annos.length; i++) {
 					Annotation type = annos[i];
 					cachedAnnotationTypes[i] = w.resolve(UnresolvedType.forSignature(new String(type.resolvedType.signature())));
 				}
 			}
 		}
 		return cachedAnnotationTypes;
 	}
 
 	public String[] getParameterNames() {
 		if (argumentNames != null)
 			return argumentNames;
 		if (realBinding instanceof FieldBinding) {
 			argumentNames = NO_ARGS;
 		} else {
 			TypeDeclaration typeDecl = getTypeDeclaration();
 			AbstractMethodDeclaration methodDecl = (typeDecl == null ? null : typeDecl.declarationOf((MethodBinding) realBinding));
 			Argument[] args = (methodDecl == null ? null : methodDecl.arguments); // dont
 			// like
 			// this
 			// -
 			// why
 			// isnt
 			// the
 			// method
 			// found
 			// sometimes? is it because other errors are
 			// being reported?
 			if (args == null) {
 				argumentNames = NO_ARGS;
 			} else {
 				argumentNames = new String[args.length];
 				for (int i = 0; i < argumentNames.length; i++) {
 					argumentNames[i] = new String(methodDecl.arguments[i].name);
 				}
 			}
 		}
 		return argumentNames;
 	}
 
 	private Annotation[] getEclipseAnnotations() {
 		TypeDeclaration tDecl = getTypeDeclaration();
 		if (tDecl != null) {// if the code is broken then tDecl may be null
 			if (realBinding instanceof MethodBinding) {
 				AbstractMethodDeclaration methodDecl = tDecl.declarationOf((MethodBinding) realBinding);
 				return methodDecl.annotations;
 			} else if (realBinding instanceof FieldBinding) {
 				FieldDeclaration fieldDecl = tDecl.declarationOf((FieldBinding) realBinding);
 				return fieldDecl.annotations;
 			}
 		}
 		return null;
 	}
 
 	private TypeDeclaration getTypeDeclaration() {
 		if (realBinding instanceof MethodBinding) {
 			MethodBinding mb = (MethodBinding) realBinding;
 			if (mb != null) {
 				SourceTypeBinding stb = (SourceTypeBinding) mb.declaringClass;
 				if (stb != null) {
 					ClassScope cScope = stb.scope;
 					if (cScope != null)
 						return cScope.referenceContext;
 				}
 			}
 		} else if (realBinding instanceof FieldBinding) {
 			FieldBinding fb = (FieldBinding) realBinding;
 			if (fb != null) {
 				SourceTypeBinding stb = (SourceTypeBinding) fb.declaringClass;
 				if (stb != null) {
 					ClassScope cScope = stb.scope;
 					if (cScope != null)
 						return cScope.referenceContext;
 				}
 			}
 		}
 		return null;
 	}
 
    /**
     * Return true if this is the default constructor.  The default constructor
     * is the one generated if there isn't one in the source.  Eclipse
     * helpfully uses a bit to indicate the default constructor.
     *
     * @return true if this is the default constructor. 
     */
	public boolean isDefaultConstructor() {
		if (!(realBinding instanceof MethodBinding)) {
			return false;
		}
		MethodBinding mb = (MethodBinding) realBinding;
		return mb.isConstructor() && ((mb.modifiers & ExtraCompilerModifiers.AccIsDefaultConstructor) != 0);
	}

 }
