 /*******************************************************************************
  * Copyright (c) 2012 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.gerrit.tests;
 
 import java.util.List;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
 import org.eclipse.mylyn.commons.sdk.util.ManagedTestSuite;
 import org.eclipse.mylyn.commons.sdk.util.TestConfiguration;
 import org.eclipse.mylyn.gerrit.tests.core.GerritConnectorTest;
 import org.eclipse.mylyn.gerrit.tests.core.GerritSynchronizationTest;
 import org.eclipse.mylyn.gerrit.tests.core.client.GerritClientTest;
 import org.eclipse.mylyn.gerrit.tests.core.client.GerritVersionTest;
 import org.eclipse.mylyn.gerrit.tests.core.client.OpenIdAuthenticationTest;
 import org.eclipse.mylyn.gerrit.tests.support.GerritFixture;
 import org.eclipse.mylyn.gerrit.tests.ui.GerritUrlHandlerTest;
 import org.eclipse.mylyn.internal.gerrit.core.remote.GerritDataLocatorTest;
 import org.eclipse.mylyn.internal.gerrit.core.remote.GerritReviewRemoteFactoryTest;
 import org.eclipse.mylyn.internal.gerrit.core.remote.PatchSetRemoteFactoryTest;
 
 /**
  * @author Steffen Pingel
  */
 public class AllGerritTests {
 
 	public static Test suite() {
 		TestSuite suite = new ManagedTestSuite(AllGerritTests.class.getName());
 		addTests(suite, TestConfiguration.getDefault());
 		return suite;
 	}
 
 	public static Test suite(TestConfiguration configuration) {
 		TestSuite suite = new TestSuite(AllGerritTests.class.getName());
 		addTests(suite, configuration);
 		return suite;
 	}
 
 	private static void addTests(TestSuite suite, TestConfiguration configuration) {
 		suite.addTestSuite(GerritVersionTest.class);
 		if (!configuration.isLocalOnly()) {
 			// network tests
 			suite.addTestSuite(OpenIdAuthenticationTest.class);
 			List<GerritFixture> fixtures = configuration.discover(GerritFixture.class, "gerrit"); //$NON-NLS-1$
 			for (GerritFixture fixture : fixtures) {
 				if (!fixture.isExcluded()) {
 					addTests(suite, fixture);
 				}
 			}
 		}
 	}
 
 	private static void addTests(TestSuite suite, GerritFixture fixture) {
 		fixture.createSuite(suite);
 		fixture.add(GerritClientTest.class);
 		fixture.add(GerritConnectorTest.class);
 		fixture.add(GerritSynchronizationTest.class);
 		fixture.add(GerritDataLocatorTest.class);
 		fixture.add(GerritReviewRemoteFactoryTest.class);
 		fixture.add(PatchSetRemoteFactoryTest.class);
		fixture.add(GerritUrlHandlerTest.class);
 		fixture.done();
 	}
 
 }
