 package com.objogate.wl.driver.tests;
 
 import javax.swing.JFileChooser;
 import javax.swing.JLabel;
 import java.awt.Component;
 import java.io.File;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 import junit.framework.Assert;
 import org.hamcrest.Matcher;
 import static org.hamcrest.MatcherAssert.assertThat;
 import org.hamcrest.Matchers;
 import static org.hamcrest.Matchers.equalTo;
 import static org.hamcrest.Matchers.not;
 import org.junit.Before;
 import org.junit.Test;
 import com.objogate.wl.UI;
 import com.objogate.wl.driver.JFileChooserDriver;
 import com.objogate.wl.probe.ComponentIdentity;
 import com.objogate.wl.probe.RecursiveComponentFinder;
 
 public class JFileChooserDriverTest extends AbstractComponentDriverTest<JFileChooserDriver> {
     private JFileChooser chooser;
     private File testFile = new File("build/grandparent.dir/parent.dir/child.dir/", "test.file");
     private ModalDialogShower modalDialogShower;
 
     @Before
     public void setUp() throws Exception {
         view(new JLabel(getClass().getName()));
         File dir = testFile.getParentFile();
         delete(dir);
         dir.mkdirs();
         testFile.createNewFile();
 
         chooser = new JFileChooser(dir);
         chooser.setControlButtonsAreShown(true);
         driver = new JFileChooserDriver(gesturePerformer, chooser);
 
         modalDialogShower = new ModalDialogShower(chooser);
     }
 
     private void delete(File dir) {
         File[] files = dir.listFiles();
         if (files != null)
             for (File file : files) {
                 if (file.isDirectory())
                     delete(file);
 
                 file.delete();
             }
     }
 
     public void testPrintOutSubComponents() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
         ComponentIdentity<Component> parentOrOwnerFinder = new ComponentIdentity<Component>(chooser);
         RecursiveComponentFinder<Component> finder = new RecursiveComponentFinder<Component>(Component.class, Matchers.anything(), parentOrOwnerFinder);
         finder.probe();
         for (Component component : finder.components()) {
             System.out.println("component = " + component);
         }
         driver.cancel();
 
         modalDialogShower.waitForModalDialogToReturn();
     }
 
     @Test
     public void testCanClickCancelButton() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
         driver.cancel();
 
         int result = modalDialogShower.waitForModalDialogToReturn();
 
         assertThat(result, equalTo(JFileChooser.CANCEL_OPTION));
     }
 
     @Test
     public void testCanClickDefaultApproveButton() throws InterruptedException {
         modalDialogShower.showInAnotherThread(null);
 
         //The OK button isn't enabled without selecting a file on Auqa / Mac
         driver.selectFile(testFile.getName());
         driver.approve();
 
         int result = modalDialogShower.waitForModalDialogToReturn();
 
         assertThat(result, equalTo(JFileChooser.APPROVE_OPTION));
     }
 
     @Test
     public void testCanClickNamedApproveButton() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
         driver.enterManually("whatever");
         driver.approve();
 
         int result = modalDialogShower.waitForModalDialogToReturn();
 
         assertThat(result, equalTo(JFileChooser.APPROVE_OPTION));
     }
 
     @Test
     public void testCanSelectFile() throws Exception {
         modalDialogShower.showInAnotherThread("Go");
 
         driver.selectFile(testFile.getName());
         driver.approve();
 
         int result = modalDialogShower.waitForModalDialogToReturn();
 
         assertThat(result, equalTo(JFileChooser.APPROVE_OPTION));
         assertThat(chooser.getSelectedFile().getName(), equalTo(testFile.getName()));
     }
 
     @Test
     public void testWillTypeGivenTextIntoTextbox() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
         driver.enterManually("somethingorother");
         driver.approve();
 
         int result = modalDialogShower.waitForModalDialogToReturn();
 
         assertThat(chooser.getSelectedFile().getName(), equalTo("somethingorother"));
         assertThat(result, equalTo(JFileChooser.APPROVE_OPTION));
     }
 
     @Test
     public void testCanNavigateUpTheDirectoryTree() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
        driver.currentDirectory(equalTo(testFile.getParentFile()));
 
         driver.upOneFolder();
 
        driver.currentDirectory(equalTo(testFile.getParentFile().getParentFile()));
 
         driver.upOneFolder();
 
        driver.currentDirectory(equalTo(testFile.getParentFile().getParentFile().getParentFile()));
     }
 
     @Test
     public void testCanNavigateUpAndDownTheDirectoryTree() throws InterruptedException {
         modalDialogShower.showInAnotherThread("Go");
 
         driver.currentDirectory(testFile.getParentFile());
 
         driver.upOneFolder();
         driver.intoDir(testFile.getParentFile().getName());
 
         driver.currentDirectory(testFile.getParentFile());
     }
 
     @Test
     public void testCanClickOnTheHomeButton() throws InterruptedException {
         if (UI.is(UI.AQUA) || UI.is(UI.WINDOWS))
             return;
 
         modalDialogShower.showInAnotherThread(null);
 
         driver.currentDirectory(not(userHome()));
 
         driver.home();
 
         driver.currentDirectory(userHome());
     }
 
     @Test
     public void testCanClickOnTheDesktopButton() throws InterruptedException {
         if (UI.is(UI.AQUA) || UI.is(UI.METAL))
             return;
 
         modalDialogShower.showInAnotherThread("Go");
 
         driver.currentDirectory(not(desktop()));
 
         driver.desktop();
 
         driver.currentDirectory(desktop());
     }
 
     @Test
     public void testCanClickOnTheDocumentsButton() throws InterruptedException {
         if (UI.is(UI.AQUA) || UI.is(UI.METAL))
             return;
 
         modalDialogShower.showInAnotherThread("Go");
 
         driver.currentDirectory(not(documents()));
 
         driver.documents();
 
         driver.currentDirectory(documents());
     }
 
     @Test
     public void testCanCreateNewFolder() throws InterruptedException {
         File testFolder = new File(testFile.getParentFile(), "test.folder");
 
         modalDialogShower.showInAnotherThread("Go");
 
         driver.createNewFolder(testFolder.getName());
 
         modalDialogShower.waitForModalDialogToReturn();
 
         Assert.assertTrue("Folder " + testFolder.getAbsolutePath() + " exists", testFolder.exists());
     }
 
     class ModalDialogShower {
         private final JFileChooser chooser;
         private final CountDownLatch latch = new CountDownLatch(1);
         private int[] results = new int[]{-999};
 
         public ModalDialogShower(JFileChooser chooser) {
             this.chooser = chooser;
         }
 
         public void showInAnotherThread(final String approveButtonText) {
 
             new Thread(new Runnable() {
                 public void run() {
                     results[0] = chooser.showDialog(frame, approveButtonText);
                     latch.countDown();
                 }
             }).start();
         }
 
         public int waitForModalDialogToReturn() throws InterruptedException {
             latch.await(5, TimeUnit.SECONDS);
             return results[0];
         }
     }
     
 //todo these need to be moved into Platform
     private Matcher<File> userHome() {
         return Matchers.equalTo(new File(System.getProperty("user.home")));
     }
 
     private Matcher<File> desktop() {
         return Matchers.equalTo(new File(System.getProperty("user.home") + File.separatorChar + "Desktop"));
     }
 
     private Matcher<File> documents() {
         return Matchers.equalTo(new File(System.getProperty("user.home") + File.separatorChar + "My Documents"));
     }
 }
