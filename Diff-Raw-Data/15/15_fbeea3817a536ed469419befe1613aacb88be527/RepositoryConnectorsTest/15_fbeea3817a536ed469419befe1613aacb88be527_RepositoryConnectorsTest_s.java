 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.tests.integration;
 
 import junit.extensions.ActiveTestSuite;
 import junit.framework.TestCase;
 import junit.framework.TestSuite;
 
 import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
 import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
 
 /**
  * Test harness for iterating over all connectors and performing a test.
  * 
  * @author Eugene Kuleshov
  */
 public class RepositoryConnectorsTest extends TestCase {
 
 	private final AbstractRepositoryConnector connector;
 
 	public RepositoryConnectorsTest(AbstractRepositoryConnector connector) {
 		super("testRepositoryConnector");
 		this.connector = connector;
 	}
 
 	public void testRepositoryConnector() {
		assertNotNull(connector.getRepositoryType());
 		// add bulk connector tests here...
 	}
 
 	public static TestSuite suite() {
 		TestSuite suite = new ActiveTestSuite(RepositoryConnectorsTest.class.getName());
 
 		for (AbstractRepositoryConnector repositoryConnector : TasksUiPlugin.getRepositoryManager()
 				.getRepositoryConnectors()) {
 			suite.addTest(new RepositoryConnectorsTest(repositoryConnector));
 		}
 
 		return suite;
 	}
 
 }
