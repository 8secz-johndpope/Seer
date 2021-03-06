 import models.RCACase;
 import models.User;
 import org.junit.Test;
 import play.mvc.Before;
 import play.test.UnitTest;
 import utils.EncodingUtils;
 
 public class UserTest extends UnitTest {
 
 	@Test
 	public void testPasswordChangeTest() {
 		String userEmail = "userPassword@arcatool.fi";
 		String userPassword = "password";
 		new User(userEmail, userPassword).save();
 		User normalUser = User.find("byEmailAndPassword", userEmail, EncodingUtils.encodeSHA1(userPassword)).first();
 		assertNotNull(normalUser);
 		assertEquals(normalUser.password, EncodingUtils.encodeSHA1(userPassword));
 		normalUser.changePassword("newPassword");
 		assertEquals(normalUser.password, EncodingUtils.encodeSHA1("newPassword"));
 	}
 
     @Test
     public void addRcaCaseTest() {
         User rcaCaseUser = new User("rcaCaseUser@arcatool.fi", "password").save();
 	    assertNotNull(rcaCaseUser);
 	    rcaCaseUser.addRCACase("new unique rca case", "type", true, "test company", "14", true).save();
 	    rcaCaseUser.save();
 	    rcaCaseUser.refresh();
 	    assertTrue(rcaCaseUser.cases.size() == 1);
 	    RCACase rcaCase = RCACase.find("byName", "new unique rca case").first();
 	    assertNotNull(rcaCase);
 	    assertTrue(rcaCaseUser.cases.contains(rcaCase));
 	    assertTrue(rcaCase.problem.name.equals("new unique rca case"));
     }
 
 }
