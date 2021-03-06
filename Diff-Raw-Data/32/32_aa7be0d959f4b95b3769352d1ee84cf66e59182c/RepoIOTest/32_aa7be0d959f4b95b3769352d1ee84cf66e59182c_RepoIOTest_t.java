 package test.ccn.network.daemons.repo;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.Arrays;
 
 import org.junit.AfterClass;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import com.parc.ccn.data.ContentName;
 import com.parc.ccn.data.ContentObject;
 import com.parc.ccn.data.query.Interest;
 import com.parc.ccn.data.security.PublisherID;
 import com.parc.ccn.data.security.PublisherKeyID;
 import com.parc.ccn.library.io.repo.RepositoryOutputStream;
 import com.parc.ccn.library.profiles.SegmentationProfile;
 import com.parc.ccn.network.daemons.repo.RFSImpl;
 import com.parc.ccn.network.daemons.repo.Repository;
 import com.parc.ccn.network.daemons.repo.RepositoryException;
 
 /**
  * 
  * @author rasmusse
  * 
  * To run this test you must first unzip repotest.zip into the directory "repotest"
  * and then run the ccnd and the repository with that directory as its root.
  */
 
 public class RepoIOTest extends RepoTestBase {
 	
 	protected static String _repoTestDir = "repotest";
 	
 	@BeforeClass
 	public static void setUpBeforeClass() throws Exception {
 		RepoTestBase.setUpBeforeClass();
 	}
 	
 	@AfterClass
 	public static void cleanup() throws Exception {
 	}
 	
 	@Before
 	public void setUp() throws Exception {
 	}
 	
 	@Test
 	public void testReadViaRepo() throws Throwable {
 		ContentName name = ContentName.fromNative("/repoTest/data1");
 		
		// Since we have 2 pieces of data with the name /repoTest/data1 we need to compute both
 		// digests to make sure we get the right data.
 		ContentName name1 = new ContentName(name, ContentObject.contentDigest("Here's my data!"));
 		ContentName clashName = ContentName.fromNative("/" + RFSImpl.META_DIR + "/repoTest/data1");
 		ContentName digestName = new ContentName(name, ContentObject.contentDigest("Testing2"));
 		String tooLongName = "0123456789";
 		for (int i = 0; i < 30; i++)
 			tooLongName += "0123456789";
		
		// Have 2 pieces of data with the same name here too.
 		ContentName longName = ContentName.fromNative("/repoTest/" + tooLongName);
		longName = new ContentName(longName, ContentObject.contentDigest("Long name!"));
 		ContentName badCharName = ContentName.fromNative("/repoTest/" + "*x?y<z>u");
 		ContentName badCharLongName = ContentName.fromNative("/repoTest/" + tooLongName + "*x?y<z>u");
 		//PublisherKeyID pkid1 = new PublisherKeyID("9s4o5j263snpdl59phc5vf0jhpqgdtghg155smuo2gbnk5ui8f3");
 		//PublisherKeyID pkid2 = new PublisherKeyID("1paij2p7setof6ognk3b34q0hesnv1jpov07h9qvqnveqahv9ml2");
 		//PublisherKeyID pkid2 = new PublisherKeyID("-6ldct6o3h27gp7f8bsksr5veh380uc670voem50580h5le0m9au");
 		
 		checkData(name1, "Here's my data!");
 		checkData(clashName, "Clashing Name");
 		checkData(digestName, "Testing2");
 		checkData(longName, "Long name!");
 		checkData(badCharName, "Funny characters!");
 		checkData(badCharLongName, "Long and funny");
 		//checkDataAndPublisher(name, "Testing2", pkid1);
 		//checkDataAndPublisher(name, "Testing2", pkid2);
 	}
 	
 	@Test
 	public void testPolicyViaCCN() throws Exception {
 		System.out.println("Testing namespace policy setting");
 		checkNameSpace("/repoTest/data2", true);
 		FileInputStream fis = new FileInputStream(_topdir + "/test/ccn/network/daemons/repo/policyTest.xml");
 		byte [] content = new byte[fis.available()];
 		fis.read(content);
 		fis.close();
 		RepositoryOutputStream ros = putLibrary.repoOpen(ContentName.fromNative(_globalPrefix + '/' + 
 				_repoName + '/' + Repository.REPO_DATA + '/' + Repository.REPO_POLICY), null,
 				putLibrary.getDefaultPublisher());
 		ros.write(content, 0, content.length);
 		ros.close();
 		Thread.sleep(1000);
 		checkNameSpace("/repoTest/data3", false);
 		checkNameSpace("/testNameSpace/data1", true);
 	}
 	
 	@Test
 	public void testWriteToRepo() throws Exception {
 		System.out.println("Testing writing streams to repo");
 		byte [] data = new byte[4000];
 		byte value = 1;
 		for (int i = 0; i < data.length; i++)
 			data[i] = value++;
 		RepositoryOutputStream ros = putLibrary.repoOpen(ContentName.fromNative("/testNameSpace/stream"), 
 														null, putLibrary.getDefaultPublisher());
 		ros.setBlockSize(100);
 		ros.write(data, 0, data.length);
 		ros.close();
 		Thread.sleep(5000);
 		for (int i = 0; i < 40; i++) {
 			byte [] testData = new byte[100];
 			System.arraycopy(data, i * 100, testData, 0, 100);
 			if (!checkDataFromFile(new File(_repoTestDir + File.separator + "/testNameSpace/stream"), testData, i, false)) {
 				Assert.assertTrue(checkDataFromFile(new File(_repoTestDir + File.separator + RFSImpl.META_DIR + File.separator 
 						+ RFSImpl.ENCODED_FILES + "/0testNameSpace/0stream"), testData, i, true));
 			}
 		}
 	}
 	
 	private void checkData(ContentName name, String data) throws IOException, InterruptedException{
 		checkData(new Interest(name), data.getBytes());
 	}
 	private void checkData(Interest interest, byte[] data) throws IOException, InterruptedException{
 		ContentObject testContent = getLibrary.get(interest, 10000);
 		Assert.assertFalse(testContent == null);
 		Assert.assertTrue(Arrays.equals(data, testContent.content()));		
 	}
 	private void checkDataAndPublisher(ContentName name, String data, PublisherKeyID publisher) 
 				throws IOException, InterruptedException {
 		Interest interest = new Interest(name, new PublisherID(publisher));
 		ContentObject testContent = getLibrary.get(interest, 10000);
 		Assert.assertFalse(testContent == null);
 		Assert.assertEquals(data, new String(testContent.content()));
 		Assert.assertTrue(testContent.signedInfo().getPublisherKeyID().equals(publisher));
 	}
 	private boolean checkDataFromFile(File testFile, byte[] data, int block, boolean inMeta) throws RepositoryException {
 		if (!testFile.isDirectory()) {
 			return false;
 		}
 		Assert.assertTrue(testFile.isDirectory());
 		File [] contents = testFile.listFiles();
 		Assert.assertFalse(contents == null);
 		//if (contents.length == 1) {
 			//contents = contents[0].listFiles();
 			//Assert.assertFalse(contents == null);
 		//}
 		byte[] segmentBytes = new byte[block == 0 ? 1 : 2];
 		segmentBytes[0] = SegmentationProfile.SEGMENT_MARKER;
 		if (block > 0)
 			segmentBytes[1] = (byte)block;
 		byte[] encodedName = RFSImpl.encodeComponent(segmentBytes);
 		String segmentName = new String(encodedName);
 		if (inMeta)
 			segmentName = "0" + segmentName;
 		int slot = -1;
 		for (int i = 0; i < contents.length; i++) {
 			if (contents[i].getName().endsWith(segmentName)) {
 				slot = i;
 				break;
 			}
 		}
 		if (slot == -1)
 			return false;
 		Assert.assertTrue(contents[slot].isDirectory());
 		contents = contents[slot].listFiles();
 		Assert.assertFalse(contents == null);
 		ContentObject co = RFSImpl.getContentFromFile(contents[0]);
 		Assert.assertTrue(co != null);
 		Assert.assertTrue(Arrays.equals(data, co.content()));
 		return true;
 	}
 	
 	private void checkNameSpace(String contentName, boolean expected) throws Exception {
 		ContentName name = ContentName.fromNative(contentName);
 		RepositoryOutputStream ros = putLibrary.repoOpen(name, null, putLibrary.getDefaultPublisher());
 		byte [] data = "Testing 1 2 3".getBytes();
 		ros.write(data, 0, data.length);
 		ContentName baseName = ros.getBaseName();
 		try {
 			ros.close();
 		} catch (IOException ex) {}	// File not put causes an I/O exception
 		Thread.sleep(1000);
 		File testFile = new File("repotest" + baseName);
 		if (expected)
 			Assert.assertTrue(testFile.exists());
 		else
 			Assert.assertFalse(testFile.exists());
 	}
 }
