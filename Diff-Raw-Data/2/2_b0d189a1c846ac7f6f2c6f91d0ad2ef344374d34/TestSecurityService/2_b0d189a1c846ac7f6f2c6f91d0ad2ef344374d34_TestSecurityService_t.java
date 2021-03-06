 /*
  ************************************************************************************
  * Copyright (C) 2001-2009 encuestame: system online surveys Copyright (C) 2009
  * encuestame Development Team.
  * Licensed under the Apache Software License version 2.0
  * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to  in writing,  software  distributed
  * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
  * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
  * specific language governing permissions and limitations under the License.
  ************************************************************************************
  */
 package org.encuestame.test.business.service;
 
 import static org.junit.Assert.*;
 
 import java.util.Date;
 import java.util.List;
 
 import org.encuestame.business.service.SecurityService;
 import org.encuestame.business.service.imp.ISecurityService;
 import org.encuestame.core.exception.EnMeDomainNotFoundException;
 import org.encuestame.core.exception.EnMeExpcetion;
 import org.encuestame.persistence.domain.security.SecGroup;
 import org.encuestame.persistence.domain.security.SecPermission;
 import org.encuestame.persistence.domain.security.SecUser;
 import org.encuestame.persistence.domain.security.SecUserSecondary;
 import org.encuestame.persistence.domain.security.SecUserTwitterAccounts;
 import org.encuestame.persistence.domain.EnMePermission;
 import org.encuestame.core.util.ConvertDomainBean;
 import org.encuestame.test.business.service.config.AbstractServiceBase;
 import org.encuestame.test.config.AbstractBaseUnitBeans;
 import org.encuestame.utils.security.SignUpBean;
 import org.encuestame.utils.security.UnitTwitterAccountBean;
 import org.encuestame.utils.web.UnitGroupBean;
 import org.encuestame.utils.web.UnitPermission;
 import org.encuestame.utils.web.UnitUserBean;
 import org.junit.Before;
 import org.junit.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.test.annotation.ExpectedException;
 
 /**
  * Test Security Service.
  * @author Picado, Juan juan@encuestame.org
  * @since 08/11/2009 11:35:01
  * @version $Id$
  */
 public class TestSecurityService extends AbstractServiceBase{
 
     /** {@link SecurityService}. **/
     @Autowired
     private ISecurityService securityService;
 
     /** User Primary. **/
     private SecUser userPrimary;
 
     /** User Secondary. **/
     private SecUserSecondary secUserSecondary;
 
     /** {@link SecPermission}. **/
     private SecPermission secPermission;
 
     /**
      * Before.
      */
     @Before
     public void initService(){
         securityService.setSuspendedNotification(getActivateNotifications());
         this.userPrimary = createUser();
         this.secUserSecondary = createSecondaryUser("default", this.userPrimary);
         final SecGroup group = createGroups("admin");
         final SecGroup group2 = createGroups("editors");
       //  this.secUserSecondary.getSecGroups().add(group);
        // this.secUserSecondary.getSecGroups().add(group2);
         this.secPermission = createPermission(EnMePermission.ENCUESTAME_EDITOR.name());
         createPermission(EnMePermission.ENCUESTAME_OWNER.name());
         createPermission(EnMePermission.ENCUESTAME_PUBLISHER.name());
         createPermission(EnMePermission.ENCUESTAME_ADMIN.name());
         this.secUserSecondary.getSecUserPermissions().add(this.secPermission);
         getSecGroup().saveOrUpdate(this.secUserSecondary);
     }
 
     /**
      * Test findUserByUserName.
      */
     @Test
     public void testfindUserByUserName(){
         final SecUserSecondary secondary = this.securityService.findUserByUserName(this.secUserSecondary.getUsername());
         assertEquals(this.secUserSecondary.getUid(), secondary.getUid());
         assertEquals(this.secUserSecondary.getPassword(), secondary.getPassword());
         assertEquals(this.secUserSecondary.getCompleteName(), secondary.getCompleteName());
     }
 
     /**
      * Test findUserByEmail.
      */
     @Test
     public void testfindUserByEmail(){
         final UnitUserBean secondary = this.securityService.findUserByEmail("fake@email.com");
         assertNull(secondary);
         final UnitUserBean secondary2 = this.securityService.findUserByEmail(this.secUserSecondary.getUserEmail());
         assertEquals(this.secUserSecondary.getUid(), secondary2.getId());
         assertEquals(this.secUserSecondary.getUsername(), secondary2.getUsername());
     }
 
     /**
      * Test loadGroups.
      * @throws EnMeDomainNotFoundException
      */
     @Test
     public void testloadGroups() throws EnMeDomainNotFoundException{
         createGroups("admin", this.userPrimary);
         createGroups("user", this.userPrimary);
         final List<UnitGroupBean> groups = this.securityService.loadGroups(this.secUserSecondary.getUsername());
         assertEquals(groups.size(), 2);
     }
 
     /**
      * Load Groups Exception.
      * @throws EnMeDomainNotFoundException
      */
     @Test(expected = EnMeDomainNotFoundException.class)
     public void testloadGroupsException() throws EnMeDomainNotFoundException{
          this.securityService.loadGroups("xxxxxx");
     }
 
     /**
      * test updateTwitterAccount.
      */
     @Test
     public void testupdateTwitterAccount(){
         SecUserTwitterAccounts account = createDefaultSettedTwitterAccount(this.userPrimary);
         final UnitTwitterAccountBean bean = ConvertDomainBean.convertTwitterAccountToBean(account);
         this.securityService.updateTwitterAccount(bean, "12345", false);
         account = getSecUserDao().getTwitterAccount(account.getId());
         assertEquals(account.getTwitterPassword(), "12345");
         assertEquals(account.getVerfied(), false);
         //with id null.
         this.securityService.updateTwitterAccount(new UnitTwitterAccountBean(), "12345", false);
         bean.setAccountId(1234L);
         this.securityService.updateTwitterAccount(bean, "12345", false);
     }
 
     /**
      * test updateSecretTwitterCredentials.
      * @throws EnMeExpcetion
      */
     @Test
     public void testupdateSecretTwitterCredentials() throws EnMeExpcetion{
          SecUserTwitterAccounts account = createDefaultSettedTwitterAccount(this.userPrimary);
          final UnitTwitterAccountBean bean = ConvertDomainBean.convertTwitterAccountToBean(account);
          bean.setKey(getProperty("twitter.test.token"));
          bean.setSecret(getProperty("twitter.test.tokenSecret"));
          this.securityService.updateSecretTwitterCredentials(bean, this.secUserSecondary.getUsername());
          //pin null
          bean.setPin(null);
          this.securityService.updateSecretTwitterCredentials(bean, this.secUserSecondary.getUsername());
          //pinn empty
          bean.setPin("");
          this.securityService.updateSecretTwitterCredentials(bean, this.secUserSecondary.getUsername());
          //fake data
          account.setToken("fake key");
          account.setSecretToken("fake secret");
          getSecUserDao().saveOrUpdate(account);
          this.securityService.updateSecretTwitterCredentials(bean, this.secUserSecondary.getUsername());
     }
 
     /**
      * test updateOAuthTokenSocialAccount.
      * @throws EnMeExpcetion
      */
     @Test
     public void testupdateOAuthTokenSocialAccount() throws EnMeExpcetion{
         SecUserTwitterAccounts account = createDefaultSettedTwitterAccount(this.userPrimary);
         this.securityService.updateOAuthTokenSocialAccount(account.getId(), "12345", "fakeTokenSecret", this.secUserSecondary.getUsername());
         account = getSecUserDao().getTwitterAccount(account.getId());
         assertEquals(account.getSecretToken(), "fakeTokenSecret");
     }
 
     /**
      * test updateOAuthTokenSocialAccount with exception.
      * @throws EnMeExpcetion
      */
     @Test(expected = EnMeExpcetion.class)
     public void testupdateOAuthTokenSocialAccountException() throws EnMeExpcetion{
         this.securityService.updateOAuthTokenSocialAccount(12345L, "12345", "fakeTokenSecret", this.secUserSecondary.getUsername());
     }
 
     /**
      * test getTwitterAccount.
      */
     @Test
     public void testgetTwitterAccount(){
         SecUserTwitterAccounts account = createDefaultSettedTwitterAccount(this.userPrimary);
         final UnitTwitterAccountBean accountBean = this.securityService.getTwitterAccount(account.getId());
         assertEquals(account.getId(), accountBean.getAccountId());
     }
 
     /**
      * test deleteUser.
      * @throws EnMeDomainNotFoundException
      */
     @Test(timeout = 30000)
     public void testdeleteUser() throws EnMeDomainNotFoundException{
         final SecUserSecondary tempUser = createSecondaryUser("second user", this.userPrimary);
         final Long id = tempUser.getUid();
         this.securityService.deleteUser(ConvertDomainBean.convertSecondaryUserToUserBean(tempUser));
         final SecUserSecondary tempUser2 = createSecondaryUser("second user", getProperty("mail.test.email"), this.userPrimary);
         this.securityService.setSuspendedNotification(true);
         this.securityService.deleteUser(ConvertDomainBean.convertSecondaryUserToUserBean(tempUser2));
         assertNull(getSecUserDao().getSecondaryUserById(id));
     }
 
     /**
      * test deleteUser.
      * @throws EnMeDomainNotFoundException
      */
     @Test(expected = EnMeDomainNotFoundException.class)
     public void testdeleteUserNotFound() throws EnMeDomainNotFoundException{
         this.securityService.deleteUser(ConvertDomainBean.convertSecondaryUserToUserBean(new SecUserSecondary()));
     }
 
     /**
      * test addNewTwitterAccount.
      * @throws EnMeDomainNotFoundException
      */
     @Test
     public void testaddNewTwitterAccount() throws EnMeDomainNotFoundException{
         this.securityService.addNewTwitterAccount("encuestameTest", this.secUserSecondary.getUsername());
         assertEquals(getSecUserDao().getTwitterAccountByUser(this.userPrimary).size(), 1);
     }
 
     /**
      * Generate Hash Code Invitation.
      */
     @Test
     public void testGenerateHashCodeInvitation(){
         assertNotNull(securityService.generateHashCodeInvitation());
     }
 
     /**
      * Test Load All Permissions.
      */
     @Test
     public void testLoadAllListPermission(){
          assertEquals("Should be equals", 4, securityService.loadAllListPermission().size());
     }
 
     /**
      * @throws Exception
      */
     @Test
     public void testLoadListUsers() throws Exception{
         addGroupUser(super.createSecondaryUser("user 1",this.userPrimary),super.createGroups("editor"));
         addGroupUser(super.createSecondaryUser("user 2",this.userPrimary),super.createGroups("admon"));
    //     assertEquals("Should be equals", 3, securityService.loadListUsers("user 1").size());
     }
 
     /**
      * Test User By Username.
      * @throws EnMeExpcetion EnMeExpcetion
      */
     @Test
     public void testSearchUserByUsername() throws EnMeExpcetion{
       final SecUserSecondary userDomain = createSecondaryUser("user 1",this.userPrimary);
       createSecondaryUser("user 2",this.userPrimary);
       final UnitUserBean userBean = securityService.searchUserByUsername(userDomain.getUsername());
       assertEquals("Should be equals",userDomain.getUsername(),userBean.getUsername());
     }
 
     /**
      * @throws EnMeExpcetion EnMeExpcetion
      */
     @Test
     public void testSearchUserByUsernameNotFound() throws EnMeExpcetion{
         assertNull(securityService.searchUserByUsername("user test"));
     }
 
     /**
      * Test Default User Permission.
      */
     @Test
     public void testDefaulUserPermission(){
         final String defaultPermission = securityService.getDefaultUserPermission();
         assertEquals("Should be","ENCUESTAME_USER".toString(), defaultPermission.toString());
     }
 
 
     /**
      *Test Delete Group.
      */
     @Test
     public void testDeleteGroup(){
         final SecGroup groupDomain = createGroups("admin");
         final Long idGroup = groupDomain.getGroupId();
         final UnitGroupBean group = ConvertDomainBean.convertGroupDomainToBean(groupDomain);
         securityService.deleteGroup(group.getId());
         final SecGroup groupRetrieve = getSecGroup().getGroupById(idGroup);
         assertNull(groupRetrieve);
     }
 
     /**
      * Setter.
      * @param securityService the securityService to set
      */
     public final void setSecurityService(SecurityService securityService) {
         this.securityService = securityService;
     }
 
     /**
      *Test delete Group.
      */
     @Test
     public void testdeleteGroup(){
       final SecGroup groupDomain = createGroups("admin");
       final Long idGroup = groupDomain.getGroupId();
       final UnitGroupBean group = ConvertDomainBean.convertGroupDomainToBean(groupDomain);
       securityService.deleteGroup(group.getId());
       final SecGroup groupRetrieve = getSecGroup().getGroupById(idGroup);
       assertNull(groupRetrieve);
 
     }
 
     /**
     *Test delete User.
      * @throws EnMeExpcetion exception
      */
     @Test
     public void testDeleteUser() throws EnMeExpcetion{
      final SecUserSecondary secUsers = createSecondaryUser("administrator",this.userPrimary);
      final Long idUser = secUsers.getUid();
      //final String username = secUsers.getUsername();
      final UnitUserBean user = ConvertDomainBean.convertSecondaryUserToUserBean(secUsers);
      securityService.deleteUser(user);
      final SecUser userRetrieve = getSecUserDao().getUserById(idUser);
      assertNull(userRetrieve);
     }
 
 
 
     /**
      * Test Update Group.
      * @throws EnMeExpcetion
      */
     @Test
     public void testUpdateGroup() throws EnMeExpcetion{
       SecGroup secgroups = createGroups("guests");
       Long idGroupUpdate = secgroups.getGroupId();
       UnitGroupBean groupBean = ConvertDomainBean.convertGroupDomainToBean(secgroups);
       groupBean.setGroupName("editors");
       securityService.updateGroup(groupBean);
       SecGroup groupUpdateRetrieve =  getSecGroup().getGroupById(idGroupUpdate);
       assertEquals("Should be","editors",groupUpdateRetrieve.getGroupName());
 
     }
 
     /**
      *Test Update User.
      * @throws EnMeExpcetion exception
      **/
     @Test
     public void testUpdateUser() throws EnMeExpcetion{
       final SecUserSecondary secUsers = createSecondaryUser("developer",this.userPrimary);
       final Long idUser = secUsers.getUid();
       final UnitUserBean userBean = ConvertDomainBean.convertSecondaryUserToUserBean(secUsers);
       userBean.setName("editor");
       securityService.updateUser(userBean);
       final SecUserSecondary userUpdateRetrieve = getSecUserDao().getSecondaryUserById(idUser);
       assertEquals("shouldbe", "editor", userUpdateRetrieve.getCompleteName());
     }
 
     /**
      * Test Create Permission.
      */
     @Test
     public void testCreatePermission(){
       final SecPermission secPerm = createPermission("writer");
       final UnitPermission permissionBean = ConvertDomainBean.convertPermissionToBean(secPerm);
       securityService.createPermission(permissionBean);
       final SecPermission permissionRetrieve = getSecPermissionDaoImp().getPermissionById(secPerm.getIdPermission());
       assertNotNull(permissionRetrieve);
       assertEquals("should be","writer", permissionRetrieve.getPermissionDescription());
 
 
     }
 
     /**
      * Test Renew Password.
      * @throws EnMeExpcetion exception
      */
     @Test
     public void testRenewPassword() throws EnMeExpcetion{
       final SecUserSecondary secUser = createSecondaryUser("paola",this.userPrimary);
       final String passwd = secUser.getPassword();
       final UnitUserBean userPass = ConvertDomainBean.convertSecondaryUserToUserBean(secUser);
       final String retrievePassword = securityService.renewPassword(userPass, passwd);
       assertEquals("should be equals", passwd, retrievePassword);
     }
 
     /**
      * Test Renew Password without Pass.
      * @throws EnMeExpcetion  EnMeExpcetion
      */
    @Test
    @ExpectedException(EnMeExpcetion.class)
    public void testRenewPasswordwithoutPass()throws EnMeExpcetion{
       final SecUserSecondary secUser = createSecondaryUser("diana",this.userPrimary);
       UnitUserBean userPassBean = ConvertDomainBean.convertSecondaryUserToUserBean(secUser);
       final String retrievePassword = securityService.renewPassword(userPassBean,null);
       assertEquals("should be equals", null, retrievePassword);
     }
 
     /**
      * Test Create User without Email.
      * @throws EnMeExpcetion EnMeExpcetion
      **/
       @Test
       @ExpectedException(EnMeExpcetion.class)
        public void testCreateUserwithoutEmail() throws EnMeExpcetion{
         final UnitUserBean userCreateBean = new UnitUserBean();
         userCreateBean.setEmail(null);
         userCreateBean.setUsername("diana");
         securityService.createUser(userCreateBean, this.secUserSecondary.getUsername());
        }
 
       /**
        * Test Create User without Username.
        * @throws EnMeExpcetion EnMeExpcetion
        */
       @Test
       @ExpectedException(EnMeExpcetion.class)
        public void testCreateUserwithoutUsername() throws EnMeExpcetion{
         final UnitUserBean userCreateBean = new UnitUserBean();
         userCreateBean.setEmail("paola@jotadeveloper.com");
         userCreateBean.setUsername(null);
         securityService.createUser(userCreateBean, this.secUserSecondary.getUsername());
        }
 
       /**
        * Create default permissions.
        */
       private void createDefaultPermission(){
           createPermission("ENCUESTAME_USER");
       }
 
       /**
        * Test Create User with Username.
        * @throws EnMeExpcetion EnMeExpcetion
        */
       @Test
        public void testCreateUserwithUsernameEmail() throws EnMeExpcetion{
         createDefaultPermission();
         final UnitUserBean userCreateBean = new UnitUserBean();
         userCreateBean.setEmail("demo3@demo.org");
         userCreateBean.setUsername("demo3");
         userCreateBean.setStatus(true);
         userCreateBean.setName("demo3");
         userCreateBean.setPassword(null);
         userCreateBean.setDateNew(new Date());
         userCreateBean.setPrimaryUserId(createUser().getUid());
         securityService.createUser(userCreateBean, this.secUserSecondary.getUsername());
         //TODO: need assert
         final SecUserSecondary user = getSecUserDao().getUserByUsername(userCreateBean.getUsername());
         assertNotNull("should be equals", user);
         assertEquals("should be equals", 1, user.getSecUserPermissions().size());
        }
 
       /**
        * Test Create User without Password.
        * @throws EnMeExpcetion EnMeExpcetion
        */
       @Test
        public void testCreateUserwithoutPassword() throws EnMeExpcetion{
         createDefaultPermission();
         SecUserSecondary secCreateUser = new SecUserSecondary();
         UnitUserBean userCreateBean = ConvertDomainBean.convertSecondaryUserToUserBean(secCreateUser);
         userCreateBean.setPassword(null);
         userCreateBean.setEmail("demo2@demo.org");
         userCreateBean.setUsername("demo2");
         userCreateBean.setStatus(true);
         userCreateBean.setName("Diana Paola");
         userCreateBean.setPrimaryUserId(createUser().getUid());
         userCreateBean.setDateNew(new Date());
         securityService.createUser(userCreateBean, this.secUserSecondary.getUsername());
         //TODO: need assert
         final SecUserSecondary user = getSecUserDao().getUserByUsername(userCreateBean.getUsername());
         assertNotNull("should be equals", user);
         assertEquals("should be equals", 1, user.getSecUserPermissions().size());
        }
 
       /**
        * Test Create User without Password.
        * @throws EnMeExpcetion EnMeExpcetion
        */
       @Test
        public void testCreateUserwithPassword() throws EnMeExpcetion{
         createDefaultPermission();
         SecUserSecondary secCreateUser = new SecUserSecondary();
         UnitUserBean userCreateBean = ConvertDomainBean.convertSecondaryUserToUserBean(secCreateUser);
         userCreateBean.setPassword("12345");
         userCreateBean.setEmail("demo1@demo.org");
         userCreateBean.setUsername("demo1");
         userCreateBean.setStatus(true);
         userCreateBean.setName("Diana Paola");
         userCreateBean.setDateNew(new Date());
         userCreateBean.setPrimaryUserId(createUser().getUid());
         securityService.createUser(userCreateBean, this.secUserSecondary.getUsername());
         //TODO: need assert
         final SecUserSecondary user = getSecUserDao().getUserByUsername(userCreateBean.getUsername());
         assertNotNull("should be equals", user);
         assertEquals("should be equals", 1, user.getSecUserPermissions().size());
        }
 
 
       /**
        * Test Create Group.
      * @throws EnMeDomainNotFoundException
        */
       @Test
       public void testCreateGroup() throws EnMeDomainNotFoundException{
         SecGroup secCreateGroup = new SecGroup();
         secCreateGroup.setGroupId(12L);
         secCreateGroup.setGroupDescriptionInfo("1111");
         secCreateGroup.setGroupName("vvvv");
         secCreateGroup.setIdState(1L);
         final UnitGroupBean createGroupBean = ConvertDomainBean.convertGroupDomainToBean(secCreateGroup);
         securityService.createGroup(createGroupBean, this.secUserSecondary.getUsername());
         //TODO: need assert
       }
 
       /**
        * Test Assing Permission without Id an Username.
        * @throws EnMeExpcetion  EnMeExpcetion
        */
       @Test
       public void testAssignPermissionwithIdUsername() throws EnMeExpcetion{
         final SecUserSecondary secUser = createSecondaryUser("demo",this.userPrimary);
         final UnitUserBean userPermissionBean = ConvertDomainBean.convertSecondaryUserToUserBean(secUser);
         final UnitPermission permissionBean = ConvertDomainBean.convertPermissionToBean(this.secPermission);
         userPermissionBean.setId(userPermissionBean.getId());
         userPermissionBean.setUsername("demo");
         securityService.assignPermission(userPermissionBean, permissionBean);
         assertEquals("should be equals", 1, secUser.getSecUserPermissions().size());
       }
 
         /**
          * Test Assign Permission without Username.
          * @throws EnMeExpcetion EnMeExpcetion
          */
         @Test
         @ExpectedException(EnMeExpcetion.class)
         public void testAssignPermissionwithoutIdUsername() throws EnMeExpcetion{
           final UnitUserBean userPermissionBean = new UnitUserBean();
           final UnitPermission permissionBean = ConvertDomainBean.convertPermissionToBean(this.secPermission);
           //modify id user id.
           userPermissionBean.setId(1L);
           securityService.assignPermission(userPermissionBean, permissionBean);
       }
 
 
         /**
          * Test Assign Permission with Permission.
          * @throws EnMeExpcetion EnMeExpcetion
          */
         @Test
         @ExpectedException(EnMeExpcetion.class)
         public void testAssignPermissionwithPermission() throws EnMeExpcetion{
             final SecUserSecondary secUser = createSecondaryUser("juanpicado2",this.userPrimary);
             final UnitUserBean userPermissionBean = ConvertDomainBean.convertSecondaryUserToUserBean(secUser);
             final UnitPermission permissionBean = new UnitPermission();
             //modify id permission.
             permissionBean.setId(1L);
             permissionBean.setPermission("auditor");
             securityService.assignPermission(userPermissionBean, permissionBean);
       }
 
 
         /**
          * Test Assign Permission with Permission Id and User Id.
          * @throws EnMeExpcetion EnMeExpcetion
          */
         @Test
         @ExpectedException(EnMeExpcetion.class)
         public void testAssignPermissionwithPermissionIdandUserId() throws EnMeExpcetion{
           UnitUserBean userPermissionBean = new UnitUserBean();
           UnitPermission permissionBean = new UnitPermission();
           securityService.assignPermission(userPermissionBean, permissionBean);
 
       }
 
         /**
          * Test Assign Group.
          * @throws EnMeExpcetion EnMeExpcetion
          */
         @Test
         public void testAssignGroup() throws EnMeExpcetion{
           final SecUserSecondary users=  createSecondaryUser("juanpicado",this.userPrimary);
           final SecGroup groups = createGroups("encuestador");
           UnitUserBean userBean = ConvertDomainBean.convertSecondaryUserToUserBean(users);
           UnitGroupBean groupBean = ConvertDomainBean.convertGroupDomainToBean(groups);
           //securityService.assingGroup(userBean, groupBean);
         }
 
         /**
          * Test searchUsersByEmail.
          */
         @Test
         public void testsearchUsersByEmail(){
             final SecUserSecondary email = createSecondaryUser("emailUser1", this.userPrimary);
             List<SecUserSecondary> emailUsers = this.securityService.searchUsersByEmail(email.getUserEmail());
             assertEquals(emailUsers.size(), 1);
         }
 
         /**
          * Test searchUsersByEmail.
          */
         @Test
         public void testsearchUsersByUsername(){
             createSecondaryUser("emailUser2", this.userPrimary);
             List<SecUserSecondary> emailUsers = this.securityService.searchUsersByUsername("emailUser2");
             assertEquals(emailUsers.size(), 1);
         }
 
         /**
          * Test assingGroupFromUser.
          * @throws EnMeExpcetion exception
          */
     /*    @Test
         public void testassingGroupFromUser() throws EnMeExpcetion{
             final UnitUserBean userBean = ConvertDomainBean.convertSecondaryUserToUserBean(this.secUserSecondary);
             final UnitGroupBean groupBean = ConvertDomainBean
                                 .convertGroupDomainToBean(createGroups("admin", this.userPrimary));
             this.securityService.assingGroupFromUser(userBean, groupBean);
             final SecUserSecondary userWithGroup = this.securityService.findUserByUserName(userBean.getUsername());
             assertEquals(userWithGroup.getSecGroups().size(), 3);
         }*/
 
         /**
          * Test assingGroupFromUser with Exception.
          * @throws EnMeExpcetion Exception
          */
         @Test(expected = EnMeExpcetion.class)
         public void testassingGroupFromUserException() throws EnMeExpcetion{
             final UnitUserBean userBean = ConvertDomainBean.convertSecondaryUserToUserBean(this.secUserSecondary);
            //this.securityService.assingGroupFromUser(userBean, new UnitGroupBean());
         }
 
         /**
          *Test Load Bean Permission.
          * @throws EnMeExpcetion EnMeExpcetion
          */
         @Test
         public void testloadBeanPermission() throws EnMeExpcetion{
             final UnitPermission permission = securityService.loadBeanPermission(this.secPermission.getPermission());
             assertNotNull(permission);
         }
         /**
          * Test singupUser.
          */
         @Test
         public void testsingupUser(){
             final SignUpBean bean = createSignUpBean("newUser", "newUser@gmail.com", "12345");
             this.securityService.singupUser(bean);
         }
 }
