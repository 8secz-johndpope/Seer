 package org.jboss.tools.vpe.ui.bot.test;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.List;
 import java.util.Properties;
 import org.eclipse.core.runtime.FileLocator;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
 import org.eclipse.swtbot.swt.finder.SWTBot;
 import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
 import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
 import org.jboss.tools.jst.jsp.jspeditor.JSPMultiPageEditor;
 import org.jboss.tools.test.TestProperties;
 import org.jboss.tools.ui.bot.test.Activator;
 import org.jboss.tools.ui.bot.test.JBTSWTBotTestCase;
 import org.jboss.tools.ui.bot.test.SWTBotJSPMultiPageEditor;
 import org.jboss.tools.ui.bot.test.WidgetVariables;
 import org.jboss.tools.vpe.editor.VpeController;
 import org.jboss.tools.vpe.editor.VpeEditorPart;
 import org.jboss.tools.vpe.editor.mapping.VpeNodeMapping;
 import org.mozilla.interfaces.nsIDOMDocument;
 import org.mozilla.interfaces.nsIDOMElement;
 import org.mozilla.interfaces.nsIDOMNode;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 
 public abstract class VPEAutoTestCase extends JBTSWTBotTestCase{
 	
 	protected static Properties projectProperties;
 	protected static final String TEST_PAGE = "inputUserName.jsp";
 	protected static String PROJECT_PROPERTIES = "projectProperties.properties";
 	
 	/**
 	 * Variable defines JBoss EAP 4.3 server location on a file system
 	 */
 	
 	protected static String JBOSS_EAP_HOME;
 	protected static String JBT_TEST_PROJECT_NAME;
 	
 	/* (non-Javadoc)
 	 * This static block read properties from 
 	 * org.jboss.tools.vpe.ui.bot.test/resources/projectProperties.properties file
 	 * and set up parameters for project which you would like to create. You may change a number of parameters
 	 * in static block and their values in property file.
 	 */
 	
 	static {
 		try {
 			InputStream inputStream = VPEAutoTestCase.class.getResourceAsStream("/"+PROJECT_PROPERTIES);
 			projectProperties = new TestProperties();
 			projectProperties.load(inputStream);
 			inputStream.close();
 		} 
 		catch (IOException e) {
 			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Can't load properties from " + PROJECT_PROPERTIES + " file", e);
 			Activator.getDefault().getLog().log(status);
 			e.printStackTrace();
 		}
 		catch (IllegalStateException e) {
 			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Property file " + PROJECT_PROPERTIES + " was not found", e);
 			Activator.getDefault().getLog().log(status);
 			e.printStackTrace();
 		}
 		JBOSS_EAP_HOME = projectProperties.getProperty("JBossEap4.3");
 		JBT_TEST_PROJECT_NAME = projectProperties.getProperty("JSFProjectName");
 	}
 	
 	/**
 	 * @see #clearWorkbench()
 	 * @see #createJSFProject(String)
 	 */
 	
 	protected void setUp() throws Exception {
 		super.setUp();
 		clearWorkbench();
 		SWTBot innerBot = bot.viewByTitle(WidgetVariables.PACKAGE_EXPLORER).bot();
 		SWTBotTree tree = innerBot.tree();
 		try {
 			tree.getTreeItem(JBT_TEST_PROJECT_NAME);
 		} catch (WidgetNotFoundException e) {
 			createJSFProject(JBT_TEST_PROJECT_NAME);
 		}
 	}
 	
 	/**
 	 * Tears down the fixture. Verify Error Log. Close all dialogs which may be not closed
 	 * after test executing.
 	 * @see #clearWorkbench()
 	 */
 	
 	@Override
 	protected void tearDown() throws Exception {
 		clearWorkbench();
 		super.tearDown();
 	}
 	
 	/**
 	 * Create JSF Project with <b>jsfProjectName</b>
 	 * @param jsfProjectName - name of created project
 	 */
 	
 	protected void createJSFProject(String jsfProjectName){
 		bot.menu("File").menu("New").menu("Other...").click();
 		bot.shell("New").activate();
 		SWTBotTree tree = bot.tree();
 		delay();
 		tree.expandNode("JBoss Tools Web").expandNode("JSF").select("JSF Project");
 		bot.button("Next >").click();
 		bot.textWithLabel("Project Name*").setText(jsfProjectName);
 		bot.comboBoxWithLabel("Template*").setSelection("JSFKickStartWithoutLibs");
 		bot.button("Next >").click();
 		try {
 			bot.comboBoxWithLabel("Runtime*").setSelection("jboss-eap Runtime");
 			delay();
 			bot.button("Finish").click();
 			try {
 				bot.button("Yes").click();
 				openErrorLog();
 				openPackageExplorer();
 //				openProgressStatus();
 			} catch (WidgetNotFoundException e) {
 			}
		} catch (WidgetNotFoundException e) {
 			bot.button(0).click();
 			SWTBotTree  innerTree = bot.tree();
 			delay();
 			innerTree.expandNode("JBoss Enterprise Middleware").select("JBoss Enterprise Application Platform 4.3 Runtime");
 			bot.button("Next >").click();
 			bot.textWithLabel("Home Directory").setText(JBOSS_EAP_HOME);
 			bot.button("Finish").click();
 			delay();
 			bot.button("Finish").click();
 			try {
 				bot.button("Yes").click();
 				openErrorLog();
 				openPackageExplorer();
 //				openProgressStatus();
 			} catch (WidgetNotFoundException e2) {
 			}
 		}
 		try {
 			waitForBlockingJobsAcomplished(60*1000L, BUILDING_WS);
 		} catch (InterruptedException e) {
 		}
 		setException(null);
 	}
 	
 	/**
 	 * Test content of elements from <b>editor</b> by IDs.<p>
 	 * Tested elements from source editor should have id's attributes that
 	 * correspond to expected one from <b>expectedVPEContentFile</b>.
 	 * @param expectedVPEContentFile - file name, for example, <i>"ShowNonVisualTags.xml"</i>
 	 * with expected VPE DOM Elements and id's attributes correspond to source <b>editor</b> element
 	 * @param editor - {@link JSPMultiPageEditor} that contains source code with tested elements and current id.
 	 * @throws Throwable
 	 * @see SWTBotJSPMultiPageEditor
 	 * @see Throwable
 	 */
 	@Deprecated
 	protected void performContentTestByIDs(String expectedVPEContentFile, SWTBotJSPMultiPageEditor editor) throws Throwable{	
 		
 		JSPMultiPageEditor multiPageEditor = editor.getJspMultiPageEditor();
 		assertNotNull(multiPageEditor);
 		
 		VpeController controller = TestUtil.getVpeController(multiPageEditor);
 		
 		String expectedVPEContentFilePath = getPathToResources(expectedVPEContentFile);
 		
 		File xmlTestFile = new File (expectedVPEContentFilePath);
 		
 		Document xmlTestDocument = TestDomUtil.getDocument(xmlTestFile);
 		assertNotNull("Can't get test file, possibly file not exists "+xmlTestFile,xmlTestDocument); //$NON-NLS-1$
 
 		List<String> ids = TestDomUtil.getTestIds(xmlTestDocument);
 
 		for (String id : ids) {
 
 			compareElements(controller, xmlTestDocument, id, id);
 		}
 
 		if (getException() != null) {
 			throw getException();
 		}
 	
 	}
 	
 	private void compareElements(VpeController controller,
 			Document xmlTestDocument, String elementId, String xmlTestId)
 			throws ComparisonException {
 
 		// get element by id
 		nsIDOMElement vpeElement = findElementById(controller, elementId);
 		assertNotNull("Cann't find element with id="+elementId,vpeElement); //$NON-NLS-1$
 
 		// get test element by id - get <test id="..." > element and get his
 		// first child
 		Element xmlModelElement = TestDomUtil.getFirstChildElement(TestDomUtil
 				.getElemenById(xmlTestDocument, xmlTestId));
 
 		assertNotNull(xmlModelElement);
 
 		// compare DOMs
 		try {
 			TestDomUtil.compareNodes(vpeElement, xmlModelElement);
 		} catch (ComparisonException e) {
 			fail(e.getMessage());
 		}
 
 	}
 
 	private nsIDOMElement findElementById(VpeController controller,
 			String elementId) {
 
 		Element sourceElement = findSourceElementById(controller, elementId);
 
 		VpeNodeMapping nodeMapping = controller.getDomMapping().getNodeMapping(
 				sourceElement);
 
 		if (nodeMapping == null)
 			return null;
 
 		return (nsIDOMElement) nodeMapping.getVisualNode();
 	}
 	
 	private Element findSourceElementById(VpeController controller,
 			String elementId) {
 
 		return getSourceDocument(controller).getElementById(elementId);
 	}
 	
 	private Document getSourceDocument(VpeController controller) {
 		return controller.getSourceBuilder().getSourceDocument();
 	}
 	
 	protected String getPathToResources(String testPage) throws IOException {
 		String filePath = FileLocator.toFileURL(Platform.getBundle(Activator.PLUGIN_ID).getEntry("/")).getFile()+"resources/"+testPage;
 		File file = new File(filePath);
 		if (!file.isFile()) {
 			filePath = FileLocator.toFileURL(Platform.getBundle(Activator.PLUGIN_ID).getEntry("/")).getFile()+testPage;
 		}
 		return filePath;
 	}
 	
 	@Override
 	protected void activePerspective() {
 		if (!bot.perspectiveByLabel("Web Development").isActive()) {
 			bot.perspectiveByLabel("Web Development").activate();
 		}
 	}
 	
 	protected void openPalette(){
 		try {
 			bot.viewByTitle(WidgetVariables.PALETTE);
 		} catch (WidgetNotFoundException e) {
 			bot.menu("Window").menu("Show View").menu("Other...").click();
 			SWTBotTree viewTree = bot.tree();
 			delay();
 			viewTree.expandNode("JBoss Tools Web").expandNode(
 					WidgetVariables.PALETTE).select();
 			bot.button("OK").click();
 		}
 	}
 	
 	/**
 	 * Close all dialogs and editors, which may be not closed
 	 * after test executing.
 	 * @see #isUnuseDialogOpened()
 	 * @see #closeUnuseDialogs()
 	 */
 	
 	protected void clearWorkbench(){
 		while (isUnuseDialogOpened()) {
 			closeUnuseDialogs();
 		}
 		List<? extends SWTBotEditor> editors = bot.editors();
 		if (editors != null) {
 			for (int i = 0; i < editors.size(); i++) {
 				editors.get(i).close();
 			}
 		}
 	}
 	
 	/**
 	 * Test content for elements from all VPE DOM that are nested with <i>BODY</i> descriptor
 	 * @param expectedVPEContentFile - file name, for example, <i>"VerificationOfNameSpaces.xml"</i>
 	 * with expected VPE DOM Elements that are nested with <i>BODY</i> descriptor
 	 * @param editor - {@link JSPMultiPageEditor} that contains source code of currently tested page.
 	 * @throws Throwable
 	 */
 	
 	protected void performContentTestByDocument(String expectedVPEContentFile, SWTBotJSPMultiPageEditor editor) throws Throwable{
 		JSPMultiPageEditor multiPageEditor = editor.getJspMultiPageEditor();
 		assertNotNull(multiPageEditor);
 	
 		nsIDOMDocument visualDocument = ((VpeEditorPart)multiPageEditor.getVisualEditor()).getVisualEditor().getDomDocument();
 		
 		String expectedVPEContentFilePath = getPathToResources(expectedVPEContentFile);
 		
 		File xmlTestFile = new File (expectedVPEContentFilePath);
 		
 		Document xmlTestDocument = TestDomUtil.getDocument(xmlTestFile);
 		assertNotNull("Can't get test file, possibly file not exists "+xmlTestFile,xmlTestDocument); //$NON-NLS-1$
 		
 		compareDocuments(visualDocument, xmlTestDocument);
 
 	}
 	
 	private void compareDocuments (nsIDOMDocument visualDocument, Document xmlTestDocument) throws ComparisonException{
 		nsIDOMNode visualBodyNode = visualDocument.getElementsByTagName("BODY").item(0);
 		Node testBodyNode = xmlTestDocument.getElementsByTagName("BODY").item(0);
 		TestDomUtil.compareNodes(visualBodyNode, testBodyNode);
 	}
 	
 	/**
 	 * Try to close all unnecessary dialogs, that could prevent next tests fails
 	 */
 	
 	protected abstract void closeUnuseDialogs();
 
 	/**
 	 * Verify if any dialog that should be closed is opened
 	 */
 	
 	protected abstract boolean isUnuseDialogOpened();
 	
 }
