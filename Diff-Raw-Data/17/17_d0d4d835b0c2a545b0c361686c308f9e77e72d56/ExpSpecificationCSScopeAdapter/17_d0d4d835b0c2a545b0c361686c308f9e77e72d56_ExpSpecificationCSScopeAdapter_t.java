 /**
  * <copyright>
  *
  * Copyright (c) 2010,2011 E.D.Willink and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     E.D.Willink - initial API and implementation
  *
  * </copyright>
  *
  * $Id: ExpSpecificationCSScopeAdapter.java,v 1.2 2011/04/20 19:02:15 ewillink Exp $
  */
 package org.eclipse.ocl.examples.xtext.essentialocl.scoping;
 
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.ocl.examples.pivot.ExpressionInOcl;
 import org.eclipse.ocl.examples.pivot.Type;
 import org.eclipse.ocl.examples.pivot.Variable;
 import org.eclipse.ocl.examples.pivot.utilities.PivotUtil;
 import org.eclipse.ocl.examples.xtext.base.scope.EnvironmentView;
 import org.eclipse.ocl.examples.xtext.base.scope.ScopeView;
 import org.eclipse.ocl.examples.xtext.base.scoping.cs.ElementCSScopeAdapter;
 import org.eclipse.ocl.examples.xtext.essentialocl.essentialOCLCST.ExpSpecificationCS;
 
 public class ExpSpecificationCSScopeAdapter extends ElementCSScopeAdapter
 {
 	public static final ExpSpecificationCSScopeAdapter INSTANCE = new ExpSpecificationCSScopeAdapter();
 
 	@Override
 	public ScopeView computeLookup(EObject target, EnvironmentView environmentView, ScopeView scopeView) {
 		ExpSpecificationCS targetElement = (ExpSpecificationCS)target;
 		ExpressionInOcl pivot = PivotUtil.getPivot(ExpressionInOcl.class, targetElement);
 		if (pivot != null) {
 			Variable resultVariable = pivot.getResultVariable();
 			if (resultVariable != null) {
 				environmentView.addNamedElement(resultVariable);
 			}
 			for (Variable parameterVariable : pivot.getParameterVariables()) {
 				environmentView.addNamedElement(parameterVariable);
 			}
 			Variable contextVariable = pivot.getContextVariable();
 			if (contextVariable != null) {
 				environmentView.addNamedElement(contextVariable);
 				if (!environmentView.hasFinalResult()) {
 					Type type = contextVariable.getType();
					if (type != null)
					{
						environmentView.addElementsOfScope(type, scopeView);
						if (!environmentView.hasFinalResult()) {
							environmentView.addElementsOfScope(type.getPackage(), scopeView);
						}
 					}
 				}
 			}
 		}
 		return scopeView.getOuterScope();
 	}
 }
