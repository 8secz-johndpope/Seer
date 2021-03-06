 package edu.washington.cs.oneswarm.test.integration;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.List;
 import java.util.logging.Logger;
 
 import junit.framework.Assert;
 
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import com.aelitis.azureus.ui.UIFunctionsManager;
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.Selenium;
 
 import edu.washington.cs.oneswarm.f2f.chat.Chat;
 import edu.washington.cs.oneswarm.f2f.chat.ChatDAO;
 import edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarm;
 import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
 import edu.washington.cs.oneswarm.test.util.TestUtils;
 
 public class ChatTest {
 
 	private static Logger logger = Logger.getLogger(ChatTest.class.getName());
 
 	/** The locally running selenium test server. */
 	static Process seleniumServer;
 
 	/** The selenium control interface. */
 	private static Selenium selenium;
 
 	/** The OneSwarm instance with which we will chat. */
 	private static LocalOneSwarm localOneSwarm;
 
 	@BeforeClass
 	public static void setUpClass() throws Exception {
 		seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));
 
 		// Start a local client in this JVM
 		TestUtils.awaitJVMOneSwarmStart();
 
 		// One additional remote client with which we'll chat
 		localOneSwarm = TestUtils.spawnConnectedOneSwarmInstance();
 
 		selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox",
 				TestUtils.JVM_INSTANCE_WEB_UI) {
 			// Fix for bug: http://code.google.com/p/selenium/issues/detail?id=408
         	@Override
 			public void open(String url) {
         		commandProcessor.doCommand("open", new String[] {url,"true"});
         	}
         };
         selenium.start();
 	}
 
 	@Test
 	public void testSendReceiveChat() throws Exception {
 		/*
 		 * Test plan: Send a chat message using the Web UI and verify that it is displayed
 		 * in the web UI of the remote host.
 		 */
 
 		selenium.openWindow("http://127.0.0.1:4000/", "jvm");
 
 		selenium.selectWindow("jvm");
 
 		// Wait for the friends list AJAX load to complete
 		TestUtils.awaitElement(selenium, "//td[2]/div/div");
 
 		// Double-click the first friend in the list, opens chat.
 		selenium.click("//td[2]/div/div");
 		selenium.click("//td[2]/div/div");
 
 		// Send chat message
 		final String chatMessage = "ChatMessage JVM to Local";
 		TestUtils.awaitElement(selenium, "chatTextBox");
 		selenium.focus("chatTextBox");
 		selenium.typeKeys("chatTextBox", chatMessage);
 		selenium.keyPress("chatTextBox", "13");
 
 		// Verify local display of the chat message.
 		new ConditionWaiter(new ConditionWaiter.Predicate() {
 			public boolean satisfied() {
 				return selenium.isTextPresent(chatMessage);
 			}
 		}, 5000).await();
 
 		// Switch to the other instance
 		selenium.openWindow("http://127.0.0.1:3000/", "local");
 		selenium.selectWindow("local");
 
 		// Verify notification presence -- this could take up to 10 seconds since
 		// we have a 10 seconds poll (See {@code FriendListPanel.java}).
 		new ConditionWaiter(new ConditionWaiter.Predicate() {
 			public boolean satisfied() {
 				return selenium.isElementPresent("link=1 unread message");
 			}
 		},15000).await();
 
 		// Click to bring up chat box
 		selenium.click("link=1 unread message");
 
 		// Verify message in chat box
 		new ConditionWaiter(new ConditionWaiter.Predicate() {
 			public boolean satisfied() {
 				return selenium.isTextPresent("ChatMessage JVM to Local");
 			}
 		},5000).await();
 
 		// Finally, verify that this message was stored in our local database
 		List<Chat> storedMessage = ChatDAO.get().getMessagesForUser(localOneSwarm.getPublicKey(),
 				true, 1);
 		Assert.assertEquals(storedMessage.get(0).getMessage(), chatMessage);
 
 		selenium.close();
 	}
 
 	/** Closes the web UI */
 	@After
 	public void tearDownTest() throws Exception {
 		selenium.close();
 	}
 
 	@AfterClass
 	public static void tearDownClass() throws Exception {
 		logger.info("Tearing down test. Quitting JVM instance");
 		// Quit OneSwarm
 		if (UIFunctionsManager.getUIFunctions() != null) {
 			UIFunctionsManager.getUIFunctions().requestShutdown();
 		}
 		logger.info("Sending shutdown to oop instance");
 		localOneSwarm.getCoordinator().addCommand("shutdown");
 		new ConditionWaiter(new ConditionWaiter.Predicate() {
 			public boolean satisfied() {
 				return localOneSwarm.getCoordinator().getPendingCommands().size() == 0;
 			}
 		}, 10000).await();
 		logger.info("selenium.stop()");
 		// Quit browser
 		if (selenium != null) {
 			selenium.stop();
 		}
 		logger.info("selenium server stop");
 		// Quit RC Server
 		if (seleniumServer != null) {
 			seleniumServer.destroy();
 		}
 	}
 
 	/** Boilerplate code for running as executable. */
	public static void main (String [] args) throws IOException {
 		TestUtils.swtCompatibleTestRunner(ChatTest.class);
 	}
 }
