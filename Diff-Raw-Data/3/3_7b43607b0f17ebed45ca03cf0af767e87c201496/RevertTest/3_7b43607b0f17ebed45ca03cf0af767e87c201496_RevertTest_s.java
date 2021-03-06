 package org.tmatesoft.svn.test;
 
 import org.junit.Assert;
 import org.junit.Test;
 import org.tmatesoft.svn.core.*;
 import org.tmatesoft.svn.core.wc.ISVNEventHandler;
 import org.tmatesoft.svn.core.wc.SVNEvent;
 import org.tmatesoft.svn.core.wc.SVNEventAction;
 import org.tmatesoft.svn.core.wc.SVNStatusType;
 import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
 import org.tmatesoft.svn.core.wc2.SvnRevert;
 import org.tmatesoft.svn.core.wc2.SvnStatus;
 import org.tmatesoft.svn.core.wc2.SvnTarget;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 public class RevertTest {
 
     @Test
     public void testRevertCopyWithoutLocalModifications() throws Exception {
         testRevertCopy("testRevertCopyWithoutLocalModifications", false, false, false, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testRevertCopyWithLocalModifications() throws Exception {
         testRevertCopy("testRevertCopyWithLocalModifications", false, false, true, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testRevertCopyWithoutLocalModificationsEvenWithSpecialOption() throws Exception {
         testRevertCopy("testRevertCopyWithoutLocalModificationsEvenWithSpecialOption", false, true, false, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testDontRevertCopyWitLocalTextModificationsSpecialOption() throws Exception {
         testRevertCopy("testDontRevertCopyWitLocalTextModificationsSpecialOption", false, true, true, false, true, SVNStatusType.STATUS_UNVERSIONED, true, SVNStatusType.STATUS_UNVERSIONED);
     }
 
     @Test
     public void testDontRevertCopyWitLocalPropertiesModificationsSpecialOption() throws Exception {
         testRevertCopy("testDontRevertCopyWitLocalPropertiesModificationsSpecialOption", false, true, false, true, true, SVNStatusType.STATUS_UNVERSIONED, true, SVNStatusType.STATUS_UNVERSIONED);
     }
 
     @Test
     public void testRevertCopyWithoutLocalModificationsRecursiveRevert() throws Exception {
         testRevertCopy("testRevertCopyWithoutLocalModificationsRecursiveRevert", true, false, false, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testRevertCopyWithLocalModificationsRecursiveRevert() throws Exception {
         testRevertCopy("testRevertCopyWithLocalModificationsRecursiveRevert", true, false, true, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testRevertCopyWithoutLocalModificationsEvenWithSpecialOptionRecursiveRevert() throws Exception {
         testRevertCopy("testRevertCopyWithoutLocalModificationsEvenWithSpecialOptionRecursiveRevert", true, true, false, false, true, SVNStatusType.STATUS_UNVERSIONED, false, null);
     }
 
     @Test
     public void testDontRevertCopyWitLocalTextModificationsSpecialOptionRecursiveRevert() throws Exception {
         testRevertCopy("testDontRevertCopyWitLocalTextModificationsSpecialOptionRecursiveRevert", true, true, true, false, true, SVNStatusType.STATUS_UNVERSIONED, true, SVNStatusType.STATUS_UNVERSIONED);
     }
 
     @Test
     public void testDontRevertCopyWitLocalPropertiesModificationsSpecialOptionRecursiveRevert() throws Exception {
         testRevertCopy("testDontRevertCopyWitLocalPropertiesModificationsSpecialOptionRecursiveRevert", true, true, false, true, true, SVNStatusType.STATUS_UNVERSIONED, true, SVNStatusType.STATUS_UNVERSIONED);
     }
 
     @Test
     public void testUnmodifiedFileIsUntouchedPreserveModifiedOption() throws Exception {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUnmodifiedFileIsUntouchedPreserveModifiedOption", options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("file");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File file = workingCopy.getFile("file");
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             revert.setSingleTarget(SvnTarget.fromFile(file));
             revert.setDepth(SVNDepth.INFINITY);
             revert.setPreserveModifiedCopies(true);
             revert.run();
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             Assert.assertTrue(file.isFile());
             Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(file).getNodeStatus());
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
     @Test
     public void testModifiedFileIsRevertedPreserveModifiedOption() throws Exception {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testModifiedFileIsRevertedPreserveModifiedOption", options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("file");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File file = workingCopy.getFile("file");
             TestUtil.writeFileContentsString(file, "contents");
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             revert.setSingleTarget(SvnTarget.fromFile(file));
             revert.setDepth(SVNDepth.INFINITY);
             revert.setPreserveModifiedCopies(true);
             revert.run();
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             Assert.assertTrue(file.isFile());
             Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(file).getNodeStatus());
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
 
     @Test
     public void testUnmodifiedFileIsUntouchedPreserveModifiedOptionRecursiveRevert() throws Exception {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUnmodifiedFileIsUntouchedPreserveModifiedOptionRecursiveRevert", options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("file");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File file = workingCopy.getFile("file");
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             revert.setSingleTarget(SvnTarget.fromFile(workingCopy.getWorkingCopyDirectory()));
             revert.setDepth(SVNDepth.INFINITY);
             revert.setPreserveModifiedCopies(true);
             revert.run();
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             Assert.assertTrue(file.isFile());
             Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(file).getNodeStatus());
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
 
     @Test
     public void testModifiedFileIsRevertedPreserveModifiedOptionRecursiveRevert() throws Exception {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testModifiedFileIsRevertedPreserveModifiedOptionRecursiveRevert", options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("file");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File file = workingCopy.getFile("file");
             TestUtil.writeFileContentsString(file, "contents");
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             revert.setSingleTarget(SvnTarget.fromFile(workingCopy.getWorkingCopyDirectory()));
             revert.setDepth(SVNDepth.INFINITY);
             revert.setPreserveModifiedCopies(true);
             revert.run();
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             Assert.assertTrue(file.isFile());
             Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(file).getNodeStatus());
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
     @Test
     public void testModifiedCopiedFileSpecialOptionDeeperTree() throws Exception {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".test", options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("sourceDirectory/sourceFile");
             commitBuilder.addDirectory("targetDirectory");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File targetFile = workingCopy.getFile("targetDirectory/targetFile");
 
             workingCopy.copy("sourceDirectory/sourceFile", "targetDirectory/targetFile");
             TestUtil.writeFileContentsString(targetFile, "contents");
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             revert.setSingleTarget(SvnTarget.fromFile(workingCopy.getWorkingCopyDirectory()));
             revert.setDepth(SVNDepth.INFINITY);
             revert.setPreserveModifiedCopies(true);
             revert.run();
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             Assert.assertEquals(SVNStatusType.STATUS_UNVERSIONED, statuses.get(targetFile).getNodeStatus());
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
     private void testRevertCopy(String testName, boolean recursiveRevert, boolean preserveModifiedCopies, boolean modifyFileContents, boolean modifyProperties, boolean expectedFileExistence16, SVNStatusType expectedNodeStatus16, boolean expectedFileExistence17, SVNStatusType expectedNodeStatus17) throws SVNException {
         final TestOptions options = TestOptions.getInstance();
 
         final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
         final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + "." + testName, options);
         try {
             final SVNURL url = sandbox.createSvnRepository();
 
             final CommitBuilder commitBuilder = new CommitBuilder(url);
             commitBuilder.addFile("sourceFile");
             commitBuilder.commit();
 
             final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
             final File targetFile = workingCopy.getFile("targetFile");
             workingCopy.copy("sourceFile", "targetFile");
 
             if (modifyFileContents) {
                 TestUtil.writeFileContentsString(targetFile, "contents");
             }
 
             if (modifyProperties) {
                 workingCopy.setProperty(targetFile, "custom", SVNPropertyValue.create("custom value"));
             }
 
             final List<SVNEvent> events = new ArrayList<SVNEvent>();
 
             svnOperationFactory.setEventHandler(new ISVNEventHandler() {
                @Override
                 public void handleEvent(SVNEvent event, double progress) throws SVNException {
                     events.add(event);
                 }
 
                @Override
                 public void checkCancelled() throws SVNCancelException {
                 }
             });
 
             final SvnRevert revert = svnOperationFactory.createRevert();
             if (recursiveRevert) {
                 revert.setSingleTarget(SvnTarget.fromFile(workingCopy.getWorkingCopyDirectory()));
                 revert.setDepth(SVNDepth.INFINITY);
             } else {
                 revert.setSingleTarget(SvnTarget.fromFile(targetFile));
             }
             revert.setPreserveModifiedCopies(preserveModifiedCopies);
             revert.run();
 
             Assert.assertEquals(1, events.size());
             Assert.assertEquals(SVNEventAction.REVERT, events.get(0).getAction());
             Assert.assertEquals(targetFile, events.get(0).getFile());
 
             final Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopy.getWorkingCopyDirectory());
             if (TestUtil.isNewWorkingCopyTest()) {
                 Assert.assertEquals(expectedFileExistence17, targetFile.exists());
                 if (expectedNodeStatus17 == null) {
                     Assert.assertNull(statuses.get(targetFile));
                 } else {
                     Assert.assertEquals(expectedNodeStatus16, statuses.get(targetFile).getNodeStatus());
                 }
             } else {
                 Assert.assertEquals(expectedFileExistence16, targetFile.exists());
                 if (expectedNodeStatus16 == null) {
                     Assert.assertNull(statuses.get(targetFile));
                 } else {
                     Assert.assertEquals(expectedNodeStatus16, statuses.get(targetFile).getNodeStatus());
                 }
             }
 
         } finally {
             svnOperationFactory.dispose();
             sandbox.dispose();
         }
     }
 
     private String getTestName() {
         return "RevertTest";
     }
 }
