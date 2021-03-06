 /* This code is part of WoT, a plugin for Freenet. It is distributed 
  * under the GNU General Public License, version 2 (or at your option
  * any later version). See http://www.gnu.org/ for details of the GPL. */
 package plugins.WoT;
 
 import java.net.MalformedURLException;
 
 import plugins.WoT.exceptions.DuplicateTrustException;
 import plugins.WoT.exceptions.InvalidParameterException;
 import plugins.WoT.exceptions.NotInTrustTreeException;
 import plugins.WoT.exceptions.NotTrustedException;
 import plugins.WoT.exceptions.UnknownIdentityException;
 
 import com.db4o.ext.ExtObjectContainer;
 
 /**
  * @author xor (xor@freenetproject.org), Julien Cornuwel (batosai@freenetproject.org)
  */
 public class WoTTest extends DatabaseBasedTest {
 
 	private final String uriO = "USK@8VTguDZehMlShIb7Q~F1wYpOnDK7pSZVwrGArACP~04,MK0wfPtNud~nWyp~oy0Kr1~kFuYfJ9~LlxNribWD4Us,AQACAAE/WoT/0";
 	private final String uriS = "USK@hAOgofNsQEbT~aRqGuXwt8vI7tOeQVCrcIHrD9PvS6g,fG7LHRJhczCAApOwgaXNJO41L8wRIZj9oN37LSLZZY8,AQACAAE/WoT/0";
 	private final String uriA = "USK@MF2Vc6FRgeFMZJ0s2l9hOop87EYWAydUZakJzL0OfV8,fQeN-RMQZsUrDha2LCJWOMFk1-EiXZxfTnBT8NEgY00,AQACAAE/WoT/0";
 	private final String uriB = "USK@R3Lp2s4jdX-3Q96c0A9530qg7JsvA9vi2K0hwY9wG-4,ipkgYftRpo0StBlYkJUawZhg~SO29NZIINseUtBhEfE,AQACAAE/WoT/0";
 	private final String uriC = "USK@qd-hk0vHYg7YvK2BQsJMcUD5QSF0tDkgnnF6lnWUH0g,xTFOV9ddCQQk6vQ6G~jfL6IzRUgmfMcZJ6nuySu~NUc,AQACAAE/WoT/0";
 
 	public void testInitTrustTree() throws MalformedURLException, InvalidParameterException, UnknownIdentityException, NotInTrustTreeException {
 		mWoT.createOwnIdentity(uriA, uriA, "A", true, "Test"); /* This also initializes the trust tree */
 		
 		flushCaches();
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 0);
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllTrusts().size() == 0);
 		assertTrue(mWoT.getAllScores().size() == 1);
 		
 		flushCaches();
 		OwnIdentity a = mWoT.getOwnIdentityByURI(uriA);
 
 		Score score = mWoT.getScore(a,a);
 		assertTrue(score.getScore() == 100);
 		assertTrue(score.getRank() == 0);
 		assertTrue(score.getCapacity() == 100);
 		assertTrue(score.getTreeOwner() == a);
 		assertTrue(score.getTarget() == a);
 	}
 	
 	public void testSetTrust1() throws InvalidParameterException, MalformedURLException {
 		OwnIdentity a = new OwnIdentity(uriA, uriA, "A", true);
 		Identity b = new Identity(uriB, "B", true);
		/* We store them manually so that the WoT does not initialize the trust tree */
		mWoT.getDB().store(a, 5);
		mWoT.getDB().store(b, 5);
 		
 		// With A's trust tree not initialized, B shouldn't get a Score.
 		mWoT.setTrust(a, b, (byte)10, "Foo");
 		
 		flushCaches();
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllTrusts().size() == 1);
 		assertTrue(mWoT.getAllScores().size() == 0);
 	}
 	
 	public void testSetTrust2() throws MalformedURLException, InvalidParameterException, DuplicateTrustException, NotTrustedException, NotInTrustTreeException {
 
 		OwnIdentity a = mWoT.createOwnIdentity(uriA, uriA, "A", true, "Test"); /* Initializes it's trust tree */
 		Identity b = new Identity(uriB, "B", true);
		mWoT.getDB().store(b, 5);
 		
 		mWoT.setTrust(a, b, (byte)100, "Foo");
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllTrusts().size() == 1);
 		assertTrue(mWoT.getAllScores().size() == 2);
 		
 		// Check the Trust object
 		flushCaches();
 		Trust t = mWoT.getTrust(a, b);
 		assertTrue(t.getTruster() == a);
 		assertTrue(t.getTrustee() == b);
 		assertTrue(t.getValue() == 100);
 		assertTrue(t.getComment().equals("Foo"));
 		
 		// Check a's Score object
 		flushCaches();
 		Score scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 		
 		// Check B's Score object
 		flushCaches();
 		Score scoreB = mWoT.getScore(a, b);
 		assertTrue(scoreB.getScore() == 100);
 		assertTrue(scoreB.getRank() == 1);
 		assertTrue(scoreB.getCapacity() == 40);
 		
 		// Change the trust value and comment
 		mWoT.setTrust(a, b, (byte)50, "Bar");
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllTrusts().size() == 1);
 		assertTrue(mWoT.getAllScores().size() == 2);
 		
 		// Check the Trust object
 		flushCaches();
 		t = mWoT.getTrust(a, b);
 		assertTrue(t.getTruster() == a);
 		assertTrue(t.getTrustee() == b);
 		assertTrue(t.getValue() == 50);
 		assertTrue(t.getComment().equals("Bar"));
 		
 		// Check a's Score object
 		flushCaches();
 		scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 
 		// Check B's Score object
 		flushCaches();
 		scoreB = mWoT.getScore(a, b);
 		assertTrue(scoreB.getScore() == 50);
 		assertTrue(scoreB.getRank() == 1);
 		assertTrue(scoreB.getCapacity() == 40);
 	}
 	
 	public void testRemoveTrust() throws MalformedURLException, InvalidParameterException, UnknownIdentityException,
 		NotInTrustTreeException {
 		
 		ExtObjectContainer db = mWoT.getDB();
 		
 		OwnIdentity a = mWoT.createOwnIdentity(uriA, uriA, "A", true, "Test");
		Identity b = new Identity(uriB, "B", true); /* Do not init the trust tree */
		Identity c = new Identity(uriC, "C", true); /* Do not init the trust tree */
		db.store(b);
		db.store(c);
 		
 		mWoT.setTrust(a, b, (byte)100, "Foo");
 		mWoT.setTrustWithoutCommit(b, c, (byte)50, "Bar"); // There is no committing setTrust() for non-OwnIdentity (trust-list import uses rollback() on error)
 		db.commit();
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 2);
 		assertTrue(mWoT.getAllTrusts().size() == 2);
 		assertTrue(mWoT.getAllScores().size() == 3);
 		
 		// Check a's Score object
 		flushCaches();
 		Score scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 		
 		// Check B's Score object
 		flushCaches();
 		Score scoreB = mWoT.getScore(a, b);
 		assertTrue(scoreB.getScore() == 100);
 		assertTrue(scoreB.getRank() == 1);
 		assertTrue(scoreB.getCapacity() == 40);
 		
 		// Check C's Score object
 		flushCaches();
 		Score scoreC = mWoT.getScore(a, c);
 		assertTrue(scoreC.getScore() == 20);
 		assertTrue(scoreC.getRank() == 2);
 		assertTrue(scoreC.getCapacity() == 16);
 		
 		mWoT.setTrust(a, b, (byte)-1, "Bastard");
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 2);
 		assertTrue(mWoT.getAllTrusts().size() == 2);
 		assertTrue(mWoT.getAllScores().size() == 2);
 		
 		// Check a's Score object
 		flushCaches();
 		scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 		
 		// Check B's Score object
 		flushCaches();
 		scoreB = mWoT.getScore(a, b);
 		assertTrue(scoreB.getScore() == -1);
 		assertTrue(scoreB.getRank() == 1);
 		assertTrue(scoreB.getCapacity() == 0);
 		
 		// C should not have a score anymore
 		try {
 			mWoT.getScore(a, c);
 			fail();
 		}
 		catch (NotInTrustTreeException e) {}
 	}
 	
 	public void testTrustLoop() throws MalformedURLException, InvalidParameterException, NotInTrustTreeException {
 		ExtObjectContainer db = mWoT.getDB();
 		
 		OwnIdentity a = mWoT.createOwnIdentity(uriA, uriA, "A", true, "Test");
		Identity b = new Identity(uriB, "B", true); /* Do not init the trust tree */
		Identity c = new Identity(uriC, "C", true); /* Do not init the trust tree */
		db.store(b);
		db.store(c);
 		
 		mWoT.setTrust(a, b, (byte)100, "Foo");
 		mWoT.setTrustWithoutCommit(b, c, (byte)50, "Bar"); // There is no committing setTrust() for non-OwnIdentity (trust-list import uses rollback() on error)
 		mWoT.setTrustWithoutCommit(c, a, (byte)100, "Bleh");
 		mWoT.setTrustWithoutCommit(c, b, (byte)50, "Oops");
 		db.commit();
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllOwnIdentities().size() == 1);
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 2);
 		assertTrue(mWoT.getAllTrusts().size() == 4);
 		assertTrue(mWoT.getAllScores().size() == 3);
 
 		// Check a's Score object
 		flushCaches();
 		Score scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 		
 		// Check B's Score object
 		flushCaches();
 		Score scoreB = mWoT.getScore(a, b);
 		assertTrue(scoreB.getScore() == 100); // 100 and not 108 because own trust values override calculated scores.
 		assertTrue(scoreB.getRank() == 1);
 		assertTrue(scoreB.getCapacity() == 40);
 		
 		// Check C's Score object
 		flushCaches();
 		Score scoreC = mWoT.getScore(a, c);
 		assertTrue(scoreC.getScore() == 20);
 		assertTrue(scoreC.getRank() == 2);
 		assertTrue(scoreC.getCapacity() == 16);
 	}
 	
 	public void testOwnIndentitiesTrust() throws MalformedURLException, InvalidParameterException, NotInTrustTreeException {
 		OwnIdentity a = mWoT.createOwnIdentity(uriA, uriA, "A", true, "Test");
 		OwnIdentity b = mWoT.createOwnIdentity(uriB, uriB, "B", true, "Test");
 
 		mWoT.setTrust(a, b, (byte)100, "Foo");
 		mWoT.setTrust(b, a, (byte)100, "Bar");
 		
 		// Check we have the correct number of objects
 		flushCaches();
 		assertTrue(mWoT.getAllOwnIdentities().size() == 2);
 		assertTrue(mWoT.getAllNonOwnIdentities().size() == 0);
 		assertTrue(mWoT.getAllTrusts().size() == 2);
 		assertTrue(mWoT.getAllScores().size() == 4);
 		
 		// Check a's own Score object
 		flushCaches();
 		Score scoreA = mWoT.getScore(a, a);
 		assertTrue(scoreA.getScore() == 100);
 		assertTrue(scoreA.getRank() == 0);
 		assertTrue(scoreA.getCapacity() == 100);
 		
 		// Check a's Score object
 		flushCaches();
 		Score scoreAfromB = mWoT.getScore(b, a);
 		assertTrue(scoreAfromB.getScore() == 100);
 		assertTrue(scoreAfromB.getRank() == 1);
 		assertTrue(scoreAfromB.getCapacity() == 40);
 				
 		// Check B's own Score object
 		flushCaches();
 		Score scoreB = mWoT.getScore(a, a);
 		assertTrue(scoreB.getScore() == 100);
 		assertTrue(scoreB.getRank() == 0);
 		assertTrue(scoreB.getCapacity() == 100);
 
 		// Check B's Score object
 		flushCaches();
 		Score scoreBfromA = mWoT.getScore(b, a);
 		assertTrue(scoreBfromA.getScore() == 100);
 		assertTrue(scoreBfromA.getRank() == 1);
 		assertTrue(scoreBfromA.getCapacity() == 40);
 	}
 	
 	/**
 	 * Test whether the same scores are calculated if trust lists are fetched in different order.
 	 */
 	public void testStability() throws Exception {
 		ExtObjectContainer db = mWoT.getDB();
 			
 		OwnIdentity o = mWoT.createOwnIdentity(uriO, uriO, "O", true, "Test"); // Tree owner
 		Identity s = new Identity(uriS, "S", true); mWoT.storeAndCommit(s); // Seed identity
 		Identity a = new Identity(uriA, "A", true); mWoT.storeAndCommit(a); // A / B are downloaded in different orders.
 		Identity b = new Identity(uriB, "B", true); mWoT.storeAndCommit(b);
 		Identity c = new Identity(uriC, "C", true); mWoT.storeAndCommit(c);
 		
 		// You get all the identities from the seed identity.
 		mWoT.setTrust(o, s, (byte)100, "I trust the seed identity.");
 		mWoT.setTrustWithoutCommit(s, a, (byte)0, "Introduction puzzle solved");
 		mWoT.setTrustWithoutCommit(s, b, (byte)0, "Introduction puzzle solved");
 		mWoT.setTrustWithoutCommit(s, c, (byte)0, "Introduction puzzle solved");
 		db.commit();
 
 		// First you download A. A distrusts B and trusts C
 		mWoT.setTrustWithoutCommit(a, b, (byte)-100, "Distrust");
 		mWoT.setTrustWithoutCommit(a, c, (byte)100, "Trust");
 		db.commit();
 		
 		// Then you download B who distrusts A and C
 		mWoT.setTrustWithoutCommit(b, a, (byte)-100, "Distrust");
 		mWoT.setTrustWithoutCommit(b, c, (byte)-100, "Trust");
 		db.commit();
 		
 		final int scoreA = mWoT.getScore(o, a).getScore();
 		final int scoreB = mWoT.getScore(o, b).getScore();
 		final int scoreC = mWoT.getScore(o, c).getScore();
 		
 		// Now we want a fresh WoT.
 		tearDown();
 		setUp();
 		db = mWoT.getDB();
 		
 		o = mWoT.createOwnIdentity(uriO, uriO, "O", true, "Test");
 		s = new Identity(uriS, "S", true); mWoT.storeAndCommit(s);		
 		a = new Identity(uriA, "A", true); mWoT.storeAndCommit(a);
 		b = new Identity(uriB, "B", true); mWoT.storeAndCommit(b);
 		c = new Identity(uriC, "C", true); mWoT.storeAndCommit(c);
 		
 		// You get all the identities from the seed identity.
 		mWoT.setTrust(o, s, (byte)100, "I trust the seed identity.");
 		mWoT.setTrustWithoutCommit(s, a, (byte)0, "Introduction puzzle solved");
 		mWoT.setTrustWithoutCommit(s, b, (byte)0, "Introduction puzzle solved");
 		mWoT.setTrustWithoutCommit(s, c, (byte)0, "Introduction puzzle solved");
 		db.commit();
 		
 		// Alternative download order: First B...
 		mWoT.setTrustWithoutCommit(b, a, (byte)-100, "Distrust");
 		mWoT.setTrustWithoutCommit(b, c, (byte)-100, "Trust");
 		db.commit();
 		
 		// .. then A
 		mWoT.setTrustWithoutCommit(a, b, (byte)-100, "Distrust");
 		mWoT.setTrustWithoutCommit(a, c, (byte)100, "Trust");
 		db.commit();
 		
 		assertEquals(scoreA, mWoT.getScore(o, a).getScore());
 		assertEquals(scoreB, mWoT.getScore(o, b).getScore());
 		assertEquals(scoreC, mWoT.getScore(o, c).getScore());
 	}
 }
