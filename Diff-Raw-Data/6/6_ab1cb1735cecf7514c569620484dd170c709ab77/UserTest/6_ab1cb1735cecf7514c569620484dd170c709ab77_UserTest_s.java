 package cours.ulaval.glo4003.model;
 
 import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
 
 import org.junit.Before;
 import org.junit.Test;
 
 public class UserTest {
 
 	private static String IDUL = "brgaa";
 	private static String PASSWORD = "motdepasse";
 	private static Role ROLE = Role.Directeur;
 
 	private User user;
 
 	@Before
 	public void setUp() {
 		user = new User(IDUL, PASSWORD, ROLE);
 	}
 
 	@Test
 	public void canInstantiateAUser() {
 
 		assertNotNull(user);
 	}
 
 	@Test
 	public void canGetUsername() {
 
 		assertEquals(IDUL, user.getIdul());
 	}
 
 	@Test
 	public void canValidateCredentials() {
 
 		assertTrue(user.validateCredentials(PASSWORD));
 	}
 
 	@Test
 	public void canValidateWrongCredentials() {
 
		assertFalse(user.validateCredentials(anyString()));
 	}
 
 	@Test
 	public void canSetRoleOfAUser() {
 		user.setRole(Role.Enseignant);
 
 		assertEquals(Role.Enseignant, user.getRole());
 	}
 }
