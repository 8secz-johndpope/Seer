 /*******************************************************************************
  * Copyright (c) 2004, 2009 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.bugzilla.tests;
 
 import junit.framework.TestCase;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.mylyn.bugzilla.tests.support.BugzillaFixture;
 import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
 import org.eclipse.mylyn.commons.net.AuthenticationType;
 import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
 import org.eclipse.mylyn.internal.bugzilla.core.BugzillaClient;
 import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
 import org.eclipse.mylyn.tasks.core.TaskRepository;
 import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
 import org.eclipse.mylyn.tasks.core.data.TaskData;
 import org.eclipse.mylyn.tasks.core.data.TaskMapper;
 import org.eclipse.mylyn.tests.util.TestUtil.PrivilegeLevel;
 
 /**
  * @author Rob Elves
  * @author Mik Kersten
  */
 public class RepositoryReportFactoryTest extends TestCase {
 
 	TaskRepository repository;
 
 	BugzillaClient client;
 
 	BugzillaRepositoryConnector connector;
 
 	@Override
 	protected void setUp() throws Exception {
 		repository = BugzillaFixture.current().repository();
 		client = BugzillaFixture.current().client();
 		connector = BugzillaFixture.current().connector();
 	}
 
 	public void testInvalidCredentials222() throws Exception {
 		String errorMessage = "";
 		try {
 			client.logout(new NullProgressMonitor());
 			repository.setCredentials(AuthenticationType.REPOSITORY,
 					new AuthenticationCredentials("invalid", "invalid"), false);
 			connector.getTaskData(repository, "1", new NullProgressMonitor());
			fail("CoreException expected but not found");
 		} catch (CoreException e) {
 			errorMessage = e.getStatus().getMessage();
 		}
 		assertTrue(errorMessage.startsWith("Unable to login"));
 		repository.flushAuthenticationCredentials();
 	}
 
 	public void testBugNotFound222() {
 
 		String errorMessage = "";
 		try {
 			connector.getClientManager().repositoryAdded(repository);
 			connector.getTaskData(repository, "-1", new NullProgressMonitor());
 		} catch (CoreException e) {
 			errorMessage = e.getStatus().getMessage();
 		}
 		assertTrue(errorMessage.startsWith("Repository error from"));
 	}
 
 	public void testPostingAndReadingAttributes() throws Exception {
 		TaskData data = BugzillaFixture.current().createTask(PrivilegeLevel.USER, "testPostingAndReading() summary",
 				"testPostingAndReading() description");
 		data.getRoot().getMappedAttribute(TaskAttribute.COMPONENT).setValue("ManualC2");
 		data.getRoot().getMappedAttribute(TaskAttribute.PRIORITY).setValue("P5");
 		data.getRoot().getMappedAttribute(TaskAttribute.SEVERITY).setValue("enhancement");
 		data.getRoot().getMappedAttribute(BugzillaAttribute.REP_PLATFORM.getKey()).setValue("PC");
 		data.getRoot().getMappedAttribute(BugzillaAttribute.OP_SYS.getKey()).setValue("Linux");
 		data.getRoot().getMappedAttribute(BugzillaAttribute.VERSION.getKey()).setValue("R2.0");
 //		data.getRoot().getMappedAttribute(BugzillaAttribute.DEPENDSON.getKey()).setValue("6, 7");
 //		data.getRoot().getMappedAttribute(BugzillaAttribute.BLOCKED.getKey()).setValue("13, 14");
 
 		BugzillaFixture.current().submitTask(data, client);
 		data = BugzillaFixture.current().getTask(data.getTaskId(), client);
 		assertNotNull(data);
 		assertEquals("ManualC2", data.getRoot().getMappedAttribute(TaskAttribute.COMPONENT).getValue());
 		assertEquals("P5", data.getRoot().getMappedAttribute(TaskAttribute.PRIORITY).getValue());
 		assertEquals("enhancement", data.getRoot().getMappedAttribute(TaskAttribute.SEVERITY).getValue());
 		assertEquals("PC", data.getRoot().getMappedAttribute(BugzillaAttribute.REP_PLATFORM.getKey()).getValue());
 		assertEquals("Linux", data.getRoot().getMappedAttribute(BugzillaAttribute.OP_SYS.getKey()).getValue());
 		assertEquals("R2.0", data.getRoot().getMappedAttribute(BugzillaAttribute.VERSION.getKey()).getValue());
 		assertEquals("R2.0", data.getRoot().getMappedAttribute(BugzillaAttribute.VERSION.getKey()).getValue());
 //		assertEquals("6, 7", root.getAttribute(BugzillaAttribute.BLOCKED.getKey()).getValue());
 //		assertEquals("13, 14", root.getAttribute(BugzillaAttribute.BLOCKED.getKey()).getValue());
 
 		TaskMapper mapper = new TaskMapper(data);
 		assertEquals("testPostingAndReading() summary", mapper.getSummary());
 		assertEquals("testPostingAndReading() description", mapper.getDescription());
 		assertEquals("ManualTest", mapper.getProduct());
 	}
 
 	// FIXME: Test posting and retrieval of time values
 //	public void testTimeTracking222() throws Exception {
 //		assertEquals("7.50", report.getRoot().getAttribute(BugzillaAttribute.ESTIMATED_TIME.getKey()).getValue());
 //		assertEquals("4.00", report.getRoot().getAttribute(BugzillaAttribute.ACTUAL_TIME.getKey()).getValue());
 //		assertEquals("3.00", report.getRoot().getAttribute(BugzillaAttribute.REMAINING_TIME.getKey()).getValue());
 //		assertEquals("2005-03-04", report.getRoot().getAttribute(BugzillaAttribute.DEADLINE.getKey()).getValue());
 //	}
 
 	public void testDeltaTsTruncation() {
 		String ts1 = "2006-07-06 03:22:08 0900";
 		String ts1_truncated = "2006-07-06 03:22:08";
 		assertEquals(ts1_truncated, BugzillaClient.stripTimeZone(ts1));
 
 		String ts2 = "2006-07-06 03:22:08";
 		String ts2_truncated = "2006-07-06 03:22:08";
 		assertEquals(ts2_truncated, BugzillaClient.stripTimeZone(ts2));
 
 		String ts3 = "2006-07-06 03:22:08 PST";
 		String ts3_truncated = "2006-07-06 03:22:08";
 		assertEquals(ts3_truncated, BugzillaClient.stripTimeZone(ts3));
 	}
 
 }
