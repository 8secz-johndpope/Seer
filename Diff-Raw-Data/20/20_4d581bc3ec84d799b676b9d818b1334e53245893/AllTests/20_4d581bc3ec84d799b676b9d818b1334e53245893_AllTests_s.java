 /*******************************************************************************
  * Copyright (c) 2000, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.tests.ccvs.core;

import junit.extensions.TestSetup;
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
import org.eclipse.core.runtime.Platform;
 import org.eclipse.team.tests.ccvs.core.mappings.ResourceMapperTests;
 import org.eclipse.team.tests.ccvs.core.provider.AllTestsProvider;
 import org.eclipse.team.tests.ccvs.core.subscriber.AllTestsTeamSubscriber;
 import org.eclipse.team.tests.ccvs.ui.AllUITests;
 
 public class AllTests extends EclipseTest {
 
 	public AllTests() {
 		super();
 	}
 
 	public AllTests(String name) {
 		super(name);
 	}
 
 	public static Test suite() {
 		TestSuite suite = new TestSuite();
		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			suite.addTest(AllTestsProvider.suite());
			suite.addTest(AllTestsTeamSubscriber.suite());
			suite.addTest(AllUITests.suite());
			suite.addTest(ResourceMapperTests.suite());
			return new CVSUITestSetup(suite);
		} 
		return new TestSetup(suite);
 	}
 }
