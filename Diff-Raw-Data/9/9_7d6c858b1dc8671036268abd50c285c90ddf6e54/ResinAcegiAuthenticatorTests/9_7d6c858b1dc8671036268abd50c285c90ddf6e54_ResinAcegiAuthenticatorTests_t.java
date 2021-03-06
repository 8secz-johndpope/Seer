 /* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.springframework.security.adapters.resin;
 
 import junit.framework.TestCase;
 
 import org.springframework.security.GrantedAuthority;
 import org.springframework.security.GrantedAuthorityImpl;
 
 import org.springframework.security.adapters.PrincipalSpringSecurityUserToken;
 
 import java.security.Principal;
 
 import javax.servlet.ServletException;
 
 
 /**
  * Tests {@link ResinAcegiAuthenticator}.
  *
  * @author Ben Alex
  * @version $Id:ResinAcegiAuthenticatorTests.java 2151 2007-09-22 11:54:13Z luke_t $
  */
 public class ResinAcegiAuthenticatorTests extends TestCase {
     //~ Instance fields ================================================================================================
 
     private final String ADAPTER_KEY = "my_key";
 
     //~ Constructors ===================================================================================================
 
     public ResinAcegiAuthenticatorTests() {
         super();
     }
 
     public ResinAcegiAuthenticatorTests(String arg0) {
         super(arg0);
     }
 
     //~ Methods ========================================================================================================
 
     public static void main(String[] args) {
         junit.textui.TestRunner.run(ResinAcegiAuthenticatorTests.class);
     }
 
     public final void setUp() throws Exception {
         super.setUp();
     }
 
     public void testAdapterAbortsIfAppContextDoesNotContainAnAuthenticationBean()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-invalid.xml");
         adapter.setKey(ADAPTER_KEY);
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertEquals("Bean context must contain at least one bean of type AuthenticationManager",
                 expected.getMessage());
         }
     }
 
     public void testAdapterAbortsIfNoAppContextSpecified()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setKey(ADAPTER_KEY);
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertEquals("appContextLocation must be defined", expected.getMessage());
         }
 
         adapter.setAppContextLocation("");
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertEquals("appContextLocation must be defined", expected.getMessage());
         }
     }
 
     public void testAdapterAbortsIfNoKeySpecified() throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertEquals("key must be defined", expected.getMessage());
         }
 
         adapter.setKey("");
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertEquals("key must be defined", expected.getMessage());
         }
     }
 
     public void testAdapterAbortsWithIncorrectApplicationContextLocation()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("FILE_DOES_NOT_EXIST");
         adapter.setKey(ADAPTER_KEY);
 
         try {
             adapter.init();
             fail("Should have thrown ServletException");
         } catch (ServletException expected) {
             assertTrue(expected.getMessage().startsWith("Cannot locate"));
         }
     }
 
     public void testAdapterStartsUpSuccess() throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertTrue(true);
     }
 
     public void testAuthenticationFailsForIncorrectPassword()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertEquals(null, adapter.loginImpl("rod", "kangaroo"));
     }
 
     public void testAuthenticationFailsForIncorrectUserName()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertEquals(null, adapter.loginImpl("melissa", "koala"));
     }
 
     public void testAuthenticationSuccess() throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
 
         Principal result = adapter.loginImpl("rod", "koala");
 
         if (!(result instanceof PrincipalSpringSecurityUserToken)) {
             fail("Should have returned PrincipalSpringSecurityUserToken");
         }
 
         PrincipalSpringSecurityUserToken castResult = (PrincipalSpringSecurityUserToken) result;
         assertEquals("rod", castResult.getPrincipal());
         assertEquals("koala", castResult.getCredentials());
        assertEquals("ROLE_TELLER", castResult.getAuthorities()[1].getAuthority());
        assertEquals("ROLE_SUPERVISOR", castResult.getAuthorities()[0].getAuthority());
         assertEquals(ADAPTER_KEY.hashCode(), castResult.getKeyHash());
     }
 
     public void testAuthenticationSuccessUsingAlternateMethod()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
 
         Principal result = adapter.loginImpl(null, null, null, "rod", "koala");
 
         if (!(result instanceof PrincipalSpringSecurityUserToken)) {
             fail("Should have returned PrincipalSpringSecurityUserToken");
         }
 
         PrincipalSpringSecurityUserToken castResult = (PrincipalSpringSecurityUserToken) result;
         assertEquals("rod", castResult.getPrincipal());
         assertEquals("koala", castResult.getCredentials());
        assertEquals("ROLE_TELLER", castResult.getAuthorities()[1].getAuthority());
        assertEquals("ROLE_SUPERVISOR", castResult.getAuthorities()[0].getAuthority());
         assertEquals(ADAPTER_KEY.hashCode(), castResult.getKeyHash());
     }
 
     public void testAuthenticationWithNullPasswordHandledGracefully()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertEquals(null, adapter.loginImpl("rod", null));
     }
 
     public void testAuthenticationWithNullUserNameHandledGracefully()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertEquals(null, adapter.loginImpl(null, "koala"));
     }
 
     public void testGetters() throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         assertEquals(ADAPTER_KEY, adapter.getKey());
         assertEquals("org/springframework/security/adapters/adaptertest-valid.xml", adapter.getAppContextLocation());
     }
 
     public void testHasRoleWithANullPrincipalFails() throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertTrue(!adapter.isUserInRole(null, null, null, null, "ROLE_ONE"));
     }
 
     public void testHasRoleWithAPrincipalTheAdapterDidNotCreateFails()
         throws Exception {
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertTrue(!adapter.isUserInRole(null, null, null,
                 new Principal() {
                 public String getName() {
                     return "MockPrincipal";
                 }
             }, "ROLE_ONE"));
     }
 
     public void testHasRoleWithPrincipalAcegiUserToken()
         throws Exception {
         PrincipalSpringSecurityUserToken token = new PrincipalSpringSecurityUserToken("KEY", "Test", "Password",
                 new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl("ROLE_TWO")},
                 null);
         ResinAcegiAuthenticator adapter = new ResinAcegiAuthenticator();
         adapter.setAppContextLocation("org/springframework/security/adapters/adaptertest-valid.xml");
         adapter.setKey(ADAPTER_KEY);
         adapter.init();
         assertTrue(adapter.isUserInRole(null, null, null, token, "ROLE_ONE"));
         assertTrue(adapter.isUserInRole(null, null, null, token, "ROLE_ONE"));
         assertTrue(!adapter.isUserInRole(null, null, null, token, "ROLE_WE_DO_NOT_HAVE"));
     }
 }
