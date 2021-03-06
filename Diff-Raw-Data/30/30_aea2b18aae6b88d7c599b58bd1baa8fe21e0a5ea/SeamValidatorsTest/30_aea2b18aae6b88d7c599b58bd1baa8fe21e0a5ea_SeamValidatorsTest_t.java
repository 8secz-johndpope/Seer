 /*******************************************************************************
  * Copyright (c) 2007 Red Hat, Inc.
  * Distributed under license by Red Hat, Inc. All rights reserved.
  * This program is made available under the terms of the
  * Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Red Hat, Inc. - initial API and implementation
  ******************************************************************************/
 package org.jboss.tools.seam.core.test;
 
 import java.io.IOException;
 import java.util.Set;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.jface.preference.IPersistentPreferenceStore;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.jboss.tools.seam.core.ISeamComponent;
 import org.jboss.tools.seam.core.ISeamComponentMethod;
 import org.jboss.tools.seam.core.ISeamProject;
 import org.jboss.tools.seam.core.SeamComponentMethodType;
 import org.jboss.tools.seam.core.SeamCorePlugin;
 import org.jboss.tools.seam.core.SeamPreferences;
 import org.jboss.tools.seam.internal.core.SeamProject;
 import org.jboss.tools.test.util.JUnitUtils;
 import org.jboss.tools.test.util.JobUtils;
 import org.jboss.tools.test.util.ProjectImportTestSetup;
 import org.jboss.tools.tests.AbstractResourceMarkerTest;
 
 public class SeamValidatorsTest extends AbstractResourceMarkerTest {
 	IProject project = null;
 	
 	public static final String MARKER_TYPE = "org.eclipse.wst.validation.problemmarker";
 
 	public SeamValidatorsTest() {
 		super("Seam Validator Tests");
 	}
 
 	public SeamValidatorsTest(String name) {
 		super(name);
 	}
 
 	protected void setUp() throws Exception {
 		JobUtils.waitForIdle();
 		IResource project = ResourcesPlugin.getWorkspace().getRoot().findMember("SeamWebWarTestProject");
 		if(project == null) {
 			ProjectImportTestSetup setup = new ProjectImportTestSetup(
 					this,
 					"org.jboss.tools.seam.core.test",
 					"projects/SeamWebWarTestProject",
 					"SeamWebWarTestProject");
 			project = setup.importProject();
 		}
 		this.project = project.getProject();
 		JobUtils.waitForIdle();
 	}
 
 	private ISeamProject getSeamProject(IProject project) {
 		refreshProject(project);
 		
 		ISeamProject seamProject = null;
 		try {
 			seamProject = (ISeamProject)project.getNature(SeamProject.NATURE_ID);
 		} catch (Exception e) {
 			JUnitUtils.fail("Cannot get seam nature.",e);
 		}
 		assertNotNull("Seam project is null", seamProject);
 		return seamProject;
 	}
 
 	/**
 	 * Test for https://jira.jboss.org/jira/browse/JBIDE-784
 	 * @throws CoreException 
 	 */
 	public void testJavaFileOutsideClassPath() throws CoreException {
 		IFile file = project.getFile("WebContent/Authenticator.java");
 		String[] messages = getMarkersMessage(file);
 		assertTrue("Problem marker was found in WebContent/Authenticator.java file. Seam EL validator should not validate it.", messages.length == 0);
 	}
 
 	/**
 	 * Test for http://jira.jboss.com/jira/browse/JBIDE-1318
 	 * @throws CoreException 
 	 */
 	public void testJBIDE1318() throws CoreException {
 		getSeamProject(project);
 		IFile testJSP = project.getFile("WebContent/test.jsp");
 		testJSP.touch(null);
 		JobUtils.waitForIdle();
 		assertMarkerIsNotCreated(testJSP, MARKER_TYPE, "actor cannot be resolved");
 	}
 
 	public void testVarAttributes() throws CoreException {
 		// Test for http://jira.jboss.com/jira/browse/JBIDE-999
 		IFile file = project.getFile("WebContent/varAttributes.xhtml");
 		int number = getMarkersNumber(file);
 		assertTrue("Problem marker was found in varAttributes.xhtml file. Validator did not recognize 'var' attribute.", number == 0);
 	}
 
 	public void testJiraJbide1696() throws CoreException {
 		//getSeamProject(project);
 		
 		// Test for http://jira.jboss.com/jira/browse/JBIDE-1696
 		IFile subclassComponentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SubclassTestComponent.java");
 		assertMarkerIsCreated(subclassComponentFile, MARKER_TYPE, "Stateful component \"testComponentJBIDE1696\" must have a method marked @Remove", 25);
 		IFile superclassComponentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SuperclassTestComponent.java");
 		IFile superclassComponentFileWithRemove = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SuperclassTestComponent.withRemove");
 		try{
 			superclassComponentFile.setContents(superclassComponentFileWithRemove.getContents(), true, false, null);
 		}catch(Exception e){
 			JUnitUtils.fail("Error during changing 'SuperclassTestComponent.java' content to 'SuperclassTestComponent.withRemove'", e);
 		}
 		refreshProject(project);
 		int number = getMarkersNumber(subclassComponentFile);
 		assertTrue("We changed super class of component but it still don't see changes.", number == 0);
 	}
 
 	public void testJiraJbide1631() throws CoreException {
 		// Test for http://jira.jboss.com/jira/browse/JBIDE-1631
 		String jbide1631XHTMLFile = "WebContent/JBIDE-1631.xhtml";
 		String jbide1631XHTMLFile2 = "WebContent/JBIDE-1631.1";
 		
 		assertMarkerIsCreated(jbide1631XHTMLFile, jbide1631XHTMLFile2, "\"foo1\" cannot be resolved", 16 );
 		assertMarkerIsCreated(project.getFile(jbide1631XHTMLFile), MARKER_TYPE, "\"foo2\" cannot be resolved", 16 );
 	}
 
 	public void testComponentsValidator() {
 		IFile bbcComponentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/BbcComponent.java");
 		IFile statefulComponentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.java");
 		IFile componentsFile = project.getFile("WebContent/WEB-INF/components.xml");
 
 		int number = getMarkersNumber(bbcComponentFile);
 		assertEquals("Problem marker was found in BbcComponent.java file", 0, number);
 
 		number = getMarkersNumber(statefulComponentFile);
 		assertEquals("Problem marker was found in StatefulComponent.java file", 0, number);
 
 		number = getMarkersNumber(componentsFile);
 		assertEquals("Problem marker was found in components.xml file", 0, number);
 
 		// Duplicate component name
 		System.out.println("Test - Duplicate component name");
 
 		IFile bbcComponentFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/BbcComponent.2");
 		try{
 			bbcComponentFile.setContents(bbcComponentFile2.getContents(), true, false, null);
 			bbcComponentFile.touch(null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'BbcComponent.java' content to " +
 					"'BbcComponent.2'", ex);
 		}
 
 		refreshProject(project);
 
 		number = getMarkersNumber(bbcComponentFile);
 			assertFalse("Problem marker 'Duplicate component name' not found", number == 0);
 
 		String[] messages = getMarkersMessage(bbcComponentFile);
 
 		assertEquals("Problem marker 'Duplicate component name' not found","Duplicate component name: \"abcComponent\"", messages[0]);
 
 		int[] lineNumbers = getMarkersNumbersOfLine(bbcComponentFile);
 
 		assertEquals("Problem marker has wrong line number", 7, lineNumbers[0]);
 
 		// Stateful component does not contain @Remove method
 		System.out.println("Test - Stateful component does not contain @Remove method");
 		
 		IFile statefulComponentFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.2");
 		try{
 			statefulComponentFile.setContents(statefulComponentFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'StatefulComponent.java' content to " +
 					"'StatefulComponent.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(statefulComponentFile);
 		assertFalse("Problem marker 'Stateful component does not contain @Remove method' not found' not found", number == 0);
 		
 		messages = getMarkersMessage(statefulComponentFile);
 		assertEquals("Problem marker 'Stateful component does not contain @Remove method' not found", "Stateful component \"statefulComponent\" must have a method marked @Remove", messages[0]);
 
 		lineNumbers = getMarkersNumbersOfLine(statefulComponentFile);
 
 		assertEquals("Problem marker has wrong line number", 16, lineNumbers[0]);
 
 		// Stateful component does not contain @Destroy method
 		System.out.println("Test - Stateful component does not contain @Destroy method");
 
 		IFile statefulComponentFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.3");
 		try{
 			statefulComponentFile.setContents(statefulComponentFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'StatefulComponent.java' content to " +
 					"'StatefulComponent.3'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(statefulComponentFile);
 		assertFalse("Problem marker 'Stateful component does not contain @Destroy method' not found' not found' not found", number == 0);
 		
 		messages = getMarkersMessage(statefulComponentFile);
 		assertEquals("Problem marker 'Stateful component does not contain @Destroy method' not found", "Stateful component \"statefulComponent\" must have a method marked @Destroy", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(statefulComponentFile);
 		
 		assertEquals("Problem marker has wrong line number", 16, lineNumbers[0]);
 		
 		// Stateful component has wrong scope
 		System.out.println("Test - Stateful component has wrong scope");
 		
 		IFile statefulComponentFile4 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.4");
 		try{
 			statefulComponentFile.setContents(statefulComponentFile4.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'StatefulComponent.java' content to " +
 					"'StatefulComponent.4'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(statefulComponentFile);
 		assertFalse("Problem marker 'Stateful component has wrong scope' not found' not found' not found", number == 0);
 		
 		messages = getMarkersMessage(statefulComponentFile);
 		assertEquals("Problem marker 'Stateful component has wrong scope' not found", "Stateful component \"statefulComponent\" should not have org.jboss.seam.ScopeType.PAGE, nor org.jboss.seam.ScopeType.STATELESS", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(statefulComponentFile);
 		
 		assertEquals("Problem marker has wrong line number", 16, lineNumbers[0]);
 		
 		// Component class name cannot be resolved to a type
 		System.out.println("Test - Component class name cannot be resolved to a type");
 		
 		IFile componentsFile2 = project.getFile("WebContent/WEB-INF/components.2");
 		
 		try{
 			componentsFile.setContents(componentsFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'components.xml' content to " +
 					"'components.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(componentsFile);
 		assertFalse("Problem marker 'Component class name cannot be resolved to a type' was not found", number == 0);
 		
 		messages = getMarkersMessage(componentsFile);
 		assertEquals("Problem marker 'Component class name cannot be resolved to a type' was not found", "\"org.domain.SeamWebWarTestProject.session.StateComponent\" cannot be resolved to a type", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(componentsFile);
 		
 		assertEquals("Problem marker has wrong line number", 15, lineNumbers[0]);
 
 		// Component class does not contain setter for property
 		System.out.println("Test - Component class does not contain setter for property");
 		
 		IFile componentsFile3 = project.getFile("WebContent/WEB-INF/components.3");
 		
 		try{
 			componentsFile.setContents(componentsFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'components.xml' content to " +
 					"'components.3'", ex);
 		}
 		
 		IFile statefulComponentFile5 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.5");
 
 		try{
 			statefulComponentFile.setContents(statefulComponentFile5.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'StatefulComponent.java' content to " +
 					"'StatefulComponent.5'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(componentsFile);
 		assertFalse("Problem marker 'Component class does not contain setter for property' not found' not found' not found", number == 0);
 		
 		messages = getMarkersMessage(componentsFile);
 		assertEquals("Problem marker 'Component class does not have a setter or a field for the property' not found", "Class \"StatefulComponent\" of component \"statefulComponent\" does not have a setter or a field for the property \"abc\"", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(componentsFile);
 		
 		assertEquals("Problem marker has wrong line number", 16, lineNumbers[0]);
 		
 		// resolve error in BbcComponent.java
 		IFile bbcComponentFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/BbcComponent.3");
 		try{
 			bbcComponentFile.setContents(bbcComponentFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'BbcComponent.java' content to " +
 					"'BbcComponent.3'", ex);
 		}
 	}
 	
 	public void testEntitiesValidator() {
 		IFile abcEntityFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/entity/abcEntity.java");
 		
 		int number = getMarkersNumber(abcEntityFile);
 		assertTrue("Problem marker was found in abcEntity.java", number == 0);
 		
 		// Entity component has wrong scope
 		System.out.println("Test - Entity component has wrong scope");
 		
 		IFile abcEntityFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/entity/abcEntity.2");
 		try{
 			abcEntityFile.setContents(abcEntityFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcEntity.java' content to " +
 					"'abcEntity.2'", ex);
 		}
 
 		refreshProject(project);
 
 		number = getMarkersNumber(abcEntityFile);
 		assertFalse("Problem marker 'Entity component has wrong scope' was not found'", number == 0);
 
 		String[] messages = getMarkersMessage(abcEntityFile);
 		assertEquals("Problem marker 'Entity component has wrong scope' was not found", "Entity component \"abcEntity\" should not have org.jboss.seam.ScopeType.STATELESS", messages[0]);
 
 		int[] lineNumbers = getMarkersNumbersOfLine(abcEntityFile);
 
 		assertEquals("Problem marker has wrong line number", 15, lineNumbers[0]);
 	}
 
 	public void testComponentLifeCycleMethodsValidator() throws CoreException {
 		final String TARGET_FILE_NAME 
 			= "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.java";
 		
 		IFile componentsFile = project.getFile("WebContent/WEB-INF/components.xml");
 
 		final String NEW_CONTENT_FILE_NAME6 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.6";
 
 		// Duplicate @Destroy method
 		System.out.println("Test - Duplicate @Destroy method");
 	
 		refreshProject(project);
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME, 
 				NEW_CONTENT_FILE_NAME6, 
 				".*\"destroyMethod\".*", 34);
 		
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME, ".*\"destroyMethod2\"", 39);
 
 		// Duplicate @Create method
 		System.out.println("Test - Duplicate @Create method");
 
 		final String NEW_CONTENT_FILE_NAME7 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.7";
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME7, ".*@Create.*\"createMethod\".*", 36);
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME, ".*@Create.*\"createMethod2\".*", 41);
 
 		// Duplicate @Unwrap method
 		System.out.println("Test - Duplicate @Unwrap method");
 
 		final String NEW_CONTENT_FILE_NAME8 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.8";
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME8, ".*@Unwrap.*\"unwrapMethod\".*", 40);
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME, ".*@Unwrap.*\"unwrapMethod2\".*", 45);
 
 		IFile componentsFileWithoutSTComponent = project.getFile("WebContent/WEB-INF/components.5");
 		try {
 			componentsFile.setContents(componentsFileWithoutSTComponent.getContents(), true, false, null);
 		} catch(Exception ex) {
 			JUnitUtils.fail("Error in changing 'components.xml' content to " +
 					"'components.5'", ex);
 		}
 		refreshProject(project);
 
 		// Only JavaBeans and stateful session beans support @Destroy methods
 		System.out.println("Test - Only JavaBeans and stateful session beans support @Destroy methods");
 
 		final String NEW_CONTENT_FILE_NAME9 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.9";
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME9, ".*@Destroy.*\"destroyMethod\".*", 25);
 
 		// Only component class can have @Create method
 		System.out.println("Test - Only component class can have @Create method");
 
 		final String NEW_CONTENT_FILE_NAME10 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.10";
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME10, ".*@Create.*\"createMethod\".*", 25);
 
 		// Only component class can have @Unwrap method
 		System.out.println("Test - Only component class can have @Unwrap method");
 
 		final String NEW_CONTENT_FILE_NAME11 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.11";
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME11, "Only component class can have @Unwrap method \"unwrapMethod\"", 26);
 		
 		// Only component class can have @Observer method
 		System.out.println("Test - Only component class can have @Observer method");
 		
 		final String NEW_CONTENT_FILE_NAME12 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.12";
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME12, "Only component class can have @Observer method \"observerMethod\"", 26);
 
 		// Duplicate @Remove method
 
 		final String NEW_CONTENT_FILE_NAME1 = "src/action/org/domain/SeamWebWarTestProject/session/StatefulComponent.1";
 
 		//assertTrue("Wrong number of problem markers", lineNumbers.length == messages.length && messages.length == 2);
 
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,NEW_CONTENT_FILE_NAME1, "Duplicate @Remove method \"removeMethod1\"", 18);
 		assertMarkerIsCreated(
 				TARGET_FILE_NAME,"Duplicate @Remove method \"removeMethod2\"", 22);
 
 		//IFile componentsFileWithSTComponent = project.getFile("WebContent/WEB-INF/components.2");
 		try {
 			componentsFile.setContents(componentsFileWithoutSTComponent.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'components.xml' content to " +
 					"'components.6'", ex);
 		}
 	}
 
 	/**
 	 * @param statefulComponentFile
 	 * @param string
 	 * @param i
 	 * @throws CoreException 
 	 */
 	protected void assertMarkerIsCreated(String targetPath, String newContentPath,
 			String pattern, int line) throws CoreException {
 		
 		IFile newContentFile = project.getFile(newContentPath);
 		IFile targetFile = project.getFile(targetPath);
 		targetFile.setContents(newContentFile.getContents(), true, false, null);
 		refreshProject(project);
 		assertMarkerIsCreated(targetFile, MARKER_TYPE, pattern, line);
 	}
 	
 	/**
 	 * @param statefulComponentFile
 	 * @param string
 	 * @param i
 	 * @throws CoreException 
 	 */
 	protected void assertMarkerIsCreated(String targetPath,
 			String pattern, int line) throws CoreException {
 		
 		IFile targetFile = project.getFile(targetPath);
 		assertMarkerIsCreated(targetFile, MARKER_TYPE, pattern, line);
 	}
 
 	/**
 	 * The validator should check duplicate @Remove methods only in stateful session bean component
 	 * This method tests usual component (not stateful sessian bean) with two @Remove methods. It must not have error markers.  
 	 */
 	public void testDuplicateRemoveMethodInComponent() {
 		getSeamProject(project);
 		IFile componentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/UsualComponent.java");
 		int number = getMarkersNumber(componentFile);
 		assertEquals("Problem marker was found in UsualComponent.java file", 0, number);
 	}
 
 	public void testFactoriesValidator() {
 		IFile Component12File = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/Component12.java");
 		
 		refreshProject(project);
 		
 		int number = getMarkersNumber(Component12File);
 		assertTrue("Problem marker was found in Component12.java", number == 0);
 
 		// Unknown factory name
 		System.out.println("Test - Unknown factory name");
 		
 		IFile Component12File2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/Component12.2");
 		try{
 			Component12File.setContents(Component12File2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'Component12File2.java' content to " +
 					"'Component12File2.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(Component12File);
 		assertFalse("Problem marker 'Unknown factory name' was not found", number == 0);
 		
 		String[] messages = getMarkersMessage(Component12File);
 
 		assertEquals("Problem marker 'Unknown factory name' was not found", "Factory method \"messageList2\" with a void return type must have an associated @Out/Databinder", messages[0]);
 		
 		int[] lineNumbers = getMarkersNumbersOfLine(Component12File);
 		
 		assertEquals("Problem marker has wrong line number", 24, lineNumbers[0]);
 	}
 	
 	public void testBijectionsValidator() {
 		IFile selectionTestFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionTest.java");
 		IFile selectionIndexTestFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionIndexTest.java");
 		
 		refreshProject(project);
 		
 		int number = getMarkersNumber(selectionTestFile);
 		assertEquals("Problem marker was found in SelectionIndexTest.java", 0, number);
 		
 		number = getMarkersNumber(selectionIndexTestFile);
 		assertEquals("Problem marker was found in SelectionIndexTest.java", 0, number);
 
 		// Multiple data binder
 		System.out.println("Test - Multiple data binder");
 		
 		IFile selectionTestFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionTest.2");
 		try{
 			selectionTestFile.setContents(selectionTestFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'SelectionTest.java' content to " +
 					"'SelectionTest.2'", ex);
 		}
 
 		IFile selectionIndexTestFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionIndexTest.2");
 		try{
 			selectionIndexTestFile.setContents(selectionIndexTestFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'SelectionIndexTest.java' content to " +
 					"'SelectionIndexTest.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(selectionTestFile);
 		assertFalse("Problem marker 'Multiple data binder' was not found", number == 0);
 
 		String[] messages = getMarkersMessage(selectionTestFile);
 		assertTrue("Problem marker 'Multiple data binder", messages[0].startsWith("@DataModelSelection and @DataModelSelectionIndex without name of the DataModel requires the only one @DataModel in the component"));
 
 		int[] lineNumbers = getMarkersNumbersOfLine(selectionTestFile);
 		
 		assertTrue("Wrong number of problem markers", lineNumbers.length == messages.length && messages.length == 2);
 		
 		assertTrue("Problem marker has wrong line number", lineNumbers[0] == 21 || lineNumbers[0] == 24);
 		assertTrue("Problem marker has wrong line number", lineNumbers[0] == 21 || lineNumbers[0] == 24);
 
 		number = getMarkersNumber(selectionIndexTestFile);
 		assertFalse("Problem marker 'Multiple data binder' was not found", number == 0);
 		
 		messages = getMarkersMessage(selectionIndexTestFile);
 		assertTrue("Problem marker 'Multiple data binder", messages[0].startsWith("@DataModelSelection and @DataModelSelectionIndex without name of the DataModel requires the only one @DataModel in the component"));
 
 		lineNumbers = getMarkersNumbersOfLine(selectionIndexTestFile);
 		
 		assertTrue("Wrong number of problem markers", lineNumbers.length == messages.length && messages.length == 2);
 		
 		assertTrue("Problem marker has wrong line number", lineNumbers[0] == 21 || lineNumbers[0] == 24);
 		assertTrue("Problem marker has wrong line number", lineNumbers[0] == 21 || lineNumbers[0] == 24);
 		
 		// Unknown @DataModel/@Out name
 		System.out.println("Test - Unknown @DataModel/@Out name");
 		
 		IFile selectionTestFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionTest.3");
 		try{
 			selectionTestFile.setContents(selectionTestFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'SelectionTest.java' content to " +
 					"'SelectionTest.3'", ex);
 		}
 
 		IFile selectionIndexTestFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/SelectionIndexTest.3");
 		try{
 			selectionIndexTestFile.setContents(selectionIndexTestFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'SelectionIndexTest.java' content to " +
 					"'SelectionIndexTest.3'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(selectionTestFile);
 		assertFalse("Problem marker 'Unknown @DataModel/@Out name' not found' not found' not found' not found", number == 0);
 
 		messages = getMarkersMessage(selectionTestFile);
 		assertTrue("Problem marker 'Unknown @DataModel/@Out name", messages[0].startsWith("Unknown @DataModel/@Out name: \"messageList2\""));
 
 		lineNumbers = getMarkersNumbersOfLine(selectionTestFile);
 		
 		assertEquals("Problem marker has wrong line number", 27, lineNumbers[0]);
 		
 		number = getMarkersNumber(selectionIndexTestFile);
 		assertFalse("Problem marker 'Unknown @DataModel/@Out name' not found' not found' not found' not found", number == 0);
 
 		messages = getMarkersMessage(selectionIndexTestFile);
 		assertTrue("Problem marker 'Unknown @DataModel/@Out name", messages[0].startsWith("Unknown @DataModel/@Out name: \"messageList2\""));
 		
 		lineNumbers = getMarkersNumbersOfLine(selectionIndexTestFile);
 
 		assertEquals("Problem marker has wrong line number", 27, lineNumbers[0]);
 	}
 
 	public void testContextVariablesValidator() {
 		modifyPreferences();
 		IPreferenceStore store = SeamCorePlugin.getDefault().getPreferenceStore();
 		System.out.println("UNKNOWN_EL_VARIABLE_NAME value- "+store.getString(SeamPreferences.UNKNOWN_EL_VARIABLE_NAME));
 
 		IFile contextVariableTestFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/ContextVariableTest.java");
 		
 		refreshProject(project);
 		
 		int number = getMarkersNumber(contextVariableTestFile);
 		assertEquals("Problem marker was found in contextVariableTestFile.java", 0, number);
 		
 		// Duplicate variable name
 		System.out.println("Test - Duplicate variable name");
 		
 		IFile contextVariableTestFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/ContextVariableTest.2");
 		try{
 			contextVariableTestFile.setContents(contextVariableTestFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'ContextVariableTest.java' content to " +
 					"'ContextVariableTest.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		String[] messages = getMarkersMessage(contextVariableTestFile);
 		
		assertEquals("Not all problem markers 'Duplicate variable name' was found", 2, messages.length);
 		
 		for(int i=0;i<4;i++)
 			assertEquals("Problem marker 'Duplicate variable name' not found", "Duplicate variable name: \"messageList\"", messages[i]);
 		
 		int[] lineNumbers = getMarkersNumbersOfLine(contextVariableTestFile);
 		
		for(int i=0;i<2;i++)
			assertTrue("Problem marker has wrong line number", (lineNumbers[i] == 36)||(lineNumbers[i] == 41));
 		
 		// Unknown variable name
 		System.out.println("Test - Unknown variable name");
 		
 		IFile contextVariableTestFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/ContextVariableTest.3");
 		try{
 			contextVariableTestFile.setContents(contextVariableTestFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'ContextVariableTest.java' content to " +
 					"'ContextVariableTest.3'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(contextVariableTestFile);
 		assertFalse("Problem marker 'Unknown variable name' not found' not found' not found' not found", number == 0);
 		
 		messages = getMarkersMessage(contextVariableTestFile);
 		
 		assertEquals("Problem marker 'Unknown variable name' not found", "Unknown context variable name: \"messageList5\"", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(contextVariableTestFile);
 		
 		assertEquals("Problem marker has wrong line number", 22, lineNumbers[0]);
 
 	}
 
 	public void testExpressionLanguageValidator() throws CoreException {
 		modifyPreferences();
 		IPreferenceStore store = SeamCorePlugin.getDefault().getPreferenceStore();
 		System.out.println("UNKNOWN_EL_VARIABLE_PROPERTY_NAME value- "+store.getString(SeamPreferences.UNKNOWN_EL_VARIABLE_PROPERTY_NAME));
 		System.out.println("UNKNOWN_VARIABLE_NAME value- "+store.getString(SeamPreferences.UNKNOWN_VARIABLE_NAME));
 		System.out.println("UNPAIRED_GETTER_OR_SETTER value- "+store.getString(SeamPreferences.UNPAIRED_GETTER_OR_SETTER));
 
 		IFile abcComponentXHTMLFile = project.getFile("WebContent/abcComponent.xhtml");
 		IFile abcComponentFile = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/AbcComponent.java");
 		
 		refreshProject(project);
 		
 		int number = getMarkersNumber(abcComponentXHTMLFile);
 		assertEquals("Problem marker was found in abcComponent.xhtml", 0, number);
 		
 		number = getMarkersNumber(abcComponentFile);
 		assertEquals("Problem marker was found in AbcComponent.java", 0, number);
 
 		// Context variable cannot be resolved
 		System.out.println("Test - Context variable cannot be resolved");
 
 		IFile abcComponentXHTMLFile2 = project.getFile("WebContent/abcComponent.2");
 		try{
 			abcComponentXHTMLFile.setContents(abcComponentXHTMLFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcComponent.xhtml' content to " +
 					"'abcComponent.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(abcComponentXHTMLFile);
 		assertFalse("Problem marker 'Context variable cannot be resolved' not found' not found' not found' not found", number == 0);
 		
 		String[] messages = getMarkersMessage(abcComponentXHTMLFile);
 		
 		assertEquals("Problem marker 'Context variable cannot be resolved' not found", "\"bcComponent\" cannot be resolved", messages[0]);
 		
 		int[] lineNumbers = getMarkersNumbersOfLine(abcComponentXHTMLFile);
 		
 		assertEquals("Problem marker has wrong line number", 22, lineNumbers[0]);
 		
 		// Property cannot be resolved
 		System.out.println("Test - Property cannot be resolved");
 
 		IFile abcComponentXHTMLFile3 = project.getFile("WebContent/abcComponent.3");
 		try{
 			abcComponentXHTMLFile.setContents(abcComponentXHTMLFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcComponent.xhtml' content to " +
 					"'abcComponent.3'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(abcComponentXHTMLFile);
 		assertFalse("Problem marker 'Property cannot be resolved' was not found", number == 0);
 		
 		messages = getMarkersMessage(abcComponentXHTMLFile);
 		
 		assertEquals("Problem marker 'Property cannot be resolved' was not found", "\"actionType2\" cannot be resolved", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(abcComponentXHTMLFile);
 		
 		assertEquals("Problem marker has wrong line number", 22, lineNumbers[0]);
 		
 		// Unpaired Getter/Setter
 		System.out.println("Test - Unpaired Getter/Setter");
 		
 		IFile abcComponentXHTMLFile4 = project.getFile("WebContent/abcComponent.4");
 		try{
 			abcComponentXHTMLFile.setContents(abcComponentXHTMLFile4.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcComponent.xhtml' content to " +
 					"'abcComponent.4'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(abcComponentXHTMLFile);
 		assertEquals("Problem marker was found in abcComponent.xhtml", 0, number);
 
 		IFile abcComponentFile2 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/AbcComponent.2");
 		try{
 			abcComponentFile.setContents(abcComponentFile2.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcComponent.java' content to " +
 					"'abcComponent.2'", ex);
 		}
 		
 		refreshProject(project);
 		
 		number = getMarkersNumber(abcComponentXHTMLFile);
 		assertFalse("Problem marker 'Unpaired Getter/Setter' was not found' not found' not found' not found", number == 0);
 
 		messages = getMarkersMessage(abcComponentXHTMLFile);
 
 		assertEquals("Problem marker 'Unpaired Getter/Setter' was not found", "Property \"actionType\" has only Setter. Getter is missing.", messages[0]);
 		
 		lineNumbers = getMarkersNumbersOfLine(abcComponentXHTMLFile);
 		
 		assertEquals("Problem marker has wrong line number", 22, lineNumbers[0]);
 
 		IFile abcComponentFile3 = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/AbcComponent.3");
 		try{
 			abcComponentFile.setContents(abcComponentFile3.getContents(), true, false, null);
 		}catch(Exception ex){
 			JUnitUtils.fail("Error in changing 'abcComponent.java' content to " +
 					"'abcComponent.3'", ex);
 		}
 
 		refreshProject(project);
 
 		number = getMarkersNumber(abcComponentXHTMLFile);
 		assertFalse("Problem marker 'Unpaired Getter/Setter' was not found", number == 0);
 
 		messages = getMarkersMessage(abcComponentXHTMLFile);
 
 		assertEquals("Problem marker 'Unpaired Getter/Setter' was not found", "Property \"actionType\" has only Getter. Setter is missing.", messages[0]);
 
 		lineNumbers = getMarkersNumbersOfLine(abcComponentXHTMLFile);
 
 		assertEquals("Problem marker has wrong line number", 22, lineNumbers[0]);
 	} 	
 	
 	public void testInheritedMethods() {
 		ISeamProject seamProject = getSeamProject(project);
 
 		ISeamComponent c = seamProject.getComponent("inheritedComponent");
 		assertNotNull("Component inheritedComponent is not found", c);
 
 		Set<ISeamComponentMethod> ms = c.getMethodsByType(SeamComponentMethodType.DESTROY);
 		assertTrue("Seam tools does not see @Destroy-annotated method declared in super class", ms.size() > 0);
 
 		ms = c.getMethodsByType(SeamComponentMethodType.REMOVE);
 		assertTrue("Seam tools does not see @Remove-annotated method declared in super class", ms.size() > 0);
 
 		IFile f = project.getFile("src/action/org/domain/SeamWebWarTestProject/session/InheritedComponent.java");
 		int errorsCount = getMarkersNumber(f);
 		assertEquals("Seam tools validator does not see annotated methods declared in super class", 0, errorsCount);
 	}
 
 	private void modifyPreferences(){
 		IPreferenceStore store = SeamCorePlugin.getDefault().getPreferenceStore();
 		store.putValue(SeamPreferences.UNKNOWN_EL_VARIABLE_NAME, SeamPreferences.ERROR);
 		store.putValue(SeamPreferences.UNKNOWN_EL_VARIABLE_PROPERTY_NAME, SeamPreferences.ERROR);
 		store.putValue(SeamPreferences.UNKNOWN_VARIABLE_NAME, SeamPreferences.ERROR);
 		store.putValue(SeamPreferences.UNPAIRED_GETTER_OR_SETTER, SeamPreferences.ERROR);
 
 		if(store instanceof IPersistentPreferenceStore) {
 			try {
 				((IPersistentPreferenceStore)store).save();
 			} catch (IOException e) {
 				SeamCorePlugin.getPluginLog().logError(e);
 			}
 		}
 	}
 	
 	private void refreshProject(IProject project){
 		try {
 			project.refreshLocal(IResource.DEPTH_INFINITE, null);
 			JobUtils.waitForIdle();
 			JobUtils.delay(2000);
 		} catch (CoreException e) {
 			// ignore
 		}
 	}
 	
 	public static int getMarkersNumber(IResource resource){
 		try{
 			IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
 			for(int i=0;i<markers.length;i++){
 				System.out.println("Marker - "+markers[i].getAttribute(IMarker.MESSAGE, ""));
 			}
 			return markers.length;
 		}catch(CoreException ex){
 			JUnitUtils.fail("Can'r get problem markers", ex);
 		}
 		return -1;
 	}
 }
