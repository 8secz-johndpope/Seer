 /*******************************************************************************
  * Copyright (c) 2008, 2009 Obeo.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     Obeo - initial API and implementation
  *******************************************************************************/
 package org.eclipse.acceleo.model.mtl.impl.spec;
 
 import java.util.List;
 
 import org.eclipse.acceleo.model.mtl.Query;
 import org.eclipse.acceleo.model.mtl.impl.QueryInvocationImpl;
 import org.eclipse.ocl.EvaluationVisitorDecorator;
 import org.eclipse.ocl.ecore.OCLExpression;
 import org.eclipse.ocl.util.ToStringVisitor;
 import org.eclipse.ocl.utilities.Visitor;
 
 /**
  * Specializes the implementation of the QueryInvocation so that its accept() method delegates the evaluation
  * to our visitor.
  * 
  * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
  */
 public class QueryInvocationSpec extends QueryInvocationImpl {
 	/**
 	 * We know the visitor will be a decorator if the MTLEvaluationVisitor is in use (expected behavior of the
 	 * MTL evaluation engine). This ensures we delegate the call to this decorator.
 	 * 
 	 * @param v
 	 *            The current evaluation visitor.
 	 * @param <T>
 	 *            see {@link OCLExpression#accept(Visitor)}.
 	 * @param <U>
 	 *            see {@link OCLExpression#accept(Visitor)}.
 	 * @return Result of this QueryInvocation evaluation.
 	 */
 	@Override
 	@SuppressWarnings("unchecked")
 	public <T, U extends Visitor<T, ?, ?, ?, ?, ?, ?, ?, ?, ?>> T accept(U v) {
 		if (v instanceof EvaluationVisitorDecorator) {
 			return (T)((EvaluationVisitorDecorator)v).visitExpression(this);
 		} else if (v instanceof ToStringVisitor) {
 			return (T)toString();
 		}
 		throw new UnsupportedOperationException();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 * 
 	 * @see org.eclipse.ocl.ecore.impl.OCLExpressionImpl#toString()
 	 */
 	@Override
 	public String toString() {
 		final Query def = getDefinition();
 		final List<OCLExpression> args = getArgument();
 
		if (def == null) {
			return "unresolved query invocation : " + getName() + "()";
		} else {
			final StringBuilder toString = new StringBuilder(def.getName());
			toString.append('(');
			for (int i = 0; i < args.size(); i++) {
				toString.append(args.get(i).toString());
				if (i + 1 < args.size()) {
					toString.append(',');
				}
 			}
			toString.append(')');
			return toString.toString();
 		}
 	}
 }
