 /******************************************************************************* 
  * Copyright (c) 2011 Red Hat, Inc. 
  * Distributed under license by Red Hat, Inc. All rights reserved. 
  * This program is made available under the terms of the 
  * Eclipse Public License v1.0 which accompanies this distribution, 
  * and is available at http://www.eclipse.org/legal/epl-v10.html 
  * 
  * Contributors: 
  * Red Hat, Inc. - initial API and implementation 
  ******************************************************************************/
 package org.jboss.tools.cdi.core.test.tck;
 
 import java.util.Set;
 
 import org.jboss.tools.cdi.core.CDIUtil;
 import org.jboss.tools.cdi.core.IClassBean;
 import org.jboss.tools.cdi.core.IInjectionPoint;
 import org.jboss.tools.cdi.core.IInjectionPointMethod;
 import org.jboss.tools.common.java.IAnnotationDeclaration;
 
 /**
  * @author Alexey Kazakov
  */
 public class CDIUtilTest extends TCKTest {
 
 	/**
 	 * See https://issues.jboss.org/browse/JBIDE-9685 Seam JMS: CDI validator should be aware of JMS resource injections
 	 */
 	public void testMethodParameter() {
 		IClassBean bean = getClassBean("JavaSource/org/jboss/jsr299/tck/tests/jbt/core/TestInjection.java");
 		assertNotNull("Can't find the bean.", bean);
 		Set<IInjectionPoint> injections = bean.getInjectionPoints();
 		for (IInjectionPoint injectionPoint : injections) {
			if(injectionPoint instanceof IInjectionPointMethod) { // TODO Remove this check when https://issues.jboss.org/browse/JBIDE-9698 (Replace interface IInjectionPointMethod by IInitializerMethod) is fixed
				continue;
			}
 			IAnnotationDeclaration declaration = CDIUtil.getAnnotationDeclaration(injectionPoint, "org.jboss.jsr299.tck.tests.jbt.test.core.TestQualifier");
 			String elementName = injectionPoint.getSourceMember().getElementName();
 			if(elementName.equals("i4")) {
 				assertNull(declaration);
 			} else {
 				assertNotNull(declaration);
 			}
 			declaration = CDIUtil.getAnnotationDeclaration(injectionPoint, "org.jboss.jsr299.tck.tests.jbt.test.core.TestQualifier3");
 			if(elementName.equals("i1") || elementName.equals("i2") || elementName.equals("i3")) {
 				assertNull(declaration);
 			} else {
 				assertNotNull(declaration);
 			}
 		}
 	}
 }
