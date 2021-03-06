 /*******************************************************************************
  * Copyright (c) 2010 xored software, Inc.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
  *******************************************************************************/
 package org.eclipse.dltk.javascript.internal.core.codeassist;
 
 import java.util.HashSet;
 import java.util.Set;
 
 import org.eclipse.dltk.codeassist.ScriptCompletionEngine;
 import org.eclipse.dltk.compiler.CharOperation;
 import org.eclipse.dltk.compiler.env.IModuleSource;
 import org.eclipse.dltk.core.CompletionProposal;
 import org.eclipse.dltk.core.DLTKCore;
 import org.eclipse.dltk.core.IAccessRule;
 import org.eclipse.dltk.core.ISourceModule;
 import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
 import org.eclipse.dltk.internal.javascript.ti.ITypeInferenceContext;
 import org.eclipse.dltk.internal.javascript.ti.IValueProvider;
 import org.eclipse.dltk.internal.javascript.ti.PositionReachedException;
 import org.eclipse.dltk.internal.javascript.ti.TypeInferencer2;
 import org.eclipse.dltk.internal.javascript.typeinference.CompletionPath;
 import org.eclipse.dltk.javascript.ast.Script;
 import org.eclipse.dltk.javascript.ast.StringLiteral;
 import org.eclipse.dltk.javascript.core.JavaScriptKeywords;
 import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
 import org.eclipse.dltk.javascript.typeinference.IValueCollection;
 import org.eclipse.dltk.javascript.typeinference.IValueParent;
 import org.eclipse.dltk.javascript.typeinference.IValueReference;
 import org.eclipse.dltk.javascript.typeinference.ReferenceKind;
 import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IMethod;
 import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IParameter;
 import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IVariable;
 import org.eclipse.dltk.javascript.typeinfo.MemberPredicate;
 import org.eclipse.dltk.javascript.typeinfo.TypeMemberQuery;
 import org.eclipse.dltk.javascript.typeinfo.TypeMode;
 import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
 import org.eclipse.dltk.javascript.typeinfo.model.ClassType;
 import org.eclipse.dltk.javascript.typeinfo.model.Element;
 import org.eclipse.dltk.javascript.typeinfo.model.JSType;
 import org.eclipse.dltk.javascript.typeinfo.model.Member;
 import org.eclipse.dltk.javascript.typeinfo.model.Method;
 import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
 import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
 import org.eclipse.dltk.javascript.typeinfo.model.Type;
 import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
 
 public class JavaScriptCompletionEngine2 extends ScriptCompletionEngine
 		implements JSCompletionEngine {
 
 	private boolean useEngine = true;
 
 	public boolean isUseEngine() {
 		return useEngine;
 	}
 
 	public void setUseEngine(boolean useEngine) {
 		this.useEngine = useEngine;
 	}
 
 	public void complete(final IModuleSource cu, int position, int i) {
 		this.requestor.beginReporting();
 		String content = cu.getSourceContents();
 		if (position < 0 || position > content.length()) {
 			return;
 		}
 
 		final TypeInferencer2 inferencer2 = new TypeInferencer2();
 		inferencer2.setModelElement(cu.getModelElement());
 
 		final Script script = JavaScriptParserUtil.parse(cu, null);
 		final NodeFinder nodeFinder = new NodeFinder(content, position,
 				position);
 		nodeFinder.locate(script);
 		if (nodeFinder.getNode() instanceof StringLiteral) {
 			// don't complete inside string literals
 			return;
 		}
 		final PositionCalculator calculator = new PositionCalculator(content,
 				position, false);
 		final CompletionVisitor visitor = new CompletionVisitor(inferencer2,
 				position);
 		inferencer2.setVisitor(visitor);
 		if (cu instanceof org.eclipse.dltk.core.ISourceModule) {
 			inferencer2
 					.setModelElement((org.eclipse.dltk.core.ISourceModule) cu);
 		}
 		try {
 			inferencer2.doInferencing(script);
 		} catch (PositionReachedException e) {
 			// e.printStackTrace();
 		}
 
 		final CompletionPath path = new CompletionPath(
 				calculator.getCompletion());
 		final Reporter reporter = new Reporter(path.lastSegment(), position);
 		if (calculator.isMember() && !path.isEmpty()
 				&& path.lastSegment() != null) {
 			doCompletionOnMember(inferencer2, visitor.getCollection(), path,
 					reporter);
 		} else {
 			doGlobalCompletion(inferencer2, visitor.getCollection(), reporter);
 		}
 		this.requestor.endReporting();
 	}
 
 	public void completeTypes(ISourceModule module, TypeMode mode,
 			String prefix, int offset) {
 		final TypeInferencer2 inferencer2 = new TypeInferencer2();
 		inferencer2.setModelElement(module);
 		doCompletionOnType(inferencer2, mode, new Reporter(prefix, offset));
 	}
 
 	private void doCompletionOnType(ITypeInferenceContext context,
 			TypeMode mode, Reporter reporter) {
 		Set<String> typeNames = context.listTypes(mode, reporter.getPrefix());
 		for (String typeName : typeNames) {
 			final Type type = context.getType(typeName);
 			if (type != null && type.isVisible()) {
 				reporter.reportTypeRef(type);
 			}
 		}
 	}
 
 	private static boolean exists(IValueParent item) {
 		if (item instanceof IValueReference) {
 			return ((IValueReference) item).exists();
 		} else {
 			return true;
 		}
 	}
 
 	/**
 	 * @param context
 	 * @param collection
 	 * @param startPart
 	 */
 	private void doCompletionOnMember(ITypeInferenceContext context,
 			IValueCollection collection, CompletionPath path, Reporter reporter) {
 		IValueParent item = collection;
 		for (int i = 0; i < path.segmentCount() - 1; ++i) {
 			if (path.isName(i)) {
 				final String segment = path.segment(i);
 				if ("this".equals(segment) && item instanceof IValueCollection) {
 					item = ((IValueCollection) item).getThis();
 				} else {
 					item = item.getChild(segment);
 				}
 				if (!exists(item))
 					break;
 			} else if (path.isFunction(i)) {
 				item = item.getChild(IValueReference.FUNCTION_OP);
 				if (!exists(item))
 					break;
 			} else {
 				assert path.isArray(i);
 				item = item.getChild(IValueReference.ARRAY_OP);
 				if (!exists(item))
 					break;
 			}
 		}
 
 		if (item != null && exists(item)) {
 			reportItems(context, reporter, item, true);
 		}
 	}
 
 	protected void reportItems(ITypeInferenceContext context,
 			Reporter reporter, IValueParent item, boolean testPrivate) {
 		reporter.report(context, item, testPrivate);
 		if (item instanceof IValueCollection) {
 			IValueCollection coll = (IValueCollection) item;
 			for (;;) {
 				coll = coll.getParent();
 				if (coll == null)
 					break;
 				reporter.report(context, coll, testPrivate);
 			}
 		}
 	}
 
 	protected void reportGlobals(ITypeInferenceContext context,
 			Reporter reporter) {
 		final Set<String> globals = context.listGlobals(reporter.getPrefix());
 		for (String global : globals) {
 			if (reporter.canReport(global)) {
 				Member element = context.resolve(global);
 				if (element != null && element.isVisible()) {
 					reporter.report(global, element);
 				}
 			}
 		}
 	}
 
 	private class Reporter {
 
 		final char[] prefix;
 		private final String prefixStr;
 		final int position;
 		final Set<Object> processed = new HashSet<Object>();
 		final boolean camelCase = DLTKCore.ENABLED.equals(DLTKCore
 				.getOption(DLTKCore.CODEASSIST_CAMEL_CASE_MATCH));
 
 		public Reporter(String prefix, int position) {
 			this.prefixStr = prefix != null ? prefix : "";
 			this.prefix = prefixStr.toCharArray();
 			this.position = position;
 			setSourceRange(position - this.prefix.length, position);
 		}
 
 		public void ignore(String generatedIdentifier) {
 			processed.add(generatedIdentifier);
 		}
 
 		public String getPrefix() {
 			return prefixStr;
 		}
 
 		public int getPosition() {
 			return position;
 		}
 
 		public void report(String name, Element element) {
 			if (element instanceof Member && processed.add(name)) {
 				reportMember((Member) element, name);
 			}
 		}
 
 		public boolean canReport(String name) {
 			return matches(name) && !processed.contains(name);
 		}
 
 		private boolean matches(String name) {
 			return CharOperation.prefixEquals(prefix, name, false) || camelCase
 					&& CharOperation.camelCaseMatch(prefix, name.toCharArray());
 		}
 
 		public void report(ITypeInferenceContext context, IValueParent item,
 				boolean testPrivate) {
 			boolean superScope = false;
 			if (item instanceof IValueProvider) {
 				superScope = ((IValueProvider) item).getValue().getAttribute(
 						IReferenceAttributes.SUPER_SCOPE, false) == Boolean.TRUE;
 			}
 			final Set<String> deleted = item.getDeletedChildren();
 			for (String childName : item.getDirectChildren()) {
 				if (childName.equals(IValueReference.FUNCTION_OP))
 					continue;
 				if (!deleted.contains(childName) && matches(childName)
 						&& processed.add(childName)) {
 					IValueReference child = item.getChild(childName);
 					if (child.exists()) {
 						if (testPrivate) {
 							IMethod method = (IMethod) child
 									.getAttribute(IReferenceAttributes.PARAMETERS);
 							IVariable variable = (IVariable) child
 									.getAttribute(IReferenceAttributes.VARIABLE);
 							if ((method != null && (method.isPrivate() || (method
 									.isProtected() && !superScope)))
 									|| (variable != null && (variable
 											.isPrivate() || (variable
 											.isProtected() && !superScope)))) {
 								continue;
 							}
 						} else if (child
 								.getAttribute(IReferenceAttributes.PRIVATE) == Boolean.TRUE)
 							continue;
 						reportReference(child);
 					}
 				}
 			}
 			if (item instanceof IValueReference) {
 				final IValueReference valueRef = (IValueReference) item;
 				final TypeMemberQuery typeQuery = new TypeMemberQuery();
 				for (JSType type : valueRef.getDeclaredTypes()) {
 					if (type instanceof ClassType) {
 						final Type t = ((ClassType) type).getTarget();
 						if (t != null) {
 							typeQuery.add(t, MemberPredicate.STATIC);
 						}
 					} else {
 						final Type t = TypeUtil.extractType(context
 								.resolveTypeRef(type));
 						if (t != null) {
 							typeQuery.add(t, MemberPredicate.NON_STATIC);
 						}
 					}
 				}
 				for (JSType type : valueRef.getTypes()) {
 					if (type instanceof ClassType) {
 						final Type t = ((ClassType) type).getTarget();
 						if (t != null) {
 							typeQuery.add(t, MemberPredicate.STATIC);
 						}
 					} else {
 						final Type t = TypeUtil.extractType(context
 								.resolveTypeRef(type));
 						if (t != null) {
 							typeQuery.add(t, MemberPredicate.NON_STATIC);
 						}
 					}
 				}
 				for (Member member : typeQuery) {
 					if (processed.add(MethodKey.createKey(member))
 							&& member.isVisible()
 							&& CharOperation.prefixEquals(prefix,
 									member.getName(), false)) {
 						reportMember(member, member.getName());
 					}
 				}
 			}
 		}
 
 		/**
 		 * @param member
 		 */
 		private void reportMember(Member member, String memberName) {
 			boolean isFunction = member instanceof Method;
 			CompletionProposal proposal = CompletionProposal.create(
 					isFunction ? CompletionProposal.METHOD_REF
 							: CompletionProposal.FIELD_REF, position);
 
 			int relevance = computeBaseRelevance();
 			// relevance += computeRelevanceForInterestingProposal();
 			relevance += computeRelevanceForCaseMatching(prefix, memberName);
 			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
 			proposal.setRelevance(relevance);
 
 			proposal.setCompletion(memberName);
 			proposal.setName(memberName);
 			proposal.setExtraInfo(member);
 			proposal.setReplaceRange(startPosition - offset, endPosition
 					- offset);
 			if (isFunction) {
 				Method method = (Method) member;
 				int paramCount = method.getParameters().size();
 				if (paramCount > 0) {
 					final String[] params = new String[paramCount];
 					for (int i = 0; i < paramCount; ++i) {
 						Parameter parameter = method.getParameters().get(i);
 						if (parameter.getKind() == ParameterKind.OPTIONAL) {
 							params[i] = '[' + parameter.getName() + ']';
 						} else {
 							params[i] = parameter.getName();
 						}
 					}
 					proposal.setParameterNames(params);
 				}
 			}
 			requestor.accept(proposal);
 		}
 
 		/**
 		 * @param reference
 		 */
 		private void reportReference(IValueReference reference) {
 			int proposalKind = CompletionProposal.FIELD_REF;
 			final ReferenceKind kind = reference.getKind();
			if (reference.getAttribute(IReferenceAttributes.PHANTOM, true) == null
 					&& (kind == ReferenceKind.FUNCTION || reference
 							.hasChild(IValueReference.FUNCTION_OP))) {
 				proposalKind = CompletionProposal.METHOD_REF;
 			} else if (kind == ReferenceKind.LOCAL) {
 				proposalKind = CompletionProposal.LOCAL_VARIABLE_REF;
 			}
 			CompletionProposal proposal = CompletionProposal.create(
 					proposalKind, position);
 
 			int relevance = computeBaseRelevance();
 			relevance += computeRelevanceForInterestingProposal();
 			relevance += computeRelevanceForCaseMatching(prefix,
 					reference.getName());
 			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
 			proposal.setRelevance(relevance);
 
 			proposal.setCompletion(reference.getName());
 			proposal.setName(reference.getName());
 			proposal.setExtraInfo(reference);
 			proposal.setReplaceRange(startPosition - offset, endPosition
 					- offset);
 			if (proposalKind == CompletionProposal.METHOD_REF) {
 				final IMethod method = (IMethod) reference.getAttribute(
 						IReferenceAttributes.PARAMETERS, true);
 				if (method != null) {
 					int paramCount = method.getParameterCount();
 					if (paramCount > 0) {
 						final String[] params = new String[paramCount];
 						for (int i = 0; i < paramCount; ++i) {
 							IParameter parameter = method.getParameters()
 									.get(i);
 							if (parameter.isOptional()) {
 								params[i] = '[' + parameter.getName() + ']';
 							} else {
 								params[i] = parameter.getName();
 							}
 						}
 						proposal.setParameterNames(params);
 					}
 				}
 			}
 			requestor.accept(proposal);
 		}
 
 		public void reportTypeRef(Type type) {
 			if (!processed.add(type.getName())) {
 				return;
 			}
 			CompletionProposal proposal = CompletionProposal.create(
 					CompletionProposal.TYPE_REF, position);
 			int relevance = computeBaseRelevance();
 			// relevance += computeRelevanceForInterestingProposal();
 			relevance += computeRelevanceForCaseMatching(prefix, type.getName());
 			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
 			proposal.setRelevance(relevance);
 			proposal.setCompletion(type.getName());
 			proposal.setName(type.getName());
 			proposal.setExtraInfo(type);
 			proposal.setReplaceRange(startPosition - offset, endPosition
 					- offset);
 			requestor.accept(proposal);
 		}
 
 	}
 
 	static class MethodKey {
 		final String name;
 		final String signature;
 
 		/**
 		 * @param name
 		 */
 		public MethodKey(Method method) {
 			this.name = method.getName();
 			StringBuilder sb = new StringBuilder();
 			for (Parameter parameter : method.getParameters()) {
 				final JSType paramType = parameter.getType();
 				if (paramType != null) {
 					sb.append(paramType.getName());
 				}
 				sb.append(',');
 			}
 			this.signature = sb.toString();
 		}
 
 		@Override
 		public int hashCode() {
 			return name.hashCode();
 		}
 
 		@Override
 		public boolean equals(Object obj) {
 			if (obj instanceof MethodKey) {
 				final MethodKey other = (MethodKey) obj;
 				return name.equals(other.name)
 						&& signature.equals(other.signature);
 			}
 			return false;
 		}
 
 		protected static Object createKey(Member member) {
 			if (member instanceof Method && member.getDeclaringType() != null
 					&& member.getDeclaringType().getKind() == TypeKind.JAVA) {
 				return new MethodKey((Method) member);
 			} else {
 				return member.getName();
 			}
 		}
 
 	}
 
 	/**
 	 * @param context
 	 * @param collection
 	 * @param reporter
 	 */
 	private void doGlobalCompletion(ITypeInferenceContext context,
 			IValueCollection collection, Reporter reporter) {
 		reportItems(context, reporter, collection, false);
 		if (useEngine) {
 			doCompletionOnType(context, TypeMode.CODE, reporter);
 			doCompletionOnKeyword(reporter.getPrefix(), reporter.getPosition());
 			reportGlobals(context, reporter);
 		}
 	}
 
 	private void doCompletionOnKeyword(String startPart, int position) {
 		setSourceRange(position - startPart.length(), position);
 		String[] keywords = JavaScriptKeywords.getJavaScriptKeywords();
 		findKeywords(startPart.toCharArray(), keywords, true);
 	}
 
 }
